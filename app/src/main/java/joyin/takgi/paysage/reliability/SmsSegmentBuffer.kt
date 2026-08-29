package joyin.takgi.paysage.reliability

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 广播和收件箱观察两条链路的分段聚合入口。
 *
 * 每次收到分段先进入 [SmsSegmentMerger],同发送方窗口内的新段继续合并,
 * 窗口静默后才把合并结果交给 [SmsForwardDispatcher] 去重转发,
 * 从而让跨广播到达的长短信以一条完整内容转发。
 */
object SmsSegmentBuffer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val merger = SmsSegmentMerger()
    private val mutex = Mutex()
    private val flushJobs = mutableMapOf<String, Job>()

    suspend fun enqueue(context: Context, request: SmsForwardRequest) {
        val appContext = context.applicationContext
        val ready: List<SmsSegmentMerger.MergedMessage>
        mutex.withLock {
            ready = merger.onSegment(
                sender = request.sender,
                content = request.content,
                timestamp = request.timestamp,
                source = request.source,
                now = System.currentTimeMillis()
            )
        }
        ready.forEach { dispatch(appContext, it) }
        scheduleFlush(appContext, request.sender)
    }

    private suspend fun scheduleFlush(appContext: Context, sender: String) {
        val job = scope.launch {
            delay(merger.mergeWindowMs)
            val expired: SmsSegmentMerger.MergedMessage?
            mutex.withLock {
                expired = merger.expire(sender)
            }
            expired?.let { dispatch(appContext, it) }
        }
        mutex.withLock {
            flushJobs.remove(sender)?.cancel()
            flushJobs[sender] = job
        }
    }

    private suspend fun dispatch(context: Context, message: SmsSegmentMerger.MergedMessage) {
        SmsForwardDispatcher.dispatch(
            context,
            SmsForwardRequest(
                sender = message.sender,
                content = message.content,
                timestamp = message.timestamp,
                source = message.source
            )
        )
    }
}
