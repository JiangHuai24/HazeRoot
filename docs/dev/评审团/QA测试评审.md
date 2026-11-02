# HazeRoot 测试与验收评审报告（QA 视角）

> 版本：v1.0 · 角色：资深 Android QA 测试工程师 + 测试架构师
> 评审对象：HazeRoot「通用 root 脚本执行器」
> 技术栈：Java 17 + AppCompat + Material 1.11 + ConstraintLayout（无 Compose），minSdk 26 / targetSdk 34
> 评审基线：源码 `Engine.java`(1347 行)、`QueueFragment.java`(1678 行)、`FilesFragment.java`(3380 行)，以及 `docs/dev/` 全部 .md
> 结论先行：**当前代码库零测试资产、零测试依赖、三个 God Object 全部不可测；PRD 的 AC 大部分是「愿望型」而非「可测型」；在补测试与拆解之前，任何一次「执行/解码/手势」相关改动都是在无安全网状态下高空作业。**

---

## 0. 一句话总评

HazeRoot 的「产品方向」和「改造文档」写得比代码本身成熟一个量级——PRD/架构/交互规格已经规划到了 `core/io` 八模块和 FSM 六态，但**当前源码仍是三坨 1300~3400 行的单体，且 `app/src` 下只有 `main`，没有任何 `test/` 与 `androidTest/`，`build.gradle.kts` 里没有 JUnit/Mockito/Robolectric/Espresso/ArchUnit/Checkstyle 依赖**。这意味着：文档承诺的「NFR-4 可测试性、NFR-5 门禁」当前 **0% 落地**，所谓「先补特性测试锁行为再重构」的 M1 里程碑，起点是负数。

---

## 1. 测试策略与可测试性评估

### 1.1 现状盘点（事实）

| 项 | 现状 | 证据 |
|---|---|---|
| 单元测试目录 | 无 | `app/src` 仅 `main`，无 `test`/`androidTest` |
| 测试依赖 | 无 | `app/build.gradle.kts:36-43` 仅 5 个实现依赖，无 `testImplementation` |
| 静态检查 | 无 | 无 Checkstyle/ArchUnit/Lint 插件 |
| CI | 无 | 工程化方案里的 M0 未落地 |
| 可注入接缝 | 无 | `Engine.INSTANCE` 静态单例直连 `ProcessBuilder("su")` |

### 1.2 核心逻辑「无法测试」清单（为什么）

下述逻辑在当前结构下**没有一行能被 JVM 单测直接调用**，原因是它们要么与 Android 平台/静态单例/原生进程硬耦合，要么根本是 private 内联在巨型方法里：

1. **两级解码器（`hybridDecode`）**：`Engine.java:1277-1342`，`private final`，且依赖 `Charsets.UTF_8.newDecoder()` 与 GBK 表——本身纯逻辑，但被 `runOne` 的巨型方法内联调用，无公开入口、无字节流注入点。**不可测原因：private + 无 Seam。**
2. **ANSI 状态机（`runOne$feedAnsi`/`runOne$applySgr`）**：`Engine.java:1189-1253` / `1132-1186`，全是 `private static final`，被 `runOne` 的字符循环内联，输入是「已解码 char」而非字节流。**不可测原因：状态机与 I/O 循环、提示识别、自动应答耦合在一个 260 行的 while 循环里。**
3. **执行状态机**：现状根本没有显式 FSM——`runningNow`/`abortFlag`/`finalized`/`hadAutoInput`/`currentProc` 五个 `static volatile` boolean/引用互相打架（`Engine.java:59-74`），PRD 承诺的六态 FSM 不存在。**不可测原因：状态散落为静态可变字段，无法断言状态迁移。**
4. **入队契约**：不存在。三个入口各自直接 `Engine.INSTANCE.getScripts().add(...)`（`QueueFragment.addScriptByPath:737`、`addScriptDialog$lambda$46:1528`），没有 `QueueRepository.enqueue()`，没有去重。**不可测原因：没有「契约」这个对象。**
5. **应答规则（`resolveAnswer`）**：`Engine.java:1344-1346` 是一个**直接 `return null` 的空实现**——PRD 里 `AnswerRuleMatcher` 的核心逻辑当前是假的。**不可测原因：逻辑不存在。**
6. **录制落盘**：散落在 `sendRaw`(215-255) 的 `recordLock` 块和 `runOne` 收尾(1066-1074)，无 `RecordingRepository`。**不可测原因：无边界。**

