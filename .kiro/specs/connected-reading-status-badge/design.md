# Design Document

## Overview

本设计文档描述了如何实现连接式阅读状态标签（Connected Reading Status Badge），将书籍卡片上的"阅读中"状态文字和进度百分比无缝连接在一起，形成一个统一的视觉元素。

设计目标：
- 创建视觉上连贯的状态+进度显示
- 保持 Material Design 3 设计语言
- 确保代码可复用和可维护
- 不影响其他阅读状态的显示

## Architecture

### Component Structure

```
BookCard (现有组件)
├── Box (封面容器)
│   ├── AsyncImage (封面图片)
│   └── ConnectedReadingStatusBadge (新组件) - 用于阅读中状态
│       ├── Row (水平布局)
│       │   ├── Surface (状态文字部分 - 左侧圆角)
│       │   └── Surface (进度百分比部分 - 右侧圆角)
│   └── Surface (单一状态标签) - 用于其他状态
```

### Design Pattern

采用组合模式（Composition Pattern）：
- `ConnectedReadingStatusBadge` 作为独立的可组合函数
- `BookCard` 根据阅读状态条件渲染不同的标签组件
- 使用 Jetpack Compose 的声明式 UI 范式

## Components and Interfaces

### 1. ConnectedReadingStatusBadge 组件

新建的可组合函数，用于渲染连接式状态标签。

```kotlin
@Composable
fun ConnectedReadingStatusBadge(
    statusText: String,
    progressPercentage: Int,
    statusColor: Color = Color(0xFF4CAF50),
    progressColor: Color = Color(0xFF2E7D32),
    modifier: Modifier = Modifier
)
```

**参数说明：**
- `statusText`: 状态文字（如"阅读中"）
- `progressPercentage`: 进度百分比（0-100）
- `statusColor`: 状态部分的背景颜色
- `progressColor`: 进度部分的背景颜色
- `modifier`: 修饰符，用于外部布局控制

**实现细节：**

1. 使用 `Row` 布局，`horizontalArrangement = Arrangement.spacedBy(0.dp)` 确保无间隙
2. 左侧 Surface：
   - 形状：`RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 0.dp, bottomEnd = 0.dp)`
   - 背景色：`statusColor`
   - 内容：状态文字
   - 内边距：`horizontal = 8.dp, vertical = 4.dp`
3. 右侧 Surface：
   - 形状：`RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 4.dp, bottomEnd = 4.dp)`
   - 背景色：`progressColor`
   - 内容：进度百分比文字（如"25%"）
   - 内边距：`horizontal = 8.dp, vertical = 4.dp`
4. 文字样式：`MaterialTheme.typography.labelSmall`，颜色为白色

### 2. BookCard 组件修改

修改现有的 `BookCard` 组件，在渲染状态标签时根据阅读状态选择不同的实现。

**修改位置：** `app/src/main/java/takagi/ru/paysage/ui/screens/LibraryScreen.kt`

**修改逻辑：**

```kotlin
// 在 BookCard 的 Box 内部
val readingStatus = book.getReadingStatus()

when (readingStatus) {
    BookReadingStatus.READING -> {
        // 使用新的连接式标签
        ConnectedReadingStatusBadge(
            statusText = stringResource(R.string.status_reading),
            progressPercentage = (book.currentPage.toFloat() / book.totalPages.coerceAtLeast(1) * 100).toInt(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        )
    }
    else -> {
        // 保持现有的单一标签实现
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
            shape = MaterialTheme.shapes.small,
            color = when (readingStatus) {
                BookReadingStatus.FINISHED -> Color(0xFF2196F3)
                BookReadingStatus.LATEST -> Color(0xFFFF5722)
                BookReadingStatus.UNREAD -> Color(0xFFC62828)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Text(
                text = when (readingStatus) {
                    BookReadingStatus.FINISHED -> stringResource(R.string.status_finished)
                    BookReadingStatus.LATEST -> stringResource(R.string.status_latest)
                    BookReadingStatus.UNREAD -> stringResource(R.string.status_unread)
                    else -> ""
                },
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
```

## Data Models

无需修改现有数据模型。使用现有的：
- `Book` 数据类（包含 `currentPage` 和 `totalPages`）
- `BookReadingStatus` 枚举（包含 READING, FINISHED, LATEST, UNREAD 等状态）

## Visual Design Specifications

### Color Scheme

