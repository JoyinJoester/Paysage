package joyin.takgi.paysage.reliability

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SmsRetryWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        SmsReliabilityManager.scheduleAlarm(applicationContext)
        if (!SmsNetworkMonitor.isConnected(applicationContext)) {
            return Result.retry()
        }

        val summary = SmsForwarder(applicationContext).retryPending()
        SmsReliabilityNotifier.notifyRetrySummary(applicationContext, summary)
        return if (summary.failed == 0) Result.success() else Result.retry()
    }
}
