package joyin.takgi.paysage.esim

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BerTlvParserTest {
    @Test
    fun parsesConstructedFciTemplate() {
        val payload = byteArrayOf(
            0x6F,
            0x05,
            0x84.toByte(),
            0x03,
            0x01,
            0x02,
            0x03
        )

        val parsed = BerTlvParser.parseAll(payload)

        assertEquals(1, parsed.size)
        assertEquals("6F", parsed.first().tagHex)
        assertTrue(parsed.first().isConstructed)
        assertEquals(1, parsed.first().children.size)
        assertEquals("84", parsed.first().children.first().tagHex)
        assertArrayEquals(byteArrayOf(0x01, 0x02, 0x03), parsed.first().children.first().value)
    }

    @Test
    fun parsesMultiByteTagAndLongLength() {
        val value = ByteArray(130) { 0x55 }
        val payload = byteArrayOf(0xBF.toByte(), 0x2D, 0x81.toByte(), 0x82.toByte()) + value

        val parsed = BerTlvParser.parseAll(payload)

        assertEquals(1, parsed.size)
        assertEquals("BF2D", parsed.first().tagHex)
        assertArrayEquals(value, parsed.first().value)
    }
}
