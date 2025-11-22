package takagi.ru.paysage.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 在线书源实体类
 * 用于管理在线阅读的书源配置
 */
@Entity(
    tableName = "book_sources",
    indices = [
        Index(value = ["categoryType"], name = "index_sources_categoryType"),
        Index(value = ["isEnabled"], name = "index_sources_enabled"),
        Index(value = ["priority"], name = "index_sources_priority")
    ]
)
data class BookSource(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // 基本信息
    val name: String,                           // 书源名称
    val baseUrl: String,                        // 基础URL
    val categoryType: CategoryType,             // 支持的分类类型
    
    // 配置信息
    val isEnabled: Boolean = true,              // 是否启用
    val priority: Int = 0,                      // 优先级（数字越大优先级越高）
    
    // 书源规则（JSON格式）
    val searchRule: String? = null,             // 搜索规则
    val bookInfoRule: String? = null,           // 书籍信息规则
    val chapterListRule: String? = null,        // 章节列表规则
    val contentRule: String? = null,            // 内容规则
    
    // 统计信息
    val totalBooks: Int = 0,                    // 书源中的书籍总数
    val successRate: Float = 0f,                // 成功率（0-1）
    
    // 时间戳
    val lastUsedAt: Long? = null,               // 最后使用时间
    val addedAt: Long = System.currentTimeMillis()  // 添加时间
)

/**
 * 书源类型
 */
enum class BookSourceType {
    WEB,        // 网页书源
    API,        // API书源
    RSS         // RSS订阅源
}

/**
 * 书源状态
 */
enum class BookSourceStatus {
    ACTIVE,     // 活跃
    INACTIVE,   // 不活跃
    ERROR       // 错误
}
