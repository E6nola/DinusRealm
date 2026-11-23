package id.dinusrealm.app.core.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import id.dinusrealm.app.DinusRealmApp
import id.dinusrealm.app.core.repo.DinusRepository
import id.dinusrealm.app.widget.DinusScheduleWidget

/**
 * Re-arms scheduled class timers, periodic JobScheduler syncs, and Home Screen Widget
 * after device reboot or app update.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val app = context.applicationContext as? DinusRealmApp ?: return
        if (!app.container.vault.hasCredentials()) return

        NotificationHelper.ensureChannels(context)
        val todaySlots = ScheduleTimeUtil.resolveTodaySlots(
            allSlots = app.container.db.getScheduleSlots(),
            savedTodayDateKey = app.container.db.getKv(DinusRepository.KV_TODAY_SLOTS_DATE)
        )
        ClassScheduler.rescheduleTodayTimers(context, todaySlots, app.container.vault.getSettings())
        ClassScheduler.schedulePeriodicJobs(context)
        DinusScheduleWidget.updateAll(context)
    }
}
