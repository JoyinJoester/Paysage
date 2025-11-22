# Legado 翻页模式 - 项目规格

## 概述

本项目旨在为 Paysage 阅读器实现多种翻页模式，学习并借鉴 Legado 阅读器的优秀翻页交互设计。

## 目标

- 实现 5 种翻页模式：仿真、滑动、覆盖、滚动、无动画
- 提供流畅的动画效果（≥30 FPS）
- 支持自定义配置和方向控制
- 优化性能和内存使用
- 提供良好的可访问性支持

## 文档结构

- **requirements.md** - 详细的功能需求和验收标准
- **design.md** - 架构设计和技术实现方案
- **tasks.md** - 分步实现任务列表

## 核心特性

### 1. 仿真翻页（Simulation）
- 使用贝塞尔曲线模拟真实书页卷曲
- 动态阴影和高光效果
- 支持拖拽和自动完成动画

### 2. 滑动翻页（Slide）
- 简洁流畅的滑动效果
- 页面跟随手指移动
- 快速响应和平滑过渡

### 3. 覆盖翻页（Cover）
- 下一页覆盖当前页
- 边缘阴影效果
- 类似杂志翻阅体验

### 4. 滚动翻页（Scroll）
- 垂直连续滚动
- 惯性滚动支持
- 适合长篇阅读

### 5. 无动画翻页（None）
- 即时切换页面
- 最低资源消耗
- 适合低端设备

## 技术栈

- **Kotlin** - 主要开发语言
- **Jetpack Compose** - UI 框架
- **Android Canvas** - 自定义绘制
- **Coroutines** - 异步处理
- **LruCache** - 位图缓存

## 架构设计

```
ReaderScreen (Compose UI)
    ↓
PageFlipContainer (Compose Wrapper)
    ↓
PageFlipView (Custom View)
    ↓
PageFlipManager (Mode Manager)
    ↓
PageDelegate (Abstract)
    ↓
├── SimulationPageDelegate
├── SlidePageDelegate
├── CoverPageDelegate
├── ScrollPageDelegate
└── NoAnimPageDelegate
```

## 开发计划

### 阶段 1: 核心框架（任务 1-2）
- 创建基础架构
- 实现管理器和缓存

### 阶段 2: 翻页模式（任务 3-7）
- 实现 5 种翻页模式
- 优化动画效果

### 阶段 3: UI 集成（任务 8-10）
- 创建自定义 View
- Compose 集成

### 阶段 4: 功能完善（任务 11-16）
- 手势处理
- 设置界面
- 性能优化

### 阶段 5: 测试和文档（任务 17-20）
- 单元测试
- UI 测试
- 性能测试
- 文档编写

## 性能目标

- 动画帧率：≥30 FPS（目标 60 FPS）
- 内存使用：≤100 MB（3 页缓存）
- 启动时间：≤500 ms
- 模式切换：≤100 ms

## 参考资源

- Legado 源码：`legado-master/app/src/main/java/io/legado/app/ui/book/read/page/`
- Android Canvas 文档
- Jetpack Compose 文档
- Material Design 动画指南

## 贡献指南

1. 阅读需求和设计文档
2. 选择任务开始实现
3. 遵循代码规范
4. 编写测试用例
5. 提交代码审查

## 许可证

本项目遵循 Paysage 项目的许可证。
