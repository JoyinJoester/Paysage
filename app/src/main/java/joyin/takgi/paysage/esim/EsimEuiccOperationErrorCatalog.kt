package joyin.takgi.paysage.esim

import android.content.Context
import android.telephony.euicc.EuiccManager
import joyin.takgi.paysage.R

data class EsimEuiccOperationExplanation(
    val title: String,
    val detail: String,
    val recoveryHint: String
)

object EsimEuiccOperationErrorCatalog {
    fun explain(context: Context, result: EsimDownloadResult): EsimEuiccOperationExplanation? {
        if (!canExplain(result)) return null

        val title = result.errorCode?.let { code ->
            errorLabelText(context, code)
        } ?: when (result.status) {
            EsimDownloadStatus.NeedsConfirmation -> context.getString(R.string.title_esim_system_confirmation_required)
            else -> context.getString(R.string.title_esim_system_operation_incomplete)
        }
        val detail = listOfNotNull(
            result.operationCode?.let {
                context.getString(
                    R.string.format_esim_operation_phase,
                    operationLabelText(context, it)
                )
            },
            result.smdxSubjectCode?.let { "Subject $it" },
            result.smdxReasonCode?.let { "Reason $it" }
        ).joinToString(" / ").ifBlank { context.getString(R.string.message_esim_no_detailed_diagnostics) }

        return EsimEuiccOperationExplanation(
            title = title,
            detail = detail,
            recoveryHint = context.getString(recoveryHintResId(result.errorCode))
        )
    }

    /** 是否存在可解释的诊断信息,纯逻辑便于单元测试 */
    fun canExplain(result: EsimDownloadResult): Boolean {
        if (result.status != EsimDownloadStatus.Failed && result.status != EsimDownloadStatus.NeedsConfirmation) {
            return false
        }
        return result.errorCode != null || result.operationCode != null ||
            result.smdxSubjectCode != null || result.smdxReasonCode != null
    }

    fun errorLabel(context: Context, errorCode: Int): String = errorLabelText(context, errorCode)

    fun operationLabel(context: Context, operationCode: Int): String =
        operationLabelText(context, operationCode)

    fun recoveryHint(context: Context, errorCode: Int?): String =
        context.getString(recoveryHintResId(errorCode))

    fun isKnownErrorCode(errorCode: Int): Boolean =
        errorLabelResId(errorCode) != R.string.format_error_esim_unknown

    fun isKnownOperationCode(operationCode: Int): Boolean =
        operationLabelResId(operationCode) != R.string.format_operation_esim_unknown

    // 未知码的资源是带 %1$d 占位符的模板,需要带参格式化
    private fun errorLabelText(context: Context, errorCode: Int): String =
        if (isKnownErrorCode(errorCode)) {
            context.getString(errorLabelResId(errorCode))
        } else {
            context.getString(R.string.format_error_esim_unknown, errorCode)
        }

    private fun operationLabelText(context: Context, operationCode: Int): String =
        if (isKnownOperationCode(operationCode)) {
            context.getString(operationLabelResId(operationCode))
        } else {
            context.getString(R.string.format_operation_esim_unknown, operationCode)
        }

