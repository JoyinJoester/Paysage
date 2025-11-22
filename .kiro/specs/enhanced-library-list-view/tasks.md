# 实现计划

- [x] 1. 更新 BookListItem 组件的基础布局


  - 修改 LibraryScreen.kt 中的 BookListItem 组件
  - 将封面尺寸从 80dp × 120dp 更新为 120dp × 180dp
  - 为封面添加 12dp 圆角（使用 MaterialTheme.shapes.medium）
  - 调整 Row 布局的间距为 16dp
  - 调整卡片的内边距为 16dp
  - _需求: 1.1, 1.2, 1.3, 1.4, 2.1, 2.2, 2.3, 2.4, 6.1, 6.2, 6.3, 6.4_

- [x] 2. 实现封面区域的圆角和占位符


  - 使用 Modifier.clip() 为封面图片应用圆角
  - 确保 AsyncImage 使用 ContentScale.Crop
  - 优化默认图标占位符的尺寸为 48dp
  - 确保圆角在所有状态下保持一致
  - _需求: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4_

- [x] 3. 实现 StatusBadge 组件

  - 在 LibraryScreen.kt 中创建 StatusBadge 组件
  - 实现不同阅读状态的颜色映射（未读、阅读中、已读、最新）
  - 将状态标签放置在封面的左上角（8dp 内边距）
  - 使用 MaterialTheme.shapes.small 作为标签形状
  - _需求: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7_

- [x] 4. 实现连接式阅读状态标签

  - 创建 ConnectedReadingStatusBadge 组件（如果尚未存在，则复用现有实现）
  - 实现左侧状态文字区域（绿色背景 #4CAF50）
  - 实现右侧进度百分比区域（深绿色背景 #2E7D32）
  - 确保两个区域无缝连接（左侧圆角，右侧圆角）
  - 计算并显示阅读进度百分比
  - _需求: 5.1, 5.4, 4.2_

- [x] 5. 更新信息区域布局

  - 修改 BookListItem 中的 Column 布局
  - 设置 verticalArrangement 为 Arrangement.SpaceBetween
  - 确保标题使用 MaterialTheme.typography.titleMedium
  - 设置标题最多显示 2 行，超出显示省略号
  - 添加 4dp 的垂直间距
  - _需求: 6.5, 6.6_

- [x] 6. 实现作者信息显示

  - 在标题下方添加作者信息显示
  - 使用 book.author?.let {} 处理可选的作者信息
  - 使用 MaterialTheme.typography.bodyMedium 样式
  - 使用 MaterialTheme.colorScheme.onSurfaceVariant 颜色
  - 设置最多显示 1 行，超出显示省略号
  - 添加 4dp 的垂直间距
  - _需求: 3.1, 3.2, 3.3, 3.4_

- [x] 7. 实现 MetadataRow 组件

  - 创建 MetadataRow 组件显示页数和文件格式
  - 使用 Row 布局水平排列元数据
  - 设置 8dp 的水平间距
  - 页数使用 MaterialTheme.colorScheme.onSurfaceVariant 颜色
  - 文件格式使用 MaterialTheme.colorScheme.primary 颜色
  - 使用 MaterialTheme.typography.bodySmall 样式
  - _需求: 6.6_

- [x] 8. 实现 ProgressSection 组件

  - 创建 ProgressSection 组件显示阅读进度
  - 使用 Row 布局水平排列进度条和进度文本
  - 设置进度条高度为 6dp（比默认的 4dp 更明显）
  - 使用 MaterialTheme.colorScheme.primary 作为进度条颜色
  - 在进度条右侧显示 "当前页/总页数" 格式的文本
  - 设置 8dp 的水平间距
  - 只在 showProgress 为 true 且 currentPage > 0 时显示
  - _需求: 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 9. 优化图片加载性能

  - 确保 AsyncImage 使用 memoryCacheKey(book.id.toString())
  - 设置 crossfade 动画时长为 300ms
  - 使用 Coil 的内存缓存和磁盘缓存
  - _需求: 8.3, 8.4_

- [x] 10. 优化列表滚动性能

  - 确保 BookListView 使用 LazyColumn
  - 为每个列表项设置唯一的 key（使用 book.id）
  - 设置合适的 contentPadding 和 verticalArrangement
  - 避免在 Composable 中进行复杂计算
  - _需求: 8.1, 8.2, 8.5_

- [x] 11. 添加国际化字符串资源


  - 在 res/values/strings.xml 中添加状态标签字符串
  - 在 res/values-zh/strings.xml 中添加中文翻译
  - 确保所有硬编码的文本都使用 stringResource
  - _需求: 5.6_

- [x] 12. 实现响应式交互

  - 确保 BookListItem 使用 ExpressiveCard
  - 验证点击时触发 onClick 回调
  - 确保整个卡片区域都可以点击
  - 验证 Material 3 风格的涟漪效果
  - _需求: 7.1, 7.2, 7.3, 7.4_

- [x] 13. 编写单元测试


  - 测试 StatusBadge 的状态颜色和文本逻辑
  - 测试进度百分比计算的准确性
  - 测试边界情况（currentPage = 0, totalPages = 0）
  - _需求: 所有需求_

- [x] 14. 编写 UI 测试



  - 测试 BookListItem 在有封面和无封面情况下的渲染
  - 测试有作者和无作者情况下的渲染
  - 测试不同阅读状态的显示
  - 测试点击交互和动画效果
  - _需求: 所有需求_
