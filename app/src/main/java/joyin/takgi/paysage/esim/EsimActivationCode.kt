package joyin.takgi.paysage.esim

import androidx.annotation.StringRes
import joyin.takgi.paysage.R

data class EsimText(
    @StringRes val resId: Int,
    val args: List<Any> = emptyList()
)

data class EsimActivationCode(
    val raw: String,
    val encoded: String,
    val formatVersion: String?,
    val smdpAddress: String?,
    val matchingId: String?,
    val oid: String?,
    val confirmationCodeRequired: Boolean,
    val confirmationCode: String?,
    val imei: String?,
    val extraParameters: List<String>
) {
    val isValid: Boolean
        get() = encoded.contains("$") &&
            !smdpAddress.isNullOrBlank() &&
            EsimSmdpAddressValidator.isValid(smdpAddress)

    val normalizedLpa: String
        get() = if (encoded.startsWith("LPA:", ignoreCase = true)) encoded else "LPA:$encoded"

    fun withStandaloneConfirmationCode(value: String): EsimActivationCode {
        val cleanValue = value.trim()
        return if (cleanValue.isBlank()) {
            this
        } else {
            copy(confirmationCode = cleanValue)
        }
    }

    fun withImei(value: String): EsimActivationCode =
        copy(imei = value.trim().takeIf { it.isNotBlank() })
}

data class EsimActivationCodeDraft(
    val activationCode: EsimActivationCode?,
    val message: EsimText
) {
    val isValid: Boolean
        get() = activationCode?.isValid == true
}

enum class EsimActivationCheckSeverity {
    Pass,
    Warning,
    Error
}

data class EsimActivationCheck(
    val severity: EsimActivationCheckSeverity,
    val title: EsimText,
    val message: EsimText
)

data class EsimActivationPreflightReport(
    val checks: List<EsimActivationCheck>
) {
    val hasErrors: Boolean
        get() = checks.any { it.severity == EsimActivationCheckSeverity.Error }

    val hasWarnings: Boolean
        get() = checks.any { it.severity == EsimActivationCheckSeverity.Warning }

    val summary: EsimText
        get() = when {
            hasErrors -> esimText(R.string.summary_preflight_has_errors)
            hasWarnings -> esimText(R.string.summary_preflight_has_warnings)
            else -> esimText(R.string.summary_preflight_passed)
        }
}

fun esimText(@StringRes resId: Int, vararg args: Any): EsimText =
    EsimText(resId = resId, args = args.toList())

object EsimActivationCodeParser {
    fun parse(input: String): EsimActivationCode {
        val raw = input.trim()
        val encoded = raw.removeLpaPrefix().trim()
        val parts = encoded.split("$")

        return EsimActivationCode(
            raw = raw,
            encoded = encoded,
            formatVersion = parts.getOrNull(0)?.takeIf { it.isNotBlank() },
            smdpAddress = parts.getOrNull(1)?.takeIf { it.isNotBlank() },
            matchingId = parts.getOrNull(2)?.takeIf { it.isNotBlank() },
            oid = parts.getOrNull(3)?.takeIf { it.isNotBlank() },
            confirmationCodeRequired = parts.getOrNull(4)?.trim() == "1",
            confirmationCode = null,
            imei = null,
            extraParameters = if (parts.size > 5) parts.drop(5).filter { it.isNotBlank() } else emptyList()
        )
    }
}

object EsimActivationCodeExtractor {
    fun extract(input: String): EsimActivationCodeDraft {
        val candidate = candidateFromInput(input)
        if (candidate.isNullOrBlank()) {
            return EsimActivationCodeDraft(null, esimText(R.string.message_no_esim_lpa_code_in_qr))
        }

        val activationCode = runCatching {
            EsimActivationCodeParser.parse(candidate)
        }.getOrNull()

        return if (activationCode?.isValid == true) {
            EsimActivationCodeDraft(activationCode, esimText(R.string.message_esim_lpa_code_recognized))
        } else {
            EsimActivationCodeDraft(null, esimText(R.string.message_qr_not_complete_lpa))
        }
    }

    private fun candidateFromInput(input: String): String? {
        val clean = input.trim()
        if (clean.isBlank()) return null
        directCandidate(clean)?.let { return it }
        queryValueCandidates(clean).forEach { value ->
            directCandidate(value)?.let { return it }
        }
        return null
    }

