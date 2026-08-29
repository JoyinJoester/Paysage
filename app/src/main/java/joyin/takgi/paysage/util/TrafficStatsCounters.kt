package joyin.takgi.paysage.util

import android.net.TrafficStats

/**
 * 最终兜底:TrafficStats 系统总量。
 * 部分国产 ROM 连 /sys/class/net 的计数节点也会限制,
 * TrafficStats 是框架公开 API,所有设备都可用。
 * 注意:热点转发流量会在客户端口与蜂窝口各计一次,总量口径略有放大,
 * 仅在前两条按接口的采样路径全部失败时使用。
 */
object TrafficStatsCounters {

    const val PSEUDO_INTERFACE = "total"

    /** iface -> (rxBytes, txBytes);API 不支持时返回 -1,视为不可用 */
    fun read(): Map<String, Pair<Long, Long>> {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        if (rx < 0 && tx < 0) return emptyMap()
        return mapOf(PSEUDO_INTERFACE to (rx.coerceAtLeast(0) to tx.coerceAtLeast(0)))
    }
}
