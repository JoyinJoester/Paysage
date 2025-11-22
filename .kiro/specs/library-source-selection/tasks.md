# Implementation Plan

- [x] 1. 添加字符串资源和数据模型


  - 在 `strings.xml` 和 `strings-zh.xml` 中添加源选择相关的字符串资源
  - 创建 `SourceType` 枚举定义不同类型的内容源
  - 创建 `LocalSourceConfig` 数据类存储本地源配置
  - _Requirements: 4.1, 4.2, 4.3_

- [x] 2. 实现数据持久化层


  - 创建 `SourceSelectionRepository` 类使用 DataStore 持久化配置
  - 实现保存和读取本地漫画路径的方法
  - 实现保存和读取本地阅读路径的方法
  - _Requirements: 2.5_

- [x] 3. 创建 SourceSelectionViewModel


  - 创建 `SourceSelectionViewModel` 类继承 `AndroidViewModel`
  - 实现本地漫画路径的状态管理（StateFlow）
  - 实现本地阅读路径的状态管理（StateFlow）
  - 实现路径更新和清除方法
  - 集成 `SourceSelectionRepository` 进行数据持久化
  - _Requirements: 2.5_

- [x] 4. 创建源选择 UI 组件

- [x] 4.1 创建 SourceSelectionOption 组件


  - 实现可复用的选项卡片组件
  - 添加图标、标题、副标题显示
  - 实现点击交互和涟漪效果
  - 添加可选的右侧箭头图标
  - 应用 Material 3 Expressive 卡片样式
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [x] 4.2 创建 SourceSelectionContent 组件


  - 实现主要的源选择页面布局
  - 添加页面标题和分隔线
  - 集成四个选项：本地漫画、本地阅读、漫画源、阅读源
  - 显示已选择的本地文件夹路径
  - 实现滚动支持
  - _Requirements: 1.1, 1.2, 2.1, 2.2, 3.1, 3.2, 5.1, 5.2, 5.3_

- [x] 5. 扩展 NavigationState


  - 在 `NavigationState` 数据类中添加 `showSourceSelection` 布尔字段
  - 更新 `saveNavigationState` 函数保存新字段
  - 更新 `restoreNavigationState` 函数恢复新字段
  - 更新 `NavigationStateSaver` 支持新字段
  - _Requirements: 1.1_

- [x] 6. 集成到 TwoLayerNavigationScaffold

- [x] 6.1 修改文件夹按钮行为


  - 将 `onFolderPickerClick` 参数改为 `onSourceSelectionClick`
  - 更新按钮点击事件以打开源选择页面而非直接打开文件夹选择器
  - 在三种布局（Compact、Medium、Expanded）中应用修改
  - _Requirements: 1.1, 1.4_

- [x] 6.2 在 SecondaryDrawer 中集成源选择页面


  - 在 `SecondaryDrawerContent` 中添加源选择页面的显示逻辑
  - 使用 `AnimatedContent` 实现页面切换动画
  - 根据 `showSourceSelection` 状态控制页面显示
  - 应用 fadeIn/fadeOut 动画效果
  - _Requirements: 1.1, 1.3, 5.4_

- [x] 7. 更新 MainActivity 集成

- [x] 7.1 添加文件夹选择器启动器


  - 创建本地漫画文件夹选择器的 ActivityResultLauncher
  - 创建本地阅读文件夹选择器的 ActivityResultLauncher
  - 实现文件夹选择结果的处理逻辑
  - 更新 ViewModel 中的路径状态
  - _Requirements: 2.3, 2.4, 2.5_

- [x] 7.2 实现导航逻辑

  - 添加源选择页面的状态管理
  - 实现从源选择页面到在线书源管理的导航
  - 传递正确的 category 参数（manga 或 novel）
  - 处理返回导航
  - _Requirements: 3.3, 3.4_

- [x] 8. 实现错误处理


  - 添加路径验证函数检查文件夹是否有效
  - 实现权限被拒绝的错误提示（Snackbar）
  - 实现无效路径的错误处理和清除逻辑
  - 添加错误日志记录
  - _Requirements: 2.3, 2.4_

- [x] 9. 添加无障碍支持

  - 为所有选项添加 contentDescription
  - 确保所有可点击元素的触摸目标至少 48dp
  - 添加语义标签（semantics modifier）
  - 测试 TalkBack 支持
  - _Requirements: 5.4_

- [x] 10. 编写测试


- [x] 10.1 编写 ViewModel 单元测试


  - 测试路径状态更新
  - 测试路径保存和读取
  - 测试路径清除功能
  - _Requirements: 2.5_

- [x] 10.2 编写 UI 测试


  - 测试源选择页面显示
  - 测试选项点击交互
  - 测试路径显示更新
  - 测试页面切换动画
  - _Requirements: 1.1, 1.2, 2.1, 2.2, 3.1, 3.2_

- [x] 10.3 编写集成测试


  - 测试从导航按钮到源选择页面的完整流程
  - 测试文件夹选择器集成
  - 测试在线书源页面导航
  - 测试数据持久化
  - _Requirements: 1.1, 2.3, 2.4, 3.3, 3.4_
