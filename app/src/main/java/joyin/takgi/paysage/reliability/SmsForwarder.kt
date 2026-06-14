package joyin.takgi.paysage.reliability

import android.content.Context
import joyin.takgi.paysage.R
import joyin.takgi.paysage.data.AccountType
import joyin.takgi.paysage.data.AppDatabase
import joyin.takgi.paysage.data.ForwardAccount
import joyin.takgi.paysage.data.ForwardLog
import joyin.takgi.paysage.data.PendingForwardMessage
import joyin.takgi.paysage.repository.FilterRepository
import joyin.takgi.paysage.security.ForwardAccountSecretStore
import joyin.takgi.paysage.sender.EmailSender
import joyin.takgi.paysage.sender.TelegramSender
import kotlinx.coroutines.flow.first
import kotlin.math.min

class SmsForwarder(context: Context) {
    private val appContext = context.applicationContext
    private val db = AppDatabase.getDatabase(appContext)
    private val filterRepository = FilterRepository(db.filterDao())
    private val logDao = db.forwardLogDao()
    private val accountDao = db.forwardAccountDao()
    private val pendingDao = db.pendingForwardDao()
    private val forwardingControlStore = SmsForwardingControlStore(appContext)
    private val secretStore = ForwardAccountSecretStore(appContext)

    suspend fun forwardOrQueue(request: SmsForwardRequest): SmsForwardOutcome =
        process(
            request = request,
            queueOnFailure = true,
            writeFailureLog = true,
            writeSuccessLog = true
        )

    suspend fun retryPending(limit: Int = DEFAULT_RETRY_LIMIT): SmsRetrySummary {
        val dueMessages = pendingDao.dueMessages(System.currentTimeMillis(), limit)
        var succeeded = 0
        var failed = 0

        dueMessages.forEach { pending ->
            val request = SmsForwardRequest(
                sender = pending.sender,
                content = pending.content,
                timestamp = pending.smsTimestamp,
                source = pending.source
            )
            val outcome = process(
                request = request,
                queueOnFailure = false,
                writeFailureLog = false,
                writeSuccessLog = true
            )
            if (outcome.forwarded || outcome.filtered) {
                pendingDao.deleteById(pending.id)
                succeeded += 1
            } else {
                val attempts = pending.attempts + 1
                pendingDao.update(
                    pending.copy(
                        attempts = attempts,
                        lastError = outcome.message,
                        nextAttemptAt = System.currentTimeMillis() + retryDelayMillis(attempts),
                        updatedAt = System.currentTimeMillis()
                    )
                )
                failed += 1
            }
        }

        return SmsRetrySummary(
            attempted = dueMessages.size,
            succeeded = succeeded,
            failed = failed
        )
    }

    suspend fun queue(request: SmsForwardRequest, reason: String) {
        queueIfNeeded(request, reason, attempts = 0)
    }

    private suspend fun process(
        request: SmsForwardRequest,
        queueOnFailure: Boolean,
        writeFailureLog: Boolean,
        writeSuccessLog: Boolean
    ): SmsForwardOutcome {
        if (forwardingControlStore.isPaused()) {
            if (queueOnFailure && writeFailureLog) {
                logDao.insert(request.toLog(emailSuccess = false, telegramSuccess = false, filtered = true))
            }
            return SmsForwardOutcome(
                request = request,
                forwarded = false,
                queued = false,
                filtered = queueOnFailure,
                emailSuccess = false,
                telegramSuccess = false,
                message = appContext.getString(R.string.message_sms_forwarding_paused)
            )
        }

        if (!filterRepository.shouldForward(request.sender, request.content)) {
            logDao.insert(request.toLog(emailSuccess = false, telegramSuccess = false, filtered = true))
            return SmsForwardOutcome(
                request = request,
                forwarded = false,
                queued = false,
                filtered = true,
                emailSuccess = false,
                telegramSuccess = false,
                message = appContext.getString(R.string.message_sms_filtered)
            )
        }

        val accounts = accountDao.getEnabled().first()
        val matchedAccounts = accounts.filter { it.matchesSender(request.sender) }
        if (matchedAccounts.isEmpty()) {
            logDao.insert(request.toLog(emailSuccess = false, telegramSuccess = false, filtered = true))
            return SmsForwardOutcome(
                request = request,
                forwarded = false,
                queued = false,
                filtered = true,
                emailSuccess = false,
                telegramSuccess = false,
                message = appContext.getString(R.string.message_no_matching_forward_account)
            )
        }

        if (!SmsNetworkMonitor.isConnected(appContext)) {
            val message = appContext.getString(R.string.message_network_unavailable_sms_cached)
            if (queueOnFailure) {
                queueIfNeeded(request, message, attempts = 0)
            }
            if (writeFailureLog) {
                logDao.insert(request.toLog(emailSuccess = false, telegramSuccess = false, filtered = false))
            }
            return SmsForwardOutcome(
                request = request,
                forwarded = false,
                queued = queueOnFailure,
                filtered = false,
                emailSuccess = false,
                telegramSuccess = false,
                message = message
            )
        }

        var anyEmailSuccess = false
        var anyTelegramSuccess = false

        matchedAccounts.forEach { account ->
            when (account.type) {
                AccountType.EMAIL -> {
                    val securedAccount = normalizeEmailSecrets(account)
                    if (securedAccount != account) {
                        accountDao.update(securedAccount)
                    }
                    val credentialRef = secretStore.accountCredentialRef(securedAccount)
                    val credential = secretStore.readCredential(credentialRef).ifBlank {
                        securedAccount.smtpPassword
                    }
                    val encryptionKey = if (securedAccount.emailEncryptionEnabled) {
                        secretStore.readEncryptionKey(secretStore.accountEncryptionKeyRef(securedAccount))
                    } else {
                        ""
                    }
                    val encryptionReady = !securedAccount.emailEncryptionEnabled || encryptionKey.isNotBlank()

                    if (
                        securedAccount.smtpHost.isNotBlank() &&
                        securedAccount.toEmail.isNotBlank() &&
                        credential.isNotBlank() &&
                        encryptionReady
                    ) {
                        val emailSender = EmailSender(
                            securedAccount.smtpHost,
                            securedAccount.smtpPort,
                            securedAccount.smtpUsername,
                            credential,
                            securedAccount.toEmail,
                            appContext,
                            securedAccount.smtpAuthType,
                            encryptionKey
                        )
                        if (emailSender.send(request.sender, request.content, request.timestamp).isSuccess) {
                            anyEmailSuccess = true
                        }
                    }
                }
                AccountType.TELEGRAM -> {
                    if (account.botToken.isNotBlank() && account.chatId.isNotBlank()) {
                        val telegramSender = TelegramSender(account.botToken, account.chatId, appContext)
                        if (telegramSender.send(request.sender, request.content, request.timestamp).isSuccess) {
                            anyTelegramSuccess = true
                        }
                    }
                }
            }
        }

        val forwarded = anyEmailSuccess || anyTelegramSuccess
        if (forwarded) {
            if (writeSuccessLog) {
                logDao.insert(
                    request.toLog(
                        emailSuccess = anyEmailSuccess,
                        telegramSuccess = anyTelegramSuccess,
                        filtered = false
                    )
                )
            }
            return SmsForwardOutcome(
                request = request,
                forwarded = true,
                queued = false,
                filtered = false,
                emailSuccess = anyEmailSuccess,
                telegramSuccess = anyTelegramSuccess,
                message = buildSuccessMessage(anyEmailSuccess, anyTelegramSuccess)
            )
        }

        val message = appContext.getString(R.string.message_all_forward_channels_failed)
        if (queueOnFailure) {
            queueIfNeeded(request, message, attempts = 0)
        }
        if (writeFailureLog) {
            logDao.insert(request.toLog(emailSuccess = false, telegramSuccess = false, filtered = false))
        }
        return SmsForwardOutcome(
            request = request,
            forwarded = false,
            queued = queueOnFailure,
            filtered = false,
            emailSuccess = false,
            telegramSuccess = false,
            message = message
        )
    }

