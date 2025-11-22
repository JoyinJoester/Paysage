package takagi.ru.paysage.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 阅读历史记录数据类
 */
data class HistoryItem(
    val id: Long = 0,
    val bookId: Long,           // 关联的书籍ID
    val title: String,          // 书名
    val author: String = "",    // 作者
    val thumbnailPath: String?, // 封面路径
    val fileType: String,       // 文件类型（ZIP, PDF等）
    val fileSize: Long,         // 文件大小
    val filePath: String,       // 文件路径
    val lastReadTime: Long,     // 最后阅读时间
    val progress: Float,        // 阅读进度 (0.0 - 1.0)
    val currentPage: Int = 0,   // 当前页码
    val totalPages: Int = 0     // 总页数
)

/**
 * 阅读历史记录实体类（Room）
 */
@Entity(
    tableName = "reading_history",
    indices = [
        Index(value = ["last_read_time"], name = "idx_history_last_read_time"),
        Index(value = ["book_id"], name = "idx_history_book_id")
    ]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "book_id")
    val bookId: Long,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "author")
    val author: String,
    
    @ColumnInfo(name = "thumbnail_path")
    val thumbnailPath: String?,
    
    @ColumnInfo(name = "file_type")
    val fileType: String,
    
    @ColumnInfo(name = "file_size")
    val fileSize: Long,
    
    @ColumnInfo(name = "file_path")
    val filePath: String,
    
    @ColumnInfo(name = "last_read_time")
    val lastReadTime: Long,
    
    @ColumnInfo(name = "progress")
    val progress: Float,
    
    @ColumnInfo(name = "current_page")
    val currentPage: Int,
    
    @ColumnInfo(name = "total_pages")
    val totalPages: Int
)

/**
 * 扩展函数：HistoryEntity 转 HistoryItem
 */
fun HistoryEntity.toHistoryItem(): HistoryItem {
    return HistoryItem(
        id = id,
        bookId = bookId,
        title = title,
        author = author,
        thumbnailPath = thumbnailPath,
        fileType = fileType,
        fileSize = fileSize,
        filePath = filePath,
        lastReadTime = lastReadTime,
        progress = progress,
        currentPage = currentPage,
        totalPages = totalPages
    )
}

/**
 * 扩展函数：HistoryItem 转 HistoryEntity
 */
fun HistoryItem.toHistoryEntity(): HistoryEntity {
    return HistoryEntity(
        id = id,
        bookId = bookId,
        title = title,
        author = author,
        thumbnailPath = thumbnailPath,
        fileType = fileType,
        fileSize = fileSize,
        filePath = filePath,
        lastReadTime = lastReadTime,
        progress = progress,
        currentPage = currentPage,
        totalPages = totalPages
    )
}
