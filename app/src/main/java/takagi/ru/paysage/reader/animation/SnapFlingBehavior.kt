package takagi.ru.paysage.reader.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import kotlin.math.abs

/**
 * 自定义 Fling 行为,实现跨页吸附
 * 
 * 参考 RecyclerView 的 SnapHelper 实现,在用户快速滑动(Fling)或拖拽结束后,
 * 自动吸附到最近的页面。
 * 
 * @param pagerState 分页状态管理器
 * @param snapThreshold 吸附阈值(0f 到 1f),默认 0.3f 表示滑动超过 30% 就翻页
 * @param velocityThreshold 速度阈值(dp/s),默认 1000f,超过此速度直接翻页
 */
class SnapFlingBehavior(
    private val pagerState: CoverFlipPagerState,
    private val snapThreshold: Float = 0.3f,
    private val velocityThreshold: Float = 1000f
) : FlingBehavior {
    
    /**
     * 执行 Fling 动画
     * 
     * 当用户快速滑动时,系统会调用此方法。
     * 我们需要计算目标页面,然后执行吸附动画。
     * 如果在边界,则执行回弹动画。
     * 
     * @param initialVelocity 初始速度(dp/s)
     * @return 剩余速度
     */
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        // 1. 检测边界
        if (isAtBoundary(pagerState.currentPage, pagerState.pageCount, initialVelocity)) {
            // 在边界,执行回弹动画
            return showBounceAnimation(initialVelocity)
        }
        
        // 2. 计算目标页面
        val targetPage = calculateTargetPage(
            currentPage = pagerState.currentPage,
            scrollProgress = pagerState.scrollProgress,
            velocity = initialVelocity,
            pageCount = pagerState.pageCount
        )
        
        // 3. 执行吸附动画
        return snapToPage(targetPage, initialVelocity)
    }
    
    /**
     * 显示边界回弹动画
     * 
     * 当用户在第一页向右滑或最后一页向左滑时,显示回弹效果。
     * 
     * @param initialVelocity 初始速度
     * @return 剩余速度
     */
    private suspend fun ScrollScope.showBounceAnimation(initialVelocity: Float): Float {
        val bounceDistance = 100f  // 回弹距离(像素)
        val direction = if (initialVelocity > 0) 1f else -1f
        
        val animatable = Animatable(0f, Float.VectorConverter)
        
        // 1. 弹出动画(100ms)
        animatable.animateTo(
            targetValue = bounceDistance * direction,
            animationSpec = tween(durationMillis = 100)
        ) {
            val delta = value - animatable.value
            scrollBy(delta)
        }
        
        // 2. 回弹动画(200ms,使用 spring)
        animatable.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) {
            val delta = value - animatable.value
            scrollBy(delta)
        }
        
        return 0f
    }
    
    /**
     * 计算目标页面
     * 
     * 根据当前滚动进度和速度判断应该吸附到哪一页。
     * 包含边界检测,确保不会越界。
     * 
     * @param currentPage 当前页索引
     * @param scrollProgress 滚动进度(0f 到 1f)
     * @param velocity 滑动速度(dp/s)
     * @param pageCount 总页面数
     * @return 目标页面索引
     */
    private fun calculateTargetPage(
        currentPage: Int,
        scrollProgress: Float,
        velocity: Float,
        pageCount: Int
    ): Int {
        val absProgress = abs(scrollProgress)
        val absVelocity = abs(velocity)
        
        val targetPage = when {
            // 速度足够大,直接翻页
            absVelocity > velocityThreshold -> {
                if (velocity > 0) currentPage + 1 else currentPage - 1
            }
            // 滚动超过阈值,翻页
            absProgress > snapThreshold -> {
                if (scrollProgress > 0) currentPage + 1 else currentPage - 1
            }
            // 否则回到当前页
            else -> currentPage
        }
        
        // 边界检测:确保目标页面在有效范围内
        return targetPage.coerceIn(0, pageCount - 1)
    }
    
    /**
     * 检测是否在边界
     * 
     * @param currentPage 当前页索引
     * @param pageCount 总页面数
     * @param direction 滑动方向(正数向右,负数向左)
     * @return 是否在边界
     */
    private fun isAtBoundary(currentPage: Int, pageCount: Int, direction: Float): Boolean {
        return (currentPage == 0 && direction > 0) || 
               (currentPage == pageCount - 1 && direction < 0)
    }
    
    /**
     * 吸附到指定页面
     * 
     * 使用 Animatable 实现平滑滚动动画。
     * 
     * @param targetPage 目标页面索引
     * @param initialVelocity 初始速度
     * @return 剩余速度
     */
    private suspend fun ScrollScope.snapToPage(
        targetPage: Int,
        initialVelocity: Float
    ): Float {
        // 计算目标偏移量
        val currentOffset = pagerState.scrollOffset
        val targetOffset = 0f  // 目标是让页面完全对齐,偏移量为 0
        
        // 计算需要滚动的距离
        val distance = targetOffset - currentOffset + 
                      (targetPage - pagerState.currentPage) * pagerState.pageWidth
        
        // 使用 Animatable 实现平滑滚动
        val animatable = Animatable(0f, Float.VectorConverter)
        
        // 根据速度选择动画规格
        val animationSpec = getAnimationSpec(abs(initialVelocity))
        
        animatable.animateTo(
            targetValue = distance,
            initialVelocity = initialVelocity,
            animationSpec = animationSpec
        ) {
            // 更新滚动位置
            val delta = value - animatable.value
            scrollBy(delta)
        }
        
        return animatable.velocity
    }
    
    /**
     * 根据速度获取动画规格
     * 
     * 高速滑动使用较短的动画时长,低速滑动使用较长的动画时长。
     * 
     * @param velocity 速度绝对值
     * @return 动画规格
     */
    private fun getAnimationSpec(velocity: Float): AnimationSpec<Float> {
        return if (velocity > velocityThreshold * 2) {
            // 高速:200ms
            tween(durationMillis = 200)
        } else {
            // 正常速度:300ms,使用 spring 动画
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        }
    }
    
    /**
     * 吸附到最近的可见 View
     * 
     * 用于处理拖拽结束后的吸附(Scroll Idle 情况)。
     * 
     * @param layoutInfo LazyRow 的布局信息
     * @return Pair(滚动距离, 目标页索引),如果无需吸附则返回 null
     */
    suspend fun ScrollScope.snapToTargetExistingView(
        layoutInfo: LazyListLayoutInfo
    ): Pair<Int, Int>? {
        val visibleItems = layoutInfo.visibleItemsInfo
        
        if (visibleItems.isEmpty()) return null
        
        // 找到距离中心最近的 View
        var closestChild: LazyListItemInfo? = null
        var absClosest = Int.MAX_VALUE
        var scrollDistance = 0
        
        // LazyRow 的中心点
        val containerCenter = layoutInfo.viewportStartOffset + 
                             layoutInfo.viewportSize.width / 2
        
        for (item in visibleItems) {
            val childCenter = item.offset + item.size / 2
            val absDistance = abs(childCenter - containerCenter)
            
            if (absDistance < absClosest) {
                absClosest = absDistance
                closestChild = item
                scrollDistance = childCenter - containerCenter
            }
        }
        
        closestChild ?: return null
        
        // 平滑滚动到目标位置
        val animatable = Animatable(0f, Float.VectorConverter)
        animatable.animateTo(
            targetValue = scrollDistance.toFloat(),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) {
            val delta = value - animatable.value
            scrollBy(delta)
        }
        
        return Pair(scrollDistance, closestChild.index)
    }
}
