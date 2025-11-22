# 实现计划

- [x] 1. 创建 CoverFlipPagerState 分页状态管理器

  - 创建 CoverFlipPagerState 类,包含 LazyListState
  - 实现当前页索引(currentPage)的状态管理
  - 实现滚动偏移量(scrollOffset)的状态管理
  - 实现滚动进度(scrollProgress)的计算
  - 实现页面宽度(pageWidth)的获取和更新
  - 实现 scrollToPage 方法,支持动画滚动到指定页面
  - 实现 updateState 方法,从 LazyListLayoutInfo 更新状态
  - _需求: 1.1, 1.2, 2.1, 2.2_


- [x] 2. 实现 SnapFlingBehavior 吸附行为

  - [x] 2.1 创建 SnapFlingBehavior 类

    - 实现 FlingBehavior 接口
    - 实现 performFling 方法,处理快速滑动
    - 添加吸附阈值配置(snapThreshold = 0.3f)
    - 添加速度阈值配置(velocityThreshold = 1000f)
    - _需求: 2.4, 5.1, 5.2, 7.1, 7.2, 7.3_

  

  - [x] 2.2 实现 Scroll Idle 吸附逻辑

    - 实现 snapToTargetExistingView 方法
    - 找到距离中心最近的可见 View
    - 计算滚动距离
    - 使用 animateScrollBy 平滑滚动到目标位置

    - _需求: 2.1, 2.2, 2.3, 7.5_

  
  - [x] 2.3 实现 Fling 吸附逻辑
    - 实现 findTargetSnapPosition 方法
    - 找到两个方向最近的 View
    - 根据速度方向选择目标页面
    - 实现 snapFromFling 方法
    - 使用 Animatable 实现平滑滚动动画

    - 配置 spring 动画规格(300ms 时长)
    - 根据速度调整动画时长(高速时 200ms)
    - _需求: 2.4, 2.5, 5.1, 5.2, 5.3, 5.4, 7.3, 7.4_

- [x] 3. 实现 PageScrollListener 滚动监听器

  - 创建 PageScrollListener 类
  - 实现 onScroll 方法,接收 PagerState

  - 计算当前页索引(position)

  - 计算偏移量(positionOffset 和 positionOffsetPixels)
  - 触发 onPageScrolled 回调
  - _需求: 2.1, 2.2, 2.3_

- [x] 4. 实现 CoverFlipTransformer 覆盖效果变换器

  - [x] 4.1 创建 PageTransform 数据类

    - 定义 translationX 属性(X 轴位移)
    - 定义 alpha 属性(透明度)
    - 定义 zIndex 属性(绘制顺序)
    - _需求: 1.3, 1.4, 4.1, 4.2_
  

  - [x] 4.2 实现 transformPage 方法
    - 计算页面位置(position: -1f 到 1f)

    - 处理左侧页面(position < -1f):设置为不可见
    - 处理第一个可见页面(position <= 0f):底层,静止,translationX = 0
    - 处理第二个可见页面(position <= 1f):上层,跟随滚动,translationX = -position * pageWidth
    - 处理右侧页面(position > 1f):设置为不可见


    - 使用 zIndex 控制绘制顺序(上层 zIndex = 1f, 下层 zIndex = 0f)

    - _需求: 1.3, 1.4, 1.5, 3.1, 3.2, 3.3, 3.4, 3.5, 4.3, 4.4, 4.5_

  
  - [x] 4.3 实现 calculateShadowAlpha 方法





    - 只在上层页面(position > 0)显示阴影
    - 阴影透明度随 position 线性增加(0f 到 maxAlpha)
    - 配置最大透明度为 0.4f

    - _需求: 8.1, 8.2, 8.3, 8.4, 8.5_

