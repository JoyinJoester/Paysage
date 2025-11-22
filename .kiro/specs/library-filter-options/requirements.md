# 需求文档

## 简介

本功能旨在实现书库侧边栏中的过滤选项功能，包括收藏、最近阅读和分类三个选项，使用户能够通过侧边栏快速筛选和访问不同类别的书籍。

## 术语表

- **LibraryScreen**: 书库主界面，显示用户的所有书籍
- **SecondaryDrawer**: 第二层侧边栏，显示书库的详细菜单选项
- **LibraryViewModel**: 书库视图模型，管理书库数据和业务逻辑
- **BookRepository**: 书籍数据仓库，提供书籍数据访问接口
- **NavigationState**: 导航状态管理，定义导航菜单项

## 需求

### 需求 1：收藏书籍筛选

**用户故事：** 作为用户，我想要查看我收藏的书籍，以便快速访问我喜欢的内容

#### 验收标准

1. WHEN 用户点击侧边栏中的"收藏"选项，THE LibraryScreen SHALL 显示所有标记为收藏的书籍
2. WHILE 显示收藏书籍列表时，THE LibraryScreen SHALL 保持当前的布局模式（列表/紧凑网格/纯封面）
3. WHEN 收藏列表为空时，THE LibraryScreen SHALL 显示空状态提示信息
4. THE LibraryViewModel SHALL 提供获取收藏书籍列表的方法
5. THE BookRepository SHALL 支持按收藏状态查询书籍

### 需求 2：最近阅读筛选

**用户故事：** 作为用户，我想要查看最近阅读的书籍，以便继续阅读或回顾最近的阅读内容

#### 验收标准

1. WHEN 用户点击侧边栏中的"最近阅读"选项，THE LibraryScreen SHALL 显示最近阅读的书籍列表
2. THE LibraryScreen SHALL 按最后阅读时间降序排列最近阅读的书籍
3. THE LibraryScreen SHALL 限制最近阅读列表最多显示20本书籍
4. WHILE 显示最近阅读列表时，THE LibraryScreen SHALL 保持当前的布局模式
5. WHEN 最近阅读列表为空时，THE LibraryScreen SHALL 显示空状态提示信息
6. THE BookRepository SHALL 支持按最后阅读时间查询书籍

### 需求 3：分类浏览

**用户故事：** 作为用户，我想要按分类浏览书籍，以便更好地组织和查找我的书籍

#### 验收标准

1. WHEN 用户点击侧边栏中的"分类"选项，THE LibraryScreen SHALL 显示分类列表界面
2. THE LibraryScreen SHALL 显示所有存在的书籍分类及每个分类的书籍数量
3. WHEN 用户点击某个分类时，THE LibraryScreen SHALL 显示该分类下的所有书籍
4. THE LibraryScreen SHALL 支持从分类书籍列表返回到分类列表
5. WHEN 某个分类下没有书籍时，THE LibraryScreen SHALL 显示空状态提示信息
6. THE BookRepository SHALL 支持获取所有分类列表和按分类查询书籍

### 需求 4：导航状态管理

**用户故事：** 作为用户，我想要在不同筛选视图之间切换时保持流畅的体验，以便高效地浏览书籍

#### 验收标准

1. WHEN 用户选择不同的筛选选项时，THE LibraryScreen SHALL 更新URL路由参数
2. THE LibraryScreen SHALL 根据路由参数自动加载对应的筛选视图
3. WHEN 用户使用系统返回按钮时，THE LibraryScreen SHALL 正确返回到上一个筛选状态
4. THE LibraryScreen SHALL 在筛选视图切换时保持搜索栏和其他UI状态
5. THE NavigationState SHALL 正确处理带有筛选参数的路由

### 需求 5：空状态处理

**用户故事：** 作为用户，当筛选结果为空时，我想要看到清晰的提示信息，以便了解当前状态

#### 验收标准

1. WHEN 收藏列表为空时，THE LibraryScreen SHALL 显示"暂无收藏的书籍"提示
2. WHEN 最近阅读列表为空时，THE LibraryScreen SHALL 显示"暂无最近阅读记录"提示
3. WHEN 分类列表为空时，THE LibraryScreen SHALL 显示"暂无分类"提示
4. WHEN 某个分类下没有书籍时，THE LibraryScreen SHALL 显示"该分类下暂无书籍"提示
5. THE LibraryScreen SHALL 在空状态提示中提供相关操作建议
