package joyin.takgi.paysage.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.telephony.TelephonyManager
import android.telephony.SignalStrength
import joyin.takgi.paysage.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

data class LatencyResult(
    val target: String,
    val latencyMs: Long,
    val success: Boolean
)

data class NetworkSpeedResult(
    val networkType: String,
    val signalStrength: String,
    val latencies: List<LatencyResult>,
    val downloadSpeedMbps: Float,
    val dataConsumedMB: Float,
    val testDurationMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)

object NetworkSpeedCollector {
    private const val CACHE_DURATION_MS = 60_000L // 1 minute
    private var cachedResult: NetworkSpeedResult? = null
    private var cacheTimestamp: Long = 0

    private val testTargets = listOf(
        "https://www.google.com" to "Google",
        "https://www.cloudflare.com" to "Cloudflare",
        "https://www.github.com" to "GitHub",
        "https://www.baidu.com" to "百度",
        "https://www.aliyun.com" to "阿里云"
    )

    private const val DOWNLOAD_TEST_URL = "https://speed.cloudflare.com/__down?bytes=3000000" // 3MB
    private const val TIMEOUT_MS = 10_000
    private const val LATENCY_TIMEOUT_MS = 5_000

    suspend fun collectNetworkSpeed(context: Context, useCache: Boolean = true): NetworkSpeedResult {
        if (useCache) {
            val cached = cachedResult
            if (cached != null && System.currentTimeMillis() - cacheTimestamp < CACHE_DURATION_MS) {
                return cached
            }
        }

        val startTime = System.currentTimeMillis()

        return withContext(Dispatchers.IO) {
            val networkType = getNetworkType(context)
            val signalStrength = getSignalStrength(context)

            // Test latency to all targets in parallel
            val latencies = testTargets.map { (url, name) ->
                async {
                    testLatency(url, name)
                }
            }.map { it.await() }

            // Test download speed
            val downloadResult = testDownloadSpeed()

            val duration = System.currentTimeMillis() - startTime

            val result = NetworkSpeedResult(
                networkType = networkType,
                signalStrength = signalStrength,
                latencies = latencies,
                downloadSpeedMbps = downloadResult.first,
                dataConsumedMB = downloadResult.second,
                testDurationMs = duration
            )

            cachedResult = result
            cacheTimestamp = System.currentTimeMillis()

            result
        }
    }

