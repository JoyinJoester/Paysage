package takagi.ru.paysage.reader

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import takagi.ru.paysage.util.ImageFilter
import takagi.ru.paysage.util.ImageFilterUtil

/**
 * 阅读器性能集成测试
 */
@RunWith(AndroidJUnit4::class)
class ReaderPerformanceTest {
    
    private lateinit var context: Context
    private lateinit var memoryManager: BitmapMemoryManager
    private lateinit var cacheManager: PageCacheManager
    private lateinit var performanceMonitor: PerformanceMonitor
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        memoryManager = BitmapMemoryManager(context)
        cacheManager = PageCacheManager(
            maxRawCacheSize = 10,
            maxFilterCacheSize = 5,
            memoryManager = memoryManager
        )
        performanceMonitor = PerformanceMonitor()
    }
    
    @After
    fun tearDown() {
        cacheManager.clearAll()
    }
    
    @Test
    fun testFirstLoadPerformance() {
        val bookId = 1L
        val pageIndex = 0
        val bitmap = createTestBitmap()
        
        // 测试首次加载性能
        val startTime = System.currentTimeMillis()
        
        cacheManager.putRawPage(bookId, pageIndex, bitmap)
        val cached = cacheManager.getRawPage(bookId, pageIndex)
        
        val loadTime = System.currentTimeMillis() - startTime
        
        assertNotNull(cached)
        assertTrue("First load should be fast", loadTime < 200)
        
        performanceMonitor.recordPageLoad(loadTime)
    }
    
    @Test
    fun testCacheHitPerformance() {
        val bookId = 1L
        val pageIndex = 0
        val bitmap = createTestBitmap()
        
        // 先存入缓存
        cacheManager.putRawPage(bookId, pageIndex, bitmap)
        
        // 测试缓存命中性能
        val startTime = System.currentTimeMillis()
        val cached = cacheManager.getRawPage(bookId, pageIndex)
        val loadTime = System.currentTimeMillis() - startTime
        
        assertNotNull(cached)
        assertTrue("Cache hit should be very fast", loadTime < 50)
        
        performanceMonitor.recordPageLoad(loadTime)
    }
    
    @Test
    fun testFastPageTurning() {
        val bookId = 1L
        val bitmaps = mutableListOf<Bitmap>()
        
        // 创建多个页面
        for (i in 0 until 10) {
            bitmaps.add(createTestBitmap())
        }
        
        // 测试快速连续翻页
        val startTime = System.currentTimeMillis()
        
        for (i in 0 until 10) {
            cacheManager.putRawPage(bookId, i, bitmaps[i])
        }
        
        for (i in 0 until 10) {
            val cached = cacheManager.getRawPage(bookId, i)
            assertNotNull(cached)
        }
        
        val totalTime = System.currentTimeMillis() - startTime
        val avgTime = totalTime / 10
        
        assertTrue("Average page turn should be fast", avgTime < 100)
    }
    
    @Test
    fun testFilterPerformance() = runBlocking {
        val bitmap = createTestBitmap()
        val filter = ImageFilter(brightness = 10f, contrast = 1.2f)
        
        // 测试过滤器应用性能
        val startTime = System.currentTimeMillis()
        val filtered = ImageFilterUtil.applyFilter(bitmap, filter)
        val filterTime = System.currentTimeMillis() - startTime
        
        assertNotNull(filtered)
        assertTrue("Filter application should be reasonably fast", filterTime < 500)
    }
    
    @Test
    fun testMemoryPressure() {
        val bookId = 1L
        val bitmaps = mutableListOf<Bitmap>()
        
        // 创建大量页面测试内存压力
        for (i in 0 until 20) {
            val bitmap = createTestBitmap()
            bitmaps.add(bitmap)
            cacheManager.putRawPage(bookId, i, bitmap)
        }
        
        // 验证 LRU 淘汰工作正常
        val stats = cacheManager.getCacheStats()
        assertTrue("Cache should not exceed max size", stats.rawCacheSize <= 10)
        
        // 验证内存使用在合理范围内
        val memoryUsage = cacheManager.getMemoryUsage()
        val maxAllowed = memoryManager.getMaxAllowedMemory()
        assertTrue("Memory usage should be within limits", memoryUsage < maxAllowed)
    }
    
    @Test
    fun testCacheEfficiency() {
        val bookId = 1L
        val bitmap = createTestBitmap()
        
        // 存入缓存
        cacheManager.putRawPage(bookId, 0, bitmap)
        
        // 多次访问同一页面
        for (i in 0 until 10) {
            val cached = cacheManager.getRawPage(bookId, 0)
            assertNotNull(cached)
        }
        
        // 验证缓存命中率
        val hitRate = cacheManager.getCacheHitRate()
        assertTrue("Cache hit rate should be high", hitRate > 0.8f)
    }
    
    @Test
    fun testPerformanceMonitoring() {
        // 记录多次加载
        performanceMonitor.recordPageLoad(50)
        performanceMonitor.recordPageLoad(100)
        performanceMonitor.recordPageLoad(150)
        
        val report = performanceMonitor.getReport()
        
        assertEquals(100L, report.averageLoadTime)
        assertEquals(50L, report.minLoadTime)
        assertEquals(150L, report.maxLoadTime)
        assertEquals(3, report.totalPageLoads)
    }
    
    @Test
    fun testConcurrentAccess() {
        val bookId = 1L
        val bitmap = createTestBitmap()
        
        // 测试并发访问缓存
        val threads = mutableListOf<Thread>()
        
        for (i in 0 until 5) {
            val thread = Thread {
                cacheManager.putRawPage(bookId, i, bitmap)
                val cached = cacheManager.getRawPage(bookId, i)
                assertNotNull(cached)
            }
            threads.add(thread)
            thread.start()
        }
        
        // 等待所有线程完成
        threads.forEach { it.join() }
        
        // 验证缓存状态正常
        val stats = cacheManager.getCacheStats()
        assertTrue(stats.rawCacheSize > 0)
    }
    
    @Test
    fun testMemoryCleanup() {
        val bookId = 1L
        val bitmap = createTestBitmap()
        
        // 存入缓存
        cacheManager.putRawPage(bookId, 0, bitmap)
        
        val memoryBefore = cacheManager.getMemoryUsage()
        assertTrue(memoryBefore > 0)
        
        // 清理缓存
        cacheManager.clearAll()
        
        val memoryAfter = cacheManager.getMemoryUsage()
        assertEquals(0L, memoryAfter)
    }
    
    private fun createTestBitmap(): Bitmap {
        return Bitmap.createBitmap(1000, 1500, Bitmap.Config.ARGB_8888)
    }
}
