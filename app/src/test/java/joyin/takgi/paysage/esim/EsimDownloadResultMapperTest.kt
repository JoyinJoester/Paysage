package joyin.takgi.paysage.esim

import android.telephony.euicc.EuiccManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EsimDownloadResultMapperTest {
    // fromCallback 需要 Context 生成文案,纯 JVM 测试改用无 Context 的
    // statusFor / 结构化字段断言验证映射逻辑
    @Test
    fun mapsSuccessResult() {
        assertEquals(
            EsimDownloadStatus.Succeeded,
            EsimDownloadResultMapper.statusFor(EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_OK)
        )
    }

    @Test
    fun mapsResolvableResult() {
        assertEquals(
            EsimDownloadStatus.NeedsConfirmation,
            EsimDownloadResultMapper.statusFor(EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_RESOLVABLE_ERROR)
        )
    }

    @Test
    fun mapsErrorResult() {
        assertEquals(
            EsimDownloadStatus.Failed,
            EsimDownloadResultMapper.statusFor(EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_ERROR)
        )
    }

    @Test
    fun unknownResultCodeFallsBackToFailed() {
        assertEquals(EsimDownloadStatus.Failed, EsimDownloadResultMapper.statusFor(12345))
    }

    @Test
    fun failureKeepsDiagnosticFields() {
        val result = EsimDownloadResultMapper.failure(
            requestId = "req-3",
            message = "diagnostic failure"
        )

        assertEquals(EsimDownloadStatus.Failed, result.status)
        assertEquals("req-3", result.requestId)
        assertEquals("diagnostic failure", result.message)
        assertNull(result.detailedCode)
        assertNull(result.smdxSubjectCode)
        assertTrue(result.updatedAtMillis > 0)
    }
}