> 结论：PRD NFR-4「解码器/ANSI/状态机/入队契约均有单元测试，覆盖率 ≥60%」在当前代码结构下**物理上无法达成**——不是「没写测试」，是「没有可测试的单元」。必须先执行 M1「插接缝」（`SuGateway`/`CharsetDetector`/`AnsiParser`/`PromptDetector`/`QueueRepository` 全部抽接口），否则任何测试框架都无处下针。

---

## 2. 压力 / 边界测试盲区（会埋雷的场景）

以下场景**没有任何测试覆盖，且源码里已经存在对应的结构性缺陷**：

| # | 场景 | 现存代码缺陷 | 埋雷后果 |
|---|---|---|---|
| 1 | 终端超长输出（>20 万字符，AC1.4） | `trimTerm()`(Engine.java:185-189) 按 `char` 裁剪，裁剪点可能落在 ANSI 标记 `\u0001+style` 中间 | 孤立 `\u0001` 导致 `appendStyled` 错位 → 乱码/丢行 |
| 2 | UTF-8/GBK 混合 + 块边界截断（AC1.2） | `hybridDecode`(1277-1342) 中 GBK 分支 `0x81<=b<0xFF` 会**先吞掉** `0xC2-0xFF` 的 2 字节 UTF-8 序列（如 `0xC2 0xA0` 全角空格），2 字节 UTF-8 分支 `z = 0xC2<=b<0xE0` 在 GBK 之后才判断，几乎不可达 | GBK 尾字节把合法 UTF-8 序列截成错字 |
| 3 | 并发 su / 快速切窗（NFR-3） | `SU_LOCK` 串行锁(Engine.java:72) 只保护 `su`/`execBytes`，`checkRoot`(257) **不持锁**；`FilesFragment.Pane.load` 每窗起独立线程各发 `ls -Al`，双窗并行 + ELF 探测(995) 再发第三个 su | KernelSU 高并发拒绝/超时（注释已自证），root 判定与文件列表竞态 |
| 4 | 文件大目录（数千条目） | `ls -Al` 全量读入内存后逐行 `LS_RE` 解析(90, 857-937)；`probeElves` 对 >200 文件直接跳过(1010) 导致 ELF 图标失效 | 大目录 ANR/漏标；`LS_RE` 贪婪 `(.+)$` 对含空格文件名 OK 但对含 ` -> ` 字符串的文件名误判 symlink |
| 5 | 编码边界（单字节 GBK 尾字节、孤立代理对） | `hybridDecode` 尾部 `pendingTail`(998, 1341) 用 `bytes.length - i` 返回未消费字节，但 `runOne` 只把 `carry` 保留，**跨块状态只在「GBK 2 字节不完整」时正确，UTF-8 4 字节(emoji) 跨块时 `pendingTail` 计算依赖 `cb.remaining()`(988) 而非真实字节语义** | emoji/四字节字符在块边界被切成 `�` |
| 6 | 快速手势 + 行 View 复用（AC3.6） | `bindSwipe` 每次 bind 重建触摸闭包并捕获 `v`/`$item`(1571-1643)，`longPress` Runnable 持有 View(1604-1609) | 历史「虚影/闪退」根因仍在，未按交互规格「只建一次 GestureController」落地 |
| 7 | 主线程同步 su（无并发场景时也踩雷） | 见 §3 缺陷 #2 | 无网络/无 root 卡顿时直接 ANR |

---

