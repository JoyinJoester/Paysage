package takagi.ru.paysage.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 分类实体类
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val name: String,
    val description: String? = null,
    val color: Int? = null,
    val icon: String? = null,
    val bookCount: Int = 0,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
