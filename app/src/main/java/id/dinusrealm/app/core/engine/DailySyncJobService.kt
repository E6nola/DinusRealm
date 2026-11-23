package id.dinusrealm.app.core.engine

import android.app.job.JobParameters
import android.app.job.JobService
import id.dinusrealm.app.DinusRealmApp
import id.dinusrealm.app.core.network.ApiResult
import id.dinusrealm.app.core.network.SiadinParsers
import id.dinusrealm.app.core.repo.DinusRepository
import id.dinusrealm.app.widget.DinusScheduleWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Periodic JobScheduler service syncing schedule cache, widget, and today's T-30m/watcher timers.
 */
class DailySyncJobService : JobService() {

    private var runningJob: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        val app = application as? DinusRealmApp ?: return false
        if (!app.container.vault.hasCredentials()) return false

        runningJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val todayRes = app.container.api.getTodaySchedule()
                val weeklyRes = app.container.api.getWeeklySchedule()

                val existing = app.container.db.getScheduleSlots()
                val savedDate = app.container.db.getKv(DinusRepository.KV_TODAY_SLOTS_DATE)
                val todayFromCache = if (savedDate == ScheduleTimeUtil.todayDateKey()) {
                    existing.filter { it.dayIndex == 0 }
                } else emptyList()
                val todaySlots = if (todayRes is ApiResult.Success) {
                    SiadinParsers.preserveSlotMetadata(
                        todayRes.data.map { it.copy(dayIndex = 0) },
                        existing
                    )
                } else {
                    todayFromCache
                }
                val weeklySlots = if (weeklyRes is ApiResult.Success) {
                    SiadinParsers.preserveSlotMetadata(
                        weeklyRes.data.filter { it.dayIndex > 0 },
                        existing
                    )
                } else {
                    existing.filter { it.dayIndex > 0 }
                }

                if (todayRes is ApiResult.Success || weeklyRes is ApiResult.Success) {
                    app.container.db.replaceScheduleSlots(todaySlots + weeklySlots)
                    app.container.db.putKv(
                        DinusRepository.KV_TODAY_SLOTS_DATE,
                        if (todaySlots.isNotEmpty()) ScheduleTimeUtil.todayDateKey() else ""
                    )
                    val activeToday = if (todaySlots.isNotEmpty()) {
                        todaySlots
                    } else {
                        val idx = ScheduleTimeUtil.currentSiadinDay()
                        weeklySlots.filter { it.dayIndex == idx }
                    }
                    ClassScheduler.rescheduleTodayTimers(
                        applicationContext,
                        activeToday,
                        app.container.vault.getSettings()
                    )
                    DinusScheduleWidget.updateAll(applicationContext)
                }
            } finally {
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        runningJob?.cancel()
        return true
    }
}
