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
        if (result.status != EsimDownloadStatus.Failed && result.status != EsimDownloadStatus.NeedsConfirmation) {
            return null
        }
        val error = result.errorCode?.let { errorLabel(context, it) }
        val operation = result.operationCode?.let { operationLabel(context, it) }
        if (error == null && operation == null && result.smdxSubjectCode == null && result.smdxReasonCode == null) {
            return null
        }

        val title = error ?: when (result.status) {
            EsimDownloadStatus.NeedsConfirmation -> context.getString(R.string.title_esim_system_confirmation_required)
            else -> context.getString(R.string.title_esim_system_operation_incomplete)
        }
        val detail = listOfNotNull(
            operation?.let { context.getString(R.string.format_esim_operation_phase, it) },
            result.smdxSubjectCode?.let { "Subject $it" },
            result.smdxReasonCode?.let { "Reason $it" }
        ).joinToString(" / ").ifBlank { context.getString(R.string.message_esim_no_detailed_diagnostics) }

        return EsimEuiccOperationExplanation(
            title = title,
            detail = detail,
            recoveryHint = recoveryHint(context, result.errorCode)
        )
    }

    fun errorLabel(context: Context, errorCode: Int): String = when (errorCode) {
        EuiccManager.ERROR_CARRIER_LOCKED -> context.getString(R.string.error_esim_carrier_locked)
        EuiccManager.ERROR_INVALID_ACTIVATION_CODE -> context.getString(R.string.error_esim_invalid_activation_code)
        EuiccManager.ERROR_INVALID_CONFIRMATION_CODE -> context.getString(R.string.error_esim_invalid_confirmation_code)
        EuiccManager.ERROR_INCOMPATIBLE_CARRIER -> context.getString(R.string.error_esim_incompatible_carrier)
        EuiccManager.ERROR_EUICC_INSUFFICIENT_MEMORY -> context.getString(R.string.error_esim_euicc_insufficient_memory)
        EuiccManager.ERROR_TIME_OUT -> context.getString(R.string.error_esim_timeout)
        EuiccManager.ERROR_EUICC_MISSING -> context.getString(R.string.error_esim_euicc_missing)
        EuiccManager.ERROR_UNSUPPORTED_VERSION -> context.getString(R.string.error_esim_unsupported_version)
        EuiccManager.ERROR_SIM_MISSING -> context.getString(R.string.error_esim_sim_missing)
        EuiccManager.ERROR_INSTALL_PROFILE -> context.getString(R.string.error_esim_install_profile)
        EuiccManager.ERROR_DISALLOWED_BY_PPR -> context.getString(R.string.error_esim_disallowed_by_ppr)
        EuiccManager.ERROR_ADDRESS_MISSING -> context.getString(R.string.error_esim_address_missing)
        EuiccManager.ERROR_CERTIFICATE_ERROR -> context.getString(R.string.error_esim_certificate_error)
        EuiccManager.ERROR_NO_PROFILES_AVAILABLE -> context.getString(R.string.error_esim_no_profiles_available)
        EuiccManager.ERROR_CONNECTION_ERROR -> context.getString(R.string.error_esim_connection_error)
        EuiccManager.ERROR_INVALID_RESPONSE -> context.getString(R.string.error_esim_invalid_response)
        EuiccManager.ERROR_OPERATION_BUSY -> context.getString(R.string.error_esim_operation_busy)
        EuiccManager.ERROR_INVALID_PORT -> context.getString(R.string.error_esim_invalid_port)
        else -> context.getString(R.string.format_error_esim_unknown, errorCode)
    }

    fun operationLabel(context: Context, operationCode: Int): String = when (operationCode) {
        EuiccManager.OPERATION_SYSTEM -> context.getString(R.string.operation_esim_system_service)
        EuiccManager.OPERATION_SIM_SLOT -> context.getString(R.string.operation_esim_sim_slot)
        EuiccManager.OPERATION_EUICC_CARD -> context.getString(R.string.operation_esim_euicc_card)
        EuiccManager.OPERATION_SWITCH -> context.getString(R.string.operation_esim_profile_switch)
        EuiccManager.OPERATION_DOWNLOAD -> context.getString(R.string.operation_esim_profile_download)
        EuiccManager.OPERATION_METADATA -> context.getString(R.string.operation_esim_profile_metadata)
        EuiccManager.OPERATION_EUICC_GSMA -> context.getString(R.string.operation_esim_gsma_flow)
        EuiccManager.OPERATION_APDU -> context.getString(R.string.operation_esim_apdu)
        EuiccManager.OPERATION_SMDX -> "SM-DP+/SM-DS"
        EuiccManager.OPERATION_SMDX_SUBJECT_REASON_CODE -> "SM-DP+ Subject/Reason"
        EuiccManager.OPERATION_HTTP -> context.getString(R.string.operation_esim_http)
        else -> context.getString(R.string.format_operation_esim_unknown, operationCode)
    }

    fun recoveryHint(context: Context, errorCode: Int?): String = when (errorCode) {
        EuiccManager.ERROR_INVALID_ACTIVATION_CODE,
        EuiccManager.ERROR_ADDRESS_MISSING -> context.getString(R.string.recovery_esim_check_activation_code)
        EuiccManager.ERROR_INVALID_CONFIRMATION_CODE -> context.getString(R.string.recovery_esim_check_confirmation_code)
        EuiccManager.ERROR_CARRIER_LOCKED,
        EuiccManager.ERROR_INCOMPATIBLE_CARRIER -> context.getString(R.string.recovery_esim_check_carrier)
        EuiccManager.ERROR_EUICC_INSUFFICIENT_MEMORY -> context.getString(R.string.recovery_esim_free_profile_space)
        EuiccManager.ERROR_OPERATION_BUSY -> context.getString(R.string.recovery_esim_wait_operation)
        EuiccManager.ERROR_TIME_OUT,
        EuiccManager.ERROR_CONNECTION_ERROR -> context.getString(R.string.recovery_esim_retry_stable_network)
        EuiccManager.ERROR_INVALID_PORT -> context.getString(R.string.recovery_esim_refresh_port)
        EuiccManager.ERROR_EUICC_MISSING,
        EuiccManager.ERROR_SIM_MISSING -> context.getString(R.string.recovery_esim_check_device_service)
        EuiccManager.ERROR_CERTIFICATE_ERROR,
        EuiccManager.ERROR_INVALID_RESPONSE -> context.getString(R.string.recovery_esim_contact_carrier)
        EuiccManager.ERROR_DISALLOWED_BY_PPR -> context.getString(R.string.recovery_esim_policy_blocked)
        else -> context.getString(R.string.recovery_esim_use_system_fallback)
    }
}
