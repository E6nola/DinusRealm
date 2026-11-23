package id.dinusrealm.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.dinusrealm.app.core.engine.NotificationHelper
import id.dinusrealm.app.core.i18n.LocalAppLanguage
import id.dinusrealm.app.core.i18n.tr
import id.dinusrealm.app.core.model.AppLanguage
import id.dinusrealm.app.core.model.ThemeMode
import id.dinusrealm.app.core.repo.DinusRepository
import id.dinusrealm.app.ui.SubScreen
import id.dinusrealm.app.ui.components.CurvedHeroHeader
import id.dinusrealm.app.ui.components.FruitIconBadge
import id.dinusrealm.app.ui.components.SorbetCard
import id.dinusrealm.app.ui.components.SorbetMessageStrip
import id.dinusrealm.app.ui.components.tabular
import id.dinusrealm.app.ui.theme.DinusIcons
import id.dinusrealm.app.ui.theme.DinusTheme
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    repo: DinusRepository,
    onBack: () -> Unit,
    onNavigateSub: (SubScreen) -> Unit
) {
    val p = DinusTheme.palette
    val lang = LocalAppLanguage.current
    val context = LocalContext.current
    val settings by repo.settings.collectAsState()
    var confirmLogout by remember { mutableStateOf(false) }
    var testStatus by remember { mutableStateOf<String?>(null) }
    var nickDraft by remember(settings.nickname) { mutableStateOf(settings.nickname) }

    Column(modifier = Modifier.fillMaxSize()) {
        CurvedHeroHeader(
            title = tr("Settings", "Pengaturan"),
            subtitle = tr(
                "Display name, language, notifications & theme",
                "Nama panggilan, bahasa, notifikasi & tema"
            ),
            gradientStart = p.peach,
            gradientEnd = p.mango,
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PermissionChecklistCard(showOnlyIfMissing = false)
            }

            if (!testStatus.isNullOrBlank()) {
                item {
                    SorbetMessageStrip(
                        message = testStatus!!,
                        accent = p.mint,
                        softBg = p.mintSoft,
                        onDismiss = { testStatus = null }
                    )
                }
            }

            item {
                SorbetCard {
                    Text(tr("Display Name", "Nama Panggilan"), style = MaterialTheme.typography.titleMedium, color = p.ink)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tr(
                            "Customize how the home screen greets you instead of your legal name.",
                            "Atur sapaan di Beranda jika tidak ingin memakai nama lengkap SiAdin."
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = p.inkMuted
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = nickDraft,
                        onValueChange = { nickDraft = it },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        placeholder = {
                            Text(tr("e.g. Joe (blank = SiAdin name)", "mis. Joe (kosong = nama SiAdin)"))
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (p.isDark) p.lime else p.cardActive,
                            unfocusedBorderColor = p.border,
                            focusedTextColor = p.ink,
                            unfocusedTextColor = p.ink
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val cleaned = nickDraft.trim()
                                repo.updateSettings(settings.copy(nickname = cleaned))
                                testStatus = if (cleaned.isNotEmpty()) {
                                    tr(lang, "Greeting updated to \"$cleaned\".", "Panggilan diperbarui jadi \"$cleaned\".")
                                } else {
                                    tr(lang, "Reset to default SiAdin name.", "Dikembalikan ke nama bawaan SiAdin.")
                                }
                            },
                            shape = RoundedCornerShape(100.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = p.lime, contentColor = p.onLime),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(tr("Save Name", "Simpan Nama"), style = MaterialTheme.typography.labelLarge)
                        }
                        if (settings.nickname.isNotBlank() || nickDraft.isNotBlank()) {
                            TextButton(onClick = {
                                nickDraft = ""
                                repo.updateSettings(settings.copy(nickname = ""))
                                testStatus = tr(lang, "Reset to default SiAdin name.", "Dikembalikan ke nama bawaan SiAdin.")
                            }) {
                                Text(tr("Reset", "Reset"), color = p.inkMuted, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            item {
                SorbetCard {
                    Text(tr("Language", "Bahasa"), style = MaterialTheme.typography.titleMedium, color = p.ink)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            AppLanguage.ENGLISH to "English",
                            AppLanguage.INDONESIAN to "Indonesia"
                        ).forEach { (itemLang, label) ->
                            val selected = settings.language == itemLang
                            Surface(
                                onClick = { repo.updateSettings(settings.copy(language = itemLang)) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(100.dp),
                                color = if (selected) p.cardActive else p.surfaceVariant,
                                border = BorderStroke(1.dp, if (selected && !p.isDark) p.cardActive else p.border)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) p.onCardActive else p.inkMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 12.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                SorbetCard {
                    Text(tr("Appearance", "Tampilan Aplikasi"), style = MaterialTheme.typography.titleMedium, color = p.ink)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            ThemeMode.SYSTEM to tr("System", "Sistem"),
                            ThemeMode.LIGHT to tr("Light", "Terang"),
                            ThemeMode.DARK to tr("Dark", "Gelap")
                        ).forEach { (mode, label) ->
                            val selected = settings.themeMode == mode
                            Surface(
                                onClick = { repo.updateSettings(settings.copy(themeMode = mode)) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(100.dp),
                                color = if (selected) p.cardActive else p.surfaceVariant,
                                border = BorderStroke(1.dp, if (selected && !p.isDark) p.cardActive else p.border)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) p.onCardActive else p.inkMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 12.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                SorbetCard {
                    Text(tr("Notifications & Auto-Attendance", "Notifikasi & Auto-Absen"), style = MaterialTheme.typography.titleMedium, color = p.ink)
                    Spacer(Modifier.height(10.dp))

                    SettingToggleRow(
                        title = tr("T-30m Class Alert", "Notif Kelas T-30 Menit"),
                        subtitle = tr(
                            "Course, room & lecturer alert 30 minutes before class",
                            "Info matkul, ruang kelas & dosen 30 menit sebelum mulai"
                        ),
                        checked = settings.classNotifEnabled,
                        onCheckedChange = { repo.updateSettings(settings.copy(classNotifEnabled = it)) }
                    )

                    Spacer(Modifier.height(10.dp))

                    SettingToggleRow(
                        title = tr("Smart Attendance Watcher", "Pemantau Presensi Pintar"),
                        subtitle = tr(
                            "Runs only during active classes; sends notification with [Attend Now]",
                            "Aktif hanya saat jam kelas; kirim notif dengan tombol [Absen Sekarang]"
                        ),
                        checked = settings.attendanceWatcherEnabled,
                        onCheckedChange = { repo.updateSettings(settings.copy(attendanceWatcherEnabled = it)) }
                    )

                    Spacer(Modifier.height(10.dp))

                    SettingToggleRow(
                        title = tr("Last-Minute Auto-Attend", "Auto-Absen Detik Terakhir"),
                        subtitle = tr(
                            "Automatically submits before class ends if still unmarked",
                            "Otomatis absen jika belum klik notif menjelang kelas berakhir"
                        ),
                        checked = settings.autoAbsenEnabled,
                        onCheckedChange = { repo.updateSettings(settings.copy(autoAbsenEnabled = it)) }
                    )

                    if (settings.autoAbsenEnabled) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                tr("Trigger before class ends", "Batas waktu sebelum selesai"),
                                style = MaterialTheme.typography.labelMedium,
                                color = p.inkMuted
                            )
                            Text(
                                "${settings.autoAbsenSecondsBeforeEnd} ${tr("sec", "detik")}",
                                style = tabular(MaterialTheme.typography.labelLarge),
                                color = p.ink
                            )
                        }
                        Slider(
                            value = settings.autoAbsenSecondsBeforeEnd.toFloat(),
                            onValueChange = {
                                val snapped = ((it / 15f).roundToInt() * 15).coerceIn(30, 180)
                                repo.updateSettings(settings.copy(autoAbsenSecondsBeforeEnd = snapped))
                            },
                            valueRange = 30f..180f,
                            colors = SliderDefaults.colors(
                                thumbColor = p.lime,
                                activeTrackColor = if (p.isDark) p.lime else p.cardActive,
                                inactiveTrackColor = p.surfaceVariant
                            )
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    SettingToggleRow(
                        title = tr("Billing & New VA Alerts", "Notif Tagihan & VA Baru"),
                        subtitle = tr(
                            "Periodically checks regular & non-regular bills and VA numbers",
                            "Cek berkala tagihan reguler & non-reguler beserta nomor VA"
                        ),
                        checked = settings.billingNotifEnabled,
                        onCheckedChange = { repo.updateSettings(settings.copy(billingNotifEnabled = it)) }
                    )

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = { testStatus = NotificationHelper.notifySimulatedAttendanceOpen(context, lang) },
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = p.lime,
                            contentColor = p.onLime
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(tr("Test Attendance Notification", "Tes Notif Absen Sekarang"), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            item {
                SorbetCard(onClick = { onNavigateSub(SubScreen.AttendanceLogs) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FruitIconBadge(DinusIcons.CheckCircle, p.mint, p.mintSoft)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(tr("Auto-Attendance History", "Riwayat Auto-Absen & Aksi Notif"), style = MaterialTheme.typography.titleMedium, color = p.ink)
                                Text(tr("View local auto-attendance action logs", "Pantau log presensi otomatis lokal"), style = MaterialTheme.typography.labelMedium, color = p.inkMuted)
                            }
                        }
                        Icon(DinusIcons.ChevronRight, contentDescription = null, tint = p.inkMuted)
                    }
                }
            }

            item {
                Button(
                    onClick = { confirmLogout = true },
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = p.strawberrySoft,
                        contentColor = p.strawberry
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(DinusIcons.LogOut, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(tr("Sign Out", "Keluar Akun"), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            containerColor = p.surface,
            title = { Text(tr("Sign out of DinusRealm?", "Keluar dari DinusRealm?"), color = p.ink) },
            text = {
                Text(
                    tr(
                        "Encrypted credentials, local schedule cache, and the attendance watcher will stop.",
                        "Kredensial terenkripsi, jadwal lokal, dan pemantau presensi akan dihentikan."
                    ),
                    color = p.inkMuted
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    repo.logout()
                }) {
                    Text(tr("Sign Out", "Keluar"), color = p.strawberry)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) {
                    Text(tr("Cancel", "Batal"), color = p.inkMuted)
                }
            }
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val p = DinusTheme.palette
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = p.ink)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = p.inkMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = p.onLime,
                checkedTrackColor = p.lime,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = p.surfaceVariant
            )
        )
    }
}
