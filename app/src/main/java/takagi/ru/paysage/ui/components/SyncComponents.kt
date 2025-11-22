package takagi.ru.paysage.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.Indication
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import takagi.ru.paysage.R
import takagi.ru.paysage.data.model.SyncOptions
import takagi.ru.paysage.data.model.SyncType
import takagi.ru.paysage.repository.SyncOptionsRepository
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow

/**
 * 记住同步选项的 Composable 函数
 * 从 DataStore 中读取并观察同步选项的变化
 * 
 * @return 同步选项的 State，当 DataStore 中的值变化时会自动更新
 */
@Composable
fun rememberSyncOptions(): State<SyncOptions> {
    val context = LocalContext.current
    val repository = remember { SyncOptionsRepository(context) }
    return repository.syncOptionsFlow.collectAsState(initial = SyncOptions())
}

/**
 * 书库同步对话框
 * 提供同步选项配置和操作按钮的模态对话框
 * 
 * @param onDismiss 对话框关闭回调
 * @param onSync 同步操作回调，接收同步类型和选项
 * @param modifier 修饰符
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
    
    // 当前对话框中的选项状态
    var removeDeletedFiles by remember { mutableStateOf(syncOptions.removeDeletedFiles) }
    var updateModifiedFiles by remember { mutableStateOf(syncOptions.updateModifiedFiles) }
    var generateMissingThumbnails by remember { mutableStateOf(syncOptions.generateMissingThumbnails) }
    var scanSubfolders by remember { mutableStateOf(syncOptions.scanSubfolders) }
    var skipHiddenFolders by remember { mutableStateOf(syncOptions.skipHiddenFolders) }
    var parallelSync by remember { mutableStateOf(syncOptions.parallelSync) }
    
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
                .fillMaxWidth(0.8f)
                .widthIn(max = 600.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = stringResource(R.string.library_sync_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // File Type Card
                FileTypeCard()
                
                // Maintenance Card
                MaintenanceCard(
                    removeDeletedFiles = removeDeletedFiles,
                    updateModifiedFiles = updateModifiedFiles,
                    generateMissingThumbnails = generateMissingThumbnails,
                    onRemoveDeletedFilesChange = { removeDeletedFiles = it },
                    onUpdateModifiedFilesChange = { updateModifiedFiles = it },
                    onGenerateMissingThumbnailsChange = { generateMissingThumbnails = it }
                )
                
                // More Options Card
                MoreOptionsCard(
                    scanSubfolders = scanSubfolders,
                    skipHiddenFolders = skipHiddenFolders,
                    parallelSync = parallelSync,
                    onScanSubfoldersChange = { scanSubfolders = it },
                    onSkipHiddenFoldersChange = { skipHiddenFolders = it },
                    onParallelSyncChange = { parallelSync = it }
                )
                
                // Action Buttons
                ActionButtons(
                    onFullSync = {
                        val options = SyncOptions(
                            removeDeletedFiles = removeDeletedFiles,
                            updateModifiedFiles = updateModifiedFiles,
                            generateMissingThumbnails = generateMissingThumbnails,
                            scanSubfolders = scanSubfolders,
                            skipHiddenFolders = skipHiddenFolders,
                            parallelSync = parallelSync
                        )
                        onSync(SyncType.FULL, options)
                        onDismiss()
                    },
                    onMaintenance = {
                        val options = SyncOptions(
                            removeDeletedFiles = removeDeletedFiles,
                            updateModifiedFiles = updateModifiedFiles,
                            generateMissingThumbnails = generateMissingThumbnails,
                            scanSubfolders = scanSubfolders,
                            skipHiddenFolders = skipHiddenFolders,
                            parallelSync = parallelSync
                        )
                        onSync(SyncType.MAINTENANCE, options)
                        onDismiss()
                    },
                    onSync = {
                        val options = SyncOptions(
                            removeDeletedFiles = removeDeletedFiles,
                            updateModifiedFiles = updateModifiedFiles,
                            generateMissingThumbnails = generateMissingThumbnails,
                            scanSubfolders = scanSubfolders,
                            skipHiddenFolders = skipHiddenFolders,
                            parallelSync = parallelSync
                        )
                        onSync(SyncType.INCREMENTAL, options)
                        onDismiss()
                    }
                )
            }
        }
    }
}

/**
 * 文件类型卡片
 * 显示支持的漫画文件和压缩文件格式
 * 
 * @param modifier 修饰符
 */
@Composable
fun FileTypeCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.file_types),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Comic Files Section
            FileTypeSection(
                title = stringResource(R.string.comic_files),
                formats = listOf(".cbz", ".cbr", ".cbt", ".cb7")
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Archive Files Section
            FileTypeSection(
                title = stringResource(R.string.archive_files),
                formats = listOf(".zip", ".rar", ".7z", ".tar")
            )
        }
    }
}

/**
 * 文件类型区块
 * 显示特定类型的文件格式列表
 * 
 * @param title 区块标题
 * @param formats 文件格式列表
 * @param modifier 修饰符
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FileTypeSection(
    title: String,
    formats: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            formats.forEach { format ->
                AssistChip(
                    onClick = { },
                    label = { Text(format) },
                    enabled = false,
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        disabledLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
        }
    }
}

/**
 * 同步选项复选框
 * 带波纹效果的复选框组件，支持点击整行切换状态
 * 
 * @param checked 复选框选中状态
 * @param onCheckedChange 状态变化回调
 * @param label 复选框标签文本
 * @param description 复选框描述文本（可选）
 * @param modifier 修饰符
 */
