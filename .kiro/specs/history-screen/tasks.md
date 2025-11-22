# 历史记录功能实现任务列表

- [x] 1. 创建数据模型和数据库结构


  - 创建HistoryItem数据类，包含所有必要字段（标题、缩略图、文件信息等）
  - 创建DownloadStatus枚举类，定义下载状态
  - 创建HistoryEntity Room实体类，映射数据库表结构
  - 创建Converters类，处理DownloadStatus的类型转换
  - _Requirements: 4.1, 4.2_



- [ ] 1.1 创建HistoryDao接口
  - 实现getAllHistory()方法，返回Flow<List<HistoryEntity>>
  - 实现getHistoryById()方法，根据ID查询单条记录
  - 实现insertHistory()方法，插入新的历史记录
  - 实现updateProgress()方法，更新下载进度
  - 实现updateStatus()方法，更新下载状态
  - 实现deleteHistoryById()方法，删除单条记录
  - 实现deleteAllHistory()方法，清空所有记录
  - 实现getHistoryCount()方法，获取记录总数


  - _Requirements: 4.2, 4.3_

- [ ] 1.2 更新PaysageDatabase
  - 在PaysageDatabase中添加HistoryEntity到entities列表
  - 添加historyDao()抽象方法


  - 创建数据库迁移MIGRATION_7_8，添加download_history表
  - 在download_time字段上创建索引
  - 更新数据库版本号到8
  - _Requirements: 4.2_

- [ ] 2. 实现Repository层
  - 创建HistoryRepository接口，定义所有数据操作方法
  - 创建HistoryRepositoryImpl实现类
  - 实现getAllHistory()方法，将HistoryEntity转换为HistoryItem


  - 实现insertHistory()方法，将HistoryItem转换为HistoryEntity并保存
  - 实现updateProgress()和updateStatus()方法
  - 实现deleteHistory()和clearAllHistory()方法
  - 实现getHistoryCount()方法
  - 添加错误处理逻辑
  - _Requirements: 4.2, 4.3, 4.4_

- [ ] 3. 创建HistoryViewModel
  - 创建HistoryViewModel类，继承ViewModel
  - 定义historyItems StateFlow，存储历史记录列表
  - 定义isLoading StateFlow，管理加载状态
  - 定义error StateFlow，管理错误信息


  - 定义selectedItem StateFlow，管理选中的历史记录项
  - 实现loadHistory()方法，从Repository加载数据
  - 实现deleteHistoryItem()方法，删除单条记录
  - 实现clearAllHistory()方法，清空所有记录
  - 实现selectItem()方法，选择历史记录项
  - 实现clearError()方法，清除错误信息
  - 添加错误处理和日志记录
  - _Requirements: 4.3, 4.4, 5.3, 5.4, 6.5, 6.6_



- [ ] 4. 创建UI辅助组件
  - 创建HistoryThumbnail组件，显示缩略图或默认图标
  - 使用AsyncImage加载缩略图，添加crossfade动画
  - 创建FileTypeChip组件，显示文件类型标签
  - 创建EmptyHistoryView组件，显示空状态提示
  - 添加格式化工具函数formatFileSize()，格式化文件大小
  - 添加格式化工具函数formatDownloadTime()，格式化下载时间
  - 确保所有组件支持深色模式
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 7.2, 7.3_



- [ ] 5. 创建HistoryListItem组件
  - 创建HistoryListItem可组合函数
  - 使用Card作为容器，设置elevation和padding
  - 实现Row布局，包含缩略图和内容信息
  - 添加缩略图，使用HistoryThumbnail组件，尺寸80dp
  - 添加标题Text，支持最多2行显示，超出显示省略号
  - 添加文件信息行，包含FileTypeChip和文件大小
  - 添加下载时间Text
  - 添加LinearProgressIndicator显示下载进度


  - 使用pointerInput实现点击和长按手势检测
  - 添加无障碍支持，设置contentDescription
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 5.1, 5.2_

- [ ] 6. 创建HistoryTopBar组件
  - 创建HistoryTopBar可组合函数
  - 使用TopAppBar作为容器
  - 添加返回按钮，点击时触发onBackClick回调
  - 添加标题Text，显示"历史记录"
  - 添加菜单按钮（IconButton with MoreVert icon）
  - 实现DropdownMenu，包含"清空历史记录"和"筛选"选项


  - 添加清空确认对话框
  - 确保符合Material Design 3规范
  - _Requirements: 2.1, 6.1, 6.2, 6.3, 6.4, 7.1_

