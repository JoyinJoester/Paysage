package joyin.takgi.paysage.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import joyin.takgi.paysage.R
import joyin.takgi.paysage.reliability.SmsContentObserver
import joyin.takgi.paysage.reliability.SmsReliabilityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class SmsKeepAliveService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var observerThread: HandlerThread? = null
    private var observer: SmsContentObserver? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(getString(R.string.notification_sms_guardian_title))
                .setContentText(getString(R.string.notification_sms_guardian_text))
                .setOngoing(true)
                .setShowWhen(false)
                .build()
        )
        SmsReliabilityManager.ensureScheduled(this)
        registerSmsObserver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        SmsReliabilityManager.ensureScheduled(this)
        return START_STICKY
    }

    override fun onDestroy() {
        observer?.let { contentResolver.unregisterContentObserver(it) }
        observer = null
        observerThread?.quitSafely()
        observerThread = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun registerSmsObserver() {
        if (observer != null) return
        val thread = HandlerThread("PaysageSmsObserver").also { it.start() }
        val contentObserver = SmsContentObserver(
            context = applicationContext,
            handler = Handler(thread.looper),
            scope = scope
        )
        contentResolver.registerContentObserver(
            Telephony.Sms.CONTENT_URI,
            true,
            contentObserver
        )
        observerThread = thread
        observer = contentObserver
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_sms_guardian_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_sms_guardian_channel_desc)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "sms_keep_alive_channel"
        private const val NOTIFICATION_ID = 2101
    }
}
