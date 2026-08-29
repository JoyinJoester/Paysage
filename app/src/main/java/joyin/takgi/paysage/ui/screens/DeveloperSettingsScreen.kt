package joyin.takgi.paysage.ui.screens

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import joyin.takgi.paysage.R
import joyin.takgi.paysage.debug.DeveloperLogExport
import joyin.takgi.paysage.debug.DeveloperLogExporter
import joyin.takgi.paysage.ui.components.M3eActionButton
import joyin.takgi.paysage.ui.components.M3ePanel
import joyin.takgi.paysage.ui.components.M3eTopBar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun DeveloperSettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var latestExport by remember { mutableStateOf<DeveloperLogExport?>(null) }
    var statusMessage by remember { mutableStateOf("") }
    var isBusy by remember { mutableStateOf(false) }

    fun collectAndShare() {
        scope.launch {
            isBusy = true
            try {
                val export = DeveloperLogExporter.collect(context)
                latestExport = export
                val share = DeveloperLogExporter.createShareIntent(context, export.report)
                val chooser = Intent.createChooser(
                    share.intent,
                    context.getString(R.string.developer_log_share_title)
                ).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val shareStartResult = runCatching { context.startActivity(chooser) }
                statusMessage = shareStartResult.fold(
                    onSuccess = {
                        val baseMessage = if (share.warning == null) {
                            context.getString(R.string.developer_log_export_ready)
                        } else {
                            context.getString(R.string.developer_log_export_ready_with_warning, share.warning)
                        }
                        share.externalCopyPath?.let { path ->
                            context.getString(R.string.developer_log_export_ready_with_path, baseMessage, path)
                        } ?: baseMessage
                    },
                    onFailure = { shareError ->
                        Log.e("DeveloperSettings", "Developer log report generated but share failed", shareError)
                        val baseMessage = context.getString(
                            R.string.developer_log_share_failed_after_export,
                            shareError.safeDescription()
                        )
                        share.externalCopyPath?.let { path ->
                            context.getString(R.string.developer_log_export_ready_with_path, baseMessage, path)
                        } ?: baseMessage
                    }
                )
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Log.e("DeveloperSettings", "Failed to export developer logs", error)
                statusMessage = context.getString(
                    R.string.developer_log_export_failed_with_reason,
                    error.safeDescription()
                )
            } finally {
                isBusy = false
            }
        }
    }

    fun clearLogs() {
        scope.launch {
            isBusy = true
            statusMessage = if (DeveloperLogExporter.clearLogcat(context)) {
                context.getString(R.string.developer_log_buffer_cleared)
            } else {
                context.getString(R.string.developer_log_buffer_clear_failed)
            }
            isBusy = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            M3eTopBar(
                title = stringResource(R.string.screen_developer_settings_title),
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
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
        ) {
            item {
                M3ePanel(modifier = Modifier.fillMaxWidth(), prominent = true) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.BugReport, contentDescription = null)
                            Text(
                                text = stringResource(R.string.developer_log_export_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = stringResource(R.string.developer_log_export_summary),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        M3eActionButton(
                            text = if (isBusy) {
                                stringResource(R.string.status_processing)
                            } else {
                                stringResource(R.string.developer_log_export_action)
                            },
                            onClick = ::collectAndShare,
                            enabled = !isBusy,
                            prominent = true,
                            icon = Icons.Default.Share,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                M3ePanel(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.developer_log_snapshot_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val export = latestExport
                        Text(
                            text = if (export == null) {
                                stringResource(R.string.developer_log_snapshot_empty)
                            } else {
                                stringResource(
                                    R.string.developer_log_snapshot_format,
                                    export.lineCount,
                                    export.errorCount,
                                    export.warningCount
                                )
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        M3eActionButton(
                            text = stringResource(R.string.developer_log_clear_action),
                            onClick = ::clearLogs,
                            enabled = !isBusy,
                            icon = Icons.Default.DeleteSweep,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (statusMessage.isNotBlank()) {
                item {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

private fun Throwable.safeDescription(): String {
    val message = message?.takeIf { it.isNotBlank() }
    return if (message == null) {
        javaClass.simpleName
    } else {
        "${javaClass.simpleName}: ${message.take(180)}"
    }
}