    fun errorLabelResId(errorCode: Int): Int = when (errorCode) {
        EuiccManager.ERROR_CARRIER_LOCKED -> R.string.error_esim_carrier_locked
        EuiccManager.ERROR_INVALID_ACTIVATION_CODE -> R.string.error_esim_invalid_activation_code
        EuiccManager.ERROR_INVALID_CONFIRMATION_CODE -> R.string.error_esim_invalid_confirmation_code
        EuiccManager.ERROR_INCOMPATIBLE_CARRIER -> R.string.error_esim_incompatible_carrier
        EuiccManager.ERROR_EUICC_INSUFFICIENT_MEMORY -> R.string.error_esim_euicc_insufficient_memory
        EuiccManager.ERROR_TIME_OUT -> R.string.error_esim_timeout
        EuiccManager.ERROR_EUICC_MISSING -> R.string.error_esim_euicc_missing
        EuiccManager.ERROR_UNSUPPORTED_VERSION -> R.string.error_esim_unsupported_version
        EuiccManager.ERROR_SIM_MISSING -> R.string.error_esim_sim_missing
        EuiccManager.ERROR_INSTALL_PROFILE -> R.string.error_esim_install_profile
        EuiccManager.ERROR_DISALLOWED_BY_PPR -> R.string.error_esim_disallowed_by_ppr
        EuiccManager.ERROR_ADDRESS_MISSING -> R.string.error_esim_address_missing
        EuiccManager.ERROR_CERTIFICATE_ERROR -> R.string.error_esim_certificate_error
        EuiccManager.ERROR_NO_PROFILES_AVAILABLE -> R.string.error_esim_no_profiles_available
        EuiccManager.ERROR_CONNECTION_ERROR -> R.string.error_esim_connection_error
        EuiccManager.ERROR_INVALID_RESPONSE -> R.string.error_esim_invalid_response
        EuiccManager.ERROR_OPERATION_BUSY -> R.string.error_esim_operation_busy
        EuiccManager.ERROR_INVALID_PORT -> R.string.error_esim_invalid_port
        else -> R.string.format_error_esim_unknown
    }

    fun operationLabelResId(operationCode: Int): Int = when (operationCode) {
        EuiccManager.OPERATION_SYSTEM -> R.string.operation_esim_system_service
        EuiccManager.OPERATION_SIM_SLOT -> R.string.operation_esim_sim_slot
        EuiccManager.OPERATION_EUICC_CARD -> R.string.operation_esim_euicc_card
        EuiccManager.OPERATION_SWITCH -> R.string.operation_esim_profile_switch
        EuiccManager.OPERATION_DOWNLOAD -> R.string.operation_esim_profile_download
        EuiccManager.OPERATION_METADATA -> R.string.operation_esim_profile_metadata
        EuiccManager.OPERATION_EUICC_GSMA -> R.string.operation_esim_gsma_flow
        EuiccManager.OPERATION_APDU -> R.string.operation_esim_apdu
        EuiccManager.OPERATION_SMDX -> R.string.operation_esim_smdx
        EuiccManager.OPERATION_SMDX_SUBJECT_REASON_CODE -> R.string.operation_esim_smdx_subject_reason
        EuiccManager.OPERATION_HTTP -> R.string.operation_esim_http
        else -> R.string.format_operation_esim_unknown
    }

    fun recoveryHintResId(errorCode: Int?): Int = when (errorCode) {
        EuiccManager.ERROR_INVALID_ACTIVATION_CODE,
        EuiccManager.ERROR_ADDRESS_MISSING -> R.string.recovery_esim_check_activation_code
        EuiccManager.ERROR_INVALID_CONFIRMATION_CODE -> R.string.recovery_esim_check_confirmation_code
        EuiccManager.ERROR_CARRIER_LOCKED,
        EuiccManager.ERROR_INCOMPATIBLE_CARRIER -> R.string.recovery_esim_check_carrier
        EuiccManager.ERROR_EUICC_INSUFFICIENT_MEMORY -> R.string.recovery_esim_free_profile_space
        EuiccManager.ERROR_OPERATION_BUSY -> R.string.recovery_esim_wait_operation
        EuiccManager.ERROR_TIME_OUT,
        EuiccManager.ERROR_CONNECTION_ERROR -> R.string.recovery_esim_retry_stable_network
        EuiccManager.ERROR_INVALID_PORT -> R.string.recovery_esim_refresh_port
        EuiccManager.ERROR_EUICC_MISSING,
        EuiccManager.ERROR_SIM_MISSING -> R.string.recovery_esim_check_device_service
        EuiccManager.ERROR_CERTIFICATE_ERROR,
        EuiccManager.ERROR_INVALID_RESPONSE -> R.string.recovery_esim_contact_carrier
        EuiccManager.ERROR_DISALLOWED_BY_PPR -> R.string.recovery_esim_policy_blocked
        else -> R.string.recovery_esim_use_system_fallback
    }
}
