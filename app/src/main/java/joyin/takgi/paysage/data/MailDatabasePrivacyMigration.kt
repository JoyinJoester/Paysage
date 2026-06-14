package joyin.takgi.paysage.data

import java.security.MessageDigest
import java.util.Locale

object MailDatabasePrivacyMigration {
    fun migrateNonceKey(oldNonceKey: String, sender: String, nonce: String): String =
        if (oldNonceKey.startsWith(HASH_PREFIX)) {
            oldNonceKey
        } else {
            stableHash("${normalizeEmail(sender)}\n$nonce")
        }

    fun migrateNonceSender(sender: String): String =
        senderFingerprint(sender)

    fun migrateNonceValue(nonce: String): String =
        if (nonce.startsWith(HASH_PREFIX)) {
            nonce
        } else {
            stableHash(nonce)
        }

    fun migrateRecordMessageKey(messageKey: String): String =
        if (messageKey.startsWith(HASH_PREFIX)) {
            messageKey
        } else {
            stableHash(messageKey)
        }

    fun migrateRecordSender(normalizedSender: String): String =
        senderFingerprint(normalizedSender)

    fun senderFingerprint(value: String): String =
        stableHash("mail-sender\n${normalizeEmail(value)}")

    fun stableHash(value: String): String =
        HASH_PREFIX + MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }

    private fun normalizeEmail(value: String): String =
        value.trim().lowercase(Locale.ROOT)

    private const val HASH_PREFIX = "sha256:"
}
