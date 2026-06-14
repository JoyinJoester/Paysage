package joyin.takgi.paysage.esim.ccid

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import joyin.takgi.paysage.esim.EsimApduDiagnosticStep
import joyin.takgi.paysage.esim.EsimApduDiagnostics
import joyin.takgi.paysage.esim.EsimApdu
import joyin.takgi.paysage.esim.EsimFciAnalyzer
import joyin.takgi.paysage.esim.toIsdRMessage

data class UsbCcidAtrResult(
    val success: Boolean,
    val message: String,
    val atrHex: String?
)

data class UsbCcidApduResult(
    val success: Boolean,
    val message: String,
    val statusWord: String?,
    val responseByteCount: Int,
    val atrHex: String?,
    val fciSummary: String? = null,
    val diagnosticSteps: List<EsimApduDiagnosticStep> = emptyList()
)

class UsbCcidAtrReader(private val usbManager: UsbManager) {
    fun readAtr(device: UsbDevice): UsbCcidAtrResult {
        val ready = when (val openResult = openCcid(device)) {
            is CcidOpenResult.Error -> return UsbCcidAtrResult(false, openResult.message, null)
            is CcidOpenResult.Ready -> openResult
        }
        return runCatching {
            ready.connection.useClaimed(ready.ccidInterface) {
                val dataBlock = sendCcidCommand(
                    bulkOut = ready.bulkOut,
                    bulkIn = ready.bulkIn,
                    command = CcidMessages.buildIccPowerOn(sequence = 1),
                    failureContext = "CCID PowerOn"
                ).getOrElse { error ->
                    return@useClaimed UsbCcidAtrResult(false, error.message ?: "读取 ATR 失败。", null)
                }

                if (!dataBlock.isCommandSuccessful) {
                    return@useClaimed UsbCcidAtrResult(
                        success = false,
                        message = dataBlock.commandErrorMessage(),
                        atrHex = null
                    )
                }
                UsbCcidAtrResult(
                    success = dataBlock.data.isNotEmpty(),
                    message = if (dataBlock.data.isNotEmpty()) "已读取 ATR。" else "读卡器没有返回 ATR。",
                    atrHex = dataBlock.data.takeIf { it.isNotEmpty() }?.toHex()
                )
            }
        }.getOrElse { error ->
            UsbCcidAtrResult(false, error.message ?: "USB CCID 操作失败。", null)
        }
    }

    fun selectIsdR(device: UsbDevice): UsbCcidApduResult {
        val ready = when (val openResult = openCcid(device)) {
            is CcidOpenResult.Error -> return UsbCcidApduResult(false, openResult.message, null, 0, null)
            is CcidOpenResult.Ready -> openResult
        }
        return runCatching {
            ready.connection.useClaimed(ready.ccidInterface) {
                val steps = mutableListOf<EsimApduDiagnosticStep>()
                val powerOn = sendCcidCommand(
                    bulkOut = ready.bulkOut,
                    bulkIn = ready.bulkIn,
                    command = CcidMessages.buildIccPowerOn(sequence = 1),
                    failureContext = "CCID PowerOn"
                ).getOrElse { error ->
                    val message = error.message ?: "USB 读卡器上电失败。"
                    return@useClaimed UsbCcidApduResult(
                        success = false,
                        message = message,
                        statusWord = null,
                        responseByteCount = 0,
                        atrHex = null,
                        diagnosticSteps = listOf(EsimApduDiagnostics.failed("CCID PowerOn", message))
                    )
                }
                if (!powerOn.isCommandSuccessful) {
                    val message = powerOn.commandErrorMessage()
                    return@useClaimed UsbCcidApduResult(
                        success = false,
                        message = message,
                        statusWord = null,
                        responseByteCount = 0,
                        atrHex = null,
                        diagnosticSteps = listOf(EsimApduDiagnostics.failed("CCID PowerOn", message))
                    )
                }
                val atrHex = powerOn.data.takeIf { it.isNotEmpty() }?.toHex()
                steps += EsimApduDiagnostics.powerOn(
                    success = powerOn.data.isNotEmpty(),
                    message = if (powerOn.data.isNotEmpty()) "读卡器已上电并返回 ATR。" else "读卡器已上电，但未返回 ATR。",
                    atrHex = atrHex
                )

                val selectResponse = sendCcidCommand(
                    bulkOut = ready.bulkOut,
                    bulkIn = ready.bulkIn,
                    command = CcidMessages.buildXfrBlock(
                        apdu = EsimApdu.buildSelectIsdR(),
                        sequence = 2
                    ),
                    failureContext = "CCID XfrBlock"
                ).getOrElse { error ->
                    val message = error.message ?: "发送 SELECT ISD-R 失败。"
                    return@useClaimed UsbCcidApduResult(
                        success = false,
                        message = message,
                        statusWord = null,
                        responseByteCount = 0,
                        atrHex = atrHex,
                        diagnosticSteps = steps + EsimApduDiagnostics.failed("SELECT ISD-R", message)
                    )
                }
                if (!selectResponse.isCommandSuccessful) {
                    val message = selectResponse.commandErrorMessage()
                    return@useClaimed UsbCcidApduResult(
                        success = false,
                        message = message,
                        statusWord = null,
                        responseByteCount = 0,
                        atrHex = atrHex,
                        diagnosticSteps = steps + EsimApduDiagnostics.failed("SELECT ISD-R", message)
                    )
                }

                val apduResponse = runCatching {
                    EsimApdu.parseResponse(selectResponse.data)
                }.getOrElse { error ->
                    val message = error.message ?: "APDU 响应解析失败。"
                    return@useClaimed UsbCcidApduResult(
                        success = false,
                        message = message,
                        statusWord = null,
                        responseByteCount = selectResponse.data.size,
                        atrHex = atrHex,
                        diagnosticSteps = steps + EsimApduDiagnostics.failed("SELECT ISD-R", message)
                    )
                }
                val fciSummary = EsimFciAnalyzer.summarize(apduResponse.data)?.diagnosticText
                steps += EsimApduDiagnostics.selectIsdR(apduResponse, fciSummary)
                UsbCcidApduResult(
                    success = apduResponse.isAccepted,
                    message = apduResponse.toIsdRMessage(),
                    statusWord = apduResponse.statusWordHex,
                    responseByteCount = apduResponse.data.size,
                    atrHex = atrHex,
                    fciSummary = fciSummary,
                    diagnosticSteps = steps
                )
            }
        }.getOrElse { error ->
            val message = error.message ?: "USB CCID APDU 操作失败。"
            UsbCcidApduResult(
                success = false,
                message = message,
                statusWord = null,
                responseByteCount = 0,
                atrHex = null,
                diagnosticSteps = listOf(EsimApduDiagnostics.failed("USB CCID", message))
            )
        }
    }

