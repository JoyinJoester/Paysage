# Requirements Document

## Introduction

本功能旨在改进书籍封面卡片上的阅读状态显示，将"阅读中"状态标签和进度百分比连接在一起，形成一个统一的视觉元素，提升用户体验和界面美观度。

## Glossary

- **BookCard**: 书籍封面卡片组件，用于在书库界面展示单本书籍的封面和基本信息
- **ReadingStatusBadge**: 阅读状态标签，显示书籍的当前阅读状态（阅读中、已完成、最新、未读等）
- **ProgressIndicator**: 进度指示器，显示书籍的阅读进度百分比
- **ConnectedBadge**: 连接式标签，将状态文字和进度百分比无缝连接的复合组件
- **LibraryScreen**: 书库主界面，包含书籍网格展示

## Requirements

### Requirement 1

**User Story:** 作为一个用户，我希望在书籍封面卡片上看到连接在一起的阅读状态和进度显示，以便更直观地了解阅读进度

#### Acceptance Criteria

1. WHEN THE BookCard 显示阅读中状态的书籍时，THE BookCard SHALL 在左上角渲染一个连接式标签，包含状态文字和进度百分比
2. THE ConnectedBadge SHALL 将状态文字部分和进度百分比部分无缝连接，中间无间隙
3. THE ConnectedBadge 的状态文字部分 SHALL 使用绿色背景（#4CAF50）
4. THE ConnectedBadge 的进度百分比部分 SHALL 使用深绿色背景（#2E7D32）
5. THE ConnectedBadge SHALL 保持圆角设计，左侧和右侧分别有圆角

### Requirement 2

**User Story:** 作为一个用户，我希望连接式标签的视觉效果与图片示例一致，以获得更好的视觉体验

#### Acceptance Criteria

1. THE ConnectedBadge 的左侧部分（状态文字） SHALL 具有左上和左下圆角
2. THE ConnectedBadge 的右侧部分（进度百分比） SHALL 具有右上和右下圆角
3. THE ConnectedBadge 的两个部分 SHALL 在中间位置完全贴合，无视觉间隙
4. THE ConnectedBadge 中的文字 SHALL 使用白色，确保在深色背景上清晰可读
5. THE ConnectedBadge SHALL 使用 Material Design 的 labelSmall 字体样式

### Requirement 3

**User Story:** 作为一个用户，我希望其他阅读状态（已完成、最新、未读）的显示保持不变，以保持界面一致性

#### Acceptance Criteria

1. WHEN THE BookCard 显示已完成状态的书籍时，THE BookCard SHALL 仅显示蓝色的"已完成"标签
2. WHEN THE BookCard 显示最新状态的书籍时，THE BookCard SHALL 仅显示橙红色的"最新"标签
3. WHEN THE BookCard 显示未读状态的书籍时，THE BookCard SHALL 仅显示深红色的"未读"标签
4. THE BookCard SHALL 为非阅读中状态保持现有的单一标签设计
5. THE BookCard SHALL 确保所有状态标签都位于封面左上角，具有相同的内边距（8dp）

### Requirement 4

**User Story:** 作为开发者，我希望代码结构清晰且可维护，以便未来进行修改和扩展

#### Acceptance Criteria

1. THE ConnectedBadge SHALL 作为一个独立的可组合函数实现
2. THE ConnectedBadge SHALL 接受状态文字、进度百分比、状态颜色和进度颜色作为参数
3. THE BookCard SHALL 根据阅读状态决定是否使用 ConnectedBadge 或单一标签
4. THE ConnectedBadge SHALL 使用 Row 布局将两个部分水平排列
5. THE ConnectedBadge SHALL 确保代码符合 Jetpack Compose 最佳实践
