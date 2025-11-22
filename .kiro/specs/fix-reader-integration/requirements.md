# 阅读器集成修复需求文档

## 简介

修复当前阅读器的图片显示问题和集成Legado风格的阅读功能，确保漫画阅读体验流畅稳定。

## 术语表

- **ReaderScreen**: 当前使用的阅读器界面组件
- **EnhancedReaderScreen**: 已实现但未集成的增强版阅读器
- **PageFlipContainer**: Legado风格的翻页容器组件
- **TextReaderView**: 文本阅读视图组件
- **ContentFit**: 图片适配模式（FIT_WIDTH, FIT_HEIGHT, FIT_SCREEN等）

## 需求

### 需求 1: 修复图片显示问题

**用户故事**: 作为用户，我希望打开漫画时图片能正确适配屏幕，而不是放大显示只能看到一部分

#### 验收标准

1. WHEN 用户打开漫画，THE ReaderScreen SHALL 以适配屏幕的方式显示完整图片
2. WHEN 图片宽度大于高度，THE ReaderScreen SHALL 使用FIT_WIDTH模式适配
3. WHEN 图片高度大于宽度，THE ReaderScreen SHALL 使用FIT_HEIGHT模式适配
4. WHEN 用户双击图片，THE ReaderScreen SHALL 在原始大小和适配大小之间切换
5. WHEN 图片已缩放，THE ReaderScreen SHALL 允许用户拖动查看不同区域

### 需求 2: 修复黑屏和崩溃问题

**用户故事**: 作为用户，我希望阅读器稳定运行不会突然黑屏或退出

#### 验收标准

1. WHEN 加载图片失败，THE ReaderScreen SHALL 显示错误提示而不是黑屏
2. WHEN 内存不足，THE ReaderScreen SHALL 降低图片质量而不是崩溃
3. WHEN 翻页动画执行，THE ReaderScreen SHALL 正确管理Bitmap生命周期
4. WHEN 页面切换，THE ReaderScreen SHALL 及时释放不需要的图片资源
5. IF 发生异常，THEN THE ReaderScreen SHALL 捕获异常并显示友好提示

### 需求 3: 集成Legado阅读功能

**用户故事**: 作为用户，我希望使用之前实现的Legado风格阅读功能，包括文本阅读和增强的设置面板

#### 验收标准

1. THE ReaderScreen SHALL 支持图片和文本两种内容类型
2. WHEN 内容类型为文本，THE ReaderScreen SHALL 使用TextReaderView组件
3. WHEN 内容类型为图片，THE ReaderScreen SHALL 使用当前的图片阅读逻辑
4. THE ReaderScreen SHALL 集成QuickSettingsPanel快速设置面板
5. THE ReaderScreen SHALL 集成ReadingSettingsDialog完整设置对话框

### 需求 4: 优化翻页体验

**用户故事**: 作为用户，我希望翻页动画流畅自然，不会卡顿或出现视觉问题

#### 验收标准

1. WHEN 用户滑动翻页，THE PageFlipContainer SHALL 在200ms内完成动画
2. WHEN 翻页动画执行，THE PageFlipContainer SHALL 保持60fps帧率
3. WHEN 快速连续翻页，THE ReaderScreen SHALL 取消未完成的动画
4. THE PageFlipContainer SHALL 预加载下一页图片以减少等待时间
5. WHEN 翻页失败，THE ReaderScreen SHALL 回退到当前页面

### 需求 5: 改进错误处理

**用户故事**: 作为用户，我希望遇到问题时能看到清晰的错误信息和解决建议

#### 验收标准

1. WHEN 图片加载失败，THE ReaderScreen SHALL 显示"图片加载失败，点击重试"
2. WHEN 文件不存在，THE ReaderScreen SHALL 显示"文件已被移动或删除"
3. WHEN 内存不足，THE ReaderScreen SHALL 显示"内存不足，已降低图片质量"
4. WHEN 格式不支持，THE ReaderScreen SHALL 显示"不支持的文件格式"
5. THE ReaderScreen SHALL 提供"重试"按钮允许用户重新加载
