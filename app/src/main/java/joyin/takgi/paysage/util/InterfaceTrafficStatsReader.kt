package joyin.takgi.paysage.util

import android.net.TrafficStats
import java.net.NetworkInterface

/**
 * 第三级采样:接口名来自 NetworkInterface(走 getifaddrs,不受
 * /proc、/sys 读取限制),字节计数用 TrafficStats 的按接口原生方法。
 * 相比总量读数可保留"只统计蜂窝 WAN 口"的准确性;
 * 原生读取同样被限制时(返回 -1)该接口不可用,交给总量兜底。
 */
object InterfaceTrafficStatsReader {

    /** iface -> (rxBytes, txBytes) */
    fun read(): Map<String, Pair<Long, Long>> {
        val names = runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return emptyMap()
            buildList {
                while (interfaces.hasMoreElements()) {
                    add(interfaces.nextElement().name)
                }
            }
        }.getOrDefault(emptyList())

        val result = LinkedHashMap<String, Pair<Long, Long>>()
        names.forEach { name ->
            val rx = runCatching { TrafficStats.getRxBytes(name) }.getOrDefault(-1L)
            val tx = runCatching { TrafficStats.getTxBytes(name) }.getOrDefault(-1L)
            if (rx >= 0 && tx >= 0) {
                result[name] = rx to tx
            }
        }
        return result
    }
}
