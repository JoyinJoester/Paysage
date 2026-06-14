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
    val ISD_R_AID: ByteArray = byteArrayOf(
        0xA0.toByte(),
        0x00,
        0x00,
        0x05,
        0x59,
        0x10,
        0x10,
        0xFF.toByte(),
        0xFF.toByte(),
        0xFF.toByte(),
        0xFF.toByte(),
        0x89.toByte(),
        0x00,
        0x00,
        0x01,
        0x00
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
}
