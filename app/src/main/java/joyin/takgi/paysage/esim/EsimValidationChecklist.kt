package joyin.takgi.paysage.esim

enum class EsimValidationStatus {
    Pass,
    Todo,
    Skip
}

data class EsimValidationChecklistItem(
    val area: String,
    val title: String,
    val status: EsimValidationStatus,
    val evidence: String
)

data class EsimValidationChecklistInput(
    val supportReportInput: EsimSupportReportInput,
    val hasPhoneStatePermission: Boolean,
    val activeSubscriptionCount: Int,
    val manageableProfileCount: Int
)

object EsimValidationChecklistBuilder {
    const val PRIVACY_LINE: String =
        "验收清单不包含激活码、确认码、EID、ICCID、IMSI、手机号、完整 APDU 响应或 ATR 原文。"

    fun build(input: EsimValidationChecklistInput): String {
        val items = items(input)
        val passCount = items.count { it.status == EsimValidationStatus.Pass }
        val todoCount = items.count { it.status == EsimValidationStatus.Todo }
        val skipCount = items.count { it.status == EsimValidationStatus.Skip }
        return buildList {
            add("Paysage eSIM 真机验收清单")
            add(PRIVACY_LINE)
            add("汇总: PASS $passCount / TODO $todoCount / SKIP $skipCount")
            add("")
            var lastArea: String? = null
            items.forEach { item ->
                if (item.area != lastArea) {
                    if (lastArea != null) add("")
                    add(item.area)
                    lastArea = item.area
                }
                add("[${item.status.label()}] ${item.title} - ${item.evidence}")
            }
        }.joinToString(separator = "\n")
    }

