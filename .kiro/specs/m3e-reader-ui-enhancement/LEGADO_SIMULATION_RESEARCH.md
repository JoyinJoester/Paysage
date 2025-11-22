# Legado 仿真翻页算法研究

## 概述

Legado 的 SimulationPageDelegate 实现了逼真的书籍翻页效果，包括页面卷曲、阴影和光照。这是最复杂但也是最美观的翻页动画。

## 核心算法

### 1. 贝塞尔曲线计算

仿真翻页的核心是使用贝塞尔曲线来模拟页面卷曲的边缘。

**关键点：**
- **触摸点 (Touch Point)**: 用户手指的位置
- **角点 (Corner Point)**: 页面被卷起的角（通常是右上角或右下角）
- **控制点 (Control Points)**: 贝塞尔曲线的控制点，决定卷曲的形状

**计算步骤：**
1. 根据触摸点位置确定卷曲的角点
2. 计算触摸点和角点的中点
3. 使用中点和垂直线计算贝塞尔曲线的控制点
4. 绘制二次贝塞尔曲线作为卷曲边缘

### 2. 页面分区

仿真翻页将页面分为几个区域：
- **未卷曲区域**: 保持原样的部分
- **卷曲正面**: 正在翻起的页面正面
- **卷曲背面**: 翻起页面的背面（通常显示为灰色）
- **下一页**: 被揭示的新页面

### 3. 变换矩阵

使用变换矩阵实现页面卷曲的透视效果：
- 旋转变换：模拟页面翻转
- 缩放变换：模拟透视效果
- 平移变换：调整位置

### 4. 阴影效果

**两种阴影：**
1. **卷曲边缘阴影**: 在卷曲边缘绘制径向渐变阴影
2. **页面下方阴影**: 在翻起的页面下方绘制阴影，增强立体感

**实现方式：**
- 使用 RadialGradient 创建径向渐变
- 阴影透明度随卷曲程度变化
- 阴影位置跟随卷曲边缘

### 5. 光照效果

在卷曲区域添加高光效果，模拟光线照射：
- 在卷曲正面添加白色半透明渐变
- 光照强度随卷曲角度变化
- 增强真实感

## 关键参数

```kotlin
// 触摸点
var touchX: Float
var touchY: Float

// 角点
var cornerX: Float
var cornerY: Float

// 贝塞尔曲线控制点
var bezierControl1X: Float
var bezierControl1Y: Float
var bezierControl2X: Float
var bezierControl2Y: Float

// 阴影参数
val shadowAlpha = 0.3f
val shadowRadius = 50f

// 光照参数
val highlightAlpha = 0.2f
```

## 性能优化

1. **Canvas 硬件加速**: 启用硬件加速提高绘制性能
2. **减少过度绘制**: 只绘制可见区域
3. **使用 saveLayer**: 优化图层合成
4. **缓存计算结果**: 避免重复计算贝塞尔曲线

## 实现难点

1. **贝塞尔曲线计算**: 需要精确的数学计算
2. **变换矩阵**: 正确应用旋转和透视变换
3. **阴影绘制**: 径向渐变的位置和范围计算
4. **性能优化**: 保持 60fps 的流畅度

## 简化实现方案

对于 Paysage 的实现，我们可以采用简化方案：

1. **基础卷曲**: 使用简单的贝塞尔曲线
2. **基础阴影**: 使用线性渐变代替径向渐变
3. **省略光照**: 初期可以省略光照效果
4. **固定角点**: 简化角点计算逻辑

## 参考资源

- Legado 源码: `io.legado.app.ui.book.read.page.delegate.SimulationPageDelegate`
- 贝塞尔曲线: https://en.wikipedia.org/wiki/B%C3%A9zier_curve
- Canvas 变换: https://developer.android.com/reference/android/graphics/Canvas

## 下一步

基于以上研究，我们将实现一个简化版的仿真翻页动画，包含：
1. 基础的贝塞尔曲线卷曲效果
2. 简单的阴影效果
3. 页面分区绘制
4. 手势跟随支持
