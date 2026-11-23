package id.dinusrealm.app.core.engine

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import id.dinusrealm.app.core.i18n.currentLanguage
import id.dinusrealm.app.core.i18n.tr
import id.dinusrealm.app.core.model.AppLanguage
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import id.dinusrealm.app.MainActivity
import id.dinusrealm.app.R
import id.dinusrealm.app.core.model.BillingItem
import id.dinusrealm.app.core.model.OnlineAttendanceItem
import java.text.NumberFormat
import java.util.Locale

/**
 * Registers notification channels inheriting the phone's system default notification sound
 * (no custom ringtone override) and provides direct-action notifications for attendance & billing.
 */
object NotificationHelper {

    const val CHANNEL_CLASS_REMINDER = "dinus_class_reminder_v4"
    const val CHANNEL_ATTENDANCE_OPEN = "dinus_attendance_open_v4"
    const val CHANNEL_BILLING = "dinus_billing_v4"
    const val CHANNEL_WATCHER_SILENT = "dinus_watcher_silent"

    private val OBSOLETE_CHANNELS = listOf(
        "dinus_class_reminder",
        "dinus_attendance_open",
        "dinus_billing",
        "dinus_class_reminder_v2",
        "dinus_attendance_open_v2",
        "dinus_billing_v2",
        "dinus_class_reminder_v3",
        "dinus_attendance_open_v3",
        "dinus_billing_v3"
    )

    const val WATCHER_FOREGROUND_ID = 4101

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        OBSOLETE_CHANNELS.forEach { oldId ->
            runCatching { nm.deleteNotificationChannel(oldId) }
        }

        val lang = currentLanguage(context)

        val reminder = NotificationChannel(
            CHANNEL_CLASS_REMINDER,
            tr(lang, "Class Reminder (T-30 Min)", "Pengingat Kelas (T-30 Menit)"),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = tr(
                lang,
                "Room & lecturer alert 30 minutes before class starts",
                "Notifikasi ruang kelas dan dosen 30 menit sebelum kuliah dimulai"
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 120)
        }

        val attendance = NotificationChannel(
            CHANNEL_ATTENDANCE_OPEN,
            tr(lang, "Attendance Open & Auto-Attend", "Presensi Dibuka & Auto-Absen"),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = tr(
                lang,
                "Instant notification with a direct Attend Now button when opened",
                "Notifikasi instan dengan tombol Absen Sekarang saat dosen membuka presensi"
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 140, 80, 140)
        }

        val billing = NotificationChannel(
            CHANNEL_BILLING,
            tr(lang, "Tuition & Billing Alerts", "Tagihan & Pembayaran Kuliah"),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = tr(
                lang,
                "New bill or amount change alert with Virtual Account number",
                "Info tagihan baru atau perubahan nominal beserta nomor Virtual Account"
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 120)
        }

        val silentWatcher = NotificationChannel(
            CHANNEL_WATCHER_SILENT,
            tr(lang, "Active Attendance Monitor", "Pemantau Presensi Aktif"),
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = tr(
                lang,
                "Runs silently only while scheduled classes are active",
                "Berjalan senyap hanya saat jam kuliah berlangsung"
            )
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }

        nm.createNotificationChannels(listOf(reminder, attendance, billing, silentWatcher))
    }

    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun buildWatcherNotification(context: Context, activeSummary: String): Notification {
        ensureChannels(context)
        val lang = currentLanguage(context)
        return NotificationCompat.Builder(context, CHANNEL_WATCHER_SILENT)
            .setSmallIcon(R.drawable.ic_notif_dinus)
            .setContentTitle(tr(lang, "Active Attendance Monitor", "Pemantau Presensi Aktif"))
            .setContentText(activeSummary)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(mainActivityPendingIntent(context, 4100, MainActivity.SCREEN_PRESENCE))
            .build()
    }

    fun notifyClassReminder(
        context: Context,
        notifId: Int,
        courseName: String,
        room: String,
        startTime: String,
        endTime: String,
        lecturer: String
    ) {
        if (!canPostNotifications(context)) return
        ensureChannels(context)
        val lang = currentLanguage(context)
        val roomLabel = room.ifBlank { tr(lang, "Room TBA", "Ruang menyusul") }
        val lecturerLine = if (lecturer.isNotBlank()) " · $lecturer" else ""
        val body = "${tr(lang, "Room", "Ruang")} $roomLabel · $startTime–$endTime$lecturerLine"

        val notif = NotificationCompat.Builder(context, CHANNEL_CLASS_REMINDER)
            .setSmallIcon(R.drawable.ic_notif_dinus)
            .setContentTitle(tr(lang, "In 30 min: $courseName", "30 menit lagi: $courseName"))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(mainActivityPendingIntent(context, notifId))
            .build()

        NotificationManagerCompat.from(context).notify(notifId, notif)
    }

    fun notifyAttendanceOpen(context: Context, item: OnlineAttendanceItem) {
        if (!canPostNotifications(context)) return
        ensureChannels(context)
        val lang = currentLanguage(context)
        val notifId = notifIdForMaster(item.idMaster)

        val actionIntent = Intent(context, AttendanceActionReceiver::class.java).apply {
            action = AttendanceActionReceiver.ACTION_SUBMIT_ATTENDANCE
            putExtra(AttendanceActionReceiver.EXTRA_ID_MASTER, item.idMaster)
            putExtra(AttendanceActionReceiver.EXTRA_KDDS, item.kdds)
            putExtra(AttendanceActionReceiver.EXTRA_COURSE_NAME, item.courseName)
            putExtra(AttendanceActionReceiver.EXTRA_NOTIF_ID, notifId)
        }
        val actionPending = PendingIntent.getBroadcast(
            context,
            notifId,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val attendAction = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_dinus,
            tr(lang, "Attend Now", "Absen Sekarang"),
            actionPending
        ).setShowsUserInterface(false).build()

        val roomPart = if (item.room.isNotBlank()) "${tr(lang, "Room", "Ruang")} ${item.room} · " else ""
        val timePart = if (item.startTime.isNotBlank() && item.endTime.isNotBlank()) {
            "${item.startTime}–${item.endTime}"
        } else {
            tr(lang, "Active session", "Sesi aktif")
        }
        val text = roomPart + tr(
            lang,
            "$timePart is open. Tap Attend Now to submit directly.",
            "$timePart sudah dibuka. Ketuk Absen Sekarang langsung dari notif."
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ATTENDANCE_OPEN)
            .setSmallIcon(R.drawable.ic_notif_dinus)
            .setContentTitle("${tr(lang, "Attendance Open", "Presensi Dibuka")}: ${item.courseName}")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(false)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(mainActivityPendingIntent(context, notifId, MainActivity.SCREEN_PRESENCE))
            .addAction(attendAction)
            .build()

        NotificationManagerCompat.from(context).notify(notifId, notif)
    }

    fun notifyAttendanceSubmitting(context: Context, notifId: Int, courseName: String) {
        if (!canPostNotifications(context)) return
        ensureChannels(context)
        val lang = currentLanguage(context)
        val msg = tr(lang, "Submitting attendance to SiAdin...", "Mengirim presensi ke SiAdin...")

        val notif = NotificationCompat.Builder(context, CHANNEL_ATTENDANCE_OPEN)
            .setSmallIcon(R.drawable.ic_notif_dinus)
            .setContentTitle(courseName)
            .setContentText(msg)
            .setProgress(0, 0, true)
            .setSilent(true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(mainActivityPendingIntent(context, notifId, MainActivity.SCREEN_PRESENCE))
            .build()

        NotificationManagerCompat.from(context).notify(notifId, notif)
    }

    fun notifyAttendanceResult(
        context: Context,
        notifId: Int,
        courseName: String,
        modeLabel: String,
        success: Boolean,
        message: String,
        retryIdMaster: String? = null,
        retryKdds: String = "",
        isSimulation: Boolean = false
    ) {
        if (!canPostNotifications(context)) return
        ensureChannels(context)
        val lang = currentLanguage(context)
        val title = if (success) "$modeLabel: $courseName" else "${tr(lang, "Check Attendance", "Presensi Perlu Dicek")}: $courseName"

        val builder = NotificationCompat.Builder(context, CHANNEL_ATTENDANCE_OPEN)
            .setSmallIcon(R.drawable.ic_notif_dinus)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(mainActivityPendingIntent(context, notifId, MainActivity.SCREEN_PRESENCE))

        if (!success && !isSimulation && !retryIdMaster.isNullOrBlank()) {
            val retryIntent = Intent(context, AttendanceActionReceiver::class.java).apply {
                action = AttendanceActionReceiver.ACTION_SUBMIT_ATTENDANCE
                putExtra(AttendanceActionReceiver.EXTRA_ID_MASTER, retryIdMaster)
                putExtra(AttendanceActionReceiver.EXTRA_KDDS, retryKdds)
                putExtra(AttendanceActionReceiver.EXTRA_COURSE_NAME, courseName)
                putExtra(AttendanceActionReceiver.EXTRA_NOTIF_ID, notifId)
            }
            val retryPending = PendingIntent.getBroadcast(
                context,
                notifId + 2,
                retryIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val retryAction = NotificationCompat.Action.Builder(
                R.drawable.ic_notif_dinus,
                tr(lang, "Retry", "Coba Lagi"),
                retryPending
            ).setShowsUserInterface(false).build()
            builder.addAction(retryAction)
        }

        NotificationManagerCompat.from(context).notify(notifId, builder.build())
    }

    fun notifyBillingAlert(context: Context, bill: BillingItem) {
        if (!canPostNotifications(context)) return
        ensureChannels(context)
        val lang = currentLanguage(context)
        val notifId = 6000 + (bill.id.hashCode() and 0x0FFF)
        val rp = NumberFormat.getNumberInstance(Locale("in", "ID")).format(bill.remainingAmount)
        val vaPair = listOfNotNull(
            bill.vaBca.takeIf { it.isNotBlank() }?.let { "BCA $it" to it },
            bill.vaBjb.takeIf { it.isNotBlank() }?.let { "Bank Jateng $it" to it },
            bill.vaMandiri.takeIf { it.isNotBlank() }?.let { "Mandiri $it" to it }
        ).firstOrNull()

        val duePart = if (bill.dueDate.isNotBlank()) " · ${tr(lang, "Due", "Batas")}: ${bill.dueDate}" else ""
        val vaText = vaPair?.first?.let { "\nVA: $it" } ?: ""
        val remPrefix = tr(lang, "Due Rp$rp", "Sisa Rp$rp")
        val msg = "$remPrefix (${bill.category})$duePart$vaText"

        val builder = NotificationCompat.Builder(context, CHANNEL_BILLING)
            .setSmallIcon(R.drawable.ic_notif_dinus)
            .setContentTitle("${tr(lang, "Bill", "Tagihan")}: ${bill.title}")
            .setContentText("$remPrefix$duePart")
            .setStyle(NotificationCompat.BigTextStyle().bigText(msg))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(mainActivityPendingIntent(context, notifId))

        if (vaPair != null) {
            val copyIntent = Intent(context, AttendanceActionReceiver::class.java).apply {
                action = AttendanceActionReceiver.ACTION_COPY_VA
                putExtra(AttendanceActionReceiver.EXTRA_VA_NUMBER, vaPair.second)
            }
            val copyPending = PendingIntent.getBroadcast(
                context,
                notifId + 1,
                copyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(R.drawable.ic_notif_dinus, tr(lang, "Copy VA", "Salin VA"), copyPending)
        }

        NotificationManagerCompat.from(context).notify(notifId, builder.build())
    }

    /**
     * Fires a safe dry-run local attendance notification with an interactive `[Attend Now]` button
     * using the phone's default OS notification sound (never contacts SiAdin).
     */
    fun notifySimulatedAttendanceOpen(
        context: Context,
        lang: AppLanguage = currentLanguage(context)
    ): String {
        if (!canPostNotifications(context)) {
            return tr(
                lang,
                "Notification permission is off — enable it at the top first.",
                "Izin notifikasi belum aktif — aktifkan izin notifikasi di bagian atas dulu."
            )
        }
        ensureChannels(context)
        val notifId = 4910
        val sampleCourse = tr(lang, "Sample Class", "Contoh Matkul")
        val simIntent = Intent(context, AttendanceActionReceiver::class.java).apply {
            action = AttendanceActionReceiver.ACTION_SIMULATE_ATTENDANCE
            putExtra(AttendanceActionReceiver.EXTRA_COURSE_NAME, sampleCourse)
            putExtra(AttendanceActionReceiver.EXTRA_NOTIF_ID, notifId)
        }
        val simPending = PendingIntent.getBroadcast(
            context,
            notifId,
            simIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val attendAction = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_dinus,
            tr(lang, "Attend Now", "Absen Sekarang"),
            simPending
        ).setShowsUserInterface(false).build()

        val body = tr(
            lang,
            "Room H.4.11 · 10:00–12:00. Tap Attend Now to test direct attendance without opening the app.",
            "Ruang H.4.11 · 10:00–12:00. Ketuk Absen Sekarang untuk tes absen langsung tanpa buka app."
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ATTENDANCE_OPEN)
            .setSmallIcon(R.drawable.ic_notif_dinus)
            .setContentTitle(tr(lang, "Attendance Open (Simulation): $sampleCourse", "Presensi Dibuka (Simulasi): $sampleCourse"))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(false)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(mainActivityPendingIntent(context, notifId, MainActivity.SCREEN_PRESENCE))
            .addAction(attendAction)
            .build()
        NotificationManagerCompat.from(context).notify(notifId, notif)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = nm.getNotificationChannel(CHANNEL_ATTENDANCE_OPEN)
        if (ch != null && ch.importance == NotificationManager.IMPORTANCE_NONE) {
            return tr(
                lang,
                "Notification channel is muted in Android settings.",
                "Channel notifikasi dibisukan di pengaturan Android HP."
            )
        }

        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return when {
            am.ringerMode == AudioManager.RINGER_MODE_SILENT ->
                tr(
                    lang,
                    "Simulation notification sent (phone is in Silent mode). Pull down shade & tap Attend Now.",
                    "Simulasi notif dikirim (HP sedang mode Senyap). Tarik notif & ketuk Absen Sekarang."
                )
            am.ringerMode == AudioManager.RINGER_MODE_VIBRATE ->
                tr(
                    lang,
                    "Simulation notification sent (phone is in Vibrate mode). Pull down shade & tap Attend Now.",
                    "Simulasi notif dikirim (HP sedang mode Getar). Tarik notif & ketuk Absen Sekarang."
                )
            am.getStreamVolume(AudioManager.STREAM_NOTIFICATION) == 0 ->
                tr(
                    lang,
                    "Simulation notification sent (Notification volume is 0). Pull down shade & tap Attend Now.",
                    "Simulasi notif dikirim (volume Notifikasi 0). Tarik notif & ketuk Absen Sekarang."
                )
            else ->
                tr(
                    lang,
                    "Simulation sent with your phone's default notification sound — pull down the notification shade & tap Attend Now!",
                    "Simulasi terkirim mengikuti suara default HP — tarik panel notifikasi & ketuk tombol Absen Sekarang!"
                )
        }
    }

    fun notifIdForMaster(idMaster: String): Int = 5000 + (idMaster.hashCode() and 0x0FFF)

    private fun mainActivityPendingIntent(
        context: Context,
        requestCode: Int,
        openScreen: String? = null
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (openScreen != null) {
                putExtra(MainActivity.EXTRA_OPEN_SCREEN, openScreen)
            }
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
