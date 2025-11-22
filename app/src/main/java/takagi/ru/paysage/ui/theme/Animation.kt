package takagi.ru.paysage.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive Animation Specs
 * 富有表现力的动画配置
 */
object ExpressiveAnimations {
    
    // 标准动画时长
    const val DURATION_SHORT = 200
    const val DURATION_MEDIUM = 300
    const val DURATION_LONG = 500
    const val DURATION_EXTRA_LONG = 700
    
    // Emphasized 加速插值器（Material 3 推荐）
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    
    // 标准 Expressive 动画规格
    val standardEasing = tween<Float>(
        durationMillis = DURATION_MEDIUM,
        easing = EmphasizedEasing
    )
    
    val enterEasing = tween<Float>(
        durationMillis = DURATION_LONG,
        easing = EmphasizedDecelerateEasing
    )
    
    val exitEasing = tween<Float>(
        durationMillis = DURATION_SHORT,
        easing = EmphasizedAccelerateEasing
    )
    
    // 弹簧动画 - 用于生动的交互
    val bouncySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    val gentleSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )
    
    // 页面切换动画
    val pageTransitionSpec = tween<Float>(
        durationMillis = DURATION_MEDIUM,
        easing = EmphasizedEasing
    )
    
    // 卡片展开/收起动画
    val expandCollapseSpec = tween<Float>(
        durationMillis = DURATION_LONG,
        easing = EmphasizedEasing
    )
}

/**
 * Expressive 尺寸规格
 */
object ExpressiveDimensions {
    // 内边距
    val paddingExtraSmall = 4.dp
    val paddingSmall = 8.dp
    val paddingMedium = 16.dp
    val paddingLarge = 24.dp
    val paddingExtraLarge = 32.dp
    
    // 卡片尺寸
    val cardElevation = 4.dp
    val cardElevationHovered = 8.dp
    val cardElevationPressed = 2.dp
    
    // 按钮尺寸
    val buttonHeight = 48.dp
    val smallButtonHeight = 40.dp
    val largeButtonHeight = 56.dp
    
    // 图标尺寸
    val iconSizeSmall = 18.dp
    val iconSizeMedium = 24.dp
    val iconSizeLarge = 36.dp
    val iconSizeExtraLarge = 48.dp
    
    // FAB 尺寸
    val fabSize = 56.dp
    val fabSizeSmall = 40.dp
    val fabSizeLarge = 96.dp
}

/**
 * Expressive 透明度
 */
object ExpressiveAlpha {
    const val disabled = 0.38f
    const val medium = 0.6f
    const val high = 0.87f
    const val full = 1.0f
}
