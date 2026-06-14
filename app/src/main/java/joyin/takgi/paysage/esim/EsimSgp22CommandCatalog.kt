package joyin.takgi.paysage.esim

enum class EsimSgp22CommandRisk {
    DiagnosticReadOnly,
    ProfileReadOnly,
    Mutating,
    Destructive
}

enum class EsimSgp22CommandStatus {
    Available,
    Planned,
    Blocked
}

data class EsimSgp22CommandSpec(
    val id: String,
    val title: String,
    val description: String,
    val risk: EsimSgp22CommandRisk,
    val status: EsimSgp22CommandStatus,
    val requiresIsdR: Boolean,
    val requiresSecureChannel: Boolean,
    val note: String
)

data class EsimSgp22SafetyContext(
    val isdRSelected: Boolean,
    val allowExperimentalReadOnly: Boolean,
    val privilegedOrAuthorized: Boolean,
    val userConfirmedSensitiveAction: Boolean
)

data class EsimSgp22SafetyDecision(
    val allowed: Boolean,
    val message: String
)

object EsimSgp22CommandCatalog {
    val commands: List<EsimSgp22CommandSpec> = listOf(
        EsimSgp22CommandSpec(
            id = "select_isd_r",
            title = "SELECT ISD-R",
            description = "选择 eUICC ISD-R 应用并读取 FCI 摘要。",
            risk = EsimSgp22CommandRisk.DiagnosticReadOnly,
            status = EsimSgp22CommandStatus.Available,
            requiresIsdR = false,
            requiresSecureChannel = false,
            note = "当前 USB/OMAPI 高级诊断已实现。"
        ),
        EsimSgp22CommandSpec(
            id = "get_euicc_info",
            title = "读取 eUICC 信息",
            description = "读取系统公开的 eUICC OS、可用空间与端口能力摘要。",
            risk = EsimSgp22CommandRisk.DiagnosticReadOnly,
            status = EsimSgp22CommandStatus.Available,
            requiresIsdR = false,
            requiresSecureChannel = false,
            note = "当前使用 Android 公开 EuiccManager 信息接口，不读取 EID、ICCID、IMSI。"
        ),
        EsimSgp22CommandSpec(
            id = "read_only_apdu_scaffold",
            title = "只读 APDU 框架",
            description = "提供 ES10x STORE DATA 分段、GET RESPONSE 和脱敏 TLV 响应摘要。",
            risk = EsimSgp22CommandRisk.DiagnosticReadOnly,
            status = EsimSgp22CommandStatus.Available,
            requiresIsdR = true,
            requiresSecureChannel = false,
            note = "当前仅作为 profile/通知只读能力的协议地基，不在界面执行敏感 profile 读取。"
        ),
        EsimSgp22CommandSpec(
            id = "list_profiles",
            title = "读取 profile 列表",
            description = "读取 eUICC 内 profile 元数据，包括禁用 profile。",
            risk = EsimSgp22CommandRisk.ProfileReadOnly,
            status = EsimSgp22CommandStatus.Planned,
            requiresIsdR = true,
            requiresSecureChannel = false,
            note = "APDU 分段和脱敏摘要基础已完成；真机执行前仍需完成授权、样本解析和展示策略。"
        ),
        EsimSgp22CommandSpec(
            id = "list_notifications",
            title = "读取通知队列",
            description = "读取 eUICC 待处理通知，用于后续通知确认流程。",
            risk = EsimSgp22CommandRisk.ProfileReadOnly,
            status = EsimSgp22CommandStatus.Planned,
            requiresIsdR = true,
            requiresSecureChannel = false,
            note = "APDU 分段和脱敏摘要基础已完成；确认/删除通知需要额外授权边界。"
        ),
        EsimSgp22CommandSpec(
            id = "enable_disable_profile",
            title = "启用/停用 profile",
            description = "改变 eUICC profile 启用状态。",
            risk = EsimSgp22CommandRisk.Mutating,
            status = EsimSgp22CommandStatus.Blocked,
            requiresIsdR = true,
            requiresSecureChannel = true,
            note = "普通应用不应绕过系统 LPA、运营商和设备策略直接执行。"
        ),
        EsimSgp22CommandSpec(
            id = "delete_profile",
            title = "删除 profile",
            description = "从 eUICC 删除指定 profile。",
            risk = EsimSgp22CommandRisk.Destructive,
            status = EsimSgp22CommandStatus.Blocked,
            requiresIsdR = true,
            requiresSecureChannel = true,
            note = "破坏性操作必须走系统确认或厂商授权环境。"
        )
    )
}

object EsimSgp22SafetyPolicy {
    fun evaluate(
        command: EsimSgp22CommandSpec,
        context: EsimSgp22SafetyContext
    ): EsimSgp22SafetyDecision {
        if (command.status == EsimSgp22CommandStatus.Blocked) {
            return EsimSgp22SafetyDecision(false, command.note)
        }
        if (command.requiresIsdR && !context.isdRSelected) {
            return EsimSgp22SafetyDecision(false, "需要先完成 ISD-R 通道诊断。")
        }
        return when (command.risk) {
            EsimSgp22CommandRisk.DiagnosticReadOnly -> EsimSgp22SafetyDecision(true, "允许执行只读诊断。")
            EsimSgp22CommandRisk.ProfileReadOnly -> {
                if (command.status != EsimSgp22CommandStatus.Available) {
                    EsimSgp22SafetyDecision(false, "命令尚未实现，当前只展示规划状态。")
                } else if (!context.allowExperimentalReadOnly) {
                    EsimSgp22SafetyDecision(false, "需要用户开启实验性只读 profile 诊断。")
                } else {
                    EsimSgp22SafetyDecision(true, "允许执行实验性只读 profile 诊断。")
                }
            }
            EsimSgp22CommandRisk.Mutating,
            EsimSgp22CommandRisk.Destructive -> {
                if (context.privilegedOrAuthorized && context.userConfirmedSensitiveAction) {
                    EsimSgp22SafetyDecision(true, "授权环境中可交给受控流程执行。")
                } else {
                    EsimSgp22SafetyDecision(false, "需要系统/厂商授权和用户明确确认。")
                }
            }
        }
    }
}
