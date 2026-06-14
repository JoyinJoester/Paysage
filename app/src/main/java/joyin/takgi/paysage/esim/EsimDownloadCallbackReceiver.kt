package joyin.takgi.paysage.esim

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.euicc.EuiccManager

class EsimDownloadCallbackReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val requestId = intent.getStringExtra(EXTRA_REQUEST_ID).orEmpty()
        val result = EsimDownloadResultMapper.fromCallback(
            context = context,
            requestId = requestId,
            resultCode = resultCode,
            detailedCode = intent.optionalInt(EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_DETAILED_CODE),
            operationCode = intent.optionalInt(EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_OPERATION_CODE),
            errorCode = intent.optionalInt(EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_ERROR_CODE),
            smdxSubjectCode = intent.getStringExtra(EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_SMDX_SUBJECT_CODE),
            smdxReasonCode = intent.getStringExtra(EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_SMDX_REASON_CODE)
        )
        EsimDownloadResultStore(context).write(result)
        EsimOperationNotifier.notifyResult(context, result)

        if (result.status == EsimDownloadStatus.NeedsConfirmation &&
            intent.hasExtra(EXTRA_EMBEDDED_SUBSCRIPTION_RESOLUTION_INTENT)
        ) {
            val resolutionIntent = Intent(context, EsimResolutionActivity::class.java)
                .putExtra(EsimResolutionActivity.EXTRA_REQUEST_ID, requestId)
                .putExtra(EsimResolutionActivity.EXTRA_RESULT_INTENT, intent)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(resolutionIntent)
        }
    }

    companion object {
        const val EXTRA_REQUEST_ID = "joyin.takgi.paysage.esim.extra.REQUEST_ID"
        private const val EXTRA_EMBEDDED_SUBSCRIPTION_RESOLUTION_INTENT =
            "android.telephony.euicc.extra.EMBEDDED_SUBSCRIPTION_RESOLUTION_INTENT"
        private const val ACTION_DOWNLOAD_CALLBACK = "joyin.takgi.paysage.esim.action.DOWNLOAD_CALLBACK"

        fun pendingIntent(context: Context, requestId: String): PendingIntent {
            val intent = Intent(context, EsimDownloadCallbackReceiver::class.java)
                .setAction(ACTION_DOWNLOAD_CALLBACK)
                .putExtra(EXTRA_REQUEST_ID, requestId)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE
                } else {
                    0
                }
            return PendingIntent.getBroadcast(context, requestId.hashCode(), intent, flags)
        }
    }
}

private fun Intent.optionalInt(key: String): Int? =
    if (hasExtra(key)) getIntExtra(key, 0) else null
