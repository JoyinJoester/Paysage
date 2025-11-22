package takagi.ru.paysage.reader.theme

import androidx.compose.ui.graphics.Color

/**
 * 阅读主题
 * 
 * 定义阅读器的颜色方案和样式
 */
data class ReaderTheme(
    val id: String,
    val name: String,
    val backgroundColor: Color,
    val textColor: Color,
    val toolbarBackgroundColor: Color,
    val toolbarTextColor: Color,
    val accentColor: Color,
    val isDark: Boolean
) {
    companion object {
        /**
         * 默认主题（白色背景）
         */
        val Default = ReaderTheme(
            id = "default",
            name = "默认",
            backgroundColor = Color(0xFFFFFFFF),
            textColor = Color(0xFF000000),
            toolbarBackgroundColor = Color(0xFFF5F5F5),
            toolbarTextColor = Color(0xFF000000),
            accentColor = Color(0xFF6200EE),
            isDark = false
        )

        /**
         * 护眼主题（淡绿色背景）
         */
        val EyeCare = ReaderTheme(
            id = "eye_care",
            name = "护眼",
            backgroundColor = Color(0xFFC7EDCC),
            textColor = Color(0xFF1B1B1B),
            toolbarBackgroundColor = Color(0xFFB0E0B6),
            toolbarTextColor = Color(0xFF1B1B1B),
            accentColor = Color(0xFF4CAF50),
            isDark = false
        )

        /**
         * 夜间主题（深色背景）
         */
        val Night = ReaderTheme(
            id = "night",
            name = "夜间",
            backgroundColor = Color(0xFF1E1E1E),
            textColor = Color(0xFFE0E0E0),
            toolbarBackgroundColor = Color(0xFF2D2D2D),
            toolbarTextColor = Color(0xFFE0E0E0),
            accentColor = Color(0xFFBB86FC),
            isDark = true
        )

        /**
         * 羊皮纸主题（米黄色背景）
         */
        val Parchment = ReaderTheme(
            id = "parchment",
            name = "羊皮纸",
            backgroundColor = Color(0xFFF4ECD8),
            textColor = Color(0xFF3E2723),
            toolbarBackgroundColor = Color(0xFFE8DCC0),
            toolbarTextColor = Color(0xFF3E2723),
            accentColor = Color(0xFF8D6E63),
            isDark = false
        )

        /**
         * 深蓝主题（深蓝色背景）
         */
        val DeepBlue = ReaderTheme(
            id = "deep_blue",
            name = "深蓝",
            backgroundColor = Color(0xFF0D1B2A),
            textColor = Color(0xFFE0E1DD),
            toolbarBackgroundColor = Color(0xFF1B263B),
            toolbarTextColor = Color(0xFFE0E1DD),
            accentColor = Color(0xFF415A77),
            isDark = true
        )

        /**
         * 所有预设主题
         */
        val AllThemes = listOf(
            Default,
            EyeCare,
            Night,
            Parchment,
            DeepBlue
        )

        /**
         * 根据 ID 获取主题
         */
        fun getThemeById(id: String): ReaderTheme {
            return AllThemes.find { it.id == id } ?: Default
        }
    }
}

/**
 * 自定义主题构建器
 */
class ReaderThemeBuilder {
    private var id: String = "custom"
    private var name: String = "自定义"
    private var backgroundColor: Color = Color.White
    private var textColor: Color = Color.Black
    private var toolbarBackgroundColor: Color = Color.LightGray
    private var toolbarTextColor: Color = Color.Black
    private var accentColor: Color = Color.Blue
    private var isDark: Boolean = false

    fun id(id: String) = apply { this.id = id }
    fun name(name: String) = apply { this.name = name }
    fun backgroundColor(color: Color) = apply { this.backgroundColor = color }
    fun textColor(color: Color) = apply { this.textColor = color }
    fun toolbarBackgroundColor(color: Color) = apply { this.toolbarBackgroundColor = color }
    fun toolbarTextColor(color: Color) = apply { this.toolbarTextColor = color }
    fun accentColor(color: Color) = apply { this.accentColor = color }
    fun isDark(isDark: Boolean) = apply { this.isDark = isDark }

    fun build(): ReaderTheme {
        return ReaderTheme(
            id = id,
            name = name,
            backgroundColor = backgroundColor,
            textColor = textColor,
            toolbarBackgroundColor = toolbarBackgroundColor,
            toolbarTextColor = toolbarTextColor,
            accentColor = accentColor,
            isDark = isDark
        )
    }
}
