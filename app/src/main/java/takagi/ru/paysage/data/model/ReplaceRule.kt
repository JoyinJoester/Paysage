package takagi.ru.paysage.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 替换规则数据模型
 */
@Entity(tableName = "replace_rules")
data class ReplaceRule(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    // 规则名称
    val name: String,
    
    // 匹配模式
    val pattern: String,
    
    // 替换内容
    val replacement: String,
    
    // 是否使用正则表达式
    val isRegex: Boolean = false,
    
    // 是否启用
    val isEnabled: Boolean = true,
    
    // 应用范围
    val scope: ReplaceScope = ReplaceScope.ALL,
    
    // 排序顺序
    val order: Int = 0,
    
    // 作用的书籍ID列表(当scope为BOOK时使用)
    val bookIds: String = "", // 逗号分隔的书籍ID
    
    // 作用的书源ID列表(当scope为SOURCE时使用)
    val sourceIds: String = "" // 逗号分隔的书源ID
)

/**
 * 替换规则应用范围
 */
enum class ReplaceScope {
    ALL,      // 全部书籍
    BOOK,     // 指定书籍
    SOURCE    // 指定书源
}
