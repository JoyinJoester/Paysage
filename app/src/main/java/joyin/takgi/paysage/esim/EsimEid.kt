package joyin.takgi.paysage.esim

enum class EsimEidReadStatus {
    Succeeded,
    Unavailable,
    PermissionDenied,
    Failed
}

data class EsimEidReadResult(
    val status: EsimEidReadStatus,
    val eid: String?,
    val message: String
) {
    val isSuccess: Boolean
        get() = status == EsimEidReadStatus.Succeeded && !eid.isNullOrBlank()
}

object EsimEidFormatter {
    const val PRIVACY_NOTICE: String = "默认不读取、不保存；仅在你点击后向系统请求 EID。"

    fun normalize(raw: String): String =
        raw.filter { it.isLetterOrDigit() }.uppercase()

    fun grouped(raw: String): String {
        val normalized = normalize(raw)
        return normalized.chunked(GROUP_SIZE).joinToString(" ")
    }

    fun masked(raw: String): String {
        val normalized = normalize(raw)
        if (normalized.length <= VISIBLE_EDGE_COUNT * 2) return grouped(normalized)
        val head = normalized.take(VISIBLE_EDGE_COUNT)
        val tail = normalized.takeLast(VISIBLE_EDGE_COUNT)
        return "$head ${MASK} $tail"
    }

    private const val GROUP_SIZE = 4
    private const val VISIBLE_EDGE_COUNT = 4
    private const val MASK = "•••• ••••"
}
