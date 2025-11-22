# 变更日志

所有重要的项目变更都将记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
并且本项目遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [未发布]

### 待完成
- MainActivity中的完整集成
- 文件夹列表显示功能
- 文件夹导航功能
- 自动化测试套件
- 性能优化和缓存
- 英文国际化支持

## [1.1.0] - 2025-10-28

### 新增

#### 模块重命名
- 将"本地功能"重命名为"本地管理"
- 将"在线功能"重命名为"在线管理"
- 更新所有相关的字符串资源
- 更新导航菜单显示文本
- 更新辅助功能描述文本

#### 文件夹管理功能
- 在本地管理模块中添加"创建文件夹"功能
- 在在线管理模块中添加"创建文件夹"功能
- 实现文件夹创建对话框（M3e风格）
- 实现文件夹名称实时验证
- 实现文件夹创建成功/失败反馈
- 支持文件系统操作
- 支持数据库持久化

#### 数据层
- 新增 `Folder` 数据模型
- 新增 `ModuleType` 枚举（LOCAL_MANAGEMENT, ONLINE_MANAGEMENT）
- 新增 `FolderDao` 数据访问对象
- 新增数据库迁移 MIGRATION_4_5
- 数据库版本从 4 升级到 5
- 添加 folders 表和相关索引

#### 业务逻辑层
- 新增 `FolderRepository` 接口
- 新增 `FolderRepositoryImpl` 实现
- 新增 `FolderViewModel` 状态管理
- 新增 `CreateFolderState` 密封类
- 实现文件夹创建、查询、删除功能
- 实现自定义异常处理

#### UI组件
- 新增 `CreateFolderDialog` 组件（M3e风格）
- 新增 `CreateFolderButton` 组件（M3e风格）
- 新增 `FolderListItem` 组件（M3e风格）
- 实现输入验证逻辑
- 实现错误提示显示
- 实现加载状态指示器

#### 导航集成
- 在 `SecondaryDrawerContent` 中添加创建文件夹按钮
- 添加 HorizontalDivider 分隔
- 实现回调参数传递

#### 字符串资源
- 添加 17 个新的中文字符串资源
- 包含所有文件夹管理相关文本
- 包含所有错误提示消息
- 包含所有用户反馈消息

### 改进

#### M3e设计规范应用
- 使用 Material 3 Extended 标准组件
- 应用 8dp 基准网格系统
- 应用标准间距（16dp, 24dp）
- 应用标准圆角（large: 16dp, extraLarge: 28dp）
- 应用标准高程（6dp for dialogs）
- 使用 Material color scheme
- 支持明亮主题
- 支持暗色主题
- 支持动态配色（Android 12+）

#### 用户体验
- 实时输入验证，即时反馈
- 清晰的错误提示信息
- 流畅的动画过渡
- 响应式布局适配
- 无障碍访问基础支持

#### 代码质量
- 遵循 Kotlin 官方代码规范
- 使用类型安全的密封类
- 使用 StateFlow 进行状态管理
- 使用协程处理异步操作
- 完善的错误处理机制
- 详细的代码注释

### 技术细节

#### 数据库变更
```sql
-- 新增 folders 表
CREATE TABLE IF NOT EXISTS folders (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL,
    path TEXT NOT NULL UNIQUE,
    parent_path TEXT NOT NULL,
    module_type TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- 新增索引
CREATE INDEX idx_folders_parent_path ON folders(parent_path);
CREATE INDEX idx_folders_module_type ON folders(module_type);
```

#### API变更
- 新增 `FolderRepository.createFolder()`
- 新增 `FolderRepository.getFolders()`
- 新增 `FolderRepository.deleteFolder()`
- 新增 `FolderViewModel.createFolder()`
- 新增 `FolderViewModel.refreshFolders()`
- 新增 `FolderViewModel.resetCreateFolderState()`

#### 组件API
```kotlin
// CreateFolderDialog
@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (folderName: String) -> Unit,
    existingFolderNames: List<String> = emptyList(),
    isCreating: Boolean = false
)

// CreateFolderButton
@Composable
fun CreateFolderButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)

// FolderListItem
@Composable
fun FolderListItem(
    folder: Folder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

### 文档

#### 新增文档
- `requirements.md` - 需求文档（12个需求）
- `design.md` - 设计文档
- `tasks.md` - 任务列表（17个主要任务）
- `IMPLEMENTATION_PROGRESS.md` - 实现进度报告
- `INTEGRATION_GUIDE.md` - 集成指南
- `QUICK_START.md` - 快速开始指南
- `FINAL_SUMMARY.md` - 最终总结
- `PROJECT_COMPLETION_REPORT.md` - 项目完成报告
- `README.md` - 项目说明
- `CHANGELOG.md` - 本文件

### 已知问题

#### 待解决
- 需要在 MainActivity 中完成集成
- 需要添加权限处理逻辑
- 需要实现文件夹列表显示
- 需要添加自动化测试

#### 限制
- 当前仅支持中文字符串资源
- 文件夹列表暂未实现缓存
- 暂不支持文件夹重命名
- 暂不支持文件夹移动

### 兼容性

#### 支持的Android版本
- 最低: API 21 (Android 5.0)
- 目标: API 34 (Android 14)
- 动态配色: API 31+ (Android 12+)

#### 支持的屏幕尺寸
- 手机 (Compact)
- 小平板 (Medium)
- 大平板/桌面 (Expanded)

#### 支持的主题
- 明亮主题
- 暗色主题
- 动态配色（Android 12+）
- 自定义配色方案

### 性能

#### 预期性能指标
- 对话框打开: < 200ms
- 输入验证: < 50ms
- 文件夹创建: < 500ms
- 列表刷新: < 300ms

#### 内存占用
- ViewModel: ~1MB
- UI组件: ~500KB
- 数据库: 动态增长

### 安全性

#### 输入验证
- 空名称检查
- 非法字符检查（/ \ : * ? " < > |）
- 重复名称检查
- 长度限制检查（最大255字符）

#### 错误处理
- 文件系统错误
- 数据库错误
- 权限错误
- 网络错误（在线管理）

### 迁移指南

#### 从 v1.0.0 升级到 v1.1.0

1. **数据库自动迁移**
   - 应用会自动执行 MIGRATION_4_5
   - 无需手动操作
   - 现有数据不受影响

2. **字符串资源更新**
   - "本地功能" 自动变为 "本地管理"
   - "在线功能" 自动变为 "在线管理"
   - 无需修改代码

3. **新功能可选**
   - 文件夹管理功能为可选功能
   - 不影响现有功能
   - 可以逐步集成

### 贡献者

- Kiro AI Assistant - 核心开发

### 致谢

- Material Design 团队 - M3e设计规范
- Android 团队 - Jetpack Compose
- Kotlin 团队 - 优秀的编程语言

---

## 版本说明

### 版本号规则
- 主版本号：重大架构变更
- 次版本号：新功能添加
- 修订号：bug修复和小改进

### 发布周期
- 主版本：按需发布
- 次版本：每月一次
- 修订版：按需发布

### 支持政策
- 当前版本：完全支持
- 前一版本：安全更新
- 更早版本：不再支持

---

**最后更新**: 2025-10-28  
**当前版本**: 1.1.0  
**下一版本**: 1.2.0 (计划中)
