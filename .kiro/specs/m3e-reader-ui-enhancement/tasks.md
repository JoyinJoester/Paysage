# Implementation Plan - M3E Reader UI Enhancement

## Phase 1: 基础架构搭建

- [x] 1. 创建 ReaderScreen 基础结构


  - 创建 `ReaderScreen.kt` 文件
  - 定义 `ReaderUiState` 数据类
  - 实现基础的 Scaffold 布局
  - 集成 ReaderViewModel
  - _Requirements: 1.1, 1.2, 1.3_

- [x] 2. 实现触摸区域数据模型


  - 创建 `TouchZone.kt` 文件
  - 定义 `TouchZone` 枚举（九个区域）
  - 定义 `TouchAction` 枚举（动作类型）
  - 实现 `TouchZoneConfig` 数据类
  - _Requirements: 6.1-6.12_

- [x] 3. 实现 TouchZoneDetector 组件


  - 创建 `TouchZoneDetector.kt` 文件
  - 实现触摸点到区域的映射算法
  - 实现 `detectTapGestures` 手势检测
  - 实现触摸事件分发逻辑
  - _Requirements: 6.1-6.9_

- [x] 4. 实现九宫格可视化覆盖层



  - 创建 `TouchZoneOverlay.kt` 文件
  - 使用 Canvas 绘制九宫格线条
  - 在每个区域显示动作标签
  - 实现半透明背景效果
  - _Requirements: 6.12_

## Phase 2: 翻页动画系统实现（核心）

- [x] 5. 创建翻页动画基础架构


  - 创建 `PageFlipAnimator.kt` 接口文件
  - 定义 `PageFlipMode` 枚举
  - 定义 `FlipDirection` 枚举
  - 创建 `PageFlipAnimationManager` 类
  - _Requirements: 5.1-5.10, 17.1_

- [x] 6. 实现滑动翻页动画（最简单）


  - 创建 `SlidePageFlip.kt` 文件
  - 实现 `PageFlipAnimator` 接口
  - 实现 `drawFlipFrame` 方法（平移动画）
  - 测试滑动效果的流畅性
  - _Requirements: 5.3, 17.4_

- [x] 7. 实现覆盖翻页动画


  - 创建 `CoverPageFlip.kt` 文件
  - 实现页面从右侧覆盖的效果
  - 添加阴影增强层次感
  - 测试覆盖效果
  - _Requirements: 5.2, 17.3_

- [x] 8. 实现仿真翻页动画（最复杂，重点学习 Legado）

- [x] 8.1 研究 Legado 的 SimulationPageDelegate


  - 分析 Legado 源码中的贝塞尔曲线计算
  - 理解触摸点和角点的关系
  - 学习卷曲路径的计算方法
  - _Requirements: 17.2_

- [x] 8.2 实现基础仿真翻页



  - 创建 `SimulationPageFlip.kt` 文件
  - 实现 `calculateCurlPath` 方法
  - 实现基础的页面卷曲效果
  - _Requirements: 5.1, 17.2_

- [x] 8.3 实现卷曲页面的背面和正面

  - 实现 `drawCurlBackSide` 方法
  - 实现 `drawCurlFrontSide` 方法
  - 应用变换矩阵实现透视效果
  - _Requirements: 5.1, 17.2_

- [x] 8.4 实现阴影和光照效果

  - 实现 `drawCurlShadow` 方法（径向渐变）
  - 实现 `drawCurlHighlight` 方法
  - 调整阴影和光照参数
  - _Requirements: 5.1, 17.9_

- [x] 9. 实现滚动翻页动画


  - 创建 `ScrollPageFlip.kt` 文件
  - 实现垂直连续滚动效果
  - 支持平滑滚动
  - _Requirements: 5.4, 17.5_

- [x] 10. 实现无动画模式


  - 创建 `NonePageFlip.kt` 文件
  - 实现即时切换页面
  - _Requirements: 5.5_

