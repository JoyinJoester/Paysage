# Requirements Document

## Introduction

完全重构 ReaderScreen 阅读器界面，删除所有现有代码并从零开始构建一个简洁、稳定的阅读器。同时删除设置中所有翻页动画相关的配置选项。新的阅读器将专注于核心阅读功能，提供简单可靠的用户体验。

## Glossary

- **ReaderScreen**: 应用的主要阅读器界面组件
- **PageFlipSettings**: 翻页动画设置界面组件
- **AppSettings**: 应用设置数据模型
- **SettingsViewModel**: 设置视图模型
- **ReaderViewModel**: 阅读器视图模型
- **Bitmap**: Android 图片位图对象
- **Compose**: Jetpack Compose UI 框架

## Requirements

### Requirement 1

**User Story:** 作为开发者，我想要删除所有现有的 ReaderScreen 代码，以便从零开始重构

#### Acceptance Criteria

1. WHEN 执行重构任务时，THE System SHALL 删除 ReaderScreen.kt 文件中的所有代码内容
2. WHEN 执行重构任务时，THE System SHALL 删除 EnhancedReaderScreen.kt 文件
3. WHEN 执行重构任务时，THE System SHALL 删除 PageFlipSettings.kt 文件
4. WHEN 执行重构任务时，THE System SHALL 删除所有翻页动画相关的组件文件

### Requirement 2

**User Story:** 作为开发者，我想要从设置中删除所有翻页动画配置，以便简化应用设置

#### Acceptance Criteria

1. WHEN 更新 AppSettings 数据模型时，THE System SHALL 删除 pageFlipMode 字段
2. WHEN 更新 AppSettings 数据模型时，THE System SHALL 删除所有翻页动画速度相关字段
3. WHEN 更新 SettingsViewModel 时，THE System SHALL 删除所有翻页动画相关的方法
4. WHEN 更新设置界面时，THE System SHALL 删除翻页动画设置选项

### Requirement 3

**User Story:** 作为用户，我想要一个简洁的阅读器界面，以便专注于阅读内容

#### Acceptance Criteria

1. THE ReaderScreen SHALL 显示当前页面的图片内容
2. WHEN 用户点击屏幕中央时，THE ReaderScreen SHALL 切换工具栏的显示/隐藏状态
3. WHEN 工具栏可见时，THE ReaderScreen SHALL 显示顶部导航栏和底部进度条
4. THE ReaderScreen SHALL 使用简单的淡入淡出动画显示/隐藏工具栏

### Requirement 4

**User Story:** 作为用户，我想要基本的翻页功能，以便浏览漫画内容

#### Acceptance Criteria

1. WHEN 用户向左滑动时，THE ReaderScreen SHALL 切换到下一页
2. WHEN 用户向右滑动时，THE ReaderScreen SHALL 切换到上一页
3. WHEN 翻页时，THE ReaderScreen SHALL 使用简单的淡入淡出过渡效果
4. WHEN 到达第一页或最后一页时，THE ReaderScreen SHALL 阻止继续翻页

### Requirement 5

**User Story:** 作为用户，我想要查看阅读进度，以便了解当前位置

#### Acceptance Criteria

1. WHEN 工具栏可见时，THE ReaderScreen SHALL 在顶部显示当前页码和总页数
2. WHEN 工具栏可见时，THE ReaderScreen SHALL 在底部显示进度滑块
3. WHEN 用户拖动进度滑块时，THE ReaderScreen SHALL 跳转到对应页面
4. THE ReaderScreen SHALL 在页码文本中显示格式为 "当前页 / 总页数"

### Requirement 6

**User Story:** 作为用户，我想要基本的缩放功能，以便查看图片细节

#### Acceptance Criteria

1. WHEN 用户双击图片时，THE ReaderScreen SHALL 在原始大小和放大2倍之间切换
2. WHEN 用户使用双指捏合手势时，THE ReaderScreen SHALL 缩放图片
3. WHEN 图片放大时，THE ReaderScreen SHALL 允许用户拖动图片查看不同区域
4. WHEN 图片缩放比例为1.0时，THE ReaderScreen SHALL 禁用拖动功能

### Requirement 7

**User Story:** 作为用户，我想要返回到书籍列表，以便选择其他书籍

#### Acceptance Criteria

1. WHEN 工具栏可见时，THE ReaderScreen SHALL 在顶部显示返回按钮
2. WHEN 用户点击返回按钮时，THE ReaderScreen SHALL 调用 onBackClick 回调
3. WHEN 用户点击返回按钮时，THE ReaderScreen SHALL 保存当前阅读进度
4. THE ReaderScreen SHALL 在返回前清理所有资源

### Requirement 8

**User Story:** 作为开发者，我想要清理所有未使用的翻页动画代码，以便减少代码复杂度

#### Acceptance Criteria

1. THE System SHALL 删除 PageFlipManager 及相关的 PageDelegate 实现
2. THE System SHALL 删除 PageTransitionController 和相关过渡动画组件
3. THE System SHALL 删除 PageFlipContainer 组件
4. THE System SHALL 删除 TouchZone 相关的所有代码和配置
