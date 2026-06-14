package joyin.takgi.paysage.reliability

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import joyin.takgi.paysage.mail.MailInboxAccountStore
import joyin.takgi.paysage.mail.MailInboxReliabilityManager
import joyin.takgi.paysage.mail.MailInboxRealtimeSettingsStore
import joyin.takgi.paysage.mail.MailInboxRealtimeServiceController

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            SmsReliabilityManager.ensureScheduled(context)
            SmsReliabilityManager.enqueueImmediateRetry(context)
            MailInboxReliabilityManager.ensureScheduled(context)
            val account = MailInboxAccountStore(context).read()
            val realtimeSettings = MailInboxRealtimeSettingsStore(context).read()
            MailInboxRealtimeServiceController.reconcile(context, account, realtimeSettings)
        }
    }
}
