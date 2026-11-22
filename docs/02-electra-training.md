# VoiceLedger 离线记账语义模型训练方案

本文给接手训练的 Agent。目标是用选定的中文 ELECTRA 基模，把一句中文记账文本解析为可编辑的账单草稿，并交付能在 Android 上离线运行的模型。业务约定、训练数据和验收样例放在一起，便于训练端与客户端对齐。

## 1. 业务与当前实现

VoiceLedger（语音记账）是一个 Android 本地账本。用户可以手动记账，或录入一句话，例如「昨天吃饭花了二十八块五」。语音由 Vosk 转成文本，语义模型接收转写文本或用户直接输入的文本，提取账目字段后回填编辑表单。用户可以修改草稿，点保存后才写入 Room。应用还支持按月浏览、搜索、编辑和删除账单。

仓库已经实现 Kotlin、Jetpack Compose、Room 和 Vosk 语音识别。当前语音回调把转写追加到备注，`SpeechText.extractAmountCents` 取第一个阿拉伯数字回填金额。例如「11月21日打车32」可能误填 11 元。日期抽取、用途识别、类别预测和独立的一句话文本入口属于待接入能力。README 和 `docs/01-需求与验收标准.md` 中的 DeepSeek 属于早期方案，本次训练和手机运行采用端侧 ELECTRA。

第一版约定处理一条输入中的**单笔、已发生的人民币收支**。多笔交易先识别并提示拆开输入。原价、优惠、实付等数字出现在同一条消息里，仍可对应一笔交易。通过用户确认的草稿才能保存。

## 2. 学习目标与账单映射

基模固定为 `hfl/chinese-electra-180g-small-discriminator`。它是 12 层、隐藏维度 256 的中文 ELECTRA Small，公开 PyTorch 权重约 49.4 MB，模型卡标注 Apache 2.0。预训练任务是逐 token 判断文本是否被替换。微调时加载编码器权重，并为记账任务初始化新的任务头。

| 模型输出 | 标注含义 | Android 侧处理 |
| --- | --- | --- |
| `AMOUNT` | 原文里能确定的实际收款或付款金额片段，保留单位（若有） | Kotlin 解析「二十八块五」「28.5」等表达，再用 `Money` 校验，存为 `amountCents: Long`。范围 1 至 100,000,000 分 |
| `DATE_TIME` | 原文明确出现的交易时间片段，例如「昨天中午」 | 根据输入时的参考时间和设备时区解析为 `occurredAt` 毫秒时间戳。日期未提及时保留编辑器的默认时间，并让用户看得到、能修改。测试时固定参考时间和时区 |
| `MERCHANT` | 有明文依据的商家或交易平台，例如「瑞幸」 | 回填 `merchant`。个人名、朋友等交易对手不当作商户 |
| `ITEM` | 具体事项，例如「咖啡」「打车」「话费」 | 当前 `Transaction` 没有独立事项列。可用来回填空白 `note`，已编辑的备注保留 |
| 句子类别 | 现有 `Category` 的枚举值，或需拦截的输入类别 | 对可记输入，使用 `Category.direction` 得到收支方向。一并回填 `direction` 和 `category`，保持两者一致 |

分类头使用以下固定标签，和 app 的 `Category.kt` 对齐：

- 支出：`FOOD` 餐饮、`TRANSPORT` 交通、`SHOPPING` 购物、`HOUSING` 居住、`ENTERTAINMENT` 娱乐、`MEDICAL` 医疗、`EDUCATION` 教育、`SOCIAL` 人情、`OTHER_EXPENSE` 其他。
- 收入：`SALARY` 工资、`PART_TIME` 兼职、`RED_PACKET` 红包、`INVESTMENT` 理财、`OTHER_INCOME` 其他。
- 控制标签：`NO_TRANSACTION` 表示否定、计划、询问、闲聊或明确的本人账户互转。`MULTIPLE_TRANSACTIONS` 表示明确描述两笔及以上已发生交易。`NEEDS_REVIEW` 表示金额不存在或不能唯一确定、方向歧义等情况。控制标签用于解析流程，保存时仍只接受现有账单枚举。

第一版沿用现金流口径。明确收到退款或还款，可记为 `OTHER_INCOME`。明确自己还了钱，可记为 `OTHER_EXPENSE`。原账冲销和双账户转账留待另一个业务版本定义。金额模糊或多笔时展示原文，提示补充或拆开，由用户继续手动记账。

## 3. 数据和标注

