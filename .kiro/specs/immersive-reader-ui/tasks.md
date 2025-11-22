# 沉浸式阅读界面优化实现计划

- [x] 1. 创建触摸区域核心组件


  - 创建 TouchZone.kt 文件，包含 TouchZone 枚举和 TouchZoneConfig 数据类
  - 实现 isNextPage()、isPreviousPage() 和 isCenter() 方法，根据阅读方向判断区域功能
  - 实现 TouchZoneDetector 类，提供 detectZone() 和 getZoneBounds() 方法
  - _需求: 1.1, 1.2, 1.3, 1.4_



- [ ] 2. 扩展 ReaderViewModel 支持触摸区域
  - 在 ReaderUiState 中添加 touchZoneConfig 和 lastTappedZone 字段
  - 修改 isUiVisible 默认值为 false（默认隐藏工具栏）
  - 实现 handleTouchZone() 方法，处理触摸区域点击事件


  - 实现 updateTouchZoneConfig() 方法，更新触摸区域配置
  - _需求: 2.1, 2.2, 3.1, 3.2, 4.1, 4.2_

- [ ] 3. 修改 PageView 组件集成触摸区域检测
  - 添加 touchZoneConfig 和 onTouchZone 参数到 PageView
  - 创建 TouchZoneDetector 实例并使用 remember 缓存
  - 在 pointerInput 中添加触摸区域检测逻辑
  - 实现触觉反馈（使用 LocalHapticFeedback）
  - 确保只在未缩放（scale <= 1f）时处理触摸区域
  - 保持现有的双击缩放和滑动手势功能
  - _需求: 1.5, 4.3, 4.4, 4.5, 4.6, 4.7, 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 4. 修改 DualPageView 组件支持触摸区域


  - 添加 touchZoneConfig 和 onTouchZone 参数到 DualPageView
  - 集成 TouchZoneDetector 进行触摸检测
  - 实现双页模式下的翻页逻辑（一次翻两页）
  - 确保触摸区域在双页模式下正确映射
  - _需求: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 5. 更新 ReaderScreen 使用触摸区域功能


  - 修改 TopAppBar 和 BottomAppBar 使用 AnimatedVisibility
  - 实现 fadeIn/fadeOut 和 slideIn/slideOut 动画效果
  - 在打开书籍时默认隐藏工具栏
  - 根据工具栏可见性动态调整 padding
  - 传递 touchZoneConfig 和 onTouchZone 到 PageView/DualPageView
  - 调用 viewModel.handleTouchZone() 处理触摸事件
  - _需求: 2.3, 2.4, 2.5, 3.3, 3.4, 3.5_

- [x] 6. 创建触摸区域调试覆盖层


  - 创建 TouchZoneDebugOverlay.kt 文件
  - 使用 Canvas 绘制触摸区域边界
  - 高亮显示最近点击的区域
  - 显示区域功能标签（"上一页"、"下一页"、"显示/隐藏"）
  - 在 PageView 中集成调试覆盖层（仅在 debugVisualization 启用时）
  - _需求: 6.1, 6.2, 6.3, 6.4, 6.5_

- [x] 7. 扩展 AppSettings 添加触摸区域配置


  - 在 AppSettings 数据类中添加 touchZoneEnabled、touchZoneHapticFeedback 和 touchZoneDebugMode 字段
  - 在 SettingsViewModel 中添加更新这些设置的方法
  - 在 SettingsRepository 中实现持久化逻辑
  - _需求: 6.4, 7.1_

- [x] 8. 在设置界面添加触摸区域选项


  - 在 AppearanceSettingsScreen 或阅读设置界面添加触摸区域配置选项
  - 添加"启用触摸区域"开关
  - 添加"触觉反馈"开关
  - 添加"调试模式"开关（仅在开发版本显示）
  - 使用 ExpressiveComponents 保持 UI 一致性
  - _需求: 6.4, 7.1_

- [x] 9. 编写单元测试


  - 创建 TouchZoneDetectorTest.kt，测试区域检测准确性
  - 测试不同屏幕尺寸下的区域划分
  - 测试边界条件和边缘情况
  - 创建 TouchZoneTest.kt，测试 isNextPage/isPreviousPage 逻辑
  - 测试不同阅读方向下的区域映射
  - _需求: 所有需求_

- [x] 10. 编写 UI 测试


  - 创建 TouchZoneUITest.kt，测试触摸区域交互
  - 测试点击中间区域切换工具栏
  - 测试点击周边区域翻页
  - 测试工具栏动画效果
  - 测试手势优先级（滑动优先于点击）
  - 测试缩放时禁用触摸区域
  - _需求: 所有需求_

- [x] 11. 编写集成测试



  - 创建 ImmersiveReaderIntegrationTest.kt
  - 测试完整阅读流程（打开书籍 → 隐藏工具栏 → 点击切换 → 翻页）
  - 测试配置变更后触摸区域仍然正常工作
  - 测试不同阅读方向和模式的组合
  - 测试与现有手势的兼容性
  - _需求: 所有需求_
