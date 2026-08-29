package joyin.takgi.paysage.esim.lpa

import android.se.omapi.Channel
import android.se.omapi.Reader
import android.se.omapi.SEService
import android.se.omapi.Session
import android.util.Log
import android.os.Build
import joyin.takgi.paysage.esim.toHex
import net.typeblog.lpac_jni.ApduInterface
import java.util.concurrent.atomic.AtomicInteger

class PaysageOmapiApduInterface(
    private val service: SEService,
    private val readerName: String,
    private val logicalSlotIndex: Int? = null,
    private val diagnosticRecorder: ((stage: String, message: String) -> Unit)? = null
) : ApduInterface {
    private lateinit var session: Session
    private val nextHandle = AtomicInteger(0)
    private val channels = mutableMapOf<Int, Channel>()

    override val valid: Boolean
        get() = service.isConnected && this::session.isInitialized && !session.isClosed

    @Synchronized
    override fun connect() {
        if (this::session.isInitialized && !session.isClosed) return
        record(
            "omapi_apdu_connect_start",
            "reader=$readerName logicalSlot=${logicalSlotIndex ?: "legacy"} serviceConnected=${service.isConnected}"
        )
        val reader = if (logicalSlotIndex != null) {
            service.getUiccReaderCompat(logicalSlotIndex + 1)
        } else {
            service.getReaders().orEmpty().first { candidate -> candidate.safeName() == readerName }
        }
        record(
            "omapi_apdu_reader",
            "reader=${reader.safeName()} logicalSlot=${logicalSlotIndex ?: "legacy"} present=${runCatching { reader.isSecureElementPresent }.getOrNull()}"
        )
        session = reader.openSession()
        record(
            "omapi_apdu_connect_ok",
            "reader=${reader.safeName()} logicalSlot=${logicalSlotIndex ?: "legacy"} atrBytes=${runCatching { session.atr?.size ?: 0 }.getOrDefault(0)}"
        )
    }

    @Synchronized
    override fun disconnect() {
        record(
            "omapi_apdu_disconnect_start",
            "openChannels=${channels.size} sessionInitialized=${this::session.isInitialized}"
        )
        channels.values.forEach { channel ->
            if (channel.isOpen) channel.close()
        }
        channels.clear()
        if (this::session.isInitialized && !session.isClosed) {
            session.close()
        }
        record("omapi_apdu_disconnect_ok", "reader=$readerName logicalSlot=${logicalSlotIndex ?: "legacy"}")
    }

    @Synchronized
    override fun logicalChannelOpen(aid: ByteArray): Int {
        if (!this::session.isInitialized || session.isClosed) connect()
        val aidHex = aid.toHex()
        record(
            "omapi_apdu_open_channel_start",
            "reader=$readerName logicalSlot=${logicalSlotIndex ?: "legacy"} aid=$aidHex"
        )
        val channel = runCatching { session.openLogicalChannel(aid) }
            .onFailure { error ->
                record(
                    "omapi_apdu_open_channel_error",
                    "reader=$readerName logicalSlot=${logicalSlotIndex ?: "legacy"} aid=$aidHex error=${error.javaClass.simpleName} message=${error.message.orEmpty()}"
                )
            }
            .getOrThrow()
            ?: run {
                record(
                    "omapi_apdu_open_channel_null",
                    "reader=$readerName logicalSlot=${logicalSlotIndex ?: "legacy"} aid=$aidHex"
                )
                error("Failed to open logical channel ($aidHex).")
            }
        val handle = nextHandle.incrementAndGet()
        synchronized(channels) { channels[handle] = channel }
        record(
            "omapi_apdu_open_channel_ok",
            "reader=$readerName logicalSlot=${logicalSlotIndex ?: "legacy"} aid=$aidHex handle=$handle"
        )
        return handle
    }

    @Synchronized
    override fun logicalChannelClose(handle: Int) {
        record(
            "omapi_apdu_close_channel_start",
            "reader=$readerName logicalSlot=${logicalSlotIndex ?: "legacy"} handle=$handle"
        )
        val channel = channels[handle] ?: error("Invalid OMAPI logical channel handle $handle.")
        if (channel.isOpen) channel.close()
        synchronized(channels) { channels.remove(handle) }
        record(
            "omapi_apdu_close_channel_ok",
            "reader=$readerName logicalSlot=${logicalSlotIndex ?: "legacy"} handle=$handle"
        )
    }

    @Synchronized
    override fun transmit(handle: Int, tx: ByteArray): ByteArray {
        val channel = channels[handle] ?: error("Invalid OMAPI logical channel handle $handle.")
        repeat(MAX_TRANSMIT_ATTEMPTS) { attempt ->
            val response = runCatching { channel.transmit(tx) }
                .onFailure { error ->
                    record(
                        "omapi_apdu_transmit_error",
                        "reader=$readerName logicalSlot=${logicalSlotIndex ?: "legacy"} handle=$handle bytes=${tx.size} attempt=${attempt + 1} error=${error.javaClass.simpleName} message=${error.message.orEmpty()}"
                    )
                }
                .getOrThrow()
            if (response.size == 2 && response[0] == 0x66.toByte() && response[1] == 0x01.toByte()) {
                Log.d(TAG, "OMAPI checksum error 6601, retrying (${attempt + 1}/$MAX_TRANSMIT_ATTEMPTS).")
                record(
                    "omapi_apdu_transmit_retry",
                    "reader=$readerName logicalSlot=${logicalSlotIndex ?: "legacy"} handle=$handle sw=6601 attempt=${attempt + 1}"
                )
            } else {
                record(
                    "omapi_apdu_transmit_ok",
                    "reader=$readerName logicalSlot=${logicalSlotIndex ?: "legacy"} handle=$handle bytes=${tx.size} responseBytes=${response.size} sw=${response.statusWordHexOrUnknown()}"
                )
                return response
            }
        }
        record(
            "omapi_apdu_transmit_exhausted",
            "reader=$readerName logicalSlot=${logicalSlotIndex ?: "legacy"} handle=$handle bytes=${tx.size}"
        )
        error("OMAPI APDU transmit failed after checksum retries.")
    }

    private fun record(stage: String, message: String) {
        runCatching { diagnosticRecorder?.invoke(stage, message) }
    }

    companion object {
        private const val TAG = "PaysageOmapiApdu"
        private const val MAX_TRANSMIT_ATTEMPTS = 11
    }
}

private fun ByteArray.statusWordHexOrUnknown(): String =
    if (size >= 2) {
        "%02X%02X".format(this[size - 2].toInt() and 0xFF, this[size - 1].toInt() and 0xFF)
    } else {
        "unknown"
    }

private fun Reader.safeName(): String =
    name.orEmpty().ifBlank { "Unnamed reader" }

private fun SEService.getUiccReaderCompat(slotNumber: Int): Reader =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        getUiccReader(slotNumber)
    } else {
        getReaders().orEmpty().first { reader ->
            val name = reader.safeName()
            name == "SIM$slotNumber" || (slotNumber == 1 && name == "SIM")
        }
    }
