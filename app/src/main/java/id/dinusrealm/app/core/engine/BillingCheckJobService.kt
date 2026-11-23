package id.dinusrealm.app.core.engine

import android.app.job.JobParameters
import android.app.job.JobService
import id.dinusrealm.app.DinusRealmApp
import id.dinusrealm.app.core.network.ApiResult
import id.dinusrealm.app.core.network.SiadinParsers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Periodic JobScheduler service checking regular & non-regular bills and alerting on new/changed bills.
 */
class BillingCheckJobService : JobService() {

    private var runningJob: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        val app = application as? DinusRealmApp ?: return false
        val settings = app.container.vault.getSettings()
        if (!app.container.vault.hasCredentials() || !settings.billingNotifEnabled) return false

        runningJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val regRes = app.container.api.getRegularBilling()
                val nonRegRes = app.container.api.getNonRegularBilling()
                if (regRes !is ApiResult.Success && nonRegRes !is ApiResult.Success) return@launch

                val current = buildList {
                    if (regRes is ApiResult.Success) addAll(regRes.data)
                    if (nonRegRes is ApiResult.Success) addAll(nonRegRes.data)
                }
                val previous = app.container.db.getBillingSnapshots()
                val changed = SiadinParsers.detectBillingChanges(previous, current)
                if (changed.isNotEmpty()) {
                    val methods = (app.container.api.getPaymentInfo() as? ApiResult.Success)?.data?.methods.orEmpty()
                    val bca = methods.firstOrNull { it.bankName.contains("BCA", true) }?.vaNumber.orEmpty()
                    val bjb = methods.firstOrNull { it.bankName.contains("Jateng", true) }?.vaNumber.orEmpty()
                    val mandiri = methods.firstOrNull { it.bankName.contains("Mandiri", true) }?.vaNumber.orEmpty()
                    changed.forEach { bill ->
                        val enriched = bill.copy(
                            vaBca = bill.vaBca.ifBlank { bca },
                            vaBjb = bill.vaBjb.ifBlank { bjb },
                            vaMandiri = bill.vaMandiri.ifBlank { mandiri }
                        )
                        NotificationHelper.notifyBillingAlert(applicationContext, enriched)
                    }
                }
                app.container.db.replaceBillingSnapshots(current)
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
