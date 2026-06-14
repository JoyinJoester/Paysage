package joyin.takgi.paysage.esim

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EsimSgp22ReadOnlyTest {
    @Test
    fun buildsSingleStoreDataBlock() {
        val blocks = EsimSgp22StoreDataApdu.buildStoreDataBlocks(
            payload = byteArrayOf(0xBF.toByte(), 0x2D, 0x00),
            maxDataBytes = 10
        )

        assertEquals(1, blocks.size)
        val block = blocks.first()
        assertEquals(0, block.blockNumber)
        assertFalse(block.moreBlocks)
        assertArrayEquals(
            byteArrayOf(0x80.toByte(), 0xE2.toByte(), 0x11, 0x00, 0x03, 0xBF.toByte(), 0x2D, 0x00, 0x00),
            block.apdu
        )
    }

    @Test
    fun buildsSegmentedStoreDataBlocks() {
        val blocks = EsimSgp22StoreDataApdu.buildStoreDataBlocks(
            payload = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05),
            maxDataBytes = 2
        )

        assertEquals(3, blocks.size)
        assertEquals(0, blocks[0].blockNumber)
        assertTrue(blocks[0].moreBlocks)
        assertArrayEquals(byteArrayOf(0x80.toByte(), 0xE2.toByte(), 0x91.toByte(), 0x00, 0x02, 0x01, 0x02, 0x00), blocks[0].apdu)
        assertEquals(1, blocks[1].blockNumber)
        assertTrue(blocks[1].moreBlocks)
        assertArrayEquals(byteArrayOf(0x80.toByte(), 0xE2.toByte(), 0x91.toByte(), 0x01, 0x02, 0x03, 0x04, 0x00), blocks[1].apdu)
        assertEquals(2, blocks[2].blockNumber)
        assertFalse(blocks[2].moreBlocks)
        assertArrayEquals(byteArrayOf(0x80.toByte(), 0xE2.toByte(), 0x11, 0x02, 0x01, 0x05, 0x00), blocks[2].apdu)
    }

    @Test
    fun buildsGetResponseApdu() {
        assertArrayEquals(
            byteArrayOf(0x00, 0xC0.toByte(), 0x00, 0x00, 0x10),
            EsimSgp22StoreDataApdu.buildGetResponse(0x10)
        )
    }

    @Test
    fun plansGetResponseWhenMoreDataIsAvailable() {
        val followUp = EsimSgp22ApduFollowUpPlanner.evaluate(
            Iso7816Response(data = byteArrayOf(), sw1 = 0x61, sw2 = 0x10)
        )

        assertEquals(EsimSgp22ApduFollowUpKind.GetResponse, followUp.kind)
        assertEquals(0x10, followUp.suggestedLe)
        assertArrayEquals(
            byteArrayOf(0x00, 0xC0.toByte(), 0x00, 0x00, 0x10),
            followUp.suggestedApdu
        )
        assertTrue(followUp.message.contains("GET RESPONSE"))
    }

    @Test
    fun plansGetResponseWithZeroLeForMaximumShortResponse() {
        val followUp = EsimSgp22ApduFollowUpPlanner.evaluate(
            Iso7816Response(data = byteArrayOf(), sw1 = 0x61, sw2 = 0x00)
        )

        assertEquals(EsimSgp22ApduFollowUpKind.GetResponse, followUp.kind)
        assertEquals(0x00, followUp.suggestedLe)
        assertArrayEquals(
            byteArrayOf(0x00, 0xC0.toByte(), 0x00, 0x00, 0x00),
            followUp.suggestedApdu
        )
        assertTrue(followUp.message.contains("最大长度"))
    }

    @Test
    fun plansRetryWhenCardReportsWrongLength() {
        val followUp = EsimSgp22ApduFollowUpPlanner.evaluate(
            Iso7816Response(data = byteArrayOf(), sw1 = 0x6C, sw2 = 0x20)
        )

        assertEquals(EsimSgp22ApduFollowUpKind.RetryWithLe, followUp.kind)
        assertEquals(0x20, followUp.suggestedLe)
        assertEquals(null, followUp.suggestedApdu)
        assertTrue(followUp.message.contains("重新发送上一条命令"))
    }

    @Test
    fun summarizesReadOnlyResponseWithoutRawValues() {
        val response = Iso7816Response(
            data = byteArrayOf(
                0xE3.toByte(),
                0x08,
                0x5A,
                0x06,
                0x98.toByte(),
                0x76,
                0x54,
                0x32,
                0x10,
                0x00
            ),
            sw1 = 0x90,
            sw2 = 0x00
        )

        val summary = EsimSgp22ReadOnlyResponseSummarizer.summarize(response)

        assertTrue(summary.accepted)
        assertEquals(EsimSgp22ApduFollowUpKind.Complete, summary.followUp.kind)
        assertEquals("9000", summary.statusWordHex)
        assertEquals(listOf("E3"), summary.topLevelTags)
        assertEquals(
            listOf(
                EsimSgp22TlvNodeSummary("E3", 8, 1, EsimSgp22TlvPrivacy.Structure),
                EsimSgp22TlvNodeSummary("5A", 6, 0, EsimSgp22TlvPrivacy.Sensitive)
            ),
            summary.nodeSummaries
        )
        assertFalse(summary.displayText.contains("987654321000"))
        assertTrue(summary.displayText.contains("已脱敏"))
        assertTrue(summary.displayText.contains("敏感或可能敏感"))
    }

    @Test
    fun summarizesGetResponseFollowUpWithoutRawValues() {
        val summary = EsimSgp22ReadOnlyResponseSummarizer.summarize(
            Iso7816Response(
                data = byteArrayOf(0x5A, 0x02, 0x98.toByte(), 0x76),
                sw1 = 0x61,
                sw2 = 0x08
            )
        )

        assertFalse(summary.accepted)
        assertEquals(EsimSgp22ApduFollowUpKind.GetResponse, summary.followUp.kind)
        assertTrue(summary.displayText.contains("GET RESPONSE"))
        assertFalse(summary.displayText.contains("9876"))
    }

    @Test
    fun classifiesLongPrimitiveValuesAsPotentiallySensitive() {
        val summary = EsimSgp22ReadOnlyResponseSummarizer.summarize(
            Iso7816Response(
                data = byteArrayOf(
                    0x81.toByte(),
                    0x04,
                    0x01,
                    0x02,
                    0x03,
                    0x04,
                    0x82.toByte(),
                    0x01,
                    0x05
                ),
                sw1 = 0x90,
                sw2 = 0x00
            )
        )

        assertEquals(
            EsimSgp22TlvPrivacy.PotentiallySensitive,
            summary.nodeSummaries.first { it.tagHex == "81" }.privacy
        )
        assertEquals(
            EsimSgp22TlvPrivacy.NonSensitive,
            summary.nodeSummaries.first { it.tagHex == "82" }.privacy
        )
        assertFalse(summary.displayText.contains("01020304"))
    }

    @Test
    fun classifiesKnownIdentifierTagsAsSensitiveEvenWhenShort() {
        val summary = EsimSgp22ReadOnlyResponseSummarizer.summarize(
            Iso7816Response(
                data = byteArrayOf(0x5A, 0x02, 0x12, 0x34),
                sw1 = 0x90,
                sw2 = 0x00
            )
        )

        assertEquals(EsimSgp22TlvPrivacy.Sensitive, summary.nodeSummaries.first().privacy)
        assertFalse(summary.displayText.contains("1234"))
    }

    @Test
    fun formatsRedactedTlvTreeWithHierarchyAndPrivacy() {
        val summary = EsimSgp22ReadOnlyResponseSummarizer.summarize(
            Iso7816Response(
                data = byteArrayOf(
                    0xE3.toByte(),
                    0x0A,
                    0xE1.toByte(),
                    0x08,
                    0x5A,
                    0x06,
                    0x98.toByte(),
                    0x76,
                    0x54,
                    0x32,
                    0x10,
                    0x00
                ),
                sw1 = 0x90,
                sw2 = 0x00
            )
        )

        assertEquals(
            listOf(
                "E3 len=10 children=1 privacy=结构",
                "  E1 len=8 children=1 privacy=结构",
                "    5A len=6 children=0 privacy=敏感"
            ),
            summary.redactedTreeLines
        )
        assertFalse(summary.redactedTreeLines.joinToString().contains("987654321000"))
    }

    @Test
    fun limitsRedactedTlvTreeNodeCount() {
        val nodes = BerTlvParser.parseAll(
            byteArrayOf(
                0x81.toByte(),
                0x01,
                0x01,
                0x82.toByte(),
                0x01,
                0x02,
                0x83.toByte(),
                0x01,
                0x03
            )
        )

        val lines = EsimSgp22RedactedTlvTreeFormatter.format(nodes, maxNodes = 2)

        assertEquals(
            listOf(
                "81 len=1 children=0 privacy=普通",
                "82 len=1 children=0 privacy=普通",
                "... node limit"
            ),
            lines
        )
        assertFalse(lines.joinToString().contains("010203"))
    }
}
