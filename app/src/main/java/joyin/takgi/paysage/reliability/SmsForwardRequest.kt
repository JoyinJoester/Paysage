package joyin.takgi.paysage.reliability

data class SmsForwardRequest(
    val sender: String,
    val content: String,
    val timestamp: Long,
    val source: String
)

data class SmsForwardOutcome(
    val request: SmsForwardRequest,
    val forwarded: Boolean,
    val queued: Boolean,
    val filtered: Boolean,
    val emailSuccess: Boolean,
    val telegramSuccess: Boolean,
    val message: String
) {
    val failed: Boolean
        get() = !forwarded && !queued && !filtered

    val needsAttention: Boolean
        get() = queued || failed
}

data class SmsRetrySummary(
    val attempted: Int,
    val succeeded: Int,
    val failed: Int
)
