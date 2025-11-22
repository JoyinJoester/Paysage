# 实现计划

- [x] 1. 修改 PageGestureHandler 实现实时跟随


  - 添加 `handleDragUpdate` 方法处理拖动过程中的实时更新
  - 修改 `handleDragStart` 方法添加边界检测参数 `canGoBack` 和 `canGoForward`
  - 修正方向判断逻辑，确保向右滑动时 `dragDistance` 为正值
  - 修改 `onTransitionUpdate` 回调签名，添加 `dragDistance` 参数传递实际拖动距离
  - 实现有效拖动距离计算，根据方向过滤无效拖动
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 3.1, 3.2, 3.3, 3.5_



- [ ] 2. 扩展 TransitionState 数据模型
  - 在 `TransitionState` 中添加 `dragDistance: Float` 字段存储实际拖动距离
  - 更新 `TransitionState.Idle` 伴生对象包含新字段


  - 修改所有创建 `TransitionState` 的地方传递 `dragDistance` 参数
  - _Requirements: 3.1, 3.2_

- [ ] 3. 修改 PageTransitionController 支持实时更新
  - 修改 `updateTransition` 方法接受 `dragDistance` 参数


  - 更新 `_transitionState` 时同时更新 `progress` 和 `dragDistance`
  - 确保 `startTransition` 初始化时 `dragDistance` 为 0
  - 在 `animateToComplete` 和 `animateToCancel` 中正确处理 `dragDistance` 的动画
  - _Requirements: 3.1, 3.2, 3.4_

- [ ] 4. 重构 PageTransitionContainer 实现堆叠式渲染
  - 添加 `dragDistance: Float` 参数到 `PageTransitionContainer` 函数签名


  - 修改布局结构：下层显示新页面（固定位置），上层显示当前页面（跟随手指）
  - 修正当前页面的 `translationX` 计算：向右滑动使用正值，向左滑动使用负值
  - 实现边缘阴影效果，透明度随 `progress` 变化（0 到 0.3）
  - 设置当前页面的 `shadowElevation` 为 8dp 创建层级感
  - 确保新页面完全不动（`translationX = 0`）
  - _Requirements: 1.1, 1.2, 2.1, 2.2, 2.3, 2.4, 8.1, 8.2, 8.4_

- [ ] 5. 修改 PageViewWithTransition 集成实时拖动
  - 添加 `previousBitmap: Bitmap?` 参数支持向前翻页
  - 添加状态变量：`dragStartOffset`、`currentDragOffset`、`currentDragDistance`


  - 修改 `detectDragGestures` 的 `onDragStart` 回调，传递边界检测参数
  - 实现 `onDrag` 回调实时更新 `currentDragOffset` 和 `currentDragDistance`
  - 在 `onDrag` 中调用 `gestureHandler.handleDragUpdate` 实时更新手势状态
  - 修改 `onDragEnd` 回调，使用当前 `progress` 判断是否完成翻页
  - 实现 `onDragCancel` 回调，调用 `gestureHandler.cancelDrag` 并重置状态
  - 根据 `transitionState.direction` 选择显示 `nextBitmap` 或 `previousBitmap`
  - 将 `currentDragDistance` 传递给 `PageTransitionContainer`
  - _Requirements: 1.1, 1.2, 1.4, 3.1, 3.2, 3.3, 3.4, 8.3, 9.1, 9.2_



- [ ] 6. 实现边界情况处理
  - 在 `PageViewWithTransition` 的 `onDragStart` 中计算 `canGoBack` 和 `canGoForward`
  - 当 `currentPage == 0` 时设置 `canGoBack = false`
  - 当 `currentPage == totalPages - 1` 时设置 `canGoForward = false`
  - 在 `PageGestureHandler.handleDragStart` 中检查边界条件，返回 `false` 阻止手势
  - 实现 `calculateBoundaryResistance` 函数提供边界阻力效果（最大 20px）

  - 创建 `BoundaryFeedback` Composable 显示边界视觉反馈（红色闪烁 100ms）
  - 在 `PageViewWithTransition` 中集成边界反馈显示
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ] 7. 优化动画曲线和时长
  - 在 `PageTransitionController.animateToComplete` 中使用 `FastOutSlowInEasing`
  - 设置完成动画时长为 300ms
  - 在 `PageTransitionController.animateToCancel` 中使用 `FastOutSlowInEasing`

  - 设置回弹动画时长为 250ms
  - 实现快速滑动检测：当速度超过 2000 px/s 时缩短动画时长到 200ms
  - 在 `PageGestureHandler.handleDragEnd` 中添加速度参数并传递实际速度值
  - _Requirements: 4.5, 5.2, 5.3, 7.1, 7.2, 10.1, 10.2, 10.3_

