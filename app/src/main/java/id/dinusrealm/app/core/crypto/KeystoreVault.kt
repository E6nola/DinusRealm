package id.dinusrealm.app.core.crypto

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import id.dinusrealm.app.core.model.AppLanguage
import id.dinusrealm.app.core.model.AppSettings
import id.dinusrealm.app.core.model.ThemeMode
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Abstraction for credential & token storage so network logic is unit-testable on JVM.
 */
interface AuthStorage {
    fun getNim(): String?
    fun getPassword(): String?
    fun saveCredentials(nim: String, password: String)
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun getExpiredAt(): Long
    fun saveTokens(accessToken: String, refreshToken: String?, expiredAt: Long)
    fun clearSession()
}

/**
 * Zero-dependency credential and token vault backed by AndroidKeyStore (AES-256-GCM).
 */
class KeystoreVault(context: Context) : AuthStorage {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getNim(): String? = decrypt(prefs.getString(KEY_NIM, null))

    override fun getPassword(): String? = decrypt(prefs.getString(KEY_PASSWORD, null))

    override fun saveCredentials(nim: String, password: String) {
        prefs.edit()
            .putString(KEY_NIM, encrypt(nim))
            .putString(KEY_PASSWORD, encrypt(password))
            .apply()
    }

    override fun getAccessToken(): String? = decrypt(prefs.getString(KEY_ACCESS_TOKEN, null))

    override fun getRefreshToken(): String? = decrypt(prefs.getString(KEY_REFRESH_TOKEN, null))

    override fun getExpiredAt(): Long = prefs.getLong(KEY_EXPIRED_AT, 0L)

    override fun saveTokens(accessToken: String, refreshToken: String?, expiredAt: Long) {
        val editor = prefs.edit()
            .putString(KEY_ACCESS_TOKEN, encrypt(accessToken))
            .putLong(KEY_EXPIRED_AT, expiredAt)
        if (!refreshToken.isNullOrBlank()) {
            editor.putString(KEY_REFRESH_TOKEN, encrypt(refreshToken))
        }
        editor.apply()
    }

    override fun clearSession() {
        prefs.edit()
            .remove(KEY_NIM)
            .remove(KEY_PASSWORD)
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRED_AT)
            .remove(KEY_NICKNAME)
            .apply()
    }

    fun hasCredentials(): Boolean = !getNim().isNullOrBlank() && !getPassword().isNullOrBlank()

    fun getSettings(): AppSettings {
        val modeRaw = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val mode = runCatching { ThemeMode.valueOf(modeRaw) }.getOrDefault(ThemeMode.SYSTEM)
        val langRaw = prefs.getString(KEY_LANGUAGE, AppLanguage.ENGLISH.name) ?: AppLanguage.ENGLISH.name
        val lang = runCatching { AppLanguage.valueOf(langRaw) }.getOrDefault(AppLanguage.ENGLISH)
        return AppSettings(
            themeMode = mode,
            language = lang,
            nickname = prefs.getString(KEY_NICKNAME, "") ?: "",
            classNotifEnabled = prefs.getBoolean(KEY_CLASS_NOTIF, true),
            attendanceWatcherEnabled = prefs.getBoolean(KEY_WATCHER, true),
            autoAbsenEnabled = prefs.getBoolean(KEY_AUTO_ABSEN, true),
            autoAbsenSecondsBeforeEnd = prefs.getInt(KEY_AUTO_ABSEN_SEC, 90),
            billingNotifEnabled = prefs.getBoolean(KEY_BILLING_NOTIF, true)
        )
    }

    fun updateSettings(settings: AppSettings) {
        prefs.edit()
            .putString(KEY_THEME_MODE, settings.themeMode.name)
            .putString(KEY_LANGUAGE, settings.language.name)
            .putString(KEY_NICKNAME, settings.nickname.trim())
            .putBoolean(KEY_CLASS_NOTIF, settings.classNotifEnabled)
            .putBoolean(KEY_WATCHER, settings.attendanceWatcherEnabled)
            .putBoolean(KEY_AUTO_ABSEN, settings.autoAbsenEnabled)
            .putInt(KEY_AUTO_ABSEN_SEC, settings.autoAbsenSecondsBeforeEnd.coerceIn(30, 180))
            .putBoolean(KEY_BILLING_NOTIF, settings.billingNotifEnabled)
            .apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(1 + iv.size + cipherBytes.size)
        combined[0] = iv.size.toByte()
        System.arraycopy(iv, 0, combined, 1, iv.size)
        System.arraycopy(cipherBytes, 0, combined, 1 + iv.size, cipherBytes.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String?): String? {
        if (encoded.isNullOrBlank()) return null
        return runCatching {
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            val ivLen = combined[0].toInt() and 0xFF
            val iv = combined.copyOfRange(1, 1 + ivLen)
            val cipherBytes = combined.copyOfRange(1 + ivLen, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        }.getOrNull()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "dinusrealm_vault_key_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val PREFS_NAME = "dinusrealm_secure_prefs"

        private const val KEY_NIM = "v_nim"
        private const val KEY_PASSWORD = "v_pw"
        private const val KEY_ACCESS_TOKEN = "v_at"
        private const val KEY_REFRESH_TOKEN = "v_rt"
        private const val KEY_EXPIRED_AT = "v_exp"

        private const val KEY_THEME_MODE = "s_theme"
        private const val KEY_LANGUAGE = "s_lang"
        private const val KEY_NICKNAME = "s_nickname"
        private const val KEY_CLASS_NOTIF = "s_class_notif"
        private const val KEY_WATCHER = "s_watcher"
        private const val KEY_AUTO_ABSEN = "s_auto_absen"
        private const val KEY_AUTO_ABSEN_SEC = "s_auto_absen_sec"
        private const val KEY_BILLING_NOTIF = "s_billing_notif"
    }
}
