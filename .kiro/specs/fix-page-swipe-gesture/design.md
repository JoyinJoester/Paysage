# Design Document

## Overview

修复阅读器翻页手势的核心问题:将基于累积拖动距离的检测改为基于单次手势的检测。主要修改 `ReaderTransitionHelper.kt` 和 `PageView` 中的手势处理逻辑,确保一次滑动手势只触发一次翻页操作。

## Architecture

### 当前问题分析

当前实现使用 `detectHorizontalDragGestures` 和 `detectVerticalDragGestures`,这些函数在拖动过程中会持续触发回调,传入的 `dragAmount` 是每次回调的增量距离。当前代码在每次回调中都检查 `dragAmount > 50` 或 `dragAmount < -50`,导致一次长距离滑动会多次满足条件,触发多次翻页。

### 解决方案

使用 `detectDragGestures` 或 `detectDragGesturesAfterLongPress` 的完整生命周期:
- `onDragStart`: 记录起始位置
- `onDrag`: 累积拖动距离
- `onDragEnd`: 根据总距离判断是否翻页

或者使用状态标志防止重复触发:
- 添加 `isGestureHandled` 标志
- 在第一次满足条件时设置标志并触发翻页
- 在手势结束时重置标志

## Components and Interfaces

### 1. 修改 PageViewWithTransition

**位置**: `app/src/main/java/takagi/ru/paysage/reader/transition/ReaderTransitionHelper.kt`

**修改内容**:
- 使用 `detectDragGestures` 替代 `detectHorizontalDragGestures` 和 `detectVerticalDragGestures`
- 添加手势状态管理
- 在 `onDragEnd` 中根据总距离判断翻页方向

**新增状态**:
```kotlin
var dragStartOffset by remember { mutableStateOf(Offset.Zero) }
var currentDragOffset by remember { mutableStateOf(Offset.Zero) }
var isGestureActive by remember { mutableStateOf(false) }
```

### 2. 修改 PageView (单页视图)

**位置**: `app/src/main/java/takagi/ru/paysage/ui/screens/ReaderScreen.kt`

**修改内容**:
- 同样使用 `detectDragGestures` 进行统一处理
- 确保手势逻辑与 `PageViewWithTransition` 一致

### 3. 修改 DualPageView (双页视图)

**位置**: `app/src/main/java/takagi/ru/paysage/ui/screens/ReaderScreen.kt`

**修改内容**:
- 应用相同的手势处理逻辑
- 确保双页模式下也不会出现连续翻页

## Data Models

### GestureState (新增)

```kotlin
data class GestureState(
    val isActive: Boolean = false,
    val startOffset: Offset = Offset.Zero,
    val currentOffset: Offset = Offset.Zero,
    val isHandled: Boolean = false
) {
    val totalDragX: Float
        get() = currentOffset.x - startOffset.x
    
    val totalDragY: Float
        get() = currentOffset.y - startOffset.y
}
```

## Error Handling

### 手势冲突处理

- 当用户正在缩放时,禁用翻页手势
- 当过渡动画正在进行时,忽略新的翻页手势
- 确保手势状态在异常情况下能正确重置

### 边界情况

- 第一页时向前翻页:不触发任何操作
- 最后一页时向后翻页:不触发任何操作
- 快速连续滑动:只响应第一次手势,后续手势在动画完成前被忽略

## Testing Strategy

### 单元测试

- 测试手势状态管理逻辑
- 测试距离计算的正确性
- 测试边界条件处理

### 集成测试

- 测试单页模式下的翻页手势
- 测试双页模式下的翻页手势
- 测试不同阅读方向下的手势行为

### 手动测试场景

1. 短距离滑动(< 50px):不应翻页
2. 中等距离滑动(50-200px):应翻一页
3. 长距离滑动(> 200px):应只翻一页
4. 快速滑动(高速度):应翻一页
5. 连续滑动:每次滑动只翻一页

## Implementation Notes

### 关键修改点

1. **使用 detectDragGestures**:
   - 提供完整的手势生命周期
   - 可以在 onDragEnd 中统一处理翻页逻辑

2. **添加防抖机制**:
   - 使用 `isGestureHandled` 标志
   - 确保一次手势只触发一次翻页

3. **保持现有功能**:
   - 不影响缩放功能
   - 不影响触摸区域检测
   - 不影响过渡动画效果

### 性能考虑

- 手势检测应该是轻量级的
- 避免在拖动过程中进行重度计算
- 使用 remember 缓存状态以减少重组

### 兼容性

- 确保与现有的触摸区域系统兼容
- 确保与过渡动画系统兼容
- 确保在不同阅读方向下都能正常工作
