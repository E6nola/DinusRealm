package id.dinusrealm.app.core.engine

import id.dinusrealm.app.core.model.ScheduleSlot
import java.util.Calendar
import java.util.Locale

/**
 * Pure schedule-time calculation helpers (JVM unit-testable with explicit timestamps).
 */
object ScheduleTimeUtil {

    fun parseMinutesOfDay(hhmm: String): Int? {
        val clean = hhmm.trim().replace('.', ':')
        val match = Regex("""^(\d{1,2}):(\d{2})$""").matchEntire(clean) ?: return null
        val h = match.groupValues[1].toIntOrNull() ?: return null
        val m = match.groupValues[2].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    fun calendarDayToSiadinDay(calDayOfWeek: Int): Int = when (calDayOfWeek) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        Calendar.SUNDAY -> 7
        else -> 0
    }

    /**
     * Computes an epoch millis trigger today at `hhmm` shifted by `offsetMinutes` (e.g., -30).
     * Returns null if time string is invalid or trigger time has already passed.
     */
    fun computeTriggerMillis(nowMillis: Long, hhmm: String, offsetMinutes: Int): Long? {
        val targetMin = parseMinutesOfDay(hhmm) ?: return null
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, targetMin / 60)
            set(Calendar.MINUTE, targetMin % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, offsetMinutes)
        }
        return cal.timeInMillis.takeIf { it > nowMillis }
    }

    fun isWithinClassWindow(
        nowMinutes: Int,
        startTime: String,
        endTime: String,
        graceBeforeMin: Int = 2,
        graceAfterMin: Int = 2
    ): Boolean {
        val start = parseMinutesOfDay(startTime) ?: return false
        val end = parseMinutesOfDay(endTime) ?: return false
        return nowMinutes in (start - graceBeforeMin)..(end + graceAfterMin)
    }

    fun secondsUntilClassEnd(nowMinutes: Int, nowSecond: Int, endTime: String): Int? {
        val end = parseMinutesOfDay(endTime) ?: return null
        val nowTotalSec = nowMinutes * 60 + nowSecond.coerceIn(0, 59)
        val endTotalSec = end * 60
        return endTotalSec - nowTotalSec
    }

    fun shouldTriggerAutoAbsen(
        nowMinutes: Int,
        nowSecond: Int,
        endTime: String,
        thresholdSeconds: Int
    ): Boolean {
        val remaining = secondsUntilClassEnd(nowMinutes, nowSecond, endTime) ?: return false
        return remaining in 1..thresholdSeconds.coerceAtLeast(15)
    }

    /**
     * Selects the active class first, otherwise the earliest upcoming class today, otherwise null.
     */
    fun findHeroSlot(todaySlots: List<ScheduleSlot>, nowMinutes: Int): ScheduleSlot? {
        if (todaySlots.isEmpty()) return null
        val active = todaySlots.firstOrNull { isWithinClassWindow(nowMinutes, it.startTime, it.endTime, graceBeforeMin = 15, graceAfterMin = 0) }
        if (active != null) return active
        return todaySlots
            .filter { (parseMinutesOfDay(it.endTime) ?: Int.MAX_VALUE) > nowMinutes }
            .minByOrNull { parseMinutesOfDay(it.startTime) ?: Int.MAX_VALUE }
    }

    fun currentMinutesAndSecond(nowMillis: Long = System.currentTimeMillis()): Pair<Int, Int> {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val mins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val sec = cal.get(Calendar.SECOND)
        return mins to sec
    }

    fun currentSiadinDay(nowMillis: Long = System.currentTimeMillis()): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return calendarDayToSiadinDay(cal.get(Calendar.DAY_OF_WEEK))
    }

    fun todayDateKey(nowMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun computeNextDailyTriggerMillis(nowMillis: Long, hour: Int = 5, minute: Int = 30): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= nowMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return cal.timeInMillis
    }

    fun resolveTodaySlots(
        allSlots: List<ScheduleSlot>,
        savedTodayDateKey: String?,
        nowMillis: Long = System.currentTimeMillis()
    ): List<ScheduleSlot> {
        val explicitToday = if (savedTodayDateKey == todayDateKey(nowMillis)) {
            allSlots.filter { it.dayIndex == 0 }
        } else emptyList()
        return if (explicitToday.isNotEmpty()) explicitToday
        else allSlots.filter { it.dayIndex == currentSiadinDay(nowMillis) }
    }
}
