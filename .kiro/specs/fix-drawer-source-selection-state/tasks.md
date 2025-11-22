# Implementation Plan

- [x] 1. 修复 NavigationViewModel 中的状态重置逻辑





  - 在 `selectPrimaryItem` 方法中添加 `showSourceSelection = false` 以重置源选择状态
  - 确保状态更新是原子性的，使用 `copy()` 方法
  - 验证 `saveState` 方法正确保存更新后的状态
  - _Requirements: 1.1, 1.3, 2.1, 2.2, 2.3_

- [ ] 2. 添加单元测试验证修复




  - 创建测试用例验证 `selectPrimaryItem` 会重置 `showSourceSelection`
  - 测试多次切换一层菜单项的场景
  - 测试状态持久化和恢复的场景
  - _Requirements: 2.4_

- [ ] 3. 手动测试验证修复效果
  - 测试基本流程：选择文件夹 → 切换菜单项 → 验证内容正确
  - 测试多次切换场景
  - 测试配置变更（旋转屏幕）后的状态恢复
  - _Requirements: 1.2, 1.3, 1.4_