## 3. 缺陷分析（最可能出 bug 的 5 个点，按严重度排序）

### 🔴 缺陷 1：终端缓冲线程不安全 + 裁剪切断 ANSI 标记对（致命）

- **位置**：`Engine.java:83`（`StringBuilder termBuffer`）、`185-189`（`trimTerm`）、`168-206`（`log`/`logRaw`/`plainOf`）；`QueueFragment.java:826-923`（`syncTerm`/`appendStyled`）。
- **根因**：`termBuffer` 是普通 `StringBuilder`，在执行线程 `runOne` 里 `append`，在主线程 `syncTerm` 里 `toString()`，**无任何同步**；`trimTerm` 按字符数 `delete(0, len-MAX)` 裁剪，不保证裁剪点落在 ANSI 标记对边界；`syncTerm` 里 `Kt.contains(dst, '\u0001')` 的兜底判断(838)因为 `appendStyled` 已经把标记去掉、`dst` 里永远没有 `\u0001`，所以**兜底永远不生效**。
- **后果**：高输出频率下读到中间态（半行/半标记），裁剪后孤立 `\u0001` 让 `appendStyled` 后续所有颜色 span 错位——直接表现为用户反馈的「显示不全/乱码/丢行」，且**无法用日志稳定复现**。
- **建议**：`termBuffer` 改成有锁的环形行缓冲（`TerminalRow[]`），裁剪必须在**行边界**做；`syncTerm` 的增量游标改为「已渲染行号」而非「字符数」，与标记结构彻底解耦。这是 FR-1 的核心，必须最优先。

### 🔴 缺陷 2：主线程同步 su → ANR（致命）

- **位置**：`FilesFragment.java:2880-2906`（`renameDialog$lambda$76` 主线程 `exec("mv ...")`）、`3001-3037`（`newDialog$lambda$80` 主线程 `exec("mkdir/touch")`）、`3302-3326`（`chmodDialog$lambda$99` 主线程 `exec("chmod")`）、`2855-2866`（`detailDialog` 主线程 `isElf`）、`2838-2852`（`openEditor` 主线程 `isElf`）、`1585-1595`（`editScriptDialog$lambda$50` 主线程 `isElf`）。
- **根因**：这些回调都在 `DialogInterface.OnClickListener`（主线程）里**同步调用** `Engine.INSTANCE.exec(...)`。`exec` 在有 root 时走 `su()`(405-442)，内含 **3 次重试 × 每次最多 waitFor 无超时 + 350ms sleep**；`su` 卡住（授权弹窗未响应/守护进程异常）时最长可阻塞数秒。
- **对比自证**：同一文件里 `deleteDialog$lambda$79$lambda$78`(2938-2951)、`copyMoveTo$lambda$53`(2411-2454)、`multiDelete$lambda$34$lambda$33`(1538-1555) 都正确地放进了 `new Thread`——说明作者**知道要异步**，但 rename/new/chmod/detail/openEditor 这些路径漏了。
- **后果**：用户重命名/新建/chmod/查看详情/点开编辑的瞬间，主线程卡死 → ANR 弹窗。这是**必现型**（只需 su 响应慢）而非概率型缺陷。
- **建议**：把所有 `exec`/`isElf` 调用统一收敛到一个「后台执行 + 主线程回投」的门面（这正是 `FileOperationService` 该做的），或至少补齐这几处漏网的 `new Thread`。

### 🟠 缺陷 3：自动应答的 prompt 识别不稳定 + `resolveAnswer` 空实现（严重）