- [ ] 8. 实现多点触控取消机制
  - 在 `PageViewWithTransition` 中添加 `isMultiTouch` 状态变量

  - 使用 `awaitPointerEventScope` 监听触摸事件
  - 检测 `event.changes.size > 1` 判断多点触控
  - 当检测到第二根手指时调用 `gestureHandler.cancelDrag()`
  - 触发回弹动画（200ms）返回原始位置
  - 重置所有手势状态变量
  - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5_



- [ ] 9. 实现双向拖动支持
  - 在 `PageGestureHandler.handleDragUpdate` 中支持方向反转
  - 当拖动方向与初始方向相反时，将有效拖动距离设为 0
  - 确保 `progress` 在方向反转时平滑过渡
  - 在 `PageTransitionContainer` 中根据当前方向更新阴影位置
  - 测试中途改变方向的场景，确保无视觉故障


  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

- [ ] 10. 优化触摸响应性能
  - 确保 `detectDragGestures` 在高优先级线程执行
  - 实现触摸事件批处理，限制更新频率为 60fps（16ms 间隔）
  - 在 `handleDragUpdate` 中添加时间戳检查，避免过度更新


  - 使用 `graphicsLayer` 确保所有变换使用 GPU 加速
  - 避免在拖动过程中使用 `clip()` 或复杂的 `drawBehind`
  - _Requirements: 3.1, 7.3, 7.4, 11.1, 11.2, 11.3, 11.4, 11.5_

- [ ] 11. 集成页面预加载
  - 在 `PageGestureHandler.handleDragStart` 中触发相邻页面预加载


  - 根据 `direction` 决定预加载 `currentPage + 1` 或 `currentPage - 1`
  - 使用现有的 `PagePreloader` 在 100ms 内完成预加载
  - 实现 `TransitionMemoryLock` 类锁定过渡期间的页面，防止被回收
  - 在过渡完成或取消后解锁页面
  - _Requirements: 7.3, 7.4_

- [x] 12. 添加错误处理


  - 在 `PageTransitionContainer` 中检查 `nextBitmap` 是否为 null
  - 如果位图加载失败，调用 `controller.cancelTransition` 并显示错误提示
  - 在 `startTransition` 中添加 try-catch 捕获 `OutOfMemoryError`
  - 内存不足时清理缓存并降级到无动画模式
  - 在手势冲突时（缩放 vs 翻页）正确取消翻页手势
  - _Requirements: 所有需求的健壮性_



- [ ] 13. 更新 ReaderScreen 集成新的翻页逻辑
  - 修改 `ReaderScreen` 中调用 `PageViewWithTransition` 的地方
  - 传递 `previousBitmap` 参数（从 ViewModel 获取）
  - 确保 `currentPage` 和 `totalPages` 正确传递
  - 移除旧的手势处理代码，避免冲突
  - 测试单页模式和双页模式下的翻页功能
  - _Requirements: 所有需求的集成_

- [ ] 14. 编写单元测试
  - 测试 `PageGestureHandler.handleDragUpdate` 的方向和距离计算
  - 测试边界检测逻辑（第一页和最后一页）
  - 测试阈值判断（30% 阈值触发翻页）
  - 测试速度判断（1000 px/s 触发翻页）
  - 测试多点触控取消机制
  - 测试双向拖动支持
  - _Requirements: 所有需求的正确性验证_

- [ ] 15. 编写集成测试
  - 测试完整的翻页流程（开始 → 拖动 → 完成）
  - 测试回弹动画（拖动不足时返回原位）
  - 测试边界情况（第一页向右滑动、最后一页向左滑动）
  - 测试快速滑动触发翻页
  - 测试中途取消翻页
  - 测试性能（维持 60fps）
  - _Requirements: 所有需求的端到端验证_

- [ ] 16. 性能测试和优化
  - 使用 Profiler 测量拖动过程中的帧率
  - 验证平均帧时间 < 16.67ms（60fps）
  - 测量触摸到视觉反馈的延迟 < 50ms
  - 优化位图加载和缓存策略
  - 确保内存使用在合理范围内
  - _Requirements: 7.1, 7.2, 7.3, 11.1, 11.2, 11.3, 11.4, 11.5_
