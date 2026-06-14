package joyin.takgi.paysage.mail

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import joyin.takgi.paysage.R
import joyin.takgi.paysage.data.AppDatabase
import joyin.takgi.paysage.data.MailCommandNonceEntity
import joyin.takgi.paysage.data.MailCommandRecordEntity
import joyin.takgi.paysage.data.MailTrustedSenderEntity
import joyin.takgi.paysage.reliability.SmsForwardingControlStore
import joyin.takgi.paysage.reliability.SmsReliabilityManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

data class MailInboxRefreshResult(
    val success: Boolean,
    val message: String,
    val failureKind: MailInboxFailureKind = MailInboxFailureKind.None,
    val fetched: Int = 0,
    val executed: Int = 0,
    val ignored: Int = 0,
    val rejected: Int = 0
)

object MailInboxRefreshConcurrencyGuard {
    private val mutex = Mutex()

    suspend fun <T> runSerialized(block: suspend () -> T): T =
        mutex.withLock {
            block()
        }
}

class MailInboxRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.getDatabase(appContext)
    private val accountStore = MailInboxAccountStore(appContext)
    private val secretStore = MailTrustedSenderSecretStore(appContext)
    private val runtimeStore = MailInboxRuntimeStore(appContext)
    private val forwardingControlStore = SmsForwardingControlStore(appContext)
    private val senderDao = database.mailTrustedSenderDao()
    private val nonceDao = database.mailCommandNonceDao()
    private val recordDao = database.mailCommandRecordDao()

    fun observeTrustedSenders(): Flow<List<MailTrustedSenderEntity>> = senderDao.observeAll()

    fun observeRecentRecords(limit: Int = 50): Flow<List<MailCommandRecordEntity>> =
        recordDao.observeRecent(limit)

    fun readAccount(): MailInboxAccountConfig = accountStore.read()

    fun readRuntimeStatus(): MailInboxRuntimeStatus = runtimeStore.read()

    fun isForwardingPaused(): Boolean = forwardingControlStore.isPaused()

    suspend fun saveAccount(config: MailInboxAccountConfig) {
        val normalizedConfig = config.copy(
            host = config.host.trim(),
            username = config.username.trim()
        )
        accountStore.write(
            normalizedConfig
        )
        MailInboxReliabilityManager.ensureScheduled(appContext)
        if (!normalizedConfig.enabled || !normalizedConfig.isConfigured) {
            MailInboxIdleService.stop(appContext)
        }
    }

    suspend fun testConnection(config: MailInboxAccountConfig): MailConnectionTestResult =
        MailImapClient(config, appContext).testConnection()

    suspend fun saveTrustedSender(
        email: String,
        allowedActions: Set<MailCommandAction>,
        secret: String,
        enabled: Boolean = true
    ): Result<MailTrustedSenderEntity> = runCatching {
        val normalizedEmail = MailAddressNormalizer.normalize(email)
            ?: throw IllegalArgumentException(appContext.getString(R.string.message_invalid_trusted_sender_email))
        val cleanSecret = secret.trim()
        require(cleanSecret.length >= MIN_SECRET_LENGTH) {
            appContext.getString(R.string.format_command_secret_min_length, MIN_SECRET_LENGTH)
        }
        val now = System.currentTimeMillis()
        val actions = allowedActions.ifEmpty { setOf(MailCommandAction.Status) }
        val existing = senderDao.findByEmail(normalizedEmail)
        val entity = if (existing == null) {
            MailTrustedSenderEntity(
                email = normalizedEmail,
                allowedActions = actions.toStorageString(),
                enabled = enabled,
                createdAt = now,
                updatedAt = now
            ).also { inserted ->
                val id = senderDao.insert(inserted).toInt()
                secretStore.write(normalizedEmail, cleanSecret)
                return@runCatching inserted.copy(id = id)
            }
        } else {
            existing.copy(
                allowedActions = actions.toStorageString(),
                enabled = enabled,
                updatedAt = now
            ).also { updated ->
                senderDao.update(updated)
                secretStore.write(normalizedEmail, cleanSecret)
            }
        }
        entity
    }

    suspend fun setSenderEnabled(sender: MailTrustedSenderEntity, enabled: Boolean) {
        senderDao.update(
            sender.copy(
                enabled = enabled,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun rotateSenderSecret(sender: MailTrustedSenderEntity): String {
        val secret = generateSecret()
        secretStore.write(sender.email, secret)
        senderDao.update(sender.copy(updatedAt = System.currentTimeMillis()))
        return secret
    }

    suspend fun deleteTrustedSender(sender: MailTrustedSenderEntity) {
        senderDao.delete(sender)
        secretStore.remove(sender.email)
    }

    suspend fun dryRunCommand(
        rawSender: String,
        body: String,
        nowEpochMillis: Long = System.currentTimeMillis()
    ): MailCommandDryRunResult {
        val trustedSenders = senderDao.getAllOnce().map { entity ->
            TrustedMailSender(
                email = entity.email,
                allowedActions = entity.allowedActionSet(),
                secret = secretStore.read(entity.email).orEmpty(),
                enabled = entity.enabled
            )
        }
        return MailCommandDryRun.evaluate(
            rawSender = rawSender,
            body = body,
            trustedSenders = trustedSenders,
            usedNonces = nonceDao.usedNonceKeys().toSet(),
            nowEpochMillis = nowEpochMillis,
            context = appContext
        )
    }

    suspend fun refreshInbox(limit: Int = DEFAULT_REFRESH_LIMIT): MailInboxRefreshResult =
        MailInboxRefreshConcurrencyGuard.runSerialized {
            refreshInboxLocked(limit)
        }

    private suspend fun refreshInboxLocked(limit: Int = DEFAULT_REFRESH_LIMIT): MailInboxRefreshResult {
        val config = accountStore.read()
        if (!config.enabled) {
            runtimeStore.recordFailure(
                kind = MailInboxFailureKind.Disabled,
                message = appContext.getString(R.string.message_mail_inbox_disabled)
            )
            return MailInboxRefreshResult(
                success = false,
                message = appContext.getString(R.string.message_mail_inbox_disabled),
                failureKind = MailInboxFailureKind.Disabled
            )
        }
        if (!config.isConfigured) {
            runtimeStore.recordFailure(
                kind = MailInboxFailureKind.InvalidConfig,
                message = appContext.getString(R.string.message_mail_config_incomplete_sentence)
            )
            return MailInboxRefreshResult(
                success = false,
                message = appContext.getString(R.string.message_mail_config_incomplete_sentence),
                failureKind = MailInboxFailureKind.InvalidConfig
            )
        }

        val trustedEntities = senderDao.getAllOnce()
        val trustedByEmail = trustedEntities.associateBy { it.email }
        val trustedSenders = trustedEntities.mapNotNull { entity ->
            val secret = secretStore.read(entity.email)
            if (secret.isNullOrBlank()) {
                null
            } else {
                TrustedMailSender(
                    email = entity.email,
                    allowedActions = entity.allowedActionSet(),
                    secret = secret,
                    enabled = entity.enabled
                )
            }
        }

        val fetchedMessages = MailImapClient(config, appContext).fetchRecentMessages(limit) { summary ->
            summary.normalizedFrom?.let { normalized ->
                trustedByEmail[normalized]?.enabled == true
            } == true
        }.getOrElse { error ->
            val failureKind = classifyFetchFailure(error)
            val message = error.message
                ?.takeIf { it.isNotBlank() }
                ?.let { MailInboxPrivacySanitizer.redact(it) }
                ?: appContext.getString(R.string.message_mail_refresh_failed)
            runtimeStore.recordFailure(
                kind = failureKind,
                message = message
            )
            return MailInboxRefreshResult(
                success = false,
                message = message,
                failureKind = failureKind
            )
        }

        val candidateMessageKeys = fetchedMessages
            .flatMap { MailMessageIdentity.lookupKeys(it.summary) }
            .distinct()
        val existingMessageKeys = candidateMessageKeys
            .takeIf { it.isNotEmpty() }
            ?.let { recordDao.findExistingMessageKeys(it).toSet() }
            .orEmpty()
        val newMessages = fetchedMessages.filterNot { summary ->
            MailMessageIdentity.lookupKeys(summary.summary).any { it in existingMessageKeys }
        }

        val now = System.currentTimeMillis()
        nonceDao.deleteExpired(now - NONCE_RETENTION_AFTER_EXPIRY_MS)
        recordDao.deleteOlderThan(MailCommandRecordRetention.cutoffMillis(now))
        val usedNonces = nonceDao.usedNonceKeys().toMutableSet()
        var executed = 0
        var ignored = 0
        var rejected = 0

        newMessages.forEach { message ->
            val outcome = processFetchedMessage(
                message = message,
                trustedEntities = trustedByEmail,
                trustedSenders = trustedSenders,
                usedNonces = usedNonces,
                now = now
            )
            if (outcome.executed) {
                executed += 1
            } else {
                ignored += 1
            }
            if (outcome.securityRejected) {
                rejected += 1
            }
            recordDao.insert(outcome.record)
        }

        val resultMessage = appContext.getString(
            R.string.format_mail_refresh_success,
            fetchedMessages.size,
            newMessages.size,
            executed,
            ignored
        )
        runtimeStore.recordSuccess(
            message = resultMessage,
            fetched = fetchedMessages.size,
            executed = executed,
            ignored = ignored,
            rejected = rejected
        )
        return MailInboxRefreshResult(
            success = true,
            message = resultMessage,
            fetched = fetchedMessages.size,
            executed = executed,
            ignored = ignored,
            rejected = rejected
        )
    }

    private suspend fun processFetchedMessage(
        message: MailFetchedMessage,
        trustedEntities: Map<String, MailTrustedSenderEntity>,
        trustedSenders: List<TrustedMailSender>,
        usedNonces: MutableSet<String>,
        now: Long
    ): ProcessedMailMessage {
        val summary = message.summary
        val normalizedFrom = summary.normalizedFrom.orEmpty()
        val matchedEntity = trustedEntities[normalizedFrom]
        val commandParseResult = if (matchedEntity?.enabled == true) {
            message.bodyText?.let { MailCommandParser.parse(it, appContext) }
                ?: Result.failure(
                    MailCommandParseException(
                        MailCommandDecisionCode.NoCommand,
                        appContext.getString(R.string.message_mail_no_paysage_command)
                    )
                )
        } else {
            null
        }

        val command = commandParseResult?.getOrNull()
        val decision = when {
            matchedEntity == null -> MailCommandDecision(
                allowed = false,
                code = MailCommandDecisionCode.SenderNotWhitelisted,
                message = appContext.getString(R.string.message_mail_sender_not_whitelisted)
            )
            !matchedEntity.enabled -> MailCommandDecision(
                allowed = false,
                code = MailCommandDecisionCode.SenderDisabled,
                message = appContext.getString(R.string.message_mail_sender_disabled)
            )
            secretStore.read(matchedEntity.email).isNullOrBlank() -> MailCommandDecision(
                allowed = false,
                code = MailCommandDecisionCode.InvalidAuthenticator,
                message = appContext.getString(R.string.message_mail_sender_missing_secret)
            )
            commandParseResult?.exceptionOrNull() is MailCommandParseException -> {
                val error = commandParseResult.exceptionOrNull() as MailCommandParseException
                MailCommandDecision(
                    allowed = false,
                    code = error.code,
                    message = error.message
                )
            }
            command == null -> MailCommandDecision(
                allowed = false,
                code = MailCommandDecisionCode.NoCommand,
                message = appContext.getString(R.string.message_mail_no_paysage_command)
            )
            else -> MailCommandSecurityPolicy.evaluate(
                rawSender = summary.from,
                command = command,
                trustedSenders = trustedSenders,
                usedNonces = usedNonces,
                nowEpochMillis = now,
                context = appContext
            )
        }

        var executed = false
        var resultMessage = decision.message
        if (decision.allowed && command != null) {
            val nonceKey = MailCommandSecurityPolicy.nonceKey(summary.from, command.nonce)
            usedNonces.add(nonceKey)
            nonceDao.insert(
                MailCommandNonceEntity(
                    nonceKey = nonceKey,
                    sender = MailCommandRecordPrivacy.senderFingerprint(normalizedFrom),
                    nonce = MailCommandSecurityPolicy.nonceForStorage(command.nonce),
                    action = command.action.wireName,
                    usedAt = now,
                    expiresAt = command.expiresAtEpochMillis
                )
            )
            resultMessage = executeCommand(command.action)
            executed = true
        }

        return ProcessedMailMessage(
            executed = executed,
            securityRejected = decision.code.isSecurityRejection(),
            record = MailCommandRecordEntity(
                messageKey = MailMessageIdentity.primaryKey(summary),
                messageNumber = summary.messageNumber,
                sender = MailCommandRecordPrivacy.redactAddress(summary.from),
                normalizedSender = MailCommandRecordPrivacy.senderFingerprint(normalizedFrom),
                subject = MailCommandRecordPrivacy.redactSubject(summary.subject),
                receivedAtMillis = summary.receivedAtMillis,
                action = command?.action?.wireName.orEmpty(),
                decisionCode = decision.code.name,
                allowed = decision.allowed,
                executed = executed,
                resultMessage = resultMessage,
                processedAtMillis = now
            )
        )
    }

    private fun classifyFetchFailure(error: Throwable): MailInboxFailureKind =
        MailInboxFailureClassifier.classify(error)

    private suspend fun executeCommand(action: MailCommandAction): String =
        when (action) {
            MailCommandAction.Status -> {
                val paused = forwardingControlStore.isPaused()
                val pendingCount = database.pendingForwardDao().pendingCount()
                if (paused) {
                    appContext.getString(R.string.format_mail_status_forwarding_paused, pendingCount)
                } else {
                    appContext.getString(R.string.format_mail_status_forwarding_running, pendingCount)
                }
            }
            MailCommandAction.RetryCache -> {
                SmsReliabilityManager.enqueueImmediateRetry(appContext)
                appContext.getString(R.string.message_mail_retry_cache_scheduled)
            }
            MailCommandAction.PauseForwarding -> {
                forwardingControlStore.setPaused(true)
                appContext.getString(R.string.message_sms_forwarding_paused)
            }
            MailCommandAction.ResumeForwarding -> {
                forwardingControlStore.setPaused(false)
                SmsReliabilityManager.enqueueImmediateRetry(appContext)
                appContext.getString(R.string.message_sms_forwarding_resumed_retry_scheduled)
            }
        }

    companion object {
        private const val DEFAULT_REFRESH_LIMIT = 20
        private const val MIN_SECRET_LENGTH = 12
        private const val NONCE_RETENTION_AFTER_EXPIRY_MS = 30L * 24L * 60L * 60L * 1000L

        fun generateSecret(): String {
            val bytes = ByteArray(24)
            SecureRandom().nextBytes(bytes)
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        }
    }
}

private data class ProcessedMailMessage(
    val executed: Boolean,
    val securityRejected: Boolean,
    val record: MailCommandRecordEntity
)

private fun MailCommandDecisionCode.isSecurityRejection(): Boolean =
    this in setOf(
        MailCommandDecisionCode.InvalidCommand,
        MailCommandDecisionCode.SenderDisabled,
        MailCommandDecisionCode.ActionNotAllowed,
        MailCommandDecisionCode.MissingAuthenticator,
        MailCommandDecisionCode.InvalidAuthenticator,
        MailCommandDecisionCode.Expired,
        MailCommandDecisionCode.ExpiresTooFar,
        MailCommandDecisionCode.NonceReused
    )

private fun Set<MailCommandAction>.toStorageString(): String =
    sortedBy { it.wireName }.joinToString(",") { it.wireName }

fun MailTrustedSenderEntity.allowedActionSet(): Set<MailCommandAction> =
    allowedActions.split(",")
        .mapNotNull { MailCommandAction.fromWireName(it) }
        .toSet()
        .ifEmpty { setOf(MailCommandAction.Status) }

fun MailCommandAction.displayName(context: Context? = null): String =
    if (context != null) {
        context.getString(displayNameRes())
    } else {
        when (this) {
            MailCommandAction.Status -> "Check status"
            MailCommandAction.RetryCache -> "Retry cache"
            MailCommandAction.PauseForwarding -> "Pause forwarding"
            MailCommandAction.ResumeForwarding -> "Resume forwarding"
        }
    }

private fun MailCommandAction.displayNameRes(): Int =
    when (this) {
        MailCommandAction.Status -> R.string.action_mail_command_status
        MailCommandAction.RetryCache -> R.string.action_mail_command_retry_cache
        MailCommandAction.PauseForwarding -> R.string.action_mail_command_pause_forwarding
        MailCommandAction.ResumeForwarding -> R.string.action_mail_command_resume_forwarding
    }

object MailCommandRecordPrivacy {
    private val sensitiveFieldPattern = Regex(
        pattern = "(?i)(password|passwd|authorization|auth|token|access_token|refresh_token|secret|key|sig|signature|nonce|密码|口令|授权码|认证码|验证码|令牌|密钥|指令密钥|签名)\\s*[:=：]\\s*[^\\s;；，,]+"
    )

    fun redactSubject(subject: String): String =
        subject
            .replace(sensitiveFieldPattern) { match ->
                "${match.groupValues[1]}: [已隐藏]"
            }
            .trim()
            .take(160)

    fun redactAddress(raw: String): String =
        MailAddressNormalizer.normalize(raw)?.let { normalized ->
            val local = normalized.substringBefore('@')
            val domain = normalized.substringAfter('@')
            val visibleLocal = local.take(2).ifBlank { "*" }
            val visibleDomain = domain.substringBefore('.').take(2).ifBlank { "*" }
            "$visibleLocal***@$visibleDomain***"
        } ?: raw.take(80)

    fun senderFingerprint(raw: String): String {
        val normalized = MailAddressNormalizer.normalize(raw)
            ?: raw.trim().lowercase()
        return stableHash("mail-sender\n$normalized")
    }

    fun stableHash(value: String): String =
        "sha256:" + MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
}

object MailMessageIdentity {
    fun primaryKey(summary: MailMessageSummary): String =
        if (summary.messageId.isNotBlank()) {
            messageIdKey(summary)
        } else {
            legacyKey(summary)
        }

    fun lookupKeys(summary: MailMessageSummary): Set<String> =
        if (summary.messageId.isNotBlank()) {
            linkedSetOf(messageIdKey(summary), legacyKey(summary))
        } else {
            linkedSetOf(legacyKey(summary))
        }

    private fun messageIdKey(summary: MailMessageSummary): String =
        MailCommandRecordPrivacy.stableHash(
            listOf(
                "message-id",
                summary.normalizedFrom.orEmpty(),
                summary.messageId.trim().trim('<', '>').lowercase()
            ).joinToString("\n")
        )

    private fun legacyKey(summary: MailMessageSummary): String =
        MailCommandRecordPrivacy.stableHash(
            listOf(
                summary.normalizedFrom.orEmpty(),
                summary.messageNumber.toString(),
                summary.receivedAtMillis.toString(),
                summary.subject.hashCode().toString()
            ).joinToString("::")
        )
}

class MailTrustedSenderSecretStore(context: Context) {
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

    fun read(email: String): String? =
        MailAddressNormalizer.normalize(email)?.let { normalized ->
            preferences.getString(secretKey(normalized), null)
        }

    fun write(email: String, secret: String) {
        val normalized = MailAddressNormalizer.normalize(email) ?: return
        preferences.edit()
            .putString(secretKey(normalized), secret)
            .apply()
    }

    fun remove(email: String) {
        val normalized = MailAddressNormalizer.normalize(email) ?: return
        preferences.edit()
            .remove(secretKey(normalized))
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "paysage_mail_trusted_sender_secrets"

        private fun secretKey(email: String): String = "secret:$email"
    }
}
