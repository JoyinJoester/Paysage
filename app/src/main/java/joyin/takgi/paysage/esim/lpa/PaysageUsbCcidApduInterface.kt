package joyin.takgi.paysage.esim.lpa

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.SystemClock
import android.util.Log
import net.typeblog.lpac_jni.ApduInterface
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * USB CCID APDU bridge aligned with EasyEUICC's unprivileged external eUICC path.
 *
 * It intentionally keeps the same behavior for reader power-on, TPDU readers, logical channels,
 * APDU 6C/61 follow-ups, CCID sequence checks, and endpoint-size chunking.
 */
class PaysageUsbCcidApduInterface(
    private val usbManager: UsbManager,
    private val device: UsbDevice,
    private val forceTpduMode: Boolean = false,
    private val usbInterfaceId: Int? = null,
    sharedContext: PaysageUsbCcidContext? = null,
    private val diagnosticRecorder: ((stage: String, message: String) -> Unit)? = null
) : ApduInterface {
    private val channels = mutableSetOf<Int>()
    private val ownsContext = sharedContext == null
    private val ccidContext = sharedContext ?: PaysageUsbCcidContext(
        usbManager = usbManager,
        device = device,
        forceTpduMode = forceTpduMode,
        usbInterfaceId = usbInterfaceId
    )

    val atr: ByteArray?
        get() = ccidContext.atr

    override val valid: Boolean
        get() = channels.isNotEmpty()

    @Synchronized
    override fun connect() {
        record(
            "usb_ccid_apdu_connect_start",
            "device=${device.deviceName} vid=${device.vendorId} pid=${device.productId} interface=${usbInterfaceId ?: "auto"} shared=${!ownsContext} tpduForced=$forceTpduMode"
        )
        runCatching { ccidContext.connect() }
            .onFailure { error ->
                record(
                    "usb_ccid_apdu_connect_error",
                    "device=${device.deviceName} interface=${usbInterfaceId ?: "auto"} error=${error.javaClass.simpleName} message=${error.message.orEmpty()}"
                )
            }
            .getOrThrow()
        record(
            "usb_ccid_apdu_connect_powered",
            "device=${device.deviceName} interface=${usbInterfaceId ?: "auto"} atrBytes=${atr?.size ?: 0} tpdu=${ccidContext.useTpdu}"
        )
        if (ccidContext.useTpdu) {
            runCatching { configureTpduParameters() }
                .onFailure { error ->
                    record(
                        "usb_ccid_apdu_tpdu_error",
                        "device=${device.deviceName} interface=${usbInterfaceId ?: "auto"} error=${error.javaClass.simpleName} message=${error.message.orEmpty()}"
                    )
                }
                .getOrThrow()
            record(
                "usb_ccid_apdu_tpdu_ok",
                "device=${device.deviceName} interface=${usbInterfaceId ?: "auto"}"
            )
        }

        val terminalCapabilities = buildCmd(
            cla = 0x80.toByte(),
            ins = 0xAA.toByte(),
            p1 = 0x00,
            p2 = 0x00,
            data = TERMINAL_CAPABILITIES,
            le = null
        )
        val terminalResponse = transmitApduByChannel(terminalCapabilities, BASIC_CHANNEL)
        record(
            "usb_ccid_apdu_connect_ok",
            "device=${device.deviceName} interface=${usbInterfaceId ?: "auto"} terminalSw=${terminalResponse.statusWordHexOrUnknown()}"
        )
    }

    @Synchronized
    override fun disconnect() {
        record(
            "usb_ccid_apdu_disconnect_start",
            "device=${device.deviceName} interface=${usbInterfaceId ?: "auto"} openChannels=${channels.size} ownsContext=$ownsContext"
        )
        channels.toList().forEach(::logicalChannelClose)
        if (ownsContext) {
            ccidContext.close()
        }
        record(
            "usb_ccid_apdu_disconnect_ok",
            "device=${device.deviceName} interface=${usbInterfaceId ?: "auto"}"
        )
    }

    @Synchronized
    override fun logicalChannelOpen(aid: ByteArray): Int {
        ensureConnected()
        val aidHex = aid.toHex()
        record(
            "usb_ccid_apdu_open_channel_start",
            "device=${device.deviceName} interface=${usbInterfaceId ?: "auto"} aid=$aidHex"
        )
        val openResponse = runCatching {
            transmitApduByChannel(manageChannelCmd(open = true, channel = BASIC_CHANNEL), BASIC_CHANNEL)
        }.getOrElse { error ->
            record(
                "usb_ccid_apdu_open_channel_error",
                "device=${device.deviceName} interface=${usbInterfaceId ?: "auto"} aid=$aidHex error=${error.javaClass.simpleName} message=${error.message.orEmpty()}"
            )
            return -1
        }
        if (!isSuccessResponse(openResponse) || openResponse.isEmpty()) {
            Log.d(TAG, "OPEN LOGICAL CHANNEL failed: ${openResponse.summary()}")
            record(
                "usb_ccid_apdu_open_channel_rejected",
                "device=${device.deviceName} interface=${usbInterfaceId ?: "auto"} aid=$aidHex sw=${openResponse.statusWordHexOrUnknown()} bytes=${openResponse.size}"
            )
            return -1
        }

        val channelId = openResponse[0].toInt() and 0xFF
        channels += channelId
        record(
            "usb_ccid_apdu_open_channel_allocated",
            "device=${device.deviceName} interface=${usbInterfaceId ?: "auto"} aid=$aidHex handle=$channelId"
        )

        val selectResponse = transmitApduByChannel(selectByDfCmd(aid, channelId), channelId)
        if (!isSuccessResponse(selectResponse)) {
            Log.d(TAG, "SELECT ISD-R failed: ${selectResponse.summary()}")
            record(
                "usb_ccid_apdu_select_rejected",
                "device=${device.deviceName} interface=${usbInterfaceId ?: "auto"} aid=$aidHex handle=$channelId sw=${selectResponse.statusWordHexOrUnknown()} bytes=${selectResponse.size}"
            )
            logicalChannelClose(channelId)
            return -1
        }
        record(
            "usb_ccid_apdu_open_channel_ok",
            "device=${device.deviceName} interface=${usbInterfaceId ?: "auto"} aid=$aidHex handle=$channelId selectSw=${selectResponse.statusWordHexOrUnknown()}"
        )
        return channelId
    }

    @Synchronized
    fun selectAidForDiagnostics(aid: ByteArray): ByteArray {
        ensureConnected()
        val openResponse = transmitApduByChannel(
            manageChannelCmd(open = true, channel = BASIC_CHANNEL),
            BASIC_CHANNEL
        )
        check(isSuccessResponse(openResponse) && openResponse.isNotEmpty()) {
            "OPEN LOGICAL CHANNEL failed: ${openResponse.summary()}"
        }

        val channelId = openResponse[0].toInt() and 0xFF
        return try {
            transmitApduByChannel(selectByDfCmd(aid, channelId), channelId)
        } finally {
            runCatching {
                val response = transmitApduByChannel(manageChannelCmd(open = false, channel = channelId), channelId)
                if (!isSuccessResponse(response)) {
                    Log.d(TAG, "CLOSE LOGICAL CHANNEL failed: ${response.summary()}")
                }
            }
        }
    }

    @Synchronized
    override fun logicalChannelClose(handle: Int) {
        if (handle !in channels) return
        record(
            "usb_ccid_apdu_close_channel_start",
            "device=${device.deviceName} interface=${usbInterfaceId ?: "auto"} handle=$handle"
        )
        runCatching {
            val response = transmitApduByChannel(manageChannelCmd(open = false, channel = handle), handle)
            if (!isSuccessResponse(response)) {
                Log.d(TAG, "CLOSE LOGICAL CHANNEL failed: ${response.summary()}")
                record(
                    "usb_ccid_apdu_close_channel_rejected",
                    "device=${device.deviceName} interface=${usbInterfaceId ?: "auto"} handle=$handle sw=${response.statusWordHexOrUnknown()} bytes=${response.size}"
                )
            }
        }.onFailure { error ->
            record(
                "usb_ccid_apdu_close_channel_error",
                "device=${device.deviceName} interface=${usbInterfaceId ?: "auto"} handle=$handle error=${error.javaClass.simpleName} message=${error.message.orEmpty()}"
            )
        }
        channels -= handle
        record(
            "usb_ccid_apdu_close_channel_ok",
            "device=${device.deviceName} interface=${usbInterfaceId ?: "auto"} handle=$handle"
        )
    }

    @Synchronized
    override fun transmit(handle: Int, tx: ByteArray): ByteArray {
        check(handle in channels) { "Invalid logical channel handle $handle." }
        return runCatching { transmitApduByChannel(tx, handle) }
            .onSuccess { response ->
                record(
                    "usb_ccid_apdu_transmit_ok",
                    "device=${device.deviceName} interface=${usbInterfaceId ?: "auto"} handle=$handle bytes=${tx.size} responseBytes=${response.size} sw=${response.statusWordHexOrUnknown()}"
                )
            }
            .onFailure { error ->
                record(
                    "usb_ccid_apdu_transmit_error",
                    "device=${device.deviceName} interface=${usbInterfaceId ?: "auto"} handle=$handle bytes=${tx.size} error=${error.javaClass.simpleName} message=${error.message.orEmpty()}"
                )
            }
            .getOrThrow()
    }

    private fun configureTpduParameters() {
        val atrBytes = atr ?: return
        val parsedAtr = ParsedAtr.parse(atrBytes)
        val ta1 = parsedAtr.ta1 ?: 0x11.toByte()
        val pps = byteArrayOf(0xFF.toByte(), 0x10.toByte(), ta1, 0x00.toByte())
        ccidContext.sendXfrBlockData(pps)

        val parameters = byteArrayOf(
            ta1,
            if (parsedAtr.ts == 0x3F.toByte()) 0x02 else 0x00,
            parsedAtr.tc1 ?: 0,
            parsedAtr.tc2 ?: 0x0A,
            0x00
        )
        ccidContext.sendParamBlock(parameters)
    }

    private fun transmitApduByChannel(tx: ByteArray, channel: Int): ByteArray {
        val realTx = tx.copyOf()
        check(realTx.isNotEmpty()) { "APDU must not be empty." }
        realTx[0] = ((realTx[0].toInt() and 0xFC) or (channel and 0x03)).toByte()

        var response = ccidContext.sendXfrBlockData(realTx)
        check(response.size >= 2) { "APDU response shorter than SW1/SW2." }

        var sw1 = response[response.size - 2].toInt() and 0xFF
        var sw2 = response[response.size - 1].toInt() and 0xFF
        if (sw1 == SW1_WRONG_LENGTH) {
            realTx[realTx.lastIndex] = response[response.lastIndex]
            response = ccidContext.sendXfrBlockData(realTx)
            sw1 = response[response.size - 2].toInt() and 0xFF
            sw2 = response[response.size - 1].toInt() and 0xFF
        }

        while (sw1 == SW1_RESPONSE_AVAILABLE) {
            val getResponse = byteArrayOf(realTx[0], 0xC0.toByte(), 0x00, 0x00, sw2.toByte())
            val next = ccidContext.sendXfrBlockData(getResponse)
            response = response.copyOfRange(0, response.size - 2) + next
            sw1 = response[response.size - 2].toInt() and 0xFF
            sw2 = response[response.size - 1].toInt() and 0xFF
        }

        return response
    }

    private fun ensureConnected() {
        if (!ccidContext.connected) connect()
    }

    private fun record(stage: String, message: String) {
        runCatching { diagnosticRecorder?.invoke(stage, message) }
    }

    companion object {
        private const val TAG = "PaysageUsbCcidApdu"
        private const val BASIC_CHANNEL = 0
        private const val SW1_RESPONSE_AVAILABLE = 0x61
        private const val SW1_WRONG_LENGTH = 0x6C
        private val TERMINAL_CAPABILITIES = "A9088100820101830107".decodeHex()
    }
}

