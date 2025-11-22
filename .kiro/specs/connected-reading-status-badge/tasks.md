# Implementation Plan

- [x] 1. 创建 ConnectedReadingStatusBadge 可组合函数


  - 在 `LibraryScreen.kt` 文件底部添加新的可组合函数
  - 实现接受状态文字、进度百分比、颜色参数的函数签名
  - 使用 Row 布局，设置 `horizontalArrangement = Arrangement.spacedBy(0.dp)` 确保无间隙
  - 创建左侧 Surface，使用左圆角形状 `RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)`，绿色背景
  - 创建右侧 Surface，使用右圆角形状 `RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)`，深绿色背景
  - 为两个 Surface 添加文字内容，使用 `MaterialTheme.typography.labelSmall` 和白色文字
  - 设置适当的内边距（horizontal = 8.dp, vertical = 4.dp）
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5, 4.1, 4.2, 4.4, 4.5_

- [x] 2. 修改 BookCard 组件以使用新的连接式标签


  - 在 `BookCard` 组件中定位状态标签渲染代码
  - 将现有的阅读中状态渲染逻辑替换为条件判断
  - 当状态为 READING 时，调用 `ConnectedReadingStatusBadge` 并传入状态文字和进度百分比
  - 计算进度百分比：`(book.currentPage.toFloat() / book.totalPages.coerceAtLeast(1) * 100).toInt()`
  - 保持其他状态（FINISHED, LATEST, UNREAD）的现有单一标签实现不变
  - 确保所有标签都使用相同的位置和外边距（`Modifier.align(Alignment.TopStart).padding(8.dp)`）
  - _Requirements: 1.1, 3.1, 3.2, 3.3, 3.4, 3.5, 4.3_

- [x] 3. 验证和测试实现



  - 运行应用并导航到书库界面
  - 验证阅读中状态的书籍显示连接式标签
  - 检查状态文字和进度百分比之间无视觉间隙
  - 验证颜色正确（左侧绿色 #4CAF50，右侧深绿色 #2E7D32）
  - 验证圆角正确应用（左侧左圆角，右侧右圆角）
  - 验证其他状态（已完成、最新、未读）仍显示单一标签
  - 测试不同进度百分比的显示（0%, 25%, 50%, 100%）
  - 检查多语言环境下的文字显示
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 3.5_
