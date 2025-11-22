package takagi.ru.paysage.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import takagi.ru.paysage.R

/**
 * 创建文件夹对话框
 */
@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (folderName: String) -> Unit,
    existingFolderNames: List<String> = emptyList(),
    isCreating: Boolean = false
) {
    var folderName by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    val validation = remember(folderName, existingFolderNames) {
        validateFolderName(folderName, existingFolderNames)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.create_folder),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text(stringResource(R.string.folder_name)) },
                    placeholder = { Text(stringResource(R.string.folder_name_hint)) },
                    isError = validation !is FolderNameValidation.Valid && folderName.isNotEmpty(),
                    supportingText = {
                        if (validation !is FolderNameValidation.Valid && folderName.isNotEmpty()) {
                            Text(
                                text = getValidationMessage(validation, context),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    singleLine = true,
                    enabled = !isCreating,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (isCreating) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = { onConfirm(folderName) },
                enabled = validation is FolderNameValidation.Valid && !isCreating
            ) {
                Text(stringResource(R.string.dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isCreating
            ) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    )
}

/**
 * 文件夹名称验证结果
 */
sealed class FolderNameValidation {
    object Valid : FolderNameValidation()
    object Empty : FolderNameValidation()
    object TooLong : FolderNameValidation()
    object InvalidChars : FolderNameValidation()
    object AlreadyExists : FolderNameValidation()
}

/**
 * 验证文件夹名称
 */
fun validateFolderName(
    name: String,
    existingNames: List<String>
): FolderNameValidation {
    return when {
        name.isBlank() -> FolderNameValidation.Empty
        name.length > 255 -> FolderNameValidation.TooLong
        name.containsIllegalChars() -> FolderNameValidation.InvalidChars
        name in existingNames -> FolderNameValidation.AlreadyExists
        else -> FolderNameValidation.Valid
    }
}

/**
 * 检查是否包含非法字符
 */
private fun String.containsIllegalChars(): Boolean {
    val illegalChars = listOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
    return any { it in illegalChars }
}

/**
 * 获取验证错误消息
 */
fun getValidationMessage(
    validation: FolderNameValidation,
    context: android.content.Context
): String {
    return when (validation) {
        is FolderNameValidation.Empty -> context.getString(R.string.folder_name_empty)
        is FolderNameValidation.TooLong -> context.getString(R.string.folder_name_too_long)
        is FolderNameValidation.InvalidChars -> context.getString(R.string.folder_name_invalid)
        is FolderNameValidation.AlreadyExists -> context.getString(R.string.folder_name_exists)
        is FolderNameValidation.Valid -> ""
    }
}


/**
 * 创建文件夹按钮
 */
@Composable
fun CreateFolderButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Icon(
            imageVector = Icons.Default.CreateNewFolder,
            contentDescription = stringResource(R.string.create_folder_desc),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.create_folder),
            style = MaterialTheme.typography.labelLarge
        )
    }
}


/**
 * 文件夹列表项
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderListItem(
    folder: takagi.ru.paysage.data.model.Folder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectionToggle: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Surface(
        onClick = if (isEditMode && onSelectionToggle != null) onSelectionToggle else onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.large,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 编辑模式下显示复选框
            if (isEditMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelectionToggle?.invoke() },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                },
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatDate(folder.createdAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 编辑模式下显示操作按钮
            if (isEditMode && !isSelected) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (onRename != null) {
                        IconButton(
                            onClick = onRename,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.rename_folder),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(40.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete_folder),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            } else if (!isEditMode) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 格式化日期
 */
private fun formatDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    val weeks = days / 7
    val months = days / 30
    val years = days / 365
    
    return when {
        seconds < 60 -> "刚刚"
        minutes < 60 -> "${minutes}分钟前"
        hours < 24 -> "${hours}小时前"
        days < 7 -> "${days}天前"
        weeks < 4 -> "${weeks}周前"
        months < 12 -> "${months}个月前"
        else -> "${years}年前"
    }
}
