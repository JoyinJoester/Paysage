package joyin.takgi.paysage.esim

data class EsimSgp22SampleAnalysis(
    val success: Boolean,
    val title: String,
    val message: String,
    val statusWord: String?,
    val redactedLines: List<String>,
    val classification: EsimSgp22SampleClassification? = null,
    val commandContext: EsimSgp22SampleContext = EsimSgp22SampleContext.Unknown,
    val privacyAudit: EsimSgp22SamplePrivacyAudit? = null,
    val redactedReport: String? = null
)

data class EsimSgp22SamplePrivacyAudit(
    val totalNodeCount: Int,
    val structureNodeCount: Int,
    val sensitiveNodeCount: Int,
    val potentiallySensitiveNodeCount: Int,
    val nonSensitiveNodeCount: Int,
    val hasParseWarning: Boolean,
    val summary: String,
    val shareGuidance: String
)

data class EsimSgp22SampleClassification(
    val kind: EsimSgp22SampleKind,
    val confidence: EsimSgp22SampleConfidence,
    val title: String,
    val reason: String,
    val recommendation: String
)

enum class EsimSgp22SampleKind(val label: String) {
    StatusOnly("仅状态字"),
    SelectFci("SELECT/FCI 响应"),
    ProfileMetadataCandidate("profile 元数据候选"),
    NotificationCandidate("通知/事件候选"),
    GenericReadOnlyTlv("通用只读 TLV"),
    ParseWarning("TLV 解析异常")
}

enum class EsimSgp22SampleConfidence(val label: String) {
    High("高"),
    Medium("中"),
    Low("低")
}

enum class EsimSgp22SampleContext(val label: String) {
    Unknown("自动判断"),
    SelectIsdR("SELECT ISD-R"),
    ProfileReadOnly("profile 只读"),
    NotificationReadOnly("notification 只读"),
    GenericReadOnly("通用只读")
}

object EsimSgp22SampleAnalyzer {
    fun analyze(
        input: String,
        commandContext: EsimSgp22SampleContext = EsimSgp22SampleContext.Unknown
    ): EsimSgp22SampleAnalysis {
        val normalized = input
            .replace("0x", "", ignoreCase = true)
            .filterNot { it.isWhitespace() || it == ':' || it == '-' || it == '_' }
            .uppercase()

        if (normalized.isBlank()) {
            return EsimSgp22SampleAnalysis(
                success = false,
                title = "样本为空",
                message = "请粘贴包含状态字的 APDU 响应十六进制文本。",
                statusWord = null,
                redactedLines = emptyList(),
                commandContext = commandContext
            )
        }
        if (normalized.length % 2 != 0) {
            return EsimSgp22SampleAnalysis(
                success = false,
                title = "十六进制长度不完整",
                message = "样本必须由完整字节组成，每个字节 2 个十六进制字符。",
                statusWord = null,
                redactedLines = emptyList(),
                commandContext = commandContext
            )
        }
        if (!HEX_REGEX.matches(normalized)) {
            return EsimSgp22SampleAnalysis(
                success = false,
                title = "包含非十六进制字符",
                message = "请只保留 0-9、A-F、空格、冒号或短横线。",
                statusWord = null,
                redactedLines = emptyList(),
                commandContext = commandContext
            )
        }

        val bytes = normalized.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
        val response = runCatching { EsimApdu.parseResponse(bytes) }.getOrElse { error ->
            return EsimSgp22SampleAnalysis(
                success = false,
                title = "缺少状态字",
                message = error.message ?: "APDU 响应至少需要 SW1 和 SW2。",
                statusWord = null,
                redactedLines = emptyList(),
                commandContext = commandContext
            )
        }
        val summary = EsimSgp22ReadOnlyResponseSummarizer.summarize(response)
        val redactedLines = summary.redactedTreeLines.ifEmpty {
            listOf("无响应数据，仅包含状态字。")
        }
        val classification = EsimSgp22SampleClassifier.classify(summary, commandContext)
        val privacyAudit = EsimSgp22SamplePrivacyAuditor.audit(summary)
        return EsimSgp22SampleAnalysis(
            success = true,
            title = if (summary.accepted) "样本已脱敏解析" else "样本已解析，状态未成功",
            message = summary.displayText,
            statusWord = summary.statusWordHex,
            redactedLines = redactedLines,
            classification = classification,
            commandContext = commandContext,
            privacyAudit = privacyAudit,
            redactedReport = EsimSgp22SampleReportFormatter.format(
                summary = summary,
                classification = classification,
                commandContext = commandContext,
                privacyAudit = privacyAudit,
                redactedLines = redactedLines
            )
        )
    }

