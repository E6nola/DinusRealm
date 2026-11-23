package id.dinusrealm.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import id.dinusrealm.app.core.i18n.LocalAppLanguage
import id.dinusrealm.app.core.i18n.tr
import id.dinusrealm.app.core.model.StudentProfile
import id.dinusrealm.app.core.network.ApiResult
import id.dinusrealm.app.core.repo.DinusRepository
import id.dinusrealm.app.ui.components.CurvedHeroHeader
import id.dinusrealm.app.ui.components.FruitBadge
import id.dinusrealm.app.ui.components.KeyValueRow
import id.dinusrealm.app.ui.components.SorbetCard
import id.dinusrealm.app.ui.components.SorbetLoadingBox
import id.dinusrealm.app.ui.components.SorbetMessageStrip
import id.dinusrealm.app.ui.components.SorbetSegmentTabs
import id.dinusrealm.app.ui.theme.DinusIcons
import id.dinusrealm.app.ui.theme.DinusTheme
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    repo: DinusRepository,
    onBack: () -> Unit
) {
    val p = DinusTheme.palette
    val lang = LocalAppLanguage.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tabIdx by remember { mutableIntStateOf(0) }
    var profile by remember { mutableStateOf<StudentProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var isErrorFeedback by remember { mutableStateOf(false) }

    var isEditing by remember { mutableStateOf(false) }
    var hpMhs by remember { mutableStateOf("") }
    var emailLain by remember { mutableStateOf("") }
    var alamatSmg by remember { mutableStateOf("") }
    var urlBlog by remember { mutableStateOf("") }

    var oldPw by remember { mutableStateOf("") }
    var newPw by remember { mutableStateOf("") }
    var confirmPw by remember { mutableStateOf("") }

    fun load() {
        isLoading = true
        scope.launch {
            val res = repo.api.getProfile()
            if (res is ApiResult.Success) {
                profile = res.data
                hpMhs = res.data.hpMhs
                emailLain = res.data.emailLain
                alamatSmg = res.data.alamatSmg
                urlBlog = res.data.urlBlog
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    val pr = profile ?: StudentProfile(nim = repo.vault.getNim().orEmpty())

    Column(modifier = Modifier.fillMaxSize()) {
        CurvedHeroHeader(
            title = pr.name.ifBlank { tr("Student Profile", "Profil Mahasiswa") },
            subtitle = listOfNotNull(pr.nim.takeIf { it.isNotBlank() }, pr.academicStatus.takeIf { it.isNotBlank() }).joinToString(" · "),
            gradientStart = p.cobalt,
            gradientEnd = p.berry,
            onBack = onBack,
            rightStat = pr.kelas.ifBlank { null },
            rightStatLabel = if (pr.kelas.isNotBlank()) tr("Class", "Kelas") else null
        )

        Spacer(Modifier.height(6.dp))
        SorbetSegmentTabs(
            listOf(tr("Biodata", "Biodata"), tr("Password", "Ubah Sandi"), tr("Help", "Bantuan")),
            tabIdx,
            { tabIdx = it },
            accent = p.cobalt
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
                        accent = if (isErrorFeedback) p.strawberry else p.mint,
                        softBg = if (isErrorFeedback) p.strawberrySoft else p.mintSoft,
                        onDismiss = { feedback = null }
                    )
                }
            }

            when (tabIdx) {
                0 -> {
                    if (isLoading && profile == null) {
                        item { SorbetLoadingBox(tr("Loading biodata...", "Memuat biodata..."), p.cobalt) }
                    } else {
                        if (pr.dosenWaliName.isNotBlank()) {
                            item {
                                SorbetCard(containerColor = p.surfaceVariant, borderColor = p.border) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(tr("Academic Advisor", "Dosen Wali"), style = MaterialTheme.typography.titleMedium, color = p.ink)
                                        FruitBadge(tr("Active", "Aktif"), p.onLime, p.lime)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(pr.dosenWaliName, style = MaterialTheme.typography.bodyLarge, color = p.ink)
                                    if (pr.dosenWaliEmail.isNotBlank()) {
                                        Text(pr.dosenWaliEmail, style = MaterialTheme.typography.labelMedium, color = p.inkMuted)
                                    }
                                    val waNum = pr.dosenWaliWa.ifBlank { pr.dosenWaliPhone }.replace(Regex("[^0-9]"), "")
                                    if (waNum.isNotBlank()) {
                                        val intl = if (waNum.startsWith("0")) "62${waNum.drop(1)}" else waNum
                                        Spacer(Modifier.height(8.dp))
                                        FilledTonalButton(
                                            onClick = {
                                                runCatching {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send/?phone=$intl")))
                                                }
                                            },
                                            shape = RoundedCornerShape(100.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = p.surface, contentColor = p.mint)
                                        ) {
                                            Text(tr("Chat via WhatsApp", "Hubungi via WhatsApp"))
                                        }
                                    }
                                }
                            }
                        }

                        if (isEditing) {
                            item {
                                SorbetCard {
                                    Text(tr("Update Contact Details", "Perbarui Data Kontak"), style = MaterialTheme.typography.titleMedium, color = p.ink)
                                    Spacer(Modifier.height(10.dp))
                                    OutlinedTextField(value = hpMhs, onValueChange = { hpMhs = it }, label = { Text(tr("Phone Number", "No. HP Mahasiswa")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(value = emailLain, onValueChange = { emailLain = it }, label = { Text(tr("Alternate Email", "Email Cadangan")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(value = alamatSmg, onValueChange = { alamatSmg = it }, label = { Text(tr("Semarang Address", "Alamat Semarang")) }, modifier = Modifier.fillMaxWidth())
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(value = urlBlog, onValueChange = { urlBlog = it }, label = { Text(tr("Student Blog URL", "URL Blog Mahasiswa")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    Spacer(Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { isEditing = false },
                                            shape = RoundedCornerShape(100.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = p.surfaceVariant, contentColor = p.ink),
                                            modifier = Modifier.weight(1f)
                                        ) { Text(tr("Cancel", "Batal")) }
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    val res = repo.api.updateProfile(
                                                        mapOf(
                                                            "hp_mhs" to hpMhs,
                                                            "email_lain" to emailLain,
                                                            "alamat_smg" to alamatSmg,
                                                            "url_blog" to urlBlog
                                                        )
                                                    )
                                                    isErrorFeedback = res is ApiResult.Error
                                                    feedback = when (res) {
                                                        is ApiResult.Success -> res.data
                                                        is ApiResult.Error -> res.message
                                                    }
                                                    isEditing = false
                                                    load()
                                                }
                                            },
                                            shape = RoundedCornerShape(100.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = p.lime, contentColor = p.onLime),
                                            modifier = Modifier.weight(1f)
                                        ) { Text(tr("Save", "Simpan")) }
                                    }
                                }
                            }
                        } else {
                            item {
                                SorbetCard {
                                    Text(tr("Contact & Personal", "Kontak & Pribadi"), style = MaterialTheme.typography.titleMedium, color = p.ink)
                                    Spacer(Modifier.height(8.dp))
                                    KeyValueRow(tr("Campus Email", "Email Kampus"), pr.emailMhs)
                                    KeyValueRow(tr("Alternate Email", "Email Cadangan"), pr.emailLain)
                                    KeyValueRow(tr("Phone", "No. HP"), pr.hpMhs)
                                    KeyValueRow(tr("Birth Place & Date", "Tempat, Tgl Lahir"), "${pr.tmpLahir} ${pr.tglLahir}".trim())
                                    KeyValueRow(tr("Religion", "Agama"), pr.agama)
                                    KeyValueRow(tr("Blood Type", "Gol. Darah"), pr.golDarah)
                                    KeyValueRow(tr("Blog", "Blog"), pr.urlBlog)
                                }
                            }
                            item {
                                SorbetCard {
                                    Text(tr("Address & Parents", "Alamat & Orang Tua"), style = MaterialTheme.typography.titleMedium, color = p.ink)
                                    Spacer(Modifier.height(8.dp))
                                    KeyValueRow(tr("Semarang Address", "Alamat Semarang"), pr.alamatSmg)
                                    KeyValueRow(tr("Hometown Address", "Alamat Asal"), pr.alamatAsal)
                                    KeyValueRow(tr("District", "Kecamatan"), pr.nmKecamatan)
                                    KeyValueRow(tr("Parents' Address", "Alamat Ortu"), pr.alamatOrtu)
                                    KeyValueRow(tr("Father's Name", "Nama Ayah"), pr.nmAyah)
                                    KeyValueRow(tr("Mother's Name", "Nama Ibu"), pr.nmIbuKandung)
                                    KeyValueRow(tr("Parents' Phone", "No. Telp Ortu"), pr.telpOrtu)
                                }
                            }
                            item {
                                Button(
                                    onClick = { isEditing = true },
                                    shape = RoundedCornerShape(100.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = p.lime, contentColor = p.onLime),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text(tr("Edit Contact & Address", "Edit Kontak & Alamat"))
                                }
                            }
                        }
                    }
                }

                1 -> {
                    item {
                        SorbetCard {
                            Text(tr("Change SiAdin Password", "Ubah Kata Sandi SiAdin"), style = MaterialTheme.typography.titleMedium, color = p.ink)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                tr(
                                    "Your new password syncs automatically to the local encrypted vault for silent auto-relogin.",
                                    "Sandi baru otomatis disinkronkan ke vault terenkripsi lokal agar silent auto-relogin tetap berjalan."
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = p.inkMuted
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = oldPw,
                                onValueChange = { oldPw = it },
                                label = { Text(tr("Current password", "Kata sandi lama")) },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newPw,
                                onValueChange = { newPw = it },
                                label = { Text(tr("New password", "Kata sandi baru")) },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = confirmPw,
                                onValueChange = { confirmPw = it },
                                label = { Text(tr("Confirm new password", "Konfirmasi kata sandi baru")) },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    if (oldPw.isEmpty() || newPw.isEmpty()) {
                                        isErrorFeedback = true
                                        feedback = tr(lang, "All password fields are required.", "Semua kolom sandi wajib diisi.")
                                    } else if (newPw != confirmPw) {
                                        isErrorFeedback = true
                                        feedback = tr(lang, "New passwords do not match!", "Kata sandi baru tidak cocok!")
                                    } else {
                                        scope.launch {
                                            val res = repo.api.changePassword(oldPw, newPw, confirmPw)
                                            when (res) {
                                                is ApiResult.Success -> {
                                                    repo.vault.getNim()?.let { repo.vault.saveCredentials(it, newPw) }
                                                    isErrorFeedback = false
                                                    feedback = res.data
                                                    oldPw = ""; newPw = ""; confirmPw = ""
                                                }
                                                is ApiResult.Error -> {
                                                    isErrorFeedback = true
                                                    feedback = res.message
                                                }
                                            }
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(100.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = p.lime, contentColor = p.onLime),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text(tr("Save New Password", "Simpan Kata Sandi Baru"))
                            }
                        }
                    }
                }

                2 -> {
                    item {
                        MenuNavCard(
                            tr("UDINUS FAQ & Help Center", "FAQ & Pusat Bantuan UDINUS"),
                            tr("Open faq.dinus.ac.id", "Buka faq.dinus.ac.id"),
                            DinusIcons.Academic, p.ink, p.surfaceVariant
                        ) {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://faq.dinus.ac.id/"))) }
                        }
                    }
                    item {
                        MenuNavCard(
                            tr("UDINUS WhatsApp Helpdesk", "Helpdesk WhatsApp UDINUS"),
                            tr("Official chat +62 811-2685-577", "Chat resmi +62 811-2685-577"),
                            DinusIcons.Bell, p.mint, p.mintSoft
                        ) {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send/?phone=628112685577"))) }
                        }
                    }
                    item {
                        MenuNavCard(
                            tr("UDINUS Privacy Policy", "Kebijakan Privasi UDINUS"),
                            "dinus.ac.id/privacy-policy",
                            DinusIcons.Shield, p.ink, p.surfaceVariant
                        ) {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://dinus.ac.id/privacy-policy/"))) }
                        }
                    }
                }
            }
        }
    }
}
