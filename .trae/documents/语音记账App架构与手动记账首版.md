# 语音记账 App —— 架构设计与「手动记账」首版实现计划

## 1. 目标与范围

**本次目标**：在现有空壳工程上搭好可长期扩展的架构，并交付一版"普通记账"应用——手动新增、按月浏览、搜索、编辑、删除、本地持久化全部可用。

**明确不做**（留给后续迭代）：
- 语音录音与端侧 ASR 模型（下一阶段）
- DeepSeek 解析、本地正则兜底（再下一阶段）
- 分类统计图表、预算提醒（需求文档中标为 P1）

**为以后扩展留的钩子**（现在只落数据结构与分层，不写功能）：
- 实体现已包含 `source`、`rawInput`、`parser` 三个字段：以后语音/AI 产生的记录天然可区分来源、可回溯原始文本，无需数据库迁移。
- 分层上把"输入 → 结构化记录"的位置留给后续 `parser` 模块，UI 与数据层不用改。

## 2. 当前状态分析

| 项 | 现状 |
| --- | --- |
| 工程 | AGP 9.5.2 + Gradle 9.6.0 + Kotlin 2.2.10（内置 Kotlin，未单独引入 `kotlin-android` 插件），Compose BOM 2026.02.01 |
| 入口 | [MainActivity.kt](file:///c:/Users/llx%20z/myFile/simpleCoding/jizhang/app/src/main/java/com/wbnbwclll/voiceledger/MainActivity.kt) 仅一个 `Greeting` 示例 |
| 依赖 | 只有 Compose、Activity、Lifecycle Runtime、JUnit；无 Room、无 ViewModel、无导航 |
| 构建 | `gradlew assembleDebug` 已验证成功；依赖走阿里云镜像，Gradle 发行包走腾讯云镜像；本机代理写在用户级 `~/.gradle/gradle.properties`（不入库） |
| 设备 | 已装 SDK Platform 37 与 AVD `Medium_Phone_API_37.0`；`adb devices` 当前无在线设备 |
| 约束 | 需求文档 [01-需求与验收标准.md](file:///c:/Users/llx%20z/myFile/simpleCoding/jizhang/docs/01-需求与验收标准.md) 中 AC-C / AC-L / AC-P 三组为本版验收口径 |

## 3. 架构设计

### 3.1 分层

```
Compose UI (Screen + ViewModel)
        │  StateFlow<UiState> / 事件回调
        ▼
LedgerRepository（唯一数据入口，实体 ↔ 领域模型映射）
        │
        ▼
Room（LedgerDatabase / TransactionDao）
```

- 单 Activity + Navigation Compose 两个路由：`ledger`（列表）、`editor?entryId={id}`（新建/编辑共用）。
- 依赖注入不引入 Hilt：`VoiceLedgerApp`（Application）持有 database 与 repository，ViewModel 通过 `viewModelFactory` 获取，避免 KSP/Hilt 版本风险。
- ViewModel 全部使用 `StateFlow` + `viewModelScope`，UI 无状态（有状态提升），便于以后把语音结果作为一次"预填草稿"注入编辑页。

### 3.2 目录结构（新增）

```
app/src/main/java/com/wbnbwclll/voiceledger/
├── VoiceLedgerApp.kt                  Application，持有 db/repository
├── MainActivity.kt                    改：承载 NavHost
├── model/
│   ├── Transaction.kt                 领域模型（不含 Room 注解）
│   ├── Direction.kt                   EXPENSE / INCOME
│   ├── Category.kt                    固定枚举：9 个支出 + 5 个收入，含 displayName 与所属 direction
│   └── EntrySource.kt                 MANUAL / VOICE / TEXT（本次只用 MANUAL）
├── data/
│   ├── local/TransactionEntity.kt     表结构，金额存"分"
│   ├── local/TransactionDao.kt        查询/写入
│   ├── local/LedgerDatabase.kt        @Database(version = 1)，导出 schema
│   └── LedgerRepository.kt            模型映射 + 业务查询
├── ui/
│   ├── navigation/LedgerNavHost.kt    路由定义
│   ├── ledger/LedgerScreen.kt
│   ├── ledger/LedgerViewModel.kt
│   ├── ledger/LedgerUiState.kt        月份、关键字、列表、合计、加载态
│   ├── editor/EditorScreen.kt
│   ├── editor/EditorViewModel.kt
│   ├── editor/EditorUiState.kt        表单字段、校验错误、是否编辑态
│   └── components/                    空状态、金额文本、月份选择、删除确认框
└── util/
    ├── Money.kt                       元字符串 ↔ 分（Long），格式化
    └── MonthRange.kt                  LocalDate → 当月起止毫秒（系统时区）
```

### 3.3 数据模型

`transactions` 表（对齐需求文档 2.4 节，字段一次到位）：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Long 自增主键 | |
| amountCents | Long | 金额，单位"分"，禁止浮点 |
| direction | String | `EXPENSE` / `INCOME` |
| category | String | `Category.name` |
| merchant | String? | 商户，可空 |
| note | String? | 备注，可空 |
| occurredAt | Long | 发生时间毫秒 |
| source | String | `MANUAL`（预留 VOICE/TEXT） |
| rawInput | String? | 原始输入文本（预留，本次为 null） |
| parser | String | `MANUAL`（预留 AI/LOCAL_RULE） |
| createdAt / updatedAt | Long | 毫秒 |

DAO 方法（全部 `Flow` 或 `suspend`）：
- `observeByMonth(startMillis, endMillis, keyword): Flow<List<TransactionEntity>>` —— 区间 + 可选关键字（`note`/`merchant`/`category` LIKE）
- `observeById(id): Flow<TransactionEntity?>`
- `getById(id): TransactionEntity?`（suspend，编辑页一次性加载）
- `upsert(entity): Long`、`deleteById(id)`

金额合计在 ViewModel 里对列表求和（单月数据量小，不额外写聚合 SQL）。

### 3.4 界面行为

**账单列表**
- 顶部：月份切换（‹ 2026年11月 ›）+ 当月合计 "支出 ¥xx.xx / 收入 ¥xx.xx"。
- 搜索框：按备注/商户/分类模糊匹配（对应 AC-L-03）。
- 列表项：分类名、备注（空则显示"无备注"）、日期时间、金额（支出红色、收入绿色）；按 `occurredAt` 倒序。
- 点击条目 → 编辑页；左滑 → 删除确认对话框（对应 AC-L-05）。
- 空状态："本月还没有记账"（对应 AC-L-02）。
- 右下 FAB "记一笔" → 新建页。

**编辑/新建页**
- 支出/收入切换 → 分类选项随之变化。
- 金额输入框：数字键盘，实时校验（>0、最多两位小数），非法时保存按钮禁用并提示（对应 AC-C-04）。
- 日期选择：Material3 `DatePickerDialog`；时间默认当前时刻。
- 备注、商户选填。
- 编辑态额外显示"删除"入口。
- 保存后返回列表，列表与合计立即刷新（对应 AC-C-01/L-04）。

## 4. 变更清单

### 4.1 构建配置

**`gradle/libs.versions.toml`** 新增：
```toml
room = "2.8.5"
ksp = "2.2.10-2.0.2"
lifecycle = "2.11.1"
navigationCompose = "2.10.2"
coroutines = "1.11.0"
```
以及对应 `room-runtime` / `room-ktx` / `room-compiler` / `lifecycle-viewmodel-compose` / `navigation-compose` / `kotlinx-coroutines-android` 库项和 `ksp` 插件项（`com.google.devtools.ksp`）。

**`build.gradle.kts`（根）**：`alias(libs.plugins.ksp) apply false`。

**`app/build.gradle.kts`**：
- 应用 `alias(libs.plugins.ksp)`；
- 新增上述依赖（`room-compiler` 用 `ksp(...)`）；
- 增加 schema 导出：
  ```kotlin
  ksp { arg("room.schemaLocation", "$projectDir/schemas") }
  ```
- `local.properties` 不动；不新增任何密钥字段。

**`AndroidManifest.xml`**：`android:name=".VoiceLedgerApp"`。

### 4.2 新增/修改源码

按 3.2 目录结构逐个新建；`MainActivity` 改为 `setContent { VoiceLedgerTheme { LedgerNavHost() } }`；中文文案统一进 `res/values/strings.xml`。

### 4.3 测试

新增 `app/src/test/java/.../util/MoneyTest.kt`：`"12.3" → 1230`、`"0.1"+"0.2" = 30 分`、`"abc"` 返回 null、`"0"` 拒绝。
新增 `util/MonthRangeTest.kt`：1 月/12 月边界、跨年月份区间。

这两组测试同时作为 AC-C-05（浮点误差）与月份筛选的验证证据。

## 5. 关键决策与理由

| 决策 | 理由 |
| --- | --- |
| Room + KSP 2.2.10-2.0.2 | 与 Kotlin 2.2.10 配套；阿里云镜像已确认存在该版本。Room 2.8.5 为当前稳定版 |
| 不引入 Hilt | 两个页面规模，手写 Application 级容器足够；Hilt 需额外 KSP 处理，AGP 9 下风险与收益不成正比 |
| 金额存 Long（分） | 直接规避浮点误差，符合需求文档数据模型 |
| 实体直接带语音/AI 预留字段 | 现在加零成本，以后加要写迁移；属于必要的扩展点，而非过度设计 |
| 领域模型与实体分离 | 以后 AI 解析产物只需构造 `model.Transaction`，不依赖 Room 注解；映射代码量很小 |
| 导航用 Navigation Compose | 后续加"语音记账页"只需加一个路由，不用重搭页面容器 |
| 保留搜索与月份筛选 | 需求文档 AC-L-01/03 已列入 P0，不是额外功能 |

## 6. 风险与回退

| 风险 | 应对 |
| --- | --- |
| AGP 9.5.2 内置 Kotlin 与 KSP 插件不兼容 | **先做门禁验证**：只加依赖、建一个最小 `@Entity`+`@Dao`，立即 `assembleDebug`。若 KSP 失败，回退顺序：① 显式加 `org.jetbrains.kotlin.android` 插件；② 放弃 Room，用 `SQLiteOpenHelper` 手写 DAO（数据模型不变） |
| Room 与配置缓存（`org.gradle.configuration-cache=true`）冲突 | 构建报 KSP/配置缓存错误时，先临时 `--no-configuration-cache` 验证，再决定是否关闭该开关 |
| 无在线设备 | 优先启动已存在的 AVD `Medium_Phone_API_37.0`；启动失败则改用真机，或只做编译 + 单元测试，并在报告中如实标注"未做真机验收" |
| Compose BOM 2026.02.01 与 lifecycle 2.11.1 版本组合异常 | 出现 API 不存在时降到 lifecycle 2.9.4，再重跑 |

## 7. 实施顺序

1. 加依赖与 KSP，建最小实体+DAO，`assembleDebug` 通过（门禁）。
2. 补齐 model / entity / dao / database / repository。
3. 工具类 + 单元测试（Money、MonthRange），`testDebugUnitTest` 通过。
4. 列表页 + ViewModel + 导航。 5. 编辑页 + ViewModel。
6. Manifest 注册 Application，跑通端到端。
7. 按第 8 节验证；本地提交一条 commit（推送等你确认）。

## 8. 验证步骤（可截图为证）

1. `gradlew.bat assembleDebug` → `BUILD SUCCESSFUL`。
2. `gradlew.bat testDebugUnitTest` → Money/MonthRange 用例全绿（截图作为 AC-C-05 证据）。
3. 启动模拟器并安装 APK 后逐条走：
   - 新增一笔 12.30 → 列表出现、合计 12.30；再记 0.10 + 0.20 → 合计 0.30（AC-C-05）。
   - 金额留空/填 `abc` → 保存禁用并提示（AC-C-04）。
   - 编辑金额为 99 → 列表与合计更新（AC-L-04）；删除 → 消失且合计减少（AC-L-05）。
   - 切到无数据月份 → 空状态文案（AC-L-02）；搜索"打车" → 只剩匹配项（AC-L-03）。
   - 杀进程重开 → 数据仍在（AC-P-01）。
4. 检查 `app/schemas/` 下生成的建表 JSON 已入库，作为"建表说明"证据。

## 9. 交付物

- 可编译运行的 APK 与手动记账全流程。
- 更新后的 README（功能清单、运行方式）。
- `app/schemas/1.json`（建表结构）。
- 一条阶段 commit。
