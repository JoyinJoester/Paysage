package joyin.takgi.paysage.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import joyin.takgi.paysage.R
import joyin.takgi.paysage.reliability.root.RootKeepAliveScript
import joyin.takgi.paysage.reliability.root.RootPrivilegeManager
import joyin.takgi.paysage.reliability.root.RootSmsFallback
import joyin.takgi.paysage.reliability.root.RootSettingsStore
import joyin.takgi.paysage.reliability.root.RootShell
import joyin.takgi.paysage.ui.components.M3eActionButton
import joyin.takgi.paysage.ui.components.M3ePanel
import joyin.takgi.paysage.ui.components.M3eSettingsSwitchRow
import joyin.takgi.paysage.ui.components.M3eTopBar
import kotlinx.coroutines.launch

@Composable
fun RootModeScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { RootSettingsStore(context) }

    var rootAvailable by remember { mutableStateOf<Boolean?>(null) }
    var keepAliveEnabled by remember { mutableStateOf(store.keepAliveScriptEnabled) }
    var smsFallbackEnabled by remember { mutableStateOf(store.smsFallbackEnabled) }
    var busy by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        busy = true
        rootAvailable = RootShell.isAvailable()
        busy = false
    }

    fun requireRoot(): Boolean {
        if (rootAvailable == false) {
            statusMessage = context.getString(R.string.message_root_not_available)
            return false
        }
        return true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            M3eTopBar(
                title = stringResource(R.string.screen_root_mode_title),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            M3ePanel(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.title_root_status),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = when (rootAvailable) {
                            null -> stringResource(R.string.status_root_checking)
                            true -> stringResource(R.string.status_root_available)
                            false -> stringResource(R.string.status_root_unavailable)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.summary_root_mode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            M3ePanel(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    M3eActionButton(
                        text = stringResource(R.string.action_root_grant_all),
                        enabled = !busy && rootAvailable == true,
                        onClick = {
                            scope.launch {
                                busy = true
                                val (succeeded, failures) = RootPrivilegeManager.grantAll(context)
                                busy = false
                                statusMessage = if (failures.isEmpty()) {
                                    context.getString(R.string.message_root_grant_success)
                                } else {
                                    context.getString(
                                        R.string.format_root_grant_partial,
                                        succeeded,
                                        failures.size
                                    )
                                }
                            }
                        }
                    )
                    Text(
                        text = stringResource(R.string.summary_root_grant_all),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    M3eSettingsSwitchRow(
                        title = stringResource(R.string.title_root_keep_alive),
                        summary = stringResource(R.string.summary_root_keep_alive),
                        checked = keepAliveEnabled,
                        enabled = !busy && rootAvailable == true,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                busy = true
                                val result = if (enabled) {
                                    RootKeepAliveScript.install(context)
                                } else {
                                    RootKeepAliveScript.remove()
                                }
                                busy = false
                                if (result.success || !enabled) {
                                    keepAliveEnabled = enabled
                                    store.keepAliveScriptEnabled = enabled
                                    statusMessage = context.getString(
                                        if (enabled) {
                                            R.string.message_root_keep_alive_installed
                                        } else {
                                            R.string.message_root_keep_alive_removed
                                        }
                                    )
                                } else {
                                    statusMessage = context.getString(R.string.message_root_operation_failed)
                                }
                            }
                        }
                    )

                    M3eSettingsSwitchRow(
                        title = stringResource(R.string.title_root_sms_fallback),
                        summary = stringResource(R.string.summary_root_sms_fallback),
                        checked = smsFallbackEnabled,
                        enabled = !busy && rootAvailable == true,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                busy = true
                                store.smsFallbackEnabled = enabled
                                smsFallbackEnabled = enabled
                                if (enabled) {
                                    RootSmsFallback.check(context)
                                }
                                busy = false
                                statusMessage = context.getString(R.string.message_root_settings_saved)
                            }
                        }
                    )
                }
            }

            if (statusMessage.isNotBlank()) {
                M3ePanel(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
