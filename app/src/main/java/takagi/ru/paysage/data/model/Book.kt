package takagi.ru.paysage.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 书籍实体类
 */
@Entity(
    tableName = "books",
    indices = [
        Index(value = ["lastReadAt"], name = "index_books_lastReadAt"),
        Index(value = ["categoryType"], name = "index_books_categoryType"),
        Index(value = ["categoryType", "isOnline"], name = "index_books_category_online"),
        Index(value = ["categoryType", "lastReadAt"], name = "index_books_category_read")
    ]
)
data class Book(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // 基本信息
    val title: String,
    val filePath: String,
    val fileSize: Long,
    val fileFormat: BookFormat,
    
    // 元数据
    val coverPath: String? = null,
    val author: String? = null,
    val publisher: String? = null,
    val description: String? = null,
    
    // 阅读信息
    val totalPages: Int = 0,
    val currentPage: Int = 0,
    val isFinished: Boolean = false,
    val isFavorite: Boolean = false,
    
    // 分类和标签
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val sortPreference: String? = null,
    
    // 新增：分类系统字段
    val categoryType: CategoryType = CategoryType.MANGA,  // 分类类型（漫画/小说）
    val isOnline: Boolean = false,                        // 是否为在线书籍
    val sourceId: Long? = null,                           // 书源ID（在线书籍）
    val sourceUrl: String? = null,                        // 书源URL（在线书籍）
    
    // 时间戳
    val addedAt: Long = System.currentTimeMillis(),
    val lastReadAt: Long? = null,
    val lastModifiedAt: Long = System.currentTimeMillis()
)

/**
 * 书籍格式枚举
 */
enum class BookFormat(val extension: String, val mimeType: String) {
    PDF("pdf", "application/pdf"),
    CBZ("cbz", "application/x-cbz"),
    CBR("cbr", "application/x-cbr"),
    CBT("cbt", "application/x-cbt"),
    CB7("cb7", "application/x-cb7"),
    ZIP("zip", "application/zip"),
    RAR("rar", "application/x-rar-compressed"),
    TAR("tar", "application/x-tar"),
    SEVEN_ZIP("7z", "application/x-7z-compressed");
    
    companion object {
        fun fromExtension(ext: String): BookFormat? {
            return values().find { 
                it.extension.equals(ext, ignoreCase = true) 
            }
        }
        
        fun fromFileName(fileName: String): BookFormat? {
            val ext = fileName.substringAfterLast('.', "")
            return fromExtension(ext)
        }
    }
}

/**
 * 书籍阅读状态
 */
enum class BookReadingStatus {
    UNREAD,    // 未读
    READING,   // 阅读中
    FINISHED,  // 已读
    LATEST     // 最新（最近添加的书籍）
}

/**
 * 获取书籍的阅读状态
 */
fun Book.getReadingStatus(): BookReadingStatus {
    return when {
        isFinished -> BookReadingStatus.FINISHED
        currentPage > 0 -> BookReadingStatus.READING
        // 判断是否为最新（7天内添加且从未打开过的书籍）
        lastReadAt == null && System.currentTimeMillis() - addedAt < 7 * 24 * 60 * 60 * 1000 -> BookReadingStatus.LATEST
        else -> BookReadingStatus.UNREAD
    }
}

/**
 * 阅读模式
 */
enum class ReadingMode {
    SINGLE_PAGE,      // 单页模式
    DOUBLE_PAGE,      // 双页模式
    CONTINUOUS_SCROLL // 连续滚动模式
}

/**
 * 翻页方向
 */
enum class PageTurnDirection {
    LEFT_TO_RIGHT,  // 从左到右
    RIGHT_TO_LEFT,  // 从右到左（漫画模式）
    VERTICAL        // 垂直滚动
}