    private val HEX_REGEX = Regex("^[0-9A-F]+$")
}

object EsimSgp22SampleReportFormatter {
    fun format(
        summary: EsimSgp22ReadOnlyResponseSummary,
        classification: EsimSgp22SampleClassification,
        commandContext: EsimSgp22SampleContext,
        privacyAudit: EsimSgp22SamplePrivacyAudit,
        redactedLines: List<String>
    ): String = buildString {
        appendLine("Paysage SGP.22 脱敏样本分析")
        appendLine("隐私: 不包含原始 APDU、TLV value、ICCID、EID、IMSI 或手机号。")
        appendLine("命令上下文: ${commandContext.label}")
        appendLine("SW: ${summary.statusWordHex}")
        appendLine("状态: ${if (summary.accepted) "已接收" else "未成功"}")
        appendLine("下一步: ${summary.followUp.message}")
        appendLine("样本类型: ${classification.title}")
        appendLine("类型标签: ${classification.kind.label}")
        appendLine("置信度: ${classification.confidence.label}")
        appendLine("依据: ${classification.reason}")
        appendLine("建议: ${classification.recommendation}")
        appendLine("隐私审计: ${privacyAudit.summary}")
        appendLine("外发建议: ${privacyAudit.shareGuidance}")
        appendLine(
            "节点计数: total=${privacyAudit.totalNodeCount}, structure=${privacyAudit.structureNodeCount}, " +
                "sensitive=${privacyAudit.sensitiveNodeCount}, potentiallySensitive=${privacyAudit.potentiallySensitiveNodeCount}, " +
                "nonSensitive=${privacyAudit.nonSensitiveNodeCount}"
        )
        appendLine("脱敏 TLV 树:")
        redactedLines.forEach { line ->
            appendLine("- $line")
        }
    }.trimEnd()
}

object EsimSgp22SamplePrivacyAuditor {
    fun audit(summary: EsimSgp22ReadOnlyResponseSummary): EsimSgp22SamplePrivacyAudit {
        val structureCount = summary.nodeSummaries.count { it.privacy == EsimSgp22TlvPrivacy.Structure }
        val sensitiveCount = summary.nodeSummaries.count { it.privacy == EsimSgp22TlvPrivacy.Sensitive }
        val potentiallySensitiveCount = summary.nodeSummaries.count {
            it.privacy == EsimSgp22TlvPrivacy.PotentiallySensitive
        }
        val nonSensitiveCount = summary.nodeSummaries.count { it.privacy == EsimSgp22TlvPrivacy.NonSensitive }
        val totalCount = summary.nodeSummaries.size
        val protectedCount = sensitiveCount + potentiallySensitiveCount
        val hasParseWarning = summary.parseWarning != null
        val summaryText = when {
            hasParseWarning -> "TLV 解析存在异常，已停止展示原始 value，仅保留可解析结构和状态字。"
            totalCount == 0 -> "无 TLV 数据域，仅包含状态字；没有可展示的 value。"
            protectedCount > 0 -> "共 $totalCount 个 TLV 节点，其中 $protectedCount 个按敏感或可能敏感处理，均未展示 value。"
            else -> "共 $totalCount 个 TLV 节点，未命中已知敏感 tag；仍只展示 tag、长度、层级和隐私分类。"
        }
        val guidance = when {
            hasParseWarning -> "可用于排查格式问题；外发前建议确认样本是否完整，避免误判业务类型。"
            protectedCount > 0 -> "可外发脱敏片段；不要附带原始 APDU、截图中的完整 value 或运营商原始样本。"
            totalCount == 0 -> "可外发状态字和下一步建议；如果需要业务解释，应补充命令上下文。"
            else -> "可外发脱敏片段；若后续发现新敏感 tag，应补充隐私分类规则。"
        }
        return EsimSgp22SamplePrivacyAudit(
            totalNodeCount = totalCount,
            structureNodeCount = structureCount,
            sensitiveNodeCount = sensitiveCount,
            potentiallySensitiveNodeCount = potentiallySensitiveCount,
            nonSensitiveNodeCount = nonSensitiveCount,
            hasParseWarning = hasParseWarning,
            summary = summaryText,
            shareGuidance = guidance
        )
    }
}

