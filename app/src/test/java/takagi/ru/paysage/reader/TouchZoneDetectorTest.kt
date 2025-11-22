package takagi.ru.paysage.reader

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * TouchZoneDetector 单元测试
 */
class TouchZoneDetectorTest {
    
    private lateinit var detector: TouchZoneDetector
    private val screenWidth = 1080f
    private val screenHeight = 1920f
    
    @Before
    fun setup() {
        detector = TouchZoneDetector(TouchZoneConfig())
    }
    
    @Test
    fun `detectZone should return TOP_LEFT for top-left corner`() {
        val offset = Offset(100f, 100f)
        val zone = detector.detectZone(offset, screenWidth, screenHeight)
        assertEquals(TouchZone.TOP_LEFT, zone)
    }
    
    @Test
    fun `detectZone should return TOP_CENTER for top-center`() {
        val offset = Offset(540f, 100f)
        val zone = detector.detectZone(offset, screenWidth, screenHeight)
        assertEquals(TouchZone.TOP_CENTER, zone)
    }
    
    @Test
    fun `detectZone should return TOP_RIGHT for top-right corner`() {
        val offset = Offset(900f, 100f)
        val zone = detector.detectZone(offset, screenWidth, screenHeight)
        assertEquals(TouchZone.TOP_RIGHT, zone)
    }
    
    @Test
    fun `detectZone should return MIDDLE_LEFT for middle-left`() {
        val offset = Offset(100f, 960f)
        val zone = detector.detectZone(offset, screenWidth, screenHeight)
        assertEquals(TouchZone.MIDDLE_LEFT, zone)
    }
    
    @Test
    fun `detectZone should return CENTER for center`() {
        val offset = Offset(540f, 960f)
        val zone = detector.detectZone(offset, screenWidth, screenHeight)
        assertEquals(TouchZone.CENTER, zone)
    }
    
    @Test
    fun `detectZone should return MIDDLE_RIGHT for middle-right`() {
        val offset = Offset(900f, 960f)
        val zone = detector.detectZone(offset, screenWidth, screenHeight)
        assertEquals(TouchZone.MIDDLE_RIGHT, zone)
    }
    
    @Test
    fun `detectZone should return BOTTOM_LEFT for bottom-left corner`() {
        val offset = Offset(100f, 1800f)
        val zone = detector.detectZone(offset, screenWidth, screenHeight)
        assertEquals(TouchZone.BOTTOM_LEFT, zone)
    }
    
    @Test
    fun `detectZone should return BOTTOM_CENTER for bottom-center`() {
        val offset = Offset(540f, 1800f)
        val zone = detector.detectZone(offset, screenWidth, screenHeight)
        assertEquals(TouchZone.BOTTOM_CENTER, zone)
    }
    
    @Test
    fun `detectZone should return BOTTOM_RIGHT for bottom-right corner`() {
        val offset = Offset(900f, 1800f)
        val zone = detector.detectZone(offset, screenWidth, screenHeight)
        assertEquals(TouchZone.BOTTOM_RIGHT, zone)
    }
    
    @Test
    fun `detectZone should handle boundary conditions correctly`() {
        // Test exact boundary at 1/3 width
        val boundaryX = screenWidth / 3f
        val offset1 = Offset(boundaryX - 1f, 100f)
        val offset2 = Offset(boundaryX + 1f, 100f)
        
        assertEquals(TouchZone.TOP_LEFT, detector.detectZone(offset1, screenWidth, screenHeight))
        assertEquals(TouchZone.TOP_CENTER, detector.detectZone(offset2, screenWidth, screenHeight))
    }
    
    @Test
    fun `detectZone should work with different screen sizes`() {
        val smallWidth = 720f
        val smallHeight = 1280f
        
        val offset = Offset(360f, 640f)
        val zone = detector.detectZone(offset, smallWidth, smallHeight)
        assertEquals(TouchZone.CENTER, zone)
    }
    
    @Test
    fun `getZoneBounds should return correct bounds for CENTER`() {
        val bounds = detector.getZoneBounds(TouchZone.CENTER, screenWidth, screenHeight)
        
        val expectedLeft = screenWidth / 3f
        val expectedTop = screenHeight / 3f
        val expectedRight = screenWidth * 2f / 3f
        val expectedBottom = screenHeight * 2f / 3f
        
        assertEquals(expectedLeft, bounds.left, 0.01f)
        assertEquals(expectedTop, bounds.top, 0.01f)
        assertEquals(expectedRight, bounds.right, 0.01f)
        assertEquals(expectedBottom, bounds.bottom, 0.01f)
    }
    
    @Test
    fun `getZoneBounds should return correct bounds for all zones`() {
        TouchZone.values().forEach { zone ->
            val bounds = detector.getZoneBounds(zone, screenWidth, screenHeight)
            
            // Verify bounds are within screen
            assertTrue(bounds.left >= 0f)
            assertTrue(bounds.top >= 0f)
            assertTrue(bounds.right <= screenWidth)
            assertTrue(bounds.bottom <= screenHeight)
            
            // Verify bounds have positive dimensions
            assertTrue(bounds.width > 0f)
            assertTrue(bounds.height > 0f)
        }
    }
}
