package joyin.takgi.paysage.reliability

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import joyin.takgi.paysage.reliability.root.RootKeepAliveScript
import joyin.takgi.paysage.reliability.root.RootPrivilegeManager
import joyin.takgi.paysage.reliability.root.RootSettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            PaysageBackgroundGuard.ensureAggressive(context)
            restoreRootEnhancements(context)
        }
    }

    private fun restoreRootEnhancements(context: Context) {
        val store = RootSettingsStore(context)
        if (!store.keepAliveScriptEnabled && !store.smsFallbackEnabled) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (store.keepAliveScriptEnabled) {
                    RootKeepAliveScript.install(context)
                    RootPrivilegeManager.grantAll(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