class PaysageUsbCcidContext(
    private val usbManager: UsbManager,
    private val device: UsbDevice,
    private val forceTpduMode: Boolean = false,
    private val usbInterfaceId: Int? = null
) {
    private var connection: UsbDeviceConnection? = null
    private var claimedInterface: UsbInterface? = null
    private var transceiver: EasyCcidTransceiver? = null

    val connected: Boolean
        get() = connection != null && transceiver != null

    val atr: ByteArray?
        get() = currentAtr

    val useTpdu: Boolean
        get() = forceTpduMode || isKnownTpduReader(device.vendorId, device.productId)

    private var currentAtr: ByteArray? = null

    @Synchronized
    fun connect() {
        if (connected) return
        check(usbManager.hasPermission(device)) { "USB reader is not authorized." }

        val ccidInterface = device.findCcidInterface(usbInterfaceId)
            ?: error(usbInterfaceId?.let { "USB CCID interface $it was not found on this reader." } ?: "No CCID interface found.")
        val (bulkIn, bulkOut) = ccidInterface.bulkPair()
        check(bulkIn != null && bulkOut != null) { "No CCID Bulk IN/OUT endpoint pair found." }

        val opened = usbManager.openDevice(device)
            ?: error("Unable to open USB reader.")
        var claimed = false
        try {
            claimed = opened.claimInterface(ccidInterface, true)
            check(claimed) { "Unable to claim CCID interface." }

            val description = CcidDescription.fromRawDescriptors(opened.rawDescriptors)
                ?: error("CCID descriptor not found.")
            check(description.hasT0Protocol) { "Unsupported card reader; T=0 support is required." }

            connection = opened
            claimedInterface = ccidInterface
            transceiver = EasyCcidTransceiver(opened, bulkIn, bulkOut, description)
            currentAtr = transceiver!!.iccPowerOn().data
        } catch (error: Throwable) {
            if (claimed) runCatching { opened.releaseInterface(ccidInterface) }
            opened.close()
            clearConnection()
            throw error
        }
    }

    @Synchronized
    fun close() {
        val opened = connection
        val iface = claimedInterface
        if (opened != null && iface != null) {
            runCatching { opened.releaseInterface(iface) }
        }
        opened?.close()
        clearConnection()
    }

    fun sendXfrBlockData(payload: ByteArray): ByteArray =
        transceiverOrError().sendXfrBlock(payload).data
            ?: throw UsbTransportException("USB-CCID error - empty APDU data block.")

    fun sendParamBlock(payload: ByteArray): ByteArray =
        transceiverOrError().sendParamBlock(payload)

    private fun transceiverOrError(): EasyCcidTransceiver =
        transceiver ?: error("USB CCID transceiver is not connected.")

    private fun clearConnection() {
        connection = null
        claimedInterface = null
        transceiver = null
        currentAtr = null
    }
}

