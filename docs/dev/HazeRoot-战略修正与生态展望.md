# HazeRoot 产品战略修正与生态展望

> 版本：v1.0 · 角色：产品经理（PM）+ 架构师（Architect）
> 起因：作者澄清「希望在保持专业度的同时，具备通用性，未来会围绕 HazeRoot 衍生出：临时 root 提权 App、一键 root 的 PC 端工具、手机内工具联动，以降低这一领域门槛」。
> 影响：本文件修正 PRD 定位与 ARCHITECTURE 边界，作为后续所有设计的**战略前提**。

---

## 0. 核心修正：HazeRoot 不是「单点 App」，而是「root 工具生态的核心枢纽」

### 0.1 原理解（已修正）

原 PRD 把 HazeRoot 定位为「通用 root 脚本执行器 + 文件管理」，是**单点产品**思维。这没错，但太窄——它没有回答「为什么是 HazeRoot 来做这件事，而不是别的工具」。

### 0.2 修正后的定位

**HazeRoot = root 工具生态的「控制台/枢纽」**，未来生态由三层组成：

| 层 | 形态 | 职责 | 与 HazeRoot 的关系 |
|---|---|---|---|
| **核心层（已存在）** | HazeRoot Android App | root 脚本执行 + 文件管理 + 终端 + 方案重放 | 生态的中枢，一切能力的「调度台」 |
| **能力层（衍生 1）** | 临时 root 提权 App | 一次性/临时提权（降低「永久 root」门槛与风险） | 从 HazeRoot 发起，作为「执行前置能力」 |
| **工具层（衍生 2）** | 一键 root PC 端工具 | 在 PC 上引导用户完成 root 流程（解锁/刷入/注入） | 作为 HazeRoot 的「装机/引导入口」，降低入门门槛 |
| **联动层（衍生 3）** | 手机内工具联动 | HazeRoot 与临时提权、PC 工具之间的状态/命令互通 | 通过统一的「设备能力协议」互联 |

**一句话战略**：HazeRoot 是「root 用户的操作系统」，其它都是围绕它的「能力插件」和「引导入口」。

---

## 1. 战略修正带来的三个设计原则（必须写进架构）

### 原则 A：能力可插拔（Plugin / Capability 化）

HazeRoot 的核心引擎**不硬编码「root 从哪来」**，而是抽象出 `RootCapability` 接口：

```java
public interface RootCapability {
    boolean isAvailable();          // 该能力是否就绪
    boolean acquire();              // 获取/发起该能力（如跳转临时提权 App）
    String  providerId();           // 能力提供者标识
    int     priority();             // 多能力共存时的优先级
}
```

- 现在实现：`SuRootCapability`（Magisk/KernelSU/APatch 的 `su`）
- 未来实现：`TempRootCapability`（临时提权 App）、`PcRootCapability`（PC 工具引导）

**这样，「临时 root 提权 App」「一键 root PC 工具」都不是另起炉灶，而是 HazeRoot 的 `RootCapability` 插件。**

### 原则 B：设备能力协议（Device Capability Protocol）

跨「App ↔ PC 工具」的联动，需要一套**稳定的、版本化的协议**，而不是各自私有的 adb 命令堆砌。

```
┌─────────────────────────────────────────────────┐
│        HazeRoot 设备能力协议（Capability Protocol） │
├─────────────────────────────────────────────────┤
│  命令：acquire_root / release_root / exec / probe │
│  状态：root_ready / root_pending / root_failed    │
│  传输：ADB 通道 / 本地 IPC / 网络（可扩展）          │
└─────────────────────────────────────────────────┘
```

- 好处：PC 工具、临时提权 App、HazeRoot 三者用同一套语义对话，降低联动成本。

### 原则 C：降低门槛 = 引导式体验，不是阉割专业度

