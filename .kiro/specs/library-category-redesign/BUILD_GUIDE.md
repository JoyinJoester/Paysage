# 书库分类系统 - 编译和运行指南

## 📦 编译前检查

### 1. 确认所有文件已创建

运行以下命令检查新增文件：

```bash
# 数据层
ls app/src/main/java/takagi/ru/paysage/data/model/CategoryType.kt
ls app/src/main/java/takagi/ru/paysage/data/model/BookSource.kt
ls app/src/main/java/takagi/ru/paysage/data/dao/BookSourceDao.kt

# Repository层
ls app/src/main/java/takagi/ru/paysage/repository/BookRepositoryExtensions.kt
ls app/src/main/java/takagi/ru/paysage/repository/OnlineSourceRepository.kt

# ViewModel层
ls app/src/main/java/takagi/ru/paysage/viewmodel/LibraryViewModelExtensions.kt
ls app/src/main/java/takagi/ru/paysage/viewmodel/OnlineSourceViewModel.kt

# UI层
ls app/src/main/java/takagi/ru/paysage/ui/components/CategoryFilterBar.kt
ls app/src/main/java/takagi/ru/paysage/ui/components/BookSourceComponents.kt
ls app/src/main/java/takagi/ru/paysage/ui/screens/OnlineSourceScreen.kt
```

### 2. 检查依赖

确保 `build.gradle` 包含以下依赖：

```gradle
dependencies {
    // Compose
    implementation "androidx.compose.ui:ui:$compose_version"
    implementation "androidx.compose.material3:material3:$material3_version"
    implementation "androidx.compose.material:material-icons-extended:$compose_version"
    
    // Room
    implementation "androidx.room:room-runtime:$room_version"
    implementation "androidx.room:room-ktx:$room_version"
    kapt "androidx.room:room-compiler:$room_version"
    
    // Lifecycle
    implementation "androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycle_version"
    implementation "androidx.lifecycle:lifecycle-runtime-compose:$lifecycle_version"
    
    // Coroutines
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutines_version"
    
    // Serialization (for JSON)
    implementation "org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization_version"
}
```

## 🔧 编译步骤

### 1. 清理项目

```bash
./gradlew clean
```

### 2. 同步Gradle

在Android Studio中：
- File → Sync Project with Gradle Files

或命令行：
```bash
./gradlew --refresh-dependencies
```

### 3. 编译项目

```bash
./gradlew assembleDebug
```

### 4. 检查编译错误

