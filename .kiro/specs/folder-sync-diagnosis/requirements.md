# 需求文档 - 文件夹同步问题诊断与修复

## 简介

本功能旨在诊断和修复在线管理和本地管理之间出现的非预期文件夹同步问题。尽管两个系统使用独立的数据库配置（通过 `ModuleType` 枚举区分），但在实际使用中发现文件夹数据出现了意外的同步行为。本需求文档定义了诊断流程、问题分析和修复方案的详细要求。

## 术语表

- **FolderSystem**: 文件夹管理系统，负责处理本地和在线模块的文件夹操作
- **ModuleType**: 模块类型枚举，包含 LOCAL_MANAGEMENT（本地管理）和 ONLINE_MANAGEMENT（在线管理）
- **FolderDao**: 文件夹数据访问对象，提供数据库操作接口
- **FolderRepository**: 文件夹仓库层，封装业务逻辑和数据访问
- **DatabaseIsolation**: 数据库隔离机制，确保不同模块的数据独立存储
- **FileSystemPath**: 文件系统路径，用于存储实际文件夹的物理位置
- **SyncBehavior**: 同步行为，指数据在不同模块间的传播机制
- **DiagnosticLog**: 诊断日志，记录系统操作和数据流动的详细信息

## 需求

### 需求 1: 数据库隔离验证

**用户故事:** 作为系统管理员，我希望验证本地管理和在线管理的数据库配置是否真正独立，以便确认没有共享存储路径或同步机制

#### 验收标准

1. THE FolderSystem SHALL maintain separate database records for LOCAL_MANAGEMENT and ONLINE_MANAGEMENT module types
2. WHEN a folder is created with module_type = LOCAL_MANAGEMENT, THE FolderSystem SHALL NOT create corresponding records with module_type = ONLINE_MANAGEMENT
3. THE FolderSystem SHALL enforce database-level isolation through the module_type column in all queries
4. WHEN querying folders by path, THE FolderSystem SHALL include module_type as a mandatory filter parameter
5. THE FolderSystem SHALL provide diagnostic queries to verify record counts per module type
6. THE FolderSystem SHALL log all database operations with module_type information for audit purposes

### 需求 2: 文件系统路径分析

**用户故事:** 作为开发人员，我希望分析文件系统路径配置，以便确认本地和在线管理是否使用了相同的物理存储路径

#### 验收标准

1. THE FolderSystem SHALL provide a diagnostic function to list all unique parent_path values grouped by module_type
2. WHEN analyzing file system paths, THE FolderSystem SHALL identify any path overlaps between LOCAL_MANAGEMENT and ONLINE_MANAGEMENT
3. THE FolderSystem SHALL validate that parent_path values for different module types point to distinct physical directories
4. IF path overlap is detected, THEN THE FolderSystem SHALL report the conflicting paths with detailed information
5. THE FolderSystem SHALL check file system permissions for each parent_path to ensure proper access control
6. THE FolderSystem SHALL verify that no symbolic links or mount points create unintended path sharing

### 需求 3: 数据创建流程追踪

**用户故事:** 作为开发人员，我希望追踪文件夹创建的完整流程，以便识别可能导致跨模块数据复制的代码路径

#### 验收标准

1. THE FolderSystem SHALL log every folder creation operation with timestamp, module_type, parent_path, and caller information
2. WHEN a folder is created, THE FolderSystem SHALL record the complete call stack for debugging purposes
3. THE FolderSystem SHALL provide a diagnostic mode that traces all database insert operations
4. THE FolderSystem SHALL verify that FolderRepository.createFolder() receives and preserves the correct module_type parameter
5. THE FolderSystem SHALL validate that FolderDao.insert() stores the module_type value without modification
6. WHEN multiple folders are created in sequence, THE FolderSystem SHALL ensure each operation maintains its intended module_type

### 需求 4: 后台服务和定时任务检查

**用户故事:** 作为系统管理员，我希望检查是否存在后台服务或定时任务在自动同步文件夹数据，以便识别非预期的同步机制

#### 验收标准

1. THE FolderSystem SHALL provide a diagnostic tool to list all active background services and scheduled tasks
2. THE FolderSystem SHALL scan the codebase for WorkManager, AlarmManager, and JobScheduler implementations
3. WHEN analyzing background operations, THE FolderSystem SHALL identify any tasks that access FolderDao or FolderRepository
4. THE FolderSystem SHALL verify that no synchronization logic exists between LOCAL_MANAGEMENT and ONLINE_MANAGEMENT modules
5. THE FolderSystem SHALL check for any observers or listeners that trigger cross-module data operations
6. IF synchronization code is found, THEN THE FolderSystem SHALL report the location and purpose of such code

### 需求 5: API 接口数据交互验证

