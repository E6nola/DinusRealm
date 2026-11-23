package id.dinusrealm.app.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import id.dinusrealm.app.core.i18n.tr
import id.dinusrealm.app.core.model.AcademicAnnouncement
import id.dinusrealm.app.core.model.HeregistrasiInfo
import id.dinusrealm.app.core.model.QuestionnaireItem
import id.dinusrealm.app.core.model.QuestionnaireOverview
import id.dinusrealm.app.core.model.QuizGateStatus
import id.dinusrealm.app.core.network.ApiResult
import id.dinusrealm.app.core.repo.DinusRepository
import id.dinusrealm.app.ui.components.CurvedHeroHeader
import id.dinusrealm.app.ui.components.FruitBadge
import id.dinusrealm.app.ui.components.KeyValueRow
import id.dinusrealm.app.ui.components.SorbetCard
import id.dinusrealm.app.ui.components.SorbetEmptyState
import id.dinusrealm.app.ui.components.SorbetLoadingBox
import id.dinusrealm.app.ui.components.SorbetMessageStrip
import id.dinusrealm.app.ui.components.SorbetSegmentTabs
import id.dinusrealm.app.ui.components.renderHtmlPlain
import id.dinusrealm.app.ui.theme.DinusIcons
import id.dinusrealm.app.ui.theme.DinusTheme
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun HeregistrasiScreen(
    repo: DinusRepository,
    onBack: () -> Unit
) {
    val p = DinusTheme.palette
    val scope = rememberCoroutineScope()
    var info by remember { mutableStateOf<HeregistrasiInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var feedback by remember { mutableStateOf<String?>(null) }
    val editedFields = remember { mutableStateMapOf<String, String>() }

    fun load() {
        isLoading = true
        scope.launch {
            val res = repo.api.getHeregistrasi()
            when (res) {
                is ApiResult.Success -> {
                    info = res.data
                    editedFields.clear()
                    res.data.fields.forEach { (k, v) -> editedFields[k] = v }
                }
                is ApiResult.Error -> feedback = res.message
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(modifier = Modifier.fillMaxSize()) {
        CurvedHeroHeader(
            title = tr("Re-Registration & Docs", "Heregistrasi & Berkas"),
            subtitle = tr("Semester re-registration & supporting documents", "Daftar ulang semester & berkas pendukung"),
            gradientStart = p.mint,
            gradientEnd = p.sky,
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                item { SorbetLoadingBox(tr("Loading re-registration data...", "Memuat data heregistrasi..."), p.mint) }
            } else {
                val data = info
                if (data == null) {
                    item {
                        SorbetEmptyState(
                            icon = DinusIcons.Shield,
                            accent = p.mint,
                            softBg = p.mintSoft,
                            title = tr("Re-registration data unavailable", "Data heregistrasi belum tersedia"),
                            subtitle = tr("Could not fetch the re-registration form right now.", "Tidak dapat mengambil formulir heregistrasi saat ini."),
                            actionLabel = tr("Reload", "Muat Ulang"),
                            onAction = { load() }
                        )
                    }
                } else {
                    item {
                        SorbetCard(containerColor = p.mintSoft, borderColor = p.mint.copy(alpha = 0.25f)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(tr("Registration Period", "Periode Heregistrasi"), style = MaterialTheme.typography.titleMedium, color = p.ink)
                                FruitBadge(
                                    text = if (data.isShow) tr("Active Period", "Periode Aktif") else tr("Closed Period", "Di Luar Periode"),
                                    accent = if (data.isShow) p.mint else p.inkMuted,
                                    softBg = p.surface
                                )
                            }
                            if (data.academicStatus.isNotBlank() || data.wave.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                KeyValueRow(tr("Status", "Status"), data.academicStatus)
                                KeyValueRow(tr("Wave", "Gelombang"), data.wave)
                            }
                        }
                    }

                    if (data.docs.isNotEmpty()) {
                        item {
                            SorbetCard {
                                Text(tr("Supporting Documents", "Berkas Pendukung"), style = MaterialTheme.typography.titleMedium, color = p.ink)
                                Spacer(Modifier.height(8.dp))
                                data.docs.forEach { doc ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(doc.label, style = MaterialTheme.typography.bodyMedium, color = p.ink)
                                        FruitBadge(
                                            text = if (doc.uploaded) tr("Uploaded", "Terunggah") else tr("Missing", "Belum"),
                                            accent = if (doc.uploaded) p.mint else p.strawberry,
                                            softBg = if (doc.uploaded) p.mintSoft else p.strawberrySoft
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (data.fields.isNotEmpty()) {
                        item {
                            SorbetCard {
                                Text(tr("Form Summary", "Ringkasan Data"), style = MaterialTheme.typography.titleMedium, color = p.ink)
                                Spacer(Modifier.height(8.dp))
                                data.fields.entries.take(12).forEach { (k, v) ->
                                    KeyValueRow(k.replace('_', ' '), v)
                                }
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val res = repo.api.submitHeregistrasi(editedFields.toMap())
                                            feedback = when (res) {
                                                is ApiResult.Success -> res.data
                                                is ApiResult.Error -> res.message
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(100.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = p.lime, contentColor = p.onLime),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(tr("Save Re-Registration", "Simpan Heregistrasi"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuestionnaireScreen(
    repo: DinusRepository,
    onBack: () -> Unit
) {
    val p = DinusTheme.palette
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tabIdx by remember { mutableIntStateOf(0) }
    var utsOverview by remember { mutableStateOf(QuestionnaireOverview()) }
    var uasOverview by remember { mutableStateOf(QuestionnaireOverview()) }
    var quizGate by remember { mutableStateOf<QuizGateStatus?>(null) }
    var activeForm by remember { mutableStateOf<QuestionnaireItem?>(null) }
    val answers = remember { mutableStateMapOf<String, Int>() }
    var feedback by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    fun load() {
        isLoading = true
        scope.launch {
            coroutineScope {
                val q1 = async { repo.api.getQuestionnaires("UTS") }
                val q2 = async { repo.api.getQuestionnaires("UAS") }
                val gate = async { repo.api.checkFilledQuiz() }
                (q1.await() as? ApiResult.Success)?.let { utsOverview = it.data }
                (q2.await() as? ApiResult.Success)?.let { uasOverview = it.data }
                (gate.await() as? ApiResult.Success)?.let { quizGate = it.data }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    val activeType = if (tabIdx == 0) "UTS" else "UAS"
    val activeOverview = if (tabIdx == 0) utsOverview else uasOverview
    val shown = activeOverview.items

    Column(modifier = Modifier.fillMaxSize()) {
        CurvedHeroHeader(
            title = activeForm?.courseName?.ifBlank { activeForm?.title } ?: tr("Questionnaires & Quiz", "Kuisioner & Kuis"),
            subtitle = if (activeForm != null) {
                tr("Rating scale: 1 (Poor) – 4 (Excellent)", "Skala penilaian 1 (Kurang) – 4 (Sangat Baik)")
            } else {
                tr("Course & lecturer evaluations for UTS/UAS", "Evaluasi dosen UTS/UAS")
            },
            gradientStart = p.peach,
            gradientEnd = p.strawberry,
            onBack = {
                if (activeForm != null) activeForm = null else onBack()
            },
            rightStat = if (activeOverview.total > 0) "${activeOverview.filled}/${activeOverview.total}" else null,
            rightStatLabel = if (activeOverview.total > 0) tr("Completed", "Terisi") else null
        )

        Spacer(Modifier.height(6.dp))
        if (activeForm == null) {
            SorbetSegmentTabs(
                listOf(tr("UTS Survey", "Kuisioner UTS"), tr("UAS Survey", "Kuisioner UAS")),
                tabIdx,
                { tabIdx = it },
                accent = p.peach
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!feedback.isNullOrBlank()) {
                item {
                    SorbetMessageStrip(feedback!!, p.mint, p.mintSoft, onDismiss = { feedback = null })
                }
            }

            if (activeForm != null) {
                val form = activeForm!!
                if (form.questions.isEmpty()) {
                    item {
                        SorbetEmptyState(
                            icon = DinusIcons.Bell,
                            accent = p.mango,
                            softBg = p.mangoSoft,
                            title = tr("No questions in survey", "Daftar pernyataan kosong"),
                            subtitle = tr("This evaluation has no active items yet.", "Kuisioner ini belum memiliki butir pertanyaan aktif.")
                        )
                    }
                } else {
                    items(form.questions.size) { idx ->
                        val q = form.questions[idx]
                        val chosen = answers[q.id] ?: 4
                        SorbetCard {
                            Text("${idx + 1}. ${q.text}", style = MaterialTheme.typography.bodyMedium, color = p.ink)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                (1..4).forEach { score ->
                                    val selected = chosen == score
                                    Surface(
                                        onClick = { answers[q.id] = score },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(100.dp),
                                        color = if (selected) p.cardActive else p.surfaceVariant,
                                        border = BorderStroke(1.dp, if (selected && !p.isDark) p.cardActive else p.border)
                                    ) {
                                        Text(
                                            text = "$score",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (selected) p.onCardActive else p.inkMuted,
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Button(
                            onClick = {
                                scope.launch {
                                    val arr = JSONArray()
                                    form.questions.forEach { q ->
                                        arr.put(JSONObject().put("id", q.id).put("nilai", answers[q.id] ?: 4))
                                    }
                                    val payload = JSONObject().put("id", form.id).put("answers", arr)
                                    val res = repo.api.submitQuestionnaire(activeType, payload)
                                    feedback = when (res) {
                                        is ApiResult.Success -> res.data
                                        is ApiResult.Error -> res.message
                                    }
                                    activeForm = null
                                    load()
                                }
                            },
                            shape = RoundedCornerShape(100.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = p.lime, contentColor = p.onLime),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(tr("Submit Evaluation", "Kirim Kuisioner"))
                        }
                    }
                }
            } else {
                val gate = quizGate
                if (gate != null && gate.isShow && !gate.allFilled && gate.quizLink.isNotBlank()) {
                    item {
                        SorbetCard(containerColor = p.mangoSoft, borderColor = p.mango.copy(alpha = 0.25f)) {
                            Text(tr("Mandatory Campus Quiz Pending", "Kuis Wajib Kampus Belum Lengkap"), style = MaterialTheme.typography.titleMedium, color = p.ink)
                            Spacer(Modifier.height(6.dp))
                            FilledTonalButton(
                                onClick = {
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(gate.quizLink))) }
                                },
                                shape = RoundedCornerShape(100.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = p.surface, contentColor = p.mango)
                            ) {
                                Text(tr("Open Quiz Link", "Buka Tautan Kuis"))
                            }
                        }
                    }
                }

                if (isLoading) {
                    item { SorbetLoadingBox(tr("Loading questionnaires...", "Memuat kuisioner..."), p.peach) }
                } else if (shown.isEmpty()) {
                    item {
                        SorbetEmptyState(
                            icon = DinusIcons.CheckCircle,
                            accent = p.mint,
                            softBg = p.mintSoft,
                            title = if (activeOverview.allFilled && activeOverview.total > 0) {
                                tr(
                                    "$activeType Survey Complete (${activeOverview.filled}/${activeOverview.total})",
                                    "Kuisioner $activeType Lengkap (${activeOverview.filled}/${activeOverview.total})"
                                )
                            } else {
                                tr("No open questionnaires", "Tidak ada kuisioner terbuka")
                            },
                            subtitle = tr(
                                "All $activeType surveys have been completed or the period is closed.",
                                "Semua kuisioner $activeType sudah diisi atau periode belum dibuka."
                            ),
                            actionLabel = tr("Reload", "Muat Ulang"),
                            onAction = { load() }
                        )
                    }
                } else {
                    items(shown.size) { i ->
                        val item = shown[i]
                        SorbetCard(
                            onClick = if (!item.isFilled) {
                                {
                                    answers.clear()
                                    item.questions.forEach { answers[it.id] = 4 }
                                    activeForm = item
                                }
                            } else null
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(item.courseName.ifBlank { item.title }, style = MaterialTheme.typography.titleMedium, color = p.ink)
                                    if (item.lecturer.isNotBlank()) {
                                        Text(item.lecturer, style = MaterialTheme.typography.labelMedium, color = p.inkMuted)
                                    }
                                }
                                FruitBadge(
                                    text = if (item.isFilled) tr("Completed", "Sudah Diisi") else tr("Fill Now", "Isi Sekarang"),
                                    accent = if (item.isFilled) p.mint else p.ink,
                                    softBg = if (item.isFilled) p.mintSoft else p.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnnouncementsScreen(
    repo: DinusRepository,
    onBack: () -> Unit
) {
    val p = DinusTheme.palette
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<AcademicAnnouncement>>(emptyList()) }
    var selected by remember { mutableStateOf<AcademicAnnouncement?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    fun load() {
        isLoading = true
        scope.launch {
            val res = repo.api.getAnnouncements()
            if (res is ApiResult.Success) list = res.data
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(modifier = Modifier.fillMaxSize()) {
        CurvedHeroHeader(
            title = if (selected != null) tr("Announcement Detail", "Detail Pengumuman") else tr("Academic Notices", "Pengumuman Akademik"),
            subtitle = if (selected != null) selected!!.date else tr("Official announcements from UDINUS", "Informasi & berita resmi UDINUS"),
            gradientStart = p.berry,
            gradientEnd = p.cobalt,
            onBack = {
                if (selected != null) selected = null else onBack()
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (selected != null) {
                val item = selected!!
                item {
                    SorbetCard {
                        if (item.category.isNotBlank()) {
                            FruitBadge(item.category, p.ink, p.surfaceVariant)
                            Spacer(Modifier.height(8.dp))
                        }
                        Text(item.title, style = MaterialTheme.typography.titleLarge, color = p.ink)
                        if (item.date.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(item.date, style = MaterialTheme.typography.labelMedium, color = p.inkMuted)
                        }
                        Spacer(Modifier.height(12.dp))
                        val body = renderHtmlPlain(item.htmlBody.ifBlank { item.summary })
                        Text(body.ifBlank { tr("Loading announcement content...", "Memuat isi pengumuman...") }, style = MaterialTheme.typography.bodyMedium, color = p.ink)
                    }
                }
            } else if (isLoading) {
                item { SorbetLoadingBox(tr("Loading announcements...", "Memuat pengumuman..."), p.berry) }
            } else if (list.isEmpty()) {
                item {
                    SorbetEmptyState(
                        icon = DinusIcons.Bell,
                        accent = p.berry,
                        softBg = p.berrySoft,
                        title = tr("No announcements", "Belum ada pengumuman"),
                        subtitle = tr("Latest academic updates will appear here.", "Informasi akademik terbaru akan tampil di sini."),
                        actionLabel = tr("Reload", "Muat Ulang"),
                        onAction = { load() }
                    )
                }
            } else {
                items(list.size) { i ->
                    val item = list[i]
                    SorbetCard(onClick = {
                        selected = item
                        if (item.id.isNotBlank()) {
                            scope.launch {
                                val det = repo.api.getAnnouncementDetail(item.id)
                                if (det is ApiResult.Success) selected = det.data
                            }
                        }
                    }) {
                        if (item.date.isNotBlank()) {
                            Text(item.date, style = MaterialTheme.typography.labelSmall, color = p.inkMuted)
                            Spacer(Modifier.height(4.dp))
                        }
                        Text(item.title, style = MaterialTheme.typography.titleMedium, color = p.ink)
                        val preview = renderHtmlPlain(item.summary)
                        if (preview.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(preview, style = MaterialTheme.typography.bodyMedium, color = p.inkMuted, maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}