「降低门槛」不等于「砍功能」，而是：
1. **专业用户**：保留完整 root 脚本执行、终端、文件管理能力（不缩水）；
2. **新用户**：增加「引导式入口」——检测到未 root 时，不再是冷冰冰的「无 root」，而是「一键 root 引导 → 临时提权 → 永久 root 三选一」的清晰路径；
3. **PC 工具**：作为「第一次 root 的引导」出现，让不懂 adb 的用户也能 root。

---

## 2. 对 PRD 的具体修正

### 2.1 新增需求

| 需求编号 | 需求名 | 优先级 |
|---|---|---|
| FR-10 | `RootCapability` 抽象层（能力可插拔） | P1（架构前置） |
| FR-11 | 未 root 时的「引导式 root 路径」（三选一：永久/临时/PC 引导） | P2 |
| FR-12 | 设备能力协议 v0（App↔PC 联动语义定义） | P2 |

### 2.2 修正后的核心领域对象（新增）

| 对象 | 职责 |
|---|---|
| `RootCapability` | 抽象 root 能力来源（su / 临时 / PC） |
| `CapabilityRegistry` | 注册、探测、优先级排序、切换 root 能力 |
| `DeviceLink` | 与 PC 工具/临时提权 App 的联动会话 |

### 2.3 修正后的 Root 状态

从原来的「boolean root 有无」升级为「能力视图」：

```
RootCapability 状态：
  NONE        —— 无任何能力
  SU_READY    —— su 可用（永久 root）
  TEMP_READY  —— 临时提权可用
  PC_GUIDED   —— 通过 PC 工具引导中
  PENDING     —— 能力获取中
```

---

## 3. 对 ARCHITECTURE 的修正

### 3.1 分层不变，`core` 新增 `capability` 模块

```
core/
├── su/            # SuGateway（现有 su 能力）
├── capability/    # RootCapability + CapabilityRegistry（新增）
├── io/            # 解码/ANSI
├── prompt/        # 提示/应答
├── queue/         # 队列
├── session/       # 会话状态机
├── recorder/      # 录制
└── watchdog/      # 看门狗
```

`core/capability` 依赖 `core/su`，但 `domain` 只依赖 `RootCapability` 接口（不感知具体是 su 还是临时提权）。

### 3.2 未来生态的模块形态（前瞻，不立即实现）

```
HazeRoot（核心 App）
TempRoot App（独立 App，实现同一 Capability Protocol 的 provider 端）
PC Root Tool（桌面工具，实现 Capability Protocol 的 host 端）
```

三者**共享协议定义**（一个 git 子模块 / 独立 protocol 模块），避免三份协议漂移。

---

## 4. 本轮迭代的边界（不扩大范围）

**本轮（M0–M7）只做核心层**，但**架构上预留接缝**：
- `RootCapability` 接口现在就定义（FR-10），哪怕先只有 `SuRootCapability` 一个实现；
- 未 root 引导（FR-11）和 PC 协议（FR-12）**本轮不实现**，但接口和状态机预留，避免未来「推倒重来」。

**这样既守住了「本轮聚焦 HazeRoot 本身」，又兑现了「为生态衍生留好地基」的战略意图。**

---

## 5. 待作者确认的新决策点

| # | 决策 | 建议 | 状态 |
|---|---|---|---|
| D6 | 本轮是否先落地 `RootCapability` 抽象层（FR-10） | 建议是——成本低（接口+注册表），却是生态衍生的地基 | ⏳ 待确认 |
| D7 | 「引导式 root」是否纳入本轮 UI（FR-11） | 建议本轮只做「能力探测 + 状态展示」，完整引导流程下轮 | ⏳ 待确认 |
| D8 | 设备能力协议是否本轮定义 v0 草案 | 建议只写文档草案，不编码 | ⏳ 待确认 |

---

*本文件是对 PRD/ARCHITECTURE 的战略级修正，优先级高于前两者中「单点产品」的表述。后续 PRD v1.1 与 ARCHITECTURE v1.1 将据此合并修订。*