private class EasyCcidTransceiver(
    private val usbConnection: UsbDeviceConnection,
    private val usbBulkIn: UsbEndpoint,
    private val usbBulkOut: UsbEndpoint,
    private val usbCcidDescription: CcidDescription
) {
    data class DataBlock(
        val length: Int,
        val slot: Byte,
        val sequence: Byte,
        val status: Byte,
        val error: Byte,
        val chainParameter: Byte,
        val data: ByteArray?
    ) {
        val iccStatus: Byte
            get() = (status.toInt() and 0x03).toByte()

        val commandStatus: Byte
            get() = ((status.toInt() shr 6) and 0x03).toByte()

        val isStatusTimeoutExtensionRequest: Boolean
            get() = commandStatus == COMMAND_STATUS_TIME_EXTENSION_REQUEST

        val isStatusSuccess: Boolean
            get() = iccStatus == ICC_STATUS_SUCCESS && commandStatus == COMMAND_STATUS_SUCCESS

        fun withData(bytes: ByteArray): DataBlock {
            require(data == null) { "Cannot add data twice." }
            return copy(data = bytes)
        }

        companion object {
            fun parseHeader(headerBytes: ByteArray): DataBlock {
                val buffer = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)
                val type = buffer.get()
                require(type == MESSAGE_TYPE_RDR_TO_PC_DATA_BLOCK.toByte()) {
                    "Header has incorrect type value."
                }
                return DataBlock(
                    length = buffer.int,
                    slot = buffer.get(),
                    sequence = buffer.get(),
                    status = buffer.get(),
                    error = buffer.get(),
                    chainParameter = buffer.get(),
                    data = null
                )
            }
        }
    }

    data class CcidErrorException(
        val errorResponse: DataBlock
    ) : Exception("USB-CCID error: status=${errorResponse.status}, error=${errorResponse.error}")

    private val inputBuffer = ByteArray(usbBulkIn.maxPacketSize.coerceAtLeast(CCID_HEADER_LENGTH))
    private var currentSequenceNumber: Byte = 0

    fun sendXfrBlock(
        payload: ByteArray,
        levelParam: Short = LEVEL_PARAM_START_SINGLE_CMD_APDU
    ): DataBlock {
        val startedAt = SystemClock.elapsedRealtime()
        val sequenceNumber = currentSequenceNumber++
        val length = payload.size
        val data = byteArrayOf(
            MESSAGE_TYPE_PC_TO_RDR_XFR_BLOCK.toByte(),
            length.toByte(),
            (length shr 8).toByte(),
            (length shr 16).toByte(),
            (length shr 24).toByte(),
            SLOT_NUMBER.toByte(),
            sequenceNumber,
            0x00.toByte(),
            (levelParam.toInt() and 0x00FF).toByte(),
            (levelParam.toInt() shr 8).toByte()
        ) + payload
        sendChunked(data)
        val block = receiveDataBlock(sequenceNumber)
        Log.d(TAG, "USB XfrBlock took ${SystemClock.elapsedRealtime() - startedAt}ms")
        return block
    }

    fun sendParamBlock(payload: ByteArray): ByteArray {
        val sequenceNumber = currentSequenceNumber++
        val length = payload.size
        val data = byteArrayOf(
            MESSAGE_TYPE_PC_TO_RDR_SET_PARAMETERS.toByte(),
            length.toByte(),
            (length shr 8).toByte(),
            (length shr 16).toByte(),
            (length shr 24).toByte(),
            SLOT_NUMBER.toByte(),
            sequenceNumber,
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte()
        ) + payload
        sendChunked(data)
        return receiveParamBlock(sequenceNumber)
    }

    fun iccPowerOn(): DataBlock {
        skipAvailableInput()
        var response: DataBlock? = null
        for (voltage in usbCcidDescription.voltages) {
            response = try {
                iccPowerOnVoltage(voltage.powerOnValue)
            } catch (error: CcidErrorException) {
                if (error.errorResponse.error.toInt() == POWER_SELECT_ERROR) {
                    iccPowerOff()
                    continue
                }
                throw error
            }
            break
        }
        return response ?: throw UsbTransportException("Could not power up ICC.")
    }

    private fun iccPowerOnVoltage(voltage: Byte): DataBlock {
        val sequenceNumber = currentSequenceNumber++
        val command = byteArrayOf(
            MESSAGE_TYPE_PC_TO_RDR_ICC_POWER_ON.toByte(),
            0x00,
            0x00,
            0x00,
            0x00,
            SLOT_NUMBER.toByte(),
            sequenceNumber,
            voltage,
            0x00,
            0x00
        )
        sendRaw(command, 0, command.size)
        return receiveDataBlock(sequenceNumber)
    }

    private fun iccPowerOff() {
        val sequenceNumber = currentSequenceNumber++
        val command = byteArrayOf(
            MESSAGE_TYPE_PC_TO_RDR_ICC_POWER_OFF.toByte(),
            0x00,
            0x00,
            0x00,
            0x00,
            SLOT_NUMBER.toByte(),
            sequenceNumber,
            0x00,
            0x00,
            0x00
        )
        sendRaw(command, 0, command.size)
    }

    private fun receiveParamBlock(expectedSequenceNumber: Byte): ByteArray {
        var response: ByteArray
        do {
            response = receiveParamBlockImmediate(expectedSequenceNumber)
        } while (response[7] == 0x80.toByte())
        return response
    }

    private fun receiveParamBlockImmediate(expectedSequenceNumber: Byte): ByteArray {
        var attempts = 3
        var readBytes: Int
        do {
            readBytes = usbConnection.bulkTransfer(
                usbBulkIn,
                inputBuffer,
                inputBuffer.size,
                DEVICE_COMMUNICATE_TIMEOUT_MS
            )
        } while (readBytes <= 0 && attempts-- > 0)
        if (readBytes <= 0) throw UsbTransportException("USB-CCID error - failed to receive parameter block.")
        if (inputBuffer[0] != MESSAGE_TYPE_RDR_TO_PC_PARAMETERS.toByte()) {
            throw UsbTransportException("USB-CCID error - bad parameter block header.")
        }
        if (expectedSequenceNumber != inputBuffer[6]) {
            throw UsbTransportException("USB-CCID error - parameter sequence mismatch.")
        }
        return inputBuffer.copyOf(readBytes)
    }

    private fun receiveDataBlock(expectedSequenceNumber: Byte): DataBlock {
        var response: DataBlock
        do {
            response = receiveDataBlockImmediate(expectedSequenceNumber)
        } while (response.isStatusTimeoutExtensionRequest)
        if (!response.isStatusSuccess) {
            throw CcidErrorException(response)
        }
        return response
    }

    private fun receiveDataBlockImmediate(expectedSequenceNumber: Byte): DataBlock {
        var attempts = 3
        var readBytes: Int
        do {
            readBytes = usbConnection.bulkTransfer(
                usbBulkIn,
                inputBuffer,
                inputBuffer.size,
                DEVICE_COMMUNICATE_TIMEOUT_MS
            )
        } while (readBytes <= 0 && attempts-- > 0)
        if (readBytes < CCID_HEADER_LENGTH) {
            throw UsbTransportException("USB-CCID error - failed to receive CCID header.")
        }
        if (inputBuffer[0] != MESSAGE_TYPE_RDR_TO_PC_DATA_BLOCK.toByte()) {
            throw UsbTransportException("USB-CCID error - bad CCID header type ${inputBuffer[0]}.")
        }
        var result = DataBlock.parseHeader(inputBuffer)
        if (expectedSequenceNumber != result.sequence) {
            throw UsbTransportException("USB-CCID error - expected sequence $expectedSequenceNumber, got ${result.sequence}.")
        }

        val dataBuffer = ByteArray(result.length)
        var bufferedBytes = readBytes - CCID_HEADER_LENGTH
        if (bufferedBytes > dataBuffer.size) {
            throw UsbTransportException("USB-CCID error - response data exceeds declared length.")
        }
        if (bufferedBytes > 0) {
            System.arraycopy(inputBuffer, CCID_HEADER_LENGTH, dataBuffer, 0, bufferedBytes)
        }
        while (bufferedBytes < dataBuffer.size) {
            readBytes = usbConnection.bulkTransfer(
                usbBulkIn,
                inputBuffer,
                inputBuffer.size,
                DEVICE_COMMUNICATE_TIMEOUT_MS
            )
            if (readBytes <= 0) throw UsbTransportException("USB error - failed reading response data.")
            if (bufferedBytes + readBytes > dataBuffer.size) {
                throw UsbTransportException("USB-CCID error - response data exceeds declared length.")
            }
            System.arraycopy(inputBuffer, 0, dataBuffer, bufferedBytes, readBytes)
            bufferedBytes += readBytes
        }
        check(dataBuffer.size == result.length) { "USB-CCID length mismatch." }
        result = result.withData(dataBuffer)
        return result
    }

    private fun sendChunked(data: ByteArray) {
        var sentBytes = 0
        while (sentBytes < data.size) {
            val chunk = usbBulkOut.maxPacketSize.coerceAtMost(data.size - sentBytes)
            sendRaw(data, sentBytes, chunk)
            sentBytes += chunk
        }
    }

    private fun sendRaw(data: ByteArray, offset: Int, length: Int) {
        val sent = usbConnection.bulkTransfer(
            usbBulkOut,
            data,
            offset,
            length,
            DEVICE_COMMUNICATE_TIMEOUT_MS
        )
        if (sent != length) {
            throw UsbTransportException("USB error - failed to transmit data ($sent/$length).")
        }
    }

    private fun skipAvailableInput() {
        do {
            val ignored = usbConnection.bulkTransfer(
                usbBulkIn,
                inputBuffer,
                inputBuffer.size,
                DEVICE_SKIP_TIMEOUT_MS
            )
            if (ignored > 0) Log.d(TAG, "Skipped $ignored stale USB bytes.")
        } while (ignored > 0)
    }

    companion object {
        private const val TAG = "EasyCcidTransceiver"
        private const val CCID_HEADER_LENGTH = 10
        private const val MESSAGE_TYPE_RDR_TO_PC_DATA_BLOCK = 0x80
        private const val MESSAGE_TYPE_RDR_TO_PC_PARAMETERS = 0x82
        private const val MESSAGE_TYPE_PC_TO_RDR_ICC_POWER_ON = 0x62
        private const val MESSAGE_TYPE_PC_TO_RDR_ICC_POWER_OFF = 0x63
        private const val MESSAGE_TYPE_PC_TO_RDR_XFR_BLOCK = 0x6F
        private const val MESSAGE_TYPE_PC_TO_RDR_SET_PARAMETERS = 0x61
        private const val SLOT_NUMBER = 0x00
        private const val ICC_STATUS_SUCCESS: Byte = 0
        private const val COMMAND_STATUS_SUCCESS: Byte = 0
        private const val COMMAND_STATUS_TIME_EXTENSION_REQUEST: Byte = 2
        private const val POWER_SELECT_ERROR = 7
        private const val DEVICE_COMMUNICATE_TIMEOUT_MS = 5_000
        private const val DEVICE_SKIP_TIMEOUT_MS = 100
        private const val LEVEL_PARAM_START_SINGLE_CMD_APDU: Short = 0x0000
    }
}

