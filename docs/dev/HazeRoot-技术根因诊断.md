# HazeRoot 技术根因诊断（一手源码证据）

> 由主 AI 直接通读源码得出，作为后续 PRD / 架构改造的硬事实依据。
> 源码位置：`D:\WorkBuddy\RootScriptRunner\app\src\main\java\com\hazeroot\scriptrunner\`

---

## 一、终端"显示不全"的根因（Engine.java + QueueFragment.java）

### 1.1 环形缓冲裁剪可能切断多字节字符（高危）

`Engine.java` `trimTerm()`：

```java
private final void trimTerm() {
    if (termBuffer.length() > MAX_TERM_CHARS) {          // MAX_TERM_CHARS = 200000
        termBuffer.delete(0, termBuffer.length() - MAX_TERM_CHARS);
    }
}
```

**问题**：`delete(0, ...)` 按 `char` 计数裁剪，但 `termBuffer` 里混有：
1. **中文等多字节 UTF-8 解码后的 char**（Java 内部是 UTF-16，一个汉字是 1 个 char，这块问题不大）；
2. **ANSI 样式标记 `\u0001 + styleChar` 的成对结构**——裁剪点若落在 `\u0001` 和它的 styleChar 之间，会产生一个**孤立的 `\u0001`**，导致 `appendStyled` 后续解析错位。

**更严重**：`QueueFragment.syncTerm()` 用 `renderedChars`（字符数）做增量渲染的游标，但 `termBuffer` 带标记、`dst`（Editable）去标记，两者长度不一致。当 buffer 被 `trimTerm()` 从头裁剪后，`buf.length() < renderedChars` 分支会 `renderedChars=0 + dst.clear()` 全量重绘——这是对的兜底。但 `Kt.contains(dst, '\u0001')` 这个判断**只在 dst 里出现 `\u0001` 时才全量重绘**；正常情况下 `appendStyled` 已把标记去掉，`dst` 里**永远不会有 `\u0001`**，所以这个"防裁剪错位"的兜底**从未生效**——一旦裁剪点切在标记中间，增量渲染就会错位，表现为"显示不全/乱码/丢失行"。

### 1.2 增量渲染的游标与去标记长度不一致

`syncTerm()`：
```java
String buf = Engine.INSTANCE.getTermBuffer().toString();  // 带 \u0001 标记
...
String substring = buf.substring(this.renderedChars);      // 按"带标记"的长度切
appendStyled(dst, substring);                              // 内部去掉标记 append 到 dst
this.renderedChars = buf.length();                         // 游标=带标记长度
```
`appendStyled$flush`：
```java
int from = $dst.length();                                  // dst 是"去标记"长度
$dst.append(substring);
$dst.setSpan(colorSpan, from, $dst.length(), 33);          // 索引基于 dst，是对的
```
**结论**：`renderedChars` 用"带标记"长度，`dst` 用"去标记"长度，**两者各自自洽但不可互比**。`if (buf.length() < renderedChars)` 是带标记间的比较（自洽），`if (dst.length() > 0)` 是去标记的判断（自洽）——逻辑能跑，但**一旦 `termBuffer` 被 `trimTerm()` 裁剪且裁剪点落在标记中间，`renderedChars` 与真实内容就对不上**，产生错位渲染。

### 1.3 `log()` / `logRaw()` 的线程安全

`termBuffer` 是普通 `StringBuilder`，在**执行线程**（`runOne` 读输出→`logRaw`）里 append，在**主线程**（`syncTerm`）里 toString+clear 渲染。`StringBuilder` 非线程安全，且 `termBuffer` 与 `renderedChars` 之间无同步——`emit("log")` 走 `main.post`，但 `termBuffer.append` 是在执行线程直接做的，`syncTerm` 在 main 线程读。**存在读写竞争**，高输出频率下可能读到中间态（半行/半标记），表现为偶发显示不全。

### 1.4 输出块缓冲导致"攒批"

交接文档已记录：PTY 路径（`busybox script`）下 stdout 块缓冲，长跑脚本进度不实时。这不是"显示不全"，但用户会感知为"卡住/不刷新"。

---

## 二、文件页"长文件名显示不好"（item_file.xml + FilesFragment.java）

### 2.1 布局证据

`item_file.xml`：
```xml
<TextView android:id="@id/fName"
    android:ellipsize="end" android:singleLine="true"
    android:textSize="14sp" android:textStyle="bold" .../>
<TextView android:id="@id/fMeta"   <!-- 大小/时间/权限 -->
    android:singleLine="true" android:textSize="10sp" android:fontFamily="monospace" .../>
