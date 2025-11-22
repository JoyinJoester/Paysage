package takagi.ru.paysage.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.unit.Dp

/**
 * 应用状态栏 insets 内边距
 */
fun Modifier.statusBarsPadding(): Modifier = composed {
    this.windowInsetsPadding(WindowInsets.statusBars)
}

/**
 * 应用导航栏 insets 内边距
 */
fun Modifier.navigationBarsPadding(): Modifier = composed {
    this.windowInsetsPadding(WindowInsets.navigationBars)
}

/**
 * 应用系统栏 insets 内边距（状态栏 + 导航栏）
 */
fun Modifier.systemBarsPadding(): Modifier = composed {
    this.windowInsetsPadding(WindowInsets.systemBars)
}

/**
 * 获取状态栏高度
 */
@Composable
fun rememberStatusBarHeight(): Dp {
    val insets = WindowInsets.statusBars.asPaddingValues()
    return insets.calculateTopPadding()
}

/**
 * 获取导航栏高度
 */
@Composable
fun rememberNavigationBarHeight(): Dp {
    val insets = WindowInsets.navigationBars.asPaddingValues()
    return insets.calculateBottomPadding()
}
