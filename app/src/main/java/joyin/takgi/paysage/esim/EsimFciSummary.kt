package joyin.takgi.paysage.esim

data class EsimFciSummary(
    val topLevelTags: List<String>,
    val allTags: List<String>,
    val hasFciTemplate: Boolean,
    val hasProprietaryTemplate: Boolean,
    val dedicatedFileNameMatchesIsdR: Boolean?,
    val redactedTreeLines: List<String>
) {
    val displayText: String
        get() {
            val parts = mutableListOf<String>()
            if (topLevelTags.isNotEmpty()) {
                parts += "FCI ${topLevelTags.joinToString("/")}"
            }
            dedicatedFileNameMatchesIsdR?.let { matches ->
                parts += if (matches) "AID 匹配 ISD-R" else "AID 非 ISD-R"
            }
            if (hasProprietaryTemplate) {
                parts += "含 A5 参数"
            }
            return parts.joinToString(" / ").ifBlank { "FCI 已解析" }
        }

    val diagnosticText: String
        get() = listOfNotNull(
            displayText,
            redactedTreeLines.takeIf { it.isNotEmpty() }?.joinToString(
                separator = "; ",
                prefix = "TLV 树: "
            )
        ).joinToString(" / ")
}

object EsimFciAnalyzer {
    fun summarize(selectResponseData: ByteArray): EsimFciSummary? {
        if (selectResponseData.isEmpty()) return null
        val tlvs = runCatching { BerTlvParser.parseAll(selectResponseData) }.getOrNull()
            ?: return null
        val flattened = tlvs.flatMap { it.walk() }
        val dedicatedFileName = flattened.firstOrNull { it.tagHex == TAG_DEDICATED_FILE_NAME }
        return EsimFciSummary(
            topLevelTags = tlvs.map { it.tagHex },
            allTags = flattened.map { it.tagHex }.distinct(),
            hasFciTemplate = tlvs.any { it.tagHex == TAG_FCI_TEMPLATE },
            hasProprietaryTemplate = flattened.any { it.tagHex == TAG_PROPRIETARY_TEMPLATE },
            dedicatedFileNameMatchesIsdR = dedicatedFileName?.value?.contentEquals(EsimApdu.ISD_R_AID),
            redactedTreeLines = EsimSgp22RedactedTlvTreeFormatter.format(
                nodes = tlvs,
                maxNodes = MAX_FCI_TREE_NODES,
                maxDepth = MAX_FCI_TREE_DEPTH
            )
        )
    }

    private fun BerTlv.walk(): List<BerTlv> =
        listOf(this) + children.flatMap { it.walk() }

    private const val TAG_FCI_TEMPLATE = "6F"
    private const val TAG_DEDICATED_FILE_NAME = "84"
    private const val TAG_PROPRIETARY_TEMPLATE = "A5"
    private const val MAX_FCI_TREE_NODES = 12
    private const val MAX_FCI_TREE_DEPTH = 4
}
