package joyin.takgi.paysage.esim

data class Iso7816Response(
    val data: ByteArray,
    val sw1: Int,
    val sw2: Int
) {
    val statusWordHex: String
        get() = "%02X%02X".format(sw1, sw2)

    val isSuccess: Boolean
        get() = sw1 == 0x90 && sw2 == 0x00

    val isWarning: Boolean
        get() = sw1 == 0x62 || sw1 == 0x63

    val isAccepted: Boolean
        get() = isSuccess || isWarning

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Iso7816Response) return false
        return data.contentEquals(other.data) && sw1 == other.sw1 && sw2 == other.sw2
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + sw1
        result = 31 * result + sw2
        return result
    }
}

object EsimApdu {
    const val ISD_R_AID_HEX: String = "A0000005591010FFFFFFFF8900000100"
    const val ESIM_ME_AID_HEX: String = "A0000005591010000000008900000300"
    const val FIVE_BER_AID_HEX: String = "A0000005591010FFFFFFFF8900050500"
    const val XESIM_AID_HEX: String = "A0000005591010FFFFFFFF8900000177"
    const val LINKSFIELD_AID_HEX: String = "A000000559104C696E6B736669656C64"
    const val ESTK_PRODUCT_AID_HEX: String = "A06573746B6D65FFFFFFFFFFFF6D6774"
    const val ESTK_SE0_AID_HEX: String = "A06573746B6D65FFFF4953442D522030"
    const val ESTK_SE1_AID_HEX: String = "A06573746B6D65FFFF4953442D522031"
    const val ESTK_AUX_AID_HEX: String = "A06573746B6D65FFFFFFFF4953442D52"

    val ISD_R_AID: ByteArray = ISD_R_AID_HEX.decodeAidHex()
    val ESIM_ME_AID: ByteArray = ESIM_ME_AID_HEX.decodeAidHex()
    val FIVE_BER_AID: ByteArray = FIVE_BER_AID_HEX.decodeAidHex()
    val XESIM_AID: ByteArray = XESIM_AID_HEX.decodeAidHex()
    val LINKSFIELD_AID: ByteArray = LINKSFIELD_AID_HEX.decodeAidHex()
    val ESTK_PRODUCT_AID: ByteArray = ESTK_PRODUCT_AID_HEX.decodeAidHex()
    val ESTK_SE0_AID: ByteArray = ESTK_SE0_AID_HEX.decodeAidHex()
    val ESTK_SE1_AID: ByteArray = ESTK_SE1_AID_HEX.decodeAidHex()
    val ESTK_AUX_AID: ByteArray = ESTK_AUX_AID_HEX.decodeAidHex()

    val ESTK_PREFERRED_ISD_R_AIDS: List<ByteArray> = listOf(
        ESTK_SE0_AID,
        ESTK_SE1_AID
    )

    val KNOWN_ISD_R_AIDS: List<ByteArray> = listOf(
        ISD_R_AID,
        ESIM_ME_AID,
        FIVE_BER_AID,
        XESIM_AID,
        LINKSFIELD_AID,
        ESTK_SE0_AID,
        ESTK_SE1_AID,
        ESTK_AUX_AID
    )

    fun buildSelectIsdR(): ByteArray = buildSelectByAid(ISD_R_AID)

    fun buildSelectByAid(aid: ByteArray): ByteArray {
        require(aid.size in 5..16) { "AID length must be between 5 and 16 bytes" }
        return byteArrayOf(
            0x00,
            0xA4.toByte(),
            0x04,
            0x00,
            aid.size.toByte()
        ) + aid + byteArrayOf(0x00)
    }

    fun parseResponse(response: ByteArray): Iso7816Response {
        require(response.size >= 2) { "APDU response must include SW1 and SW2" }
        val sw1 = response[response.lastIndex - 1].toInt() and 0xFF
        val sw2 = response[response.lastIndex].toInt() and 0xFF
        return Iso7816Response(
            data = response.copyOfRange(0, response.size - 2),
            sw1 = sw1,
            sw2 = sw2
        )
    }

    fun aidLabel(aid: ByteArray): String = when {
        aid.contentEquals(ESTK_SE0_AID) -> "eSTK SE0"
        aid.contentEquals(ESTK_SE1_AID) -> "eSTK SE1"
        aid.contentEquals(ESTK_AUX_AID) -> "eSTK AUX"
        aid.contentEquals(ESIM_ME_AID) -> "eSIM.me"
        aid.contentEquals(FIVE_BER_AID) -> "5ber.eSIM"
        aid.contentEquals(XESIM_AID) -> "Xesim"
        aid.contentEquals(LINKSFIELD_AID) -> "LinksField"
        aid.contentEquals(ISD_R_AID) -> "GSMA ISD-R"
        else -> aid.toHex()
    }

    fun isEstkSpecificIsdRAid(aid: ByteArray): Boolean =
        aid.contentEquals(ESTK_SE0_AID) || aid.contentEquals(ESTK_SE1_AID)

    fun aidHex(aid: ByteArray): String = aid.toHex()

    fun decodeAidHex(hex: String): ByteArray = hex.decodeAidHex()

    fun withEstkPreferredAids(aids: List<ByteArray>): List<ByteArray> =
        (ESTK_PREFERRED_ISD_R_AIDS + aids).distinctBy { aidHex(it) }
}

object Iso7816 {
    const val EUICC_DEFAULT_ISDR_AID_HEX: String = "A0000005591010FFFFFFFF8900000100"
}

private fun String.decodeAidHex(): ByteArray {
    require(length % 2 == 0) { "AID hex must have even length" }
    return ByteArray(length / 2) { index ->
        substring(index * 2, index * 2 + 2).toInt(16).toByte()
    }
}
