package takagi.ru.paysage.data

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import takagi.ru.paysage.data.model.BookFormat
import takagi.ru.paysage.data.model.CategoryType
import takagi.ru.paysage.data.model.PageTurnDirection
import takagi.ru.paysage.data.model.ReadingMode

/**
 * Room 数据库类型转换器
 */
class Converters {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    // BookFormat 转换
    @TypeConverter
    fun fromBookFormat(format: BookFormat): String {
        return format.name
    }
    
    @TypeConverter
    fun toBookFormat(value: String): BookFormat {
        return BookFormat.valueOf(value)
    }
    
    // ReadingMode 转换
    @TypeConverter
    fun fromReadingMode(mode: ReadingMode): String {
        return mode.name
    }
    
    @TypeConverter
    fun toReadingMode(value: String): ReadingMode {
        return ReadingMode.valueOf(value)
    }
    
    // PageTurnDirection 转换
    @TypeConverter
    fun fromPageTurnDirection(direction: PageTurnDirection): String {
        return direction.name
    }
    
    @TypeConverter
    fun toPageTurnDirection(value: String): PageTurnDirection {
        return PageTurnDirection.valueOf(value)
    }
    
    // CategoryType 转换
    @TypeConverter
    fun fromCategoryType(type: CategoryType): String {
        return type.name
    }
    
    @TypeConverter
    fun toCategoryType(value: String): CategoryType {
        return try {
            CategoryType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            CategoryType.MANGA // 默认值
        }
    }
    
    // List<String> 转换
    @TypeConverter
    fun fromStringList(list: List<String>): String {
        return json.encodeToString(list)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() 
        else json.decodeFromString(value)
    }
}
