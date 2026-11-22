# 语音记账（VoiceLedger）

一个 Android 端记账 App：按住说话或打字输入一句话（如"昨天打车 32"），由 AI 解析成结构化账单，用户确认后入账；断网时用本地规则兜底，账单存储在 Room 数据库，支持按月浏览、搜索、编辑和删除。

- 需求与验收标准：[docs/01-需求与验收标准.md](docs/01-需求与验收标准.md)
- 形态：Android 原生（Kotlin + Jetpack Compose + Room）
- 语音识别：端侧离线小模型 Vosk，模型随 APK 打包
- 自然语言解析：DeepSeek API（计划阶段，尚未接入）

## 已实现功能（当前版本）

- 手动记账：金额（分存储、无浮点误差）、收支切换、分类、日期时间、备注、商户
- 一句话记账：在首页输入文字，或点输入框右下角的麦克风录音。再次点击结束录音，离线转写回填到输入框，可继续修改
- 草稿确认：点击「继续」进入编辑页，输入内容带入备注，尝试用本地规则填写金额。确认金额、分类等字段后手动保存
- 端侧离线识别：Vosk 中文小模型（约 42 MB），不联网、音频不落盘
- 账单列表：按月份切换、按月合计（支出/收入）、关键字搜索（备注/商户/分类）
- 编辑与删除：点击条目修改，左滑或编辑页删除，均带二次确认
- 本地持久化：Room 数据库，杀进程后数据不丢
- 输入校验：金额为空/为 0/非数字时提示并禁用保存

尚未实现（后续迭代）：DeepSeek 自然语言解析、中文数字金额（"二十八块五"）、统计图表与预算提醒。

## 安装包与模型

- [Android 调试版 APK](dist/VoiceLedger-input-debug.apk)，约 93 MiB，内置离线中文语音模型。
- [Vosk 中文语音模型](app/src/main/assets/vosk-model-cn)，克隆后可直接参与 Android 构建。权重来源为 [Vosk 模型目录](https://alphacephei.com/vosk/models)。
- [ELECTRA 中文基模](models/electra/chinese-electra-180g-small-discriminator)，含 PyTorch 和 TensorFlow 权重、配置及分词文件。基模用作后续语义微调，训练方案见 [ELECTRA 训练文档](docs/02-electra-training.md)，测试用例见 [测试集](data/electra)。

APK 和 ELECTRA 权重使用 Git LFS。若克隆后看到的是小型指针文件，安装 Git LFS 并在仓库内执行 `git lfs pull`。

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

## 目录结构

```
app/src/main/java/com/wbnbwclll/voiceledger/   源码
app/src/main/res/                              资源与主题
app/src/test、app/src/androidTest/             单元测试与仪器测试
docs/                                          需求与过程记录
```
