# 需求文档

## 简介

本功能旨在将系统中的"本地功能"和"在线功能"模块重命名为"本地管理"和"在线管理"，并按照Material 3 Extended (M3e)设计规范对相关用户界面进行全面重构。同时，在两个管理模块的抽屉菜单中添加"创建文件夹"功能，提升用户的内容组织能力。

## 术语表

- **应用系统 (Application_System)**: Paysage阅读应用的整体系统
- **本地管理模块 (Local_Management_Module)**: 原"本地功能"模块，用于管理设备本地存储的书籍内容
- **在线管理模块 (Online_Management_Module)**: 原"在线功能"模块，用于管理在线书源和在线书籍内容
- **导航抽屉 (Navigation_Drawer)**: 应用的侧边栏导航菜单
- **M3e设计规范 (M3e_Design_Specification)**: Material 3 Extended设计系统，包含布局、配色、组件、动画等设计标准
- **文件夹创建对话框 (Folder_Creation_Dialog)**: 用于输入新文件夹名称的弹出对话框
- **字符串资源 (String_Resources)**: Android应用中存储文本内容的XML资源文件
- **UI组件 (UI_Components)**: 用户界面的可复用组件，如按钮、对话框、列表项等
- **用户 (User)**: 使用Paysage应用的终端用户

## 需求

### 需求 1: 模块名称重命名

**用户故事:** 作为用户，我希望看到更准确的模块名称"本地管理"和"在线管理"，以便更清楚地理解各模块的管理功能定位

#### 验收标准

1. WHEN 应用启动时，THE Application_System SHALL 在导航抽屉中显示"本地管理"替代"本地功能"
2. WHEN 应用启动时，THE Application_System SHALL 在导航抽屉中显示"在线管理"替代"在线功能"
3. THE Application_System SHALL 在所有中文字符串资源文件中将"本地功能"替换为"本地管理"
4. THE Application_System SHALL 在所有中文字符串资源文件中将"在线功能"替换为"在线管理"
5. THE Application_System SHALL 在所有相关的辅助功能描述文本中使用新的模块名称

### 需求 2: M3e设计规范UI重构

**用户故事:** 作为用户，我希望本地管理和在线管理模块的界面遵循M3e设计规范，以便获得现代化、一致且美观的用户体验

#### 验收标准

1. THE Application_System SHALL 在本地管理模块和在线管理模块中应用M3e标准的间距规范（8dp基准网格系统）
2. THE Application_System SHALL 在本地管理模块和在线管理模块中应用M3e标准的边距规范（16dp、24dp标准边距）
3. THE Application_System SHALL 在本地管理模块和在线管理模块中使用M3e标准的字体排版系统（Display、Headline、Title、Body、Label层级）
4. THE Application_System SHALL 在本地管理模块和在线管理模块中使用M3e标准的图标样式（Material Symbols）
5. THE Application_System SHALL 在本地管理模块和在线管理模块中使用M3e标准的按钮样式（Filled、Outlined、Text按钮变体）
6. THE Application_System SHALL 在本地管理模块和在线管理模块中应用M3e标准的配色方案（基于Material Theme Builder的色彩系统）
7. THE Application_System SHALL 在本地管理模块和在线管理模块中实现M3e标准的状态层效果（Hover、Focus、Press、Drag状态）
8. THE Application_System SHALL 在本地管理模块和在线管理模块中应用M3e标准的动画时长（100ms、200ms、300ms标准时长）
9. THE Application_System SHALL 在本地管理模块和在线管理模块中使用M3e标准的缓动曲线（Emphasized、Standard缓动函数）
10. THE Application_System SHALL 在本地管理模块和在线管理模块中使用M3e标准组件（Card、List、Dialog、FAB等）

### 需求 3: 导航抽屉视觉更新

**用户故事:** 作为用户，我希望导航抽屉的视觉设计符合M3e规范，以便在切换模块时获得流畅一致的体验

#### 验收标准

1. THE Application_System SHALL 在导航抽屉中使用M3e标准的NavigationDrawer组件样式
2. THE Application_System SHALL 为导航抽屉项目应用M3e标准的选中状态指示器（Active Indicator）
3. THE Application_System SHALL 在导航抽屉中使用M3e标准的图标和文本间距（12dp）
4. THE Application_System SHALL 为导航抽屉项目应用M3e标准的涟漪效果（Ripple Effect）
5. WHEN 用户点击导航项时，THE Application_System SHALL 播放M3e标准的过渡动画（300ms，Emphasized Decelerate缓动）

### 需求 4: 创建文件夹功能

**用户故事:** 作为用户，我希望在本地管理和在线管理模块中创建文件夹，以便更好地组织我的书籍内容

#### 验收标准

1. WHEN 用户打开本地管理模块的导航抽屉时，THE Application_System SHALL 显示"创建文件夹"操作按钮
2. WHEN 用户打开在线管理模块的导航抽屉时，THE Application_System SHALL 显示"创建文件夹"操作按钮
3. WHEN 用户点击"创建文件夹"按钮时，THE Application_System SHALL 显示文件夹创建对话框
4. THE Folder_Creation_Dialog SHALL 包含一个文本输入框用于输入文件夹名称
5. THE Folder_Creation_Dialog SHALL 包含"确定"和"取消"两个操作按钮
6. THE Folder_Creation_Dialog SHALL 遵循M3e标准的对话框设计规范

### 需求 5: 文件夹名称验证

**用户故事:** 作为用户，我希望系统验证我输入的文件夹名称，以便避免创建无效或重复的文件夹

#### 验收标准

