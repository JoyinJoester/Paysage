package takagi.ru.paysage.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import takagi.ru.paysage.data.model.Folder
import takagi.ru.paysage.ui.theme.ExpressiveAnimations

/**
 * M3E 风格的文件夹卡片组件
 * 
 * 特性：
 * - 选中状态的缩放动画（1.02x）
 * - 阴影提升（4dp → 8dp）
 * - 2dp primary 色边框动画
 * - primaryContainer 背景色渐变（10% 透明度）
 * - 使用 spring 动画（DampingRatioMediumBouncy）
 * 
 * @param folder 文件夹数据
 * @param isSelected 是否选中
 * @param isEditMode 是否处于编辑模式
 * @param onSelect 选中回调（编辑模式下）
 * @param onClick 点击回调（正常模式下）
 * @param modifier 修饰符
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveFolderCard(
    folder: Folder,
    isSelected: Boolean,
    isEditMode: Boolean,
    onSelect: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 缩放动画 - 选中时放大到 1.02x
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "folder_card_scale"
    )
    
    // 阴影提升动画 - 选中时从 4dp 提升到 8dp
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 4.dp,
        animationSpec = tween(
            durationMillis = ExpressiveAnimations.DURATION_SHORT
        ),
        label = "folder_card_elevation"
    )
    
    // 边框颜色动画 - 选中时显示 primary 色边框
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) 
            MaterialTheme.colorScheme.primary 
        else 
            Color.Transparent,
        animationSpec = tween(
            durationMillis = ExpressiveAnimations.DURATION_SHORT
        ),
        label = "folder_card_border"
    )
    
    // 背景颜色动画 - 选中时使用 primaryContainer（10% 透明度）
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        else
            MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(
            durationMillis = ExpressiveAnimations.DURATION_SHORT
        ),
        label = "folder_card_background"
    )
    
    Card(
        onClick = if (isEditMode) onSelect else onClick,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.medium
            )
            .semantics {
                contentDescription = if (isEditMode) {
                    "文件夹 ${folder.name}，${if (isSelected) "已选中" else "未选中"}"
                } else {
                    "文件夹 ${folder.name}"
                }
            },
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation
        ),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 文件夹图标
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 文件夹信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // TODO: 添加书籍数量显示（需要从 Repository 获取）
                Text(
                    text = "0 本书",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 选中指示器 - 仅在编辑模式下显示
            AnimatedVisibility(
                visible = isEditMode,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy
                    )
                ) + fadeIn(),
                exit = scaleOut(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy
                    )
                ) + fadeOut()
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

/**
 * 编辑模式头部组件
 * 
 * 特性：
 * - 渐变背景（primaryContainer → surface）
 * - 圆角底部（24dp）和阴影（4dp）
 * - 显示"编辑文件夹"标题和"已选择 X 项"副标题
 * - 取消按钮和全选/取消全选按钮
 * 
 * @param selectedCount 已选择的文件夹数量
 * @param totalCount 文件夹总数
 * @param onCancel 取消编辑模式回调
 * @param onSelectAll 全选回调
 * @param onDeselectAll 取消全选回调
 * @param modifier 修饰符
 */
