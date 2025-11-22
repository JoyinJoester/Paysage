# 实现计划

- [x] 1. 扩展数据层以支持上次阅读查询


  - 在 BookDao 中添加查询方法，按 lastReadAt 降序获取最近阅读的书籍
  - 在 BookRepository 中添加 getLastReadBookFlow() 和 getLastReadBook() 方法
  - 在 Book 实体类上为 lastReadAt 字段添加数据库索引以优化查询性能
  - _需求: 1.1, 2.1_



- [ ] 2. 扩展 LibraryViewModel 以提供上次阅读状态
  - 添加 lastReadBook StateFlow，从 Repository 收集最近阅读的书籍数据


  - 使用 stateIn 操作符将 Flow 转换为 StateFlow，设置合适的共享策略
  - _需求: 1.1, 2.1_

- [ ] 3. 创建 LastReadingFAB 组件
  - 创建新文件 LastReadingFAB.kt，实现悬浮按钮组件
  - 实现 Material 3 风格的 FloatingActionButton，尺寸为 56dp
  - 添加圆形进度指示器，使用两层 CircularProgressIndicator 实现进度环效果
  - 使用 MenuBook 图标作为按钮图标


  - 实现显示/隐藏动画，使用 AnimatedVisibility 和 fadeIn/fadeOut + scaleIn/scaleOut
  - 实现进度更新动画，使用 animateFloatAsState 平滑过渡
  - 添加无障碍支持，包括 contentDescription 和 semantics 修饰符
  - _需求: 1.1, 1.2, 1.3, 3.1, 3.2, 3.3, 3.4, 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 4. 在 LibraryScreen 中集成 LastReadingFAB
  - 在 LibraryScreen 的 Scaffold 中添加 floatingActionButton 参数


  - 从 ViewModel 收集 lastReadBook 状态
  - 实现点击回调，导航到书籍阅读界面
  - 添加文件存在性检查，文件不存在时显示 Snackbar 错误提示
  - 为书籍列表添加底部内边距，避免FAB遮挡内容


  - 使用 WindowInsets 确保FAB在不同屏幕配置下的正确定位
  - _需求: 1.1, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5, 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 5. 添加国际化字符串资源



  - 在 values/strings.xml 中添加英文字符串资源
  - 在 values-zh/strings.xml 中添加中文字符串资源
  - 包含的字符串: continue_reading, continue_reading_with_title, reading_progress, book_file_not_found
  - _需求: 5.1_

- [ ] 6. 编写单元测试
  - 为 BookDao 的 getLastReadBook 方法编写测试用例
  - 为 BookRepository 的 getLastReadBook 方法编写测试用例
  - 测试场景包括: 无书籍、无阅读记录、多本书籍、忽略null的lastReadAt
  - _需求: 所有需求_

- [ ] 7. 编写 UI 测试
  - 为 LastReadingFAB 组件编写 UI 测试
  - 测试场景包括: 无阅读记录时不显示、有阅读记录时显示、显示正确进度、点击导航、文件不存在时显示错误
  - _需求: 所有需求_
