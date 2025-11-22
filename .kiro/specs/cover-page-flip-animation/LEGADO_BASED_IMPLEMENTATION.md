# 基于 Legado 的覆盖翻页实现

## 问题分析

经过 500+ 次会话的尝试，问题的根源在于：

1. **错误的架构选择**：之前尝试使用自定义手势检测和动画
2. **数据源问题**：`getAllPageBitmaps()` 返回包含占位符的列表
3. **没有参考 Legado 的核心实现**：Legado 使用 RecyclerView + translationX

## Legado 的核心原理

根据《翻页覆盖动画开发指南.md》，Legado 的实现关键：

### 1. 覆盖效果实现

```kotlin
// 在滑动过程中，最多可见2个View
// 将第二个View跟随屏幕，其他View保持跟随画布滑动
for (i in 0 until layoutManager.childCount) {
    layoutManager.getChildAt(i)?.also { view ->
        if (i == 1) {
            // view.left是个负数，offsetPx（=-view.left）是个正数
            view.translationX = offsetPx.toFloat() - view.width
        } else {
            view.translationX = 0f
        }
    }
}
```

### 2. 控制绘制顺序

```kotlin
// 前面的View后绘制，保证前面的View在后面的View的绘制层级之上
override fun onGetChildDrawingOrder(childCount: Int, i: Int) = childCount - i - 1
```

## Compose 实现方案

### 核心代码

```kotlin
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CoverFlipPageReader(
    pages: List<Bitmap>,
    initialPage: Int = 0,
    onTap: (TouchZone) -> Unit = {},
    onPageChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, pages.lastIndex),
        pageCount = { pages.size }
    )
    
    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        beyondBoundsPageCount = 1
    ) { page ->
        val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // 覆盖翻页效果：当前页之后的页面跟随屏幕移动
                    if (page > pagerState.currentPage) {
                        translationX = -pageOffset.absoluteValue * size.width
                    }
                }
        ) {
            AsyncImage(
                model = pages[page],
                contentDescription = "Page ${page + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
```

### 关键点解释

1. **HorizontalPager**：Compose 的横向分页组件，类似 ViewPager
2. **graphicsLayer.translationX**：对应 View 的 translationX
3. **pageOffset 计算**：
   - `currentPage - page`：页面相对位置
   - `+ currentPageOffsetFraction`：滑动偏移量（0-1）
4. **覆盖效果条件**：`if (page > pagerState.currentPage)`
   - 只对当前页之后的页面应用 translationX
   - 让它们跟随屏幕而不是跟随画布

## 数据源问题

### 当前问题

`viewModel.getAllPageBitmaps()` 返回：
```kotlin
fun getAllPageBitmaps(): List<Bitmap> {
    val totalPages = _uiState.value.totalPages
    val bitmaps = mutableListOf<Bitmap>()
    
    for (i in 0 until totalPages) {
        val bitmap = pageCache[i] ?: createPlaceholderBitmap() // 1x1 占位符！
        bitmaps.add(bitmap)
    }
    
    return bitmaps
}
```

### 解决方案

有两个选择：

#### 方案 A：修改 getAllPageBitmaps()

```kotlin
fun getAllPageBitmaps(): List<Bitmap?> {
    val totalPages = _uiState.value.totalPages
    return (0 until totalPages).map { pageCache[it] }
}
```

然后在 CoverFlipPageReader 中过滤 null：
```kotlin
CoverFlipPageReader(
    pages = viewModel.getAllPageBitmaps().filterNotNull(),
    // ...
)
```

**问题**：页面索引会错位

#### 方案 B：使用懒加载（推荐）

创建一个支持懒加载的版本：

```kotlin
@Composable
fun LazyLoadCoverFlipReader(
    totalPages: Int,
    currentPage: Int,
    onLoadPage: (Int) -> Bitmap?,
    onTap: (TouchZone) -> Unit = {},
    onPageChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        initialPage = currentPage,
        pageCount = { totalPages }
    )
    
    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        beyondBoundsPageCount = 1
    ) { page ->
        val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
        val bitmap = onLoadPage(page)
        
        if (bitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (page > pagerState.currentPage) {
                            translationX = -pageOffset.absoluteValue * size.width
                        }
                    }
            ) {
                AsyncImage(
                    model = bitmap,
                    contentDescription = "Page ${page + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // 显示加载中
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
```

使用方式：
```kotlin
LazyLoadCoverFlipReader(
    totalPages = uiState.totalPages,
    currentPage = uiState.currentPage,
    onLoadPage = { page -> viewModel.getPageBitmap(page) },
    onTap = { zone -> /* ... */ },
    onPageChange = { page -> viewModel.goToPage(page) }
)
```

## 当前实现状态

### 已完成
- ✅ 创建 `CoverFlipPageReader.kt`
- ✅ 实现基于 Legado 的覆盖翻页逻辑
- ✅ 集成到 ReaderScreen
- ✅ 构建成功

### 待解决
- ⚠️ 数据源问题：`getAllPageBitmaps()` 返回占位符
- ⚠️ 需要实现懒加载版本或修改数据源

## 下一步行动

### 选项 1：快速修复（使用现有缓存）

修改 ReaderScreen 使用已缓存的页面：

```kotlin
when (uiState.readerConfig.pageMode) {
    PageMode.COVER -> {
        // 只使用已缓存的页面
        val cachedPages = (0 until uiState.totalPages)
            .mapNotNull { viewModel.getPageBitmap(it) }
        
        if (cachedPages.isNotEmpty()) {
            CoverFlipPageReader(
                pages = cachedPages,
                initialPage = 0, // 需要重新计算索引
                onTap = { zone -> /* ... */ },
                onPageChange = { /* ... */ }
            )
        }
    }
}
```

**问题**：索引映射复杂

### 选项 2：实现懒加载（推荐）

1. 在 ReaderViewModel 添加方法：
```kotlin
fun getPageBitmap(page: Int): Bitmap? = pageCache[page]
```

2. 创建 `LazyLoadCoverFlipReader.kt`

3. 在 ReaderScreen 中使用懒加载版本

## 测试验证

### 验证步骤

1. 打开应用，选择一本漫画
2. 在设置中选择"覆盖翻页"模式
3. 测试翻页：
   - 向左滑动：下一页应该从右侧覆盖当前页
   - 向右滑动：上一页应该从左侧出现
   - 动画应该流畅，没有跳跃

### 预期效果

- 页面像真实的书页一样从右向左翻过去
- 下一页覆盖在当前页之上
- 滑动流畅，没有延迟
- 边界情况正确处理（第一页/最后一页）

---

**创建时间**：2025-10-30
**状态**：✅ 核心实现完成，⚠️ 需要解决数据源问题
**下一步**：实现懒加载版本或修改数据源
