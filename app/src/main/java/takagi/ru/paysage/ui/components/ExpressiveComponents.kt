package takagi.ru.paysage.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import takagi.ru.paysage.ui.theme.ExpressiveAnimations
import takagi.ru.paysage.ui.theme.ExpressiveDimensions

/**
 * Expressive Card - 带有悬停和按压动画的卡片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.medium,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable ColumnScope.() -> Unit
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    // 动画效果
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.95f
            isHovered -> 1.02f
            else -> 1f
        },
        animationSpec = ExpressiveAnimations.bouncySpring,
        label = "card_scale"
    )
    
    val elevationDp by animateDpAsState(
        targetValue = when {
            isPressed -> ExpressiveDimensions.cardElevationPressed
            isHovered -> ExpressiveDimensions.cardElevationHovered
            else -> ExpressiveDimensions.cardElevation
        },
        label = "card_elevation"
    )
    
    Card(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevationDp
        ),
        border = border,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Expressive Button - 带有生动动画的按钮
 */
@Composable
fun ExpressiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.large,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = ExpressiveAnimations.bouncySpring,
        label = "button_scale"
    )
    
    Button(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Expressive FAB - 带有旋转和缩放动画的 FAB
 */
@Composable
fun ExpressiveFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.large,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: androidx.compose.ui.graphics.Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit,
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = ExpressiveAnimations.bouncySpring,
        label = "fab_scale"
    )
    
    val rotation by animateFloatAsState(
        targetValue = if (isPressed) 15f else 0f,
        animationSpec = ExpressiveAnimations.bouncySpring,
        label = "fab_rotation"
    )
    
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            rotationZ = rotation
        },
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Expressive Chip - 带有动画的芯片组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.small,
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors(),
    elevation: SelectableChipElevation? = FilterChipDefaults.filterChipElevation(),
    border: BorderStroke? = BorderStroke(
        width = 1.dp,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    ),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = ExpressiveAnimations.bouncySpring,
        label = "chip_scale"
    )
    
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        interactionSource = interactionSource
    )
}

/**
 * Expressive IconButton - 带有脉冲动画的图标按钮
 */
@Composable
fun ExpressiveIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = ExpressiveAnimations.bouncySpring,
        label = "icon_button_scale"
    )
    
    IconButton(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Expressive NavigationRailItem - 带有生动动画的导航栏项
 * 用于第一层导航栏，提供按压缩放和颜色过渡动画
 */
@Composable
fun ExpressiveNavigationRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // 按压缩放动画
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = ExpressiveAnimations.bouncySpring,
        label = "nav_item_scale"
    )
    
    // 背景颜色动画
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = androidx.compose.animation.core.tween(ExpressiveAnimations.DURATION_MEDIUM),
        label = "nav_item_bg"
    )
    
    // 图标颜色动画
    val iconColor by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = androidx.compose.animation.core.tween(ExpressiveAnimations.DURATION_MEDIUM),
        label = "nav_item_icon"
    )
    
    // 文本颜色动画
    val textColor by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = androidx.compose.animation.core.tween(ExpressiveAnimations.DURATION_MEDIUM),
        label = "nav_item_text"
    )
    
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier
                    .size(ExpressiveDimensions.iconSizeMedium)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                tint = iconColor
            )
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = textColor
            )
        },
        modifier = modifier.minimumInteractiveComponentSize(),
        enabled = enabled,
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = iconColor,
            unselectedIconColor = iconColor,
            selectedTextColor = textColor,
            unselectedTextColor = textColor,
            indicatorColor = backgroundColor
        ),
        interactionSource = interactionSource
    )
}

// M3EPullRefreshIndicator 已移除
// 原因：PullToRefreshBox API 在当前 Compose 版本中不可用
// 下拉刷新功能已暂时禁用，将在未来版本中使用更新的 API 重新实现

/**
 * 书库过滤状态枚举
 */
enum class BookFilterStatus {
    ALL,
    LATEST,
    READING,
    FINISHED,
    UNREAD
}

/**
 * 获取书籍过滤状态对应的颜色
 */
@Composable
fun getFilterStatusColor(status: BookFilterStatus): androidx.compose.ui.graphics.Color {
    return when (status) {
        BookFilterStatus.ALL -> MaterialTheme.colorScheme.primary
        BookFilterStatus.LATEST -> androidx.compose.ui.graphics.Color(0xFFFF5722) // 橙红色
        BookFilterStatus.READING -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // 绿色
        BookFilterStatus.FINISHED -> androidx.compose.ui.graphics.Color(0xFF2196F3) // 蓝色
        BookFilterStatus.UNREAD -> androidx.compose.ui.graphics.Color(0xFFC62828) // 深红色
    }
}

