package takagi.ru.paysage.reader.animation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * 无动画翻页
 * 
 * 即时切换页面，无任何动画效果
 * 
 * 特点：
 * - 最快的翻页速度
 * - 最低的性能消耗
 * - 适合性能较差的设备
 */
class NonePageFlip : PageFlipAnimator {
    
    override var progress: Float = 0f
        private set
    override var isAnimating: Boolean = false
        private set
    
    override fun startFlip(
        direction: FlipDirection,
        startOffset: Offset?,
        onComplete: () -> Unit
    ) {
        // 无动画模式，直接完成
        progress = 1f
        isAnimating = false
        onComplete()
    }
    
    override fun updateGesture(offset: Offset) {
        // 无动画模式不处理手势
    }
    
    override fun releaseGesture(velocity: Offset) {
        // 无动画模式不处理手势
    }
    
    override fun cancelFlip() {
        isAnimating = false
        progress = 0f
    }
    
    override fun DrawScope.drawFlipFrame(
        currentPage: ImageBitmap?,
        nextPage: ImageBitmap?,
        progress: Float
    ) {
        // 根据进度决定显示哪一页
        val pageToShow = if (progress >= 0.5f) nextPage else currentPage
        
        pageToShow?.let {
            drawImage(it)
        }
    }
    

}
