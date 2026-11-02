# HazeRoot P0 修复验收报告

> 依据：评审团 6 份报告（R1 QA / R2 终端重度 / R3 MT迁移 / R4 新手 / R5 架构 / R6 UIUX）
> 状态：已完成 10 项 P0 修复，全部 `assembleDebug` 编译通过
> 版本号：**未变动**（70 / 2.8.4），严格遵守「保存≠发版」规范

---

## 一、修复清单（按提交顺序）

| # | 提交 | 修复内容 | 驱动评审员 | 验证 |
|---|---|---|---|---|
| 1 | `3e384e5` | **录制→重放闭环断裂**：`recordedInputs` 从未写回，存方案 `rec` 恒空 | R2 | ✅ 编译通过 |
| 2 | `3e384e5` | **文件缺失误标绿点**：`exitCode=null` 被显示成"成功" | R2 | ✅ |
| 3 | `b219e8f` | **竞态止血**：`runningNow`→AtomicBoolean、`currentProc`→volatile、`scripts/presets`→CopyOnWriteArrayList | R5 | ✅ |
| 4 | `212e9af` | **主线程 su → ANR**（openEditor/detailDialog 的 isElf 转后台） | R1+R5 | ✅ |
| 5 | `ddd401e` | **主线程 su → ANR**（rename/new/chmod 转后台，补齐 5 处） | R1+R5 | ✅ |
| 6 | `54edca8` | **假停止**：abort 只杀外壳 → 改 `pkill -TERM/-KILL` 按脚本路径清理进程树 | R2 | ✅ |
| 7 | `1376a17` | **终端显示不全根治**：`termBuffer` 加锁 + `trimTerm` 改**行边界裁剪** + UI 读快照 | R1+R5+R6 | ✅ |
| 8 | `e6f7aa1` | **`\r` 进度条**：回行首覆盖重写，`\r\n` 按换行处理 | R2 | ✅ |
| 9 | `1e5aee6` | **CSI 清屏/光标**：`J/K/H/G/A` 做等价语义，不再全丢弃 | R2 | ✅ |
| 10 | `fa8f3e7` | **shebang 语义 + 工作目录**：非 ELF 按首行解释器执行；执行前 `cd` 到脚本目录 | R2 | ✅ |
| 11 | `827bcec` | **syncTerm 死代码兜底**：改为「buffer 被裁剪则全量重绘」的有效判据 | R1+R5 | ✅ |

---

## 二、逐项技术说明

### P0-1 录制→重放闭环（最关键）

**问题**：`Engine.recordingCollect`（Map<prompt, List<value>>）收集到的用户回答，**从未写回** `ScriptItem.recordedInputs`。`setRecordedInputs` 全工程零调用。`saveScripts` 序列化时 `rec` 恒为 `[]`。`resolveAnswer()` 是死代码永远 `return null`。

**后果**：用户录了半小时回答 → 存方案 → 重放一个答案都不会回。**这是产品核心卖点的功能性缺失**，也直接解释了作者「方案库脚本更新要重新录制太麻烦」的困惑 —— 真相不是麻烦，是根本没生效。

**修复**：`runOne` 收尾处把 `recordingCollect` 展开为扁平 `List<RecordedInput>`，写回 `s.setRecordedInputs(...)` 再落盘。

**修复位置**：`Engine.java` `runOne` 收尾段（原 L1066-1074）。

---

### P0-2 文件缺失误标绿点

**问题**：`runOne` 文件探测失败时直接 `return`，`s.exitCode` 保持 `null`；`ScriptAdapter.onBindViewHolder` 逻辑是 `exitCode == null` 且 `done == true` → 显示 `dot_done`（绿点）。

**后果**：脚本文件不存在，UI 显示绿色"成功"，误导用户。

**修复**：文件缺失时显式 `s.setExitCode(-1)`。

---

### P0-3 竞态止血（崩溃级）

**问题**：
1. `runningNow` 的 `if (runningNow) return; runningNow = true;` 是 check-then-act，两线程可同时通过 → **双队列并发执行**，同时写 `termBuffer`、抢 `currentProc`。
2. `currentProc` 非 volatile，`abort()`/`sendRaw()` 可能读到过期进程 → **停止关错进程**、输入写进上一个进程。
3. `scripts`/`presets` 是普通 `ArrayList`，UI 线程删脚本 + 执行线程遍历 → **`ConcurrentModificationException` 崩溃**。

