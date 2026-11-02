# HazeRoot 交互与实现改造规格

> 版本：v1.0 · 目标读者：HazeRoot 开发者
> 技术约束：Java 17 + AppCompat + Material 1.11 + ConstraintLayout（无 Compose）
> 涉及核心源码：`Engine.java`（执行/解码/队列）、`QueueFragment.java`（终端+队列）、`FilesFragment.java`（双窗文件管理，3351 行）、`MainActivity.java`（Tab 壳）
> 目标：逐条根治 7 大痛点，并让 MT 管理器用户可无缝迁移。

---

## 0. 结论速览（TL;DR）

| 痛点 | 根因关键词 | 核心改法 |
|---|---|---|
| 1 执行 sh 显示不全 | 解码边界错 + 增量渲染粒度粗 + 环形缓冲裁剪丢失 + 吸底失效 | 字节流式 UTF-8 解码器 + 行级脏标记渲染 + 可变历史行数 + 「吸底/上翻」状态机 |
| 2 长文件名 | 单行硬截断 + 信息层级缺失 | 三区布局（图标/主信息/辅信息）+ 主名 `ellipsize=end` + 副行第二行 + 长按/点按弹完整名 |
| 3 手势乱 | 状态混用、闭包重建、回调失配 | 单一 GestureController + 明确四态状态机 + 事件只读快照，杜绝「虚影/闪退」 |
| 4 加队列乱 | 多入口无统一反馈、去重缺失 | 统一 QueueService + 唯一 key 去重 + 三入口同一结果态 |
| 5 页间联动差 | 无事件总线、无跳转协议 | 单一 AppEventBus + DeepLink 路由 + 执行态跨页徽标 |
| 6 MT 迁移难 | 范式不对齐 | 对齐 MT 的「长按多选/CAB/双窗/面包屑/左滑操作」语义，差异化「终端面板」 |
| 7 终端难用 | 无辅助键、输入区与输出区未分离、无历史回放 | 输出面板 + 底部输入条 + 辅助键行 + 滚动状态栏 |

---

## 1. 第一层：调研结论（含来源）

> 注：本轮 `web_search` 因会话缺少 DeepSeek Search API Key 不可用，采用 `web_fetch`/`Invoke-WebRequest` 直采权威源；GitHub 源可直连，Termux 相关 wiki 被 Anubis 反爬拦截，改用 Termux 源码仓库 + Wikipedia/xterm 规范作为一手依据。

### 1.1 安卓文件管理器交互范式（MT / ES / Solid Explorer）

| 范式 | 主流规范做法 | 结论/参考来源 |
|---|---|---|
| 双窗布局 | 主窗+副窗，中缝可拖调比例，当前窗用背景亮度/描边区分 | Solid Explorer/MT 双窗均以「当前窗高 1 档背景」区分 |
| 路径导航 | **面包屑可点回跳**优于纯文本框；长路径折叠为「…/末级」 | Material 3 App bars 官方将 breadcrumb 作为层级导航首选 |
| 点按/长按 | 点按=打开；长按=进入多选并选中该项；多选模式下点按=切换选中 | 这是 Android 文件管理器事实标准（MT/ES/Solid 一致） |
| 左滑 | 左滑=该条目的快捷操作（删除/更多），不进入多选 | ES/Solid 的 swipe-to-action 范式 |
| 长文件名截断 | 列表主名单行 `ellipsize=end`；详情/第二行补全；点按或长按弹完整名 | Material 3 Lists 建议「主行+副行」结构承载长内容 |
| 操作菜单 | 高频项宫格/bottom sheet；删除等破坏性操作进「更多」并独立配色 | Material 3 bottom sheets / menus 官方规范 |

**来源：**
- Material 3 Lists：https://m3.material.io/components/lists/overview · https://m3.material.io/components/lists/guidelines
- Material 3 Bottom sheets：https://m3.material.io/components/bottom-sheets/overview
- Material 3 Menus：https://m3.material.io/components/menus/overview
- Material 3 App bars（面包屑导航）：https://m3.material.io/components/app-bars-top/guidelines

### 1.2 移动端终端 App 交互（Termux / iSH / a-Shell / JuiceSSH）

**核心事实（来自 Termux 一手源码）：**

