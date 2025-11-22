# Implementation Plan

- [x] 1. 修改 PageViewWithTransition 的手势处理





  - 在 `ReaderTransitionHelper.kt` 中修改 `PageViewWithTransition` 组件
  - 移除现有的 `detectHorizontalDragGestures` 和 `detectVerticalDragGestures`
  - 使用 `detectDragGestures` 实现统一的手势处理
  - 添加手势状态管理(起始位置、当前位置、是否已处理)
  - 在 `onDragEnd` 中根据总拖动距离判断翻页方向
  - 确保只在 scale <= 1f 时响应翻页手势
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4_

- [x] 2. 修改 PageView 的手势处理





  - 在 `ReaderScreen.kt` 中修改 `PageView` 组件
  - 应用与 `PageViewWithTransition` 相同的手势处理逻辑
  - 确保水平和垂直滑动都使用统一的 `detectDragGestures`
  - 根据阅读方向判断应该响应哪个方向的滑动
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 2.2, 2.3_

- [x] 3. 修改 DualPageView 的手势处理





  - 在 `ReaderScreen.kt` 中修改 `DualPageView` 组件
  - 应用相同的手势处理逻辑
  - 确保双页模式下一次滑动只翻两页(一组)
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2_

- [x] 4. 添加手势防抖和状态管理


  - 在手势处理中添加 `isGestureHandled` 标志
  - 确保在过渡动画进行时不响应新的手势
  - 在手势完成后正确重置状态
  - 处理边界情况(第一页/最后一页)
  - _Requirements: 1.3, 1.4, 2.2, 2.4, 3.4_

- [x] 5. 验证和测试修复



  - 测试短距离滑动不触发翻页
  - 测试中等距离滑动触发一次翻页
  - 测试长距离滑动只触发一次翻页
  - 测试快速滑动的响应
  - 测试不同阅读方向下的手势行为
  - 确保缩放功能不受影响
  - 确保触摸区域检测不受影响
  - _Requirements: 1.1, 1.2, 2.1, 2.2, 3.1, 3.2, 3.3_
