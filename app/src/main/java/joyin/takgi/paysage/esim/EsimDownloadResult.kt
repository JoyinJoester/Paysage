package joyin.takgi.paysage.esim

import android.content.Context
import android.telephony.euicc.EuiccManager
import joyin.takgi.paysage.R

enum class EsimDownloadStatus {
    Idle,
    Pending,
    NeedsConfirmation,
    Succeeded,
    Failed
}

data class EsimDownloadResult(
    val requestId: String,
    val status: EsimDownloadStatus,
    val message: String,
    val resultCode: Int?,
    val detailedCode: Int?,
    val operationCode: Int?,
    val errorCode: Int?,
    val smdxSubjectCode: String?,
    val smdxReasonCode: String?,
    val updatedAtMillis: Long
) {
    companion object {
        val Idle = EsimDownloadResult(
            requestId = "",
            status = EsimDownloadStatus.Idle,
            message = "还没有发起过 eSIM 下载请求。",
            resultCode = null,
            detailedCode = null,
            operationCode = null,
            errorCode = null,
            smdxSubjectCode = null,
            smdxReasonCode = null,
            updatedAtMillis = 0L
        )
    }
}

class EsimDownloadResultStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = context.applicationContext.getSharedPreferences(
        "paysage_esim_download_result",
        Context.MODE_PRIVATE
    )

    fun read(): EsimDownloadResult {
        val statusName = preferences.getString(KEY_STATUS, null) ?: return EsimDownloadResult.Idle
        val status = EsimDownloadStatus.entries.firstOrNull { it.name == statusName }
            ?: return EsimDownloadResult.Idle
        return EsimDownloadResult(
            requestId = preferences.getString(KEY_REQUEST_ID, "").orEmpty(),
            status = status,
            message = preferences.getString(KEY_MESSAGE, "").orEmpty(),
            resultCode = preferences.intOrNull(KEY_RESULT_CODE),
            detailedCode = preferences.intOrNull(KEY_DETAILED_CODE),
            operationCode = preferences.intOrNull(KEY_OPERATION_CODE),
            errorCode = preferences.intOrNull(KEY_ERROR_CODE),
            smdxSubjectCode = preferences.getString(KEY_SMDX_SUBJECT, null),
            smdxReasonCode = preferences.getString(KEY_SMDX_REASON, null),
            updatedAtMillis = preferences.getLong(KEY_UPDATED_AT, 0L)
        )
    }

    fun readHistory(): List<EsimDownloadResult> {
        val count = preferences.getInt(KEY_HISTORY_COUNT, 0)
            .coerceIn(0, EsimDownloadHistoryPolicy.MAX_HISTORY_ITEMS)
        return (0 until count).mapNotNull { index ->
            val statusName = preferences.getString(historyKey(index, KEY_STATUS), null)
                ?: return@mapNotNull null
            val status = EsimDownloadStatus.entries.firstOrNull { it.name == statusName }
                ?: return@mapNotNull null
            EsimDownloadResult(
                requestId = preferences.getString(historyKey(index, KEY_REQUEST_ID), "").orEmpty(),
                status = status,
                message = preferences.getString(historyKey(index, KEY_MESSAGE), "").orEmpty(),
                resultCode = preferences.intOrNull(historyKey(index, KEY_RESULT_CODE)),
                detailedCode = preferences.intOrNull(historyKey(index, KEY_DETAILED_CODE)),
                operationCode = preferences.intOrNull(historyKey(index, KEY_OPERATION_CODE)),
                errorCode = preferences.intOrNull(historyKey(index, KEY_ERROR_CODE)),
                smdxSubjectCode = preferences.getString(historyKey(index, KEY_SMDX_SUBJECT), null),
                smdxReasonCode = preferences.getString(historyKey(index, KEY_SMDX_REASON), null),
                updatedAtMillis = preferences.getLong(historyKey(index, KEY_UPDATED_AT), 0L)
            )
        }
    }

    fun clearHistory() {
        val editor = preferences.edit()
        repeat(EsimDownloadHistoryPolicy.MAX_HISTORY_ITEMS) { index ->
            removeHistoryItem(editor, index)
        }
        editor.putInt(KEY_HISTORY_COUNT, 0).apply()
    }

    fun markPending(requestId: String) {
        markPending(requestId, appContext.getString(R.string.message_esim_result_pending))
    }

    fun markPending(requestId: String, message: String) {
        write(
            EsimDownloadResult(
                requestId = requestId,
                status = EsimDownloadStatus.Pending,
                message = message,
                resultCode = null,
                detailedCode = null,
                operationCode = null,
                errorCode = null,
                smdxSubjectCode = null,
                smdxReasonCode = null,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    fun write(result: EsimDownloadResult) {
        preferences.edit()
            .putString(KEY_REQUEST_ID, result.requestId)
            .putString(KEY_STATUS, result.status.name)
            .putString(KEY_MESSAGE, result.message)
            .putNullableInt(KEY_RESULT_CODE, result.resultCode)
            .putNullableInt(KEY_DETAILED_CODE, result.detailedCode)
            .putNullableInt(KEY_OPERATION_CODE, result.operationCode)
            .putNullableInt(KEY_ERROR_CODE, result.errorCode)
            .putString(KEY_SMDX_SUBJECT, result.smdxSubjectCode)
            .putString(KEY_SMDX_REASON, result.smdxReasonCode)
            .putLong(KEY_UPDATED_AT, result.updatedAtMillis)
            .apply()
        writeHistory(EsimDownloadHistoryPolicy.upsert(readHistory(), result))
    }

    private fun writeHistory(history: List<EsimDownloadResult>) {
        val editor = preferences.edit()
        val bounded = history.take(EsimDownloadHistoryPolicy.MAX_HISTORY_ITEMS)
        editor.putInt(KEY_HISTORY_COUNT, bounded.size)
        bounded.forEachIndexed { index, result ->
            editor
                .putString(historyKey(index, KEY_REQUEST_ID), result.requestId)
                .putString(historyKey(index, KEY_STATUS), result.status.name)
                .putString(historyKey(index, KEY_MESSAGE), result.message)
                .putNullableInt(historyKey(index, KEY_RESULT_CODE), result.resultCode)
                .putNullableInt(historyKey(index, KEY_DETAILED_CODE), result.detailedCode)
                .putNullableInt(historyKey(index, KEY_OPERATION_CODE), result.operationCode)
                .putNullableInt(historyKey(index, KEY_ERROR_CODE), result.errorCode)
                .putString(historyKey(index, KEY_SMDX_SUBJECT), result.smdxSubjectCode)
                .putString(historyKey(index, KEY_SMDX_REASON), result.smdxReasonCode)
                .putLong(historyKey(index, KEY_UPDATED_AT), result.updatedAtMillis)
        }
        for (index in bounded.size until EsimDownloadHistoryPolicy.MAX_HISTORY_ITEMS) {
            removeHistoryItem(editor, index)
        }
        editor.apply()
    }

    private fun removeHistoryItem(
        editor: android.content.SharedPreferences.Editor,
        index: Int
    ) {
        editor
            .remove(historyKey(index, KEY_REQUEST_ID))
            .remove(historyKey(index, KEY_STATUS))
            .remove(historyKey(index, KEY_MESSAGE))
            .remove(historyKey(index, KEY_RESULT_CODE))
            .remove(historyKey(index, KEY_DETAILED_CODE))
            .remove(historyKey(index, KEY_OPERATION_CODE))
            .remove(historyKey(index, KEY_ERROR_CODE))
            .remove(historyKey(index, KEY_SMDX_SUBJECT))
            .remove(historyKey(index, KEY_SMDX_REASON))
            .remove(historyKey(index, KEY_UPDATED_AT))
    }

    private fun historyKey(index: Int, key: String): String = "history_${index}_$key"

    companion object {
        private const val KEY_HISTORY_COUNT = "history_count"
        private const val KEY_REQUEST_ID = "request_id"
        private const val KEY_STATUS = "status"
        private const val KEY_MESSAGE = "message"
        private const val KEY_RESULT_CODE = "result_code"
        private const val KEY_DETAILED_CODE = "detailed_code"
        private const val KEY_OPERATION_CODE = "operation_code"
        private const val KEY_ERROR_CODE = "error_code"
        private const val KEY_SMDX_SUBJECT = "smdx_subject"
        private const val KEY_SMDX_REASON = "smdx_reason"
        private const val KEY_UPDATED_AT = "updated_at"
    }
}

object EsimDownloadHistoryPolicy {
    const val MAX_HISTORY_ITEMS: Int = 12

    fun upsert(
        existing: List<EsimDownloadResult>,
        result: EsimDownloadResult
    ): List<EsimDownloadResult> {
        if (result.requestId.isBlank()) return existing.take(MAX_HISTORY_ITEMS)
        return (listOf(result) + existing.filterNot { it.requestId == result.requestId })
            .take(MAX_HISTORY_ITEMS)
    }

    fun title(context: Context, result: EsimDownloadResult): String {
        val operation = when {
            result.requestId.startsWith("download-") -> context.getString(R.string.operation_download_esim)
            result.requestId.startsWith("switch-port-") -> context.getString(R.string.operation_switch_port)
            result.requestId.startsWith("switch-") -> context.getString(R.string.operation_switch_esim)
            result.requestId.startsWith("delete-") -> context.getString(R.string.operation_delete_esim)
            result.requestId.startsWith("rename-") -> context.getString(R.string.operation_rename_esim)
            else -> context.getString(R.string.operation_esim_system)
        }
        return context.getString(R.string.format_esim_history_title, operation, result.status.label(context))
    }
}

fun EsimDownloadStatus.label(context: Context): String = when (this) {
    EsimDownloadStatus.Idle -> context.getString(R.string.status_esim_idle)
    EsimDownloadStatus.Pending -> context.getString(R.string.status_esim_pending)
    EsimDownloadStatus.NeedsConfirmation -> context.getString(R.string.status_esim_needs_confirmation)
    EsimDownloadStatus.Succeeded -> context.getString(R.string.status_esim_succeeded)
    EsimDownloadStatus.Failed -> context.getString(R.string.status_esim_failed)
}

object EsimDownloadResultMapper {
    fun fromCallback(
        context: Context,
        requestId: String,
        resultCode: Int,
        detailedCode: Int?,
        operationCode: Int?,
        errorCode: Int?,
        smdxSubjectCode: String?,
        smdxReasonCode: String?
    ): EsimDownloadResult {
        val status = when (resultCode) {
            EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_OK -> EsimDownloadStatus.Succeeded
            EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_RESOLVABLE_ERROR -> EsimDownloadStatus.NeedsConfirmation
            else -> EsimDownloadStatus.Failed
        }
        return EsimDownloadResult(
            requestId = requestId,
            status = status,
            message = messageFor(context, status, detailedCode, operationCode, errorCode, smdxSubjectCode, smdxReasonCode),
            resultCode = resultCode,
            detailedCode = detailedCode,
            operationCode = operationCode,
            errorCode = errorCode,
            smdxSubjectCode = smdxSubjectCode,
            smdxReasonCode = smdxReasonCode,
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    fun failure(requestId: String, message: String): EsimDownloadResult =
        EsimDownloadResult(
            requestId = requestId,
            status = EsimDownloadStatus.Failed,
            message = message,
            resultCode = EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_ERROR,
            detailedCode = null,
            operationCode = null,
            errorCode = null,
            smdxSubjectCode = null,
            smdxReasonCode = null,
            updatedAtMillis = System.currentTimeMillis()
        )

    private fun messageFor(
        context: Context,
        status: EsimDownloadStatus,
        detailedCode: Int?,
        operationCode: Int?,
        errorCode: Int?,
        smdxSubjectCode: String?,
        smdxReasonCode: String?
    ): String {
        return when (status) {
            EsimDownloadStatus.Succeeded -> context.getString(R.string.message_esim_result_succeeded)
            EsimDownloadStatus.NeedsConfirmation -> context.getString(R.string.message_esim_result_needs_confirmation)
            EsimDownloadStatus.Failed -> {
                val details = listOfNotNull(
                    detailedCode?.let { context.getString(R.string.format_esim_detail_code, it) },
                    operationCode?.let { context.getString(R.string.format_esim_operation_code, it) },
                    errorCode?.let { context.getString(R.string.format_esim_error_code, it) },
                    smdxSubjectCode?.let { "Subject $it" },
                    smdxReasonCode?.let { "Reason $it" }
                ).joinToString(context.getString(R.string.separator_list))
                if (details.isBlank()) {
                    context.getString(R.string.message_esim_result_failed)
                } else {
                    context.getString(R.string.format_esim_result_failed_details, details)
                }
            }
            EsimDownloadStatus.Idle -> context.getString(R.string.message_esim_result_idle)
            EsimDownloadStatus.Pending -> context.getString(R.string.message_esim_result_pending)
        }
    }
}

private fun android.content.SharedPreferences.intOrNull(key: String): Int? =
    if (contains(key)) getInt(key, 0) else null

private fun android.content.SharedPreferences.Editor.putNullableInt(
    key: String,
    value: Int?
): android.content.SharedPreferences.Editor =
    if (value == null) remove(key) else putInt(key, value)
