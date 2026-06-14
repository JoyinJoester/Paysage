package joyin.takgi.paysage.esim

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import joyin.takgi.paysage.MainActivity
import joyin.takgi.paysage.R

object EsimOperationNotifier {
    fun notifyResult(context: Context, result: EsimDownloadResult) {
        if (result.requestId.isBlank() || result.status == EsimDownloadStatus.Pending) {
            return
        }
        val settings = EsimSettingsStore(context).read()
        if (!EsimOperationNotificationPolicy.shouldNotify(settings, result.requestId)) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_esim_operations_channel),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.notification_esim_operations_channel_desc)
                }
            )
        }

        val title = EsimDownloadHistoryPolicy.title(context, result)
        val explanation = EsimEuiccOperationErrorCatalog.explain(context, result)
        val content = explanation?.let {
            "${result.message} ${it.recoveryHint}"
        } ?: result.message
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(result.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(mainActivityIntent(context))
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(result.requestId.hashCode(), notification)
    }

    private fun mainActivityIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            }
        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    private const val CHANNEL_ID = "paysage_esim_operations"
}

object EsimOperationNotificationPolicy {
    fun shouldNotify(settings: EsimUserSettings, requestId: String): Boolean = when {
        requestId.startsWith("download-") -> settings.notifyDownloadOperations
        requestId.startsWith("switch-") -> settings.notifySwitchOperations
        requestId.startsWith("delete-") -> settings.notifyDeleteOperations
        requestId.startsWith("rename-") -> settings.notifyRenameOperations
        else -> true
    }
}
