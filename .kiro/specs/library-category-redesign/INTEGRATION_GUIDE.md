# 书库分类系统 - 集成指南

## 🚀 快速开始

本指南将帮助您将新的分类系统集成到现有的Paysage应用中。

## 📋 前置条件

1. ✅ 确保所有新增文件已添加到项目中
2. ✅ 数据库版本已更新到4
3. ✅ 所有依赖项已正确配置

## 🔧 集成步骤

### 第一步：验证文件完整性

确保以下文件已正确添加到项目中：

**数据层**:
- `data/model/CategoryType.kt`
- `data/model/BookSource.kt`
- `data/dao/BookSourceDao.kt`

**Repository层**:
- `repository/BookRepositoryExtensions.kt`
- `repository/OnlineSourceRepository.kt`

**ViewModel层**:
- `viewmodel/LibraryViewModelExtensions.kt`
- `viewmodel/OnlineSourceViewModel.kt`

**UI层**:
- `ui/components/CategoryFilterBar.kt`
- `ui/components/BookSourceComponents.kt`
- `ui/components/AddSourceDialog.kt`
- `ui/screens/OnlineSourceScreen.kt`

### 第二步：使用CategoryFilterBar

在LibraryScreen中，CategoryFilterBar已经集成。您可以通过以下方式使用它：

```kotlin
@Composable
fun LibraryScreen(
    // ... 其他参数
) {
    var categoryType by remember { mutableStateOf(CategoryType.MANGA) }
    var displayMode by remember { mutableStateOf(DisplayMode.LOCAL) }
    
    Column {
        CategoryFilterBar(
            selectedCategory = categoryType,
            onCategoryChange = { categoryType = it },
            displayMode = displayMode,
            onDisplayModeChange = { displayMode = it }
        )
        
        // 根据categoryType和displayMode显示内容
    }
}
```

### 第三步：使用OnlineSourceScreen

在线书源管理屏幕已经创建，可以通过导航系统访问：

```kotlin
// 在NavHost中添加路由
composable("online_sources/{category}") { backStackEntry ->
    val category = backStackEntry.arguments?.getString("category")
    val categoryType = CategoryType.fromString(category)
    
    OnlineSourceScreen(
        categoryType = categoryType,
        onBackClick = { navController.popBackStack() }
    )
}
```

### 第四步：使用AddSourceDialog

添加书源对话框可以这样使用：

```kotlin
var showAddDialog by remember { mutableStateOf(false) }
val viewModel: OnlineSourceViewModel = viewModel()

// 显示对话框
AddSourceDialog(
    isVisible = showAddDialog,
    onDismiss = { showAddDialog = false },
    onConfirm = { source ->
        viewModel.addSource(source)
        showAddDialog = false
    },
    initialCategoryType = CategoryType.MANGA
)
```

## 🔍 常见问题

### 1. 编译错误：找不到CategoryType

**解决方案**: 添加导入语句
```kotlin
import takagi.ru.paysage.data.model.CategoryType
import takagi.ru.paysage.data.model.DisplayMode
```

### 2. 数据库迁移失败

**解决方案**: 
1. 清除应用数据
2. 确保MIGRATION_3_4已正确添加到数据库构建器
3. 检查迁移脚本中的SQL语句

### 3. CategoryFilterBar不显示

**解决方案**: 
1. 检查是否正确导入了组件
2. 确保传递了正确的参数
3. 检查是否有布局冲突

## 📱 使用示例

### 基本用法

```kotlin
@Composable
fun MyLibraryScreen() {
    var categoryType by remember { mutableStateOf(CategoryType.MANGA) }
    var displayMode by remember { mutableStateOf(DisplayMode.LOCAL) }
    
    Column {
        CategoryFilterBar(
            selectedCategory = categoryType,
            onCategoryChange = { categoryType = it },
            displayMode = displayMode,
            onDisplayModeChange = { displayMode = it }
        )
        
        // 显示书籍列表
    }
}
```

### 书源管理

```kotlin
@Composable
fun MySourceManagement() {
    val viewModel: OnlineSourceViewModel = viewModel()
    val sources by viewModel.bookSources.collectAsState()
    
    OnlineSourceScreen(
        categoryType = CategoryType.MANGA,
        onBackClick = { /* 返回 */ }
    )
}
```

## 🎯 下一步

1. 测试分类切换功能
2. 测试书源管理功能
3. 验证数据库迁移
4. 检查UI显示效果

## 📚 相关文档

- [需求文档](requirements.md)
- [设计文档](design.md)
- [任务列表](tasks.md)
- [最终总结](FINAL_SUMMARY.md)
