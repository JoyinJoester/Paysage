package takagi.ru.paysage.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import takagi.ru.paysage.data.model.HistoryItem
import takagi.ru.paysage.viewmodel.HistoryViewModel

/**
 * 主页和历史记录页面的滑动容器
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LibraryWithHistoryPager(
    onBookClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    filter: String? = null,
    category: String? = null,
    onNavigateToCategory: ((String) -> Unit)? = null,
    historyViewModel: HistoryViewModel,
    onHistoryItemClick: (HistoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 2 }
    )
    val scope = rememberCoroutineScope()
    
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        // 反向滑动：从左向右滑动进入历史记录
        reverseLayout = false
    ) { page ->
        when (page) {
            0 -> {
                // 主页（书库）
                LibraryScreen(
                    onBookClick = onBookClick,
                    onSettingsClick = onSettingsClick,
                    onOpenDrawer = onOpenDrawer,
                    filter = filter,
                    category = category,
                    onNavigateToCategory = onNavigateToCategory
                )
            }
            1 -> {
                // 历史记录页面 - 仿照图片UI风格
                HistoryScreen(
                    viewModel = historyViewModel,
                    onBackClick = {
                        // 滑动回主页
                        scope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    },
                    onItemClick = onHistoryItemClick
                )
            }
        }
    }
}
