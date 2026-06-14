package joyin.takgi.paysage.esim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EsimValidationChecklistBuilderTest {
    @Test
    fun buildsChecklistWithDeviceEvidenceAndPrivacyLine() {
        val checklist = EsimValidationChecklistBuilder.build(
            EsimValidationChecklistInput(
                supportReportInput = supportInput(
                    supportState = supportState(
                        hasUsbHostFeature = true,
                        hasOmapiUiccFeature = false
                    ),
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
                    usbIsdRResults = mapOf(
                        "/dev/bus/usb/001/002" to EsimIsdRProbeResult(
                            success = true,
                            message = "ISD-R 已响应，APDU 通道可用。",
                            statusWord = "9000",
                            responseByteCount = 8,
                            atrHex = "3B001122",
                            fciSummary = "FCI 6F",
                            diagnosticSteps = emptyList()
                        )
                    )
                ),
                hasPhoneStatePermission = true,
                activeSubscriptionCount = 2,
                manageableProfileCount = 1
            )
        )

        assertTrue(checklist.contains("Paysage eSIM 真机验收清单"))
        assertTrue(checklist.contains(EsimValidationChecklistBuilder.PRIVACY_LINE))
        assertTrue(checklist.contains("[PASS] 系统 eSIM 服务"))
        assertTrue(checklist.contains("[PASS] 活动订阅概览 - 已授权，系统可见活动订阅 2 个"))
        assertTrue(checklist.contains("[PASS] 授权可管理 profile - 系统/运营商授权可管理 1 个"))
        assertTrue(checklist.contains("[PASS] USB SELECT ISD-R - USB ISD-R 已响应"))
        assertTrue(checklist.contains("[SKIP] OMAPI reader - 设备未声明 OMAPI UICC"))
        assertFalse(checklist.contains("/dev/bus/usb"))
        assertFalse(checklist.contains("3B001122"))
        assertFalse(checklist.contains("89049032000000000000000000000001"))
    }

    @Test
    fun returnsTodoForMissingRealDevicePaths() {
        val items = EsimValidationChecklistBuilder.items(
            EsimValidationChecklistInput(
                supportReportInput = supportInput(
                    supportState = supportState(
                        hasEuiccFeature = false,
                        euiccManagerEnabled = false,
                        canOpenManagement = false,
                        canOpenQrActivation = false,
                        hasUsbHostFeature = true,
                        hasOmapiUiccFeature = true
                    ),
                    usbReaders = emptyList(),
                    omapiReaders = null
                ),
                hasPhoneStatePermission = false,
                activeSubscriptionCount = 0,
                manageableProfileCount = 0
            )
        )

        assertEquals(
            EsimValidationStatus.Todo,
            items.first { it.title == "eUICC 能力检测" }.status
        )
        assertEquals(
            EsimValidationStatus.Todo,
            items.first { it.title == "活动订阅概览" }.status
        )
        assertEquals(
            EsimValidationStatus.Todo,
            items.first { it.title == "USB CCID reader" }.status
        )
        assertEquals(
            EsimValidationStatus.Todo,
            items.first { it.title == "OMAPI reader" }.status
        )
        assertEquals(
            EsimValidationStatus.Pass,
            items.first { it.title == "危险命令阻断" }.status
        )
    }

    private fun supportInput(
        supportState: EsimSupportState = supportState(),
        usbReaders: List<EsimUsbCcidReaderSummary> = emptyList(),
        omapiReaders: List<EsimOmapiReaderSummary>? = emptyList(),
        usbIsdRResults: Map<String, EsimIsdRProbeResult> = emptyMap(),
        omapiIsdRResults: Map<String, EsimIsdRProbeResult> = emptyMap()
    ): EsimSupportReportInput =
        EsimSupportReportInput(
            supportState = supportState,
            euiccInfo = EsimEuiccInfoSummary(
                available = true,
                message = "ok",
                osVersion = "1.0",
                memory = EsimEuiccInfoFormatter.memory(1024),
                ports = emptyList()
            ),
            lastResult = EsimDownloadResult(
                requestId = "idle",
                status = EsimDownloadStatus.Idle,
                message = "idle",
                resultCode = null,
                detailedCode = null,
                operationCode = null,
                errorCode = null,
                smdxSubjectCode = null,
                smdxReasonCode = null,
                updatedAtMillis = 0L
            ),
            history = emptyList(),
            usbReaders = usbReaders,
            omapiReaders = omapiReaders,
            usbIsdRResults = usbIsdRResults,
            omapiIsdRResults = omapiIsdRResults,
            sgp22IsdRReady = usbIsdRResults.values.any { it.success } ||
                omapiIsdRResults.values.any { it.success }
        )

    private fun supportState(
        hasEuiccFeature: Boolean = true,
        euiccManagerEnabled: Boolean = true,
        canOpenManagement: Boolean = true,
        canOpenQrActivation: Boolean = true,
        hasUsbHostFeature: Boolean = false,
        hasOmapiUiccFeature: Boolean = false
    ): EsimSupportState =
        EsimSupportState(
            hasTelephonySubscriptionFeature = true,
            hasEuiccFeature = hasEuiccFeature,
            hasMepFeature = false,
            hasUsbHostFeature = hasUsbHostFeature,
            hasOmapiUiccFeature = hasOmapiUiccFeature,
            euiccManagerEnabled = euiccManagerEnabled,
            canOpenManagement = canOpenManagement,
            canOpenQrActivation = canOpenQrActivation
        )
}