1. WHEN 用户在文件夹创建对话框中输入空白名称时，THE Application_System SHALL 禁用"确定"按钮
2. WHEN 用户输入的文件夹名称包含非法字符（/ \ : * ? " < > |）时，THE Application_System SHALL 显示错误提示信息
3. WHEN 用户输入的文件夹名称已存在于当前目录时，THE Application_System SHALL 显示重复名称警告信息
4. WHEN 用户输入的文件夹名称长度超过255个字符时，THE Application_System SHALL 显示长度限制错误信息
5. THE Application_System SHALL 在用户输入时实时验证文件夹名称的有效性

### 需求 6: 文件夹创建反馈

**用户故事:** 作为用户，我希望在创建文件夹后收到明确的成功或失败反馈，以便了解操作结果

#### 验收标准

1. WHEN 文件夹创建成功时，THE Application_System SHALL 显示成功提示消息（Snackbar，持续3秒）
2. WHEN 文件夹创建失败时，THE Application_System SHALL 显示失败提示消息（Snackbar，持续5秒）并说明失败原因
3. THE Application_System SHALL 在显示反馈消息时使用M3e标准的Snackbar组件样式
4. WHEN 文件夹创建成功时，THE Application_System SHALL 自动关闭文件夹创建对话框
5. WHEN 文件夹创建失败时，THE Application_System SHALL 保持对话框打开状态以便用户修改输入

### 需求 7: 文件夹即时显示

**用户故事:** 作为用户，我希望新创建的文件夹立即显示在目录结构中，以便我可以马上使用它

#### 验收标准

1. WHEN 在本地管理模块中创建文件夹成功时，THE Application_System SHALL 在本地管理模块的目录列表中立即显示新文件夹
2. WHEN 在在线管理模块中创建文件夹成功时，THE Application_System SHALL 在在线管理模块的目录列表中立即显示新文件夹
3. THE Application_System SHALL 将新创建的文件夹按字母顺序插入到目录列表的正确位置
4. THE Application_System SHALL 为新创建的文件夹应用M3e标准的列表项样式
5. WHEN 新文件夹显示时，THE Application_System SHALL 播放M3e标准的进入动画（200ms，Standard Decelerate缓动）

### 需求 8: 无障碍访问支持

**用户故事:** 作为使用辅助技术的用户，我希望所有新增和修改的UI元素都支持无障碍访问，以便我能够正常使用这些功能

#### 验收标准

1. THE Application_System SHALL 为"创建文件夹"按钮提供清晰的内容描述（contentDescription）
2. THE Application_System SHALL 为文件夹创建对话框的所有交互元素提供适当的语义标签
3. THE Application_System SHALL 确保文件夹创建对话框支持键盘导航
4. THE Application_System SHALL 为文件夹名称输入框提供输入提示（hint）和错误提示的语音反馈
5. THE Application_System SHALL 确保所有触摸目标的最小尺寸为48dp × 48dp（符合WCAG 2.1 AAA标准）
6. THE Application_System SHALL 确保所有文本与背景的对比度至少为4.5:1（符合WCAG 2.1 AA标准）

### 需求 9: 主题适配

**用户故事:** 作为用户，我希望新的UI元素在明亮主题和暗色主题下都能正常显示，以便在不同环境下获得舒适的视觉体验

#### 验收标准

1. THE Application_System SHALL 在明亮主题下为本地管理和在线管理模块应用M3e标准的明亮配色方案
2. THE Application_System SHALL 在暗色主题下为本地管理和在线管理模块应用M3e标准的暗色配色方案
3. THE Application_System SHALL 在明亮主题下为文件夹创建对话框应用M3e标准的明亮配色方案
4. THE Application_System SHALL 在暗色主题下为文件夹创建对话框应用M3e标准的暗色配色方案
5. WHEN 用户切换主题时，THE Application_System SHALL 立即更新所有UI元素的配色（无需重启应用）

### 需求 10: 动态配色支持

**用户故事:** 作为Android 12+用户，我希望新的UI元素支持动态配色功能，以便界面颜色与我的壁纸协调一致

#### 验收标准

1. WHERE 设备运行Android 12或更高版本，THE Application_System SHALL 支持从系统壁纸提取动态配色方案
2. WHERE 用户启用了动态配色功能，THE Application_System SHALL 将动态配色应用到本地管理和在线管理模块
3. WHERE 用户启用了动态配色功能，THE Application_System SHALL 将动态配色应用到文件夹创建对话框
4. WHERE 设备不支持动态配色，THE Application_System SHALL 回退到预设的M3e配色方案
5. WHEN 系统壁纸更改时，THE Application_System SHALL 自动更新动态配色方案

### 需求 11: 性能要求

**用户故事:** 作为用户，我希望UI重构后的界面响应迅速流畅，以便获得良好的使用体验

#### 验收标准

1. WHEN 用户打开导航抽屉时，THE Application_System SHALL 在100毫秒内完成渲染
2. WHEN 用户点击"创建文件夹"按钮时，THE Application_System SHALL 在200毫秒内显示对话框
3. WHEN 用户在文件夹名称输入框中输入时，THE Application_System SHALL 在50毫秒内完成验证并更新UI状态
4. WHEN 文件夹创建成功时，THE Application_System SHALL 在300毫秒内完成目录列表的更新和动画
5. THE Application_System SHALL 确保所有动画的帧率不低于60fps

### 需求 12: 向后兼容性

**用户故事:** 作为现有用户，我希望更新后的应用能够保留我的现有数据和设置，以便无缝过渡到新版本

#### 验收标准

1. THE Application_System SHALL 在更新后保留所有现有的书籍数据
2. THE Application_System SHALL 在更新后保留所有现有的用户设置
3. THE Application_System SHALL 在更新后保留所有现有的书源配置
4. THE Application_System SHALL 在更新后保留所有现有的文件夹结构
5. THE Application_System SHALL 在首次启动更新后的版本时自动迁移旧的模块名称引用
