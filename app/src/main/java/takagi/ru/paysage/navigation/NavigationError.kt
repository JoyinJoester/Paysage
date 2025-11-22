package takagi.ru.paysage.navigation

import android.util.Log

/**
 * 导航错误密封类
 */
sealed class NavigationError {
    /**
     * 无效的路由
     */
    object InvalidRoute : NavigationError()
    
    /**
     * 抽屉状态不匹配
     */
    object DrawerStateMismatch : NavigationError()
    
    /**
     * 导航失败
     */
    data class NavigationFailed(val message: String) : NavigationError()
}

/**
 * 处理导航错误
 */
fun handleNavigationError(
    error: NavigationError,
    onNavigateToDefault: () -> Unit = {}
) {
    when (error) {
        is NavigationError.InvalidRoute -> {
            // 记录错误并导航到默认页面
            Log.e("Navigation", "Invalid route detected, navigating to default")
            onNavigateToDefault()
        }
        is NavigationError.DrawerStateMismatch -> {
            // 重置抽屉状态
            Log.w("Navigation", "Drawer state mismatch detected, attempting to reset")
        }
        is NavigationError.NavigationFailed -> {
            // 显示错误提示
            Log.e("Navigation", "Navigation failed: ${error.message}")
        }
    }
}
