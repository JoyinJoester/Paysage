package joyin.takgi.paysage.reliability

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import joyin.takgi.paysage.data.AppDatabase

class SmsRetryWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        SmsReliabilityManager.scheduleAlarm(applicationContext)
        if (!SmsNetworkMonitor.isConnected(applicationContext)) {
            return Result.retry()
        }

        // 转发日志保留 30 天:热点机长期运行,不清理会无限膨胀
        runCatching {
            AppDatabase.getDatabase(applicationContext).forwardLogDao()
                .deleteOlderThan(System.currentTimeMillis() - LOG_RETENTION_MS)
        }

        val summary = SmsForwarder(applicationContext).retryPending()
        SmsReliabilityNotifier.notifyRetrySummary(applicationContext, summary)
        return if (summary.failed == 0) Result.success() else Result.retry()
    }

    companion object {
        private const val LOG_RETENTION_MS = 30L * 24L * 60L * 60L * 1000L
    }
}