- **位置**：`Engine.java:107-113`（`promptKey`）、`1010-1015`（CR 处理）、`1031-1053`（自动应答）、`1344-1346`（`resolveAnswer` 空实现）。
- **根因**：
  1. `promptKey()` 取 `trim(currentPartial)`，而 `currentPartial` 只在**遇到 `\n`** 时才被 `flushPartial` 清空(1260-1275)。**大量交互式提示不带换行**（如 `Password:`、`Are you sure? [y/N]`），此时 partial 会无限累积，prompt key 每次都变，录制(`sendRaw:233`)和重放(`runOne:1032`)的 key 永远对不上 → 自动应答失效。
  2. `\r` 处理(1010-1015)只 `pendingCr=true`，**从不 flush 也不做回车覆盖**，`\r` 结尾的进度条/提示行被丢弃，进一步加剧 partial 累积。
  3. `resolveAnswer` 直接 `return null`，说明当前**根本没有规则匹配逻辑**，PRD FR 里「应答规则匹配」是空壳。
- **后果**：录制→重放闭环（产品卖点）在真实交互脚本上大概率失败；「自动应答」是 PRD 招牌能力却不可用。
- **建议**：`PromptDetector` 必须改为「行尾模式 + 超时 + 不完整行截断」三段判据；在没拆出 `core/prompt` 前，至少先给 `resolveAnswer` 补真逻辑，并用 FakeSuGateway 做纯逻辑单测。

### 🟠 缺陷 4：PTY 退出码采集不可靠（严重）

- **位置**：`Engine.java:872-885`（`usePty` 与 `exitProbe` 注入）、`1059-1065`（`waitCode = proc.waitFor()`）、`1260-1275`（`flushPartial` 解析 `__HR_EXIT_%s__`）。
- **根因**：
  1. 退出码探测 `__HR_EXIT_%s__` 只在 `usePty`（busybox script）路径注入(880)；**非 PTY 路径**(883) 直接 `su -c inner`，`proc.waitFor()` 返回的是 `su` 的退出码，而 KernelSU/部分 su 实现不会透传内层脚本的 `$?` → 脚本失败也显示 exit 0。
  2. 即便 PTY 路径注入了探测串，`busybox script` 是否可用由 `hasBusyboxScript()`(771-792) 缓存决定，一旦探测脚本因 `busybox script` 输出含 `Usage:` 被误判为「不可用」，永久降级到不可靠的非 PTY 路径。
- **后果**：失败脚本被标绿（exit 0），`runQueue` 后续「非 0 告警」逻辑(1086-1095)失效；用户无法判断脚本是否真的成功。
- **建议**：无论 PTY 与否，统一用 `sh -c` 包裹 + 显式 `echo __HR_EXIT_$?__` 注入；`codeOverride` 优先于 `waitCode`(1065) 的取值逻辑要明确为「以注入串为准，waitFor 仅兜底」。

### 🟠 缺陷 5：`abort()` 杀不干净 + `su()` 重试对非幂等命令有副作用（严重）

- **位置**：`Engine.java:794-821`（`abort`）、`405-442`（`su` 3 次重试）、`461-478`（`exec`）。
- **根因**：
  1. `abort()` 只 `currentProc.destroy()`。`currentProc` 在 PTY 路径下是 `busybox script` **外壳**，`destroy()` 杀掉外壳后内层 `sh 脚本` 可能变成孤儿继续跑——用户点了「停止」但脚本还在后台执行，且 `currentProc` 被置 null 后无法再补杀。
  2. `su()` 的注释自称「本项目 su 命令都是幂等的，重试安全」(409)——**这是错误假设**：`FilesFragment` 里 `mv`/`rm -rf`/`cp`/`tar` 全都走 `exec`（进而走 `su`），一旦第一次已成功但 `waitFor()` 返回非 0（信号/竞态），第二次重试会对已移动/已删除的文件再执行一次，`mv` 第二次因源不存在返回非 0 → 整体返回 null → 上层误报「失败」，但文件其实已经动过。
  3. `exec()` 非 root 分支(466-477)与 `su()` 分支(405-442)行为不一致：前者无重试、无串行锁、超时语义不同。
- **后果**：停止键不可靠（脚本泄漏继续跑）；重命名/删除出现「操作成功但 UI 报失败」的幻影错误。
- **建议**：`su` 重试必须按「命令类型」区分（读命令可重试、写命令禁止重试），或改为「重试前先校验副作用是否已发生」；`abort` 需要杀**进程组**而非单个外壳进程。

