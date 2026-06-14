package joyin.takgi.paysage.esim.ccid

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CcidMessagesTest {
    @Test
    fun buildsIccPowerOnMessage() {
        val message = CcidMessages.buildIccPowerOn(sequence = 7, slot = 1, powerSelect = 0)

        assertEquals(10, message.size)
        assertEquals(0x62, message[0].toInt() and 0xFF)
        assertEquals(1, message[5].toInt() and 0xFF)
        assertEquals(7, message[6].toInt() and 0xFF)
        assertEquals(0, message[7].toInt() and 0xFF)
    }

    @Test
    fun buildsXfrBlockMessage() {
        val apdu = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00)
        val message = CcidMessages.buildXfrBlock(
            apdu = apdu,
            sequence = 9,
            slot = 1,
            blockWaitTimeExtension = 2,
            levelParameter = 0x1234
        )

        assertEquals(14, message.size)
        assertEquals(0x6F, message[0].toInt() and 0xFF)
        assertEquals(4, message[1].toInt() and 0xFF)
        assertEquals(0, message[2].toInt() and 0xFF)
        assertEquals(0, message[3].toInt() and 0xFF)
        assertEquals(0, message[4].toInt() and 0xFF)
        assertEquals(1, message[5].toInt() and 0xFF)
        assertEquals(9, message[6].toInt() and 0xFF)
        assertEquals(2, message[7].toInt() and 0xFF)
        assertEquals(0x34, message[8].toInt() and 0xFF)
        assertEquals(0x12, message[9].toInt() and 0xFF)
        assertArrayEquals(apdu, message.copyOfRange(10, message.size))
    }

    @Test
    fun parsesDataBlockWithAtr() {
        val atr = byteArrayOf(0x3B, 0x00)
        val response = byteArrayOf(
            0x80.toByte(),
            0x02, 0x00, 0x00, 0x00,
            0x00,
            0x07,
            0x00,
            0x00,
            0x00,
            atr[0],
            atr[1]
        )

        val parsed = CcidMessages.parseDataBlock(response)

        assertEquals(7, parsed.sequence)
        assertTrue(parsed.isCommandSuccessful)
        assertArrayEquals(atr, parsed.data)
    }
}
