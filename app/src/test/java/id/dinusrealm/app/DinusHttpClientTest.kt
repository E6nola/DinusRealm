package id.dinusrealm.app

import id.dinusrealm.app.core.crypto.AuthStorage
import id.dinusrealm.app.core.network.ApiResult
import id.dinusrealm.app.core.network.DinusHttpClient
import id.dinusrealm.app.core.network.HttpResponse
import id.dinusrealm.app.core.network.HttpTransport
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

private class FakeAuthStorage(
    private var nim: String? = "A11.2024.00001",
    private var password: String? = "secret123",
    private var accessToken: String? = "expired_token",
    private var refreshToken: String? = "bad_refresh"
) : AuthStorage {
    override fun getNim(): String? = nim
    override fun getPassword(): String? = password
    override fun saveCredentials(nim: String, password: String) {
        this.nim = nim
        this.password = password
    }
    override fun getAccessToken(): String? = accessToken
    override fun getRefreshToken(): String? = refreshToken
    override fun getExpiredAt(): Long = 0L
    override fun saveTokens(accessToken: String, refreshToken: String?, expiredAt: Long) {
        this.accessToken = accessToken
        if (!refreshToken.isNullOrBlank()) this.refreshToken = refreshToken
    }
    override fun clearSession() {
        nim = null; password = null; accessToken = null; refreshToken = null
    }
}

class DinusHttpClientTest {

    @Test
    fun `401 tries refresh-token then silently relogins and retries original request`() = runTest {
        val storage = FakeAuthStorage()
        val calls = mutableListOf<String>()

        val transport = object : HttpTransport {
            override fun execute(method: String, url: String, headers: Map<String, String>, jsonBody: String?): HttpResponse {
                calls += "$method $url auth=${headers["Authorization"]}"
                return when {
                    url.endsWith("refresh-token") -> HttpResponse(401, """{"code":401,"message":"Refresh expired"}""")
                    url.endsWith("login") -> HttpResponse(200, """{"code":200,"data":{"access_token":"fresh_tok","refresh_token":"fresh_ref"}}""")
                    url.endsWith("get-jadwal-hari-ini") -> {
                        if (headers["Authorization"] == "Bearer fresh_tok") {
                            HttpResponse(200, """{"code":200,"data":[]}""")
                        } else {
                            HttpResponse(401, """{"code":401,"message":"Token expired"}""")
                        }
                    }
                    else -> HttpResponse(404, "{}")
                }
            }
        }

        val client = DinusHttpClient(transport, storage)
        val result = client.getSiadin("get-jadwal-hari-ini")

        assertTrue(result is ApiResult.Success)
        assertEquals("fresh_tok", storage.getAccessToken())
        assertEquals(1, calls.count { it.contains("refresh-token") })
        assertEquals(1, calls.count { it.contains("login") })
        assertEquals(2, calls.count { it.contains("get-jadwal-hari-ini") })
    }

    @Test
    fun `concurrent 401 requests coalesce behind Mutex with only one login`() = runTest {
        val storage = FakeAuthStorage(refreshToken = null)
        val loginCount = AtomicInteger(0)

        val transport = object : HttpTransport {
            override fun execute(method: String, url: String, headers: Map<String, String>, jsonBody: String?): HttpResponse {
                return when {
                    url.endsWith("login") -> {
                        Thread.sleep(40)
                        val n = loginCount.incrementAndGet()
                        HttpResponse(200, """{"code":200,"data":{"access_token":"tok_$n"}}""")
                    }
                    headers["Authorization"]?.startsWith("Bearer tok_") == true -> {
                        HttpResponse(200, """{"code":200,"data":[]}""")
                    }
                    else -> HttpResponse(401, """{"code":401,"message":"Expired"}""")
                }
            }
        }

        val client = DinusHttpClient(transport, storage)
        val results = (1..5).map { async { client.getSiadin("get-jadwal") } }.awaitAll()

        assertTrue(results.all { it is ApiResult.Success })
        assertEquals(1, loginCount.get())
    }

    @Test
    fun `invalid stored password triggers sessionInvalidated without infinite loop`() = runTest {
        val storage = FakeAuthStorage(refreshToken = null)
        var invalidated = false

        val transport = object : HttpTransport {
            override fun execute(method: String, url: String, headers: Map<String, String>, jsonBody: String?): HttpResponse {
                return HttpResponse(401, """{"code":401,"message":"Username atau password salah"}""")
            }
        }

        val client = DinusHttpClient(transport, storage, onSessionInvalidated = { invalidated = true })
        val result = client.getSiadin("profile")

        assertTrue(result is ApiResult.Error)
        assertTrue((result as ApiResult.Error).sessionInvalidated)
        assertTrue(invalidated)
    }

    @Test
    fun `concurrent requests are throttled to at most 2 in-flight transport calls`() = runTest {
        val storage = FakeAuthStorage(accessToken = "valid_tok")
        val inFlight = AtomicInteger(0)
        val peak = AtomicInteger(0)

        val transport = object : HttpTransport {
            override fun execute(method: String, url: String, headers: Map<String, String>, jsonBody: String?): HttpResponse {
                val cur = inFlight.incrementAndGet()
                peak.updateAndGet { prev -> max(prev, cur) }
                try {
                    Thread.sleep(35)
                    return HttpResponse(200, """{"code":200,"data":[]}""")
                } finally {
                    inFlight.decrementAndGet()
                }
            }
        }

        val client = DinusHttpClient(transport, storage)
        val results = (1..6).map { idx ->
            async(kotlinx.coroutines.Dispatchers.IO) { client.getSiadin("endpoint-$idx") }
        }.awaitAll()

        assertTrue(results.all { it is ApiResult.Success })
        assertTrue("Expected peak <= 2 but was ${peak.get()}", peak.get() <= 2)
    }
}
