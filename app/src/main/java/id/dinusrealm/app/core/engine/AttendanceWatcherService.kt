package id.dinusrealm.app.core.engine

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import id.dinusrealm.app.DinusRealmApp
import id.dinusrealm.app.core.i18n.currentLanguage
import id.dinusrealm.app.core.i18n.tr
import id.dinusrealm.app.core.model.AttendanceLogEntry
import id.dinusrealm.app.core.model.OnlineAttendanceItem
import id.dinusrealm.app.core.model.ScheduleSlot
import id.dinusrealm.app.core.network.ApiResult
import id.dinusrealm.app.core.repo.DinusRepository
import id.dinusrealm.app.widget.DinusScheduleWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Lightweight ForegroundService active ONLY during scheduled class hours.
 * Polls `get-krs-online` + `get-jadwal-hari-ini` every 45s (20s near end when auto-attend is on),
 * fires an interactive notification with `[Attend Now]` as soon as a lecturer opens attendance
 * mid-session, and optionally submits last-minute auto-attendance if still unmarked.
 */
class AttendanceWatcherService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val notifiedMasterIds = mutableSetOf<String>()
    private var isLoopRunning = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val lang = currentLanguage(this)
        val initialNotif = NotificationHelper.buildWatcherNotification(
            this,
            tr(lang, "Monitoring active class attendance...", "Memantau jadwal presensi kelas aktif...")
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelper.WATCHER_FOREGROUND_ID,
                initialNotif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NotificationHelper.WATCHER_FOREGROUND_ID, initialNotif)
        }

        if (!isLoopRunning) {
            isLoopRunning = true
            scope.launch { runWatcherLoop() }
        }
        return START_NOT_STICKY
    }

    private suspend fun runWatcherLoop() {
        val container = (application as DinusRealmApp).container
        try {
            while (scope.isActive) {
                val settings = container.vault.getSettings()
                if (!container.vault.hasCredentials() || !settings.attendanceWatcherEnabled) {
                    break
                }

                val (nowMin, nowSec) = ScheduleTimeUtil.currentMinutesAndSecond()
                val cachedToday = loadTodayCachedSlots(container)
                val activeCached = cachedToday.filter {
                    !it.isAttended && ScheduleTimeUtil.isWithinClassWindow(nowMin, it.startTime, it.endTime)
                }

                val krsRes = container.api.getKrsOnline()
                val todayRes = container.api.getTodaySchedule()

                val krsOnlineList = (krsRes as? ApiResult.Success)?.data.orEmpty()
                val todayApiList = (todayRes as? ApiResult.Success)?.data.orEmpty()
                val onlineItems = mergeWatcherCandidates(
                    krsOnline = krsOnlineList,
                    todaySlots = todayApiList,
                    cachedToday = cachedToday
                )

                // Keep widget & local cache fresh when getTodaySchedule succeeds
                if (todayRes is ApiResult.Success) {
                    val weeklyOnly = container.db.getScheduleSlots().filter { it.dayIndex > 0 }
                    val preservedToday = todayRes.data.map { slot ->
                        val onlineMatch = findMatchingOnline(slot, onlineItems)
                        slot.copy(
                            dayIndex = 0,
                            isAttended = slot.isAttended || (onlineMatch?.isAttended == true),
                            kdds = slot.kdds.ifBlank { onlineMatch?.kdds.orEmpty() },
                            idMaster = onlineMatch?.idMaster?.ifBlank { null } ?: slot.idMaster,
                            isOpenClass = slot.isOpenClass || (onlineMatch?.isOpenClass == true)
                        )
                    }
                    container.db.replaceScheduleSlots(preservedToday + weeklyOnly)
                    container.db.putKv(
                        DinusRepository.KV_TODAY_SLOTS_DATE,
                        if (preservedToday.isNotEmpty()) ScheduleTimeUtil.todayDateKey() else ""
                    )
                    DinusScheduleWidget.updateAll(this)
                }

                // Clear stale open notifications if attendance was recorded via app/web
                for (item in onlineItems) {
                    if (item.isAttended && item.idMaster.isNotBlank() && notifiedMasterIds.remove(item.idMaster)) {
                        NotificationManagerCompat.from(this).cancel(NotificationHelper.notifIdForMaster(item.idMaster))
                    }
                }

                val openUnattended = onlineItems.filter {
                    it.isOpenClass && !it.isAttended && it.idMaster.isNotBlank()
                }
                val activeUnattended = onlineItems.filter {
                    !it.isAttended && ScheduleTimeUtil.isWithinClassWindow(nowMin, it.startTime, it.endTime)
                }

                // Always notify immediately for any genuinely open unattended session,
                // even if SiAdin omitted start/end times on get-krs-online.
                for (item in openUnattended) {
                    if (notifiedMasterIds.add(item.idMaster)) {
                        NotificationHelper.notifyAttendanceOpen(this, item)
                    }
                }

                var nearEnd = false
                val autoCandidates = (activeUnattended + openUnattended).distinctBy {
                    it.idMaster.ifBlank { "${it.kdmk}|${it.courseName}|${it.startTime}" }
                }
                for (item in autoCandidates) {
                    val remainingSec = ScheduleTimeUtil.secondsUntilClassEnd(nowMin, nowSec, item.endTime) ?: Int.MAX_VALUE
                    if (settings.autoAbsenEnabled && remainingSec <= (settings.autoAbsenSecondsBeforeEnd + 180)) {
                        nearEnd = true
                    }

                    if (item.isOpenClass &&
                        !item.isAttended &&
                        item.idMaster.isNotBlank() &&
                        settings.autoAbsenEnabled &&
                        ScheduleTimeUtil.shouldTriggerAutoAbsen(
                            nowMin,
                            nowSec,
                            item.endTime,
                            settings.autoAbsenSecondsBeforeEnd
                        )
                    ) {
                        val submitRes = container.api.submitOnlineAttendance(item.idMaster, item.kdds)
                        val ok = submitRes is ApiResult.Success
                        val msg = when (submitRes) {
                            is ApiResult.Success -> submitRes.data
                            is ApiResult.Error -> submitRes.message
                        }
                        container.db.insertAttendanceLog(
                            AttendanceLogEntry(
                                idMaster = item.idMaster,
                                courseName = item.courseName,
                                mode = "AUTO_LAST_MINUTE",
                                success = ok,
                                message = msg,
                                timestampMillis = System.currentTimeMillis()
                            )
                        )
                        NotificationHelper.notifyAttendanceResult(
                            context = this,
                            notifId = NotificationHelper.notifIdForMaster(item.idMaster),
                            courseName = item.courseName,
                            modeLabel = tr(
                                settings.language,
                                "Auto-Attendance Recorded",
                                "Auto-Absen Berhasil"
                            ),
                            success = ok,
                            message = msg,
                            retryIdMaster = if (!ok) item.idMaster else null,
                            retryKdds = item.kdds
                        )
                    }
                }

                // Keep running while an unattended scheduled class is active or an open session waits
                val stillActive = activeUnattended.isNotEmpty() || activeCached.isNotEmpty() || openUnattended.isNotEmpty()
                if (!stillActive) {
                    break
                }

                delay(if (nearEnd) 20_000L else 45_000L)
            }
        } finally {
            stopSelf()
        }
    }

    private fun loadTodayCachedSlots(container: id.dinusrealm.app.AppContainer): List<ScheduleSlot> =
        ScheduleTimeUtil.resolveTodaySlots(
            allSlots = container.db.getScheduleSlots(),
            savedTodayDateKey = container.db.getKv(DinusRepository.KV_TODAY_SLOTS_DATE)
        )

    private fun findMatchingOnline(slot: ScheduleSlot, items: List<OnlineAttendanceItem>): OnlineAttendanceItem? {
        val normName = slot.courseName.trim().lowercase()
        return items.firstOrNull { item ->
            (!slot.idMaster.isNullOrBlank() && item.idMaster == slot.idMaster) ||
                (slot.kdmk.isNotBlank() && item.kdmk.equals(slot.kdmk, ignoreCase = true)) ||
                (normName.isNotBlank() &&
                    item.courseName.trim().lowercase() == normName &&
                    (slot.klpk.isBlank() || item.klpk.isBlank() || item.klpk.equals(slot.klpk, ignoreCase = true)))
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

private fun matchSlotForOnline(item: OnlineAttendanceItem, slots: List<ScheduleSlot>): ScheduleSlot? {
    val normName = item.courseName.trim().lowercase()
    return slots.firstOrNull { s ->
        (item.idMaster.isNotBlank() && s.idMaster == item.idMaster) ||
            (item.kdmk.isNotBlank() && s.kdmk.equals(item.kdmk, ignoreCase = true)) ||
            (normName.isNotBlank() &&
                s.courseName.trim().lowercase() == normName &&
                (item.klpk.isBlank() || s.klpk.isBlank() || s.klpk.equals(item.klpk, ignoreCase = true)))
    } ?: if (normName.isNotBlank()) {
        slots.firstOrNull { it.courseName.trim().lowercase() == normName }
    } else null
}

private fun matchOnlineForSlot(slot: ScheduleSlot, items: List<OnlineAttendanceItem>): OnlineAttendanceItem? {
    val normName = slot.courseName.trim().lowercase()
    return items.firstOrNull { item ->
        (!slot.idMaster.isNullOrBlank() && item.idMaster == slot.idMaster) ||
            (slot.kdmk.isNotBlank() && item.kdmk.equals(slot.kdmk, ignoreCase = true)) ||
            (normName.isNotBlank() &&
                item.courseName.trim().lowercase() == normName &&
                (slot.klpk.isBlank() || item.klpk.isBlank() || item.klpk.equals(slot.klpk, ignoreCase = true)))
    } ?: if (normName.isNotBlank()) {
        items.firstOrNull { it.courseName.trim().lowercase() == normName }
    } else null
}

internal fun mergeWatcherCandidates(
    krsOnline: List<OnlineAttendanceItem>,
    todaySlots: List<ScheduleSlot>,
    cachedToday: List<ScheduleSlot>
): List<OnlineAttendanceItem> {
    val enrichedOnline = krsOnline.map { item ->
        val todayMatch = matchSlotForOnline(item, todaySlots)
        val cacheMatch = matchSlotForOnline(item, cachedToday)
        item.copy(
            kdmk = item.kdmk.ifBlank { todayMatch?.kdmk.orEmpty() }.ifBlank { cacheMatch?.kdmk.orEmpty() },
            klpk = item.klpk.ifBlank { todayMatch?.klpk.orEmpty() }.ifBlank { cacheMatch?.klpk.orEmpty() },
            startTime = item.startTime.ifBlank { todayMatch?.startTime.orEmpty() }.ifBlank { cacheMatch?.startTime.orEmpty() },
            endTime = item.endTime.ifBlank { todayMatch?.endTime.orEmpty() }.ifBlank { cacheMatch?.endTime.orEmpty() },
            room = item.room.ifBlank { todayMatch?.room.orEmpty() }.ifBlank { cacheMatch?.room.orEmpty() },
            lecturer = item.lecturer.ifBlank { todayMatch?.lecturer.orEmpty() }.ifBlank { cacheMatch?.lecturer.orEmpty() },
            isAttended = item.isAttended || (todayMatch?.isAttended == true),
            isOpenClass = item.isOpenClass || item.idMaster.isNotBlank() || (todayMatch?.isOpenClass == true)
        )
    }

    val merged = enrichedOnline.toMutableList()
    for (slot in todaySlots) {
        if (matchOnlineForSlot(slot, merged) == null) {
            val cacheMatch = cachedToday.firstOrNull {
                (slot.kdmk.isNotBlank() && it.kdmk.equals(slot.kdmk, ignoreCase = true)) ||
                    it.courseName.trim().equals(slot.courseName.trim(), ignoreCase = true)
            }
            merged += OnlineAttendanceItem(
                idMaster = slot.idMaster.orEmpty(),
                kdds = slot.kdds.ifBlank { cacheMatch?.kdds.orEmpty() },
                kdmk = slot.kdmk.ifBlank { cacheMatch?.kdmk.orEmpty() },
                klpk = slot.klpk.ifBlank { cacheMatch?.klpk.orEmpty() },
                courseName = slot.courseName,
                startTime = slot.startTime.ifBlank { cacheMatch?.startTime.orEmpty() },
                endTime = slot.endTime.ifBlank { cacheMatch?.endTime.orEmpty() },
                room = slot.room.ifBlank { cacheMatch?.room.orEmpty() },
                lecturer = slot.lecturer.ifBlank { cacheMatch?.lecturer.orEmpty() },
                isAttended = slot.isAttended,
                isOpenClass = slot.isOpenClass || !slot.idMaster.isNullOrBlank(),
                meetingNumber = slot.meetingNumber
            )
        }
    }
    return merged
}
