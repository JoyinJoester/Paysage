# Paysage for Android

<div align="center">

**中文**

![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)
![Local First](https://img.shields.io/badge/Local--First-SMS%20Forwarding-0F766E?style=for-the-badge)
![CI](https://img.shields.io/badge/CI-GitHub%20Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white)

<p><strong>短信/彩信转发、远程指令、设备仪表盘——为热点机打造的常驻管家</strong></p>
<p>Android · Local Direct · Email / Telegram / Webhook · Root & LSPosed Optional</p>

</div>

---

## 概览

**Paysage** 是一款面向"热点机 / 备用机"场景的 Android 消息转发与设备管理工具。它把收到的短信和彩信直接从本机转发到邮箱、Telegram、Bark、Server酱、钉钉、飞书、企业微信或任意 Webhook,**不经过任何中间服务器**;同时支持通过授权邮箱或 Telegram Bot 发送 `/paysage ...` 指令,远程开关转发、查询设备状态、触发重试。

它重点解决转发类工具的常见痛点:后台被杀、Doze 延迟、网络失败漏发、长短信被拆成多条、权限复杂、用户不知道当前是否正常运行。

---

## 用户先看

### Paysage 适合谁

- 把旧手机当**热点机 / 上网终端**,人不在旁边,需要把短信(验证码、通知)实时转发到主力设备。
- 希望数据**本地直发**,不接受第三方转发服务器经手短信。
- 需要**远程管理**设备:开关转发、查电量温度、看网络延迟。
- 有 Root / LSPosed 环境,愿意用系统级手段把稳定性拉满(可选,没有也完全可用)。

### 你能得到什么

- 8 类转发通道:Email(SMTP/XOAUTH2)、Telegram、通用 Webhook、Bark、Server酱、钉钉、飞书、企业微信。
- 短信 + 彩信文本采集,多链路防漏:广播 + 收件箱观察 + 无障碍通知 + 分段合并 + 入口去重。
- 离线缓存与自动补发:网络断开时入缓存,恢复后自动重试,30 天转发日志可查。
- 设备仪表盘:实时上下行速率曲线、网络延迟、流量统计、电池温度电压、内外网 IP。
- 远程指令:`/paysage sw | status | network`,带执行回执。
- 可选 Root 增强与 LSPosed 模块(见下文详细说明)。

### 快速开始

1. 从 [Releases](https://github.com/JoyinJoester/Paysage/releases) 下载 APK(Android 8.0+)。
2. 打开 App,进入 **设置 → 权限管理**,补齐短信、通知等权限。
3. 进入 **设置 → 转发设置 → 转发账号**,添加 Email 或 Telegram 等账号,并在账号详情页**测试发送**。
4. 回到首页开启**稳定守护**,减少后台被杀和指令延迟。
5. 需要远程控制时,配置 **邮件收件箱指令中心** 或 **Telegram Bot 指令**。

### 已知限制

- 彩信只转发文本部分,图片/音视频附件不转发(避免大流量与隐私风险)。
- 部分厂商 ROM 会限制后台与系统计数器读取,建议配合稳定守护(或 Root 增强)使用。
- 邮件指令的 `#paysage` 签名格式面向高级用户,普通场景建议用简单指令 + 白名单。

---

## 转发能力

### 通道一览

| 通道 | 配置要点 | 说明 |
| --- | --- | --- |
| Email | SMTP 主机 + 授权码/OAuth2 | 支持 Gmail 预设、XOAUTH2、可选正文 AES-GCM 加密 |
| Telegram | Bot Token + Chat ID | 支持自动读取最近聊天填 Chat ID |
| 通用 Webhook | 完整 URL + 可选 Token | POST JSON(`title`/`message`),Token 走 `X-Paysage-Token` 头 |
| Bark | 设备 Key | 地址可留空走官方服务器 |
| Server酱 | SendKey | 微信推送 |
| 钉钉机器人 | 机器人 Webhook + 可选加签 | 自动计算 HMAC-SHA256 时间戳签名 |
| 飞书机器人 | 机器人 Webhook | 文本消息 |
| 企业微信机器人 | 机器人 Webhook | 文本消息(自动截断 2000 字) |

所有通道均为**本地直发**,不经中间服务器。多账号可并存,支持按号码白名单匹配账号。

### 防漏防重

- **采集链路**:SMS 广播(SMS_RECEIVED)+ 收件箱 ContentObserver + 无障碍通知增强(+ 可选 Root 兜底 / LSPosed)。
- **分段合并**:同一发送方 3 秒窗口内的长短信分段合并为一条,重复段忽略、更完整的段替换,跨广播到达的长短信不会拆成多条转发。
- **入口去重**:认领式去重(90 秒认领 + 3 分钟判重窗口),多链路并发只转发一次;通知栏标题残缺的副本会被正文包含判重拦下。
- **失败兜底**:发送失败或断网自动进入离线缓存,按指数退避重试;转发日志保留 30 天,条目可点进详情。

### 过滤规则

- 号码黑名单 / 白名单、正文关键词屏蔽、关键词放行、正则屏蔽。
- 支持从**转发日志详情页选词直接添加**:点开任意日志,点选正文中的词,一键生成关键词规则;发件人可一键加黑/白名单。
- 重复规则自动跳过,规则即时生效。

---

## 远程指令

所有简单指令以 `/paysage` 开头(群聊用 `/paysage@BotUsername ...`):

| 指令 | 作用 | 备注 |
| --- | --- | --- |
| `/paysage sw` / `shareswitch` | 开关短信转发 | 当前开启则暂停,暂停则恢复 |
| `/paysage status` / `sta` | 设备状态 | 电量、温度、电压、健康度等 |
| `/paysage network` / `net` | 网络测速 | 多目标延迟 + 下载速度,约消耗 3MB 流量 |

邮件指令:在 **设置 → 转发设置 → 邮件收件箱指令中心** 配置 IMAP 收件箱并添加授权邮箱,从授权邮箱发送正文仅含一条指令的邮件即可,执行后自动回执。高级用户可用带过期时间、nonce 和 HMAC 签名的 `#paysage` 格式。

Telegram 指令:配置 Bot Token 后点击 **获取 Chat ID**,私聊或群聊发送指令,Bot 回复执行结果、转发状态和缓存数量。

---

## 设备仪表盘

底栏第二个标签,面向热点机场景的实时状态面板:

- **网络速度**:每秒读取内核字节计数器做差分,上下行速率 + 60 秒曲线。被动采样、零测试流量,不影响网速;仅页面停留期间检测,离开即停。
- **延迟**:请求运营商 204 探测地址测往返耗时,10 秒刷新,<200ms 绿 / <500ms 黄 / 超时红。
- **本次流量统计 / 电池与温度 / 内网 IP / 公网 IP**:会话累计流量,电量、温度、电压、充电状态,热点网段与公网出口 IP。
- **顶栏一键推送**:把当前仪表盘状态推送到 Telegram 或邮箱(多通道弹窗选择,单通道直接推送)。

采样准确性:优先按接口精确统计蜂窝 WAN 口(热点 AP 口不计入,避免客户端流量双重计数);系统限制逐级回退,最后一级为全局总量估算,会如实标注。

---

## Root 权限说明(可选)

**Paysage 不需要 Root 也能完整工作。** Root 模式是给已经刷了 Magisk/KernelSU 的用户准备的稳定性增强,全部能力都需要在 **设置 → Root 模式** 里手动开启,默认关闭。

### 开启后 Root 具体做三件事

**1. 一键授权并解除后台限制**

- 做什么:`pm grant` 静默授予短信/电话/相机/通知等危险权限;`cmd appops` 放行后台运行与前台服务启动;`dumpsys deviceidle whitelist` 把应用加入电池优化白名单。
- 解决什么:免去在十几层厂商设置页里手动翻"自启动、省电策略、后台弹出"等开关,一条命令全部到位。

**2. Root 看门狗保活**

- 做什么:向 `/data/adb/service.d/` 写入一个 Magisk 开机脚本,每 20 秒检查应用进程,发现进程不在就用 `am start-foreground-service` 拉起保活服务;重启后脚本自动生效。
- 解决什么:厂商 ROM 杀后台后,Android 层的自启/守护可能被拦,root 看门狗是系统级兜底,被杀最多 20 秒就复活。
- 如何撤销:在 Root 模式页关闭开关即删除脚本;应用卸载后脚本检测不到包,连续 30 次(约 10 分钟)会自删退出,不留孤儿进程。

**3. 短信库兜底采集**

- 做什么:用 root 把系统短信库 `mmssms.db` 只读拷贝到应用私有缓存,按 `_ID` 增量读取新收件箱,随后走统一的分段合并与去重。
- 解决什么:当 ROM 连短信广播和收件箱观察都拦截时,这是第四条采集链路,保证验证码不漏。

### 安全边界

- Root 只在用户显式开启对应开关后才被调用;关闭开关立即停止并清理(脚本删除、不再读取)。
- su 会话在离开 Root 模式页面时立即关闭,不常驻。
- 短信库副本存放在应用私有缓存目录,权限 600,用完即删。
- 不修改任何系统分区、不提权其他应用、不上传任何数据。
- 关闭全部 Root 开关后,应用回到与无 Root 设备完全相同的行为。

---

## LSPosed 模块说明(可选)

Paysage 同时是一个 LSPosed 模块(基于 libxposed API 100):

- **作用域**:`com.android.phone`(电话进程)。
- **做什么**:hook 短信入站分发方法,在 framework 收到短信的第一现场读取内容并发给应用,绕过广播拦截与进程限制——采集可靠性的理论上限。
- **启用方式**:LSPosed 管理器中启用模块,勾选作用域 `com.android.phone`,重启系统界面或重启手机。
- **防伪造**:桥接广播要求发送方持有 `MODIFY_PHONE_STATE` 系统权限,第三方应用无法注入假短信。
- **安全**:所有 hook 都有异常保护,失败只记日志,不影响电话功能。

---

## 稳定运行建议

- 在 **权限管理** 中授予短信读取、接收和通知权限(或用 Root 一键授权)。
- 允许忽略电池优化,打开首页的 **稳定守护**。
- 邮件指令低延迟需求开启 IMAP 实时监听;Telegram 指令近实时依赖稳定守护常驻。
- 在厂商系统里把 Paysage 加入自启动/后台白名单(有 Root 可一键完成)。

---

## 安全与隐私

- **本地直发**:消息直接从设备发送到你配置的通道,无中间服务器、无统计 SDK、无 Google Play Services。
- **凭证加密**:SMTP 密码、OAuth2 Token、加密密钥使用 Android Keystore + EncryptedSharedPreferences。
- **可选端到端加密**:Email 转发正文可用 AES-GCM 加密。
- **备份安全**:云备份与设备迁移默认排除加密凭证和数据库(Keystore 密钥不随备份迁移,强排避免恢复即损坏),换机后重新配置账号即可。
- **授权控制**:邮件指令需授权邮箱白名单(+可选 HMAC 签名);Telegram 指令只处理已配置 Chat ID。

---

## 技术栈

- **语言**:Kotlin
- **UI**:Jetpack Compose + Material 3(莫奈动态色 / OLED 纯黑)
- **架构**:Repository + Room + Flow/Coroutines
- **安全存储**:Android Keystore + EncryptedSharedPreferences
- **后台**:Foreground Service + WorkManager + AlarmManager
- **邮件**:JavaMail for Android(SMTP/IMAP)
- **eSIM**:external/lpac-jni(lpac JNI 封装,独立模块)
- **Xposed**:libxposed API(API 100,vendored 源码)
- **系统要求**:Android 8.0+(API 26),目标 SDK 36

---

## 项目结构

```text
app/src/main/java/joyin/takgi/paysage/
├── accessibility/          # 短信通知无障碍增强
├── data/                   # Room 实体和 DAO
├── debug/                  # 设备日志导出
├── esim/                   # eSIM/eUICC 采集与诊断(外部卡经 lpac-jni)
├── mail/                   # 邮件收件箱、指令解析、执行回执
├── receiver/               # SMS 广播与 Xposed 桥接收
├── reliability/            # 保活、重试、分段合并、入口去重
│   └── root/               # Root 增强:su 会话、授权、看门狗、短信库兜底
├── repository/             # 过滤规则仓库
├── security/               # 转发账号敏感凭证加密存储
├── sender/                 # Email / Telegram / Webhook 类发送器
├── service/                # 前台服务
├── telegram/               # Telegram Bot 配置探测和指令轮询
├── ui/                     # Compose 页面、主题和动效
├── util/                   # 通用工具(网速监测、延迟探测、公网 IP 等)
└── xposed/                 # LSPosed 模块入口(hook com.android.phone)
external/lpac-jni/          # lpac 的 JNI 封装(独立 Gradle 模块)
```

---

## 开发构建

```bash
./gradlew :app:assembleDebug          # Debug 构建
./gradlew :app:assembleRelease        # Release 构建(R8 + 资源收缩)
./gradlew :app:testDebugUnitTest      # 全量单元测试
./gradlew :app:installDebug           # 安装到已连接设备
```

- APK 输出命名:`Paysage-Android-arm64-v8a+armeabi-v7a-1.0.0-YYMMDDVV-NN.APK`。
- **versionCode** 按"epoch 天数 × 100 + 当日序号"自动递增,无需手动维护。
- **CI**:GitHub Actions 在 push/PR 时自动跑全量单测与 release 编译。
- **Release 签名(可选)**:在项目根目录创建 `keystore.properties`(已被 gitignore):

```properties
storeFile=/path/to/your.jks
storePassword=你的store密码
keyAlias=你的alias
keyPassword=你的key密码
```

配置后 `assembleRelease` 自动签名;没有该文件时输出未签名 APK(可用 `apksigner` 手动签名)。

---

## 故障排查

### 短信没有转发

1. 检查短信接收/读取权限;转发总开关是否被 `/paysage sw` 暂停。
2. 检查转发账号是否已配置并通过测试发送;过滤规则是否排除了该号码。
3. 打开 **转发日志**,点进具体条目查看完整内容与状态。
4. 厂商 ROM 拦截严重时,开启 Root 增强(短信库兜底)或 LSPosed 模块。

### Telegram 指令没有反应

1. 确认 Telegram 账号已启用且测试发送成功;Chat ID 是当前私聊/群聊的 ID。
2. 指令必须写成 `/paysage sw`,群聊建议 `/paysage@BotUsername sw`。
3. 多设备不要共用同一个 Bot Token(getUpdates 会互相冲突)。
4. 开启稳定守护,降低后台调度延迟。

### 邮件指令被忽略

1. IMAP 收件箱已配置并测试通过;发件邮箱在授权列表中。
2. 邮件正文只放一条指令,避免多指令混排。
3. 查看邮件指令中心的操控日志。

---

## 致谢与许可

- UI 结构与文档组织参考 [Monica for Android](https://github.com/Monica-Pass/Monica-for-Android) 的体验。
- eSIM 能力基于 [lpac](https://github.com/estk/lpac)(经 `external/lpac-jni` 封装)。
- LSPosed 模块基于 [libxposed/api](https://github.com/libxposed/api)(Apache-2.0,vendored 于 `app/src/vendored`)。

Paysage 使用与 OpenEUICC 一致的 **GNU General Public License v3.0** 分发,详见 [LICENSE](./LICENSE)。

---

<div align="center">

**用 Kotlin、Compose 和一点点强迫症打造**

[回到顶部](#paysage-for-android)

</div>
