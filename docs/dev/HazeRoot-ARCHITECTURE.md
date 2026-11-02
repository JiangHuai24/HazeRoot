# HazeRoot 架构设计文档（ARCHITECTURE.md）

> 版本：v1.0 · 角色：架构师（Architect）· 状态：待评审
> 依据：工程化重构方案 + 交互改造规格 + PRD FR-9/FR-1/FR-3/FR-5
> 目标：给出「怎么组织 + 边界在哪 + 依赖方向 + 关键状态机」，作为拆任务与编码的施工图。

---

## 0. 设计原则（不可违背）

1. **不推倒重来**：绞杀者（Strangler Fig）+ 按抽象分支（Branch by Abstraction）+ Feature Toggle，小步、可编译、可回滚。
2. **单向依赖**：`ui → domain → data`，`core` 独立，禁止反向。`domain` 禁止 import `android.*`。
3. **单类 ≤300 行、单方法 ≤40 行**（Checkstyle 硬门禁）。
4. **接缝优先**：所有外部系统/平台能力留接口（`SuGateway`/`ProcessExecutor`/`FileSystemGateway`）。
5. **状态机显式化**：执行状态用 FSM，禁止 boolean 标志互相打架。
6. **终端数据单向流动**：进程 → 解码 → ANSI → 环形缓冲 → UI，UI 只读缓冲不反向写。

---

## 1. 分层与依赖方向

```
com.hazeroot
├── app/          # 应用壳 + DI 根
├── ui/           # 表现层（只渲染 + 转发事件）
├── domain/       # 领域层（纯 Java，禁 android.*）
├── data/         # 数据层（实现 repository 接口）
└── core/         # 执行引擎（独立，禁依赖上层）
```

依赖方向（ArchUnit 强制）：
```
ui ──► domain ──► data
  │        │
  └────────┼──────► core
           │
core 不依赖任何上层；domain 依赖 repository 接口，不依赖 data 实现
```

---

## 2. 核心领域对象（domain/model）

| 对象 | 职责 | 关键字段 |
|---|---|---|
| `Script` | 脚本单元 | path, name, args, workDir, needRoot, source |
| `QueueItem` | 队列条目 | id, script, order, state, answerRules |
| `Preset` | 方案 | name, scripts, answerRules, targetApp, createdAt |
| `AnswerRule` | 应答规则 | matchPattern, order, answer, repeatable |
| `ExecutionSession` | 执行会话 | id, processHandle, io, startAt, endAt, state |
| `TerminalBuffer` | 终端环形缓冲 | rows, capacity, append, truncate, diff |
| `FileEntry` | 文件条目 | path, name, type, size, mtime, executable |
| `ExecutionState` | 状态枚举 | 见 §5 |

---

## 3. 核心接口（接缝 Seam）

### core/su
```java
public interface SuGateway {
    /** 提权执行，返回 stdout；非0退出返回 null */
    String su(String cmd);
    byte[] suBytes(String cmd);
    boolean checkRoot();
    boolean refreshRoot();
}
public interface ProcessExecutor {
    Process start(String[] cmd);
}
```

### domain/repository
```java
public interface QueueRepository {
    boolean enqueue(Script script, Map<String,String> args);   // 唯一入队入口
    List<QueueItem> list();
    void remove(String id);
    void clear();
}
public interface PresetRepository { /* CRUD + expandToQueue */ }
public interface RecordingRepository { /* 录制落盘/读取 */ }
```

### domain/usecase
```java
public interface ExecutionCoordinator {
    void start(List<QueueItem> items, boolean auto, String targetApp);
    void stop();
    void sendInput(String text);
    ExecutionState currentState();
}
```

---

## 4. 终端渲染数据流（FR-1，单向）

```
su 进程 stdout/stderr (byte[])
   │
   ▼
Utf8Decoder（字节流式，跨 chunk 保持 mUtf8ToFollow/mUtf8Index）
   │  (int codePoint)
   ▼
AnsiParser（ESC/CSI/OSC 状态机 → 屏幕操作 + 样式）
   │  写字符/换行/清屏/设样式
   ▼
TerminalBuffer（TerminalRow[] 环形缓冲，行级存储 + 脏行标记）
   │  行号映射 externalToInternalRow()
   ▼
TerminalRenderer（可见行全量重绘 + 行内 run 合批 drawText）
   │
   ▼
TerminalView（吸底/上翻状态机 mTopRow/mAutoScroll）
```

**关键决策**（对齐 Termux 实践，见交互规格 A 节）：
- 解码：**字节流跨块状态机**，禁止按 chunk `new String` 重解码；
- 渲染：**行级脏标记 + 可见行全量重绘**，禁止逐字符脏渲染（历史 bug 主源）；
- 滚动：显式「吸底/上翻」状态机；
- 裁剪：按**行边界**裁剪，绝不断字符。

---

## 5. 执行状态机（FSM，FR-9 AC9.3）

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
          │  prompt 命中   │ prompt 未命中   │ input 到达
          │        ┌──────▼───────┐       │
          │        │ WAITING_INPUT│───────┘
          │        │ （等待输入）  │
          │        └──────────────┘
          │  进程退出/看门狗
          ├──────────────► COMPLETED（exit0）
          ├──────────────► STOPPED（用户停止）
          └──────────────► FAILED（非0/异常）
