package joyin.takgi.paysage.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class NetworkSpeedSnapshot(
    val downloadBytesPerSec: Long = 0,
    val uploadBytesPerSec: Long = 0,
    val downloadTotalBytes: Long = 0,
    val uploadTotalBytes: Long = 0,
    val history: List<Long> = emptyList(),
    val countedInterfaces: List<String> = emptyList()
)

/**
 * 被动网速监测:周期读取 /proc/net/dev 的内核字节计数并做差分,
 * 不发起任何网络请求,不会影响网速。采样循环由调用方(页面)的
 * 协程作用域驱动,页面销毁协程即取消,离开页面就停止检测。
 */
class NetworkSpeedMonitor(
    private val intervalMs: Long = SAMPLE_INTERVAL_MS,
    private val historySize: Int = HISTORY_SIZE,
    private val counterReader: () -> Map<String, Pair<Long, Long>> = {
        // Android 10+ 禁止应用读 /proc/net/dev,读不到时回退 /sys/class/net
        runCatching { NetDevParser.parse(File(PROC_NET_DEV).readText()) }.getOrDefault(emptyMap())
            .ifEmpty { SystemNetCounters.read() }
    }
) {
    private val _state = MutableStateFlow(NetworkSpeedSnapshot())
    val state: StateFlow<NetworkSpeedSnapshot> = _state.asStateFlow()

    private var lastSample: Pair<Long, Map<String, Pair<Long, Long>>>? = null
    private val history = ArrayDeque<Long>()
    private var downloadTotal = 0L
    private var uploadTotal = 0L

    suspend fun run() {
        while (true) {
            sample()
            delay(intervalMs)
        }
    }

    fun sample() {
        val now = System.currentTimeMillis()
        val counters = counterReader()
        val previous = lastSample
        lastSample = now to counters

        if (previous == null) {
            _state.value = NetworkSpeedSnapshot(
                countedInterfaces = NetDevParser.wanInterfaceNames(counters.keys)
            )
            return
        }

        val (lastTime, lastCounters) = previous
        val elapsedMs = (now - lastTime).coerceAtLeast(1)
        val wanNames = NetDevParser.wanInterfaceNames(counters.keys)

        var rxDelta = 0L
        var txDelta = 0L
        wanNames.forEach { name ->
            val current = counters[name] ?: return@forEach
            val before = lastCounters[name] ?: return@forEach
            rxDelta += (current.first - before.first).coerceAtLeast(0)
            txDelta += (current.second - before.second).coerceAtLeast(0)
        }

        val downloadSpeed = rxDelta * 1000 / elapsedMs
        val uploadSpeed = txDelta * 1000 / elapsedMs
        downloadTotal += rxDelta
        uploadTotal += txDelta

        history.addLast(downloadSpeed + uploadSpeed)
        while (history.size > historySize) history.removeFirst()

        _state.value = NetworkSpeedSnapshot(
            downloadBytesPerSec = downloadSpeed,
            uploadBytesPerSec = uploadSpeed,
            downloadTotalBytes = downloadTotal,
            uploadTotalBytes = uploadTotal,
            history = history.toList(),
            countedInterfaces = wanNames
        )
    }

    companion object {
        const val SAMPLE_INTERVAL_MS = 1_000L
        const val HISTORY_SIZE = 60
        const val PROC_NET_DEV = "/proc/net/dev"
    }
}
