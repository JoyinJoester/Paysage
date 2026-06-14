package joyin.takgi.paysage.esim

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Iso7816ApduTest {
    @Test
    fun buildsSelectIsdRApdu() {
        val apdu = EsimApdu.buildSelectIsdR()

        assertEquals(22, apdu.size)
        assertEquals(0x00, apdu[0].toInt() and 0xFF)
        assertEquals(0xA4, apdu[1].toInt() and 0xFF)
        assertEquals(0x04, apdu[2].toInt() and 0xFF)
        assertEquals(0x00, apdu[3].toInt() and 0xFF)
        assertEquals(16, apdu[4].toInt() and 0xFF)
        assertArrayEquals(EsimApdu.ISD_R_AID, apdu.copyOfRange(5, 21))
        assertEquals(0x00, apdu[21].toInt() and 0xFF)
    }

    @Test
    fun parsesApduResponseStatusWord() {
        val response = EsimApdu.parseResponse(
            byteArrayOf(0x6F, 0x02, 0x84.toByte(), 0x00, 0x90.toByte(), 0x00)
        )

        assertArrayEquals(byteArrayOf(0x6F, 0x02, 0x84.toByte(), 0x00), response.data)
        assertEquals(0x90, response.sw1)
        assertEquals(0x00, response.sw2)
        assertEquals("9000", response.statusWordHex)
        assertTrue(response.isAccepted)
    }
}
