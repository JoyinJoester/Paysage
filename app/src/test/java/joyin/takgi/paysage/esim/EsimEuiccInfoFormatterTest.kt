package joyin.takgi.paysage.esim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EsimEuiccInfoFormatterTest {
    @Test
    fun formatsBytesWithStableUnits() {
        assertEquals("512 B", EsimEuiccInfoFormatter.formatBytes(512))
        assertEquals("1.5 KB", EsimEuiccInfoFormatter.formatBytes(1536))
        assertEquals("1 MB", EsimEuiccInfoFormatter.formatBytes(1024 * 1024))
    }

    @Test
    fun treatsNegativeMemoryAsUnavailable() {
        val memory = EsimEuiccInfoFormatter.memory(-1)

        assertNull(memory.bytes)
        assertEquals("未公开", memory.displayText)
        assertEquals("系统未公开 eUICC 可用空间。", memory.message)
    }

    @Test
    fun buildsReadablePortMessages() {
        assertEquals(
            "端口 1 可用于下载或切换。",
            EsimEuiccInfoFormatter.portMessage(0, EsimEuiccPortAvailability.Available)
        )
        assertEquals(
            "端口 2 当前不可用或已被占用。",
            EsimEuiccInfoFormatter.portMessage(1, EsimEuiccPortAvailability.Unavailable)
        )
    }

    @Test
    fun buildsSwitchPortLabels() {
        assertEquals("切换", EsimEuiccInfoFormatter.switchActionLabel(null))
        assertEquals("切换到端口 3", EsimEuiccInfoFormatter.switchActionLabel(2))
        assertEquals("系统自动", EsimEuiccInfoFormatter.selectedSwitchPortLabel(null))
        assertEquals("端口 2", EsimEuiccInfoFormatter.selectedSwitchPortLabel(1))
    }

    @Test
    fun buildsSubscriptionCardAndPortLabels() {
        assertEquals("eUICC 卡 4", EsimEuiccInfoFormatter.subscriptionCardLabel(4))
        assertEquals("eUICC 卡未公开", EsimEuiccInfoFormatter.subscriptionCardLabel(null))
        assertEquals("eUICC 卡未公开", EsimEuiccInfoFormatter.subscriptionCardLabel(-1))
        assertEquals("当前端口 2", EsimEuiccInfoFormatter.subscriptionPortLabel(1))
        assertEquals("当前端口未公开", EsimEuiccInfoFormatter.subscriptionPortLabel(null))
    }
}
