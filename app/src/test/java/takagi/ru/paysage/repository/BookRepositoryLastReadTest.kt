package takagi.ru.paysage.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import takagi.ru.paysage.data.PaysageDatabase
import takagi.ru.paysage.data.model.Book
import takagi.ru.paysage.data.model.BookFormat

/**
 * BookRepository 上次阅读功能单元测试
 */
@RunWith(AndroidJUnit4::class)
class BookRepositoryLastReadTest {
    
    private lateinit var context: Context
    private lateinit var database: PaysageDatabase
    private lateinit var repository: BookRepository
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            context,
            PaysageDatabase::class.java
        ).allowMainThreadQueries().build()
        
        // 使用测试数据库创建 repository
        repository = BookRepository(context)
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    /**
     * 测试：无书籍时返回 null
     */
    @Test
    fun getLastReadBook_returnsNull_whenNoBooks() = runTest {
        // When
        val result = repository.getLastReadBook()
        
        // Then
        assertNull(result)
    }
    
    /**
     * 测试：无阅读记录时返回 null
     */
    @Test
    fun getLastReadBook_returnsNull_whenNoReadBooks() = runTest {
        // Given
        val book = Book(
            title = "Test Book",
            filePath = "/test/book.cbz",
            fileSize = 1000,
            fileFormat = BookFormat.CBZ,
            lastReadAt = null
        )
        repository.insertBook(book)
        
        // When
        val result = repository.getLastReadBook()
        
        // Then
        assertNull(result)
    }
    
    /**
     * 测试：多本书时返回最近阅读的
     */
    @Test
    fun getLastReadBook_returnsMostRecent_whenMultipleBooks() = runTest {
        // Given
        val book1 = Book(
            title = "Book 1",
            filePath = "/test/book1.cbz",
            fileSize = 1000,
            fileFormat = BookFormat.CBZ,
            lastReadAt = 1000L
        )
        val book2 = Book(
            title = "Book 2",
            filePath = "/test/book2.cbz",
            fileSize = 2000,
            fileFormat = BookFormat.CBZ,
            lastReadAt = 3000L // 最近阅读
        )
        val book3 = Book(
            title = "Book 3",
            filePath = "/test/book3.cbz",
            fileSize = 3000,
            fileFormat = BookFormat.CBZ,
            lastReadAt = 2000L
        )
        
        repository.insertBook(book1)
        repository.insertBook(book2)
        repository.insertBook(book3)
        
        // When
        val result = repository.getLastReadBook()
        
        // Then
        assertNotNull(result)
        assertEquals("Book 2", result?.title)
        assertEquals(3000L, result?.lastReadAt)
    }
    
    /**
     * 测试：忽略 lastReadAt 为 null 的书籍
     */
    @Test
    fun getLastReadBook_ignoresNullLastReadAt() = runTest {
        // Given
        val book1 = Book(
            title = "Book 1",
            filePath = "/test/book1.cbz",
            fileSize = 1000,
            fileFormat = BookFormat.CBZ,
            lastReadAt = null
        )
        val book2 = Book(
            title = "Book 2",
            filePath = "/test/book2.cbz",
            fileSize = 2000,
            fileFormat = BookFormat.CBZ,
            lastReadAt = 1000L
        )
        
        repository.insertBook(book1)
        repository.insertBook(book2)
        
        // When
        val result = repository.getLastReadBook()
        
        // Then
        assertNotNull(result)
        assertEquals("Book 2", result?.title)
    }
    
    /**
     * 测试：Flow 版本返回 null（无书籍）
     */
    @Test
    fun getLastReadBookFlow_returnsNull_whenNoBooks() = runTest {
        // When
        val result = repository.getLastReadBookFlow().first()
        
        // Then
        assertNull(result)
    }
    
    /**
     * 测试：Flow 版本返回最近阅读的书籍
     */
    @Test
    fun getLastReadBookFlow_returnsMostRecent() = runTest {
        // Given
        val book1 = Book(
            title = "Book 1",
            filePath = "/test/book1.cbz",
            fileSize = 1000,
            fileFormat = BookFormat.CBZ,
            lastReadAt = 1000L
        )
        val book2 = Book(
            title = "Book 2",
            filePath = "/test/book2.cbz",
            fileSize = 2000,
            fileFormat = BookFormat.CBZ,
            lastReadAt = 3000L
        )
        
        repository.insertBook(book1)
        repository.insertBook(book2)
        
        // When
        val result = repository.getLastReadBookFlow().first()
        
        // Then
        assertNotNull(result)
        assertEquals("Book 2", result?.title)
    }
    
    /**
     * 测试：更新阅读进度后，lastReadAt 被更新
     */
    @Test
    fun updateReadingProgress_updatesLastReadAt() = runTest {
        // Given
        val book = Book(
            title = "Test Book",
            filePath = "/test/book.cbz",
            fileSize = 1000,
            fileFormat = BookFormat.CBZ,
            totalPages = 100,
            currentPage = 0,
            lastReadAt = null
        )
        val bookId = repository.insertBook(book)
        
        // When
        val timestamp = System.currentTimeMillis()
        repository.updateReadingProgress(bookId, 50)
        
        // Then
        val result = repository.getLastReadBook()
        assertNotNull(result)
        assertEquals("Test Book", result?.title)
        assertEquals(50, result?.currentPage)
        assertNotNull(result?.lastReadAt)
        assertTrue(result?.lastReadAt!! >= timestamp)
    }
}