**用户故事:** 作为开发人员，我希望验证本地管理和在线管理的 API 接口是否存在意外的数据交互，以便排除接口层面的同步问题

#### 验收标准

1. THE FolderSystem SHALL analyze all ViewModel methods that interact with FolderRepository
2. WHEN examining API calls, THE FolderSystem SHALL verify that module_type parameters are correctly passed through all layers
3. THE FolderSystem SHALL identify any shared state or singleton instances that could cause data leakage between modules
4. THE FolderSystem SHALL validate that UI components correctly specify module_type when calling ViewModel methods
5. THE FolderSystem SHALL check for any caching mechanisms that might mix data from different module types
6. THE FolderSystem SHALL ensure that StateFlow and LiveData emissions maintain module_type separation

### 需求 6: 系统日志分析

**用户故事:** 作为系统管理员，我希望分析系统日志以查找文件夹创建和更新的记录，以便确定同步发生的具体时机和条件

#### 验收标准

1. THE FolderSystem SHALL implement comprehensive logging for all folder operations including create, update, delete, and query
2. WHEN a folder operation occurs, THE FolderSystem SHALL log the operation type, module_type, folder_id, timestamp, and result
3. THE FolderSystem SHALL provide a log analysis tool to filter and correlate folder operations by module_type
4. THE FolderSystem SHALL identify temporal patterns where folders appear in both modules within short time windows
5. THE FolderSystem SHALL track the sequence of operations to determine if cross-module duplication follows specific user actions
6. THE FolderSystem SHALL export diagnostic logs in a structured format for external analysis

### 需求 7: 用户权限和配置检查

**用户故事:** 作为系统管理员，我希望检查用户权限设置，以便确认没有配置导致数据自动同步的权限规则

#### 验收标准

1. THE FolderSystem SHALL verify that no application-level permissions grant cross-module data access
2. THE FolderSystem SHALL check database access control lists to ensure module_type-based isolation
3. WHEN analyzing permissions, THE FolderSystem SHALL identify any shared preferences or settings that affect folder management
4. THE FolderSystem SHALL validate that user roles and permissions do not override module_type restrictions
5. THE FolderSystem SHALL ensure that no configuration files contain sync-related settings
6. THE FolderSystem SHALL provide a permission audit report highlighting any potential security or isolation issues

### 需求 8: 修复方案实施

**用户故事:** 作为开发人员，我希望实施修复方案以彻底解决文件夹同步问题，以便确保本地和在线管理完全独立

#### 验收标准

1. WHEN the root cause is identified, THE FolderSystem SHALL implement targeted fixes to eliminate cross-module synchronization
2. THE FolderSystem SHALL add database constraints to enforce module_type isolation at the schema level
3. THE FolderSystem SHALL implement validation checks in FolderRepository to prevent incorrect module_type usage
4. THE FolderSystem SHALL add unit tests to verify module_type isolation across all folder operations
5. THE FolderSystem SHALL provide a data migration tool to clean up any existing duplicate records
6. WHEN fixes are applied, THE FolderSystem SHALL verify through integration tests that no cross-module data leakage occurs
7. THE FolderSystem SHALL document the root cause, fix implementation, and prevention measures in technical documentation

### 需求 9: 监控和预防机制

**用户故事:** 作为系统管理员，我希望建立监控和预防机制，以便及时发现和阻止未来可能出现的同步问题

#### 验收标准

1. THE FolderSystem SHALL implement runtime assertions to detect cross-module data operations
2. WHEN a potential isolation violation is detected, THE FolderSystem SHALL log a warning and prevent the operation
3. THE FolderSystem SHALL provide a health check API to verify module_type isolation status
4. THE FolderSystem SHALL implement automated tests that run on every build to verify data isolation
5. THE FolderSystem SHALL add code review guidelines to prevent introduction of cross-module synchronization logic
6. THE FolderSystem SHALL create monitoring dashboards to track folder operations by module_type in production

### 需求 10: 诊断工具开发

**用户故事:** 作为开发人员，我希望开发专用的诊断工具，以便快速分析和定位文件夹同步问题

#### 验收标准

1. THE FolderSystem SHALL provide a diagnostic UI screen accessible from developer settings
2. THE DiagnosticTool SHALL display real-time statistics of folder counts per module_type
3. THE DiagnosticTool SHALL allow filtering and searching of folder records by various criteria
4. THE DiagnosticTool SHALL provide a "Find Duplicates" function to identify folders with identical names across modules
5. THE DiagnosticTool SHALL export diagnostic reports in JSON and CSV formats
6. THE DiagnosticTool SHALL include a "Verify Isolation" button that runs comprehensive checks and displays results
7. THE DiagnosticTool SHALL be accessible only in debug builds to prevent exposure in production
