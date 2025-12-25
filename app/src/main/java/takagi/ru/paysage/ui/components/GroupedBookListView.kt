package takagi.ru.paysage.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import takagi.ru.paysage.data.model.Book
import takagi.ru.paysage.data.model.FilterMode
import takagi.ru.paysage.data.model.GroupInfo

/**
 * 分组书籍列表视图
 * 
 * @param groups 分组信息列表
 * @param filterMode 当前筛选模式（用于显示不同的图标）
 * @param onBookClick 书籍点击回调
 * @param onBookLongClick 书籍长按回调
 * @param modifier Modifier
 */
@Composable
fun GroupedBookListView(
    groups: List<GroupInfo>,
    filterMode: FilterMode,
    onBookClick: (Book) -> Unit,
    onBookLongClick: ((Book) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // 追踪每个分组的展开状态
    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }
    
    if (groups.isEmpty()) {
        // 空状态
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = getIconForFilterMode(filterMode),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "暂无内容",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = getEmptyHintForFilterMode(filterMode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(groups, key = { it.name }) { group ->
                val isExpanded = expandedGroups[group.name] ?: false
                
                GroupHeader(
                    group = group,
                    filterMode = filterMode,
                    isExpanded = isExpanded,
                    onToggle = {
                        expandedGroups[group.name] = !isExpanded
                    }
                )
                
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    GroupBookGrid(
                        books = group.books,
                        onBookClick = onBookClick,
                        onBookLongClick = onBookLongClick
                    )
                }
            }
        }
    }
}

/**
 * 分组标题
 */
@Composable
private fun GroupHeader(
    group: GroupInfo,
    filterMode: FilterMode,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "expand_rotation"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = getIconForFilterMode(filterMode),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 分组名称和书籍数量
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${group.bookCount} 本书",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 展开/收起指示器
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "收起" else "展开",
                modifier = Modifier.rotate(rotationAngle),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 分组内的书籍网格
 */
@Composable
private fun GroupBookGrid(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    onBookLongClick: ((Book) -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
    ) {
        // 使用水平滚动的书籍封面行
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(books, key = { it.id }) { book ->
                CompactBookCard(
                    book = book,
                    onClick = { onBookClick(book) },
                    onLongClick = { onBookLongClick?.invoke(book) }
                )
            }
        }
    }
}

/**
 * 紧凑型书籍卡片（用于分组视图）
 */
@Composable
private fun CompactBookCard(
    book: Book,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .width(100.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick?.invoke() }
                )
            }
    ) {
        // 封面
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            if (book.coverPath != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(book.coverPath)
                        .crossfade(true)
                        .build(),
                    contentDescription = book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = book.title.take(1),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // 标题
        Text(
            text = book.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 根据筛选模式获取对应图标
 */
private fun getIconForFilterMode(filterMode: FilterMode): ImageVector {
    return when (filterMode) {
        FilterMode.AUTHOR -> Icons.Default.Person
        FilterMode.SERIES -> Icons.Outlined.CollectionsBookmark
        FilterMode.YEAR -> Icons.Outlined.CalendarMonth
        FilterMode.SOURCE_FOLDER -> Icons.Outlined.Folder
        else -> Icons.Outlined.Folder
    }
}

/**
 * 根据筛选模式获取空状态提示文本
 */
private fun getEmptyHintForFilterMode(filterMode: FilterMode): String {
    return when (filterMode) {
        FilterMode.AUTHOR -> "添加带有作者信息的书籍后将显示在这里"
        FilterMode.SERIES -> "添加书籍后将按系列分组显示"
        FilterMode.YEAR -> "添加书籍后将按年度分组显示"
        FilterMode.SOURCE_FOLDER -> "添加书源后将按文件夹分组显示"
        else -> "暂无书籍"
    }
}
