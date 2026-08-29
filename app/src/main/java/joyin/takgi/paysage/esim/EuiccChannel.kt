package joyin.takgi.paysage.esim

import net.typeblog.lpac_jni.ApduInterface
import net.typeblog.lpac_jni.LocalProfileAssistant

/**
 * Represents a valid eUICC channel with LPA and APDU interface.
 * Based on EasyEUICC's EuiccChannel interface.
 */
interface EuiccChannel {
    val type: String

    val slotId: Int // PHYSICAL slot
    val logicalSlotId: Int
    val portId: Int

    /**
     * Secure element ID on cards with multiple SEs
     */
    val seId: SecureElementId

    /**
     * Does this channel belong to a chip that supports multiple SEs?
     */
    var hasMultipleSE: Boolean

    val lpa: LocalProfileAssistant

    val valid: Boolean

    /**
     * Answer to Reset (ATR) value of the underlying interface, if any
     */
    val atr: ByteArray?

    /**
     * The underlying APDU interface for this channel
     */
    val apduInterface: ApduInterface

    /**
     * The AID of the ISD-R channel currently in use
     */
    val isdrAid: ByteArray

    fun close()

    /**
     * Semi-obscure wrapper over secure element ID to prevent misuse
     */
    data class SecureElementId(val id: Int) {
        companion object {
            val DEFAULT = SecureElementId(0)

            fun createFromInt(id: Int) = SecureElementId(id)
        }
    }
}
