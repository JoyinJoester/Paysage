# 阅读历史功能更新

## 更新概述

将历史记录功能从"下载历史"改为"阅读历史"，UI风格匹配参考图片。

## 主要变更

### 1. 数据模型 (HistoryItem.kt)
- ✅ 移除：`downloadTime`, `status` (DownloadStatus)
- ✅ 新增：`bookId`, `author`, `lastReadTime`, `currentPage`, `totalPages`
- ✅ 表名：`download_history` → `reading_history`

### 2. 数据访问层 (HistoryDao.kt)
- ✅ 更新所有SQL查询以使用新表名 `reading_history`
- ✅ 新增：`getHistoryByBookId()` - 根据书籍ID查询
- ✅ 更新：`updateProgress()` - 支持页码和时间戳
- ✅ 新增：`updateLastReadTime()` - 更新最后阅读时间

### 3. 业务逻辑层 (HistoryRepository.kt)
- ✅ 移除：`updateStatus()` 方法
- ✅ 新增：`addOrUpdateHistory()` - 智能添加或更新
- ✅ 新增：`getHistoryByBookId()` - 按书籍ID查询
- ✅ 更新：`updateReadingProgress()` - 支持进度、页码、时间

### 4. UI组件 (HistoryComponents.kt)
- ✅ 重新设计 `HistoryListItem` 匹配参考图片风格：
  - 左侧：90x120dp 封面缩略图
  - 右侧：书名、作者、文件信息、阅读时间、进度条
  - 使用 Surface 替代 Card，更简洁的设计
- ✅ 更新：`formatReadTime()` 替代 `formatDownloadTime()`
- ✅ 显示作者信息（如果有）
- ✅ 显示阅读时间图标

### 5. ViewModel (HistoryViewModel.kt)
- ✅ 移除：`updateStatus()` 方法
- ✅ 新增：`addOrUpdateHistory()` - 添加或更新阅读记录
- ✅ 更新：`updateReadingProgress()` - 更新阅读进度和页码

### 6. 数据库迁移 (PaysageDatabase.kt)
- ✅ 版本：8 → 9
- ✅ 新增：MIGRATION_8_9
  - 删除旧的 `download_history` 表
  - 创建新的 `reading_history` 表
  - 创建索引：`last_read_time`, `book_id`

### 7. 字符串资源
- ✅ 更新中文：`history_empty` → "暂无阅读记录"
- ✅ 更新中文：`history_clear_confirm` → "确定要清空所有阅读记录吗？"
- ✅ 新增：`history_continue_reading` → "继续阅读"

### 8. 测试辅助工具 (HistoryTestHelper.kt)
- ✅ 更新测试数据以匹配新的数据结构
- ✅ 添加作者信息、页码等字段
- ✅ 使用 `addOrUpdateHistory()` 方法

## UI设计特点

参考图片中的历史记录页面特点：
- 每个条目显示封面、书名、作者、文件类型、大小、阅读时间
- 底部有阅读进度条
- 简洁的卡片式设计
- 时间戳格式：yyyy年MM月dd日 HH:mm:ss

## 数据库变更

```sql
-- 旧表（已删除）
download_history (
  id, title, thumbnail_path, file_type, file_size, 
  file_path, download_time, progress, status
)

-- 新表
reading_history (
  id, book_id, title, author, thumbnail_path, 
  file_type, file_size, file_path, last_read_time, 
  progress, current_page, total_pages
)
```

## 自动记录阅读历史

阅读历史会在用户阅读书籍时自动记录：
- 在 `ReaderViewModel.loadPage()` 中调用 `updateReadingHistory()`
- 每次翻页都会更新历史记录
- 包含书籍信息、阅读进度、当前页码等

## 注意事项

- 数据库迁移会删除所有旧的下载历史数据
- 新的历史记录基于书籍ID，同一本书只保留一条记录
- 每次阅读会自动更新 `lastReadTime` 和 `progress`
- 历史记录更新失败不会影响阅读体验（静默失败）
