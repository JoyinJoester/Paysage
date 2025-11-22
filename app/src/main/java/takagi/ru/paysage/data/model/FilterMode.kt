package takagi.ru.paysage.data.model

/**
 * 书库筛选模式
 */
enum class FilterMode {
    ALL,           // 全部书籍
    FAVORITES,     // 收藏
    RECENT,        // 最近阅读
    CATEGORIES,    // 分类列表
    CATEGORY       // 特定分类的书籍
}

/**
 * 分类信息
 */
data class CategoryInfo(
    val name: String,
    val bookCount: Int
)

/**
 * 分类统计结果（用于 Room 查询）
 */
data class CategoryCount(
    val category: String,
    val count: Int
)
