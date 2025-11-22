# 阅读器翻页性能优化实现任务

- [x] 1. 实现 BitmapMemoryManager 内存管理器


  - 创建 `app/src/main/java/takagi/ru/paysage/reader/BitmapMemoryManager.kt`
  - 实现采样率计算方法，根据屏幕尺寸和原始图片尺寸计算最优采样率
  - 实现内存监控方法，检查当前内存使用是否超过阈值（25%）
  - 实现 Bitmap 安全回收方法
  - 实现内存警告监听器注册
  - _需求: 1.4, 1.5_



- [ ] 2. 实现 PageCacheManager 页面缓存管理器
  - 创建 `app/src/main/java/takagi/ru/paysage/reader/PageCacheManager.kt`
  - 实现原始图片 LRU 缓存（容量 10）
  - 实现过滤图片 LRU 缓存（容量 5）
  - 实现缓存键生成逻辑（bookId_pageIndex 和 bookId_pageIndex_filterHash）
  - 实现缓存命中率统计
  - 实现内存使用量计算
  - 实现缓存清理方法（clearAll, clearFilterCache, removeBook）


  - 在 LRU 淘汰时自动回收 Bitmap 内存
  - _需求: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 3. 扩展 ImageFilter 数据类


  - 修改 `app/src/main/java/takagi/ru/paysage/util/ImageFilterUtil.kt`
  - 为 ImageFilter 添加 hashCode() 方法用于缓存键生成
  - 添加 isActive() 扩展方法判断是否需要应用过滤器
  - _需求: 2.4_

- [ ] 4. 实现 PagePreloader 页面预加载器
  - 创建 `app/src/main/java/takagi/ru/paysage/reader/PagePreloader.kt`


  - 实现预加载任务调度，使用低优先级协程（Dispatchers.IO）
  - 实现单页模式预加载逻辑（向前 2 页，向后 1 页）
  - 实现双页模式预加载逻辑（向前 4 页）
  - 实现任务取消机制，快速翻页时取消未完成的预加载
  - 检查缓存避免重复加载
  - _需求: 2.1, 2.2, 2.3, 2.4, 2.5_



- [ ] 5. 实现 PerformanceMonitor 性能监控器
  - 创建 `app/src/main/java/takagi/ru/paysage/reader/PerformanceMonitor.kt`
  - 实现页面加载时间记录（保留最近 100 次）
  - 实现缓存命中率统计
  - 实现平均加载时间计算


  - 实现性能报告生成
  - 添加日志输出，当加载时间超过 200ms 时记录警告
  - _需求: 5.4, 7.1, 7.2, 7.3, 7.4_

- [ ] 6. 优化 FileParser 图片解码
  - 修改 `app/src/main/java/takagi/ru/paysage/utils/FileParser.kt`
  - 添加支持 BitmapFactory.Options 参数的解码方法
  - 实现两阶段解码：先获取尺寸（inJustDecodeBounds=true），再应用采样率解码
  - 在所有解码方法中应用采样率优化
  - _需求: 4.1, 4.2_

- [ ] 7. 重构 ReaderViewModel 集成缓存和预加载
  - 修改 `app/src/main/java/takagi/ru/paysage/viewmodel/ReaderViewModel.kt`
  - 初始化 PageCacheManager、BitmapMemoryManager、PagePreloader、PerformanceMonitor
  - 重写 loadPage 方法为 loadPageOptimized：
    - 先检查原始缓存


    - 缓存未命中时从文件解码（应用采样率）
    - 将解码结果存入原始缓存
    - 检查过滤缓存
    - 过滤缓存未命中时应用过滤器
    - 将过滤结果存入过滤缓存
    - 更新 UI


    - 触发预加载
    - 记录性能指标
    - 检查内存并在必要时清理缓存
  - 实现 fastPageTurn 方法用于快速连续翻页
  - 在 closeBook 和 onCleared 中清理缓存和回收 Bitmap


  - _需求: 1.1, 1.2, 2.1, 2.2, 3.1, 3.2, 3.3, 3.4, 4.4, 5.1, 5.2, 5.4, 5.5_

- [ ] 8. 优化 ReaderScreen UI 响应性
  - 修改 `app/src/main/java/takagi/ru/paysage/ui/screens/ReaderScreen.kt`
  - 为 Image 组件添加 key 参数优化重组（key = currentPage）
  - 使用 remember 缓存过滤后的 Bitmap，避免重复计算
  - 移除 UI 层的过滤器应用逻辑（已移到 ViewModel）


  - 优化双页模式的第二页加载逻辑
  - _需求: 3.5, 5.3, 6.5_

- [ ] 9. 实现过滤器参数变化处理
  - 修改 `app/src/main/java/takagi/ru/paysage/viewmodel/ReaderViewModel.kt`
  - 添加 onFilterChanged 方法


  - 当过滤器参数改变时清空过滤缓存
  - 重新加载当前页面应用新过滤器
  - _需求: 3.2_

- [ ] 10. 实现内存压力处理
  - 修改 `app/src/main/java/takagi/ru/paysage/reader/BitmapMemoryManager.kt`
  - 实现 handleLowMemory 方法



  - 在 ReaderViewModel 中注册内存警告监听
  - 内存不足时清空过滤缓存
  - 如果还不够，清空一半原始缓存
  - 捕获 OutOfMemoryError 并使用更大采样率重试
  - _需求: 4.4, 4.5_

- [ ] 11. 优化双页模式性能
  - 修改 `app/src/main/java/takagi/ru/paysage/viewmodel/ReaderViewModel.kt`
  - 在双页模式下同时缓存两个页面
  - 调整预加载策略为 4 个页面（2 组双页）
  - 实现页面复用逻辑（翻页后第 2 页变成第 1 页）
  - 在 ReaderScreen 中优化双页切换逻辑
  - _需求: 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ] 12. 添加性能配置和调试功能
  - 创建 `app/src/main/java/takagi/ru/paysage/reader/ReaderConfig.kt`
  - 定义缓存大小、预加载页数、内存阈值等配置常量
  - 实现根据设备内存动态调整缓存大小
  - 在 AppSettings 中添加 showPerformanceOverlay 选项
  - 创建 PerformanceOverlay 组件显示性能指标（调试模式）
  - 在 ReaderViewModel 中添加定期性能日志输出
  - _需求: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 13. 编写性能测试
  - 创建 `app/src/test/java/takagi/ru/paysage/reader/PageCacheManagerTest.kt`
  - 测试 LRU 淘汰策略
  - 测试缓存命中率计算
  - 测试并发访问安全性
  - 创建 `app/src/test/java/takagi/ru/paysage/reader/BitmapMemoryManagerTest.kt`
  - 测试采样率计算
  - 测试内存阈值检查
  - 创建 `app/src/androidTest/java/takagi/ru/paysage/reader/ReaderPerformanceTest.kt`
  - 测试首次加载性能 < 200ms
  - 测试缓存命中性能 < 50ms
  - 测试快速连续翻页
  - 测试不同文件格式（ZIP, PDF）
  - _需求: 所有需求_
