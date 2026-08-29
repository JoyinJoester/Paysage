package joyin.takgi.paysage.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import joyin.takgi.paysage.reliability.CallAlertDispatcher
import joyin.takgi.paysage.reliability.CallAlertKind
import joyin.takgi.paysage.reliability.CallAlertRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PhoneCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        if (state != TelephonyManager.EXTRA_STATE_RINGING) return

        val pendingResult = goAsync()
        val request = CallAlertRequest(
            number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER).orEmpty(),
            kind = CallAlertKind.Incoming,
            timestamp = System.currentTimeMillis(),
            source = "phone_state"
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                CallAlertDispatcher(context).dispatch(request)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
