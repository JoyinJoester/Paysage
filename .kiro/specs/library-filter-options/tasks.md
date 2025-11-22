# 实现计划

- [x] 1. 扩展数据访问层


  - 在 BookDao 中添加查询最近阅读书籍和分类统计的方法
  - 添加 getRecentBooksFlow(limit: Int) 方法，按最后阅读时间降序返回书籍
  - 添加 getCategoriesWithCount() 方法，返回分类名称和书籍数量
  - _需求: 1.4, 1.5, 2.6, 3.6_

- [x] 2. 扩展数据仓库层


  - 在 BookRepository 中添加对应的数据流方法
  - 实现 getRecentBooksFlow(limit: Int) 方法
  - 实现 getCategoriesWithCountFlow() 方法
  - 实现 getRecentBooks(limit: Int) 挂起函数
  - _需求: 1.4, 1.5, 2.6, 3.6_

- [x] 3. 创建数据模型


  - 创建 FilterMode 枚举类，定义筛选模式（ALL, FAVORITES, RECENT, CATEGORIES, CATEGORY）
  - 创建 CategoryInfo 数据类，包含分类名称和书籍数量
  - _需求: 4.1, 4.2, 3.2_

- [x] 4. 扩展视图模型层


  - 在 LibraryViewModel 中添加筛选状态管理
  - 添加 filterMode StateFlow 管理当前筛选模式
  - 添加 selectedCategory StateFlow 管理选中的分类
  - 添加 recentBooks StateFlow 获取最近阅读的书籍
  - 添加 categoriesWithCount StateFlow 获取分类统计
  - 实现 displayBooks StateFlow，根据筛选模式组合不同的数据流
  - 实现 setFilterMode(mode, category) 方法设置筛选模式
  - 实现 parseFilterFromRoute(filter, category) 方法解析路由参数
  - _需求: 1.1, 1.2, 2.1, 2.2, 3.1, 3.3, 4.1, 4.2, 4.3_

- [x] 5. 创建分类列表视图组件


  - 创建 CategoriesListView Composable 函数
  - 实现分类列表的 LazyColumn 布局
  - 创建 CategoryCard Composable 函数显示单个分类
  - 显示分类名称和书籍数量
  - 处理分类点击事件，导航到分类书籍视图
  - _需求: 3.1, 3.2, 3.3_

- [x] 6. 扩展空状态视图组件


  - 创建 EmptyFilterView Composable 函数
  - 根据不同的筛选模式显示对应的空状态提示
  - 为收藏、最近阅读、分类列表和特定分类提供专门的空状态视图
  - 在空状态中提供操作建议
  - _需求: 1.3, 2.5, 3.5, 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 7. 修改 LibraryScreen 支持筛选视图


  - 修改 LibraryScreen 函数签名，添加 filter 和 category 参数
  - 使用 LaunchedEffect 解析路由参数并更新 ViewModel 状态
  - 根据 filterMode 显示不同的视图（分类列表或书籍列表）
  - 集成 CategoriesListView 和 EmptyFilterView 组件
  - 确保搜索栏和其他 UI 状态在筛选切换时保持不变
  - _需求: 1.1, 1.2, 2.1, 2.2, 3.1, 3.3, 4.4_

- [x] 8. 更新导航系统


  - 在 Screen.Library 中添加 createRoute 方法支持筛选参数
  - 修改 SecondaryDrawerContent 中的 LibraryDrawerContent
  - 处理菜单项点击事件，根据 item.id 导航到对应的筛选视图
  - 使用 launchSingleTop 避免重复导航
  - 确保系统返回按钮正确处理筛选状态
  - _需求: 4.1, 4.2, 4.3, 4.5_

- [x] 9. 添加字符串资源



  - 在 strings.xml 中添加中文字符串资源
  - 在 strings.xml (en) 中添加英文字符串资源
  - 包括空状态提示、分类相关文本等
  - _需求: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 10. 错误处理和边界情况



  - 在 ViewModel 中添加 try-catch 处理数据库查询错误
  - 验证路由参数的有效性
  - 处理无效的分类名称
  - 确保空数据不会导致应用崩溃
  - _需求: 1.3, 2.5, 3.5, 5.1, 5.2, 5.3, 5.4_
