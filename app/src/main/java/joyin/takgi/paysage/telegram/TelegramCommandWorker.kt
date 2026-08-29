package joyin.takgi.paysage.telegram

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class TelegramCommandWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val result = runCatching {
            TelegramCommandRepository(applicationContext).refreshCommands()
        }.getOrElse {
            return Result.retry()
        }
        return if (result.success) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}

object TelegramCommandReliabilityManager {
    fun ensureScheduled(context: Context) {
        val appContext = context.applicationContext
        val request = PeriodicWorkRequestBuilder<TelegramCommandWorker>(
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

    fun enqueueImmediateCheck(context: Context) {
        val appContext = context.applicationContext
        val request = OneTimeWorkRequestBuilder<TelegramCommandWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(appContext).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private const val WORK_NAME = "paysage_telegram_command_periodic"
    private const val IMMEDIATE_WORK_NAME = "paysage_telegram_command_immediate"
    private const val REPEAT_INTERVAL_MINUTES = 15L
}
