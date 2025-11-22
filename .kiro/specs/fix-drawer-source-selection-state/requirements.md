# Requirements Document

## Introduction

修复两层导航抽屉中的状态管理问题：当用户在一层抽屉中选择"选择文件夹"页面后，切换到其他一层菜单项（如设置、关于等）时，第二层抽屉仍然显示"选择文件夹"页面，而不是显示对应菜单项的正确内容。

## Glossary

- **NavigationViewModel**: 管理两层导航抽屉状态的 ViewModel
- **showSourceSelection**: 布尔状态标志，控制是否显示源选择页面
- **selectedPrimaryItem**: 当前选中的一层导航项
- **SecondaryDrawerContent**: 第二层抽屉内容组件

## Requirements

### Requirement 1

**User Story:** 作为用户，当我点击一层抽屉的不同菜单项时，我希望第二层抽屉显示对应菜单项的正确内容，而不是一直显示之前的源选择页面

#### Acceptance Criteria

1. WHEN 用户选择一层导航项，THE NavigationViewModel SHALL 将 showSourceSelection 状态重置为 false
2. WHEN 用户点击"选择文件夹"按钮，THE NavigationViewModel SHALL 将 showSourceSelection 状态设置为 true
3. WHEN showSourceSelection 为 false 且 selectedPrimaryItem 改变，THE SecondaryDrawerContent SHALL 显示与 selectedPrimaryItem 对应的内容
4. WHEN showSourceSelection 为 true，THE SecondaryDrawerContent SHALL 显示源选择页面，无论 selectedPrimaryItem 的值是什么

### Requirement 2

**User Story:** 作为开发者，我希望导航状态管理逻辑清晰且易于维护，确保状态转换的一致性

#### Acceptance Criteria

1. THE NavigationViewModel SHALL 在 selectPrimaryItem 方法中自动重置 showSourceSelection 为 false
2. THE NavigationViewModel SHALL 在状态更新时同步保存到 SavedStateHandle
3. THE NavigationViewModel SHALL 确保所有状态转换都是原子性的
4. THE NavigationViewModel SHALL 提供明确的方法来控制 showSourceSelection 状态
