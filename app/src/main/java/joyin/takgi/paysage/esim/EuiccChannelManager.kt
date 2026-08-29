package joyin.takgi.paysage.esim

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import joyin.takgi.paysage.esim.lpa.PaysageLpacHttpInterface
import joyin.takgi.paysage.esim.lpa.PaysageOmapiApduInterface
import joyin.takgi.paysage.esim.lpa.PaysageUsbCcidApduInterface
import joyin.takgi.paysage.esim.lpa.PaysageUsbCcidContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.typeblog.lpac_jni.ApduInterface
import net.typeblog.lpac_jni.impl.LocalProfileAssistantImpl
import java.util.concurrent.Executor
import kotlin.coroutines.resume

/**
 * Channel manager for eUICC access with caching and vendor-specific AID handling.
 * Based on EasyEUICC's DefaultEuiccChannelManager.
 */
class EuiccChannelManager(private val context: Context) {
    companion object {
        private const val TAG = "EuiccChannelManager"
        const val USB_CHANNEL_ID = -1
    }

    private val channelCache = mutableListOf<EuiccChannel>()
    private var usbChannels = mutableListOf<EuiccChannel>()
    private val lock = Mutex()
    private var seService: android.se.omapi.SEService? = null

    private val usbManager by lazy {
        context.getSystemService(Context.USB_SERVICE) as UsbManager
    }

    /**
     * Try to open channels using known AIDs with vendor-specific ordering and stopping logic.
     * This is the core of EasyEUICC's channel opening strategy.
     */
    private suspend fun tryOpenChannelWithKnownAids(
        openFn: suspend (ByteArray, EuiccChannel.SecureElementId) -> EuiccChannel?
    ): List<EuiccChannel> {
        var isdrAidList = parseIsdrAidList(getStoredAidList())
        val ret = mutableListOf<EuiccChannel>()
        val openedAids = mutableListOf<ByteArray>()
        var hasReset = false
        var vendorDecider: VendorAidDecider? = null
        var seId = 0

        outer@ while (true) {
            for (aid in isdrAidList) {
                if (vendorDecider != null && !vendorDecider.shouldOpenMore(openedAids, aid)) {
                    break@outer
                }

                val channel = openFn(aid, EuiccChannel.SecureElementId.createFromInt(seId))?.let { channel ->
                    if (channel.valid) {
                        seId += 1
                        channel
                    } else {
                        channel.close()
                        null
                    }
                }

                if (!hasReset) {
                    val res = channel?.queryVendorAidListTransformation(isdrAidList)
                    if (res != null) {
                        // Reset the for loop since we needed to replace the AID list due to vendor-specific code
                        Log.i(TAG, "AID list replaced by vendor transformation, resetting open attempt")
                        isdrAidList = res.first
                        vendorDecider = res.second
                        seId = 0
                        ret.clear()
                        openedAids.clear()
                        channel.close()
                        hasReset = true // Don't let anything reset again
                        continue@outer
                    }
                }

                if (channel != null) {
                    ret.add(channel)
                    openedAids.add(aid)

                    // Don't try opening more than 1 channel unless there is a vendor
                    // implementation for deciding when we should stop opening more channels
                    if (vendorDecider == null) {
                        break@outer
                    }
                }
            }

            // If we get here we should exit, since the inner loop completed without resetting
            break
        }

        // Set the hasMultipleSE field now since we only get to know that after we have iterated all AIDs
        ret.forEach { it.hasMultipleSE = (seId > 1) }

        return ret
    }

    /**
     * Find or open channels for a given OMAPI slot and port
     */
    suspend fun findChannelsBySlotAndPort(
        physicalSlotId: Int,
        portId: Int,
        logicalSlotId: Int
    ): List<EuiccChannel>? = withContext(Dispatchers.IO) {
        lock.withLock {
            // Check cache first
            val existing = channelCache.filter {
                it.slotId == physicalSlotId && it.portId == portId
            }

            if (existing.isNotEmpty()) {
                if (existing.all { it.valid && it.logicalSlotId == logicalSlotId }) {
                    return@withContext existing
                } else {
                    // If any channel shouldn't be considered valid anymore, close all and reopen
                    existing.forEach {
                        it.close()
                        channelCache.remove(it)
                    }
                }
            }

            // Try to open new channels
            val channels = tryOpenChannelWithKnownAids { isdrAid, seId ->
                tryOpenOmapiChannel(physicalSlotId, portId, logicalSlotId, isdrAid, seId)
            }

            if (channels.isNotEmpty()) {
                channelCache.addAll(channels)
                return@withContext channels
            } else {
                Log.w(TAG, "Opened channel for slot $physicalSlotId port $portId, but channel is invalid")
                return@withContext null
            }
        }
    }

