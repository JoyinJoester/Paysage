# Implementation Plan - 沉浸式状态栏

- [ ] 1. 创建 SystemBarController 核心组件
  - 实现 `SystemBarController` 对象，提供配置透明系统栏的方法
  - 实现 `setupTransparentSystemBars()` 方法，设置状态栏和导航栏为透明，并配置图标颜色
  - 实现 `setupImmersiveMode()` 方法，支持隐藏/显示系统栏
  - 实现 `supportsTransparentNavigationBar()` 方法，检查设备兼容性
  - 处理不同 Android 版本的兼容性（Android 5.0+）
  - 处理刘海屏和挖孔屏的显示区域配置
  - _Requirements: 1.1, 1.2, 5.1, 5.2, 5.3_

- [ ] 2. 实现 WindowInsets 处理组件
  - 创建 `WindowInsetsHandler.kt` 文件
  - 实现 `Modifier.statusBarsPadding()` 扩展函数
  - 实现 `Modifier.navigationBarsPadding()` 扩展函数
  - 实现 `Modifier.systemBarsPadding()` 扩展函数
  - 实现 `Modifier.imePadding()` 扩展函数用于输入法
  - 实现 `rememberStatusBarHeight()` 组合函数
  - 实现 `rememberNavigationBarHeight()` 组合函数
  - _Requirements: 3.1, 3.2_

- [ ] 3. 集成系统栏配置到主题系统
  - 修改 `PaysageTheme` 组合函数，添加 `transparentSystemBars` 参数
  - 移除硬编码的 `window.statusBarColor = colorScheme.primary.toArgb()` 设置
  - 在 `SideEffect` 中调用 `SystemBarController.setupTransparentSystemBars()`
  - 根据 `darkTheme` 状态自动配置系统栏图标颜色（深色/浅色）
  - 确保主题切换时系统栏样式平滑过渡
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 6.1, 6.2, 6.3, 6.4_

- [ ] 4. 在 TwoLayerNavigationScaffold 中应用 insets
  - 在 Compact 模式的第一层导航栏添加 `.statusBarsPadding()`
  - 在 Medium 模式的 PermanentDrawerSheet 添加 `.statusBarsPadding()`
  - 在 Expanded 模式的两层抽屉都添加 `.statusBarsPadding()`
  - 确保第二层抽屉内容不被系统栏遮挡
  - 测试不同窗口尺寸下的显示效果
  - _Requirements: 3.3_

- [ ] 5. 在 LibraryScreen 中应用 insets
  - 检查 LibraryScreen 的 Scaffold 实现
  - 确保 TopAppBar 正确处理状态栏 insets
  - 确保内容区域使用 Scaffold 提供的 paddingValues
  - 测试列表滚动时内容不被系统栏遮挡
  - _Requirements: 3.1, 3.2_

- [ ] 6. 在 ReaderScreen 中应用 insets
  - 在阅读器控制栏添加 `.statusBarsPadding()`
  - 确保漫画内容可以全屏显示（不应用 padding）
  - 在底部控制栏添加 `.navigationBarsPadding()`
  - 测试横屏和竖屏模式下的显示效果
  - _Requirements: 3.1, 3.2, 4.1, 4.2_

- [ ] 7. 实现阅读器沉浸模式
  - 创建 `ImmersiveModeState` 数据类
  - 实现 `rememberImmersiveModeState()` 组合函数
  - 实现 `ImmersiveModeEffect` 组合函数，管理系统栏显示/隐藏
  - 在 ReaderScreen 中集成沉浸模式
  - 实现点击屏幕临时显示系统栏的逻辑
  - 添加沉浸模式开关到阅读器设置
  - _Requirements: 4.3, 4.4_

- [ ] 8. 在其他界面应用 insets
  - 在 SettingsScreen 中应用适当的 insets padding
  - 在 BookmarksScreen 中应用适当的 insets padding
  - 确保所有对话框和弹出层不被系统栏遮挡
  - 测试所有界面的显示效果
  - _Requirements: 3.4_

- [ ] 9. 添加沉浸模式设置选项
  - 在 `SettingsRepository` 中添加沉浸模式相关设置
  - 创建 `ImmersiveModeSettings` 数据类
  - 在设置界面添加沉浸模式开关
  - 添加"自动隐藏系统栏"选项
  - 添加"点击显示系统栏"选项
  - _Requirements: 4.3, 4.4_

- [ ] 10. 编写单元测试
  - 测试 SystemBarController 的配置逻辑
  - 测试不同 Android 版本的兼容性处理
  - 测试 Insets 计算的正确性
  - 测试沉浸模式状态管理
  - _Requirements: 5.1, 5.2_

- [ ] 11. 编写 UI 测试
  - 测试不同主题下系统栏图标颜色
  - 测试内容是否正确应用 padding
  - 测试沉浸模式的启用/禁用
  - 测试横屏/竖屏切换
  - _Requirements: 2.1, 2.2, 3.1, 3.2, 4.3_

- [ ] 12. 设备兼容性测试和优化
  - 在不同 Android 版本设备上测试（5.0 - 14）
  - 在刘海屏/挖孔屏设备上测试显示效果
  - 在折叠屏设备上测试（如果可能）
  - 优化性能，避免过度重组
  - 修复发现的兼容性问题
  - _Requirements: 5.1, 5.2, 5.3, 5.4_
