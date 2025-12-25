package takagi.ru.paysage.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import takagi.ru.paysage.R
import takagi.ru.paysage.data.model.Book
import takagi.ru.paysage.ui.theme.ExpressiveAnimations
import takagi.ru.paysage.util.FormatUtils
import takagi.ru.paysage.util.UriUtils
import java.io.File
import kotlinx.coroutines.launch

/**
 * 书籍详情头部组件 - M3E 风格
 * 显示封面、标题、格式标签和文件大小
 */
@Composable
fun BookDetailHeader(
    book: Book,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 封面 - 增加尺寸和圆角
        Surface(
            modifier = Modifier
                .width(130.dp)
                .aspectRatio(0.7f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shadowElevation = 6.dp
        ) {
            val coverExists = remember(book.coverPath) {
                book.coverPath?.let { File(it).exists() } ?: false
            }
            
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
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    },
                    error = {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Book,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Book,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // 信息区域
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 标题
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = MaterialTheme.typography.titleLarge.lineHeight * 1.1
                ),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // 格式标签
            AssistChip(
                onClick = {},
                label = { 
                    Text(
                        text = book.fileFormat.extension.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                border = null,
                modifier = Modifier.height(26.dp)
            )
            
            // 文件大小
            Text(
                text = stringResource(
                    R.string.book_detail_file_size,
                    FormatUtils.formatFileSize(book.fileSize)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 快速操作按钮组件 - M3E 风格
 * 包含收藏和阅读按钮
 */
@Composable
fun BookDetailActions(
    book: Book,
    onToggleFavorite: () -> Unit,
    onOpenBook: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 收藏按钮 - OutlinedButton
        OutlinedButton(
            onClick = onToggleFavorite,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Icon(
                imageVector = if (book.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (book.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.book_detail_favorite))
        }
        
        // 阅读按钮 - FilledTonalButton (Prominent action)
        FilledTonalButton(
            onClick = onOpenBook,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.book_detail_view_book))
        }
    }
}

/**
 * 排序选项组件 - M3E 风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailSortSection(
    currentSort: String?,
    onSortChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val sortOptions = listOf(
        "new" to R.string.book_detail_sort_new,
        "title" to R.string.book_detail_sort_title,
        "author" to R.string.book_detail_sort_author,
        "recent" to R.string.book_detail_sort_recent,
        "added" to R.string.book_detail_sort_added
    )
    
    val currentSortLabel = sortOptions.find { it.first == currentSort }?.second 
        ?: R.string.book_detail_sort_new
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = stringResource(currentSortLabel),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.book_detail_sort_by)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = MaterialTheme.shapes.medium,
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                sortOptions.forEach { (value, labelRes) ->
                    DropdownMenuItem(
                        text = { Text(stringResource(labelRes)) },
                        onClick = {
                            onSortChange(value)
                            expanded = false
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    )
                }
            }
        }
    }
}

/**
 * 标签管理组件 - M3E 风格
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookDetailTagSection(
    tags: List<String>,
    onEditTags: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onEditTags,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (tags.isEmpty()) stringResource(R.string.book_detail_no_tags) else "${tags.size} 个标签",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.book_detail_edit_tags),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        AssistChip(
                            onClick = { },
                            label = { Text(tag) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = null,
                            shape = MaterialTheme.shapes.small
                        )
                    }
                }
            }
        }
    }
}

/**
 * 标签编辑对话框
 */
@Composable
fun TagEditDialog(
    tags: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var editedTags by remember { mutableStateOf(tags.toMutableList()) }
    var newTag by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.book_detail_edit_tags)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 现有标签
                if (editedTags.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        editedTags.forEachIndexed { index, tag ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { editedTags = editedTags.toMutableList().apply { removeAt(index) } },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "删除标签",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
                
                // 添加新标签
                OutlinedTextField(
                    value = newTag,
                    onValueChange = { newTag = it },
                    label = { Text(stringResource(R.string.book_detail_tag_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (newTag.isNotBlank() && !editedTags.contains(newTag)) {
                                    editedTags = editedTags.toMutableList().apply { add(newTag) }
                                    newTag = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.book_detail_add_tag))
                        }
                    },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(editedTags) }) {
                Text(stringResource(R.string.dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
        modifier = modifier
    )
}

/**
 * 阅读进度组件 - M3E 风格
 */
@Composable
fun BookDetailProgressSection(
    book: Book,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val progress = if (book.totalPages > 0) {
        book.currentPage.toFloat() / book.totalPages
    } else 0f
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${book.currentPage}",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "/${book.totalPages}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            // 进度条
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            // 底部信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.book_detail_last_read),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (book.lastReadAt != null) {
                            FormatUtils.formatRelativeTime(context, book.lastReadAt)
                        } else {
                            stringResource(R.string.book_detail_not_read)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.book_detail_pages),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * 作者信息组件 - M3E ListItem 风格
 */
@Composable
fun BookDetailAuthorSection(
    author: String?,
    modifier: Modifier = Modifier
) {
    if (author != null) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 1.dp
        ) {
            ListItem(
                headlineContent = { Text(author) },
                overlineContent = { Text(stringResource(R.string.book_detail_author)) },
                leadingContent = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )
        }
    }
}

/**
 * 文件路径组件 - M3E ListItem 风格
 * 修复 content:// URI 检测问题
 */
@Composable
fun BookDetailPathSection(
    filePath: String,
    onCopyPath: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // 检查文件是否存在（支持 content URI）
    val isContentUri = filePath.startsWith("content://")
    val fileExists = remember(filePath) {
        if (isContentUri) {
            UriUtils.isContentUriAccessible(context, filePath)
        } else {
            File(filePath).exists()
        }
    }
    
    // 获取可显示路径
    val displayPath = remember(filePath) {
        if (isContentUri) {
            UriUtils.getReadablePath(context, filePath) ?: filePath
        } else {
            filePath
        }
    }
    
    Surface(
        onClick = onCopyPath,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        ListItem(
            headlineContent = { 
                Text(
                    text = displayPath,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                ) 
            },
            overlineContent = { Text(stringResource(R.string.book_detail_directory)) },
            leadingContent = {
                Icon(
                    imageVector = if (isContentUri) Icons.Outlined.FolderZip else Icons.Outlined.Folder,
                    contentDescription = null,
                    tint = if (fileExists) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
            },
            supportingContent = if (!fileExists) {
                {
                    Text(
                        text = stringResource(R.string.book_detail_file_not_found),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else null,
            trailingContent = {
                Icon(
                    Icons.Default.ContentCopy, 
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
    }
}

/**
 * 底部操作栏组件 - M3 BottomAppBar 风格
 */
@Composable
fun BookDetailBottomActions(
    isFavorite: Boolean,
    onClose: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    BottomAppBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        // 删除按钮
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.error
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // 分享按钮
        IconButton(onClick = onShare) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "分享"
            )
        }
        
        // 编辑按钮
        IconButton(onClick = onEdit) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "编辑"
            )
        }
        
        // 关闭按钮
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭"
            )
        }
    }
}

/**
 * 删除确认对话框
 */
@Composable
fun DeleteConfirmDialog(
    bookTitle: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.book_detail_delete_confirm_title)) },
        text = { Text(stringResource(R.string.book_detail_delete_confirm_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
        modifier = modifier
    )
}

/**
 * 主书籍详情底部弹窗组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailBottomSheet(
    book: Book,
    onDismiss: () -> Unit,
    onOpenBook: (Long) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onEditBook: (Long) -> Unit,
    onShareBook: (Book) -> Unit,
    onDeleteBook: (Long) -> Unit,
    onUpdateTags: (Long, List<String>) -> Unit,
    onUpdateSortPreference: (Long, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTagDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier.fillMaxWidth(),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // 头部
                item {
                    BookDetailHeader(book = book)
                }
                
                // 快速操作
                item {
                    BookDetailActions(
                        book = book,
                        onToggleFavorite = { onToggleFavorite(book.id) },
                        onOpenBook = {
                            onOpenBook(book.id)
                            onDismiss()
                        }
                    )
                }
                
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
                
                // 排序选项
                item {
                    BookDetailSortSection(
                        currentSort = book.sortPreference,
                        onSortChange = { onUpdateSortPreference(book.id, it) }
                    )
                }
                
                // 标签
                item {
                    BookDetailTagSection(
                        tags = book.tags,
                        onEditTags = { showTagDialog = true }
                    )
                }
                
                // 阅读进度
                item {
                    BookDetailProgressSection(book = book)
                }
                
                // 作者
                item {
                    BookDetailAuthorSection(author = book.author)
                }
                
                // 文件路径
                item {
                    BookDetailPathSection(
                        filePath = book.filePath,
                        onCopyPath = {
                            FormatUtils.copyToClipboard(
                                context,
                                "Book Path",
                                book.filePath
                            )
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.book_detail_path_copied)
                                )
                            }
                        }
                    )
                }
            }
            
            // 底部操作栏
            BookDetailBottomActions(
                isFavorite = book.isFavorite,
                onClose = onDismiss,
                onToggleFavorite = { onToggleFavorite(book.id) },
                onEdit = { onEditBook(book.id) },
                onShare = { onShareBook(book) },
                onDelete = { showDeleteDialog = true }
            )
        }
        
        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(16.dp)
        )
    }
    
    // 标签编辑对话框
    if (showTagDialog) {
        TagEditDialog(
            tags = book.tags,
            onDismiss = { showTagDialog = false },
            onSave = { newTags ->
                onUpdateTags(book.id, newTags)
                showTagDialog = false
            }
        )
    }
    
    // 删除确认对话框
    if (showDeleteDialog) {
        DeleteConfirmDialog(
            bookTitle = book.title,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                onDeleteBook(book.id)
                showDeleteDialog = false
                onDismiss()
            }
        )
    }
}
