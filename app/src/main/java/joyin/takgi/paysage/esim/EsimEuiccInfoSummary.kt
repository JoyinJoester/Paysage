package joyin.takgi.paysage.esim

import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

data class EsimEuiccInfoSummary(
    val available: Boolean,
    val message: String,
    val osVersion: String?,
    val memory: EsimEuiccMemorySummary,
    val ports: List<EsimEuiccPortSummary>,
    val source: String = "Android EuiccManager",
    val sensitiveIdentifierPolicy: String = EsimEuiccInfoFormatter.SENSITIVE_IDENTIFIER_POLICY
)

data class EsimEuiccMemorySummary(
    val bytes: Long?,
    val displayText: String,
    val message: String
)

data class EsimEuiccPortSummary(
    val portIndex: Int,
    val availability: EsimEuiccPortAvailability,
    val message: String
)

enum class EsimEuiccPortAvailability {
    Available,
    Unavailable,
    Unknown
}

object EsimEuiccInfoFormatter {
    const val SENSITIVE_IDENTIFIER_POLICY: String = "摘要不读取 EID、ICCID、IMSI 或手机号。EID 需在支持工具中单独点击读取。"

    fun memory(bytes: Long?): EsimEuiccMemorySummary = when {
        bytes == null -> unavailableMemory("当前系统版本未提供 eUICC 可用空间接口。")
        bytes < 0L -> unavailableMemory("系统未公开 eUICC 可用空间。")
        else -> EsimEuiccMemorySummary(
            bytes = bytes,
            displayText = formatBytes(bytes),
            message = "系统返回 eUICC 可用空间。"
        )
    }

    fun unavailableMemory(message: String): EsimEuiccMemorySummary =
        EsimEuiccMemorySummary(
            bytes = null,
            displayText = "未公开",
            message = message
        )

    fun portMessage(
        portIndex: Int,
        availability: EsimEuiccPortAvailability
    ): String = when (availability) {
        EsimEuiccPortAvailability.Available -> "端口 ${portIndex + 1} 可用于下载或切换。"
        EsimEuiccPortAvailability.Unavailable -> "端口 ${portIndex + 1} 当前不可用或已被占用。"
        EsimEuiccPortAvailability.Unknown -> "端口 ${portIndex + 1} 状态未公开。"
    }

    fun switchActionLabel(portIndex: Int?): String =
        portIndex?.let { "切换到端口 ${it + 1}" } ?: "切换"

    fun selectedSwitchPortLabel(portIndex: Int?): String =
        portIndex?.let { "端口 ${it + 1}" } ?: "系统自动"

    fun subscriptionCardLabel(cardId: Int?): String =
        cardId?.takeIf { it >= 0 }?.let { "eUICC 卡 $it" } ?: "eUICC 卡未公开"

    fun subscriptionPortLabel(portIndex: Int?): String =
        portIndex?.takeIf { it >= 0 }?.let { "当前端口 ${it + 1}" } ?: "当前端口未公开"

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"

        var value = bytes.toDouble()
        var unitIndex = 0
        val units = listOf("B", "KB", "MB", "GB")
        while (value >= 1024.0 && unitIndex < units.lastIndex) {
            value /= 1024.0
            unitIndex += 1
        }

        val rounded = round(value)
        val pattern = if (abs(value - rounded) < 0.05) {
            "%.0f %s"
        } else {
            "%.1f %s"
        }
        return String.format(Locale.US, pattern, value, units[unitIndex])
    }
}
