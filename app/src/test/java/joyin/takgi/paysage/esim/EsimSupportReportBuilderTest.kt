package joyin.takgi.paysage.esim

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EsimSupportReportBuilderTest {
    @Test
    fun buildsSanitizedSupportReport() {
        val report = EsimSupportReportBuilder.build(
            EsimSupportReportInput(
                supportState = EsimSupportState(
                    hasTelephonySubscriptionFeature = true,
                    hasEuiccFeature = true,
                    hasMepFeature = true,
                    hasUsbHostFeature = true,
                    hasOmapiUiccFeature = false,
                    euiccManagerEnabled = true,
                    canOpenManagement = true,
                    canOpenQrActivation = true
                ),
                euiccInfo = EsimEuiccInfoSummary(
                    available = true,
                    message = "ok",
                    osVersion = "1.2.3",
                    memory = EsimEuiccInfoFormatter.memory(1024),
                    ports = listOf(
                        EsimEuiccPortSummary(
                            portIndex = 0,
                            availability = EsimEuiccPortAvailability.Available,
                            message = "ok"
                        )
                    )
                ),
                lastResult = result("download-1", EsimDownloadStatus.Failed),
                history = listOf(result("switch-1", EsimDownloadStatus.Succeeded)),
                usbReaders = listOf(
                    EsimUsbCcidReaderSummary(
                        deviceName = "/dev/bus/usb/001/002",
                        productName = "Reader",
                        manufacturerName = "Vendor",
                        vendorId = 0x1234,
                        productId = 0x5678,
                        deviceClass = 0,
                        interfaceCount = 2,
                        ccidInterfaceCount = 1,
                        hasPermission = true
                    )
                ),
                omapiReaders = emptyList(),
                usbIsdRResults = mapOf(
                    "/dev/bus/usb/001/002" to EsimIsdRProbeResult(
                        success = false,
                        message = "卡片提示还有 16 字节响应数据，建议发送 GET RESPONSE。",
                        statusWord = "6110",
                        responseByteCount = 0,
                        atrHex = "3B001122",
                        fciSummary = null,
                        diagnosticSteps = listOf(
                            EsimApduDiagnostics.selectIsdR(
                                Iso7816Response(data = byteArrayOf(), sw1 = 0x61, sw2 = 0x10),
                                null
                            )
                        )
                    )
                )
            )
        )

        assertTrue(report.contains("Paysage eSIM 支持排障报告"))
        assertTrue(report.contains("MEP 多端口: 是"))
        assertTrue(report.contains("下载 eSIM / 失败"))
        assertTrue(report.contains("VID 1234 PID 5678"))
        assertTrue(report.contains("ISD-R 诊断"))
        assertTrue(report.contains("USB Vendor / Reader: 未通过 / SW=6110"))
        assertTrue(report.contains("GET RESPONSE"))
        assertTrue(report.contains("SGP.22 门禁"))
        assertTrue(report.contains("只读 APDU 框架"))
        assertTrue(report.contains("删除 profile: 已阻断"))
        assertTrue(report.contains(EsimSupportReportBuilder.PRIVACY_LINE))
        assertFalse(report.contains("CONF-456"))
        assertFalse(report.contains("89049032000000000000000000000001"))
        assertFalse(report.contains("/dev/bus/usb"))
        assertFalse(report.contains("3B001122"))
    }

    @Test
    fun reportsIsdRReadyStateForSgp22Gate() {
        val report = EsimSupportReportBuilder.build(
            EsimSupportReportInput(
                supportState = EsimSupportState(
                    hasTelephonySubscriptionFeature = true,
                    hasEuiccFeature = true,
                    hasMepFeature = false,
                    hasUsbHostFeature = true,
                    hasOmapiUiccFeature = true,
                    euiccManagerEnabled = true,
                    canOpenManagement = true,
                    canOpenQrActivation = true
                ),
                euiccInfo = EsimEuiccInfoSummary(
                    available = true,
                    message = "ok",
                    osVersion = null,
                    memory = EsimEuiccInfoFormatter.memory(null),
                    ports = emptyList()
                ),
                lastResult = result("idle", EsimDownloadStatus.Idle),
                history = emptyList(),
                usbReaders = emptyList(),
                omapiReaders = null,
                sgp22IsdRReady = true
            )
        )

        assertTrue(report.contains("ISD-R: 就绪"))
        assertTrue(report.contains("SELECT ISD-R: 已接入 / 允许"))
        assertTrue(report.contains("读取 profile 列表: 计划中 / 不可执行"))
    }

    @Test
    fun reportsOmapiIsdRDiagnosticSummaryWithoutRawValues() {
        val report = EsimSupportReportBuilder.build(
            EsimSupportReportInput(
                supportState = EsimSupportState(
                    hasTelephonySubscriptionFeature = true,
                    hasEuiccFeature = true,
                    hasMepFeature = false,
                    hasUsbHostFeature = false,
                    hasOmapiUiccFeature = true,
                    euiccManagerEnabled = true,
                    canOpenManagement = true,
                    canOpenQrActivation = true
                ),
                euiccInfo = EsimEuiccInfoSummary(
                    available = true,
                    message = "ok",
                    osVersion = "2.0",
                    memory = EsimEuiccInfoFormatter.memory(2048),
                    ports = emptyList()
                ),
                lastResult = result("idle", EsimDownloadStatus.Idle),
                history = emptyList(),
                usbReaders = emptyList(),
                omapiReaders = listOf(
                    EsimOmapiReaderSummary(
                        name = "SIM1",
                        isUicc = true,
                        isSecureElementPresent = true
                    )
                ),
                omapiIsdRResults = mapOf(
                    "SIM1" to EsimIsdRProbeResult(
                        success = true,
                        message = "ISD-R 已响应，APDU 通道可用。",
                        statusWord = "9000",
                        responseByteCount = 8,
                        atrHex = null,
                        fciSummary = "FCI: 6F / AID 匹配 ISD-R",
                        diagnosticSteps = listOf(
                            EsimApduDiagnostics.selectIsdR(
                                Iso7816Response(data = byteArrayOf(0x6F, 0x00), sw1 = 0x90, sw2 = 0x00),
                                "FCI: 6F / AID 匹配 ISD-R"
                            )
                        )
                    )
                )
            )
        )

        assertTrue(report.contains("OMAPI SIM1: 通过 / SW=9000 / 响应=8字节"))
        assertTrue(report.contains("AID 匹配 ISD-R"))
        assertTrue(report.contains(EsimSupportReportBuilder.PRIVACY_LINE))
        assertFalse(report.contains("80E2"))
        assertFalse(report.contains("89049032000000000000000000000001"))
    }

    private fun result(
        requestId: String,
        status: EsimDownloadStatus
    ): EsimDownloadResult =
        EsimDownloadResult(
            requestId = requestId,
            status = status,
            message = "message",
            resultCode = 2,
            detailedCode = 42,
            operationCode = 8,
            errorCode = 9,
            smdxSubjectCode = null,
            smdxReasonCode = null,
            updatedAtMillis = 0L
        )
}
