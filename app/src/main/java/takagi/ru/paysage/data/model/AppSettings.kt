package takagi.ru.paysage.data.model

import takagi.ru.paysage.util.ImageFilter

/**
 * 应用设置数据类
 */
data class AppSettings(
    // 主题设置
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorScheme: ColorScheme = ColorScheme.DEFAULT,
    val dynamicColorEnabled: Boolean = true, // 动态颜色（壁纸取色）默认开启
    val customPrimaryColor: Long = 0xFFFF6B35, // 自定义主色（活力橙）
    val customSecondaryColor: Long = 0xFF6A4C9C, // 自定义次要色（深紫）
    val customTertiaryColor: Long = 0xFF00BFA5, // 自定义第三色（青绿）
    
    // 语言设置
    val language: Language = Language.SYSTEM,
    
    // 阅读设置
    val readingDirection: ReadingDirection = ReadingDirection.LEFT_TO_RIGHT,
    val readingMode: ReadingMode = ReadingMode.SINGLE_PAGE,
    val keepScreenOn: Boolean = true,
    val volumeKeyNavigation: Boolean = true,
    
    // 图片过滤设置
    val imageFilter: ImageFilter = ImageFilter(),
    
    // 扫描设置
    val scanPaths: List<String> = emptyList(),
    val autoScanOnStart: Boolean = false,
    val lastScannedFolderUri: String? = null, // 最后扫描的文件夹URI
    
    // 显示设置
    val showProgress: Boolean = true,
    val gridColumns: Int = 3,
    val libraryLayout: LibraryLayout = LibraryLayout.COVER_ONLY, // 默认纯封面网格
    
    // 其他
    val enableAnalytics: Boolean = false,
    val lastVersion: String = "",
    
    // 调试设置
    val showPerformanceOverlay: Boolean = false,  // 显示性能监控覆盖层
    
    // 文本阅读器设置
    val textSize: Int = 18,  // 文字大小（sp）
    val textColor: Int = 0xFF000000.toInt(),  // 文字颜色
    val bgColor: Int = 0xFFFFFFFF.toInt(),  // 背景颜色
    val lineSpacing: Float = 1.5f,  // 行间距倍数
    val paragraphSpacing: Float = 1.0f,  // 段落间距倍数
    val paddingLeft: Int = 24,  // 左边距（dp）
    val paddingRight: Int = 24,  // 右边距（dp）
    val paddingTop: Int = 32,  // 上边距（dp）
    val paddingBottom: Int = 32  // 下边距（dp）
)

/**
 * 主题模式
 */
enum class ThemeMode {
    LIGHT,      // 明亮
    DARK,       // 暗色
    SYSTEM      // 跟随系统
}

/**
 * 配色方案
 */
enum class ColorScheme {
    DEFAULT,         // 默认（Material 3 紫色）
    OCEAN_BLUE,      // 海洋蓝
    SUNSET_ORANGE,   // 日落橙
    FOREST_GREEN,    // 森林绿
    TECH_PURPLE,     // 科技紫
    BLACK_MAMBA,     // 黑曼巴（Kobe Lakers）
    GREY_STYLE,      // 小黑紫（Cai Xukun）
    CUSTOM           // 自定义
}

/**
 * 阅读方向
 */
enum class ReadingDirection {
    LEFT_TO_RIGHT,  // 从左到右
    RIGHT_TO_LEFT,  // 从右到左
    VERTICAL        // 垂直滚动
}

/**
 * 语言设置
 */
enum class Language {
    SYSTEM,    // 跟随系统
    ENGLISH,   // 英语
    CHINESE    // 中文
}

/**
 * 库布局类型
 */
enum class LibraryLayout {
    LIST,           // 列表视图（详细信息）
    COMPACT_GRID,   // 紧凑网格（大卡片）
    COVER_ONLY      // 纯封面网格
}