    private fun directCandidate(input: String): String? {
        val clean = input.trim().trim('"', '\'')
        if (clean.startsWith("LPA:", ignoreCase = true) || clean.startsWith("1${'$'}")) {
            return clean.takeLpaToken()
        }
        val embeddedIndex = clean.indexOf("LPA:", ignoreCase = true)
        if (embeddedIndex >= 0) {
            return clean.substring(embeddedIndex).takeLpaToken()
        }
        return null
    }

    private fun String.takeLpaToken(): String =
        trim()
            .takeWhile { !it.isWhitespace() && it != '&' }
            .trimEnd('.', ',', ';')

    private fun queryValueCandidates(input: String): List<String> {
        val query = input.substringAfter('?', missingDelimiterValue = "")
            .substringBefore('#')
        if (query.isBlank()) return emptyList()

        return query.split('&')
            .flatMap { pair ->
                val key = pair.substringBefore('=', missingDelimiterValue = pair)
                val value = pair.substringAfter('=', missingDelimiterValue = "")
                listOf(key, value)
            }
            .mapNotNull { value -> value.urlDecodeOrNull()?.trim()?.takeIf { it.isNotBlank() } }
    }
}

object EsimActivationCodePreflight {
    fun analyze(
        activationCode: EsimActivationCode,
        standaloneConfirmationCodeProvided: Boolean
    ): EsimActivationPreflightReport =
        EsimActivationPreflightReport(
            checks = listOf(
                formatVersionCheck(activationCode),
                smdpAddressCheck(activationCode),
                matchingIdCheck(activationCode),
                activationCodeMetadataCheck(activationCode),
                confirmationCodeCheck(activationCode, standaloneConfirmationCodeProvided),
                imeiCheck(activationCode),
                extraParametersCheck(activationCode)
            )
        )

    private fun formatVersionCheck(code: EsimActivationCode): EsimActivationCheck {
        val version = code.formatVersion.orEmpty()
        return when {
            version.isBlank() -> check(
                severity = EsimActivationCheckSeverity.Error,
                titleRes = R.string.title_format_version_missing,
                messageRes = R.string.message_format_version_missing
            )
            version != "1" -> check(
                severity = EsimActivationCheckSeverity.Warning,
                titleRes = R.string.title_format_version_not_one,
                messageRes = R.string.message_format_version_not_one,
                version
            )
            else -> check(
                severity = EsimActivationCheckSeverity.Pass,
                titleRes = R.string.title_format_version,
                messageRes = R.string.message_format_version_recognized
            )
        }
    }

    private fun smdpAddressCheck(code: EsimActivationCode): EsimActivationCheck {
        val address = code.smdpAddress.orEmpty()
        return when {
            address.isBlank() -> check(
                severity = EsimActivationCheckSeverity.Error,
                titleRes = R.string.title_smdp_address_missing,
                messageRes = R.string.message_smdp_address_missing
            )
            address.any { it.isWhitespace() } -> check(
                severity = EsimActivationCheckSeverity.Error,
                titleRes = R.string.title_smdp_address_spaces,
                messageRes = R.string.message_smdp_address_spaces
            )
            EsimSmdpAddressValidator.error(address) != null -> check(
                severity = EsimActivationCheckSeverity.Error,
                titleRes = R.string.title_smdp_address_unavailable,
                message = EsimSmdpAddressValidator.error(address) ?: esimText(R.string.error_smdp_domain_required)
            )
            else -> check(
                severity = EsimActivationCheckSeverity.Pass,
                titleRes = R.string.title_smdp_address,
                messageRes = R.string.message_smdp_address_ok
            )
        }
    }

    private fun matchingIdCheck(code: EsimActivationCode): EsimActivationCheck {
        val matchingId = code.matchingId.orEmpty()
        return when {
            matchingId.isBlank() -> check(
                severity = EsimActivationCheckSeverity.Warning,
                titleRes = R.string.title_matching_id_empty,
                messageRes = R.string.message_matching_id_empty
            )
            matchingId.any { it.isWhitespace() } -> check(
                severity = EsimActivationCheckSeverity.Error,
                titleRes = R.string.title_matching_id_spaces,
                messageRes = R.string.message_matching_id_spaces
            )
            else -> check(
                severity = EsimActivationCheckSeverity.Pass,
                titleRes = R.string.title_matching_id,
                messageRes = R.string.message_matching_id_ok
            )
        }
    }

