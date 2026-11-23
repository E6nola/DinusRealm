package id.dinusrealm.app.core.engine

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import id.dinusrealm.app.core.model.AppSettings
import id.dinusrealm.app.core.model.ScheduleSlot

/**
 * Lightweight OS scheduler using native AlarmManager (silent notification triggers)
 * and JobScheduler (periodic schedule sync & billing diff).
 */
object ClassScheduler {

    private const val JOB_DAILY_SYNC = 7001
    private const val JOB_BILLING_CHECK = 7002
    private const val MAX_TIMERS_PER_DAY = 20
    private const val DAILY_RESCHEDULE_REQ_CODE = 2900

    fun rescheduleTodayTimers(
        context: Context,
        todaySlots: List<ScheduleSlot>,
        settings: AppSettings
    ) {
        val appContext = context.applicationContext
        val am = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelAllClassTimers(appContext, am)

        val nowMillis = System.currentTimeMillis()
        val dailyIntent = Intent(appContext, ClassReminderReceiver::class.java).apply {
            action = ClassReminderReceiver.ACTION_DAILY_RESCHEDULE
        }
        scheduleTrigger(
            am = am,
            context = appContext,
            requestCode = DAILY_RESCHEDULE_REQ_CODE,
            triggerAtMillis = ScheduleTimeUtil.computeNextDailyTriggerMillis(nowMillis),
            intent = dailyIntent
        )

        if (!settings.classNotifEnabled && !settings.attendanceWatcherEnabled) {
            return
        }

        val (nowMin, _) = ScheduleTimeUtil.currentMinutesAndSecond(nowMillis)
        var shouldStartWatcherNow = false

        todaySlots.take(MAX_TIMERS_PER_DAY).forEachIndexed { idx, slot ->
            if (settings.classNotifEnabled) {
                val reminderAt = ScheduleTimeUtil.computeTriggerMillis(nowMillis, slot.startTime, offsetMinutes = -30)
                if (reminderAt != null) {
                    val intent = Intent(appContext, ClassReminderReceiver::class.java).apply {
                        action = ClassReminderReceiver.ACTION_CLASS_REMINDER
                        putExtra(ClassReminderReceiver.EXTRA_COURSE_NAME, slot.courseName)
                        putExtra(ClassReminderReceiver.EXTRA_ROOM, slot.room)
                        putExtra(ClassReminderReceiver.EXTRA_START_TIME, slot.startTime)
                        putExtra(ClassReminderReceiver.EXTRA_END_TIME, slot.endTime)
                        putExtra(ClassReminderReceiver.EXTRA_LECTURER, slot.lecturer)
                        putExtra(ClassReminderReceiver.EXTRA_NOTIF_ID, 3000 + idx)
                    }
                    scheduleTrigger(am, appContext, 2000 + idx, reminderAt, intent)
                }
            }

            if (settings.attendanceWatcherEnabled && !slot.isAttended) {
                if (ScheduleTimeUtil.isWithinClassWindow(nowMin, slot.startTime, slot.endTime)) {
                    shouldStartWatcherNow = true
                } else {
                    val watcherAt = ScheduleTimeUtil.computeTriggerMillis(nowMillis, slot.startTime, offsetMinutes = -2)
                    if (watcherAt != null) {
                        val intent = Intent(appContext, ClassReminderReceiver::class.java).apply {
                            action = ClassReminderReceiver.ACTION_START_WATCHER
                        }
                        scheduleTrigger(am, appContext, 2500 + idx, watcherAt, intent)
                    }
                }
            }
        }

        if (shouldStartWatcherNow) {
            runCatching {
                ContextCompat.startForegroundService(
                    appContext,
                    Intent(appContext, AttendanceWatcherService::class.java)
                )
            }
        }
    }

    fun schedulePeriodicJobs(context: Context) {
        val appContext = context.applicationContext
        val js = appContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

        val syncJob = JobInfo.Builder(
            JOB_DAILY_SYNC,
            ComponentName(appContext, DailySyncJobService::class.java)
        )
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPeriodic(6L * 60L * 60L * 1000L)
            .setPersisted(true)
            .build()

        val billingJob = JobInfo.Builder(
            JOB_BILLING_CHECK,
            ComponentName(appContext, BillingCheckJobService::class.java)
        )
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPeriodic(12L * 60L * 60L * 1000L)
            .setPersisted(true)
            .build()

        js.schedule(syncJob)
        js.schedule(billingJob)
    }

    fun cancelAll(context: Context) {
        val appContext = context.applicationContext
        val am = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelAllClassTimers(appContext, am)
        val js = appContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        js.cancel(JOB_DAILY_SYNC)
        js.cancel(JOB_BILLING_CHECK)
        appContext.stopService(Intent(appContext, AttendanceWatcherService::class.java))
    }

    private fun cancelAllClassTimers(context: Context, am: AlarmManager) {
        for (idx in 0 until MAX_TIMERS_PER_DAY) {
            val remIntent = Intent(context, ClassReminderReceiver::class.java).apply {
                action = ClassReminderReceiver.ACTION_CLASS_REMINDER
            }
            PendingIntent.getBroadcast(
                context,
                2000 + idx,
                remIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )?.let { am.cancel(it) }

            val watchIntent = Intent(context, ClassReminderReceiver::class.java).apply {
                action = ClassReminderReceiver.ACTION_START_WATCHER
            }
            PendingIntent.getBroadcast(
                context,
                2500 + idx,
                watchIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )?.let { am.cancel(it) }
        }

        val dailyIntent = Intent(context, ClassReminderReceiver::class.java).apply {
            action = ClassReminderReceiver.ACTION_DAILY_RESCHEDULE
        }
        PendingIntent.getBroadcast(
            context,
            DAILY_RESCHEDULE_REQ_CODE,
            dailyIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )?.let { am.cancel(it) }
    }

    private fun scheduleTrigger(
        am: AlarmManager,
        context: Context,
        requestCode: Int,
        triggerAtMillis: Long,
        intent: Intent
    ) {
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (canExact) {
            runCatching {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }.onFailure {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }
}