private class ParsedAtr private constructor(
    val ts: Byte?,
    val ta1: Byte?,
    val tc1: Byte?,
    val tc2: Byte?
) {
    companion object {
        fun parse(atr: ByteArray): ParsedAtr {
            require(atr.size >= 2) { "ATR too short." }
            val ts = atr[0]
            val t0 = atr[1]
            val tx1 = arrayOf<Byte?>(null, null, null, null)
            val tx2 = arrayOf<Byte?>(null, null, null, null)
            var pointer = 2

            for (i in 0..3) {
                if (t0.toInt() and (0x10 shl i) != 0) {
                    if (pointer < atr.size) tx1[i] = atr[pointer++]
                }
            }
            val td1 = tx1[3] ?: 0
            for (i in 0..3) {
                if (td1.toInt() and (0x10 shl i) != 0) {
                    if (pointer < atr.size) tx2[i] = atr[pointer++]
                }
            }
            return ParsedAtr(ts = ts, ta1 = tx1[0], tc1 = tx1[2], tc2 = tx2[2])
        }
    }
}

private data class CcidDescription(
    private val voltageSupport: Byte,
    private val protocols: Int,
    private val features: Int
) {
    enum class Voltage(powerOnValue: Int, mask: Int) {
        AUTO(0, 0),
        V50(1, 1),
        V30(2, 2),
        V18(3, 4);

        val powerOnValue = powerOnValue.toByte()
        val mask = mask.toByte()
    }

    val voltages: List<Voltage>
        get() {
            if (hasFeature(FEATURE_AUTOMATIC_VOLTAGE)) return listOf(Voltage.AUTO)
            return Voltage.entries.filter { voltage ->
                voltage.mask.toInt() and voltageSupport.toInt() != 0
            }.ifEmpty { listOf(Voltage.AUTO) }
        }

    val hasT0Protocol: Boolean
        get() = protocols and MASK_T0_PROTOCOL != 0

    private fun hasFeature(feature: Int): Boolean =
        features and feature != 0

    companion object {
        private const val DESCRIPTOR_LENGTH = 0x36
        private const val DESCRIPTOR_TYPE = 0x21
        private const val SLOT_OFFSET = 4
        private const val FEATURES_OFFSET = 40
        private const val FEATURE_AUTOMATIC_VOLTAGE = 0x00008
        private const val MASK_T0_PROTOCOL = 1

        fun fromRawDescriptors(raw: ByteArray): CcidDescription? =
            runCatching {
                val buffer = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
                while (buffer.remaining() >= 2) {
                    buffer.mark()
                    val length = buffer.get().toInt() and 0xFF
                    val type = buffer.get().toInt() and 0xFF
                    if (length < 2 || buffer.position() + length - 2 > raw.size) return@runCatching null
                    if (type == DESCRIPTOR_TYPE && length == DESCRIPTOR_LENGTH) {
                        buffer.reset()
                        buffer.position(buffer.position() + SLOT_OFFSET + 1)
                        val voltageSupport = buffer.get()
                        val protocols = buffer.int
                        buffer.reset()
                        buffer.position(buffer.position() + FEATURES_OFFSET)
                        val features = buffer.int
                        return@runCatching CcidDescription(voltageSupport, protocols, features)
                    }
                    buffer.position(buffer.position() + length - 2)
                }
                null
            }.getOrNull()
    }
}

