package joyin.takgi.paysage.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CallLog
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import joyin.takgi.paysage.R
import joyin.takgi.paysage.reliability.CallLogObserver
import joyin.takgi.paysage.reliability.MmsContentObserver
import joyin.takgi.paysage.reliability.SmsContentObserver
import joyin.takgi.paysage.reliability.SmsReliabilityManager
import joyin.takgi.paysage.telegram.TelegramCommandRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SmsKeepAliveService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var observerThread: HandlerThread? = null
    private var smsObserver: SmsContentObserver? = null
    private var mmsObserver: MmsContentObserver? = null
    private var callLogObserver: CallLogObserver? = null
    private var telegramCommandJob: Job? = null

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
        registerMmsObserver()
        registerCallLogObserver()
        startTelegramCommandLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        SmsReliabilityManager.ensureScheduled(this)
        registerSmsObserver()
        registerMmsObserver()
        registerCallLogObserver()
        startTelegramCommandLoop()
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        SmsReliabilityManager.ensureScheduled(this)
        SmsReliabilityManager.enqueueImmediateRetry(this)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        smsObserver?.let { contentResolver.unregisterContentObserver(it) }
        mmsObserver?.let { contentResolver.unregisterContentObserver(it) }
        callLogObserver?.let { contentResolver.unregisterContentObserver(it) }
        smsObserver = null
        callLogObserver = null
        observerThread?.quitSafely()
        observerThread = null
        telegramCommandJob?.cancel()
        telegramCommandJob = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun registerSmsObserver() {
        if (smsObserver != null) return
        val contentObserver = SmsContentObserver(
            context = applicationContext,
            handler = observerHandler(),
            scope = scope
        )
        contentResolver.registerContentObserver(
            Telephony.Sms.CONTENT_URI,
            true,
            contentObserver
        )
        smsObserver = contentObserver
    }

    private fun registerMmsObserver() {
        if (mmsObserver != null) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_MMS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val contentObserver = MmsContentObserver(
            context = applicationContext,
            handler = observerHandler(),
            scope = scope
        )
        contentResolver.registerContentObserver(
            Telephony.Mms.CONTENT_URI,
            true,
            contentObserver
        )
        mmsObserver = contentObserver
    }

    private fun registerCallLogObserver() {
        if (callLogObserver != null) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val contentObserver = CallLogObserver(
            context = applicationContext,
            handler = observerHandler(),
            scope = scope
        )
        contentResolver.registerContentObserver(
            CallLog.Calls.CONTENT_URI,
            true,
            contentObserver
        )
        callLogObserver = contentObserver
    }

    private fun observerHandler(): Handler {
        val thread = observerThread ?: HandlerThread("PaysageObservers").also {
            it.start()
            observerThread = it
        }
        return Handler(thread.looper)
    }

    private fun startTelegramCommandLoop() {
        if (telegramCommandJob?.isActive == true) return
        telegramCommandJob = scope.launch {
            val repository = TelegramCommandRepository(applicationContext)
            while (isActive) {
                val result = runCatching {
                    repository.refreshCommands(
                        pollTimeoutSeconds = TELEGRAM_COMMAND_LONG_POLL_SECONDS
                    )
                }.getOrNull()
                delay(
                    if (result?.hasAccounts == true) {
                        TELEGRAM_COMMAND_ACTIVE_POLL_DELAY_MS
                    } else {
                        TELEGRAM_COMMAND_IDLE_POLL_DELAY_MS
                    }
                )
            }
        }
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
        private const val TELEGRAM_COMMAND_LONG_POLL_SECONDS = 25
        private const val TELEGRAM_COMMAND_ACTIVE_POLL_DELAY_MS = 1_000L
        private const val TELEGRAM_COMMAND_IDLE_POLL_DELAY_MS = 60_000L
    }
}
