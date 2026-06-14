package joyin.takgi.paysage.mail

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import joyin.takgi.paysage.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.AuthenticationFailedException
import javax.mail.Address
import javax.mail.FetchProfile
import javax.mail.Folder
import javax.mail.MessagingException
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.Session

data class MailInboxAccountConfig(
    val host: String = "",
    val port: Int = 993,
    val username: String = "",
    val password: String = "",
    val useSsl: Boolean = true,
    val enabled: Boolean = false
) {
    val isConfigured: Boolean
        get() = host.isNotBlank() && username.isNotBlank() && password.isNotBlank() && port in 1..65535
}

enum class MailAccountIssue {
    MissingHost,
    InvalidPort,
    MissingUsername,
    MissingPassword
}

enum class MailConnectionStatus {
    Succeeded,
    InvalidConfig,
    AuthenticationFailed,
    NetworkOrServerFailed
}

data class MailConnectionTestResult(
    val status: MailConnectionStatus,
    val message: String
) {
    val success: Boolean
        get() = status == MailConnectionStatus.Succeeded
}

data class MailMessageSummary(
    val messageNumber: Int,
    val messageId: String = "",
    val from: String,
    val normalizedFrom: String?,
    val subject: String,
    val receivedAtMillis: Long,
    val seen: Boolean
)

data class MailFetchedMessage(
    val summary: MailMessageSummary,
    val bodyText: String?
)

object MailMessageAddressFormatter {
    fun fromHeader(addresses: Array<Address>?): String =
        addresses
            ?.joinToString(", ") { address -> address.toString() }
            .orEmpty()
}

object MailInboxAccountValidator {
    fun validate(config: MailInboxAccountConfig): List<MailAccountIssue> = buildList {
        if (config.host.isBlank()) add(MailAccountIssue.MissingHost)
        if (config.port !in 1..65535) add(MailAccountIssue.InvalidPort)
        if (config.username.isBlank()) add(MailAccountIssue.MissingUsername)
        if (config.password.isBlank()) add(MailAccountIssue.MissingPassword)
    }
}

class MailInboxAccountStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            appContext,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun read(): MailInboxAccountConfig =
        MailInboxAccountConfig(
            host = preferences.getString(KEY_HOST, "").orEmpty(),
            port = preferences.getInt(KEY_PORT, 993),
            username = preferences.getString(KEY_USERNAME, "").orEmpty(),
            password = preferences.getString(KEY_PASSWORD, "").orEmpty(),
            useSsl = preferences.getBoolean(KEY_USE_SSL, true),
            enabled = preferences.getBoolean(KEY_ENABLED, false)
        )

    fun write(config: MailInboxAccountConfig) {
        preferences.edit()
            .putString(KEY_HOST, config.host.trim())
            .putInt(KEY_PORT, config.port)
            .putString(KEY_USERNAME, config.username.trim())
            .putString(KEY_PASSWORD, config.password)
            .putBoolean(KEY_USE_SSL, config.useSsl)
            .putBoolean(KEY_ENABLED, config.enabled)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "paysage_mail_inbox_account"
        private const val KEY_HOST = "host"
        private const val KEY_PORT = "port"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_USE_SSL = "use_ssl"
        private const val KEY_ENABLED = "enabled"
    }
}

