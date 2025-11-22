package takagi.ru.paysage.reader

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BitmapMemoryManagerTest {
    
    private lateinit var context: Context
    private lateinit var memoryManager: BitmapMemoryManager
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        memoryManager = BitmapMemoryManager(context)
    }
    
    @Test
    fun testCalculateSampleSize_NoDownsampling() {
        // 图片小于目标尺寸，不需要采样
        val sampleSize = memoryManager.calculateSampleSize(
            originalWidth = 800,
            originalHeight = 600,
            targetWidth = 1000,
            targetHeight = 800
        )
        
        assertEquals(1, sampleSize)
    }
    
    @Test
    fun testCalculateSampleSize_2xDownsampling() {
        // 图片是目标尺寸的 2 倍，采样率应该是 2
        val sampleSize = memoryManager.calculateSampleSize(
            originalWidth = 2000,
            originalHeight = 1600,
            targetWidth = 1000,
            targetHeight = 800
        )
        
        assertEquals(2, sampleSize)
    }
    
    @Test
    fun testCalculateSampleSize_4xDownsampling() {
        // 图片是目标尺寸的 4 倍，采样率应该是 4
        val sampleSize = memoryManager.calculateSampleSize(
            originalWidth = 4000,
            originalHeight = 3200,
            targetWidth = 1000,
            targetHeight = 800
        )
        
        assertEquals(4, sampleSize)
    }
    
    @Test
    fun testCalculateSampleSize_LargeImage() {
        // 超大图片
        val sampleSize = memoryManager.calculateSampleSize(
            originalWidth = 8000,
            originalHeight = 6000,
            targetWidth = 1000,
            targetHeight = 800
        )
        
        assertTrue(sampleSize >= 4)
    }
    
    @Test
    fun testGetAvailableMemory() {
        val availableMemory = memoryManager.getAvailableMemory()
        
        // 可用内存应该大于 0
        assertTrue(availableMemory > 0)
    }
    
    @Test
    fun testGetMaxAllowedMemory() {
        val maxAllowed = memoryManager.getMaxAllowedMemory()
        
        // 最大允许内存应该大于 0
        assertTrue(maxAllowed > 0)
        
        // 应该小于总内存
        val report = memoryManager.getMemoryReport()
        assertTrue(maxAllowed < report.totalMemory)
    }
    
    @Test
    fun testRecycleBitmap() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        
        assertFalse(bitmap.isRecycled)
        
        memoryManager.recycleBitmap(bitmap)
        
        assertTrue(bitmap.isRecycled)
    }
    
    @Test
    fun testRecycleBitmap_Null() {
        // 不应该抛出异常
        memoryManager.recycleBitmap(null)
    }
    
    @Test
    fun testRecycleBitmap_AlreadyRecycled() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.recycle()
        
        // 不应该抛出异常
        memoryManager.recycleBitmap(bitmap)
    }
    
    @Test
    fun testMemoryWarningListener() {
        var listenerCalled = false
        
        memoryManager.registerMemoryWarningListener {
            listenerCalled = true
        }
        
        // 触发内存检查（可能会触发监听器）
        memoryManager.shouldClearCache()
        
        // 注意：在测试环境中可能不会触发，这只是验证注册不会崩溃
        // 实际触发需要真实的内存压力
    }
    
    @Test
    fun testGetMemoryReport() {
        val report = memoryManager.getMemoryReport()
        
        assertNotNull(report)
        assertTrue(report.totalMemory > 0)
        assertTrue(report.availableMemory > 0)
        assertTrue(report.usedMemory >= 0)
        assertTrue(report.maxAllowedMemory > 0)
        assertTrue(report.usageRatio >= 0f && report.usageRatio <= 1f)
    }
    
    @Test
    fun testMemoryReport_ToString() {
        val report = memoryManager.getMemoryReport()
        val string = report.toString()
        
        assertNotNull(string)
        assertTrue(string.contains("Memory Report"))
        assertTrue(string.contains("Total"))
        assertTrue(string.contains("Available"))
        assertTrue(string.contains("Used"))
    }
}
