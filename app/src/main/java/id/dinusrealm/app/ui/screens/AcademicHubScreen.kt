package id.dinusrealm.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import id.dinusrealm.app.core.i18n.tr
import id.dinusrealm.app.ui.SubScreen
import id.dinusrealm.app.ui.components.CurvedHeroHeader
import id.dinusrealm.app.ui.components.FruitIconBadge
import id.dinusrealm.app.ui.components.SorbetCard
import id.dinusrealm.app.ui.theme.DinusIcons
import id.dinusrealm.app.ui.theme.DinusTheme

@Composable
fun AcademicHubScreen(onNavigateSub: (SubScreen) -> Unit) {
    val p = DinusTheme.palette
    Column(modifier = Modifier.fillMaxSize()) {
        CurvedHeroHeader(
            title = tr("Academic Services", "Layanan Akademik"),
            subtitle = tr("Attendance, KRS, KHS, Exams & Announcements", "Presensi, KRS, KHS, Ujian & Pengumuman"),
            gradientStart = p.berry,
            gradientEnd = p.cobalt
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                MenuNavCard(
                    tr("Attendance & RPS Confirmation", "Presensi & Konfirmasi RPS"),
                    tr("Online attendance, meeting recap & RPS", "Kehadiran per matkul, detail pertemuan & RPS"),
                    DinusIcons.CheckCircle, p.mint, p.mintSoft
                ) { onNavigateSub(SubScreen.Presence) }
            }
            item {
                MenuNavCard(
                    tr("Study Plan (KRS)", "Kartu Rencana Studi (KRS)"),
                    tr("Current semester KRS & past KRS history", "KRS semester ini & riwayat KRS lalu"),
                    DinusIcons.Calendar, p.sky, p.skySoft
                ) { onNavigateSub(SubScreen.Krs) }
            }
            item {
                MenuNavCard(
                    tr("KHS, GPA Chart & Transcript", "KHS, Grafik IP & Transkrip"),
                    tr("Semester grades, GPA/IPS chart & transcript", "Nilai semester, grafik IPK/IPS & transkrip"),
                    DinusIcons.Academic, p.berry, p.berrySoft
                ) { onNavigateSub(SubScreen.Khs) }
            }
            item {
                MenuNavCard(
                    tr("Exam Schedule (UTS & UAS)", "Jadwal Ujian (UTS & UAS)"),
                    tr("Exam dates, rooms, seat numbers & eligibility", "Tanggal ujian, ruang, nomor kursi & syarat"),
                    DinusIcons.Clock, p.mango, p.mangoSoft
                ) { onNavigateSub(SubScreen.Exams) }
            }
            item {
                MenuNavCard(
                    tr("Re-Registration & Documents", "Heregistrasi & Berkas"),
                    tr("Semester re-registration status & documents", "Status registrasi ulang & kelengkapan berkas"),
                    DinusIcons.Shield, p.mint, p.mintSoft
                ) { onNavigateSub(SubScreen.Heregistrasi) }
            }
            item {
                MenuNavCard(
                    tr("Course Questionnaires & Quizzes", "Kuisioner Dosen & Kuis"),
                    tr("UTS/UAS evaluations & mandatory campus quizzes", "Isi kuisioner UTS/UAS & cek kuis wajib"),
                    DinusIcons.Bell, p.peach, p.peachSoft
                ) { onNavigateSub(SubScreen.Questionnaire) }
            }
            item {
                MenuNavCard(
                    tr("Academic Announcements", "Pengumuman Akademik"),
                    tr("Official news & notices from UDINUS", "Informasi & berita resmi UDINUS"),
                    DinusIcons.Home, p.cobalt, p.cobaltSoft
                ) { onNavigateSub(SubScreen.Announcements) }
            }
        }
    }
}

@Composable
fun MoreHubScreen(onNavigateSub: (SubScreen) -> Unit) {
    val p = DinusTheme.palette
    Column(modifier = Modifier.fillMaxSize()) {
        CurvedHeroHeader(
            title = tr("Finance & Profile", "Keuangan & Profil"),
            subtitle = tr("Billing, 5 Bank VAs, Biodata & Settings", "Tagihan, 5 Bank VA, Biodata & Pengaturan"),
            gradientStart = p.strawberry,
            gradientEnd = p.peach
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                MenuNavCard(
                    tr("Billing & Virtual Accounts", "Tagihan & Virtual Account"),
                    tr("Invoices, fee breakdown & 1-tap 5 VA copy", "Tagihan, rincian biaya & salin 5 VA 1-tap"),
                    DinusIcons.Wallet, p.strawberry, p.strawberrySoft
                ) { onNavigateSub(SubScreen.Billing) }
            }
            item {
                MenuNavCard(
                    tr("Student Profile & Advisor", "Profil & Dosen Wali"),
                    tr("Biodata, advisor contact & change password", "Biodata, kontak dosen wali & ubah sandi"),
                    DinusIcons.User, p.cobalt, p.cobaltSoft
                ) { onNavigateSub(SubScreen.Profile) }
            }
            item {
                MenuNavCard(
                    tr("Settings, Language & Alerts", "Pengaturan & Notif Pintar"),
                    tr("Nickname, language, local sound & theme", "Nama panggilan, bahasa, notif lokal & tema"),
                    DinusIcons.Settings, p.peach, p.peachSoft
                ) { onNavigateSub(SubScreen.Settings) }
            }
            item {
                MenuNavCard(
                    tr("Auto-Attendance Logs", "Riwayat Auto-Absen"),
                    tr("Local auto-attendance & notification logs", "Log presensi otomatis & aksi notifikasi"),
                    DinusIcons.CheckCircle, p.mint, p.mintSoft
                ) { onNavigateSub(SubScreen.AttendanceLogs) }
            }
        }
    }
}

@Composable
fun MenuNavCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    softBg: Color,
    onClick: () -> Unit
) {
    val p = DinusTheme.palette
    SorbetCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                FruitIconBadge(icon = icon, accent = accent, softBg = softBg)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = p.ink)
                    Text(subtitle, style = MaterialTheme.typography.labelMedium, color = p.inkMuted)
                }
            }
            Icon(DinusIcons.ChevronRight, contentDescription = null, tint = p.inkMuted, modifier = Modifier.size(18.dp))
        }
    }
}
