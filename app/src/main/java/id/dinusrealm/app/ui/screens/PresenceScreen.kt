package id.dinusrealm.app.ui.screens

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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.dinusrealm.app.core.i18n.LocalAppLanguage
import id.dinusrealm.app.core.i18n.tr
import id.dinusrealm.app.core.model.AppLanguage
import id.dinusrealm.app.core.model.AttendanceCourse
import id.dinusrealm.app.core.model.AttendanceMeeting
import id.dinusrealm.app.core.model.OnlineAttendanceItem
import id.dinusrealm.app.core.network.ApiResult
import id.dinusrealm.app.core.repo.DinusRepository
import id.dinusrealm.app.ui.components.CircularRingBadge
import id.dinusrealm.app.ui.components.CurvedHeroHeader
import id.dinusrealm.app.ui.components.FruitBadge
import id.dinusrealm.app.ui.components.SorbetCard
import id.dinusrealm.app.ui.components.SorbetEmptyState
import id.dinusrealm.app.ui.components.SorbetLoadingBox
import id.dinusrealm.app.ui.components.SorbetMessageStrip
import id.dinusrealm.app.ui.components.SorbetSegmentTabs
import id.dinusrealm.app.ui.components.tabular
import id.dinusrealm.app.ui.theme.DinusIcons
import id.dinusrealm.app.ui.theme.DinusTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PresenceScreen(
    repo: DinusRepository,
    onBack: () -> Unit
) {
    val p = DinusTheme.palette
    val scope = rememberCoroutineScope()
    var tabIdx by remember { mutableIntStateOf(1) }

    val onlineState by repo.onlineAttendance.collectAsState()
    var onlineItems by remember(onlineState) { mutableStateOf(onlineState) }
    var summaryList by remember { mutableStateOf<List<AttendanceCourse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var selectedCourse by remember { mutableStateOf<AttendanceCourse?>(null) }
    var meetingDetails by remember { mutableStateOf<List<AttendanceMeeting>>(emptyList()) }
    var isLoadingDetail by remember { mutableStateOf(false) }

    fun loadAll() {
        isLoading = true
        scope.launch {
            val onlineRes = repo.api.getKrsOnline()
            if (onlineRes is ApiResult.Success) onlineItems = onlineRes.data
            val sumRes = repo.api.getAttendanceSummary()
            when (sumRes) {
                is ApiResult.Success -> summaryList = sumRes.data
                is ApiResult.Error -> if (tabIdx == 1) feedback = sumRes.message
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadAll() }

    val avgPct = if (summaryList.isNotEmpty()) summaryList.map { it.percentage }.average() else 0.0

    Column(modifier = Modifier.fillMaxSize()) {
        CurvedHeroHeader(
            title = tr("Attendance & RPS", "Presensi & RPS"),
            subtitle = tr("Online attendance, meeting recap & RPS", "Kehadiran online, rekap pertemuan & RPS"),
            gradientStart = p.mint,
            gradientEnd = p.mint,
            onBack = onBack,
            rightStat = if (summaryList.isNotEmpty()) "${avgPct.toInt()}%" else null,
            rightStatLabel = if (summaryList.isNotEmpty()) tr("Avg. Present", "Rata-rata Hadir") else null
        )

        Spacer(Modifier.height(6.dp))

        SorbetSegmentTabs(
            tabs = listOf(tr("Today", "Hari Ini"), tr("Semester Recap", "Rekap Semester")),
            selectedIndex = tabIdx,
            onSelect = { tabIdx = it },
            accent = p.mint
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!feedback.isNullOrBlank()) {
                item {
                    SorbetMessageStrip(
                        message = feedback!!,
                        accent = p.mint,
                        softBg = p.mintSoft,
                        onDismiss = { feedback = null }
                    )
                }
            }

            if (isLoading) {
                item { SorbetLoadingBox(tr("Loading attendance...", "Memuat data presensi..."), p.mint) }
            } else if (tabIdx == 0) {
                if (onlineItems.isEmpty()) {
                    item {
                        SorbetEmptyState(
                            icon = DinusIcons.CheckCircle,
                            accent = p.mint,
                            softBg = p.mintSoft,
                            title = tr("No online attendance open today", "Belum ada presensi online hari ini"),
                            subtitle = tr(
                                "When your lecturer opens attendance during class, the Attend button appears here and in notifications.",
                                "Saat jam kuliah dibuka dosen, tombol Absen Sekarang muncul di sini dan di notifikasi."
                            ),
                            actionLabel = tr("Check Semester Recap", "Cek Rekap Semester"),
                            onAction = { tabIdx = 1 }
                        )
                    }
                } else {
                    items(onlineItems.size) { idx ->
                        OnlinePresenceCard(
                            item = onlineItems[idx],
                            onAttend = {
                                scope.launch {
                                    val res = repo.submitAttendance(it.idMaster, it.kdds, it.courseName)
                                    feedback = when (res) {
                                        is ApiResult.Success -> res.data
                                        is ApiResult.Error -> res.message
                                    }
                                    loadAll()
                                }
                            },
                            onConfirmRps = { item, sesuai ->
                                scope.launch {
                                    val res = repo.confirmRps(item.idMaster, sesuai)
                                    feedback = when (res) {
                                        is ApiResult.Success -> res.data
                                        is ApiResult.Error -> res.message
                                    }
                                    loadAll()
                                }
                            }
                        )
                    }
                }
            } else {
                if (summaryList.isEmpty()) {
                    item {
                        SorbetEmptyState(
                            icon = DinusIcons.Calendar,
                            accent = p.berry,
                            softBg = p.berrySoft,
                            title = tr("Attendance recap is empty", "Rekap presensi kosong"),
                            subtitle = tr("Semester attendance data is not available yet.", "Data rekap kehadiran semester belum tersedia."),
                            actionLabel = tr("Retry", "Coba Lagi"),
                            onAction = { loadAll() }
                        )
                    }
                } else {
                    items(summaryList.size) { idx ->
                        val c = summaryList[idx]
                        AttendanceSummaryCard(course = c) {
                            selectedCourse = c
                            isLoadingDetail = true
                            meetingDetails = emptyList()
                            scope.launch {
                                val det = repo.api.getAttendanceDetail(c.kdmk.ifBlank { c.idJadwal })
                                if (det is ApiResult.Success) meetingDetails = det.data
                                isLoadingDetail = false
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedCourse != null) {
        AlertDialog(
            onDismissRequest = { selectedCourse = null },
            containerColor = p.surface,
            title = { Text(selectedCourse!!.courseName, color = p.ink, style = MaterialTheme.typography.titleMedium) },
            text = {
                if (isLoadingDetail) {
                    Text(tr("Loading meeting details...", "Memuat riwayat pertemuan..."), color = p.inkMuted)
                } else if (meetingDetails.isEmpty()) {
                    Text(tr("No meeting details available.", "Belum ada rincian pertemuan."), color = p.inkMuted)
                } else {
                    LazyColumn(
                        modifier = Modifier.height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(meetingDetails.size) { i ->
                            val m = meetingDetails[i]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(m.label, style = MaterialTheme.typography.labelLarge, color = p.ink)
                                    if (m.date.isNotBlank()) {
                                        Text(m.date, style = tabular(MaterialTheme.typography.labelSmall), color = p.inkMuted)
                                    }
                                    if (m.topic.isNotBlank()) {
                                        Text(m.topic, style = MaterialTheme.typography.labelSmall, color = p.inkMuted)
                                    }
                                }
                                val isPresent = m.status.contains("hadir", ignoreCase = true) ||
                                    m.status.contains("present", ignoreCase = true)
                                val localizedStatus = when (m.status.lowercase()) {
                                    "hadir" -> tr("Present", "Hadir")
                                    "izin" -> tr("Excused", "Izin")
                                    "sakit" -> tr("Sick", "Sakit")
                                    "alpha" -> tr("Absent", "Alpha")
                                    else -> m.status
                                }
                                FruitBadge(
                                    text = localizedStatus,
                                    accent = if (isPresent) p.mint else p.strawberry,
                                    softBg = if (isPresent) p.mintSoft else p.strawberrySoft
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedCourse = null }) {
                    Text(tr("Close", "Tutup"), color = p.ink)
                }
            }
        )
    }
}

@Composable
private fun OnlinePresenceCard(
    item: OnlineAttendanceItem,
    onAttend: (OnlineAttendanceItem) -> Unit,
    onConfirmRps: (OnlineAttendanceItem, Boolean) -> Unit
) {
    val p = DinusTheme.palette
    val accent = if (item.isAttended) p.mint else p.mango
    val soft = if (item.isAttended) p.mintSoft else p.mangoSoft

    SorbetCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${item.startTime}–${item.endTime}", style = tabular(MaterialTheme.typography.labelLarge), color = accent)
            FruitBadge(
                when {
                    item.isAttended -> tr("Present", "Sudah Presensi")
                    item.isOpenClass -> tr("Attend Now", "Bisa Absen")
                    else -> tr("Waiting", "Menunggu")
                },
                accent,
                soft
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(item.courseName, style = MaterialTheme.typography.titleMedium, color = p.ink)
        if (item.room.isNotBlank() || item.lecturer.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                listOfNotNull(
                    item.room.takeIf { it.isNotBlank() }?.let { "${tr("Room", "Ruang")} $it" },
                    item.lecturer.takeIf { it.isNotBlank() }
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = p.inkMuted
            )
        }

        if (!item.isAttended && item.idMaster.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onAttend(item) },
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(containerColor = p.lime, contentColor = p.onLime),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(tr("Attend Now", "Absen Sekarang"))
            }
        }

        if ((item.rpsPlan.isNotBlank() || item.rpsRealization.isNotBlank()) && !item.isRpsConfirmed && item.idMaster.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            SorbetCard(containerColor = p.surfaceVariant, cornerRadius = 14.dp, contentPadding = PaddingValues(12.dp)) {
                Text(tr("Confirm RPS Realization", "Konfirmasi Realisasi RPS"), style = MaterialTheme.typography.labelLarge, color = p.ink)
                if (item.rpsPlan.isNotBlank()) Text("${tr("Plan", "Rencana")}: ${item.rpsPlan}", style = MaterialTheme.typography.labelSmall, color = p.inkMuted)
                if (item.rpsRealization.isNotBlank()) Text("${tr("Realization", "Realisasi")}: ${item.rpsRealization}", style = MaterialTheme.typography.labelSmall, color = p.inkMuted)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { onConfirmRps(item, true) },
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = p.mintSoft, contentColor = p.mint),
                        modifier = Modifier.weight(1f)
                    ) { Text(tr("Match", "Sesuai")) }
                    FilledTonalButton(
                        onClick = { onConfirmRps(item, false) },
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = p.strawberrySoft, contentColor = p.strawberry),
                        modifier = Modifier.weight(1f)
                    ) { Text(tr("Mismatch", "Tidak")) }
                }
            }
        }
    }
}

@Composable
private fun AttendanceSummaryCard(course: AttendanceCourse, onClick: () -> Unit) {
    val p = DinusTheme.palette
    val pct = course.percentage.coerceIn(0.0, 100.0)
    val accent = when {
        pct >= 75.0 -> p.mint
        pct >= 60.0 -> p.mango
        else -> p.strawberry
    }

    SorbetCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularRingBadge(percentage = pct, accent = accent)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(course.kdmk, style = tabular(MaterialTheme.typography.labelSmall), color = p.inkMuted)
                Text(course.courseName, style = MaterialTheme.typography.titleMedium, color = p.ink)
                if (course.scheduleSummary.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(course.scheduleSummary, style = tabular(MaterialTheme.typography.labelSmall), color = p.inkMuted)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FruitBadge("${tr("Present", "Hadir")} ${course.hadir}", p.mint, p.mintSoft)
                    if (course.alpha > 0) FruitBadge("${tr("Absent", "Alpha")} ${course.alpha}", p.strawberry, p.strawberrySoft)
                    else FruitBadge("0 ${tr("Absent", "Alpha")}", p.ink, p.surfaceVariant)
                }
            }
        }
    }
}

