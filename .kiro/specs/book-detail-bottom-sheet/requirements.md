# 书籍详情底部弹窗需求文档

## 简介

本功能为 Paysage 应用添加书籍详情底部弹窗（Bottom Sheet），用户可以通过长按书籍卡片触发弹窗，查看书籍的详细信息并进行快速操作。

## 术语表

- **System**: Paysage 应用系统
- **Bottom Sheet**: 从屏幕底部向上滑出的模态弹窗组件
- **Book Card**: 书库界面中显示的书籍卡片组件
- **User**: 使用 Paysage 应用的用户
- **Reading Progress**: 书籍的阅读进度信息，包括当前页码和总页数
- **Tag**: 用户为书籍添加的分类标签
- **File Format**: 书籍文件的格式类型（如 ZIP、CBZ、PDF 等）

## 需求

### 需求 1：长按触发详情弹窗

**用户故事：** 作为用户，我想要长按书籍卡片时弹出详情页面，以便快速查看书籍信息和进行操作

#### 验收标准

1. WHEN User 长按 Book Card 超过 500 毫秒, THE System SHALL 显示 Bottom Sheet
2. WHEN Bottom Sheet 显示时, THE System SHALL 在 300 毫秒内完成展开动画
3. WHEN User 点击 Bottom Sheet 外部区域, THE System SHALL 关闭 Bottom Sheet
4. WHEN User 向下滑动 Bottom Sheet, THE System SHALL 关闭 Bottom Sheet
5. WHEN Bottom Sheet 打开时, THE System SHALL 阻止背景内容的交互

### 需求 2：显示书籍基本信息

**用户故事：** 作为用户，我想要在详情弹窗中看到书籍的基本信息，以便了解书籍的详细内容

#### 验收标准

1. WHEN Bottom Sheet 显示时, THE System SHALL 在顶部显示书籍封面图片
2. WHEN Bottom Sheet 显示时, THE System SHALL 在封面下方显示书籍标题
3. WHEN Bottom Sheet 显示时, THE System SHALL 显示文件格式标签
4. WHEN Bottom Sheet 显示时, THE System SHALL 显示文件大小（以 MB 或 GB 为单位）
5. WHEN 书籍有作者信息时, THE System SHALL 在编辑者区域显示作者名称
6. WHEN 书籍没有封面时, THE System SHALL 显示默认书籍图标

### 需求 3：显示阅读进度信息

**用户故事：** 作为用户，我想要在详情弹窗中看到阅读进度，以便了解我的阅读状态

#### 验收标准

1. WHEN 书籍的 currentPage 大于 0 时, THE System SHALL 显示当前页码和总页数
2. WHEN 书籍的 lastReadAt 不为空时, THE System SHALL 显示上次阅读时间
3. WHEN 书籍的 lastReadAt 为空时, THE System SHALL 显示"还未读过"文本
4. WHEN 书籍的 isFinished 为 true 时, THE System SHALL 显示"已读完"状态
5. THE System SHALL 使用进度条可视化显示阅读进度百分比

### 需求 4：标签管理功能

**用户故事：** 作为用户，我想要在详情弹窗中查看和编辑书籍标签，以便更好地组织我的书库

#### 验收标准

1. WHEN Bottom Sheet 显示时, THE System SHALL 显示标签编辑区域
2. WHEN 书籍有标签时, THE System SHALL 显示所有已添加的标签
3. WHEN 书籍没有标签时, THE System SHALL 显示"无 # 标签"占位文本
4. WHEN User 点击标签编辑按钮, THE System SHALL 打开标签编辑界面
5. THE System SHALL 允许 User 添加、删除和修改标签

### 需求 5：排序选项功能

**用户故事：** 作为用户，我想要在详情弹窗中调整书籍的排序方式，以便按照我的偏好查看书籍

#### 验收标准

1. WHEN Bottom Sheet 显示时, THE System SHALL 显示排序下拉菜单
2. THE System SHALL 提供至少 3 种排序选项（新的、标题、作者）
3. WHEN User 选择排序选项时, THE System SHALL 更新书籍的排序偏好
4. WHEN User 选择排序选项时, THE System SHALL 在 200 毫秒内显示选择反馈
5. THE System SHALL 保存 User 的排序偏好设置

### 需求 6：快速操作按钮

**用户故事：** 作为用户，我想要在详情弹窗中快速执行常用操作，以便提高使用效率

#### 验收标准

1. WHEN Bottom Sheet 显示时, THE System SHALL 在封面右侧显示书架按钮和查看按钮
2. WHEN User 点击书架按钮时, THE System SHALL 切换书籍的收藏状态
3. WHEN User 点击查看按钮时, THE System SHALL 打开书籍阅读界面
4. WHEN Bottom Sheet 显示时, THE System SHALL 在底部显示操作按钮栏
5. THE System SHALL 在底部操作栏提供关闭、收藏、编辑、分享和删除按钮

### 需求 7：文件路径显示

**用户故事：** 作为用户，我想要在详情弹窗中看到书籍的文件路径，以便了解文件的存储位置

#### 验收标准

1. WHEN Bottom Sheet 显示时, THE System SHALL 在底部显示目录路径
2. THE System SHALL 显示完整的文件路径
3. WHEN 文件路径过长时, THE System SHALL 使用滚动或省略号显示路径
4. WHEN User 点击路径时, THE System SHALL 复制路径到剪贴板
5. WHEN 路径被复制时, THE System SHALL 显示"已复制"提示

### 需求 8：响应式布局适配

**用户故事：** 作为用户，我想要详情弹窗能够适配不同的屏幕尺寸，以便在各种设备上都能正常使用

#### 验收标准

1. WHEN 设备屏幕宽度小于 600dp 时, THE System SHALL 使用单列布局
2. WHEN 设备屏幕宽度大于等于 600dp 时, THE System SHALL 使用双列布局
3. THE System SHALL 确保 Bottom Sheet 高度不超过屏幕高度的 90%
4. WHEN 内容超出可视区域时, THE System SHALL 提供垂直滚动功能
5. THE System SHALL 在横屏和竖屏模式下都能正常显示

### 需求 9：无障碍支持

**用户故事：** 作为视障用户，我想要详情弹窗支持无障碍功能，以便我能够使用屏幕阅读器访问内容

#### 验收标准

1. THE System SHALL 为所有交互元素提供内容描述
2. THE System SHALL 确保焦点顺序符合逻辑阅读顺序
3. THE System SHALL 支持 TalkBack 屏幕阅读器
4. THE System SHALL 确保所有文本的对比度符合 WCAG AA 标准
5. THE System SHALL 为图标按钮提供语义化的标签
