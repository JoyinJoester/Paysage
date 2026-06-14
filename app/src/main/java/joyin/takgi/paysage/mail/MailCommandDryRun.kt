package joyin.takgi.paysage.mail

import android.content.Context
import joyin.takgi.paysage.R

data class MailCommandDryRunResult(
    val allowed: Boolean,
    val code: MailCommandDecisionCode,
    val message: String,
    val action: MailCommandAction?,
    val normalizedSender: String?
) {
    val wouldExecute: Boolean
        get() = allowed && action != null
}

object MailCommandDryRun {
    fun evaluate(
        rawSender: String,
        body: String,
        trustedSenders: List<TrustedMailSender>,
        usedNonces: Set<String>,
        nowEpochMillis: Long = System.currentTimeMillis(),
        context: Context? = null
    ): MailCommandDryRunResult {
        val normalizedSender = MailAddressNormalizer.normalize(rawSender)
            ?: return MailCommandDryRunResult(
                allowed = false,
                code = MailCommandDecisionCode.InvalidSender,
                message = text(context, R.string.message_mail_sender_invalid),
                action = null,
                normalizedSender = null
            )

        val trusted = trustedSenders.firstOrNull {
            MailAddressNormalizer.normalize(it.email) == normalizedSender
        } ?: return MailCommandDryRunResult(
            allowed = false,
            code = MailCommandDecisionCode.SenderNotWhitelisted,
            message = text(context, R.string.message_mail_sender_not_whitelisted),
            action = null,
            normalizedSender = normalizedSender
        )

        if (!trusted.enabled) {
            return MailCommandDryRunResult(
                allowed = false,
                code = MailCommandDecisionCode.SenderDisabled,
                message = text(context, R.string.message_mail_sender_disabled),
                action = null,
                normalizedSender = normalizedSender
            )
        }

        if (trusted.secret.isBlank()) {
            return MailCommandDryRunResult(
                allowed = false,
                code = MailCommandDecisionCode.InvalidAuthenticator,
                message = text(context, R.string.message_mail_sender_missing_secret),
                action = null,
                normalizedSender = normalizedSender
            )
        }

        val commandResult = MailCommandParser.parse(body, context)
        val command = commandResult.getOrNull()
        val parseError = commandResult.exceptionOrNull() as? MailCommandParseException
        if (parseError != null) {
            return MailCommandDryRunResult(
                allowed = false,
                code = parseError.code,
                message = parseError.message,
                action = null,
                normalizedSender = normalizedSender
            )
        }

        val decision = MailCommandSecurityPolicy.evaluate(
            rawSender = rawSender,
            command = command,
            trustedSenders = trustedSenders,
            usedNonces = usedNonces,
            nowEpochMillis = nowEpochMillis,
            context = context
        )

        return MailCommandDryRunResult(
            allowed = decision.allowed,
            code = decision.code,
            message = decision.message,
            action = command?.action,
            normalizedSender = normalizedSender
        )
    }

    private fun text(context: Context?, resId: Int): String =
        context?.getString(resId) ?: when (resId) {
            R.string.message_mail_sender_invalid -> "Sender address is invalid."
            R.string.message_mail_sender_not_whitelisted -> "Sender is not in the whitelist."
            R.string.message_mail_sender_disabled -> "Whitelisted mailbox is disabled."
            R.string.message_mail_sender_missing_secret -> "Trusted mailbox is missing command secret. Rotate the secret."
            else -> ""
        }
}