/**
 * 带状态圆点的下拉菜单项
 */
@Composable
fun StatusDropdownMenuItem(
    status: BookFilterStatus,
    text: String,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 状态圆点
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = getFilterStatusColor(status),
                            shape = CircleShape
                        )
                )
                
                Text(text)
            }
        },
        onClick = onClick
    )
}

/**
 * M3E 风格的书库过滤栏
 * 包含状态过滤、搜索、布局切换和排序功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryFilterBar(
    selectedFilter: BookFilterStatus,
    onFilterChange: (BookFilterStatus) -> Unit,
    isSearchMode: Boolean,
    onSearchModeChange: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    currentLayout: takagi.ru.paysage.data.model.LibraryLayout,
    onLayoutChange: () -> Unit,
    onSortClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showFilterMenu by remember { mutableStateOf(false) }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧：状态过滤或搜索输入框
            if (isSearchMode) {
                // 搜索模式
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = stringResource(takagi.ru.paysage.R.string.search_in_category),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            innerTextField()
                        }
                    )
                    
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "清除",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // 过滤模式
                Box {
                    Row(
                        modifier = Modifier
                            .clickable { showFilterMenu = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 圆形指示器
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = getFilterStatusColor(selectedFilter),
                                    shape = CircleShape
                                )
                        )
                        
                        Text(
                            text = when (selectedFilter) {
                                BookFilterStatus.ALL -> stringResource(takagi.ru.paysage.R.string.filter_all)
                                BookFilterStatus.LATEST -> stringResource(takagi.ru.paysage.R.string.filter_latest)
                                BookFilterStatus.READING -> stringResource(takagi.ru.paysage.R.string.filter_reading)
                                BookFilterStatus.FINISHED -> stringResource(takagi.ru.paysage.R.string.filter_finished)
                                BookFilterStatus.UNREAD -> stringResource(takagi.ru.paysage.R.string.filter_unread)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // 下拉菜单
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        StatusDropdownMenuItem(
                            status = BookFilterStatus.ALL,
                            text = stringResource(takagi.ru.paysage.R.string.filter_all),
                            onClick = {
                                onFilterChange(BookFilterStatus.ALL)
                                showFilterMenu = false
                            }
                        )
                        StatusDropdownMenuItem(
                            status = BookFilterStatus.LATEST,
                            text = stringResource(takagi.ru.paysage.R.string.filter_latest),
                            onClick = {
                                onFilterChange(BookFilterStatus.LATEST)
                                showFilterMenu = false
                            }
                        )
                        StatusDropdownMenuItem(
                            status = BookFilterStatus.READING,
                            text = stringResource(takagi.ru.paysage.R.string.filter_reading),
                            onClick = {
                                onFilterChange(BookFilterStatus.READING)
                                showFilterMenu = false
                            }
                        )
                        StatusDropdownMenuItem(
                            status = BookFilterStatus.FINISHED,
                            text = stringResource(takagi.ru.paysage.R.string.filter_finished),
                            onClick = {
                                onFilterChange(BookFilterStatus.FINISHED)
                                showFilterMenu = false
                            }
                        )
                        StatusDropdownMenuItem(
                            status = BookFilterStatus.UNREAD,
                            text = stringResource(takagi.ru.paysage.R.string.filter_unread),
                            onClick = {
                                onFilterChange(BookFilterStatus.UNREAD)
                                showFilterMenu = false
                            }
                        )
                    }
                }
            }
            
            // 右侧：功能按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 搜索按钮
                IconButton(
                    onClick = { onSearchModeChange(!isSearchMode) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isSearchMode) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (isSearchMode) "关闭搜索" else "搜索",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 布局切换按钮
                IconButton(
                    onClick = onLayoutChange,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = when (currentLayout) {
                            takagi.ru.paysage.data.model.LibraryLayout.LIST -> Icons.Default.List
                            takagi.ru.paysage.data.model.LibraryLayout.COMPACT_GRID -> Icons.Default.GridView
                            takagi.ru.paysage.data.model.LibraryLayout.COVER_ONLY -> Icons.Default.ViewModule
                        },
                        contentDescription = "切换布局",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 排序按钮（预留）
                IconButton(
                    onClick = onSortClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "排序",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
