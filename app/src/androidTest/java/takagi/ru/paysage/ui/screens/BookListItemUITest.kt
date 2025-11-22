package takagi.ru.paysage.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import takagi.ru.paysage.data.model.Book
import takagi.ru.paysage.data.model.BookFormat
import takagi.ru.paysage.ui.theme.PaysageTheme

/**
 * UI 测试：BookListItem 组件渲染和交互
 */
@RunWith(AndroidJUnit4::class)
class BookListItemUITest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    /**
     * 测试有封面的书籍渲染
     */
    @Test
    fun testBookListItemWithCover() {
        val book = Book(
            id = 1,
            title = "Test Book with Cover",
            filePath = "/test/path",
            fileSize = 1000,
            fileFormat = BookFormat.PDF,
            coverPath = "/test/cover.jpg",
            author = "Test Author",
            totalPages = 100,
            currentPage = 50,
            isFinished = false,
            addedAt = System.currentTimeMillis()
        )
        
        composeTestRule.setContent {
            PaysageTheme {
                BookListItem(
                    book = book,
                    showProgress = true,
                    onClick = {}
                )
            }
        }
        
        // 验证标题显示
        composeTestRule.onNodeWithText("Test Book with Cover").assertExists()
        
        // 验证作者显示
        composeTestRule.onNodeWithText("Test Author").assertExists()
        
        // 验证页数显示
        composeTestRule.onNodeWithText("100 页").assertExists()
        
        // 验证文件格式显示
        composeTestRule.onNodeWithText("PDF").assertExists()
        
        // 验证进度文本显示
        composeTestRule.onNodeWithText("50/100").assertExists()
    }
    
    /**
     * 测试无封面的书籍渲染
     */
    @Test
    fun testBookListItemWithoutCover() {
        val book = Book(
            id = 1,
            title = "Test Book without Cover",
            filePath = "/test/path",
            fileSize = 1000,
            fileFormat = BookFormat.CBZ,
            coverPath = null,
            author = "Test Author",
            totalPages = 200,
            currentPage = 0,
            isFinished = false,
            addedAt = System.currentTimeMillis()
        )
        
        composeTestRule.setContent {
            PaysageTheme {
                BookListItem(
                    book = book,
                    showProgress = true,
                    onClick = {}
                )
            }
        }
        
        // 验证标题显示
        composeTestRule.onNodeWithText("Test Book without Cover").assertExists()
        
        // 验证作者显示
        composeTestRule.onNodeWithText("Test Author").assertExists()
        
        // 验证页数显示
        composeTestRule.onNodeWithText("200 页").assertExists()
        
        // 验证文件格式显示
        composeTestRule.onNodeWithText("CBZ").assertExists()
    }
    
    /**
     * 测试无作者信息的书籍渲染
     */
    @Test
    fun testBookListItemWithoutAuthor() {
        val book = Book(
            id = 1,
            title = "Test Book without Author",
            filePath = "/test/path",
            fileSize = 1000,
            fileFormat = BookFormat.ZIP,
            coverPath = null,
            author = null,
            totalPages = 150,
            currentPage = 75,
            isFinished = false,
            addedAt = System.currentTimeMillis()
        )
        
        composeTestRule.setContent {
            PaysageTheme {
                BookListItem(
                    book = book,
                    showProgress = true,
                    onClick = {}
                )
            }
        }
        
        // 验证标题显示
        composeTestRule.onNodeWithText("Test Book without Author").assertExists()
        
        // 验证页数显示
        composeTestRule.onNodeWithText("150 页").assertExists()
        
        // 验证文件格式显示
        composeTestRule.onNodeWithText("ZIP").assertExists()
        
        // 验证进度文本显示
        composeTestRule.onNodeWithText("75/150").assertExists()
    }
    
    /**
     * 测试未读状态的书籍渲染
     */
    @Test
    fun testBookListItemUnreadStatus() {
        val book = Book(
            id = 1,
            title = "Unread Book",
            filePath = "/test/path",
            fileSize = 1000,
            fileFormat = BookFormat.PDF,
            coverPath = null,
            author = "Test Author",
            totalPages = 100,
            currentPage = 0,
            isFinished = false,
            addedAt = System.currentTimeMillis() - (8 * 24 * 60 * 60 * 1000) // 8天前
        )
        
        composeTestRule.setContent {
            PaysageTheme {
                BookListItem(
                    book = book,
                    showProgress = true,
                    onClick = {}
                )
            }
        }
        
        // 验证未读状态标签显示
        composeTestRule.onNodeWithText("未读").assertExists()
    }
    
    /**
     * 测试阅读中状态的书籍渲染
     */
    @Test
    fun testBookListItemReadingStatus() {
        val book = Book(
            id = 1,
            title = "Reading Book",
            filePath = "/test/path",
            fileSize = 1000,
            fileFormat = BookFormat.PDF,
            coverPath = null,
            author = "Test Author",
            totalPages = 100,
            currentPage = 45,
            isFinished = false,
            addedAt = System.currentTimeMillis()
        )
        
        composeTestRule.setContent {
            PaysageTheme {
                BookListItem(
                    book = book,
                    showProgress = true,
                    onClick = {}
                )
            }
        }
        
        // 验证阅读中状态标签显示
        composeTestRule.onNodeWithText("阅读中").assertExists()
        
        // 验证进度百分比显示
        composeTestRule.onNodeWithText("45%").assertExists()
    }
    
    /**
     * 测试已读状态的书籍渲染
     */
    @Test
    fun testBookListItemFinishedStatus() {
        val book = Book(
            id = 1,
            title = "Finished Book",
            filePath = "/test/path",
            fileSize = 1000,
            fileFormat = BookFormat.PDF,
            coverPath = null,
            author = "Test Author",
            totalPages = 100,
            currentPage = 100,
            isFinished = true,
            addedAt = System.currentTimeMillis()
        )
        
        composeTestRule.setContent {
            PaysageTheme {
                BookListItem(
                    book = book,
                    showProgress = true,
                    onClick = {}
                )
            }
        }
        
        // 验证已读状态标签显示
        composeTestRule.onNodeWithText("已读").assertExists()
    }
    
    /**
     * 测试最新状态的书籍渲染
     */
    @Test
    fun testBookListItemLatestStatus() {
        val book = Book(
            id = 1,
            title = "Latest Book",
            filePath = "/test/path",
            fileSize = 1000,
            fileFormat = BookFormat.PDF,
            coverPath = null,
            author = "Test Author",
            totalPages = 100,
            currentPage = 0,
            isFinished = false,
            addedAt = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000) // 3天前
        )
        
        composeTestRule.setContent {
            PaysageTheme {
                BookListItem(
                    book = book,
                    showProgress = true,
                    onClick = {}
                )
            }
        }
        
        // 验证最新状态标签显示
        composeTestRule.onNodeWithText("最新").assertExists()
    }
    
    /**
     * 测试点击交互
     */
    @Test
    fun testBookListItemClickInteraction() {
        var clicked = false
        val book = Book(
            id = 1,
            title = "Clickable Book",
            filePath = "/test/path",
            fileSize = 1000,
            fileFormat = BookFormat.PDF,
            coverPath = null,
            author = "Test Author",
            totalPages = 100,
            currentPage = 50,
            isFinished = false,
            addedAt = System.currentTimeMillis()
        )
        
        composeTestRule.setContent {
            PaysageTheme {
                BookListItem(
                    book = book,
                    showProgress = true,
                    onClick = { clicked = true }
                )
            }
        }
        
        // 点击列表项
        composeTestRule.onNodeWithText("Clickable Book").performClick()
        
        // 验证点击回调被触发
        assert(clicked)
    }
    
    /**
     * 测试隐藏进度条
     */
    @Test
    fun testBookListItemWithProgressHidden() {
        val book = Book(
            id = 1,
            title = "Book with Hidden Progress",
            filePath = "/test/path",
            fileSize = 1000,
            fileFormat = BookFormat.PDF,
            coverPath = null,
            author = "Test Author",
            totalPages = 100,
            currentPage = 50,
            isFinished = false,
            addedAt = System.currentTimeMillis()
        )
        
        composeTestRule.setContent {
            PaysageTheme {
                BookListItem(
                    book = book,
                    showProgress = false,
                    onClick = {}
                )
            }
        }
        
        // 验证标题显示
        composeTestRule.onNodeWithText("Book with Hidden Progress").assertExists()
        
        // 验证进度文本不显示
        composeTestRule.onNodeWithText("50/100").assertDoesNotExist()
    }
}
