# 需求文档

## 简介

本功能为 Paysage 阅读器实现"覆盖翻页"动画效果。这是一种经典的页面切换动画，上层页面完全覆盖下层页面，当触发翻页时，上层页面整体移开（向左、向右或向上滑动），下层页面在动画过程中逐渐显露出来。这种效果简洁流畅，提供清晰的视觉反馈。

## 术语表

- **Reader System**: 阅读器系统，负责显示和管理漫画/图书内容的核心模块
- **Cover Flip**: 覆盖翻页，上层页面覆盖下层页面，翻页时上层移开显示下层的动画效果
- **Top Layer**: 上层页面，当前显示的页面，在翻页时会移开
- **Bottom Layer**: 下层页面，下一页或上一页的内容，在翻页时逐渐显露
- **Drag Gesture**: 拖动手势，用户通过滑动屏幕触发的交互
- **Transition Progress**: 过渡进度，从 0.0（开始）到 1.0（完成）的动画进度值
- **Shadow Effect**: 阴影效果，在上层页面边缘显示的阴影，增强深度感
- **Threshold**: 阈值，判断是否完成翻页的临界值（通常为屏幕宽度的 30%）
- **GraphicsLayer**: Compose 的图形层，用于硬件加速的变换和动画
- **Touch Zone**: 触摸区域，屏幕上用于触发不同操作的区域划分

## 需求

### 需求 1: 双层页面结构

**用户故事:** 作为阅读器用户，我希望在翻页时能看到上下两层页面，以便清楚地看到页面切换的过程

#### 验收标准

1. THE Reader System SHALL maintain two page layers during transition: Top Layer displaying current page and Bottom Layer displaying next or previous page
2. WHEN a page transition starts, THE Reader System SHALL position the Top Layer completely covering the Bottom Layer with zero pixel gap
3. THE Reader System SHALL render both layers at full screen resolution with dimensions matching the device screen size
4. WHILE the transition is in progress, THE Reader System SHALL keep both layers visible and properly aligned
5. WHEN the transition completes, THE Reader System SHALL display only the Bottom Layer as the new current page

### 需求 2: 平滑移动动画

**用户故事:** 作为阅读器用户，我希望上层页面能够平滑地移开，以便获得流畅的视觉体验

#### 验收标准

1. WHEN the user drags the Top Layer, THE Reader System SHALL translate the Top Layer horizontally following the drag position with zero millisecond latency
2. THE Reader System SHALL calculate translation offset as a linear function of drag distance from 0 pixels to screen width
3. WHILE dragging, THE Reader System SHALL update the Top Layer position at minimum 60 frames per second
4. WHEN the user releases the drag, THE Reader System SHALL animate the Top Layer to its final position using spring animation with 300 millisecond duration
5. THE Reader System SHALL use hardware-accelerated GraphicsLayer transformations for all translation operations

### 需求 3: 下层页面显露

**用户故事:** 作为阅读器用户，我希望在上层页面移开时能逐渐看到下层页面，以便预览即将阅读的内容

#### 验收标准

1. WHEN the Top Layer begins moving, THE Reader System SHALL immediately reveal the Bottom Layer beneath it
2. THE Reader System SHALL keep the Bottom Layer stationary at its original position throughout the transition
3. WHILE the Top Layer moves, THE Reader System SHALL reveal the Bottom Layer proportionally to the translation distance
4. THE Reader System SHALL ensure the Bottom Layer is fully visible when the Top Layer translation reaches 100 percent of screen width
5. WHEN the transition completes, THE Reader System SHALL remove the Top Layer from the view hierarchy

### 需求 4: 视觉对齐

**用户故事:** 作为阅读器用户，我希望两层页面在切换过程中保持完美对齐，以便获得专业的视觉效果

#### 验收标准

1. THE Reader System SHALL align both Top Layer and Bottom Layer to the same coordinate origin with zero pixel offset
2. WHEN rendering pages, THE Reader System SHALL apply identical scaling and centering logic to both layers
3. THE Reader System SHALL ensure both layers have the same dimensions matching the screen size
4. WHILE transitioning, THE Reader System SHALL maintain pixel-perfect alignment between the two layers
5. THE Reader System SHALL prevent any visual artifacts such as gaps, overlaps, or misalignment during the transition

### 需求 5: 动画时长控制

**用户故事:** 作为阅读器用户，我希望翻页动画的速度适中，既不太快也不太慢

#### 验收标准

1. THE Reader System SHALL complete automatic page transitions in 300 milliseconds when user releases drag
2. WHEN the user drags the page, THE Reader System SHALL follow the drag in real-time with zero artificial delay
3. THE Reader System SHALL use easing function with deceleration curve for smooth animation completion
4. WHEN the drag velocity exceeds 1000 pixels per second, THE Reader System SHALL reduce animation duration to 200 milliseconds
5. THE Reader System SHALL allow configuration of animation duration between 200 milliseconds and 500 milliseconds

