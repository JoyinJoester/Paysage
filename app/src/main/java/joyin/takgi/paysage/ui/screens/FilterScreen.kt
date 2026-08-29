package joyin.takgi.paysage.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import joyin.takgi.paysage.R
import joyin.takgi.paysage.data.AppDatabase
import joyin.takgi.paysage.data.FilterRule
import joyin.takgi.paysage.data.FilterType
import joyin.takgi.paysage.repository.FilterRepository
import joyin.takgi.paysage.ui.components.M3eMainFab
import joyin.takgi.paysage.ui.components.M3ePanel
import joyin.takgi.paysage.ui.components.M3eTopBar
import joyin.takgi.paysage.ui.motion.PaysageAnimatedPage
import kotlinx.coroutines.launch

private data class FilterRuleTypeMeta(
    val type: FilterType,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val inputLabelRes: Int,
    @StringRes val inputHelpRes: Int
)

private val filterRuleTypes = listOf(
    FilterRuleTypeMeta(
        type = FilterType.BODY_KEYWORD_BLOCK,
        titleRes = R.string.filter_type_body_keyword_block,
        descriptionRes = R.string.filter_desc_body_keyword_block,
        inputLabelRes = R.string.label_filter_keyword,
        inputHelpRes = R.string.helper_filter_body_keyword_block
    ),
    FilterRuleTypeMeta(
        type = FilterType.BLACKLIST,
        titleRes = R.string.filter_type_blacklist,
        descriptionRes = R.string.filter_desc_blacklist,
        inputLabelRes = R.string.label_phone_number_short,
        inputHelpRes = R.string.helper_filter_sender_blacklist
    ),
    FilterRuleTypeMeta(
        type = FilterType.WHITELIST,
        titleRes = R.string.filter_type_whitelist,
        descriptionRes = R.string.filter_desc_whitelist,
        inputLabelRes = R.string.label_phone_number_short,
        inputHelpRes = R.string.helper_filter_sender_whitelist
    ),
    FilterRuleTypeMeta(
        type = FilterType.BODY_KEYWORD_ALLOW,
        titleRes = R.string.filter_type_body_keyword_allow,
        descriptionRes = R.string.filter_desc_body_keyword_allow,
        inputLabelRes = R.string.label_filter_keyword,
        inputHelpRes = R.string.helper_filter_keyword_allow
    ),
    FilterRuleTypeMeta(
        type = FilterType.BODY_REGEX_BLOCK,
        titleRes = R.string.filter_type_body_regex_block,
        descriptionRes = R.string.filter_desc_body_regex_block,
        inputLabelRes = R.string.label_filter_regex,
        inputHelpRes = R.string.helper_filter_body_regex_block
    )
)

private fun filterRuleTypeMeta(type: FilterType): FilterRuleTypeMeta {
    if (type == FilterType.KEYWORD) {
        return FilterRuleTypeMeta(
            type = FilterType.KEYWORD,
            titleRes = R.string.filter_type_keyword,
            descriptionRes = R.string.filter_desc_keyword,
            inputLabelRes = R.string.label_filter_keyword,
            inputHelpRes = R.string.helper_filter_body_keyword_block
        )
    }
    return filterRuleTypes.first { it.type == type }
}

private fun FilterType.usesBodyInput(): Boolean {
    return this == FilterType.BODY_KEYWORD_BLOCK ||
        this == FilterType.KEYWORD ||
        this == FilterType.BODY_KEYWORD_ALLOW ||
        this == FilterType.BODY_REGEX_BLOCK
}

private fun isValidRegex(pattern: String): Boolean {
    return runCatching { Regex(pattern) }.isSuccess
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { FilterRepository(AppDatabase.getDatabase(context).filterDao()) }
    val rules by repository.allRules.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var isAddingRule by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            M3eTopBar(
                title = if (isAddingRule) {
                    stringResource(R.string.title_add_filter_rule)
                } else {
                    stringResource(R.string.action_filter_rules)
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isAddingRule) {
                                isAddingRule = false
                            } else {
                                onBackClick()
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isAddingRule) {
                M3eMainFab(
                    expanded = false,
                    onClick = { isAddingRule = true },
                    collapsedIcon = Icons.Default.Add,
                    expandedIcon = Icons.Default.Add,
                    contentDescription = stringResource(R.string.title_add_filter_rule)
                )
            }
        }
    ) { padding ->
        PaysageAnimatedPage(
            targetState = isAddingRule,
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopStart,
            isForward = { initial, target -> !initial && target }
        ) { adding ->
            if (adding) {
                AddRulePage(
                    modifier = Modifier.padding(padding),
                    onDismiss = { isAddingRule = false },
                    onAdd = { type, value ->
                        scope.launch {
                            repository.insert(FilterRule(type = type, value = value))
                        }
                        isAddingRule = false
                    }
                )
            } else if (rules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.message_no_filter_rules))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(rules) { rule ->
                        FilterRuleItem(
                            rule = rule,
                            onDelete = {
                                scope.launch {
                                    repository.delete(rule)
                                }
                            },
                            onToggle = {
                                scope.launch {
                                    repository.update(rule.copy(isEnabled = !rule.isEnabled))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterRuleItem(
    rule: FilterRule,
    onDelete: () -> Unit,
    onToggle: () -> Unit
) {
    val meta = filterRuleTypeMeta(rule.type)
    M3ePanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(meta.titleRes),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = stringResource(meta.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(text = rule.value, style = MaterialTheme.typography.bodyLarge)
            }

            Row {
                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = { onToggle() }
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, stringResource(R.string.action_delete))
                }
            }
        }
    }
}

@Composable
fun AddRulePage(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onAdd: (FilterType, String) -> Unit
) {
    var selectedType by remember { mutableStateOf(FilterType.BODY_KEYWORD_BLOCK) }
    var value by remember { mutableStateOf("") }
    val trimmedValue = value.trim()
    val selectedMeta = filterRuleTypeMeta(selectedType)
    val hasRegexError = selectedType == FilterType.BODY_REGEX_BLOCK &&
        trimmedValue.isNotEmpty() &&
        !isValidRegex(trimmedValue)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.label_filter_rule_type),
            style = MaterialTheme.typography.titleMedium
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            filterRuleTypes.forEach { meta ->
                FilterRuleTypeOption(
                    meta = meta,
                    selected = selectedType == meta.type,
                    onClick = { selectedType = meta.type }
                )
            }
        }

        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = { Text(stringResource(selectedMeta.inputLabelRes)) },
            supportingText = {
                Text(
                    if (hasRegexError) {
                        stringResource(R.string.message_filter_regex_invalid)
                    } else {
                        stringResource(selectedMeta.inputHelpRes)
                    }
                )
            },
            isError = hasRegexError,
            minLines = if (selectedType.usesBodyInput()) 2 else 1,
            maxLines = if (selectedType.usesBodyInput()) 5 else 1,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.action_cancel))
            }
            Button(
                onClick = { onAdd(selectedType, trimmedValue) },
                enabled = trimmedValue.isNotEmpty() && !hasRegexError,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.action_add))
            }
        }
    }
}

@Composable
private fun FilterRuleTypeOption(
    meta: FilterRuleTypeMeta,
    selected: Boolean,
    onClick: () -> Unit
) {
    M3ePanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        contentPadding = PaddingValues(14.dp),
        prominent = selected,
        elevated = selected
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(meta.titleRes),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = stringResource(meta.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}
