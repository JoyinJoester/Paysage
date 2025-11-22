# 书库分类系统重设计 - 需求文档

## 简介

本功能旨在重新设计侧边栏书库页面，采用Material 3 Expressive (M3E)设计风格，实现漫画和阅读两大分类系统，提供独立的文件管理和在线阅读功能。

## 术语表

- **System**: Paysage阅读应用
- **Library**: 书库，用于管理和展示用户的书籍集合
- **Category**: 分类，指"漫画"或"阅读"两大主分类
- **Local Reading**: 本地阅读，从设备存储读取文件进行阅读
- **Online Reading**: 在线阅读，通过网络书源导入内容进行阅读
- **Book Source**: 书源，提供在线书籍内容的网络接口
- **M3E**: Material 3 Expressive，Material Design 3的表现力设计系统
- **Navigation Drawer**: 导航抽屉，侧边栏导航组件
- **Metadata**: 元数据，包括书籍标题、作者、进度等信息

## 需求

### 需求 1: M3E设计风格重构

**用户故事**: 作为用户，我希望书库界面采用现代化的M3E设计风格，以获得更加生动和富有表现力的视觉体验。

#### 验收标准

1. THE System SHALL 使用M3E设计规范中定义的配色方案（Primary: #FF6B35, Secondary: #6A4C9C, Tertiary: #00BFA5）
2. THE System SHALL 应用M3E形状系统中定义的圆角规格（卡片16dp，按钮12dp，对话框24dp）
3. THE System SHALL 使用ExpressiveCard、ExpressiveButton等M3E组件替换现有的标准Material组件
4. THE System SHALL 实现M3E动画系统中的Emphasized Easing缓动曲线（CubicBezier 0.2, 0.0, 0.0, 1.0）
5. THE System SHALL 确保所有交互元素具有M3E风格的悬停和按压动画效果

### 需求 2: 双分类系统

**用户故事**: 作为用户，我希望能够将漫画和小说分开管理，以便更好地组织我的阅读内容。

#### 验收标准

1. THE System SHALL 创建"漫画"和"阅读"两个主分类
2. WHEN 用户打开书库页面, THE System SHALL 默认显示本地功能视图
3. THE System SHALL 为每个分类提供独立的导航入口
4. THE System SHALL 在导航抽屉中清晰展示两个分类选项
5. WHEN 用户选择某个分类, THE System SHALL 仅显示该分类下的书籍

### 需求 3: 独立文件管理

**用户故事**: 作为用户，我希望每个分类能够独立管理文件，避免不同类型内容的混淆。

#### 验收标准

1. THE System SHALL 为"漫画"分类提供独立的文件夹选择功能
2. THE System SHALL 为"阅读"分类提供独立的文件夹选择功能
3. THE System SHALL 为每个分类维护独立的数据库表或标识字段
4. WHEN 用户在"漫画"分类中添加文件, THE System SHALL 仅在"漫画"分类中显示该文件
5. WHEN 用户在"阅读"分类中添加文件, THE System SHALL 仅在"阅读"分类中显示该文件

### 需求 4: 独立元数据管理

**用户故事**: 作为用户，我希望漫画和小说的元数据能够分开存储，确保数据的准确性和独立性。

#### 验收标准

1. THE System SHALL 为"漫画"分类的书籍存储独立的元数据（标题、作者、进度、封面等）
2. THE System SHALL 为"阅读"分类的书籍存储独立的元数据
3. THE System SHALL 确保两个分类的元数据不会相互干扰
4. WHEN 用户更新某个分类中书籍的元数据, THE System SHALL 仅更新该分类的数据库记录
5. THE System SHALL 支持为不同分类设置不同的元数据字段（如漫画的章节数、小说的字数）

### 需求 5: 在线阅读功能

**用户故事**: 作为用户，我希望在每个分类下都能使用在线阅读功能，通过网络书源获取内容。

#### 验收标准

1. THE System SHALL 在"漫画"分类中提供"源阅读"功能入口
2. THE System SHALL 在"阅读"分类中提供"源阅读"功能入口
3. WHEN 用户点击"源阅读"入口, THE System SHALL 显示书源管理界面
4. THE System SHALL 支持用户添加、编辑和删除书源
5. WHEN 用户通过书源导入内容, THE System SHALL 将内容关联到当前分类