@Composable
fun EditModeHeader(
    selectedCount: Int,
    totalCount: Int,
    onCancel: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(
            bottomStart = 24.dp,
            bottomEnd = 24.dp
        ),
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        // 渐变背景
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 顶部行：取消按钮和全选/取消全选按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 取消按钮
                    TextButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "取消",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("取消")
                    }
                    
                    // 全选/取消全选按钮
                    TextButton(
                        onClick = if (selectedCount == totalCount) onDeselectAll else onSelectAll
                    ) {
                        Text(if (selectedCount == totalCount) "取消全选" else "全选")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 标题
                Text(
                    text = "编辑文件夹",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 副标题
                Text(
                    text = "已选择 $selectedCount 项",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 底部操作栏组件
 * 
 * 特性：
 * - 渐变背景（surface → surfaceVariant）
 * - 圆角顶部（24dp）和阴影（8dp）
 * - 使用 ExpressiveButton 组件创建重命名、删除、排序按钮
 * - 重命名仅在选中 1 项时启用
 * 
 * @param selectedCount 已选择的文件夹数量
 * @param onRename 重命名回调
 * @param onDelete 删除回调
 * @param onSort 排序回调
 * @param modifier 修饰符
 */
@Composable
fun EditModeBottomBar(
    selectedCount: Int,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onSort: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 24.dp
        ),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        // 渐变背景
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 重命名按钮 - 仅在选中 1 项时启用
                ExpressiveButton(
                    onClick = onRename,
                    enabled = selectedCount == 1,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("重命名")
                }
                
                // 删除按钮
                ExpressiveButton(
                    onClick = onDelete,
                    enabled = selectedCount > 0,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("删除")
                }
                
                // 排序按钮
                ExpressiveButton(
                    onClick = onSort,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("排序")
                }
            }
        }
    }
}

/**
 * 编辑模式容器 - 带进入/退出动画
 * 
 * 特性：
 * - slideInVertically 进入动画（300ms EmphasizedDecelerateEasing）
 * - slideOutVertically 退出动画
 * - 使用 AnimatedVisibility 包装编辑模式 UI
 * 
 * @param visible 是否可见
 * @param content 内容
 */
@Composable
fun EditModeContainer(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.slideInVertically(
            animationSpec = tween(
                durationMillis = ExpressiveAnimations.DURATION_MEDIUM,
                easing = ExpressiveAnimations.EmphasizedDecelerateEasing
            ),
            initialOffsetY = { -it }
        ) + androidx.compose.animation.fadeIn(
            animationSpec = tween(
                durationMillis = ExpressiveAnimations.DURATION_MEDIUM
            )
        ),
        exit = androidx.compose.animation.slideOutVertically(
            animationSpec = tween(
                durationMillis = ExpressiveAnimations.DURATION_SHORT,
                easing = ExpressiveAnimations.EmphasizedAccelerateEasing
            ),
            targetOffsetY = { -it }
        ) + androidx.compose.animation.fadeOut(
            animationSpec = tween(
                durationMillis = ExpressiveAnimations.DURATION_SHORT
            )
        )
    ) {
        content()
    }
}

/**
 * 文件夹卡片列表项 - 带延迟出现动画
 * 
 * @param folder 文件夹数据
 * @param isSelected 是否选中
 * @param isEditMode 是否处于编辑模式
 * @param index 索引（用于计算延迟）
 * @param onSelect 选中回调
 * @param onClick 点击回调
 * @param modifier 修饰符
 */
@Composable
fun AnimatedFolderCard(
    folder: Folder,
    isSelected: Boolean,
    isEditMode: Boolean,
    index: Int,
    onSelect: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 50L) // 每个延迟 50ms
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn(
            animationSpec = tween(
                durationMillis = ExpressiveAnimations.DURATION_MEDIUM
            )
        ) + androidx.compose.animation.expandVertically(
            animationSpec = tween(
                durationMillis = ExpressiveAnimations.DURATION_MEDIUM,
                easing = ExpressiveAnimations.EmphasizedDecelerateEasing
            )
        )
    ) {
        ExpressiveFolderCard(
            folder = folder,
            isSelected = isSelected,
            isEditMode = isEditMode,
            onSelect = onSelect,
            onClick = onClick,
            modifier = modifier
        )
    }
}

/**
 * M3E 风格的重命名对话框
 * 
 * 特性：
 * - 使用 extraLarge 形状（32dp 圆角）
 * - OutlinedTextField 带 medium 圆角
 * - ExpressiveButton 作为确认按钮
 * - 名称验证和错误提示
 * 
 * @param currentName 当前文件夹名称
 * @param onDismiss 关闭对话框回调
 * @param onConfirm 确认重命名回调
 */
