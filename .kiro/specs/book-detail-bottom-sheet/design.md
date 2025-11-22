# 书籍详情底部弹窗设计文档

## 概述

本设计文档描述了书籍详情底部弹窗（Book Detail Bottom Sheet）的架构、组件和实现细节。该功能允许用户通过长按书籍卡片触发一个从底部滑出的弹窗，显示书籍的详细信息并提供快速操作。

## 架构

### 组件层次结构

```
LibraryScreen
├── BookCard (with long press gesture)
│   └── BookDetailBottomSheet (Modal Bottom Sheet)
│       ├── BookDetailHeader (封面、标题、格式、大小)
│       ├── BookDetailActions (书架、查看按钮)
│       ├── BookDetailSortSection (排序选项)
│       ├── BookDetailTagSection (标签管理)
│       ├── BookDetailProgressSection (阅读进度)
│       ├── BookDetailAuthorSection (编辑者信息)
│       ├── BookDetailPathSection (文件路径)
│       └── BookDetailBottomActions (底部操作栏)
```

### 数据流

```
User Long Press → LibraryScreen State Update → Show Bottom Sheet
                                              ↓
                                    BookDetailBottomSheet
                                              ↓
                                    Display Book Details
                                              ↓
                                    User Actions → ViewModel
                                              ↓
                                    Update Database → Refresh UI
```

## 组件设计

### 1. BookDetailBottomSheet

主容器组件，使用 Material 3 的 ModalBottomSheet。

#### 属性

```kotlin
@Composable
fun BookDetailBottomSheet(
    book: Book,
    onDismiss: () -> Unit,
    onOpenBook: (Long) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onEditBook: (Long) -> Unit,
    onShareBook: (Book) -> Unit,
    onDeleteBook: (Long) -> Unit,
    onUpdateTags: (Long, List<String>) -> Unit,
    onUpdateSortPreference: (Long, String) -> Unit,
    modifier: Modifier = Modifier
)
```

#### 布局结构

- 使用 `ModalBottomSheet` 作为容器
- 内部使用 `LazyColumn` 支持滚动
- 最大高度为屏幕高度的 90%
- 圆角半径：28dp（Material 3 Expressive）
- 背景色：`MaterialTheme.colorScheme.surface`

### 2. BookDetailHeader

显示书籍的基本信息：封面、标题、格式标签和文件大小。

#### 布局

```
┌─────────────────────────────────────┐
│  ┌──────┐                           │
│  │      │  书籍标题                  │
│  │ 封面 │  ┌─────┐  大小: 27.8 MB   │
│  │      │  │ ZIP │                  │
│  └──────┘  └─────┘                  │
└─────────────────────────────────────┘
```

#### 实现细节

- 封面尺寸：120dp × 180dp
- 封面圆角：12dp
- 标题字体：`MaterialTheme.typography.headlineSmall`
- 格式标签：使用 `Surface` 组件，背景色为 `secondaryContainer`
- 文件大小：使用 `bodyMedium` 字体，颜色为 `onSurfaceVariant`

### 3. BookDetailActions

快速操作按钮区域，包含书架按钮和查看按钮。

#### 布局

```
┌─────────────────────────────────────┐
│  ┌──────┐  ┌──────┐                 │
│  │  📚  │  │  👁  │                 │
│  │ 书架 │  │ 查看 │                 │
│  └──────┘  └──────┘                 │
└─────────────────────────────────────┘
```

#### 实现细节

- 使用 `Row` 布局，间距 12dp
- 按钮使用 `ExpressiveButton` 组件
- 书架按钮：切换收藏状态，图标根据状态变化
- 查看按钮：打开书籍阅读界面

### 4. BookDetailSortSection

排序选项下拉菜单。

#### 布局

```
┌─────────────────────────────────────┐
│  新的  ▼                             │
└─────────────────────────────────────┘
```

#### 实现细节

- 使用 `ExposedDropdownMenuBox` 组件
- 排序选项：新的、标题、作者、最近阅读、添加时间
- 选中项使用 `primary` 颜色高亮
- 保存用户的排序偏好到数据库

### 5. BookDetailTagSection

标签管理区域，显示和编辑书籍标签。

#### 布局

```
┌─────────────────────────────────────┐
│  #  无 # 标签              ✏️        │
└─────────────────────────────────────┘
```

或

```
┌─────────────────────────────────────┐
│  #  [漫画] [日语] [完结]    ✏️       │
└─────────────────────────────────────┘
```

#### 实现细节

- 使用 `Surface` 组件作为容器
- 标签使用 `ExpressiveChip` 组件
- 点击编辑按钮打开标签编辑对话框
- 标签使用 `FlowRow` 布局，自动换行
- 空状态显示"无 # 标签"占位文本

