package joyin.takgi.paysage.reliability

import android.content.Context
import joyin.takgi.paysage.mail.MailInboxAccountStore
import joyin.takgi.paysage.mail.MailInboxReliabilityManager
import joyin.takgi.paysage.mail.MailInboxRealtimeSettingsStore
import joyin.takgi.paysage.mail.MailInboxRealtimeServiceController
import joyin.takgi.paysage.telegram.TelegramCommandReliabilityManager

object PaysageBackgroundGuard {
    fun ensure(context: Context) {
        val appContext = context.applicationContext
        SmsReliabilityManager.ensureScheduled(appContext)
        BatteryAlertReliabilityManager.ensureScheduled(appContext)
        MailInboxReliabilityManager.ensureScheduled(appContext)
        TelegramCommandReliabilityManager.ensureScheduled(appContext)
        restoreMailRealtime(appContext)
    }

    fun ensureAggressive(context: Context) {
        val appContext = context.applicationContext
        ensure(appContext)
        SmsReliabilityManager.startKeepAlive(appContext)
        BatteryAlertReliabilityManager.enqueueImmediateCheck(appContext)
        MailInboxReliabilityManager.enqueueImmediateCheck(appContext)
        TelegramCommandReliabilityManager.enqueueImmediateCheck(appContext)
    }

    fun restoreMailRealtime(context: Context): Boolean {
        val appContext = context.applicationContext
        val account = MailInboxAccountStore(appContext).read()
        val realtimeSettings = MailInboxRealtimeSettingsStore(appContext).read()
        return MailInboxRealtimeServiceController.reconcile(appContext, account, realtimeSettings)
    }
}