    private fun getNetworkType(context: Context): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return "Unknown"
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "Unknown"

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                "WiFi"
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                when (telephonyManager.dataNetworkType) {
                    TelephonyManager.NETWORK_TYPE_NR -> "5G"
                    TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
                    TelephonyManager.NETWORK_TYPE_HSPAP,
                    TelephonyManager.NETWORK_TYPE_HSPA,
                    TelephonyManager.NETWORK_TYPE_HSUPA,
                    TelephonyManager.NETWORK_TYPE_HSDPA -> "3G HSPA"
                    TelephonyManager.NETWORK_TYPE_UMTS,
                    TelephonyManager.NETWORK_TYPE_EVDO_0,
                    TelephonyManager.NETWORK_TYPE_EVDO_A,
                    TelephonyManager.NETWORK_TYPE_EVDO_B -> "3G"
                    TelephonyManager.NETWORK_TYPE_EDGE,
                    TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
                    else -> "Cellular"
                }
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Unknown"
        }
    }

    private fun getSignalStrength(context: Context): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return "N/A"
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "N/A"

        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return "N/A"
        }

        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        return runCatching {
            val signalStrength = telephonyManager.signalStrength
            val level = signalStrength?.level ?: -1
            when (level) {
                4 -> "Excellent"
                3 -> "Good"
                2 -> "Fair"
                1 -> "Poor"
                0 -> "Very Poor"
                else -> "Unknown"
            }
        }.getOrDefault("Unknown")
    }

    private suspend fun testLatency(urlString: String, name: String): LatencyResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val startTime = System.currentTimeMillis()
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = LATENCY_TIMEOUT_MS
            connection.readTimeout = LATENCY_TIMEOUT_MS
            connection.instanceFollowRedirects = false

            val responseCode = connection.responseCode
            val latency = System.currentTimeMillis() - startTime

            LatencyResult(
                target = name,
                latencyMs = latency,
                success = responseCode in 200..399
            )
        } catch (e: Exception) {
            LatencyResult(
                target = name,
                latencyMs = -1L,
                success = false
            )
        } finally {
            connection?.disconnect()
        }
    }

    private suspend fun testDownloadSpeed(): Pair<Float, Float> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(DOWNLOAD_TEST_URL)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS

            val startTime = System.currentTimeMillis()
            val inputStream = connection.inputStream
            val buffer = ByteArray(8192)
            var totalBytes = 0L
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                totalBytes += bytesRead
            }

            val duration = System.currentTimeMillis() - startTime
            inputStream.close()

            if (duration > 0) {
                val speedMbps = (totalBytes * 8.0 / 1_000_000.0 / (duration / 1000.0)).toFloat()
                val dataMB = totalBytes / 1_000_000.0f
                Pair(speedMbps, dataMB)
            } else {
                Pair(0f, 0f)
            }
        } catch (e: Exception) {
            Pair(0f, 0f)
        } finally {
            connection?.disconnect()
        }
    }

    fun formatNetworkSpeedReport(context: Context, result: NetworkSpeedResult): String {
        return buildString {
            appendLine(context.getString(R.string.network_status_title))
            appendLine("━━━━━━━━━━━━")
            appendLine()

            appendLine("🌐 ${context.getString(R.string.network_type)}: ${result.networkType}")

            if (result.signalStrength != "N/A") {
                val signalEmoji = when (result.signalStrength) {
                    "Excellent" -> "📶"
                    "Good" -> "📶"
                    "Fair" -> "📶"
                    "Poor" -> "📡"
                    else -> "📡"
                }
                appendLine("$signalEmoji ${context.getString(R.string.signal_strength)}: ${result.signalStrength}")
            }

            appendLine()
            appendLine("⏱️ ${context.getString(R.string.latency_test_results)}:")

            result.latencies.forEach { latency ->
                if (latency.success) {
                    val emoji = when {
                        latency.latencyMs < 100 -> "✅"
                        latency.latencyMs < 300 -> "⚠️"
                        else -> "🔴"
                    }
                    appendLine("  $emoji ${latency.target}: ${latency.latencyMs}ms")
                } else {
                    appendLine("  ❌ ${latency.target}: ${context.getString(R.string.network_unreachable)}")
                }
            }

            appendLine()
            if (result.downloadSpeedMbps > 0) {
                val speedEmoji = when {
                    result.downloadSpeedMbps >= 10 -> "⬇️"
                    result.downloadSpeedMbps >= 1 -> "⬇️"
                    else -> "🐌"
                }
                appendLine("$speedEmoji ${context.getString(R.string.download_speed)}: ${String.format("%.2f", result.downloadSpeedMbps)} Mbps")

                if (result.downloadSpeedMbps < 1) {
                    appendLine("⚠️ ${context.getString(R.string.network_slow_warning)}")
                }
            } else {
                appendLine("❌ ${context.getString(R.string.download_test_failed)}")
            }

            appendLine()
            appendLine("📊 ${context.getString(R.string.test_info)}:")
            appendLine("  • ${context.getString(R.string.data_consumed)}: ${String.format("%.2f", result.dataConsumedMB)} MB")
            appendLine("  • ${context.getString(R.string.test_duration)}: ${String.format("%.1f", result.testDurationMs / 1000.0)}s")

            appendLine()
            val formattedTime = java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                java.util.Locale.getDefault()
            ).format(java.util.Date(result.timestamp))
            appendLine("🕐 ${context.getString(R.string.last_updated)}: $formattedTime")
        }
    }
}
