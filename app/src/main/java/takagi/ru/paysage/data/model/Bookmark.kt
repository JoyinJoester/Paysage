package takagi.ru.paysage.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 书签数据模型
 */
@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // 书籍ID
    val bookId: Long,
    
    // 页码
    val pageNumber: Int,
    
    // 书签标题
    val title: String? = null,
    
    // 书签备注
    val note: String = "",
    
    // 创建时间
    val createdAt: Long = System.currentTimeMillis()
)
