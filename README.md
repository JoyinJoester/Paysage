# Paysage - Android 短信转发应用

一个简洁优雅的 Android 短信转发应用，采用 Material 3 设计，支持将短信转发到邮箱和 Telegram。

## 功能特性

✅ **双通道转发**
- 📧 邮箱转发（支持 Gmail、QQ 邮箱、163 等 SMTP 服务）
- 📱 Telegram Bot 转发

✅ **智能过滤**
- 白名单：仅转发指定号码
- 黑名单：拒绝垃圾号码
- 关键词：按内容筛选（如"验证码"）

✅ **Material 3 设计**
- 干净简洁的界面
- 流畅的动画和交互
- 深色模式支持

## 安装使用

### 1. 安装 APK
生成的 APK 位于：`app/build/outputs/apk/debug/app-debug.apk`

安装到 Android 设备后，首次打开会请求以下权限：
- **读取短信**（READ_SMS）
- **接收短信**（RECEIVE_SMS）
- **通知权限**（POST_NOTIFICATIONS，Android 13+）

### 2. 配置邮箱转发

进入"配置转发设置"界面：

1. 启用邮箱转发开关
2. 填写 SMTP 配置：
   - **SMTP 服务器**：如 `smtp.gmail.com`（Gmail）、`smtp.qq.com`（QQ 邮箱）
   - **端口**：通常为 `587`（TLS）或 `465`（SSL）
   - **账号**：你的邮箱地址
   - **密码**：邮箱密码或应用专用密码
   - **接收邮箱**：接收转发短信的邮箱地址
3. 点击"保存设置"

**常用邮箱 SMTP 配置**：
- Gmail：`smtp.gmail.com:587`（需要开启"不太安全的应用访问"或使用应用专用密码）
- QQ 邮箱：`smtp.qq.com:587`（需要生成授权码）
- 163 邮箱：`smtp.163.com:465`

### 3. 配置 Telegram 转发

1. 创建 Telegram Bot：
   - 打开 Telegram，搜索 `@BotFather`
   - 发送 `/newbot` 创建机器人
   - 按提示设置名称，获得 Bot Token（格式：`123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11`）

2. 获取 Chat ID：
   - 向你的 Bot 发送任意消息
   - 访问 `https://api.telegram.org/bot<你的Token>/getUpdates`
   - 在返回的 JSON 中找到 `chat.id` 字段

3. 在应用中填写：
   - **Bot Token**：从 BotFather 获得的 Token
   - **Chat ID**：你的聊天 ID
   - 启用 Telegram 转发开关

### 4. 设置过滤规则

进入"管理过滤规则"界面：

- **白名单**：只有匹配的号码会被转发（如：`10086`）
- **黑名单**：匹配的号码不会被转发（如：`95`，拦截骚扰电话）
- **关键词**：包含关键词的短信会被转发（如：`验证码`、`快递`）

点击右下角 ➕ 按钮添加规则。

## 工作原理

1. **短信监听**：应用通过 `SmsReceiver` 监听系统短信广播
2. **过滤判断**：根据用户设置的规则判断是否转发
3. **并发转发**：使用 Kotlin 协程同时发送到邮箱和 Telegram
4. **前台服务**：转发过程在前台服务中执行，确保稳定性

## 技术栈

- **语言**：Kotlin
- **UI**：Jetpack Compose + Material 3
- **架构**：MVVM + Repository Pattern
- **数据库**：Room（存储过滤规则）
- **存储**：DataStore（加密存储配置）
- **邮件**：JavaMail API
- **网络**：HttpURLConnection（Telegram API）

## 项目结构

```
app/src/main/java/joyin/takgi/paysage/
├── MainActivity.kt              # 主入口 + 权限请求
├── receiver/
│   └── SmsReceiver.kt          # 短信广播接收器
├── service/
│   └── ForwardService.kt       # 前台转发服务
├── sender/
│   ├── EmailSender.kt          # SMTP 邮件发送
│   └── TelegramSender.kt       # Telegram Bot API
├── repository/
│   ├── ConfigRepository.kt     # 配置管理
│   └── FilterRepository.kt     # 过滤规则管理
├── data/
│   ├── FilterRule.kt           # 过滤规则实体
│   ├── FilterDao.kt            # Room DAO
│   └── AppDatabase.kt          # Room 数据库
└── ui/
    ├── screens/
    │   ├── HomeScreen.kt       # 主界面
    │   ├── SettingsScreen.kt   # 设置界面
    │   └── FilterScreen.kt     # 过滤规则界面
    └── theme/
        └── Theme.kt            # M3 主题
```

## 注意事项

⚠️ **隐私安全**：
- 邮箱密码和 Bot Token 使用 DataStore 加密存储
- 建议使用邮箱的"应用专用密码"而非主密码
- 不要将配置信息分享给他人

⚠️ **后台运行**：
- Android 8+ 需要允许应用在后台运行
- 部分手机需要在电池优化中添加白名单

⚠️ **网络权限**：
- 需要联网才能转发到邮箱和 Telegram
- 建议在 Wi-Fi 下使用以节省流量

## 开发构建

```bash
# 编译 Debug 版本
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug

# 清理构建
./gradlew clean
```

## 许可证

MIT License

---

**开发者**：Powered by Claude Code
**版本**：1.0
