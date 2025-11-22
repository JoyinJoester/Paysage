package takagi.ru.paysage.repository

import android.content.Context
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import takagi.ru.paysage.data.PaysageDatabase
import takagi.ru.paysage.data.model.Book
import takagi.ru.paysage.data.model.CategoryType
import takagi.ru.paysage.data.model.DisplayMode

/**
 * BookRepository 分类系统扩展
 * 提供按分类类型和显示模式过滤书籍的功能
 */

// 书籍缓存，键格式: "categoryType_displayMode"
private val bookCache = LruCache<String, List<Book>>(10)

/**
 * 根据分类类型和显示模式获取书籍
 * 使用LruCache缓存提升性能
 */
suspend fun BookRepository.getBooksByCategory(
    categoryType: CategoryType,
    displayMode: DisplayMode
): List<Book> = withContext(Dispatchers.IO) {
    val cacheKey = "${categoryType.name}_${displayMode.name}"
    
    // 先检查缓存
    bookCache.get(cacheKey)?.let { return@withContext it }
    
    // 从数据库查询
    val database = PaysageDatabase.getDatabase(context)
    val bookDao = database.bookDao()
    
    val books = bookDao.getBooksByCategory(
        categoryType = categoryType,
        isOnline = displayMode == DisplayMode.ONLINE
    )
    
    // 更新缓存
    bookCache.put(cacheKey, books)
    
    books
}

/**
 * 根据分类类型和显示模式获取书籍（Flow）
 */
fun BookRepository.getBooksByCategoryFlow(
    categoryType: CategoryType,
    displayMode: DisplayMode
): Flow<List<Book>> {
    val database = PaysageDatabase.getDatabase(context)
    val bookDao = database.bookDao()
    
    return bookDao.getBooksByCategoryFlow(
        categoryType = categoryType,
        isOnline = displayMode == DisplayMode.ONLINE
    )
}

/**
 * 根据分类类型获取所有书籍（不区分在线/本地）
 */
fun BookRepository.getAllBooksByCategoryTypeFlow(
    categoryType: CategoryType
): Flow<List<Book>> {
    val database = PaysageDatabase.getDatabase(context)
    val bookDao = database.bookDao()
    
    return bookDao.getAllBooksByCategoryTypeFlow(categoryType)
}

/**
 * 获取指定分类类型的书籍数量
 */
suspend fun BookRepository.getBookCountByCategoryType(
    categoryType: CategoryType
): Int = withContext(Dispatchers.IO) {
    val database = PaysageDatabase.getDatabase(context)
    val bookDao = database.bookDao()
    
    bookDao.getBookCountByCategoryType(categoryType)
}

/**
 * 获取指定分类类型和在线状态的书籍数量
 */
suspend fun BookRepository.getBookCountByCategoryAndOnline(
    categoryType: CategoryType,
    displayMode: DisplayMode
): Int = withContext(Dispatchers.IO) {
    val database = PaysageDatabase.getDatabase(context)
    val bookDao = database.bookDao()
    
    bookDao.getBookCountByCategoryAndOnline(
        categoryType = categoryType,
        isOnline = displayMode == DisplayMode.ONLINE
    )
}

/**
 * 更新书籍的分类类型
 */
suspend fun BookRepository.updateBookCategoryType(
    bookId: Long,
    categoryType: CategoryType
) = withContext(Dispatchers.IO) {
    val database = PaysageDatabase.getDatabase(context)
    val bookDao = database.bookDao()
    
    bookDao.updateCategoryType(bookId, categoryType)
    
    // 清除缓存
    bookCache.evictAll()
}

/**
 * 批量更新书籍的分类类型
 */
suspend fun BookRepository.updateBookCategoryTypes(
    bookIds: List<Long>,
    categoryType: CategoryType
) = withContext(Dispatchers.IO) {
    val database = PaysageDatabase.getDatabase(context)
    val bookDao = database.bookDao()
    
    bookDao.updateCategoryTypes(bookIds, categoryType)
    
    // 清除缓存
    bookCache.evictAll()
}

/**
 * 获取指定分类类型的收藏书籍
 */
fun BookRepository.getFavoriteBooksByCategoryFlow(
    categoryType: CategoryType
): Flow<List<Book>> {
    val database = PaysageDatabase.getDatabase(context)
    val bookDao = database.bookDao()
    
    return bookDao.getFavoriteBooksByCategoryFlow(categoryType)
}

/**
 * 获取指定分类类型的最近阅读书籍
 */
fun BookRepository.getRecentBooksByCategoryFlow(
    categoryType: CategoryType,
    limit: Int = 20
): Flow<List<Book>> {
    val database = PaysageDatabase.getDatabase(context)
    val bookDao = database.bookDao()
    
    return bookDao.getRecentBooksByCategoryFlow(categoryType, limit)
}

/**
 * 搜索指定分类类型的书籍
 */
fun BookRepository.searchBooksByCategoryFlow(
    categoryType: CategoryType,
    query: String
): Flow<List<Book>> {
    val database = PaysageDatabase.getDatabase(context)
    val bookDao = database.bookDao()
    
    return bookDao.searchBooksByCategoryFlow(categoryType, query)
}

/**
 * 清除书籍缓存
 */
fun BookRepository.clearBookCache() {
    bookCache.evictAll()
}

/**
 * 获取缓存统计信息
 */
fun BookRepository.getCacheStats(): CacheStats {
    return CacheStats(
        size = bookCache.size(),
        maxSize = bookCache.maxSize(),
        hitCount = bookCache.hitCount(),
        missCount = bookCache.missCount(),
        putCount = bookCache.putCount(),
        evictionCount = bookCache.evictionCount()
    )
}

/**
 * 缓存统计信息
 */
data class CacheStats(
    val size: Int,
    val maxSize: Int,
    val hitCount: Int,
    val missCount: Int,
    val putCount: Int,
    val evictionCount: Int
) {
    val hitRate: Float
        get() = if (hitCount + missCount > 0) {
            hitCount.toFloat() / (hitCount + missCount)
        } else 0f
}

/**
 * 获取BookRepository的Context（内部使用）
 */
private val BookRepository.context: Context
    get() = javaClass.getDeclaredField("context").apply {
        isAccessible = true
    }.get(this) as Context