@Composable
fun RenameFolderDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // 验证名称
    fun validateName(name: String): String? {
        return when {
            name.isBlank() -> "文件夹名称不能为空"
            name.length > 50 -> "文件夹名称不能超过 50 个字符"
            name.contains("/") || name.contains("\\") -> "文件夹名称不能包含 / 或 \\"
            else -> null
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Text(
                text = "重命名文件夹",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                        errorMessage = validateName(it)
                    },
                    label = { Text("文件夹名称") },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it) } },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            ExpressiveButton(
                onClick = {
                    val error = validateName(newName)
                    if (error == null) {
                        onConfirm(newName)
                        onDismiss()
                    } else {
                        errorMessage = error
                    }
                },
                enabled = errorMessage == null && newName.isNotBlank()
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * M3E 风格的删除确认对话框
 * 
 * 特性：
 * - 48dp 警告图标（error 色）
 * - 使用 extraLarge 形状
 * - ExpressiveButton（error 容器色）作为删除按钮
 * - 显示删除数量和警告信息
 * 
 * @param deleteCount 要删除的文件夹数量
 * @param onDismiss 关闭对话框回调
 * @param onConfirm 确认删除回调
 */
@Composable
fun DeleteConfirmDialog(
    deleteCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "确认删除",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "您确定要删除 $deleteCount 个文件夹吗？",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "此操作无法撤销，文件夹中的所有书籍将被移至未分类。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            ExpressiveButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text("删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 排序选项枚举
 */
enum class FolderSortOption(val displayName: String) {
    NAME_ASC("名称（A-Z）"),
    NAME_DESC("名称（Z-A）"),
    DATE_ASC("创建时间（旧→新）"),
    DATE_DESC("创建时间（新→旧）"),
    BOOK_COUNT_ASC("书籍数量（少→多）"),
    BOOK_COUNT_DESC("书籍数量（多→少）")
}

/**
 * M3E 风格的排序选项对话框
 * 
 * 特性：
 * - 使用 extraLarge 形状
 * - 选项列表（RadioButton + Surface）
 * - 选中项使用 primaryContainer 背景
 * - 6 种排序选项
 * 
 * @param currentOption 当前排序选项
 * @param onDismiss 关闭对话框回调
 * @param onConfirm 确认排序回调
 */
@Composable
fun SortOptionsDialog(
    currentOption: FolderSortOption,
    onDismiss: () -> Unit,
    onConfirm: (FolderSortOption) -> Unit
) {
    var selectedOption by remember { mutableStateOf(currentOption) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Text(
                text = "排序方式",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FolderSortOption.values().forEach { option ->
                    Surface(
                        onClick = { selectedOption = option },
                        shape = MaterialTheme.shapes.medium,
                        color = if (selectedOption == option)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedOption == option,
                                onClick = { selectedOption = option }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = option.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selectedOption == option)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            ExpressiveButton(
                onClick = {
                    onConfirm(selectedOption)
                    onDismiss()
                }
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * M3E 风格的错误提示 Snackbar
 * 
 * 特性：
 * - 使用 errorContainer 背景色
 * - 添加错误图标（20dp）
 * - 使用 medium 圆角
 * - 实现自动消失和手动关闭
 * 
 * @param snackbarHostState Snackbar 宿主状态
 * @param modifier 修饰符
 */
@Composable
fun ErrorSnackbarHost(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier,
        snackbar = { data ->
            Snackbar(
                shape = MaterialTheme.shapes.medium,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = data.visuals.message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    )
}

/**
 * 显示错误消息的辅助函数
 */
suspend fun showErrorMessage(
    snackbarHostState: SnackbarHostState,
    message: String,
    duration: SnackbarDuration = SnackbarDuration.Short
) {
    snackbarHostState.showSnackbar(
        message = message,
        duration = duration
    )
}