### 6. BookDetailProgressSection

阅读进度信息区域。

#### 布局

```
┌─────────────────────────────────────┐
│  0/26                                │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│  页数        上次阅读      还未读过   │
└─────────────────────────────────────┘
```

#### 实现细节

- 页码显示：`currentPage/totalPages`
- 进度条：使用 `LinearProgressIndicator`
- 三列信息：页数、上次阅读时间、阅读状态
- 上次阅读时间格式化：相对时间（如"2小时前"）
- 未读状态显示"还未读过"

### 7. BookDetailAuthorSection

编辑者/作者信息区域。

#### 布局

```
┌─────────────────────────────────────┐
│  👤  编剧                            │
│      ちさんゆにおん (ななもと)        │
└─────────────────────────────────────┘
```

#### 实现细节

- 使用 `Row` 布局
- 图标：`Icons.Default.Person`
- 标签：`bodyMedium` 字体，颜色为 `onSurfaceVariant`
- 作者名：`bodyLarge` 字体
- 如果没有作者信息，不显示此区域

### 8. BookDetailPathSection

文件路径显示区域。

#### 布局

```
┌─────────────────────────────────────┐
│  📁  目录                            │
│      /storage/emulated/0/manga      │
└─────────────────────────────────────┘
```

#### 实现细节

- 使用 `Row` 布局
- 图标：`Icons.Default.Folder`
- 路径文本：`bodySmall` 字体，颜色为 `onSurfaceVariant`
- 点击复制路径到剪贴板
- 复制后显示 Snackbar 提示"已复制"
- 路径过长时使用 `maxLines = 2` 和 `overflow = TextOverflow.Ellipsis`

### 9. BookDetailBottomActions

底部操作按钮栏。

#### 布局

```
┌─────────────────────────────────────┐
│  ✕   ⭐   ✏️   📤   🗑️              │
└─────────────────────────────────────┘
```

#### 实现细节

- 使用 `Row` 布局，均匀分布
- 按钮使用 `ExpressiveIconButton` 组件
- 按钮列表：
  - 关闭：`Icons.Default.Close`
  - 收藏：`Icons.Default.Star` / `Icons.Default.StarBorder`
  - 编辑：`Icons.Default.Edit`
  - 分享：`Icons.Default.Share`
  - 删除：`Icons.Default.Delete`
- 删除操作需要确认对话框

## 数据模型

### BookDetailUiState

```kotlin
data class BookDetailUiState(
    val isVisible: Boolean = false,
    val selectedBook: Book? = null,
    val isEditingTags: Boolean = false,
    val tempTags: List<String> = emptyList(),
    val showDeleteConfirmation: Boolean = false
)
```

### 扩展 Book 数据模型

需要添加以下字段（如果不存在）：

```kotlin
data class Book(
    // ... 现有字段 ...
    val sortPreference: String? = null, // 排序偏好
    val tags: List<String> = emptyList() // 标签列表
)
```

## 交互设计

### 长按手势

使用 Compose 的 `pointerInput` 修饰符实现长按检测：

```kotlin
Modifier.pointerInput(Unit) {
    detectTapGestures(
        onLongPress = {
            // 触发底部弹窗
            onShowBookDetail(book)
        },
        onTap = {
            // 正常点击打开书籍
            onBookClick(book.id)
        }
    )
}
```

### 动画

#### 弹窗展开/收起动画

- 使用 `ModalBottomSheet` 的默认动画
- 展开时长：500ms
- 收起时长：300ms
- 缓动函数：`EmphasizedDecelerateEasing`

#### 按钮交互动画

- 使用 `ExpressiveIconButton` 和 `ExpressiveButton`
- 按压缩放：0.92x
- 动画时长：200ms
- 弹簧效果：`Spring.DampingRatioMediumBouncy`

#### 收藏状态切换动画

- 图标旋转：360度
- 缩放：1.0x → 1.3x → 1.0x
- 动画时长：400ms

### 响应式布局

#### 小屏幕（< 600dp）

- 单列布局
- 封面尺寸：100dp × 150dp
- 底部操作栏图标间距：8dp

#### 中等屏幕（600dp - 840dp）

- 单列布局
- 封面尺寸：120dp × 180dp
- 底部操作栏图标间距：12dp

#### 大屏幕（> 840dp）

- 双列布局（左侧封面和信息，右侧操作）
- 封面尺寸：160dp × 240dp
- 底部操作栏图标间距：16dp

## 无障碍支持

### 语义化标签

所有交互元素都需要提供 `contentDescription`：

```kotlin
Icon(
    imageVector = Icons.Default.Close,
    contentDescription = stringResource(R.string.close_detail_sheet)
)
```

### 焦点顺序

