package joyin.takgi.paysage.esim

import android.content.Context
import joyin.takgi.paysage.R

data class EsimSupportReportInput(
    val supportState: EsimSupportState,
    val euiccInfo: EsimEuiccInfoSummary,
    val lastResult: EsimDownloadResult,
    val history: List<EsimDownloadResult>,
    val usbReaders: List<EsimUsbCcidReaderSummary>,
    val omapiReaders: List<EsimOmapiReaderSummary>?,
    val usbIsdRResults: Map<String, EsimIsdRProbeResult> = emptyMap(),
    val omapiIsdRResults: Map<String, EsimIsdRProbeResult> = emptyMap(),
    val sgp22IsdRReady: Boolean = false
)

object EsimSupportReportBuilder {
    const val PRIVACY_LINE: String =
        "不包含激活码、确认码、EID、ICCID、IMSI、手机号、完整 APDU 响应或 ATR 原文。"

    fun build(input: EsimSupportReportInput): String =
        build(input = input, context = null)

    fun build(input: EsimSupportReportInput, context: Context?): String {
        val text = ReportText(context)
        val lines = mutableListOf<String>()
        lines += text.get(R.string.report_esim_support_title)
        lines += privacyLine(context)
        lines += ""
        lines += text.get(R.string.report_section_device_capabilities)
        lines += "eUICC: ${input.supportState.hasEuiccFeature.label(text)}"
        lines += "${text.get(R.string.report_label_esim_service)}: ${input.supportState.euiccManagerEnabled.label(text)}"
        lines += "${text.get(R.string.report_label_system_management_fallback)}: ${input.supportState.canOpenManagement.label(text)}"
        lines += "${text.get(R.string.report_label_system_activation_fallback)}: ${input.supportState.canOpenQrActivation.label(text)}"
        lines += "${text.get(R.string.report_label_mep_multi_port)}: ${input.supportState.hasMepFeature.label(text)}"
        lines += "USB Host: ${input.supportState.hasUsbHostFeature.label(text)}"
        lines += "OMAPI UICC: ${input.supportState.hasOmapiUiccFeature.label(text)}"
        lines += ""
        lines += text.get(R.string.title_euicc_summary)
        lines += "${text.get(R.string.label_os)}: ${input.euiccInfo.osVersion ?: text.get(R.string.value_not_disclosed)}"
        lines += "${text.get(R.string.label_available_space)}: ${input.euiccInfo.memory.displayText}"
        lines += "${text.get(R.string.label_ports)}: ${
            text.get(
                R.string.format_report_ports_available,
                input.euiccInfo.ports.count { it.availability == EsimEuiccPortAvailability.Available },
                input.euiccInfo.ports.size
            )
        }"
        lines += ""
        lines += text.get(R.string.report_section_recent_system_operation)
        lines += resultLine(input.lastResult, text)
        val recentHistory = input.history.take(MAX_HISTORY_LINES)
        if (recentHistory.isNotEmpty()) {
            lines += ""
            lines += text.get(R.string.report_section_operation_history)
            recentHistory.forEach { result ->
                lines += resultLine(result, text)
            }
        }
        lines += ""
        lines += "USB CCID"
        if (input.usbReaders.isEmpty()) {
            lines += text.get(R.string.report_value_no_usb_reader)
        } else {
            input.usbReaders.forEach { reader ->
                lines += "${reader.manufacturerName} / ${reader.productName} / VID ${reader.vendorId.toString(16).uppercase()} PID ${reader.productId.toString(16).uppercase()} / ${reader.ccidInterfaceCount}/${reader.interfaceCount} CCID / ${if (reader.hasPermission) text.get(R.string.report_value_authorized) else text.get(R.string.report_value_not_authorized)}"
            }
        }
        lines += ""
        lines += "OMAPI"
        val omapiReaders = input.omapiReaders
        when {
            omapiReaders == null -> lines += text.get(R.string.report_value_not_checked)
            omapiReaders.isEmpty() -> lines += text.get(R.string.report_value_no_omapi_reader)
            else -> omapiReaders.forEach { reader ->
                lines += "${reader.name} / ${if (reader.isUicc) "UICC" else text.get(R.string.report_value_non_uicc)} / ${if (reader.isSecureElementPresent) text.get(R.string.report_value_secure_element_present) else text.get(R.string.report_value_secure_element_unavailable)}"
            }
        }
        lines += ""
        lines += text.get(R.string.report_section_isdr_diagnostics)
        lines += isdRDiagnosticLines(input, text)
        lines += ""
        lines += text.get(R.string.report_section_sgp22_gate)
        lines += sgp22GateLines(input.sgp22IsdRReady, text)
        return lines.joinToString(separator = "\n")
    }

    fun privacyLine(context: Context?): String =
        context?.getString(R.string.message_diagnostics_privacy) ?: PRIVACY_LINE

    private fun isdRDiagnosticLines(input: EsimSupportReportInput, text: ReportText): List<String> {
        val lines = mutableListOf<String>()
        val usbLines = input.usbReaders.mapNotNull { reader ->
            input.usbIsdRResults[reader.deviceName]?.let { result ->
                "USB ${reader.manufacturerName} / ${reader.productName}: ${result.reportLine(text)}"
            }
        }
        if (usbLines.isEmpty()) {
            lines += text.get(R.string.report_value_usb_not_probed)
        } else {
            lines += usbLines
        }

        val omapiReaders = input.omapiReaders
        val omapiLines = when {
            omapiReaders == null -> emptyList()
            omapiReaders.isEmpty() -> emptyList()
            else -> omapiReaders.mapNotNull { reader ->
                input.omapiIsdRResults[reader.name]?.let { result ->
                    "OMAPI ${reader.name}: ${result.reportLine(text)}"
                }
            }
        }
        if (omapiLines.isEmpty()) {
            lines += text.get(R.string.report_value_omapi_not_probed)
        } else {
            lines += omapiLines
        }
        return lines
    }

    private fun EsimIsdRProbeResult.reportLine(text: ReportText): String {
        val stepDetails = diagnosticSteps
            .mapNotNull { step ->
                val status = when (step.status) {
                    EsimApduDiagnosticStatus.Succeeded -> text.get(R.string.report_step_succeeded)
                    EsimApduDiagnosticStatus.Failed -> text.get(R.string.report_step_failed)
                    EsimApduDiagnosticStatus.Skipped -> text.get(R.string.report_step_skipped)
                }
                step.detailText.takeIf { it.isNotBlank() }?.let { "${step.title} $status $it" }
            }
            .take(MAX_DIAGNOSTIC_STEP_LINES)
        return listOfNotNull(
            if (success) text.get(R.string.report_probe_passed) else text.get(R.string.report_probe_not_passed),
            statusWord?.let { "SW=$it" },
            responseByteCount.takeIf { it > 0 }?.let { text.get(R.string.format_report_response_bytes, it) },
            fciSummary,
            stepDetails.takeIf { it.isNotEmpty() }?.joinToString(" | ")
        ).joinToString(" / ")
    }

    private fun sgp22GateLines(isdRReady: Boolean, text: ReportText): List<String> {
        val context = EsimSgp22SafetyContext(
            isdRSelected = isdRReady,
            allowExperimentalReadOnly = false,
            privilegedOrAuthorized = false,
            userConfirmedSensitiveAction = false
        )
        val decisions = EsimSgp22CommandCatalog.commands.map { command ->
            command to EsimSgp22SafetyPolicy.evaluate(command, context)
        }
        val availableCount = decisions.count { it.second.allowed }
        val plannedCount = decisions.count { it.first.status == EsimSgp22CommandStatus.Planned }
        val blockedCount = decisions.count { it.first.status == EsimSgp22CommandStatus.Blocked }
        return listOf(
            "ISD-R: ${if (isdRReady) text.get(R.string.report_value_isdr_ready) else text.get(R.string.report_value_isdr_not_ready)}",
            text.get(R.string.format_report_sgp22_gate_counts, availableCount, plannedCount, blockedCount)
        ) + decisions.map { (command, decision) ->
            "${command.title}: ${command.status.reportLabel(text)} / ${if (decision.allowed) text.get(R.string.report_value_allowed) else text.get(R.string.report_value_not_executable)}"
        }
    }

    private fun resultLine(result: EsimDownloadResult, text: ReportText): String {
        if (result.status == EsimDownloadStatus.Idle) return result.message
        val codes = listOfNotNull(
            result.resultCode?.let { "result=$it" },
            result.detailedCode?.let { "detail=$it" },
            result.operationCode?.let { "op=$it" },
            result.errorCode?.let { "error=$it" }
        ).joinToString(", ")
        val title = text.context?.let { EsimDownloadHistoryPolicy.title(it, result) }
            ?: result.requestId.ifBlank { result.status.name }
        return listOf(
            title,
            result.message,
            codes
        ).filter { it.isNotBlank() }.joinToString(" / ")
    }

    private fun Boolean.label(text: ReportText): String =
        if (this) text.get(R.string.report_value_yes) else text.get(R.string.report_value_no)

    private fun EsimSgp22CommandStatus.reportLabel(text: ReportText): String = when (this) {
        EsimSgp22CommandStatus.Available -> text.get(R.string.report_status_available)
        EsimSgp22CommandStatus.Planned -> text.get(R.string.report_status_planned)
        EsimSgp22CommandStatus.Blocked -> text.get(R.string.report_status_blocked)
    }

    private class ReportText(val context: Context?) {
        fun get(resId: Int, vararg args: Any): String =
            context?.getString(resId, *args) ?: fallback(resId, *args)

        private fun fallback(resId: Int, vararg args: Any): String =
            when (resId) {
                R.string.report_esim_support_title -> "Paysage eSIM 支持排障报告"
                R.string.report_section_device_capabilities -> "设备能力"
                R.string.report_label_esim_service -> "eSIM 服务"
                R.string.report_label_system_management_fallback -> "系统兜底入口"
                R.string.report_label_system_activation_fallback -> "系统激活兜底入口"
                R.string.report_label_mep_multi_port -> "MEP 多端口"
                R.string.title_euicc_summary -> "eUICC 摘要"
                R.string.label_os -> "OS"
                R.string.value_not_disclosed -> "未公开"
                R.string.label_available_space -> "空间"
                R.string.label_ports -> "端口"
                R.string.report_section_recent_system_operation -> "最近系统操作"
                R.string.report_section_operation_history -> "操作历史"
                R.string.report_section_isdr_diagnostics -> "ISD-R 诊断"
                R.string.report_section_sgp22_gate -> "SGP.22 门禁"
                R.string.report_value_yes -> "是"
                R.string.report_value_no -> "否"
                R.string.report_value_not_checked -> "尚未检测"
                R.string.report_value_no_usb_reader -> "未检测到读卡器"
                R.string.report_value_no_omapi_reader -> "未检测到 reader"
                R.string.report_value_authorized -> "已授权"
                R.string.report_value_not_authorized -> "未授权"
                R.string.report_value_non_uicc -> "非 UICC"
                R.string.report_value_secure_element_present -> "安全元素存在"
                R.string.report_value_secure_element_unavailable -> "安全元素不可用"
                R.string.report_value_usb_not_probed -> "USB: 尚未探测"
                R.string.report_value_omapi_not_probed -> "OMAPI: 尚未探测"
                R.string.report_value_isdr_ready -> "就绪"
                R.string.report_value_isdr_not_ready -> "未就绪"
                R.string.report_value_allowed -> "允许"
                R.string.report_value_not_executable -> "不可执行"
                R.string.report_probe_passed -> "通过"
                R.string.report_probe_not_passed -> "未通过"
                R.string.report_step_succeeded -> "通过"
                R.string.report_step_failed -> "失败"
                R.string.report_step_skipped -> "跳过"
                R.string.report_status_available -> "已接入"
                R.string.report_status_planned -> "计划中"
                R.string.report_status_blocked -> "已阻断"
                R.string.format_report_ports_available -> "${args[0]}/${args[1]} 可用"
                R.string.format_report_response_bytes -> "响应=${args[0]}字节"
                R.string.format_report_sgp22_gate_counts -> "可执行只读: ${args[0]} / 计划: ${args[1]} / 阻断: ${args[2]}"
                else -> ""
            }
    }

    private const val MAX_HISTORY_LINES = 5
    private const val MAX_DIAGNOSTIC_STEP_LINES = 4
}
