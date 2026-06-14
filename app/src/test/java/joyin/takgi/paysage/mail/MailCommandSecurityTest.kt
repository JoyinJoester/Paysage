package joyin.takgi.paysage.mail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MailCommandSecurityTest {
    private val future = "2026-06-13T00:10:00Z"
    private val now = Instant.parse("2026-06-13T00:00:00Z").toEpochMilli()

    @Test
    fun normalizesDisplayNameEmailAddress() {
        assertEquals(
            "owner@example.com",
            MailAddressNormalizer.normalize("\"Owner\" <OWNER@Example.COM>")
        )
    }

    @Test
    fun normalizesQuotedDisplayNameContainingComma() {
        assertEquals(
            "owner@example.com",
            MailAddressNormalizer.normalize("\"Owner, Team\" <OWNER@Example.COM>")
        )
    }

    @Test
    fun rejectsSenderHeaderWithMultipleAddresses() {
        assertEquals(
            null,
            MailAddressNormalizer.normalize("owner@example.com, intruder@example.com")
        )
    }

    @Test
    fun rejectsInvalidEmailAddress() {
        assertEquals(null, MailAddressNormalizer.normalize("not an email"))
    }

    @Test
    fun parsesCommandBlockWithKey() {
        val command = parse(
            """
            random text
            #paysage retry-cache
            key: secret-123
            expires: $future
            nonce: abc
            """.trimIndent()
        )

        assertEquals(MailCommandAction.RetryCache, command.action)
        assertEquals("secret-123", command.key)
        assertEquals("abc", command.nonce)
        assertTrue(command.expiresAtEpochMillis > now)
    }

    @Test
    fun parsesCommandPrefixCaseInsensitively() {
        val command = parse(
            """
            #Paysage status
            key: secret-123
            expires: $future
            nonce: n1
            """.trimIndent()
        )

        assertEquals(MailCommandAction.Status, command.action)
    }

    @Test
    fun allowsTrailingMailTextAfterBlankLine() {
        val command = parse(
            """
            #paysage status
            key: secret-123
            expires: $future
            nonce: n1

            From: previous@example.com
            这段是邮件备注，不属于指令块。
            """.trimIndent()
        )

        assertEquals(MailCommandAction.Status, command.action)
        assertEquals("secret-123", command.key)
        assertEquals("n1", command.nonce)
    }

    @Test
    fun rejectsCommandLineWithExtraTokens() {
        val result = MailCommandParser.parse(
            """
            #paysage status please
            key: secret-123
            expires: $future
            nonce: n1
            """.trimIndent()
        )

        val error = result.exceptionOrNull() as MailCommandParseException
        assertEquals(MailCommandDecisionCode.InvalidCommand, error.code)
        assertTrue(error.message.contains("格式"))
    }

    @Test
    fun similarHashtagPrefixIsNotTreatedAsCommand() {
        val result = MailCommandParser.parse(
            """
            #paysagex status
            key: secret-123
            expires: $future
            nonce: n1
            """.trimIndent()
        )

        val error = result.exceptionOrNull() as MailCommandParseException
        assertEquals(MailCommandDecisionCode.NoCommand, error.code)
    }

    @Test
    fun rejectsMultipleCommandBlocksInOneMessage() {
        val result = MailCommandParser.parse(
            """
            #paysage status
            key: secret-123
            expires: $future
            nonce: n1
            #paysage retry-cache
            key: secret-123
            expires: $future
            nonce: n2
            """.trimIndent()
        )

        val error = result.exceptionOrNull() as MailCommandParseException
        assertEquals(MailCommandDecisionCode.InvalidCommand, error.code)
        assertTrue(error.message.contains("只能包含一条"))
    }

    @Test
    fun allowsWhitelistedSenderWithCorrectKeyAndPermission() {
        val command = parseCommandWithKey(MailCommandAction.Status, key = "secret-123", nonce = "n1")
        val decision = MailCommandSecurityPolicy.evaluate(
            rawSender = "\"Owner\" <owner@example.com>",
            command = command,
            trustedSenders = listOf(
                TrustedMailSender(
                    email = "OWNER@example.com",
                    allowedActions = setOf(MailCommandAction.Status),
                    secret = "secret-123"
                )
            ),
            usedNonces = emptySet(),
            nowEpochMillis = now
        )

        assertTrue(decision.allowed)
        assertEquals(MailCommandDecisionCode.Allowed, decision.code)
    }

    @Test
    fun rejectsNonWhitelistedSenderBeforeCommandTrust() {
        val command = parseCommandWithKey(MailCommandAction.Status, key = "secret-123", nonce = "n1")
        val decision = MailCommandSecurityPolicy.evaluate(
            rawSender = "intruder@example.com",
            command = command,
            trustedSenders = listOf(
                TrustedMailSender(
                    email = "owner@example.com",
                    allowedActions = setOf(MailCommandAction.Status),
                    secret = "secret-123"
                )
            ),
            usedNonces = emptySet(),
            nowEpochMillis = now
        )

        assertFalse(decision.allowed)
        assertEquals(MailCommandDecisionCode.SenderNotWhitelisted, decision.code)
    }

    @Test
    fun rejectsAmbiguousMultipleSenderAddressesBeforeCommandTrust() {
        val command = parseCommandWithKey(MailCommandAction.Status, key = "secret-123", nonce = "n1")
        val decision = MailCommandSecurityPolicy.evaluate(
            rawSender = "owner@example.com, intruder@example.com",
            command = command,
            trustedSenders = ownerSender(),
            usedNonces = emptySet(),
            nowEpochMillis = now
        )

        assertFalse(decision.allowed)
        assertEquals(MailCommandDecisionCode.InvalidSender, decision.code)
    }

    @Test
    fun rejectsWrongKey() {
        val command = parseCommandWithKey(MailCommandAction.Status, key = "wrong", nonce = "n1")
        val decision = evaluateOwner(command)

        assertFalse(decision.allowed)
        assertEquals(MailCommandDecisionCode.InvalidAuthenticator, decision.code)
    }

    @Test
    fun rejectsMissingAuthenticator() {
        val command = parse(
            """
            #paysage status
            expires: $future
            nonce: n1
            """.trimIndent()
        )
        val decision = evaluateOwner(command)

        assertFalse(decision.allowed)
        assertEquals(MailCommandDecisionCode.MissingAuthenticator, decision.code)
    }

    @Test
    fun rejectsTrustedSenderWithBlankSecret() {
        val command = parse(
            """
            #paysage status
            expires: $future
            nonce: n1
            sig: sha256=${"0".repeat(64)}
            """.trimIndent()
        )
        val decision = MailCommandSecurityPolicy.evaluate(
            rawSender = "owner@example.com",
            command = command,
            trustedSenders = listOf(
                TrustedMailSender(
                    email = "owner@example.com",
                    allowedActions = setOf(MailCommandAction.Status),
                    secret = ""
                )
            ),
            usedNonces = emptySet(),
            nowEpochMillis = now
        )

        assertFalse(decision.allowed)
        assertEquals(MailCommandDecisionCode.InvalidAuthenticator, decision.code)
    }

    @Test
    fun rejectsDuplicateCommandFields() {
        val result = MailCommandParser.parse(
            """
            #paysage status
            key: visible-key
            key: hidden-key
            expires: $future
            nonce: n1
            """.trimIndent()
        )

        val error = result.exceptionOrNull() as MailCommandParseException
        assertEquals(MailCommandDecisionCode.InvalidCommand, error.code)
        assertTrue(error.message.contains("重复"))
    }

    @Test
    fun rejectsUnsupportedCommandFields() {
        val result = MailCommandParser.parse(
            """
            #paysage status
            key: secret-123
            expires: $future
            nonce: n1
            debug: should-not-be-accepted
            """.trimIndent()
        )

        val error = result.exceptionOrNull() as MailCommandParseException
        assertEquals(MailCommandDecisionCode.InvalidCommand, error.code)
        assertTrue(error.message.contains("不支持"))
    }

    @Test
    fun parseErrorsDoNotEchoUnsupportedFieldNames() {
        val result = MailCommandParser.parse(
            """
            #paysage status
            key: secret-123
            expires: $future
            nonce: n1
            owner@example.com: app-password-secret
            """.trimIndent()
        )

        val error = result.exceptionOrNull() as MailCommandParseException
        assertEquals(MailCommandDecisionCode.InvalidCommand, error.code)
        assertTrue(error.message.contains("不支持"))
        assertFalse(error.message.contains("owner@example.com"))
        assertFalse(error.message.contains("app-password-secret"))
    }

    @Test
    fun rejectsBlankCommandFields() {
        val result = MailCommandParser.parse(
            """
            #paysage status
            key:
            expires: $future
            nonce: n1
            """.trimIndent()
        )

        val error = result.exceptionOrNull() as MailCommandParseException
        assertEquals(MailCommandDecisionCode.InvalidCommand, error.code)
        assertTrue(error.message.contains("不能为空"))
    }

    @Test
    fun rejectsOverlongCommandFields() {
        val result = MailCommandParser.parse(
            """
            #paysage status
            key: secret-123
            expires: $future
            nonce: ${"n".repeat(129)}
            """.trimIndent()
        )

        val error = result.exceptionOrNull() as MailCommandParseException
        assertEquals(MailCommandDecisionCode.InvalidCommand, error.code)
        assertTrue(error.message.contains("过长"))
    }

    @Test
    fun rejectsControlCharactersInCommandFields() {
        val result = MailCommandParser.parse(
            """
            #paysage status
            key: secret${'\u0001'}123
            expires: $future
            nonce: n1
            """.trimIndent()
        )

        val error = result.exceptionOrNull() as MailCommandParseException
        assertEquals(MailCommandDecisionCode.InvalidCommand, error.code)
        assertTrue(error.message.contains("控制字符"))
    }

    @Test
    fun rejectsMalformedSignatureField() {
        val result = MailCommandParser.parse(
            """
            #paysage status
            sig: sha256=not-hex
            expires: $future
            nonce: n1
            """.trimIndent()
        )

        val error = result.exceptionOrNull() as MailCommandParseException
        assertEquals(MailCommandDecisionCode.InvalidCommand, error.code)
        assertTrue(error.message.contains("签名格式"))
    }

    @Test
    fun rejectsActionOutsideSenderPermission() {
        val command = parseCommandWithKey(MailCommandAction.RetryCache, key = "secret-123", nonce = "n1")
        val decision = evaluateOwner(command)

        assertFalse(decision.allowed)
        assertEquals(MailCommandDecisionCode.ActionNotAllowed, decision.code)
    }

    @Test
    fun rejectsExpiredCommand() {
        val command = parse(
            """
            #paysage status
            key: secret-123
            expires: 2020-06-13T12:30:00+08:00
            nonce: n1
            """.trimIndent()
        )
        val decision = evaluateOwner(command)

        assertFalse(decision.allowed)
        assertEquals(MailCommandDecisionCode.Expired, decision.code)
    }

    @Test
    fun rejectsCommandExpiryTooFarInFuture() {
        val command = parse(
            """
            #paysage status
            key: secret-123
            expires: 2026-06-13T00:31:00Z
            nonce: n1
            """.trimIndent()
        )
        val decision = evaluateOwner(command)

        assertFalse(decision.allowed)
        assertEquals(MailCommandDecisionCode.ExpiresTooFar, decision.code)
    }

    @Test
    fun rejectsReusedNonce() {
        val command = parseCommandWithKey(MailCommandAction.Status, key = "secret-123", nonce = "n1")
        val decision = MailCommandSecurityPolicy.evaluate(
            rawSender = "owner@example.com",
            command = command,
            trustedSenders = ownerSender(),
            usedNonces = setOf(MailCommandSecurityPolicy.nonceKey("owner@example.com", "n1")),
            nowEpochMillis = now
        )

        assertFalse(decision.allowed)
        assertEquals(MailCommandDecisionCode.NonceReused, decision.code)
    }

    @Test
    fun rejectsLegacyPlaintextNonceKeyFromOlderBuilds() {
        val command = parseCommandWithKey(MailCommandAction.Status, key = "secret-123", nonce = "n1")
        val decision = MailCommandSecurityPolicy.evaluate(
            rawSender = "owner@example.com",
            command = command,
            trustedSenders = ownerSender(),
            usedNonces = setOf("owner@example.com::n1"),
            nowEpochMillis = now
        )

        assertFalse(decision.allowed)
        assertEquals(MailCommandDecisionCode.NonceReused, decision.code)
    }

    @Test
    fun nonceStorageIdentifiersDoNotExposeRawSenderOrNonce() {
        val nonceKey = MailCommandSecurityPolicy.nonceKey("Owner@example.com", "nonce-secret-123")
        val nonceForStorage = MailCommandSecurityPolicy.nonceForStorage("nonce-secret-123")

        assertTrue(nonceKey.startsWith("sha256:"))
        assertTrue(nonceForStorage.startsWith("sha256:"))
        assertFalse(nonceKey.contains("owner@example.com"))
        assertFalse(nonceKey.contains("nonce-secret-123"))
        assertFalse(nonceForStorage.contains("nonce-secret-123"))
    }

    @Test
    fun allowsWhitelistedSenderWithHmacSignature() {
        val commandWithoutSig = parse(
            """
            #paysage status
            expires: $future
            nonce: n1
            """.trimIndent()
        )
        val signed = parse(
            """
            #paysage status
            expires: $future
            nonce: n1
            sig: ${MailCommandSignature.sign(commandWithoutSig, "secret-123")}
            """.trimIndent()
        )
        val decision = evaluateOwner(signed)

        assertTrue(decision.allowed)
        assertEquals(MailCommandDecisionCode.Allowed, decision.code)
    }

    @Test
    fun rejectsCommandWithBothKeyAndSignature() {
        val signature = validStatusSignature(nonce = "n1")
        val command = parse(
            """
            #paysage status
            key: secret-123
            expires: $future
            nonce: n1
            sig: $signature
            """.trimIndent()
        )
        val decision = evaluateOwner(command)

        assertFalse(decision.allowed)
        assertEquals(MailCommandDecisionCode.InvalidAuthenticator, decision.code)
    }

    @Test
    fun rejectsCommandWithWrongKeyEvenWhenSignatureIsCorrect() {
        val signature = validStatusSignature(nonce = "n2")
        val command = parse(
            """
            #paysage status
            key: wrong
            expires: $future
            nonce: n2
            sig: $signature
            """.trimIndent()
        )
        val decision = evaluateOwner(command)

        assertFalse(decision.allowed)
        assertEquals(MailCommandDecisionCode.InvalidAuthenticator, decision.code)
    }

    @Test
    fun rejectsCommandWithCorrectKeyEvenWhenSignatureIsWrong() {
        val signature = validStatusSignature(nonce = "n3").replaceLastHexDigit()
        val command = parse(
            """
            #paysage status
            key: secret-123
            expires: $future
            nonce: n3
            sig: $signature
            """.trimIndent()
        )
        val decision = evaluateOwner(command)

        assertFalse(decision.allowed)
        assertEquals(MailCommandDecisionCode.InvalidAuthenticator, decision.code)
    }

    private fun parseCommandWithKey(
        action: MailCommandAction,
        key: String,
        nonce: String
    ): ParsedMailCommand =
        parse(
            """
            #paysage ${action.wireName}
            key: $key
            expires: $future
            nonce: $nonce
            """.trimIndent()
        )

    private fun validStatusSignature(nonce: String): String {
        val unsigned = parse(
            """
            #paysage status
            expires: $future
            nonce: $nonce
            """.trimIndent()
        )
        return MailCommandSignature.sign(unsigned, "secret-123")
    }

    private fun String.replaceLastHexDigit(): String {
        val last = last()
        val replacement = if (last == '0') '1' else '0'
        return dropLast(1) + replacement
    }

    private fun parse(body: String): ParsedMailCommand =
        MailCommandParser.parse(body).getOrThrow()

    private fun evaluateOwner(command: ParsedMailCommand): MailCommandDecision =
        MailCommandSecurityPolicy.evaluate(
            rawSender = "owner@example.com",
            command = command,
            trustedSenders = ownerSender(),
            usedNonces = emptySet(),
            nowEpochMillis = now
        )

    private fun ownerSender(): List<TrustedMailSender> =
        listOf(
            TrustedMailSender(
                email = "owner@example.com",
                allowedActions = setOf(MailCommandAction.Status),
                secret = "secret-123"
            )
        )
}
