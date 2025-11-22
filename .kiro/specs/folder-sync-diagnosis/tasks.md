# 实施计划 - 文件夹同步问题诊断与修复

## 任务列表

- [x] 1. 扩展数据访问层


  - 在 FolderDao 中添加诊断相关的查询方法
  - 实现按模块类型统计文件夹数量的方法
  - 实现查找跨模块重复文件夹的方法
  - 实现获取唯一父路径的方法
  - 实现查找共享路径的方法
  - _Requirements: 1.1, 1.2, 2.1, 2.2_

- [x] 2. 实现核心诊断组件

- [x] 2.1 创建数据模型


  - 创建 DiagnosticReport 数据类
  - 创建 IsolationStatus 数据类
  - 创建 PathConflict 数据类
  - 创建 DuplicateFolder 数据类
  - 创建 FolderStatistics 数据类
  - _Requirements: 1.5, 6.2_

- [x] 2.2 实现 DatabaseAnalyzer


  - 实现按模块统计文件夹数量的方法
  - 实现查找潜在重复文件夹的方法
  - 实现获取唯一父路径的方法
  - 实现相似度计算算法
  - _Requirements: 1.1, 1.2, 2.1_

- [x] 2.3 实现 FileSystemAnalyzer


  - 实现路径冲突分析方法
  - 实现路径存在性验证方法
  - 实现路径权限检查方法
  - 实现符号链接检测方法
  - _Requirements: 2.2, 2.3, 2.4, 2.5_

- [x] 2.4 实现 IsolationValidator


  - 实现查询验证方法
  - 实现文件夹操作验证方法
  - 实现完整隔离检查方法
  - 定义 ValidationResult 和 IsolationViolation 数据类
  - _Requirements: 1.3, 1.4, 5.2, 5.3_

- [x] 3. 实现诊断日志系统

- [x] 3.1 创建 DiagnosticLogger


  - 实现日志记录方法
  - 实现日志读取方法
  - 实现操作历史查询方法
  - 实现操作模式分析方法
  - 实现可疑模式检测算法
  - _Requirements: 3.1, 3.2, 6.1, 6.2, 6.5_

- [x] 3.2 定义日志数据模型


  - 创建 LogEntry 数据类
  - 定义 FolderOperation 枚举
  - 定义 OperationResult 枚举
  - 创建 OperationPatternReport 数据类
  - 创建 SuspiciousPattern 数据类
  - _Requirements: 6.2, 6.3_

- [x] 4. 实现诊断工具主类


  - 创建 FolderDiagnosticTool 类
  - 实现运行完整诊断的方法
  - 实现验证数据库隔离的方法
  - 实现分析文件系统路径的方法
  - 实现查找重复文件夹的方法
  - 实现导出诊断日志的方法
  - 集成 DatabaseAnalyzer、FileSystemAnalyzer 和 IsolationValidator
  - _Requirements: 1.1, 1.5, 2.1, 2.4, 6.6, 10.5_

- [x] 5. 实现数据迁移和修复工具

- [x] 5.1 创建 DataMigrationTool


  - 实现清理重复文件夹的方法
  - 实现修复模块类型的方法
  - 实现查找重复文件夹的私有方法
  - 定义 CleanupStrategy 枚举
  - 创建 CleanupReport 和 FixReport 数据类
  - _Requirements: 8.5, 8.6_

- [x] 5.2 实现 IsolationEnforcer


  - 创建 IsolationEnforcer 类
  - 实现文件夹创建拦截方法
  - 实现路径验证方法
  - 定义路径规则配置
  - 定义 EnforcementResult 密封类
  - _Requirements: 8.3, 9.1, 9.2_

- [x] 6. 添加数据库约束和迁移



  - 在 Folder 实体中添加唯一索引定义
  - 创建数据库迁移 MIGRATION_6_7
  - 实现清理重复数据的 SQL
  - 实现添加唯一约束的 SQL
  - 在 PaysageDatabase 中注册新迁移
  - _Requirements: 8.2_

- [ ] 7. 集成隔离强制到 Repository 层



  - 在 FolderRepositoryImpl 构造函数中添加 IsolationEnforcer 和 DiagnosticLogger 参数
  - 在 createFolder 方法中集成隔离检查
  - 在 createFolder 方法中添加操作日志记录
  - 在 renameFolder 方法中添加验证和日志
  - 在 deleteFolders 方法中添加验证和日志
  - 添加路径验证私有方法
  - _Requirements: 3.4, 3.5, 8.3_

- [ ] 8. 创建诊断 UI 组件
- [ ] 8.1 实现 DiagnosticViewModel
  - 创建 DiagnosticViewModel 类
  - 定义 diagnosticReport StateFlow
  - 定义 isRunning StateFlow
  - 实现 runDiagnostic 方法
  - 实现 exportReport 方法
  - _Requirements: 10.1, 10.2_

