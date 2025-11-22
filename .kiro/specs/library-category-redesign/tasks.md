# 书库分类系统重设计 - 实现任务

## 任务列表

- [x] 1. 数据模型和数据库扩展




- [ ] 1.1 创建CategoryType和DisplayMode枚举
  - 在data/model包中创建CategoryType.kt，定义MANGA和NOVEL两种分类类型
  - 在data/model包中创建DisplayMode.kt，定义LOCAL和ONLINE两种显示模式


  - _需求: 2.1, 2.2, 3.1_

- [ ] 1.2 扩展Book实体类
  - 在Book.kt中添加categoryType字段（默认值MANGA）
  - 添加isOnline字段（默认值false）


  - 添加sourceId和sourceUrl字段（可空）
  - 为categoryType字段添加数据库索引
  - _需求: 3.3, 3.4, 4.1, 4.2_



- [ ] 1.3 创建BookSource实体类
  - 创建data/model/BookSource.kt
  - 定义id、name、baseUrl、categoryType、isEnabled等字段
  - 创建对应的Room DAO接口




  - _需求: 5.3, 5.4_

- [ ] 1.4 实现数据库迁移
  - 创建Migration脚本，添加新字段到books表
  - 创建book_sources表


  - 创建必要的索引
  - 实现用户数据迁移逻辑（根据文件格式自动分类现有书籍）
  - _需求: 3.3, 9.1, 9.2_

- [x] 2. Repository层扩展

- [ ] 2.1 扩展BookRepository
  - 添加getBooksByCategory方法，支持按categoryType和isOnline过滤
  - 添加updateBookCategory方法，更新书籍分类
  - 实现LruCache缓存机制





  - 添加分页查询支持（每页50条）
  - _需求: 3.1, 3.4, 9.2, 9.3_

- [ ] 2.2 创建OnlineSourceRepository
  - 创建repository/OnlineSourceRepository.kt


  - 实现getAllSources、getSourcesByCategory方法
  - 实现addSource、updateSource、deleteSource方法
  - 实现toggleSourceEnabled方法
  - _需求: 5.3, 5.4_


- [ ] 2.3 添加Repository单元测试
  - 测试getBooksByCategory方法的分类过滤功能
  - 测试缓存机制是否正常工作
  - 测试分页查询功能
  - _需求: 9.2_

- [ ] 3. ViewModel层扩展
- [ ] 3.1 扩展LibraryViewModel
  - 添加categoryType: StateFlow<CategoryType>状态
  - 添加displayMode: StateFlow<DisplayMode>状态
  - 添加setCategoryType和setDisplayMode方法
  - 修改displayBooks逻辑，支持按categoryType和displayMode过滤
  - _需求: 2.3, 2.4, 3.1, 6.1_

- [ ] 3.2 创建OnlineSourceViewModel
  - 创建viewmodel/OnlineSourceViewModel.kt
  - 添加bookSources: StateFlow<List<BookSource>>状态
  - 实现getSourcesByCategory方法
  - 实现toggleSourceEnabled、addSource、deleteSource方法
  - _需求: 5.3, 5.4_

- [ ] 3.3 添加ViewModel单元测试
  - 测试分类切换功能
  - 测试显示模式切换功能
  - 测试书籍过滤逻辑
  - _需求: 2.5_

- [-] 4. 导航系统重构

- [x] 4.1 扩展PrimaryNavItem枚举


  - 将Library项拆分为LocalLibrary和OnlineLibrary
  - 为每个项添加hasSecondaryMenu属性
  - 更新图标和标签资源
  - _需求: 7.1, 7.2, 7.3_

- [x] 4.2 创建分类导航配置


  - 创建LocalLibraryNavItems对象，定义"漫画"和"阅读"两个次级导航项
  - 创建OnlineLibraryNavItems对象，定义"漫画书源"和"小说书源"两个次级导航项
  - 配置路由参数（category=manga/novel）
  - _需求: 2.3, 7.2, 7.3_

- [ ] 4.3 更新TwoLayerNavigationScaffold
  - 修改SecondaryDrawerContent，支持根据选中的PrimaryNavItem显示不同的次级菜单
  - 实现次级菜单的展开/收起动画（200ms，Emphasized Easing）
  - 高亮显示当前选中的导航项
  - _需求: 7.2, 7.3, 7.4, 10.2_

- [-] 5. UI组件实现

