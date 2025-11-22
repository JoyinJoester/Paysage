# Implementation Plan

- [x] 1. 创建数据模型和持久化层





  - 创建SyncOptions、SyncType、SyncResult数据类
  - 实现SyncOptionsRepository使用DataStore进行持久化
  - 添加rememberSyncOptions Composable函数
  - _Requirements: 1.1, 9.1, 9.2, 9.3_

- [x] 2. 实现M3E风格的下拉刷新指示器





  - 创建M3EPullRefreshIndicator组件
  - 实现基于下拉距离的动态缩放和透明度动画
  - 使用ExpressiveAnimations.bouncySpring实现弹性动画
  - 集成CircularProgressIndicator使用primary颜色
  - _Requirements: 1.1, 1.3, 1.5_

- [x] 3. 在LibraryScreen中集成PullToRefreshBox



  - 添加PullToRefreshBox包裹现有内容
  - 实现下拉手势触发逻辑
  - 添加showSyncDialog状态管理
  - 连接M3EPullRefreshIndicator
  - _Requirements: 1.1, 1.2, 1.4_

- [x] 4. 创建LibrarySyncDialog基础结构





  - 实现Dialog容器，宽度为屏幕80%，最大600dp
  - 使用MaterialTheme.shapes.extraLarge作为形状
  - 添加标题和滚动容器
  - 实现对话框外部点击关闭功能
  - _Requirements: 6.1, 6.2, 6.5, 8.4, 8.5_

- [x] 5. 实现FileTypeCard组件




  - 创建FileTypeCard显示文件类型信息
  - 实现FileTypeSection显示漫画文件格式（.cbz, .cbr, .cbt, .cb7）
  - 实现FileTypeSection显示压缩文件格式（.zip, .rar, .7z, .tar）
  - 使用FlowRow布局和AssistChip展示格式
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 6. 实现SyncCheckbox组件


  - 创建带波纹效果的复选框组件
  - 实现点击整行切换状态
  - 添加无障碍语义标注（contentDescription, stateDescription, role）
  - 使用primary颜色和300ms动画时长
  - _Requirements: 3.5, 4.5, 8.1, 8.3, 10.1, 10.3_

- [x] 7. 实现MaintenanceCard组件


  - 创建MaintenanceCard卡片容器
  - 添加"移出已删除文件"复选框，默认未选中
  - 添加"从修改过的文件更新数据"复选框，默认未选中
  - 添加"生成已删除的缩略图"复选框，默认未选中
  - 实现选项状态变化回调
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.6_

- [x] 8. 实现MoreOptionsCard组件


  - 创建MoreOptionsCard卡片容器
  - 添加"扫描子文件夹"复选框，默认选中
  - 添加"跳过被定义为隐藏的库文件夹"复选框，默认选中
  - 添加"并行同步"复选框，默认未选中
  - 实现选项状态变化回调
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.6_

- [x] 9. 实现ActionButtons组件


  - 创建ActionButtons容器，与内容区保持16dp间距
  - 实现"开始完整同步"主按钮（primary颜色）
  - 实现"维护"按钮（outlined样式）
  - 实现"同步"按钮（filledTonal样式）
  - 添加按钮图标和波纹反馈效果
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 8.2_

- [x] 10. 组装LibrarySyncDialog完整对话框


  - 集成FileTypeCard、MaintenanceCard、MoreOptionsCard
  - 设置卡片间16dp间距，内部元素8dp间距
  - 集成ActionButtons并连接回调
  - 实现选项状态管理和持久化
  - _Requirements: 6.3, 6.4, 9.1, 9.2_

- [x] 11. 扩展LibraryViewModel支持同步操作


  - 添加syncOptions StateFlow从SyncOptionsRepository
  - 实现performSync方法接收SyncType和SyncOptions
  - 在执行同步前保存选项到DataStore
  - 更新uiState显示同步进度和结果
  - 实现错误处理和重试逻辑
  - _Requirements: 7.1, 7.2, 9.4_

- [x] 12. 在BookRepository中实现同步逻辑

  - 实现executeMaintenance方法处理维护操作
  - 实现executeIncrementalSync方法处理增量同步
  - 实现executeFullSync方法处理完整同步
  - 实现scanFiles方法支持并行和串行扫描
  - 添加removeDeletedFiles、updateModifiedFiles、generateMissingThumbnails辅助方法
  - 实现lastSyncTimestamp的存储和读取
  - _Requirements: 5.2, 5.3, 5.4_

- [x] 13. 添加字符串资源


  - 在values/strings.xml添加英文字符串资源
  - 在values-zh/strings.xml添加中文字符串资源
  - 包含所有对话框标题、标签、描述和提示文本
  - _Requirements: 2.1, 3.1, 4.1, 5.1, 7.4, 10.2, 10.4_

- [x] 14. 实现同步结果反馈


  - 在LibraryScreen中显示同步进度CircularProgressIndicator
  - 同步完成后显示Snackbar提示结果
  - Snackbar显示新增和更新书籍数量
  - 添加"确定"按钮关闭Snackbar
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 15. 实现错误处理和恢复


  - 添加权限检查逻辑
  - 实现withRetry重试机制（最多3次）
  - 捕获并处理IOException、SecurityException、SQLiteException
  - 在SyncResult中记录错误信息
  - 显示错误对话框或Snackbar
  - _Requirements: 9.4_

- [x] 16. 添加无障碍支持


  - 为所有按钮添加contentDescription
  - 为复选框添加stateDescription
  - 实现键盘导航支持（Tab、Enter、Escape）
  - 确保触摸目标最小48dp
  - 验证颜色对比度符合WCAG AA标准
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

- [ ] 17. 编写单元测试
- [ ] 17.1 测试SyncOptions持久化
  - 测试保存和加载SyncOptions
  - 测试默认值处理
  - _Requirements: 9.1, 9.2, 9.3_

- [ ] 17.2 测试同步逻辑
  - 测试增量同步仅处理新增和修改文件
  - 测试完整同步处理所有文件
  - 测试维护操作的各个选项
  - _Requirements: 5.2, 5.3, 5.4_

- [ ] 17.3 测试错误处理
  - 测试权限错误处理
  - 测试文件访问错误处理
  - 测试重试机制
  - _Requirements: 9.4_

- [ ] 18. 编写UI测试
- [ ] 18.1 测试下拉刷新交互
  - 测试下拉手势显示对话框
  - 测试下拉指示器动画
  - _Requirements: 1.1, 1.2, 1.3_

- [ ] 18.2 测试对话框交互
  - 测试复选框切换
  - 测试按钮点击
  - 测试对话框关闭
  - _Requirements: 3.5, 4.5, 5.6, 8.4_

- [ ] 18.3 测试无障碍功能
  - 测试TalkBack朗读
  - 测试键盘导航
  - _Requirements: 10.3, 10.4, 10.5_

- [x] 19. 性能优化和测试



  - 实现分批处理（每批100个文件）
  - 限制并行协程数量（最多4个）
  - 使用批量数据库插入
  - 测试并行同步性能提升
  - _Requirements: 4.3_
