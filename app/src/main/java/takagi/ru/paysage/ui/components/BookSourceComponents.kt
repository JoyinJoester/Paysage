package takagi.ru.paysage.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import takagi.ru.paysage.R
import takagi.ru.paysage.data.model.BookSource
import takagi.ru.paysage.data.model.CategoryType
import takagi.ru.paysage.ui.theme.CategoryColors

/**
 * 书源卡片组件
 */
@Composable
fun BookSourceCard(
    source: BookSource,
    onClick: () -> Unit,
    onToggleEnabled: () -> Unit,
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
            // 左侧：书源信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 书源名称
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // 书源URL
                Text(
                    text = source.baseUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // 统计信息
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 分类标签
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = when (source.categoryType) {
                            CategoryType.MANGA -> CategoryColors.MangaContainer
                            CategoryType.NOVEL -> CategoryColors.NovelContainer
                        }
                    ) {
                        Text(
                            text = stringResource(
                                if (source.categoryType == CategoryType.MANGA)
                                    R.string.category_manga
                                else
                                    R.string.category_novel
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = when (source.categoryType) {
                                CategoryType.MANGA -> CategoryColors.MangaOnContainer
                                CategoryType.NOVEL -> CategoryColors.NovelOnContainer
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    // 优先级
                    if (source.priority > 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = source.priority.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // 书籍数量
                    if (source.totalBooks > 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Book,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = source.totalBooks.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // 右侧：启用开关
            Switch(
                checked = source.isEnabled,
                onCheckedChange = { onToggleEnabled() }
            )
        }
    }
}

/**
 * 紧凑版书源卡片
 */
@Composable
fun CompactBookSourceCard(
    source: BookSource,
    onClick: () -> Unit,
    onToggleEnabled: () -> Unit,
    modifier: Modifier = Modifier
) {
    ExpressiveCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 顶部：名称和开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Switch(
                    checked = source.isEnabled,
                    onCheckedChange = { onToggleEnabled() },
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // URL
            Text(
                text = source.baseUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // 标签行
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 分类
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            stringResource(
                                if (source.categoryType == CategoryType.MANGA)
                                    R.string.category_manga
                                else
                                    R.string.category_novel
                            ),
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.height(24.dp)
                )
                
                // 优先级
                if (source.priority > 0) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                "P${source.priority}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        },
                        modifier = Modifier.height(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * 书源列表项（用于列表视图）
 */
@Composable
fun BookSourceListItem(
    source: BookSource,
    onClick: () -> Unit,
    onToggleEnabled: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
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
            // 图标
            Icon(
                Icons.Default.CloudQueue,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = when (source.categoryType) {
                    CategoryType.MANGA -> CategoryColors.MangaPrimary
                    CategoryType.NOVEL -> CategoryColors.NovelPrimary
                }
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = source.baseUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 开关
            Switch(
                checked = source.isEnabled,
                onCheckedChange = { onToggleEnabled() }
            )
        }
    }
}
