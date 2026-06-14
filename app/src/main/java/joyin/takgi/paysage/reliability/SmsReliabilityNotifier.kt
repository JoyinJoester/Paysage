package joyin.takgi.paysage.reliability

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

object SmsReliabilityNotifier {
    fun notifyForwardOutcome(context: Context, outcome: SmsForwardOutcome) {
        if (!outcome.needsAttention && !outcome.forwarded) return
        if (!canNotify(context)) return

        ensureChannel(context)
        val title = when {
            outcome.forwarded -> context.getString(R.string.title_sms_forward_success)
            outcome.queued -> context.getString(R.string.title_sms_forward_queued)
            else -> context.getString(R.string.title_sms_forward_failed)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(outcome.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(outcome.message))
            .setContentIntent(mainActivityIntent(context))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context)
            .notify(outcome.request.timestamp.hashCode(), notification)
    }

    fun notifyRetrySummary(context: Context, summary: SmsRetrySummary) {
        if (summary.attempted == 0 || !canNotify(context)) return
        ensureChannel(context)
        val message = context.getString(
            R.string.format_sms_retry_summary,
            summary.attempted,
            summary.succeeded,
            summary.failed
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.title_sms_retry_done))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(mainActivityIntent(context))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context)
            .notify(RETRY_NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_sms_reliability_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_sms_reliability_channel_desc)
            }
        )
    }

    private fun canNotify(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
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

    private const val CHANNEL_ID = "paysage_sms_reliability"
    private const val RETRY_NOTIFICATION_ID = 3201
}
