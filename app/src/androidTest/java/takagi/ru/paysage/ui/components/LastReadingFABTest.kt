package takagi.ru.paysage.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import takagi.ru.paysage.data.model.Book
import takagi.ru.paysage.data.model.BookFormat

/**
 * LastReadingFAB UI 测试
 */
@RunWith(AndroidJUnit4::class)
class LastReadingFABTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    /**
     * 测试：无阅读记录时不显示 FAB
     */
    @Test
    fun fab_notDisplayed_whenNoLastReadBook() {
        // Given
        var clicked = false
        
        // When
        composeTestRule.setContent {
            LastReadingFAB(
                lastReadBook = null,
                onClick = { clicked = true }
            )
        }
        
        // Then
        composeTestRule.onNodeWithContentDescription("继续阅读", substring = true)
            .assertDoesNotExist()
    }
    
    /**
     * 测试：有阅读记录时显示 FAB
     */
    @Test
    fun fab_displayed_whenHasLastReadBook() {
        // Given
        val book = Book(
            id = 1,
            title = "Test Book",
            filePath = "/test/book.cbz",
            fileSize = 1000,
            fileFormat = BookFormat.CBZ,
            totalPages = 100,
            currentPage = 50,
            lastReadAt = System.currentTimeMillis()
        )
        
        // When
        composeTestRule.setContent {
            LastReadingFAB(
                lastReadBook = book,
                onClick = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithContentDescription("继续阅读 Test Book")
            .assertIsDisplayed()
    }
    
    /**
     * 测试：显示正确的进度
     */
    @Test
    fun fab_showsCorrectProgress() {
        // Given
        val book = Book(
            id = 1,
            title = "Test Book",
            filePath = "/test/book.cbz",
            fileSize = 1000,
            fileFormat = BookFormat.CBZ,
            totalPages = 100,
            currentPage = 50, // 50%
            lastReadAt = System.currentTimeMillis()
        )
        
        // When
        composeTestRule.setContent {
            LastReadingFAB(
                lastReadBook = book,
                onClick = {}
            )
        }
        
        // Then - 检查语义描述中包含进度信息
        composeTestRule.onNode(
            hasStateDescription("阅读进度 50%")
        ).assertIsDisplayed()
    }
    
    /**
     * 测试：点击后触发回调
     */
    @Test
    fun fab_navigatesToBook_whenClicked() {
        // Given
        val book = Book(
            id = 1,
            title = "Test Book",
            filePath = "/test/book.cbz",
            fileSize = 1000,
            fileFormat = BookFormat.CBZ,
            totalPages = 100,
            currentPage = 50,
            lastReadAt = System.currentTimeMillis()
        )
        var clicked = false
        
        // When
        composeTestRule.setContent {
            LastReadingFAB(
                lastReadBook = book,
                onClick = { clicked = true }
            )
        }
        
        // Then
        composeTestRule.onNodeWithContentDescription("继续阅读 Test Book")
            .performClick()
        
        assert(clicked)
    }
    
    /**
     * 测试：FAB 可点击
     */
    @Test
    fun fab_isClickable() {
        // Given
        val book = Book(
            id = 1,
            title = "Test Book",
            filePath = "/test/book.cbz",
            fileSize = 1000,
            fileFormat = BookFormat.CBZ,
            totalPages = 100,
            currentPage = 50,
            lastReadAt = System.currentTimeMillis()
        )
        
        // When
        composeTestRule.setContent {
            LastReadingFAB(
                lastReadBook = book,
                onClick = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithContentDescription("继续阅读 Test Book")
            .assertHasClickAction()
    }
    
    /**
     * 测试：进度为 0% 时正确显示
     */
    @Test
    fun fab_showsZeroProgress_whenNotStarted() {
        // Given
        val book = Book(
            id = 1,
            title = "Test Book",
            filePath = "/test/book.cbz",
            fileSize = 1000,
            fileFormat = BookFormat.CBZ,
            totalPages = 100,
            currentPage = 0, // 0%
            lastReadAt = System.currentTimeMillis()
        )
        
        // When
        composeTestRule.setContent {
            LastReadingFAB(
                lastReadBook = book,
                onClick = {}
            )
        }
        
        // Then
        composeTestRule.onNode(
            hasStateDescription("阅读进度 0%")
        ).assertIsDisplayed()
    }
    
    /**
     * 测试：进度为 100% 时正确显示
     */
    @Test
    fun fab_showsFullProgress_whenFinished() {
        // Given
        val book = Book(
            id = 1,
            title = "Test Book",
            filePath = "/test/book.cbz",
            fileSize = 1000,
            fileFormat = BookFormat.CBZ,
            totalPages = 100,
            currentPage = 100, // 100%
            lastReadAt = System.currentTimeMillis()
        )
        
        // When
        composeTestRule.setContent {
            LastReadingFAB(
                lastReadBook = book,
                onClick = {}
            )
        }
        
        // Then
        composeTestRule.onNode(
            hasStateDescription("阅读进度 100%")
        ).assertIsDisplayed()
    }
}
