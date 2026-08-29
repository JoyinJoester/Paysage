package joyin.takgi.paysage.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * 公网 IP 查询:仅在用户打开网速仪表盘或手动刷新时各发一次极小请求,
 * 不参与任何周期监测。
 */
object PublicIpChecker {

    private val ENDPOINTS = listOf(
        "https://myip.ipip.net",
        "https://api.ipify.org",
        "https://ifconfig.me/ip"
    )

    private val IP_PATTERN = Regex("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b")

    suspend fun fetch(): Result<String> = withContext(Dispatchers.IO) {
        var lastError: Exception? = null
        for (endpoint in ENDPOINTS) {
            runCatching {
                val connection = URL(endpoint).openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 5_000
                    connection.readTimeout = 5_000
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("User-Agent", "curl/8.0")
                    if (connection.responseCode !in 200..299) {
                        throw Exception("HTTP ${connection.responseCode}")
                    }
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    IP_PATTERN.find(body)?.value
                        ?: throw Exception("no IPv4 found in response")
                } finally {
                    connection.disconnect()
                }
            }.onSuccess { return@withContext Result.success(it) }
                .onFailure { lastError = it as? Exception ?: Exception(it) }
        }
        Result.failure(lastError ?: Exception("all endpoints failed"))
    }
}
