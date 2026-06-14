package joyin.takgi.paysage.esim

enum class EsimApduDiagnosticStatus {
    Succeeded,
    Failed,
    Skipped
}

data class EsimApduDiagnosticStep(
    val title: String,
    val status: EsimApduDiagnosticStatus,
    val message: String,
    val statusWord: String? = null,
    val responseByteCount: Int = 0,
    val summary: String? = null
) {
    val detailText: String
        get() = listOfNotNull(
            statusWord?.let { "SW $it" },
            responseByteCount.takeIf { it > 0 }?.let { "响应 $it 字节" },
            summary
        ).joinToString(" / ")
}

object EsimApduDiagnostics {
    fun powerOn(
        success: Boolean,
        message: String,
        atrHex: String?
    ): EsimApduDiagnosticStep =
        EsimApduDiagnosticStep(
            title = "CCID PowerOn",
            status = if (success) EsimApduDiagnosticStatus.Succeeded else EsimApduDiagnosticStatus.Failed,
            message = message,
            responseByteCount = atrHex?.length?.div(2) ?: 0,
            summary = atrHex?.let { "ATR 已返回" }
        )

    fun omapiSession(
        success: Boolean,
        message: String,
        atrHex: String?
    ): EsimApduDiagnosticStep =
        EsimApduDiagnosticStep(
            title = "OMAPI Session",
            status = if (success) EsimApduDiagnosticStatus.Succeeded else EsimApduDiagnosticStatus.Failed,
            message = message,
            responseByteCount = atrHex?.length?.div(2) ?: 0,
            summary = atrHex?.let { "ATR 已返回" }
        )

    fun selectIsdR(
        response: Iso7816Response,
        fciSummary: String?
    ): EsimApduDiagnosticStep {
        val followUp = EsimSgp22ApduFollowUpPlanner.evaluate(response)
        val followUpSummary = followUp.takeIf { it.kind != EsimSgp22ApduFollowUpKind.Complete }?.let {
            "下一步: ${it.message}"
        }
        return EsimApduDiagnosticStep(
            title = "SELECT ISD-R",
            status = if (response.isAccepted) {
                EsimApduDiagnosticStatus.Succeeded
            } else {
                EsimApduDiagnosticStatus.Failed
            },
            message = response.toIsdRMessage(),
            statusWord = response.statusWordHex,
            responseByteCount = response.data.size,
            summary = listOfNotNull(fciSummary, followUpSummary).joinToString(" / ").ifBlank { null }
        )
    }

    fun failed(
        title: String,
        message: String
    ): EsimApduDiagnosticStep =
        EsimApduDiagnosticStep(
            title = title,
            status = EsimApduDiagnosticStatus.Failed,
            message = message
        )

    fun skipped(
        title: String,
        message: String
    ): EsimApduDiagnosticStep =
        EsimApduDiagnosticStep(
            title = title,
            status = EsimApduDiagnosticStatus.Skipped,
            message = message
        )
}

fun Iso7816Response.toIsdRMessage(): String = when {
    isAccepted -> "ISD-R 已响应，APDU 通道可用。"
    sw1 == 0x61 -> EsimSgp22ApduFollowUpPlanner.evaluate(this).message
    sw1 == 0x6C -> EsimSgp22ApduFollowUpPlanner.evaluate(this).message
    sw1 == 0x6A && sw2 == 0x82 -> "未找到 ISD-R AID，可能不是 eUICC 或 reader 未连接到目标卡。"
    sw1 == 0x69 && sw2 == 0x82 -> "卡片或安全策略拒绝访问 ISD-R。"
    else -> "已完成 APDU 传输，安全元素返回状态字 $statusWordHex。"
}
