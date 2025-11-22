package takagi.ru.paysage.reader.animation

import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 覆盖翻页分页状态管理器
 * 
 * 管理 LazyRow 的分页状态,包括当前页索引、滚动偏移量、滚动进度等。
 * 类似于 ViewPager 的 PagerState。
 * 
 * @param initialPage 初始页面索引
 * @param pageCount 总页面数
 */
@Stable
class CoverFlipPagerState(
    initialPage: Int = 0,
    val pageCount: Int
) {
    /**
     * LazyListState 用于控制 LazyRow
     */
    val lazyListState = LazyListState(initialPage)
    
    /**
     * 当前页索引
     */
    var currentPage by mutableStateOf(initialPage)
        private set
    
    /**
     * 滚动偏移量(像素)
     * 表示当前页面相对于容器起始位置的偏移
     */
    var scrollOffset by mutableStateOf(0f)
        private set
    
    /**
     * 页面宽度(像素)
     * 从 LazyListLayoutInfo 获取
     */
    var pageWidth by mutableStateOf(0)
        private set
    
    /**
     * 滚动进度(0f 到 1f)
     * 表示当前滚动到下一页的进度
     */
    val scrollProgress: Float
        get() = if (pageWidth > 0) {
            scrollOffset / pageWidth
        } else 0f
    
    /**
     * 是否正在滚动
     */
    val isScrollInProgress: Boolean
        get() = lazyListState.isScrollInProgress
    
    /**
     * 滚动到指定页面
     * 
     * @param page 目标页面索引
     */
    suspend fun scrollToPage(page: Int) {
        val targetPage = page.coerceIn(0, pageCount - 1)
        lazyListState.animateScrollToItem(targetPage)
        currentPage = targetPage
    }
    
    /**
     * 立即跳转到指定页面(无动画)
     * 
     * @param page 目标页面索引
     */
    suspend fun jumpToPage(page: Int) {
        val targetPage = page.coerceIn(0, pageCount - 1)
        lazyListState.scrollToItem(targetPage)
        currentPage = targetPage
    }
    
    /**
     * 更新状态
     * 
     * 从 LazyListLayoutInfo 中提取信息并更新内部状态。
     * 应该在 LaunchedEffect 中调用,监听 layoutInfo 的变化。
     * 
     * @param layoutInfo LazyRow 的布局信息
     */
    fun updateState(layoutInfo: LazyListLayoutInfo) {
        val firstVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull()
        if (firstVisibleItem != null) {
            // 更新当前页索引
            currentPage = firstVisibleItem.index
            
            // 更新滚动偏移量
            // offset 是负数,表示 item 相对于容器起始位置的偏移
            scrollOffset = -firstVisibleItem.offset.toFloat()
            
            // 更新页面宽度
            if (firstVisibleItem.size > 0) {
                pageWidth = firstVisibleItem.size
            }
        }
    }
    
    /**
     * 获取指定页面的位置
     * 
     * @param pageIndex 页面索引
     * @return 页面位置(-1f 到 1f, 0f 表示完全可见)
     */
    fun getPagePosition(pageIndex: Int): Float {
        return (pageIndex - currentPage) - scrollProgress
    }
}
