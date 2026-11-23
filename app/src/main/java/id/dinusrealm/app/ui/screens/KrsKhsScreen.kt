package id.dinusrealm.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import id.dinusrealm.app.core.i18n.tr
import id.dinusrealm.app.core.model.ExamScheduleBundle
import id.dinusrealm.app.core.model.IpChartPoint
import id.dinusrealm.app.core.model.KhsCourse
import id.dinusrealm.app.core.model.KhsSummary
import id.dinusrealm.app.core.model.KrsCourse
import id.dinusrealm.app.core.model.KrsSummary
import id.dinusrealm.app.core.model.SemesterKhsGroup
import id.dinusrealm.app.core.model.SemesterKrsGroup
import id.dinusrealm.app.core.model.TranscriptSummary
import id.dinusrealm.app.core.network.ApiResult
import id.dinusrealm.app.core.repo.DinusRepository
import id.dinusrealm.app.ui.components.ColorfulCourseCard
import id.dinusrealm.app.ui.components.CurvedHeroHeader
import id.dinusrealm.app.ui.components.FruitBadge
import id.dinusrealm.app.ui.components.KeyValueRow
import id.dinusrealm.app.ui.components.SorbetCard
import id.dinusrealm.app.ui.components.SorbetEmptyState
import id.dinusrealm.app.ui.components.SorbetLoadingBox
import id.dinusrealm.app.ui.components.SorbetMessageStrip
import id.dinusrealm.app.ui.components.SorbetSegmentTabs
import id.dinusrealm.app.ui.components.tabular
import id.dinusrealm.app.ui.theme.DinusIcons
import id.dinusrealm.app.ui.theme.DinusTheme
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun KrsScreen(
    repo: DinusRepository,
    onBack: () -> Unit
) {
    val p = DinusTheme.palette
    val scope = rememberCoroutineScope()
    var tabIdx by remember { mutableIntStateOf(0) }
    var summary by remember { mutableStateOf(KrsSummary()) }
    var currentCourses by remember { mutableStateOf<List<KrsCourse>>(emptyList()) }
    var pastGroups by remember { mutableStateOf<List<SemesterKrsGroup>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun load() {
        isLoading = true
        scope.launch {
            coroutineScope {
                val h = async { repo.api.getKrsHeader() }
                val c = async { repo.api.getKrsCurrent() }
                val pRes = async { repo.api.getKrsHistoryGroups() }
                (h.await() as? ApiResult.Success)?.let { summary = it.data }
                (c.await() as? ApiResult.Success)?.data?.let { (_, list) -> currentCourses = list }
                (pRes.await() as? ApiResult.Success)?.let { pastGroups = it.data }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(modifier = Modifier.fillMaxSize()) {
        CurvedHeroHeader(
            title = tr("Study Plan (KRS)", "Kartu Rencana Studi"),
            subtitle = listOfNotNull(summary.semester.takeIf { it.isNotBlank() }, summary.academicYear.takeIf { it.isNotBlank() })
                .joinToString(" · ")
                .ifBlank { tr("Active plan & SKS history", "Rencana & riwayat SKS") },
            gradientStart = p.cobalt,
            gradientEnd = p.sky,
            onBack = onBack,
            rightStat = "${summary.currentSks}",
            rightStatLabel = "SKS · ${summary.courseCount} ${tr("Courses", "MK")}"
        )

        Spacer(Modifier.height(6.dp))
        SorbetSegmentTabs(listOf(tr("Current", "Semester Ini"), tr("History", "KRS Lalu")), tabIdx, { tabIdx = it }, accent = p.sky)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isLoading) {
                item { SorbetLoadingBox(tr("Loading KRS...", "Memuat KRS..."), p.sky) }
            } else if (tabIdx == 0) {
                if (currentCourses.isEmpty()) {
                    item {
                        SorbetEmptyState(
                            icon = DinusIcons.Calendar,
                            accent = p.sky,
                            softBg = p.skySoft,
                            title = tr("No active KRS found", "KRS semester ini kosong"),
                            subtitle = tr("No courses recorded for the active semester.", "Belum ada mata kuliah tercatat pada KRS aktif."),
                            actionLabel = tr("Reload", "Muat Ulang"),
                            onAction = { load() }
                        )
                    }
                } else {
                    items(currentCourses.size) { i ->
                        KrsCourseItemCard(currentCourses[i], i, p)
                    }
                }
            } else {
                if (pastGroups.isEmpty()) {
                    item {
                        SorbetEmptyState(
                            icon = DinusIcons.Calendar,
                            accent = p.berry,
                            softBg = p.berrySoft,
                            title = tr("KRS history is empty", "Riwayat KRS kosong"),
                            subtitle = tr("No previous semester KRS records found.", "Belum ada data KRS semester sebelumnya.")
                        )
                    }
                } else {
                    pastGroups.forEachIndexed { gIdx, group ->
                        item(key = "krs_hdr_$gIdx") {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = if (gIdx > 0) 6.dp else 0.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(group.academicYear, style = MaterialTheme.typography.titleMedium, color = p.ink)
                                FruitBadge("${group.courses.sumOf { it.sks }} SKS", p.ink, p.surfaceVariant)
                            }
                        }
                        items(group.courses.size, key = { idx -> "krs_${gIdx}_$idx" }) { idx ->
                            KrsCourseItemCard(group.courses[idx], idx + gIdx, p)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KrsCourseItemCard(item: KrsCourse, idx: Int, p: id.dinusrealm.app.ui.theme.DinusFruitPalette) {
    val (cStart, cEnd) = p.cycleAccent(idx)
    val meta = listOfNotNull(
        item.klpk.takeIf { it.isNotBlank() },
        item.scheduleText.takeIf { it.isNotBlank() },
        item.statusTag.takeIf { it.isNotBlank() }
    ).joinToString(" · ")
    ColorfulCourseCard(
        colorStart = cStart,
        colorEnd = cEnd,
        badgeCode = item.room.ifBlank { item.kdmk },
        title = item.courseName,
        subtitle = meta,
        trailingPill = "${item.sks} SKS"
    )
}

@Composable
fun KhsScreen(
    repo: DinusRepository,
    onBack: () -> Unit
) {
    val p = DinusTheme.palette
    val scope = rememberCoroutineScope()
    var tabIdx by remember { mutableIntStateOf(0) }
    var header by remember { mutableStateOf(KhsSummary()) }
    var nowSummary by remember { mutableStateOf(KhsSummary()) }
    var nowCourses by remember { mutableStateOf<List<KhsCourse>>(emptyList()) }
    var pastGroups by remember { mutableStateOf<List<SemesterKhsGroup>>(emptyList()) }
    var chartPoints by remember { mutableStateOf<List<IpChartPoint>>(emptyList()) }
    var transcript by remember { mutableStateOf(TranscriptSummary()) }
    var isLoading by remember { mutableStateOf(true) }

    fun load() {
        isLoading = true
        scope.launch {
            coroutineScope {
                val h = async { repo.api.getKhsHeader() }
                val n = async { repo.api.getKhsCurrent() }
                val a = async { repo.api.getKhsHistoryGroups() }
                val g = async { repo.api.getIpChart() }
                val t = async { repo.api.getTranscript() }
                (h.await() as? ApiResult.Success)?.let { header = it.data }
                (n.await() as? ApiResult.Success)?.data?.let { (sum, list) ->
                    nowSummary = sum
                    nowCourses = list
                }
                (a.await() as? ApiResult.Success)?.let { pastGroups = it.data }
                (g.await() as? ApiResult.Success)?.let { chartPoints = it.data }
                (t.await() as? ApiResult.Success)?.let { transcript = it.data }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(modifier = Modifier.fillMaxSize()) {
        CurvedHeroHeader(
            title = tr("Grade Report (KHS)", "KHS, Grafik & Transkrip"),
            subtitle = "${header.totalSks} ${tr("SKS earned", "SKS lulus")} · ${nowSummary.academicYear.ifBlank { tr("GPA & Transcript", "Indeks Prestasi") }}",
            gradientStart = p.berry,
            gradientEnd = p.berry,
            onBack = onBack,
            rightStat = header.ipk,
            rightStatLabel = tr("Cumulative GPA", "IPK Kumulatif")
        )

        Spacer(Modifier.height(6.dp))
        SorbetSegmentTabs(
            listOf(
                tr("Current", "KHS Ini"),
                tr("History", "KHS Lalu"),
                tr("GPA Chart", "Grafik IP"),
                tr("Transcript", "Transkrip")
            ),
            tabIdx,
            { tabIdx = it },
            accent = p.berry
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isLoading) {
                item { SorbetLoadingBox(tr("Loading grades...", "Memuat nilai akademik..."), p.berry) }
            } else {
                when (tabIdx) {
                    0 -> {
                        if (nowCourses.isEmpty()) {
                            item {
                                SorbetEmptyState(
                                    icon = DinusIcons.Academic,
                                    accent = p.berry,
                                    softBg = p.berrySoft,
                                    title = tr("No grades posted yet", "KHS semester ini belum terinput"),
                                    subtitle = tr("Current semester final grades will appear here.", "Nilai akhir semester berjalan akan tampil di sini."),
                                    actionLabel = tr("View History", "Cek KHS Lalu"),
                                    onAction = { tabIdx = 1 }
                                )
                            }
                        } else {
                            items(nowCourses.size) { i -> KhsCourseCard(nowCourses[i]) }
                        }
                    }

                    1 -> {
                        if (pastGroups.isEmpty()) {
                            item {
                                SorbetEmptyState(
                                    icon = DinusIcons.Academic,
                                    accent = p.berry,
                                    softBg = p.berrySoft,
                                    title = tr("Grade history is empty", "KHS semester lalu kosong"),
                                    subtitle = tr("No past semester KHS records available.", "Belum ada riwayat KHS tersedia.")
                                )
                            }
                        } else {
                            pastGroups.forEachIndexed { gIdx, group ->
                                item(key = "khs_hdr_$gIdx") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = if (gIdx > 0) 6.dp else 0.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(group.academicYear, style = MaterialTheme.typography.titleMedium, color = p.ink)
                                        FruitBadge("IPS ${group.ips}", p.ink, p.surfaceVariant)
                                    }
                                }
                                items(group.courses.size, key = { idx -> "khs_${gIdx}_$idx" }) { idx ->
                                    KhsCourseCard(group.courses[idx])
                                }
                            }
                        }
                    }

                    2 -> {
                        if (chartPoints.isEmpty()) {
                            item {
                                SorbetEmptyState(
                                    icon = DinusIcons.Academic,
                                    accent = p.berry,
                                    softBg = p.berrySoft,
                                    title = tr("GPA chart unavailable", "Grafik IP belum tersedia"),
                                    subtitle = tr("Semester GPA records are empty.", "Data riwayat IP per semester masih kosong.")
                                )
                            }
                        } else {
                            item {
                                SorbetCard {
                                    Text(tr("Semester IPS & Cumulative GPA Trend", "Tren IPS & IPK per Semester"), style = MaterialTheme.typography.titleMedium, color = p.ink)
                                    Spacer(Modifier.height(14.dp))
                                    val ipsColor = p.mango
                                    val ipkColor = p.mint
                                    val gridColor = p.border
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(170.dp)
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        val w = size.width
                                        val h = size.height
                                        for (step in 0..4) {
                                            val y = h * (step / 4f)
                                            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
                                        }
                                        val count = chartPoints.size
                                        fun pointX(i: Int) = if (count <= 1) w / 2f else (i.toFloat() / (count - 1)) * w
                                        fun pointY(v: Double) = h - ((v.coerceIn(0.0, 4.0) / 4.0).toFloat() * h)

                                        val ipsPath = Path()
                                        val ipkPath = Path()
                                        chartPoints.forEachIndexed { idx, pt ->
                                            val x = pointX(idx)
                                            val yIps = pointY(pt.ips)
                                            val yIpk = pointY(pt.ipk)
                                            if (idx == 0) {
                                                ipsPath.moveTo(x, yIps)
                                                ipkPath.moveTo(x, yIpk)
                                            } else {
                                                ipsPath.lineTo(x, yIps)
                                                ipkPath.lineTo(x, yIpk)
                                            }
                                        }
                                        drawPath(ipsPath, ipsColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                                        drawPath(ipkPath, ipkColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                                        chartPoints.forEachIndexed { idx, pt ->
                                            val x = pointX(idx)
                                            drawCircle(ipsColor, 5.dp.toPx(), Offset(x, pointY(pt.ips)))
                                            drawCircle(ipkColor, 5.dp.toPx(), Offset(x, pointY(pt.ipk)))
                                        }
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FruitBadge("IPS", ipsColor, p.mangoSoft)
                                        FruitBadge(tr("Cumulative GPA", "IPK Kumulatif"), ipkColor, p.mintSoft)
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    chartPoints.forEach { pt ->
                                        KeyValueRow(
                                            pt.semesterLabel,
                                            "IPS ${"%.2f".format(pt.ips)} · ${tr("GPA", "IPK")} ${"%.2f".format(pt.ipk)}"
                                        )
                                    }
                                }
                            }
                            if (header.gradeDistribution.isNotEmpty()) {
                                item {
                                    SorbetCard {
                                        Text(tr("Grade Distribution", "Sebaran Nilai Huruf"), style = MaterialTheme.typography.titleMedium, color = p.ink)
                                        Spacer(Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                            header.gradeDistribution.take(6).forEach { (grade, count) ->
                                                FruitBadge("$grade: $count", p.ink, p.surfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        item {
                            SorbetCard {
                                KeyValueRow(tr("Total Credits", "Total SKS Transkrip"), "${transcript.totalSks} SKS")
                                KeyValueRow(tr("Cumulative GPA", "IPK Kumulatif"), header.ipk, valueColor = p.mint)
                                KeyValueRow(tr("Completed Courses", "Jumlah Mata Kuliah"), "${transcript.courses.size} ${tr("Courses", "MK")}")
                            }
                        }
                        items(transcript.courses.size) { i -> KhsCourseCard(transcript.courses[i]) }
                    }
                }
            }
        }
    }
}

@Composable
private fun KhsCourseCard(course: KhsCourse) {
    val p = DinusTheme.palette
    val isGood = course.grade.startsWith("A") || course.grade.startsWith("B")
    val accent = if (isGood) p.mint else if (course.grade == "-") p.inkMuted else p.mango
    val soft = if (isGood) p.mintSoft else if (course.grade == "-") p.surfaceVariant else p.mangoSoft

    SorbetCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Text("${course.kdmk} · ${course.sks} SKS", style = tabular(MaterialTheme.typography.labelSmall), color = p.inkMuted)
                Spacer(Modifier.height(2.dp))
                Text(course.courseName, style = MaterialTheme.typography.titleMedium, color = p.ink)
                val details = listOfNotNull(
                    course.lecturer.takeIf { it.isNotBlank() },
                    course.utsScore.takeIf { it.isNotBlank() && it != "-" }?.let { "UTS $it" },
                    course.uasScore.takeIf { it.isNotBlank() && it != "-" }?.let { "UAS $it" }
                ).joinToString(" · ")
                if (details.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(details, style = tabular(MaterialTheme.typography.labelSmall), color = p.inkMuted)
                }
            }
            FruitBadge(course.grade, accent, soft)
        }
    }
}

@Composable
fun ExamsScreen(
    repo: DinusRepository,
    onBack: () -> Unit
) {
    val p = DinusTheme.palette
    val scope = rememberCoroutineScope()
    var tabIdx by remember { mutableIntStateOf(0) }
    var utsBundle by remember { mutableStateOf(ExamScheduleBundle()) }
    var uasBundle by remember { mutableStateOf(ExamScheduleBundle()) }
    var isLoading by remember { mutableStateOf(true) }

    fun load() {
        isLoading = true
        scope.launch {
            coroutineScope {
                val u1 = async { repo.api.getExamSchedule("UTS") }
                val u2 = async { repo.api.getExamSchedule("UAS") }
                (u1.await() as? ApiResult.Success)?.let { utsBundle = it.data }
                (u2.await() as? ApiResult.Success)?.let { uasBundle = it.data }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    val activeBundle = if (tabIdx == 0) utsBundle else uasBundle

    Column(modifier = Modifier.fillMaxSize()) {
        CurvedHeroHeader(
            title = tr("Exam Schedule", "Jadwal Ujian"),
            subtitle = tr("UTS, UAS, seat numbers & eligibility", "UTS, UAS, nomor kursi & syarat pembayaran"),
            gradientStart = p.mango,
            gradientEnd = p.peach,
            onBack = onBack,
            rightStat = "${activeBundle.items.size}",
            rightStatLabel = if (tabIdx == 0) tr("UTS Exams", "Mata Uji UTS") else tr("UAS Exams", "Mata Uji UAS")
        )

        Spacer(Modifier.height(6.dp))
        SorbetSegmentTabs(listOf("UTS", "UAS"), tabIdx, { tabIdx = it }, accent = p.mango)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (activeBundle.paymentStatusMessage.isNotBlank()) {
                item {
                    SorbetMessageStrip(
                        message = activeBundle.paymentStatusMessage,
                        accent = if (activeBundle.isPaid) p.mint else p.strawberry,
                        softBg = if (activeBundle.isPaid) p.mintSoft else p.strawberrySoft
                    )
                }
            }

            if (isLoading) {
                item { SorbetLoadingBox(tr("Loading exam schedule...", "Memuat jadwal ujian..."), p.mango) }
            } else if (activeBundle.items.isEmpty()) {
                item {
                    SorbetEmptyState(
                        icon = DinusIcons.Clock,
                        accent = p.mango,
                        softBg = p.mangoSoft,
                        title = tr("Exam schedule unavailable", "Jadwal ujian belum tersedia"),
                        subtitle = tr(
                            "${if (tabIdx == 0) "UTS" else "UAS"} schedule has not been released yet.",
                            "Jadwal ${if (tabIdx == 0) "UTS" else "UAS"} belum dirilis oleh akademik."
                        ),
                        actionLabel = tr("Reload", "Muat Ulang"),
                        onAction = { load() }
                    )
                }
            } else {
                items(activeBundle.items.size) { i ->
                    val ex = activeBundle.items[i]
                    SorbetCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = listOfNotNull(ex.date.takeIf { it.isNotBlank() }, ex.time.takeIf { it.isNotBlank() })
                                    .joinToString(" · ")
                                    .ifBlank { tr("Schedule TBA", "Jadwal menyusul") },
                                style = tabular(MaterialTheme.typography.labelLarge),
                                color = p.mango
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (ex.room.isNotBlank()) FruitBadge(ex.room, p.ink, p.surfaceVariant)
                                if (ex.seatNumber.isNotBlank()) FruitBadge("${tr("Seat", "Kursi")} ${ex.seatNumber}", p.onLime, p.lime)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(ex.courseName, style = MaterialTheme.typography.titleMedium, color = p.ink)
                        val extra = listOfNotNull(
                            ex.klpk.takeIf { it.isNotBlank() },
                            ex.sks.takeIf { it > 0 }?.let { "$it SKS" },
                            ex.practicalDate.takeIf { it.isNotBlank() }?.let { "${tr("Lab", "Praktik")}: $it ${ex.practicalTime}" }
                        ).joinToString(" · ")
                        if (extra.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(extra, style = MaterialTheme.typography.labelMedium, color = p.inkMuted)
                        }
                    }
                }
            }
        }
    }
}