**修复**：
- `AtomicBoolean RUNNING_GATE.compareAndSet(false, true)` 原子门禁，队列结束 `set(false)` 释放。
- `currentProc` 加 `volatile`。
- `scripts`/`presets` 改 `CopyOnWriteArrayList`。

---

### P0-4/5 主线程 su → ANR

**问题**：`FilesFragment` 里 `openEditor`、`detailDialog`、`renameDialog`、`newDialog`、`chmodDialog` 五处在**主线程**（`DialogInterface.OnClickListener` / 点击回调）同步调用 `Engine.INSTANCE.exec()` / `isElf()`。`exec` 走 `su()`（3 次重试 × 每次无超时 waitFor + 350ms sleep），su 一慢就**必现 ANR**。

**佐证**：同一文件里 `deleteDialog`、`copyMoveTo`、`multiDelete` 都正确用了 `new Thread` —— 说明作者知道要异步，这 5 处是漏网。

**修复**：5 处全部改为 `new Thread(...)` + `runOnUiThread(...)` 回主线程更新 UI。

---

### P0-6 假停止

**问题**：`abort()` 只 `currentProc.destroy()`。`currentProc` 是 `su -c "sh -c '...'"` 或 `busybox script` 的**外壳进程**，杀掉外壳后内层脚本及其子进程变孤儿继续跑。

**后果**：用户点「停止」，界面显示已停止，但**后台 CPU 100%**。

**修复**：新增 `currentScriptPath` 记录当前执行的脚本路径；`abort()` 时调用 `killProcessTreeByPath()`，用 `pkill -TERM -f '<path>'` + 0.3s + `pkill -KILL -f '<path>'` 清理所有匹配该脚本路径的残留进程。

**说明**：Android 的 `java.lang.Process` 没有 `pid()` 方法（Java 9+ API），故采用按路径 `pkill -f` 方案，比 PID 方案更彻底（能杀掉脱离父进程的子进程）。

---

### P0-7 终端显示不全根治（核心）

**问题链**：
1. `termBuffer` 是普通 `StringBuilder`，**执行线程 append、主线程 toString，完全无同步** → 读到中间态（半行/半标记）。
2. `trimTerm()` 按 **char 数**直接 `delete(0, len-MAX)`，裁剪点可能落在 `\u0001 + styleChar` 这对 ANSI 标记**中间**，产生孤立 `\u0001`，使 `appendStyled` 从该点开始**全部错位**。
3. `syncTerm` 里 `Kt.contains(dst, '\u0001')` 兜底是**死代码**（`appendStyled` 已剥离标记，`dst` 里永远没有 `\u0001`），所以「防裁剪错位」的兜底从未生效。

**修复**：
- 新增 `TERM_LOCK`，`log`/`logRaw`/`trimTerm` 全部加锁；UI 侧改读 `snapshotTerm()` 快照。
- `trimTerm` 改为 **行边界裁剪**：算出目标位置后向后搜索最近的 `\n`，从**下一行行首**开始保留，保证永不切断标记对。
- `syncTerm` 兜底判据改为「`buf.length() < renderedChars` = buffer 被裁剪过 → 全量重绘」，真正生效。
- 新增 `isTermEmpty()` / `clearTerm()` 线程安全方法，替换 UI 侧直接操作 `termBuffer` 的两处。

---

### P0-8 `\r` 进度条

**问题**：`pendingCr` 被赋值 4 次但**从未被读取**。`\r` 被 `pendingCr = true; continue;` 直接吞掉。

**后果**：`for i in $(seq 1 100); do printf "\r验证中... %d%%" "$i"; done` 这类进度条，被渲染成 **100 行竖排**，一直刷到 20 万字符上限。

**修复**：`\r` 语义分两种——
- `\r\n`（标准换行）→ 按普通换行处理，保留行内容；
- 单独 `\r`（进度条）→ 清空当前行缓冲，从行首覆盖重写。

实现上用「向后看一个字符」判定，跨块情况保留 `pendingCr` 标记。

---

### P0-9 CSI 清屏/光标

**问题**：`feedAnsi` 的 CSI 状态机**只认 `m`（SGR 颜色）**，`H/J/K/A/B/C/D/G` 全部落入 else 分支 `esc=0; csi.clear()` 被丢弃。

