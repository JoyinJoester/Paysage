package takagi.ru.paysage.ui.screens

import org.junit.Assert.*
import org.junit.Test
import takagi.ru.paysage.data.model.Book
import takagi.ru.paysage.data.model.BookFormat
import takagi.ru.paysage.data.model.BookReadingStatus
import takagi.ru.paysage.data.model.getReadingStatus

/**
 * 单元测试：BookListItem 组件相关逻辑
 */
class BookListItemTest {
    
    /**
     * 测试未读状态的书籍
     */
    @Test
    fun testUnreadBookStatus() {
        val book = Book(
            id = 1,
            title = "Test Book",
            filePath = "/test/path",
            fileSize = 1000,
            fileFormat = BookFormat.PDF,
            totalPages = 100,
            currentPage = 0,
            isFinished = false,
            addedAt = System.currentTimeMillis() - (8 * 24 * 60 * 60 * 1000) // 8天前
        )
        
        val status = book.getReadingStatus()
        assertEquals(BookReadingStatus.UNREAD, status)
    }
    
    /**
     * 测试阅读中状态的书籍
     */
    @Test
    fun testReadingBookStatus() {
        val book = Book(
            id = 1,
            title = "Test Book",
            filePath = "/test/path",
            fileSize = 1000,
            fileFormat = BookFormat.PDF,
            totalPages = 100,
            currentPage = 50,
            isFinished = false,
            addedAt = System.currentTimeMillis()
        )
        
        val status = book.getReadingStatus()
        assertEquals(BookReadingStatus.READING, status)
    }
    
    /**
     * 测试已读状态的书籍
     */
    @Test
    fun testFinishedBookStatus() {
        val book = Book(
            id = 1,
            title = "Test Book",
            filePath = "/test/path",
            fileSize = 1000,
            fileFormat = BookFormat.PDF,
            totalPages = 100,
            currentPage = 100,
            isFinished = true,
            addedAt = System.currentTimeMillis()
        )
        
        val status = book.getReadingStatus()
        assertEquals(BookReadingStatus.FINISHED, status)
    }
    
    /**
     * 测试最新状态的书籍（7天内添加）
     */
    @Test
    fun testLatestBookStatus() {
        val book = Book(
            id = 1,
            title = "Test Book",
            filePath = "/test/path",
            fileSize = 1000,
            fileFormat = BookFormat.PDF,
            totalPages = 100,
            currentPage = 0,
            isFinished = false,
            addedAt = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000) // 3天前
        )
        
        val status = book.getReadingStatus()
        assertEquals(BookReadingStatus.LATEST, status)
    }
    
    /**
     * 测试进度百分比计算 - 正常情况
     */
    @Test
    fun testProgressPercentageCalculation() {
        val book = Book(
            id = 1,
            title = "Test Book",
            filePath = "/test/path",
            fileSize = 1000,
            fileFormat = BookFormat.PDF,
            totalPages = 100,
            currentPage = 45,
            isFinished = false,
            addedAt = System.currentTimeMillis()
        )
        
        val percentage = (book.currentPage.toFloat() / book.totalPages * 100).toInt()
        assertEquals(45, percentage)
    }
    
    /**
     * 测试进度百分比计算 - 边界情况：currentPage = 0
     */
    @Test
    fun testProgressPercentageWithZeroCurrentPage() {
        val book = Book(
            id = 1,
            title = "Test Book",
            filePath = "/test/path",
            fileSize = 1000,
            fileFormat = BookFormat.PDF,
            totalPages = 100,
            currentPage = 0,
            isFinished = false,
            addedAt = System.currentTimeMillis()
        )
        
        val percentage = (book.currentPage.toFloat() / book.totalPages * 100).toInt()
        assertEquals(0, percentage)
    }
    
    /**
     * 测试进度百分比计算 - 边界情况：totalPages = 0
     */
    @Test
    fun testProgressPercentageWithZeroTotalPages() {
        val book = Book(
            id = 1,
            title = "Test Book",
            filePath = "/test/path",
            fileSize = 1000,
            fileFormat = BookFormat.PDF,
            totalPages = 0,
            currentPage = 0,
            isFinished = false,
            addedAt = System.currentTimeMillis()
        )
        
        // 使用 coerceAtLeast(1) 避免除零错误
        val percentage = (book.currentPage.toFloat() / book.totalPages.coerceAtLeast(1) * 100).toInt()
        assertEquals(0, percentage)
    }
    
    /**
     * 测试进度百分比计算 - 完成状态
     */
    @Test
    fun testProgressPercentageWhenFinished() {
        val book = Book(
            id = 1,
            title = "Test Book",
            filePath = "/test/path",
            fileSize = 1000,
            fileFormat = BookFormat.PDF,
            totalPages = 100,
            currentPage = 100,
            isFinished = true,
            addedAt = System.currentTimeMillis()
        )
        
        val percentage = (book.currentPage.toFloat() / book.totalPages * 100).toInt()
        assertEquals(100, percentage)
    }
    
    /**
     * 测试进度百分比计算 - 小数舍入
     */
    @Test
    fun testProgressPercentageRounding() {
        val book = Book(
            id = 1,
            title = "Test Book",
            filePath = "/test/path",
            fileSize = 1000,
            fileFormat = BookFormat.PDF,
            totalPages = 3,
            currentPage = 1,
            isFinished = false,
            addedAt = System.currentTimeMillis()
        )
        
        // 1/3 = 0.333... -> 33%
        val percentage = (book.currentPage.toFloat() / book.totalPages * 100).toInt()
        assertEquals(33, percentage)
    }
}
