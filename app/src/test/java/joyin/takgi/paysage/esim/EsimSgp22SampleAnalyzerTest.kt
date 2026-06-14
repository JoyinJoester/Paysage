package joyin.takgi.paysage.esim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EsimSgp22SampleAnalyzerTest {
    @Test
    fun analyzesRedactedResponseSample() {
        val analysis = EsimSgp22SampleAnalyzer.analyze(
            "E3 08 5A 06 98 76 54 32 10 00 90 00"
        )

        assertTrue(analysis.success)
        assertEquals("9000", analysis.statusWord)
        assertTrue(analysis.message.contains("已脱敏"))
        assertTrue(analysis.redactedLines.contains("E3 len=8 children=1 privacy=结构"))
        assertTrue(analysis.redactedLines.contains("  5A len=6 children=0 privacy=敏感"))
        assertEquals(EsimSgp22SampleKind.ProfileMetadataCandidate, analysis.classification?.kind)
        assertEquals(EsimSgp22SampleConfidence.Medium, analysis.classification?.confidence)
        assertEquals(2, analysis.privacyAudit?.totalNodeCount)
        assertEquals(1, analysis.privacyAudit?.structureNodeCount)
        assertEquals(1, analysis.privacyAudit?.sensitiveNodeCount)
        assertTrue(analysis.privacyAudit?.summary.orEmpty().contains("按敏感或可能敏感处理"))
        assertFalse(analysis.message.contains("987654321000"))
        assertFalse(analysis.redactedLines.joinToString().contains("987654321000"))
        assertFalse(analysis.classification?.reason.orEmpty().contains("987654321000"))
        assertFalse(analysis.redactedReport.orEmpty().contains("987654321000"))
    }

    @Test
    fun acceptsCommonHexSeparators() {
        val analysis = EsimSgp22SampleAnalyzer.analyze("0x61:0x10")

        assertTrue(analysis.success)
        assertEquals("6110", analysis.statusWord)
        assertTrue(analysis.message.contains("GET RESPONSE"))
        assertEquals(listOf("无响应数据，仅包含状态字。"), analysis.redactedLines)
        assertEquals(EsimSgp22SampleKind.StatusOnly, analysis.classification?.kind)
        assertEquals(EsimSgp22SampleConfidence.High, analysis.classification?.confidence)
        assertEquals(0, analysis.privacyAudit?.totalNodeCount)
        assertTrue(analysis.privacyAudit?.shareGuidance.orEmpty().contains("状态字"))
    }

    @Test
    fun classifiesSelectFciSample() {
        val analysis = EsimSgp22SampleAnalyzer.analyze("6F 05 84 03 01 02 03 90 00")

        assertTrue(analysis.success)
        assertEquals(EsimSgp22SampleKind.SelectFci, analysis.classification?.kind)
        assertEquals(EsimSgp22SampleConfidence.High, analysis.classification?.confidence)
        assertTrue(analysis.classification?.recommendation.orEmpty().contains("ISD-R"))
        assertFalse(analysis.redactedLines.joinToString().contains("010203"))
    }

    @Test
    fun usesDeclaredSelectContextForFciSample() {
        val analysis = EsimSgp22SampleAnalyzer.analyze(
            input = "6F 05 84 03 01 02 03 90 00",
            commandContext = EsimSgp22SampleContext.SelectIsdR
        )

        assertTrue(analysis.success)
        assertEquals(EsimSgp22SampleContext.SelectIsdR, analysis.commandContext)
        assertEquals(EsimSgp22SampleKind.SelectFci, analysis.classification?.kind)
        assertEquals(EsimSgp22SampleConfidence.High, analysis.classification?.confidence)
        assertTrue(analysis.classification?.reason.orEmpty().contains("用户声明"))
    }

    @Test
    fun declaredProfileContextRaisesConfidenceWhenProfileTagsExist() {
        val analysis = EsimSgp22SampleAnalyzer.analyze(
            input = "E3 08 5A 06 98 76 54 32 10 00 90 00",
            commandContext = EsimSgp22SampleContext.ProfileReadOnly
        )

        assertTrue(analysis.success)
        assertEquals(EsimSgp22SampleKind.ProfileMetadataCandidate, analysis.classification?.kind)
        assertEquals(EsimSgp22SampleConfidence.High, analysis.classification?.confidence)
        assertTrue(analysis.classification?.recommendation.orEmpty().contains("生命周期操作仍保持阻断"))
    }

    @Test
    fun buildsRedactedReportForManualCopy() {
        val analysis = EsimSgp22SampleAnalyzer.analyze(
            input = "E3 08 5A 06 98 76 54 32 10 00 90 00",
            commandContext = EsimSgp22SampleContext.ProfileReadOnly
        )
        val report = analysis.redactedReport.orEmpty()

        assertTrue(report.contains("Paysage SGP.22 脱敏样本分析"))
        assertTrue(report.contains("命令上下文: profile 只读"))
        assertTrue(report.contains("SW: 9000"))
        assertTrue(report.contains("样本类型: profile 只读样本候选"))
        assertTrue(report.contains("置信度: 高"))
        assertTrue(report.contains("隐私审计: 共 2 个 TLV 节点"))
        assertTrue(report.contains("节点计数: total=2, structure=1, sensitive=1"))
        assertTrue(report.contains("E3 len=8 children=1 privacy=结构"))
        assertTrue(report.contains("不包含原始 APDU"))
        assertFalse(report.contains("987654321000"))
    }

    @Test
    fun doesNotBuildRedactedReportForInvalidSample() {
        val analysis = EsimSgp22SampleAnalyzer.analyze("ABC")

        assertFalse(analysis.success)
        assertEquals(null, analysis.redactedReport)
    }

    @Test
    fun classifiesNotificationCandidateConservatively() {
        val analysis = EsimSgp22SampleAnalyzer.analyze("E3 03 80 01 01 90 00")

        assertTrue(analysis.success)
        assertEquals(EsimSgp22SampleKind.NotificationCandidate, analysis.classification?.kind)
        assertEquals(EsimSgp22SampleConfidence.Low, analysis.classification?.confidence)
        assertTrue(analysis.classification?.reason.orEmpty().contains("低置信度"))
    }

    @Test
    fun declaredNotificationContextKeepsCandidateBounded() {
        val analysis = EsimSgp22SampleAnalyzer.analyze(
            input = "E3 03 80 01 01 90 00",
            commandContext = EsimSgp22SampleContext.NotificationReadOnly
        )

        assertTrue(analysis.success)
        assertEquals(EsimSgp22SampleKind.NotificationCandidate, analysis.classification?.kind)
        assertEquals(EsimSgp22SampleConfidence.Medium, analysis.classification?.confidence)
        assertTrue(analysis.classification?.recommendation.orEmpty().contains("当前不读取或确认"))
    }

    @Test
    fun classifiesGenericReadOnlyTlvWhenContextIsInsufficient() {
        val analysis = EsimSgp22SampleAnalyzer.analyze("80 01 01 90 00")

        assertTrue(analysis.success)
        assertEquals(EsimSgp22SampleKind.GenericReadOnlyTlv, analysis.classification?.kind)
        assertEquals(EsimSgp22SampleConfidence.Low, analysis.classification?.confidence)
    }

    @Test
    fun carriesDeclaredContextForInvalidSamples() {
        val analysis = EsimSgp22SampleAnalyzer.analyze(
            input = "ABC",
            commandContext = EsimSgp22SampleContext.ProfileReadOnly
        )

        assertFalse(analysis.success)
        assertEquals(EsimSgp22SampleContext.ProfileReadOnly, analysis.commandContext)
    }

    @Test
    fun classifiesParseWarningBeforeStatusOnly() {
        val analysis = EsimSgp22SampleAnalyzer.analyze("E3 08 5A 06 12 34 90 00")

        assertTrue(analysis.success)
        assertEquals(EsimSgp22SampleKind.ParseWarning, analysis.classification?.kind)
        assertEquals(EsimSgp22SampleConfidence.High, analysis.classification?.confidence)
        assertEquals(true, analysis.privacyAudit?.hasParseWarning)
        assertTrue(analysis.privacyAudit?.summary.orEmpty().contains("解析存在异常"))
        assertTrue(analysis.message.contains("解析提示"))
    }

    @Test
    fun rejectsEmptySample() {
        val analysis = EsimSgp22SampleAnalyzer.analyze("   ")

        assertFalse(analysis.success)
        assertEquals("样本为空", analysis.title)
    }

    @Test
    fun rejectsOddLengthHex() {
        val analysis = EsimSgp22SampleAnalyzer.analyze("ABC")

        assertFalse(analysis.success)
        assertEquals("十六进制长度不完整", analysis.title)
    }

    @Test
    fun rejectsNonHexCharacters() {
        val analysis = EsimSgp22SampleAnalyzer.analyze("90 0Z")

        assertFalse(analysis.success)
        assertEquals("包含非十六进制字符", analysis.title)
    }
}
