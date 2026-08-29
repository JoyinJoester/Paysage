package joyin.takgi.paysage.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * 网络延迟探测:请求 204 探测地址,测"发起请求到收到响应"的耗时,
 * 类似 FlClash 的 url-test。探测包只有几十字节,每 10 秒一次开销可忽略。
 * 国内可达的探测地址优先,国外地址作为备选。
 */
object LatencyProbe {

    private val ENDPOINTS = listOf(
        "http://connect.rom.miui.com/generate_204",
        "http://wifi.vivo.com.cn/generate_204",
        "http://connectivitycheck.platform.hicloud.com/generate_204",
        "https://www.gstatic.com/generate_204"
    )

    /** 返回首个成功端点的耗时(毫秒);全部失败返回 null */
    suspend fun measure(timeoutMs: Int = 3_000): Long? = withContext(Dispatchers.IO) {
        for (endpoint in ENDPOINTS) {
            val latency = runCatching { probe(endpoint, timeoutMs) }.getOrNull()
            if (latency != null) return@withContext latency
        }
        null
    }

    private fun probe(endpoint: String, timeoutMs: Int): Long? {
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Paysage/1.0")
            val start = System.currentTimeMillis()
            val code = connection.responseCode
            val elapsed = System.currentTimeMillis() - start
            // 204/301/302 等都算连通;5xx 视为探测失败
            return if (code in 200..399) elapsed else null
        } finally {
            connection.disconnect()
        }
    }
}
