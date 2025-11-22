# 实现任务列表 - 两层导航抽屉

## 任务概述

本任务列表将指导实现 Paysage 应用的两层导航抽屉系统。任务按照依赖关系排序，每个任务都是可独立执行的代码实现步骤。

---

## 任务列表

- [x] 1. 创建导航状态管理和数据模型


  - 创建 `NavigationState.kt` 文件，定义导航状态数据类
  - 实现 `PrimaryNavItem` 枚举，包含书库、设置、关于三个主菜单项
  - 实现 `SecondaryNavItem` 数据类，定义第二层菜单项结构
  - 创建导航配置对象：`LibraryNavItems`、`SettingsNavItems`、`AboutNavItems`
  - 实现状态保存和恢复函数：`saveNavigationState` 和 `restoreNavigationState`
  - _需求: 1.2, 2.1, 3.1, 6.1_

- [x] 2. 实现增强的导航栏组件

  - [x] 2.1 创建 ExpressiveNavigationRailItem 组件


    - 在 `ExpressiveComponents.kt` 中添加 `ExpressiveNavigationRailItem` 可组合函数
    - 实现按压缩放动画（0.92 倍缩放）
    - 实现选中/未选中状态的颜色过渡动画
    - 添加弹簧回弹效果使用 `ExpressiveAnimations.bouncySpring`
    - 确保最小触摸目标尺寸为 48dp
    - _需求: 1.4, 3.5, 5.5, 6.3_

  - [x] 2.2 创建 PrimaryNavigationRail 组件


    - 创建 `navigation/PrimaryNavigationRail.kt` 文件
    - 实现宽度为 80dp 的垂直导航栏
    - 使用 `surfaceContainer` 作为背景色
    - 遍历 `PrimaryNavItem` 枚举渲染导航项
    - 集成 `ExpressiveNavigationRailItem` 组件
    - 添加顶部间距和布局优化
    - _需求: 1.1, 1.2, 1.3, 5.1_

- [x] 3. 实现第二层抽屉内容组件

  - [x] 3.1 创建 SecondaryDrawerContent 主容器


    - 创建 `navigation/SecondaryDrawerContent.kt` 文件
    - 实现宽度为 280dp 的垂直布局
    - 使用 `surface` 颜色作为背景
    - 添加标题显示当前选中的第一层菜单名称
    - 实现根据 `selectedPrimaryItem` 切换内容的逻辑
    - 添加 `AnimatedContent` 实现内容切换动画
    - _需求: 2.1, 2.5, 5.2_


  - [x] 3.2 实现书库菜单内容 (LibraryDrawerContent)

    - 在 `SecondaryDrawerContent.kt` 中创建 `LibraryDrawerContent` 函数
    - 渲染书库菜单项列表：全部书籍、收藏、最近阅读、分类
    - 使用 `NavigationDrawerItem` 组件
    - 添加图标和文本标签
    - 实现点击事件处理
    - _需求: 2.2_

  - [x] 3.3 实现设置菜单内容 (SettingsDrawerContent)

    - 在 `SecondaryDrawerContent.kt` 中创建 `SettingsDrawerContent` 函数
    - 渲染设置菜单项列表：主题设置、阅读设置、缓存管理、关于应用
    - 使用 `NavigationDrawerItem` 组件
    - 添加图标和文本标签
    - 实现点击事件处理
    - _需求: 2.3_


  - [x] 3.4 实现关于菜单内容 (AboutDrawerContent)

    - 在 `SecondaryDrawerContent.kt` 中创建 `AboutDrawerContent` 函数
    - 渲染关于菜单项列表：版本信息、开源许可、GitHub
    - 使用 `NavigationDrawerItem` 组件
    - 添加图标和文本标签
    - 实现点击事件处理（action 回调）
    - _需求: 2.4_

- [x] 4. 实现两层导航脚手架主容器

  - [x] 4.1 创建基础 TwoLayerNavigationScaffold 组件


    - 创建 `navigation/TwoLayerNavigationScaffold.kt` 文件
    - 定义组件参数：navigationState、回调函数、content
    - 使用 `rememberDrawerState` 管理抽屉状态
    - 实现 `ModalNavigationDrawer` 作为第二层容器
    - 集成 `PrimaryNavigationRail` 和 `SecondaryDrawerContent`
    - 实现内容区域布局（Row 布局：第一层 + 第二层 + 内容）
    - _需求: 1.1, 2.1, 3.1_

  - [x] 4.2 实现抽屉状态同步和交互逻辑

    - 添加 `LaunchedEffect` 监听 `navigationState.isSecondaryDrawerOpen` 变化
    - 实现抽屉状态与 `DrawerState` 的双向同步
    - 实现点击第一层图标时展开第二层抽屉
    - 实现点击第二层选项后关闭抽屉
    - 实现点击遮罩层关闭抽屉
    - _需求: 1.3, 3.1, 3.2, 3.4_


  - [x] 4.3 添加手势支持

    - 实现从左边缘向右滑动展开抽屉
    - 配置 `ModalNavigationDrawer` 的手势启用参数
    - 确保手势与抽屉状态正确同步
    - _需求: 3.3_

