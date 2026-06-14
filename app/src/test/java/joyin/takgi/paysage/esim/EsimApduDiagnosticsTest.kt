package joyin.takgi.paysage.esim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EsimApduDiagnosticsTest {
    @Test
    fun buildsPowerOnStepWithoutRawAtr() {
        val step = EsimApduDiagnostics.powerOn(
            success = true,
            message = "读卡器已上电并返回 ATR。",
            atrHex = "3B00"
        )

        assertEquals("CCID PowerOn", step.title)
        assertEquals(EsimApduDiagnosticStatus.Succeeded, step.status)
        assertEquals("响应 2 字节 / ATR 已返回", step.detailText)
    }

    @Test
    fun buildsSelectIsdRStepWithFciSummary() {
        val response = Iso7816Response(
            data = byteArrayOf(0x6F, 0x00),
            sw1 = 0x90,
            sw2 = 0x00
        )

        val step = EsimApduDiagnostics.selectIsdR(response, "FCI 6F")

        assertEquals("SELECT ISD-R", step.title)
        assertEquals(EsimApduDiagnosticStatus.Succeeded, step.status)
        assertEquals("9000", step.statusWord)
        assertEquals("SW 9000 / 响应 2 字节 / FCI 6F", step.detailText)
        assertTrue(step.message.contains("ISD-R 已响应"))
    }

    @Test
    fun explainsSelectIsdRMoreDataStatus() {
        val response = Iso7816Response(
            data = byteArrayOf(),
            sw1 = 0x61,
            sw2 = 0x10
        )

        val step = EsimApduDiagnostics.selectIsdR(response, null)

        assertEquals(EsimApduDiagnosticStatus.Failed, step.status)
        assertEquals("6110", step.statusWord)
        assertTrue(step.message.contains("GET RESPONSE"))
        assertTrue(step.detailText.contains("下一步"))
        assertTrue(step.detailText.contains("GET RESPONSE"))
    }

    @Test
    fun explainsSelectIsdRWrongLengthStatus() {
        val response = Iso7816Response(
            data = byteArrayOf(),
            sw1 = 0x6C,
            sw2 = 0x20
        )

        val step = EsimApduDiagnostics.selectIsdR(response, null)

        assertEquals(EsimApduDiagnosticStatus.Failed, step.status)
        assertEquals("6C20", step.statusWord)
        assertTrue(step.message.contains("Le=32"))
        assertTrue(step.detailText.contains("重新发送上一条命令"))
    }
}
