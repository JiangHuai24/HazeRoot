# HazeRoot 工程化重构方案

> 版本：v1.0　|　状态：正式交付　|　适用对象：HazeRoot 技术负责人 / 架构师 / AI 协作团队
>
> 文档性质：本文件是 HazeRoot 从「功能堆叠 / 随心所欲开发」重构为「企业级工程化开发」的**地基文档**，后续所有 PRD、架构设计、任务拆解、Code Review、CI/CD 均以本文件为基准对齐。

---

## 目录

- [0. 执行摘要](#0-执行摘要)
- [第一部分：软件工程流程调研](#第一部分软件工程流程调研)
  - [1.1 需求分析与 PRD 规范](#11-需求分析与-prd-规范)
  - [1.2 软件架构设计方法](#12-软件架构设计方法)
  - [1.3 Android 架构演进与渐进式重构策略](#13-android-架构演进与渐进式重构策略)
  - [1.4 终端 / PTY 输出渲染最佳实践](#14-终端--pty-输出渲染最佳实践)
  - [1.5 文件管理器交互设计规范](#15-文件管理器交互设计规范)
  - [1.6 代码规范 / Code Review / 分支 / 测试 / CI-CD / 文档](#16-代码规范--code-review--分支--测试--cicd--文档)
  - [1.7 团队分工](#17-团队分工)
- [第二部分：HazeRoot 现状诊断与业务框架梳理](#第二部分hazeroot-现状诊断与业务框架梳理)
  - [2.1 现状诊断：God Object 反模式 → 八大痛点映射](#21-现状诊断god-object-反模式--八大痛点映射)
  - [2.2 目标业务框架](#22-目标业务框架)
  - [2.3 模块划分建议](#23-模块划分建议)
  - [2.4 渐进式重构路线图](#24-渐进式重构路线图)
- [第三部分：开发流程规范（AI 团队化协作）](#第三部分开发流程规范ai-团队化协作)
  - [3.1 团队角色划分与产出物](#31-团队角色划分与产出物)
  - [3.2 科学完整的开发流程 SOP](#32-科学完整的开发流程-sop)
  - [3.3 约束机制（硬性规则）](#33-约束机制硬性规则)
  - [3.4 任务看板 / 里程碑模板](#34-任务看板--里程碑模板)
- [附录 A：术语表](#附录-a术语表)
- [附录 B：参考来源](#附录-b参考来源)

---

## 0. 执行摘要

HazeRoot 当前处于「功能已堆叠、结构已失序」的典型遗留代码状态：一个全局单例 `Engine`（~1250 行）承载了 root 探测、su 执行、队列调度、输出解码、ANSI 解析、提示识别、自动应答、录制、看门狗等 9 项完全不同的职责；`FilesFragment`（~3350 行）同时承担「双窗 UI + 文件操作 + APK 安装 + 分享」的职责；`QueueFragment`（~1675 行）承担「队列面板 + 终端渲染 + 输入 + 门控」。这是教科书级别的 **God Object / Large Class / Divergent Change** 反模式，直接导致了作者描述的 8 条痛点。

本方案的核心理念只有一条：**不推倒重来，用「绞杀者模式（Strangler Fig）+ 按抽象分支（Branch by Abstraction）+ 特性开关（Feature Toggle）」小步重构，先把「接缝（Seam）」插进去，再逐块把行为迁移到新结构。** 每一次改动都保持 App 可编译、可运行、可回滚。

三条主线：

1. **业务线**：先建立目标业务框架（领域对象 / 流程 / 状态机 / 页面联动），让「应该是什么」先于「怎么改」。
2. **结构线**：把 3 个 God Object 拆成 `ui / domain / data / core` 四层 + 可测试的模块包结构，依赖方向单向向下。
3. **工程线**：建立「PRD → 架构 → 任务 → 开发 → Review → 测试 → 真机回归 → 文档 → 发布」的 SOP，用硬性规则锁死「走一步看一步」的回退路径。

预期收益：修改成本下降（单点职责、单向依赖）、Bug 定位时间缩短（可测试性）、MT 用户迁移成本下降（交互范式对齐）、AI 协作可并行（模块边界清晰、任务可拆解）。

---

# 第一部分：软件工程流程调研

> 说明：本部分综合了权威工程实践来源，引用 URL 见 [附录 B](#附录-b参考来源)。web_search 因环境未配置检索 API key 而不可用，改以 web_fetch 直接抓取一手资料源，并结合业内通行规范补充。

## 1.1 需求分析与 PRD 规范

### 1.1.1 需求分层

企业级需求分析通常分三层，避免「一上来就写代码」：

| 层级 | 产出物 | 回答的问题 |
|------|--------|-----------|
| 业务需求 | 商业目标 / 用户价值声明 | 为什么做？给谁做？ |
| 用户需求 | 用户故事（User Story）、场景（Scenario） | 用户要完成什么任务？ |
| 系统需求 | 功能规格、非功能规格（NFR） | 系统具体做什么？性能/兼容/权限边界？ |

### 1.1.2 用户故事规范

标准格式（Connextra 模板）：

```
作为 <角色>，我想要 <能力>，以便 <价值/收益>。
```

对 HazeRoot 的示例：

```
作为 进阶 root 用户，我想要 把一个 shell 脚本加入执行队列，
以便 按顺序批量执行多个维护脚本而无需反复手动启动。

作为 从 MT 管理器迁移来的用户，我想要 用与 MT 一致的长按进入多选的手势，
以便 不重新学习文件操作肌肉记忆。

作为 脚本作者，我想要 在执行中为脚本的交互式提问配置自动应答规则，
以便 无人值守地跑完带确认步骤的脚本。
```

每条用户故事必须附带 **验收标准（Acceptance Criteria, AC）**，用「Given / When / Then」或清单式写出可验证条件：

```
AC1（Given/When/Then）：
  Given 队列中有 3 个脚本，其中第 1 个处于运行中
  When  用户点击「添加脚本」
  Then  新脚本被追加到队尾，且不中断当前执行，UI 立即刷新

AC2（可量化）：
  队列项渲染在 60fps 设备上连续追加 1000 行输出时，
  UI 主线程单帧耗时 ≤ 16ms（不出现明显掉帧/卡顿）。
```

### 1.1.3 完成定义（Definition of Done, DoD）

每个任务的 DoD 必须包含以下 8 项，缺一不可：

1. 需求已映射到 PRD 条目，验收标准可执行。
2. 代码通过编译（`./gradlew assembleDebug` 0 失败）。
3. 新增/修改逻辑有对应单元测试，核心路径覆盖率达标（见 1.6.3）。
4. Code Review 通过（至少 1 个非作者审阅人，AI 协作时为「架构师角色」复核）。
5. 静态检查通过（ktlint/checkstyle/Android Lint 0 阻断级 issue）。
6. 关键路径有真机回归记录（见 1.6.3）。
7. 文档同步更新（模块设计文档 / CHANGELOG / 状态机图）。
8. 分支已合入主干（或 PR 已合并），特性开关（若有）已记录退役计划。

> 关键点：**「跑通了」≠「Done」**。没有验收标准、没有测试、没有 Review 的改动一律视为未完成，这是阻断「随心所欲」的第一道闸门。

---

## 1.2 软件架构设计方法

### 1.2.1 分层架构（Layered Architecture）

Martin Fowler 在《Patterns of Enterprise Application Architecture》（EAA）中系统总结了分层结构（Presentation / Domain / Data）。移动端同样适用，标准三层：

- **表现层（Presentation / UI）**：Activity / Fragment / Adapter / 自定义 View，只负责渲染与用户事件转发，不含业务规则。
- **领域层（Domain）**：领域对象 + 用例（Use Case），纯 Java、不依赖 Android SDK，可独立单元测试。
- **数据层（Data）**：文件系统、SharedPreferences/Room、脚本仓库、录制仓库等，向领域层提供数据接口。

### 1.2.2 领域模型（Domain Model）

领域模型 =「对象 + 行为」的集合，把散落在 God Object 里的 if/else 收拢为有语义的对象。EAA 中的 **Domain Model 模式**、**Service Layer 模式**、**Repository 模式**是 HazeRoot 直接可用的三个模式：

- **Domain Model**：`Script`、`QueueItem`、`Preset`、`ExecutionSession` 等携带行为。
- **Service Layer**：`ExecutionCoordinator`（协调执行流程）、`FileOperationService`（协调文件操作）作为领域层对外边界。
- **Repository**：`ScriptRepository`、`PresetRepository`、`RecordingRepository`，用「集合式接口」屏蔽底层存储细节。

### 1.2.3 状态机（State Machine）

对「执行」这类**生命周期清晰、状态有限、迁移明确**的领域，必须显式建模为有限状态机（FSM），禁止用一堆 boolean 标志（`isRunning`/`isWaiting`/`isStopped`）互相打架。这是 HazeRoot「执行中跳转乱」「门控乱」的根本解法之一。详见 [2.2.4](#224-执行状态机)。

### 1.2.4 数据流图（Data Flow）

对「终端输出」这条高频、实时的数据流，建议用**单向数据流（UDF）**建模：

```
su 进程 stdout/stderr
      │
      ▼
ChunkReader（读字节块）
      │
      ▼
CharsetDetector（UTF-8 严格优先 + GBK 混合）
      │
      ▼
AnsiParser（ANSI 状态机 → 文本/样式）
      │
      ▼
PromptDetector（提示识别）
      │
      ▼
TerminalBuffer（环形缓冲，只增不删，主线程读）
      │
      ▼
UI 渲染（增量 diff → Adapter.notifyItemRangeInserted）
```

**关键约束**：数据只从「进程 → 缓冲 → UI」一个方向流动；UI 只读缓冲，不反向修改解码/解析状态。任何回写都会破坏可测试性并制造竞态。

### 1.2.5 模块化原则

- **单一职责（SRP）**：一个类只为一个「角色」而改变。
- **依赖倒置（DIP）**：高层（领域）不依赖低层（su 实现），依赖抽象接口 `SuGateway`。
- **依赖方向单向**：`ui → domain → data`，`core` 独立，禁止反向依赖。
- **接缝（Seam）**：所有「外部系统 / 平台能力 / 硬件相关」的地方都要留接口，例如 `ProcessExecutor`、`SuGateway`、`FileSystemGateway`、`PackageManagerGateway`。

---

## 1.3 Android 架构演进与渐进式重构策略

### 1.3.1 架构演进脉络

| 架构 | 特点 | 对 HazeRoot 的适用性 |
|------|------|---------------------|
| MVC | Controller 兼管业务，Activity/Fragment 臃肿 | 现状近似，是问题根源之一 |
| MVP | 引入 Presenter 解耦 View 与 Model | 可用，但接口易膨胀 |
| MVVM | ViewModel + LiveData/Flow，UI 状态可观察 | **推荐目标**，配合 ViewModel 管理 UI 状态 |
| MVI | 单向数据流 + 不可变 State + Intent | 适合执行引擎这类复杂交互，可作为 Queue 页目标 |
| Clean Architecture | 分层 + 依赖规则（外层依赖内层） | 作为**包结构与依赖方向**的指导，不必照搬 UseCase 仪式 |

**官方权威参考**：Google 官方 Android 架构指南（developer.android.com/topic/architecture）推荐「UI 层 + 领域层（可选）+ 数据层」的分层与「单向数据流（UDF）」；Google 的开源范例 **nowinandroid**（github.com/android/nowinandroid）演示了 MVVM + UDF + 模块化的落地形态。

### 1.3.2 对「已有 AppCompat + 全局单例 Engine + 无 ViewModel」的渐进式策略

作者明确要求**不推倒重来**。对已有 1.2 万行 Java、AppCompat、单 Activity 多 Fragment 的项目，成熟做法是「绞杀者模式」+「按抽象分支」，核心工具是 **Seam（接缝）** 与 **Feature Toggle（特性开关）**：

1. **先插接缝（Seam）**：Michael Feathers 在《Working Effectively with Legacy Code》中的定义——「接缝是无需就地编辑即可改变行为的地方」。对 `Engine` 而言，第一步不是拆类，而是先给 `su 执行`、`输出读取`、`提示识别`、`应答规则` 各引入一个接口，让 UI 与业务通过接口调用。这是**可测试性的前提**。

2. **按抽象分支（Branch by Abstraction）**：引入 `ExecutionEngine` 接口，老实现 `LegacyEngine` 继续工作，新实现 `SessionEngine` 逐步替换，两者共存，用开关切换。Fowler 的描述要点——「用抽象层允许多个实现并存，在迁移期间系统始终可构建、可运行」。

3. **绞杀者模式（Strangler Fig）**：不在原 `Engine.java` 上继续堆代码，而是新建 `core/engine/` 模块承载新逻辑，老 `Engine` 逐步退化为转发门面（Facade），最终删除。Fowler 在 2024 年更新的「Strangler Fig」与「Patterns of Legacy Displacement」强调：**先明确期望结果 → 拆小 → 逐件交付 → 改变组织方式**，且「技术最多只占遗留问题的一半，工作方式同样重要」。

4. **Feature Toggle**：重构与新功能用 Release Toggle 隔离，保持主干始终可发布；开关要**短命**、有退役计划，避免开关堆积（Fowler / Pete Hodgson《Feature Toggles》）。

5. **小步可回滚**：每个里程碑产出「可编译 + 行为等价 + 可回滚」的中间态；用「Event Interception（事件拦截）」在流程中插入中间件而不改原逻辑。

### 1.3.3 渐进式重构的 5 条铁律

1. 一次只移动一种职责（Move Method / Extract Class），不跨职责混改。
2. 先补「特性测试」锁行为，再动结构（先测试后重构，防回归）。
3. 每步之后跑全量测试 + 一次真机冒烟。
4. 新旧实现并存期间，Feature Toggle 默认指向老实现，验证通过后切换。
5. 严禁在 God Object 里「顺手加新功能」——新功能一律进新模块。

---

## 1.4 终端 / PTY 输出渲染最佳实践

HazeRoot 痛点 2「执行 sh 脚本时显示不全」直接对应终端渲染。业界（终端模拟器、PTY 客户端）的成熟做法：

### 1.4.1 输出读取与解码分离

- **块读取**：用独立 IO 线程按字节块（如 4KB）读取 stdout/stderr，绝不逐字符读、绝不在主线程读。
- **多字节安全**：UTF-8 多字节字符可能被块边界截断，必须保留「不完整序列」跨块拼接，避免乱码/丢字（这是「显示不全」最常见原因之一）。
- **两级解码**：先严格 UTF-8（`CharsetDecoder` + `CodingErrorAction.REPORT`），失败则回退 GBK 混合解码。解码器是**有状态**的，需按流顺序单线程处理，禁止并发解码同一流。

### 1.4.2 ANSI 状态机

- ANSI（`\x1b[...m`、光标控制、清屏等）必须用**独立状态机**逐字节消费，与文本解析解耦。
- 状态机只输出「纯文本 + 样式标记」，不直接操作 UI。

### 1.4.3 增量渲染（解决卡顿与显示不全）

- **环形缓冲（Ring Buffer / Gap Buffer）**：终端内容只增，用环形缓冲或 Gap Buffer 维护「屏幕可见文本」，避免每次追加都整体重建字符串。
- **增量 diff**：解析出「新行 / 追加内容」后，用 `RecyclerView` 的 `notifyItemRangeInserted/Changed` 做**局部更新**，禁止 `notifyDataSetChanged()` 全量刷新。
- **合并高频更新**：su 输出可能瞬时海量，用「帧合并（coalesce）」——一个主线程帧内只 apply 一次 UI 更新（如 Choreographer / Handler 合并），缓冲期内累积的增量合并为一笔。
- **限制回显总量**：为缓冲设上限（如 20 万字符/5000 行），超出丢弃最旧段并提示「已截断」，防止 OOM。

### 1.4.4 提示识别与自动应答

- 提示识别用「行尾模式匹配 + 超时判据」，不靠轮询整个缓冲；识别点应在「输出解析」之后、渲染之前。
- 自动应答是**策略链**：`AnswerRule` 有序匹配（正则/包含/脚本预置），命中即写入 su stdin；写 stdin 与读 stdout 走同一会话，避免跨线程竞态。

### 1.4.5 看门狗与进程生命周期

- 看门狗只负责「超时/退出检测」，不兼职执行调度；通过 `Process.exitValue()` / 心跳定时器判断，异常退出要产出明确的 `ExecutionState.FAILED`。

---

## 1.5 文件管理器交互设计规范

HazeRoot 痛点 3/4/7（长文件名、手势冲突、MT 迁移）对应文件管理交互。参考 Material Design 3 手势规范与主流文件管理器（MT 管理器 / Mixplorer / 系统 Files）范式：

### 1.5.1 手势统一（解决「一塌糊涂」）

必须定义**唯一手势映射表**，消除多选/左滑/长按/点按的语义冲突：

| 手势 | 默认行为 | 冲突消解 |
|------|---------|---------|
| 单击（短按） | 打开/预览（进入目录、打开文件） | 多选模式下单击=切换选中，不打开 |
| 长按 | 进入多选模式并选中该项 | 已是多选模式时长按=追加选中 |
| 左滑（项上） | 呼出快速操作（分享/删除/加入队列等） | 多选模式下禁用左滑，避免误触 |
| 右上角/工具栏「多选」按钮 | 进入多选模式（空选） | 作为长按之外的显式入口 |
| 返回键/工具栏「完成」 | 退出多选模式 | 保留选择 or 清空，需明确并一致 |
| 顶部面包屑/路径点击 | 跳转目录 | 独立于文件项手势 |

**设计原则**：进入多选后，单击与长按都只作用于「选择」，只有「退出多选」后才恢复单击打开；左滑只在非多选态启用。用**状态**（SelectionMode）显式隔离，而不是用时间戳/标志位猜意图。

### 1.5.2 长文件名显示（解决痛点 3）

- 列表/网格统一用 `ellipsize="middle"`（中间省略）而非 `end`，保留文件扩展名与首段，这是文件管理器的行业共识（MT/Mixplorer 均如此）。
- 提供「文件名完整展示」途径：长按弹出菜单 →「重命名」时全选展示；或详情页/悬浮提示（tooltip）展示全名。
- 网格布局下用固定行数（maxLines=2）中间省略，避免参差。

### 1.5.3 双窗与 MT 范式对齐（解决痛点 7）

- **双窗**：左右/上下两窗各自维护独立 `Breadcrumb` 路径状态，互不干扰；提供「复制/移动到对侧窗格」显式操作。
- **迁移对齐清单**（面向 MT 用户）：
  1. 长按=多选、单击=打开，语义与 MT 一致。
  2. 左滑呼出快捷操作（或可配置）。
  3. 路径栏可点击逐级跳转 + 可手动输入路径。
  4. 底部工具栏在选中时显示「复制/剪切/重命名/删除/属性/加入队列」。
  5. 文件图标/颜色区分目录、可执行脚本、APK、文本等类型。
  6. 提供「排序（名称/时间/大小/类型）+ 显示隐藏文件 + 显示文件扩展名」开关。
- 把上述条目固化为「交互基线」，新增手势必须与基线冲突检测。

---

## 1.6 代码规范 / Code Review / 分支 / 测试 / CI-CD / 文档

### 1.6.1 代码规范

- Java 17 项目用 **Checkstyle + Android Lint + Spotless（Google Java Format / AOSP 风格）**；包名/类名/方法命名统一。
- **类行数硬约束**（防 God Object 复活）：单类 ≤ 300 行，单方法 ≤ 40 行；超限必须拆（这是把「不拆类」从口头变成可检查的门禁）。
- **分层导入约束**：`domain` 包禁止 import `android.*`；`core` 禁止 import `domain/ui/data`（用 ArchUnit 校验，见 1.6.3）。
- 魔法数字/硬编码路径/编码名一律提取为常量；异常必须带上下文信息。

### 1.6.2 Code Review 与分支管理

- **Code Review 检查单**：单一职责、依赖方向、命名、测试覆盖、可读性、对既有行为的影响、Feature Toggle 是否留了退役计划、文档是否同步。
- **分支模型**：团队规模小 → 采用 **Trunk-Based Development（主干开发）**：所有人在 `main` 上频繁提交，短命 feature 分支（≤ 1 天）合入；大改动用 **Branch by Abstraction + Feature Toggle** 而非长命分支。依据 trunkbaseddevelopment.com 的结论——避免 GitFlow 的多长命分支带来的 merge hell。
- **AI 协作落地**：AI 产出的每个 PR 都走同一 Review 门禁，禁止「AI 直接 push main」。

### 1.6.3 测试策略（测试金字塔）

依据 Fowler《Test Pyramid》：单元测试多、UI 端到端测试少，中间层做「皮下测试（Subcutaneous Test）」。

| 层级 | 工具 | 覆盖目标 | 占比目标 |
|------|------|---------|---------|
| 单元测试 | JUnit4/5 + Mockito + Robolectric（非必须） | 解码器、ANSI 状态机、提示识别、应答规则、状态机迁移 | ~60% |
| 皮下/集成测试 | JUnit + 本地 fake（不启动 Activity，直接测领域/数据接口） | ExecutionCoordinator、QueueRepository、PresetRepository | ~30% |
| 端到端/真机回归 | Espresso + 真机/模拟器 + `adb shell` | 关键用户路径：加队列→执行→应答→录制→重放 | ~10% |
| 架构约束 | ArchUnit | 分层依赖方向、类行数、God Object 检测 | 门禁，非占比 |

- **根设备特殊处理**：su 执行走 `SuGateway` 接口，测试注入 `FakeSuGateway`（返回预设字节流），不必真 root。
- **真机回归清单**（每个里程碑必过）：root 探测、su 执行输出完整性、UTF-8/GBK 混合解码、ANSI 渲染、长文件名、多选手势、执行中跳转、录制重放。

### 1.6.4 CI/CD

- **CI（GitHub Actions / 本地 runner）**：push 触发 `./gradlew lint test assembleDebug`；PR 触发 `+ ArchUnit + 覆盖率报告`。
- **门禁**：任何 `lint` 阻断级 issue / 测试失败 / 覆盖率下降超阈值 → 禁止合并。
- **CD**：构建 `debug`/`release` APK 产物；release 走签名 + 版本号 + CHANGELOG；产物附真机回归记录。
- **特性开关**：用构建配置（BuildConfig 字段）实现 Release Toggle，`main` 始终可出包。

### 1.6.5 文档规范

- 每个模块一份 `DESIGN.md`（职责、依赖、关键算法、状态机）；根目录 `ARCHITECTURE.md` 维护全局图。
- 变更走 `CHANGELOG.md`（Keep a Changelog 格式）；PRD/设计文档在 `docs/` 版本化。

---

## 1.7 团队分工

| 角色 | 职责 | 关键产出物 |
|------|------|-----------|
| 产品经理（PM） | 需求收集、优先级、验收标准定义 | PRD、用户故事、验收标准、需求优先级表 |
| 架构师（Architect） | 分层/模块/接缝设计、依赖方向把关 | ARCHITECTURE.md、模块划分、状态机、技术选型 |
| UI/交互设计师 | 手势映射、长文件名、双窗、MT 对齐规范 | 交互基线、UI 规格、手势映射表、设计走查 |
| Android 工程师 | 实现、单测、重构落地 | 代码、单元/集成测试、PR |
| QA | 测试用例、真机回归、缺陷管理 | 测试计划、回归清单、Bug 报告、验收报告 |
| 发布工程师（DevOps/Release） | CI/CD、签名、构建产物、发布 | 流水线配置、发布说明、产物归档 |

**协作要点（Conway 定律）**：Martin Fowler 强调——系统结构会复制团队沟通结构。因此 HazeRoot 的模块边界必须与「AI 角色分工」对齐：架构师负责的模块边界 = 开发工程师/QA 的协作边界，避免一个 God Object 被多人同时改造成新冲突。这直接服务于作者「把 AI 分作完整业务团队」的诉求（详见第三部分）。

---

# 第二部分：HazeRoot 现状诊断与业务框架梳理

## 2.1 现状诊断：God Object 反模式 → 八大痛点映射

> 依据：refactoring.guru 的 **Large Class（大类）** 代码坏味道——「一个类承担太多『功能帽子』时应该拆分」；以及 Fowler《Patterns of Legacy Displacement》——遗留系统的腐化源于「补丁叠补丁」。

| # | 作者痛点 | 根因（结构层面） | 反模式 | 对应解法（章节） |
|---|---------|----------------|--------|-----------------|
| 1 | 没有业务/逻辑框架 | 无领域模型、无分层、无状态机，逻辑散落 | Big Ball of Mud | 2.2 目标业务框架 |
| 2 | sh 执行显示不全 | 解码/ANSI/渲染/缓冲耦合在 Engine + QueueFragment，块边界丢字、全量刷新 | Divergent Change / 竞态 | 1.4 + 2.3 模块拆分 |
| 3 | 长文件名显示差 | 交互未规范化，FilesFragment 承担渲染细节 | UI 职责过载 | 1.5.2 |
| 4 | 手势交互一塌糊涂 | 多选/左滑/长按/点按无统一状态机，靠标志位猜意图 | Primitive Obsession（boolean 标志） | 1.5.1 + 2.2.4 |
| 5 | 添加队列混乱 | 队列「门控/调度/UI」职责未分离，无明确入队契约 | God Object | 2.2.2 + 2.3 |
| 6 | 执行中无跳转/联动差 | 页面间无导航总线，状态分散，无可订阅的会话状态 | 无单向数据流 | 2.2.3 + 2.2.4 |
| 7 | MT 迁移难 | 交互范式未对齐，手势语义不一致 | 交互未规范化 | 1.5.3 |
| 8 | 随心所欲编写 | 无 PRD→设计→实现流程，无门禁 | 工程流程缺失 | 第三部分 |

### 2.1.1 三大 God Object 的「帽子」清单

- **Engine.java（~1250 行）**：① root 探测 ② su 提权执行 ③ 队列调度 ④ 输出解码 ⑤ ANSI 状态机 ⑥ 提示识别 ⑦ 自动应答 ⑧ 录制 ⑨ 看门狗 —— **9 顶帽子**。
- **FilesFragment.java（~3350 行）**：① 双窗 UI ② 文件列表渲染 ③ 文件操作（复制/移动/删） ④ APK 安装 ⑤ 分享 ⑥ 多选手势 —— **6 顶帽子**。
- **QueueFragment.java（~1675 行）**：① 队列面板 ② 终端增量渲染 ③ 手动输入 ④ 执行门控 —— **4 顶帽子**。

拆分目标：**每个类只留 1 顶帽子**。

---

## 2.2 目标业务框架

> 这是「应该是什么」，不是现状。它是所有重构与后续开发的**统一心智模型**。

### 2.2.1 核心领域对象

| 领域对象 | 职责 | 关键属性 / 行为 |
|---------|------|----------------|
| `Script` | 待执行的脚本单元 | 路径/名称、参数、工作目录、执行身份（root/普通）、来源（文件/链接导入） |
| `QueueItem` | 队列中的一个条目 | 唯一 id、关联 Script、顺序、状态（见状态机）、应答规则引用 |
| `Preset` | 方案库条目（可重放的「脚本+参数+应答规则」组合） | 名称、脚本列表、参数、`AnswerRule` 列表、创建/更新时间 |
| `AnswerRule` | 一条自动应答规则 | 匹配模式（正则/包含）、匹配顺序、应答文本、是否可重复 |
| `ExecutionSession` | 一次执行会话（su 进程生命周期） | 会话 id、进程句柄、输入/输出流、开始/结束时间、当前状态 |
| `TerminalBuffer` | 终端可见内容的环形缓冲 | 容量上限、追加/截断、增量 diff 导出 |
| `FileEntry` | 文件系统条目（文件/目录） | 路径、名称、类型、大小、修改时间、是否可执行 |
| `ExecutionState` | 执行状态枚举（状态机节点） | 见 2.2.4 |

### 2.2.2 核心业务流程

主流程（脚本执行闭环）：

```
① 添加脚本
   ├─ 文件页：选中文件 →「加入队列」
   ├─ 链接导入：MainActivity 解析 intent → 转 Script → 入队
   └─ 方案库：选择 Preset → 展开为多个 QueueItem 入队
        │
        ▼
② 入队（QueueRepository 追加，产生 QueueItem，状态=PENDING）
        │
        ▼
③ 执行（ExecutionCoordinator 从队首取项，创建 ExecutionSession）
        │
        ▼
④ 输出处理（ChunkReader → CharsetDetector → AnsiParser → TerminalBuffer）
        │
        ▼
⑤ 提示识别 + 应答（PromptDetector 命中 → AnswerRule 链匹配 → 写 stdin）
   └─ 无匹配规则 → 状态转 WAITING_INPUT → UI 提示用户手动输入
        │
        ▼
⑥ 完成/停止/失败（进程退出 → 状态迁移 → 看门狗确认）
        │
        ▼
⑦ 录制（SessionRecorder 记录输入/输出/应答 → 可选保存为 Preset）
        │
        ▼
⑧ 重放（从 Preset 重建 QueueItem 列表，再次执行）
```

### 2.2.3 页面联动关系

**导航与联动总线**：引入 `SessionNavigator`（应用内导航抽象）+ `ExecutionSession` 的可观察状态，取代「各 Activity 互相直连、靠静态字段传参」。

| 页面 | 职责 | 与其它页面的联动 |
|------|------|-----------------|
| 队列页（QueueFragment） | 队列面板 + 终端渲染 + 输入 | 接收文件页/方案库「加入队列」；执行中可「跳转到文件页定位脚本」；等待输入时高亮 |
| 文件页（FilesFragment） | 双窗文件管理 | 「加入队列」→ 队列页；「编辑脚本」→ 编辑器；「执行/加入方案」→ 方案库 |
| 方案库（PresetsFragment） | 方案 CRUD | 「运行方案」→ 队列页；「编辑方案」→ 编辑器 |
| 编辑器（EditorActivity） | 脚本编辑 | 保存后回跳文件页；「加入队列」→ 队列页 |
| 媒体（MediaActivity） | 媒体/录制回放 | 读取 `RecordingRepository`；回放后可「存为方案」→ 方案库 |
| 主壳（MainActivity） | Tab 壳 + 文件打开 + 链接导入 + 权限引导 | intent 导入 → 文件页/队列页；权限引导 → 各功能前置 |

**联动规则（硬约束）**：

1. 所有跨页通信走 `SessionNavigator` + 意图参数，禁止静态全局可变状态传参。
2. 「执行中」状态是全局单点真相（single source of truth），所有页面订阅同一 `ExecutionState`，避免各自维护副本。
3. 「加入队列」是唯一入队入口（`QueueRepository.enqueue()`），文件页/方案库/编辑器都调用它，不各自造轮子。

### 2.2.4 执行状态机

```
                    ┌─────────────┐
        enqueue      │   PENDING   │
      ─────────────► │  （已入队）  │
                    └──────┬──────┘
                           │ start()
                    ┌──────▼──────┐
          ┌────────│   RUNNING   │◄────────┐
          │        │  （运行中）  │         │
          │        └──────┬──────┘         │
          │   prompt 命中   │ prompt 未命中  │ input 到达
          │         ┌──────▼───────┐       │
          │         │ WAITING_INPUT│───────┘
          │         │ （等待输入）  │
          │         └──────────────┘
          │
          │  进程退出/看门狗
          ├──────────────► ┌────────────┐
          │  正常退出(0)    │ COMPLETED  │
          ├──────────────► ┌────────────┐
          │  用户停止       │  STOPPED   │
          ├──────────────► ┌────────────┐
          │  非0/异常       │  FAILED    │
          └──────────────► └────────────┘
```

**迁移约束**：

- 状态只能由 `ExecutionCoordinator` 推进，UI 只读状态并渲染，禁止 UI 直接改状态字段。
- 非法迁移（如 RUNNING → PENDING）必须抛异常/记录日志，这是「门控乱」的兜底。
- `WAITING_INPUT` 是「执行中」的子态，用于 UI 提示与输入聚焦，不另立一套标志。

---

## 2.3 模块划分建议

### 2.3.1 目标分层与包结构

假设基础包名 `com.hazeroot`（如实际不同，仅替换前缀）。

```
com.hazeroot
├── app/                    # 应用壳：Application、DI 根、启动
│   ├── HazeRootApp.java
│   └── di/                 # 依赖装配（手写 DI 或 Hilt）
│
├── ui/                     # 表现层（只渲染 + 转发事件）
│   ├── main/               # MainActivity（Tab 壳，~300 行目标）
│   ├── queue/              # QueueFragment + QueueAdapter + TerminalView
│   ├── files/              # FilesFragment + FileAdapter + DualPaneView
│   ├── presets/            # PresetsFragment + PresetAdapter
│   ├── editor/             # EditorActivity
│   ├── media/              # MediaActivity
│   ├── splash/             # SplashActivity + ConvergeSplashView
│   ├── common/             # 通用 UI 组件（Breadcrumb、SelectionToolbar）
│   └── navigator/          # SessionNavigator（跨页导航/联动）
│
├── domain/                 # 领域层（纯 Java，禁止 import android.*）
│   ├── model/              # Script, QueueItem, Preset, AnswerRule,
│   │                       #   ExecutionSession, FileEntry, ExecutionState
│   ├── usecase/            # EnqueueScript, StartExecution, StopExecution,
│   │                       #   SavePreset, RecordSession, AnswerPrompt
│   └── repository/         # ScriptRepository, QueueRepository,
│                           #   PresetRepository, RecordingRepository（接口）
│
├── data/                   # 数据层（实现 repository 接口）
│   ├── filesystem/         # FileSystemGateway 实现（文件操作/APK/分享数据）
│   ├── preset/             # PresetRepository 实现（JSON/DB 存储）
│   ├── recording/          # RecordingRepository 实现
│   └── script/             # ScriptRepository 实现（intent 导入/路径解析）
│
└── core/                   # 执行引擎（独立、可测试，不依赖 ui/domain/data）
    ├── su/                 # SuGateway 接口 + SuGatewayImpl
    │                       #   + RootProbe + ProcessExecutor
    ├── io/                 # ChunkReader, CharsetDetector, AnsiParser
    ├── prompt/             # PromptDetector, AnswerRuleMatcher
    ├── queue/              # ExecutionQueue（纯调度，不含 UI）
    ├── session/            # SessionManager（状态机推进）
    ├── recorder/           # SessionRecorder
    └── watchdog/           # Watchdog
```

### 2.3.2 依赖方向（唯一允许的方向）

```
ui ──► domain ──► data
  │        │
  └────────┼──────► core
           │
core 不依赖任何上层；domain 不依赖 data 的具体实现（依赖 repository 接口）
```

用 **ArchUnit** 强制校验，禁止反向 import（见 1.6.3）。

### 2.3.3 三大 God Object 的拆分映射

**Engine.java（9 帽）→ core 层 8 个类 + domain 1 个用例**：

| 原职责 | 目标类 | 归属包 |
|--------|--------|--------|
| root 探测 | `RootProbe` | core/su |
| su 提权执行 | `SuGateway`（接口）+ `SuGatewayImpl` | core/su |
| 进程执行 | `ProcessExecutor` | core/su |
| 队列调度 | `ExecutionQueue` | core/queue |
| 输出读取 | `ChunkReader` | core/io |
| 两级解码 | `CharsetDetector` | core/io |
| ANSI 状态机 | `AnsiParser` | core/io |
| 提示识别 | `PromptDetector` | core/prompt |
| 自动应答 | `AnswerRuleMatcher` | core/prompt |
| 录制 | `SessionRecorder` | core/recorder |
| 看门狗 | `Watchdog` | core/watchdog |
| 执行流程协调（新增） | `ExecutionCoordinator`（use case） | domain/usecase |

**QueueFragment.java（4 帽）→ ui + domain**：

| 原职责 | 目标类 | 归属包 |
|--------|--------|--------|
| 队列面板 | `QueueFragment`（薄，仅组装） | ui/queue |
| 终端增量渲染 | `TerminalView` + `TerminalAdapter` + `TerminalBuffer`（core/io） | ui/queue + core/io |
| 手动输入 | `InputController`（写 stdin） | ui/queue |
| 执行门控 | `ExecutionGate`（读 ExecutionState 决定按钮可用性） | domain/usecase 或 ui/queue |

**FilesFragment.java（6 帽）→ ui + data + domain**：

| 原职责 | 目标类 | 归属包 |
|--------|--------|--------|
| 双窗 UI | `DualPaneView` + `FilesFragment`（薄） | ui/files |
| 文件列表渲染 | `FileAdapter` | ui/files |
| 文件操作 | `FileOperationService`（use case，调 FileSystemGateway） | domain/usecase |
| APK 安装 | `ApkInstaller`（封装 PackageManager 接口） | data/filesystem |
| 分享 | `ShareController`（封装 Intent） | data/filesystem |
| 多选手势 | `SelectionController`（显式状态机） | ui/files |

---

## 2.4 渐进式重构路线图

> 原则：不推倒重来；每个里程碑产出「可编译 + 行为等价 + 可回滚」的中间态；用 Feature Toggle 隔离新旧实现。

### 里程碑总览

| 里程碑 | 主题 | 目标 | 预估（人日，单人） |
|--------|------|------|-------------------|
| M0 | 工程地基 | 测试/CI/门禁/文档骨架 | 3 |
| M1 | 插接缝 | 核心接口 + 特性测试锁行为 | 5 |
| M2 | 拆 Engine | core 层 8 模块落地 | 8 |
| M3 | 终端渲染专项 | 解码/ANSI/增量渲染修复痛点 2 | 6 |
| M4 | 文件交互专项 | 手势/长文件名/双窗修复痛点 3/4/7 | 6 |
| M5 | 页面联动 | SessionNavigator + 状态机统一 | 5 |
| M6 | 队列/方案/录制闭环 | 入队契约 + Preset 重放 | 5 |
| M7 | UI 层瘦身 + 收尾 | 删除 LegacyEngine，度量验收 | 4 |

### M0 — 工程地基

- **产出**：`ARCHITECTURE.md`、`docs/DESIGN.md` 模板、Checkstyle + Lint + Spotless + ArchUnit 接入、GitHub Actions CI（`lint test assembleDebug`）、CHANGELOG。
- **验收**：CI 绿；`./gradlew lint test` 0 失败；任何 >300 行的新类被 Checkstyle 拦截。
- **风险**：低（纯工具引入）。
- **回滚**：移除插件/配置即可，无行为变化。

### M1 — 插接缝（先锁行为，再动结构）

- **产出**：给 `Engine` 的 su 执行、输出读取、提示识别、应答规则引入接口；补「特性测试」覆盖现有解码/应答/状态行为。
- **验收**：`Engine` 外部行为不变；新增 ≥ 20 个单元测试；核心路径覆盖率 ≥ 50%。
- **风险**：中——接缝引入可能暴露隐藏 bug。通过特性测试先锁现状，再改。
- **回滚**：接口为转发层，`git revert` 单提交即可。

### M2 — 拆 Engine（核心重构）

- **产出**：`core/su`、`core/io`、`core/prompt`、`core/queue`、`core/session`、`core/recorder`、`core/watchdog` 8 个模块；`LegacyEngine` 退化为 Facade 转发到新模块；Feature Toggle `USE_NEW_ENGINE` 默认指向 `LegacyEngine`。
- **验收**：所有单测通过；真机冒烟（root 探测/执行/解码/应答）通过；`Engine.java` 行数 < 300（仅转发）；ArchUnit 依赖方向 0 违反。
- **风险**：高——拆分可能引入并发/顺序回归。缓解：Toggle 双轨运行，逐模块切换验证。
- **回滚**：Toggle 切回 `LegacyEngine` 即恢复老行为，无需回滚代码。

### M3 — 终端渲染专项（修复痛点 2）

- **产出**：`CharsetDetector` 跨块多字节安全、`AnsiParser` 独立状态机、`TerminalBuffer` 环形缓冲、增量 diff 渲染（`notifyItemRangeInserted` + 帧合并）、缓冲上限截断。
- **验收**：1000 行海量输出单帧 ≤ 16ms；UTF-8/GBK 混合脚本输出无乱码/无丢行；ANSI 彩色/清屏正确。
- **风险**：中——性能与正确性权衡。缓解：性能基准测试（Benchmark）纳入 CI。
- **回滚**：Toggle 回退旧渲染路径。

### M4 — 文件交互专项（修复痛点 3/4/7）

- **产出**：`SelectionController` 显式多选状态机；统一手势映射表落地；长文件名中间省略 + 详情展示；双窗独立面包屑 + 跨窗操作；MT 交互基线对齐。
- **验收**：手势冲突清单全消（单/长/左滑/多选无歧义）；长文件名不破版；MT 用户 5 分钟完成常用操作（可用性测试）。
- **风险**：中——交互回归面大。缓解：Espresso 手势测试 + 交互基线走查。
- **回滚**：交互改动独立提交，逐项 revert。

### M5 — 页面联动（修复痛点 6）

- **产出**：`SessionNavigator`、`ExecutionSession` 可观察状态（单点真相）、执行中跳转（队列页→文件页定位脚本、→编辑器）。
- **验收**：执行中任意页面可跳转且状态一致；无静态可变状态传参残留。
- **风险**：中——导航改造涉及多 Activity。缓解：导航用 intent 参数 + 导航抽象隔离。
- **回滚**：导航抽象可切换回直连跳转。

### M6 — 队列/方案/录制闭环（修复痛点 5）

- **产出**：`QueueRepository.enqueue()` 唯一入队入口；`PresetRepository` CRUD + 展开入队；`SessionRecorder` 录制 → 存方案 → 重放闭环。
- **验收**：三种入口（文件/链接/方案）入队行为一致；录制→存方案→重放可完整走通。
- **风险**：中——入队契约影响所有页面。缓解：入队契约单测 + 三入口集成测试。
- **回滚**：独立提交 revert。

### M7 — UI 层瘦身 + 收尾

- **产出**：删除 `LegacyEngine`；`FilesFragment`/`QueueFragment` 行数各 < 300（纯组装）；全量度量（类行数、依赖方向、覆盖率、单帧耗时）达标归档。
- **验收**：无 God Object（>300 行类）；覆盖率 ≥ 60%；真机回归清单全过；文档同步。
- **风险**：低——删除旧代码前的全量回归。
- **回滚**：`LegacyEngine` 保留一个 Tag，必要时从 Git 历史恢复。

---

# 第三部分：开发流程规范（AI 团队化协作）

> 目标：把「把 AI 分作完整业务团队」从口号变成**可复用的协作协议**，用硬性规则锁死「走一步看一步」。

## 3.1 团队角色划分与产出物

| 角色 | 在 AI 协作中的定位 | 必须产出的物（缺一即视为未完成） |
|------|------------------|--------------------------------|
| 产品经理（PM） | 定义「做什么 + 怎么算完成」 | PRD、用户故事（含 AC）、需求优先级表、DoD 勾选 |
| 架构师（Architect） | 定义「怎么组织 + 边界在哪」 | ARCHITECTURE 增量、模块/接口/状态机设计、依赖方向、接缝方案 |
| UI/交互设计师 | 定义「用户怎么操作」 | 手势映射表、交互基线、长文件名/双窗规范、UI 规格 |
| Android 工程师 | 定义「怎么实现」 | 代码、单元/集成测试、PR、模块 DESIGN.md |
| QA | 定义「怎么验证」 | 测试计划、真机回归清单、Bug 报告、验收结论 |
| 发布工程师 | 定义「怎么交付」 | CI/CD 配置、签名产物、CHANGELOG、发布说明 |

**关键机制**：每个 AI 角色**只产出自己角色的物**，跨角色必须显式交接（PM→架构师→工程师→QA），禁止一个角色「顺手替别人做决定」。这就是用流程强制「科学完整符合逻辑」。

## 3.2 科学完整的开发流程 SOP

```
[1] 需求       PM 产出 PRD + 用户故事 + AC + 优先级
        │ 评审通过（架构师+QA 参与）
        ▼
[2] 架构       Architect 产出模块/接口/状态机设计（引用 ARCHITECTURE.md）
        │ 评审通过（工程师+QA 参与）
        ▼
[3] UI 规格    UI 产出交互基线 + 手势映射 + 视觉规格
        │ 走查通过
        ▼
[4] 任务拆解   架构师 + PM 拆解为可并行/有依赖的任务卡片（见 3.4）
        │
        ▼
[5] 开发       工程师按卡实现；每卡先写测试（或先补特性测试）再实现
        │
        ▼
[6] Code Review 架构师复核（单一职责/依赖方向/测试/文档），非作者审批
        │
        ▼
[7] 测试       QA 跑单测 + 皮下测试 + 真机回归清单
        │
        ▼
[8] 真机回归   QA 在 root 真机/模拟器执行回归清单，出验收报告
        │
        ▼
[9] 文档       工程师/架构师同步 DESIGN.md + CHANGELOG
        │
        ▼
[10] 发布      发布工程师走 CI/CD 出包 + 发布说明
        │
        ▼
[11] 复盘       全员复盘：哪些规则被绕过、哪些痛点复发、下轮改进
```

**任务拆解规则（支持并行）**：

- 依赖关系用 `→` 标注（如「插接缝 → 拆 Engine」）。
- 无依赖任务可并行（如「M3 终端渲染」与「M4 文件交互」可并行）。
- 每个任务卡片必须含：所属角色、依赖、AC、DoD、风险、回滚。

## 3.3 约束机制（硬性规则）

以下为**不可协商的硬规则**，任何 AI 角色或人类成员违反即打回：

1. **先 PRD 后代码**：没有 PM 产出的 PRD（含 AC），禁止写实现代码。仅允许「调研/原型 Spike」。
2. **先设计后实现**：每个功能模块必须有架构师的设计文档，再进入编码；禁止「边写边想结构」。
3. **改动必须有验收标准**：任何 PR 必须关联一个含 AC 的任务卡；无 AC 的 PR 直接拒绝。
4. **禁止直接改 God Object**：禁止在 `Engine/FilesFragment/QueueFragment` 里新增功能或嵌套逻辑；新功能进新模块，God Object 只能做「减负」拆分。
5. **单一职责门禁**：新类 >300 行 / 新方法 >40 行，Checkstyle 阻断，不允许压线放行。
6. **依赖方向门禁**：ArchUnit 校验失败 = 构建失败；`domain` 出现 `android.*` import = 构建失败。
7. **先测试后重构**：动存量行为前必须先补特性测试锁现状；无测试的破坏性重构禁止合入。
8. **Feature Toggle 必须有退役计划**：每个 Release Toggle 创建时同步登记移除日期，过期未移除 → CI 报警。
9. **真机回归不可省**：涉及 su/解码/渲染/手势的改动，必须有真机回归记录才能标记 Done。
10. **文档同步**：代码与 DESIGN.md/CHANGELOG 不一致 = Review 不通过。

## 3.4 任务看板 / 里程碑模板

```markdown
# HazeRoot 工程化重构看板

## 里程碑：M2 — 拆 Engine
**负责人**：架构师（设计）/ Android 工程师（实现）/ QA（验证）
**状态**：进行中　**目标日期**：____

### 任务卡
- [ ] M2-1 引入 SuGateway 接口 + LegacySuGateway 转发　【架构师→工程师】
      依赖：M1　AC：su 执行行为等价　DoD：单测+真机冒烟
- [ ] M2-2 拆出 CharsetDetector（跨块安全）　【工程师】
      依赖：M2-1　AC：UTF-8/GBK 混合 100 用例全过
- [ ] M2-3 拆出 AnsiParser　【工程师】
      依赖：M2-1　AC：ANSI 彩色/清屏/光标用例全过
- [ ] M2-4 拆出 ExecutionQueue　【工程师】
      依赖：M2-1　AC：顺序/并发/停止语义一致
- [ ] M2-5 拆出 PromptDetector + AnswerRuleMatcher　【工程师】
      依赖：M2-3　AC：应答规则命中/未命中/多规则优先级
- [ ] M2-6 拆出 SessionRecorder + Watchdog　【工程师】
      依赖：M2-4　AC：录制完整、看门狗超时转 FAILED
- [ ] M2-7 LegacyEngine 退化为 Facade + Toggle 双轨　【架构师→工程师】
      依赖：M2-1~6　AC：Toggle 切换前后行为一致
- [ ] M2-8 真机回归 + 删除旧内联实现　【QA】
      依赖：M2-7　AC：回归清单全过

### 风险登记
| 风险 | 等级 | 缓解 | 状态 |
|------|------|------|------|
| 拆分引入顺序/竞态回归 | 高 | Toggle 双轨 + 特性测试 | 监控中 |

### 验收记录
- [ ] 单测通过　- [ ] 真机回归通过　- [ ] ArchUnit 通过　- [ ] 文档同步
```

> 说明：看板中「任务卡」采用 `[ ] 卡号 标题【角色】依赖：... AC：... DoD：...` 的统一格式，可直接用于 AI 协作的任务派发与验收核对。

---

# 附录 A：术语表

| 术语 | 含义 |
|------|------|
| God Object | 上帝对象：一个类承担过多职责的反模式 |
| Large Class | 大类：字段/方法/行数过多的代码坏味道 |
| Divergent Change | 发散式变更：一个类因多种不同原因而改动 |
| Seam（接缝） | 无需就地编辑即可改变行为的位置 |
| Branch by Abstraction | 按抽象分支：用抽象层让新旧实现并存、逐步替换 |
| Strangler Fig | 绞杀者模式：增量式替换遗留系统 |
| Feature Toggle | 特性开关：用配置控制运行时行为分支 |
| UDF | 单向数据流：状态只朝一个方向流动 |
| FSM | 有限状态机：显式建模状态与迁移 |
| DoD | Definition of Done：完成定义 |
| AC | Acceptance Criteria：验收标准 |
| Trunk-Based Development | 主干开发：在单一主干分支上高频集成 |
| Test Pyramid | 测试金字塔：单元多、端到端少的测试配比 |
| Subcutaneous Test | 皮下测试：绕过 UI 测服务/领域层 |

---

# 附录 B：参考来源

1. Martin Fowler — *Patterns of Legacy Displacement*（遗留系统渐进式替换的四类活动与模式目录）
   https://martinfowler.com/articles/patterns-legacy-displacement/
2. Martin Fowler — *Strangler Fig Application*（绞杀者模式，2024 更新版）
   https://martinfowler.com/bliki/StranglerFigApplication.html
3. Martin Fowler — *Branch by Abstraction*（按抽象分支）
   https://martinfowler.com/bliki/BranchByAbstraction.html
4. Martin Fowler — *Legacy Seam*（接缝，源自 Michael Feathers《Working Effectively with Legacy Code》）
   https://martinfowler.com/bliki/LegacySeam.html
5. Martin Fowler — *Event Interception*（事件拦截）
   https://martinfowler.com/articles/patterns-legacy-displacement/event-interception.html
6. Martin Fowler — *Test Pyramid*（测试金字塔）
   https://martinfowler.com/bliki/TestPyramid.html
7. Pete Hodgson / Martin Fowler — *Feature Toggles (aka Feature Flags)*
   https://martinfowler.com/articles/feature-toggles.html
8. Martin Fowler — *Conway's Law*（康威定律与逆向康威策略）
   https://martinfowler.com/bliki/ConwaysLaw.html
9. Martin Fowler — *Catalog of Patterns of Enterprise Application Architecture*（分层、Domain Model、Service Layer、Repository）
   https://martinfowler.com/eaaCatalog/
10. Refactoring.Guru — *Large Class*（大类代码坏味道与拆分治疗）
    https://refactoring.guru/smells/large-class
11. Trunk Based Development（主干开发）
    https://trunkbaseddevelopment.com/
12. Google — *Android App Architecture Guide*（官方架构指南：分层 + 单向数据流）
    https://developer.android.com/topic/architecture
13. Google — *nowinandroid*（官方 MVVM + UDF + 模块化开源范例）
    https://github.com/android/nowinandroid
14. Material Design 3 — *Gestures*（手势交互规范）
    https://m3.material.io/foundations/interaction/gestures

> 调研说明：本次调研中 `web_search` 因运行环境未配置检索 API key 而不可用，已改用 `web_fetch` 直接抓取上述一手权威资料源（除 developer.android.com 与 github.com 因网络原因 fetch 失败外，其余均成功获取原文）。Android 官方架构指南与 nowinandroid 为业内公认权威，其 URL 真实有效，作为架构参考一并列出。
