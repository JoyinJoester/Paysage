package joyin.takgi.paysage.esim

import android.content.Context
import joyin.takgi.paysage.R
import net.typeblog.lpac_jni.LocalProfileInfo
import net.typeblog.lpac_jni.LocalProfileNotification
import net.typeblog.lpac_jni.ProfileClass
import net.typeblog.lpac_jni.EuiccInfo2
import net.typeblog.lpac_jni.Version

enum class EsimExternalProfileSourceKind {
    UsbCcid,
    Omapi
}

data class EsimExternalProfileSource(
    val kind: EsimExternalProfileSourceKind,
    val key: String,
    val label: String,
    val index: Int,
    val isdrAidHex: String = EsimApdu.ISD_R_AID_HEX,
    val logicalSlotIndex: Int? = null,
    val usbInterfaceId: Int? = null
) {
    val commandPrefix: String
        get() = when (kind) {
            EsimExternalProfileSourceKind.UsbCcid -> "usb"
            EsimExternalProfileSourceKind.Omapi -> "omapi"
        }

    val isdrAid: ByteArray
        get() = EsimApdu.decodeAidHex(isdrAidHex)

    val isdrAidLabel: String
        get() = EsimApdu.aidLabel(isdrAid)

    val identity: String
        get() = buildString {
            append(kind.name)
            append(':')
            append(key)
            append(':')
            if (kind == EsimExternalProfileSourceKind.UsbCcid) {
                append("if")
                append(usbInterfaceId ?: 0)
                append(':')
            }
            logicalSlotIndex?.let {
                append("slot")
                append(it)
                append(':')
            }
            append(isdrAidHex)
        }

    val stableToken: String
        get() = Integer.toUnsignedString(identity.hashCode(), 36)
}

enum class EsimExternalProfileState {
    Enabled,
    Disabled
}

data class EsimExternalProfileSummary(
    val commandId: String,
    val iccid: String,
    val displayName: String,
    val nickname: String,
    val providerName: String,
    val isdpAid: String,
    val state: EsimExternalProfileState,
    val profileClass: String,
    val source: EsimExternalProfileSource,
    val legacyCommandId: String = commandId
) {
    val maskedIccid: String
        get() = iccid.maskIccid()

    fun matchesIdentifier(raw: String): Boolean {
        val id = raw.trim()
        return id.equals(commandId, ignoreCase = true) ||
            id.equals(legacyCommandId, ignoreCase = true) ||
            id.equals(iccid, ignoreCase = true) ||
            id.equals(maskedIccid, ignoreCase = true) ||
            (id.length >= 4 && iccid.endsWith(id, ignoreCase = true))
    }
}

data class EsimExternalProfileReadResult(
    val profiles: List<EsimExternalProfileSummary>,
    val messages: List<String>,
    val availableSources: List<EsimExternalProfileSource> = emptyList(),
    val summaryMessageOverride: String? = null
) {
    val summaryMessage: String
        get() = summaryMessageOverride ?: when {
            profiles.isNotEmpty() -> "Read ${profiles.size} external eUICC profile(s)."
            messages.isNotEmpty() -> messages.joinToString(separator = "；")
            else -> "No external eUICC profiles were read."
    }
}

internal fun compactExternalProfileReadMessages(
    context: Context,
    messages: List<String>
): String {
    val cleanMessages = messages.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    if (cleanMessages.isEmpty()) return context.getString(R.string.message_no_external_profiles_read)
    if (cleanMessages.size <= 2) {
        return cleanMessages.joinToString(separator = context.getString(R.string.separator_semicolon))
    }

    val accessDeniedCount = cleanMessages.count { it.indicatesAccessRuleDenied() }
    val timeoutCount = cleanMessages.count { it.indicatesTimeout() }
    val communicationCount = cleanMessages.count { it.indicatesSecureElementCommunicationFailure() }
    val knownCount = accessDeniedCount + timeoutCount + communicationCount
    val otherCount = (cleanMessages.size - knownCount).coerceAtLeast(0)

    return buildList {
        if (accessDeniedCount > 0) {
            add(context.getString(R.string.format_external_read_access_rule_summary, accessDeniedCount))
        }
        if (timeoutCount > 0) {
            add(context.getString(R.string.format_external_read_timeout_summary, timeoutCount))
        }
        if (communicationCount > 0) {
            add(context.getString(R.string.format_external_read_communication_summary, communicationCount))
        }
        if (otherCount > 0) {
            add(context.getString(R.string.format_external_read_other_summary, otherCount))
        }
        add(context.getString(R.string.message_external_read_details_in_logs))
    }.joinToString(separator = context.getString(R.string.separator_semicolon))
}