@Composable
fun SyncCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    description: String? = null,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    // 动画状态
    val checkboxAlpha by animateFloatAsState(
        targetValue = if (checked) 1f else 0.6f,
        animationSpec = tween(durationMillis = 300),
        label = "checkboxAlpha"
    )
    
    val textColor by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 300),
        label = "textColor"
    )
    
    // 无障碍语义
    val stateDesc = if (checked) "已选中" else "未选中"
    val contentDesc = buildString {
        append(label)
        if (description != null) {
            append(", ")
            append(description)
        }
        append(", ")
        append(stateDesc)
    }
    
    val ripple = rememberRipple()
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = ripple,
                role = Role.Checkbox,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(vertical = 8.dp)
            .semantics {
                this.contentDescription = contentDesc
                this.stateDescription = stateDesc
                this.role = Role.Checkbox
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier
                .size(48.dp)
                .alpha(checkboxAlpha),
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
            
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * 维护选项卡片
 * 显示维护相关的同步选项
 * 
 * @param removeDeletedFiles 移出已删除文件选项状态
 * @param updateModifiedFiles 更新修改文件选项状态
 * @param generateMissingThumbnails 生成缺失缩略图选项状态
 * @param onRemoveDeletedFilesChange 移出已删除文件状态变化回调
 * @param onUpdateModifiedFilesChange 更新修改文件状态变化回调
 * @param onGenerateMissingThumbnailsChange 生成缺失缩略图状态变化回调
 * @param modifier 修饰符
 */
@Composable
fun MaintenanceCard(
    removeDeletedFiles: Boolean,
    updateModifiedFiles: Boolean,
    generateMissingThumbnails: Boolean,
    onRemoveDeletedFilesChange: (Boolean) -> Unit,
    onUpdateModifiedFilesChange: (Boolean) -> Unit,
    onGenerateMissingThumbnailsChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.maintenance),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            SyncCheckbox(
                checked = removeDeletedFiles,
                onCheckedChange = onRemoveDeletedFilesChange,
                label = stringResource(R.string.remove_deleted_files),
                description = stringResource(R.string.remove_deleted_files_desc)
            )
            
            SyncCheckbox(
                checked = updateModifiedFiles,
                onCheckedChange = onUpdateModifiedFilesChange,
                label = stringResource(R.string.update_modified_files),
                description = stringResource(R.string.update_modified_files_desc)
            )
            
            SyncCheckbox(
                checked = generateMissingThumbnails,
                onCheckedChange = onGenerateMissingThumbnailsChange,
                label = stringResource(R.string.generate_missing_thumbnails),
                description = stringResource(R.string.generate_missing_thumbnails_desc)
            )
        }
    }
}

/**
 * 更多选项卡片
 * 显示额外的同步配置选项
 * 
 * @param scanSubfolders 扫描子文件夹选项状态
 * @param skipHiddenFolders 跳过隐藏文件夹选项状态
 * @param parallelSync 并行同步选项状态
 * @param onScanSubfoldersChange 扫描子文件夹状态变化回调
 * @param onSkipHiddenFoldersChange 跳过隐藏文件夹状态变化回调
 * @param onParallelSyncChange 并行同步状态变化回调
 * @param modifier 修饰符
 */
@Composable
fun MoreOptionsCard(
    scanSubfolders: Boolean,
    skipHiddenFolders: Boolean,
    parallelSync: Boolean,
    onScanSubfoldersChange: (Boolean) -> Unit,
    onSkipHiddenFoldersChange: (Boolean) -> Unit,
    onParallelSyncChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.more_options),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            SyncCheckbox(
                checked = scanSubfolders,
                onCheckedChange = onScanSubfoldersChange,
                label = stringResource(R.string.scan_subfolders),
                description = stringResource(R.string.scan_subfolders_desc)
            )
            
            SyncCheckbox(
                checked = skipHiddenFolders,
                onCheckedChange = onSkipHiddenFoldersChange,
                label = stringResource(R.string.skip_hidden_folders),
                description = stringResource(R.string.skip_hidden_folders_desc)
            )
            
            SyncCheckbox(
                checked = parallelSync,
                onCheckedChange = onParallelSyncChange,
                label = stringResource(R.string.parallel_sync),
                description = stringResource(R.string.parallel_sync_desc)
            )
        }
    }
}

/**
 * 同步操作按钮组
 * 提供完整同步、维护和增量同步三个操作按钮
 * 
 * @param onFullSync 完整同步按钮点击回调
 * @param onMaintenance 维护按钮点击回调
 * @param onSync 同步按钮点击回调
 * @param modifier 修饰符
 */
@Composable
fun ActionButtons(
    onFullSync: () -> Unit,
    onMaintenance: () -> Unit,
    onSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 开始完整同步按钮 - Primary
        Button(
            onClick = onFullSync,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .semantics {
                    contentDescription = "开始完整同步，重新扫描所有文件"
                    role = Role.Button
                },
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.start_full_sync),
                style = MaterialTheme.typography.labelLarge
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 维护按钮 - Outlined
            OutlinedButton(
                onClick = onMaintenance,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .semantics {
                        contentDescription = "执行维护操作，清理和更新书库"
                        role = Role.Button
                    },
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.maintenance),
                    style = MaterialTheme.typography.labelLarge
                )
            }
            
            // 同步按钮 - FilledTonal
            FilledTonalButton(
                onClick = onSync,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .semantics {
                        contentDescription = "增量同步，仅扫描新增和修改的文件"
                        role = Role.Button
                    },
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.sync),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