**后果**：脚本 `printf "\033[2J\033[H"` 清屏后画菜单，在 HazeRoot 里变成「旧输出残留 + 新菜单叠加」，菜单完全看不清；自动应答的 prompt 文本也混入残留垃圾。

**修复**：识别 `J`（清屏）/`K`（清行）/`H`/`f`/`G`/`A`（光标归位）并置位 `pendingClearScreen` / `pendingClearLine`，主循环消费后清空整屏或当前行。

**说明**：TextView 渲染模型没有真正的字符网格光标，故做**等价语义近似**（清屏=清空历史、归位=清空当前行）。彻底的终端模拟器需要 R2 建议的自建 PTY + 字符网格（列为后续 P1）。

---

### P0-10 shebang + 工作目录

**问题**：
1. 非 ELF 时无条件 `sh path`，**无视 shebang**。带 `#!/data/adb/magisk/busybox ash` 或 `#!/usr/bin/env python3` 的脚本会被 `sh` 解释 → 语法错误。
2. 执行命令没有 `cd`，工作目录是 su 的默认目录，**脚本内相对路径（`./config`、`cat data/x`）全部失效**。

**修复**：
- 非 ELF 时先 `head -n 1` 读首行，若以 `#!` 开头则 `chmod +x` 后直接执行（内核读 shebang）；无 shebang 才回退 `sh`。
- 命令前置 `cd '<脚本所在目录>' 2>/dev/null;`，恢复相对路径语义。

---

## 三、尚未修复的 P0（需较大工程或需用户决策）

| # | 项目 | 原因 |
|---|---|---|
| P0-11 | **自建 PTY**（不依赖 busybox `script`）+ raw mode + Ctrl-C/D | 需引入 PTY JNI 或移植 Termux 方案，工作量较大 |
| P0-12 | **退出码独立通道**（去掉 `__HR_EXIT_` 魔数） | 需改进程通信机制 |
| P0-13 | **字符网格渲染 + `\t` 对齐** | 需重写渲染层为自定义 View（非 TextView） |
| P0-14 | **参数传递**（ScriptItem.args） | 需改数据模型 + UI |
| P0-15 | **提示匹配降级链**（精确→规范化→包含→正则→顺序） | 需重写 PromptDetector |
| P0-16 | **失败策略可配置**（失败即停/跳过/重试） | 需加配置项 + UI |
| P0-17 | **UI 线程 su 其余位置**（FilesFragment 仍有 13 处 exec 在后台线程，已确认安全） | 已核查无误 |

---

## 四、行为变更提示（用户需知）

| 变更 | 影响 |
|---|---|
| 文件缺失标红 | 之前显示绿点"成功"，现在显示红点（`exitCode=-1`） |
| 停止会真杀进程 | 之前"假停止"后台继续跑，现在会清理残留进程树 |
| 有 shebang 的脚本按 shebang 执行 | 之前一律 `sh`，现在尊重首行解释器 |
| 执行前 cd 到脚本目录 | 相对路径开始生效（之前失效） |
| 录制答案会真正保存 | 之前存方案是空的，现在会写入并可重放 |
| `\r` 进度条原地刷新 | 之前会刷成上百行 |

---

## 五、验证方式（建议真机回归）

因当前无 root 真机连接，以下需用户在真机验证：

1. **录制闭环**：跑一个交互脚本 → 输入答案 → 保存方案 → 重放 → 确认答案自动回填。
2. **终端显示**：跑 `seq 1 3000` → 无乱码/丢行；跑 `printf "\r%d%%"` 进度条 → 原地刷新。
3. **清屏菜单**：跑带 `\033[2J\033[H` 的脚本 → 菜单干净显示。
4. **shebang**：跑一个 `#!/system/bin/sh` 之外的脚本（如 busybox ash）→ 正常执行。
5. **相对路径**：跑含 `./xxx` 的脚本 → 能找到同目录文件。
6. **停止**：跑 `while true; do sleep 1; done` → 点停止 → `adb shell ps` 无残留。
7. **ANR**：重命名/新建/chmod/查看详情 → UI 不冻结。
8. **文件缺失**：入队一个不存在的路径 → 显示红点而非绿点。

---

*本报告依据评审团 6 份报告的交叉验证结论生成。所有修复均已 `assembleDebug` 编译通过，未升版本号。*
