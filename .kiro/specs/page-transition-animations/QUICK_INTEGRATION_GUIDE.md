# 快速集成指南

## 核心组件已完成 ✅

所有核心的过渡动画组件已经实现并可以使用：

### 1. 动画系统
- 5种过渡模式：Slide, Overlay, SideBySide, Fade, Curl
- 完整的动画计算和GPU加速支持
- 可配置的速度和灵敏度

### 2. 手势处理
- 边缘滑动检测
- 拖动进度跟踪
- 速度检测和自动完成

### 3. 状态管理
- 过渡状态跟踪
- 动画控制器
- 配置管理

## 最小集成示例

### 在 ReaderScreen 中使用

```kotlin
import takagi.ru.paysage.reader.transition.*

@Composable
fun ReaderScreen(/* ... */) {
    val scope = rememberCoroutineScope()
    
    // 1. 创建过渡控制器
    val controller = remember {
        PageTransitionController(
            scope = scope,
            config = TransitionConfig(
                mode = TransitionMode.Slide(),
                duration = 300
            ),
            onPageChange = { page -> 
                viewModel.goToPage(page)
            }
        )
    }
    
    val transitionState by controller.transitionState
    val animator by controller.currentAnimator
    
    // 2. 使用 PageTransitionContainer 渲染
    PageTransitionContainer(
        currentPageBitmap = currentPageBitmap,
        nextPageBitmap = nextPageBitmap,
        transitionState = transitionState,
        animator = animator,
        modifier = Modifier.fillMaxSize()
    )
}
```

## 切换动画模式

```kotlin
// 滑动模式（默认）
controller.updateMode(TransitionMode.Slide())

// 覆盖模式
controller.updateMode(TransitionMode.Overlay)

// 并排模式
controller.updateMode(TransitionMode.SideBySide)

// 淡入淡出
controller.updateMode(TransitionMode.Fade)

// 卷曲效果
controller.updateMode(TransitionMode.Curl)

// 无动画
controller.updateMode(TransitionMode.None)
```

## 手动触发过渡

```kotlin
// 开始过渡到下一页
controller.startTransition(
    from = currentPage,
    to = currentPage + 1,
    direction = TransitionDirection.FORWARD
)

// 更新进度（0.0 到 1.0）
controller.updateTransition(0.5f)

// 完成过渡
controller.completeTransition(animated = true)

// 取消过渡
controller.cancelTransition(animated = true)
```

## 配置选项

```kotlin
val config = TransitionConfig(
    mode = TransitionMode.Slide(),
    duration = 300,  // 毫秒
    edgeSensitivity = EdgeSensitivity.MEDIUM,
    enableShadow = true,
    enableHaptic = true,
    threshold = 0.3f  // 30% 完成阈值
)
```

## 性能优化提示

### 1. 使用硬件加速
所有动画器都使用 `graphicsLayer` 实现，自动启用 GPU 加速。

### 2. 预加载下一页
```kotlin
// 在 ViewModel 中
fun preloadNextPage() {
    viewModelScope.launch {
        val nextPage = currentPage + 1
        if (nextPage < totalPages) {
            _nextPageBitmap.value = loadPage(nextPage)
        }
    }
}
```

### 3. 内存管理
```kotlin
// 过渡完成后释放旧页面
controller.onPageChange = { newPage ->
    viewModel.goToPage(newPage)
    viewModel.releaseOldPages()
}
```

## 常见问题

### Q: 如何禁用动画？
A: 使用 `TransitionMode.None` 或设置 `duration = 0`

### Q: 如何调整动画速度？
A: 修改 `TransitionConfig.duration`，推荐值：
- 快速：200ms
- 正常：300ms
- 慢速：500ms

### Q: 如何处理手势冲突？
A: 在缩放状态下禁用翻页手势：
```kotlin
if (scale <= 1f) {
    // 允许翻页手势
} else {
    // 禁用翻页，只允许平移
}
```

### Q: 性能不佳怎么办？
A: 自动降级到更简单的模式：
```kotlin
if (frameRate < 45) {
    controller.updateMode(TransitionMode.Fade)
}
```

## 下一步

1. 在 ReaderScreen 中集成 PageTransitionContainer
2. 添加手势检测（使用 PageGestureHandler）
3. 在设置中添加过渡模式选择
4. 测试性能并优化

## 完整示例

查看 `IMPLEMENTATION_SUMMARY.md` 获取完整的集成示例和详细说明。

## 支持

如有问题，请参考：
- `design.md` - 详细设计文档
- `IMPLEMENTATION_SUMMARY.md` - 实现总结
- 源代码注释 - 每个类都有详细的 KDoc