### 需求 6: 本地与在线阅读切换

**用户故事**: 作为用户，我希望能够在本地阅读和在线阅读之间无缝切换，获得统一的阅读体验。

#### 验收标准

1. THE System SHALL 在分类视图中同时显示本地书籍和在线书籍
2. THE System SHALL 为本地书籍和在线书籍提供视觉区分标识
3. WHEN 用户点击本地书籍, THE System SHALL 从本地存储加载内容
4. WHEN 用户点击在线书籍, THE System SHALL 从网络书源加载内容
5. THE System SHALL 为在线书籍提供缓存机制以支持离线阅读

### 需求 7: 导航结构优化

**用户故事**: 作为用户，我希望导航结构清晰直观，能够快速访问不同的功能模块。

#### 验收标准

1. THE System SHALL 在导航抽屉顶部显示"本地功能"和"在线功能"两个主入口
2. WHEN 用户选择"本地功能", THE System SHALL 展开显示"漫画"和"阅读"两个分类
3. WHEN 用户选择"在线功能", THE System SHALL 展开显示"源阅读"相关选项
4. THE System SHALL 使用M3E风格的展开/收起动画
5. THE System SHALL 高亮显示当前选中的导航项

### 需求 8: 响应式布局

**用户故事**: 作为用户，我希望书库界面能够适配不同的设备尺寸，在手机和平板上都有良好的显示效果。

#### 验收标准

1. WHEN 设备屏幕宽度小于600dp, THE System SHALL 使用单列布局
2. WHEN 设备屏幕宽度在600dp到840dp之间, THE System SHALL 使用双列布局
3. WHEN 设备屏幕宽度大于840dp, THE System SHALL 使用三列布局
4. THE System SHALL 根据屏幕方向自动调整布局
5. THE System SHALL 确保所有触摸目标尺寸不小于48dp

### 需求 9: 数据库性能优化

**用户故事**: 作为用户，我希望书库能够快速加载和响应，即使有大量书籍也不会卡顿。

#### 验收标准

1. THE System SHALL 为分类字段创建数据库索引
2. THE System SHALL 使用分页加载机制，每次加载不超过50本书籍
3. WHEN 用户滚动到列表底部, THE System SHALL 自动加载下一页内容
4. THE System SHALL 在后台线程执行数据库查询操作
5. WHEN 数据库查询时间超过100ms, THE System SHALL 显示加载指示器

### 需求 10: 交互动效

**用户故事**: 作为用户，我希望界面交互具有流畅的动画效果，提升使用体验。

#### 验收标准

1. WHEN 用户点击分类切换按钮, THE System SHALL 使用300ms的淡入淡出动画切换内容
2. WHEN 用户展开或收起导航项, THE System SHALL 使用200ms的展开/收起动画
3. WHEN 用户点击书籍卡片, THE System SHALL 显示0.95倍缩放的按压效果
4. THE System SHALL 为所有列表项添加进入动画（从下方滑入）
5. THE System SHALL 使用Emphasized Easing缓动曲线确保动画自然流畅

### 需求 11: 默认视图设置

**用户故事**: 作为用户，我希望打开书库时默认显示本地功能页面，快速访问我的本地书籍。

#### 验收标准

1. WHEN 用户首次打开应用, THE System SHALL 默认显示"本地功能"视图
2. WHEN 用户在"本地功能"视图中, THE System SHALL 默认显示"漫画"分类
3. THE System SHALL 记住用户上次访问的分类
4. WHEN 用户重新打开应用, THE System SHALL 恢复到上次访问的分类
5. THE System SHALL 在设置中提供"默认分类"选项供用户自定义

### 需求 12: 可访问性支持

**用户故事**: 作为有视觉障碍的用户，我希望能够使用屏幕阅读器访问书库的所有功能。

#### 验收标准

1. THE System SHALL 为所有交互元素提供有意义的contentDescription
2. THE System SHALL 确保所有文本颜色与背景的对比度不低于4.5:1
3. THE System SHALL 支持系统字体大小设置
4. THE System SHALL 为所有图标按钮提供文字标签
5. THE System SHALL 确保键盘导航能够访问所有功能
