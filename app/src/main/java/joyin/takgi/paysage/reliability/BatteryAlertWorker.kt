package joyin.takgi.paysage.reliability

import android.content.Context
import androidx.work.Data
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class BatteryAlertWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        BatteryAlertReliabilityManager.scheduleAlarm(applicationContext)
        val alerts = BatteryThresholdNotifier.checkAndNotify(
            context = applicationContext,
            bypassCooldown = inputData.getBoolean(KEY_BYPASS_COOLDOWN, false)
        )
        BatteryThresholdAlertForwarder(applicationContext).forward(alerts)
        return Result.success()
    }

    companion object {
        const val KEY_BYPASS_COOLDOWN = "bypass_cooldown"

        fun inputData(bypassCooldown: Boolean): Data =
            Data.Builder()
                .putBoolean(KEY_BYPASS_COOLDOWN, bypassCooldown)
                .build()
    }
}
