package joyin.takgi.paysage.esim

data class EsimSgp22StoreDataBlock(
    val blockNumber: Int,
    val moreBlocks: Boolean,
    val data: ByteArray,
    val apdu: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EsimSgp22StoreDataBlock) return false
        return blockNumber == other.blockNumber &&
            moreBlocks == other.moreBlocks &&
            data.contentEquals(other.data) &&
            apdu.contentEquals(other.apdu)
    }

    override fun hashCode(): Int {
        var result = blockNumber
        result = 31 * result + moreBlocks.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + apdu.contentHashCode()
        return result
    }
}

data class EsimSgp22TlvNodeSummary(
    val tagHex: String,
    val length: Int,
    val childCount: Int,
    val privacy: EsimSgp22TlvPrivacy
)

enum class EsimSgp22TlvPrivacy {
    Structure,
    Sensitive,
    PotentiallySensitive,
    NonSensitive
}

data class EsimSgp22ReadOnlyResponseSummary(
    val statusWordHex: String,
    val accepted: Boolean,
    val followUp: EsimSgp22ApduFollowUp,
    val topLevelTags: List<String>,
    val nodeSummaries: List<EsimSgp22TlvNodeSummary>,
    val redactedTreeLines: List<String>,
    val parseWarning: String?,
    val displayText: String
)

enum class EsimSgp22ApduFollowUpKind {
    Complete,
    GetResponse,
    RetryWithLe,
    Failed
}

data class EsimSgp22ApduFollowUp(
    val kind: EsimSgp22ApduFollowUpKind,
    val statusWordHex: String,
    val message: String,
    val suggestedLe: Int? = null,
    val suggestedApdu: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EsimSgp22ApduFollowUp) return false
        return kind == other.kind &&
            statusWordHex == other.statusWordHex &&
            message == other.message &&
            suggestedLe == other.suggestedLe &&
            suggestedApdu.contentEqualsNullable(other.suggestedApdu)
    }

    override fun hashCode(): Int {
        var result = kind.hashCode()
        result = 31 * result + statusWordHex.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + (suggestedLe ?: 0)
        result = 31 * result + (suggestedApdu?.contentHashCode() ?: 0)
        return result
    }
}

object EsimSgp22StoreDataApdu {
    const val DEFAULT_MAX_DATA_BYTES = 223

    fun buildStoreDataBlocks(
        payload: ByteArray,
        maxDataBytes: Int = DEFAULT_MAX_DATA_BYTES,
        cla: Int = ES10X_CLA
    ): List<EsimSgp22StoreDataBlock> {
        require(payload.isNotEmpty()) { "ES10x STORE DATA payload must not be empty" }
        require(maxDataBytes in 1..MAX_SHORT_APDU_DATA_BYTES) {
            "maxDataBytes must fit in a short APDU"
        }

        val blocks = mutableListOf<EsimSgp22StoreDataBlock>()
        var offset = 0
        var blockNumber = 0
        while (offset < payload.size) {
            require(blockNumber <= 0xFF) { "ES10x STORE DATA block number overflow" }
            val end = minOf(offset + maxDataBytes, payload.size)
            val chunk = payload.copyOfRange(offset, end)
            val moreBlocks = end < payload.size
            val apdu = byteArrayOf(
                cla.toByte(),
                STORE_DATA_INS.toByte(),
                if (moreBlocks) P1_MORE_BLOCKS.toByte() else P1_LAST_BLOCK.toByte(),
                blockNumber.toByte(),
                chunk.size.toByte()
            ) + chunk + byteArrayOf(LE_EXTENDED_RESPONSE)

            blocks += EsimSgp22StoreDataBlock(
                blockNumber = blockNumber,
                moreBlocks = moreBlocks,
                data = chunk,
                apdu = apdu
            )
            offset = end
            blockNumber += 1
        }
        return blocks
    }

    fun buildGetResponse(length: Int): ByteArray {
        require(length in 0..MAX_SHORT_APDU_DATA_BYTES) { "GET RESPONSE length must fit in one byte" }
        return byteArrayOf(
            0x00,
            GET_RESPONSE_INS.toByte(),
            0x00,
            0x00,
            length.toByte()
        )
    }

