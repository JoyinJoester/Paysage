package takagi.ru.paysage.reader

import org.junit.Assert.*
import org.junit.Test
import takagi.ru.paysage.data.model.ReadingDirection

/**
 * TouchZone 单元测试
 */
class TouchZoneTest {
    
    @Test
    fun `isNextPage should return correct zones for LEFT_TO_RIGHT`() {
        val direction = ReadingDirection.LEFT_TO_RIGHT
        
        assertTrue(TouchZone.TOP_RIGHT.isNextPage(direction))
        assertTrue(TouchZone.MIDDLE_RIGHT.isNextPage(direction))
        assertTrue(TouchZone.BOTTOM_CENTER.isNextPage(direction))
        assertTrue(TouchZone.BOTTOM_RIGHT.isNextPage(direction))
        
        assertFalse(TouchZone.TOP_LEFT.isNextPage(direction))
        assertFalse(TouchZone.MIDDLE_LEFT.isNextPage(direction))
        assertFalse(TouchZone.BOTTOM_LEFT.isNextPage(direction))
        assertFalse(TouchZone.TOP_CENTER.isNextPage(direction))
        assertFalse(TouchZone.CENTER.isNextPage(direction))
    }
    
    @Test
    fun `isNextPage should return correct zones for RIGHT_TO_LEFT`() {
        val direction = ReadingDirection.RIGHT_TO_LEFT
        
        assertTrue(TouchZone.TOP_LEFT.isNextPage(direction))
        assertTrue(TouchZone.MIDDLE_LEFT.isNextPage(direction))
        assertTrue(TouchZone.BOTTOM_CENTER.isNextPage(direction))
        assertTrue(TouchZone.BOTTOM_LEFT.isNextPage(direction))
        
        assertFalse(TouchZone.TOP_RIGHT.isNextPage(direction))
        assertFalse(TouchZone.MIDDLE_RIGHT.isNextPage(direction))
        assertFalse(TouchZone.BOTTOM_RIGHT.isNextPage(direction))
        assertFalse(TouchZone.TOP_CENTER.isNextPage(direction))
        assertFalse(TouchZone.CENTER.isNextPage(direction))
    }
    
    @Test
    fun `isNextPage should return correct zones for VERTICAL`() {
        val direction = ReadingDirection.VERTICAL
        
        assertTrue(TouchZone.BOTTOM_LEFT.isNextPage(direction))
        assertTrue(TouchZone.BOTTOM_CENTER.isNextPage(direction))
        assertTrue(TouchZone.BOTTOM_RIGHT.isNextPage(direction))
        
        assertFalse(TouchZone.TOP_LEFT.isNextPage(direction))
        assertFalse(TouchZone.TOP_CENTER.isNextPage(direction))
        assertFalse(TouchZone.TOP_RIGHT.isNextPage(direction))
        assertFalse(TouchZone.MIDDLE_LEFT.isNextPage(direction))
        assertFalse(TouchZone.CENTER.isNextPage(direction))
        assertFalse(TouchZone.MIDDLE_RIGHT.isNextPage(direction))
    }
    
    @Test
    fun `isPreviousPage should return correct zones for LEFT_TO_RIGHT`() {
        val direction = ReadingDirection.LEFT_TO_RIGHT
        
        assertTrue(TouchZone.TOP_LEFT.isPreviousPage(direction))
        assertTrue(TouchZone.MIDDLE_LEFT.isPreviousPage(direction))
        assertTrue(TouchZone.BOTTOM_LEFT.isPreviousPage(direction))
        assertTrue(TouchZone.TOP_CENTER.isPreviousPage(direction))
        
        assertFalse(TouchZone.TOP_RIGHT.isPreviousPage(direction))
        assertFalse(TouchZone.MIDDLE_RIGHT.isPreviousPage(direction))
        assertFalse(TouchZone.BOTTOM_CENTER.isPreviousPage(direction))
        assertFalse(TouchZone.BOTTOM_RIGHT.isPreviousPage(direction))
        assertFalse(TouchZone.CENTER.isPreviousPage(direction))
    }
    
    @Test
    fun `isPreviousPage should return correct zones for RIGHT_TO_LEFT`() {
        val direction = ReadingDirection.RIGHT_TO_LEFT
        
        assertTrue(TouchZone.TOP_RIGHT.isPreviousPage(direction))
        assertTrue(TouchZone.MIDDLE_RIGHT.isPreviousPage(direction))
        assertTrue(TouchZone.BOTTOM_RIGHT.isPreviousPage(direction))
        assertTrue(TouchZone.TOP_CENTER.isPreviousPage(direction))
        
        assertFalse(TouchZone.TOP_LEFT.isPreviousPage(direction))
        assertFalse(TouchZone.MIDDLE_LEFT.isPreviousPage(direction))
        assertFalse(TouchZone.BOTTOM_CENTER.isPreviousPage(direction))
        assertFalse(TouchZone.BOTTOM_LEFT.isPreviousPage(direction))
        assertFalse(TouchZone.CENTER.isPreviousPage(direction))
    }
    
    @Test
    fun `isPreviousPage should return correct zones for VERTICAL`() {
        val direction = ReadingDirection.VERTICAL
        
        assertTrue(TouchZone.TOP_LEFT.isPreviousPage(direction))
        assertTrue(TouchZone.TOP_CENTER.isPreviousPage(direction))
        assertTrue(TouchZone.TOP_RIGHT.isPreviousPage(direction))
        
        assertFalse(TouchZone.MIDDLE_LEFT.isPreviousPage(direction))
        assertFalse(TouchZone.CENTER.isPreviousPage(direction))
        assertFalse(TouchZone.MIDDLE_RIGHT.isPreviousPage(direction))
        assertFalse(TouchZone.BOTTOM_LEFT.isPreviousPage(direction))
        assertFalse(TouchZone.BOTTOM_CENTER.isPreviousPage(direction))
        assertFalse(TouchZone.BOTTOM_RIGHT.isPreviousPage(direction))
    }
    
    @Test
    fun `isCenter should return true only for CENTER zone`() {
        assertTrue(TouchZone.CENTER.isCenter())
        
        assertFalse(TouchZone.TOP_LEFT.isCenter())
        assertFalse(TouchZone.TOP_CENTER.isCenter())
        assertFalse(TouchZone.TOP_RIGHT.isCenter())
        assertFalse(TouchZone.MIDDLE_LEFT.isCenter())
        assertFalse(TouchZone.MIDDLE_RIGHT.isCenter())
        assertFalse(TouchZone.BOTTOM_LEFT.isCenter())
        assertFalse(TouchZone.BOTTOM_CENTER.isCenter())
        assertFalse(TouchZone.BOTTOM_RIGHT.isCenter())
    }
    
    @Test
    fun `no zone should be both next and previous for same direction`() {
        ReadingDirection.values().forEach { direction ->
            TouchZone.values().forEach { zone ->
                // A zone cannot be both next and previous (except CENTER which is neither)
                if (zone.isNextPage(direction)) {
                    assertFalse("Zone $zone should not be both next and previous for $direction",
                        zone.isPreviousPage(direction))
                }
            }
        }
    }
    
    @Test
    fun `CENTER should never be next or previous page`() {
        ReadingDirection.values().forEach { direction ->
            assertFalse(TouchZone.CENTER.isNextPage(direction))
            assertFalse(TouchZone.CENTER.isPreviousPage(direction))
        }
    }
}
