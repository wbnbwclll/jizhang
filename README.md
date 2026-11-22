# 语音记账（VoiceLedger）

一个 Android 端记账 App：按住说话或打字输入一句话（如"昨天打车 32"），由 AI 解析成结构化账单，用户确认后入账；断网时用本地规则兜底，账单存储在 Room 数据库，支持按月浏览、搜索、编辑和删除。

- 需求与验收标准：[docs/01-需求与验收标准.md](docs/01-需求与验收标准.md)
- 形态：Android 原生（Kotlin + Jetpack Compose + Room）
- 语音识别：端侧离线小模型 Vosk（已接入，模型需本地准备）
- 自然语言解析：DeepSeek API（计划阶段，尚未接入）

## 已实现功能（当前版本）

- 手动记账：金额（分存储、无浮点误差）、收支切换、分类、日期时间、备注、商户
- 语音记账：编辑页点「语音记账」开始录音，再次点击结束识别，识别文本自动填入备注，文本中的阿拉伯数字金额自动填入金额
- 端侧离线识别：Vosk 中文小模型（约 42 MB），不联网、音频不落盘
- 账单列表：按月份切换、按月合计（支出/收入）、关键字搜索（备注/商户/分类）
- 编辑与删除：点击条目修改，左滑或编辑页删除，均带二次确认
- 本地持久化：Room 数据库，杀进程后数据不丢
- 输入校验：金额为空/为 0/非数字时提示并禁用保存

尚未实现（后续迭代）：DeepSeek 自然语言解析、中文数字金额（"二十八块五"）、统计图表与预算提醒。

## 语音模型准备（首次构建必做）

模型文件体积大，不纳入版本管理。首次克隆仓库后执行：

```powershell
curl.exe -L -o "$env:TEMP\vosk-cn.zip" https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip
Expand-Archive "$env:TEMP\vosk-cn.zip" -DestinationPath "app/src/main/assets" -Force
Rename-Item "app/src/main/assets/vosk-model-small-cn-0.22" "vosk-model-cn"
Set-Content "app/src/main/assets/vosk-model-cn/uuid" -Value "vosk-model-small-cn-0.22" -NoNewline
```

缺少该目录时应用仍可编译运行，只是「语音记账」会提示模型加载失败，手动记账不受影响。
带模型的 debug APK 约 93 MB。

## 运行环境

- Android Studio（自带 JDK 21+）
- Android SDK Platform 37、Build-Tools
- Gradle 9.6.0（由 Gradle Wrapper 自动下载）
- 最低运行版本：Android 8.0（API 26）

## 数据存储

- 数据库文件：`ledger.db`，表 `transactions`，版本 v1
- 建表结构导出在 `app/schemas/com.wbnbwclll.voiceledger.data.local.LedgerDatabase/1.json`
- 金额字段 `amountCents` 为整数"分"；`source` / `rawInput` / `parser` 为后续语音与 AI 解析预留

## 测试

```bash
gradlew.bat testDebugUnitTest
```

覆盖金额解析（含 0.1+0.2 浮点误差用例）与月份区间计算。

## 构建与安装

```bash
# Windows
gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

也可直接用 Android Studio 打开本仓库根目录，等待 Gradle Sync 完成后点 Run。

## 依赖加速（国内网络）

`settings.gradle.kts` 已优先使用阿里云 Maven 镜像；Gradle 发行包使用腾讯云镜像。
若本机需要代理访问 `dl.google.com`，在用户级 `~/.gradle/gradle.properties` 中配置（不要写进本仓库）：

```properties
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=7897
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=7897
```

## 配置说明

- `local.properties` 由 Android Studio 自动生成，包含本机 SDK 路径，**不纳入版本管理**。
- DeepSeek API Key 只写入 `local.properties`，通过 `BuildConfig` 读取；仓库中仅保留 `local.properties.sample` 占位示例。
- 离线 ASR 模型文件体积较大，不提交到仓库，按后续说明单独下载。

## 目录结构

```
app/src/main/java/com/wbnbwclll/voiceledger/   源码
app/src/main/res/                              资源与主题
app/src/test、app/src/androidTest/             单元测试与仪器测试
docs/                                          需求与过程记录
```
