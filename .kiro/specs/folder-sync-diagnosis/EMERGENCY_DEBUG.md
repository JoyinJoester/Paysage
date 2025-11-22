# 🚨 紧急调试指南 - 文件夹仍然同步

## 问题确认

你报告：**修复后仍然同步！**

这说明问题比我们想象的更深层。让我们立即进行深度调试。

## 🔍 关键问题诊断

### 问题 1: 你使用的是相同的 parentPath 吗？⚠️⚠️⚠️

**这是最可能的原因！**

即使数据库中 `module_type` 不同，如果你在**相同的 `parentPath`** 下创建文件夹，那么：

```
本地管理:
  parentPath: "/storage/emulated/0/Paysage"  ← 相同！
  moduleType: LOCAL_MANAGEMENT
  创建文件夹: "测试"

在线管理:
  parentPath: "/storage/emulated/0/Paysage"  ← 相同！
  moduleType: ONLINE_MANAGEMENT
  查询时会看到: "测试" ← 因为 parent_path 相同！
```

**解决方案**: 本地和在线**必须使用不同的 parentPath**！

```kotlin
// 正确的配置
本地管理: parentPath = "/storage/emulated/0/Paysage/Local"
在线管理: parentPath = "/storage/emulated/0/Paysage/Online"
```

### 问题 2: 检查你的实际调用

**请在你的代码中添加日志**：

```kotlin
// 在 FolderViewModel.createFolder() 中添加
fun createFolder(
    parentPath: String,
    folderName: String,
    moduleType: ModuleType
) {
    // 添加这些日志！
    Log.d("FolderDebug", "=== 创建文件夹 ===")
    Log.d("FolderDebug", "parentPath: $parentPath")
    Log.d("FolderDebug", "folderName: $folderName")
    Log.d("FolderDebug", "moduleType: $moduleType")
    
    viewModelScope.launch {
        // ... 原有代码
    }
}

// 在 FolderViewModel.refreshFolders() 中添加
fun refreshFolders(path: String, moduleType: ModuleType) {
    // 添加这些日志！
    Log.d("FolderDebug", "=== 刷新文件夹列表 ===")
    Log.d("FolderDebug", "path: $path")
    Log.d("FolderDebug", "moduleType: $moduleType")
    
    viewModelScope.launch {
        try {
            val folders = folderRepository.getFolders(path, moduleType, _sortOption.value)
            Log.d("FolderDebug", "查询到 ${folders.size} 个文件夹")
            folders.forEach {
                Log.d("FolderDebug", "  - ${it.name} (moduleType: ${it.moduleType}, parentPath: ${it.parentPath})")
            }
            _folders.value = folders
        } catch (e: Exception) {
            Log.e("FolderDebug", "刷新失败", e)
            _folders.value = emptyList()
        }
    }
}
```

### 问题 3: 数据库中的实际数据

**请执行这个查询查看数据库**：

```sql
-- 查看所有文件夹
SELECT id, name, parent_path, module_type, path FROM folders ORDER BY name;

-- 查找重复
SELECT name, parent_path, COUNT(*) as count, GROUP_CONCAT(module_type) as types
FROM folders 
GROUP BY name, parent_path 
HAVING count > 1;
```

使用 Android Studio 的 Database Inspector 或 adb shell:
```bash
adb shell
su  # 如果需要 root
cd /data/data/takagi.ru.paysage/databases/
sqlite3 paysage_database
.tables
SELECT * FROM folders;
```

## 🎯 立即执行的调试步骤

### 步骤 1: 添加详细日志

在 `FolderViewModel.kt` 中添加上面的日志代码。

### 步骤 2: 重新编译运行

```bash
./gradlew clean assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 步骤 3: 测试并查看日志

```bash
# 清空日志
adb logcat -c