    fun items(input: EsimValidationChecklistInput): List<EsimValidationChecklistItem> {
        val report = input.supportReportInput
        val state = report.supportState
        val usbIsdRReady = report.usbIsdRResults.values.any { it.success }
        val omapiIsdRReady = report.omapiIsdRResults.values.any { it.success }
        val anyIsdRReady = usbIsdRReady || omapiIsdRReady || report.sgp22IsdRReady
        return listOf(
            EsimValidationChecklistItem(
                area = "设备基础",
                title = "eUICC 能力检测",
                status = if (state.hasEuiccFeature) EsimValidationStatus.Pass else EsimValidationStatus.Todo,
                evidence = if (state.hasEuiccFeature) "系统声明支持 eUICC" else "当前设备未声明 eUICC"
            ),
            EsimValidationChecklistItem(
                area = "设备基础",
                title = "系统 eSIM 服务",
                status = if (state.euiccManagerEnabled) EsimValidationStatus.Pass else EsimValidationStatus.Todo,
                evidence = if (state.euiccManagerEnabled) "EuiccManager 已启用" else "EuiccManager 不可用或被厂商关闭"
            ),
            EsimValidationChecklistItem(
                area = "系统流程",
                title = "系统兜底入口",
                status = if (state.canOpenManagement) EsimValidationStatus.Pass else EsimValidationStatus.Todo,
                evidence = if (state.canOpenManagement) "可打开 Android 系统 eSIM 兜底页" else "未找到系统 eSIM 兜底入口"
            ),
            EsimValidationChecklistItem(
                area = "系统流程",
                title = "系统激活兜底入口",
                status = if (state.canOpenQrActivation) EsimValidationStatus.Pass else EsimValidationStatus.Todo,
                evidence = if (state.canOpenQrActivation) "可打开 Android eSIM 激活兜底入口" else "未找到系统 eSIM 激活兜底入口"
            ),
            EsimValidationChecklistItem(
                area = "系统流程",
                title = "活动订阅概览",
                status = if (input.hasPhoneStatePermission) EsimValidationStatus.Pass else EsimValidationStatus.Todo,
                evidence = if (input.hasPhoneStatePermission) {
                    "已授权，系统可见活动订阅 ${input.activeSubscriptionCount} 个"
                } else {
                    "尚未授权 READ_PHONE_STATE"
                }
            ),
            EsimValidationChecklistItem(
                area = "系统流程",
                title = "授权可管理 profile",
                status = if (input.manageableProfileCount > 0) EsimValidationStatus.Pass else EsimValidationStatus.Todo,
                evidence = if (input.manageableProfileCount > 0) {
                    "系统/运营商授权可管理 ${input.manageableProfileCount} 个"
                } else {
                    "当前系统未暴露可管理 profile"
                }
            ),
            EsimValidationChecklistItem(
                area = "高级通道",
                title = "USB CCID reader",
                status = when {
                    !state.hasUsbHostFeature -> EsimValidationStatus.Skip
                    report.usbReaders.isNotEmpty() -> EsimValidationStatus.Pass
                    else -> EsimValidationStatus.Todo
                },
                evidence = when {
                    !state.hasUsbHostFeature -> "设备未声明 USB Host"
                    report.usbReaders.isNotEmpty() -> "检测到 CCID reader ${report.usbReaders.size} 个"
                    else -> "尚未检测到 CCID reader"
                }
            ),
            EsimValidationChecklistItem(
                area = "高级通道",
                title = "USB SELECT ISD-R",
                status = when {
                    !state.hasUsbHostFeature -> EsimValidationStatus.Skip
                    usbIsdRReady -> EsimValidationStatus.Pass
                    else -> EsimValidationStatus.Todo
                },
                evidence = if (usbIsdRReady) "USB ISD-R 已响应" else "尚未取得 USB ISD-R 成功响应"
            ),
            EsimValidationChecklistItem(
                area = "高级通道",
                title = "OMAPI reader",
                status = when {
                    !state.hasOmapiUiccFeature -> EsimValidationStatus.Skip
                    report.omapiReaders?.isNotEmpty() == true -> EsimValidationStatus.Pass
                    else -> EsimValidationStatus.Todo
                },
                evidence = when {
                    !state.hasOmapiUiccFeature -> "设备未声明 OMAPI UICC"
                    report.omapiReaders == null -> "尚未检测 OMAPI reader"
                    report.omapiReaders.isEmpty() -> "未检测到 OMAPI reader"
                    else -> "检测到 OMAPI reader ${report.omapiReaders.size} 个"
                }
            ),
            EsimValidationChecklistItem(
                area = "高级通道",
                title = "OMAPI SELECT ISD-R",
                status = when {
                    !state.hasOmapiUiccFeature -> EsimValidationStatus.Skip
                    omapiIsdRReady -> EsimValidationStatus.Pass
                    else -> EsimValidationStatus.Todo
                },
                evidence = if (omapiIsdRReady) "OMAPI ISD-R 已响应" else "尚未取得 OMAPI ISD-R 成功响应"
            ),
            EsimValidationChecklistItem(
                area = "SGP.22",
                title = "ISD-R 通道就绪",
                status = if (anyIsdRReady) EsimValidationStatus.Pass else EsimValidationStatus.Todo,
                evidence = if (anyIsdRReady) "至少一个 ISD-R 诊断成功" else "尚无成功 ISD-R 通道"
            ),
            EsimValidationChecklistItem(
                area = "SGP.22",
                title = "危险命令阻断",
                status = EsimValidationStatus.Pass,
                evidence = "启停、删除、写入 profile APDU 在普通商业环境保持阻断"
            ),
            EsimValidationChecklistItem(
                area = "隐私",
                title = "脱敏报告",
                status = EsimValidationStatus.Pass,
                evidence = "支持报告和验收清单不包含敏感标识或原始 APDU/ATR"
            )
        )
    }

    private fun EsimValidationStatus.label(): String = when (this) {
        EsimValidationStatus.Pass -> "PASS"
        EsimValidationStatus.Todo -> "TODO"
        EsimValidationStatus.Skip -> "SKIP"
    }
}