private class UsbTransportException(message: String) : Exception(message)

private fun UsbDevice.findCcidInterface(interfaceId: Int?): UsbInterface? {
    val interfaces = ccidInterfacesForExternalEuicc()
    return if (interfaceId != null) {
        interfaces.firstOrNull { it.id == interfaceId }
    } else {
        interfaces.firstOrNull()
    }
}

private fun UsbDevice.ccidInterfacesForExternalEuicc(): List<UsbInterface> {
    val interfaces = (0 until interfaceCount).map { getInterface(it) }
    val interfaceLevelCcid = interfaces.filter { it.interfaceClass == UsbConstants.USB_CLASS_CSCID }
    if (interfaceLevelCcid.isNotEmpty()) return interfaceLevelCcid
    if (deviceClass != UsbConstants.USB_CLASS_CSCID) return emptyList()
    return interfaces.filter {
        val (bulkIn, bulkOut) = it.bulkPair()
        bulkIn != null && bulkOut != null
    }
}

private fun UsbInterface.bulkPair(): Pair<UsbEndpoint?, UsbEndpoint?> {
    val endpoints = (0 until endpointCount)
        .map { getEndpoint(it) }
        .filter { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK }
    return Pair(
        endpoints.find { it.direction == UsbConstants.USB_DIR_IN },
        endpoints.find { it.direction == UsbConstants.USB_DIR_OUT }
    )
}

