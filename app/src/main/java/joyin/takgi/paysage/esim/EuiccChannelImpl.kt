package joyin.takgi.paysage.esim

import android.util.Log
import net.typeblog.lpac_jni.ApduInterface
import net.typeblog.lpac_jni.LocalProfileAssistant

/**
 * Implementation of EuiccChannel wrapping an LPA instance.
 * Based on EasyEUICC's EuiccChannelImpl.
 */
class EuiccChannelImpl(
    override val type: String,
    override val slotId: Int,
    override val logicalSlotId: Int,
    override val portId: Int,
    override val seId: EuiccChannel.SecureElementId,
    override val lpa: LocalProfileAssistant,
    override val apduInterface: ApduInterface,
    override val isdrAid: ByteArray,
    override val atr: ByteArray? = null
) : EuiccChannel {
    companion object {
        private const val TAG = "EuiccChannelImpl"
    }

    private var _hasMultipleSE: Boolean = false
    private var hasMultipleSESet: Boolean = false

    override var hasMultipleSE: Boolean
        get() = _hasMultipleSE
        set(value) {
            if (hasMultipleSESet) {
                throw IllegalStateException("hasMultipleSE can only be set once")
            }
            _hasMultipleSE = value
            hasMultipleSESet = true
        }

    override val valid: Boolean by lazy {
        runCatching {
            lpa.valid && lpa.eID.isNotBlank()
        }.getOrElse { false }
    }

    override fun close() {
        runCatching {
            lpa.close()
        }.onFailure { e ->
            Log.w(TAG, "Failed to close LPA for slot $slotId port $portId seId ${seId.id}", e)
        }
    }

    override fun toString(): String {
        return "EuiccChannel(type=$type, slot=$slotId, logicalSlot=$logicalSlotId, port=$portId, seId=${seId.id}, valid=$valid)"
    }
}
