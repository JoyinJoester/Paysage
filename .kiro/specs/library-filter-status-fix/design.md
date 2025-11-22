# 设计文档

## 概述

本设计文档描述如何修复图书馆界面的状态筛选功能，包括实现书籍过滤逻辑、状态指示器颜色变化和下拉菜单中的状态圆点显示。

## 架构

### 组件关系

```
LibraryScreen
├── LibraryFilterBar (状态筛选栏)
│   ├── StatusIndicator (左侧圆形指示器)
│   └── DropdownMenu (状态选择菜单)
│       └── StatusMenuItem (带圆点的菜单项)
└── BookList (过滤后的书籍列表)
```

### 数据流

1. 用户在 `LibraryFilterBar` 中选择状态
2. `selectedFilter` 状态更新
3. `LibraryScreen` 根据 `selectedFilter` 过滤书籍列表
4. `StatusIndicator` 根据 `selectedFilter` 更新颜色
5. 过滤后的书籍列表重新渲染

## 组件和接口

### 1. 状态颜色映射

创建一个辅助函数来统一管理状态颜色：

```kotlin
/**
 * 获取书籍过滤状态对应的颜色
 */
fun getFilterStatusColor(status: BookFilterStatus): Color {
    return when (status) {
        BookFilterStatus.ALL -> Color.Unspecified // 使用主题色
        BookFilterStatus.LATEST -> Color(0xFFFF5722) // 橙红色
        BookFilterStatus.READING -> Color(0xFF4CAF50) // 绿色
        BookFilterStatus.FINISHED -> Color(0xFF2196F3) // 蓝色
        BookFilterStatus.UNREAD -> Color(0xFFC62828) // 深红色
    }
}
```

### 2. LibraryScreen 修改

在 `LibraryScreen` 中添加书籍过滤逻辑：

```kotlin
// 根据选中的过滤器过滤书籍
val filteredBooks = remember(displayBooks, selectedFilter) {
    when (selectedFilter) {
        BookFilterStatus.ALL -> displayBooks
        BookFilterStatus.LATEST -> displayBooks.filter { 
            it.getReadingStatus() == BookReadingStatus.LATEST 
        }
        BookFilterStatus.READING -> displayBooks.filter { 
            it.getReadingStatus() == BookReadingStatus.READING 
        }
        BookFilterStatus.FINISHED -> displayBooks.filter { 
            it.getReadingStatus() == BookReadingStatus.FINISHED 
        }
        BookFilterStatus.UNREAD -> displayBooks.filter { 
            it.getReadingStatus() == BookReadingStatus.UNREAD 
        }
    }
}
```

### 3. LibraryFilterBar 修改

#### 3.1 状态指示器颜色

修改左侧圆形指示器，使其颜色随选中状态变化：

```kotlin
// 圆形指示器
Box(
    modifier = Modifier
        .size(32.dp)
        .background(
            color = if (selectedFilter == BookFilterStatus.ALL) {
                MaterialTheme.colorScheme.primary
            } else {
                getFilterStatusColor(selectedFilter)
            },
            shape = CircleShape
        )
)
```

#### 3.2 下拉菜单项

创建一个新的可组合函数来显示带圆点的菜单项：

```kotlin
@Composable
fun StatusDropdownMenuItem(
    status: BookFilterStatus,
    text: String,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 状态圆点
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = if (status == BookFilterStatus.ALL) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                getFilterStatusColor(status)
                            },
                            shape = CircleShape
                        )
                )
                
                Text(text)
            }
        },
        onClick = onClick
    )
}
```

然后在下拉菜单中使用：

```kotlin
DropdownMenu(
    expanded = showFilterMenu,
    onDismissRequest = { showFilterMenu = false }
) {
    StatusDropdownMenuItem(
        status = BookFilterStatus.ALL,
        text = stringResource(takagi.ru.paysage.R.string.filter_all),
        onClick = {
            onFilterChange(BookFilterStatus.ALL)
            showFilterMenu = false
        }
    )
    StatusDropdownMenuItem(
        status = BookFilterStatus.LATEST,
        text = stringResource(takagi.ru.paysage.R.string.filter_latest),
        onClick = {
            onFilterChange(BookFilterStatus.LATEST)
            showFilterMenu = false
        }
    )
    // ... 其他状态
}
```

## 数据模型

### BookFilterStatus 到 BookReadingStatus 映射

| BookFilterStatus | BookReadingStatus | 过滤条件 |
|-----------------|-------------------|---------|
| ALL | - | 所有书籍 |
| LATEST | LATEST | 7天内添加 |
| READING | READING | currentPage > 0 且未完成 |
| FINISHED | FINISHED | isFinished = true |
| UNREAD | UNREAD | currentPage = 0 且非最新 |

### 状态颜色映射

| 状态 | 颜色代码 | 颜色名称 |
|-----|---------|---------|
| ALL | MaterialTheme.colorScheme.primary | 主题色 |
| LATEST | #FF5722 | 橙红色 |
| READING | #4CAF50 | 绿色 |
| FINISHED | #2196F3 | 蓝色 |
| UNREAD | #C62828 | 深红色 |

## 错误处理

1. **空列表处理**: 当过滤后的书籍列表为空时，显示"暂无符合条件的书籍"提示
2. **状态不匹配**: 确保 `BookFilterStatus` 和 `BookReadingStatus` 的映射逻辑一致

## 测试策略

### 单元测试

1. 测试 `getFilterStatusColor()` 函数返回正确的颜色
2. 测试书籍过滤逻辑对每种状态返回正确的书籍列表

### UI 测试

1. 验证点击不同状态时，书籍列表正确更新
2. 验证状态指示器颜色正确变化
3. 验证下拉菜单中的圆点颜色正确显示

### 集成测试

1. 测试从"全部"切换到其他状态的完整流程
2. 测试在搜索模式和过滤模式之间切换时的行为

## 性能考虑

1. 使用 `remember` 缓存过滤后的书籍列表，避免不必要的重新计算
2. 过滤操作在内存中进行，不需要数据库查询
3. 状态颜色映射使用纯函数，性能开销可忽略

## 可访问性

1. 确保状态圆点有足够的对比度
2. 为状态指示器添加语义描述
3. 确保下拉菜单项可以通过键盘导航
