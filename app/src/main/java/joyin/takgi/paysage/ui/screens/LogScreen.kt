package joyin.takgi.paysage.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import joyin.takgi.paysage.R
import joyin.takgi.paysage.data.AppDatabase
import joyin.takgi.paysage.data.ForwardLog
import joyin.takgi.paysage.ui.components.M3ePanel
import joyin.takgi.paysage.ui.components.M3eTopBar
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val logDao = remember { AppDatabase.getDatabase(context).forwardLogDao() }
    val logs by logDao.getRecent().collectAsState(initial = emptyList())

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            M3eTopBar(
                title = stringResource(R.string.action_forward_logs),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(stringResource(R.string.message_no_forward_records))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(logs) { log ->
                    LogItem(log)
                }
            }
        }
    }
}

@Composable
fun LogItem(log: ForwardLog) {
    val context = LocalContext.current
    M3ePanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = log.sender,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = formatTime(log.timestamp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = log.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (log.filtered) {
                    AssistChip(
                        onClick = {},
                        label = { Text(context.getString(R.string.chip_filtered)) }
                    )
                } else {
                    if (log.emailSuccess) {
                        AssistChip(
                            onClick = {},
                            label = { Text(context.getString(R.string.chip_email)) }
                        )
                    }
                    if (log.telegramSuccess) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Telegram") }
                        )
                    }
                    if (!log.emailSuccess && !log.telegramSuccess) {
                        AssistChip(
                            onClick = {},
                            label = { Text(context.getString(R.string.chip_forward_failed)) }
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
