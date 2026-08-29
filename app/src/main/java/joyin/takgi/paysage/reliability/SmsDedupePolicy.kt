package joyin.takgi.paysage.reliability

/**
 * 一条待转发的短信记录,由 [SmsDedupeStore] 持久化。
 */
data class SmsDedupeEntry(
    val sender: String,
    val content: String,
    val timestamp: Long
)

/**
 * 短信转发入口去重判定。
 *
 * 同一条短信会同时从多条链路到达:广播、收件箱 ContentObserver、通知无障碍。
 * 无障碍通道的内容取自通知栏,发送方可能是联系人名而非号码,长短信还常被
 * 通知栏截成只有标题或开头一小段;因此除了全文一致(忽略空白差异)外,
 * 还把"较短的一方被较长的一方完整包含"视为同一条消息的重复上报。
 * 发送方不参与比对,避免号码/联系人名两种表示导致去重失效。
 */
object SmsDedupePolicy {
    const val DEDUPE_WINDOW_MS = 3L * 60L * 1000L
    private const val HISTORY_WINDOW_MS = 60L * 60L * 1000L
    private const val MAX_ENTRIES = 64
    private const val MIN_CONTAINED_LENGTH = 6

    fun isNew(entries: List<SmsDedupeEntry>, request: SmsDedupeEntry, now: Long): Boolean =
        entries.none { isSameMessage(it, request, now) }

    fun record(entries: List<SmsDedupeEntry>, request: SmsDedupeEntry, now: Long): List<SmsDedupeEntry> =
        (entries.filter { now - it.timestamp <= HISTORY_WINDOW_MS } + request.copy(timestamp = now))
            .takeLast(MAX_ENTRIES)

    private fun isSameMessage(entry: SmsDedupeEntry, request: SmsDedupeEntry, now: Long): Boolean {
        if (now - entry.timestamp !in 0..DEDUPE_WINDOW_MS) return false
        val recorded = normalize(entry.content)
        val incoming = normalize(request.content)
        if (recorded.isEmpty() || incoming.isEmpty()) return false
        if (recorded == incoming) return true
        val shorter = if (recorded.length <= incoming.length) recorded else incoming
        val longer = if (recorded.length <= incoming.length) incoming else recorded
        return shorter.length >= MIN_CONTAINED_LENGTH && longer.contains(shorter)
    }

    private fun normalize(content: String): String = content.filterNot { it.isWhitespace() }
}