焦点顺序应该遵循视觉阅读顺序：

1. 封面和标题
2. 快速操作按钮（书架、查看）
3. 排序选项
4. 标签编辑
5. 阅读进度
6. 作者信息
7. 文件路径
8. 底部操作栏

### 屏幕阅读器支持

使用 `semantics` 修饰符提供额外的语义信息：

```kotlin
Modifier.semantics {
    contentDescription = "书籍详情：${book.title}"
    stateDescription = if (book.isFavorite) "已收藏" else "未收藏"
}
```

### 对比度

确保所有文本和图标的对比度符合 WCAG AA 标准（至少 4.5:1）。

## 错误处理

### 文件不存在

如果书籍文件已被删除：

- 显示警告图标
- 禁用"查看"按钮
- 在文件路径区域显示"文件不存在"提示
- 提供"从书库移除"选项

### 封面加载失败

如果封面图片加载失败：

- 显示默认书籍图标
- 使用 `placeholder` 和 `error` 参数处理加载状态

### 操作失败

如果操作（如删除、编辑）失败：

- 显示 Snackbar 错误提示
- 提供重试选项
- 记录错误日志

## 性能优化

### 懒加载

- 使用 `LazyColumn` 实现内容滚动
- 仅在弹窗可见时加载数据

### 图片缓存

- 使用 Coil 的内存缓存和磁盘缓存
- 设置合理的缓存大小限制

### 状态管理

- 使用 `remember` 缓存计算结果
- 使用 `derivedStateOf` 避免不必要的重组

### 动画优化

- 使用 `graphicsLayer` 而不是 `Modifier.scale` 提高性能
- 避免在动画期间进行复杂计算

## 测试策略

### 单元测试

- 测试长按手势检测逻辑
- 测试数据格式化函数（文件大小、时间）
- 测试标签管理逻辑

### UI 测试

- 测试弹窗的显示和隐藏
- 测试所有按钮的点击事件
- 测试标签编辑流程
- 测试删除确认对话框

### 集成测试

- 测试与 ViewModel 的交互
- 测试数据库更新
- 测试文件系统操作

### 无障碍测试

- 使用 TalkBack 测试屏幕阅读器支持
- 测试焦点导航
- 测试对比度和可读性

## 国际化

### 需要翻译的字符串

```xml
<!-- 书籍详情底部弹窗 -->
<string name="book_detail_title">书籍详情</string>
<string name="book_detail_add_to_shelf">书架</string>
<string name="book_detail_view_book">查看</string>
<string name="book_detail_sort_by">排序方式</string>
<string name="book_detail_sort_new">新的</string>
<string name="book_detail_sort_title">标题</string>
<string name="book_detail_sort_author">作者</string>
<string name="book_detail_sort_recent">最近阅读</string>
<string name="book_detail_sort_added">添加时间</string>
<string name="book_detail_no_tags">无 # 标签</string>
<string name="book_detail_edit_tags">编辑标签</string>
<string name="book_detail_pages">页数</string>
<string name="book_detail_last_read">上次阅读</string>
<string name="book_detail_not_read">还未读过</string>
<string name="book_detail_author">编剧</string>
<string name="book_detail_directory">目录</string>
<string name="book_detail_path_copied">已复制路径</string>
<string name="book_detail_close">关闭</string>
<string name="book_detail_favorite">收藏</string>
<string name="book_detail_edit">编辑</string>
<string name="book_detail_share">分享</string>
<string name="book_detail_delete">删除</string>
<string name="book_detail_delete_confirm_title">删除书籍</string>
<string name="book_detail_delete_confirm_message">确定要删除这本书吗？此操作无法撤销。</string>
<string name="book_detail_file_not_found">文件不存在</string>
<string name="book_detail_remove_from_library">从书库移除</string>
```

## 依赖项

### 新增依赖

```gradle
// Compose Material 3
implementation "androidx.compose.material3:material3:1.2.0"

// Compose Foundation (for gestures)
implementation "androidx.compose.foundation:foundation:1.6.0"

// Coil for image loading
implementation "io.coil-kt:coil-compose:2.5.0"
```

### 现有依赖

- Jetpack Compose
- Material 3
- Kotlin Coroutines
- Room Database
- ViewModel

## 实现注意事项

1. **长按手势冲突**：确保长按手势不会与其他手势（如滚动）冲突
2. **状态保存**：在配置更改（如旋转）时保存弹窗状态
3. **内存泄漏**：确保在弹窗关闭时清理资源
4. **线程安全**：所有数据库操作应在后台线程执行
5. **动画流畅性**：确保动画在低端设备上也能流畅运行
6. **深色模式**：确保在深色模式下所有颜色都适配良好
7. **边缘情况**：处理空数据、超长文本等边缘情况
