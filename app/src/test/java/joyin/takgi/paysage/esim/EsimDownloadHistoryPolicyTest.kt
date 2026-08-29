package joyin.takgi.paysage.esim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EsimDownloadHistoryPolicyTest {
    @Test
    fun upsertReplacesSameRequestIdAtTop() {
        val pending = result("download-1", EsimDownloadStatus.Pending)
        val final = result("download-1", EsimDownloadStatus.Succeeded)

        val history = EsimDownloadHistoryPolicy.upsert(
            EsimDownloadHistoryPolicy.upsert(emptyList(), pending),
            final
        )

        assertEquals(1, history.size)
        assertEquals(EsimDownloadStatus.Succeeded, history.first().status)
    }

    @Test
    fun upsertKeepsNewestFirstAndCapsSize() {
        val history = (1..20).fold(emptyList<EsimDownloadResult>()) { existing, index ->
            EsimDownloadHistoryPolicy.upsert(existing, result("switch-$index", EsimDownloadStatus.Pending))
        }

        assertEquals(EsimDownloadHistoryPolicy.MAX_HISTORY_ITEMS, history.size)
        assertEquals("switch-20", history.first().requestId)
        assertEquals("switch-9", history.last().requestId)
    }

    @Test
    fun ignoresBlankRequestId() {
        val history = EsimDownloadHistoryPolicy.upsert(
            listOf(result("rename-1", EsimDownloadStatus.Pending)),
            result("", EsimDownloadStatus.Failed)
        )

        assertEquals(1, history.size)
        assertEquals("rename-1", history.first().requestId)
    }

    @Test
    fun operationClassifierMapsRequestIdPrefixes() {
        // title() 需要 Context,纯 JVM 测试改为验证前缀分类逻辑
        assertEquals(
            joyin.takgi.paysage.R.string.operation_download_esim,
            EsimDownloadHistoryPolicy.titleOperationResId("download-1")
        )
        assertEquals(
            joyin.takgi.paysage.R.string.operation_switch_port,
            EsimDownloadHistoryPolicy.titleOperationResId("switch-port-1-1")
        )
        assertEquals(
            joyin.takgi.paysage.R.string.operation_esim_system,
            EsimDownloadHistoryPolicy.titleOperationResId("unknown-1")
        )
    }

    private fun result(
        requestId: String,
        status: EsimDownloadStatus
    ): EsimDownloadResult =
        EsimDownloadResult(
            requestId = requestId,
            status = status,
            message = status.name,
            resultCode = null,
            detailedCode = null,
            operationCode = null,
            errorCode = null,
            smdxSubjectCode = null,
            smdxReasonCode = null,
            updatedAtMillis = requestId.hashCode().toLong()
        )
}
