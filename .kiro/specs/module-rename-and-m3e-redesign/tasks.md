# 实现计划

## 任务列表

- [x] 1. 更新字符串资源


  - 修改`app/src/main/res/values-zh/strings.xml`文件
  - 将"本地功能"替换为"本地管理"
  - 将"在线功能"替换为"在线管理"
  - 添加文件夹创建相关的新字符串资源
  - _需求: 1.1, 1.2, 1.3, 1.4, 1.5_




- [ ] 2. 创建数据模型和数据库支持
- [ ] 2.1 创建Folder数据模型
  - 创建`app/src/main/java/takagi/ru/paysage/data/model/Folder.kt`
  - 定义Folder实体类，包含id、name、path、parentPath、moduleType、createdAt、updatedAt字段


  - 添加Room注解
  - _需求: 7.1, 7.2, 7.3_



- [ ] 2.2 创建FolderDao
  - 创建`app/src/main/java/takagi/ru/paysage/data/dao/FolderDao.kt`
  - 实现insert、getFoldersByPath、deleteByPath、getFolderById方法

  - _需求: 7.1, 7.2, 7.3_



- [ ] 2.3 更新数据库配置
  - 在`PaysageDatabase.kt`中添加Folder实体
  - 创建数据库迁移脚本

  - 添加folders表和相关索引
  - _需求: 7.1, 7.2, 7.3, 12.1, 12.2, 12.3, 12.4, 12.5_



- [ ] 3. 实现Repository层
- [x] 3.1 创建FolderRepository接口和实现

  - 创建`app/src/main/java/takagi/ru/paysage/repository/FolderRepository.kt`
  - 实现createFolder、getFolders、deleteFolder方法
  - 添加文件系统操作逻辑
  - 实现错误处理和异常定义


  - _需求: 4.1, 4.2, 5.1, 5.2, 5.3, 5.4, 5.5, 6.1, 6.2_

- [ ] 4. 实现ViewModel层
- [ ] 4.1 创建FolderViewModel
  - 创建`app/src/main/java/takagi/ru/paysage/viewmodel/FolderViewModel.kt`
  - 实现createFolder、refreshFolders、resetCreateFolderState方法
  - 定义CreateFolderState密封类


  - 管理文件夹列表状态和创建状态
  - _需求: 4.1, 4.2, 6.1, 6.2, 6.3, 6.4, 6.5, 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 5. 创建UI组件
- [x] 5.1 创建CreateFolderDialog组件


  - 创建`app/src/main/java/takagi/ru/paysage/ui/components/CreateFolderDialog.kt`
  - 实现文件夹名称输入对话框
  - 应用M3e设计规范（AlertDialog、OutlinedTextField、按钮样式）
  - 实现实时输入验证
  - 显示验证错误提示
  - 添加加载状态指示器
  - _需求: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 4.3, 5.1, 5.2, 5.3, 5.4, 5.5, 8.2, 8.3, 8.4, 9.3, 9.4, 9.5, 10.3_




- [ ] 5.2 创建CreateFolderButton组件
  - 在`SecondaryDrawerContent.kt`或新建组件文件中实现
  - 使用FilledTonalButton样式
  - 应用M3e间距和圆角规范
  - 添加图标和文本
  - _需求: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 4.1, 4.2, 8.1, 8.5_

- [ ] 5.3 创建FolderListItem组件
  - 创建`app/src/main/java/takagi/ru/paysage/ui/components/FolderListItem.kt`
  - 实现文件夹列表项UI
  - 应用M3e列表项样式
  - 显示文件夹图标、名称、创建时间
  - 添加点击交互
  - _需求: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 6. 更新导航抽屉内容
- [ ] 6.1 修改SecondaryDrawerContent.kt
  - 在LibraryDrawerContent中添加onCreateFolderClick参数
  - 在抽屉顶部添加CreateFolderButton
  - 添加HorizontalDivider分隔按钮和列表
  - 应用M3e间距规范
  - _需求: 3.1, 3.2, 3.3, 3.4, 3.5, 4.1, 4.2_

- [ ] 6.2 更新TwoLayerNavigationScaffold.kt
  - 添加文件夹创建相关的回调参数
  - 传递回调到SecondaryDrawerContent


  - 确保在所有布局模式（Compact、Medium、Expanded）中都支持
  - _需求: 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ] 7. 集成ViewModel到UI