---

## 4. 验收标准（AC）可测性审查

按「可测 AC = 有明确输入/可观察输出/可量化断言」标准，逐项判级：

| AC | 可测性 | 判语 |
|---|---|---|
| AC1.1（UTF-8 中文完整无乱码） | ✅ 可测 | 有明确输入脚本与断言，但需先有解码器单测入口 |
| AC1.2（GBK/UTF-8 混合同屏） | ⚠️ 半可测 | 「各自正确、无错位」无错位定义；且当前 `hybridDecode` 有 §2#2 结构性缺陷 |
| AC1.3（ANSI 颜色/清屏/光标） | ❌ 不可测 | 「清屏/光标行为正确」缺定义；当前代码**根本不支持清屏/光标**（`feedAnsi` 只处理 `m`/OSC，无 CSI 光标移动），AC 描述的是不存在的功能 |
| AC1.4（>20 万字符不 OOM/ANR，按行丢弃+提示「已截断」） | ❌ 不可测 | 当前 `trimTerm` **没有「已截断」提示**，且按 char 裁剪非按行——AC 与实现完全对不上 |
| AC1.5（1000 行单帧 ≤16ms） | ⚠️ 半可测 | 16ms 可测（Systrace/FrameMetrics），但当前无帧率测试基建；`TextView` 全量 append 大概率不达标 |
| AC1.6（上翻不吸底） | ❌ 不可测 | 当前 `syncTerm` 只有 `wasAtBottom` 单点判断(856)，**没有「上翻/吸底」显式状态机**，AC 描述的功能不存在 |
| AC1.7（静默超时提示） | ✅ 可测 | 看门狗(899-906) 已有，可断言日志 |
| AC2.1（长文件名中间省略保扩展名） | ❌ 不可测 | `item_file.xml:13` 是 `ellipsize=end`，与 AC 要求的 `middle` 相反 |
| AC2.2（长按弹全名） | ❌ 不可测 | 无此交互 |
| AC3.1-3.7（手势状态机） | ❌ 不可测 | AC 描述的是「单一 GestureController 四态」，当前是 `bindSwipe` 巨型闭包状态机，与 PRD 目标态完全不符；AC3.5（多选左滑忽略）当前代码**在多选态下仍会进 `mode==1` 左滑**（`bindSwipe$lambda$39:1758-1760` 未判 `multiMode`），与 AC 冲突 |
| AC4.1-4.4（统一入队+去重） | ❌ 不可测 | 无 `QueueRepository.enqueue()`，无去重，AC 描述的功能不存在 |
| AC5.1-5.5（页面联动/事件总线） | ❌ 不可测 | 无 AppEventBus；文件页「执行」走 `tryStartDirect` 后**不切队列页**（`FilesFragment.executeFile$lambda$50:2116` 只 toast，不 switchTo），与 AC5.1 直接矛盾 |
| AC6.1（MT 六项对齐） | ⚠️ 半可测 | 主观「行为与 MT 一致」无自动化断言，只能靠人工走查清单 |
| AC7.1-7.7（换肤） | ❌ 暂不可测 | PRD 已注明「作者确认前不编码」，AC 是方向性描述 |
| AC9.1-9.4（架构门禁） | ✅ 可测 | 这是唯一「真正可自动测」的一族：Checkstyle 行数门禁、ArchUnit 依赖方向、FSM 枚举、唯一入队入口，均可机器断言 |

> **关键结论**：PRD 的 AC 大面积描述的是「目标态」（重构后），而评审对象是「现状代码」。**按现状代码验收，AC1.2/1.3/1.4/1.6/2.x/3.x/4.x/5.x 全部 fail**；但若按「重构后目标态」验收，这些 AC 在代码未变之前根本无法执行。**这是 PRD 与代码之间最严重的断层**：AC 是为「理想的 core/io 八模块」写的，而不是为「当前的 God Object」写的。建议 PRD 增加「现状基线 AC」，否则无法区分「重构前已坏」和「重构引入回归」。

