# 书籍详情底部弹窗实现任务

- [x] 1. 添加国际化字符串资源



  - 在 `app/src/main/res/values/strings.xml` 和 `app/src/main/res/values-zh/strings.xml` 中添加所有需要的字符串资源
  - 包括：书架、查看、排序选项、标签、进度、作者、目录、操作按钮等文本
  - _需求: 1, 2, 3, 4, 5, 6, 7_

- [x] 2. 扩展 Book 数据模型


  - 在 `Book.kt` 中添加 `sortPreference` 字段（如果不存在）
  - 确保 `tags` 字段已存在并正确配置
  - 更新数据库迁移脚本（如果需要）
  - _需求: 2, 4, 5_


- [x] 3. 创建 BookDetailUiState 数据类

  - 在新文件 `app/src/main/java/takagi/ru/paysage/ui/state/BookDetailUiState.kt` 中创建状态类
  - 包含：isVisible、selectedBook、isEditingTags、tempTags、showDeleteConfirmation 等字段
  - _需求: 1, 4, 6_


- [x] 4. 实现工具函数

  - 创建 `app/src/main/java/takagi/ru/paysage/util/FormatUtils.kt` 文件
  - 实现文件大小格式化函数（字节转 MB/GB）
  - 实现相对时间格式化函数（如"2小时前"）
  - 实现路径复制到剪贴板函数
  - _需求: 2, 3, 7_

- [x] 5. 创建 BookDetailHeader 组件


  - 在新文件 `app/src/main/java/takagi/ru/paysage/ui/components/BookDetailComponents.kt` 中创建组件
  - 显示封面图片（使用 Coil 加载）
  - 显示书籍标题
  - 显示文件格式标签
  - 显示文件大小
  - 处理封面加载失败的情况
  - _需求: 2.1, 2.2, 2.3, 2.4, 2.6_


- [ ] 6. 创建 BookDetailActions 组件
  - 在 `BookDetailComponents.kt` 中创建组件
  - 实现书架按钮（切换收藏状态）
  - 实现查看按钮（打开书籍）
  - 使用 ExpressiveButton 组件
  - 添加按钮图标和文本
  - _需求: 6.2, 6.3_


- [ ] 7. 创建 BookDetailSortSection 组件
  - 在 `BookDetailComponents.kt` 中创建组件
  - 实现下拉菜单（ExposedDropdownMenuBox）
  - 提供排序选项：新的、标题、作者、最近阅读、添加时间
  - 保存用户选择的排序偏好
  - 添加选中状态的视觉反馈
  - _需求: 5.1, 5.2, 5.3, 5.4, 5.5_


- [ ] 8. 创建 BookDetailTagSection 组件
  - 在 `BookDetailComponents.kt` 中创建组件
  - 显示书籍标签列表（使用 FlowRow）
  - 显示"无 # 标签"占位文本（当没有标签时）
  - 实现编辑按钮
  - 使用 ExpressiveChip 组件显示标签

  - _需求: 4.1, 4.2, 4.3, 4.4_

- [ ] 9. 创建标签编辑对话框
  - 在 `BookDetailComponents.kt` 中创建 `TagEditDialog` 组件
  - 实现标签输入框
  - 实现添加标签功能
  - 实现删除标签功能
  - 实现保存和取消按钮
  - _需求: 4.5_


- [ ] 10. 创建 BookDetailProgressSection 组件
  - 在 `BookDetailComponents.kt` 中创建组件
  - 显示当前页码和总页数
  - 显示上次阅读时间（格式化为相对时间）
  - 显示"还未读过"状态（当 lastReadAt 为空时）
  - 显示"已读完"状态（当 isFinished 为 true 时）
  - 使用 LinearProgressIndicator 显示进度条
  - _需求: 3.1, 3.2, 3.3, 3.4, 3.5_


- [ ] 11. 创建 BookDetailAuthorSection 组件
  - 在 `BookDetailComponents.kt` 中创建组件
  - 显示作者/编辑者信息
  - 使用 Person 图标
  - 当没有作者信息时不显示此区域
  - _需求: 2.5_


- [ ] 12. 创建 BookDetailPathSection 组件
  - 在 `BookDetailComponents.kt` 中创建组件
  - 显示文件路径
  - 实现点击复制路径功能
  - 显示复制成功的 Snackbar 提示
  - 处理路径过长的情况（使用 maxLines 和 ellipsis）

  - _需求: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 13. 创建 BookDetailBottomActions 组件
  - 在 `BookDetailComponents.kt` 中创建组件
  - 实现关闭按钮
  - 实现收藏按钮（带状态切换动画）
  - 实现编辑按钮
  - 实现分享按钮
  - 实现删除按钮

  - 使用 ExpressiveIconButton 组件
  - _需求: 6.5_

