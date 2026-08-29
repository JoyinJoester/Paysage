package joyin.takgi.paysage.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import joyin.takgi.paysage.R
import joyin.takgi.paysage.data.AppDatabase
import joyin.takgi.paysage.data.FilterRule
import joyin.takgi.paysage.data.FilterType
import joyin.takgi.paysage.data.ForwardLog
import joyin.takgi.paysage.ui.components.M3eActionButton
import joyin.takgi.paysage.ui.components.M3ePanel
import joyin.takgi.paysage.ui.components.M3eTopBar
import joyin.takgi.paysage.util.FilterWordTokenizer
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 转发日志详情:查看完整内容,并把号码/正文中的词一键加入过滤规则。
 * 点词选中(可多选),每个选中词各生成一条关键词屏蔽规则。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogDetailScreen(
    logId: Int,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val logDao = db.forwardLogDao()
    val filterRepository = remember { joyin.takgi.paysage.repository.FilterRepository(db.filterDao()) }

    var log by remember { mutableStateOf<ForwardLog?>(null) }
    var selectedWords by remember { mutableStateOf(setOf<String>()) }
    var statusMessage by remember { mutableStateOf("") }

    LaunchedEffect(logId) {
        log = logDao.getById(logId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            M3eTopBar(
                title = stringResource(R.string.screen_log_detail_title),
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
        val currentLog = log
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (currentLog == null) {
                Text(
                    text = stringResource(R.string.message_log_not_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            M3ePanel(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = currentLog.sender,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = formatLogTime(currentLog.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = currentLog.content,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LogStatusChips(currentLog)
                    }
                }
            }

            M3ePanel(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.title_add_sender_filter),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        M3eActionButton(
                            text = stringResource(R.string.action_add_sender_blacklist),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                scope.launch {
                                    statusMessage = addFilterRule(
                                        filterRepository,
                                        FilterType.BLACKLIST,
                                        currentLog.sender.trim(),
                                        context.getString(R.string.message_filter_added)
                                    )
                                }
                            }
                        )
                        M3eActionButton(
                            text = stringResource(R.string.action_add_sender_whitelist),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                scope.launch {
                                    statusMessage = addFilterRule(
                                        filterRepository,
                                        FilterType.WHITELIST,
                                        currentLog.sender.trim(),
                                        context.getString(R.string.message_filter_added)
                                    )
                                }
                            }
                        )
                    }

                    Text(
                        text = stringResource(R.string.title_pick_words_to_filter),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.summary_pick_words_to_filter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val candidateWords = remember(currentLog.content) {
                        FilterWordTokenizer.tokenize(currentLog.content)
                    }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        candidateWords.forEach { word ->
                            FilterChip(
                                selected = word in selectedWords,
                                onClick = {
                                    selectedWords = if (word in selectedWords) {
                                        selectedWords - word
                                    } else {
                                        selectedWords + word
                                    }
                                },
                                label = { Text(word) }
                            )
                        }
                    }
                    M3eActionButton(
                        text = if (selectedWords.isEmpty()) {
                            stringResource(R.string.action_add_keywords_empty)
                        } else {
                            stringResource(R.string.format_add_keywords, selectedWords.size)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedWords.isNotEmpty(),
                        onClick = {
                            scope.launch {
                                var added = 0
                                selectedWords.forEach { word ->
                                    if (addFilterRuleQuietly(filterRepository, FilterType.BODY_KEYWORD_BLOCK, word)) {
                                        added += 1
                                    }
                                }
                                selectedWords = emptySet()
                                statusMessage = context.getString(R.string.format_keywords_added, added)
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

@Composable
private fun LogStatusChips(log: ForwardLog) {
    val context = LocalContext.current
    if (log.filtered) {
        AssistChip(onClick = {}, label = { Text(context.getString(R.string.chip_filtered)) })
    } else {
        if (log.emailSuccess) {
            AssistChip(onClick = {}, label = { Text(context.getString(R.string.chip_email)) })
        }
        if (log.telegramSuccess) {
            AssistChip(onClick = {}, label = { Text("Telegram") })
        }
        if (!log.emailSuccess && !log.telegramSuccess) {
            AssistChip(onClick = {}, label = { Text(context.getString(R.string.chip_forward_failed)) })
        }
    }
}

private suspend fun addFilterRule(
    repository: joyin.takgi.paysage.repository.FilterRepository,
    type: FilterType,
    value: String,
    successMessage: String
): String {
    if (value.isBlank()) return ""
    return if (addFilterRuleQuietly(repository, type, value)) {
        successMessage
    } else {
        ""
    }
}

/** 已存在同类型同值规则时跳过,返回是否真正新增 */
private suspend fun addFilterRuleQuietly(
    repository: joyin.takgi.paysage.repository.FilterRepository,
    type: FilterType,
    value: String
): Boolean {
    if (repository.exists(type, value)) return false
    repository.insert(FilterRule(type = type, value = value))
    return true
}

private fun formatLogTime(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
