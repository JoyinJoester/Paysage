package joyin.takgi.paysage.reliability

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsDedupePolicyTest {

    private val body = "【招商银行】您尾号1234的账户于08月29日消费100.00元"

    @Test
    fun exactSameContentIsDuplicate() {
        val entries = listOf(entry(body, timestamp = 1_000L))
        val request = SmsDedupeEntry("106579888888", body, 1_000L)
        assertFalse(SmsDedupePolicy.isNew(entries, request, now = 2_000L))
    }

    @Test
    fun whitespaceDifferenceStillDuplicate() {
        val entries = listOf(entry(body.replace("消费", " 消费 "), timestamp = 1_000L))
        val request = SmsDedupeEntry("106579888888", body, 1_000L)
        assertFalse(SmsDedupePolicy.isNew(entries, request, now = 2_000L))
    }

    @Test
    fun notificationTitleOnlyCopyIsDuplicate() {
        // 无障碍通道拿到的可能只有标题或开头一小段
        val entries = listOf(entry(body, timestamp = 1_000L))
        val request = SmsDedupeEntry("招商银行", "【招商银行】您尾号1234", 1_000L)
        assertFalse(SmsDedupePolicy.isNew(entries, request, now = 2_000L))
    }

    @Test
    fun fullBodyAfterTruncatedCopyIsDuplicate() {
        // 先到截断副本、后到完整正文,同样判重
        val entries = listOf(entry("【招商银行】您尾号1234", timestamp = 1_000L))
        val request = SmsDedupeEntry("106579888888", body, 1_000L)
        assertFalse(SmsDedupePolicy.isNew(entries, request, now = 2_000L))
    }

    @Test
    fun senderNameMismatchStillDuplicate() {
        // 无障碍通道发送方是联系人名,广播通道是号码
        val entries = listOf(entry(body, sender = "106579888888", timestamp = 1_000L))
        val request = SmsDedupeEntry("张三", body, 1_000L)
        assertFalse(SmsDedupePolicy.isNew(entries, request, now = 2_000L))
    }

    @Test
    fun differentContentIsNotDuplicate() {
        val entries = listOf(entry(body, timestamp = 1_000L))
        val request = SmsDedupeEntry("106579888888", "【招商银行】您的验证码是9527", 1_000L)
        assertTrue(SmsDedupePolicy.isNew(entries, request, now = 2_000L))
    }

    @Test
    fun sameContentOutsideWindowIsNotDuplicate() {
        val entries = listOf(entry(body, timestamp = 1_000L))
        val request = SmsDedupeEntry("106579888888", body, 1_000L)
        assertTrue(
            SmsDedupePolicy.isNew(entries, request, now = 1_000L + SmsDedupePolicy.DEDUPE_WINDOW_MS + 1)
        )
    }

    @Test
    fun shortContainedFragmentIsNotTreatedAsDuplicate() {
        // 过短的片段(不足最小包含长度)不参与包含判重,避免误杀真实短消息
        val entries = listOf(entry(body, timestamp = 1_000L))
        val request = SmsDedupeEntry("106579888888", "【招商】", 1_000L)
        assertTrue(SmsDedupePolicy.isNew(entries, request, now = 2_000L))
    }

    @Test
    fun recordKeepsRequestAndPrunesOldEntries() {
        val old = entry(body, timestamp = 0L)
        val request = SmsDedupeEntry("106579888888", "new message", timestamp = 5_000L)
        val recorded = SmsDedupePolicy.record(listOf(old), request, now = 10_000L)
        assertTrue(recorded.any { it.content == "new message" && it.timestamp == 10_000L })
    }

    private fun entry(
        content: String,
        sender: String = "106579888888",
        timestamp: Long
    ) = SmsDedupeEntry(sender, content, timestamp)
}