@Composable
fun AttendanceLogsScreen(
    repo: DinusRepository,
    onBack: () -> Unit
) {
    val p = DinusTheme.palette
    val lang = LocalAppLanguage.current
    val logs by repo.attendanceLogs.collectAsState()
    val formatter = remember(lang) {
        val locale = if (lang == AppLanguage.INDONESIAN) Locale("in", "ID") else Locale.ENGLISH
        SimpleDateFormat("dd MMM · HH:mm:ss", locale)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CurvedHeroHeader(
            title = tr("Auto-Attendance History", "Riwayat Auto-Absen"),
            subtitle = tr("Local auto-attendance & notification action logs", "Log presensi otomatis & aksi notifikasi"),
            gradientStart = p.mint,
            gradientEnd = p.sky,
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (logs.isEmpty()) {
                item {
                    SorbetEmptyState(
                        icon = DinusIcons.CheckCircle,
                        accent = p.mint,
                        softBg = p.mintSoft,
                        title = tr("No logs yet", "Belum ada riwayat"),
                        subtitle = tr(
                            "Every submission from notifications or last-minute auto-attend is recorded here.",
                            "Setiap absen dari notifikasi atau auto-absen detik terakhir tercatat di sini."
                        )
                    )
                }
            } else {
                items(logs.size) { idx ->
                    val entry = logs[idx]
                    val modeLabel = when (entry.mode) {
                        "AUTO_LAST_MINUTE" -> tr("Auto-Attend", "Auto-Absen")
                        "NOTIF_ACTION" -> tr("Notif Action", "Tombol Notif")
                        else -> tr("App", "Aplikasi")
                    }
                    val accent = if (entry.success) p.mint else p.strawberry
                    val soft = if (entry.success) p.mintSoft else p.strawberrySoft

                    SorbetCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(formatter.format(Date(entry.timestampMillis)), style = tabular(MaterialTheme.typography.labelSmall), color = p.inkMuted)
                            FruitBadge(modeLabel, accent, soft)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(entry.courseName, style = MaterialTheme.typography.titleMedium, color = p.ink)
                        Spacer(Modifier.height(2.dp))
                        Text(entry.message, style = MaterialTheme.typography.bodyMedium, color = accent, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