- [ ] 14. 创建删除确认对话框
  - 在 `BookDetailComponents.kt` 中创建 `DeleteConfirmDialog` 组件

  - 显示确认消息
  - 实现确定和取消按钮
  - _需求: 6.5_

- [ ] 15. 创建主 BookDetailBottomSheet 组件
  - 在 `BookDetailComponents.kt` 中创建主组件
  - 使用 ModalBottomSheet 作为容器
  - 组合所有子组件（Header、Actions、Sort、Tags、Progress、Author、Path、BottomActions）
  - 使用 LazyColumn 支持滚动
  - 设置最大高度为屏幕高度的 90%
  - 实现圆角和背景色

  - _需求: 1.1, 1.2, 1.3, 1.4, 1.5, 8.3, 8.4_

- [x] 16. 在 LibraryViewModel 中添加状态管理

  - 添加 `bookDetailUiState` StateFlow
  - 实现 `showBookDetail(book: Book)` 方法
  - 实现 `hideBookDetail()` 方法
  - 实现 `updateBookTags(bookId: Long, tags: List<String>)` 方法
  - 实现 `updateSortPreference(bookId: Long, preference: String)` 方法
  - 实现 `toggleFavorite(bookId: Long)` 方法
  - 实现 `deleteBook(bookId: Long)` 方法
  - _需求: 1, 4, 5, 6_

- [x] 17. 实现长按手势检测


  - 在 `LibraryScreen.kt` 中修改 `BookCard` 组件
  - 使用 `pointerInput` 修饰符添加长按检测
  - 长按时长设置为 500 毫秒
  - 触发时调用 ViewModel 的 `showBookDetail` 方法
  - 保持原有的点击功能
  - _需求: 1.1_

- [x] 18. 集成 BookDetailBottomSheet 到 LibraryScreen

  - 在 `LibraryScreen.kt` 中添加 BookDetailBottomSheet 组件
  - 监听 ViewModel 的 bookDetailUiState
  - 传递所有必要的回调函数
  - 处理弹窗的显示和隐藏
  - _需求: 1.1, 1.2, 1.3, 1.4_


- [ ] 19. 实现响应式布局适配
  - 在 `BookDetailBottomSheet` 中添加屏幕尺寸检测
  - 小屏幕（< 600dp）：单列布局，封面 100×150dp
  - 中等屏幕（600-840dp）：单列布局，封面 120×180dp
  - 大屏幕（> 840dp）：双列布局，封面 160×240dp
  - 调整按钮间距和字体大小

  - _需求: 8.1, 8.2, 8.5_

- [ ] 20. 实现收藏状态切换动画
  - 在 `BookDetailBottomActions` 中添加收藏图标动画
  - 实现 360 度旋转动画
  - 实现缩放动画（1.0x → 1.3x → 1.0x）
  - 动画时长设置为 400ms
  - 使用弹簧效果
  - _需求: 6.2_


- [ ] 21. 实现文件不存在错误处理
  - 在 `BookDetailBottomSheet` 中检查文件是否存在
  - 显示警告图标
  - 禁用"查看"按钮
  - 在路径区域显示"文件不存在"提示
  - 提供"从书库移除"选项
  - _需求: 6.3_


- [x] 22. 实现分享功能

  - 在 ViewModel 中实现 `shareBook(book: Book)` 方法
  - 使用 Android 的 Intent.ACTION_SEND
  - 分享书籍文件或路径信息
  - 处理分享失败的情况
  - _需求: 6.5_


- [ ] 23. 添加无障碍支持
  - 为所有交互元素添加 contentDescription
  - 使用 semantics 修饰符添加语义信息
  - 设置正确的焦点顺序
  - 确保对比度符合 WCAG AA 标准
  - 测试 TalkBack 支持
  - _需求: 9.1, 9.2, 9.3, 9.4, 9.5_

- [x] 24. 编写单元测试

  - 测试文件大小格式化函数
  - 测试相对时间格式化函数
  - 测试标签管理逻辑
  - 测试 ViewModel 的状态更新
  - _需求: 所有_


- [x] 25. 编写 UI 测试

  - 测试长按手势触发弹窗
  - 测试弹窗的显示和隐藏
  - 测试所有按钮的点击事件
  - 测试标签编辑流程
  - 测试删除确认对话框
  - _需求: 所有_
