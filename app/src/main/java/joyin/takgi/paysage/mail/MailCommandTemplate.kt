package joyin.takgi.paysage.mail

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

object MailCommandTemplate {
    const val KEY_PLACEHOLDER = "<paste-command-key>"
    const val WRONG_KEY = "wrong-key-for-smoke"

    fun keyCommand(
        action: MailCommandAction,
        key: String = KEY_PLACEHOLDER,
        expiresRaw: String = defaultExpiresRaw(),
        nonce: String = defaultNonce(action)
    ): String =
        """
        #paysage ${action.wireName}
        key: $key
        expires: $expiresRaw
        nonce: $nonce
        """.trimIndent()

    fun hmacCommand(
        action: MailCommandAction,
        secret: String,
        expiresRaw: String = defaultExpiresRaw(),
        nonce: String = defaultNonce(action)
    ): String {
        val command = MailCommandParser.parse(
            """
            #paysage ${action.wireName}
            expires: $expiresRaw
            nonce: $nonce
            """.trimIndent()
        ).getOrThrow()
        return """
        #paysage ${action.wireName}
        sig: ${MailCommandSignature.sign(command, secret)}
        expires: $expiresRaw
        nonce: $nonce
        """.trimIndent()
    }

    fun expiredCommand(
        action: MailCommandAction = MailCommandAction.Status,
        key: String = KEY_PLACEHOLDER,
        nonce: String = "expired-${System.currentTimeMillis()}"
    ): String =
        keyCommand(
            action = action,
            key = key,
            expiresRaw = "2020-01-01T00:00:00Z",
            nonce = nonce
        )

    fun wrongKeyCommand(
        action: MailCommandAction = MailCommandAction.Status,
        expiresRaw: String = defaultExpiresRaw(),
        nonce: String = defaultNonce(action)
    ): String =
        keyCommand(
            action = action,
            key = WRONG_KEY,
            expiresRaw = expiresRaw,
            nonce = nonce
        )

    fun replayPair(
        action: MailCommandAction = MailCommandAction.Status,
        key: String = KEY_PLACEHOLDER,
        nonce: String = defaultNonce(action)
    ): Pair<String, String> {
        val first = keyCommand(action = action, key = key, nonce = nonce)
        return first to first
    }

    fun defaultExpiresRaw(now: Instant = Instant.now()): String =
        now.plus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS).toString()

    fun defaultNonce(
        action: MailCommandAction,
        nowMillis: Long = System.currentTimeMillis(),
        uniqueSuffix: String = UUID.randomUUID().toString().take(12)
    ): String =
        "smoke-${action.wireName}-$nowMillis-$uniqueSuffix"
}
