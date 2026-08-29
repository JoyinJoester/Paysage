package joyin.takgi.paysage.util

import java.io.File

/**
 * 从 /sys/class/net 逐接口读取字节计数。
 * Android 10+ 应用被 SELinux 禁止读取 /proc/net/dev,
 * sysfs 的 statistics 节点不受该限制,作为主采样路径的回退。
 */
object SystemNetCounters {

    /** iface -> (rxBytes, txBytes) */
    fun read(netDir: File = File("/sys/class/net")): Map<String, Pair<Long, Long>> =
        runCatching {
            val names = netDir.listFiles()?.mapNotNull { it.name }.orEmpty()
            buildMap {
                names.forEach { name ->
                    val rx = readLong(File(netDir, "$name/statistics/rx_bytes"))
                    val tx = readLong(File(netDir, "$name/statistics/tx_bytes"))
                    if (rx != null && tx != null) {
                        put(name, rx to tx)
                    }
                }
            }
        }.getOrDefault(emptyMap())

    private fun readLong(file: File): Long? =
        runCatching {
            file.readText().trim().toLongOrNull()
        }.getOrNull()
}