1. **等宽字体是门面第一要素**：`TerminalRenderer` 用 `Paint.measureText("X")` 计算 `mFontWidth`，用 `getFontSpacing()` 计算 `mFontLineSpacing`，据此做**字符网格渲染**（每个字符一个 cell），这是终端清晰无错位的关键。来源：`TerminalRenderer.java`（termux-app）。
2. **ANSI 渲染走专用状态机**：`TerminalEmulator.processByte()` 逐字节解析，UTF-8 多字节序列（2/3/4 字节）用 `mUtf8ToFollow` 计数器跨 chunk 累积，非法序列落到 `UNICODE_REPLACEMENT_CHAR (0xFFFD)`。ESC/CSI/OSC/DCS 用 `ESC_*` 状态常量显式建模。来源：`TerminalEmulator.java`（termux-app）。
3. **环形缓冲 + 可变历史行数**：`TerminalBuffer` 是 `TerminalRow[]` 环形缓冲，`mActiveTranscriptRows` 控制保留在历史里的行数上限，`mScreenFirstRow` 指向可见屏起始；`externalToInternalRow()` 做「逻辑行 → 数组下标」映射。来源：`TerminalBuffer.java`（termux-app）。
4. **吸底滚动是显式状态机**：`TerminalView.onScreenUpdated()` 中——若正在选文本或 `isAutoScrollDisabled()`，则**不**吸底；否则 `if (mTopRow != 0) setTopRow(0)` 强制回底。`mTopRow` 为负数表示上翻了多少行。来源：`TerminalView.java`（termux-app）。
5. **编码/解码顺序**：先 `append(byte[], length)` → 逐字节 `processByte` 做 UTF-8 组码 → `processCodePoint(int)` 做字符/控制序列分发。**解码发生在渲染之前、且在字节流层面跨块保持状态**——这是「显示不全」最常见根因的对照基准。
6. **增量渲染**：Termux 是「变化时 `invalidate()` + `onDraw` 全量重绘可见行」，性能靠「只画可见行 + 行内 run 合并（`drawTextRun` 把同 style 相邻字符合批）」而非「只画脏行」。来源：`TerminalView.java` / `TerminalRenderer.java`。

**来源：**
- Termux 终端模拟器：https://github.com/termux/termux-app/blob/master/terminal-emulator/src/main/java/com/termux/terminal/TerminalEmulator.java
- Termux 环形缓冲：https://github.com/termux/termux-app/blob/master/terminal-emulator/src/main/java/com/termux/terminal/TerminalBuffer.java
- Termux 渲染器：https://github.com/termux/termux-app/blob/master/terminal-view/src/main/java/com/termux/view/TerminalRenderer.java
- Termux 视图/滚动：https://github.com/termux/termux-app/blob/master/terminal-view/src/main/java/com/termux/view/TerminalView.java
- ANSI 转义序列：https://en.wikipedia.org/wiki/ANSI_escape_code
- xterm 控制序列规范：https://invisible-island.net/xterm/ctlseqs/ctlseqs.html
- 组合字符（combining char，Zalgo 防护）：https://en.wikipedia.org/wiki/Combining_character

### 1.3 长文件名显示方案

| 方案 | 适用 | 结论 |
|---|---|---|
| 单行 `ellipsize=end` | 列表主行 | **默认方案**，配合「副行第二行」补全关键尾段（如扩展名） |
| 两行折行 | 文件名超长但需完整可见 | 列表项固定两行时用，密度下降，**不默认** |
| marquee 滚动 | 极少场景 | 会抢占手势与焦点，**不用** |
| 展开查看/点击弹全名 | 兜底 | **必做**：长按或点按条目详情弹 Dialog/Toast 显示完整路径+文件名 |
| 信息层级 | 通用 | 主行=名称；副行=大小·时间·类型图标·扩展名；类型图标用「颜色+图标」双重编码 |

**来源：** Material 3 Lists（见上）；Refactoring UI 的「hierarchy is everything」原则：https://refactoringui.com/

### 1.4 脚本执行器/任务队列产品的交互

| 维度 | 规范做法 |
|---|---|
| 队列状态 | 四态显式表达：待执行（灰）/执行中（运行色+呼吸）/已完成（成功色）/失败（错误色+可重试） |
| 一键入队 | 从文件页「点脚本 → 动作 → 加入队列」+ 顶部/底部 Toast/徽标反馈；重复入队按 key 去重 |
| 进度呈现 | 当前项进度条 + 队列总数进度（如「3/7」）+ 终端实时输出联动 |
| 页间联动 | 执行开始→自动切到队列/终端页；完成→回跳来源页或停留（可配置）；跨页用**事件总线**而非直接引用 Fragment |

---

## 2. 第二层：具体改造规格（核心交付）

> 下列每条均给出「可编码」的布局结构 / 状态机伪代码 / 组件选择，可直接落到 AppCompat。

---

## A. 终端渲染与「显示不全」根治

### A.1 根因诊断表（逐条对应解法）

