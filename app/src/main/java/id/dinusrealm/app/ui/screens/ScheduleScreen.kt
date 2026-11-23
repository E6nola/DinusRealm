package id.dinusrealm.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.dinusrealm.app.core.engine.ScheduleTimeUtil
import id.dinusrealm.app.core.i18n.LocalAppLanguage
import id.dinusrealm.app.core.i18n.dayName
import id.dinusrealm.app.core.i18n.tr
import id.dinusrealm.app.core.repo.DinusRepository
import id.dinusrealm.app.ui.SubScreen
import id.dinusrealm.app.ui.components.ColorfulCourseCard
import id.dinusrealm.app.ui.components.CurvedHeroHeader
import id.dinusrealm.app.ui.components.SorbetEmptyState
import id.dinusrealm.app.ui.components.SorbetSegmentTabs
import id.dinusrealm.app.ui.theme.DinusIcons
import id.dinusrealm.app.ui.theme.DinusTheme

@Composable
fun ScheduleScreen(
    repo: DinusRepository,
    onNavigateSub: (SubScreen) -> Unit
) {
    val p = DinusTheme.palette
    val lang = LocalAppLanguage.current
    val todaySlots by repo.todaySlots.collectAsState()
    val weeklySlots by repo.weeklySlots.collectAsState()
    var selectedDay by remember { mutableIntStateOf(0) }

    val dayLabels = remember(lang) {
        (0..5).map { dayName(it, lang) }
    }

    val shownSlots = remember(selectedDay, todaySlots, weeklySlots) {
        if (selectedDay == 0) {
            if (todaySlots.isNotEmpty()) todaySlots
            else weeklySlots.filter { it.dayIndex == ScheduleTimeUtil.currentSiadinDay() }
        } else {
            weeklySlots.filter { it.dayIndex == selectedDay }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CurvedHeroHeader(
            title = tr("Class Schedule", "Jadwal Kuliah"),
            subtitle = tr("Cached offline for instant parking-lot checks", "Tersimpan offline untuk cek instan dari parkiran"),
            gradientStart = p.mango,
            gradientEnd = p.mango,
            rightStat = "${weeklySlots.size}",
            rightStatLabel = tr("Sessions / Wk", "Sesi / Minggu"),
            bottomContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { onNavigateSub(SubScreen.Exams) },
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = p.surface,
                            contentColor = p.ink
                        )
                    ) {
                        Text(tr("View Exam Schedule (UTS/UAS)", "Lihat Jadwal Ujian (UTS/UAS)"), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        )

        Spacer(Modifier.height(8.dp))

        SorbetSegmentTabs(
            tabs = dayLabels,
            selectedIndex = selectedDay,
            onSelect = { selectedDay = it }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (shownSlots.isEmpty()) {
                item {
                    val hasMonday = weeklySlots.any { it.dayIndex == 1 }
                    SorbetEmptyState(
                        icon = DinusIcons.Calendar,
                        accent = p.mango,
                        softBg = p.mangoSoft,
                        title = if (selectedDay == 0) tr("No classes today", "Tidak ada kelas hari ini")
                            else "${dayLabels[selectedDay]} ${tr("is free", "kosong")}",
                        subtitle = if (selectedDay == 0 && hasMonday) {
                            tr(
                                "Today has no classes scheduled. Tap below to inspect Monday's schedule.",
                                "Hari ini libur atau tidak ada perkuliahan. Ketuk tombol di bawah untuk lihat jadwal hari Senin."
                            )
                        } else {
                            tr("No classes scheduled for the selected day.", "Tidak ada mata kuliah terjadwal pada hari yang dipilih.")
                        },
                        actionLabel = if (selectedDay == 0 && hasMonday) tr("View Monday Schedule", "Lihat Jadwal Senin")
                            else tr("Reload Schedule", "Muat Ulang Jadwal"),
                        onAction = {
                            if (selectedDay == 0 && hasMonday) selectedDay = 1
                            else repo.refreshCoreDataAsync()
                        }
                    )
                }
            } else {
                items(shownSlots.size) { idx ->
                    val slot = shownSlots[idx]
                    val (cStart, cEnd) = p.cycleAccent(idx + selectedDay)
                    val meta = listOfNotNull(
                        "${slot.startTime} – ${slot.endTime}",
                        slot.lecturer.takeIf { it.isNotBlank() },
                        slot.kdmk.takeIf { it.isNotBlank() }
                    ).joinToString(" · ")
                    val isSlotOpen = selectedDay == 0 &&
                        slot.dayIndex == 0 &&
                        slot.isOpenClass &&
                        !slot.isAttended &&
                        !slot.idMaster.isNullOrBlank()
                    val badge = when {
                        slot.isAttended -> tr("Present", "Hadir")
                        isSlotOpen -> tr("Attend", "Bisa Absen")
                        slot.sks > 0 -> "${slot.sks} SKS"
                        else -> slot.klpk
                    }
                    ColorfulCourseCard(
                        colorStart = cStart,
                        colorEnd = cEnd,
                        badgeCode = slot.room.ifBlank { slot.klpk },
                        title = slot.courseName,
                        subtitle = meta,
                        trailingPill = badge,
                        isHighlighted = isSlotOpen
                    )
                }
            }
        }
    }
}
