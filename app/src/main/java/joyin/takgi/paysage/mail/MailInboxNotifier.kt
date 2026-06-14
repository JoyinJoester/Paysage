package joyin.takgi.paysage.mail

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

object MailInboxNotifier {
    fun notifyRefreshIssue(context: Context, result: MailInboxRefreshResult) {
        if (result.success || result.failureKind == MailInboxFailureKind.Disabled) return
        if (!canNotify(context)) return

        val advice = MailInboxRecoveryAdvisor.adviceFor(context, result.failureKind)
        val message = advice?.message ?: MailInboxPrivacySanitizer.redact(result.message)
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(advice?.title ?: context.getString(R.string.title_mail_inbox_check_failed))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(mainActivityIntent(context))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context)
            .notify(REFRESH_ISSUE_NOTIFICATION_ID, notification)
    }

    fun notifyRejectedCommands(context: Context, result: MailInboxRefreshResult) {
        if (!result.success || result.rejected <= 0 || !canNotify(context)) return

        val advice = MailInboxRecoveryAdvisor.adviceFor(context, MailInboxFailureKind.CommandRejected)
            ?: return
        val message = context.getString(R.string.format_mail_rejected_commands, result.rejected, advice.message)
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(advice.title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(mainActivityIntent(context))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context)
            .notify(COMMAND_REJECTED_NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_mail_inbox_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_mail_inbox_channel_desc)
            }
        )
    }

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun mainActivityIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            }
        return PendingIntent.getActivity(context, 1, intent, flags)
    }

    private const val CHANNEL_ID = "paysage_mail_inbox"
    private const val REFRESH_ISSUE_NOTIFICATION_ID = 6101
    private const val COMMAND_REJECTED_NOTIFICATION_ID = 6102
}
