package takagi.ru.paysage.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import takagi.ru.paysage.R
import takagi.ru.paysage.data.model.BookSource
import takagi.ru.paysage.data.model.CategoryType
import takagi.ru.paysage.ui.theme.ExpressiveShapes

/**
 * 添加书源对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSourceDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (BookSource) -> Unit,
    initialCategoryType: CategoryType = CategoryType.MANGA,
    editingSource: BookSource? = null
) {
    if (!isVisible) return
    
    // 表单状态
    var name by remember { mutableStateOf(editingSource?.name ?: "") }
    var baseUrl by remember { mutableStateOf(editingSource?.baseUrl ?: "") }
    var categoryType by remember { mutableStateOf(editingSource?.categoryType ?: initialCategoryType) }
    var priority by remember { mutableStateOf(editingSource?.priority?.toString() ?: "0") }
    var isEnabled by remember { mutableStateOf(editingSource?.isEnabled ?: true) }
    
    // 验证状态
    var nameError by remember { mutableStateOf<String?>(null) }
    var urlError by remember { mutableStateOf<String?>(null) }
    var priorityError by remember { mutableStateOf<String?>(null) }
    
    // 验证函数
    fun validateForm(): Boolean {
        var isValid = true
        
        // 验证名称
        if (name.isBlank()) {
            nameError = "请输入书源名称"
            isValid = false
        } else {
            nameError = null
        }
        
        // 验证URL
        if (baseUrl.isBlank()) {
            urlError = "请输入书源地址"
            isValid = false
        } else if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            urlError = "请输入有效的URL地址"
            isValid = false
        } else {
            urlError = null
        }
        
        // 验证优先级
        val priorityInt = priority.toIntOrNull()
        if (priorityInt == null || priorityInt < 0) {
            priorityError = "请输入有效的优先级（0或正整数）"
            isValid = false
        } else {
            priorityError = null
        }
        
        return isValid
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = takagi.ru.paysage.ui.theme.CategoryShapes.dialog,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (editingSource != null) "编辑书源" else stringResource(R.string.add_source),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    ExpressiveIconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭"
                        )
                    }
                }
                
                // 书源名称
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        nameError = null
                    },
                    label = { Text(stringResource(R.string.source_name)) },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // 书源地址
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { 
                        baseUrl = it
                        urlError = null
                    },
                    label = { Text(stringResource(R.string.source_url)) },
                    isError = urlError != null,
                    supportingText = urlError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    singleLine = true,
                    placeholder = { Text("https://example.com") }
                )
                
                // 分类类型选择
                Column {
                    Text(
                        text = "分类类型",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = categoryType == CategoryType.MANGA,
                            onClick = { categoryType = CategoryType.MANGA },
                            label = { Text(stringResource(R.string.category_manga)) },
                            modifier = Modifier.weight(1f)
                        )
                        
                        FilterChip(
                            selected = categoryType == CategoryType.NOVEL,
                            onClick = { categoryType = CategoryType.NOVEL },
                            label = { Text(stringResource(R.string.category_novel)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // 优先级
                OutlinedTextField(
                    value = priority,
                    onValueChange = { 
                        priority = it
                        priorityError = null
                    },
                    label = { Text(stringResource(R.string.source_priority)) },
                    isError = priorityError != null,
                    supportingText = priorityError?.let { { Text(it) } } ?: {
                        Text("数字越大优先级越高，0为默认优先级")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                
                // 启用状态
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "启用状态",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (isEnabled) "书源已启用" else "书源已禁用",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it }
                    )
                }
                
                // 按钮行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    
                    Button(
                        onClick = {
                            if (validateForm()) {
                                val source = BookSource(
                                    id = editingSource?.id ?: 0,
                                    name = name.trim(),
                                    baseUrl = baseUrl.trim(),
                                    categoryType = categoryType,
                                    isEnabled = isEnabled,
                                    priority = priority.toInt(),
                                    addedAt = editingSource?.addedAt ?: System.currentTimeMillis()
                                )
                                onConfirm(source)
                            }
                        }
                    ) {
                        Text(if (editingSource != null) "保存" else "添加")
                    }
                }
            }
        }
    }
}
