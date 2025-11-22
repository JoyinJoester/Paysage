# Legado Reader Migration - 实施指南

## 重要说明

本项目旨在将 Legado 阅读器的功能移植到 Paysage。由于 Legado 是开源项目（GPL-3.0 许可证），我们需要遵守其许可证要求。

### 许可证合规

1. **Legado 许可证**: GPL-3.0
2. **Paysage 许可证**: 需要确认
3. **合规要求**: 
   - 如果直接使用 Legado 代码，Paysage 也需要采用 GPL-3.0 或兼容许可证
   - 需要保留原始版权声明
   - 需要提供源代码访问

### 推荐方案

鉴于许可证问题，我建议采用以下方案之一：

#### 方案 A: 参考实现（推荐）
- 学习 Legado 的架构和设计思想
- 使用 Paysage 自己的实现
- 保持功能相似但代码独立
- 无许可证冲突

#### 方案 B: 直接集成
- 将 Legado 作为依赖库
- Paysage 采用 GPL-3.0 许可证
- 完全合规但限制了 Paysage 的许可证选择

#### 方案 C: 混合方案
- 核心功能参考实现
- 非核心功能直接使用（如果许可证兼容）
- 明确标注来源

## 当前项目状态

Paysage 已经实现了部分阅读器功能：
- ✅ 基础翻页动画（Slide, Cover, Simulation）
- ✅ 触摸区域检测
- ✅ 图片过滤
- ✅ 双页模式
- ✅ 性能优化（Bitmap 缓存、预加载）

## 需要从 Legado 学习的核心功能

### 1. 文本排版引擎
**Legado 的优势**:
- 精确的文本测量和换行
- 中文排版优化（避头尾）
- 段落和行间距处理

**实施建议**:
```kotlin
// 创建简化的文本排版系统
class TextLayoutEngine {
    fun layoutChapter(
        text: String,
        width: Int,
        height: Int,
        config: ReaderConfig
    ): List<TextPage> {
        // 实现文本分页逻辑
        // 参考 Legado 的 ChapterProvider 和 TextPageFactory
    }
}
```

### 2. ReadView 架构
**Legado 的优势**:
- 三页缓存机制（prev, cur, next）
- 统一的 PageDelegate 接口
- 高效的触摸事件处理

**实施建议**:
```kotlin
// 创建类似的 ReadView 架构
class PaysageReadView(context: Context) : FrameLayout(context) {
    private val prevPage: PageView
    private val curPage: PageView  
    private val nextPage: PageView
    private var pageDelegate: PageDelegate? = null
    
    // 实现页面切换和动画逻辑
}
```

### 3. 配置系统
**Legado 的优势**:
- 丰富的配置选项
- 实时配置更新
- 配置持久化

**实施建议**:
- 扩展 Paysage 现有的 SettingsRepository
- 添加阅读器特定的配置项
- 实现配置变更监听

### 4. 高级功能
- 文本选择和操作
- 自动翻页
- 搜索功能
- 朗读功能

## 实施步骤

### Phase 1: 核心架构（1-2周）
1. 设计 Paysage 的文本排版系统
2. 实现基础的 TextPage 数据结构
3. 创建 ReadView 架构

### Phase 2: 文本渲染（1-2周）
1. 实现文本测量和换行
2. 实现分页算法
3. 优化中文排版

### Phase 3: UI 集成（1周）
1. 创建 Compose 桥接层
2. 实现配置对话框
3. 集成到现有导航

### Phase 4: 高级功能（2-3周）
1. 文本选择
2. 搜索功能
3. 自动翻页
4. 其他功能

### Phase 5: 优化和测试（1-2周）
1. 性能优化
2. Bug 修复
3. 完整测试

## 技术参考

### Legado 核心概念

#### 1. TextPage 结构
```kotlin
data class TextPage(
    val index: Int,
    val lines: List<TextLine>,
    val chapterIndex: Int,
    val height: Float
)

data class TextLine(
    val text: String,
    val columns: List<TextColumn>,
    val lineTop: Float,
    val lineBottom: Float
)
```

#### 2. PageDelegate 模式
```kotlin
abstract class PageDelegate {
    abstract fun onTouch(event: MotionEvent): Boolean
    abstract fun onDraw(canvas: Canvas)
    abstract fun computeScroll()
}
```

#### 3. 文本排版流程
1. 加载章节内容
2. 根据配置计算可用空间
3. 逐字测量并换行
4. 生成 TextPage 列表
5. 缓存排版结果

## 下一步行动

1. **确认许可证策略**: 决定采用哪种方案
2. **代码审查**: 审查 Paysage 现有代码，确定可复用部分
3. **原型开发**: 创建文本排版引擎的原型
4. **迭代开发**: 按阶段实施功能

## 资源链接

- Legado GitHub: https://github.com/gedoor/legado
- Legado 文档: 参考项目 Wiki
- Android 文本渲染: Android Canvas 和 Paint API 文档

## 注意事项

1. **不要直接复制 Legado 代码**，除非明确遵守 GPL-3.0
2. **保持代码简洁**，只实现必要功能
3. **充分测试**，确保功能正确
4. **性能优先**，阅读器性能至关重要
5. **用户体验**，保持流畅的交互

## 联系和支持

如有问题，请参考：
- Legado 项目 Issues
- Android 开发文档
- Paysage 项目文档
