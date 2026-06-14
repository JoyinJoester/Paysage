package joyin.takgi.paysage.esim

import android.content.Context

data class EsimUserSettings(
    val switchAfterDownload: Boolean = true,
    val refreshAfterSystemOperation: Boolean = true,
    val runCompatibilityCheckOnOpen: Boolean = true,
    val includeAdvancedDiagnostics: Boolean = false,
    val showOperationHistory: Boolean = true,
    val notifyDownloadOperations: Boolean = true,
    val notifySwitchOperations: Boolean = true,
    val notifyDeleteOperations: Boolean = true,
    val notifyRenameOperations: Boolean = true
)

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
            showOperationHistory = preferences.getBoolean(KEY_SHOW_OPERATION_HISTORY, true),
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
            .putBoolean(KEY_SHOW_OPERATION_HISTORY, settings.showOperationHistory)
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
        private const val KEY_SHOW_OPERATION_HISTORY = "show_operation_history"
        private const val KEY_NOTIFY_DOWNLOAD = "notify_download_operations"
        private const val KEY_NOTIFY_SWITCH = "notify_switch_operations"
        private const val KEY_NOTIFY_DELETE = "notify_delete_operations"
        private const val KEY_NOTIFY_RENAME = "notify_rename_operations"
    }
}