- [x] 5.1 创建CategoryFilterBar组件


  - 创建ui/components/CategoryFilterBar.kt
  - 实现分类切换按钮（漫画/阅读）
  - 实现显示模式切换按钮（本地/在线）
  - 应用M3E设计风格（ExpressiveSegmentedButton）
  - 添加切换动画（300ms淡入淡出）
  - _需求: 1.1, 1.3, 2.3, 10.1_

- [x] 5.2 重构LibraryScreen


  - 添加categoryType和displayMode参数
  - 集成CategoryFilterBar组件
  - 根据categoryType和displayMode过滤显示的书籍
  - 实现分类切换动画（CategorySwitchAnimation）
  - 添加列表项进入动画（BookListItemAnimation）
  - _需求: 2.3, 2.5, 6.1, 10.1, 10.4_

- [x] 5.3 创建OnlineSourceScreen


  - 创建ui/screens/OnlineSourceScreen.kt
  - 实现书源列表展示
  - 添加TopAppBar，包含返回按钮和添加书源按钮
  - 使用LazyColumn展示书源列表
  - _需求: 5.1, 5.2, 5.3_

- [x] 5.4 创建BookSourceCard组件



  - 创建ui/components/BookSourceCard.kt
  - 显示书源名称、URL和启用状态
  - 实现启用/禁用开关
  - 应用M3E设计风格（ExpressiveCard）
  - _需求: 5.3, 5.4_

- [x] 5.5 创建AddSourceDialog组件


  - 创建ui/components/AddSourceDialog.kt
  - 实现书源名称、URL、分类类型输入
  - 添加表单验证
  - 应用M3E设计风格（圆角24dp）
  - _需求: 5.4_

- [ ] 5.6 添加UI组件测试
  - 测试CategoryFilterBar的分类切换功能
  - 测试LibraryScreen的书籍过滤功能
  - 测试OnlineSourceScreen的书源列表展示
  - _需求: 2.5, 5.5_

- [-] 6. M3E设计风格应用

- [x] 6.1 创建CategoryColors对象



  - 在ui/theme/Color.kt中定义分类特定颜色
  - MangaPrimary: #FF6B35, MangaContainer: #FFDBCC
  - NovelPrimary: #6A4C9C, NovelContainer: #E9DDFF
  - OnlinePrimary: #00BFA5, OnlineContainer: #73F7DD
  - 创建getCategoryColor辅助函数
  - _需求: 1.1, 1.2_

- [x] 6.2 创建ExpressiveShapes对象



  - 在ui/theme/Shape.kt中定义组件形状
  - categoryCard: 16dp圆角
  - sourceCard: 12dp圆角
  - filterChip: 20dp圆角
  - bottomSheet: 顶部24dp圆角
  - _需求: 1.2_

- [ ] 6.3 实现分类切换动画
  - 创建CategorySwitchAnimation可组合函数
  - 使用AnimatedContent实现淡入淡出效果
  - 应用Emphasized Easing缓动曲线
  - 动画时长300ms
  - _需求: 1.4, 10.1_

- [ ] 6.4 实现列表项进入动画
  - 创建BookListItemAnimation可组合函数
  - 使用slideInVertically + fadeIn实现从下方滑入效果
  - 使用弹簧动画（MediumBouncy）
  - 错开动画时间（每项延迟50ms）
  - _需求: 1.4, 10.4_

- [x] 7. 字符串资源和国际化


- [x] 7.1 添加中文字符串资源


  - 在res/values-zh/strings.xml中添加分类相关字符串
  - category_manga, category_novel
  - display_mode_local, display_mode_online
  - nav_local_library, nav_online_library
  - manga_sources, novel_sources
  - _需求: 2.3, 7.1_

- [x] 7.2 添加英文字符串资源


  - 在res/values/strings.xml中添加对应的英文翻译
  - 确保所有新增UI文本都有国际化支持
  - _需求: 2.3, 7.1_

- [ ] 8. 路由和导航集成
- [ ] 8.1 更新NavHost配置
  - 在MainActivity中更新NavHost，添加category参数支持
  - 添加online路由，指向OnlineSourceScreen
  - 实现路由参数解析（category=manga/novel）
  - _需求: 2.3, 5.1, 7.2_

- [ ] 8.2 实现默认视图逻辑
  - 在MainActivity中设置默认路由为"library?category=manga"
  - 实现上次访问分类的记忆功能（使用DataStore）
  - 添加设置选项，允许用户自定义默认分类
  - _需求: 11.1, 11.2, 11.3, 11.4, 11.5_

