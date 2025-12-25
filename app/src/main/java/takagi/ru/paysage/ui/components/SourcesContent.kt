package takagi.ru.paysage.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import org.json.JSONArray
import org.json.JSONObject
import takagi.ru.paysage.data.model.Book

/**
 * Local source display model for UI
 */
data class LocalSourceItem(
    val id: Long,
    val displayName: String,
    val uri: String,
    val bookCount: Int = 0
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("displayName", displayName)
            put("uri", uri)
            put("bookCount", bookCount)
        }
    }
    
    companion object {
        fun fromJson(json: JSONObject): LocalSourceItem {
            return LocalSourceItem(
                id = json.getLong("id"),
                displayName = json.getString("displayName"),
                uri = json.getString("uri"),
                bookCount = json.optInt("bookCount", 0)
            )
        }
    }
}

/**
 * Helper object for persisting book sources
 */
object BookSourcesManager {
    private const val PREFS_NAME = "paysage_book_sources"
    private const val KEY_SOURCES = "local_sources"
    
    fun saveSources(context: Context, sources: List<LocalSourceItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        sources.forEach { source ->
            jsonArray.put(source.toJson())
        }
        prefs.edit().putString(KEY_SOURCES, jsonArray.toString()).apply()
    }
    
    fun loadSources(context: Context): List<LocalSourceItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_SOURCES, null) ?: return emptyList()
        
        return try {
            val jsonArray = JSONArray(jsonString)
            (0 until jsonArray.length()).map { i ->
                LocalSourceItem.fromJson(jsonArray.getJSONObject(i))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/**
 * Book Sources content panel for the drawer Layer 2.
 * Displays and manages local folder sources with integrated folder picker.
 * 
 * @param onScanSource Callback to trigger scanning of the source URI
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SourcesContent(
    onAddSourceClick: () -> Unit = {},
    onScanSource: (android.net.Uri) -> Unit = {},
    isSyncing: Boolean = false,
    syncingSourceId: Long? = null,
    allBooks: List<Book> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // 本地书源列表状态
    val localSources = remember { mutableStateListOf<LocalSourceItem>() }
    
    // 加载已保存的书源
    LaunchedEffect(Unit) {
        val savedSources = BookSourcesManager.loadSources(context)
        localSources.clear()
        localSources.addAll(savedSources)
    }
    
    // 计算每个书源的书籍数量
    val sourceBookCounts = remember(allBooks, localSources.toList()) {
        localSources.associate { source ->
            source.id to allBooks.count { book ->
                book.filePath.startsWith(source.uri) ||
                book.filePath.contains(source.uri)
            }
        }
    }
    
    // 新书源命名对话框状态
    var showNameDialog by remember { mutableStateOf(false) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var pendingFolderName by remember { mutableStateOf("") }
    var newSourceName by remember { mutableStateOf("") }
    
    // 编辑对话框状态
    var showEditDialog by remember { mutableStateOf(false) }
    var editingSource by remember { mutableStateOf<LocalSourceItem?>(null) }
    var editName by remember { mutableStateOf("") }
    
    // 删除确认对话框状态
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletingSource by remember { mutableStateOf<LocalSourceItem?>(null) }
    
    // 文件夹选择器
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            // 获取持久化权限
            try {
                context.contentResolver.takePersistableUriPermission(
                    selectedUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                // 忽略权限错误，继续处理
            }
            
            // 获取文件夹名称作为默认名称
            val documentFile = DocumentFile.fromTreeUri(context, selectedUri)
            pendingFolderName = documentFile?.name ?: "新书源"
            newSourceName = pendingFolderName
            pendingUri = selectedUri
            showNameDialog = true
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "书源",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            
            // Add folder button - 启动文件夹选择器
            Surface(
                onClick = { folderPickerLauncher.launch(null) },
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("添加", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        
        // Source list or empty state
        if (localSources.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "暂无书源",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "点击上方按钮添加本地文件夹",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Source list
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                localSources.forEach { source ->
                    val bookCount = sourceBookCounts[source.id] ?: 0
                    SourceItemCard(
                        source = source,
                        bookCount = bookCount,
                        isSyncing = isSyncing && (syncingSourceId == null || syncingSourceId == source.id),
                        onEdit = {
                            editingSource = source
                            editName = source.displayName
                            showEditDialog = true
                        },
                        onDelete = {
                            deletingSource = source
                            showDeleteDialog = true
                        },
                        onSync = {
                            val uri = android.net.Uri.parse(source.uri)
                            onScanSource(uri)
                        }
                    )
                }
            }
        }
        
        // Help text at bottom
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "书源文件夹中的书籍将显示在书库中",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    
    // 新书源命名对话框
    if (showNameDialog && pendingUri != null) {
        AlertDialog(
            onDismissRequest = { 
                showNameDialog = false
                pendingUri = null
            },
            title = { Text("设置书源名称") },
            text = {
                Column {
                    Text(
                        text = "文件夹: $pendingFolderName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = newSourceName,
                        onValueChange = { newSourceName = it },
                        label = { Text("书源名称") },
                        placeholder = { Text("例如：小说、漫画") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingUri?.let { uri ->
                            // 添加新书源到列表
                            val newSource = LocalSourceItem(
                                id = System.currentTimeMillis(),
                                displayName = newSourceName.ifBlank { pendingFolderName },
                                uri = uri.toString(),
                                bookCount = 0
                            )
                            localSources.add(newSource)
                            
                            // 保存到 SharedPreferences
                            BookSourcesManager.saveSources(context, localSources.toList())
                            
                            // 触发扫描
                            onScanSource(uri)
                        }
                        showNameDialog = false
                        pendingUri = null
                        newSourceName = ""
                    }
                ) {
                    Text("添加")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNameDialog = false
                        pendingUri = null
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }
    
    // Edit source name dialog
    if (showEditDialog && editingSource != null) {
        AlertDialog(
            onDismissRequest = { 
                showEditDialog = false
                editingSource = null
            },
            title = { Text("编辑书源名称") },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        editingSource?.let { source ->
                            // 更新列表中的书源
                            val index = localSources.indexOfFirst { it.id == source.id }
                            if (index >= 0) {
                                localSources[index] = source.copy(displayName = editName)
                                // 保存更新
                                BookSourcesManager.saveSources(context, localSources.toList())
                            }
                        }
                        showEditDialog = false
                        editingSource = null
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEditDialog = false
                        editingSource = null
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog && deletingSource != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false
                deletingSource = null
            },
            title = { Text("删除书源") },
            text = { 
                Text("确定要删除书源 \"${deletingSource?.displayName}\" 吗？此操作不会删除实际的文件。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletingSource?.let { source ->
                            localSources.removeIf { it.id == source.id }
                            // 保存更新
                            BookSourcesManager.saveSources(context, localSources.toList())
                        }
                        showDeleteDialog = false
                        deletingSource = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        deletingSource = null
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SourceItemCard(
    source: LocalSourceItem,
    bookCount: Int = 0,
    isSyncing: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSync: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Folder icon with optional sync indicator
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Name and book count
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = source.displayName,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = if (isSyncing) "同步中..." else "$bookCount 本书",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSyncing) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Sync button
                IconButton(
                    onClick = onSync, 
                    modifier = Modifier.size(36.dp),
                    enabled = !isSyncing
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "同步",
                        modifier = Modifier.size(18.dp),
                        tint = if (isSyncing) 
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                }
                
                // Edit button
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Delete button
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * 支持的格式展示区域
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SupportedFormatsSection() {
    Column {
        Text(
            text = "支持的格式",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 漫画格式
            FormatChip(label = "PDF", color = MaterialTheme.colorScheme.errorContainer)
            FormatChip(label = "CBZ", color = MaterialTheme.colorScheme.primaryContainer)
            FormatChip(label = "CBR", color = MaterialTheme.colorScheme.primaryContainer)
            FormatChip(label = "CB7", color = MaterialTheme.colorScheme.primaryContainer)
            FormatChip(label = "CBT", color = MaterialTheme.colorScheme.primaryContainer)
            
            // 压缩格式
            FormatChip(label = "ZIP", color = MaterialTheme.colorScheme.tertiaryContainer)
            FormatChip(label = "RAR", color = MaterialTheme.colorScheme.tertiaryContainer)
            FormatChip(label = "7Z", color = MaterialTheme.colorScheme.tertiaryContainer)
            FormatChip(label = "TAR", color = MaterialTheme.colorScheme.tertiaryContainer)
            
            // 小说格式
            FormatChip(label = "EPUB", color = MaterialTheme.colorScheme.secondaryContainer)
            FormatChip(label = "TXT", color = MaterialTheme.colorScheme.secondaryContainer)
        }
    }
}

/**
 * 格式标签 Chip
 */
@Composable
private fun FormatChip(
    label: String,
    color: androidx.compose.ui.graphics.Color
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
