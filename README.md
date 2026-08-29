# Paysage for Android

<div align="center">

![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Local First](https://img.shields.io/badge/Local--First-SMS%20Forwarding-0F766E?style=for-the-badge)

**面向 Android 的短信转发、邮件指令与稳定保活工具**

*本地直发、稳定保活、可远程指令控制。*

[功能特性](#-功能特性) · [快速开始](#-快速开始) · [Telegram-Bot-配置](#-telegram-bot-配置) · [远程指令](#-远程指令) · [构建](#-构建) · [许可证](#-许可证)

</div>

---

## 概览

**Paysage** 是一款 Android 短信转发与设备管理工具。它可以把收到的短信直接从本机转发到邮箱或 Telegram，不经过中间服务器；也可以通过授权邮箱或 Telegram Bot 发送 `/paysage ...` 指令，远程开关短信转发、查看设备状态和触发重试。

应用重点解决短信转发类工具常见痛点：后台被杀、Doze 延迟、网络失败漏发、权限复杂、用户不知道当前是否正常运行。

---

## 功能特性

### 短信转发
- **Email 转发**：支持 Gmail、自定义 SMTP、密码/授权码、XOAUTH2。
- **Telegram 转发**：通过 Telegram Bot API 直接发送到指定 Chat ID。
- **本地直发**：设备直接连接 SMTP/Telegram，不经过 Paysage 中间服务器。
- **过滤规则**：支持号码白名单，避免无关短信被转发。
- **转发总开关**：可以在 App 内关闭，也可以通过远程指令开关。

### 稳定性与保活
- **Foreground Service**：稳定守护前台服务保持监听与重试链路。
- **WorkManager + AlarmManager**：后台周期检查、离线缓存重试、重启后恢复。
- **SMS Broadcast + ContentObserver + 无障碍增强**：多链路减少漏消息。
- **离线缓存与自动补发**：网络不可用或发送失败时进入缓存，恢复后重试。
- **健康仪表盘**：首页显示成功率、缓存、后台限制和潜在问题。

### 邮件指令中心
- **IMAP 收件箱检查**：从授权邮箱读取指令邮件。
- **白名单控制**：只有已授权邮箱的邮件可以执行指令。
- **执行日志**：记录允许、拒绝、忽略、失败等指令处理结果。
- **执行回执**：指令执行后自动向授权发件人发送当前状态。
- **实时监听可选**：可启用 IMAP IDLE 前台服务，降低邮件指令延迟。

### Telegram Bot 指令
- **直接在 App 配置 Bot Token**：Paysage 可以自动读取最近聊天并填入 Chat ID。
- **私聊和群聊**：私聊使用 `/paysage ...`，群聊可使用 `/paysage@BotUsername ...`。
- **近实时处理**：开启稳定守护后使用 Telegram 长轮询，指令延迟明显低于普通后台周期。
- **执行结果回复**：指令执行后 Bot 会回复结果、转发状态和离线缓存数量。

### 体验与设置
- **Material 3 Expressive 风格**：Compose + M3 组件，页面逐步引导。
- **莫奈配色**：支持系统动态色、内置配色、浅色/深色/跟随系统。
- **OLED 绝对深色**：深色模式下可使用纯黑背景。
- **权限管理**：集中检查短信、通知、电话、无障碍、电池优化等权限。
- **开发者设置**：真机导出日志，便于排查用户设备问题。

---

## 快速开始

### 系统要求
- Android 8.0 (API 26) 及以上。
- 需要短信、通知、网络等权限。
- 如果需要更稳定的后台运行，建议允许忽略电池优化。

### 安装 APK

Debug 构建：

```bash
./gradlew :app:assembleDebug
```

Release 构建：

```bash
./gradlew :app:assembleRelease
```

APK 输出在：

```text
app/build/outputs/apk/
```

当前项目会输出类似以下命名的 APK：

```text
Paysage-Android-arm64-v8a+armeabi-v7a-1.0.0-YYMMDDVV-NN.APK
```

### 首次使用建议

1. 打开 App，进入 **权限管理**，补齐短信、通知、后台和电池优化相关权限。
2. 进入 **转发账号**，添加 Email 或 Telegram 转发目标。
3. 在账号详情页点击 **测试发送**，确认通道真实可用。
4. 回到首页点击 **稳定优化/稳定守护**，减少后台被杀和指令延迟。
5. 如需远程控制，配置 **邮件收件箱指令中心** 或 **Telegram Bot 指令**。

---

## Telegram Bot 配置

### 1. 创建 Bot

1. 打开 Telegram，搜索 `@BotFather`。
2. 发送：

```text
/newbot
```

3. 按提示输入 Bot 名称和用户名。用户名通常需要以 `bot` 结尾。
4. BotFather 会返回一段 **Bot Token**，格式类似：

```text
123456789:AAxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

> Bot Token 等同于机器人密码，不要发给别人。如果泄露，请在 BotFather 中使用 `Revoke current token` 重新生成。

### 2. 在 Paysage 填入 Token

1. 打开 Paysage。
2. 进入 **设置 → 转发设置 → 转发账号**。
3. 添加或编辑一个 **Telegram** 账号。
4. 在 **Bot Token** 输入框粘贴 BotFather 给你的 Token。

### 3. 自动获取 Chat ID

私聊场景：

1. 在 Telegram 打开你刚创建的 Bot。
2. 点 `Start`，或发送：

```text
/start
```

3. 回到 Paysage，点击 **获取 Chat ID**。
4. App 会读取最近一条 Bot 消息并自动填入 **Chat ID**。
5. 点击 **测试发送**，能收到测试消息后保存。

群聊场景：

1. 把 Bot 拉进目标群。
2. 在群里发送一条消息，例如：

```text
/start
```

3. 回到 Paysage 点击 **获取 Chat ID**。
4. 群聊的 Chat ID 通常是负数或 `-100...` 开头。
5. 保存后，群里发送指令时建议使用：

```text
/paysage@你的Bot用户名 sw
```

### 4. 常见问题

- **401 Unauthorized**：Token 错误、已被重置、复制多了空格，重新从 BotFather 复制或 revoke 后再填。
- **Chat ID 为空**：先给 Bot 发送 `/start` 或 `hello`，再点获取。
- **测试发送失败**：查看 Paysage 页面显示的 Telegram API 错误原因，例如 `chat not found`。
- **指令延迟大**：开启首页的稳定守护；未开启时 Android 可能把后台任务延迟到 15 分钟左右。

---

## 远程指令

Paysage 目前支持通过 **授权邮箱** 和 **Telegram Bot** 执行简单指令。所有简单指令都必须以 `/paysage` 开头，`sw` 不能单独使用。

### 支持的简单指令

| 指令 | 作用 | 备注 |
| --- | --- | --- |
| `/paysage sw` | 开关短信转发 | 当前开启则暂停，当前暂停则恢复 |
| `/paysage shareswitch` | 开关短信转发 | 与 `/paysage sw` 等价 |
| `/paysage status` 或 `/paysage sta` | 查看设备状态 | 返回电池电量、温度、电压、健康度等信息 |
| `/paysage network` 或 `/paysage net` | 网络测速 | 测试延迟（多个目标）、下载速度、网络类型，约消耗 3MB 流量 |
示例：

```text
/paysage sw
/paysage shareswitch
/paysage status
/paysage sta
/paysage network
/paysage net
```

群聊中如需明确发送给某个 Bot：

```text
/paysage@YourPaysageBot sw
```

### Telegram 指令使用方式

1. 确认 Telegram 转发账号已保存，且测试发送成功。
2. 私聊 Bot 或在已配置 Chat ID 的群里发送指令。
3. Paysage 收到后会执行，并由 Bot 回复：
   - 执行动作
   - 执行结果
   - 当前短信转发状态
   - 离线缓存数量

### 邮件指令使用方式

1. 进入 **设置 → 转发设置 → 邮件收件箱指令中心**。
2. 配置 IMAP 收件箱并测试连接。
3. 添加授权邮箱。
4. 从授权邮箱发送一封邮件，正文只写一条简单指令，例如：

```text
/paysage sw
```

5. Paysage 执行后会发送回执邮件。

邮件指令支持更高级的 `#paysage` 签名格式，用于带过期时间、nonce 和 key/HMAC 的安全命令。普通用户建议优先使用简单指令和授权邮箱白名单。

---

## 稳定运行建议

为了减少漏转发、延迟和重启后失效，建议完成以下设置：

- 在 **权限管理** 中授予短信读取、短信接收和通知权限。
- 在系统电池设置中允许 Paysage 忽略电池优化。
- 打开首页的 **稳定守护**。
- 如果使用邮件指令并希望更低延迟，开启邮件实时监听。
- 如果使用 Telegram 指令并希望近实时响应，保持稳定守护运行。
- 在厂商系统中把 Paysage 加入自启动/后台白名单。

---

## 安全与隐私

- **本地直发**：短信内容直接从设备发送到你配置的 SMTP 或 Telegram。
- **无中间服务器**：Paysage 不需要自有云端转发服务。
- **凭证加密存储**：SMTP 密码、OAuth2 token、AES key 等使用 Android Keystore + EncryptedSharedPreferences 保存。
- **可选端到端加密**：Email 转发内容可使用 AES-GCM 加密。
- **Bot Token 敏感**：Telegram Bot Token 泄露后请立即在 BotFather 重置。
- **授权来源控制**：邮件指令需要授权邮箱；Telegram 指令只处理已配置 Chat ID。

---

## 技术栈

- **语言**：Kotlin
- **UI**：Jetpack Compose + Material 3
- **架构**：Repository + Room + Flow/Coroutines
- **数据库**：Room
- **安全存储**：Android Keystore + EncryptedSharedPreferences
- **后台任务**：Foreground Service + WorkManager + AlarmManager
- **邮件**：JavaMail for Android，SMTP/IMAP
- **Telegram**：Telegram Bot API
- **最低系统**：Android 8.0 (API 26)
- **目标系统**：Android 16 / SDK 36

依赖版本以 `gradle/libs.versions.toml` 与 `app/build.gradle.kts` 为准。

---

## 开发构建

```bash
# 编译 Debug
./gradlew :app:assembleDebug

# 编译 Release
./gradlew :app:assembleRelease

# 只检查 Kotlin 编译
./gradlew :app:compileDebugKotlin

# 安装 Debug 到已连接设备
./gradlew :app:installDebug
```

Release 构建启用 R8 压缩与资源收缩：

```kotlin
isMinifyEnabled = true
isShrinkResources = true
```

---

## 故障排查

### Telegram 指令没有反应

1. 确认 Telegram 账号已启用并保存。
2. 确认 Bot Token 测试发送成功。
3. 确认 Chat ID 是当前私聊或群聊的 ID。
4. 指令必须写成 `/paysage sw`，不能只写 `sw`。
5. 群聊建议写 `/paysage@BotUsername sw`。
6. 开启稳定守护，降低后台调度延迟。

### 邮件指令被忽略

1. 确认 IMAP 收件箱已配置并测试成功。
2. 确认发件邮箱已加入授权列表。
3. 邮件正文只放一条指令，避免多条指令同时出现。
4. 查看邮件指令中心的操控日志。

### 短信没有转发

1. 检查短信接收/读取权限。
2. 检查转发总开关是否被 `/paysage sw` 暂停。
3. 检查是否配置了转发账号并通过测试发送。
4. 检查过滤规则是否排除了该号码。
5. 查看首页健康仪表盘和转发日志。

---

## 项目结构

```text
app/src/main/java/joyin/takgi/paysage/
├── accessibility/          # 短信通知无障碍增强
├── data/                   # Room 实体和 DAO
├── debug/                  # 设备日志导出
├── mail/                   # 邮件收件箱、指令解析、执行回执
├── receiver/               # SMS 广播接收
├── reliability/            # 保活、重试、健康状态、后台调度
├── security/               # 转发账号敏感凭证加密存储
├── sender/                 # Email / Telegram 发送器
├── service/                # 前台服务
├── telegram/               # Telegram Bot 配置探测和指令轮询
├── ui/                     # Compose 页面、主题和动效
└── util/                   # 通用工具
```

---

## 致谢

UI 结构和文档组织参考 Monica for Android 的 Android 应用体验。分发时会保留上游许可证、来源说明和必要的开源义务。

---

## 许可证

Paysage 使用与 OpenEUICC 根项目一致的 **GNU General Public License v3.0** 分发，详见 [LICENSE](./LICENSE)。

第三方组件保留各自许可证文件，例如 `external/lpac-jni/LICENSE`、`external/lpac-jni/src/main/jni/lpac/LICENSES/` 和 `external/lpac-jni/src/main/jni/cjson/cjson/LICENSE`。

---

<div align="center">

**用 Kotlin、Compose 和一点点强迫症打造**

[回到顶部](#paysage-for-android)

</div>