- [ ] 7. 创建HistoryScreen主界面
  - 创建HistoryScreen可组合函数
  - 使用Scaffold作为根容器
  - 集成HistoryTopBar作为topBar
  - 从ViewModel收集historyItems、isLoading和error状态


  - 实现条件渲染：加载中显示LoadingView，空状态显示EmptyHistoryView
  - 使用LazyColumn显示历史记录列表
  - 为每个HistoryListItem设置onClick和onLongClick回调
  - 实现长按显示操作菜单（删除、重新下载、打开文件）
  - 添加SnackbarHost显示错误信息
  - 处理错误状态，显示Toast或Snackbar


  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 8. 实现滑动手势导航
  - 在MainActivity中创建LibraryWithHistoryPager可组合函数
  - 使用HorizontalPager实现页面切换，pageCount设置为2
  - 设置initialPage为0（主页）
  - 在page 0显示LibraryScreen
  - 在page 1显示HistoryScreen
  - 实现HistoryScreen的onBackClick，使用animateScrollToPage返回主页
  - 添加页面切换动画效果

  - 确保滑动手势流畅，无卡顿
  - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [ ] 9. 集成到MainActivity
  - 在MainActivity中创建HistoryViewModel实例
  - 更新PaysageApp函数，使用LibraryWithHistoryPager替代LibraryScreen

  - 传递必要的回调函数（onBookClick、onSettingsClick等）
  - 确保ViewModel正确注入到HistoryScreen
  - 测试从主页滑动到历史记录页面的功能
  - _Requirements: 1.1, 1.2_

- [ ] 10. 添加字符串资源
  - 在strings.xml中添加history_title字符串

  - 添加history_empty字符串
  - 添加history_clear_all字符串
  - 添加history_clear_confirm字符串
  - 添加history_delete_item字符串
  - 添加history_redownload字符串
  - 添加history_open_file字符串
  - 添加history_file_not_found字符串
  - 在strings.xml (zh)中添加对应的中文翻译

  - _Requirements: 2.1, 2.3, 6.3, 6.4, 6.5_

- [ ] 11. 实现历史记录创建逻辑
  - 在下载管理器中集成历史记录创建
  - 当用户开始下载时，调用HistoryRepository.insertHistory()
  - 传递必要的信息（标题、文件类型、文件大小等）
  - 获取返回的历史记录ID，用于后续更新
  - _Requirements: 4.1_


- [ ] 12. 实现进度更新逻辑
  - 在下载管理器中监听下载进度
  - 定期调用HistoryRepository.updateProgress()更新进度
  - 当下载完成时，调用updateStatus()更新状态为COMPLETED
  - 当下载失败时，更新状态为FAILED
  - 确保UI实时反映进度变化
  - _Requirements: 4.4, 4.5, 3.6, 3.7_


- [ ] 13. 实现历史记录项点击功能
  - 在HistoryScreen中处理onItemClick回调
  - 检查文件是否存在
  - 如果文件存在，使用Intent打开文件
  - 如果文件不存在，显示错误提示
  - 添加文件类型MIME类型映射
  - 处理没有应用可以打开文件的情况
  - _Requirements: 5.1_


- [ ] 14. 实现历史记录项长按菜单
  - 在HistoryScreen中处理onItemLongClick回调
  - 显示ModalBottomSheet或DropdownMenu
  - 添加"删除记录"选项，调用ViewModel.deleteHistoryItem()
  - 添加"重新下载"选项（如果状态为FAILED）
  - 添加"打开文件"选项


  - 添加"分享"选项
  - 实现每个选项的功能
  - _Requirements: 5.2, 5.3, 5.4, 5.5_

- [ ] 15. 实现清空历史记录功能
  - 在HistoryTopBar中处理清空按钮点击
  - 显示AlertDialog确认对话框
  - 对话框显示确认消息


  - 添加"取消"和"确定"按钮
  - 点击"确定"时调用ViewModel.clearAllHistory()
  - 显示成功提示
  - _Requirements: 6.5, 6.6_

- [ ] 16. 优化性能
  - 在HistoryDao中使用Flow实现响应式数据更新
  - 在LazyColumn中使用key参数优化重组



  - 使用Coil的内存缓存和磁盘缓存优化图片加载
  - 实现历史记录数量限制（最多500条）
  - 添加定期清理旧记录的逻辑
  - 测试大量历史记录时的性能表现
  - _Requirements: 2.4, 2.5_

- [ ] 17. 添加无障碍支持
  - 为所有图标添加contentDescription
  - 确保所有可点击元素至少48dp
  - 测试TalkBack屏幕阅读器支持
  - 确保文本和背景对比度符合WCAG标准
  - 添加语义化标签
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 18. 编写单元测试
  - 为HistoryRepository编写单元测试
  - 测试CRUD操作
  - 测试数据转换逻辑
  - 为HistoryViewModel编写单元测试
  - 测试状态管理
  - 测试用户操作（删除、清空等）
  - 测试错误处理
  - _Requirements: 所有需求_

- [ ] 19. 编写UI测试
  - 为HistoryScreen编写UI测试
  - 测试列表显示
  - 测试空状态显示
  - 测试加载状态显示
  - 测试用户交互（点击、长按）
  - 测试滑动手势
  - 测试页面切换动画
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 5.1, 5.2_

- [ ] 20. 编写集成测试
  - 编写端到端测试
  - 测试从主页滑动到历史记录页面
  - 测试历史记录的创建和显示
  - 测试历史记录的删除
  - 测试清空所有历史记录
  - 测试打开文件功能
  - _Requirements: 所有需求_