private fun buildCmd(
    cla: Byte,
    ins: Byte,
    p1: Byte,
    p2: Byte,
    data: ByteArray?,
    le: Byte?
): ByteArray =
    byteArrayOf(cla, ins, p1, p2).let { header ->
        if (data != null) header + data.size.toByte() + data else header
    }.let { body ->
        if (le != null) body + le else body
    }

private fun manageChannelCmd(open: Boolean, channel: Int): ByteArray =
    if (open) {
        buildCmd(0x00, 0x70, 0x00, 0x00, null, 0x01)
    } else {
        buildCmd(channel.toByte(), 0x70, 0x80.toByte(), channel.toByte(), null, null)
    }

private fun selectByDfCmd(aid: ByteArray, channel: Int): ByteArray =
    buildCmd(channel.toByte(), 0xA4.toByte(), 0x04, 0x00, aid, null)

private fun isSuccessResponse(response: ByteArray): Boolean =
    response.size >= 2 &&
        response[response.size - 2] == 0x90.toByte() &&
        response[response.size - 1] == 0x00.toByte()

private fun isKnownTpduReader(vendorId: Int, productId: Int): Boolean =
    vendorId == 0x0BDA && productId == 0x0169

private fun String.decodeHex(): ByteArray {
    require(length % 2 == 0) { "Hex string must have even length." }
    return ByteArray(length / 2) { index ->
        substring(index * 2, index * 2 + 2).toInt(16).toByte()
    }
}

private fun ByteArray.toHex(): String =
    joinToString(separator = "") { byte -> "%02X".format(byte) }

private fun ByteArray.summary(): String {
    if (size < 2) return "bytes=$size"
    val sw1 = this[size - 2].toInt() and 0xFF
    val sw2 = this[size - 1].toInt() and 0xFF
    return "SW=%02X%02X bytes=%d".format(sw1, sw2, size)
}

private fun ByteArray.statusWordHexOrUnknown(): String =
    if (size >= 2) {
        "%02X%02X".format(this[size - 2].toInt() and 0xFF, this[size - 1].toInt() and 0xFF)
    } else {
        "unknown"
    }
