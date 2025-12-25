package takagi.ru.paysage.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import takagi.ru.paysage.R
import takagi.ru.paysage.data.model.Book
import java.io.File

/**
 * 上次阅读卡片
 * 
 * 显示在屏幕右下角的小卡片，显示上次阅读的书籍封面和名称
 * 
 * @param lastReadBook 上次阅读的书籍，为 null 时不显示
 * @param onClick 点击回调函数
 * @param modifier 修饰符
 */
@Composable
fun LastReadingFAB(
    lastReadBook: Book?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 只有当有上次阅读记录时才显示
    AnimatedVisibility(
        visible = lastReadBook != null,
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
            animationSpec = tween(300),
            initialOffsetY = { it / 2 }
        ),
        exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(
            animationSpec = tween(300),
            targetOffsetY = { it / 2 }
        ),
        modifier = modifier
    ) {
        lastReadBook?.let { book ->
            LastReadingCard(
                book = book,
                onClick = onClick
            )
        }
    }
}

/**
 * 上次阅读卡片内容
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LastReadingCard(
    book: Book,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(200.dp)
            .height(80.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // 左侧：封面图片
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val coverExists = book.coverPath?.let { File(it).exists() } ?: false
                
                if (coverExists && book.coverPath != null) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(book.coverPath)
                            .crossfade(true)
                            .memoryCacheKey(book.id.toString())
                            .build(),
                        contentDescription = book.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        },
                        error = {
                            // 加载失败时显示书本图标或背景
                        },
                        success = {
                            SubcomposeAsyncImageContent()
                        }
                    )
                }
            }
            
            // 右侧：书名和进度
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // "上次阅读" 标签
                    Text(
                        text = stringResource(R.string.continue_reading),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // 书名
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // 进度条
                val progress = book.currentPage.toFloat() / book.totalPages.coerceAtLeast(1)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(MaterialTheme.shapes.small),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}
