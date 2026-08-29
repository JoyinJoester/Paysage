package joyin.takgi.paysage.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import joyin.takgi.paysage.R
import joyin.takgi.paysage.esim.ESIM_DEFAULT_ES10X_MSS
import joyin.takgi.paysage.esim.ESIM_HIGH_EFFICIENCY_ES10X_MSS
import joyin.takgi.paysage.esim.EsimDownloadResultStore
import joyin.takgi.paysage.esim.EsimSettingsStore
import joyin.takgi.paysage.esim.EsimUserSettings
import joyin.takgi.paysage.esim.isdrAidListSummary
import joyin.takgi.paysage.ui.components.M3ePanel
import joyin.takgi.paysage.ui.components.M3eSettingsNavigationItem
import joyin.takgi.paysage.ui.components.M3eSettingsSwitchRow
import joyin.takgi.paysage.ui.components.M3eTopBar

@Composable
fun EuiccSettingsScreen(
    onBackClick: () -> Unit,
    onDiagnosticsSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val settingsStore = remember { EsimSettingsStore(context) }
    var esimSettings by remember { mutableStateOf(settingsStore.read()) }
    var statusMessage by remember { mutableStateOf("") }

    fun updateEsimSettings(next: EsimUserSettings) {
        esimSettings = next
        settingsStore.write(next)
        statusMessage = context.getString(R.string.message_euicc_settings_saved)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            M3eTopBar(
                title = stringResource(R.string.section_euicc_settings),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                M3ePanel(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.section_euicc_settings),
                            style = MaterialTheme.typography.titleLarge
                        )
                        M3eSettingsSwitchRow(
                            title = stringResource(R.string.setting_switch_after_download),
                            summary = stringResource(R.string.setting_switch_after_download_summary),
                            checked = esimSettings.switchAfterDownload,
                            onCheckedChange = {
                                updateEsimSettings(esimSettings.copy(switchAfterDownload = it))
                            }
                        )
                        M3eSettingsSwitchRow(
                            title = stringResource(R.string.setting_refresh_after_system_operation),
                            summary = stringResource(R.string.setting_refresh_after_system_operation_summary),
                            checked = esimSettings.refreshAfterSystemOperation,
                            onCheckedChange = {
                                updateEsimSettings(esimSettings.copy(refreshAfterSystemOperation = it))
                            }
                        )
                        M3eSettingsSwitchRow(
                            title = stringResource(R.string.setting_run_compatibility_check_on_open),
                            summary = stringResource(R.string.setting_run_compatibility_check_on_open_summary),
                            checked = esimSettings.runCompatibilityCheckOnOpen,
                            onCheckedChange = {
                                updateEsimSettings(esimSettings.copy(runCompatibilityCheckOnOpen = it))
                            }
                        )
                        M3eSettingsSwitchRow(
                            title = stringResource(R.string.setting_show_operation_history),
                            summary = stringResource(R.string.setting_show_operation_history_summary),
                            checked = esimSettings.showOperationHistory,
                            onCheckedChange = {
                                updateEsimSettings(esimSettings.copy(showOperationHistory = it))
                            }
                        )
                    }
                }
            }

            item {
                M3ePanel(modifier = Modifier.fillMaxWidth()) {
                    M3eSettingsNavigationItem(
                        icon = Icons.Default.BugReport,
                        title = stringResource(R.string.section_advanced_euicc_diagnostics),
                        subtitle = stringResource(R.string.summary_euicc_diagnostics_settings),
                        onClick = onDiagnosticsSettingsClick
                    )
                }
            }

            item {
                M3ePanel(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.section_euicc_notifications),
                            style = MaterialTheme.typography.titleLarge
                        )
                        M3eSettingsSwitchRow(
                            title = stringResource(R.string.setting_auto_handle_external_notifications),
                            summary = stringResource(R.string.setting_auto_handle_external_notifications_summary),
                            checked = esimSettings.autoHandleExternalNotifications,
                            onCheckedChange = {
                                updateEsimSettings(esimSettings.copy(autoHandleExternalNotifications = it))
                            }
                        )
                        M3eSettingsSwitchRow(
                            title = stringResource(R.string.setting_remove_handled_external_notifications),
                            summary = stringResource(R.string.setting_remove_handled_external_notifications_summary),
                            checked = esimSettings.removeHandledExternalNotifications,
                            onCheckedChange = {
                                updateEsimSettings(esimSettings.copy(removeHandledExternalNotifications = it))
                            }
                        )
                        M3eSettingsSwitchRow(
                            title = stringResource(R.string.setting_notify_download_operations),
                            summary = stringResource(R.string.setting_notify_download_operations_summary),
                            checked = esimSettings.notifyDownloadOperations,
                            onCheckedChange = {
                                updateEsimSettings(esimSettings.copy(notifyDownloadOperations = it))
                            }
                        )
                        M3eSettingsSwitchRow(
                            title = stringResource(R.string.setting_notify_switch_operations),
                            summary = stringResource(R.string.setting_notify_switch_operations_summary),
                            checked = esimSettings.notifySwitchOperations,
                            onCheckedChange = {
                                updateEsimSettings(esimSettings.copy(notifySwitchOperations = it))
                            }
                        )
                        M3eSettingsSwitchRow(
                            title = stringResource(R.string.setting_notify_delete_operations),
                            summary = stringResource(R.string.setting_notify_delete_operations_summary),
                            checked = esimSettings.notifyDeleteOperations,
                            onCheckedChange = {
                                updateEsimSettings(esimSettings.copy(notifyDeleteOperations = it))
                            }
                        )
                        M3eSettingsSwitchRow(
                            title = stringResource(R.string.setting_notify_rename_operations),
                            summary = stringResource(R.string.setting_notify_rename_operations_summary),
                            checked = esimSettings.notifyRenameOperations,
                            onCheckedChange = {
                                updateEsimSettings(esimSettings.copy(notifyRenameOperations = it))
                            }
                        )
                    }
                }
            }

            if (statusMessage.isNotBlank()) {
                item {
                    M3ePanel(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                item {
                    Spacer(modifier = Modifier.height(1.dp))
                }
            }
        }
    }
}