```

**问题**：
1. `fName` `singleLine + ellipsize=end`：长文件名（如 `Fuzz_Cheat_v4.1.sh`、带日期后缀的备份）被**尾部省略号截断**，用户看不到关键后缀（`.sh`/版本号）。
2. `fMeta` 单行：大小+时间+权限全挤一行 10sp 等宽，稍长就截断。
3. **没有"点按/长按看全名"的兜底**——`pathView` 有 `onLongClick`（看完整路径），但**文件名本身**无任何展开交互。

### 2.2 改进方向
- 文件名改**两行折行**（`maxLines=2` + `ellipsize=end`），或保留单行但**省略号放中间**（`ellipsize=middle`，保住文件扩展名）；
- 增加"点按行打开 / 长按文件名弹出完整名+路径"的兜底；
- `fMeta` 拆成"大小 · 时间"两段或去掉权限串（权限用图标/色点表达）。

---

## 三、文件页"手势交互一塌糊涂"（FilesFragment.java bindSwipe）

### 3.1 现状：一个巨型 `bindSwipe` 状态机

`bindSwipe` 内部维护 `mode`（0=待定 / 1=左滑 / 2=拖选多选）+ `longFired`（长按）+ `downX/downY/startTx/dragAnchor` 等多个状态，在 `onTouch` 里用 `case 0/1/2` 处理 ACTION_DOWN/MOVE/UP。

### 3.2 历史 bug 的根因（作者反复修"虚影/闪退/闭包重建"）

1. **闭包重建**：`bindSwipe` 的 touch 闭包捕获了 `$item`、`v`（行 View）等，当 `notifyDataSetChanged` 触发时，ListView 会**重建行 View 并重新绑定闭包**，但事件序列（DOWN→MOVE→UP）可能跨越重建，导致 `mode/downX/startTx` 状态丢失 → "虚影"（选中态渲染与状态不同步）、"闪退"（`v` 已被回收但闭包仍引用）。
2. **`paneOfRow` 在 bind 时不可判定**：行 View 绑定时尚未 attach，必须到事件发生时才能用 `getPositionForView` 判定属于哪个窗格——代码里已有 `paneOfRow` 注释说明，但历史版本曾用"第一个前缀匹配"，在左右同目录/父子目录时命中错误窗格 → "点按无反应"。
3. **点按=执行 vs 长按=菜单 vs 左滑=单条操作 vs 多选=切换选中** 四种手势挤在一个状态机里，`mode` 的迁移条件（`slop` 阈值、`dx>dy*1.2` 判定左滑）边界脆弱，任何一步误判就串手势。

### 3.3 改进方向（与子智能体交互规格对齐）
- 把手势逻辑拆成**独立的手势控制器**（不在 bindSwipe 里堆），用 `GestureDetector` + `ViewConfiguration.getScaledTouchSlop()` 规范化；
- 明确状态机：`idle → (长按) selecting → (拖动) rangeSelect`，`idle → (横滑) swipeReveal`，`idle → (点按) click`，互斥且可取消；
- 多选用 `ActionMode`（系统 Contextual Action Bar）替代自绘 `multiBar`，避免自绘状态与 ListView 重建不同步。

---

## 四、队列添加 / 执行联动 / Root 检测的现状问题

### 4.1 队列添加"混乱"
- `QueueFragment.addFlow()` 弹"浏览文件/从链接导入/手动输入"三选一，但：
  - 文件页 pick 模式点脚本直接入队，**没有"入队后给什么反馈"**；
  - 从链接导入走 `MainActivity.downloadTo`，成功后 `queue.addScriptByPath` 自动入队并切队列页，但**失败静默**；
  - 手动输入路径无校验、无重复检测。
- 缺"队列项去重""入队成功提示""执行后队列状态"的清晰反馈。

### 4.2 执行中页面联动差
- 从文件页"直接执行"（`executeFile`）走 `QueueFragment` 的 `tryStartQueue`，但**执行时没有自动切到队列页/终端页**，用户看不到进度；
- 执行状态（runningNow）只在队列页有 UI，文件页/方案库页无任何"正在执行"的全局提示；
- 方案库"运行方案"会切队列页，但**文件页直接执行不会**——两条路径行为不一致。

### 4.3 Root 检测
- `checkRoot()` 已有 3 次重试 + `rootCacheAt` 15s 短期失效（这版已修复历史"rootCache 永不失效"缺陷，见 `Engine.java:257-308`、`rootAvailable():444-459`）。
- 但 `refreshRoot()` 只在冷启动/回前台/手动"重新检测"时调用，**文件页无 root 时降级走 `sh` 的行为与队列页 `su` 失败提示不一致**。

---

## 五、God object 现状（需拆分的证据）

| 文件 | 行数 | 职责过载 |
|---|---|---|
| `Engine.java` | 1347 | root/执行/解码/ANSI/队列调度/录制/看门狗/收尾 全在一个类 |
| `FilesFragment.java` | 3380 | 双窗/文件操作/APK安装/分享/多选/手势/菜单 全在一个 Fragment |
| `QueueFragment.java` | 1678 | 队列/终端渲染/输入/门控/折叠/摘要 全在一个 Fragment |

依赖关系混乱：UI 层直接调 `Engine.INSTANCE` 静态单例，`Engine` 又持 `appCtx`、`scripts`、`presets`、`termBuffer` 全局状态，无分层、无接口隔离，改动一处牵动全局——这正是"走一步看一步"导致的"开发困难和混乱"的代码层面体现。