data class EsimExternalEuiccDetailsResult(
    val details: List<EsimExternalEuiccDetails>,
    val messages: List<String>,
    val summaryMessageOverride: String? = null
) {
    val summaryMessage: String
        get() = summaryMessageOverride ?: when {
            details.isNotEmpty() -> "Read ${details.size} external eUICC detail record(s)."
            messages.isNotEmpty() -> messages.joinToString(separator = "；")
            else -> "No external eUICC details were read."
        }
}

data class EsimExternalEuiccDetails(
    val source: EsimExternalProfileSource,
    val maskedEid: String?,
    val info: EsimExternalEuiccInfo?,
    val notifications: List<EsimExternalEuiccNotification>
)

data class EsimExternalEuiccInfo(
    val vendor: EsimExternalEuiccVendorInfo?,
    val sgp22Version: String,
    val profileVersion: String,
    val firmwareVersion: String,
    val globalPlatformVersion: String,
    val sasAccreditationNumber: String,
    val ppVersion: String,
    val freeNvram: String,
    val freeRam: String,
    val signingPkidCount: Int,
    val verificationPkidCount: Int
)

data class EsimExternalEuiccVendorInfo(
    val name: String,
    val model: String,
    val firmwareVersion: String
)

data class EsimExternalEuiccNotification(
    val commandId: String,
    val legacyCommandId: String = commandId,
    val seqNumber: Long,
    val operation: String,
    val notificationAddress: String,
    val maskedIccid: String,
    val source: EsimExternalProfileSource
)

internal fun LocalProfileInfo.toExternalProfileSummary(
    source: EsimExternalProfileSource,
    index: Int
): EsimExternalProfileSummary {
    val cleanIccid = iccid.trim()
    val display = nickName.trim()
        .ifBlank { name.trim() }
        .ifBlank { providerName.trim() }
        .ifBlank { "Unnamed profile" }
    val suffix = cleanIccid.takeLast(6).ifBlank { (index + 1).toString().padStart(2, '0') }
    val token = cleanIccid.stableExternalToken()
    val legacyCommandId = "ext-${source.commandPrefix}-$token-$suffix"
    val commandId = "ext-${source.commandPrefix}-${source.stableToken}-$token-$suffix"
    return EsimExternalProfileSummary(
        commandId = commandId,
        iccid = cleanIccid,
        displayName = display,
        nickname = nickName.trim(),
        providerName = providerName.trim(),
        isdpAid = isdpAID.trim(),
        state = when (state) {
            LocalProfileInfo.State.Enabled -> EsimExternalProfileState.Enabled
            LocalProfileInfo.State.Disabled -> EsimExternalProfileState.Disabled
        },
        profileClass = profileClass.displayName(),
        source = source,
        legacyCommandId = legacyCommandId
    )
}

internal fun LocalProfileInfo.isVisibleInDefaultExternalProfileList(): Boolean =
    profileClass == ProfileClass.Operational || state == LocalProfileInfo.State.Enabled

internal fun EuiccInfo2.toExternalEuiccInfo(
    source: EsimExternalProfileSource,
    eid: String?
): EsimExternalEuiccInfo =
    EsimExternalEuiccInfo(
        vendor = externalVendorInfo(source = source, eid = eid, euiccInfo = this),
        sgp22Version = sgp22Version.toString(),
        profileVersion = profileVersion.toString(),
        firmwareVersion = euiccFirmwareVersion.toString(),
        globalPlatformVersion = globalPlatformVersion.toString(),
        sasAccreditationNumber = sasAccreditationNumber,
        ppVersion = ppVersion.toString(),
        freeNvram = EsimEuiccInfoFormatter.formatBytes(freeNvram.toLong()),
        freeRam = EsimEuiccInfoFormatter.formatBytes(freeRam.toLong()),
        signingPkidCount = euiccCiPKIdListForSigning.size,
        verificationPkidCount = euiccCiPKIdListForVerification.size
    )