@Composable
fun EuiccDiagnosticsSettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val settingsStore = remember { EsimSettingsStore(context) }
    val resultStore = remember { EsimDownloadResultStore(context) }
    var esimSettings by remember { mutableStateOf(settingsStore.read()) }
    var customAidListEditor by remember { mutableStateOf(esimSettings.customIsdrAidList) }
    var es10xMssEditor by remember { mutableStateOf(esimSettings.es10xMss.toString()) }
    var statusMessage by remember { mutableStateOf("") }

    fun updateEsimSettings(next: EsimUserSettings) {
        esimSettings = next
        settingsStore.write(next)
        statusMessage = context.getString(R.string.message_euicc_settings_saved)
    }

    val aidSummary = esimSettings.copy(customIsdrAidList = customAidListEditor).isdrAidListSummary()
    val parsedEs10xMss = es10xMssEditor.toIntOrNull()?.takeIf { it in 1..255 }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            M3eTopBar(
                title = stringResource(R.string.section_advanced_euicc_diagnostics),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                M3ePanel(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.section_advanced_euicc_diagnostics),
                            style = MaterialTheme.typography.titleLarge
                        )
                        M3eSettingsSwitchRow(
                            title = stringResource(R.string.setting_enable_omapi_diagnostics),
                            summary = stringResource(R.string.setting_enable_omapi_diagnostics_summary),
                            checked = esimSettings.includeAdvancedDiagnostics,
                            onCheckedChange = {
                                updateEsimSettings(esimSettings.copy(includeAdvancedDiagnostics = it))
                            }
                        )
                        M3eSettingsSwitchRow(
                            title = stringResource(R.string.setting_force_usb_tpdu_mode),
                            summary = stringResource(R.string.setting_force_usb_tpdu_mode_summary),
                            checked = esimSettings.forceUsbTpduMode,
                            onCheckedChange = {
                                updateEsimSettings(esimSettings.copy(forceUsbTpduMode = it))
                            }
                        )
                        M3eSettingsSwitchRow(
                            title = stringResource(R.string.setting_show_non_operational_profiles),
                            summary = stringResource(R.string.setting_show_non_operational_profiles_summary),
                            checked = esimSettings.showNonOperationalProfiles,
                            onCheckedChange = {
                                updateEsimSettings(esimSettings.copy(showNonOperationalProfiles = it))
                            }
                        )
                        Text(
                            text = stringResource(R.string.setting_es10x_mss),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.setting_es10x_mss_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    es10xMssEditor = ESIM_DEFAULT_ES10X_MSS.toString()
                                    updateEsimSettings(esimSettings.copy(es10xMss = ESIM_DEFAULT_ES10X_MSS))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.setting_es10x_mss_compatible))
                            }
                            OutlinedButton(
                                onClick = {
                                    es10xMssEditor = ESIM_HIGH_EFFICIENCY_ES10X_MSS.toString()
                                    updateEsimSettings(esimSettings.copy(es10xMss = ESIM_HIGH_EFFICIENCY_ES10X_MSS))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.setting_es10x_mss_efficient))
                            }
                        }
                        OutlinedTextField(
                            value = es10xMssEditor,
                            onValueChange = { raw ->
                                es10xMssEditor = raw.filter(Char::isDigit).take(3)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.setting_es10x_mss_custom)) },
                            supportingText = {
                                Text(stringResource(R.string.setting_es10x_mss_custom_summary))
                            },
                            isError = parsedEs10xMss == null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedButton(
                            onClick = {
                                parsedEs10xMss?.let { value ->
                                    updateEsimSettings(esimSettings.copy(es10xMss = value))
                                }
                            },
                            enabled = parsedEs10xMss != null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.action_save_es10x_mss))
                        }
                        OutlinedTextField(
                            value = customAidListEditor,
                            onValueChange = { customAidListEditor = it.uppercase() },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.setting_custom_isdr_aid_list)) },
                            supportingText = {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(stringResource(R.string.setting_custom_isdr_aid_list_summary))
                                    Text(
                                        stringResource(
                                            R.string.format_custom_isdr_aid_summary,
                                            aidSummary.effectiveAids.size,
                                            aidSummary.invalidLines.size
                                        )
                                    )
                                }
                            },
                            isError = aidSummary.invalidLines.isNotEmpty(),
                            minLines = 3
                        )
                        if (aidSummary.effectiveAidLabels.isNotEmpty()) {
                            Text(
                                text = stringResource(
                                    R.string.format_effective_isdr_aid_list,
                                    aidSummary.effectiveAidLabels.joinToString()
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                updateEsimSettings(esimSettings.copy(customIsdrAidList = customAidListEditor.trim()))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.action_save))
                        }
                        OutlinedButton(
                            onClick = {
                                customAidListEditor = ""
                                updateEsimSettings(esimSettings.copy(customIsdrAidList = ""))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.action_reset_isdr_aid_list))
                        }
                        Text(
                            text = stringResource(R.string.message_diagnostics_privacy),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = {
                                resultStore.clearHistory()
                                statusMessage = context.getString(R.string.message_esim_operation_history_cleared)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.action_clear_esim_operation_history))
                        }
                    }
                }
            }

            if (statusMessage.isNotBlank()) {
                item {
                    M3ePanel(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                item {
                    Spacer(modifier = Modifier.height(1.dp))
                }
            }
        }
    }
}
