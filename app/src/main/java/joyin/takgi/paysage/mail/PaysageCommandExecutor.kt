package joyin.takgi.paysage.mail

import android.content.Context
import joyin.takgi.paysage.R
import joyin.takgi.paysage.data.AppDatabase
import joyin.takgi.paysage.reliability.SmsForwardingControlStore
import joyin.takgi.paysage.reliability.SmsReliabilityManager
import joyin.takgi.paysage.util.DeviceStatusCollector
import joyin.takgi.paysage.util.NetworkSpeedCollector

class PaysageCommandExecutor(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.getDatabase(appContext)
    private val forwardingControlStore = SmsForwardingControlStore(appContext)

    suspend fun execute(command: ParsedMailCommand): String {
        return when (command.action) {
            MailCommandAction.Status -> {
                val paused = forwardingControlStore.isPaused()
                val pendingCount = database.pendingForwardDao().pendingCount()
                if (paused) {
                    appContext.getString(R.string.format_mail_status_forwarding_paused, pendingCount)
                } else {
                    appContext.getString(R.string.format_mail_status_forwarding_running, pendingCount)
                }
            }
            MailCommandAction.DeviceStatus -> {
                val status = DeviceStatusCollector.collectStatus(appContext)
                DeviceStatusCollector.formatStatusReport(appContext, status)
            }
            MailCommandAction.NetworkSpeed -> {
                val result = NetworkSpeedCollector.collectNetworkSpeed(appContext, useCache = true)
                NetworkSpeedCollector.formatNetworkSpeedReport(appContext, result)
            }
            MailCommandAction.RetryCache -> {
                SmsReliabilityManager.enqueueImmediateRetry(appContext)
                appContext.getString(R.string.message_mail_retry_cache_scheduled)
            }
            MailCommandAction.PauseForwarding -> {
                forwardingControlStore.setPaused(true)
                appContext.getString(R.string.message_sms_forwarding_paused)
            }
            MailCommandAction.ResumeForwarding -> {
                forwardingControlStore.setPaused(false)
                SmsReliabilityManager.enqueueImmediateRetry(appContext)
                appContext.getString(R.string.message_sms_forwarding_resumed_retry_scheduled)
            }
            MailCommandAction.ToggleForwarding -> {
                val nextPaused = !forwardingControlStore.isPaused()
                forwardingControlStore.setPaused(nextPaused)
                if (nextPaused) {
                    appContext.getString(R.string.message_sms_forwarding_paused)
                } else {
                    SmsReliabilityManager.enqueueImmediateRetry(appContext)
                    appContext.getString(R.string.message_sms_forwarding_resumed_retry_scheduled)
                }
            }
        }
    }
}
