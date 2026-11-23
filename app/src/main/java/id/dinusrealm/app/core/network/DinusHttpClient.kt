package id.dinusrealm.app.core.network

import id.dinusrealm.app.core.crypto.AuthStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(
        val message: String,
        val httpCode: Int = 0,
        val sessionInvalidated: Boolean = false
    ) : ApiResult<Nothing>()
}

/**
 * Pure-Kotlin HTTP client with Mutex-synchronized Silent Auto-Relogin.
 *
 * When a request receives HTTP 401 (or JSON body code == 401), this client:
 * 1. Serializes concurrent refresh attempts behind a Mutex so only ONE refresh/login happens.
 * 2. Tries `POST siadin/refresh-token` if a refresh_token is cached.
 * 3. Falls back to `POST siadin/login` using encrypted stored NIM & password.
 * 4. Retries the original request once with the fresh Bearer token.
 * 5. Only surfaces `sessionInvalidated = true` if campus credentials themselves are rejected.
 */
class DinusHttpClient(
    private val transport: HttpTransport,
    private val authStorage: AuthStorage,
    private val siadinBaseUrl: String = SIADIN_BASE_URL,
    private val rootApiBaseUrl: String = ROOT_API_BASE_URL,
    private val onSessionInvalidated: () -> Unit = {}
) {
    private val authMutex = Mutex()
    private val transportSemaphore = Semaphore(permits = 2)

    private suspend fun executeThrottled(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?
    ): HttpResponse = transportSemaphore.withPermit {
        transport.execute(method, url, headers, body)
    }

    suspend fun login(nim: String, password: String): ApiResult<String> = withContext(Dispatchers.IO) {
        authMutex.withLock {
            performLoginInternal(nim.trim(), password, persistCredentials = true)
        }
    }

    suspend fun getSiadin(path: String, requiresAuth: Boolean = true): ApiResult<JSONObject> =
        request("GET", siadinBaseUrl + path.trimStart('/'), null, requiresAuth)

    suspend fun postSiadin(path: String, payload: JSONObject, requiresAuth: Boolean = true): ApiResult<JSONObject> =
        request("POST", siadinBaseUrl + path.trimStart('/'), payload.toString(), requiresAuth)

    suspend fun getRoot(path: String, requiresAuth: Boolean = true): ApiResult<JSONObject> =
        request("GET", rootApiBaseUrl + path.trimStart('/'), null, requiresAuth)

    suspend fun postRoot(path: String, payload: JSONObject, requiresAuth: Boolean = true): ApiResult<JSONObject> =
        request("POST", rootApiBaseUrl + path.trimStart('/'), payload.toString(), requiresAuth)

    private suspend fun request(
        method: String,
        url: String,
        body: String?,
        requiresAuth: Boolean
    ): ApiResult<JSONObject> = withContext(Dispatchers.IO) {
        var token = if (requiresAuth) authStorage.getAccessToken() else null

        if (requiresAuth && token.isNullOrBlank()) {
            when (val relogin = refreshOrRelogin(failedToken = null)) {
                is ApiResult.Success -> token = relogin.data
                is ApiResult.Error -> return@withContext relogin
            }
        }

        val firstResp = runCatching {
            val headers = buildHeaders(token)
            executeThrottled(method, url, headers, body)
        }.getOrElse { ex ->
            return@withContext ApiResult.Error("Gagal terhubung ke server: ${ex.message ?: "Timeout"}")
        }

        if (!requiresAuth || !isUnauthorized(firstResp)) {
            return@withContext parseEnvelope(firstResp)
        }

        // Token expired / rejected -> refresh or silent re-login once
        val freshToken = when (val refresh = refreshOrRelogin(failedToken = token)) {
            is ApiResult.Success -> refresh.data
            is ApiResult.Error -> return@withContext refresh
        }

        val retryResp = runCatching {
            executeThrottled(method, url, buildHeaders(freshToken), body)
        }.getOrElse { ex ->
            return@withContext ApiResult.Error("Gagal terhubung ke server: ${ex.message ?: "Timeout"}")
        }

        parseEnvelope(retryResp)
    }

    private suspend fun refreshOrRelogin(failedToken: String?): ApiResult<String> {
        return authMutex.withLock {
            // Another coroutine may have already refreshed the token while we waited for the Mutex.
            val currentToken = authStorage.getAccessToken()
            if (!currentToken.isNullOrBlank() && currentToken != failedToken) {
                return@withLock ApiResult.Success(currentToken)
            }

            val refreshToken = authStorage.getRefreshToken()
            if (!refreshToken.isNullOrBlank()) {
                val refreshBody = JSONObject().put("refresh_token", refreshToken).toString()
                val refreshResp = runCatching {
                    executeThrottled(
                        "POST",
                        "${siadinBaseUrl}refresh-token",
                        buildHeaders(currentToken),
                        refreshBody
                    )
                }.getOrNull()

                if (refreshResp != null && refreshResp.statusCode in 200..299) {
                    val parsed = extractTokens(refreshResp.body)
                    if (parsed != null) {
                        authStorage.saveTokens(parsed.first, parsed.second, parsed.third)
                        return@withLock ApiResult.Success(parsed.first)
                    }
                }
            }

            val nim = authStorage.getNim()
            val password = authStorage.getPassword()
            if (nim.isNullOrBlank() || password.isNullOrBlank()) {
                onSessionInvalidated()
                return@withLock ApiResult.Error(
                    message = "Sesi berakhir. Silakan login kembali.",
                    httpCode = 401,
                    sessionInvalidated = true
                )
            }

            performLoginInternal(nim, password, persistCredentials = false)
        }
    }

    private suspend fun performLoginInternal(
        nim: String,
        password: String,
        persistCredentials: Boolean
    ): ApiResult<String> {
        val payload = JSONObject()
            .put("username", nim)
            .put("password", password)
            .toString()

        val resp = runCatching {
            executeThrottled("POST", "${siadinBaseUrl}login", emptyMap(), payload)
        }.getOrElse { ex ->
            return ApiResult.Error("Gagal terhubung ke server: ${ex.message ?: "Timeout"}")
        }

        val json = runCatching { JSONObject(resp.body) }.getOrNull()
        val apiCode = json?.optInt("code", resp.statusCode) ?: resp.statusCode

        if (resp.statusCode in 200..299 && apiCode in 200..299) {
            val tokens = extractTokens(resp.body)
            if (tokens != null) {
                if (persistCredentials) {
                    authStorage.saveCredentials(nim, password)
                }
                authStorage.saveTokens(tokens.first, tokens.second, tokens.third)
                return ApiResult.Success(tokens.first)
            }
        }

        val msg = json?.optString("message")?.takeIf { it.isNotBlank() }
            ?: "Login gagal (HTTP ${resp.statusCode})"
        val invalidCreds = resp.statusCode == 401 || resp.statusCode == 403 || apiCode == 401 || apiCode == 403
        if (invalidCreds && !persistCredentials) {
            // Stored password no longer matches campus account; clear only when explicitly wrong
            onSessionInvalidated()
        }
        return ApiResult.Error(message = msg, httpCode = resp.statusCode, sessionInvalidated = invalidCreds && !persistCredentials)
    }

    private fun buildHeaders(token: String?): Map<String, String> =
        if (token.isNullOrBlank()) emptyMap() else mapOf("Authorization" to "Bearer $token")

    private fun isUnauthorized(resp: HttpResponse): Boolean {
        if (resp.statusCode == 401) return true
        val json = runCatching { JSONObject(resp.body) }.getOrNull() ?: return false
        return json.optInt("code", 200) == 401
    }

    private fun parseEnvelope(resp: HttpResponse): ApiResult<JSONObject> {
        val json = runCatching { JSONObject(resp.body) }.getOrElse {
            return if (resp.statusCode in 200..299) {
                ApiResult.Error("Format respons server tidak valid", resp.statusCode)
            } else {
                ApiResult.Error("Server error (HTTP ${resp.statusCode})", resp.statusCode)
            }
        }
        val code = json.optInt("code", resp.statusCode)
        if (resp.statusCode in 200..299 && (code == 0 || code in 200..299)) {
            return ApiResult.Success(json)
        }
        val msg = json.optString("message").takeIf { it.isNotBlank() }
            ?: json.optString("msg").takeIf { it.isNotBlank() }
            ?: "Permintaan gagal (HTTP ${resp.statusCode})"
        return ApiResult.Error(msg, resp.statusCode)
    }

    private fun extractTokens(body: String): Triple<String, String?, Long>? {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val data = root.optJSONObject("data") ?: root
        val access = data.optString("access_token").takeIf { it.isNotBlank() }
            ?: data.optString("token").takeIf { it.isNotBlank() }
            ?: return null
        val refresh = data.optString("refresh_token").takeIf { it.isNotBlank() }
            ?: root.optString("refresh_token").takeIf { it.isNotBlank() }
        val expiredAt = data.optLong("expired_at", 0L)
        return Triple(access, refresh, expiredAt)
    }

    companion object {
        const val ROOT_API_BASE_URL = "https://api.dinus.ac.id/api/v1/"
        const val SIADIN_BASE_URL = "https://api.dinus.ac.id/api/v1/siadin/"
    }
}
