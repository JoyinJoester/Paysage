package takagi.ru.paysage.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import takagi.ru.paysage.data.model.CategoryInfo
import takagi.ru.paysage.data.model.FilterMode

/**
 * 分类列表视图
 */
@Composable
fun CategoriesListView(
    categories: List<CategoryInfo>,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 过滤掉无效的分类（名称为空或书籍数量为0）
    val validCategories = remember(categories) {
        categories.filter { it.name.isNotBlank() && it.bookCount > 0 }
    }
    
    if (validCategories.isEmpty()) {
        EmptyCategoriesView(modifier = modifier)
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                count = validCategories.size,
                key = { index -> validCategories[index].name }
            ) { index ->
                val categoryInfo = validCategories[index]
                CategoryCard(
                    categoryInfo = categoryInfo,
                    onClick = { 
                        if (categoryInfo.name.isNotBlank()) {
                            onCategoryClick(categoryInfo.name)
                        }
                    }
                )
            }
        }
    }
}

/**
 * 分类卡片
 */
@Composable
fun CategoryCard(
    categoryInfo: CategoryInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ExpressiveCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = categoryInfo.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${categoryInfo.bookCount} 本书籍",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 空分类视图
 */
@Composable
fun EmptyCategoriesView(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Category,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无分类",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "书籍的分类信息会在扫描时自动识别",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


/**
 * 通用空状态视图（根据筛选模式显示不同内容）
 */
@Composable
fun EmptyFilterView(
    filterMode: FilterMode,
    modifier: Modifier = Modifier
) {
    val (icon, title, message) = when (filterMode) {
        FilterMode.FAVORITES -> Triple(
            Icons.Default.FavoriteBorder,
            "暂无收藏的书籍",
            "点击书籍详情中的收藏按钮来添加收藏"
        )
        FilterMode.RECENT -> Triple(
            Icons.Default.History,
            "暂无最近阅读记录",
            "开始阅读书籍后，这里会显示最近阅读的内容"
        )
        FilterMode.CATEGORIES -> Triple(
            Icons.Default.Category,
            "暂无分类",
            "书籍的分类信息会在扫描时自动识别"
        )
        FilterMode.CATEGORY -> Triple(
            Icons.Default.Category,
            "该分类下暂无书籍",
            "尝试其他分类或返回全部书籍"
        )
        else -> Triple(
            Icons.Default.Book,
            "暂无书籍",
            "开始扫描以添加书籍"
        )
    }
    
    EmptyStateView(
        icon = icon,
        title = title,
        message = message,
        modifier = modifier
    )
}

/**
 * 通用空状态视图组件
 */
@Composable
private fun EmptyStateView(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