| # | 可能根因 | 症状 | 解法 |
|---|---|---|---|
| 1 | **UTF-8 解码跨块被截断** | 中文/emoji 显示为 `?` 或 `�`，块边界字符丢失 | 字节流式解码器（见 A.2.2），跨 chunk 保持 `mUtf8ToFollow/mUtf8Index` 状态，绝不按 chunk 用 `new String(bytes)` 重解码 |
| 2 | **增量渲染粒度错** | 只画了新增部分，旧内容被抹掉/重叠 | 改为「行级脏标记」：新增/变化行标脏，`onDraw` 重绘所有可见行（Termux 式），不做像素级局部重绘 |
| 3 | **环形缓冲裁剪丢失** | 长输出滚屏后早期内容消失或跳跃 | 缓冲裁剪在**行**边界做，绝不在字符中间切；`mActiveTranscriptRows` 可配（默认 2000 行，长输出场景按内存动态扩展） |
| 4 | **编码边界** | 汉字/宽字符被切成两半 | 环形缓冲按**行（TerminalRow）**存储，每行是独立 `char[]`，宽字符用 `charCount` 判断，不跨行拆分 |
| 5 | **PTY 块缓冲** | 输出被攒着不出，直到换行/满块 | 执行端 `stdbuf`/`-u` 关闭块缓冲（`sh -c` 前加 `stdbuf -o0 -e0` 或设 `PYTHONUNBUFFERED`）；PTY 设为 raw 模式（`termios`） |
| 6 | **滚动不吸底** | 新输出不自动滚到底 | 显式「吸底/上翻」状态机（见 A.2.4） |
| 7 | **Spannable 性能** | 长输出卡顿/ANR | 不把整屏文本塞进 `SpannableString`；用 Termux 式「行内 run 合批 + `canvas.drawText`」直接绘制，样式与文本分离 |

### A.2 终端渲染架构建议

#### A.2.1 分层架构（文字描述）

```
PTY/进程 stdout
   │  (byte[], length) 字节流
   ▼
Engine 解码层（UTF-8 组码器，跨 chunk 状态机）
   │  (int codePoint) 字符码点
   ▼
ANSI 状态机（ESC/CSI/OSC 解析 → 屏幕操作 + 样式状态）
   │  操作：写字符/移动光标/换行/清屏/设样式
   ▼
TerminalBuffer（环形缓冲 TerminalRow[]，行级存储）
   │  脏行标记 / 行号映射 externalToInternalRow()
   ▼
TerminalRenderer（按可见行绘制：行内 run 合批 + drawText）
   │  只在 onDraw 时读缓冲
   ▼
Canvas（View）
```

#### A.2.2 字节流 UTF-8 解码器（伪代码，Java）

```java
final class Utf8Decoder {
    private int toFollow = 0;      // 还需多少后续字节
    private int index = 0;         // 已累积字节数
    private final byte[] buf = new byte[4];

    /** 逐字节喂入，可能产出 0..n 个码点 */
    void feed(byte b, Consumer<Integer> sink) {
        if (toFollow > 0) {
            if ((b & 0xC0) == 0x80) {
                buf[index++] = b;
                if (--toFollow == 0) sink.accept(assemble());
            } else {
                // 非法后续字节：整段替换为 U+FFFD，并重处理当前字节
                sink.accept(0xFFFD);
                toFollow = 0; index = 0;
                feed(b, sink);
            }
            return;
        }
        if ((b & 0x80) == 0) { sink.accept(b & 0xFF); }
        else if ((b & 0xE0) == 0xC0) { start(2, b, 0x1F); }
        else if ((b & 0xF0) == 0xE0) { start(3, b, 0x0F); }
        else if ((b & 0xF8) == 0xF0) { start(4, b, 0x07); }
        else { sink.accept(0xFFFD); } // 非法起始字节
    }

    private void start(int n, byte b, int mask) {
        toFollow = n - 1; index = 1; buf[0] = b;
        // mask 记录于 assembler 中按 index 反推
    }

    private int assemble() { /* 按 index 反推首字节掩码，拼 int codePoint */ }
}
```

> 关键点：**绝不在 chunk 边界重新解码**。Termux 的 `TerminalEmulator.append()` 就是此模型（`mUtf8ToFollow`/`mUtf8InputBuffer` 跨块保持）。

#### A.2.3 增量渲染算法（行级脏标记 + 全量可见行重绘）

```java
// TerminalBuffer 维护：
Set<Integer> dirtyRows = new HashSet<>();  // 脏行号（逻辑行号）

// 写操作（写字符/清屏/滚屏/改样式）时：
void markRowDirty(int logicalRow) { dirtyRows.add(logicalRow); }

// 渲染（onDraw）时：
void render(Canvas canvas) {
    int firstVisible = mTopRow;                 // 负数为上翻
    int lastVisible  = firstVisible + mRows - 1;
    for (int row = firstVisible; row <= lastVisible; row++) {
        // 全量重绘该行（Termux 式）；dirtyRows 仅用于决定是否跳过纯静态行
        if (isRowStatic(row)) continue;         // 可选优化：全静态行跳过
        TerminalRow tr = buffer.rowAt(row);
        drawRowRunBatched(canvas, tr, row);     // 行内按 style 合批 drawText
    }
    dirtyRows.clear();
}
```

