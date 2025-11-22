# Requirements Document

## Introduction

本文档定义了书库同步下拉刷新功能的需求规范。该功能允许用户通过下拉手势触发书库同步操作，并通过Material 3 Extended风格的模态对话框配置同步选项。系统支持增量同步、完整同步和维护操作三种模式，提供灵活的文件类型过滤和同步策略配置。

## Glossary

- **LibraryScreen**: 应用程序的书库主界面，显示用户的漫画和书籍集合
- **PullToRefresh**: 下拉刷新组件，用户通过下拉手势触发刷新操作
- **SyncDialog**: 书库同步配置对话框，提供同步选项和操作按钮
- **IncrementalSync**: 增量同步操作，仅同步新增和修改的文件
- **FullSync**: 完整同步操作，扫描所有文件并重建书库索引
- **MaintenanceOperation**: 维护操作，包括清理已删除文件、更新修改文件和生成缩略图
- **M3E**: Material 3 Extended，Material Design 3的扩展设计规范
- **CircularProgressIndicator**: 圆形进度指示器，用于显示加载状态
- **FileTypeFilter**: 文件类型过滤器，定义支持的漫画和压缩文件格式
- **SyncOptions**: 同步选项配置，包括维护选项和扫描策略

## Requirements

### Requirement 1

**User Story:** 作为用户，我希望通过下拉手势触发书库同步，以便快速刷新书库内容

#### Acceptance Criteria

1. WHEN 用户在LibraryScreen上执行下拉手势，THE PullToRefresh SHALL 显示M3E风格的CircularProgressIndicator
2. WHEN 用户下拉距离超过阈值并释放，THE PullToRefresh SHALL 触发SyncDialog的显示
3. WHILE 用户正在下拉但未释放，THE CircularProgressIndicator SHALL 根据下拉距离动态调整透明度和缩放比例
4. WHEN 下拉距离未达到阈值时用户释放，THE PullToRefresh SHALL 回弹到初始状态且不触发任何操作
5. THE CircularProgressIndicator SHALL 使用MaterialTheme.colorScheme.primary作为颜色

### Requirement 2

**User Story:** 作为用户，我希望在同步对话框中查看支持的文件类型，以便了解系统能够识别哪些格式

#### Acceptance Criteria

1. WHEN SyncDialog显示时，THE SyncDialog SHALL 包含名为"文件类型"的卡片区域
2. THE "文件类型"卡片 SHALL 包含"漫画文件"区块，显示支持的漫画格式列表（.cbz, .cbr, .cbt, .cb7）
3. THE "文件类型"卡片 SHALL 包含"压缩文件"区块，显示支持的压缩格式列表（.zip, .rar, .7z, .tar）
4. THE 格式列表 SHALL 使用只读文本显示，不可编辑
5. THE "文件类型"卡片 SHALL 使用MaterialTheme.shapes.medium作为圆角形状

### Requirement 3

**User Story:** 作为用户，我希望配置维护选项，以便在同步时执行清理和优化操作

#### Acceptance Criteria

1. WHEN SyncDialog显示时，THE SyncDialog SHALL 包含名为"维护"的卡片区域
2. THE "维护"卡片 SHALL 包含复选框"移出已删除文件"，默认状态为未选中
3. THE "维护"卡片 SHALL 包含复选框"从修改过的文件更新数据"，默认状态为未选中
4. THE "维护"卡片 SHALL 包含复选框"生成已删除的缩略图"，默认状态为未选中
5. WHEN 用户点击任一复选框，THE 复选框 SHALL 切换选中状态并显示波纹反馈效果
6. THE 复选框 SHALL 使用MaterialTheme.colorScheme.primary作为选中状态颜色

### Requirement 4

**User Story:** 作为用户，我希望配置扫描策略，以便控制同步的范围和性能

#### Acceptance Criteria

1. WHEN SyncDialog显示时，THE SyncDialog SHALL 包含名为"更多"的卡片区域
2. THE "更多"卡片 SHALL 包含复选框"扫描子文件夹"，默认状态为选中
3. THE "更多"卡片 SHALL 包含复选框"跳过被定义为隐藏的库文件夹"，默认状态为选中
4. THE "更多"卡片 SHALL 包含复选框"并行同步"，默认状态为未选中
5. WHEN 用户点击任一复选框，THE 复选框 SHALL 切换选中状态并显示波纹反馈效果
6. THE 复选框 SHALL 使用MaterialTheme.colorScheme.primary作为选中状态颜色

