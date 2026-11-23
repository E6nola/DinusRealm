package id.dinusrealm.app

import id.dinusrealm.app.core.engine.ScheduleTimeUtil
import id.dinusrealm.app.core.engine.mergeWatcherCandidates
import id.dinusrealm.app.core.i18n.dayName
import id.dinusrealm.app.core.i18n.tr
import id.dinusrealm.app.core.model.AppLanguage
import id.dinusrealm.app.core.model.AppSettings
import id.dinusrealm.app.core.model.OnlineAttendanceItem
import id.dinusrealm.app.core.model.ScheduleSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ScheduleTimeUtilTest {

    @Test
    fun `parseMinutesOfDay handles colon and dot formats`() {
        assertEquals(520, ScheduleTimeUtil.parseMinutesOfDay("08:40"))
        assertEquals(520, ScheduleTimeUtil.parseMinutesOfDay("8.40"))
        assertNull(ScheduleTimeUtil.parseMinutesOfDay("25:99"))
        assertNull(ScheduleTimeUtil.parseMinutesOfDay(""))
    }

    @Test
    fun `calendarDayToSiadinDay maps Monday through Sunday cleanly`() {
        assertEquals(1, ScheduleTimeUtil.calendarDayToSiadinDay(Calendar.MONDAY))
        assertEquals(5, ScheduleTimeUtil.calendarDayToSiadinDay(Calendar.FRIDAY))
        assertEquals(7, ScheduleTimeUtil.calendarDayToSiadinDay(Calendar.SUNDAY))
    }

    @Test
    fun `computeTriggerMillis computes T-30m accurately and rejects past triggers`() {
        val base = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 23, 7, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val trigger = ScheduleTimeUtil.computeTriggerMillis(base, "08:40", -30)
        assertNotNull(trigger)
        val resCal = Calendar.getInstance().apply { timeInMillis = trigger!! }
        assertEquals(8, resCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(10, resCal.get(Calendar.MINUTE))

        // Past trigger at 07:50 - 30 = 07:20 (< 07:30) returns null
        assertNull(ScheduleTimeUtil.computeTriggerMillis(base, "07:50", -30))
    }

    @Test
    fun `isWithinClassWindow catches 4 minutes before end even with late lecturer opening`() {
        // Class 08:40 - 10:20 (520..620), check at 10:16 (616) -> 4 minutes remaining
        assertTrue(ScheduleTimeUtil.isWithinClassWindow(616, "08:40", "10:20"))
        assertFalse(ScheduleTimeUtil.isWithinClassWindow(500, "08:40", "10:20"))
        assertFalse(ScheduleTimeUtil.isWithinClassWindow(625, "08:40", "10:20"))
    }

    @Test
    fun `shouldTriggerAutoAbsen triggers inside last 90 seconds threshold only`() {
        // Class ends at 10:20 (620 min). At 10:18:45 (618m 45s) -> 75 seconds left
        assertTrue(ScheduleTimeUtil.shouldTriggerAutoAbsen(618, 45, "10:20", thresholdSeconds = 90))
        // At 10:15:00 -> 300 seconds left (too early)
        assertFalse(ScheduleTimeUtil.shouldTriggerAutoAbsen(615, 0, "10:20", thresholdSeconds = 90))
        // At 10:20:05 -> already ended (-5s)
        assertFalse(ScheduleTimeUtil.shouldTriggerAutoAbsen(620, 5, "10:20", thresholdSeconds = 90))
    }

    @Test
    fun `findHeroSlot prefers currently running class else next upcoming`() {
        val slots = listOf(
            ScheduleSlot("1", "MK1", "A", "Matkul Pagi", dayIndex = 1, dayName = "Senin", startTime = "07:00", endTime = "08:40", room = "H.3.1"),
            ScheduleSlot("2", "MK2", "A", "Matkul Siang", dayIndex = 1, dayName = "Senin", startTime = "10:20", endTime = "12:00", room = "H.4.2")
        )
        // At 08:00 (480): Matkul Pagi is active
        assertEquals("Matkul Pagi", ScheduleTimeUtil.findHeroSlot(slots, 480)?.courseName)
        // At 09:00 (540): Matkul Pagi ended, Matkul Siang is next
        assertEquals("Matkul Siang", ScheduleTimeUtil.findHeroSlot(slots, 540)?.courseName)
        // At 14:00 (840): all done
        assertNull(ScheduleTimeUtil.findHeroSlot(slots, 840))
    }

    @Test
    fun `default settings use English language and empty nickname with accurate i18n helpers`() {
        val defaults = AppSettings()
        assertEquals(AppLanguage.ENGLISH, defaults.language)
        assertEquals("", defaults.nickname)
        assertEquals("Monday", dayName(1, AppLanguage.ENGLISH))
        assertEquals("Senin", dayName(1, AppLanguage.INDONESIAN))
        assertEquals("Home", tr(AppLanguage.ENGLISH, "Home", "Beranda"))
        assertEquals("Beranda", tr(AppLanguage.INDONESIAN, "Home", "Beranda"))
    }

    @Test
    fun `computeNextDailyTriggerMillis picks today before 0530 and tomorrow after 0530`() {
        val before0530 = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 23, 4, 15, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val trigSameDay = Calendar.getInstance().apply {
            timeInMillis = ScheduleTimeUtil.computeNextDailyTriggerMillis(before0530)
        }
        assertEquals(23, trigSameDay.get(Calendar.DAY_OF_MONTH))
        assertEquals(5, trigSameDay.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, trigSameDay.get(Calendar.MINUTE))

        val after0530 = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 23, 10, 5, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val trigNextDay = Calendar.getInstance().apply {
            timeInMillis = ScheduleTimeUtil.computeNextDailyTriggerMillis(after0530)
        }
        assertEquals(24, trigNextDay.get(Calendar.DAY_OF_MONTH))
        assertEquals(5, trigNextDay.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, trigNextDay.get(Calendar.MINUTE))
    }

    @Test
    fun `mergeWatcherCandidates backfills late-opened krsOnline slot from cache and keeps extra classes`() {
        val krsOnline = listOf(
            OnlineAttendanceItem(
                idMaster = "99123",
                kdds = "77",
                kdmk = "A12.101",
                klpk = "A12.01",
                courseName = "Basis Data",
                startTime = "",
                endTime = "",
                room = "",
                lecturer = "",
                isAttended = false,
                isOpenClass = true
            )
        )
        val cachedToday = listOf(
            ScheduleSlot(
                id = "c1", kdmk = "A12.101", klpk = "A12.01", courseName = "Basis Data",
                dayIndex = 1, dayName = "Senin", startTime = "10:00", endTime = "12:00",
                room = "H.4.11", lecturer = "Dr. Dosen"
            )
        )
        val todaySlots = listOf(
            ScheduleSlot(
                id = "t2", kdmk = "A12.202", klpk = "A12.01", courseName = "Sistem Operasi",
                dayIndex = 0, dayName = "Hari Ini", startTime = "13:00", endTime = "15:00",
                room = "H.3.2"
            )
        )

        val merged = mergeWatcherCandidates(krsOnline, todaySlots, cachedToday)
        assertEquals(2, merged.size)
        val lateOpened = merged[0]
        assertEquals("99123", lateOpened.idMaster)
        assertEquals("77", lateOpened.kdds)
        assertEquals("10:00", lateOpened.startTime)
        assertEquals("12:00", lateOpened.endTime)
        assertEquals("H.4.11", lateOpened.room)
        assertTrue(lateOpened.isOpenClass)
        assertEquals("Sistem Operasi", merged[1].courseName)
    }

    @Test
    fun `resolveTodaySlots ignores stale explicit cache from yesterday`() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 23, 10, 5, 0) // Monday (dayIndex = 1)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val all = listOf(
            ScheduleSlot("old", "MK0", "A", "Yesterday Class", dayIndex = 0, dayName = "Hari Ini", startTime = "08:00", endTime = "10:00", room = "H.1"),
            ScheduleSlot("mon", "MK1", "A", "Monday Class", dayIndex = 1, dayName = "Senin", startTime = "10:00", endTime = "12:00", room = "H.2")
        )

        val staleFallback = ScheduleTimeUtil.resolveTodaySlots(all, "2026-11-22", now)
        assertEquals(1, staleFallback.size)
        assertEquals("Monday Class", staleFallback[0].courseName)

        val freshToday = ScheduleTimeUtil.resolveTodaySlots(all, "2026-11-23", now)
        assertEquals(1, freshToday.size)
        assertEquals("Yesterday Class", freshToday[0].courseName)
    }
}