object EsimSgp22SampleClassifier {
    fun classify(
        summary: EsimSgp22ReadOnlyResponseSummary,
        commandContext: EsimSgp22SampleContext = EsimSgp22SampleContext.Unknown
    ): EsimSgp22SampleClassification {
        val tags = summary.nodeSummaries.map { it.tagHex }.toSet()
        val topLevelTags = summary.topLevelTags.toSet()
        val constructedCount = summary.nodeSummaries.count { it.childCount > 0 }
        val sensitiveCount = summary.nodeSummaries.count {
            it.privacy == EsimSgp22TlvPrivacy.Sensitive ||
                it.privacy == EsimSgp22TlvPrivacy.PotentiallySensitive
        }

        return when {
            summary.parseWarning != null -> EsimSgp22SampleClassification(
                kind = EsimSgp22SampleKind.ParseWarning,
                confidence = EsimSgp22SampleConfidence.High,
                title = "TLV 解析异常样本",
                reason = "响应数据无法完整按 BER-TLV 解析: ${summary.parseWarning}",
                recommendation = "优先核对样本是否包含完整响应数据和 SW1/SW2，再用状态字判断下一步。"
            )
            summary.nodeSummaries.isEmpty() -> EsimSgp22SampleClassification(
                kind = EsimSgp22SampleKind.StatusOnly,
                confidence = EsimSgp22SampleConfidence.High,
                title = "仅状态字样本",
                reason = "响应不包含数据域，只能判断 SW=${summary.statusWordHex} 和下一步动作。",
                recommendation = "适合排查命令是否完成、是否需要 GET RESPONSE 或是否需要调整 Le。"
            )
            commandContext == EsimSgp22SampleContext.SelectIsdR -> classifySelectContext(topLevelTags, tags)
            commandContext == EsimSgp22SampleContext.ProfileReadOnly -> classifyProfileContext(tags)
            commandContext == EsimSgp22SampleContext.NotificationReadOnly -> classifyNotificationContext(
                constructedCount = constructedCount,
                sensitiveCount = sensitiveCount
            )
            commandContext == EsimSgp22SampleContext.GenericReadOnly -> EsimSgp22SampleClassification(
                kind = EsimSgp22SampleKind.GenericReadOnlyTlv,
                confidence = EsimSgp22SampleConfidence.Medium,
                title = "通用只读 TLV",
                reason = "用户声明该样本来自通用只读命令；当前仅按脱敏 TLV 结构审阅。",
                recommendation = "适合做结构和状态字审阅；如需业务解释，应补充具体 ES10x 命令名称。"
            )
            topLevelTags.contains("6F") && (tags.contains("84") || tags.contains("A5")) ->
                EsimSgp22SampleClassification(
                    kind = EsimSgp22SampleKind.SelectFci,
                    confidence = EsimSgp22SampleConfidence.High,
                    title = "SELECT/FCI 响应",
                    reason = "顶层存在 FCI Template(6F)，并包含 DF Name(84) 或 FCI Proprietary Template(A5)。",
                    recommendation = "适合判断 ISD-R 通道是否打开；不要把它当作 profile 列表或通知队列。"
                )
            tags.any { it in PROFILE_HINT_TAGS } -> EsimSgp22SampleClassification(
                kind = EsimSgp22SampleKind.ProfileMetadataCandidate,
                confidence = EsimSgp22SampleConfidence.Medium,
                title = "profile 元数据候选",
                reason = "样本包含常见标识或 profile 元数据相关 tag，已按敏感或可能敏感处理。",
                recommendation = "后续应结合实际 ES10x 命令名和真机样本校验；当前只用于脱敏审阅，不执行生命周期操作。"
            )
            constructedCount > 0 && sensitiveCount == 0 -> EsimSgp22SampleClassification(
                kind = EsimSgp22SampleKind.NotificationCandidate,
                confidence = EsimSgp22SampleConfidence.Low,
                title = "通知/事件候选",
                reason = "样本包含构造型 TLV，但没有明显 profile 标识 tag；在缺少命令上下文时只能低置信度初筛。",
                recommendation = "需要结合触发该响应的只读命令确认是否来自 notification queue。"
            )
            else -> EsimSgp22SampleClassification(
                kind = EsimSgp22SampleKind.GenericReadOnlyTlv,
                confidence = EsimSgp22SampleConfidence.Low,
                title = "通用只读 TLV",
                reason = "样本可脱敏解析，但当前 tag 组合不足以判断具体 SGP.22 业务类型。",
                recommendation = "可先用于结构审阅；如要进入 profile/notification 解析，应补充命令上下文和真机样本。"
            )
        }
    }

