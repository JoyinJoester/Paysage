package takagi.ru.paysage.ui.state

import takagi.ru.paysage.data.model.Book
import takagi.ru.paysage.data.model.Bookmark
import takagi.ru.paysage.data.model.ReaderConfig

/**
 * Legado阅读器UI状态
 */
data class LegadoReaderUiState(
    // 书籍信息
    val book: Book? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // 阅读位置
    val currentChapter: Int = 0,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val chapterTitle: String = "",
    val chapterList: List<String> = emptyList(),
    
    // UI显示状态
    val showTopBar: Boolean = false,
    val showBottomBar: Boolean = false,
    val showMenu: Boolean = false,
    
    // 对话框状态
    val showReadStyleDialog: Boolean = false,
    val showBgTextConfigDialog: Boolean = false,
    val showMoreConfigDialog: Boolean = false,
    val showTipConfigDialog: Boolean = false,
    val showPaddingConfigDialog: Boolean = false,
    val showClickActionConfigDialog: Boolean = false,
    val showBookmarkDialog: Boolean = false,
    val showSearchDialog: Boolean = false,
    val showAutoReadDialog: Boolean = false,
    val showReadAloudDialog: Boolean = false,
    val showContentEditDialog: Boolean = false,
    val showReplaceRuleDialog: Boolean = false,
    
    // 阅读配置
    val config: ReaderConfig = ReaderConfig(),
    
    // 书签列表
    val bookmarks: List<Bookmark> = emptyList(),
    
    // 文本选择
    val selectedText: String? = null,
    val showTextActionMenu: Boolean = false,
    
    // 搜索状态
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val currentSearchIndex: Int = -1,
    
    // 自动阅读状态
    val isAutoReading: Boolean = false,
    val autoReadSpeed: Int = 50,
    
    // 朗读状态
    val isReadingAloud: Boolean = false,
    val readAloudSpeed: Int = 5,
    val readAloudProgress: Float = 0f,
    
    // 提示消息
    val message: String? = null
)

/**
 * 搜索结果
 */
data class SearchResult(
    val chapterIndex: Int,
    val chapterTitle: String,
    val pageIndex: Int,
    val content: String,
    val position: Int
)
