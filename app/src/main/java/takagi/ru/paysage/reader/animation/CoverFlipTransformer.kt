package takagi.ru.paysage.reader.animation

/**
 * 覆盖效果变换器
 * 
 * 类似于 ViewPager 的 PageTransformer,用于计算页面的变换参数。
 * 实现覆盖翻页效果:上层页面跟随滚动,下层页面保持静止。
 */
class CoverFlipTransformer {
    
    /**
     * 计算页面变换
     * 
     * 根据页面位置计算变换参数(translationX, alpha, zIndex)。
     * 
     * 覆盖翻页效果说明:
     * - position = 0: 页面完全可见（当前页）
     * - position = 1: 页面在右侧屏幕外（下一页）
     * - position = 0.5: 页面滑动到一半
     * 
     * 底层页面（当前页）: 保持静止，translationX = 0
     * 上层页面（下一页）: 从右侧滑入覆盖，translationX 从 0 变化到 -pageWidth
     * 
     * @param page 页面索引
     * @param position 页面位置(-1f 到 1f, 0f 表示完全可见)
     * @param pageWidth 页面宽度(像素)
     * @return 变换参数
     */
    fun transformPage(
        page: Int,
        position: Float,
        pageWidth: Int
    ): PageTransform {
        return when {
            // 左侧页面(已经翻过的页面)
            position < -1f -> {
                PageTransform(
                    translationX = 0f,
                    alpha = 0f,
                    zIndex = 0f
                )
            }
            
            // 当前页面和之前的页面(底层,静止)
            position <= 0f -> {
                PageTransform(
                    translationX = 0f,
                    alpha = 1f,
                    zIndex = 0f  // 在下层
                )
            }
            
            // 下一页(上层,从右侧滑入覆盖)
            position <= 1f -> {
                // 覆盖翻页的关键计算:
                // LazyRow 会自动将页面放置在 position * pageWidth 的位置
                // 我们需要让页面从屏幕右侧（position = 1）滑动到屏幕左侧（position = 0）
                // 当 position = 1 时，页面应该在屏幕右侧边缘，translationX = 0
                // 当 position = 0 时，页面应该完全覆盖屏幕，translationX = -pageWidth
                // 所以 translationX = -position * pageWidth
                val offsetX = -position * pageWidth
                
                PageTransform(
                    translationX = offsetX,
                    alpha = 1f,
                    zIndex = 1f  // 在上层,确保覆盖下层页面
                )
            }
            
            // 右侧页面(还未显示的页面)
            else -> {
                PageTransform(
                    translationX = 0f,
                    alpha = 0f,
                    zIndex = 0f
                )
            }
        }
    }
    
    /**
     * 计算阴影透明度
     * 
     * 在上层页面显示阴影,增强深度感。
     * 阴影透明度随页面位置线性增加。
     * 
     * @param position 页面位置(-1f 到 1f)
     * @param maxAlpha 最大透明度,默认 0.4f
     * @return 阴影透明度(0f 到 maxAlpha)
     */
    fun calculateShadowAlpha(position: Float, maxAlpha: Float = 0.4f): Float {
        return when {
            // 只在上层页面(position > 0)显示阴影
            position in 0f..1f -> {
                // 阴影透明度随位置线性增加
                position * maxAlpha
            }
            else -> 0f
        }
    }
}

/**
 * 页面变换参数
 * 
 * @param translationX X 轴位移(像素)
 * @param alpha 透明度(0f 到 1f)
 * @param zIndex 绘制顺序(值越大越在上层)
 */
data class PageTransform(
    val translationX: Float,
    val alpha: Float,
    val zIndex: Float
)
