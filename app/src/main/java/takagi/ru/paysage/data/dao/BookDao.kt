package takagi.ru.paysage.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import takagi.ru.paysage.data.model.Book
import takagi.ru.paysage.data.model.BookFormat
import takagi.ru.paysage.data.model.CategoryType

/**
 * 书籍 DAO
 */
@Dao
interface BookDao {
    
    @Query("SELECT * FROM books ORDER BY lastReadAt DESC")
    fun getAllBooksFlow(): Flow<List<Book>>
    
    @Query("SELECT * FROM books ORDER BY lastReadAt DESC")
    suspend fun getAllBooks(): List<Book>
    
    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookById(bookId: Long): Book?
    
    @Query("SELECT * FROM books WHERE id = :bookId")
    fun getBookByIdFlow(bookId: Long): Flow<Book?>
    
    @Query("SELECT * FROM books WHERE isFavorite = 1 ORDER BY lastReadAt DESC")
    fun getFavoriteBooksFlow(): Flow<List<Book>>
    
    @Query("SELECT * FROM books WHERE category = :category ORDER BY lastReadAt DESC")
    fun getBooksByCategoryFlow(category: String): Flow<List<Book>>
    
    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%'")
    fun searchBooksFlow(query: String): Flow<List<Book>>
    
    @Query("SELECT * FROM books WHERE fileFormat = :format ORDER BY lastReadAt DESC")
    fun getBooksByFormatFlow(format: BookFormat): Flow<List<Book>>
    
    @Query("SELECT DISTINCT category FROM books WHERE category IS NOT NULL")
    fun getAllCategoriesFlow(): Flow<List<String>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<Book>)
    
    @Update
    suspend fun updateBook(book: Book)
    
    @Delete
    suspend fun deleteBook(book: Book)
    
    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBookById(bookId: Long)
    
    @Query("UPDATE books SET isFavorite = :isFavorite WHERE id = :bookId")
    suspend fun updateFavorite(bookId: Long, isFavorite: Boolean)
    
    @Query("UPDATE books SET currentPage = :page, lastReadAt = :timestamp WHERE id = :bookId")
    suspend fun updateReadingProgress(bookId: Long, page: Int, timestamp: Long)

    @Query("UPDATE books SET currentPage = :page, isFinished = :isFinished, lastReadAt = :timestamp WHERE id = :bookId")
    suspend fun updateReadingProgress(bookId: Long, page: Int, isFinished: Boolean, timestamp: Long)
    
    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBookCount(): Int
    
    @Query("SELECT COUNT(*) FROM books WHERE category = :category")
    suspend fun getBookCountByCategory(category: String): Int
    
    @Query("SELECT * FROM books WHERE lastReadAt IS NOT NULL ORDER BY lastReadAt DESC LIMIT 1")
    fun getLastReadBookFlow(): Flow<Book?>
    
    @Query("SELECT * FROM books WHERE lastReadAt IS NOT NULL ORDER BY lastReadAt DESC LIMIT 1")
    suspend fun getLastReadBook(): Book?
    
    /**
     * 获取最近阅读的书籍（按最后阅读时间降序，限制数量）
     */
    @Query("SELECT * FROM books WHERE lastReadAt IS NOT NULL ORDER BY lastReadAt DESC LIMIT :limit")
    fun getRecentBooksFlow(limit: Int = 20): Flow<List<Book>>
    
    /**
     * 获取所有分类及其书籍数量
     */
    @Query("""
        SELECT category, COUNT(*) as count 
        FROM books 
        WHERE category IS NOT NULL 
        GROUP BY category 
        ORDER BY category ASC
    """)
    fun getCategoriesWithCount(): Flow<List<takagi.ru.paysage.data.model.CategoryCount>>
    
    // ========== 新增：分类系统相关查询 ==========
    
    /**
     * 根据分类类型和在线状态获取书籍
     */
    @Query("""
        SELECT * FROM books 
        WHERE categoryType = :categoryType AND isOnline = :isOnline
        ORDER BY lastReadAt DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getBooksByCategory(
        categoryType: CategoryType,
        isOnline: Boolean,
        limit: Int = 50,
        offset: Int = 0
    ): List<Book>
    
    /**
     * 根据分类类型和在线状态获取书籍（Flow）
     */
    @Query("""
        SELECT * FROM books 
        WHERE categoryType = :categoryType AND isOnline = :isOnline
        ORDER BY lastReadAt DESC
    """)
    fun getBooksByCategoryFlow(
        categoryType: CategoryType,
        isOnline: Boolean
    ): Flow<List<Book>>
    
    /**
     * 根据分类类型获取所有书籍（不区分在线/本地）
     */
    @Query("""
        SELECT * FROM books 
        WHERE categoryType = :categoryType
        ORDER BY lastReadAt DESC
    """)
    fun getAllBooksByCategoryTypeFlow(categoryType: CategoryType): Flow<List<Book>>
    
    /**
     * 获取指定分类类型的书籍数量
     */
    @Query("SELECT COUNT(*) FROM books WHERE categoryType = :categoryType")
    suspend fun getBookCountByCategoryType(categoryType: CategoryType): Int
    
    /**
     * 获取指定分类类型和在线状态的书籍数量
     */
    @Query("""
        SELECT COUNT(*) FROM books 
        WHERE categoryType = :categoryType AND isOnline = :isOnline
    """)
    suspend fun getBookCountByCategoryAndOnline(
        categoryType: CategoryType,
        isOnline: Boolean
    ): Int
    
    /**
     * 更新书籍的分类类型
     */
    @Query("UPDATE books SET categoryType = :categoryType WHERE id = :bookId")
    suspend fun updateCategoryType(bookId: Long, categoryType: CategoryType)
    
    /**
     * 批量更新书籍的分类类型
     */
    @Query("UPDATE books SET categoryType = :categoryType WHERE id IN (:bookIds)")
    suspend fun updateCategoryTypes(bookIds: List<Long>, categoryType: CategoryType)
    
    /**
     * 获取指定分类类型的收藏书籍
     */
    @Query("""
        SELECT * FROM books 
        WHERE categoryType = :categoryType AND isFavorite = 1
        ORDER BY lastReadAt DESC
    """)
    fun getFavoriteBooksByCategoryFlow(categoryType: CategoryType): Flow<List<Book>>
    
    /**
     * 获取指定分类类型的最近阅读书籍
     */
    @Query("""
        SELECT * FROM books 
        WHERE categoryType = :categoryType AND lastReadAt IS NOT NULL
        ORDER BY lastReadAt DESC 
        LIMIT :limit
    """)
    fun getRecentBooksByCategoryFlow(
        categoryType: CategoryType,
        limit: Int = 20
    ): Flow<List<Book>>
    
    /**
     * 搜索指定分类类型的书籍
     */
    @Query("""
        SELECT * FROM books 
        WHERE categoryType = :categoryType 
        AND (title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%')
        ORDER BY lastReadAt DESC
    """)
    fun searchBooksByCategoryFlow(
        categoryType: CategoryType,
        query: String
    ): Flow<List<Book>>
}
