package joyin.takgi.paysage.mail

import joyin.takgi.paysage.data.MailTrustedSenderEntity
import java.time.Instant

object MailInboxDiagnostics {
    fun buildReport(
        account: MailInboxAccountConfig,
        runtimeStatus: MailInboxRuntimeStatus,
        realtimeSettings: MailInboxRealtimeSettings,
        trustedSenders: List<MailTrustedSenderEntity>,
        generatedAtMillis: Long = System.currentTimeMillis()
    ): String {
        val enabledSenders = trustedSenders.count { it.enabled }
        val disabledSenders = trustedSenders.size - enabledSenders
        val actions = trustedSenders
            .flatMap { it.allowedActionSet() }
            .toSet()
            .sortedBy { it.wireName }
            .joinToString(",") { it.wireName }
            .ifBlank { "none" }

        return buildString {
            appendLine("Paysage Mail Inbox Diagnostic")
            appendLine("generatedAt=${Instant.ofEpochMilli(generatedAtMillis)}")
            appendLine("account.configured=${account.isConfigured}")
            appendLine("account.enabled=${account.enabled}")
            appendLine("account.host=${redactHost(account.host)}")
            appendLine("account.username=${MailCommandRecordPrivacy.redactAddress(account.username)}")
            appendLine("account.port=${account.port}")
            appendLine("account.ssl=${account.useSsl}")
            appendLine("trustedSenders.total=${trustedSenders.size}")
            appendLine("trustedSenders.enabled=$enabledSenders")
            appendLine("trustedSenders.disabled=$disabledSenders")
            appendLine("trustedSenders.actions=$actions")
            appendLine("runtime.lastCheckAt=${runtimeStatus.lastCheckAt.toIsoOrNone()}")
            appendLine("runtime.lastSuccessAt=${runtimeStatus.lastSuccessAt.toIsoOrNone()}")
            appendLine("runtime.lastFailureAt=${runtimeStatus.lastFailureAt.toIsoOrNone()}")
            appendLine("runtime.lastFailureKind=${runtimeStatus.lastFailureKind.name}")
            appendLine("runtime.lastFetched=${runtimeStatus.lastFetched}")
            appendLine("runtime.lastExecuted=${runtimeStatus.lastExecuted}")
            appendLine("runtime.lastIgnored=${runtimeStatus.lastIgnored}")
            appendLine("runtime.lastRejected=${runtimeStatus.lastRejected}")
            appendLine("realtime.enabled=${realtimeSettings.enabled}")
            appendLine("realtime.reconnectMinutes=${realtimeSettings.idleReconnectMinutes}")
            appendLine("privacy.note=No password, command key, signature, nonce, full body, or token is included.")
        }
    }

    fun readinessIssues(
        account: MailInboxAccountConfig,
        trustedSenders: List<MailTrustedSenderEntity>
    ): List<String> = buildList {
        if (!account.isConfigured) {
            add("IMAP account is not fully configured.")
        }
        if (!account.enabled) {
            add("Mail inbox checking is disabled.")
        }
        if (trustedSenders.none { it.enabled }) {
            add("No enabled trusted sender.")
        }
    }

    private fun Long.toIsoOrNone(): String =
        if (this > 0L) {
            Instant.ofEpochMilli(this).toString()
        } else {
            "none"
        }

    private fun redactHost(host: String): String {
        val clean = host.trim()
        if (clean.isBlank()) return "not-configured"
        val parts = clean.split('.').filter { it.isNotBlank() }
        return when {
            parts.size >= 2 -> "***.${parts.takeLast(2).joinToString(".")}"
            else -> "***"
        }
    }
}
