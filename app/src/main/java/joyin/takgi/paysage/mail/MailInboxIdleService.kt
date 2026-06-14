package joyin.takgi.paysage.mail

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sun.mail.imap.IMAPFolder
import joyin.takgi.paysage.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Folder
import javax.mail.MessagingException
import javax.mail.Session

class MailInboxIdleService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listening = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, foregroundNotification(getString(R.string.notification_mail_idle_waiting)))

        if (!listening) {
            listening = true
            scope.launch {
                listenLoop()
            }
        }

        return START_STICKY
    }

    private suspend fun listenLoop() {
        val settingsStore = MailInboxRealtimeSettingsStore(applicationContext)
        val accountStore = MailInboxAccountStore(applicationContext)
        val runtimeStore = MailInboxRuntimeStore(applicationContext)
        val repository = MailInboxRepository(applicationContext)

        while (scope.isActive) {
            val settings = settingsStore.read()
            val account = accountStore.read()
            if (!settings.enabled || !account.enabled || !account.isConfigured) {
                stopSelf()
                break
            }

            val idleResult = runCatching {
                idleUntilChanged(account)
            }

            if (idleResult.isSuccess) {
                val result = repository.refreshInbox()
                MailInboxNotifier.notifyRejectedCommands(applicationContext, result)
                if (!result.success) {
                    MailInboxNotifier.notifyRefreshIssue(applicationContext, result)
                }
            } else {
                val error = idleResult.exceptionOrNull()
                val failureKind = error
                    ?.let(MailInboxFailureClassifier::classify)
                    ?: MailInboxFailureKind.NetworkOrServerFailed
                val message = error?.message
                    ?.takeIf { it.isNotBlank() }
                    ?.let { MailInboxPrivacySanitizer.redact(it) }
                    ?: getString(R.string.message_mail_idle_connection_lost)
                runtimeStore.recordFailure(
                    kind = failureKind,
                    message = message
                )
                MailInboxNotifier.notifyRefreshIssue(
                    applicationContext,
                    MailInboxRefreshResult(
                        success = false,
                        message = message,
                        failureKind = failureKind
                    )
                )
                if (MailInboxIdleFailurePolicy.shouldDisableRealtime(failureKind)) {
                    settingsStore.write(settings.copy(enabled = false))
                    stopSelf()
                    break
                }
            }

            delay(settings.idleReconnectMinutes.coerceIn(5, 60) * 60L * 1000L)
        }
    }

    private suspend fun idleUntilChanged(config: MailInboxAccountConfig) = withContext(Dispatchers.IO) {
        val protocol = if (config.useSsl) "imaps" else "imap"
        val session = Session.getInstance(buildProperties(config, protocol))
        val store = session.getStore(protocol)
        var folder: Folder? = null
        try {
            store.connect(config.host, config.port, config.username, config.password)
            folder = store.getFolder("INBOX")
            folder.open(Folder.READ_ONLY)
            val imapFolder = folder as? IMAPFolder
                ?: throw MessagingException("当前邮箱服务不支持 IMAP IDLE。")
            imapFolder.idle()
        } finally {
            runCatching { folder?.takeIf { it.isOpen }?.close(false) }
            runCatching { store.close() }
        }
    }

    private fun buildProperties(config: MailInboxAccountConfig, protocol: String): Properties =
        Properties().apply {
            put("mail.store.protocol", protocol)
            put("mail.$protocol.host", config.host)
            put("mail.$protocol.port", config.port.toString())
            put("mail.$protocol.connectiontimeout", CONNECTION_TIMEOUT_MS.toString())
            put("mail.$protocol.timeout", READ_TIMEOUT_MS.toString())
            if (config.useSsl) {
                put("mail.imaps.ssl.enable", "true")
                put("mail.imaps.ssl.protocols", "TLSv1.2 TLSv1.3")
            }
        }

    private fun foregroundNotification(text: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notification_mail_idle_title))
            .setContentText(text)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_mail_idle_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_mail_idle_channel_desc)
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        listening = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "paysage_mail_inbox_idle"
        private const val NOTIFICATION_ID = 6201
        private const val CONNECTION_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 15_000

        fun start(context: Context) {
            val appContext = context.applicationContext
            val intent = Intent(appContext, MailInboxIdleService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(intent)
            } else {
                appContext.startService(intent)
            }
        }

        fun stop(context: Context) {
            val appContext = context.applicationContext
            appContext.stopService(Intent(appContext, MailInboxIdleService::class.java))
        }
    }
}

object MailInboxIdleFailurePolicy {
    fun shouldDisableRealtime(failureKind: MailInboxFailureKind): Boolean =
        failureKind == MailInboxFailureKind.InvalidConfig ||
            failureKind == MailInboxFailureKind.AuthenticationFailed
}

object MailInboxRealtimeServiceController {
    fun shouldRun(
        account: MailInboxAccountConfig,
        settings: MailInboxRealtimeSettings
    ): Boolean =
        settings.enabled && account.enabled && account.isConfigured

    fun reconcile(
        context: Context,
        account: MailInboxAccountConfig,
        settings: MailInboxRealtimeSettings
    ): Boolean {
        if (!shouldRun(account, settings)) {
            MailInboxIdleService.stop(context)
            return false
        }
        return runCatching {
            MailInboxIdleService.start(context)
        }.isSuccess
    }
}
