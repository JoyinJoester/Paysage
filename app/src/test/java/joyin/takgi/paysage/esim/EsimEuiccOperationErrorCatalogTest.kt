package joyin.takgi.paysage.esim

import android.telephony.euicc.EuiccManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EsimEuiccOperationErrorCatalogTest {
    @Test
    fun explainsKnownFailureWithRecoveryHint() {
        val result = EsimDownloadResult(
            requestId = "req",
            status = EsimDownloadStatus.Failed,
            message = "failed",
            resultCode = EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_ERROR,
            detailedCode = null,
            operationCode = EuiccManager.OPERATION_DOWNLOAD,
            errorCode = EuiccManager.ERROR_INVALID_ACTIVATION_CODE,
            smdxSubjectCode = null,
            smdxReasonCode = null,
            updatedAtMillis = 0L
        )

        val explanation = EsimEuiccOperationErrorCatalog.explain(result)

        assertNotNull(explanation)
        assertEquals("激活码无效", explanation!!.title)
        assertTrue(explanation.detail.contains("profile 下载"))
        assertTrue(explanation.recoveryHint.contains("LPA"))
    }

    @Test
    fun skipsIdleResultWithoutErrorFields() {
        assertNull(EsimEuiccOperationErrorCatalog.explain(EsimDownloadResult.Idle))
    }

    @Test
    fun keepsUnknownErrorCodesReadable() {
        assertEquals("未知 eSIM 错误 98765", EsimEuiccOperationErrorCatalog.errorLabel(98765))
        assertEquals("未知阶段 123", EsimEuiccOperationErrorCatalog.operationLabel(123))
    }
}
