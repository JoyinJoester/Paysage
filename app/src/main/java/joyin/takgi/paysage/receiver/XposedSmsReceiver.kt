package joyin.takgi.paysage.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import joyin.takgi.paysage.reliability.SmsForwardRequest
import joyin.takgi.paysage.reliability.SmsSegmentBuffer
import joyin.takgi.paysage.xposed.PaysageXposedModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 接收 LSPosed 模块从 com.android.phone 进程转发的短信。
 * 只有持有 MODIFY_PHONE_STATE 的系统组件能发进来。
 */
class XposedSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != PaysageXposedModule.ACTION_XPOSED_SMS) return
        val sender = intent.getStringExtra(PaysageXposedModule.EXTRA_SENDER)?.takeIf { it.isNotBlank() }
            ?: return
        val content = intent.getStringExtra(PaysageXposedModule.EXTRA_CONTENT)?.takeIf { it.isNotBlank() }
            ?: return
        val timestamp = intent.getLongExtra(PaysageXposedModule.EXTRA_TIMESTAMP, System.currentTimeMillis())

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SmsSegmentBuffer.enqueue(
                    context,
                    SmsForwardRequest(
                        sender = sender,
                        content = content,
                        timestamp = timestamp,
                        source = "xposed"
                    )
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