- [x] 7.1 在MainActivity或相关Screen中集成FolderViewModel

  - 创建或获取FolderViewModel实例
  - 观察createFolderState和folders状态
  - 处理状态变化并更新UI
  - _需求: 4.1, 4.2, 6.1, 6.2, 6.3, 6.4, 6.5, 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 7.2 实现文件夹创建流程

  - 连接CreateFolderButton点击事件到对话框显示
  - 连接对话框确认事件到ViewModel.createFolder
  - 实现Snackbar反馈显示
  - 处理成功和失败状态
  - _需求: 4.1, 4.2, 4.3, 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ] 7.3 实现文件夹列表显示
  - 在导航抽屉中显示文件夹列表
  - 使用LazyColumn优化性能
  - 应用进入动画
  - 实现文件夹点击导航
  - _需求: 7.1, 7.2, 7.3, 7.4, 7.5, 11.1, 11.2, 11.3, 11.4, 11.5_

- [ ] 8. 实现输入验证逻辑
- [ ] 8.1 创建验证工具函数
  - 在CreateFolderDialog.kt或单独的工具文件中实现
  - 实现validateFolderName函数
  - 实现containsIllegalChars扩展函数
  - 定义FolderNameValidation密封类
  - _需求: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 8.2 集成验证到对话框
  - 在CreateFolderDialog中使用验证逻辑
  - 实时验证用户输入
  - 根据验证结果更新UI状态
  - 禁用/启用确定按钮
  - _需求: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 9. 应用M3e设计规范
- [ ] 9.1 确保间距和边距符合规范
  - 检查所有组件使用8dp基准网格
  - 应用标准间距（16dp、24dp等）
  - _需求: 2.1, 2.2_

- [ ] 9.2 确保字体和排版符合规范
  - 使用MaterialTheme.typography
  - 应用正确的文本样式（headlineSmall、bodyLarge等）
  - _需求: 2.3_

- [ ] 9.3 确保图标和按钮样式符合规范
  - 使用Material Icons
  - 应用正确的按钮变体（FilledTonalButton、TextButton）
  - _需求: 2.4, 2.5_

- [ ] 9.4 确保动画和过渡效果符合规范
  - 使用M3e标准动画时长（100ms、200ms、300ms）
  - 应用M3e标准缓动曲线（EmphasizedDecelerate、EmphasizedAccelerate）
  - _需求: 2.8, 2.9, 3.5_

- [ ] 9.5 确保配色方案符合规范
  - 使用MaterialTheme.colorScheme
  - 确保明亮和暗色主题都正确应用
  - _需求: 2.6, 2.7, 9.1, 9.2, 9.3, 9.4, 9.5_

- [ ] 10. 实现无障碍访问支持
- [ ] 10.1 添加内容描述
  - 为CreateFolderButton添加contentDescription
  - 为对话框元素添加语义标签
  - 为FolderListItem添加合并的contentDescription
  - _需求: 8.1, 8.2_

- [ ] 10.2 实现键盘导航
  - 在CreateFolderDialog中添加键盘事件处理
  - 支持Enter键提交
  - 支持Tab键导航
  - 自动聚焦到输入框
  - _需求: 8.3_

- [ ] 10.3 确保触摸目标尺寸
  - 验证所有交互元素最小尺寸为48dp × 48dp
  - _需求: 8.5_

- [ ] 10.4 确保颜色对比度
  - 验证所有文本与背景对比度至少为4.5:1
  - _需求: 8.6_

- [ ] 10.5 编写无障碍测试
  - 创建`app/src/androidTest/java/takagi/ru/paysage/ui/AccessibilityTest.kt`
  - 测试内容描述
  - 测试触摸目标尺寸
  - 测试键盘导航
  - _需求: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6_

- [ ] 11. 实现主题适配
- [ ] 11.1 确保动态配色支持
  - 验证Android 12+设备上的动态配色
  - 测试壁纸颜色提取
  - _需求: 10.1, 10.2, 10.3, 10.4, 10.5_

- [ ] 11.2 测试明亮和暗色主题
  - 在明亮主题下测试所有UI组件
  - 在暗色主题下测试所有UI组件
  - 验证主题切换的即时性
  - _需求: 9.1, 9.2, 9.3, 9.4, 9.5_

- [ ] 12. 性能优化
- [ ] 12.1 实现文件夹列表缓存
  - 在FolderRepository中添加LruCache
  - 实现缓存失效逻辑
  - _需求: 11.1, 11.2, 11.3, 11.4, 11.5_

