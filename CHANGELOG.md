# Paysage 更新日志

所有显著的项目变更都将记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/),
并且本项目遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

---

## [0.2.0] - 2024-XX-XX

### 🎨 新增
- **Material Design 3 Expressive 设计系统**
  - 全新的颜色方案 (橙色/紫色/青绿)
  - 完整的排版系统 (13 个文本样式)
  - 形状系统 (6 种圆角定义)
  - 动画规范 (强调曲线、弹性动画)

- **交互式组件库** (`ExpressiveComponents.kt`)
  - ExpressiveCard - 悬停放大、按压缩小、动态高程
  - ExpressiveButton - 按压缩放、旋转效果
  - ExpressiveFAB - 悬停放大、按下缩小、旋转动画
  - ExpressiveChip - 选中放大、边框颜色变化
  - ExpressiveIconButton - 悬停/按压缩放、旋转交互

- **文档体系**
  - `M3_EXPRESSIVE_DESIGN.md` - 完整设计规范
  - `ITERATION_SUMMARY.md` - 迭代总结
  - `NEXT_STEPS.md` - 下一步开发计划
  - `CHANGELOG.md` - 更新日志

### 🔄 变更
- 升级书库界面 (LibraryScreen) 使用 Expressive 组件
- 升级阅读器界面 (ReaderScreen) 使用 Expressive 组件
- 优化按钮和图标的交互反馈
- 统一使用设计系统的尺寸和间距常量

### 🐛 修复
- 修复 `animateDpAsState` 类型不匹配问题
- 修复 `SelectableChipBorder` API 变更导致的编译错误
- 修复 `FilterChip` 参数传递问题
- 解决 Material 3 API 兼容性问题

### 🔧 技术改进
- 使用 `graphicsLayer` 优化动画性能
- 添加 `remember` 缓存 InteractionSource
- 优化状态动画的响应性
- 改进代码注释和文档

### ⚡ 性能
- 动画使用硬件加速
- 列表使用 LazyGrid 虚拟化
- 优化重组性能

---

## [0.1.0] - 2024-XX-XX

### 🎉 首次发布

#### 核心功能
- **MVVM 架构**
  - ViewModel 层 (LibraryViewModel, ReaderViewModel)
  - Repository 层 (BookRepository, ReadingProgressRepository, BookmarkRepository)
  - Data 层 (Room 数据库)

- **数据库设计**
  - Book 实体 (书籍信息)
  - Bookmark 实体 (书签)
  - ReadingProgress 实体 (阅读进度)
  - Category 实体 (分类)
  - 完整的 DAO 层

- **文件支持**
  - PDF 格式 (通过 PdfRenderer)
  - ZIP 格式 (通过 ZipFile)
  - CBZ 格式 (基于 ZIP)
  - CBT 格式 (基于 TAR)

- **UI 界面**
  - 书库界面 (网格布局、搜索、扫描)
  - 阅读器界面 (页面显示、手势操作、进度条)
  - 导航系统 (Compose Navigation)

- **用户交互**
  - 文件扫描功能
  - 书籍搜索 (带 300ms 防抖)
  - 页面翻页 (上一页/下一页/跳转)
  - 手势支持 (缩放、平移、滑动)
  - 书签添加/删除

#### 技术栈
```kotlin
Kotlin 1.9.0
Jetpack Compose BOM 2024.04.01
Material Design 3 1.2.1
Room 2.6.1
Navigation Compose 2.7.5
Coroutines 1.7.3
```

#### 项目结构
```
app/src/main/java/takagi/ru/paysage/
├── data/
│   ├── model/        # 数据模型
│   ├── dao/          # 数据访问对象
│   ├── database/     # 数据库配置
│   └── repository/   # 仓储层
├── viewmodel/        # 视图模型
├── ui/
│   ├── screens/      # 界面
│   ├── theme/        # 主题
│   └── navigation/   # 导航
├── utils/            # 工具类
└── MainActivity.kt   # 主入口
```

#### 文档
- `README.md` - 项目介绍
- `ARCHITECTURE.md` - 架构说明
- `QUICKSTART.md` - 快速开始指南

---

## [未发布]

### 计划中
- [ ] RAR 格式支持 (CBR)
- [ ] 7Z 格式支持 (CB7)
- [ ] 封面图片加载 (Coil)
- [ ] 书签管理界面
- [ ] 设置界面
- [ ] 阅读统计
- [ ] 云同步功能
- [ ] 主题自定义

---

## 版本说明

### 版本号规则
遵循 `MAJOR.MINOR.PATCH` 格式:
- **MAJOR**: 重大架构变更或不兼容更新
- **MINOR**: 新功能添加,向后兼容
- **PATCH**: Bug 修复和小改进

### 版本里程碑
- `0.1.0` - 初始版本 (MVVM 架构 + 基础功能)
- `0.2.0` - M3 Expressive 设计系统
- `0.3.0` - 格式完整支持
- `0.4.0` - 用户体验优化
- `0.5.0` - 高级功能
- `1.0.0` - 正式发布

---

## 类型说明

### 🎨 新增 (Added)
新功能、新特性

### 🔄 变更 (Changed)
现有功能的改进或修改

### ❌ 废弃 (Deprecated)
即将移除的功能

### 🗑️ 移除 (Removed)
已移除的功能

### 🐛 修复 (Fixed)
Bug 修复

### 🔒 安全 (Security)
安全相关的更新

### ⚡ 性能 (Performance)
性能优化

### 🔧 技术改进 (Technical)
代码重构、技术债务处理

---

## 贡献指南

### 提交消息格式
```
<类型>: <简短描述>

<详细描述>

<关联的 Issue>
```

**类型**:
- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 代码重构
- `perf`: 性能优化
- `test`: 测试相关
- `chore`: 构建或辅助工具变动

**示例**:
```
feat: 添加 RAR 格式支持

- 集成 junrar 库
- 实现 CBR 格式解析
- 更新 FileParser 支持 RAR

Closes #123
```

---

## 反馈渠道

如果您发现 Bug 或有功能建议,请:
1. 查看 [Issues](https://github.com/yourusername/Paysage/issues)
2. 创建新 Issue 并使用适当的标签
3. 提供详细的复现步骤或需求描述

---

**感谢使用 Paysage!** 📚✨

[未发布]: https://github.com/yourusername/Paysage/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/yourusername/Paysage/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/yourusername/Paysage/releases/tag/v0.1.0