    /**
     * Find or open USB channels
     */
    suspend fun findUsbChannels(device: UsbDevice, interfaceIndex: Int): List<EuiccChannel>? = withContext(Dispatchers.IO) {
        lock.withLock {
            if (usbChannels.isNotEmpty() && usbChannels.all { it.valid }) {
                return@withContext usbChannels
            }

            // Clear old USB channels
            usbChannels.forEach { it.close() }
            usbChannels.clear()

            // Try to open new USB channels
            val channels = tryOpenChannelWithKnownAids { isdrAid, seId ->
                tryOpenUsbChannel(device, interfaceIndex, isdrAid, seId)
            }

            if (channels.isNotEmpty()) {
                usbChannels.addAll(channels)
                return@withContext channels
            } else {
                Log.w(TAG, "Opened USB channel, but channel is invalid")
                return@withContext null
            }
        }
    }

    private suspend fun tryOpenOmapiChannel(
        physicalSlotId: Int,
        portId: Int,
        logicalSlotId: Int,
        isdrAid: ByteArray,
        seId: EuiccChannel.SecureElementId
    ): EuiccChannel? {
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null

            val service = ensureSEService()

            val apduInterface = PaysageOmapiApduInterface(
                service = service,
                readerName = "",
                logicalSlotIndex = logicalSlotId
            )

            val lpa = LocalProfileAssistantImpl(
                isdrAid = isdrAid,
                rawApduInterface = apduInterface,
                rawHttpInterface = PaysageLpacHttpInterface()
            )

            if (!lpa.valid) {
                lpa.close()
                apduInterface.disconnect()
                return null
            }

            EuiccChannelImpl(
                type = "OMAPI",
                slotId = physicalSlotId,
                logicalSlotId = logicalSlotId,
                portId = portId,
                seId = seId,
                lpa = lpa,
                apduInterface = apduInterface,
                isdrAid = isdrAid,
                atr = null
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to open OMAPI channel for slot $physicalSlotId port $portId", e)
            null
        }
    }

    private suspend fun ensureSEService(): android.se.omapi.SEService {
        val existing = seService
        if (existing != null && existing.isConnected) return existing
        return connectSEService().also { seService = it }
    }

    private suspend fun connectSEService(): android.se.omapi.SEService =
        suspendCancellableCoroutine { continuation ->
            var service: android.se.omapi.SEService? = null
            val executor = Executor { runnable -> runnable.run() }
            val listener = android.se.omapi.SEService.OnConnectedListener {
                val connected = service
                if (connected != null && continuation.isActive) {
                    continuation.resume(connected)
                }
            }
            service = android.se.omapi.SEService(context, executor, listener)
            continuation.invokeOnCancellation {
                service?.shutdown()
            }
        }

    private fun tryOpenUsbChannel(
        device: UsbDevice,
        interfaceIndex: Int,
        isdrAid: ByteArray,
        seId: EuiccChannel.SecureElementId
    ): EuiccChannel? {
        return try {
            val ccidContext = PaysageUsbCcidContext(
                usbManager = usbManager,
                device = device,
                forceTpduMode = false,
                usbInterfaceId = interfaceIndex
            )
            val apduInterface = PaysageUsbCcidApduInterface(
                usbManager = usbManager,
                device = device,
                forceTpduMode = false,
                usbInterfaceId = interfaceIndex,
                sharedContext = ccidContext
            )

            apduInterface.connect()
            val handle = apduInterface.logicalChannelOpen(isdrAid)

            val lpa = LocalProfileAssistantImpl(
                isdrAid = isdrAid,
                rawApduInterface = apduInterface,
                rawHttpInterface = PaysageLpacHttpInterface()
            )

            if (!lpa.valid) {
                lpa.close()
                apduInterface.disconnect()
                return null
            }

            EuiccChannelImpl(
                type = "USB",
                slotId = USB_CHANNEL_ID,
                logicalSlotId = USB_CHANNEL_ID,
                portId = interfaceIndex,
                seId = seId,
                lpa = lpa,
                apduInterface = apduInterface,
                isdrAid = isdrAid,
                atr = ccidContext.atr
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to open USB channel for device ${device.deviceName} interface $interfaceIndex", e)
            null
        }
    }

    /**
     * Close all cached channels
     */
    suspend fun closeAll() = withContext(Dispatchers.IO) {
        lock.withLock {
            channelCache.forEach { it.close() }
            channelCache.clear()

            usbChannels.forEach { it.close() }
            usbChannels.clear()

            seService?.shutdown()
            seService = null
        }
    }

    private fun getStoredAidList(): String {
        // TODO: Load from preferences, for now use defaults
        return ""
    }

    private fun parseIsdrAidList(stored: String): List<ByteArray> {
        val defaults = listOf(
            Iso7816.EUICC_DEFAULT_ISDR_AID_HEX,
            // Add eSTK AIDs explicitly as they're common
            "A06573746B6D65FFFF4953442D522030", // SE0
            "A06573746B6D65FFFF4953442D522031"  // SE1
        )

        if (stored.isBlank()) {
            return defaults.map { it.hexToByteArray() }
        }

        // Parse stored list
        val parsed = stored.split(",").mapNotNull { aid ->
            runCatching { aid.trim().hexToByteArray() }.getOrNull()
        }

        return if (parsed.isEmpty()) {
            defaults.map { it.hexToByteArray() }
        } else {
            parsed
        }
    }
}

private fun String.hexToByteArray(): ByteArray {
    require(length % 2 == 0) { "Hex string must have even length" }
    return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