| 元素 | 颜色值 | 用途 |
|------|--------|------|
| 状态背景（阅读中） | #4CAF50 | 左侧"阅读中"文字背景 |
| 进度背景 | #2E7D32 | 右侧进度百分比背景 |
| 文字颜色 | #FFFFFF | 所有标签文字 |
| 已完成背景 | #2196F3 | 已完成状态标签 |
| 最新背景 | #FF5722 | 最新状态标签 |
| 未读背景 | #C62828 | 未读状态标签 |

### Typography

- 字体样式：`MaterialTheme.typography.labelSmall`
- 文字颜色：白色（`Color.White`）
- 确保在深色背景上有足够的对比度

### Spacing & Dimensions

- 标签位置：封面左上角
- 外边距：8dp（从封面边缘）
- 内边距：水平 8dp，垂直 4dp
- 圆角半径：4dp
- 两部分间隙：0dp（完全贴合）

### Shape Design

```
┌─────────────────────────┐
│ ╭────────╮╭──────╮      │
│ │ 阅读中 ││ 25% │      │  <- 连接式标签
│ ╰────────╯╰──────╯      │
│                         │
│      [封面图片]          │
│                         │
└─────────────────────────┘
```

左侧部分：左上、左下圆角
右侧部分：右上、右下圆角
中间：完全贴合，无间隙

## Error Handling

### Edge Cases

1. **进度计算异常**
   - 当 `totalPages` 为 0 时，使用 `coerceAtLeast(1)` 避免除零错误
   - 进度百分比限制在 0-100 范围内

2. **状态文字缺失**
   - 使用 `stringResource` 确保多语言支持
   - 如果资源缺失，显示空字符串而不是崩溃

3. **颜色值异常**
   - 提供默认颜色值作为参数默认值
   - 确保颜色值始终有效

## Testing Strategy

### Unit Tests

不需要单独的单元测试，因为这是纯 UI 组件。

### UI Tests

1. **视觉回归测试**
   - 截图对比：验证连接式标签的视觉效果
   - 确认两部分无间隙
   - 验证圆角正确应用

2. **状态测试**
   - 测试阅读中状态显示连接式标签
   - 测试其他状态显示单一标签
   - 测试不同进度百分比的显示

3. **布局测试**
   - 验证标签位置在左上角
   - 验证内外边距正确
   - 验证在不同屏幕尺寸下的显示

### Manual Testing Checklist

- [ ] 阅读中状态显示连接式标签
- [ ] 状态文字和进度百分比无间隙连接
- [ ] 左侧绿色，右侧深绿色
- [ ] 圆角正确（左侧左圆角，右侧右圆角）
- [ ] 文字清晰可读（白色）
- [ ] 已完成状态显示蓝色单一标签
- [ ] 最新状态显示橙红色单一标签
- [ ] 未读状态显示深红色单一标签
- [ ] 不同进度百分比正确显示（0%, 50%, 100%）
- [ ] 多语言环境下文字正确显示

## Implementation Notes

### File Changes

需要修改的文件：
- `app/src/main/java/takagi/ru/paysage/ui/screens/LibraryScreen.kt`
  - 添加 `ConnectedReadingStatusBadge` 可组合函数
  - 修改 `BookCard` 组件中的状态标签渲染逻辑

### Code Organization

建议将 `ConnectedReadingStatusBadge` 放在 `LibraryScreen.kt` 文件的底部，与其他辅助组件（如 `EmptyLibraryView`、`SearchAppBar` 等）放在一起。

如果未来需要在其他地方复用，可以考虑移动到 `app/src/main/java/takagi/ru/paysage/ui/components/` 目录下的独立文件中。

### Performance Considerations

- 使用 `remember` 缓存进度百分比计算结果（如果需要）
- 避免在每次重组时重新计算颜色值
- 确保 `Row` 布局的 `horizontalArrangement` 设置为 `spacedBy(0.dp)` 以避免不必要的间隙计算

### Accessibility

- 确保标签文字有足够的对比度（白色文字在深色背景上）
- 考虑为标签添加语义描述（contentDescription），方便屏幕阅读器用户
- 确保字体大小符合可访问性标准（labelSmall 通常为 11sp）

## Future Enhancements

可能的未来改进：
1. 支持自定义颜色主题
2. 添加动画效果（进度变化时的过渡动画）
3. 支持更多状态的连接式标签（如"最新 + 未读页数"）
4. 提供标签位置配置选项（左上、右上等）
