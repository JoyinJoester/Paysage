package joyin.takgi.paysage.mail

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

object MailInboxWorkerRetryPolicy {
    fun shouldRetry(result: MailInboxRefreshResult): Boolean =
        !result.success && result.failureKind == MailInboxFailureKind.NetworkOrServerFailed
}

class MailInboxWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val repository = MailInboxRepository(applicationContext)
        val account = repository.readAccount()
        if (!account.enabled || !account.isConfigured) {
            return Result.success()
        }

        val result = repository.refreshInbox()
        return if (result.success) {
            MailInboxNotifier.notifyRejectedCommands(applicationContext, result)
            Result.success()
        } else {
            MailInboxNotifier.notifyRefreshIssue(applicationContext, result)
            if (MailInboxWorkerRetryPolicy.shouldRetry(result)) {
                Result.retry()
            } else {
                Result.success()
            }
        }
    }
}

object MailInboxReliabilityManager {
    fun ensureScheduled(context: Context) {
        val appContext = context.applicationContext
        val account = MailInboxAccountStore(appContext).read()
        if (!account.enabled || !account.isConfigured) {
            cancel(appContext)
            return
        }

        val request = PeriodicWorkRequestBuilder<MailInboxWorker>(
            REPEAT_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
    }

    private const val WORK_NAME = "paysage_mail_inbox_periodic"
    private const val REPEAT_INTERVAL_MINUTES = 15L
}
