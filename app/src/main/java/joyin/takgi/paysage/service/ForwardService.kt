package joyin.takgi.paysage.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import joyin.takgi.paysage.R
import joyin.takgi.paysage.reliability.SmsForwardDispatcher
import joyin.takgi.paysage.reliability.SmsForwardRequest
import joyin.takgi.paysage.reliability.SmsForwarder
import joyin.takgi.paysage.reliability.SmsReliabilityManager
import joyin.takgi.paysage.reliability.SmsReliabilityNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ForwardService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_sms_forward_title))
            .setContentText(getString(R.string.notification_sms_forward_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOnlyAlertOnce(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        val request = intent?.toForwardRequest()
        if (request == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        scope.launch {
            val outcome = SmsForwarder(applicationContext).forwardOrQueue(request)
            SmsReliabilityManager.ensureScheduled(applicationContext)
            if (outcome.queued) {
                SmsReliabilityManager.enqueueImmediateRetry(applicationContext)
            }
            SmsReliabilityNotifier.notifyForwardOutcome(applicationContext, outcome)
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    private fun Intent.toForwardRequest(): SmsForwardRequest? {
        val sender = getStringExtra(SmsForwardDispatcher.EXTRA_SENDER)?.takeIf { it.isNotBlank() }
            ?: return null
        val content = getStringExtra(SmsForwardDispatcher.EXTRA_CONTENT)?.takeIf { it.isNotBlank() }
            ?: return null
        val timestamp = getLongExtra(SmsForwardDispatcher.EXTRA_TIMESTAMP, System.currentTimeMillis())
        val source = getStringExtra(SmsForwardDispatcher.EXTRA_SOURCE).orEmpty().ifBlank { "unknown" }
        return SmsForwardRequest(
            sender = sender,
            content = content,
            timestamp = timestamp,
            source = source
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_sms_forward_channel),
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "sms_forward_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