> 结论：**不做逐字符脏渲染**（历史 bug 主源），采用「行脏标记 + 可见行重绘」，复杂度 O(可见行数)，长输出也稳定。

#### A.2.4 滚动策略（吸底 / 上翻状态机）

```java
// TerminalView 成员：
int mTopRow = 0;                    // 0=吸底；负=上翻行数
boolean mAutoScroll = true;         // 用户是否处于「吸底」意愿
boolean mSelecting = false;

void onScreenUpdated() {
    int historyRows = buffer.getActiveTranscriptRows();
    if (mTopRow < -historyRows) mTopRow = -historyRows;   // 越界收敛

    if (mSelecting || !mAutoScroll) {
        // 用户在上翻/选文本：不吸底，只按 scrollCounter 平移
        mTopRow -= emulator.getScrollCounter();
    } else if (mTopRow != 0) {
        // 吸底：若不在底，回到底
        mTopRow = 0;
    }
    emulator.clearScrollCounter();
    invalidate();
}

// 用户手势：
void onScroll(int deltaRows) {
    int newTop = mTopRow + deltaRows;
    newTop = Math.max(-buffer.getActiveTranscriptRows(), Math.min(0, newTop));
    mTopRow = newTop;
    mAutoScroll = (newTop == 0);       // 滚回底部即恢复吸底
    invalidate();
}

// 提供一个「回到最新」FAB/按钮，任何上翻时显示，点击 mAutoScroll=true; mTopRow=0;
```

#### A.2.5 长输出（>20 万字符）环形裁剪与性能

| 参数 | 建议值 | 说明 |
|---|---|---|
| `mActiveTranscriptRows`（历史行数上限） | 默认 2000 行，最大 10000 行（可配） | 超限丢弃**最旧的行**，在行边界丢弃，绝不断字符 |
| 单行 `TerminalRow` 字符 | `char[]` 固定列宽（=屏宽），超宽 wrap 成新行 | 每行内存固定，避免 OOM |
| 渲染 | 只画可见行（约 30–60 行） | 与总行数无关，O(屏高) |
| 合成（combining char）上限 | 每列 15 个（Termux 值） | 防 Zalgo 攻击 OOM |
| 帧率控制 | 输出高峰用 `Choreographer` 节流 + `invalidate()` 合并 | 避免每字节一帧 |

#### A.2.6 输入区 / 提示行 / 光标交互规格

```
┌──────────────────────────────────────────────┐
│ 终端输出面板（深色 #282A36，等宽 13sp，1.5行高）│
│                                              │
│   ... 实时输出，吸底滚动 ...                  │
│                                              │
│   ▍光标块（2px 宽，闪烁周期 500ms）           │
├──────────────────────────────────────────────┤
│ 状态条：⌄ 自动吸底 | 当前脚本名 | 状态灯      │  ← 40dp
├──────────────────────────────────────────────┤
│ 输入条（EditText，等宽，深色）                │  ← 48dp
│ [Esc] [Tab] [Ctrl] [↑] [↓] [←] [→]  [发送⏎] │  ← 辅助键行，可折叠
└──────────────────────────────────────────────┘
```

- **光标**：用 `invalidate()` + 定时器翻转可见位（周期 500ms），不每帧全量重绘。
- **提示行**：独立于输出面板，用 `warning` 色细横幅，可收起；不占用终端输出区。
- **输入区**：与输出面板分离，`EditText` 等宽字体；发送后清空、不丢失焦点（`requestFocus()` 恢复）。
- **辅助键行**：`Esc/Tab/Ctrl/方向键` 用 `ImageButton`+`textAppearance`，发送对应的转义/控制序列（如 `Esc`→`\033`、`↑`→`\033[A`）。

---

## B. 文件页长文件名显示

### B.1 列表项布局结构（文字描述）

```
┌──────────────────────────────────────────────────────┐
│ [类型图标]  ├ 主行：文件名（单行 ellipsize=end）      │
│ (40dp)      │ 副行：大小 · 时间 · 扩展名（可选第二行）│
│             │         （仅当主行被截断时显示）        │
│                                        [更多 ⋮ 48dp] │
└──────────────────────────────────────────────────────┘
```

**XML 骨架（ConstraintLayout）：**