    private const val ES10X_CLA = 0x80
    private const val STORE_DATA_INS = 0xE2
    private const val GET_RESPONSE_INS = 0xC0
    private const val P1_LAST_BLOCK = 0x11
    private const val P1_MORE_BLOCKS = 0x91
    private const val MAX_SHORT_APDU_DATA_BYTES = 0xFF
    private const val LE_EXTENDED_RESPONSE: Byte = 0x00
}

object EsimSgp22ApduFollowUpPlanner {
    fun evaluate(response: Iso7816Response): EsimSgp22ApduFollowUp {
        val statusWord = response.statusWordHex
        return when {
            response.isAccepted -> EsimSgp22ApduFollowUp(
                kind = EsimSgp22ApduFollowUpKind.Complete,
                statusWordHex = statusWord,
                message = if (response.isWarning) {
                    "卡片返回警告状态，当前响应可脱敏展示，但真机适配时应记录该状态。"
                } else {
                    "命令已完成。"
                }
            )
            response.sw1 == SW1_RESPONSE_AVAILABLE -> {
                val le = response.sw2
                EsimSgp22ApduFollowUp(
                    kind = EsimSgp22ApduFollowUpKind.GetResponse,
                    statusWordHex = statusWord,
                    message = if (le == 0) {
                        "卡片提示还有响应数据，建议发送 GET RESPONSE，Le=00 表示短 APDU 最大长度。"
                    } else {
                        "卡片提示还有 $le 字节响应数据，建议发送 GET RESPONSE。"
                    },
                    suggestedLe = le,
                    suggestedApdu = EsimSgp22StoreDataApdu.buildGetResponse(le)
                )
            }
            response.sw1 == SW1_WRONG_LENGTH -> {
                val le = response.sw2
                EsimSgp22ApduFollowUp(
                    kind = EsimSgp22ApduFollowUpKind.RetryWithLe,
                    statusWordHex = statusWord,
                    message = if (le == 0) {
                        "卡片提示 Le 不正确，建议使用 Le=00 重新发送上一条命令。"
                    } else {
                        "卡片提示 Le 不正确，建议使用 Le=$le 重新发送上一条命令。"
                    },
                    suggestedLe = le
                )
            }
            else -> EsimSgp22ApduFollowUp(
                kind = EsimSgp22ApduFollowUpKind.Failed,
                statusWordHex = statusWord,
                message = "命令未成功，当前不继续发送后续 APDU。"
            )
        }
    }

    private const val SW1_RESPONSE_AVAILABLE = 0x61
    private const val SW1_WRONG_LENGTH = 0x6C
}

object EsimSgp22ReadOnlyResponseSummarizer {
    fun summarize(response: Iso7816Response): EsimSgp22ReadOnlyResponseSummary {
        val followUp = EsimSgp22ApduFollowUpPlanner.evaluate(response)
        val parsedNodes = runCatching {
            if (response.data.isEmpty()) emptyList() else BerTlvParser.parseAll(response.data)
        }
        val nodes = parsedNodes.getOrDefault(emptyList())
        val flatNodes = nodes.flatMap { it.walk() }
        val nodeSummaries = flatNodes.map { node ->
            EsimSgp22TlvNodeSummary(
                tagHex = node.tagHex,
                length = node.value.size,
                childCount = node.children.size,
                privacy = EsimSgp22TlvPrivacyClassifier.classify(node)
            )
        }
        val redactedTreeLines = EsimSgp22RedactedTlvTreeFormatter.format(nodes)
        val sensitiveNodeCount = nodeSummaries.count {
            it.privacy == EsimSgp22TlvPrivacy.Sensitive ||
                it.privacy == EsimSgp22TlvPrivacy.PotentiallySensitive
        }
        val topLevelTags = nodes.map { it.tagHex }
        val parseWarning = parsedNodes.exceptionOrNull()?.message
        val displayText = buildString {
            append(if (response.isAccepted) "只读响应已接收" else "只读响应未成功")
            append("，SW=${response.statusWordHex}")
            append("，下一步=")
            append(followUp.message)
            if (topLevelTags.isNotEmpty()) {
                append("，顶层标签=")
                append(topLevelTags.joinToString())
            }
            append("。已脱敏: 仅保留 TLV 标签、长度、层级和隐私分类")
            if (sensitiveNodeCount > 0) {
                append("，其中 ")
                append(sensitiveNodeCount)
                append(" 个节点按敏感或可能敏感处理")
            }
            append("，不展示 ICCID/EID/IMSI/手机号或原始值。")
            if (parseWarning != null) {
                append(" 解析提示: ")
                append(parseWarning)
            }
        }
        return EsimSgp22ReadOnlyResponseSummary(
            statusWordHex = response.statusWordHex,
            accepted = response.isAccepted,
            followUp = followUp,
            topLevelTags = topLevelTags,
            nodeSummaries = nodeSummaries,
            redactedTreeLines = redactedTreeLines,
            parseWarning = parseWarning,
            displayText = displayText
        )
    }

