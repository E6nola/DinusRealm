package id.dinusrealm.app.core.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import id.dinusrealm.app.DinusRealmApp
import id.dinusrealm.app.core.repo.DinusRepository
import id.dinusrealm.app.widget.DinusScheduleWidget

/**
 * Receives scheduled exact/inexact OS wake-ups to display T-30m class room notifications
 * or launch the AttendanceWatcherService when a class window begins.
 */
class ClassReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? DinusRealmApp ?: return
        val settings = app.container.vault.getSettings()

        when (intent.action) {
            ACTION_CLASS_REMINDER -> {
                if (!settings.classNotifEnabled) return
                val courseName = intent.getStringExtra(EXTRA_COURSE_NAME) ?: return
                val room = intent.getStringExtra(EXTRA_ROOM) ?: ""
                val startTime = intent.getStringExtra(EXTRA_START_TIME) ?: ""
                val endTime = intent.getStringExtra(EXTRA_END_TIME) ?: ""
                val lecturer = intent.getStringExtra(EXTRA_LECTURER) ?: ""
                val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 3001)
                NotificationHelper.notifyClassReminder(
                    context = context,
                    notifId = notifId,
                    courseName = courseName,
                    room = room,
                    startTime = startTime,
                    endTime = endTime,
                    lecturer = lecturer
                )
            }

            ACTION_START_WATCHER -> {
                if (!settings.attendanceWatcherEnabled || !app.container.vault.hasCredentials()) return
                runCatching {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, AttendanceWatcherService::class.java)
                    )
                }
            }

            ACTION_DAILY_RESCHEDULE -> {
                if (!app.container.vault.hasCredentials()) return
                val todaySlots = ScheduleTimeUtil.resolveTodaySlots(
                    allSlots = app.container.db.getScheduleSlots(),
                    savedTodayDateKey = app.container.db.getKv(DinusRepository.KV_TODAY_SLOTS_DATE)
                )
                ClassScheduler.rescheduleTodayTimers(context, todaySlots, settings)
                DinusScheduleWidget.updateAll(context)
            }
        }
    }

    companion object {
        const val ACTION_CLASS_REMINDER = "id.dinusrealm.app.ACTION_CLASS_REMINDER"
        const val ACTION_START_WATCHER = "id.dinusrealm.app.ACTION_START_WATCHER"
        const val ACTION_DAILY_RESCHEDULE = "id.dinusrealm.app.ACTION_DAILY_RESCHEDULE"

        const val EXTRA_COURSE_NAME = "extra_course_name"
        const val EXTRA_ROOM = "extra_room"
        const val EXTRA_START_TIME = "extra_start_time"
        const val EXTRA_END_TIME = "extra_end_time"
        const val EXTRA_LECTURER = "extra_lecturer"
        const val EXTRA_NOTIF_ID = "extra_notif_id"
    }
}
