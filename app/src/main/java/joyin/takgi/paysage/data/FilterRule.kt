package joyin.takgi.paysage.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class FilterType {
    WHITELIST,
    BLACKLIST,
    KEYWORD
}

@Entity(tableName = "filter_rules")
data class FilterRule(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: FilterType,
    val value: String,
    val isEnabled: Boolean = true
)
