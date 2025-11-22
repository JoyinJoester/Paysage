# Implementation Plan

- [x] 1. 重构 BookCompactCard 组件为叠加式布局


  - 将现有的 Column 布局改为 Box 布局以支持叠加效果
  - 移除封面下方的独立信息区域（Column 中的 padding 和内容）
  - 确保封面图片填充整个卡片区域（保持 0.7 宽高比）
  - 保留状态标签在左上角的显示
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_



- [ ] 2. 实现底部叠加层
  - [ ] 2.1 创建渐变背景层
    - 使用 Brush.verticalGradient 创建从透明到半透明黑色的渐变
    - 渐变层占据卡片底部约 30% 的高度
    - 使用 Box 的 align(Alignment.BottomCenter) 定位到底部

    - _Requirements: 4.1, 4.2, 4.5_
  
  - [ ] 2.2 添加叠加标题
    - 在渐变背景上显示书籍标题
    - 使用白色文字颜色和 titleSmall 字体样式
    - 设置 maxLines = 2 和 TextOverflow.Ellipsis

    - 添加适当的内边距（水平 12dp，垂直 8dp）
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_
  
  - [ ] 2.3 添加叠加进度条
    - 在标题下方添加 LinearProgressIndicator
    - 使用 6dp 高度和主题色


    - 根据 showProgress 参数控制显示
    - 设置适当的内边距（水平 12dp，底部 8dp）
    - 使用半透明白色作为轨道颜色
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_




- [ ] 3. 优化视觉效果
  - 调整渐变背景的透明度为 0.7f 以确保文字可读性
  - 确保封面上部 70% 区域清晰可见
  - 验证进度条紧贴封面底部边缘
  - 确保状态标签在最上层显示
  - _Requirements: 4.3, 4.4, 5.1, 5.2, 5.3_

- [ ] 4. 验证和测试
  - 在不同屏幕尺寸上测试卡片显示效果
  - 验证长标题的截断效果
  - 验证无封面时的默认图标显示
  - 验证不同阅读进度的显示效果
  - 确保状态标签（包括 ConnectedReadingStatusBadge）正确显示
  - _Requirements: 5.4, 5.5_
