package joyin.takgi.paysage.esim

import android.telephony.euicc.EuiccManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EsimDownloadResultMapperTest {
    @Test
    fun mapsSuccessResult() {
        val result = EsimDownloadResultMapper.fromCallback(
            requestId = "req-1",
            resultCode = EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_OK,
            detailedCode = null,
            operationCode = null,
            errorCode = null,
            smdxSubjectCode = null,
            smdxReasonCode = null
        )

        assertEquals(EsimDownloadStatus.Succeeded, result.status)
        assertEquals("req-1", result.requestId)
    }

    @Test
    fun mapsResolvableResult() {
        val result = EsimDownloadResultMapper.fromCallback(
            requestId = "req-2",
            resultCode = EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_RESOLVABLE_ERROR,
            detailedCode = 1,
            operationCode = 2,
            errorCode = 3,
            smdxSubjectCode = null,
            smdxReasonCode = null
        )

        assertEquals(EsimDownloadStatus.NeedsConfirmation, result.status)
    }

    @Test
    fun mapsFailureWithDiagnostics() {
        val result = EsimDownloadResultMapper.fromCallback(
            requestId = "req-3",
            resultCode = EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_ERROR,
            detailedCode = 42,
            operationCode = 8,
            errorCode = 9,
            smdxSubjectCode = "8.1",
            smdxReasonCode = "3.8"
        )

        assertEquals(EsimDownloadStatus.Failed, result.status)
        assertTrue(result.message.contains("详细码 42"))
        assertTrue(result.message.contains("Subject 8.1"))
    }
}