```xml
<androidx.constraintlayout.widget.ConstraintLayout
    android:layout_width="match_parent" android:layout_height="wrap_content"
    android:minHeight="56dp" android:paddingStart="8dp" android:paddingEnd="4dp">

    <ImageView android:id="@+id/icon" android:layout_width="40dp"
        android:layout_height="40dp" app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" app:layout_constraintBottom_toBottomOf="parent"/>

    <TextView android:id="@+id/name" style="@style/TextAppearance.HazeRoot.ListTitle"
        android:layout_width="0dp" android:layout_height="wrap_content"
        android:maxLines="1" android:ellipsize="end"
        app:layout_constraintStart_toEndOf="@id/icon"
        app:layout_constraintEnd_toStartOf="@id/more"
        app:layout_constraintTop_toTopOf="parent"/>

    <TextView android:id="@+id/sub" style="@style/TextAppearance.HazeRoot.ListSub"
        android:layout_width="0dp" android:layout_height="wrap_content"
        android:maxLines="1" android:ellipsize="middle"
        android:visibility="gone"
        app:layout_constraintStart_toEndOf="@id/icon"
        app:layout_constraintEnd_toStartOf="@id/more"
        app:layout_constraintTop_toBottomOf="@id/name"/>

    <ImageButton android:id="@+id/more" android:layout_width="48dp"
        android:layout_height="48dp" app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toTopOf="parent" app:layout_constraintBottom_toBottomOf="parent"/>
</androidx.constraintlayout.widget.ConstraintLayout>
```

### B.2 长文件名截断与兜底方案

| 规则 | 实现 |
|---|---|
| 主行截断 | `maxLines=1` + `ellipsize=end`，**永远保留扩展名尾段可见**（若主名超长，用 `ellipsize=middle` 保留「首 + … + 尾.ext」） |
| 副行补全 | 当 `name` 实际文本长度 > 可视列宽（用 `Paint.measureText` 判断）时，`sub.setVisibility(VISIBLE)`，显示「大小 · 时间 · 完整尾段」 |
| 兜底查看全名 | 长按条目（未进多选时）或点「更多 → 详情」弹 `MaterialAlertDialog`，显示**完整文件名 + 完整路径**，支持长按复制 |
| 信息层级 | 图标（颜色+图标双编码：文件夹=主题色、`.sh`=脚本徽标、APK=应用图标）；副行：大小·修改时间 |

### B.3 路径栏（面包屑 vs 文本框）

**采用「面包屑 + 上一级按钮」：**

```
[/sdcard]  ›  Download  ›  scripts  ›  run.sh        [↑]
   ↑点击跳转    ↑点击跳转      ↑点击跳转（当前项高亮）
```

- 用 `HorizontalScrollView` + 若干 `Chip/TextView` 实现，长路径只滚动不截断；无法完整展示时折叠为「…/末两级」。
- 顶部保留「上一级 ↑」图标按钮作为快捷键。
- 路径栏点击当前目录 → 弹**路径文本框**可手动粘贴/输入（满足高级用户），支持「复制路径」。

---

## C. 手势交互重构（重点）

### C.1 手势状态机（四态，显式建模）

```
状态：NORMAL（浏览） / MULTI（多选） / ACTION_MENU（单条操作弹出） / DRAG_SCROLL（滚动中）

事件（GestureController 统一分发）：
  TAP           点按（UP 在 slop 内、时长 < longPressTimeout）
  LONG_PRESS    长按（DOWN 后超 longPressTimeout 未移动）
  SWIPE_LEFT    左滑（水平位移 > swipeSlop，方向向左）
  SWIPE_RIGHT   右滑
  MOVE/UP/CANCEL

转移表：
┌──────────┬───────────────────────────────────────────────────────┐
│ 当前态   │ 事件 → 动作 + 次态                                     │
├──────────┼───────────────────────────────────────────────────────┤
│ NORMAL   │ TAP        → 打开/执行该条目；次态 NORMAL              │
│ NORMAL   │ LONG_PRESS → 进入 MULTI 并选中该项；震动反馈；次态 MULTI│
│ NORMAL   │ SWIPE_LEFT → 弹出该条单条操作菜单；次态 ACTION_MENU    │
│ NORMAL   │ MOVE       → 滚动列表；次态 DRAG_SCROLL               │
│ DRAG_SCR │ UP         → 回 NORMAL                                 │
│ MULTI    │ TAP        → 切换该项选中态；次态 MULTI                │
│ MULTI    │ LONG_PRESS → 忽略（防误触）或切换全选；次态 MULTI      │
│ MULTI    │ SWIPE_LEFT → 忽略（多选中左滑不做单条操作）            │
│ MULTI    │ 选中数=0   → 退出 MULTI；次态 NORMAL                   │
│ ACTION_… │ TAP/外部点击 → 关闭菜单；次态 NORMAL                    │
└──────────┴───────────────────────────────────────────────────────┘
```

### C.2 GestureController 伪代码（根治「闭包重建 → 虚影/闪退」）

