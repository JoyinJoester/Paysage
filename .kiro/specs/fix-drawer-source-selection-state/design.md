# Design Document

## Overview

修复两层导航抽屉中的状态管理 bug，确保当用户切换一层导航项时，源选择页面状态能够正确重置。这是一个简单的状态管理修复，主要涉及 NavigationViewModel 中的状态更新逻辑。

## Architecture

### 受影响的组件

1. **NavigationViewModel** - 需要修改 `selectPrimaryItem` 方法
2. **MainActivity** - 可能需要调整状态管理逻辑（如果需要）
3. **SecondaryDrawerContent** - 无需修改，已有正确的条件渲染逻辑

### 状态流转图

```
用户操作 → NavigationViewModel 状态更新 → UI 重新渲染

正常流程：
1. 点击一层菜单项 → selectPrimaryItem() → showSourceSelection = false
2. 点击"选择文件夹" → toggleSourceSelection(true) → showSourceSelection = true
3. 再次点击一层菜单项 → selectPrimaryItem() → showSourceSelection = false (修复点)
```

## Components and Interfaces

### NavigationViewModel 修改

**当前实现问题：**
```kotlin
fun selectPrimaryItem(item: PrimaryNavItem) {
    _navigationState.update { currentState ->
        val newState = currentState.copy(
            selectedPrimaryItem = item,
            isSecondaryDrawerOpen = true
            // 缺少: showSourceSelection = false
        )
        saveState(newState)
        newState
    }
}
```

**修复后的实现：**
```kotlin
fun selectPrimaryItem(item: PrimaryNavItem) {
    _navigationState.update { currentState ->
        val newState = currentState.copy(
            selectedPrimaryItem = item,
            isSecondaryDrawerOpen = true,
            showSourceSelection = false  // 重置源选择状态
        )
        saveState(newState)
        newState
    }
}
```

### 状态保存逻辑

`saveState` 方法已经正确处理了 `showSourceSelection` 的保存，无需修改：

```kotlin
private fun saveState(state: NavigationState) {
    savedStateHandle["selected_primary_item"] = state.selectedPrimaryItem.name
    savedStateHandle["is_drawer_open"] = state.isSecondaryDrawerOpen
    savedStateHandle["selected_secondary_item"] = state.selectedSecondaryItem
    savedStateHandle["show_source_selection"] = state.showSourceSelection
}
```

## Data Models

无需修改现有数据模型。`NavigationState` 已经包含了所有必要的字段：

```kotlin
data class NavigationState(
    val selectedPrimaryItem: PrimaryNavItem = PrimaryNavItem.LocalLibrary,
    val isSecondaryDrawerOpen: Boolean = false,
    val selectedSecondaryItem: String? = null,
    val showSourceSelection: Boolean = false
)
```

## Error Handling

这个修复不涉及错误处理，因为：
1. 状态更新是同步的
2. 不涉及外部 API 调用
3. 不涉及数据持久化失败的情况

## Testing Strategy

### 手动测试场景

1. **场景 1：基本流程**
   - 打开应用
   - 点击"选择文件夹"按钮
   - 验证：第二层抽屉显示源选择页面
   - 点击"设置"一层菜单项
   - 验证：第二层抽屉显示设置内容（而不是源选择页面）

2. **场景 2：多次切换**
   - 点击"选择文件夹"
   - 点击"本地书库"
   - 点击"在线书库"
   - 点击"关于"
   - 验证：每次切换都显示正确的内容

3. **场景 3：状态持久化**
   - 点击"选择文件夹"
   - 旋转屏幕或切换到后台
   - 返回应用
   - 点击其他一层菜单项
   - 验证：状态正确恢复且切换正常

### 单元测试（可选）

可以为 NavigationViewModel 添加单元测试：

```kotlin
@Test
fun `selectPrimaryItem should reset showSourceSelection`() {
    // Given
    viewModel.toggleSourceSelection(true)
    
    // When
    viewModel.selectPrimaryItem(PrimaryNavItem.Settings)
    
    // Then
    val state = viewModel.navigationState.value
    assertFalse(state.showSourceSelection)
    assertEquals(PrimaryNavItem.Settings, state.selectedPrimaryItem)
}
```

## Implementation Notes

1. **最小化修改**：只需要在 `selectPrimaryItem` 方法中添加一行代码
2. **向后兼容**：这个修改不会影响现有功能
3. **性能影响**：无性能影响，只是状态更新
4. **副作用**：无副作用，这是预期的行为修复

## Alternative Approaches Considered

### 方案 1：在 SecondaryDrawerContent 中处理（不推荐）
- 在 UI 层添加逻辑来检测 primaryItem 变化并重置状态
- 缺点：违反单一职责原则，状态管理应该在 ViewModel 中

### 方案 2：添加新的状态重置方法（过度设计）
- 创建一个单独的 `resetSourceSelection()` 方法
- 缺点：增加了不必要的复杂性，直接在 `selectPrimaryItem` 中处理更简洁

### 方案 3：使用 StateFlow 的 combine 操作符（过度设计）
- 使用响应式编程自动重置状态
- 缺点：增加了代码复杂度，对于这个简单的场景来说过度设计

**选择的方案**：直接在 `selectPrimaryItem` 中重置 `showSourceSelection`，这是最简单、最直接的解决方案。
