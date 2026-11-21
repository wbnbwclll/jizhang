# 语音记账（VoiceLedger）

一个 Android 端记账 App：按住说话或打字输入一句话（如"昨天打车 32"），由 AI 解析成结构化账单，用户确认后入账；断网时用本地规则兜底，账单存储在 Room 数据库，支持按月浏览、搜索、编辑和删除。

- 需求与验收标准：[docs/01-需求与验收标准.md](docs/01-需求与验收标准.md)
- 形态：Android 原生（Kotlin + Jetpack Compose + Room）
- 语音识别：端侧离线小模型（计划阶段，尚未接入）
- 自然语言解析：DeepSeek API（计划阶段，尚未接入）

## 运行环境

- Android Studio（自带 JDK 21+）
- Android SDK Platform 37、Build-Tools
- Gradle 9.6.0（由 Gradle Wrapper 自动下载）
- 最低运行版本：Android 8.0（API 26）

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