```java
final class GestureController implements View.OnTouchListener {
    enum State { NORMAL, MULTI, ACTION_MENU, DRAG_SCROLL }
    private State state = State.NORMAL;
    // 关键：每次 DOWN 抓「只读快照」，不用闭包捕获可变 row/view
    private final Rect slopRect = new Rect();
    private int downX, downY;
    private boolean movedOutOfSlop;
    private final Handler longPressHandler = new Handler(Looper.getMainLooper());

    @Override
    public boolean onTouch(View v, MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = (int) e.getX(); downY = (int) e.getY();
                movedOutOfSlop = false;
                int slop = ViewConfiguration.get(v.getContext()).getScaledTouchSlop();
                slopRect.set(downX - slop, downY - slop, downX + slop, downY + slop);
                // 长按检测：复用 View 的 postDelayed + 取消防抖
                v.getParent().requestDisallowInterceptTouchEvent(true);
                longPressHandler.removeCallbacksAndMessages(null);
                longPressHandler.postDelayed(() -> {
                    if (state != State.MULTI && !movedOutOfSlop) {
                        onLongPress(v);   // 进入 MULTI 或弹详情
                    }
                }, ViewConfiguration.getLongPressTimeout());
                return true;

            case MotionEvent.ACTION_MOVE:
                if (!slopRect.contains((int) e.getX(), (int) e.getY())) {
                    movedOutOfSlop = true;
                    longPressHandler.removeCallbacksAndMessages(null);
                }
                // 这里不做滚动判定，交给 RecyclerView 自己处理；
                // 只在 state==DRAG_SCROLL 时透传
                return state == State.DRAG_SCROLL;

            case MotionEvent.ACTION_UP:
                longPressHandler.removeCallbacksAndMessages(null);
                if (!movedOutOfSlop) {
                    onTap(v);            // TAP
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                longPressHandler.removeCallbacksAndMessages(null);
                return true;
        }
        return false;
    }

    private void onTap(View v) {
        switch (state) {
            case NORMAL: openOrRun(v); break;
            case MULTI:  toggleSelection(v); break;
            case ACTION_MENU: closeMenu(); break;
            default: break;
        }
    }

    private void onLongPress(View v) {
        if (state == State.NORMAL) {
            state = State.MULTI;
            enterMultiSelect(v);   // 选中该项 + 显示 CAB + 震动
        }
    }
}
```

> **防「虚影/闪退」三条铁律：**
> 1. 事件处理用 `MotionEvent` 坐标快照，**不在回调里捕获 RecyclerView 的 row/view 引用**；需要条目数据时用 `RecyclerView.getChildAdapterPosition(v)` 实时取，配 `NO_POSITION` 守卫。
> 2. 长按/点按回调统一在 `onTouch` 内基于**坐标**判定，不复用「点击监听 + 长按监听」双回调（双回调在滚动/复用后易失配）。
> 3. `GestureController` 随 Adapter 生命周期**只建一次**（`onCreateViewHolder` 之外持有），不每次 bind 重建。

### C.3 多选模式 + 选择高亮 + 批量操作条（CAB）

**多选模式 UI：**

```
进入 MULTI 时：
  1. 顶部普通 Toolbar 替换为 Contextual Action Bar（CAB）
     ┌─ [✓ 已选 3 项]  [复制][移动][压缩][更多 ⋮] ─┐
  2. 列表条目加「选择指示」：左侧图标位替换为 CheckBox/选中圈
  3. 选中条目背景 = primary_container 底 + primary 1.5dp 描边
  4. 底部导航隐藏（避免误触跳页）
退出 MULTI（选中数=0 或 CAB 返回键）：
  CAB 下滑退出（200ms），恢复 Toolbar + 底部导航
```

**CAB 组件选择（AppCompat）：**

| 能力 | 组件 |
|---|---|
| 上下文操作条 | `ActionMode`（`startSupportActionMode`）或自定义 `Toolbar` 切换；推荐自定义 Toolbar 以便完全控制样式与「更多」 |
| 批量操作菜单 | 高频 4 项（复制/移动/压缩/删除）进 Toolbar；其余进 `BottomSheetDialog` |
| 选择状态计数 | 标题动态 `TextView.setText("已选 " + count + " 项")` |

**状态维护：**

```java
// FilesFragment 内：
final HashSet<String> selectedPaths = new HashSet<>();  // 用 path 而非 position 做 key（防复用错位）
// Adapter bind 时：
boolean checked = selectedPaths.contains(item.path);
// 切换：
if (checked) selectedPaths.remove(path); else selectedPaths.add(path);
notifyItemChanged(pos);  // 或局部刷新，避免整表重刷导致闪
if (selectedPaths.isEmpty()) exitMultiSelect();
```

---

## D. 队列添加与执行联动

### D.1 统一 QueueService（单一数据源，替代散落入口）

