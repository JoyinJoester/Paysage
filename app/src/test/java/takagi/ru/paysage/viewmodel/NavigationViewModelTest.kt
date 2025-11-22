package takagi.ru.paysage.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import takagi.ru.paysage.navigation.PrimaryNavItem

/**
 * NavigationViewModel 单元测试
 * 测试导航状态管理和源选择状态重置逻辑
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NavigationViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: NavigationViewModel
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        savedStateHandle = SavedStateHandle()
        viewModel = NavigationViewModel(savedStateHandle)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    /**
     * 测试 selectPrimaryItem 会重置 showSourceSelection
     * 这是修复的核心功能
     */
    @Test
    fun `selectPrimaryItem should reset showSourceSelection to false`() = runTest {
        // Given: 源选择页面已打开
        viewModel.toggleSourceSelection(true)
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.navigationState.value.showSourceSelection)
        
        // When: 选择一个一层导航项
        viewModel.selectPrimaryItem(PrimaryNavItem.Settings)
        testScheduler.advanceUntilIdle()
        
        // Then: showSourceSelection 应该被重置为 false
        val state = viewModel.navigationState.value
        assertFalse("selectPrimaryItem 应该重置 showSourceSelection", state.showSourceSelection)
        assertEquals(PrimaryNavItem.Settings, state.selectedPrimaryItem)
        assertTrue(state.isSecondaryDrawerOpen)
    }
    
    /**
     * 测试多次切换一层菜单项的场景
     */
    @Test
    fun `multiple primary item switches should consistently reset showSourceSelection`() = runTest {
        // Given: 源选择页面已打开
        viewModel.toggleSourceSelection(true)
        testScheduler.advanceUntilIdle()
        
        // When: 多次切换一层菜单项
        viewModel.selectPrimaryItem(PrimaryNavItem.Settings)
        testScheduler.advanceUntilIdle()
        assertFalse("第一次切换应该重置 showSourceSelection", 
            viewModel.navigationState.value.showSourceSelection)
        
        // 再次打开源选择
        viewModel.toggleSourceSelection(true)
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.navigationState.value.showSourceSelection)
        
        // 切换到另一个菜单项
        viewModel.selectPrimaryItem(PrimaryNavItem.Library)
        testScheduler.advanceUntilIdle()
        assertFalse("第二次切换应该重置 showSourceSelection", 
            viewModel.navigationState.value.showSourceSelection)
        
        // 再次打开源选择
        viewModel.toggleSourceSelection(true)
        testScheduler.advanceUntilIdle()
        
        // 切换到第三个菜单项
        viewModel.selectPrimaryItem(PrimaryNavItem.About)
        testScheduler.advanceUntilIdle()
        assertFalse("第三次切换应该重置 showSourceSelection", 
            viewModel.navigationState.value.showSourceSelection)
    }
    
    /**
     * 测试状态持久化和恢复
     */
    @Test
    fun `state should be persisted to SavedStateHandle`() = runTest {
        // When: 选择一层导航项
        viewModel.selectPrimaryItem(PrimaryNavItem.Settings)
        testScheduler.advanceUntilIdle()
        
        // Then: 状态应该保存到 SavedStateHandle
        assertEquals("Settings", savedStateHandle.get<String>("selected_primary_item"))
        assertTrue(savedStateHandle.get<Boolean>("is_drawer_open") ?: false)
        assertFalse(savedStateHandle.get<Boolean>("show_source_selection") ?: true)
    }
    
    /**
     * 测试从 SavedStateHandle 恢复状态
     */
    @Test
    fun `state should be restored from SavedStateHandle`() = runTest {
        // Given: SavedStateHandle 中有保存的状态
        val restoredStateHandle = SavedStateHandle().apply {
            set("selected_primary_item", "Settings")
            set("is_drawer_open", true)
            set("show_source_selection", false)
            set("selected_secondary_item", "theme")
        }
        
        // When: 创建新的 ViewModel
        val restoredViewModel = NavigationViewModel(restoredStateHandle)
        testScheduler.advanceUntilIdle()
        
        // Then: 状态应该正确恢复
        val state = restoredViewModel.navigationState.value
        assertEquals(PrimaryNavItem.Settings, state.selectedPrimaryItem)
        assertTrue(state.isSecondaryDrawerOpen)
        assertFalse(state.showSourceSelection)
        assertEquals("theme", state.selectedSecondaryItem)
    }
    
    /**
     * 测试配置变更后的状态恢复场景
     */
    @Test
    fun `state should persist across configuration changes`() = runTest {
        // Given: 用户打开源选择页面
        viewModel.toggleSourceSelection(true)
        testScheduler.advanceUntilIdle()
        
        // When: 模拟配置变更（如屏幕旋转）- 创建新的 ViewModel 实例
        val newViewModel = NavigationViewModel(savedStateHandle)
        testScheduler.advanceUntilIdle()
        
        // Then: 源选择状态应该被恢复
        assertTrue("配置变更后应该恢复 showSourceSelection 状态", 
            newViewModel.navigationState.value.showSourceSelection)
        
        // When: 用户切换到其他菜单项
        newViewModel.selectPrimaryItem(PrimaryNavItem.Settings)
        testScheduler.advanceUntilIdle()
        
        // Then: showSourceSelection 应该被重置
        assertFalse("切换菜单项后应该重置 showSourceSelection", 
            newViewModel.navigationState.value.showSourceSelection)
    }
    
    /**
     * 测试 toggleSourceSelection 方法
     */
    @Test
    fun `toggleSourceSelection should update state correctly`() = runTest {
        // When: 打开源选择
        viewModel.toggleSourceSelection(true)
        testScheduler.advanceUntilIdle()
        
        // Then: 状态应该更新
        assertTrue(viewModel.navigationState.value.showSourceSelection)
        assertTrue(savedStateHandle.get<Boolean>("show_source_selection") ?: false)
        
        // When: 关闭源选择
        viewModel.toggleSourceSelection(false)
        testScheduler.advanceUntilIdle()
        
        // Then: 状态应该更新
        assertFalse(viewModel.navigationState.value.showSourceSelection)
        assertFalse(savedStateHandle.get<Boolean>("show_source_selection") ?: true)
    }
    
    /**
     * 测试初始状态
     */
    @Test
    fun `initial state should have correct defaults`() = runTest {
        // Then: 初始状态应该正确
        val state = viewModel.navigationState.value
        assertEquals(PrimaryNavItem.Library, state.selectedPrimaryItem)
        assertFalse(state.isSecondaryDrawerOpen)
        assertNull(state.selectedSecondaryItem)
        assertFalse(state.showSourceSelection)
    }
    
    /**
     * 测试 selectPrimaryItem 打开第二层抽屉
     */
    @Test
    fun `selectPrimaryItem should open secondary drawer`() = runTest {
        // Given: 第二层抽屉关闭
        viewModel.toggleSecondaryDrawer(false)
        testScheduler.advanceUntilIdle()
        assertFalse(viewModel.navigationState.value.isSecondaryDrawerOpen)
        
        // When: 选择一层导航项
        viewModel.selectPrimaryItem(PrimaryNavItem.Settings)
        testScheduler.advanceUntilIdle()
        
        // Then: 第二层抽屉应该打开
        assertTrue(viewModel.navigationState.value.isSecondaryDrawerOpen)
    }
}
