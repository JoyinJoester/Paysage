package joyin.takgi.paysage.esim

import org.json.JSONArray
import org.json.JSONObject

internal object EsimExternalProfileJson {
    fun encodeReadResult(result: EsimExternalProfileReadResult): String =
        JSONObject()
            .put("profiles", JSONArray(result.profiles.map(::encodeProfile)))
            .put("messages", JSONArray(result.messages))
            .put("availableSources", JSONArray(result.availableSources.map(::encodeSource)))
            .put("summaryMessageOverride", result.summaryMessageOverride)
            .toString()

    fun decodeReadResult(raw: String): EsimExternalProfileReadResult {
        val json = JSONObject(raw)
        return EsimExternalProfileReadResult(
            profiles = json.optJSONArray("profiles").orEmptyObjects().map(::decodeProfile),
            messages = json.optJSONArray("messages").orEmptyStrings(),
            availableSources = json.optJSONArray("availableSources").orEmptyObjects().map(::decodeSource),
            summaryMessageOverride = json.optString("summaryMessageOverride").takeIf { it.isNotBlank() }
        )
    }

    private fun encodeProfile(profile: EsimExternalProfileSummary): JSONObject =
        JSONObject()
            .put("commandId", profile.commandId)
            .put("iccid", profile.iccid)
            .put("displayName", profile.displayName)
            .put("nickname", profile.nickname)
            .put("providerName", profile.providerName)
            .put("isdpAid", profile.isdpAid)
            .put("state", profile.state.name)
            .put("profileClass", profile.profileClass)
            .put("source", encodeSource(profile.source))
            .put("legacyCommandId", profile.legacyCommandId)

    private fun decodeProfile(json: JSONObject): EsimExternalProfileSummary =
        EsimExternalProfileSummary(
            commandId = json.optString("commandId"),
            iccid = json.optString("iccid"),
            displayName = json.optString("displayName"),
            nickname = json.optString("nickname"),
            providerName = json.optString("providerName"),
            isdpAid = json.optString("isdpAid"),
            state = runCatching {
                EsimExternalProfileState.valueOf(json.optString("state"))
            }.getOrDefault(EsimExternalProfileState.Disabled),
            profileClass = json.optString("profileClass"),
            source = decodeSource(json.getJSONObject("source")),
            legacyCommandId = json.optString("legacyCommandId").ifBlank { json.optString("commandId") }
        )

    fun encodeSourceString(source: EsimExternalProfileSource): String =
        encodeSource(source).toString()

    fun decodeSourceString(raw: String): EsimExternalProfileSource =
        decodeSource(JSONObject(raw))

    fun encodeSource(source: EsimExternalProfileSource): JSONObject =
        JSONObject()
            .put("kind", source.kind.name)
            .put("key", source.key)
            .put("label", source.label)
            .put("index", source.index)
            .put("isdrAidHex", source.isdrAidHex)
            .put("logicalSlotIndex", source.logicalSlotIndex)
            .put("usbInterfaceId", source.usbInterfaceId)

    fun decodeSource(json: JSONObject): EsimExternalProfileSource =
        EsimExternalProfileSource(
            kind = runCatching {
                EsimExternalProfileSourceKind.valueOf(json.optString("kind"))
            }.getOrDefault(EsimExternalProfileSourceKind.UsbCcid),
            key = json.optString("key"),
            label = json.optString("label"),
            index = json.optInt("index"),
            isdrAidHex = json.optString("isdrAidHex").ifBlank { EsimApdu.ISD_R_AID_HEX },
            logicalSlotIndex = json.optNullableInt("logicalSlotIndex"),
            usbInterfaceId = json.optNullableInt("usbInterfaceId")
        )

    private fun JSONArray?.orEmptyObjects(): List<JSONObject> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                optJSONObject(index)?.let(::add)
            }
        }
    }

    private fun JSONArray?.orEmptyStrings(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                add(optString(index))
            }
        }
    }

    private fun JSONObject.optNullableInt(key: String): Int? =
        if (has(key) && !isNull(key)) optInt(key) else null
}
