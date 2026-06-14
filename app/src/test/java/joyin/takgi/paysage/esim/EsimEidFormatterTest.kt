package joyin.takgi.paysage.esim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EsimEidFormatterTest {
    @Test
    fun normalizesEidForDisplayAndCopy() {
        assertEquals(
            "89049032000000000000000000000001",
            EsimEidFormatter.normalize("8904 9032-0000 0000 0000 0000 0000 0001")
        )
    }

    @Test
    fun groupsEidInFourCharacterChunks() {
        assertEquals(
            "8904 9032 0000 0000 0000 0000 0000 0001",
            EsimEidFormatter.grouped("89049032000000000000000000000001")
        )
    }

    @Test
    fun masksMiddleDigitsForOnScreenDisplay() {
        val masked = EsimEidFormatter.masked("89049032000000000000000000000001")

        assertTrue(masked.startsWith("8904"))
        assertTrue(masked.endsWith("0001"))
        assertTrue(masked.contains("••••"))
    }
}