```

**约束**：
- 状态只由 `ExecutionCoordinator` 推进，UI 只读状态渲染；
- 非法迁移（RUNNING→PENDING）抛异常/记日志；
- `WAITING_INPUT` 是 RUNNING 的子态，不另立标志。

---

## 6. 页面联动（FR-5）

```
                    ┌──────────────────────────┐
                    │   AppEventBus（单例）      │
                    │  QUEUE_ITEM_STARTED       │
                    │  QUEUE_ITEM_FINISHED      │
                    │  QUEUE_CHANGED            │
                    │  EXEC_PROGRESS            │
                    │  ROOT_STATUS_CHANGED      │
                    └───────┬──────────────────┘
                            │ 订阅/发布
        ┌──────────┬────────┼─────────┬──────────┐
        │ QueueFragment │ FilesFragment │ PresetsFragment │ MainActivity │
```

**约束**：跨页通信走 `AppEventBus` + 导航抽象，禁止 Fragment 直接引用 + 静态可变状态传参。

---

## 7. 手势状态机（FR-3）

四态：`NORMAL / MULTI / ACTION_MENU / DRAG_SCROLL`（完整转移表见交互规格 C.1）。

**三条铁律**（防虚影/闪退）：
1. 事件用 `MotionEvent` 坐标快照，不捕获 RecyclerView row/view 引用；
2. 点按/长按统一在 `onTouch` 内基于坐标判定，不复用「点击监听+长按监听」双回调；
3. `GestureController` 随 Adapter 生命周期只建一次。

---

## 8. 模块拆分映射（M2 目标）

### Engine.java（9 帽 → core 8 类 + domain 1 用例）

| 原职责 | 目标类 | 包 |
|---|---|---|
| root 探测 | `RootProbe` | core/su |
| su 提权执行 | `SuGateway` + `SuGatewayImpl` | core/su |
| 进程执行 | `ProcessExecutor` | core/su |
| 队列调度 | `ExecutionQueue` | core/queue |
| 输出读取 | `ChunkReader` | core/io |
| 两级解码 | `CharsetDetector`（含 Utf8Decoder + GBK 回退） | core/io |
| ANSI 状态机 | `AnsiParser` | core/io |
| 提示识别 | `PromptDetector` | core/prompt |
| 自动应答 | `AnswerRuleMatcher` | core/prompt |
| 录制 | `SessionRecorder` | core/recorder |
| 看门狗 | `Watchdog` | core/watchdog |
| 执行协调 | `ExecutionCoordinator`（use case） | domain/usecase |

### QueueFragment.java（4 帽）
- 队列面板 → `QueueFragment`（薄组装）
- 终端渲染 → `TerminalView` + `TerminalRenderer` + `TerminalBuffer`(core/io)
- 手动输入 → `InputController`
- 执行门控 → `ExecutionGate`（读 ExecutionState）

### FilesFragment.java（6 帽）
- 双窗 UI → `DualPaneView` + `FilesFragment`（薄）
- 列表渲染 → `FileAdapter`
- 文件操作 → `FileOperationService`（use case）
- APK 安装 → `ApkInstaller`（data）
- 分享 → `ShareController`（data）
- 多选手势 → `SelectionController` + `GestureController`（ui/files）

---

## 9. 里程碑与依赖（施工顺序）

| 里程碑 | 主题 | 依赖 | 验收 |
|---|---|---|---|
| M0 | 工程地基（Checkstyle/Lint/CI/测试） | — | CI 绿，>300 行新类被拦 |
| M1 | 插接缝 + 特性测试锁行为 | M0 | ≥20 单测，覆盖率 ≥50% |
| M2 | 拆 Engine → core 8 模块 | M1 | Engine <300 行，ArchUnit 0 违反 |
| M3 | 终端渲染专项 | M2 | FR-1 AC 全过 |
| M4 | 文件交互（手势/长名/MT 对齐） | M2（可与 M3 并行） | FR-2/3/6 AC 全过 |
| M5 | 页面联动 | M2 | FR-5 AC 全过 |
| M6 | 队列/方案/录制闭环 | M5 | FR-4 AC 全过 |
| M7 | UI 换肤 + 收尾 | 作者确认 D1–D3 后 | FR-7 AC 全过 |

---

## 10. 风险与回滚

| 里程碑 | 风险 | 缓解 | 回滚 |
|---|---|---|---|
| M2 拆 Engine | 顺序/竞态回归 | Feature Toggle 双轨 + 特性测试 | Toggle 切回 LegacyEngine |
| M3 终端渲染 | 性能/正确性权衡 | Benchmark 纳入 CI | Toggle 回退旧渲染 |
| M4 文件交互 | 手势回归面大 | Espresso 手势测试 + 交互基线走查 | 交互改动独立提交逐项 revert |
| M5 页面联动 | 生命周期/泄漏 | 导航抽象 + 泄漏检查 | 导航抽象切回直连 |

---

## 11. 已固化决策（无需再讨论）

1. 分层 `ui/domain/data/core` 单向依赖（ArchUnit 门禁）。
2. 单类 ≤300 行、单方法 ≤40 行（Checkstyle 门禁）。
3. 执行状态 FSM 六态显式化。
4. 入队唯一入口 `QueueRepository.enqueue()`。
5. 终端渲染对齐 Termux 实践（字节流解码 + 行级脏标记 + 吸底状态机）。
6. 手势四态状态机 + 坐标快照（不捕获 row 引用）。
7. 版本管理三层解耦（commit 随意/push 频繁/tag 低频）。

---

*本文档是「科学完整开发流程」第 [2] 步产出。下一步：架构师 + PM 拆解任务卡（依赖/AC/DoD/回滚），然后进入开发。*
