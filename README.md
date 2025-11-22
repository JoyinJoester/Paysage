# Paysage - 漫画阅读器

## 📖 项目简介

Paysage 是一款功能强大的 Android 漫画和 PDF 阅读器应用，支持多种格式的漫画文件和文档。

## ✨ 核心功能

### 支持格式
- **PDF**: PDF 文档
- **漫画格式**: CBZ, CBR, CBT, CB7
- **压缩包**: ZIP, RAR, TAR, 7Z

### 书库管理
- 自动扫描本地文件
- 封面自动提取
- 书籍分类和标签
- 收藏功能
- 搜索功能
- 阅读统计

### 阅读功能
- 单页/双页/连续滚动模式
- 左右/垂直翻页方向
- 缩放和平移
- 书签管理
- 阅读进度自动保存
- 手势控制（点击、滑动）

## 🏗️ 技术架构

### MVVM 架构
```
UI Layer (Jetpack Compose)
    ↓
ViewModel Layer
    ↓
Repository Layer
    ↓
Data Layer (Room + File System)
```

### 核心技术栈
- **UI**: Jetpack Compose + Material Design 3
- **架构**: MVVM + Repository Pattern
- **数据库**: Room
- **导航**: Navigation Compose
- **图片加载**: Coil
- **PDF 渲染**: PdfRenderer (Android)
- **压缩文件**: Java ZipFile
- **异步**: Kotlin Coroutines + Flow
- **依赖注入**: 手动注入

## 📁 项目结构

```
app/src/main/java/takagi/ru/paysage/
├── data/                      # 数据层
│   ├── model/                 # 数据模型
│   │   ├── Book.kt           # 书籍实体
│   │   ├── Bookmark.kt       # 书签实体
│   │   ├── ReadingProgress.kt# 阅读进度
│   │   └── Category.kt       # 分类
│   ├── dao/                   # 数据访问对象
│   │   ├── BookDao.kt
│   │   ├── BookmarkDao.kt
│   │   ├── ReadingProgressDao.kt
│   │   └── CategoryDao.kt
│   ├── Converters.kt         # 类型转换器
│   └── PaysageDatabase.kt    # 数据库
│
├── repository/                # 仓储层
│   ├── BookRepository.kt     # 书库管理
│   ├── BookmarkRepository.kt # 书签管理
│   └── ReadingProgressRepository.kt
│
├── viewmodel/                 # ViewModel 层
│   ├── LibraryViewModel.kt   # 书库 VM
│   └── ReaderViewModel.kt    # 阅读器 VM
│
├── ui/                        # UI 层
│   ├── screens/              # 页面
│   │   ├── LibraryScreen.kt  # 书库界面
│   │   └── ReaderScreen.kt   # 阅读器界面
│   └── theme/                # 主题
│
├── utils/                     # 工具类
│   ├── FileParser.kt         # 文件解析
│   └── FileScanner.kt        # 文件扫描
│
├── navigation/                # 导航
│   └── Screen.kt             # 路由定义
│
└── MainActivity.kt            # 主入口
```

## 🚀 开始使用

### 环境要求
- Android Studio Hedgehog 或更高版本
- JDK 8+
- Android SDK 34
- 最低 Android 10 (API 29)

### 构建步骤

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd Paysage
   ```

2. **打开项目**
   - 使用 Android Studio 打开项目
   - 等待 Gradle 同步完成

3. **运行应用**
   - 连接 Android 设备或启动模拟器
   - 点击 Run 按钮

### 首次运行

1. **授予权限**: 应用会请求存储权限
2. **扫描文件**: 点击右下角的刷新按钮扫描本地漫画
3. **开始阅读**: 点击书籍封面开始阅读

## 📊 数据库设计

### books 表
- 存储书籍基本信息
- 封面路径、页数、阅读进度
- 分类、标签、收藏状态

### bookmarks 表
- 书签页码和标题
- 外键关联 books 表

### reading_progress 表
- 当前页码、缩放级别
- 阅读模式、翻页方向
- 滚动位置

### categories 表
- 分类名称和描述
- 书籍数量统计

## 🔧 核心功能实现

### 文件解析
`FileParser.kt` 负责：
- PDF 页面渲染（PdfRenderer）
- ZIP 压缩包解析
- 图片提取和封面生成

### 文件扫描
`FileScanner.kt` 负责：
- 递归扫描指定目录
- 过滤支持的文件格式
- 生成扫描结果

### 阅读器
`ReaderScreen.kt` 实现：
- 手势缩放和平移
- 左右滑动翻页
- 点击显示/隐藏 UI
- 进度条拖动跳页

## 🎨 UI 设计

### Material Design 3
- 动态主题色
- 深色模式支持
- Material 3 组件

### 响应式布局
- 网格布局（书库）
- 自适应列数
- 平板优化

## 🔐 权限说明

### 必需权限
- `READ_EXTERNAL_STORAGE`: 读取存储中的漫画文件
- `READ_MEDIA_IMAGES`: Android 13+ 读取图片

### 可选权限
- `MANAGE_EXTERNAL_STORAGE`: 访问所有文件（需用户手动授予）

## 🐛 已知问题

1. **RAR/7Z 支持有限**: 目前仅支持 ZIP 格式的压缩包，RAR 和 7Z 需要额外库支持
2. **大文件性能**: 大型 PDF 文件可能加载较慢
3. **内存管理**: 连续浏览大量页面可能导致内存不足

## 📝 TODO

### 高优先级
- [ ] 实现 RAR/7Z 解压支持（使用 junrar 或 sevenz）
- [ ] 添加封面缓存机制（Coil 集成）
- [ ] 实现设置界面
- [ ] 添加阅读历史记录

### 中优先级
- [ ] 双页模式实现
- [ ] 连续滚动模式
- [ ] 夜间模式/亮度调节
- [ ] 书签缩略图
- [ ] 导出/导入数据库

### 低优先级
- [ ] 云同步功能
- [ ] 在线漫画源
- [ ] 阅读统计图表
- [ ] 主题自定义

## 🔄 未来规划

### v1.1
- 完善压缩格式支持
- 优化性能和内存使用
- 添加设置页面

### v1.2
- 双页和连续滚动模式
- 阅读历史和统计
- 导入导出功能

### v2.0
- 云同步备份
- 平板优化
- Widget 支持

## 📄 许可证

本项目采用 GPL-3.0 许可证

## 👥 贡献

欢迎提交 Issue 和 Pull Request！

## 📞 联系方式

如有问题或建议，请通过 GitHub Issues 联系。

---

**版本**: 1.0.0  
**最后更新**: 2025-10-25
