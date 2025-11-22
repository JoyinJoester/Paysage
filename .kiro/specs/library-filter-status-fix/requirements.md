# 需求文档

## 简介

修复图书馆界面的状态筛选功能，使其能够正确过滤书籍，并改进视觉反馈，包括状态指示器颜色和下拉菜单中的状态圆点。

## 术语表

- **LibraryScreen**: 图书馆主界面组件
- **LibraryFilterBar**: 图书馆过滤栏组件
- **BookFilterStatus**: 书籍过滤状态枚举（ALL, LATEST, READING, FINISHED, UNREAD）
- **BookReadingStatus**: 书籍阅读状态枚举（UNREAD, READING, FINISHED, LATEST）
- **StatusIndicator**: 左侧的圆形状态指示器
- **DropdownMenu**: 状态选择下拉菜单

## 需求

### 需求 1

**用户故事:** 作为用户，我想要通过状态筛选器过滤书籍列表，以便只查看特定状态的书籍

#### 验收标准

1. WHEN 用户选择"最新"状态，THE LibraryScreen SHALL 仅显示7天内添加的书籍
2. WHEN 用户选择"阅读中"状态，THE LibraryScreen SHALL 仅显示当前页码大于0且未完成的书籍
3. WHEN 用户选择"已读"状态，THE LibraryScreen SHALL 仅显示已标记为完成的书籍
4. WHEN 用户选择"未读"状态，THE LibraryScreen SHALL 仅显示当前页码为0且不是最新添加的书籍
5. WHEN 用户选择"全部"状态，THE LibraryScreen SHALL 显示所有书籍

### 需求 2

**用户故事:** 作为用户，我想要看到左侧状态指示器的颜色随选中状态变化，以便直观了解当前筛选的状态

#### 验收标准

1. WHEN 用户选择"最新"状态，THE StatusIndicator SHALL 显示橙红色（#FF5722）
2. WHEN 用户选择"阅读中"状态，THE StatusIndicator SHALL 显示绿色（#4CAF50）
3. WHEN 用户选择"已读"状态，THE StatusIndicator SHALL 显示蓝色（#2196F3）
4. WHEN 用户选择"未读"状态，THE StatusIndicator SHALL 显示深红色（#C62828）
5. WHEN 用户选择"全部"状态，THE StatusIndicator SHALL 显示主题色（MaterialTheme.colorScheme.primary）

### 需求 3

**用户故事:** 作为用户，我想要在下拉菜单中看到每个状态选项前面有对应颜色的圆点，以便更容易识别不同状态

#### 验收标准

1. WHEN 用户打开状态筛选下拉菜单，THE DropdownMenu SHALL 在每个状态选项前显示一个圆形指示器
2. THE DropdownMenu SHALL 为"最新"选项显示橙红色圆点（#FF5722）
3. THE DropdownMenu SHALL 为"阅读中"选项显示绿色圆点（#4CAF50）
4. THE DropdownMenu SHALL 为"已读"选项显示蓝色圆点（#2196F3）
5. THE DropdownMenu SHALL 为"未读"选项显示深红色圆点（#C62828）
6. THE DropdownMenu SHALL 为"全部"选项显示主题色圆点（MaterialTheme.colorScheme.primary）
