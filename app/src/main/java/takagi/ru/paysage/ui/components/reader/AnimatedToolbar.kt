package takagi.ru.paysage.ui.components.reader

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay

/**
 * 带动画的工具栏容器
 * 
 * 提供工具栏的显示/隐藏动画效果：
 * 1. 使用 M3E Emphasized Easing 曲线
 * 2. 顶部工具栏从上方滑入/滑出
 * 3. 底部工具栏从下方滑入/滑出
 * 4. 支持自动隐藏计时器
 * 
 * 动画参数：
 * - 进入动画：400ms，EmphasizedDecelerate
 * - 退出动画：200ms，EmphasizedAccelerate
 */

/**
 * M3E Emphasized Easing 曲线
 */
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

/**
 * 动画工具栏容器
 * 
 * @param visible 是否显示工具栏
 * @param autoHideDelay 自动隐藏延迟（毫秒），0 表示不自动隐藏
 * @param onAutoHide 自动隐藏回调
 * @param topBar 顶部工具栏内容
 * @param bottomBar 底部工具栏内容
 * @param content 主要内容
 */
@Composable
fun AnimatedToolbarLayout(
    visible: Boolean,
    autoHideDelay: Long = 3000L,
    onAutoHide: () -> Unit = {},
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    // 自动隐藏计时器
    LaunchedEffect(visible) {
        if (visible && autoHideDelay > 0) {
            delay(autoHideDelay)
            onAutoHide()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 主要内容
        content()
        
        // 顶部工具栏（从上方滑入/滑出）
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                animationSpec = tween(
                    durationMillis = 400,
                    easing = EmphasizedDecelerate
                ),
                initialOffsetY = { -it }
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = 400,
                    easing = LinearEasing
                )
            ),
            exit = slideOutVertically(
                animationSpec = tween(
                    durationMillis = 200,
                    easing = EmphasizedAccelerate
                ),
                targetOffsetY = { -it }
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = 200,
                    easing = LinearEasing
                )
            ),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            topBar()
        }
        
        // 底部工具栏（从下方滑入/滑出）
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                animationSpec = tween(
                    durationMillis = 400,
                    easing = EmphasizedDecelerate
                ),
                initialOffsetY = { it }
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = 400,
                    easing = LinearEasing
                )
            ),
            exit = slideOutVertically(
                animationSpec = tween(
                    durationMillis = 200,
                    easing = EmphasizedAccelerate
                ),
                targetOffsetY = { it }
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = 200,
                    easing = LinearEasing
                )
            ),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            bottomBar()
        }
    }
}

/**
 * 简化版动画工具栏
 * 
 * 只包含顶部工具栏
 */
@Composable
fun AnimatedTopBarLayout(
    visible: Boolean,
    autoHideDelay: Long = 3000L,
    onAutoHide: () -> Unit = {},
    topBar: @Composable () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    AnimatedToolbarLayout(
        visible = visible,
        autoHideDelay = autoHideDelay,
        onAutoHide = onAutoHide,
        topBar = topBar,
        bottomBar = {},
        content = content
    )
}

/**
 * 工具栏可见性状态管理
 */
@Composable
fun rememberToolbarVisibilityState(
    initialVisible: Boolean = false,
    autoHideDelay: Long = 3000L
): ToolbarVisibilityState {
    return remember {
        ToolbarVisibilityState(
            initialVisible = initialVisible,
            autoHideDelay = autoHideDelay
        )
    }
}

/**
 * 工具栏可见性状态
 */
class ToolbarVisibilityState(
    initialVisible: Boolean = false,
    val autoHideDelay: Long = 3000L
) {
    var isVisible by mutableStateOf(initialVisible)
        private set
    
    /**
     * 显示工具栏
     */
    fun show() {
        isVisible = true
    }
    
    /**
     * 隐藏工具栏
     */
    fun hide() {
        isVisible = false
    }
    
    /**
     * 切换工具栏可见性
     */
    fun toggle() {
        isVisible = !isVisible
    }
}
