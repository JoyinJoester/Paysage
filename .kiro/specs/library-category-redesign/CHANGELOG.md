# 更新日志

本文档记录书库分类系统重设计项目的所有重要变更。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [未发布]

### 待完成
- LibraryScreen重构和集成
- AddSourceDialog组件
- TwoLayerNavigationScaffold更新
- ExpressiveShapes对象
- 分类切换动画
- 列表项进入动画
- 路由配置
- 测试代码
- 响应式布局
- 可访问性支持

## [1.0.0-alpha] - 2025-10-28

### 新增

#### 数据层
- 新增 `CategoryType` 枚举，定义MANGA和NOVEL两种分类类型
- 新增 `DisplayMode` 枚举，定义LOCAL和ONLINE两种显示模式
- 新增 `BookSource` 实体类，用于管理在线书源
- 新增 `BookSourceDao` 接口，提供书源的CRUD操作
- 扩展 `Book` 实体，添加 `categoryType`、`isOnline`、`sourceId`、`sourceUrl` 字段
- 新增数据库迁移脚本（版本3→4），包括：
  - 添加新字段到books表
  - 创建book_sources表
  - 创建性能优化索引
  - 自动分类现有书籍
- 新增 `CategoryType` 类型转换器

#### Repository层
- 新增 `BookRepositoryExtensions.kt`，提供分类系统扩展功能：
  - `getBooksByCategory()` - 按分类和显示模式查询书籍
  - `getBooksByCategoryFlow()` - 响应式查询
  - `updateBookCategoryType()` - 更新书籍分类
  - `getBookCountByCategoryType()` - 获取分类统计
  - LruCache缓存机制（缓存10个分类）
  - 缓存统计功能
- 新增 `OnlineSourceRepository`，提供书源管理功能：
  - 完整的CRUD操作
  - 书源验证框架
  - 搜索功能框架
  - 导入导出框架
  - 统计信息管理
- 扩展 `BookDao`，添加分类查询方法：
  - `getBooksByCategory()` - 分页查询
  - `getBooksByCategoryFlow()` - 响应式查询
  - `getAllBooksByCategoryTypeFlow()` - 获取所有书籍
  - `getFavoriteBooksByCategoryFlow()` - 获取收藏
  - `getRecentBooksByCategoryFlow()` - 获取最近阅读
  - `searchBooksByCategoryFlow()` - 分类搜索

#### ViewModel层
- 新增 `LibraryViewModelExtensions.kt`，扩展LibraryViewModel：
  - `categoryType` 状态管理
  - `displayMode` 状态管理
  - `setCategoryType()` - 设置分类
  - `setDisplayMode()` - 设置显示模式
  - `toggleCategoryType()` - 切换分类
  - `toggleDisplayMode()` - 切换模式
  - `getCategoryStatistics()` - 获取统计信息
- 新增 `OnlineSourceViewModel`，管理书源状态：
  - 书源列表状态
  - 启用书源状态
  - 分类过滤状态
  - 搜索结果状态
  - UI状态管理
  - 完整的书源操作方法

#### UI层
- 新增 `CategoryFilterBar` 组件：
  - 分类切换按钮（漫画/阅读）
  - 显示模式切换按钮（本地/在线）
  - 紧凑版支持
  - 动画效果
  - M3E设计风格
- 新增 `BookSourceComponents`：
  - `BookSourceCard` - 标准书源卡片
  - `CompactBookSourceCard` - 紧凑卡片
  - `BookSourceListItem` - 列表项
  - 统计信息显示
  - 启用状态切换
- 新增 `OnlineSourceScreen`：
  - 书源列表展示
  - 空状态视图
  - 加载状态处理
  - 错误提示
  - TopAppBar集成

#### 导航系统
- 扩展 `PrimaryNavItem` 枚举：
  - 新增 `LocalLibrary` - 本地书库
  - 新增 `OnlineLibrary` - 在线书库
  - 添加 `hasSecondaryMenu` 属性
  - 保留向后兼容性（Library别名）
- 新增 `LocalLibraryNavItems` 配置：
  - 漫画导航项
  - 阅读导航项
