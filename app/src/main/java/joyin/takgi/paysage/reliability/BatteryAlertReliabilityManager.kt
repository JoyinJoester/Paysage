package joyin.takgi.paysage.reliability

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object BatteryAlertReliabilityManager {
    fun ensureScheduled(context: Context) {
        val appContext = context.applicationContext
        val periodic = PeriodicWorkRequestBuilder<BatteryAlertWorker>(
            REPEAT_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodic
        )
        scheduleAlarm(appContext)
    }

    fun enqueueImmediateCheck(context: Context, bypassCooldown: Boolean = false) {
        val appContext = context.applicationContext
        val request = OneTimeWorkRequestBuilder<BatteryAlertWorker>()
            .setInputData(BatteryAlertWorker.inputData(bypassCooldown))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun scheduleAlarm(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(appContext, BatteryAlertAlarmReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            }
        val pendingIntent = PendingIntent.getBroadcast(appContext, ALARM_REQUEST_CODE, intent, flags)
        val triggerAt = System.currentTimeMillis() + ALARM_INTERVAL_MS
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    private const val PERIODIC_WORK_NAME = "paysage_battery_alert_periodic"
    private const val IMMEDIATE_WORK_NAME = "paysage_battery_alert_immediate"
    private const val REPEAT_INTERVAL_MINUTES = 15L
    private const val ALARM_INTERVAL_MS = 15L * 60L * 1000L
    private const val ALARM_REQUEST_CODE = 4302
}
