package joyin.takgi.paysage.reliability

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsSegmentMergerTest {

    private val merger = SmsSegmentMerger(mergeWindowMs = 3_000L)

    @Test
    fun singleSegmentIsFlushedByExpire() {
        merger.onSegment("106579888888", "hello", timestamp = 1_000L, source = "broadcast", now = 1_000L)
        val expired = merger.expire("106579888888")
        assertEquals("hello", expired?.content)
        assertEquals(1_000L, expired?.timestamp)
        assertEquals("broadcast", expired?.source)
    }

    @Test
    fun closeTimestampSegmentsAreJoined() {
        // 短信分段拼接与正文一致,中间不加任何分隔符
        merger.onSegment("106579888888", "part one ", timestamp = 1_000L, source = "broadcast", now = 1_000L)
        val ready = merger.onSegment("106579888888", "part two", timestamp = 1_100L, source = "broadcast", now = 1_200L)
        assertTrue(ready.isEmpty())
        val expired = merger.expire("106579888888")
        assertEquals("part onepart two", expired?.content)
        assertEquals(1_000L, expired?.timestamp)
    }

    @Test
    fun duplicateSegmentIsIgnored() {
        // 广播先到一段,收件箱观察随后报了同一行
        merger.onSegment("106579888888", "part one", timestamp = 1_000L, source = "broadcast", now = 1_000L)
        val ready = merger.onSegment("106579888888", "part one", timestamp = 1_100L, source = "content_observer", now = 1_200L)
        assertTrue(ready.isEmpty())
        assertEquals("part one", merger.expire("106579888888")?.content)
    }

    @Test
    fun supersetSegmentReplacesInsteadOfAppending() {
        // 广播只到了第一段,收件箱全文先到:应以全文替换,不能拼成 part one + 全文
        val fullBody = "【招商银行】您尾号1234的账户消费100.00元,余额2000.00元"
        merger.onSegment("106579888888", "【招商银行】您尾号1234", timestamp = 1_000L, source = "broadcast", now = 1_000L)
        val ready = merger.onSegment("106579888888", fullBody, timestamp = 1_100L, source = "content_observer", now = 1_200L)
        assertTrue(ready.isEmpty())
        assertEquals(fullBody, merger.expire("106579888888")?.content)
    }

    @Test
    fun farApartTimestampsFlushPreviousAndStartNew() {
        merger.onSegment("106579888888", "first message", timestamp = 1_000L, source = "broadcast", now = 1_000L)
        val ready = merger.onSegment(
            "106579888888",
            "second message",
            timestamp = 60_000L,
            source = "broadcast",
            now = 60_000L
        )
        assertEquals(1, ready.size)
        assertEquals("first message", ready.first().content)
        assertEquals("second message", merger.expire("106579888888")?.content)
    }

    @Test
    fun differentSendersStayIndependent() {
        merger.onSegment("106579888888", "from a", timestamp = 1_000L, source = "broadcast", now = 1_000L)
        merger.onSegment("10086", "from b", timestamp = 1_100L, source = "broadcast", now = 1_100L)
        assertEquals("from a", merger.expire("106579888888")?.content)
        assertEquals("from b", merger.expire("10086")?.content)
        assertNull(merger.expire("10086"))
    }

    @Test
    fun staleEntryIsFlushedOnNextSegment() {
        merger.onSegment("106579888888", "stale", timestamp = 1_000L, source = "broadcast", now = 1_000L)
        val ready = merger.onSegment(
            "106579888888",
            "fresh",
            timestamp = 60_000L,
            source = "broadcast",
            now = 60_000L
        )
        // stale 已超窗口:无论时间戳如何都应先吐出
        assertEquals(listOf("stale"), ready.map { it.content })
        assertEquals("fresh", merger.expire("106579888888")?.content)
    }

    @Test
    fun pendingSendersReflectsState() {
        assertTrue(merger.pendingSenders().isEmpty())
        merger.onSegment("106579888888", "hello", timestamp = 1_000L, source = "broadcast", now = 1_000L)
        assertEquals(setOf("106579888888"), merger.pendingSenders())
        merger.expire("106579888888")
        assertTrue(merger.pendingSenders().isEmpty())
    }
}
