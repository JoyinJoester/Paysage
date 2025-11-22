package takagi.ru.paysage.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import takagi.ru.paysage.R
import takagi.ru.paysage.data.model.CategoryType
import takagi.ru.paysage.data.model.DisplayMode
import takagi.ru.paysage.ui.theme.ExpressiveAnimations
import takagi.ru.paysage.ui.theme.CategoryColors
import takagi.ru.paysage.ui.theme.getCategoryColor

/**
 * 分类筛选栏组件
 * 提供分类类型和显示模式的切换功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilterBar(
    selectedCategory: CategoryType,
    onCategoryChange: (CategoryType) -> Unit,
    displayMode: DisplayMode,
    onDisplayModeChange: (DisplayMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：分类切换
            CategorySegmentedButton(
                selectedCategory = selectedCategory,
                onCategoryChange = onCategoryChange
            )
            
            // 右侧：显示模式切换
            DisplayModeToggle(
                displayMode = displayMode,
                onDisplayModeChange = onDisplayModeChange
            )
        }
    }
}

/**
 * 分类分段按钮
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySegmentedButton(
    selectedCategory: CategoryType,
    onCategoryChange: (CategoryType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 漫画按钮
        FilterChip(
            selected = selectedCategory == CategoryType.MANGA,
            onClick = { onCategoryChange(CategoryType.MANGA) },
            label = { Text(stringResource(R.string.category_manga)) },
            leadingIcon = {
                Icon(
                    Icons.Default.Book,
                    contentDescription = stringResource(R.string.category_manga_icon),
                    modifier = Modifier.size(18.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = CategoryColors.MangaContainer,
                selectedLabelColor = CategoryColors.MangaOnContainer,
                selectedLeadingIconColor = CategoryColors.MangaOnContainer
            )
        )
        
        // 阅读按钮
        FilterChip(
            selected = selectedCategory == CategoryType.NOVEL,
            onClick = { onCategoryChange(CategoryType.NOVEL) },
            label = { Text(stringResource(R.string.category_novel)) },
            leadingIcon = {
                Icon(
                    Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = stringResource(R.string.category_novel_icon),
                    modifier = Modifier.size(18.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = CategoryColors.NovelContainer,
                selectedLabelColor = CategoryColors.NovelOnContainer,
                selectedLeadingIconColor = CategoryColors.NovelOnContainer
            )
        )
    }
}

/**
 * 显示模式切换按钮
 */
@Composable
private fun DisplayModeToggle(
    displayMode: DisplayMode,
    onDisplayModeChange: (DisplayMode) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = displayMode,
        transitionSpec = {
            fadeIn(
                animationSpec = tween(
                    durationMillis = 300,
                    easing = ExpressiveAnimations.EmphasizedEasing
                )
            ) togetherWith fadeOut(
                animationSpec = tween(
                    durationMillis = 300,
                    easing = ExpressiveAnimations.EmphasizedEasing
                )
            )
        },
        label = "display_mode_toggle"
    ) { mode ->
        ExpressiveIconButton(
            onClick = {
                onDisplayModeChange(
                    if (mode == DisplayMode.LOCAL) DisplayMode.ONLINE else DisplayMode.LOCAL
                )
            },
            modifier = modifier
        ) {
            Icon(
                imageVector = if (mode == DisplayMode.LOCAL) Icons.Default.Folder else Icons.Default.Cloud,
                contentDescription = stringResource(
                    if (mode == DisplayMode.LOCAL) R.string.display_mode_local else R.string.display_mode_online
                ),
                tint = if (mode == DisplayMode.ONLINE) CategoryColors.OnlinePrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * 紧凑版分类筛选栏（用于小屏幕）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactCategoryFilterBar(
    selectedCategory: CategoryType,
    onCategoryChange: (CategoryType) -> Unit,
    displayMode: DisplayMode,
    onDisplayModeChange: (DisplayMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 分类切换
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == CategoryType.MANGA,
                    onClick = { onCategoryChange(CategoryType.MANGA) },
                    label = { Text(stringResource(R.string.category_manga)) },
                    modifier = Modifier.weight(1f)
                )
                
                FilterChip(
                    selected = selectedCategory == CategoryType.NOVEL,
                    onClick = { onCategoryChange(CategoryType.NOVEL) },
                    label = { Text(stringResource(R.string.category_novel)) },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // 显示模式切换
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = displayMode == DisplayMode.LOCAL,
                    onClick = { onDisplayModeChange(DisplayMode.LOCAL) },
                    label = { Text(stringResource(R.string.display_mode_local)) },
                    modifier = Modifier.weight(1f)
                )
                
                FilterChip(
                    selected = displayMode == DisplayMode.ONLINE,
                    onClick = { onDisplayModeChange(DisplayMode.ONLINE) },
                    label = { Text(stringResource(R.string.display_mode_online)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 带动画的分类切换容器
 */
@Composable
fun CategorySwitchAnimation(
    targetCategory: CategoryType,
    content: @Composable (CategoryType) -> Unit
) {
    AnimatedContent(
        targetState = targetCategory,
        transitionSpec = {
            fadeIn(
                animationSpec = tween(
                    durationMillis = 300,
                    easing = ExpressiveAnimations.EmphasizedEasing
                )
            ) togetherWith fadeOut(
                animationSpec = tween(
                    durationMillis = 300,
                    easing = ExpressiveAnimations.EmphasizedEasing
                )
            )
        },
        label = "category_switch"
    ) { category ->
        content(category)
    }
}
