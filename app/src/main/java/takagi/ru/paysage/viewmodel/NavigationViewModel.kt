package takagi.ru.paysage.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import takagi.ru.paysage.navigation.NavigationState
import takagi.ru.paysage.navigation.PrimaryNavItem
import takagi.ru.paysage.navigation.SecondaryNavItem

/**
 * 导航 ViewModel
 * 管理两层导航抽屉的状态
 */
class NavigationViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    // 导航状态 - 从 SavedStateHandle 恢复基本类型
    private val _navigationState = MutableStateFlow(
        NavigationState(
            selectedPrimaryItem = savedStateHandle.get<String>("selected_primary_item")?.let { 
                try { PrimaryNavItem.valueOf(it) } catch (e: Exception) { PrimaryNavItem.Library }
            } ?: PrimaryNavItem.Library,
            isSecondaryDrawerOpen = savedStateHandle.get<Boolean>("is_drawer_open") ?: false,
            selectedSecondaryItem = savedStateHandle.get<String>("selected_secondary_item"),
            showSourceSelection = savedStateHandle.get<Boolean>("show_source_selection") ?: false
        )
    )
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()
    
    /**
     * 保存状态到 SavedStateHandle
     */
    private fun saveState(state: NavigationState) {
        savedStateHandle["selected_primary_item"] = state.selectedPrimaryItem.name
        savedStateHandle["is_drawer_open"] = state.isSecondaryDrawerOpen
        savedStateHandle["selected_secondary_item"] = state.selectedSecondaryItem
        savedStateHandle["show_source_selection"] = state.showSourceSelection
    }
    
    /**
     * 选择第一层导航项
     * 自动展开第二层抽屉
     */
    fun selectPrimaryItem(item: PrimaryNavItem) {
        _navigationState.update { currentState ->
            val newState = currentState.copy(
                selectedPrimaryItem = item,
                isSecondaryDrawerOpen = true,
                showSourceSelection = false
            )
            saveState(newState)
            newState
        }
    }
    
    /**
     * 切换第二层抽屉的展开/收起状态
     */
    fun toggleSecondaryDrawer(isOpen: Boolean) {
        _navigationState.update { currentState ->
            val newState = currentState.copy(
                isSecondaryDrawerOpen = isOpen
            )
            saveState(newState)
            newState
        }
    }
    
    /**
     * 选择第二层导航项
     * 自动关闭抽屉
     */
    fun selectSecondaryItem(item: SecondaryNavItem) {
        _navigationState.update { currentState ->
            val newState = currentState.copy(
                selectedSecondaryItem = item.id,
                isSecondaryDrawerOpen = false
            )
            saveState(newState)
            newState
        }
    }
    
    /**
     * 关闭第二层抽屉
     */
    fun closeSecondaryDrawer() {
        toggleSecondaryDrawer(false)
    }
    
    /**
     * 打开第二层抽屉
     */
    fun openSecondaryDrawer() {
        toggleSecondaryDrawer(true)
    }
    
    /**
     * 切换源选择页面的显示状态
     */
    fun toggleSourceSelection(show: Boolean) {
        _navigationState.update { currentState ->
            val newState = currentState.copy(
                showSourceSelection = show
            )
            savedStateHandle["show_source_selection"] = show
            newState
        }
    }
}
