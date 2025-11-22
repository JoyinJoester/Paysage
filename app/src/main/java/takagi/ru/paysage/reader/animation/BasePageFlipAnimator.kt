package takagi.ru.paysage.reader.animation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * 基础翻页动画器抽象类
 * 
 * 提供通用的动画状态管理和基础功能
 */
abstract class BasePageFlipAnimator : PageFlipAnimator {
    protected var _isAnimating: Boolean = false
    protected var _progress: Float = 0f
    protected var _direction: FlipDirection = FlipDirection.NEXT
    protected var _startOffset: Offset = Offset.Zero
    protected var _currentOffset: Offset = Offset.Zero
    protected var _onComplete: () -> Unit = {}
    
    override val isAnimating: Boolean
        get() = _isAnimating
    
    override val progress: Float
        get() = _progress
    
    override fun startFlip(
        direction: FlipDirection,
        startOffset: Offset?,
        onComplete: () -> Unit
    ) {
        _isAnimating = true
        _progress = 0f
        _direction = direction
        _startOffset = startOffset ?: Offset.Zero
        _currentOffset = _startOffset
        _onComplete = onComplete
        onFlipStart()
    }
    
    override fun updateGesture(offset: Offset) {
        if (_isAnimating) {
            _currentOffset = offset
            onGestureUpdate(offset)
        }
    }
    
    override fun releaseGesture(velocity: Offset) {
        if (_isAnimating) {
            onGestureRelease(velocity)
        }
    }
    
    override fun cancelFlip() {
        if (_isAnimating) {
            _isAnimating = false
            _progress = 0f
            onFlipCancel()
        }
    }
    
    /**
     * 更新动画进度
     */
    protected fun updateProgress(newProgress: Float) {
        _progress = newProgress.coerceIn(0f, 1f)
        if (_progress >= 1f) {
            _isAnimating = false
            onFlipComplete()
            _onComplete()
        }
    }
    
    /**
     * 动画开始时调用
     */
    protected open fun onFlipStart() {}
    
    /**
     * 手势更新时调用
     */
    protected open fun onGestureUpdate(offset: Offset) {}
    
    /**
     * 手势释放时调用
     */
    protected open fun onGestureRelease(velocity: Offset) {}
    
    /**
     * 动画取消时调用
     */
    protected open fun onFlipCancel() {}
    
    /**
     * 动画完成时调用
     */
    protected open fun onFlipComplete() {}
    
    /**
     * 绘制当前页面（默认实现）
     */
    protected fun DrawScope.drawCurrentPage(page: ImageBitmap) {
        drawImage(page)
    }
    
    /**
     * 绘制下一页面（默认实现）
     */
    protected fun DrawScope.drawNextPage(page: ImageBitmap) {
        drawImage(page)
    }
}