### 需求 6: 触摸事件触发

**用户故事:** 作为阅读器用户，我希望能够通过触摸和拖动来控制翻页，以便自然地与应用交互

#### 验收标准

1. WHEN the user taps the right third of the screen, THE Reader System SHALL trigger next page transition with automatic animation
2. WHEN the user taps the left third of the screen, THE Reader System SHALL trigger previous page transition with automatic animation
3. WHEN the user drags horizontally from any screen position, THE Reader System SHALL start interactive transition following the drag
4. THE Reader System SHALL detect drag gestures with minimum 10 density-independent pixel movement threshold
5. WHEN the user drags beyond screen boundaries, THE Reader System SHALL clamp the translation to maximum screen width

### 需求 7: 翻页完成判断

**用户故事:** 作为阅读器用户，我希望系统能智能判断我是否想要翻页，避免误操作

#### 验收标准

1. WHEN the user releases drag and translation exceeds 30 percent of screen width, THE Reader System SHALL complete the page transition
2. WHEN the user releases drag and translation is less than 30 percent of screen width, THE Reader System SHALL animate back to original position
3. WHEN the drag velocity exceeds 1000 pixels per second in the swipe direction, THE Reader System SHALL complete the transition regardless of distance
4. THE Reader System SHALL calculate velocity using the last 100 milliseconds of drag movement
5. WHEN returning to original position, THE Reader System SHALL use 200 millisecond animation duration

### 需求 8: 阴影效果

**用户故事:** 作为阅读器用户，我希望看到上层页面的阴影效果，以便更好地感知页面的层次关系

#### 验收标准

1. WHILE the Top Layer is moving, THE Reader System SHALL render a shadow along the leading edge of the Top Layer
2. THE Reader System SHALL calculate shadow opacity based on Transition Progress from 0.0 to 0.4 alpha value
3. THE Reader System SHALL render shadow with 8 density-independent pixel blur radius
4. WHEN Transition Progress is 0.0, THE Reader System SHALL display shadow with 0.0 alpha
5. WHEN Transition Progress is 1.0, THE Reader System SHALL display shadow with 0.4 alpha

### 需求 9: 方向支持

**用户故事:** 作为阅读器用户，我希望能够向左、向右或向上滑动翻页，以便适应不同的阅读习惯

#### 验收标准

1. THE Reader System SHALL support horizontal left-to-right page transitions for previous page navigation
2. THE Reader System SHALL support horizontal right-to-left page transitions for next page navigation
3. THE Reader System SHALL support vertical top-to-bottom page transitions for scroll-style reading
4. WHEN the user drags in a direction, THE Reader System SHALL lock the transition to that axis preventing diagonal movement
5. THE Reader System SHALL determine transition direction based on the first 20 pixels of drag movement

### 需求 10: 性能优化

**用户故事:** 作为阅读器用户，我希望翻页动画流畅不卡顿，即使在查看大图时也能保持性能

#### 验收标准

1. THE Reader System SHALL use GPU hardware acceleration for all page layer transformations
2. THE Reader System SHALL pre-render both Top Layer and Bottom Layer as bitmaps before transition starts
3. THE Reader System SHALL maintain minimum 60 frames per second during the entire transition
4. WHEN rendering pages, THE Reader System SHALL use compositing layers to reduce CPU overhead
5. THE Reader System SHALL release bitmap resources within 500 milliseconds after transition completes

### 需求 11: 边界处理

**用户故事:** 作为阅读器用户，我希望在第一页或最后一页时有清晰的反馈，避免困惑

#### 验收标准

1. WHEN the user is on the first page and attempts to go to previous page, THE Reader System SHALL display a bounce animation with 100 pixel maximum displacement
2. WHEN the user is on the last page and attempts to go to next page, THE Reader System SHALL display a bounce animation with 100 pixel maximum displacement
3. THE Reader System SHALL complete bounce animation in 200 milliseconds with spring easing
4. WHEN at page boundaries, THE Reader System SHALL not load or display non-existent pages
5. THE Reader System SHALL provide visual feedback such as edge glow effect when reaching page boundaries

### 需求 12: 多点触控处理

**用户故事:** 作为阅读器用户，我希望在使用缩放等多点触控手势时，翻页动画能够正确取消

#### 验收标准

1. WHEN a second finger touches the screen during page transition, THE Reader System SHALL immediately cancel the transition
2. WHEN transition is cancelled, THE Reader System SHALL animate the Top Layer back to original position in 150 milliseconds
3. THE Reader System SHALL ignore all drag gestures when two or more fingers are detected on screen
4. WHEN multi-touch is detected, THE Reader System SHALL prioritize zoom and pan gestures over page transitions
5. THE Reader System SHALL resume normal page transition behavior when touch count returns to one finger
