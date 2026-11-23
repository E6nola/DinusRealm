package id.dinusrealm.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.dinusrealm.app.core.engine.ScheduleTimeUtil
import id.dinusrealm.app.core.i18n.LocalAppLanguage
import id.dinusrealm.app.core.i18n.dayName
import id.dinusrealm.app.core.i18n.shortDayName
import id.dinusrealm.app.core.i18n.tr
import id.dinusrealm.app.core.model.AppLanguage
import id.dinusrealm.app.core.model.ScheduleSlot
import id.dinusrealm.app.core.model.ThemeMode
import id.dinusrealm.app.core.repo.DinusRepository
import id.dinusrealm.app.ui.SubScreen
import id.dinusrealm.app.ui.components.ColorfulCourseCard
import id.dinusrealm.app.ui.components.FruitBadge
import id.dinusrealm.app.ui.components.SoftGridTile
import id.dinusrealm.app.ui.components.SorbetCard
import id.dinusrealm.app.ui.components.SorbetMessageStrip
import id.dinusrealm.app.ui.components.SorbetSegmentTabs
import id.dinusrealm.app.ui.components.formatRupiah
import id.dinusrealm.app.ui.components.tabular
import id.dinusrealm.app.ui.theme.DinusIcons
import id.dinusrealm.app.ui.theme.DinusTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    repo: DinusRepository,
    onNavigateSub: (SubScreen) -> Unit,
    onOpenScheduleTab: () -> Unit
) {
    val p = DinusTheme.palette
    val lang = LocalAppLanguage.current
    val scope = rememberCoroutineScope()

    val profile by repo.miniProfile.collectAsState()
    val krsSummary by repo.krsSummary.collectAsState()
    val khsHeader by repo.khsHeader.collectAsState()
    val todaySlots by repo.todaySlots.collectAsState()
    val weeklySlots by repo.weeklySlots.collectAsState()
    val onlineItems by repo.onlineAttendance.collectAsState()
    val billingItems by repo.billingItems.collectAsState()
    val settings by repo.settings.collectAsState()
    val isRefreshing by repo.isRefreshing.collectAsState()
    val bannerMsg by repo.bannerMessage.collectAsState()

    val (nowMin, _) = remember(todaySlots, isRefreshing) { ScheduleTimeUtil.currentMinutesAndSecond() }
    val heroSlot = remember(todaySlots, nowMin) { ScheduleTimeUtil.findHeroSlot(todaySlots, nowMin) }
    val nextDayBundle = remember<Pair<String, List<ScheduleSlot>>?>(weeklySlots, lang) {
        if (weeklySlots.isEmpty()) null
        else {
            val todayDay = ScheduleTimeUtil.currentSiadinDay()
            val candidateDays = (1..7).map { ((todayDay - 1 + it) % 7) + 1 }
            val nextDay = candidateDays.firstOrNull { d -> weeklySlots.any { it.dayIndex == d } }
            if (nextDay != null) {
                dayName(nextDay, lang) to weeklySlots.filter { it.dayIndex == nextDay }
            } else null
        }
    }

    val unpaidBills = remember(billingItems) { billingItems.filter { !it.isPaid && it.remainingAmount > 0L } }
    val totalDue = remember(unpaidBills) { unpaidBills.sumOf { it.remainingAmount } }
    var submittingId by remember { mutableStateOf<String?>(null) }
    var homeFilterTab by remember { mutableIntStateOf(0) }

    val displayName = settings.nickname.trim().takeIf { it.isNotBlank() }
        ?: profile?.name?.trim()?.split(" ")?.take(2)?.joinToString(" ")?.takeIf { it.isNotBlank() }
        ?: "Dinusian"
    val initialChar = displayName.firstOrNull()?.uppercase() ?: "D"
    val dateLabel = remember(lang) {
        val locale = if (lang == AppLanguage.INDONESIAN) Locale("id", "ID") else Locale.ENGLISH
        SimpleDateFormat("EEEE, d MMM", locale).format(Date())
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(p.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { onNavigateSub(SubScreen.Profile) },
                        shape = CircleShape,
                        color = p.cardActive,
                        border = if (p.isDark) BorderStroke(1.dp, p.border) else null,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = initialChar,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = p.lime
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text(
                            text = tr("Hi, $displayName!", "Hai, $displayName!"),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = p.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val nimSub = profile?.nim?.takeIf { it.isNotBlank() }
                            ?: repo.vault.getNim().orEmpty()
                        Text(
                            text = listOfNotNull(dateLabel, nimSub.takeIf { it.isNotBlank() })
                                .joinToString(" · "),
                            style = tabular(MaterialTheme.typography.labelMedium),
                            color = p.inkMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircleIconAction(
                        icon = if (p.isDark) DinusIcons.Sun else DinusIcons.Moon,
                        tint = p.ink,
                        bgColor = p.surface,
                        contentDescription = tr("Toggle Theme", "Ganti Tema"),
                        onClick = {
                            val next = if (p.isDark) ThemeMode.LIGHT else ThemeMode.DARK
                            repo.updateSettings(settings.copy(themeMode = next))
                        }
                    )
                    CircleIconAction(
                        icon = DinusIcons.Refresh,
                        tint = p.onCardActive,
                        bgColor = p.cardActive,
                        contentDescription = tr("Sync", "Sinkronkan"),
                        isBusy = isRefreshing,
                        onClick = { repo.refreshCoreDataAsync() }
                    )
                }
            }
        }

        if (!bannerMsg.isNullOrBlank()) {
            item {
                SorbetMessageStrip(
                    message = bannerMsg!!,
                    accent = p.mint,
                    softBg = p.mintSoft,
                    onDismiss = { repo.clearBanner() }
                )
            }
        }

        item {
            PermissionChecklistCard(showOnlyIfMissing = true)
        }

        item {
            LimeHeroBanner(
                hero = heroSlot,
                nextDayBundle = nextDayBundle,
                ipkValue = khsHeader.ipk,
                sksCount = krsSummary.currentSks,
                courseCount = krsSummary.courseCount,
                hasBills = billingItems.isNotEmpty(),
                totalDue = totalDue,
                isSubmitting = submittingId != null && submittingId == heroSlot?.idMaster,
                onSubmitAttendance = { slot ->
                    val master = slot.idMaster ?: return@LimeHeroBanner
                    submittingId = master
                    scope.launch {
                        repo.submitAttendance(master, slot.kdds, slot.courseName)
                        submittingId = null
                    }
                },
                onOpenSchedule = onOpenScheduleTab
            )
        }

        item {
            SorbetSegmentTabs(
                tabs = listOf(
                    tr("Overview", "Ringkasan"),
                    tr("Today", "Hari Ini"),
                    tr("This Week", "Minggu Ini")
                ),
                selectedIndex = homeFilterTab,
                onSelect = { homeFilterTab = it },
                modifier = Modifier.padding(horizontal = 0.dp)
            )
        }

        if (homeFilterTab == 0) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        val openCount = onlineItems.count { it.isOpenClass && !it.isAttended }
                        SoftGridTile(
                            title = tr("Attendance", "Presensi"),
                            subtitle = if (todaySlots.isNotEmpty()) {
                                "${todaySlots.count { it.isAttended }}/${todaySlots.size} ${tr("today", "hari ini")}"
                            } else tr("Recap & RPS", "Rekap & RPS"),
                            icon = DinusIcons.CheckCircle,
                            accent = p.ink,
                            softBg = p.surfaceVariant,
                            progress = 1f,
                            isFeatured = false,
                            statusLabel = if (openCount > 0) "$openCount ${tr("Open", "Buka")}"
                                else if (settings.autoAbsenEnabled) tr("Auto On", "Auto Aktif")
                                else tr("Monitor", "Pantau"),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateSub(SubScreen.Presence) }
                        )
                        SoftGridTile(
                            title = "KRS",
                            subtitle = if (krsSummary.currentSks > 0) "${krsSummary.currentSks} SKS · ${krsSummary.courseCount} ${tr("Courses", "MK")}" else tr("Study plan", "Rencana studi"),
                            icon = DinusIcons.Calendar,
                            accent = p.ink,
                            softBg = p.surfaceVariant,
                            progress = 1f,
                            statusLabel = if (krsSummary.isVerified) tr("Verified", "Terverif") else tr("Check KRS", "Cek KRS"),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateSub(SubScreen.Krs) }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        val ipkVal = khsHeader.ipk.toFloatOrNull() ?: 0f
                        SoftGridTile(
                            title = tr("KHS & GPA", "KHS & IPK"),
                            subtitle = if (ipkVal > 0f) "${tr("GPA", "IPK")} ${khsHeader.ipk} · ${khsHeader.totalSks} SKS" else tr("Grades & chart", "Nilai & grafik"),
                            icon = DinusIcons.Academic,
                            accent = p.ink,
                            softBg = p.surfaceVariant,
                            progress = 1f,
                            statusLabel = if (ipkVal > 0f) khsHeader.ipk else tr("View", "Lihat"),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateSub(SubScreen.Khs) }
                        )
                        SoftGridTile(
                            title = tr("Billing & VA", "Tagihan & VA"),
                            subtitle = if (unpaidBills.isEmpty() && billingItems.isNotEmpty()) tr("Paid · 5 Bank VAs", "Lunas · 5 Bank VA")
                                else if (totalDue > 0L) formatRupiah(totalDue)
                                else tr("BIKU & VAs", "Cek BIKU & VA"),
                            icon = DinusIcons.Wallet,
                            accent = p.ink,
                            softBg = p.surfaceVariant,
                            progress = 1f,
                            statusLabel = if (unpaidBills.isEmpty() && billingItems.isNotEmpty()) tr("Paid", "Lunas")
                                else if (totalDue > 0L) tr("Pay", "Bayar")
                                else "5 VA",
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateSub(SubScreen.Billing) }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        SoftGridTile(
                            title = tr("Exam Schedule", "Jadwal Ujian"),
                            subtitle = tr("UTS, UAS & seats", "UTS, UAS & kursi"),
                            icon = DinusIcons.Clock,
                            accent = p.ink,
                            softBg = p.surfaceVariant,
                            progress = 1f,
                            statusLabel = tr("Schedule", "Kartu"),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateSub(SubScreen.Exams) }
                        )
                        SoftGridTile(
                            title = tr("Settings", "Pengaturan"),
                            subtitle = if (settings.autoAbsenEnabled) tr("Auto ${settings.autoAbsenSecondsBeforeEnd}s · Local", "Auto ${settings.autoAbsenSecondsBeforeEnd}d · Lokal")
                                else tr("Alerts & language", "Notif & bahasa"),
                            icon = DinusIcons.Settings,
                            accent = p.ink,
                            softBg = p.surfaceVariant,
                            progress = 1f,
                            statusLabel = if (settings.classNotifEnabled) "On" else "Off",
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateSub(SubScreen.Settings) }
                        )
                    }
                }
            }
        }

        val openOthers = onlineItems.filter { it.isOpenClass && !it.isAttended && it.idMaster != heroSlot?.idMaster }
        if (openOthers.isNotEmpty() && homeFilterTab != 2) {
            item {
                Text(tr("Open Online Attendance", "Presensi Online Terbuka"), style = MaterialTheme.typography.titleMedium, color = p.ink)
            }
            items(openOthers.size) { idx ->
                val item = openOthers[idx]
                ColorfulCourseCard(
                    colorStart = p.cardActive,
                    colorEnd = p.cardActive,
                    badgeCode = item.room.ifBlank { "ONLINE" },
                    title = item.courseName,
                    subtitle = "${item.startTime}–${item.endTime}",
                    trailingPill = if (submittingId == item.idMaster) "..." else tr("Attend", "Absen"),
                    isHighlighted = true,
                    onClick = {
                        if (submittingId == null) {
                            submittingId = item.idMaster
                            scope.launch {
                                repo.submitAttendance(item.idMaster, item.kdds, item.courseName)
                                submittingId = null
                            }
                        }
                    }
                )
            }
        }

        val displayedSlots = when (homeFilterTab) {
            1 -> todaySlots
            2 -> weeklySlots
            else -> if (todaySlots.isNotEmpty()) todaySlots else nextDayBundle?.second.orEmpty()
        }
        val sectionTitle = when (homeFilterTab) {
            1 -> "${tr(lang, "Today's Classes", "Kuliah Hari Ini")} (${todaySlots.size})"
            2 -> "${tr(lang, "This Week's Schedule", "Jadwal Minggu Ini")} (${weeklySlots.size})"
            else -> if (todaySlots.isNotEmpty()) {
                "${tr(lang, "Today's Classes", "Kuliah Hari Ini")} (${todaySlots.size})"
            } else if (nextDayBundle != null) {
                "${tr(lang, "Next Class Day", "Jadwal Berikutnya")} · ${nextDayBundle.first}"
            } else {
                tr(lang, "Class Schedule", "Jadwal Kuliah")
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(sectionTitle, style = MaterialTheme.typography.titleMedium, color = p.ink)
                if (weeklySlots.isNotEmpty()) {
                    FruitBadge("${weeklySlots.size} ${tr("sessions/wk", "sesi/mgg")}", p.ink, p.surfaceVariant)
                }
            }
        }

        if (displayedSlots.isEmpty()) {
            item {
                SorbetCard(onClick = onOpenScheduleTab) {
                    Text(
                        text = if (homeFilterTab == 1 && nextDayBundle != null) tr("No classes today", "Tidak ada jadwal hari ini")
                            else tr("No schedule cached yet", "Belum ada jadwal tersimpan"),
                        style = MaterialTheme.typography.titleMedium,
                        color = p.ink
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (homeFilterTab == 1 && nextDayBundle != null) {
                            tr(
                                "Today is free. Tap 'This Week' or open the full Schedule.",
                                "Hari ini kosong. Ketuk tab 'Minggu Ini' atau buka menu Jadwal lengkap."
                            )
                        } else {
                            tr(
                                "Tap the Sync button above to fetch your SiAdin schedule.",
                                "Ketuk tombol Sinkron di kanan atas untuk memuat jadwal kuliah dari SiAdin."
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = p.inkMuted
                    )
                }
            }
        } else {
            items(displayedSlots.size) { idx ->
                val slot = displayedSlots[idx]
                val showDayPrefix = homeFilterTab == 2 || (homeFilterTab == 0 && todaySlots.isEmpty())
                val dayPrefix = if (showDayPrefix && slot.dayIndex > 0) shortDayName(slot.dayIndex, lang) else null
                val meta = listOfNotNull(
                    dayPrefix,
                    "${slot.startTime}–${slot.endTime}",
                    slot.lecturer.takeIf { it.isNotBlank() }
                ).joinToString(" · ")
                val isSlotOpen = homeFilterTab != 2 &&
                    slot.dayIndex == 0 &&
                    slot.isOpenClass &&
                    !slot.isAttended &&
                    !slot.idMaster.isNullOrBlank()
                val pill = when {
                    slot.isAttended -> tr("Present", "Hadir")
                    isSlotOpen -> tr("Attend", "Bisa Absen")
                    slot.sks > 0 -> "${slot.sks} SKS"
                    else -> slot.klpk
                }
                ColorfulCourseCard(
                    colorStart = p.surface,
                    colorEnd = p.surface,
                    badgeCode = slot.room.ifBlank { slot.klpk },
                    title = slot.courseName,
                    subtitle = meta,
                    trailingPill = pill,
                    isHighlighted = isSlotOpen,
                    onClick = onOpenScheduleTab
                )
            }
        }
    }
}

