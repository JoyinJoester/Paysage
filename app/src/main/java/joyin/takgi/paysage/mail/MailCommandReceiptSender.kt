package joyin.takgi.paysage.mail

import android.content.Context
import joyin.takgi.paysage.R
import joyin.takgi.paysage.data.AccountType
import joyin.takgi.paysage.data.AppDatabase
import joyin.takgi.paysage.data.ForwardAccount
import joyin.takgi.paysage.data.SmtpAuthType
import joyin.takgi.paysage.reliability.SmsForwardingControlStore
import joyin.takgi.paysage.security.ForwardAccountSecretStore
import joyin.takgi.paysage.sender.EmailSender
import joyin.takgi.paysage.util.DateFormatter
import kotlinx.coroutines.flow.first

data class MailCommandReceiptResult(
    val sent: Boolean,
    val message: String
)

class MailCommandReceiptSender(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.getDatabase(appContext)
    private val accountStore = MailInboxAccountStore(appContext)
    private val forwardingControlStore = SmsForwardingControlStore(appContext)
    private val secretStore = ForwardAccountSecretStore(appContext)

    suspend fun send(
        summary: MailMessageSummary,
        command: ParsedMailCommand,
        commandResult: String,
        processedAtMillis: Long
    ): MailCommandReceiptResult {
        val recipient = summary.normalizedFrom?.takeIf { it.isNotBlank() }
            ?: return MailCommandReceiptResult(
                sent = false,
                message = appContext.getString(R.string.message_mail_receipt_skipped_invalid_sender)
            )

        val sender = resolveSender(recipient)
            ?: return MailCommandReceiptResult(
                sent = false,
                message = appContext.getString(R.string.message_mail_receipt_skipped_no_smtp)
            )

        val actionName = command.action.displayName(appContext)
        val result = sender.sendPlain(
            subject = appContext.getString(R.string.format_mail_receipt_subject, actionName),
            body = buildBody(
                recipient = recipient,
                actionName = actionName,
                argument = command.argument,
                commandResult = commandResult,
                processedAtMillis = processedAtMillis
            )
        )

        return if (result.isSuccess) {
            MailCommandReceiptResult(
                sent = true,
                message = appContext.getString(R.string.message_mail_receipt_sent)
            )
        } else {
            MailCommandReceiptResult(
                sent = false,
                message = appContext.getString(
                    R.string.format_mail_receipt_send_failed,
                    result.exceptionOrNull()?.safeDescription().orEmpty().ifBlank { "unknown" }
                )
            )
        }
    }

    private suspend fun resolveSender(recipient: String): EmailSender? =
        senderFromForwardAccount(recipient) ?: senderFromInboxAccount(recipient)

    private suspend fun senderFromForwardAccount(recipient: String): EmailSender? {
        val accountDao = database.forwardAccountDao()
        val accounts = accountDao.getEnabled().first()
        accounts.asSequence()
            .filter { it.type == AccountType.EMAIL }
            .forEach { account ->
                val securedAccount = normalizeEmailSecrets(account)
                if (securedAccount != account) {
                    accountDao.update(securedAccount)
                }
                val credentialRef = secretStore.accountCredentialRef(securedAccount)
                val credential = secretStore.readCredential(credentialRef).ifBlank {
                    securedAccount.smtpPassword
                }
                if (
                    securedAccount.smtpHost.isNotBlank() &&
                    securedAccount.smtpUsername.isNotBlank() &&
                    credential.isNotBlank()
                ) {
                    return EmailSender(
                        smtpHost = securedAccount.smtpHost,
                        smtpPort = securedAccount.smtpPort,
                        username = securedAccount.smtpUsername,
                        credential = credential,
                        toEmail = recipient,
                        context = appContext,
                        authType = securedAccount.smtpAuthType,
                        encryptionKeyBase64 = ""
                    )
                }
            }
        return null
    }

    private fun senderFromInboxAccount(recipient: String): EmailSender? {
        val account = accountStore.read()
        if (!account.enabled || !account.isConfigured) return null
        return EmailSender(
            smtpHost = inferSmtpHost(account.host),
            smtpPort = inferSmtpPort(account.host),
            username = account.username,
            credential = account.password,
            toEmail = recipient,
            context = appContext,
            authType = SmtpAuthType.PASSWORD,
            encryptionKeyBase64 = ""
        )
    }

    private suspend fun buildBody(
        recipient: String,
        actionName: String,
        argument: String?,
        commandResult: String,
        processedAtMillis: Long
    ): String {
        val forwardingPaused = forwardingControlStore.isPaused()
        val pendingCount = database.pendingForwardDao().pendingCount()
        val forwardingState = if (forwardingPaused) {
            appContext.getString(R.string.status_paused)
        } else {
            appContext.getString(R.string.title_forward_running)
        }

        return buildString {
            appendLine(appContext.getString(R.string.mail_receipt_header))
            appendLine()
            appendLine(appContext.getString(R.string.format_mail_receipt_recipient, recipient))
            appendLine(appContext.getString(R.string.format_mail_receipt_action, actionName))
            argument?.takeIf { it.isNotBlank() }?.let {
                appendLine(appContext.getString(R.string.format_mail_receipt_argument, it))
            }
            appendLine(appContext.getString(R.string.format_mail_receipt_processed_at, DateFormatter.format(processedAtMillis)))
            appendLine()
            appendLine(appContext.getString(R.string.mail_receipt_result_title))
            appendLine(commandResult)
            appendLine()
            appendLine(appContext.getString(R.string.mail_receipt_current_status_title))
            appendLine(appContext.getString(R.string.format_mail_receipt_forwarding_state, forwardingState))
            appendLine(appContext.getString(R.string.format_mail_receipt_pending_cache, pendingCount))
            appendLine()
            appendLine(appContext.getString(R.string.mail_receipt_footer))
        }
    }

    private fun normalizeEmailSecrets(account: ForwardAccount): ForwardAccount {
        var next = secretStore.migratePlaintextCredential(account)
        if (next.smtpCredentialRef.isBlank() && next.id > 0) {
            next = next.copy(smtpCredentialRef = secretStore.accountCredentialRef(next))
        }
        return next
    }

    private fun inferSmtpHost(imapHost: String): String {
        val trimmed = imapHost.trim()
        return when {
            trimmed.startsWith("imap.", ignoreCase = true) ->
                "smtp.${trimmed.substringAfter('.')}"
            trimmed.contains(".imap.", ignoreCase = true) ->
                trimmed.replace(".imap.", ".smtp.", ignoreCase = true)
            else -> trimmed.replace("imap", "smtp", ignoreCase = true)
        }.ifBlank { trimmed }
    }

    private fun inferSmtpPort(imapHost: String): Int {
        val host = imapHost.lowercase()
        return when {
            host.contains("gmail") -> 587
            host.contains("outlook") || host.contains("hotmail") || host.contains("office365") -> 587
            host.contains("qq.com") || host.contains("163.com") || host.contains("126.com") -> 465
            else -> 587
        }
    }

    private fun Throwable.safeDescription(): String =
        MailInboxPrivacySanitizer.redact(
            message?.takeIf { it.isNotBlank() }?.take(180) ?: javaClass.simpleName
        )
}
