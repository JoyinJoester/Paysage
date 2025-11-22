# Requirements Document

## Introduction

本需求文档定义了对库布局"紧凑网格"视图中书籍卡片的UI改进。当前的紧凑网格卡片在封面下方有一个独立的信息区域，显示标题、页数和进度条。新设计将移除这个独立区域，改为在封面底部直接叠加显示标题和进度条，创造更紧凑、更现代的视觉效果。

## Glossary

- **CompactGridCard**: 紧凑网格布局中使用的书籍卡片组件，显示封面和基本信息
- **OverlayProgressBar**: 叠加在封面底部的进度条，显示阅读进度
- **OverlayTitle**: 叠加在进度条上方的书籍标题文本
- **BookCompactCard**: Compose组件函数，实现紧凑网格卡片的UI
- **LibraryLayout**: 库视图的布局模式枚举（列表、紧凑网格、纯封面）

## Requirements

### Requirement 1

**User Story:** 作为用户，我希望紧凑网格视图中的卡片更加紧凑，以便在屏幕上显示更多书籍

#### Acceptance Criteria

1. WHEN 用户选择紧凑网格布局，THE CompactGridCard SHALL 移除封面下方的独立信息区域
2. THE CompactGridCard SHALL 在封面图片底部直接叠加显示标题和进度条
3. THE OverlayTitle SHALL 显示在进度条的上方
4. THE OverlayProgressBar SHALL 显示在封面的最底部
5. THE CompactGridCard SHALL 保持0.7的宽高比，仅显示封面区域

### Requirement 2

**User Story:** 作为用户，我希望在紧凑网格视图中仍然能清晰看到书籍标题，以便快速识别书籍

#### Acceptance Criteria

1. THE OverlayTitle SHALL 使用半透明深色背景以确保文字可读性
2. THE OverlayTitle SHALL 最多显示2行文本
3. WHEN 标题超过2行，THE OverlayTitle SHALL 使用省略号截断
4. THE OverlayTitle SHALL 使用白色文字颜色
5. THE OverlayTitle SHALL 使用titleSmall字体样式

### Requirement 3

**User Story:** 作为用户，我希望在紧凑网格视图中看到阅读进度，以便了解每本书的阅读状态

#### Acceptance Criteria

1. WHEN showProgress设置为true，THE OverlayProgressBar SHALL 显示在封面底部
2. THE OverlayProgressBar SHALL 使用LinearProgressIndicator组件
3. THE OverlayProgressBar SHALL 根据currentPage和totalPages计算进度百分比
4. THE OverlayProgressBar SHALL 使用6dp的高度
5. THE OverlayProgressBar SHALL 使用主题色作为进度颜色

### Requirement 4

**User Story:** 作为用户，我希望叠加的标题和进度条不会完全遮挡封面，以便仍能看到封面的主要内容

#### Acceptance Criteria

1. THE OverlayTitle SHALL 使用渐变背景，从底部的半透明黑色渐变到顶部的完全透明
2. THE OverlayTitle SHALL 占据封面底部约30%的高度
3. THE OverlayProgressBar SHALL 紧贴封面底部边缘
4. THE CompactGridCard SHALL 保持封面的上部70%区域清晰可见
5. THE OverlayTitle 和 OverlayProgressBar SHALL 使用Box布局叠加在封面上

### Requirement 5

**User Story:** 作为用户，我希望紧凑网格视图中的状态标签仍然可见，以便快速了解书籍的阅读状态

#### Acceptance Criteria

1. THE CompactGridCard SHALL 在左上角保留状态标签显示
2. THE StatusBadge SHALL 显示在封面的最上层
3. THE StatusBadge SHALL 与封面边缘保持8dp的内边距
4. WHEN 书籍状态为阅读中，THE StatusBadge SHALL 使用ConnectedReadingStatusBadge组件
5. WHEN 书籍状态为其他状态，THE StatusBadge SHALL 显示相应的状态文字和颜色
