package takagi.ru.paysage.util

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * 检查是否启用了高对比度模式
 */
@Composable
fun rememberIsHighContrastEnabled(): Boolean {
    val context = LocalContext.current
    return remember {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        // Android 没有直接的高对比度 API，这里返回 false
        // 实际应用中可以通过其他方式检测，比如用户设置
        false
    }
}

/**
 * 检查是否启用了 TalkBack 屏幕阅读器
 */
@Composable
fun rememberIsTalkBackEnabled(): Boolean {
    val context = LocalContext.current
    return remember {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        accessibilityManager?.isEnabled == true && accessibilityManager.isTouchExplorationEnabled
    }
}