- [ ] 8.2 实现 DiagnosticScreen
  - 创建 DiagnosticScreen Composable
  - 实现统计卡片显示
  - 实现隔离状态卡片显示
  - 实现重复文件夹列表显示
  - 实现操作按钮（运行诊断、导出报告）
  - _Requirements: 10.1, 10.2, 10.3, 10.4_

- [ ] 8.3 实现辅助 UI 组件
  - 实现 StatisticsCard Composable
  - 实现 StatItem Composable
  - 实现 IsolationStatusCard Composable
  - 实现 DuplicateFolderItem Composable
  - _Requirements: 10.2, 10.3_

- [ ] 9. 添加诊断工具入口
  - 在开发者设置中添加诊断工具入口
  - 实现导航到诊断屏幕的路由
  - 添加仅在 debug 模式显示的条件
  - _Requirements: 10.1, 10.7_

- [ ] 10. 实施路径隔离修复
  - 在应用配置中定义本地和在线管理的根路径
  - 在 FolderRepository 中实现路径验证方法
  - 在 createFolder 方法中强制路径验证
  - 更新 UI 层确保传递正确的路径
  - _Requirements: 8.1, 8.2_

- [ ] 11. 更新导航和 UI 层
  - 在 FolderManagementScreen 中明确要求 moduleType 参数
  - 更新导航路由包含模块类型信息
  - 在导航处理中正确解析和传递 moduleType
  - 确保所有文件夹操作调用都传递正确的 moduleType
  - _Requirements: 5.4, 5.5_

- [ ] 12. 实现监控和健康检查
- [ ] 12.1 创建 HealthCheckService
  - 实现定期隔离检查方法
  - 实现健康状态报告方法
  - 实现异常检测和告警方法
  - _Requirements: 9.3, 9.4_

- [ ] 12.2 创建 MetricsCollector
  - 实现操作指标收集方法
  - 实现性能指标收集方法
  - 实现指标导出方法
  - _Requirements: 9.6_

- [ ] 13. 编写单元测试
- [ ] 13.1 DatabaseAnalyzer 测试
  - 测试查找重复文件夹功能
  - 测试按模块统计功能
  - 测试获取唯一路径功能
  - _Requirements: 1.1, 1.2_

- [ ] 13.2 IsolationValidator 测试
  - 测试模块类型验证功能
  - 测试查询验证功能
  - 测试隔离检查功能
  - _Requirements: 1.3, 1.4_

- [ ] 13.3 DiagnosticLogger 测试
  - 测试日志记录功能
  - 测试日志查询功能
  - 测试模式检测功能
  - _Requirements: 6.1, 6.2_

- [ ] 13.4 DataMigrationTool 测试
  - 测试清理重复功能
  - 测试修复模块类型功能
  - 测试不同清理策略
  - _Requirements: 8.5_

- [ ] 13.5 IsolationEnforcer 测试
  - 测试路径验证功能
  - 测试拦截机制
  - 测试不同模块类型的路径规则
  - _Requirements: 8.3, 9.1_

- [ ] 14. 编写集成测试
- [ ] 14.1 完整诊断流程测试
  - 创建测试数据
  - 运行完整诊断
  - 验证诊断报告准确性
  - _Requirements: 1.5, 6.6_

- [ ] 14.2 修复流程测试
  - 创建重复数据
  - 执行清理操作
  - 验证数据库状态
  - 验证文件系统状态
  - _Requirements: 8.5, 8.6_

- [ ] 14.3 隔离强制测试
  - 测试跨模块操作被阻止
  - 测试正确模块操作被允许
  - 测试日志记录完整性
  - _Requirements: 9.1, 9.2_

- [ ] 15. 运行诊断和问题分析
  - 在测试环境部署诊断工具
  - 运行完整诊断收集数据
  - 分析诊断报告确定根本原因
  - 记录发现的问题和模式
  - _Requirements: 1.5, 2.4, 6.4, 6.5_

- [ ] 16. 执行数据清理
  - 备份当前数据库
  - 运行数据迁移工具清理重复记录
  - 验证清理结果
  - 确认数据完整性
  - _Requirements: 8.5_

- [ ] 17. 部署和验证
  - 部署修复后的版本到测试环境
  - 执行回归测试
  - 验证隔离机制正常工作
  - 监控运行状态
  - 收集用户反馈
  - _Requirements: 8.6, 9.3, 9.4_

- [ ] 18. 文档和总结
  - 编写根本原因分析文档
  - 编写修复方案说明文档
  - 编写诊断工具使用指南
  - 更新代码审查指南
  - 创建预防措施清单
  - _Requirements: 8.7, 9.5_