```java
final class QueueService {
    // 去重 key：脚本路径 + 参数 hash（可配「允许重复」）
    private final LinkedHashMap<String, QueueItem> items = new LinkedHashMap<>();
    private final Set<QueueListener> listeners = new HashSet<>();

    enum Status { PENDING, RUNNING, DONE, FAILED, CANCELLED }

    interface QueueListener {
        void onItemAdded(QueueItem item);
        void onItemStatusChanged(QueueItem item, Status oldS, Status newS);
        void onQueueCleared();
    }

    boolean enqueue(String scriptPath, Map<String,String> args) {
        String key = scriptPath + '#' + hash(args);
        if (items.containsKey(key)) return false;   // 重复，返回 false 触发「已在队列」反馈
        QueueItem it = new QueueItem(scriptPath, args, Status.PENDING);
        items.put(key, it);
        notifyAdded(it);
        return true;
    }
}
```

### D.2 三个入队入口（统一反馈 + 去重）

| 入口 | 交互流程 | 反馈 |
|---|---|---|
| ① 文件页浏览选脚本 | 长按/点「更多 → 加入队列」| 入队成功：Snackbar「已加入队列」+ 队列 Tab 徽标 +1；重复：Snackbar「已在队列中」 |
| ② 从链接/剪贴板导入 | 「+」→ 粘贴路径/URL → 解析 → 确认入队 | 解析失败红色提示；成功同上 |
| ③ 手动输入路径 | 「+」→ 文本输入框 → 校验存在 → 入队 | 路径不存在报错并允许「仍要添加」 |

**去重反馈统一：** 三入口共用 `QueueService.enqueue()` 的 `boolean` 返回值，UI 层据此显示一致 Snackbar，避免「加乱了」。

### D.3 队列执行状态机

```
PENDING ──start──▶ RUNNING ──exit0──▶ DONE
   ▲                  │
   │                  ├─exit≠0──▶ FAILED（可重试 → PENDING）
   │                  └─用户停止──▶ CANCELLED
   └── clear ── (全部移除)
```

**状态视觉：**

| 状态 | 图标/颜色 | 附加 |
|---|---|---|
| PENDING | 灰 · 空心圆 | 可上移/下移/删除 |
| RUNNING | 运行色 #0EA5E9 · 呼吸闪烁 | 显示「当前行/耗时」+ 停止按钮 |
| DONE | 成功色 · ✓ | 可重跑 |
| FAILED | 错误色 · ✕ | 「重试」主按钮 + 错误摘要 |
| CANCELLED | 中性灰 · ⏹ | 可重跑 |

### D.4 页面联动（事件总线 + DeepLink 路由）

```java
// 全局单一事件总线（替代 Fragment 直接引用）：
final class AppEventBus {
    interface Listener { void onEvent(AppEvent e); }
    // 事件：QUEUE_ITEM_STARTED / QUEUE_ITEM_FINISHED / QUEUE_CHANGED / EXEC_PROGRESS
}

// MainActivity 订阅：
onEvent(QUEUE_ITEM_STARTED) {
    // 若用户未手动锁定页面：自动切到队列/终端 Tab
    navigateToTab(TAB_QUEUE);
    updateQueueBadge(RUNNING);
}

onEvent(EXEC_PROGRESS p) {
    // 跨页进度：无论当前在哪页，更新底部导航徽标 + 顶部状态灯
    updateGlobalProgress(p.current, p.total);
}

onEvent(QUEUE_ITEM_FINISHED) {
    // 完成回跳策略（可配置）：回来源页 / 停留队列页 / 弹 Snackbar
    if (prefs.autoReturn) navigateToTab(originTab);
    updateQueueBadge(0);
}
```

**Root 状态检测时机：**

| 时机 | 动作 |
|---|---|
| App 启动 / 从后台回前台 | 后台线程检测 `su -c id`，缓存结果 |
| 每次 `enqueue` 前 | 若缓存过期（>60s）重检；无 Root 时入队仍允许但标「需 Root」+ 提示 |
| 执行开始前 | 最终确认；无 Root 时直接失败并给可操作提示（引导授权/检查） |
| 检测结果变化 | 广播 `ROOT_STATUS_CHANGED` 更新全局 Root 徽标 |

**「从文件页点脚本执行后跳转到哪」明确规则：**

| 场景 | 目标页 | 原因 |
|---|---|---|
| 文件页点「执行」 | 自动切到**队列/终端页**（TAB_QUEUE） | 用户需要看输出与进度 |
| 文件页点「加入队列」 | 停留在文件页 | 批量添加场景，不打断 |
| 完成后（默认） | 停留队列页，Snackbar 报告结果 | 保留输出可回看 |
| 完成后（开「完成后回跳」） | 回来源文件页 | 与 MT「执行完回到原处」对齐 |

---

## E. MT 管理器对齐范式对照表

