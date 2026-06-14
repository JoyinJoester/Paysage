package joyin.takgi.paysage.esim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EsimFciSummaryTest {
    @Test
    fun summarizesIsdRSelectFci() {
        val fci = byteArrayOf(
            0x6F,
            0x14,
            0x84.toByte(),
            0x10
        ) + EsimApdu.ISD_R_AID + byteArrayOf(
            0xA5.toByte(),
            0x00
        )

        val summary = requireNotNull(EsimFciAnalyzer.summarize(fci))

        assertEquals(listOf("6F"), summary.topLevelTags)
        assertTrue(summary.hasFciTemplate)
        assertTrue(summary.hasProprietaryTemplate)
        assertEquals(true, summary.dedicatedFileNameMatchesIsdR)
        assertTrue(summary.displayText.contains("AID 匹配 ISD-R"))
        assertEquals(
            listOf(
                "6F len=20 children=2 privacy=结构",
                "  84 len=16 children=0 privacy=敏感",
                "  A5 len=0 children=0 privacy=结构"
            ),
            summary.redactedTreeLines
        )
        assertTrue(summary.diagnosticText.contains("TLV 树"))
        assertTrue(summary.diagnosticText.contains("84 len=16"))
        assertTrue(summary.diagnosticText.contains("privacy=敏感"))
        assertTrue(summary.diagnosticText.contains("AID 匹配 ISD-R"))
        assertTrue(summary.diagnosticText.contains("6F"))
        assertTrue(summary.diagnosticText.contains("A5"))
        assertTrue(summary.diagnosticText.contains("len=16"))
        assertTrue(summary.diagnosticText.contains("children=0"))
        assertTrue(summary.diagnosticText.contains("privacy=敏感"))
        assertTrue(summary.diagnosticText.contains("TLV 树"))
        assertFalse(summary.diagnosticText.contains(EsimApdu.ISD_R_AID.toHex()))
    }
}