---

## 5. 真机回归清单（每个里程碑必过）

> 原则：涉及 su/解码/渲染/手势的改动，以下清单全过才算 Done。用 root 真机（Magisk 一台 + KernelSU 一台 + 无 root 一台）。

### 5.1 执行与退出码（每里程碑必测）

| 步骤 | 操作 | 断言点 |
|---|---|---|
| 1 | 入队一个 `exit 0` 的 sh | 终端输出完整、末尾 exit code 显示 **0**、按钮回「运行全部」 |
| 2 | 入队一个 `exit 7` 的 sh | 末尾显示 **exit 7**、出现非零告警、条目标失败色 |
| 3 | 入队一个 `echo start; sleep 60; echo done` 后立刻点停止 | **3 秒内** `adb shell ps` 无残留 `sleep 60`/内层 sh 进程 |
| 4 | 连续入队 3 个脚本跑队列 | 顺序执行、每个完成态正确、第三个不提前启动 |

### 5.2 终端渲染（FR-1）

| 步骤 | 操作 | 断言点 |
|---|---|---|
| 5 | 跑 `seq 1 3000`（海量输出） | 无 ANR、无 OOM、主线程无掉帧（`adb shell dumpsys gfxinfo` 单帧 <16ms） |
| 6 | 跑输出 >20 万字符的脚本 | 最旧内容按**行**丢弃、出现「已截断」提示、无半行/乱码 |
| 7 | 跑 UTF-8 中文 + GBK 混合输出脚本 | 两种编码同屏各自正确、无 `�`/`?`/错位 |
| 8 | 跑含 ANSI 颜色/清屏/光标/`\r` 进度条的脚本 | 颜色正确、`\r` 进度行能实时刷新、清屏不残留旧行 |
| 9 | 输出到达时上翻查看历史 | 不强制回底；滚到底后恢复吸底 |

### 5.3 文件页与手势（FR-2/3/6）

| 步骤 | 操作 | 断言点 |
|---|---|---|
| 10 | 长文件名条目展示 | 扩展名 `.sh` 永不丢失（middle 省略） |
| 11 | 点按条目 | 打开/执行，不弹新建菜单 |
| 12 | 长按条目 | 进多选+选中+震动，**不打开** |
| 13 | 多选态点按 | 只切换选中，不打开 |
| 14 | 浏览态左滑 | 弹单条操作菜单，不进入多选 |
| 15 | 多选态左滑 | **忽略**，不弹单条菜单 |
| 16 | 快速滚动 + 点击 | 无虚影、无闪退、无「点空白误弹新建」 |
| 17 | 大目录（>2000 文件） | 列表加载不 ANR，滚动流畅，ELF 图标正确 |
| 18 | 左/右窗开同一目录 + 点按右窗行 | 操作落在右窗，不误判左窗 |

### 5.4 入队 / 录制重放 / 联动（FR-4/5）

| 步骤 | 操作 | 断言点 |
|---|---|---|
| 19 | 文件页加入队列 | 停留文件页、队列徽标 +1 |
| 20 | 再次加入同一路径 | 提示「已在队列中」，不重复入队 |
| 21 | 手动输入不存在路径 | 报错并允许「仍要添加」 |
| 22 | 录制一个带 `Password:`（无换行）的交互脚本 → 存方案 → 重放 | 重放自动应答成功，无需手输 |
| 23 | 文件页点「执行」 | **自动切到队列/终端页**看输出 |
| 24 | 执行中任意页面 | 底部导航/顶部有全局执行状态徽标 |
| 25 | 重命名/新建/chmod/查看详情 | 操作期间 UI 不冻结，无 ANR |

### 5.5 root 探测与降级（NFR-3）