- [x] 11. 实现手势跟随和拖动翻页


  - 在 `PageFlipAnimator` 中添加 `updateGesture` 方法
  - 实现触摸点实时跟随
  - 实现拖动释放后的自动完成动画
  - 添加边界检测和回弹效果
  - _Requirements: 5.6, 5.9, 17.6, 17.10_

## Phase 3: M3E 风格 UI 组件

- [x] 12. 实现 ReaderTopBar


  - 创建 `ReaderTopBar.kt` 文件
  - 使用 M3 TopAppBar 组件
  - 显示书籍标题和章节信息
  - 使用 ExpressiveIconButton 作为返回按钮
  - 实现半透明背景效果
  - _Requirements: 7.1-7.5_

- [x] 13. 实现 ReaderBottomBar


  - 创建 `ReaderBottomBar.kt` 文件
  - 添加翻页按钮（使用 ExpressiveIconButton）
  - 添加进度滑块
  - 显示页码信息
  - 实现半透明背景效果
  - _Requirements: 8.1-8.5_

- [x] 14. 实现工具栏显示/隐藏动画



  - 创建 `AnimatedToolbar.kt` 文件
  - 使用 AnimatedVisibility 实现滑入/滑出动画
  - 应用 M3E Emphasized Easing 曲线
  - 实现自动隐藏计时器
  - _Requirements: 2.2, 2.4, 5.1.1_

- [x] 15. 集成 QuickSettingsPanel


  - 复用现有的 `QuickSettingsPanel.kt` 组件
  - 添加翻页模式快速切换
  - 实现面板展开/收起动画
  - _Requirements: 3.1-3.5_

- [x] 16. 集成 ReadingSettingsDialog



  - 复用现有的 `ReadingSettingsDialog.kt` 组件
  - 添加翻页模式选择标签页
  - 添加触摸区域配置选项
  - _Requirements: 4.1-4.5_

- [x] 17. 实现快速设置 FAB


  - 创建 `ReaderQuickSettingsFAB.kt` 文件
  - 使用 ExpressiveFAB 组件
  - 实现 FAB 显示/隐藏动画
  - _Requirements: 9.1-9.5_

## Phase 4: 阅读内容渲染和页面管理

- [x] 18. 实现 ReaderContent 组件




  - 创建 `ReaderContent.kt` 文件
  - 集成 TouchZoneDetector
  - 使用 Canvas 绘制页面内容
  - 集成翻页动画系统
  - _Requirements: 1.1-1.5_


- [x] 19. 实现页面数据模型

  - 创建 `PageData.kt` 文件
  - 定义 `PageData` 数据类
  - 定义 `PageCache` 数据类（三页缓存）
  - _Requirements: 17.8_



- [x] 20. 实现页面预渲染系统

  - 创建 `PagePreRenderer.kt` 文件
  - 实现后台异步渲染
  - 实现页面缓存管理
  - 限制缓存大小
  - _Requirements: 14.3, 17.8_




- [x] 21. 实现页面渲染引擎


  - 创建 `PageRenderer.kt` 文件
  - 实现文本到 Bitmap 的渲染
  - 应用 ReaderConfig 配置
  - 支持自定义字体、颜色、间距等
  - _Requirements: 4.3, 11.1-11.5_

- [x] 21.5 集成 ReaderContent 到 ReaderScreen
  - 替换 PageImageView 为 ReaderContent
  - 连接触摸区域检测系统
  - 集成翻页模式配置
  - 添加 getNextPageBitmap 和 getPreviousPageBitmap 方法
  - _Requirements: 1.1-1.5, 5.1-5.10_

## Phase 5: 状态管理和数据持久化

- [x] 22. 实现 ReaderViewModel

  - 创建 `ReaderViewModel.kt` 文件
  - 管理 ReaderUiState
  - 处理翻页事件
  - 处理触摸区域事件
  - 处理配置变更
  - _Requirements: 1.1-1.5_


- [x] 23. 实现阅读进度保存


  - 在 ViewModel 中实现自动保存逻辑
  - 保存当前页码和章节
  - 实现进度恢复
  - _Requirements: 15.1-15.5_


