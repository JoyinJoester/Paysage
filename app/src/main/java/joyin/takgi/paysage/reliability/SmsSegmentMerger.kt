package joyin.takgi.paysage.reliability

import kotlin.math.abs

/**
 * 把同一发送方在短窗口内到达的分段合并成一条消息。
 *
 * 长短信的各段可能分属不同广播、或收件箱里不同的行,逐条转发会把
 * 一条短信拆成多条。合并时若新段已被已有内容包含则忽略(交叉上报的
 * 重复段),若新段更完整则整体替换(如广播先到一段、收件箱随后给了
 * 全文),只有真正的续段才追加,避免内容被拼坏。
 *
 * 纯逻辑无 Android 依赖,时间由调用方传入以便测试。
 */
class SmsSegmentMerger(val mergeWindowMs: Long = DEFAULT_MERGE_WINDOW_MS) {

    data class MergedMessage(
        val sender: String,
        val content: String,
        val timestamp: Long,
        val source: String
    )

    private class PendingSegment(
        var content: String,
        var timestamp: Long,
        var source: String,
        var lastTouch: Long
    )

    private val pending = LinkedHashMap<String, PendingSegment>()

    /**
     * 新分段进入。返回因窗口过期或被新会话取代而应当立即转发的合并结果,
     * 当前会话由调用方在窗口到期后通过 [expire] 取走。
     */
    fun onSegment(
        sender: String,
        content: String,
        timestamp: Long,
        source: String,
        now: Long
    ): List<MergedMessage> {
        val ready = mutableListOf<MergedMessage>()
        val iterator = pending.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value.lastTouch > mergeWindowMs) {
                ready.add(entry.value.toMessage(entry.key))
                iterator.remove()
            }
        }

        val existing = pending[sender]
        if (existing == null || abs(timestamp - existing.timestamp) > mergeWindowMs) {
            existing?.let { ready.add(it.toMessage(sender)) }
            pending[sender] = PendingSegment(content.trim(), timestamp, source, now)
            return ready
        }

        existing.mergeIn(content, timestamp)
        existing.lastTouch = now
        return ready
    }

    fun expire(sender: String): MergedMessage? {
        val entry = pending.remove(sender) ?: return null
        return entry.toMessage(sender)
    }

    fun pendingSenders(): Set<String> = pending.keys.toSet()

    private fun PendingSegment.mergeIn(incoming: String, incomingTimestamp: Long) {
        if (incomingTimestamp < timestamp) timestamp = incomingTimestamp
        val incomingText = incoming.trim()
        if (incomingText.isEmpty()) return
        val pendingText = content.trim()
        if (pendingText.isEmpty()) {
            content = incomingText
            return
        }
        val pendingNormalized = pendingText.normalize()
        val incomingNormalized = incomingText.normalize()
        content = when {
            incomingNormalized.contains(pendingNormalized) -> incomingText
            pendingNormalized.contains(incomingNormalized) -> pendingText
            else -> pendingText + incomingText
        }
    }

    private fun PendingSegment.toMessage(sender: String) =
        MergedMessage(sender, content, timestamp, source)

    private fun String.normalize(): String = filterNot { it.isWhitespace() }

    companion object {
        const val DEFAULT_MERGE_WINDOW_MS = 3_000L
    }
}