- [x] 5. 创建 PageContent Composable 组件


  - [x] 5.1 实现页面内容渲染

    - 使用 Canvas 绘制页面位图
    - 计算缩放比例(保持宽高比)
    - 实现图片居中对齐
    - 使用 drawIntoCanvas 和 nativeCanvas 绘制位图
    - _需求: 1.4, 3.1, 3.2, 3.3, 4.3, 4.5_
  

  - [x] 5.2 实现阴影效果渲染

    - 在页面上层绘制半透明黑色矩形
    - 根据 shadowAlpha 参数控制透明度
    - 使用 drawRect 绘制阴影
    - _需求: 8.1, 8.2, 8.3_

- [x] 6. 实现 ReaderContentWithCoverFlip 主组件
  - [x] 6.1 创建组件基础结构

    - 定义组件参数(pages, initialPage, config, onTap, onPageChange)
    - 使用 remember 创建 CoverFlipPagerState
    - 使用 remember 创建 CoverFlipTransformer
    - 使用 remember 创建 PageScrollListener
    - _需求: 1.1, 1.2_
  

  - [x] 6.2 实现状态监听和更新

    - 使用 LaunchedEffect 监听 LazyListState.layoutInfo
    - 使用 snapshotFlow 收集布局信息变化
    - 调用 pagerState.updateState 更新状态
    - 调用 scrollListener.onScroll 触发滚动回调
    - 使用 LaunchedEffect 监听 currentPage 变化
    - 触发 onPageChange 回调

    - _需求: 2.1, 2.2, 2.3_
  

  - [x] 6.3 实现 LazyRow 分页滚动
    - 使用 BoxWithConstraints 获取屏幕宽度
    - 创建 LazyRow,绑定 lazyListState
    - 配置 flingBehavior 为 SnapFlingBehavior
    - 设置 userScrollEnabled = true

    - _需求: 2.1, 2.4, 6.3, 6.4_


  
  - [x] 6.4 实现页面列表渲染
    - 使用 itemsIndexed 遍历 pages 列表
    - 为每个 item 设置唯一 key
    - 计算页面位置(calculatePagePosition)

    - 应用 CoverFlipTransformer 变换
    - 计算阴影透明度
    - _需求: 1.3, 1.4, 3.1, 3.2, 3.3, 8.1_
  

  - [x] 6.5 实现页面 Box 容器

    - 设置固定宽度(pageWidth)和填充高度
    - 使用 graphicsLayer 应用变换(translationX, alpha, zIndex)
    - 添加 pointerInput 处理点击手势
    - 调用 detectTouchZone 检测触摸区域
    - 触发 onTap 回调
    - 渲染 PageContent 组件

    - _需求: 1.4, 1.5, 4.4, 4.5, 6.1, 6.2, 10.1_

- [x] 7. 实现辅助函数
  - [x] 7.1 实现 calculatePagePosition 函数

    - 计算页面相对于当前页的位置
    - 公式: (pageIndex - currentPage) - scrollProgress

    - 返回 -1f 到 1f 之间的值,0f 表示完全可见
    - _需求: 1.3, 3.1, 3.2_
  

  - [x] 7.2 实现 detectTouchZone 函数
    - 根据触摸位置判断触摸区域
    - 支持左、中、右三个区域
    - 返回 TouchZone 枚举值
    - _需求: 6.1, 6.2_



- [x] 8. 实现边界处理
  - [x] 8.1 在 SnapFlingBehavior 中添加边界检测

    - 检测是否在第一页(currentPage == 0)
    - 检测是否在最后一页(currentPage == pageCount - 1)
    - 在 calculateTargetPage 中限制目标页面范围
    - 使用 coerceIn(0, pageCount - 1) 确保不越界
    - _需求: 11.1, 11.2, 11.4_

  

  - [x] 8.2 实现边界回弹动画
    - 在 SnapFlingBehavior 中添加 showBounceAnimation 方法
    - 检测边界滑动(第一页向右滑或最后一页向左滑)
    - 实现弹出动画(100dp 最大位移, 100ms 时长)
    - 实现回弹动画(使用 spring, 200ms 时长)
    - 可选:添加边缘发光效果

    - _需求: 11.1, 11.2, 11.3, 11.5_

