package joyin.takgi.paysage.reliability

import android.app.Activity
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import joyin.takgi.paysage.service.SmsKeepAliveService
import java.util.concurrent.TimeUnit

object SmsReliabilityManager {
    fun ensureScheduled(context: Context) {
        val appContext = context.applicationContext
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val periodic = PeriodicWorkRequestBuilder<SmsRetryWorker>(
            REPEAT_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodic
        )
        scheduleAlarm(appContext)
    }

    fun enqueueImmediateRetry(context: Context) {
        val appContext = context.applicationContext
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<SmsRetryWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun scheduleAlarm(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, SmsRetryAlarmReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            }
        val pendingIntent = PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE, intent, flags)
        val triggerAt = System.currentTimeMillis() + ALARM_INTERVAL_MS
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(PowerManager::class.java) ?: return false
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    @Suppress("DEPRECATION")
    fun isKeepAliveServiceRunning(context: Context): Boolean {
        val activityManager = context.getSystemService(ActivityManager::class.java) ?: return false
        return activityManager.getRunningServices(Int.MAX_VALUE).any { service ->
            service.service.className == SmsKeepAliveService::class.java.name
        }
    }

    fun openBatteryOptimizationWizard(activity: Activity): Boolean {
        return try {
            val requestIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:${activity.packageName}"))
            activity.startActivity(requestIntent)
            true
        } catch (_: ActivityNotFoundException) {
            openBatteryOptimizationSettings(activity)
        } catch (_: SecurityException) {
            openBatteryOptimizationSettings(activity)
        }
    }

    fun openBatteryOptimizationSettings(activity: Activity): Boolean {
        return try {
            activity.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            true
        } catch (_: ActivityNotFoundException) {
            try {
                activity.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.parse("package:${activity.packageName}"))
                )
                true
            } catch (_: ActivityNotFoundException) {
                false
            }
        } catch (_: SecurityException) {
            false
        }
    }

    private const val PERIODIC_WORK_NAME = "paysage_sms_retry_periodic"
    private const val IMMEDIATE_WORK_NAME = "paysage_sms_retry_immediate"
    private const val REPEAT_INTERVAL_MINUTES = 15L
    private const val ALARM_INTERVAL_MS = 15L * 60L * 1000L
    private const val ALARM_REQUEST_CODE = 4301
}