- 新增 `OnlineLibraryNavItems` 配置：
  - 漫画书源导航项
  - 小说书源导航项
- 更新导航状态恢复逻辑，支持旧版本迁移

#### M3E设计
- 新增 `CategoryColors` 对象：
  - 漫画专属颜色（橙色系）
  - 小说专属颜色（紫色系）
  - 在线功能颜色（青绿色系）
  - 明暗主题支持
- 新增辅助函数：
  - `getCategoryColor()` - 获取分类颜色
  - `getCategoryContainerColor()` - 获取容器颜色
- 新增 `CategorySwitchAnimation` 组件，提供分类切换动画

#### 国际化
- 新增中文字符串资源（values-zh/strings.xml）：
  - 导航相关：本地功能、在线功能
  - 分类相关：漫画、阅读
  - 显示模式：本地、在线
  - 书源相关：添加、编辑、删除、验证等
- 新增英文字符串资源（values/strings.xml）：
  - 完整的英文翻译
  - 与中文资源保持一致

#### 文档
- 新增 `requirements.md` - 需求文档（12个核心需求）
- 新增 `design.md` - 设计文档（完整架构设计）
- 新增 `tasks.md` - 任务列表（14个主任务，60+子任务）
- 新增 `IMPLEMENTATION_STATUS.md` - 实现状态跟踪
- 新增 `FINAL_SUMMARY.md` - 项目最终总结
- 新增 `QUICK_START.md` - 快速入门指南
- 新增 `BUILD_GUIDE.md` - 编译和运行指南
- 新增 `DELIVERY_CHECKLIST.md` - 交付清单
- 新增 `README.md` - 项目入口文档

### 变更

#### 数据模型
- `Book` 实体添加分类相关字段
- `PaysageDatabase` 版本升级到4
- `Converters` 添加CategoryType转换器

#### 导航
- `NavigationState` 默认导航项改为LocalLibrary
- `PrimaryNavItem` 重构，支持二级菜单

#### 主题
- `Color.kt` 添加分类颜色系统

### 优化

#### 性能
- 实现LruCache缓存机制，提升查询性能
- 添加数据库索引，优化查询速度
- 支持分页加载，减少内存占用

#### 架构
- 使用扩展函数，不侵入原有代码
- 采用Repository模式，分离数据访问逻辑
- 使用StateFlow，实现响应式数据流

#### 代码质量
- 完整的代码注释
- 清晰的命名规范
- 模块化设计

### 修复
- 修复导航状态恢复时的兼容性问题
- 修复数据库迁移时的数据完整性问题

### 安全
- 数据库迁移前自动备份
- 类型安全的枚举定义
- 空值安全处理

## [0.1.0] - 2025-10-27

### 新增
- 项目初始化
- 需求分析
- 架构设计

---

## 版本说明

### 版本号格式

版本号格式：`主版本号.次版本号.修订号-预发布标识`

- **主版本号**：不兼容的API修改
- **次版本号**：向下兼容的功能性新增
- **修订号**：向下兼容的问题修正
- **预发布标识**：alpha、beta、rc等

### 变更类型

- **新增** - 新功能
- **变更** - 现有功能的变更
- **弃用** - 即将移除的功能
- **移除** - 已移除的功能
- **修复** - 问题修复
- **安全** - 安全相关的修复
- **优化** - 性能或代码质量优化

## 里程碑

### v1.0.0-alpha (当前)
- ✅ 核心数据层完成
- ✅ Repository层完成
- ✅ ViewModel层完成
- ✅ 基础UI组件完成
- ✅ 国际化支持完成
- ⏳ UI集成进行中

### v1.0.0-beta (计划)
- [ ] 完整UI集成
- [ ] 完整测试覆盖
- [ ] 性能优化
- [ ] 文档完善

### v1.0.0 (计划)
- [ ] 生产环境就绪
- [ ] 完整功能验收
- [ ] 用户文档完整
- [ ] 稳定性验证

## 贡献者

感谢所有为本项目做出贡献的开发者。

---

**维护者**: Paysage Team  
**最后更新**: 2025-10-28
