package takagi.ru.paysage.ui.screens

import takagi.ru.paysage.data.model.Book
import takagi.ru.paysage.reader.touch.TouchZoneConfig

/**
 * M3E 阅读器 UI 状态
 * 
 * @property book 当前书籍
 * @property currentPage 当前页码
 * @property totalPages 总页数
 * @property chapterTitle 章节标题
 * @property isLoading 是否加载中
 * @property error 错误信息
 * @property showTopBar 是否显示顶部工具栏
 * @property showBottomBar 是否显示底部工具栏
 * @property showQuickSettings 是否显示快速设置面板
 * @property showReadingSettings 是否显示阅读设置对话框
 * @property config 阅读配置
 * @property touchZoneConfig 触摸区域配置
 * @property showTouchZoneOverlay 是否显示触摸区域覆盖层
 */
data class ReaderUiState(
    val book: Book? = null,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val chapterTitle: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // 工具栏状态
    val showTopBar: Boolean = false,
    val showBottomBar: Boolean = false,
    val showQuickSettings: Boolean = false,
    val showReadingSettings: Boolean = false,
    
    // 阅读配置
    val config: takagi.ru.paysage.reader.ReaderConfig = takagi.ru.paysage.reader.ReaderConfig(),
    
    // 触摸区域
    val touchZoneConfig: TouchZoneConfig = TouchZoneConfig.default(),
    val showTouchZoneOverlay: Boolean = false
)
