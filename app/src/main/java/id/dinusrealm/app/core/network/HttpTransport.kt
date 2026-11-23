package id.dinusrealm.app.core.network

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

data class HttpResponse(
    val statusCode: Int,
    val body: String
)

/**
 * Minimal HTTP transport abstraction implemented directly on top of HttpsURLConnection
 * (zero OkHttp/Retrofit dependencies), easily fakeable in JVM unit tests.
 */
interface HttpTransport {
    @Throws(IOException::class)
    fun execute(
        method: String,
        url: String,
        headers: Map<String, String> = emptyMap(),
        jsonBody: String? = null
    ): HttpResponse
}

class UrlConnectionTransport(
    private val connectTimeoutMs: Int = 15_000,
    private val readTimeoutMs: Int = 20_000
) : HttpTransport {

    override fun execute(
        method: String,
        url: String,
        headers: Map<String, String>,
        jsonBody: String?
    ): HttpResponse {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "DinusRealm/1.1 (Android)")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
            if (jsonBody != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
        }

        try {
            if (jsonBody != null) {
                conn.outputStream.use { out ->
                    out.write(jsonBody.toByteArray(StandardCharsets.UTF_8))
                }
            }

            val status = conn.responseCode
            val stream = if (status in 200..399) conn.inputStream else (conn.errorStream ?: conn.inputStream)
            val body = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() } ?: ""
            return HttpResponse(statusCode = status, body = body)
        } finally {
            conn.disconnect()
        }
    }
}