- [ ] 12.2 优化LazyColumn渲染
  - 为列表项添加稳定的key
  - 使用animateItemPlacement
  - _需求: 11.1, 11.2, 11.3, 11.4, 11.5_

- [ ] 12.3 编写性能测试
  - 创建`app/src/androidTest/java/takagi/ru/paysage/PerformanceTest.kt`
  - 测试导航抽屉打开时间
  - 测试对话框显示时间
  - 测试输入验证响应时间
  - 测试文件夹列表渲染时间
  - _需求: 11.1, 11.2, 11.3, 11.4, 11.5_

- [ ] 13. 编写单元测试
- [ ] 13.1 测试字符串资源
  - 创建`app/src/test/java/takagi/ru/paysage/StringResourcesTest.kt`
  - 验证模块名称字符串
  - 验证文件夹创建相关字符串
  - _需求: 1.1, 1.2, 1.3, 1.4, 1.5_

- [ ] 13.2 测试文件夹名称验证
  - 创建`app/src/test/java/takagi/ru/paysage/FolderValidationTest.kt`
  - 测试空名称验证
  - 测试过长名称验证
  - 测试非法字符验证
  - 测试重复名称验证
  - 测试有效名称验证
  - _需求: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 13.3 测试FolderViewModel
  - 创建`app/src/test/java/takagi/ru/paysage/viewmodel/FolderViewModelTest.kt`
  - 测试createFolder成功场景
  - 测试createFolder失败场景
  - 测试refreshFolders
  - 测试resetCreateFolderState
  - _需求: 4.1, 4.2, 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ] 13.4 测试FolderRepository
  - 创建`app/src/test/java/takagi/ru/paysage/repository/FolderRepositoryTest.kt`
  - 测试createFolder成功场景
  - 测试createFolder文件夹已存在场景
  - 测试getFolders
  - 测试deleteFolder
  - _需求: 4.1, 4.2, 5.2, 5.3_

- [ ] 14. 编写UI测试
- [ ] 14.1 测试CreateFolderDialog显示
  - 创建`app/src/androidTest/java/takagi/ru/paysage/ui/CreateFolderDialogTest.kt`
  - 测试对话框元素显示
  - 测试按钮状态
  - _需求: 4.3_

- [ ] 14.2 测试输入验证UI
  - 测试非法字符输入
  - 测试重复名称输入
  - 测试确定按钮禁用/启用
  - _需求: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 14.3 测试CreateFolderButton交互
  - 测试按钮点击
  - 测试对话框打开
  - _需求: 4.1, 4.2_

- [ ] 15. 编写集成测试
- [ ] 15.1 测试完整创建文件夹流程
  - 创建`app/src/androidTest/java/takagi/ru/paysage/integration/CreateFolderFlowTest.kt`
  - 测试从打开抽屉到创建成功的完整流程
  - 验证Snackbar反馈显示
  - 验证文件夹出现在列表中
  - _需求: 4.1, 4.2, 4.3, 6.1, 6.2, 6.3, 6.4, 6.5, 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 15.2 测试错误处理流程
  - 测试创建重复文件夹
  - 测试权限被拒绝场景
  - 验证错误提示显示
  - _需求: 5.2, 5.3, 6.2_

- [ ] 16. 更新文档
- [ ] 16.1 更新README
  - 添加新功能说明
  - 更新截图（如有）
  - _需求: 所有_

- [ ] 16.2 更新CHANGELOG
  - 记录版本变更
  - 列出新功能和改进
  - _需求: 所有_

- [ ] 17. 最终验证和发布准备
- [ ] 17.1 在不同设备上测试
  - 测试手机（Compact布局）
  - 测试小平板（Medium布局）
  - 测试大平板/桌面（Expanded布局）
  - _需求: 所有_

- [ ] 17.2 在不同Android版本上测试
  - 测试API 21-23
  - 测试API 24-30
  - 测试API 31-34（动态配色）
  - _需求: 所有，特别是10.1, 10.2, 10.3, 10.4, 10.5_

- [ ] 17.3 执行完整的回归测试
  - 验证现有功能未受影响
  - 验证数据完整性
  - 验证向后兼容性
  - _需求: 12.1, 12.2, 12.3, 12.4, 12.5_

- [ ] 17.4 更新版本号
  - 更新versionCode
  - 更新versionName
  - _需求: 所有_