- [x] 9. 实现多点触控处理

  - 在 LazyRow 的 pointerInput 中检测多点触控
  - 使用 awaitPointerEventScope 监听触摸事件

  - 检测 pointerCount > 1 的情况

  - 取消当前滚动动画(调用 lazyListState.stopScroll)
  - 回到最近的页面(调用 snapToTargetExistingView)
  - 优先处理缩放和平移手势(在外层 Box 中添加 transformable modifier)
  - _需求: 12.1, 12.2, 12.3, 12.4, 12.5_

- [x] 10. 性能优化

  - [x] 10.1 实现位图预加载和缓存

    - 创建 BitmapPreloader 类
    - 使用 LruCache 缓存位图(最多 3 个)
    - 在 ViewModel 中预加载当前页、前一页、后一页
    - 在页面切换时更新缓存
    - 在动画结束后释放不需要的资源


    - _需求: 10.2, 10.5_

  
  - [x] 10.2 优化渲染性能


    - 确保所有变换都在 graphicsLayer 中执行(GPU 加速)的对象
    - 使用 derivedStateOf 优化派生状态计算
    - 使用 remember 缓存不变
    - 避免在 Composable 中进行复杂计算

    - 使用 key 参数优化 LazyRow 的 item 重用
    - _需求: 10.1, 10.3, 10.4_
  
  - [x] 10.3 性能监控


    - 添加帧率监控(使用 Choreographer)
    - 添加内存使用监控

    - 确保动画过程中保持 60fps
    - 记录性能指标用于优化
    - _需求: 10.3_


- [x] 11. 添加配置选项
  - [x] 11.1 创建 CoverFlipConfig 数据类

    - 定义动画时长配置(animationDuration = 300ms)
    - 定义吸附阈值配置(swipeThreshold = 0.3f)
    - 定义速度阈值配置(velocityThreshold = 1000f)
    - 定义阴影配置(shadowEnabled, shadowMaxAlpha = 0.4f, shadowBlurRadius = 8dp)
    - 定义边界回弹配置(bounceEnabled, bounceMaxDisplacement = 100dp, bounceDuration = 200ms)
    - _需求: 5.1, 5.2, 5.3, 5.4, 5.5, 8.3, 11.3_
  
  - [x] 11.2 在 ReaderConfig 中集成


    - 添加 coverFlipConfig 属性
    - 添加 PageFlipMode 枚举(SLIDE, COVER, SIMULATION)
    - 添加 pageFlipMode 属性
    - 支持从配置文件读取和保存
    - _需求: 5.5_


- [x] 12. 集成到 ReaderScreen

  - [x] 12.1 修改 ReaderScreen 组件


    - 读取 ReaderConfig 中的 pageFlipMode
    - 根据模式选择不同的渲染组件
    - 当 pageFlipMode == COVER 时使用 ReaderContentWithCoverFlip
    - 否则使用原有的 ReaderContent
    - _需求: 所有需求_
  
  - [x] 12.2 连接 ViewModel

    - 从 ReaderViewModel 获取页面列表
    - 从 ReaderViewModel 获取当前页索引
    - 处理页面切换回调,更新 ViewModel 状态
    - 处理点击回调,触发相应操作
    - _需求: 所有需求_
  
  - [x] 12.3 实现页面数据管理

    - 在 ViewModel 中维护页面位图列表
    - 实现页面预加载逻辑
    - 实现页面缓存管理
    - 处理页面切换时的数据更新
    - _需求: 10.2, 10.5_

- [ ]* 12. 编写测试
  - [ ]* 12.1 单元测试
    - 测试动画状态转换逻辑
    - 测试阈值判断逻辑
    - 测试边界条件处理
    - 测试手势处理逻辑
  
  - [ ]* 12.2 UI 测试
    - 测试拖动手势响应
    - 测试动画流畅度
    - 测试边界回弹效果
    - 测试多点触控处理
  
  - [ ]* 12.3 性能测试
    - 监控帧率(目标 60fps)
    - 监控内存使用
    - 监控 CPU/GPU 负载
    - 测试大图片性能
