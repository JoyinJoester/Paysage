package takagi.ru.paysage.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import takagi.ru.paysage.R

/**
 * 源选择选项组件
 * 可复用的选项卡片，用于显示不同类型的内容源
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceSelectionOption(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showChevron: Boolean = false,
    contentDescriptionText: String = title
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .semantics {
                role = Role.Button
                contentDescription = contentDescriptionText
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 文字内容
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 可选的右侧箭头
            if (showChevron) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}


/**
 * 源选择页面内容
 * 显示所有可用的内容源选项
 */
@Composable
fun SourceSelectionContent(
    selectedLocalMangaPath: String?,
    selectedLocalReadingPath: String?,
    onLocalMangaClick: () -> Unit,
    onLocalReadingClick: () -> Unit,
    onMangaSourceClick: () -> Unit,
    onReadingSourceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // 标题
        Text(
            text = context.getString(R.string.source_selection_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )
        
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.primaryContainer
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 本地漫画选项
        SourceSelectionOption(
            icon = Icons.Default.Book,
            title = context.getString(R.string.source_local_manga),
            subtitle = selectedLocalMangaPath 
                ?: context.getString(R.string.source_no_folder_selected),
            onClick = onLocalMangaClick,
            contentDescriptionText = context.getString(R.string.source_local_manga_desc)
        )
        
        // 本地阅读选项
        SourceSelectionOption(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            title = context.getString(R.string.source_local_reading),
            subtitle = selectedLocalReadingPath 
                ?: context.getString(R.string.source_no_folder_selected),
            onClick = onLocalReadingClick,
            contentDescriptionText = context.getString(R.string.source_local_reading_desc)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 在线源分隔
        Text(
            text = context.getString(R.string.display_mode_online),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        
        // 漫画源选项
        SourceSelectionOption(
            icon = Icons.Default.CloudQueue,
            title = context.getString(R.string.source_manga_source),
            subtitle = context.getString(R.string.source_manage_online_manga),
            onClick = onMangaSourceClick,
            showChevron = true,
            contentDescriptionText = context.getString(R.string.source_manga_source_desc)
        )
        
        // 阅读源选项
        SourceSelectionOption(
            icon = Icons.Default.CloudQueue,
            title = context.getString(R.string.source_reading_source),
            subtitle = context.getString(R.string.source_manage_online_reading),
            onClick = onReadingSourceClick,
            showChevron = true,
            contentDescriptionText = context.getString(R.string.source_reading_source_desc)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
