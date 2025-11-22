package takagi.ru.paysage.reader.animation

/**
 * 页面滚动监听器
 * 
 * 类似于 RecyclerView.OnScrollListener 或 ViewPager.OnPageChangeListener,
 * 用于监听页面滚动事件。
 * 
 * @param onPageScrolled 页面滚动回调
 *   - position: 当前页索引
 *   - positionOffset: 偏移量(0f 到 1f)
 *   - positionOffsetPixels: 偏移量(像素)
 */
class PageScrollListener(
    private val onPageScrolled: (
        position: Int,
        positionOffset: Float,
        positionOffsetPixels: Int
    ) -> Unit
) {
    /**
     * 处理滚动事件
     * 
     * 从 PagerState 中提取滚动信息并触发回调。
     * 
     * @param pagerState 分页状态
     */
    fun onScroll(pagerState: CoverFlipPagerState) {
        val position = pagerState.currentPage
        val offsetPixels = pagerState.scrollOffset.toInt()
        val offset = if (pagerState.pageWidth > 0) {
            offsetPixels.toFloat() / pagerState.pageWidth
        } else 0f
        
        onPageScrolled(position, offset, offsetPixels)
    }
}
