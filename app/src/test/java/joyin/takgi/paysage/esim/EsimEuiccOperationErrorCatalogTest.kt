package joyin.takgi.paysage.esim

import android.telephony.euicc.EuiccManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EsimEuiccOperationErrorCatalogTest {
    // explain/errorLabel 需要 Context 生成文案,纯 JVM 测试改断言
    // canExplain 判定与 resId 分类映射
    private fun failedResult(
        errorCode: Int?,
        operationCode: Int? = null
    ) = EsimDownloadResult(
        requestId = "req",
        status = EsimDownloadStatus.Failed,
        message = "failed",
        resultCode = EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_ERROR,
        detailedCode = null,
        operationCode = operationCode,
        errorCode = errorCode,
        smdxSubjectCode = null,
        smdxReasonCode = null,
        updatedAtMillis = 0L
    )

    @Test
    fun knownErrorCodeMapsToDedicatedResource() {
        assertEquals(
            joyin.takgi.paysage.R.string.error_esim_invalid_activation_code,
            EsimEuiccOperationErrorCatalog.errorLabelResId(EuiccManager.ERROR_INVALID_ACTIVATION_CODE)
        )
        assertTrue(EsimEuiccOperationErrorCatalog.isKnownErrorCode(EuiccManager.ERROR_INVALID_ACTIVATION_CODE))
    }

    @Test
    fun unknownErrorCodeKeepsFormattedTemplate() {
        assertEquals(
            joyin.takgi.paysage.R.string.format_error_esim_unknown,
            EsimEuiccOperationErrorCatalog.errorLabelResId(98765)
        )
        assertFalse(EsimEuiccOperationErrorCatalog.isKnownErrorCode(98765))
    }

    @Test
    fun knownOperationCodeMapsToDedicatedResource() {
        assertEquals(
            joyin.takgi.paysage.R.string.operation_esim_profile_download,
            EsimEuiccOperationErrorCatalog.operationLabelResId(EuiccManager.OPERATION_DOWNLOAD)
        )
        assertEquals(
            joyin.takgi.paysage.R.string.format_operation_esim_unknown,
            EsimEuiccOperationErrorCatalog.operationLabelResId(123)
        )
    }

    @Test
    fun invalidActivationCodeGetsRecoveryHint() {
        assertEquals(
            joyin.takgi.paysage.R.string.recovery_esim_check_activation_code,
            EsimEuiccOperationErrorCatalog.recoveryHintResId(EuiccManager.ERROR_INVALID_ACTIVATION_CODE)
        )
        assertEquals(
            joyin.takgi.paysage.R.string.recovery_esim_use_system_fallback,
            EsimEuiccOperationErrorCatalog.recoveryHintResId(null)
        )
    }

    @Test
    fun canExplainRequiresFailureWithDiagnostics() {
        assertTrue(EsimEuiccOperationErrorCatalog.canExplain(failedResult(errorCode = 9)))
        assertTrue(
            EsimEuiccOperationErrorCatalog.canExplain(
                failedResult(errorCode = null, operationCode = EuiccManager.OPERATION_DOWNLOAD)
            )
        )
        assertFalse(EsimEuiccOperationErrorCatalog.canExplain(EsimDownloadResult.Idle))
        assertFalse(
            EsimEuiccOperationErrorCatalog.canExplain(
                failedResult(errorCode = null, operationCode = null)
            )
        )
    }
}
