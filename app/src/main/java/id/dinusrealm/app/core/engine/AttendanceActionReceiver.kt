package id.dinusrealm.app.core.engine

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import id.dinusrealm.app.DinusRealmApp
import id.dinusrealm.app.core.i18n.currentLanguage
import id.dinusrealm.app.core.i18n.tr
import id.dinusrealm.app.core.model.AttendanceLogEntry
import id.dinusrealm.app.core.network.ApiResult
import id.dinusrealm.app.core.repo.DinusRepository
import id.dinusrealm.app.widget.DinusScheduleWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Handles one-tap notification actions without opening the app:
 * 1. `[Attend Now]` / `[Absen Sekarang]` -> submits `absensi-online` directly in background and updates the notification.
 * 2. `[Simulate Attend Now]` -> dry-run simulation for Settings testing without touching SiAdin.
 * 3. `[Copy VA]` / `[Salin VA]` -> copies Virtual Account number to clipboard.
 */
class AttendanceActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val lang = currentLanguage(context)
        when (intent.action) {
            ACTION_COPY_VA -> {
                val va = intent.getStringExtra(EXTRA_VA_NUMBER) ?: return
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Virtual Account", va))
                Toast.makeText(
                    context,
                    tr(lang, "VA copied: $va", "Nomor VA disalin: $va"),
                    Toast.LENGTH_SHORT
                ).show()
            }

            ACTION_SIMULATE_ATTENDANCE -> {
                val courseName = intent.getStringExtra(EXTRA_COURSE_NAME) ?: tr(lang, "Sample Class", "Contoh Matkul")
                val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 4910)
                NotificationHelper.notifyAttendanceSubmitting(context, notifId, courseName)

                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        delay(900L)
                        NotificationHelper.notifyAttendanceResult(
                            context = context,
                            notifId = notifId,
                            courseName = courseName,
                            modeLabel = tr(lang, "Simulation Recorded", "Simulasi Presensi Berhasil"),
                            success = true,
                            message = tr(
                                lang,
                                "One-tap background attendance works! No real data was sent to SiAdin.",
                                "Absen langsung dari notifikasi berhasil! Aman — ini hanya simulasi lokal."
                            ),
                            isSimulation = true
                        )
                    } finally {
                        pending.finish()
                    }
                }
            }

            ACTION_SUBMIT_ATTENDANCE -> {
                val idMaster = intent.getStringExtra(EXTRA_ID_MASTER) ?: return
                val kdds = intent.getStringExtra(EXTRA_KDDS) ?: ""
                val courseName = intent.getStringExtra(EXTRA_COURSE_NAME) ?: tr(lang, "Class", "Kuliah")
                val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, NotificationHelper.notifIdForMaster(idMaster))

                NotificationHelper.notifyAttendanceSubmitting(context, notifId, courseName)

                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val container = (context.applicationContext as DinusRealmApp).container
                        var resolvedIdMaster = idMaster
                        var resolvedKdds = kdds

                        if (resolvedKdds.isBlank()) {
                            val onlineRes = container.api.getKrsOnline()
                            if (onlineRes is ApiResult.Success) {
                                val matched = onlineRes.data.firstOrNull {
                                    (resolvedIdMaster.isNotBlank() && it.idMaster == resolvedIdMaster) ||
                                        it.courseName.trim().equals(courseName.trim(), ignoreCase = true)
                                }
                                if (matched != null) {
                                    if (matched.idMaster.isNotBlank()) resolvedIdMaster = matched.idMaster
                                    if (matched.kdds.isNotBlank()) resolvedKdds = matched.kdds
                                }
                            }
                        }

                        val res = container.api.submitOnlineAttendance(resolvedIdMaster, resolvedKdds)
                        val success = res is ApiResult.Success
                        val msg = when (res) {
                            is ApiResult.Success -> res.data
                            is ApiResult.Error -> res.message
                        }

                        container.db.insertAttendanceLog(
                            AttendanceLogEntry(
                                idMaster = resolvedIdMaster,
                                courseName = courseName,
                                mode = "NOTIF_ACTION",
                                success = success,
                                message = msg,
                                timestampMillis = System.currentTimeMillis()
                            )
                        )

                        NotificationHelper.notifyAttendanceResult(
                            context = context,
                            notifId = notifId,
                            courseName = courseName,
                            modeLabel = tr(lang, "Attendance Recorded", "Presensi Berhasil"),
                            success = success,
                            message = msg,
                            retryIdMaster = if (!success) resolvedIdMaster else null,
                            retryKdds = resolvedKdds
                        )

                        if (success) {
                            val updatedAll = container.db.getScheduleSlots().map { slot ->
                                val isSame = (!slot.idMaster.isNullOrBlank() && slot.idMaster == resolvedIdMaster) ||
                                    (slot.dayIndex == 0 && slot.courseName.trim().equals(courseName.trim(), ignoreCase = true))
                                if (isSame) slot.copy(isAttended = true) else slot
                            }
                            container.db.replaceScheduleSlots(updatedAll)
                            if (updatedAll.any { it.dayIndex == 0 }) {
                                container.db.putKv(DinusRepository.KV_TODAY_SLOTS_DATE, ScheduleTimeUtil.todayDateKey())
                            }
                            DinusScheduleWidget.updateAll(context)
                            container.repo.refreshCoreDataAsync()
                        }
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_SUBMIT_ATTENDANCE = "id.dinusrealm.app.ACTION_SUBMIT_ATTENDANCE"
        const val ACTION_SIMULATE_ATTENDANCE = "id.dinusrealm.app.ACTION_SIMULATE_ATTENDANCE"
        const val ACTION_COPY_VA = "id.dinusrealm.app.ACTION_COPY_VA"

        const val EXTRA_ID_MASTER = "extra_id_master"
        const val EXTRA_KDDS = "extra_kdds"
        const val EXTRA_COURSE_NAME = "extra_course_name"
        const val EXTRA_NOTIF_ID = "extra_notif_id"
        const val EXTRA_VA_NUMBER = "extra_va_number"
    }
}
