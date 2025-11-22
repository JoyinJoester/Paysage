package takagi.ru.paysage.navigation

import android.os.Bundle
import org.junit.Assert.*
import org.junit.Test

/**
 * NavigationState 单元测试
 */
class NavigationStateTest {
    
    @Test
    fun `初始状态应该选中书库`() {
        val state = NavigationState()
        assertEquals(PrimaryNavItem.Library, state.selectedPrimaryItem)
        assertFalse(state.isSecondaryDrawerOpen)
        assertNull(state.selectedSecondaryItem)
    }
    
    @Test
    fun `切换第一层项应该更新状态`() {
        val state = NavigationState()
        val newState = state.copy(selectedPrimaryItem = PrimaryNavItem.Settings)
        assertEquals(PrimaryNavItem.Settings, newState.selectedPrimaryItem)
    }
    
    @Test
    fun `打开第二层抽屉应该更新状态`() {
        val state = NavigationState()
        val newState = state.copy(isSecondaryDrawerOpen = true)
        assertTrue(newState.isSecondaryDrawerOpen)
    }
    
    @Test
    fun `选择第二层项应该更新状态`() {
        val state = NavigationState()
        val newState = state.copy(selectedSecondaryItem = "all_books")
        assertEquals("all_books", newState.selectedSecondaryItem)
    }
    
    @Test
    fun `保存状态应该返回有效的 Bundle`() {
        val state = NavigationState(
            selectedPrimaryItem = PrimaryNavItem.Settings,
            isSecondaryDrawerOpen = true,
            selectedSecondaryItem = "theme"
        )
        
        val bundle = saveNavigationState(state)
        
        assertNotNull(bundle)
        assertEquals("Settings", bundle.getString("selectedPrimaryItem"))
        assertTrue(bundle.getBoolean("isSecondaryDrawerOpen"))
        assertEquals("theme", bundle.getString("selectedSecondaryItem"))
    }
    
    @Test
    fun `恢复状态应该返回正确的 NavigationState`() {
        val bundle = Bundle().apply {
            putString("selectedPrimaryItem", "Settings")
            putBoolean("isSecondaryDrawerOpen", true)
            putString("selectedSecondaryItem", "theme")
        }
        
        val state = restoreNavigationState(bundle)
        
        assertEquals(PrimaryNavItem.Settings, state.selectedPrimaryItem)
        assertTrue(state.isSecondaryDrawerOpen)
        assertEquals("theme", state.selectedSecondaryItem)
    }
    
    @Test
    fun `恢复空 Bundle 应该返回默认状态`() {
        val state = restoreNavigationState(null)
        
        assertEquals(PrimaryNavItem.Library, state.selectedPrimaryItem)
        assertFalse(state.isSecondaryDrawerOpen)
        assertNull(state.selectedSecondaryItem)
    }
    
    @Test
    fun `恢复无效枚举值应该返回默认状态`() {
        val bundle = Bundle().apply {
            putString("selectedPrimaryItem", "InvalidItem")
        }
        
        val state = restoreNavigationState(bundle)
        
        // 应该返回默认状态而不是崩溃
        assertEquals(PrimaryNavItem.Library, state.selectedPrimaryItem)
    }
    
    @Test
    fun `PrimaryNavItem 应该包含所有必需的属性`() {
        PrimaryNavItem.values().forEach { item ->
            assertNotNull(item.icon)
            assertNotNull(item.label)
            assertNotNull(item.contentDescription)
            assertTrue(item.label.isNotEmpty())
            assertTrue(item.contentDescription.isNotEmpty())
        }
    }
    
    @Test
    fun `LibraryNavItems 应该包含所有书库菜单项`() {
        val items = LibraryNavItems.items
        
        assertTrue(items.isNotEmpty())
        assertTrue(items.any { it.id == "all_books" })
        assertTrue(items.any { it.id == "favorites" })
        assertTrue(items.any { it.id == "recent" })
        assertTrue(items.any { it.id == "categories" })
        
        items.forEach { item ->
            assertNotNull(item.icon)
            assertTrue(item.label.isNotEmpty())
            assertNotNull(item.route)
        }
    }
    
    @Test
    fun `SettingsNavItems 应该包含所有设置菜单项`() {
        val items = SettingsNavItems.items
        
        assertTrue(items.isNotEmpty())
        assertTrue(items.any { it.id == "theme" })
        assertTrue(items.any { it.id == "reading" })
        assertTrue(items.any { it.id == "cache" })
        assertTrue(items.any { it.id == "about_app" })
        
        items.forEach { item ->
            assertNotNull(item.icon)
            assertTrue(item.label.isNotEmpty())
            assertNotNull(item.route)
        }
    }
}
