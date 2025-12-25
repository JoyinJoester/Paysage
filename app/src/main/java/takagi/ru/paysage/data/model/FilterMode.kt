package takagi.ru.paysage.data.model

/**
 * 书库筛选模式
 */
enum class FilterMode {
    ALL,           // 全部书籍
    FAVORITES,     // 收藏
    RECENT,        // 最近阅读
    CATEGORIES,    // 分类列表
    CATEGORY,      // 特定分类的书籍
    AUTHOR,        // 按作者分组
    SERIES,        // 按系列分组
    YEAR,          // 按年度分组
    SOURCE_FOLDER  // 按书源文件夹分组
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

/**
 * 分组信息（用于分组视图显示）
 */
data class GroupInfo(
    val name: String,       // 分组名称（如作者名、系列名、年份等）
    val bookCount: Int,     // 该分组下的书籍数量
    val books: List<Book>   // 该分组下的书籍列表
)
