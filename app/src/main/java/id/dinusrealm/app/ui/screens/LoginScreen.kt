package id.dinusrealm.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import id.dinusrealm.app.core.i18n.LocalAppLanguage
import id.dinusrealm.app.core.i18n.tr
import id.dinusrealm.app.core.network.ApiResult
import id.dinusrealm.app.core.repo.DinusRepository
import id.dinusrealm.app.ui.components.CurvedHeroHeader
import id.dinusrealm.app.ui.components.FruitBadge
import id.dinusrealm.app.ui.components.SorbetCard
import id.dinusrealm.app.ui.components.SorbetMessageStrip
import id.dinusrealm.app.ui.theme.DinusIcons
import id.dinusrealm.app.ui.theme.DinusTheme
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    repo: DinusRepository,
    sessionExpiredNotice: Boolean
) {
    val p = DinusTheme.palette
    val lang = LocalAppLanguage.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var nim by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val expiredDefaultMsg = tr(
        "SiAdin password changed or session needs a fresh sign-in.",
        "Kata sandi SiAdin berubah atau sesi perlu login ulang."
    )
    var errorMsg by remember(sessionExpiredNotice, lang) {
        mutableStateOf(if (sessionExpiredNotice) expiredDefaultMsg else null)
    }

    val emptyFieldMsg = tr("Student ID (NIM) and password are required.", "NIM dan kata sandi wajib diisi.")

    val performLogin: () -> Unit = {
        val cleanNim = nim.trim()
        if (cleanNim.isEmpty() || password.isEmpty()) {
            errorMsg = emptyFieldMsg
        } else if (!isLoading) {
            isLoading = true
            errorMsg = null
            scope.launch {
                val res = repo.login(cleanNim, password)
                isLoading = false
                if (res is ApiResult.Error) {
                    errorMsg = res.message
                }
            }
        }
    }

    Scaffold(containerColor = p.background) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = inner.calculateBottomPadding())
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            CurvedHeroHeader(
                title = tr("Welcome to DinusRealm", "Selamat Datang di DinusRealm"),
                subtitle = tr(
                    "Sign in once — your schedule & attendance stay synced.",
                    "Satu kali masuk, jadwal & absen otomatis jalan di HP kamu."
                ),
                gradientStart = p.cobalt,
                gradientEnd = p.sky,
                bottomContent = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FruitBadge("Auto-Relogin", p.ink, p.surface)
                        FruitBadge(tr("T-30m Alert", "Notif T-30m"), p.ink, p.surface)
                        FruitBadge(tr("1-Tap Attend", "1-Tap Absen"), p.onLime, p.lime)
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!errorMsg.isNullOrBlank()) {
                    SorbetMessageStrip(
                        message = errorMsg!!,
                        accent = p.strawberry,
                        softBg = p.strawberrySoft,
                        onDismiss = { errorMsg = null }
                    )
                }

                SorbetCard(cornerRadius = 24.dp) {
                    Text(tr("Sign In to SiAdin", "Masuk Akun SiAdin"), style = MaterialTheme.typography.titleLarge, color = p.ink)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tr("Use your UDINUS student NIM & password.", "Gunakan NIM & kata sandi mahasiswa UDINUS."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = p.inkMuted
                    )
                    Spacer(Modifier.height(16.dp))

                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (p.isDark) p.lime else p.cardActive,
                        unfocusedBorderColor = p.border,
                        focusedContainerColor = p.surfaceVariant.copy(alpha = 0.45f),
                        unfocusedContainerColor = p.surfaceVariant.copy(alpha = 0.45f),
                        focusedTextColor = p.ink,
                        unfocusedTextColor = p.ink,
                        focusedLabelColor = p.ink
                    )

                    OutlinedTextField(
                        value = nim,
                        onValueChange = { nim = it },
                        label = { Text(tr("Student ID / NIM (e.g. A11.20XX.00001)", "NIM (mis. A11.20XX.00001)")) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(tr("Password", "Kata Sandi")) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { performLogin() }),
                        trailingIcon = {
                            TextButton(onClick = { showPassword = !showPassword }) {
                                Text(
                                    if (showPassword) tr("Hide", "Tutup") else tr("Show", "Lihat"),
                                    color = p.ink,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(18.dp))

                    Button(
                        onClick = performLogin,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = p.lime, contentColor = p.onLime),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = p.onLime
                            )
                        } else {
                            Text(tr("Sign In", "Masuk Sekarang"), style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        TextButton(onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://accounts.dinus.ac.id/"))
                                )
                            }
                        }) {
                            Text(tr("Forgot Password?", "Lupa Kata Sandi?"), color = p.inkMuted, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                SorbetCard(
                    containerColor = p.mintSoft,
                    borderColor = p.mint.copy(alpha = 0.25f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(DinusIcons.Shield, contentDescription = null, tint = p.mint, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = tr(
                                "100% On-Device Privacy: NIM & password encrypted with AES-256 in Android Keystore.",
                                "Privasi 100% On-Device: NIM & sandi terenkripsi AES-256 di Android Keystore HP kamu."
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = p.ink
                        )
                    }
                }
            }
        }
    }
}
