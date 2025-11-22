package takagi.ru.paysage.reader.animation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * 翻页动画器接口
 * 
 * 参考 Legado 的 PageDelegate 设计，使用策略模式实现多种翻页动画
 * 
 * 核心职责：
 * 1. 管理翻页动画的状态和进度
 * 2. 处理手势跟随（拖动翻页）
 * 3. 在 Canvas 上绘制动画帧
 */
interface PageFlipAnimator {
    
    /**
     * 开始翻页动画
     * 
     * @param direction 翻页方向（NEXT 或 PREVIOUS）
     * @param startOffset 起始触摸点（用于手势跟随），null 表示自动翻页
     * @param onComplete 动画完成回调
     */
    fun startFlip(
        direction: FlipDirection,
        startOffset: Offset? = null,
        onComplete: () -> Unit
    )
    
    /**
     * 更新手势位置（用于拖动翻页）
     * 
     * 当用户拖动屏幕时，实时更新动画状态以跟随手指
     * 
     * @param offset 当前触摸点
     */
    fun updateGesture(offset: Offset)
    
    /**
     * 释放手势（用户抬起手指）
     * 
     * 根据当前进度决定是完成翻页还是取消翻页
     * 
     * @param velocity 释放时的速度
     */
    fun releaseGesture(velocity: Offset = Offset.Zero)
    
    /**
     * 取消翻页动画
     * 
     * 立即停止动画并恢复到初始状态
     */
    fun cancelFlip()
    
    /**
     * 绘制翻页动画帧
     * 
     * 在 Canvas 上绘制当前动画帧
     * 
     * @param currentPage 当前页面内容
     * @param nextPage 下一页内容（翻到下一页时）或上一页内容（翻到上一页时）
     * @param progress 动画进度 [0, 1]
     */
    fun DrawScope.drawFlipFrame(
        currentPage: ImageBitmap?,
        nextPage: ImageBitmap?,
        progress: Float
    )
    
    /**
     * 当前动画进度 [0, 1]
     */
    val progress: Float
    
    /**
     * 是否正在动画中
     */
    val isAnimating: Boolean
}

/**
 * 翻页方向
 */
enum class FlipDirection {
    /** 下一页 */
    NEXT,
    
    /** 上一页 */
    PREVIOUS
}