    private fun activationCodeMetadataCheck(code: EsimActivationCode): EsimActivationCheck {
        val messageRes = when {
            code.oid != null && code.confirmationCodeRequired -> R.string.message_activation_flags_oid_confirmation
            code.oid != null -> R.string.message_activation_flags_oid_reserved
            code.confirmationCodeRequired -> R.string.message_activation_flags_confirmation_required
            else -> R.string.message_activation_flags_none
        }
        return check(
            severity = EsimActivationCheckSeverity.Pass,
            titleRes = R.string.title_activation_code_flags,
            messageRes = messageRes
        )
    }

    private fun confirmationCodeCheck(
        code: EsimActivationCode,
        standaloneConfirmationCodeProvided: Boolean
    ): EsimActivationCheck {
        val confirmationCode = code.confirmationCode.orEmpty()
        return when {
            code.confirmationCodeRequired && confirmationCode.isBlank() -> check(
                severity = EsimActivationCheckSeverity.Error,
                titleRes = R.string.title_confirmation_required,
                messageRes = R.string.message_confirmation_required
            )
            confirmationCode.isBlank() -> check(
                severity = EsimActivationCheckSeverity.Warning,
                titleRes = R.string.title_confirmation_missing,
                messageRes = R.string.message_confirmation_missing
            )
            confirmationCode.any { it.isWhitespace() } -> check(
                severity = EsimActivationCheckSeverity.Error,
                titleRes = R.string.title_confirmation_spaces,
                messageRes = R.string.message_confirmation_spaces
            )
            standaloneConfirmationCodeProvided -> check(
                severity = EsimActivationCheckSeverity.Pass,
                titleRes = R.string.title_confirmation_code,
                messageRes = if (code.confirmationCodeRequired) {
                    R.string.message_confirmation_submitted_required
                } else {
                    R.string.message_confirmation_submitted_optional
                }
            )
            else -> check(
                severity = EsimActivationCheckSeverity.Pass,
                titleRes = R.string.title_confirmation_code,
                messageRes = R.string.message_confirmation_ready
            )
        }
    }

    private fun imeiCheck(code: EsimActivationCode): EsimActivationCheck {
        val imei = code.imei.orEmpty()
        return when {
            imei.isBlank() -> check(
                severity = EsimActivationCheckSeverity.Pass,
                titleRes = R.string.title_imei,
                messageRes = R.string.message_imei_blank_ok
            )
            !imei.all { it.isDigit() } -> check(
                severity = EsimActivationCheckSeverity.Error,
                titleRes = R.string.title_imei_non_digits,
                messageRes = R.string.message_imei_non_digits
            )
            imei.length !in 14..16 -> check(
                severity = EsimActivationCheckSeverity.Warning,
                titleRes = R.string.title_imei_length_check,
                messageRes = R.string.message_imei_length_check
            )
            else -> check(
                severity = EsimActivationCheckSeverity.Pass,
                titleRes = R.string.title_imei,
                messageRes = R.string.message_imei_ready
            )
        }
    }

    private fun extraParametersCheck(code: EsimActivationCode): EsimActivationCheck {
        return if (code.extraParameters.isEmpty()) {
            check(
                severity = EsimActivationCheckSeverity.Pass,
                titleRes = R.string.title_extra_parameters,
                messageRes = R.string.message_extra_parameters_none
            )
        } else {
            check(
                severity = EsimActivationCheckSeverity.Warning,
                titleRes = R.string.title_extra_parameters_present,
                messageRes = R.string.message_extra_parameters_count,
                code.extraParameters.size
            )
        }
    }

    private fun check(
        severity: EsimActivationCheckSeverity,
        @StringRes titleRes: Int,
        @StringRes messageRes: Int,
        vararg messageArgs: Any
    ): EsimActivationCheck =
        check(
            severity = severity,
            titleRes = titleRes,
            message = esimText(messageRes, *messageArgs)
        )

