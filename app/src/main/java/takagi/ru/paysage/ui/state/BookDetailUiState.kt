package takagi.ru.paysage.ui.state

import takagi.ru.paysage.data.model.Book

/**
 * 书籍详情底部弹窗的 UI 状态
 */
data class BookDetailUiState(
    val isVisible: Boolean = false,
    val selectedBook: Book? = null,
    val isEditingTags: Boolean = false,
    val tempTags: List<String> = emptyList(),
    val showDeleteConfirmation: Boolean = false
)