使用 UTF-8 JSONL，每行一条。字段至少有 `id`、`text`、`inputSource`、`label`、`spans`、`groupId`。`inputSource` 取 `typed` 或 `asr`。`groupId` 用于把同一原句、模板、改写和对应 ASR 变体放在同一数据分区。标注坐标以原文的 **Unicode 码点**为单位，采用左闭右开区间 `[start,end)`。Android 如果用 UTF-16 索引切片，要显式转换。分词、归一化和偏移映射应在 Python 与 Android 的测试样例中对齐。

```json
{"id":"ledger-0001","text":"昨天打车花了二十八块五","inputSource":"typed","label":"TRANSPORT","spans":[{"tag":"DATE_TIME","start":0,"end":2},{"tag":"ITEM","start":2,"end":4},{"tag":"AMOUNT","start":6,"end":11}],"groupId":"taxi-001"}
```

上例预期是支出、交通、2850 分、日期片段「昨天」、事项「打车」，商户为空。模型层保留原文片段，归一化后的金额和日期另列在端到端验收样本中。

| 输入 | 期望标注或处理 |
| --- | --- |
| 昨天中午在瑞幸买咖啡花了18块 | `FOOD`，时间「昨天中午」，商户「瑞幸」，事项「咖啡」，金额「18块」 |
| 11月21日打车32 | `TRANSPORT`，时间「11月21日」，金额「32」 |
| 咖啡原价20，优惠5，实付15 | `FOOD`，金额只标「15」 |
| 今天坐地铁3块5 | `TRANSPORT`，金额整个片段「3块5」对应 350 分 |
| 工资到账5000 | `SALARY`，金额「5000」，日期缺失 |
| 朋友还了我200 | `OTHER_INCOME`，金额「200」，商户缺失 |
| 打算明天花100买书 / 最后没买那杯18元的咖啡 | `NO_TRANSACTION` |
| 买东西花了几十块 / 给小李转了钱 | `NEEDS_REVIEW` |
| 奶茶12，地铁3块5 / 上午咖啡18下午打车32 | `MULTIPLE_TRANSACTIONS` |

标注补充约定：

- 一个有效单笔样本只有一个 `AMOUNT`。日期、件数、门牌、原价、优惠额等保留在原文里，作为干扰输入。可选字段缺失就留空，出现明确商家时才标 `MERCHANT`。
- 标签之间避免字符重叠。「瑞幸咖啡」如果作为品牌全称出现，就整体标商家。商品词单独出现时再标 `ITEM`。
- `NO_TRANSACTION`、`MULTIPLE_TRANSACTIONS` 和 `NEEDS_REVIEW` 样本先只监督句子分类，NER 训练掩蔽这些样本。缺少标注和明确没有实体是不同状态。
- 先人工整理 200～300 条用于对齐口径和跑通训练，再扩到约 3,000～5,000 条。以真实口语和录音转写为核心，用模板和改写补齐少见类别。覆盖口语数字、无标点 ASR、错别字、空格、否定、退款、折扣、时间与金额并存、重复报数和多笔。真实样本入集前去掉可识别的姓名、地址、订单号等信息。
- 按 `groupId` 分组切分，初始可按 80/10/10。若有说话人标识，端到端语音测试优先使用训练未见的说话人。建议整理至少 300 条人工复核的独立测试样本，分开报文字和 ASR 结果。测试集固定，参数和置信度阈值都在验证集选。

## 4. 训练方法

1. **先测现有基线。** 用固定测试集记录 `SpeechText` 的金额正确率，以及它在含日期、优惠、否定句上的错误。它目前只有金额规则，所以完整草稿评估同时列出当前规则能覆盖的字段。
2. **先独立微调两个任务。** 用 Python、PyTorch、Hugging Face Transformers。实体抽取用 `AutoModelForTokenClassification`，对 `AMOUNT / DATE_TIME / MERCHANT / ITEM` 使用 BIO 标签。句子分类用 `AutoModelForSequenceClassification`，输出第 2 节的类别。两个任务从同一个预训练检查点初始化，记录基模 revision、标签顺序、随机种子、依赖版本和数据版本。
3. **分词对齐。** 固定基模的 WordPiece tokenizer。借助 fast tokenizer 的字符偏移把跨度转换为 token 标签，特殊 token 和 padding 的 loss 设为 `-100`。首个实体 token 标 `B-*`，后续 token 标 `I-*`。保留能复现 token ID、原文区间和解码结果的样例，尤其是中文小数、连续数字、全角字符和 ASR 空格。
4. **起跑参数。** 用单卡 RTX 5060 做微调实验，可从最大长度 64、batch size 16 或 32、AdamW、学习率 `3e-5`、训练 5～10 个 epoch 起步。验证集停滞时早停。先统计长度分布，关键片段被截断时将本条交给人工复核，或根据测试结果把上限调到 128。
5. **按验证结果决定部署结构。** 两套独立模型便于查错，但每套都含一份编码器。若体积超出目标，再用一个 ELECTRA 编码器加 token 分类头和句子分类头，联合训练并重新评估。控制类样本的 NER loss 保持掩蔽。确认两个指标都没有明显回退后，再交付单图 ONNX。

