package joyin.takgi.paysage.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
    val context = LocalContext.current
    M3ePanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (rule.type) {
                        FilterType.WHITELIST -> context.getString(R.string.filter_type_whitelist)
                        FilterType.BLACKLIST -> context.getString(R.string.filter_type_blacklist)
                        FilterType.KEYWORD -> context.getString(R.string.filter_type_keyword)
                    },
                    style = MaterialTheme.typography.labelSmall
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
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(FilterType.WHITELIST) }
    var value by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedType == FilterType.WHITELIST,
                onClick = { selectedType = FilterType.WHITELIST },
                label = { Text(stringResource(R.string.filter_type_whitelist)) }
            )
            FilterChip(
                selected = selectedType == FilterType.BLACKLIST,
                onClick = { selectedType = FilterType.BLACKLIST },
                label = { Text(stringResource(R.string.filter_type_blacklist)) }
            )
            FilterChip(
                selected = selectedType == FilterType.KEYWORD,
                onClick = { selectedType = FilterType.KEYWORD },
                label = { Text(stringResource(R.string.filter_type_keyword)) }
            )
        }

        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = {
                Text(
                    when (selectedType) {
                        FilterType.KEYWORD -> context.getString(R.string.filter_type_keyword)
                        else -> context.getString(R.string.label_phone_number_short)
                    }
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

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
                onClick = { onAdd(selectedType, value) },
                enabled = value.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.action_add))
            }
        }
    }
}
