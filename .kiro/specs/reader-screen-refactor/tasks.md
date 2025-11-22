# Implementation Plan

- [x] 1. 删除旧的翻页动画和相关组件





  - 删除 EnhancedReaderScreen.kt 文件
  - 删除 PageFlipSettings.kt 文件
  - 删除 reader/pageflip/ 目录下所有文件
  - 删除 reader/transition/ 目录下所有文件
  - 删除 TouchZone 相关文件（TouchZone.kt, TouchZoneDetector.kt, TouchZoneDebugOverlay.kt）
  - _Requirements: 1.2, 1.3, 1.4, 8.1, 8.2, 8.3, 8.4_

- [x] 2. 清理 AppSettings 数据模型





  - 从 AppSettings.kt 删除 touchZoneEnabled 字段
  - 从 AppSettings.kt 删除 touchZoneHapticFeedback 字段
  - 从 AppSettings.kt 删除 touchZoneDebugMode 字段
  - 从 AppSettings.kt 删除 pageTransitionMode 字段
  - 从 AppSettings.kt 删除 animationSpeed 字段
  - 从 AppSettings.kt 删除 edgeSensitivity 字段
  - 从 AppSettings.kt 删除 enableTransitionEffects 字段
  - 从 AppSettings.kt 删除 enableTransitionHaptic 字段
  - 从 AppSettings.kt 删除 pageFlipMode 字段
  - 从 AppSettings.kt 删除 pageFlipAnimationSpeed 字段
  - _Requirements: 2.1, 2.2_

- [x] 3. 更新 SettingsViewModel





  - 删除所有翻页动画相关的方法（updatePageFlipMode, updatePageFlipAnimationSpeed 等）
  - 删除 TouchZone 相关的方法
  - 删除页面过渡动画相关的方法
  - _Requirements: 2.3_

- [x] 4. 创建简化的 ReaderUiState 数据类





  - 在 ReaderViewModel.kt 中定义 ReaderUiState 数据类
  - 包含字段：currentPage, totalPages, isLoading, isToolbarVisible, error, bookTitle, scale, offset
  - _Requirements: 3.1, 5.1, 5.4_

- [x] 5. 简化 ReaderViewModel





  - 删除所有翻页动画相关的状态和方法
  - 删除 TouchZone 相关的配置和方法
  - 保留核心方法：openBook, goToPage, nextPage, previousPage, toggleToolbar, cleanup
  - 实现 _uiState StateFlow
  - 实现 _currentPageBitmap StateFlow
  - _Requirements: 4.4, 7.3, 7.4_

- [x] 6. 创建 PageImageView 组件


  - 创建新的 Composable 函数 PageImageView
  - 实现图片显示功能
  - 实现双击缩放功能（1.0x ↔ 2.0x）
  - 实现双指捏合缩放功能（0.5x - 3.0x）
  - 实现拖动平移功能（仅在缩放时启用）
  - 实现点击检测（切换工具栏）
  - 实现左右滑动检测（翻页）
  - _Requirements: 3.1, 4.1, 4.2, 6.1, 6.2, 6.3, 6.4_

- [x] 7. 重写 ReaderScreen 主组件


  - 删除 ReaderScreen.kt 中的所有现有代码
  - 创建新的 ReaderScreen Composable 函数
  - 实现 Scaffold 布局结构
  - 实现状态管理（使用 ReaderViewModel）
  - 集成 PageImageView 组件
  - _Requirements: 1.1, 3.1, 3.2_

- [x] 8. 实现顶部工具栏

  - 创建 TopAppBar 组件
  - 添加返回按钮（调用 onBackClick）
  - 显示书籍标题
  - 显示当前页码和总页数（格式：当前页 / 总页数）
  - 实现淡入淡出动画（AnimatedVisibility）
  - _Requirements: 3.3, 5.1, 5.4, 7.1, 7.2_


- [x] 9. 实现底部工具栏
  - 创建 BottomAppBar 组件
  - 添加上一页按钮
  - 添加下一页按钮
  - 添加进度滑块（Slider）
  - 实现滑块拖动跳转功能
  - 实现淡入淡出动画（AnimatedVisibility）
  - _Requirements: 3.3, 3.4, 5.2, 5.3_

- [x] 10. 实现翻页逻辑
  - 在 ReaderViewModel 中实现 nextPage 方法
  - 在 ReaderViewModel 中实现 previousPage 方法
  - 在 ReaderViewModel 中实现 goToPage 方法
  - 实现边界检查（阻止超出第一页和最后一页）
  - 在 PageImageView 中连接滑动手势到翻页方法
  - 实现简单的淡入淡出页面切换动画
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [x] 11. 实现工具栏切换逻辑
  - 在 ReaderViewModel 中实现 toggleToolbar 方法
  - 在 PageImageView 中连接点击手势到 toggleToolbar
  - 确保工具栏状态正确更新 UI
  - _Requirements: 3.2, 3.3_

- [x] 12. 实现加载和错误状态

  - 创建加载指示器（CircularProgressIndicator）
  - 创建错误视图组件（ErrorView）
  - 在 ReaderScreen 中根据状态显示相应 UI
  - 实现错误重试功能
  - _Requirements: 3.1_

- [x] 13. 清理设置界面


  - 从设置界面删除翻页动画设置选项
  - 从设置界面删除 TouchZone 设置选项
  - 从设置界面删除页面过渡动画设置选项
  - _Requirements: 2.4_

- [x] 14. 更新导航和路由



  - 确保 MainActivity 或导航组件正确调用新的 ReaderScreen
  - 移除对 EnhancedReaderScreen 的引用
  - 移除对 PageFlipSettings 的引用
  - _Requirements: 1.2, 1.3_

- [ ] 15. 测试和验证
- [ ] 15.1 编写 ReaderViewModel 单元测试
  - 测试页面导航逻辑
  - 测试状态更新
  - 测试边界条件
  - _Requirements: 4.4_

- [ ] 15.2 编写 UI 集成测试
  - 测试工具栏显示/隐藏
  - 测试翻页功能
  - 测试进度滑块
  - _Requirements: 3.2, 4.1, 4.2, 5.3_

- [ ] 15.3 手动测试完整流程
  - 测试打开书籍
  - 测试各种手势操作
  - 测试缩放和拖动
  - 测试错误处理
  - _Requirements: 3.1, 6.1, 6.2, 6.3, 6.4_