private fun externalVendorInfo(
    source: EsimExternalProfileSource,
    eid: String?,
    euiccInfo: EuiccInfo2
): EsimExternalEuiccVendorInfo? {
    val aidLabel = source.isdrAidLabel
    val model = when {
        aidLabel.startsWith("eSTK") -> "eSTK removable eUICC"
        aidLabel == "eSIM.me" -> "eSIM.me removable eUICC"
        aidLabel == "5ber.eSIM" -> "5ber.eSIM removable eUICC"
        aidLabel == "Xesim" -> "Xesim removable eUICC"
        aidLabel == "LinksField" -> "LinksField removable eUICC"
        eid.isSimLinkEid() -> simLinkModelName(euiccInfo.euiccFirmwareVersion)
        else -> return null
    }
    val name = when {
        model.startsWith("eSTK") -> "eSTK.me"
        model.startsWith("eSIM.me") -> "eSIM.me"
        model.startsWith("5ber") -> "5ber"
        model.startsWith("Xesim") -> "Xesim"
        model.startsWith("LinksField") -> "LinksField"
        model.startsWith("9eSIM") -> "SIMLink"
        else -> "Removable eUICC"
    }
    return EsimExternalEuiccVendorInfo(
        name = name,
        model = model,
        firmwareVersion = euiccInfo.euiccFirmwareVersion.toString()
    )
}

private fun String?.isSimLinkEid(): Boolean {
    val clean = orEmpty()
    return clean.startsWith("890440458467274948") ||
        clean.startsWith("890440452167274948")
}

private fun simLinkModelName(version: Version): String {
    val versionName = when {
        version >= Version(37, 4, 3) -> "v3.2 (beta 1)"
        version >= Version(37, 1, 41) -> "v3.1 (beta 1)"
        version >= Version(36, 18, 5) -> "v3 (final)"
        version >= Version(36, 17, 39) -> "v3 (beta)"
        version >= Version(36, 17, 4) -> "v2s"
        version >= Version(36, 9, 3) -> "v2.1"
        version >= Version(36, 7, 2) -> "v2"
        else -> null
    }
    return versionName?.let { "9eSIM $it" } ?: "9eSIM"
}

internal fun LocalProfileNotification.toExternalNotificationSummary(
    source: EsimExternalProfileSource
): EsimExternalEuiccNotification {
    val suffix = iccid.trim().takeLast(6).ifBlank { seqNumber.toString() }
    val token = iccid.trim().stableExternalToken()
    val legacyCommandId = "notif-${source.commandPrefix}-$token-$seqNumber-$suffix"
    return EsimExternalEuiccNotification(
        commandId = "notif-${source.commandPrefix}-${source.stableToken}-$token-$seqNumber-$suffix",
        legacyCommandId = legacyCommandId,
        seqNumber = seqNumber,
        operation = profileManagementOperation.name,
        notificationAddress = notificationAddress.trim(),
        maskedIccid = iccid.maskIccid(),
        source = source
    )
}

private fun ProfileClass.displayName(): String =
    name.lowercase().replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase() else char.toString()
    }

private fun String.maskIccid(): String {
    val clean = trim()
    if (clean.length <= 8) return clean
    return "${clean.take(4)}****${clean.takeLast(4)}"
}

internal fun String.maskEid(): String {
    val clean = trim()
    if (clean.length <= 8) return clean
    return "${clean.take(4)}****${clean.takeLast(4)}"
}

private fun String.stableExternalToken(): String {
    val clean = trim()
    if (clean.isBlank()) return "unknown"
    return Integer.toUnsignedString(clean.hashCode(), 36)
}

private fun String.indicatesAccessRuleDenied(): Boolean =
    contains("访问规则拒绝") ||
        contains("ARA-M", ignoreCase = true) ||
        contains("access rule", ignoreCase = true) ||
        contains("access rules", ignoreCase = true) ||
        contains("rejected Paysage", ignoreCase = true)

private fun String.indicatesTimeout(): Boolean =
    contains("超时") ||
        contains("timeout", ignoreCase = true) ||
        contains("timed out", ignoreCase = true)

private fun String.indicatesSecureElementCommunicationFailure(): Boolean =
    contains("安全元素通信失败") ||
        contains("secure element", ignoreCase = true) ||
        contains("OMAPI 与安全元素通信失败", ignoreCase = true)
