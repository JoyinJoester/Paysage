package takagi.ru.paysage.reader

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import takagi.ru.paysage.util.ImageFilter

@RunWith(RobolectricTestRunner::class)
class PageCacheManagerTest {
    
    private lateinit var context: Context
    private lateinit var memoryManager: BitmapMemoryManager
    private lateinit var cacheManager: PageCacheManager
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        memoryManager = BitmapMemoryManager(context)
        cacheManager = PageCacheManager(
            maxRawCacheSize = 3,
            maxFilterCacheSize = 2,
            memoryManager = memoryManager
        )
    }
    
    @After
    fun tearDown() {
        cacheManager.clearAll()
    }
    
    @Test
    fun testRawCachePutAndGet() {
        val bookId = 1L
        val pageIndex = 0
        val bitmap = createTestBitmap()
        
        // 存入缓存
        cacheManager.putRawPage(bookId, pageIndex, bitmap)
        
        // 从缓存获取
        val cached = cacheManager.getRawPage(bookId, pageIndex)
        
        assertNotNull(cached)
        assertEquals(bitmap, cached)
    }
    
    @Test
    fun testRawCacheMiss() {
        val bookId = 1L
        val pageIndex = 0
        
        // 缓存未命中
        val cached = cacheManager.getRawPage(bookId, pageIndex)
        
        assertNull(cached)
    }
    
    @Test
    fun testLruEviction() {
        val bookId = 1L
        
        // 填满缓存（容量为 3）
        val bitmap1 = createTestBitmap()
        val bitmap2 = createTestBitmap()
        val bitmap3 = createTestBitmap()
        val bitmap4 = createTestBitmap()
        
        cacheManager.putRawPage(bookId, 0, bitmap1)
        cacheManager.putRawPage(bookId, 1, bitmap2)
        cacheManager.putRawPage(bookId, 2, bitmap3)
        
        // 添加第 4 个，应该淘汰第 1 个
        cacheManager.putRawPage(bookId, 3, bitmap4)
        
        // 验证第 1 个被淘汰
        assertNull(cacheManager.getRawPage(bookId, 0))
        
        // 验证其他还在
        assertNotNull(cacheManager.getRawPage(bookId, 1))
        assertNotNull(cacheManager.getRawPage(bookId, 2))
        assertNotNull(cacheManager.getRawPage(bookId, 3))
    }
    
    @Test
    fun testFilterCachePutAndGet() {
        val bookId = 1L
        val pageIndex = 0
        val bitmap = createTestBitmap()
        val filter = ImageFilter(brightness = 10f)
        
        // 存入过滤缓存
        cacheManager.putFilteredPage(bookId, pageIndex, filter, bitmap)
        
        // 从过滤缓存获取
        val cached = cacheManager.getFilteredPage(bookId, pageIndex, filter)
        
        assertNotNull(cached)
        assertEquals(bitmap, cached)
    }
    
    @Test
    fun testFilterCacheDifferentFilters() {
        val bookId = 1L
        val pageIndex = 0
        val bitmap1 = createTestBitmap()
        val bitmap2 = createTestBitmap()
        val filter1 = ImageFilter(brightness = 10f)
        val filter2 = ImageFilter(brightness = 20f)
        
        // 存入两个不同过滤器的结果
        cacheManager.putFilteredPage(bookId, pageIndex, filter1, bitmap1)
        cacheManager.putFilteredPage(bookId, pageIndex, filter2, bitmap2)
        
        // 验证可以分别获取
        val cached1 = cacheManager.getFilteredPage(bookId, pageIndex, filter1)
        val cached2 = cacheManager.getFilteredPage(bookId, pageIndex, filter2)
        
        assertEquals(bitmap1, cached1)
        assertEquals(bitmap2, cached2)
    }
    
    @Test
    fun testClearFilterCache() {
        val bookId = 1L
        val pageIndex = 0
        val bitmap = createTestBitmap()
        val filter = ImageFilter(brightness = 10f)
        
        // 存入过滤缓存
        cacheManager.putFilteredPage(bookId, pageIndex, filter, bitmap)
        
        // 清空过滤缓存
        cacheManager.clearFilterCache()
        
        // 验证过滤缓存被清空
        assertNull(cacheManager.getFilteredPage(bookId, pageIndex, filter))
    }
    
    @Test
    fun testClearAll() {
        val bookId = 1L
        val bitmap = createTestBitmap()
        val filter = ImageFilter(brightness = 10f)
        
        // 存入原始和过滤缓存
        cacheManager.putRawPage(bookId, 0, bitmap)
        cacheManager.putFilteredPage(bookId, 0, filter, bitmap)
        
        // 清空所有缓存
        cacheManager.clearAll()
        
        // 验证都被清空
        assertNull(cacheManager.getRawPage(bookId, 0))
        assertNull(cacheManager.getFilteredPage(bookId, 0, filter))
    }
    
    @Test
    fun testRemoveBook() {
        val bookId1 = 1L
        val bookId2 = 2L
        val bitmap = createTestBitmap()
        
        // 存入两本书的缓存
        cacheManager.putRawPage(bookId1, 0, bitmap)
        cacheManager.putRawPage(bookId2, 0, bitmap)
        
        // 移除第一本书
        cacheManager.removeBook(bookId1)
        
        // 验证第一本书被移除，第二本还在
        assertNull(cacheManager.getRawPage(bookId1, 0))
        assertNotNull(cacheManager.getRawPage(bookId2, 0))
    }
    
    @Test
    fun testCacheHitRate() {
        val bookId = 1L
        val bitmap = createTestBitmap()
        
        // 初始命中率为 0
        assertEquals(0f, cacheManager.getCacheHitRate(), 0.01f)
        
        // 一次未命中
        cacheManager.getRawPage(bookId, 0)
        assertEquals(0f, cacheManager.getCacheHitRate(), 0.01f)
        
        // 存入缓存
        cacheManager.putRawPage(bookId, 0, bitmap)
        
        // 一次命中
        cacheManager.getRawPage(bookId, 0)
        assertEquals(0.5f, cacheManager.getCacheHitRate(), 0.01f)  // 1 hit / 2 total
        
        // 再一次命中
        cacheManager.getRawPage(bookId, 0)
        assertEquals(0.67f, cacheManager.getCacheHitRate(), 0.01f)  // 2 hits / 3 total
    }
    
    @Test
    fun testMemoryUsage() {
        val bookId = 1L
        val bitmap = createTestBitmap()
        
        // 初始内存使用为 0
        assertEquals(0L, cacheManager.getMemoryUsage())
        
        // 存入缓存
        cacheManager.putRawPage(bookId, 0, bitmap)
        
        // 内存使用应该大于 0
        assertTrue(cacheManager.getMemoryUsage() > 0)
    }
    
    @Test
    fun testCacheStats() {
        val bookId = 1L
        val bitmap = createTestBitmap()
        
        // 存入一些缓存
        cacheManager.putRawPage(bookId, 0, bitmap)
        cacheManager.getRawPage(bookId, 0)  // hit
        cacheManager.getRawPage(bookId, 1)  // miss
        
        val stats = cacheManager.getCacheStats()
        
        assertEquals(1, stats.rawCacheSize)
        assertEquals(3, stats.maxRawCacheSize)
        assertEquals(1, stats.cacheHits)
        assertEquals(1, stats.cacheMisses)
        assertEquals(0.5f, stats.hitRate, 0.01f)
    }
    
    private fun createTestBitmap(): Bitmap {
        return Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    }
}