| 功能/交互 | MT 管理器范式 | HazeRoot 现状 | HazeRoot 目标（对齐/差异化） |
|---|---|---|---|
| 双窗布局 | 左右双窗，当前窗高亮 | 已有双窗但区分弱 | **对齐**：当前窗背景高 1 档 + 中缝拖调 |
| 路径导航 | 面包屑可点回跳 | 文本框为主 | **对齐**：面包屑 + 上一级 + 点击弹文本框 |
| 点按 | 打开/执行 | 基本一致 | **对齐**：点按=打开/执行 |
| 长按 | 进入多选并选中 | 有长按但逻辑乱 | **对齐**：长按=进入多选（状态机 C.1） |
| 多选模式点按 | 切换选中 | 与点按冲突 | **对齐**：多选中点按=切换选中 |
| 左滑 | 单条快捷操作 | 逻辑混乱 | **对齐**：左滑=单条操作菜单（非多选） |
| 批量操作条 | 顶部 CAB | 整屏宫格覆盖 | **对齐**：顶部 CAB（高频 4 项 + 更多） |
| 操作菜单 | bottom sheet / 宫格 | 宫格过密 | **对齐**：高频 6 项宫格，破坏性操作进「更多」 |
| 复制/移动/压缩/解压 | 统一「操作-目标窗-确认」三步 | 流程断裂 | **对齐**：目标窗选择 + 进度 + 完成回跳 |
| APK 安装 | 点 APK → 安装流程 | 已有 | **对齐**：点按直接进安装 |
| 长文件名 | 单行截断 + 详情 | 截断做得差 | **对齐**：主名 ellipsize + 副行 + 弹全名（B 节） |
| 类型图标 | 颜色+图标双编码 | 已有基础 | **对齐**：强化脚本/APK 徽标 |
| 终端 | 无（MT 非终端产品） | 独立终端 | **差异化**：终端是 HazeRoot 优势，做「设备面板」质感 |
| 脚本执行 | 无（MT 以文件为主） | 核心能力 | **差异化**：队列+实时输出+自动应答是招牌 |
| Root 能力 | 有（Root 文件操作） | 有 | **对齐**：Root 状态徽标全局一致 |

**必须对齐的（MT 用户迁移的关键 6 项）：** 长按多选、CAB、双窗高亮、面包屑、左滑操作、长文件名截断。
**可差异化的（HazeRoot 优势 3 项）：** 终端面板质感、脚本队列联动、自动应答交互。

---

## 3. 落地优先级与工作量估计

| 优先级 | 改造项 | 对应痛点 | 估计 | 风险 |
|---|---|---|---|---|
| P0 | 字节流 UTF-8 解码器 + 行级脏标记渲染 + 吸底状态机 | 1、7 | 中 | 需回归旧脚本输出 |
| P0 | 手势四态状态机 + GestureController 统一 | 3 | 中 | 历史手势 bug 集中区，需充分回归 |
| P1 | 文件页列表项三区布局 + 长名截断 + 面包屑 | 2、6 | 低 | 布局微调 |
| P1 | QueueService 统一 + 三入口去重反馈 | 4 | 中 | 迁移现有队列数据 |
| P1 | AppEventBus + DeepLink 路由 + 跨页徽标 | 5 | 中 | 生命周期/泄漏 |
| P2 | CAB 批量操作条 + 多选高亮 | 3、6 | 中 | ActionMode 兼容 |
| P2 | 终端输入区 + 辅助键行 + 提示行收纳 | 7 | 中 | 输入焦点管理 |
| P3 | Root 状态检测时机 + 完成回跳策略 | 5、6 | 低 | 时序 |

---

## 4. 参考来源汇总

- Material 3 Lists：https://m3.material.io/components/lists/overview
- Material 3 Bottom sheets：https://m3.material.io/components/bottom-sheets/overview
- Material 3 Menus：https://m3.material.io/components/menus/overview
- Android 触摸手势官方文档：https://developer.android.com/develop/ui/views/touch-and-input/gestures
- Android ViewConfiguration（touch slop/long-press timeout）：https://developer.android.com/reference/android/view/ViewConfiguration
- Termux TerminalEmulator（UTF-8 解码 + ANSI 状态机）：https://github.com/termux/termux-app/blob/master/terminal-emulator/src/main/java/com/termux/terminal/TerminalEmulator.java
- Termux TerminalBuffer（环形缓冲/历史行数）：https://github.com/termux/termux-app/blob/master/terminal-emulator/src/main/java/com/termux/terminal/TerminalBuffer.java
- Termux TerminalRenderer（网格渲染/run 合批）：https://github.com/termux/termux-app/blob/master/terminal-view/src/main/java/com/termux/view/TerminalRenderer.java
- Termux TerminalView（吸底滚动状态机）：https://github.com/termux/termux-app/blob/master/terminal-view/src/main/java/com/termux/view/TerminalView.java
- ANSI 转义序列：https://en.wikipedia.org/wiki/ANSI_escape_code
- xterm 控制序列规范：https://invisible-island.net/xterm/ctlseqs/ctlseqs.html
- 组合字符/Zalgo 防护：https://en.wikipedia.org/wiki/Combining_character
- Refactoring UI（信息层级）：https://refactoringui.com/
