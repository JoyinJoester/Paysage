package joyin.takgi.paysage.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class FilterType {
    WHITELIST,
    BLACKLIST,
    KEYWORD,
    BODY_KEYWORD_BLOCK,
    BODY_REGEX_BLOCK,
    BODY_KEYWORD_ALLOW
}

@Entity(tableName = "filter_rules")
data class FilterRule(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: FilterType,
    val value: String,
    val isEnabled: Boolean = true
)
