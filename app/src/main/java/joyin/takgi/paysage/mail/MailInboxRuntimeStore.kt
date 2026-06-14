package joyin.takgi.paysage.mail

import android.content.Context
import joyin.takgi.paysage.R
import javax.mail.AuthenticationFailedException
import javax.mail.MessagingException

enum class MailInboxFailureKind {
    None,
    Disabled,
    InvalidConfig,
    AuthenticationFailed,
    NetworkOrServerFailed,
    BackgroundRestricted,
    CommandRejected
}

data class MailInboxRuntimeStatus(
    val lastCheckAt: Long = 0L,
    val lastSuccessAt: Long = 0L,
    val lastFailureAt: Long = 0L,
    val lastMessage: String = "",
    val lastFailureKind: MailInboxFailureKind = MailInboxFailureKind.None,
    val lastFetched: Int = 0,
    val lastExecuted: Int = 0,
    val lastIgnored: Int = 0,
    val lastRejected: Int = 0
)

data class MailInboxRecoveryAdvice(
    val title: String,
    val message: String,
    val actionLabel: String
)

object MailInboxRecoveryAdvisor {
    fun adviceFor(context: Context, kind: MailInboxFailureKind): MailInboxRecoveryAdvice? =
        when (kind) {
            MailInboxFailureKind.None -> null
            MailInboxFailureKind.Disabled -> MailInboxRecoveryAdvice(
                title = context.getString(R.string.title_mail_inbox_disabled),
                message = context.getString(R.string.message_mail_inbox_disabled),
                actionLabel = context.getString(R.string.action_go_settings)
            )
            MailInboxFailureKind.InvalidConfig -> MailInboxRecoveryAdvice(
                title = context.getString(R.string.title_mail_config_incomplete),
                message = context.getString(R.string.message_mail_config_incomplete),
                actionLabel = context.getString(R.string.action_go_settings)
            )
            MailInboxFailureKind.AuthenticationFailed -> MailInboxRecoveryAdvice(
                title = context.getString(R.string.title_mail_login_failed),
                message = context.getString(R.string.message_mail_login_failed),
                actionLabel = context.getString(R.string.action_fix)
            )
            MailInboxFailureKind.NetworkOrServerFailed -> MailInboxRecoveryAdvice(
                title = context.getString(R.string.title_mail_server_unreachable),
                message = context.getString(R.string.message_mail_server_unreachable),
                actionLabel = context.getString(R.string.action_retry)
            )
            MailInboxFailureKind.BackgroundRestricted -> MailInboxRecoveryAdvice(
                title = context.getString(R.string.title_mail_background_restricted),
                message = context.getString(R.string.message_mail_background_restricted),
                actionLabel = context.getString(R.string.action_optimize)
            )
            MailInboxFailureKind.CommandRejected -> MailInboxRecoveryAdvice(
                title = context.getString(R.string.title_mail_commands_rejected),
                message = context.getString(R.string.message_mail_commands_rejected),
                actionLabel = context.getString(R.string.action_view)
            )
        }
}

object MailInboxFailureClassifier {
    fun classify(error: Throwable): MailInboxFailureKind =
        when (error) {
            is IllegalArgumentException -> MailInboxFailureKind.InvalidConfig
            is AuthenticationFailedException -> MailInboxFailureKind.AuthenticationFailed
            is MessagingException -> {
                if (error.nextException is AuthenticationFailedException) {
                    MailInboxFailureKind.AuthenticationFailed
                } else {
                    MailInboxFailureKind.NetworkOrServerFailed
                }
            }
            else -> MailInboxFailureKind.NetworkOrServerFailed
        }
}

class MailInboxRuntimeStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun read(): MailInboxRuntimeStatus =
        MailInboxRuntimeStatus(
            lastCheckAt = preferences.getLong(KEY_LAST_CHECK_AT, 0L),
            lastSuccessAt = preferences.getLong(KEY_LAST_SUCCESS_AT, 0L),
            lastFailureAt = preferences.getLong(KEY_LAST_FAILURE_AT, 0L),
            lastMessage = preferences.getString(KEY_LAST_MESSAGE, "").orEmpty(),
            lastFailureKind = runCatching {
                MailInboxFailureKind.valueOf(
                    preferences.getString(KEY_LAST_FAILURE_KIND, MailInboxFailureKind.None.name)
                        ?: MailInboxFailureKind.None.name
                )
            }.getOrDefault(MailInboxFailureKind.None),
            lastFetched = preferences.getInt(KEY_LAST_FETCHED, 0),
            lastExecuted = preferences.getInt(KEY_LAST_EXECUTED, 0),
            lastIgnored = preferences.getInt(KEY_LAST_IGNORED, 0),
            lastRejected = preferences.getInt(KEY_LAST_REJECTED, 0)
        )

    fun recordSuccess(
        message: String,
        fetched: Int,
        executed: Int,
        ignored: Int,
        rejected: Int
    ) {
        val now = System.currentTimeMillis()
        preferences.edit()
            .putLong(KEY_LAST_CHECK_AT, now)
            .putLong(KEY_LAST_SUCCESS_AT, now)
            .putString(KEY_LAST_MESSAGE, MailInboxPrivacySanitizer.redact(message))
            .putString(
                KEY_LAST_FAILURE_KIND,
                if (rejected > 0) {
                    MailInboxFailureKind.CommandRejected.name
                } else {
                    MailInboxFailureKind.None.name
                }
            )
            .putInt(KEY_LAST_FETCHED, fetched)
            .putInt(KEY_LAST_EXECUTED, executed)
            .putInt(KEY_LAST_IGNORED, ignored)
            .putInt(KEY_LAST_REJECTED, rejected)
            .apply()
    }

    fun recordFailure(kind: MailInboxFailureKind, message: String) {
        val now = System.currentTimeMillis()
        preferences.edit()
            .putLong(KEY_LAST_CHECK_AT, now)
            .putLong(KEY_LAST_FAILURE_AT, now)
            .putString(KEY_LAST_MESSAGE, MailInboxPrivacySanitizer.redact(message))
            .putString(KEY_LAST_FAILURE_KIND, kind.name)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "paysage_mail_inbox_runtime"
        private const val KEY_LAST_CHECK_AT = "last_check_at"
        private const val KEY_LAST_SUCCESS_AT = "last_success_at"
        private const val KEY_LAST_FAILURE_AT = "last_failure_at"
        private const val KEY_LAST_MESSAGE = "last_message"
        private const val KEY_LAST_FAILURE_KIND = "last_failure_kind"
        private const val KEY_LAST_FETCHED = "last_fetched"
        private const val KEY_LAST_EXECUTED = "last_executed"
        private const val KEY_LAST_IGNORED = "last_ignored"
        private const val KEY_LAST_REJECTED = "last_rejected"
    }
}