class MailImapClient(
    private val config: MailInboxAccountConfig,
    context: Context? = null
) {
    private val appContext = context?.applicationContext

    suspend fun testConnection(): MailConnectionTestResult = withContext(Dispatchers.IO) {
        val issues = MailInboxAccountValidator.validate(config)
        if (issues.isNotEmpty()) {
            return@withContext MailConnectionTestResult(
                status = MailConnectionStatus.InvalidConfig,
                message = text(R.string.message_mail_config_incomplete_sentence)
            )
        }

        val protocol = if (config.useSsl) "imaps" else "imap"
        val props = Properties().apply {
            put("mail.store.protocol", protocol)
            put("mail.$protocol.host", config.host)
            put("mail.$protocol.port", config.port.toString())
            put("mail.$protocol.connectiontimeout", CONNECTION_TIMEOUT_MS.toString())
            put("mail.$protocol.timeout", READ_TIMEOUT_MS.toString())
            if (config.useSsl) {
                put("mail.imaps.ssl.enable", "true")
                put("mail.imaps.ssl.protocols", "TLSv1.2 TLSv1.3")
            }
        }

        runCatching {
            val session = Session.getInstance(props)
            val store = session.getStore(protocol)
            store.use {
                it.connect(config.host, config.port, config.username, config.password)
            }
        }.fold(
            onSuccess = {
                MailConnectionTestResult(
                    status = MailConnectionStatus.Succeeded,
                    message = text(R.string.message_mail_connection_success)
                )
            },
            onFailure = { error ->
                when (error) {
                    is AuthenticationFailedException -> MailConnectionTestResult(
                        status = MailConnectionStatus.AuthenticationFailed,
                        message = text(R.string.message_mail_auth_failed_check_credentials)
                    )
                    is MessagingException -> MailConnectionTestResult(
                        status = MailConnectionStatus.NetworkOrServerFailed,
                        message = text(R.string.message_mail_server_connect_failed)
                    )
                    else -> MailConnectionTestResult(
                        status = MailConnectionStatus.NetworkOrServerFailed,
                        message = text(R.string.message_mail_connection_test_failed)
                    )
                }
            }
        )
    }

    suspend fun fetchRecentSummaries(limit: Int = DEFAULT_FETCH_LIMIT): Result<List<MailMessageSummary>> =
        fetchRecentMessages(limit = limit) { false }.map { messages ->
            messages.map { it.summary }
        }

    suspend fun fetchRecentMessages(
        limit: Int = DEFAULT_FETCH_LIMIT,
        shouldFetchBody: (MailMessageSummary) -> Boolean
    ): Result<List<MailFetchedMessage>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val issues = MailInboxAccountValidator.validate(config)
                require(issues.isEmpty()) { text(R.string.message_mail_config_incomplete_sentence) }

                val protocol = if (config.useSsl) "imaps" else "imap"
                val session = Session.getInstance(buildProperties(protocol))
                val store = session.getStore(protocol)
                var inbox: Folder? = null
                try {
                    store.connect(config.host, config.port, config.username, config.password)
                    inbox = store.getFolder("INBOX")
                    inbox.open(Folder.READ_ONLY)

                    val count = inbox.messageCount
                    if (count <= 0) {
                        emptyList()
                    } else {
                        val boundedLimit = limit.coerceIn(1, MAX_FETCH_LIMIT)
                        val start = (count - boundedLimit + 1).coerceAtLeast(1)
                        val messages = inbox.getMessages(start, count)
                        inbox.fetch(
                            messages,
                            FetchProfile().apply {
                                add(FetchProfile.Item.ENVELOPE)
                                add(FetchProfile.Item.FLAGS)
                                add("Message-ID")
                            }
                        )
                        messages
                            .asSequence()
                            .map { message ->
                                val from = MailMessageAddressFormatter.fromHeader(message.from)
                                val summary = MailMessageSummary(
                                    messageNumber = message.messageNumber,
                                    messageId = message.getHeader("Message-ID")
                                        ?.firstOrNull()
                                        .orEmpty()
                                        .trim()
                                        .take(MAX_MESSAGE_ID_CHARS),
                                    from = from,
                                    normalizedFrom = MailAddressNormalizer.normalize(from),
                                    subject = message.subject.orEmpty(),
                                    receivedAtMillis = (
                                        message.receivedDate
                                            ?: message.sentDate
                                    )?.time ?: 0L,
                                    seen = message.flags?.contains(javax.mail.Flags.Flag.SEEN) == true
                                )
                                MailFetchedMessage(
                                    summary = summary,
                                    bodyText = if (shouldFetchBody(summary)) {
                                        extractText(message, MAX_BODY_CHARS).ifBlank { null }
                                    } else {
                                        null
                                    }
                                )
                            }
                            .toList()
                            .asReversed()
                    }
                } finally {
                    runCatching { inbox?.takeIf { it.isOpen }?.close(false) }
                    runCatching { store.close() }
                }
            }
        }

    private fun extractText(part: Part, maxChars: Int): String {
        if (maxChars <= 0) return ""
        return when {
            part.isMimeType("text/plain") -> {
                part.content?.toString().orEmpty().take(maxChars)
            }
            part.isMimeType("text/html") -> {
                part.content?.toString()
                    .orEmpty()
                    .replace(Regex("<[^>]+>"), " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .take(maxChars)
            }
            part.isMimeType("multipart/*") -> {
                val multipart = part.content as? Multipart ?: return ""
                buildString {
                    for (index in 0 until multipart.count) {
                        if (length >= maxChars) break
                        append(extractText(multipart.getBodyPart(index), maxChars - length))
                        if (length < maxChars) append('\n')
                    }
                }.take(maxChars)
            }
            else -> ""
        }
    }

    private fun buildProperties(protocol: String): Properties =
        Properties().apply {
            put("mail.store.protocol", protocol)
            put("mail.$protocol.host", config.host)
            put("mail.$protocol.port", config.port.toString())
            put("mail.$protocol.connectiontimeout", CONNECTION_TIMEOUT_MS.toString())
            put("mail.$protocol.timeout", READ_TIMEOUT_MS.toString())
            if (config.useSsl) {
                put("mail.imaps.ssl.enable", "true")
                put("mail.imaps.ssl.protocols", "TLSv1.2 TLSv1.3")
            }
        }

    private fun text(resId: Int): String =
        appContext?.getString(resId) ?: when (resId) {
            R.string.message_mail_config_incomplete_sentence -> "Mailbox configuration incomplete."
            R.string.message_mail_connection_success -> "Mailbox connected successfully."
            R.string.message_mail_auth_failed_check_credentials -> "Mailbox login failed. Check username and app password."
            R.string.message_mail_server_connect_failed -> "Cannot connect to mailbox server. Check IMAP address, port, and network."
            R.string.message_mail_connection_test_failed -> "Mailbox connection test failed."
            else -> ""
        }

    companion object {
        private const val CONNECTION_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 15_000
        private const val DEFAULT_FETCH_LIMIT = 20
        private const val MAX_FETCH_LIMIT = 50
        private const val MAX_BODY_CHARS = 12_000
        private const val MAX_MESSAGE_ID_CHARS = 512
    }
}
