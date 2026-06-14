package joyin.takgi.paysage.esim

data class BerTlv(
    val tagBytes: ByteArray,
    val value: ByteArray,
    val children: List<BerTlv>
) {
    val tagHex: String
        get() = tagBytes.toHex()

    val isConstructed: Boolean
        get() = tagBytes.firstOrNull()?.toInt()?.and(CONSTRUCTED_MASK) == CONSTRUCTED_MASK

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BerTlv) return false
        return tagBytes.contentEquals(other.tagBytes) &&
            value.contentEquals(other.value) &&
            children == other.children
    }

    override fun hashCode(): Int {
        var result = tagBytes.contentHashCode()
        result = 31 * result + value.contentHashCode()
        result = 31 * result + children.hashCode()
        return result
    }

    companion object {
        private const val CONSTRUCTED_MASK = 0x20
    }
}

object BerTlvParser {
    fun parseAll(bytes: ByteArray): List<BerTlv> {
        val nodes = mutableListOf<BerTlv>()
        var offset = 0
        while (offset < bytes.size) {
            val parsed = parseOne(bytes, offset)
            nodes += parsed.node
            offset = parsed.nextOffset
        }
        return nodes
    }

    private fun parseOne(bytes: ByteArray, offset: Int): ParsedTlv {
        require(offset < bytes.size) { "TLV offset is outside input" }
        val tagStart = offset
        var cursor = offset + 1
        val firstTagByte = bytes[tagStart].toInt() and 0xFF
        if (firstTagByte and MULTI_BYTE_TAG_MASK == MULTI_BYTE_TAG_MASK) {
            var tagByte: Int
            do {
                require(cursor < bytes.size) { "TLV tag is truncated" }
                tagByte = bytes[cursor].toInt() and 0xFF
                cursor += 1
            } while (tagByte and TAG_CONTINUATION_MASK == TAG_CONTINUATION_MASK)
        }
        val tagBytes = bytes.copyOfRange(tagStart, cursor)

        require(cursor < bytes.size) { "TLV length is missing" }
        val firstLengthByte = bytes[cursor].toInt() and 0xFF
        cursor += 1
        val length = if (firstLengthByte and LONG_FORM_LENGTH_MASK == 0) {
            firstLengthByte
        } else {
            val lengthByteCount = firstLengthByte and LENGTH_BYTE_COUNT_MASK
            require(lengthByteCount in 1..3) { "Unsupported TLV length form" }
            require(cursor + lengthByteCount <= bytes.size) { "TLV length is truncated" }
            var longLength = 0
            repeat(lengthByteCount) {
                longLength = (longLength shl 8) or (bytes[cursor].toInt() and 0xFF)
                cursor += 1
            }
            longLength
        }

        require(cursor + length <= bytes.size) { "TLV value is truncated" }
        val value = bytes.copyOfRange(cursor, cursor + length)
        val isConstructed = firstTagByte and CONSTRUCTED_MASK == CONSTRUCTED_MASK
        val children = if (isConstructed && value.isNotEmpty()) {
            runCatching { parseAll(value) }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        return ParsedTlv(
            node = BerTlv(tagBytes = tagBytes, value = value, children = children),
            nextOffset = cursor + length
        )
    }

    private data class ParsedTlv(
        val node: BerTlv,
        val nextOffset: Int
    )

    private const val MULTI_BYTE_TAG_MASK = 0x1F
    private const val TAG_CONTINUATION_MASK = 0x80
    private const val CONSTRUCTED_MASK = 0x20
    private const val LONG_FORM_LENGTH_MASK = 0x80
    private const val LENGTH_BYTE_COUNT_MASK = 0x7F
}

internal fun ByteArray.toHex(): String =
    joinToString(separator = "") { byte -> "%02X".format(byte) }
