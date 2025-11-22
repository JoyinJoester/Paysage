# 实施计划 - 文件夹编辑管理功能 (M3E 风格)

- [x] 1. 创建 M3E 风格的文件夹卡片组件
  - 创建 ExpressiveFolderCard 可组合函数
  - 实现选中状态的缩放动画（1.02x）和阴影提升（4dp → 8dp）
  - 添加 2dp primary 色边框动画
  - 实现 primaryContainer 背景色渐变（10% 透明度）
  - 使用 spring 动画（DampingRatioMediumBouncy）
  - _需求: 1.1, 4.1, 4.2_

- [x] 2. 创建编辑模式头部组件
  - 创建 EditModeHeader 可组合函数
  - 实现渐变背景（primaryContainer → surface）
  - 添加圆角底部（24dp）和阴影（4dp）
  - 显示"编辑文件夹"标题（Headline Medium）和"已选择 X 项"副标题
  - 添加取消按钮和全选/取消全选按钮
  - _需求: 1.1, 4.1, 4.2, 4.4_

- [x] 3. 创建底部操作栏组件
  - 创建 EditModeBottomBar 可组合函数
  - 实现渐变背景（surface → surfaceVariant）
  - 添加圆角顶部（24dp）和阴影（8dp）
  - 使用 ExpressiveButton 组件创建重命名、删除、排序按钮
  - 实现按钮的启用/禁用状态（重命名仅在选中 1 项时启用）
  - _需求: 2.1, 3.1, 5.1_

- [x] 4. 实现编辑模式进入/退出动画
  - 实现 slideInVertically 进入动画（300ms EmphasizedDecelerateEasing）
  - 实现 slideOutVertically 退出动画
  - 添加卡片依次出现效果（每个延迟 50ms）
  - 使用 AnimatedVisibility 包装编辑模式 UI
  - _需求: 1.1, 7.5_

- [x] 5. 创建 M3E 风格的重命名对话框
  - 创建 RenameFolderDialog 可组合函数
  - 使用 extraLarge 形状（32dp 圆角）
  - 实现 OutlinedTextField 带 medium 圆角
  - 使用 ExpressiveButton 作为确认按钮
  - 添加名称验证和错误提示
  - _需求: 2.1, 2.2, 2.3, 2.6, 2.7_

- [x] 6. 创建 M3E 风格的删除确认对话框
  - 创建 DeleteConfirmDialog 可组合函数
  - 添加 48dp 警告图标（error 色）
  - 使用 extraLarge 形状
  - 使用 ExpressiveButton（error 容器色）作为删除按钮
  - 显示删除数量和警告信息
  - _需求: 3.1, 3.2, 4.1, 4.2, 4.3_

- [x] 7. 创建排序选项对话框
  - 创建 SortOptionsDialog 可组合函数
  - 使用 extraLarge 形状
  - 实现选项列表（RadioButton + Surface）
  - 选中项使用 primaryContainer 背景
  - 添加 6 种排序选项（名称、日期、书籍数量，各升降序）
  - _需求: 5.1, 5.2, 5.3, 5.5, 5.6_

- [x] 8. 扩展 FolderViewModel 支持编辑状态
- [x] 8.1 添加 FolderEditUiState 数据类
  - 定义 isEditMode、selectedFolders、sortOption 等状态
  - 创建 SortOption 枚举
  - 使用 StateFlow 管理状态
  - _需求: 1.1, 4.1, 5.1_

- [x] 8.2 实现编辑模式方法
  - 实现 enterEditMode() 和 exitEditMode()
  - 实现 toggleFolderSelection()、selectAll()、deselectAll()
  - 实现 renameFolder()、deleteSelectedFolders()
  - 实现 updateSortOption() 和排序逻辑
  - _需求: 1.1, 2.1, 3.1, 4.1, 4.2, 5.1_

- [x] 9. 扩展 FolderRepository 支持编辑操作
- [x] 9.1 实现重命名功能
  - 添加 renameFolder(folderId, newName) 方法
  - 更新 Folder 的 name 和 updatedAt 字段
  - 调用 FolderDao.updateFolder()
  - _需求: 2.1, 2.2, 2.3_

- [x] 9.2 实现删除功能
  - 添加 deleteFolders(folderIds) 方法
  - 调用 FolderDao.deleteFolders()
  - 处理批量删除
  - _需求: 3.1, 3.2, 3.3, 4.1, 4.2_

- [x] 9.3 实现排序功能
  - 添加 updateFolderOrder(folderId, newOrder) 方法
  - 更新 Folder 的 sortOrder 字段
  - 调用 FolderDao.updateFolderOrder()
  - _需求: 5.1, 6.1, 6.2_

- [x] 10. 扩展数据模型和数据库
- [x] 10.1 更新 Folder 数据模型
  - 添加 sortOrder: Int 字段（默认 0）
  - 添加 updatedAt: Long 字段
  - _需求: 5.1, 6.1_

- [x] 10.2 扩展 FolderDao
  - 添加 updateFolder(folder) 方法
  - 添加 deleteFolders(folderIds) 方法
  - 添加 updateFolderOrder(folderId, newOrder) 方法
  - 添加 getFolderById(folderId) 方法
  - _需求: 2.1, 3.1, 5.1, 6.1_

- [x] 10.3 创建数据库迁移
  - 创建迁移脚本添加 sortOrder 和 updatedAt 列
  - 测试迁移逻辑
  - _需求: 5.1, 6.1_

- [x] 11. 创建 M3E 风格的错误提示组件
  - 创建 ErrorSnackbar 可组合函数
  - 使用 errorContainer 背景色
  - 添加错误图标（20dp）
  - 使用 medium 圆角
  - 实现自动消失和手动关闭
  - _需求: 2.4, 2.5, 3.4, 3.5, 7.2, 7.3, 7.4_

- [x] 12. 集成到文件夹管理界面
- [x] 12.1 添加编辑按钮
  - 在文件夹管理屏幕添加编辑按钮（ExpressiveButton）
  - 点击进入编辑模式
  - _需求: 1.1, 1.2, 1.3_

- [x] 12.2 实现编辑模式 UI 切换
  - 根据 isEditMode 状态显示/隐藏编辑 UI
  - 显示 EditModeHeader 和 EditModeBottomBar
  - 文件夹列表使用 ExpressiveFolderCard
  - _需求: 1.1, 4.1_

- [x] 12.3 连接 ViewModel 和 UI
  - 收集 uiState 并传递给组件
  - 连接按钮点击事件到 ViewModel 方法
  - 处理对话框显示/隐藏状态
  - _需求: 所有_

- [x] 13. 添加字符串资源
  - 在 strings.xml 添加英文字符串
  - 在 strings-zh.xml 添加中文字符串
  - 包括标题、按钮、提示、错误消息等
  - _需求: 所有_

- [x] 14. 添加无障碍支持
  - 为所有交互元素添加 contentDescription
  - 添加语义标签
  - 确保触摸目标至少 48dp
  - _需求: 1.4, 7.6, 8.2, 8.4_

- [x] 15. 性能优化
  - 使用 LazyColumn 的 key 参数优化重组
  - 使用 remember 缓存动画状态
  - 避免不必要的重组
  - _需求: 7.7_