- [x] 24. 实现触摸区域配置持久化


  - 保存用户自定义的触摸区域配置
  - 实现配置导入/导出
  - _Requirements: 6.10, 15.3_


- [x] 25. 实现翻页模式配置持久化



  - 保存用户选择的翻页模式
  - 实现全局和书籍特定配置
  - _Requirements: 15.3, 15.5_

## Phase 6: 性能优化

- [x] 26. 实现 Bitmap 内存池


  - 创建 `BitmapPool.kt` 文件
  - 实现 Bitmap 复用逻辑
  - 实现内存池大小限制
  - _Requirements: 14.4_

- [x] 27. 实现翻页动画性能监控


  - 创建 `PageFlipPerformanceMonitor.kt` 文件
  - 记录帧时间
  - 计算平均帧率
  - 实现性能警告
  - _Requirements: 14.1, 14.2, 14.5_

- [x] 28. 实现动画降级策略


  - 检测性能不足情况
  - 自动降级到更简单的动画
  - 提供用户手动选择选项
  - _Requirements: 14.1, 14.2_

- [x] 29. 优化 Canvas 绘制性能


  - 使用 saveLayer 减少重绘
  - 避免过度绘制
  - 启用硬件加速
  - _Requirements: 14.1, 17.7_

## Phase 7: 主题和样式

- [x] 30. 实现阅读主题系统


  - 创建预设主题（默认、护眼、夜间）
  - 实现主题切换动画
  - 应用主题到工具栏和控件
  - _Requirements: 11.1-11.5_

- [x] 31. 实现 M3E 动画规范



  - 创建 `ReaderAnimations.kt` 文件
  - 定义 Emphasized Easing 曲线
  - 定义标准动画时长
  - _Requirements: 18.1-18.5, 19.1-19.7_

- [ ] 32. 应用 M3E 组件样式
  - 确保所有按钮使用 ExpressiveButton
  - 确保所有卡片使用 ExpressiveCard
  - 确保所有 FAB 使用 ExpressiveFAB
  - _Requirements: 20.1-20.5_

## Phase 8: 响应式布局和无障碍

- [ ] 33. 实现响应式布局
  - 适配横屏和竖屏
  - 适配平板和折叠屏
  - 保持阅读位置
  - _Requirements: 12.1-12.5_

- [ ] 34. 实现无障碍支持
  - 添加内容描述
  - 支持 TalkBack
  - 支持大字体
  - 支持高对比度
  - 支持键盘导航
  - _Requirements: 13.1-13.5_

## Phase 9: 测试

- [ ] 35. 编写触摸区域测试
  - 测试九宫格区域检测准确性
  - 测试触摸动作分发
  - 测试配置持久化
  - _Requirements: 21.3_

- [ ] 36. 编写翻页动画测试
  - 测试各种翻页模式的正确性
  - 测试动画流畅性（60fps）
  - 测试手势跟随
  - _Requirements: 21.2, 21.6_

- [ ] 37. 编写 UI 组件测试
  - 测试工具栏显示/隐藏
  - 测试快速设置面板
  - 测试阅读设置对话框
  - _Requirements: 21.1_

- [ ] 38. 编写性能测试
  - 测试页面渲染性能
  - 测试翻页动画帧率
  - 测试内存使用
  - _Requirements: 21.6_

- [ ] 39. 编写主题测试
  - 测试所有主题的显示效果
  - 测试主题切换动画
  - _Requirements: 21.4_

## Phase 10: Bug 修复和优化

- [ ] 40. 修复发现的 Bug
  - 修复触摸区域检测问题
  - 修复翻页动画卡顿
  - 修复内存泄漏
  - _Requirements: 16.1-16.5, 21.5_

- [ ] 41. 用户体验优化
  - 调整动画参数
  - 优化触摸响应
  - 改进错误提示
  - _Requirements: 16.1-16.5_

- [ ] 42. 文档和代码清理
  - 添加代码注释
  - 编写使用文档
  - 清理调试代码
  - _Requirements: 所有_
