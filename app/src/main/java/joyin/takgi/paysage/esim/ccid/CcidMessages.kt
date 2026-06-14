package joyin.takgi.paysage.esim.ccid

data class CcidDataBlock(
    val slot: Int,
    val sequence: Int,
    val status: Int,
    val error: Int,
    val chainParameter: Int,
    val data: ByteArray
) {
    val isCommandSuccessful: Boolean
        get() = status and COMMAND_STATUS_MASK == COMMAND_STATUS_SUCCESS

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CcidDataBlock) return false
        return slot == other.slot &&
            sequence == other.sequence &&
            status == other.status &&
            error == other.error &&
            chainParameter == other.chainParameter &&
            data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = slot
        result = 31 * result + sequence
        result = 31 * result + status
        result = 31 * result + error
        result = 31 * result + chainParameter
        result = 31 * result + data.contentHashCode()
        return result
    }

    companion object {
        private const val COMMAND_STATUS_MASK = 0xC0
        private const val COMMAND_STATUS_SUCCESS = 0x00
    }
}

object CcidMessages {
    private const val HEADER_SIZE = 10
    private const val PC_TO_RDR_ICC_POWER_ON = 0x62
    private const val PC_TO_RDR_XFR_BLOCK = 0x6F
    private const val RDR_TO_PC_DATA_BLOCK = 0x80

    fun buildIccPowerOn(sequence: Int, slot: Int = 0, powerSelect: Int = 0): ByteArray {
        require(sequence in 0..0xFF) { "CCID sequence must fit in one byte" }
        require(slot in 0..0xFF) { "CCID slot must fit in one byte" }
        require(powerSelect in 0..0xFF) { "CCID power select must fit in one byte" }
        return ByteArray(HEADER_SIZE).apply {
            this[0] = PC_TO_RDR_ICC_POWER_ON.toByte()
            writeLength(0)
            this[5] = slot.toByte()
            this[6] = sequence.toByte()
            this[7] = powerSelect.toByte()
        }
    }

    fun buildXfrBlock(
        apdu: ByteArray,
        sequence: Int,
        slot: Int = 0,
        blockWaitTimeExtension: Int = 0,
        levelParameter: Int = 0
    ): ByteArray {
        require(sequence in 0..0xFF) { "CCID sequence must fit in one byte" }
        require(slot in 0..0xFF) { "CCID slot must fit in one byte" }
        require(blockWaitTimeExtension in 0..0xFF) { "CCID BWI must fit in one byte" }
        require(levelParameter in 0..0xFFFF) { "CCID level parameter must fit in two bytes" }
        return ByteArray(HEADER_SIZE + apdu.size).apply {
            this[0] = PC_TO_RDR_XFR_BLOCK.toByte()
            writeLength(apdu.size)
            this[5] = slot.toByte()
            this[6] = sequence.toByte()
            this[7] = blockWaitTimeExtension.toByte()
            this[8] = (levelParameter and 0xFF).toByte()
            this[9] = (levelParameter shr 8 and 0xFF).toByte()
            apdu.copyInto(this, HEADER_SIZE)
        }
    }

    fun parseDataBlock(response: ByteArray): CcidDataBlock {
        require(response.size >= HEADER_SIZE) { "CCID response shorter than header" }
        val messageType = response[0].toInt() and 0xFF
        require(messageType == RDR_TO_PC_DATA_BLOCK) {
            "Unexpected CCID response type 0x${messageType.toString(16)}"
        }
        val length = response.readLength()
        require(response.size >= HEADER_SIZE + length) { "CCID response data is truncated" }
        return CcidDataBlock(
            slot = response[5].toInt() and 0xFF,
            sequence = response[6].toInt() and 0xFF,
            status = response[7].toInt() and 0xFF,
            error = response[8].toInt() and 0xFF,
            chainParameter = response[9].toInt() and 0xFF,
            data = response.copyOfRange(HEADER_SIZE, HEADER_SIZE + length)
        )
    }

    private fun ByteArray.writeLength(length: Int) {
        this[1] = (length and 0xFF).toByte()
        this[2] = (length shr 8 and 0xFF).toByte()
        this[3] = (length shr 16 and 0xFF).toByte()
        this[4] = (length shr 24 and 0xFF).toByte()
    }

    private fun ByteArray.readLength(): Int =
        (this[1].toInt() and 0xFF) or
            ((this[2].toInt() and 0xFF) shl 8) or
            ((this[3].toInt() and 0xFF) shl 16) or
            ((this[4].toInt() and 0xFF) shl 24)
}
