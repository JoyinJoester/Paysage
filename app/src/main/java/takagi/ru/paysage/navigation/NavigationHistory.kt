package takagi.ru.paysage.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateListOf

/**
 * 导航历史记录
 * 用于跟踪用户的导航路径
 */
class NavigationHistory {
    private val history = mutableStateListOf<String>()
    
    /**
     * 添加导航记录
     */
    fun push(route: String) {
        // 避免重复添加相同的路由
        if (history.lastOrNull() != route) {
            history.add(route)
            // 限制历史记录数量
            if (history.size > 50) {
                history.removeAt(0)
            }
        }
    }
    
    /**
     * 获取上一个路由
     */
    fun pop(): String? {
        return if (history.size > 1) {
            history.removeAt(history.lastIndex)
            history.lastOrNull()
        } else {
            null
        }
    }
    
    /**
     * 获取当前路由
     */
    fun current(): String? {
        return history.lastOrNull()
    }
    
    /**
     * 清空历史记录
     */
    fun clear() {
        history.clear()
    }
    
    /**
     * 获取历史记录列表
     */
    fun getHistory(): List<String> {
        return history.toList()
    }
    
    /**
     * 检查是否可以返回
     */
    fun canGoBack(): Boolean {
        return history.size > 1
    }
}

/**
 * 记住导航历史
 */
@Composable
fun rememberNavigationHistory(): NavigationHistory {
    return remember { NavigationHistory() }
}
