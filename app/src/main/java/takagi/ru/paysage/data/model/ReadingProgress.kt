package takagi.ru.paysage.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 阅读进度实体类
 */
@Entity(
    tableName = "reading_progress",
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bookId")]
)
data class ReadingProgress(
    @PrimaryKey
    val bookId: Long,
    
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val progress: Float = 0f, // 0.0 - 1.0
    
    // 阅读设置
    val readingMode: ReadingMode = ReadingMode.SINGLE_PAGE,
    val pageTurnDirection: PageTurnDirection = PageTurnDirection.LEFT_TO_RIGHT,
    val zoomLevel: Float = 1.0f,
    val scrollX: Float = 0f,
    val scrollY: Float = 0f,
    
    // 统计信息
    val totalReadingTime: Long = 0, // 毫秒
    val lastReadPosition: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