- [x] 5. 集成到 MainActivity 和应用导航


  - [x] 5.1 创建 NavigationViewModel


    - 创建 `viewmodel/NavigationViewModel.kt` 文件
    - 定义 `navigationState` 作为 `StateFlow`
    - 实现 `selectPrimaryItem` 方法
    - 实现 `toggleSecondaryDrawer` 方法
    - 实现 `selectSecondaryItem` 方法
    - 添加状态保存逻辑
    - _需求: 1.3, 3.1_


  - [x] 5.2 更新 MainActivity 集成导航系统

    - 在 `MainActivity.kt` 中初始化 `NavigationViewModel`
    - 在 `PaysageApp` 中使用 `TwoLayerNavigationScaffold` 包裹 `NavHost`
    - 传递导航状态和回调函数
    - 实现第二层菜单项点击后的导航逻辑
    - 移除或更新现有的顶部应用栏（如果有）
    - _需求: 1.1, 2.2, 2.3, 2.4, 3.1_


  - [x] 5.3 更新 Screen 路由支持查询参数

    - 在 `navigation/Screen.kt` 中为 Library 和 Settings 添加查询参数支持
    - 实现 `Library.createRoute(filter: String?)` 方法
    - 实现 `Settings.createRoute(section: String?)` 方法
    - 更新 NavHost 中的路由定义支持可选参数
    - _需求: 2.2, 2.3_

- [x] 6. 实现响应式布局适配

  - [x] 6.1 创建窗口尺寸检测工具


    - 创建 `util/WindowSizeClass.kt` 文件
    - 定义 `WindowSizeClass` 枚举：Compact、Medium、Expanded
    - 实现 `rememberWindowSizeClass` 可组合函数
    - 使用 `LocalConfiguration` 获取屏幕宽度
    - _需求: 4.1, 4.2, 4.3_

  - [x] 6.2 实现 Compact 布局（手机）


    - 在 `TwoLayerNavigationScaffold.kt` 中创建 `CompactNavigationLayout` 函数
    - 隐藏第一层导航栏
    - 在顶部应用栏添加菜单按钮
    - 点击菜单按钮展开第二层抽屉
    - 第二层抽屉全屏显示
    - _需求: 4.1_


  - [x] 6.3 实现 Medium 布局（小平板）

    - 在 `TwoLayerNavigationScaffold.kt` 中创建 `MediumNavigationLayout` 函数
    - 显示第一层导航栏（80dp）
    - 第二层抽屉模态显示（280dp）
    - 实现标准的两层导航交互
    - _需求: 4.2_



  - [x] 6.4 实现 Expanded 布局（大平板/桌面）

    - 在 `TwoLayerNavigationScaffold.kt` 中创建 `ExpandedNavigationLayout` 函数
    - 第一层和第二层都固定显示
    - 第二层宽度调整为 320dp
    - 移除遮罩层
    - 实现永久导航抽屉（PermanentNavigationDrawer）
    - _需求: 4.3, 4.5_



  - [x] 6.5 集成响应式布局到主容器

    - 在 `TwoLayerNavigationScaffold` 中使用 `rememberWindowSizeClass`
    - 根据 `WindowSizeClass` 选择对应的布局组件
    - 确保状态在布局切换时保持
    - 处理屏幕旋转时的状态保存
    - _需求: 4.4_

- [x] 7. 实现动画和视觉效果

  - [x] 7.1 优化第二层抽屉展开/收起动画


    - 在 `TwoLayerNavigationScaffold.kt` 中配置 `DrawerState` 动画
    - 使用 `ExpressiveAnimations.EmphasizedEasing` 缓动曲线
    - 设置动画时长为 300ms
    - 添加遮罩层透明度动画（0 到 0.32）
    - _需求: 2.5, 3.5_

  - [x] 7.2 实现菜单内容切换动画

    - 在 `SecondaryDrawerContent` 中使用 `AnimatedContent`
    - 配置淡入淡出过渡效果
    - 使用 `EmphasizedDecelerateEasing` 和 `EmphasizedAccelerateEasing`
    - 设置过渡时长为 200ms
    - _需求: 2.5_

  - [x] 7.3 添加导航项选中状态动画

    - 在 `ExpressiveNavigationRailItem` 中优化颜色过渡
    - 添加指示器形状动画
    - 实现图标大小微调动画（选中时略微放大）
    - 确保动画流畅且符合 Material 3 规范
    - _需求: 5.3, 5.4, 5.5_

