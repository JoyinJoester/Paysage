package joyin.takgi.paysage.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetDevParserTest {

    private val procNetDev = """
        Inter-|   Receive
         face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed
            lo: 51200    1024    0    0    0     0          0         0    51200    1024    0    0    0     0       0          0
      wlan0: 1000    10    0    0    0     0          0         0    2000    20    0    0    0     0       0          0
  rmnet_data0: 500000    400    0    0    0     0          0         0    90000    300    0    0    0     0       0          0
         ap0: 300000    250    0    0    0     0          0         0    450000    380    0    0    0     0       0          0
       tun0: 7000     70    0    0    0     0          0         0     8000    80    0    0    0     0       0          0
    """.trimIndent()

    @Test
    fun parsesRxAndTxCounters() {
        val counters = NetDevParser.parse(procNetDev)
        assertEquals(5, counters.size)
        assertEquals(500000L to 90000L, counters["rmnet_data0"])
        assertEquals(51200L to 51200L, counters["lo"])
    }

    @Test
    fun vpnInterfacesTakePriorityOverCellular() {
        val wan = NetDevParser.wanInterfaceNames(setOf("lo", "wlan0", "rmnet_data0", "ap0", "tun0"))
        assertEquals(listOf("tun0"), wan)
    }

    @Test
    fun cellularInterfacesWinWhenNoVpn() {
        val wan = NetDevParser.wanInterfaceNames(setOf("lo", "wlan0", "rmnet_data0", "ccmni1", "ap0"))
        // 热点 AP(ap0)不计入,避免客户端流量被重复统计
        assertEquals(listOf("ccmni1", "rmnet_data0"), wan)
    }

    @Test
    fun apInterfacesExcludedFromFallback() {
        val wan = NetDevParser.wanInterfaceNames(setOf("lo", "wlan0", "ap0", "swlan0"))
        assertEquals(listOf("wlan0"), wan)
    }

    @Test
    fun loopbackNeverCounted() {
        val wan = NetDevParser.wanInterfaceNames(setOf("lo"))
        assertTrue(wan.isEmpty())
    }
}

class NetworkSpeedMonitorTest {

    @Test
    fun accumulatesSessionTotalsFromCounterDeltas() = kotlinx.coroutines.runBlocking {
        var counters = mapOf("rmnet0" to (1_000L to 2_000L))
        val monitor = NetworkSpeedMonitor(counterReader = { counters })

        monitor.sample() // 基线
        counters = mapOf("rmnet0" to (3_000L to 2_500L))
        monitor.sample()
        counters = mapOf("rmnet0" to (3_500L to 2_500L))
        monitor.sample()
        val snapshot = monitor.state.value

        // 下载共 +2500B,上传共 +500B;速率本身依赖真实耗时,不在此断言
        assertEquals(2_500L, snapshot.downloadTotalBytes)
        assertEquals(500L, snapshot.uploadTotalBytes)
        assertEquals(listOf("rmnet0"), snapshot.countedInterfaces)
        assertEquals(2, snapshot.history.size)
    }
}