如果遇到编译错误，请参考[常见编译问题](#常见编译问题)部分。

## 🚀 运行应用

### 1. 安装到设备

```bash
./gradlew installDebug
```

### 2. 启动应用

```bash
adb shell am start -n takagi.ru.paysage/.MainActivity
```

### 3. 查看日志

```bash
adb logcat | grep -i paysage
```

## 🐛 常见编译问题

### 问题1: 找不到CategoryType类

**错误信息**:
```
Unresolved reference: CategoryType
```

**解决方案**:
1. 确认文件已创建：`app/src/main/java/takagi/ru/paysage/data/model/CategoryType.kt`
2. 清理并重新编译：`./gradlew clean build`
3. 在Android Studio中：Build → Rebuild Project

### 问题2: Room数据库版本冲突

**错误信息**:
```
Room cannot verify the data integrity. Looks like you've changed schema but forgot to update the version number.
```

**解决方案**:
1. 卸载应用：`adb uninstall takagi.ru.paysage`
2. 重新安装：`./gradlew installDebug`

或者在代码中使用：
```kotlin
Room.databaseBuilder(...)
    .fallbackToDestructiveMigration()
    .build()
```

### 问题3: 扩展函数找不到

**错误信息**:
```
Unresolved reference: getBooksByCategory
```

**解决方案**:
添加导入语句：
```kotlin
import takagi.ru.paysage.repository.getBooksByCategory
import takagi.ru.paysage.repository.getBooksByCategoryFlow
```

### 问题4: Compose版本不兼容

**错误信息**:
```
None of the following functions can be called with the arguments supplied
```

**解决方案**:
更新Compose版本到最新稳定版：
```gradle
compose_version = "1.5.4"
material3_version = "1.1.2"
```

### 问题5: Kotlin序列化错误

**错误信息**:
```
Serializer for class 'CategoryType' is not found
```

**解决方案**:
1. 添加Kotlin序列化插件：
```gradle
plugins {
    id 'org.jetbrains.kotlin.plugin.serialization' version "$kotlin_version"
}
```

2. 为枚举添加@Serializable注解：
```kotlin
@Serializable
enum class CategoryType { ... }
```

## 🧪 测试编译

### 运行单元测试

```bash
./gradlew test
```

### 运行UI测试

```bash
./gradlew connectedAndroidTest
```

## 📱 数据库迁移测试

### 1. 安装旧版本

```bash
# 安装版本3的应用
adb install app-v3.apk
```

### 2. 添加测试数据

在应用中添加一些书籍。

### 3. 安装新版本

```bash
./gradlew installDebug
```

### 4. 验证迁移

1. 打开应用
2. 检查书籍是否正确分类
3. 查看数据库日志：
```bash
adb logcat | grep -i "migration"
```

### 5. 验证数据完整性

```bash
adb shell
run-as takagi.ru.paysage
cd databases
sqlite3 paysage_database

# 检查表结构
.schema books
.schema book_sources

# 检查数据
SELECT id, title, categoryType, isOnline FROM books LIMIT 10;
SELECT * FROM book_sources;
```

## 🔍 调试技巧

### 1. 启用详细日志

在Application类中：
```kotlin
if (BuildConfig.DEBUG) {
    Timber.plant(Timber.DebugTree())
}
```

### 2. 检查数据库状态

```kotlin
// 在ViewModel中添加
init {
    viewModelScope.launch {
        val stats = getCategoryStatistics()
        Log.d("LibraryViewModel", "Category stats: $stats")
    }
}
```

### 3. 监控缓存性能

```kotlin
val cacheStats = repository.getCacheStats()
Log.d("Cache", "Hit rate: ${cacheStats.hitRate}")
```

### 4. 使用Database Inspector

在Android Studio中：
- View → Tool Windows → App Inspection
- 选择Database Inspector
- 查看books和book_sources表

## 📊 性能测试

### 1. 测试查询性能

```kotlin
val startTime = System.currentTimeMillis()
val books = repository.getBooksByCategory(CategoryType.MANGA, DisplayMode.LOCAL)
val duration = System.currentTimeMillis() - startTime
Log.d("Performance", "Query took ${duration}ms")
```

### 2. 测试缓存效果

```kotlin
// 第一次查询（无缓存）
val time1 = measureTimeMillis {
    repository.getBooksByCategory(CategoryType.MANGA, DisplayMode.LOCAL)
}

// 第二次查询（有缓存）
val time2 = measureTimeMillis {
    repository.getBooksByCategory(CategoryType.MANGA, DisplayMode.LOCAL)
}

Log.d("Cache", "First: ${time1}ms, Second: ${time2}ms")
```

### 3. 测试分页加载

```kotlin
// 加载第一页
val page1 = bookDao.getBooksByCategory(
    CategoryType.MANGA, 
    false, 
    limit = 50, 
    offset = 0
)

// 加载第二页
val page2 = bookDao.getBooksByCategory(
    CategoryType.MANGA, 
    false, 
    limit = 50, 
    offset = 50
)
```

## 🎯 验收测试清单

### 数据层
- [ ] 数据库成功迁移到版本4
- [ ] 所有索引已创建
- [ ] 现有书籍已自动分类
- [ ] 可以添加和查询书源

### Repository层
- [ ] 可以按分类查询书籍
- [ ] 缓存机制正常工作
- [ ] 分页查询返回正确数据
- [ ] 书源CRUD操作正常

### ViewModel层
- [ ] 分类状态管理正常
- [ ] 显示模式切换正常
- [ ] 统计信息准确
- [ ] 书源管理功能正常

### UI层
- [ ] CategoryFilterBar正确显示
- [ ] 分类切换动画流畅
- [ ] 书源列表正确显示
- [ ] 颜色主题正确应用

### 国际化
- [ ] 中文字符串正确显示
- [ ] 英文字符串正确显示
- [ ] 语言切换正常

## 📝 发布前检查

### 1. 代码质量

```bash
# 运行Lint检查
./gradlew lint

# 查看报告
open app/build/reports/lint-results.html
```

### 2. 代码格式化

```bash
# 格式化代码
./gradlew ktlintFormat
```

### 3. 生成发布版本

```bash
./gradlew assembleRelease
```

### 4. 签名APK

```bash
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore my-release-key.jks \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  alias_name
```

## 🔗 相关资源

- [快速入门指南](QUICK_START.md)
- [最终总结](FINAL_SUMMARY.md)
- [实现状态](IMPLEMENTATION_STATUS.md)
- [Android官方文档](https://developer.android.com/)

---

**版本**: 1.0.0-alpha  
**更新日期**: 2025-10-28
