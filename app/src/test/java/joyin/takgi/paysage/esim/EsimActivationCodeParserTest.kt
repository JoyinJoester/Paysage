package joyin.takgi.paysage.esim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EsimActivationCodeParserTest {
    @Test
    fun parseLpaCodeWithPrefix() {
        val parsed = EsimActivationCodeParser.parse("LPA:1${'$'}smdp.example.com${'$'}MATCH-123${'$'}OID${'$'}1")

        assertTrue(parsed.isValid)
        assertEquals("1", parsed.formatVersion)
        assertEquals("smdp.example.com", parsed.smdpAddress)
        assertEquals("MATCH-123", parsed.matchingId)
        assertEquals("OID", parsed.oid)
        assertTrue(parsed.confirmationCodeRequired)
        assertEquals(null, parsed.confirmationCode)
        assertEquals("LPA:1${'$'}smdp.example.com${'$'}MATCH-123${'$'}OID${'$'}1", parsed.normalizedLpa)
    }

    @Test
    fun parseRawActivationCodeWithoutPrefix() {
        val parsed = EsimActivationCodeParser.parse("1${'$'}smdp.example.com${'$'}MATCH-123")

        assertTrue(parsed.isValid)
        assertEquals("LPA:1${'$'}smdp.example.com${'$'}MATCH-123", parsed.normalizedLpa)
    }

    @Test
    fun parseLpaPrefixCaseInsensitively() {
        val parsed = EsimActivationCodeParser.parse("Lpa:1${'$'}smdp.example.com${'$'}MATCH-123")

        assertTrue(parsed.isValid)
        assertEquals("LPA:1${'$'}smdp.example.com${'$'}MATCH-123", parsed.normalizedLpa)
    }

    @Test
    fun extractLpaCodeFromQrPayload() {
        val draft = EsimActivationCodeExtractor.extract("LPA:1${'$'}smdp.example.com${'$'}MATCH-123")

        assertTrue(draft.isValid)
        assertEquals("smdp.example.com", draft.activationCode?.smdpAddress)
        assertEquals("MATCH-123", draft.activationCode?.matchingId)
    }

    @Test
    fun extractLpaCodeFromCarrierUrlQuery() {
        val draft = EsimActivationCodeExtractor.extract(
            "https://carrier.example/activate?token=abc&code=LPA%3A1%24smdp.example.com%24MATCH-123"
        )

        assertTrue(draft.isValid)
        assertEquals("LPA:1${'$'}smdp.example.com${'$'}MATCH-123", draft.activationCode?.normalizedLpa)
    }

    @Test
    fun rejectQrPayloadWithoutLpaCode() {
        val draft = EsimActivationCodeExtractor.extract("https://carrier.example/help")

        assertFalse(draft.isValid)
        assertEquals(null, draft.activationCode)
    }

    @Test
    fun standaloneConfirmationCodeDoesNotRewriteActivationCode() {
        val parsed = EsimActivationCodeParser
            .parse("1${'$'}smdp.example.com${'$'}MATCH-123")
            .withStandaloneConfirmationCode(" CONF-456 ")

        assertEquals("CONF-456", parsed.confirmationCode)
        assertEquals("1${'$'}smdp.example.com${'$'}MATCH-123", parsed.encoded)
        assertEquals("LPA:1${'$'}smdp.example.com${'$'}MATCH-123", parsed.normalizedLpa)
    }

    @Test
    fun blankStandaloneConfirmationCodeDoesNotCreateConfirmationCode() {
        val parsed = EsimActivationCodeParser
            .parse("LPA:1${'$'}smdp.example.com${'$'}MATCH-123${'$'}OID")
            .withStandaloneConfirmationCode("   ")

        assertEquals(null, parsed.confirmationCode)
        assertEquals("OID", parsed.oid)
    }

    @Test
    fun composeManualCarrierFieldsIntoActivationCode() {
        val draft = EsimActivationCodeComposer.compose(
            smdpAddress = " https://smdp.example.com/ ",
            matchingId = " MATCH-123 "
        )

        assertTrue(draft.isValid)
        assertEquals("1${'$'}smdp.example.com${'$'}MATCH-123", draft.activationCode?.encoded)
        assertEquals("LPA:1${'$'}smdp.example.com${'$'}MATCH-123", draft.activationCode?.normalizedLpa)
        assertEquals("smdp.example.com", draft.activationCode?.smdpAddress)
        assertEquals("MATCH-123", draft.activationCode?.matchingId)
    }

    @Test
    fun composeManualCarrierFieldsAllowsBlankMatchingId() {
        val draft = EsimActivationCodeComposer.compose(
            smdpAddress = "smdp.example.com",
            matchingId = " "
        )

        assertTrue(draft.isValid)
        assertEquals("1${'$'}smdp.example.com", draft.activationCode?.encoded)
        assertEquals(null, draft.activationCode?.matchingId)
    }

    @Test
    fun composeManualCarrierFieldsAcceptsFullLpaCode() {
        val draft = EsimActivationCodeComposer.compose(
            smdpAddress = "LPA:1${'$'}smdp.example.com${'$'}MATCH-123",
            matchingId = ""
        )

        assertTrue(draft.isValid)
        assertEquals("smdp.example.com", draft.activationCode?.smdpAddress)
        assertEquals("MATCH-123", draft.activationCode?.matchingId)
    }

    @Test
    fun composeManualCarrierFieldsPreservesLpaMetadata() {
        val draft = EsimActivationCodeComposer.compose(
            smdpAddress = "smdp.example.com",
            matchingId = "MATCH-123",
            oid = "OID",
            confirmationCodeRequired = true,
            imei = "123456789012345"
        )

        assertTrue(draft.isValid)
        assertEquals("LPA:1${'$'}smdp.example.com${'$'}MATCH-123${'$'}OID${'$'}1", draft.activationCode?.normalizedLpa)
        assertEquals("OID", draft.activationCode?.oid)
        assertTrue(draft.activationCode?.confirmationCodeRequired == true)
        assertEquals("123456789012345", draft.activationCode?.imei)
    }

    @Test
    fun preflightPassesCompleteActivationCode() {
        val report = EsimActivationCodePreflight.analyze(
            activationCode = EsimActivationCodeParser
                .parse("1${'$'}smdp.example.com${'$'}MATCH-123${'$'}OID${'$'}1")
                .withStandaloneConfirmationCode("CONF"),
            standaloneConfirmationCodeProvided = true
        )

        assertFalse(report.hasErrors)
        assertTrue(report.checks.any { it.severity == EsimActivationCheckSeverity.Pass && it.title == "SM-DP+ 地址" })
    }

    @Test
    fun preflightBlocksMissingRequiredConfirmationCode() {
        val report = EsimActivationCodePreflight.analyze(
            activationCode = EsimActivationCodeParser.parse("1${'$'}smdp.example.com${'$'}MATCH-123${'$'}OID${'$'}1"),
            standaloneConfirmationCodeProvided = false
        )

        assertTrue(report.hasErrors)
        assertTrue(report.checks.any { it.severity == EsimActivationCheckSeverity.Error && it.title == "确认码必填" })
    }

    @Test
    fun preflightWarnsWhenMatchingIdIsMissing() {
        val report = EsimActivationCodePreflight.analyze(
            activationCode = EsimActivationCodeParser.parse("1${'$'}smdp.example.com"),
            standaloneConfirmationCodeProvided = false
        )

        assertFalse(report.hasErrors)
        assertTrue(report.checks.any { it.severity == EsimActivationCheckSeverity.Warning && it.title == "匹配 ID 为空" })
    }

    @Test
    fun preflightBlocksWhitespaceInCarrierFields() {
        val report = EsimActivationCodePreflight.analyze(
            activationCode = EsimActivationCodeParser.parse("1${'$'}sm dp.example.com${'$'}MATCH-123"),
            standaloneConfirmationCodeProvided = false
        )

        assertTrue(report.hasErrors)
        assertTrue(report.checks.any { it.severity == EsimActivationCheckSeverity.Error && it.title == "SM-DP+ 地址包含空格" })
    }

    @Test
    fun preflightWarnsAboutExtraParameters() {
        val report = EsimActivationCodePreflight.analyze(
            activationCode = EsimActivationCodeParser.parse("1${'$'}smdp.example.com${'$'}MATCH-123${'$'}OID${'$'}1${'$'}EXTRA")
                .withStandaloneConfirmationCode("CONF"),
            standaloneConfirmationCodeProvided = false
        )

        assertFalse(report.hasErrors)
        assertTrue(report.checks.any { it.severity == EsimActivationCheckSeverity.Warning && it.title == "包含附加参数" })
    }
}