    private fun BerTlv.walk(): List<BerTlv> =
        listOf(this) + children.flatMap { it.walk() }
}

object EsimSgp22TlvPrivacyClassifier {
    fun classify(node: BerTlv): EsimSgp22TlvPrivacy = when {
        node.isConstructed -> EsimSgp22TlvPrivacy.Structure
        node.tagHex in SENSITIVE_TAGS -> EsimSgp22TlvPrivacy.Sensitive
        node.value.size >= POTENTIALLY_SENSITIVE_LENGTH -> EsimSgp22TlvPrivacy.PotentiallySensitive
        else -> EsimSgp22TlvPrivacy.NonSensitive
    }

    private val SENSITIVE_TAGS = setOf(
        "4F",
        "84",
        "5A",
        "5F20",
        "5F24",
        "5F25",
        "5F28",
        "5F2D",
        "5F34",
        "9F06",
        "9F70",
        "9F7F",
        "BF76"
    )
    private const val POTENTIALLY_SENSITIVE_LENGTH = 4
}

object EsimSgp22RedactedTlvTreeFormatter {
    fun format(
        nodes: List<BerTlv>,
        maxNodes: Int = DEFAULT_MAX_NODES,
        maxDepth: Int = DEFAULT_MAX_DEPTH
    ): List<String> {
        require(maxNodes > 0) { "maxNodes must be positive" }
        require(maxDepth >= 0) { "maxDepth must not be negative" }
        val lines = mutableListOf<String>()
        var emitted = 0

        fun visit(node: BerTlv, depth: Int) {
            if (emitted >= maxNodes) return
            val prefix = "  ".repeat(depth)
            val privacy = EsimSgp22TlvPrivacyClassifier.classify(node)
            lines += "${prefix}${node.tagHex} len=${node.value.size} children=${node.children.size} privacy=${privacy.label()}"
            emitted += 1
            if (depth >= maxDepth) {
                if (node.children.isNotEmpty() && emitted < maxNodes) {
                    lines += "${prefix}  ... depth limit"
                    emitted += 1
                }
                return
            }
            node.children.forEach { child -> visit(child, depth + 1) }
        }

        nodes.forEach { node -> visit(node, depth = 0) }
        if (emitted >= maxNodes && nodes.flatMap { it.walk() }.size > emitted) {
            lines += "... node limit"
        }
        return lines
    }

    private fun BerTlv.walk(): List<BerTlv> =
        listOf(this) + children.flatMap { it.walk() }

    private fun EsimSgp22TlvPrivacy.label(): String = when (this) {
        EsimSgp22TlvPrivacy.Structure -> "结构"
        EsimSgp22TlvPrivacy.Sensitive -> "敏感"
        EsimSgp22TlvPrivacy.PotentiallySensitive -> "可能敏感"
        EsimSgp22TlvPrivacy.NonSensitive -> "普通"
    }

    private const val DEFAULT_MAX_NODES = 40
    private const val DEFAULT_MAX_DEPTH = 6
}

private fun ByteArray?.contentEqualsNullable(other: ByteArray?): Boolean = when {
    this == null && other == null -> true
    this == null || other == null -> false
    else -> contentEquals(other)
}
