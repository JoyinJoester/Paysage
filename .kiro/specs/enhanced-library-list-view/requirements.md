# 需求文档

## 简介

本功能旨在美化库布局中的列表视图，提供更加美观和信息丰富的书籍展示方式。通过放大封面、添加圆角处理、显示作者信息、阅读进度和书籍状态等元素，为用户提供更好的视觉体验和信息获取效率。

## 术语表

- **LibraryScreen**: 书库主界面，显示用户的书籍集合
- **BookListView**: 列表视图组件，以列表形式展示书籍
- **BookListItem**: 列表项组件，单个书籍的卡片展示
- **Book**: 书籍数据模型，包含书籍的所有信息
- **CoverImage**: 封面图片，书籍的封面展示
- **ReadingProgress**: 阅读进度，显示当前阅读页数和总页数
- **BookStatus**: 书籍状态，包括未读、阅读中、已读、最新等状态

## 需求

### 需求 1：放大封面显示

**用户故事：** 作为一个用户，我想要在列表视图中看到更大的书籍封面，以便更容易识别书籍。

#### 验收标准

1. WHEN 用户查看列表视图时，THE LibraryScreen SHALL 显示封面尺寸为 120dp × 180dp 的书籍封面
2. WHERE 书籍有封面图片，THE BookListItem SHALL 使用 AsyncImage 加载并显示封面
3. WHERE 书籍没有封面图片，THE BookListItem SHALL 显示默认的书籍图标占位符
4. THE BookListItem SHALL 使用 ContentScale.Crop 确保封面图片填充整个区域

### 需求 2：封面圆角处理

**用户故事：** 作为一个用户，我想要封面图片有圆角效果，以便界面看起来更加现代和美观。

#### 验收标准

1. THE BookListItem SHALL 对封面图片应用 12dp 的圆角半径
2. THE BookListItem SHALL 使用 MaterialTheme.shapes.medium 作为封面的形状
3. THE BookListItem SHALL 确保圆角效果在所有封面状态下（加载中、加载成功、加载失败）都保持一致
4. THE BookListItem SHALL 使用 Modifier.clip() 裁剪封面图片以实现圆角效果

### 需求 3：显示作者信息

**用户故事：** 作为一个用户，我想要在列表项中看到书籍的作者信息，以便快速了解书籍的创作者。

#### 验收标准

1. WHERE 书籍有作者信息，THE BookListItem SHALL 在标题下方显示作者名称
2. THE BookListItem SHALL 使用 MaterialTheme.typography.bodyMedium 样式显示作者名称
3. THE BookListItem SHALL 使用 MaterialTheme.colorScheme.onSurfaceVariant 颜色显示作者名称
4. WHERE 书籍没有作者信息，THE BookListItem SHALL 不显示作者区域

### 需求 4：显示阅读进度

**用户故事：** 作为一个用户，我想要在列表项中看到书籍的阅读进度，以便了解我的阅读状态。

#### 验收标准

1. WHERE 书籍的当前页数大于 0，THE BookListItem SHALL 显示线性进度条
2. THE BookListItem SHALL 计算并显示阅读进度百分比（当前页数 / 总页数）
3. THE BookListItem SHALL 在进度条右侧显示文本格式的进度（例如："3/112"）
4. THE BookListItem SHALL 使用 MaterialTheme.colorScheme.primary 颜色显示进度条
5. THE BookListItem SHALL 设置进度条高度为 6dp 以提高可见性

### 需求 5：显示书籍状态标签

**用户故事：** 作为一个用户，我想要在列表项中看到书籍的状态标签，以便快速识别书籍的阅读状态。

#### 验收标准

1. THE BookListItem SHALL 根据书籍的阅读状态显示相应的状态标签
2. WHERE 书籍状态为"未读"，THE BookListItem SHALL 显示红色标签
3. WHERE 书籍状态为"阅读中"，THE BookListItem SHALL 显示绿色标签
4. WHERE 书籍状态为"已读"，THE BookListItem SHALL 显示蓝色标签
5. WHERE 书籍状态为"最新"，THE BookListItem SHALL 显示橙红色标签
6. THE BookListItem SHALL 在标签中显示对应的状态文字（未读、阅读中、已读、最新）
7. THE BookListItem SHALL 将状态标签放置在封面图片的左上角

### 需求 6：优化布局和间距

**用户故事：** 作为一个用户，我想要列表项有合理的布局和间距，以便界面看起来整洁有序。

#### 验收标准

1. THE BookListItem SHALL 使用 Row 布局水平排列封面和信息区域
2. THE BookListItem SHALL 在封面和信息区域之间设置 16dp 的间距
3. THE BookListItem SHALL 为整个卡片设置 16dp 的内边距
4. THE BookListItem SHALL 在列表项之间设置 12dp 的垂直间距
5. THE BookListItem SHALL 确保信息区域占据剩余的水平空间
6. THE BookListItem SHALL 使用 Column 布局垂直排列标题、作者、页数和进度信息

### 需求 7：响应式交互

**用户故事：** 作为一个用户，我想要列表项有良好的点击反馈，以便知道我的操作被识别。

#### 验收标准

1. WHEN 用户点击列表项时，THE BookListItem SHALL 触发 onClick 回调
2. THE BookListItem SHALL 使用 ExpressiveCard 提供 Material 3 风格的点击涟漪效果
3. THE BookListItem SHALL 在点击时提供视觉反馈（涟漪动画）
4. THE BookListItem SHALL 确保整个卡片区域都可以点击

### 需求 8：性能优化

**用户故事：** 作为一个用户，我想要列表视图流畅滚动，以便快速浏览大量书籍。

#### 验收标准

1. THE BookListView SHALL 使用 LazyColumn 实现虚拟化滚动
2. THE BookListItem SHALL 为每个书籍项设置唯一的 key（使用 book.id）
3. THE BookListItem SHALL 使用 Coil 的内存缓存功能缓存封面图片
4. THE BookListItem SHALL 设置 crossfade 动画时长为 300ms 以平滑加载封面
5. THE BookListItem SHALL 避免在滚动时进行复杂的计算或布局操作