    private suspend fun queueIfNeeded(request: SmsForwardRequest, reason: String, attempts: Int) {
        val existing = pendingDao.findExact(request.sender, request.content, request.timestamp)
        val now = System.currentTimeMillis()
        if (existing == null) {
            pendingDao.insert(
                PendingForwardMessage(
                    sender = request.sender,
                    content = request.content,
                    smsTimestamp = request.timestamp,
                    source = request.source,
                    attempts = attempts,
                    lastError = reason,
                    nextAttemptAt = now + retryDelayMillis(attempts),
                    createdAt = now,
                    updatedAt = now
                )
            )
        } else {
            pendingDao.update(
                existing.copy(
                    lastError = reason,
                    nextAttemptAt = now + retryDelayMillis(existing.attempts),
                    updatedAt = now
                )
            )
        }
    }

    private fun ForwardAccount.matchesSender(sender: String): Boolean {
        if (phoneWhitelist.isBlank()) return true
        return phoneWhitelist.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .any { sender.contains(it) }
    }

    private fun normalizeEmailSecrets(account: ForwardAccount): ForwardAccount {
        var next = secretStore.migratePlaintextCredential(account)
        if (next.smtpCredentialRef.isBlank() && next.id > 0) {
            next = next.copy(smtpCredentialRef = secretStore.accountCredentialRef(next))
        }
        if (next.emailEncryptionEnabled && next.emailEncryptionKeyRef.isBlank() && next.id > 0) {
            next = next.copy(emailEncryptionKeyRef = secretStore.accountEncryptionKeyRef(next))
        }
        return next
    }

    private fun SmsForwardRequest.toLog(
        emailSuccess: Boolean,
        telegramSuccess: Boolean,
        filtered: Boolean
    ): ForwardLog =
        ForwardLog(
            sender = sender,
            content = content,
            timestamp = timestamp,
            emailSuccess = emailSuccess,
            telegramSuccess = telegramSuccess,
            filtered = filtered
        )

    private fun buildSuccessMessage(emailSuccess: Boolean, telegramSuccess: Boolean): String {
        val channels = buildList {
            if (emailSuccess) add(appContext.getString(R.string.label_email_channel))
            if (telegramSuccess) add("Telegram")
        }
        return appContext.getString(
            R.string.format_sms_forward_success,
            channels.joinToString(appContext.getString(R.string.separator_list))
        )
    }

    private fun retryDelayMillis(attempts: Int): Long {
        val boundedAttempts = attempts.coerceIn(0, 8)
        val multiplier = 1 shl boundedAttempts
        return min(BASE_RETRY_DELAY_MS * multiplier, MAX_RETRY_DELAY_MS)
    }

    companion object {
        private const val DEFAULT_RETRY_LIMIT = 20
        private const val BASE_RETRY_DELAY_MS = 60L * 1000L
        private const val MAX_RETRY_DELAY_MS = 6L * 60L * 60L * 1000L
    }
}
