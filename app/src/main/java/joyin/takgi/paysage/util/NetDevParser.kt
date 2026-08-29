package joyin.takgi.paysage.util

/**
 * 解析 /proc/net/dev 并挑选"广域网"接口做速率统计。
 *
 * 热点机会同时有蜂窝上行接口和热点 AP 接口,客户端流量会在两者各计一次,
 * 所以只统计 WAN 侧:VPN tun 优先,其次蜂窝,最后才回退到除 AP 外的全部接口。
 * 纯逻辑无 Android 依赖,便于单元测试。
 */
object NetDevParser {

    /** iface -> (rxBytes, txBytes) */
    fun parse(procNetDev: String): Map<String, Pair<Long, Long>> =
        procNetDev.lineSequence()
            .mapNotNull { line ->
                val colon = line.indexOf(':')
                if (colon <= 0) return@mapNotNull null
                val name = line.substring(0, colon).trim()
                if (name.isEmpty()) return@mapNotNull null
                val fields = line.substring(colon + 1).trim().split(WHITESPACE)
                if (fields.size < RX_BYTES_INDEX + TX_BYTES_INDEX + 1) return@mapNotNull null
                val rx = fields[RX_BYTES_INDEX].toLongOrNull() ?: return@mapNotNull null
                val tx = fields[TX_BYTES_INDEX].toLongOrNull() ?: return@mapNotNull null
                name to (rx to tx)
            }
            .toMap()

    /**
     * 从接口名中挑出 WAN 侧接口。优先级:VPN(tun/tap)>
     * 蜂窝(rmnet/ccmni/wwan 等)> 有线或 WiFi 工作站(排除热点 AP 接口)。
     */
    fun wanInterfaceNames(names: Collection<String>): List<String> {
        val candidates = names.filter { it != LOOPBACK }
        val vpn = candidates.filter { isVpnInterface(it) }
        if (vpn.isNotEmpty()) return vpn.sorted()
        val cellular = candidates.filter { isCellularInterface(it) }
        if (cellular.isNotEmpty()) return cellular.sorted()
        return candidates
            .filter { it !in TETHER_AP_INTERFACES }
            .sorted()
    }

    fun isVpnInterface(name: String): Boolean =
        name.startsWith("tun") || name.startsWith("tap")

    fun isCellularInterface(name: String): Boolean =
        CELLULAR_PREFIXES.any { name.startsWith(it) }

    private val CELLULAR_PREFIXES = listOf(
        "rmnet", "ccmni", "wwan", "v4-rmnet", "r_rmnet", "svnet", "ipc"
    )

    private val TETHER_AP_INTERFACES = setOf("ap0", "swlan0", "softap0", "rndis0")

    private const val LOOPBACK = "lo"
    private const val RX_BYTES_INDEX = 0
    private const val TX_BYTES_INDEX = 8
    private val WHITESPACE = Regex("\\s+")
}