@Composable
private fun LimeHeroBanner(
    hero: ScheduleSlot?,
    nextDayBundle: Pair<String, List<ScheduleSlot>>?,
    ipkValue: String,
    sksCount: Int,
    courseCount: Int,
    hasBills: Boolean,
    totalDue: Long,
    isSubmitting: Boolean,
    onSubmitAttendance: (ScheduleSlot) -> Unit,
    onOpenSchedule: () -> Unit
) {
    val p = DinusTheme.palette
    val nextFallback = nextDayBundle?.second?.firstOrNull()
    val isAlert = hero != null &&
        hero.dayIndex == 0 &&
        hero.isOpenClass &&
        !hero.isAttended &&
        !hero.idMaster.isNullOrBlank()

    val bigHeadline = when {
        hero != null && hero.room.isNotBlank() -> "${hero.room} ↑"
        hero != null -> "${hero.startTime} ↑"
        nextFallback != null -> "${nextFallback.startTime} · ${nextFallback.room.ifBlank { nextDayBundle?.first.orEmpty() }}"
        else -> tr("SiAdin Ready", "SiAdin Siap")
    }
    val courseSubtitle = when {
        hero != null -> "${hero.courseName} · ${hero.startTime}–${hero.endTime}"
        nextFallback != null -> "${tr("Next", "Berikutnya")} (${nextDayBundle?.first}): ${nextFallback.courseName}"
        else -> tr("All campus data synced offline", "Semua data kampus tersinkron offline")
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = p.lime,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenSchedule)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = bigHeadline,
                        style = tabular(MaterialTheme.typography.headlineMedium).copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.8).sp
                        ),
                        color = p.onLime,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = courseSubtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = p.onLime.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isAlert || hero?.isAttended == true) DinusIcons.CheckCircle else DinusIcons.Calendar,
                            contentDescription = tr("Schedule", "Jadwal"),
                            tint = Color(0xFF141618),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            if (isAlert && !hero?.idMaster.isNullOrBlank()) {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { onSubmitAttendance(hero!!) },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF141618),
                        contentColor = p.lime
                    )
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = p.lime)
                    } else {
                        Text(tr("Attend Now", "Absen Sekarang"), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HeroTelemetryCol(
                    value = ipkValue.takeIf { it.isNotBlank() && it != "0.00" } ?: "-",
                    label = tr("Cumulative GPA", "IPK Kumulatif"),
                    inkColor = p.onLime,
                    modifier = Modifier.weight(1f)
                )
                HeroTelemetryCol(
                    value = if (sksCount > 0) "$sksCount SKS" else "-",
                    label = if (courseCount > 0) "$courseCount ${tr("Courses", "Matkul")}" else tr("Active KRS", "KRS Aktif"),
                    inkColor = p.onLime,
                    modifier = Modifier.weight(1f)
                )
                val billStatus = when {
                    hasBills && totalDue <= 0L -> tr("Paid", "Lunas")
                    totalDue > 0L -> formatRupiah(totalDue)
                    else -> tr("Active", "Aktif")
                }
                HeroTelemetryCol(
                    value = billStatus,
                    label = tr("Finance", "Keuangan"),
                    inkColor = p.onLime,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroTelemetryCol(
    value: String,
    label: String,
    inkColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = tabular(MaterialTheme.typography.titleMedium).copy(fontWeight = FontWeight.ExtraBold),
            color = inkColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = inkColor.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CircleIconAction(
    icon: ImageVector,
    tint: Color,
    bgColor: Color,
    contentDescription: String,
    isBusy: Boolean = false,
    onClick: () -> Unit
) {
    val p = DinusTheme.palette
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bgColor,
        shadowElevation = 0.dp,
        border = if (p.isDark) BorderStroke(1.dp, p.border) else null,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isBusy) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = tint)
            } else {
                Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(18.dp))
            }
        }
    }
}
