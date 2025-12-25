package takagi.ru.paysage.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import takagi.ru.paysage.R
import takagi.ru.paysage.data.model.SyncOptions
import takagi.ru.paysage.data.model.SyncType
import takagi.ru.paysage.repository.SyncOptionsRepository

/**
 * 记住同步选项的 Composable 函数
 */
@Composable
fun rememberSyncOptions(): State<SyncOptions> {
    val context = LocalContext.current
    val repository = remember { SyncOptionsRepository(context) }
    return repository.syncOptionsFlow.collectAsState(initial = SyncOptions())
}

/**
 * M3E 风格的书库同步 Bottom Sheet
 * 简洁、现代、Expressive
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySyncBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSync: (SyncType, SyncOptions) -> Unit,
    isSyncing: Boolean = false,
    syncProgress: Float = 0f,
    syncMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { SyncOptionsRepository(context) }
    val syncOptions by repository.syncOptionsFlow.collectAsState(initial = SyncOptions())
    
    // 选项状态
    var removeDeletedFiles by remember { mutableStateOf(syncOptions.removeDeletedFiles) }
    var generateMissingThumbnails by remember { mutableStateOf(syncOptions.generateMissingThumbnails) }
    var parallelSync by remember { mutableStateOf(syncOptions.parallelSync) }
    
    // 高级选项展开状态
    var showAdvancedOptions by remember { mutableStateOf(false) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题
            Text(
                text = "同步书库",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 副标题/状态
            Text(
                text = if (isSyncing) (syncMessage ?: "正在扫描文件...") else "扫描书源文件夹中的新书籍",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 同步进度指示器 (同步中显示)
            AnimatedVisibility(
                visible = isSyncing,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (syncProgress > 0f) {
                        LinearProgressIndicator(
                            progress = { syncProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(MaterialTheme.shapes.small),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(MaterialTheme.shapes.small),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            // 主同步按钮 - M3E Expressive
            Button(
                onClick = {
                    val options = SyncOptions(
                        removeDeletedFiles = removeDeletedFiles,
                        updateModifiedFiles = true,
                        generateMissingThumbnails = generateMissingThumbnails,
                        scanSubfolders = true,
                        skipHiddenFolders = true,
                        parallelSync = parallelSync
                    )
                    onSync(SyncType.FULL, options)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isSyncing,
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("同步中...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "开始同步",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 高级选项折叠区
            SyncAdvancedOptionsSection(
                expanded = showAdvancedOptions,
                onExpandToggle = { showAdvancedOptions = !showAdvancedOptions },
                removeDeletedFiles = removeDeletedFiles,
                generateMissingThumbnails = generateMissingThumbnails,
                parallelSync = parallelSync,
                onRemoveDeletedFilesChange = { removeDeletedFiles = it },
                onGenerateMissingThumbnailsChange = { generateMissingThumbnails = it },
                onParallelSyncChange = { parallelSync = it },
                enabled = !isSyncing
            )
        }
    }
}

/**
 * 高级选项折叠区 - M3E 风格
 */
@Composable
private fun SyncAdvancedOptionsSection(
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    removeDeletedFiles: Boolean,
    generateMissingThumbnails: Boolean,
    parallelSync: Boolean,
    onRemoveDeletedFilesChange: (Boolean) -> Unit,
    onGenerateMissingThumbnailsChange: (Boolean) -> Unit,
    onParallelSyncChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(300),
        label = "expandIconRotation"
    )
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(300))
    ) {
        // 折叠头部
        Surface(
            onClick = onExpandToggle,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "高级选项",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotationAngle)
                )
            }
        }
        
        // 展开内容
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(200)) + expandVertically(tween(300)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(300))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 清理无效书籍
                SyncOptionItem(
                    icon = Icons.Outlined.AutoDelete,
                    title = "清理无效书籍",
                    description = "移除已删除文件的记录",
                    checked = removeDeletedFiles,
                    onCheckedChange = onRemoveDeletedFilesChange,
                    enabled = enabled
                )
                
                // 生成缺失封面
                SyncOptionItem(
                    icon = Icons.Outlined.BrokenImage,
                    title = "生成缺失封面",
                    description = "为没有封面的书籍生成缩略图",
                    checked = generateMissingThumbnails,
                    onCheckedChange = onGenerateMissingThumbnailsChange,
                    enabled = enabled
                )
                
                // 并行扫描
                SyncOptionItem(
                    icon = Icons.Outlined.Speed,
                    title = "并行扫描",
                    description = "更快的扫描速度，消耗更多资源",
                    checked = parallelSync,
                    onCheckedChange = onParallelSyncChange,
                    enabled = enabled
                )
            }
        }
    }
}

/**
 * 单个同步选项 - M3E 开关样式
 */
@Composable
private fun SyncOptionItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val alpha = if (enabled) 1f else 0.5f
    
    Surface(
        onClick = { if (enabled) onCheckedChange(!checked) },
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha),
        shape = MaterialTheme.shapes.medium,
        color = if (checked) 
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
        else 
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Switch(
                checked = checked,
                onCheckedChange = if (enabled) onCheckedChange else null,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

// ============================================================
// 保留旧的 Dialog 版本以保持兼容性（可在迁移完成后删除）
// ============================================================

/**
 * 书库同步对话框 (保留兼容)
 */
@Composable
fun LibrarySyncDialog(
    onDismiss: () -> Unit,
    onSync: (SyncType, SyncOptions) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { SyncOptionsRepository(context) }
    val syncOptions by repository.syncOptionsFlow.collectAsState(initial = SyncOptions())
    
    var removeDeletedFiles by remember { mutableStateOf(syncOptions.removeDeletedFiles) }
    var updateModifiedFiles by remember { mutableStateOf(syncOptions.updateModifiedFiles) }
    var generateMissingThumbnails by remember { mutableStateOf(syncOptions.generateMissingThumbnails) }
    var parallelSync by remember { mutableStateOf(syncOptions.parallelSync) }
    
    var showAdvanced by remember { mutableStateOf(false) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 400.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题
                Text(
                    text = stringResource(R.string.library_sync_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "扫描书源文件夹中的新书籍",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 主同步按钮
                Button(
                    onClick = {
                        val options = SyncOptions(
                            removeDeletedFiles = removeDeletedFiles,
                            updateModifiedFiles = updateModifiedFiles,
                            generateMissingThumbnails = generateMissingThumbnails,
                            scanSubfolders = true,
                            skipHiddenFolders = true,
                            parallelSync = parallelSync
                        )
                        onSync(SyncType.FULL, options)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "开始同步",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 高级选项
                SyncAdvancedOptionsSection(
                    expanded = showAdvanced,
                    onExpandToggle = { showAdvanced = !showAdvanced },
                    removeDeletedFiles = removeDeletedFiles,
                    generateMissingThumbnails = generateMissingThumbnails,
                    parallelSync = parallelSync,
                    onRemoveDeletedFilesChange = { removeDeletedFiles = it },
                    onGenerateMissingThumbnailsChange = { generateMissingThumbnails = it },
                    onParallelSyncChange = { parallelSync = it }
                )
            }
        }
    }
}