    private fun check(
        severity: EsimActivationCheckSeverity,
        @StringRes titleRes: Int,
        message: EsimText
    ): EsimActivationCheck =
        EsimActivationCheck(
            severity = severity,
            title = esimText(titleRes),
            message = message
        )
}

object EsimActivationCodeComposer {
    fun compose(
        smdpAddress: String,
        matchingId: String,
        oid: String? = null,
        confirmationCodeRequired: Boolean = false,
        imei: String? = null
    ): EsimActivationCodeDraft {
        parseCompleteLpaFromField(smdpAddress)?.let {
            return EsimActivationCodeDraft(it.withImei(imei.orEmpty()), esimText(R.string.message_full_lpa_split))
        }
        parseCompleteLpaFromField(matchingId)?.let {
            return EsimActivationCodeDraft(it.withImei(imei.orEmpty()), esimText(R.string.message_full_lpa_split))
        }

        val cleanAddress = EsimSmdpAddressNormalizer.normalize(smdpAddress)
        val cleanMatchingId = matchingId.trim()
        val cleanOid = oid?.trim().orEmpty()

        if (cleanAddress.isBlank()) {
            return EsimActivationCodeDraft(null, esimText(R.string.message_fill_smdp_address))
        }
        if (cleanAddress.contains("$") || cleanMatchingId.contains("$")) {
            return EsimActivationCodeDraft(null, esimText(R.string.message_paste_complete_lpa_to_field_start))
        }

        val encoded = listOf(
            "1",
            cleanAddress,
            cleanMatchingId,
            cleanOid,
            if (confirmationCodeRequired) "1" else ""
        )
            .dropLastWhile { it.isBlank() }
            .joinToString("$")
        val activationCode = EsimActivationCodeParser.parse(encoded).withImei(imei.orEmpty())
        return EsimActivationCodeDraft(
            activationCode = activationCode,
            message = esimText(R.string.message_standard_lpa_generated)
        )
    }

    private fun parseCompleteLpaFromField(input: String): EsimActivationCode? {
        val clean = input.trim()
        if (!clean.startsWith("LPA:", ignoreCase = true) && !clean.startsWith("1${'$'}")) {
            return null
        }
        return runCatching { EsimActivationCodeParser.parse(clean) }
            .getOrNull()
            ?.takeIf { it.isValid }
    }
}

object EsimSmdpAddressNormalizer {
    fun normalize(input: String): String =
        input.trim()
            .removeLpaPrefix()
            .removePrefixIgnoreCase("https://")
            .removePrefixIgnoreCase("http://")
            .trim()
            .trimEnd('/')
}

object EsimSmdpAddressValidator {
    fun isValid(input: String): Boolean = error(input) == null

    fun error(input: String): EsimText? {
        val address = input.trim()
        if (!address.contains('.')) return esimText(R.string.error_smdp_domain_required)

        var fqdn = address
        var port = 443
        if (address.contains(':')) {
            val portIndex = address.lastIndexOf(':')
            fqdn = address.take(portIndex)
            port = address.substring(portIndex + 1, address.length).toIntOrNull(10) ?: 0
        }
        if (port !in 1..0xffff) return esimText(R.string.error_smdp_port_range)
        if (fqdn.isEmpty() || fqdn.length > 255) return esimText(R.string.error_smdp_domain_length)

        fqdn.split('.').forEach { label ->
            if (label.isEmpty()) return esimText(R.string.error_smdp_segment_empty)
            if (label.length > 64) return esimText(R.string.error_smdp_segment_length)
            if (label.first() == '-' || label.last() == '-') return esimText(R.string.error_smdp_hyphen_edges)
            if (!label.all { it.isLetterOrDigit() || it == '-' }) {
                return esimText(R.string.error_smdp_domain_chars)
            }
        }

        return null
    }
}

private fun String.removeLpaPrefix(): String =
    if (startsWith("LPA:", ignoreCase = true)) substring(4) else this

private fun String.removePrefixIgnoreCase(prefix: String): String =
    if (startsWith(prefix, ignoreCase = true)) substring(prefix.length) else this

private fun String.urlDecodeOrNull(): String? =
    runCatching {
        java.net.URLDecoder.decode(this, Charsets.UTF_8.name())
    }.getOrNull()
