package joyin.takgi.paysage.esim

import android.content.Context

const val ESIM_DEFAULT_ES10X_MSS = 63
const val ESIM_HIGH_EFFICIENCY_ES10X_MSS = 255

data class EsimUserSettings(
    val switchAfterDownload: Boolean = true,
    val refreshAfterSystemOperation: Boolean = true,
    val runCompatibilityCheckOnOpen: Boolean = true,
    val includeAdvancedDiagnostics: Boolean = false,
    val forceUsbTpduMode: Boolean = false,
    val es10xMss: Int = ESIM_DEFAULT_ES10X_MSS,
    val showNonOperationalProfiles: Boolean = false,
    val customIsdrAidList: String = "",
    val showOperationHistory: Boolean = true,
    val autoHandleExternalNotifications: Boolean = true,
    val removeHandledExternalNotifications: Boolean = false,
    val notifyDownloadOperations: Boolean = true,
    val notifySwitchOperations: Boolean = true,
    val notifyDeleteOperations: Boolean = true,
    val notifyRenameOperations: Boolean = true
)

data class EsimIsdrAidListSummary(
    val effectiveAids: List<ByteArray>,
    val invalidLines: List<String>,
    val usingDefaults: Boolean
) {
    val effectiveAidLabels: List<String>
        get() = effectiveAids.map { aid -> EsimApdu.aidLabel(aid) }

    val effectiveAidHex: List<String>
        get() = effectiveAids.map { aid -> EsimApdu.aidHex(aid) }
}

class EsimSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "paysage_esim_settings",
        Context.MODE_PRIVATE
    )

    fun read(): EsimUserSettings =
        EsimUserSettings(
            switchAfterDownload = preferences.getBoolean(KEY_SWITCH_AFTER_DOWNLOAD, true),
            refreshAfterSystemOperation = preferences.getBoolean(KEY_REFRESH_AFTER_OPERATION, true),
            runCompatibilityCheckOnOpen = preferences.getBoolean(KEY_COMPATIBILITY_ON_OPEN, true),
            includeAdvancedDiagnostics = preferences.getBoolean(KEY_ADVANCED_DIAGNOSTICS, false),
            forceUsbTpduMode = preferences.getBoolean(KEY_FORCE_USB_TPDU_MODE, false),
            es10xMss = preferences.getInt(KEY_ES10X_MSS, ESIM_DEFAULT_ES10X_MSS).coerceIn(1, 255),
            showNonOperationalProfiles = preferences.getBoolean(KEY_SHOW_NON_OPERATIONAL_PROFILES, false),
            customIsdrAidList = preferences.getString(KEY_CUSTOM_ISDR_AID_LIST, "").orEmpty(),
            showOperationHistory = preferences.getBoolean(KEY_SHOW_OPERATION_HISTORY, true),
            autoHandleExternalNotifications = preferences.getBoolean(KEY_AUTO_HANDLE_EXTERNAL_NOTIFICATIONS, true),
            removeHandledExternalNotifications = preferences.getBoolean(KEY_REMOVE_HANDLED_EXTERNAL_NOTIFICATIONS, false),
            notifyDownloadOperations = preferences.getBoolean(KEY_NOTIFY_DOWNLOAD, true),
            notifySwitchOperations = preferences.getBoolean(KEY_NOTIFY_SWITCH, true),
            notifyDeleteOperations = preferences.getBoolean(KEY_NOTIFY_DELETE, true),
            notifyRenameOperations = preferences.getBoolean(KEY_NOTIFY_RENAME, true)
        )

    fun write(settings: EsimUserSettings) {
        preferences.edit()
            .putBoolean(KEY_SWITCH_AFTER_DOWNLOAD, settings.switchAfterDownload)
            .putBoolean(KEY_REFRESH_AFTER_OPERATION, settings.refreshAfterSystemOperation)
            .putBoolean(KEY_COMPATIBILITY_ON_OPEN, settings.runCompatibilityCheckOnOpen)
            .putBoolean(KEY_ADVANCED_DIAGNOSTICS, settings.includeAdvancedDiagnostics)
            .putBoolean(KEY_FORCE_USB_TPDU_MODE, settings.forceUsbTpduMode)
            .putInt(KEY_ES10X_MSS, settings.es10xMss.coerceIn(1, 255))
            .putBoolean(KEY_SHOW_NON_OPERATIONAL_PROFILES, settings.showNonOperationalProfiles)
            .putString(KEY_CUSTOM_ISDR_AID_LIST, settings.customIsdrAidList)
            .putBoolean(KEY_SHOW_OPERATION_HISTORY, settings.showOperationHistory)
            .putBoolean(KEY_AUTO_HANDLE_EXTERNAL_NOTIFICATIONS, settings.autoHandleExternalNotifications)
            .putBoolean(KEY_REMOVE_HANDLED_EXTERNAL_NOTIFICATIONS, settings.removeHandledExternalNotifications)
            .putBoolean(KEY_NOTIFY_DOWNLOAD, settings.notifyDownloadOperations)
            .putBoolean(KEY_NOTIFY_SWITCH, settings.notifySwitchOperations)
            .putBoolean(KEY_NOTIFY_DELETE, settings.notifyDeleteOperations)
            .putBoolean(KEY_NOTIFY_RENAME, settings.notifyRenameOperations)
            .apply()
    }

    companion object {
        private const val KEY_SWITCH_AFTER_DOWNLOAD = "switch_after_download"
        private const val KEY_REFRESH_AFTER_OPERATION = "refresh_after_system_operation"
        private const val KEY_COMPATIBILITY_ON_OPEN = "compatibility_check_on_open"
        private const val KEY_ADVANCED_DIAGNOSTICS = "include_advanced_diagnostics"
        private const val KEY_FORCE_USB_TPDU_MODE = "force_usb_tpdu_mode"
        private const val KEY_ES10X_MSS = "es10x_mss"
        private const val KEY_SHOW_NON_OPERATIONAL_PROFILES = "show_non_operational_profiles"
        private const val KEY_CUSTOM_ISDR_AID_LIST = "custom_isdr_aid_list"
        private const val KEY_SHOW_OPERATION_HISTORY = "show_operation_history"
        private const val KEY_AUTO_HANDLE_EXTERNAL_NOTIFICATIONS = "auto_handle_external_notifications"
        private const val KEY_REMOVE_HANDLED_EXTERNAL_NOTIFICATIONS = "remove_handled_external_notifications"
        private const val KEY_NOTIFY_DOWNLOAD = "notify_download_operations"
        private const val KEY_NOTIFY_SWITCH = "notify_switch_operations"
        private const val KEY_NOTIFY_DELETE = "notify_delete_operations"
        private const val KEY_NOTIFY_RENAME = "notify_rename_operations"
    }
}

fun EsimUserSettings.effectiveIsdrAids(): List<ByteArray> {
    return isdrAidListSummary().effectiveAids
}

fun EsimUserSettings.isdrAidListSummary(): EsimIsdrAidListSummary {
    val customAids = mutableListOf<ByteArray>()
    val invalidLines = mutableListOf<String>()
    customIsdrAidList.lineSequence().forEachIndexed { index, rawLine ->
        val aidHex = rawLine.substringBefore('#').trim().replace(" ", "")
        if (aidHex.isBlank()) return@forEachIndexed
        val aid = runCatching { EsimApdu.decodeAidHex(aidHex) }.getOrNull()
        if (aid == null) {
            invalidLines += (index + 1).toString()
        } else {
            customAids += aid
        }
    }

    if (customAids.isEmpty()) {
        return EsimIsdrAidListSummary(
            effectiveAids = EsimApdu.KNOWN_ISD_R_AIDS,
            invalidLines = invalidLines,
            usingDefaults = true
        )
    }

    val withDefault = if (customAids.any { it.contentEquals(EsimApdu.ISD_R_AID) }) {
        customAids
    } else {
        customAids + EsimApdu.ISD_R_AID
    }
    return EsimIsdrAidListSummary(
        effectiveAids = withDefault.distinctBy { EsimApdu.aidHex(it) },
        invalidLines = invalidLines,
        usingDefaults = false
    )
}