| 步骤 | 操作 | 断言点 |
|---|---|---|
| 26 | 冷启动（授权链路未就绪） | 不误报「无 root」（3 次重试 + 缓存 15s 失效生效） |
| 27 | Magisk / KernelSU / APatch 各跑一遍 | root 判定一致，su 命令均成功 |
| 28 | 无 root 设备 | 文件页降级路径与队列页失败提示一致，不闪退 |

---

## 6. 测试环境挑战与取舍

### 6.1 三台设备矩阵（硬性要求）

- **Magisk 真机**（主基线）：覆盖面最广、授权弹窗稳定。
- **KernelSU 真机**（必测）：源码注释已自证 KSU 存在「并发 su 拒绝/超时」「`su -c 'a;b'` 只提权第一条」两个特有问题（`Engine.java:69-72, 414-417`），所有 `su` 相关改动必须在 KSU 上回归，否则 Magisk 通过不代表 KSU 通过。
- **无 root 设备/模拟器**：验证降级路径与「无 root 引导弹窗」不闪退。

### 6.2 真机 vs 模拟器

| 维度 | 真机 | 模拟器 |
|---|---|---|
| root/su | ✅ 唯一可测（模拟器 root 与 Magisk/KSU 行为差异大） | ❌ 不可替代 |
| 文件操作/手势 | ✅ 触摸 slop、ListView 复用行为真实 | ⚠️ 可做手势脚本但复用/触控差异大 |
| 解码/ANSI/状态机 | ✅ 真机回归兜底 | ✅ **纯逻辑应优先在 JVM 单测里跑**（配 FakeSuGateway 注入字节流） |
| CI 稳定性 | ❌ 慢、难托管 | ✅ 用于 lint/test/assemble |

**取舍结论**：解码器/ANSI/状态机/入队契约的正确性**绝不能只靠真机**（真机只能证明「这几次没炸」，不能穷举块边界/编码组合）。正确分层是：**JVM 单测覆盖纯逻辑（占比 ~60%）→ 皮下测试用 FakeSuGateway 覆盖执行协调（~30%）→ 真机只做关键路径冒烟（~10%）**。但如 §1 所述，这一步的前提是先插接缝——否则「用 FakeSuGateway」本身就是空话。

### 6.3 su 依赖的测试隔离（关键设计）

- 引入 `SuGateway` 接口后，测试注入 `FakeSuGateway` 返回**预设字节流**（含 UTF-8 截断块、GBK 混合、ANSI 序列、`\r` 进度条、`__HR_EXIT_7__` 注入串等），可在无 root 的 CI 上稳定复现 §2 的所有边界场景。
- 真机回归只负责「真实 su 行为」这一件无法模拟的事（授权链路、KSU 并发、进程组 kill）。

---

## 7. 交付前必须补的「最低可测基线」（QA 硬性建议）

按优先级，任何里程碑合入前至少满足：

1. **P0**：`app/build.gradle.kts` 增加 `testImplementation`（JUnit4 + Mockito + Robolectric）与 `androidTestImplementation`（Espresso）；`src/test`/`src/androidTest` 目录建起。
2. **P0**：抽出 `SuGateway`/`CharsetDetector`/`AnsiParser`/`PromptDetector` 四个接口（M1 接缝），否则 NFR-4 无法成立。
3. **P0**：修复 §3 缺陷 1、2（线程安全 + 主线程 su），这是两个「必现/高频」级致命项。
4. **P1**：给 `resolveAnswer` 补真逻辑 + 给 `trimTerm` 加「已截断」提示（对齐 AC1.4/应答闭环）。
5. **P1**：Checkstyle（单类 ≤300、单方法 ≤40）+ ArchUnit 接入 CI，先拦「新 God Object」，再谈拆解。

---

*本报告基于 `Engine.java`(1347)、`QueueFragment.java`(1678)、`FilesFragment.java`(3380) 及 `docs/dev/` 全部文档的一手通读。所有行号均为当前源码实际行号，可直接定位。*