    private fun classifySelectContext(
        topLevelTags: Set<String>,
        tags: Set<String>
    ): EsimSgp22SampleClassification {
        val hasFci = topLevelTags.contains("6F") && (tags.contains("84") || tags.contains("A5"))
        return if (hasFci) {
            EsimSgp22SampleClassification(
                kind = EsimSgp22SampleKind.SelectFci,
                confidence = EsimSgp22SampleConfidence.High,
                title = "SELECT/FCI 响应",
                reason = "用户声明命令上下文为 SELECT ISD-R，且样本包含 FCI Template(6F) 与 84/A5。",
                recommendation = "可用于确认 ISD-R 通道和 FCI 结构；仍不包含 profile 列表或通知队列。"
            )
        } else {
            EsimSgp22SampleClassification(
                kind = EsimSgp22SampleKind.GenericReadOnlyTlv,
                confidence = EsimSgp22SampleConfidence.Low,
                title = "SELECT 上下文异常样本",
                reason = "用户声明命令上下文为 SELECT ISD-R，但样本缺少典型 FCI Template(6F) 或 84/A5。",
                recommendation = "请核对是否粘贴了完整 SELECT 响应，或该安全元素是否返回非标准 FCI。"
            )
        }
    }

    private fun classifyProfileContext(tags: Set<String>): EsimSgp22SampleClassification {
        val hasProfileHints = tags.any { it in PROFILE_HINT_TAGS }
        return EsimSgp22SampleClassification(
            kind = EsimSgp22SampleKind.ProfileMetadataCandidate,
            confidence = if (hasProfileHints) EsimSgp22SampleConfidence.High else EsimSgp22SampleConfidence.Medium,
            title = "profile 只读样本候选",
            reason = if (hasProfileHints) {
                "用户声明命令上下文为 profile 只读，且样本包含常见 profile 标识或元数据 tag。"
            } else {
                "用户声明命令上下文为 profile 只读，但当前 tag 组合缺少典型 profile 标识。"
            },
            recommendation = "仅用于脱敏审阅和样本归档；启用、停用、删除、写入等生命周期操作仍保持阻断。"
        )
    }

    private fun classifyNotificationContext(
        constructedCount: Int,
        sensitiveCount: Int
    ): EsimSgp22SampleClassification {
        val confidence = if (constructedCount > 0 && sensitiveCount == 0) {
            EsimSgp22SampleConfidence.Medium
        } else {
            EsimSgp22SampleConfidence.Low
        }
        return EsimSgp22SampleClassification(
            kind = EsimSgp22SampleKind.NotificationCandidate,
            confidence = confidence,
            title = "notification 只读样本候选",
            reason = if (confidence == EsimSgp22SampleConfidence.Medium) {
                "用户声明命令上下文为 notification 只读，样本包含构造型 TLV，且未发现明显 profile 标识。"
            } else {
                "用户声明命令上下文为 notification 只读，但样本结构不足以形成高置信判断。"
            },
            recommendation = "需要结合实际 notification queue 命令和真机回放结果确认；当前不读取或确认 eUICC 内部通知。"
        )
    }

    private val PROFILE_HINT_TAGS = setOf(
        "5A",
        "5F20",
        "5F24",
        "5F25",
        "5F28",
        "5F2D",
        "5F34",
        "9F70",
        "9F7F",
        "BF76"
    )
}