### Requirement 5

**User Story:** 作为用户，我希望通过不同的按钮执行不同类型的同步操作，以便根据需求选择合适的同步方式

#### Acceptance Criteria

1. THE SyncDialog SHALL 在底部固定显示三个操作按钮："维护"、"同步"和"开始完整同步"
2. WHEN 用户点击"维护"按钮，THE SyncDialog SHALL 执行MaintenanceOperation并关闭对话框
3. WHEN 用户点击"同步"按钮，THE SyncDialog SHALL 执行IncrementalSync并关闭对话框
4. WHEN 用户点击"开始完整同步"按钮，THE SyncDialog SHALL 执行FullSync并关闭对话框
5. THE 按钮区域 SHALL 与内容区保持16dp的间距
6. WHEN 用户点击任一按钮，THE 按钮 SHALL 显示波纹反馈效果

### Requirement 6

**User Story:** 作为用户，我希望对话框符合Material Design 3规范，以便获得一致的视觉体验

#### Acceptance Criteria

1. THE SyncDialog SHALL 宽度为屏幕宽度的80%，最大宽度不超过600dp
2. THE SyncDialog SHALL 使用MaterialTheme.shapes.extraLarge作为对话框圆角形状
3. THE 卡片之间 SHALL 保持16dp的垂直间距
4. THE 卡片内部元素 SHALL 保持8dp的垂直间距
5. THE SyncDialog SHALL 使用MaterialTheme.colorScheme.surface作为背景颜色
6. THE SyncDialog SHALL 支持浅色和深色主题自动适配

### Requirement 7

**User Story:** 作为用户，我希望在同步过程中看到进度反馈，以便了解操作状态

#### Acceptance Criteria

1. WHEN IncrementalSync或FullSync开始执行，THE LibraryScreen SHALL 显示CircularProgressIndicator在屏幕中央
2. WHEN 同步操作完成，THE LibraryScreen SHALL 隐藏CircularProgressIndicator
3. WHEN 同步操作完成，THE LibraryScreen SHALL 在底部显示Snackbar提示同步结果
4. THE Snackbar SHALL 显示新增书籍数量和更新书籍数量
5. THE Snackbar SHALL 包含"确定"按钮用于关闭提示

### Requirement 8

**User Story:** 作为用户，我希望所有交互元素都有适当的反馈效果，以便确认我的操作已被识别

#### Acceptance Criteria

1. WHEN 用户点击复选框，THE 复选框 SHALL 显示波纹反馈效果，持续时间为300ms
2. WHEN 用户点击按钮，THE 按钮 SHALL 显示波纹反馈效果，持续时间为300ms
3. THE 波纹效果 SHALL 使用MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)作为颜色
4. WHEN 用户点击对话框外部区域，THE SyncDialog SHALL 关闭
5. THE 对话框关闭动画 SHALL 持续时间为250ms

### Requirement 9

**User Story:** 作为用户，我希望同步选项能够被持久化保存，以便下次打开对话框时保留我的偏好设置

#### Acceptance Criteria

1. WHEN 用户修改任一复选框状态，THE SyncOptions SHALL 将状态保存到本地存储
2. WHEN SyncDialog再次显示，THE 复选框 SHALL 恢复上次保存的状态
3. THE SyncOptions SHALL 使用DataStore进行持久化存储
4. WHEN 存储操作失败，THE SyncDialog SHALL 使用默认配置且不影响用户操作
5. THE 存储操作 SHALL 在后台线程执行，不阻塞UI线程

### Requirement 10

**User Story:** 作为用户，我希望对话框支持无障碍访问，以便使用辅助技术的用户也能正常使用

#### Acceptance Criteria

1. THE SyncDialog中的所有复选框 SHALL 包含contentDescription属性
2. THE SyncDialog中的所有按钮 SHALL 包含contentDescription属性
3. WHEN 使用TalkBack时，THE 复选框 SHALL 朗读标签文本和当前状态
4. WHEN 使用TalkBack时，THE 按钮 SHALL 朗读按钮文本和操作说明
5. THE 对话框 SHALL 支持键盘导航，Tab键可在元素间切换