# 查看日志
adb logcat | grep FolderDebug
```

### 步骤 4: 执行测试

1. **在本地管理创建文件夹 "测试A"**
   - 查看日志输出的 `parentPath` 和 `moduleType`
   
2. **切换到在线管理**
   - 查看日志输出的查询参数
   - 查看返回的文件夹列表

3. **分析日志**
   - 如果 `parentPath` 相同 → 这就是问题！
   - 如果 `moduleType` 不正确 → UI 层传参有问题
   - 如果查询返回了错误的数据 → 数据库有问题

## 🔧 可能的修复方案

### 方案 A: parentPath 相同（最可能）

**问题**: 本地和在线使用相同的根路径

**修复**: 找到路径配置的地方，改为：

```kotlin
// 在你的配置文件或常量中
object FolderConfig {
    val LOCAL_BASE_PATH = "/storage/emulated/0/Paysage/Local"
    val ONLINE_BASE_PATH = "/storage/emulated/0/Paysage/Online"
}

// 在调用 createFolder 时
when (moduleType) {
    ModuleType.LOCAL_MANAGEMENT -> {
        viewModel.createFolder(
            parentPath = FolderConfig.LOCAL_BASE_PATH,
            folderName = name,
            moduleType = ModuleType.LOCAL_MANAGEMENT
        )
    }
    ModuleType.ONLINE_MANAGEMENT -> {
        viewModel.createFolder(
            parentPath = FolderConfig.ONLINE_BASE_PATH,
            folderName = name,
            moduleType = ModuleType.ONLINE_MANAGEMENT
        )
    }
}
```

### 方案 B: moduleType 传递错误

**问题**: UI 层传递了错误的 moduleType

**修复**: 检查所有调用 `createFolder` 的地方，确保 `moduleType` 正确。

### 方案 C: 数据库查询有问题

**问题**: 查询没有正确过滤 module_type

**修复**: 这个我们已经检查过了，查询是正确的。

### 方案 D: UI 显示逻辑有问题

**问题**: 可能是 UI 层显示了错误的数据

**修复**: 检查 `FolderManagementScreen` 的实现。

## 📊 诊断检查表

请按顺序检查：

- [ ] **添加日志代码**
- [ ] **重新编译运行**
- [ ] **在本地管理创建文件夹**
- [ ] **查看日志中的 parentPath**
  - 如果是 `/storage/emulated/0/Paysage` → ❌ 问题找到！
  - 如果是 `/storage/emulated/0/Paysage/Local` → ✅ 继续检查
- [ ] **查看日志中的 moduleType**
  - 如果是 `LOCAL_MANAGEMENT` → ✅ 正确
  - 如果是其他 → ❌ 问题找到！
- [ ] **切换到在线管理**
- [ ] **查看日志中的查询参数**
  - path 应该是什么？
  - moduleType 应该是 `ONLINE_MANAGEMENT`
- [ ] **查看日志中返回的文件夹列表**
  - 如果包含刚才创建的文件夹 → ❌ 问题确认
  - 如果不包含 → ✅ 正常

## 🚨 紧急联系

如果以上步骤都检查了还是有问题，请提供：

1. **完整的日志输出**（从创建到查询的所有日志）
2. **数据库查询结果**（folders 表的内容）
3. **你的路径配置代码**（在哪里定义 parentPath）
4. **UI 调用代码**（在哪里调用 createFolder）

## 💡 临时解决方案

如果急需解决，可以临时这样做：

```kotlin
// 在 FolderDao 中修改查询，添加额外的路径过滤
@Query("""
    SELECT * FROM folders 
    WHERE parent_path = :path 
    AND module_type = :moduleType
    AND (
        (module_type = 'LOCAL_MANAGEMENT' AND parent_path LIKE '%/Local%')
        OR
        (module_type = 'ONLINE_MANAGEMENT' AND parent_path LIKE '%/Online%')
    )
    ORDER BY name COLLATE NOCASE ASC
""")
suspend fun getFoldersByPath(path: String, moduleType: ModuleType): List<Folder>
```

但这只是临时方案，**根本问题还是要修复 parentPath 配置**！

---
**紧急程度**: 🔴 高  
**下一步**: 添加日志，查看实际的 parentPath 和 moduleType