- [x] 8. 实现可访问性功能

  - [x] 8.1 添加语义标签和内容描述

    - 为所有导航项添加 `contentDescription`
    - 使用 `Modifier.semantics` 添加角色和状态描述
    - 为第二层菜单项添加语义标签
    - 确保抽屉状态变化可被屏幕阅读器识别
    - _需求: 6.1, 6.2, 6.4_


  - [x] 8.2 确保触摸目标尺寸

    - 为所有导航项添加 `minimumInteractiveComponentSize` 修饰符
    - 验证第一层导航项高度至少 56dp
    - 验证第二层菜单项高度至少 48dp
    - 添加适当的内边距确保触摸区域
    - _需求: 6.3_

  - [x] 8.3 实现高对比度模式支持


    - 检测系统高对比度设置
    - 在高对比度模式下调整颜色方案
    - 增加边框和分隔线的可见性
    - 确保选中状态在高对比度下清晰可见
    - _需求: 6.5_

- [x] 9. 错误处理和状态管理


  - [x] 9.1 实现导航错误处理

    - 创建 `NavigationError` 密封类
    - 实现 `handleNavigationError` 函数
    - 在导航失败时显示错误提示
    - 添加日志记录
    - 实现回退到默认页面的逻辑
    - _需求: 1.3, 3.1_

  - [x] 9.2 实现状态持久化

    - 在 `NavigationViewModel` 中使用 `SavedStateHandle`
    - 实现状态序列化和反序列化
    - 在配置变更时保存导航状态
    - 在应用重启时恢复导航状态
    - _需求: 4.4_

- [x] 10. 性能优化

  - [x] 10.1 优化第二层内容渲染

    - 使用 `key()` 为不同菜单内容添加唯一键
    - 实现懒加载，只渲染当前选中的菜单
    - 避免不必要的重组
    - 使用 `remember` 缓存静态数据
    - _需求: 2.1, 2.2, 2.3, 2.4_

  - [x] 10.2 优化动画性能

    - 使用 `remember` 缓存动画规格
    - 避免在动画中创建新对象
    - 使用 `derivedStateOf` 优化派生状态
    - 确保动画在低端设备上流畅运行
    - _需求: 3.5, 5.5_

  - [x] 10.3 优化状态管理

    - 使用 `rememberSaveable` 保存 UI 状态
    - 实现状态提升避免重复状态
    - 使用 `collectAsStateWithLifecycle` 收集 Flow
    - 优化 ViewModel 中的状态更新逻辑
    - _需求: 1.3, 3.1_

- [x] 11. 编写测试


  - [x] 11.1 编写导航状态单元测试


    - 测试 `NavigationState` 初始状态
    - 测试状态更新逻辑
    - 测试状态保存和恢复
    - 测试边界情况和错误处理
    - _需求: 1.2, 1.3_

  - [x] 11.2 编写 UI 组件测试


    - 测试 `PrimaryNavigationRail` 渲染和交互
    - 测试 `SecondaryDrawerContent` 内容切换
    - 测试 `ExpressiveNavigationRailItem` 动画效果
    - 测试响应式布局切换
    - _需求: 1.1, 2.1, 4.1, 4.2, 4.3_

  - [x] 11.3 编写集成测试

    - 测试完整的导航流程
    - 测试从第一层到第二层再到页面的导航
    - 测试抽屉展开和关闭
    - 测试手势交互
    - 测试状态在导航过程中的保持
    - _需求: 3.1, 3.2, 3.3_

  - [x] 11.4 编写可访问性测试

    - 测试语义标签是否正确
    - 测试触摸目标尺寸
    - 测试屏幕阅读器兼容性
    - 测试高对比度模式
    - _需求: 6.1, 6.2, 6.3, 6.4, 6.5_

---

## 实现说明

### 依赖关系
- 任务 2 依赖任务 1（需要导航状态模型）
- 任务 3 依赖任务 1（需要导航配置）
- 任务 4 依赖任务 2 和 3（需要第一层和第二层组件）
- 任务 5 依赖任务 4（需要完整的导航容器）
- 任务 6 可以与任务 4 并行开始
- 任务 7-10 依赖任务 5（需要基础功能完成）
- 任务 11 可以在任何时候开始编写

### 测试策略
- 所有测试任务都是必需的，确保代码质量和可靠性
- 建议在实现每个主要功能后立即编写对应的测试
- 测试覆盖单元测试、UI 测试、集成测试和可访问性测试

### 验证方法
每个任务完成后应该：
1. 编译通过，无语法错误
2. 运行应用，验证功能正常
3. 检查动画效果是否流畅
4. 测试不同屏幕尺寸的表现
5. 验证可访问性功能

---

**版本**: 1.0  
**创建日期**: 2025-10-27
