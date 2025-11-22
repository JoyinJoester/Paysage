# Design Document

## Overview

本设计文档描述了如何重构 `BookCompactCard` 组件，将其从传统的"封面+信息区域"布局改为现代的叠加式设计。新设计将标题和进度条直接叠加在封面图片上，创造更紧凑、更具视觉吸引力的卡片效果。

## Architecture

### Component Structure

```
BookCompactCard (ExpressiveCard)
└── Box (fillMaxSize)
    ├── AsyncImage/Icon (封面图片 - 填充整个卡片)
    ├── StatusBadge (左上角 - 状态标签)
    └── Box (底部叠加层)
        ├── Gradient Background (渐变背景)
        ├── Text (标题 - 叠加在渐变背景上)
        └── LinearProgressIndicator (进度条 - 底部边缘)
```

### Layout Hierarchy

1. **外层 ExpressiveCard**: 提供卡片容器和点击交互
2. **封面层 (AsyncImage)**: 填充整个卡片，保持0.7宽高比
3. **状态标签层 (StatusBadge)**: 绝对定位在左上角
4. **底部叠加层 (Box)**: 包含标题和进度条，绝对定位在底部

## Components and Interfaces

### Modified Component: BookCompactCard

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookCompactCard(
    book: Book,
    showProgress: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

**Changes:**
- 移除 `Column` 布局中的独立信息区域
- 使用 `Box` 布局实现叠加效果
- 添加渐变背景层
- 重新定位标题和进度条

### New Internal Composable: OverlayContent

```kotlin
@Composable
private fun OverlayContent(
    title: String,
    currentPage: Int,
    totalPages: Int,
    showProgress: Boolean,
    modifier: Modifier = Modifier
)
```

**Purpose:** 封装底部叠加内容（标题和进度条）的布局逻辑

**Parameters:**
- `title`: 书籍标题
- `currentPage`: 当前页码
- `totalPages`: 总页数
- `showProgress`: 是否显示进度条
- `modifier`: 修饰符

## Data Models

无需修改现有数据模型。使用现有的 `Book` 数据类。

## UI Specifications

### Dimensions

- **Card Aspect Ratio**: 0.7 (宽:高)
- **Overlay Height**: 约占卡片高度的30%
- **Progress Bar Height**: 6dp
- **Status Badge Padding**: 8dp
- **Title Padding**: 水平12dp，垂直8dp
- **Progress Bar Padding**: 水平12dp，底部8dp

### Colors

- **Overlay Gradient**: 
  - 底部: Color.Black.copy(alpha = 0.7f)
  - 顶部: Color.Transparent
- **Title Text**: Color.White
- **Progress Bar**: MaterialTheme.colorScheme.primary
- **Progress Track**: Color.White.copy(alpha = 0.3f)

### Typography

- **Title**: MaterialTheme.typography.titleSmall
- **Max Lines**: 2
- **Overflow**: TextOverflow.Ellipsis

### Visual Effects

1. **Gradient Background**: 使用 `Brush.verticalGradient` 创建从透明到半透明黑色的渐变
2. **Text Shadow**: 可选，为标题添加轻微阴影以增强可读性
3. **Progress Bar Shape**: 使用圆角矩形 (MaterialTheme.shapes.small)

## Implementation Details

### Layout Implementation

```kotlin
ExpressiveCard(onClick = onClick, modifier = modifier) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
    ) {
        // 1. 封面图片层
        CoverImage(book)
        
        // 2. 状态标签层
        StatusBadge(
            book = book,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        )
        
        // 3. 底部叠加层
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.3f) // 占据底部30%
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
            ) {
                // 标题
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
                
                // 进度条
                if (showProgress) {
                    LinearProgressIndicator(
                        progress = { book.currentPage.toFloat() / book.totalPages.coerceAtLeast(1) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .padding(horizontal = 12.dp, bottom = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}
```

### Gradient Configuration

使用 `Brush.verticalGradient` 创建从上到下的渐变：
- 起始位置 (0f): Color.Transparent
- 结束位置 (1f): Color.Black.copy(alpha = 0.7f)

这样可以确保标题区域有足够的对比度，同时不会完全遮挡封面。

### Progress Bar Styling

```kotlin
LinearProgressIndicator(
    progress = { book.currentPage.toFloat() / book.totalPages.coerceAtLeast(1) },
    modifier = Modifier
        .fillMaxWidth()
        .height(6.dp)
        .padding(horizontal = 12.dp, bottom = 8.dp)
        .clip(MaterialTheme.shapes.small),
    color = MaterialTheme.colorScheme.primary,
    trackColor = Color.White.copy(alpha = 0.3f)
)
```

## Error Handling

### Missing Cover Image

当 `book.coverPath` 为 null 时：
- 显示默认的书籍图标 (Icons.Default.Book)
- 图标大小: 64.dp
- 图标颜色: MaterialTheme.colorScheme.primary
- 背景: MaterialTheme.colorScheme.surfaceVariant

### Zero Total Pages

当 `book.totalPages` 为 0 时：
- 使用 `coerceAtLeast(1)` 避免除零错误
- 进度条显示为 0%

### Long Titles

- 使用 `maxLines = 2` 限制标题行数
- 使用 `TextOverflow.Ellipsis` 截断超长文本
- 确保标题区域有足够的高度容纳2行文本

## Testing Strategy

### Visual Regression Tests

1. **正常封面**: 验证叠加层正确显示在封面上
2. **无封面**: 验证默认图标和叠加层的显示
3. **长标题**: 验证2行截断和省略号
4. **短标题**: 验证单行标题的显示
5. **不同进度**: 验证0%, 50%, 100%进度的显示

### Component Tests

1. **BookCompactCard 渲染测试**
   - 验证封面图片加载
   - 验证标题文本显示
   - 验证进度条显示
   - 验证状态标签显示

2. **点击交互测试**
   - 验证 onClick 回调正确触发
   - 验证点击区域覆盖整个卡片

3. **showProgress 参数测试**
   - 当 showProgress = true 时，验证进度条显示
   - 当 showProgress = false 时，验证进度条隐藏

### Accessibility Tests

1. **内容描述**: 验证封面图片有正确的 contentDescription
2. **语义信息**: 验证卡片提供足够的语义信息供屏幕阅读器使用
3. **对比度**: 验证白色文字在渐变背景上有足够的对比度

## Performance Considerations

### Image Loading

- 使用 Coil 的 `crossfade(true)` 实现平滑过渡
- 使用 `memoryCacheKey` 避免重复加载
- AsyncImage 自动处理图片缓存

### Gradient Rendering

- `Brush.verticalGradient` 是高效的原生渲染
- 渐变在 Compose 中是硬件加速的
- 不会对性能产生明显影响

### Layout Performance

- 使用 `Box` 布局的叠加方式比嵌套 `Column` 更高效
- 减少了布局层级，提升渲染性能
- `aspectRatio` 修饰符确保一致的布局计算

## Migration Notes

### Breaking Changes

无破坏性变更。`BookCompactCard` 的公共接口保持不变。

### Visual Changes

用户将看到：
1. 卡片更加紧凑（移除了底部的独立信息区域）
2. 标题和进度条叠加在封面上
3. 更现代的视觉效果

### Backward Compatibility

- 所有现有的调用代码无需修改
- 参数签名保持不变
- 行为保持一致（仅视觉效果改变）

## Future Enhancements

1. **可配置的叠加层透明度**: 允许用户调整渐变背景的透明度
2. **动画效果**: 添加标题和进度条的淡入动画
3. **自适应文字大小**: 根据卡片大小自动调整标题字体大小
4. **进度百分比显示**: 在进度条旁边显示具体的百分比数字