## 5. 评估与验收

- **训练诊断**：实体严格跨度 F1，至少单列金额。类别报 macro-F1 和混淆矩阵，特别关注 `NO_TRANSACTION`、`MULTIPLE_TRANSACTIONS` 和 `NEEDS_REVIEW`。
- **主要业务指标**：整笔草稿正确率。对可记单笔，只有金额分值、方向、分类及标注要求的日期、商家、事项全对才计成功，凭空回填可选字段同样计错。对控制类，单独报告拦截正确率和误生成账单草稿率。附样本数和错误案例，手输、ASR、口语金额、日期干扰分开报。
- **模型对比**：同一份测试集依次评估规则、PyTorch、FP32 ONNX 和量化 ONNX。验收优先看金额与方向，量化版如增加金额错误或把计划句放行，保留 FP32 并记录体积差异。初版不把尚未实测的准确率写成承诺。
- **真机验收**：飞行模式下，语音转写和文本解析均可完成，草稿可编辑，保存后重启能查到。至少测冷启动首条、热运行延迟、模型文件大小、安装体积、推理时峰值内存。报告设备、系统版本和测量条件。

## 6. ONNX 交接和 Android 接入

先分别导出实体识别（`token-classification`）和句子分类（`text-classification`）的 FP32 ONNX。核对训练模型与桌面 ONNX 的输出，再做 Android CPU 推理比对。之后可尝试动态 INT8，实测图文件大小及任务指标。若采用共享编码器，导出同时返回两路 logits 的单图，保留相同输入和输出校验。

| 交付物 | 内容 |
| --- | --- |
| 模型与标签 | FP32 ONNX，及通过验收的 INT8 版本（如有）。随附 tokenizer 文件、`id2label` / `label2id`、opset、每个输入输出的名字、形状与 dtype、最大序列长度、模型来源和文件校验值 |
| 一致性样例 | 典型文本的 token ID、attention mask、token 偏移、解码跨度和期望草稿。Android 按实际模型签名准备输入，并用这些样例对照 Python |
| 训练材料 | 可复现的环境及配置、训练和导出方法、数据格式与固定的数据切分清单、验证集阈值选取方法、测试结果与失败案例 |

移动端接入点是 `ui/editor/EditorScreen.kt` 当前接收转写的地方，以及负责草稿和保存的 `EditorViewModel`。语音和文字入口应走同一套解析。推理在后台运行，完成后只回填仍对应本次输入的草稿，保留用户在此期间的手动修改。缺金额或结果冲突时保留手动输入，明确提示。弱规则兜底只回填明确且无冲突的数字，防止把日期或优惠额填成交易金额。

接入时使用现有 `EntrySource.VOICE` / `TEXT`、`rawInput` 和 `ParseSource.AI` / `LOCAL_RULE` 标记来源。当前新建路径使用手动默认值，接入时由编辑器保存逻辑传入真实来源。账单的数据库写入继续通过 `LedgerRepository`，由用户点击保存触发。

## 参考资料

- [HFL 中文 ELECTRA Small 模型卡](https://huggingface.co/hfl/chinese-electra-180g-small-discriminator) · [权重和分词文件](https://huggingface.co/hfl/chinese-electra-180g-small-discriminator/tree/main)
- [Hugging Face ELECTRA 文档](https://huggingface.co/docs/transformers/model_doc/electra) · [Optimum ONNX 导出](https://huggingface.co/docs/optimum-onnx/onnx/usage_guides/export_a_model)
- [ONNX Runtime 移动部署](https://onnxruntime.ai/docs/tutorials/mobile/) · [ONNX Runtime 量化](https://onnxruntime.ai/docs/performance/model-optimizations/quantization.html)