    private fun openCcid(device: UsbDevice): CcidOpenResult {
        if (!usbManager.hasPermission(device)) {
            return CcidOpenResult.Error("USB 读卡器尚未授权。")
        }
        val ccidInterface = device.findCcidInterface()
            ?: return CcidOpenResult.Error("未找到 CCID 接口。")
        val bulkOut = ccidInterface.findEndpoint(UsbConstants.USB_DIR_OUT)
            ?: return CcidOpenResult.Error("未找到 CCID Bulk OUT 端点。")
        val bulkIn = ccidInterface.findEndpoint(UsbConstants.USB_DIR_IN)
            ?: return CcidOpenResult.Error("未找到 CCID Bulk IN 端点。")
        val connection = usbManager.openDevice(device)
            ?: return CcidOpenResult.Error("无法打开 USB 读卡器。")
        return CcidOpenResult.Ready(connection, ccidInterface, bulkOut, bulkIn)
    }

    private fun UsbDeviceConnection.sendCcidCommand(
        bulkOut: UsbEndpoint,
        bulkIn: UsbEndpoint,
        command: ByteArray,
        failureContext: String
    ): Result<CcidDataBlock> = runCatching {
        val written = bulkTransfer(bulkOut, command, command.size, TRANSFER_TIMEOUT_MS)
        check(written == command.size) { "发送 $failureContext 失败。" }

        val buffer = ByteArray(RESPONSE_BUFFER_SIZE)
        val read = bulkTransfer(bulkIn, buffer, buffer.size, TRANSFER_TIMEOUT_MS)
        check(read > 0) { "读取 $failureContext 响应超时或失败。" }
        CcidMessages.parseDataBlock(buffer.copyOf(read))
    }

    companion object {
        private const val TRANSFER_TIMEOUT_MS = 5_000
        private const val RESPONSE_BUFFER_SIZE = 4_096
    }
}

private sealed interface CcidOpenResult {
    data class Ready(
        val connection: UsbDeviceConnection,
        val ccidInterface: UsbInterface,
        val bulkOut: UsbEndpoint,
        val bulkIn: UsbEndpoint
    ) : CcidOpenResult

    data class Error(val message: String) : CcidOpenResult
}

private fun CcidDataBlock.commandErrorMessage(): String =
    "读卡器返回错误: status=$status, error=$error。"

private fun UsbDevice.findCcidInterface(): UsbInterface? =
    (0 until interfaceCount)
        .map { getInterface(it) }
        .firstOrNull { it.interfaceClass == UsbConstants.USB_CLASS_CSCID }

private fun UsbInterface.findEndpoint(direction: Int): UsbEndpoint? =
    (0 until endpointCount)
        .map { getEndpoint(it) }
        .firstOrNull {
            it.direction == direction && it.type == UsbConstants.USB_ENDPOINT_XFER_BULK
        }

private inline fun <T> UsbDeviceConnection.useClaimed(
    usbInterface: UsbInterface,
    block: UsbDeviceConnection.() -> T
): T {
    var claimed = false
    try {
        claimed = claimInterface(usbInterface, true)
        if (!claimed) error("无法声明 CCID 接口。")
        return block()
    } finally {
        if (claimed) releaseInterface(usbInterface)
        close()
    }
}

private fun ByteArray.toHex(): String =
    joinToString(separator = "") { byte -> "%02X".format(byte) }