- [ ] 9. 性能优化
- [ ] 9.1 实现数据库索引
  - 为categoryType字段创建单列索引
  - 为(categoryType, isOnline)创建复合索引
  - 为(categoryType, lastReadAt)创建复合索引
  - _需求: 9.1, 9.2_

- [ ] 9.2 实现分页加载
  - 在BookDao中添加LIMIT和OFFSET参数
  - 在LibraryViewModel中实现分页逻辑
  - 监听滚动事件，自动加载下一页
  - 显示加载指示器（查询时间>100ms）
  - _需求: 9.2, 9.3, 9.4, 9.5_

- [ ] 9.3 实现缓存机制
  - 在BookRepository中使用LruCache缓存书籍列表
  - 缓存大小设置为10个分类
  - 实现缓存失效策略
  - _需求: 9.2_

- [ ] 10. 响应式布局
- [ ] 10.1 实现屏幕尺寸适配
  - 使用WindowSizeClass判断设备类型
  - 小于600dp：单列布局
  - 600dp-840dp：双列布局
  - 大于840dp：三列布局
  - _需求: 8.1, 8.2, 8.3_

- [ ] 10.2 实现横屏适配
  - 检测屏幕方向变化
  - 横屏时自动调整列数
  - 优化导航抽屉在横屏时的显示
  - _需求: 8.4_

- [ ] 10.3 确保触摸目标尺寸
  - 检查所有按钮和可点击元素
  - 确保最小尺寸为48dp
  - 确保元素间距至少8dp
  - _需求: 8.5, 12.5_

- [ ] 11. 可访问性支持
- [ ] 11.1 添加内容描述
  - 为所有Icon添加contentDescription
  - 为所有图片添加有意义的描述
  - 使用stringResource确保国际化
  - _需求: 12.1, 12.4_

- [ ] 11.2 添加语义化标签
  - 为重要文本添加semantics修饰符
  - 提供额外的上下文信息
  - _需求: 12.2_

- [ ] 11.3 确保颜色对比度
  - 检查所有文本颜色与背景的对比度
  - 确保对比度不低于4.5:1
  - 使用Material Theme的颜色系统
  - _需求: 12.2_

- [ ] 11.4 支持系统字体大小
  - 使用MaterialTheme.typography
  - 测试不同字体大小设置
  - 确保布局不会破坏
  - _需求: 12.3_

- [ ] 11.5 添加可访问性测试
  - 使用Accessibility Scanner测试应用
  - 修复发现的可访问性问题
  - _需求: 12.1, 12.2, 12.3, 12.4, 12.5_

- [ ] 12. 错误处理和用户反馈
- [ ] 12.1 创建ErrorView组件
  - 创建ui/components/ErrorView.kt
  - 显示错误图标、消息和重试按钮
  - 应用M3E设计风格
  - _需求: 6.3, 6.4_

- [ ] 12.2 实现网络错误处理
  - 创建OnlineSourceResult密封类
  - 在OnlineSourceRepository中捕获网络异常
  - 在UI中显示友好的错误消息
  - _需求: 6.3, 6.4_

- [ ] 12.3 添加加载状态指示
  - 在数据加载时显示CircularProgressIndicator
  - 在分页加载时显示底部加载指示器
  - 使用Skeleton Screen提升体验
  - _需求: 9.5_

- [ ] 13. 集成测试和验收
- [ ] 13.1 编写集成测试
  - 测试完整的分类切换流程
  - 测试本地/在线模式切换
  - 测试书源管理功能
  - _需求: 2.5, 5.5, 6.5_

- [ ] 13.2 编写UI测试
  - 测试导航流程
  - 测试分类筛选功能
  - 测试书籍列表展示
  - _需求: 2.5, 5.5_

- [ ] 13.3 手动测试和验收
  - 在不同设备上测试（手机、平板）
  - 测试横屏和竖屏模式
  - 测试暗色模式
  - 测试可访问性功能
  - 验证所有需求是否满足
  - _需求: 所有需求_

- [ ] 14. 文档和发布准备
- [ ] 14.1 更新ARCHITECTURE.md
  - 添加分类系统架构说明
  - 更新数据流图
  - 添加在线书源架构
  - _需求: 所有需求_

- [ ] 14.2 创建用户指南
  - 编写分类系统使用说明
  - 编写书源管理指南
  - 添加常见问题解答
  - _需求: 所有需求_

- [ ] 14.3 创建发布说明
  - 列出新增功能
  - 列出改进项
  - 列出已知问题
  - _需求: 所有需求_
