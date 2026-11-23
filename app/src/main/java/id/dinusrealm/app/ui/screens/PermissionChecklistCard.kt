package id.dinusrealm.app.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import id.dinusrealm.app.core.i18n.tr
import id.dinusrealm.app.ui.components.FruitIconBadge
import id.dinusrealm.app.ui.components.SorbetCard
import id.dinusrealm.app.ui.theme.DinusIcons
import id.dinusrealm.app.ui.theme.DinusTheme

data class PermissionHealth(
    val notifAllowed: Boolean,
    val exactTimingAllowed: Boolean,
    val batteryUnrestricted: Boolean
) {
    val allHealthy: Boolean get() = notifAllowed && exactTimingAllowed && batteryUnrestricted
}

fun readPermissionHealth(context: Context): PermissionHealth {
    val notif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    } else true

    val exact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    } else true

    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val battery = pm.isIgnoringBatteryOptimizations(context.packageName)
    return PermissionHealth(notif, exact, battery)
}

@Composable
fun PermissionChecklistCard(
    modifier: Modifier = Modifier,
    showOnlyIfMissing: Boolean = true
) {
    val context = LocalContext.current
    val p = DinusTheme.palette
    var refreshTick by remember { mutableIntStateOf(0) }
    val health = remember(refreshTick) { readPermissionHealth(context) }

    val lifecycleOwner = context as? LifecycleOwner
    DisposableEffect(lifecycleOwner) {
        if (lifecycleOwner == null) return@DisposableEffect onDispose {}
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        refreshTick++
    }

    if (showOnlyIfMissing && health.allHealthy) return

    SorbetCard(
        modifier = modifier,
        containerColor = if (health.allHealthy) p.mintSoft else p.mangoSoft,
        borderColor = (if (health.allHealthy) p.mint else p.mango).copy(alpha = 0.25f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FruitIconBadge(
                icon = if (health.allHealthy) DinusIcons.Shield else DinusIcons.Bell,
                accent = if (health.allHealthy) p.mint else p.mango,
                softBg = p.surface
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (health.allHealthy) {
                        tr("Notifications & Background Ready", "Notif & Background Optimal")
                    } else {
                        tr("Optimize Attendance Alerts", "Optimalkan Notif Presensi")
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = p.ink
                )
                Text(
                    text = if (health.allHealthy) {
                        tr(
                            "T-30m class reminders & attendance watcher are ready in the background.",
                            "Pengingat kelas T-30m & pemantau absen siap bekerja di latar belakang."
                        )
                    } else {
                        tr(
                            "Allow notifications & disable battery restrictions so alerts arrive on time.",
                            "Izinkan notifikasi & nonaktifkan pembatasan baterai agar tidak tertunda oleh HP."
                        )
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = p.inkMuted
                )
            }
        }

        if (!health.allHealthy) {
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!health.notifAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PermissionRow(
                        label = tr("Notifications", "Izin Notifikasi"),
                        actionText = tr("Allow", "Izinkan"),
                        onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    )
                }
                if (!health.exactTimingAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PermissionRow(
                        label = tr("Exact Timing (T-30m)", "Penjadwalan Tepat Waktu (T-30m)"),
                        actionText = tr("Open", "Buka Izin"),
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                )
                            }
                            refreshTick++
                        }
                    )
                }
                if (!health.batteryUnrestricted) {
                    PermissionRow(
                        label = tr("Unrestricted Battery", "Baterai Tanpa Batas (Anti-Kill)"),
                        actionText = tr("Configure", "Atur"),
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                )
                            }
                            refreshTick++
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    label: String,
    actionText: String,
    onClick: () -> Unit
) {
    val p = DinusTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = p.ink,
            modifier = Modifier.weight(1f)
        )
        FilledTonalButton(
            onClick = onClick,
            shape = RoundedCornerShape(100.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = p.surface,
                contentColor = p.peach
            )
        ) {
            Text(actionText, style = MaterialTheme.typography.labelLarge)
        }
    }
}
