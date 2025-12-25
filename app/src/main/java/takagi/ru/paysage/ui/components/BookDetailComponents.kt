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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import java.io.File
import kotlinx.coroutines.launch

/**
 * 书籍详情头部组件
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
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 封面
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(180.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val coverExists = remember(book.coverPath) {
                book.coverPath?.let { File(it).exists() } ?: false
            }
            
            if (coverExists && book.coverPath != null) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(book.coverPath)
                        .crossfade(300)
                        .memoryCacheKey(book.id.toString())
                        .build(),
                    contentDescription = book.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    },
                    error = {
                        Icon(
                            Icons.Default.Book,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    success = {
                        SubcomposeAsyncImageContent()
                    }
                )
            } else {
                Icon(
                    Icons.Default.Book,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // 信息区域
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 标题
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            // 格式和大小
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 格式标签
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = book.fileFormat.extension.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
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
 * 快速操作按钮组件
 * 包含书架和查看按钮
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
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 收藏按钮
        ExpressiveButton(
            onClick = onToggleFavorite,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (book.isFavorite) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.surface,
                contentColor = if (book.isFavorite)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(
                imageVector = if (book.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.book_detail_favorite))
        }
        
        // 查看按钮
        ExpressiveButton(
            onClick = onOpenBook,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.book_detail_view_book))
        }
    }
}

/**
 * 排序选项组件
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
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
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
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
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
                    }
                )
            }
        }
    }
}

/**
 * 标签管理组件
 */
@Composable
fun BookDetailTagSection(
    tags: List<String>,
    onEditTags: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Tag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (tags.isEmpty()) {
                    Text(
                        text = stringResource(R.string.book_detail_no_tags),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // 显示标签
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        tags.take(3).forEach { tag ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (tags.size > 3) {
                            Text(
                                text = "+${tags.size - 3}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            IconButton(onClick = onEditTags) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.book_detail_edit_tags)
                )
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
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.book_detail_edit_tags),
                    style = MaterialTheme.typography.titleLarge
                )
                
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
                                    onClick = { editedTags = editedTags.toMutableList().apply { removeAt(index) } }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "删除标签"
                                    )
                                }
                            }
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
                    }
                )
                
                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { onSave(editedTags) }) {
                        Text(stringResource(R.string.dialog_confirm))
                    }
                }
            }
        }
    }
}


/**
 * 阅读进度组件
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
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 页码
        Text(
            text = "${book.currentPage}/${book.totalPages}",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        // 进度条
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(MaterialTheme.shapes.small),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        
        // 三列信息
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 页数
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = stringResource(R.string.book_detail_pages),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 上次阅读
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.book_detail_last_read),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 阅读状态
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (book.lastReadAt != null) {
                        FormatUtils.formatRelativeTime(context, book.lastReadAt)
                    } else {
                        stringResource(R.string.book_detail_not_read)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 作者信息组件
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
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Column {
                    Text(
                        text = stringResource(R.string.book_detail_author),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = author,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * 文件路径组件
 */
@Composable
fun BookDetailPathSection(
    filePath: String,
    onCopyPath: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fileExists = remember(filePath) { File(filePath).exists() }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onCopyPath),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = if (fileExists) 
                    MaterialTheme.colorScheme.onSurfaceVariant 
                else 
                    MaterialTheme.colorScheme.error
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.book_detail_directory),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (fileExists) filePath else stringResource(R.string.book_detail_file_not_found),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (fileExists) 
                        MaterialTheme.colorScheme.onSurfaceVariant 
                    else 
                        MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 底部操作栏组件
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
    // 收藏动画
    var animateFavorite by remember { mutableStateOf(false) }
    val favoriteRotation by animateFloatAsState(
        targetValue = if (animateFavorite) 360f else 0f,
        animationSpec = tween(400),
        label = "favorite_rotation"
    )
    val favoriteScale by animateFloatAsState(
        targetValue = if (animateFavorite) 1.3f else 1f,
        animationSpec = tween(200),
        label = "favorite_scale"
    )
    
    LaunchedEffect(isFavorite) {
        animateFavorite = true
        kotlinx.coroutines.delay(400)
        animateFavorite = false
    }
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 关闭
            ExpressiveIconButton(
                onClick = onClose,
                modifier = Modifier.semantics {
                    contentDescription = "关闭详情"
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null
                )
            }
            
            // 收藏
            ExpressiveIconButton(
                onClick = {
                    animateFavorite = true
                    onToggleFavorite()
                },
                modifier = Modifier.semantics {
                    contentDescription = if (isFavorite) "取消收藏" else "收藏"
                }
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = null,
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = favoriteRotation
                        scaleX = favoriteScale
                        scaleY = favoriteScale
                    }
                )
            }
            
            // 编辑
            ExpressiveIconButton(
                onClick = onEdit,
                modifier = Modifier.semantics {
                    contentDescription = "编辑书籍"
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null
                )
            }
            
            // 分享
            ExpressiveIconButton(
                onClick = onShare,
                modifier = Modifier.semantics {
                    contentDescription = "分享书籍"
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null
                )
            }
            
            // 删除
            ExpressiveIconButton(
                onClick = onDelete,
                modifier = Modifier.semantics {
                    contentDescription = "删除书籍"
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
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
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(R.string.dialog_confirm),
                    color = MaterialTheme.colorScheme.error
                )
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
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 800.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
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
            
            // 底部操作栏
            item {
                BookDetailBottomActions(
                    isFavorite = book.isFavorite,
                    onClose = onDismiss,
                    onToggleFavorite = { onToggleFavorite(book.id) },
                    onEdit = { onEditBook(book.id) },
                    onShare = { onShareBook(book) },
                    onDelete = { showDeleteDialog = true }
                )
            }
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
