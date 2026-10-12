# HazeRoot — 完整功能与底层实现逻辑

> 作者：**HazeRoot**  
> 文档对象：本仓库（HazeRoot 工程）  
> 当前版本：v2.8.0 · versionCode 66 · applicationId `com.hazeroot.scriptrunner`  
> 项目定位：**通用 root 脚本执行器**（方向类比 MT 管理器）  
> 源码来源：本工程早期源码丢失，基于自有的 v2.7.2 版本 APK 复原（apktool + jadx），部分方法按 smali 手工回写  
> 文档范围：全部 30 个 Java 源文件 + AndroidManifest + 资源 + 构建配置

---

## 目录

1. [项目定位与技术栈](#1-项目定位与技术栈)
2. [工程结构与构建配置](#2-工程结构与构建配置)
3. [整体架构与数据流](#3-整体架构与数据流)
4. [数据模型与持久化](#4-数据模型与持久化)
5. [核心引擎 Engine（重点）](#5-核心引擎-engine重点)
6. [UI 层逐模块实现](#6-ui-层逐模块实现)
7. [支撑组件层](#7-支撑组件层)
8. [资源、权限与配置](#8-资源权限与配置)
9. [关键实现细节与坑位清单](#9-关键实现细节与坑位清单)
10. [已知问题与后续方向](#10-已知问题与后续方向)
11. [附录：文件清单与职责速查](#11-附录文件清单与职责速查)

---

## 1. 项目定位与技术栈

### 1.1 一句话定位

**HazeRoot 是一个"脚本执行器 + 一键方案重放器"**：把 root 脚本（shell 脚本或 ELF 可执行文件）按队列顺序执行，自动应答脚本的交互提问，并把整条队列连同"回答记录"存成"方案"，之后一键重放。

它的目标用户是需要反复在 root 环境跑同一套脚本的人，核心价值是**把"手动敲一堆回答"变成"点一下跑完"**。

### 1.2 技术栈

| 项                               | 值                                                                                                                                                                                  |
| ------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 语言                              | Java 17（源码由 Kotlin 复原而来，保留了 Kotlin 的调用约定）                                                                                                                                          |
| 构建                              | Gradle 8.6 + AGP（Kotlin DSL：`build.gradle.kts`）                                                                                                                                    |
| minSdk / targetSdk / compileSdk | 26 / 34 / 34                                                                                                                                                                       |
| UI                              | AppCompat + Material 1.11 + ConstraintLayout 2.0.1（**无 Compose**）                                                                                                                  |
| 依赖                              | `androidx.appcompat:1.6.1`、`androidx.core:1.12.0`、`com.google.android.material:1.11.0`、`androidx.constraintlayout:2.0.1`、`kotlin-stdlib:1.9.22`、`kotlinx-coroutines-android:1.6.4` |
| 架构                              | 单 Activity + 3 Fragment（Queue / Files / Presets）+ 4 个独立 Activity（Splash / Editor / Media / Manual）+ 1 前台 Service                                                                   |
| 状态管理                            | **全局单例 Engine**（静态字段 + volatile + Handler 主线程回调），无 ViewModel / LiveData                                                                                                            |
| 权限模型                            | 完全依赖外部 `su`（Magisk / KernelSU / APatch 任一）                                                                                                                                         |



> 注意：`kotlin-stdlib` 虽在依赖里，但重建后的代码基本不调用 Kotlin 运行时的复杂特性；缺失的 Kotlin 合成方法（`$default` 系列）由手写类 `Kt` 补齐。

---

## 2. 工程结构与构建配置

### 2.1 目录树（精简）

```
RootScriptRunner/
├── settings.gradle.kts / build.gradle.kts / gradle.properties / local.properties
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── assets/manual.html              ← 使用手册（1.8 MB，WebView 加载）
        ├── java/com/hazeroot/scriptrunner/     ← 30 个 Java 文件
        └── res/
            ├── layout/     9 个布局
            ├── menu/       3 个菜单
            ├── xml/file_paths.xml          ← FileProvider 路径映射
            ├── values/     自研字符串 / 颜色 / 样式 / id
            ├── values-night/ 夜间配色
            ├── values-zh-rCN/ 等 100+ 语言目录（反编译残留，实际只含 Material 库字符串）
            └── drawable*/  图标与背景（含 dot_* 状态点、ic_brand_h 品牌图标）
```

### 2.2 构建要点（`app/build.gradle.kts`）

```kotlin
android {
    namespace = "com.hazeroot.scriptrunner"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.hazeroot.scriptrunner"
        minSdk = 26; targetSdk = 34
        versionCode = 66; versionName = "2.8.0"
    }
    compileOptions { sourceCompatibility = VERSION_17; targetCompatibility = VERSION_17 }
    packaging { resources.excludes += setOf("META-INF/*.kotlin_module") }
}
// 重建期放开 javac 报错上限，便于一次性看到全部编译错误
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "2000", "-Xmaxwarns", "2000"))
}
```

**重建期的编译命令**（本机实测可用）：

```bash
export JAVA_HOME="C:\Program Files\Java\jdk-21"
MSYS_NO_PATHCONV=1 <gradle-8.6>/bin/gradle assembleDebug --offline
```

### 2.3 关键常量

| 常量                                    | 位置             | 值                    | 含义                |
| ------------------------------------- | -------------- | -------------------- | ----------------- |
| `SCRIPT_DIR`                          | `MainActivity` | `/data/adb/HazeRoot` | 脚本默认存放/下载目录       |
| `TAG`                                 | `Engine`       | `"ScriptRunner"`     | 日志标签              |
| `IDLE_MS`                             | `Engine`       | `30000`              | 静默判定/看门狗阈值        |
| `MAX_TERM_CHARS`                      | `Engine`       | `200000`             | 终端环形缓冲上限          |
| `MARK`                                | `Engine`       | `1`                  | 终端样式标记字符 `\u0001` |
| `S_CMD/S_ERR/S_DIM/S_WARN/S_ANSI_DEF` | `Engine`       | `c/e/d/w/n`          | 终端行样式码            |

---

## 3. 整体架构与数据流

### 3.1 分层

```
┌─────────────────────────── UI 层 ───────────────────────────┐
│ SplashActivity ──→ MainActivity ──┬── QueueFragment  (队列 + 终端)│
│  (ConvergeSplashView 动画)         ├── FilesFragment  (双窗文件管理)│
│                                   └── PresetsFragment(方案库)    │
│ EditorActivity · MediaActivity · ManualActivity                 │
└──────────────────────────┬──────────────────────────────────┘
                           │ 直接调用（静态单例）
┌──────────────────────────▼──────────────────────────────────┐
│                      Engine（全局单例）                        │
│  Root 探测 · 提权执行 · 队列调度 · 输出解码 · ANSI 解析          │
│  提示识别 · 自动应答 · 录制 · 中止 · 看门狗                      │
│  Listener 事件总线：emit("log"|"state"|"prompt"|"done")        │
└──────────────────────────┬──────────────────────────────────┘
                           │
              ┌────────────▼────────────┐   ┌──────────────────┐
              │  RunnerService(前台服务) │   │ Store/Json(持久化)│
              │  通知栏状态同步          │   │ SharedPreferences│
              └─────────────────────────┘   └──────────────────┘
                           │
                    ┌──────▼──────┐
                    │  su (外部)   │  Magisk / KernelSU / APatch
                    └─────────────┘
```

### 3.2 一次"点执行"的完整数据流

```
用户点「按顺序执行」
  → QueueFragment.tryStartQueue()
      → withGates()            ① 后台 checkRoot()
                               ② 主线程：rootOk? 通知权限? 
                                   └ 缺 root → showNeedRootDialog()
                                   └ 缺通知 → showNeedNotificationDialog()
                                   └ 都 OK → onPass()
  → doStartQueue()
      → setCollapsed(true)             收起队列面板给终端腾空间
      → RunnerService.start(ctx)       起前台服务（防杀 + 通知）
      → Engine.log("")                 终端插入空行
      → Engine.runQueue(scripts, false, null)
          → 新线程（daemon）逐条执行：
              for each script:
                  probe 文件 → 判断 ELF/shell → 组装命令
                  → ProcessBuilder("su","-c", ...) 
                  → 读输出流 → 解码 → ANSI 解析 → 累积 partial
                  → 遇 \n 落盘为一行日志
                  → 识别提示行 → emit("prompt", ...) → 自动/手动应答
                  → 结束后取退出码（子进程 exit 或 __HR_EXIT_n__ 探针）
                  → 落盘 Store.saveScripts
      → QueueFragment 通过 Engine.Listener 收到 "log"/"state" 事件
          → syncTerm() 把 termBuffer 增量渲染到 TextView（带 ANSI 颜色）
```

### 3.3 事件总线

`Engine` 通过 `Listener` 接口向 UI 广播，**回调统一切到主线程**（`Handler(Looper.getMainLooper())`）：

| type     | payload            | 触发时机                        |
| -------- | ------------------ | --------------------------- |
| `log`    | 一行的纯文本（去掉样式标记）     | 每输出一行 / 引擎自身提示              |
| `state`  | `""`               | 脚本开始/结束、队列开始/结束（触发按钮与徽标刷新）  |
| `prompt` | 当前累积的提示文本          | 提示行内容发生变化                   |
| `done`   | "队列执行完毕" / "已中止执行" | 整条队列结束（RunnerService 据此停服务） |

`addListener/removeListener`，底层是 `CopyOnWriteArrayList`。

---

## 4. 数据模型与持久化

### 4.1 `ScriptItem`（队列项）

```java
public final class ScriptItem {
    private String name;                     // 显示名
    private String path;                     // 绝对路径
    private List<RecordedInput> recordedInputs;  // 录制的问答对
    private transient boolean done;          // 本次是否已跑完（不持久化）
    private transient boolean runningNow;    // 是否正在运行（不持久化）
    private transient Integer exitCode;      // 退出码，null=未跑（不持久化）
}
```

**设计要点**：`done / runningNow / exitCode` 三个字段是 `transient`，**只存在于内存**——即重启 App 后队列会重置为"待执行"状态（脚本列表本身持久化，运行状态不持久化）。

另有 Kotlin data class 的 `copy()` / `copy$default()` / `component3()` / `equals` / `hashCode` 全套合成方法（反编译残留）。

### 4.2 `RecordedInput`（问答对）

```java
public final class RecordedInput {
    private String prompt;   // 当时的提示文本
    private String value;    // 用户输入的回答
}
```

### 4.3 `Preset`（方案）

```java
public final class Preset {
    private String name;                 // 方案名
    private List<ScriptItem> scripts;    // 方案内的脚本队列（含各自录制的回答）
    private String targetApp;            // 目标应用包名（跑完自动跳转）
}
```

### 4.4 持久化 `Store` + `Json`

- **存储介质**：`SharedPreferences("runner_store")`，两个 key：`scripts` / `presets`，值均为 JSON 字符串。
- **序列化**：`Json` 单例，用 `org.json` 手写读写，无 Gson/Moshi。

```json
// scripts 的 JSON 形态
[{"name":"xxx.sh","path":"/data/adb/HazeRoot/xxx.sh",
  "rec":[{"prompt":"请您输入 0/1/2","value":"2"}]}]

// presets 的 JSON 形态
[{"name":"我的日常启动","targetApp":"com.example.app",
  "scripts":[ ...同上... ]}]
```

- **容错**：`scriptsFromJson` / `presetsFromJson` 整体包在 `try/catch(Exception)` 中，**解析失败静默返回空列表**（不崩溃、不报错）。
- **深拷贝**：`Json.deepcopy(s)` = `scriptFromJson(scriptToJson(s))`，用于"方案 → 队列"时避免共享引用。
- **默认数据**（v2.8.0-rename 起）：`defaultScripts()` 与 `defaultPresets()` **均返回空列表**。
  > 历史背景：早期版本这两个方法预置了若干示例脚本名与专用目录。去品牌化时已全部清空，首次启动走空状态引导（`暂无脚本 / 添加一个脚本路径，开始执行队列`）。`defaultPresets()` 在早期版本中**无任何调用点**（死代码），`defaultScripts()` 仅被 `QueueFragment:129` 首次启动调用。

**首次启动逻辑**（`QueueFragment.onViewCreated`）：

```
Engine.scripts 为空？
  ├─ Store 里有 scripts 键 → 读出来装进 Engine
  └─ 没有（真首次）        → 装 defaultScripts() 并立刻写回 Store
```

---

## 5. 核心引擎 Engine（重点）

`Engine` 是整个 App 的心脏，**单例 + 全静态状态**。文件 1249 行。

### 5.1 静态状态全景

```java
// 队列与方案
private static final List<ScriptItem> scripts  = new ArrayList<>();
private static final List<Preset>     presets  = new ArrayList<>();

// 运行态
private static volatile boolean runningNow;      // 是否有队列在跑
private static volatile boolean abortFlag;       // 中止标志
private static java.lang.Process currentProc;    // 当前子进程（供 abort 用）
private static volatile boolean finalized;       // 方案收尾是否已执行
private static volatile boolean hadAutoInput;    // 本次是否发生过自动应答
private static volatile long lastOutputAt;       // 最后一次输出时间（看门狗）

// 缓存
private static volatile Boolean rootCache;         // root 探测结果缓存
private static volatile Boolean busyboxScriptOk;   // busybox script 可用性缓存

// 终端
private static final StringBuilder termBuffer = new StringBuilder();  // 环形缓冲
private static volatile String currentPartial   = "";   // 当前未换行的行
private static volatile String lastFullLine     = "";   // 上一整行
private static volatile String lastPromptText   = "";   // 最近一次提示

// 录制
private static Map<String, List<String>> recordingCollect;  // prompt → [answers]
private static final Object recordLock = new Object();

// 基础设施
private static Context appCtx;
private static final CopyOnWriteArrayList<Listener> listeners;
private static final Handler main = new Handler(Looper.getMainLooper());
private static volatile String presetTargetApp = "";
```

### 5.2 Root 能力层

#### `checkRoot()` — 是否已授权

```java
Process p = new ProcessBuilder("su","-c","id").redirectErrorStream(true).start();
p.waitFor();
return out.contains("uid=0");
```

输出含 `uid=0` 即判定成功（实际上 `id` 输出形如 `uid=0(root) gid=0(root) ...`）。

#### `rootAvailable()` — 带缓存的门面

```java
if (rootCache != null) return rootCache;
boolean ok = checkRoot();
rootCache = ok;
return ok;
```

**注意**：进程内只探测一次，用户中途去授权后需靠 `QueueFragment` 的"重新检测"走 `checkRoot()` 直连（绕过缓存）。


#### `su(cmd)` — 最重要的一处提权封装 ⚠️

```java
// 注意：部分 su 实现（如 KernelSU）对 "su -c 'a; b'" 只给第一条命令提权，
// 后续命令会降回 shell(uid 2000)，导致 cp/tar 等以非 root 身份执行。
// 用 sh -c 显式包裹，保证整条复合命令在同一个 root shell 内运行。
Process p = new ProcessBuilder("su", "-c", "sh -c " + q(cmd))
                    .redirectErrorStream(true).start();
```
- 成功（exit 0）返回 stdout；非 0 返回 `null`。
- `q(s)` 是**单引号安全转义**：`"'" + s.replace("'", "'\\''") + "'"`。

**为什么必须这样写**（实测证据，KernelSU）：

| 命令 | `whoami` / `id -u` |
|---|---|
| `su -c "whoami; id -u"` | root / **2000** ✗ |
| `su -c "whoami && id -u"` | root / **2000** ✗ |
| `su -c "sh -c 'whoami; id -u'"` | root / **0** ✓ |

这是**早期版本就存在的缺陷**（Magisk 行为不同所以没暴露），重建时被修复。

#### `exec(cmd)` / `execBytes(cmd)` — 通用执行

```java
exec(cmd):
    rootAvailable() ? su(cmd)                        // 走 su -c "sh -c '...'"
                    : ProcessBuilder("sh","-c",cmd)  // 无 root 降级
    → exit 0 返回 stdout(UTF-8 String)，否则 null

execBytes(cmd):  同上但返回 byte[]（用于二进制读取，不做字符集假设）
```

#### `isElf(path)` — 判 ELF

```java
exec("head -c 4 " + q(path) + " | od -An -tx1")
→ 去空格、去换行、转小写 → contains("7f454c46")
```
`7f 45 4c 46` 是 ELF 魔数。**注意**：`runOne` 内部并不调用这个方法，而是自己重新探测了一遍（因为要同时判断"文件是否存在"）。

#### `detectRootManager()` — 识别用户装的是哪个 root 管理器

两段式：
1. **按 root 实现关键字优先**：`su -c id` 输出里含 `KSU` → 依次试 `me.weishu.kernelsu / com.rifsxd.ksunext / com.sukisu.ultra / com.twj.wksu`；含 `APATCH` → `me.bmax.apatch`。
2. **全量包名表 + 标签模糊匹配**：先用 `getLaunchIntentForPackage` 逐个试候选包名，命中即返回 `(pkg, label)`；都不中再枚举所有 LAUNCHER 应用，用标签关键字表匹配：

```
magisk→com.topjohnwu.magisk   kitsune/德尔塔→io.github.huskydg.magisk
sukisu/杂鱼→com.sukisu.ultra   wild ksu/wksu→com.twj.wksu
kernelsu/ksu→me.weishu.kernelsu   apatch→(探测所得)   面具→com.topjohnwu.magisk
supersu→eu.chainfire.supersu   kingroot→com.kingroot.Rush
```

用于在"未授权 Root"对话框里给一个"打开管理器"按钮。

### 5.3 队列调度 `runQueue(list, auto, targetApp)`

```java
runQueue(list, auto, targetApp):
    if (runningNow) { log("runQueue 被忽略：已有任务在执行"); return; }   // 重入保护
    abortFlag = false; runningNow = true;
    presetTargetApp = auto ? targetApp.trim() : "";
    finalized = false; hadAutoInput = false;
    lastOutputAt = now();

    ThreadsKt.thread(daemon=true) {           // 独立线程跑整条队列
        for (i, s) in list:
            if (isAborted()) { aborted = true; break; }
            s.done=false; s.runningNow=true; s.exitCode=null;
            emit("state","");                  // UI 刷新徽标
            // —— 方案模式最后一棒：若本次已产生自动应答，挂"空闲看门狗"
            if (auto && targetApp 非空 && i == 最后一项 && hadAutoInput)
                idleWatch = 新线程(watchIdle)
            runOne(s, auto)                    // ★ 单脚本执行（见 5.4）
            if (idleWatch != null) idleWatch.interrupt(); idleWatch = null;
            s.runningNow=false; s.done=true;
            emit("state","");
            Store.saveScripts(appCtx, scripts);       // 逐条落盘
            if (!aborted && i < last)
                log(">> 「{name}」执行完毕，自动开始下一个脚本", S_DIM)
        // 收尾
        runningNow = false; emit("state","");
        emit("done", aborted ? "已中止执行" : "队列执行完毕");
        if (auto && !aborted && targetApp 非空) {
            sleep(1200);
            finalizePreset(targetApp);          // 跳目标应用并自杀
        }
    }
```

**要点**：
- 重入保护：`runningNow` 为真时直接忽略新请求。
- 每条脚本跑完立即 `saveScripts` 落盘，**崩了也不丢已跑完的状态**。
- 队列异常被最外层 `try/catch` 兜住 → 终端打印 `!! 执行异常: <msg>`，不会崩 App。


### 5.4 单脚本执行 `runOne(s, auto)` — 最核心的方法

> 说明：此方法在反编译时被 jadx 放弃（`UnsupportedOperationException`），重建时按 v2.7.2 的 smali（4680 行）手工回写。

#### 阶段 1：探测文件 + 判 ELF

```java
log("$ su -c \"" + path + "\"", S_CMD);         // 终端回显命令
Process pre = ProcessBuilder("su","-c",
    "test -f " + q(path) + " && head -c 4 " + q(path) + " | od -An -tx1");
probeOut = 读stdout; fileOk = (exit==0);
if (!fileOk) { log("!! 文件不存在: {path}，已跳过...", S_ERR); return; }

hex = probeOut 去空格 去换行 转小写;
isElf = hex.contains("7f454c46");
```

#### 阶段 2：组装执行命令

```java
usePty = hasBusyboxScript();        // 有 busybox script 才走 PTY 路径

String inner = isElf
    ? "chmod +x " + q(path) + " 2>/dev/null; " + q(path)   // ELF：加可执行位后直接跑
    : "sh " + q(path);                                      // 脚本：sh 解释执行

// 退出码探针：命令末尾追加，把 $? 用唯一标记吐出来
String exitProbe = "; __hr_e=$?; printf \"\n__HR_EXIT_%s__\" \"$__hr_e\"";

ProcessBuilder pb = usePty
    ? new ProcessBuilder("su","-c","busybox script -q -c " + q(inner+exitProbe) + " /dev/null")
    : new ProcessBuilder("su","-c", inner);
pb.redirectErrorStream(true);
proc = pb.start();  currentProc = proc;
```

**为什么需要 PTY**：部分加固过的 ELF 会检测 stdout 是不是 tty，非 tty 就输出异常或直接退出。`busybox script -q -c <cmd> /dev/null` 用一个伪终端再把输出回传，行为与真实终端一致。

**为什么需要退出码探针**：走 PTY 后，`busybox script` 自己的退出码通常恒为 0，拿不到真实脚本退出码，所以让脚本自己在结束前打印 `__HR_EXIT_<code>__`，由解码线程正则提取。

`hasBusyboxScript()`（结果缓存）：

```java
ProcessBuilder("su","-c","busybox script -q -c 'true' /dev/null")
→ exit==0 && !stdout.contains("Usage:")   → 可用
```
`contains("Usage:")` 是为了排除"busybox 存在但没编入 script 子命令"的情况（此时 busybox 会打印用法）。

#### 阶段 3：静默看门狗

```java
Ref.BooleanRef runDone = new BooleanRef(false);
Thread silence = new Thread(() -> runOne$lambda$9(runDone));  silence.setDaemon(true); start();

// 逻辑：每 3s 检查一次；若空闲 > 12s 且距上次提示已超 30s → 打印一次提示
log("⏳ 已 %d 秒无输出——脚本可能在初始化或等待输入（加固壳启动较慢属正常），不是卡死", S_DIM)
```
这条提示正是用户实际会在终端看到的"已 N 秒无输出"。

#### 阶段 4：预置自动应答队列

```java
LinkedHashMap<String,List<String>> pending = null;
if (auto) {                                   // 只有方案重放才预置
    pending = new LinkedHashMap<>();
    for (ri : s.recordedInputs)
        if (prompt 非空 && value 非空)
            pending.computeIfAbsent(prompt, k->new ArrayList<>()).add(value);
}
synchronized (recordLock) {
    recordingCollect = auto ? null : new LinkedHashMap<>();
}
```
- `auto=true`（方案重放）：按 `prompt → [答案...]` 建表，同一提示的多个答案按顺序消费。
- `auto=false`（手动执行）：开启录制容器 `recordingCollect`。

#### 阶段 5：读输出主循环 + 解码 + ANSI + 提示 + 应答

缓冲区与状态：

```java
byte[] rawBuf = new byte[1024];
byte[] carry  = new byte[0];              // 跨 read 的多字节残留
StringBuilder partial = new StringBuilder();
Ref.IntRef esc;  StringBuilder csi;  Ref.CharRef curColor = S_ANSI_DEF;
boolean pendingCr = false;
final Regex EXIT_RE = new Regex("__HR_EXIT_(\\d+)__");
Ref.ObjectRef<Integer> codeOverride;
```

**（a）读取与拼接残留**

```java
n = inStream.read(rawBuf);            // 阻塞读，异常/EOF 跳出
if (n < 0) break;
lastOutputAt = now();                 // 刷新看门狗
chunk = (carry.length==0) ? copyOf(rawBuf, n) : carry + rawBuf[0..n];
```

**（b）两级解码：严格 UTF-8 优先，失败才混合解码** ⭐

```java
CharsetDecoder dec = UTF_8.newDecoder()
        .onMalformedInput(REPORT)         // 遇到非法字节 → 报错而非替换
        .onUnmappableCharacter(REPLACE);
ByteBuffer cb = wrap(chunk);
CharBuffer out = CharBuffer.allocate(chunk.length + 4);
CoderResult r = dec.decode(cb, out, /*endOfInput=*/false);

if (r.isError()) {
    // 不是干净 UTF-8（多半是 GBK/混合）→ 走自定义混合解码
    Pair<String,Integer> hd = hybridDecode(chunk);
    decoded = hd.first;  pendingTail = hd.second;
    if (!"MIX".equals(lastLogCharset)) { Log.d(TAG,"输出编码检测: 本块混合解码（UTF-8/GBK 同屏）"); lastLogCharset="MIX"; }
} else {
    out.flip();
    decoded = out.toString();             // ★ 干净 UTF-8，直接用
    pendingTail = cb.remaining();          // 不足一个多字节序列的尾部（作为残留）
    if (!"UTF-8".equals(lastLogCharset)) lastLogCharset = "UTF-8";
}
carry = copyOfRange(chunk, chunk.length - pendingTail, chunk.length);
```

> **这一步是"中文乱码"问题的根因所在**：如果省掉"严格 UTF-8 先试一次"而直接调 `hybridDecode`，UTF-8 的中文（3 字节 E4-E9 开头）会被当作 GBK 解码，输出 `ɂȂ肻` 这类乱码。必须**先严格 UTF-8，失败才回退**。

**（c）`hybridDecode(bytes)` — UTF-8 / GBK 同屏混合解码器**

纯函数，返回 `Pair<解码结果, 未消费的尾部字节数>`。

```
i = 0
while i < len:
    b = bytes[i] & 0xFF
    ├ b < 0x80                     → 直接 append((char)b); i += 1
    └ 否则
        ├ u8 = ((b&0xF0)==0xE0) ? 3 : ((b&0xF8)==0xF0) ? 4 : 0
        │  若 u8>0:
        │     若 i+u8 > len → break（留给下一块）
        │     校验后续 u8-1 字节均 (x&0xC0)==0x80
        │     通过 → UTF-8 解码，结果不含 U+FFFD → append; i += u8; continue
        ├ 若 0x81 <= b < 0xFF:                      ← GBK 双字节
        │     若 i+2 > len → break
        │     GBK 解码 2 字节；若结果 length()==1 且不含 U+FFFD → append; i += 2; continue
        ├ 若 0xC2 <= b < 0xE0:                      ← 2 字节 UTF-8
        │     若 (bytes[i+1]&0xC0)==0x80 → UTF-8 解码 2 字节 append; i += 2; continue
        └ 全不匹配 → i += 1（丢弃坏字节）
return Pair(sb.toString(), bytes.length - i)
```

**关键边界（易错点）**：GBK 分支的上界是 `0xFF`（smali 里上界寄存器 `v5` 初值为 `0xff` 且未被改写），**不是 `0xC2`**——`0xC2` 是下面"2 字节 UTF-8"分支的下界常量。两者写混会导致 GBK 数据落到 UTF-8 分支产生乱码。

**（d）字符级循环 + ANSI 状态机**

```java
for (c : decoded):
    if (c == '\n') { if (partial.length()>0) flushPartial(partial, EXIT_RE, codeOverride);
                     pendingCr = false; continue; }
    if (c == '\r') { pendingCr = true; continue; }        // 回车不落盘（处理 \r\n）
    pendingCr = false;
    if (feedAnsi(esc, csi, curColor, partial, c) && showable(c))
        partial.append(c);
```

`feedAnsi` 是一个 6 状态机（`esc.element`）：

| 状态 | 含义 | 转移 |
|---|---|---|
| 0 | 普通 | 遇 `ESC(27)` → 1；否则放行 |
| 1 | ESC 后 | `'['`→2(清 csi)；`']'`→3(清 csi)；`'('`/`')'`→5；其他→0 |
| 2 | CSI 收集 | `'m'`→ 执行 `applySgr` 并回 0；数字/`;? :<> =` → 追加到 csi；其他→0 |
| 3 | OSC 收集 | `BEL(7)`→0；`ESC`→4；其他追加（>128 字符强制回 0） |
| 4 | OSC 内 ESC | `'\\'` → 0 |
| 5 | 定界符后 | 直接回 0（吃掉一个字符） |

返回 `true` 表示"当前字符是可见正文"，`false` 表示"被状态机吃掉了"。

`showable(c)`：`c=='\t' || !(isISOControl(c) || c=='\uFFFD')`——过滤控制字符与替换符。

`applySgr(curColor, partial, body)` 把 SGR 参数转成内部样式字符，**以 `\u0001 + styleChar` 的形式内联写进 partial**：

```
p == 0        → 复位（S_ANSI_DEF='n'）
30<=p<38      → 前景色 (p-30)+48  → '0'..'7'（黑/红/绿/黄/蓝/紫/青/白）
90<=p<98      → 亮色   (p-90)+65  → 'A'..'H'
p == 39       → 复位
（其余参数忽略）
setColor 只在颜色变化时才写入标记，避免冗余
```

**（e）提示识别**

```java
if (partial.length() > 0) {
    String p = plainOf(partial);            // 去掉 \u0001+样式字符
    if (!areEqual(p, currentPartial)) {
        currentPartial = p; lastPromptText = p;
        emit("prompt", p);                  // UI 显示橙色提示行
    }
}
```

**（f）自动应答**

```java
if (pending != null && !pending.isEmpty()) {
    String key = promptKey();               // currentPartial 去空白；空则用 lastFullLine
    List<String> vals = pending.get(key);
    if (vals 非空) {
        String v = vals.remove(0);          // 消费一个
        proc.getOutputStream().write((v+"\n").getBytes(UTF_8)); flush();
        log(">> [一键] 自动输入: " + v, S_CMD);
        hadAutoInput = true;
    }
}
```
匹配是**精确文本匹配**（`promptKey()` = 提示全文 trim）。这也解释了为什么录制必须一字不差地复现——脚本换了版本、提示文案变了，就匹配不上。

#### 阶段 6：收尾

```java
waitCode = proc.waitFor();
code = (codeOverride.element != null) ? codeOverride.element : waitCode;
synchronized (recordLock) {
    collect = recordingCollect;
    if (collect != null) { Store.saveScripts(appCtx, scripts);  /*录制落盘*/ }
    recordingCollect = null;
}
runDone.element = true;                    // 关掉静默看门狗
s.exitCode = code;
log("[%s 退出码: %d]", code==0 ? 'd' : S_ERR);
if (code != 0) log("!! 注意：该脚本异常退出，请检查上方报错", S_ERR);
currentProc = null;
```

#### `flushPartial(partial, EXIT_RE, codeOverride)` — 单行落盘

```java
String plain = plainOf(partial);
MatchResult m = EXIT_RE.find(plain, 0);
if (m != null) {                       // 这一行是退出码探针 → 不显示
    codeOverride.element = toIntOrNull(m.groupValues[1]);
    partial.clear(); currentPartial=""; lastPromptText="";
    return;
}
lastFullLine = trimEnd(plain);         // 记住上一整行（供 promptKey 兜底）
logRaw(partial);                       // 原样（带样式标记）写进 termBuffer
partial.clear(); currentPartial=""; lastPromptText="";
```

- `logRaw` 把带样式标记的原始内容追加到 `termBuffer`，并 `emit("log", plainOf(raw))`。
- `termBuffer` 超 `MAX_TERM_CHARS`(200k) 时从头裁掉，防止内存无限增长。
- `log(line, style)`：先 `Log.d(TAG,"term| "+line)`（便于用 logcat 抓终端内容），再写 `\u0001+style+line+\n` 进 `termBuffer`。

#### `runOne$lambda$9(runDone)` — 静默看门狗

```java
while (!runDone.element && !abortFlag) {
    long idle = now() - lastOutputAt;
    if (idle > 12000 && now() - lastTip > IDLE_MS) {   // 12s 无输出，且 30s 内没提示过
        lastTip = now();
        log("⏳ 已 N 秒无输出——脚本可能在初始化或等待输入...", S_DIM);
    }
    sleep(3000);
}
```

### 5.5 中止 `abort()`

```java
abortFlag = true;
currentProc.destroy();               // 杀子进程
currentProc.getOutputStream().close();
currentProc.getInputStream().close();
```
三重动作保证卡在 `read()` 的主循环能退出。`isAborted()` = `Thread.interrupted() || abortFlag`。

调用入口：`RunnerService` 收到 `ACTION_STOP`（通知栏"停止"按钮），或 `QueueFragment` 的停止按钮。

### 5.6 方案收尾 `finalizePreset(pkg)` + `watchIdle()`

方案重放跑完后的"最后一公里"：

```java
finalizePreset(pkg):
    if (finalized) return;
    finalized = true;
    Intent it = pm.getLaunchIntentForPackage(pkg);
    if (it == null) { log("目标应用未安装或不可启动，保持 HazeRoot 运行"); return; }
    it.addFlags(FLAG_ACTIVITY_NEW_TASK);
    startActivity(it);
    sleep(1500);
    Log.d(TAG, "退出 HazeRoot 后台进程");     // ← 代码中此处唯一的说明
    Process.killProcess(Process.myPid());
    System.exit(0);
```

> **注**：`killProcess` 处**代码里没有意图注释**，只有日志 `退出 HazeRoot 后台进程`。即真实设计意图是"跳转目标应用后结束自身后台进程"，不涉及其他考虑。（曾有一版文档在此处写了推测性说明，已删除。）

`watchIdle()`（另一条触发路径）：

```java
while (!abortFlag && !finalized) {
    if (now()-lastOutputAt > 30s && currentPartial 为空 && runningNow) {
        log(">> 脚本输出已静止 30s，判定初始化完成，执行方案收尾", S_DIM);
        finalizePreset(presetTargetApp);
        return;
    }
    sleep(1000);
}
```
**为什么有两套**：有些脚本启动后进入"守护态"永不退出，`runQueue` 主循环等不到它结束。这时若本次已产生自动应答（`hadAutoInput`），就在跑最后一个脚本时挂上 `watchIdle`，用"静止 30s"作为"初始化完成"的启发式信号，提前跳转目标应用。

### 5.7 手动输入 `sendRaw(text)`

```java
if (currentProc == null) return false;
currentProc.getOutputStream().write((text+"\n").getBytes(UTF_8)); flush();
log("> [手动输入] " + text, S_CMD);
synchronized (recordLock) {
    if (recordingCollect != null) {             // auto=false 时才录制
        String key = promptKey();
        if (key 非空) {
            vals = recordingCollect.computeIfAbsent(key, ...);
            if (!areEqual(last(vals), text)) vals.add(text);   // 连续相同不重复记
        }
    }
}
```

### 5.8 `resolveAnswer(...)` — 死代码

```java
private final String resolveAnswer(ScriptItem s, String line, boolean auto) { return null; }
```
已与 v2.7.2 smali 逐行核对：**v2.7.2 原本就是 `const/4 v0, 0x0 / return-object v0`**，且全工程无任何调用点，属死代码，无需实现。

---

## 6. UI 层逐模块实现

### 6.1 `SplashActivity` + `ConvergeSplashView` — 启动动画

**SplashActivity**：极薄。`setContentView(new ConvergeSplashView(this))`，注册三个回调：
- `onLaunch` → 启动 `MainActivity`（带 `fade_in`），置 `launched=true` 防重入
- `onComplete` → 若已 launched 则 `finish()`，否则 `finishToMain()`
- `setOnClickListener` → `view.skip()`
- `view.post{ view.start() }`

**ConvergeSplashView**：纯 `View` + `Canvas` 手绘，无 XML 动画。

*几何*：12 个碎片 = 4 行 × 3 列（拼出 "H" 的笔画），随机种子固定 `Random(7411)` 保证每次动画一致。

*每个 `Shard` 参数*：`col/row`、`angle`、`radiusF`、`rot0`、`scale0`、`arcF`、`delayMs`（`(row*3+col)*22 + rand(0..80)`）。

*时间轴（毫秒）*：

| 常量 | 值 | 含义 |
|---|---|---|
| `FLY_MS` | 1200 | 单个碎片飞行时长 |
| `CONVERGE_END` | 1500 | 收敛阶段结束 |
| `BLOCK_AT` / `BLOCK_MS` | 1380 / 320 | 圆角方块（背景）淡入 |
| `NAME_AT` / `NAME_MS` | 1650 / 450 | 名称文字出现 |
| `ZOOM_MS` | 700 | 放大时长 |
| `HOLD_END` | 2200 | = 1500+700，保持阶段结束 |
| `LAUNCH_AT` | 2410 | = 2200+210，**触发 onLaunch（切主界面）** |
| `TOTAL` | 2900 | 动画总时长 → `onComplete` |

*缓动*：`AccelerateInterpolator(1.4f)` + `DecelerateInterpolator(1.6f)`。

*绘制流程（`onDraw`）*：
1. `zoomP = clamp((t-HOLD_END)/ZOOM_MS, 0, 1)`；`fade = zoomP<0.55 ? 1 : 1-(zoomP-0.55)/0.45`；`fade<=0` 直接 return。
2. `t >= LAUNCH_AT && !launchedFired` → 触发 `onLaunch`。
3. 画圆角方块：`blockA = easeOut(clamp((t-BLOCK_AT)/BLOCK_MS,0,1))`，缩放 `0.88→1.0`。
4. 画碎片：每个碎片按 `local = clamp((t-delayMs)/FLY_MS,0,1)` 从屏外飞入到目标格位。
5. 名称文字 `namePaint`（`sans-serif-medium`，居中）。
6. 整体 `canvas.scale(zoom)`：保持阶段是 `pop = 1 + sin(π·popP)·0.04`（轻微呼吸），放大阶段是 `easeIn(zoomP)*9+1`（放大到 10×）。

*明暗配色*（关键数值）：

| | blockColor | blockStroke | fragFrom→fragTo | namePaint |
|---|---|---|---|---|
| 白天 | `#0B0B0B` | 无 (0) | `#0B0B0B`→`#FFFFFF` | `#0B0B0B` |
| 夜间 | `#1C1C1E` | `0x20FFFFFF` 半透明白描边 | `#FFFFFF`→`#FFFFFF`（恒定白） | `#FFFFFF` |

配色取自 `res/values/colors.xml`：`bg_page` 白天 `#F6F6F8`、夜间 `#0A0A0C`。

### 6.2 `MainActivity` — 主壳

**职责**：Tab 容器 + 全局入口（文件打开、链接导入、权限引导）。

*Tab 机制*：3 个 Fragment（`queue/files/presets`）一次性 `add()`，非当前页 `hide()`。`selectTab(id)` 做两件事：更新 Tab 视觉（`pill` 背景 + 图标/文字染色，激活色 `accent=#635BFF`），`show(fragment)` 切换。切走 Files 时调用 `filesFragment.resetPickMode()`。

*底部导航自适应*：`navContainer` 的 `OnLayoutChangeListener` 中，若容器高度 < 屏高 × 0.72（键盘弹出）→ 隐藏 `navIsland`。

*`handleOpenIntent(intent)`*（`ACTION_VIEW` 打开文件）：
- `file://` → 直接取 `uri.getPath()`
- `content://` → 先查 `_data` 列；拿不到则 `openInputStream` 把内容拷到 `cacheDir/open_<name>`，用副本路径
- 拿到路径后主线程：`filesFragment.openAt(path)` + `switchTo(R.id.navFiles)`（跳到文件页并定位）；定位失败提示 `无法定位该文件`

*从链接导入*（`importFromLinkDialog` → `downloadTo`）：
1. 对话框收 URL + 保存名（留空自动取链接尾段）
2. `HttpURLConnection`：连接 15s / 读 30s 超时，跟随重定向；2xx 才下载
3. 下载到 `cacheDir/dl_tmp`；空文件报"下载内容为空"
4. 成功后 `su("cp <tmp> /data/adb/HazeRoot/<name> && chmod 664 ...")`，再 `queue.addScriptByPath()` 入队并切到队列页
5. `confirmOverwrite`：同名先问"覆盖？"

*权限引导*（`maybeOnboardPermissions`，仅首次）：
```
SharedPreferences("ui_prefs").putBoolean("perm_onboard_v1", true)
→ 弹"授权引导" → requestNotiThenRoot()
     ├ 未给通知权限 → launch(POST_NOTIFICATIONS)
     └ 已给 → onboardRootStep()  (后台 checkRoot)
              ├ 成功 → toast("Root 权限已就绪 ✓")
              └ 失败 → detectRootManager() → 弹框给"打开管理器"按钮
```

### 6.3 `QueueFragment` — 队列 + 终端（核心页面，1675 行）

**布局元素**（`fragment_queue.xml`）：`termView`（可编辑 TextView，承载带色文本）、`termScroll`、`promptRow/promptView`（橙色提示行）、`manualInput`（EditText）、`btnRun/btnStop`、`rootBadge/rootDot/rootText`、`btnTheme`、`summaryText`、`countBadge`、`queueList`、`btnAdd/btnMore/btnClear/scriptsHeader`。

**初始化 `onViewCreated`**：
1. `Engine.init(applicationContext)`
2. 首启逻辑（见 4.4）填充 `Engine.scripts`
3. `termView.setText("", EDITABLE)`；若 `termBuffer` 为空则 `printWelcome()`
4. `syncTerm()` 全量渲染一次
5. 建 `ScriptAdapter`，接三个回调：
   - `onMove(from, delta)` → 交换列表项 → `saveScripts` → `notifyDataSetChanged`
   - `onEdit(idx)` → `editScriptDialog(idx)`
   - `onDelete(idx)` → 确认后删除 + 落盘
6. `RecyclerView` 用 `LinearLayoutManager`，`setNestedScrollingEnabled(false)`（外层是 ScrollView）
7. 注册 `AdapterDataObserver` → 数据一变就 `fitQueue()`（按内容调整列表高度）
8. 注册 `Engine.Listener`（`uiListener`）→ 收 `log/state/prompt/done` → `syncTerm()` / 刷新按钮 / 更新提示行

**终端增量渲染 `syncTerm()`** ⭐

```java
buf = termBuffer.toString();
dst = (Editable) termView.getText();

if (dst 含 '\u0001') { dst.clear(); renderedChars = 0; }        // 缓冲被裁剪过 → 全量重绘
if (buf.length() < renderedChars) { renderedChars = 0; dst.clear(); }

if (buf.length() > renderedChars) {
    boolean initial    = (renderedChars == 0);
    boolean wasAtBottom = !termScroll.canScrollVertically(1);
    appendStyled(dst, buf.substring(renderedChars));           // 只追加增量
    renderedChars = buf.length();
    if (initial || wasAtBottom) termScroll.post(() -> fullScroll(FOCUS_DOWN));
}
```

`appendStyled` 扫描 `\u0001+styleChar` 标记，把普通文本按当前样式分段 `setSpan(ForegroundColorSpan)` 写进 Editable。这样即使日志滚动，也不做多余的整屏重排。

**`fillSummary()`**：折叠态显示 `take(scripts,2).joinToString(" · "){name}` + `"  +N"`。这里曾因 `Kt.joinToString` 未实现 Kotlin `$default` 的 mask 语义而输出 `nullnullnull`（详见 9.4）。

**执行门控 `withGates(onPass)`**：

```
if (Engine.runningNow) return;                     // 已在跑
toast("正在检测 Root…")
后台：rootOk = Engine.checkRoot()
主线程：
  rootOk? ─ 通知权限已给? ─ onPass()                // 放行
           └ 未给 → showNeedNotificationDialog()
  └ 未授权 → showNeedRootDialog()
```
`doStartQueue()`：`setCollapsed(true)` → `RunnerService.start()` → `Engine.log("")` → `Engine.runQueue(scripts, false, null)`。
`doStartDirect(path)`：同上但队列只放这一个脚本（用于文件页"入队并立即执行"）。

**手动输入**：`manualInput` 的 `OnEditorAction`（回车）与 `btnSend` 点击都走 `onViewCreated$sendManual` → `Engine.sendRaw(text)` → 清空输入框。输入框获得焦点时把终端滚到底。

**折叠**：点 `scriptsHeader` → `setCollapsed(!collapsed)`；折叠态持久化在 `ui_prefs.scripts_collapsed`。

**`addFlow()`**：底部弹窗三选一（浏览文件 / 从链接导入 / 手动输入），分别对应文件页 pick 模式、`MainActivity.importFromLinkDialog()`、`addScriptDialog()`。

### 6.4 `FilesFragment` — 双窗文件管理器（最大文件，3351 行）

**核心结构**：左右两个 `Pane`（各含 `ListView` + 路径栏 + 上一级按钮），一个"活动窗"概念，支持多选、pick 模式（点脚本入队）。

**目录列举 `Pane.load(dir)`**（后台线程）：

```java
out = Engine.exec("ls -Al " + q(dir));       // 走 root

// 错误分流
out==null              → rootAvailable()? "无法读取目录" : "权限不足：读取 X 需要 Root 或「所有文件访问」权限"
含 "No such file..."   → "目录不存在：X"
含 "Permission denied" → "权限被拒：X"

// 逐行正则解析
LS_RE = ^([-dlbcps][rwxsStT.-]{9})\s+\d+\s+\S+\s+\S+\s+(\d+)\s+\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}\s(.+)$
   group1 = 权限串   group2 = 大小   group3 = 名字(可能含 " -> 目标")
跳过 "." 与 ".."；" -> " 拆出 linkTarget
→ FsEntry(name, dir+"/"+name, 权限首字符=='d', perm, size, linkTarget)
```
排序：**目录优先**（`compareByDescending{isDir}`），再按名字。
**符号链接跟随**：若目录只有 1 项且是软链、`follow < 3` → 自动 `load(目标)`（最多跟 3 层，防环）。
**并发保护**：每次 `load` 自增 `gen`，回调时 `g == gen` 才应用结果（避免快速切目录时的乱序覆盖）。

**快捷跳转**：`quickDirs = [/data/adb/HazeRoot, /data/local/tmp, /sdcard, /data/adb, /]`；`homePrefs()` 记住每窗的"首页"。

**文件操作清单**：

| 操作 | 实现命令 | 入口 |
|---|---|---|
| 复制/移动 | `cp -a <src> <dst>` / `mv <src> <dst>`（跨窗或同窗"粘贴"） | `copyMoveTo(src, from, move, to)` |
| 同窗复制的重名 | 自动追加 ` (n)` 后缀：`base (i).ext` | `copyMoveTo$lambda$53` 循环探测 |
| 压缩 | `tar -czf <dst> -C <dir> <name>` 或 `zip` | `zipTo` / `multiZip` |
| 解压 | `.tar.gz/.tgz`→`tar -xzf .. -C <dst>`；`.tar`→`tar -xf`；`.zip`→`unzip` | `unzipTo` / `isArchive()` |
| 改权限 | `chmod` + 八进制输入框 + 9 个权限复选框实时联动 | `chmodDialog` |
| 重命名 | `mv`（同目录改名），校验"名称不能包含 /" | `renameDialog` |
| 删除 | `rm -rf`；目录额外警告"⚠ 文件夹及其全部内容将被删除！" | `deleteDialog` / `multiDelete` |
| 分享 | 暂存到 share 目录 → FileProvider → `ACTION_SEND(_MULTIPLE)` | `sharePaths` |
| 安装 APK | 见下 | `installApk` |
| 打开编辑器 | `openEditor(e)` → `EditorActivity` | 仅文本文件 |
| 打开媒体 | `openMedia(e)` → 按扩展名判 video/audio/image → `MediaActivity` | |
| 入队 | `enqueue(path)` → `QueueFragment.addScriptByPath` | pick 模式点选 |

**扩展名分类常量**：
- `TEXT_EXT`: txt md log json xml ini conf cfg properties yaml yml csv py js ts html htm css java c cpp h kt sql bat list prop
- `VIDEO_EXT`: mp4 mkv avi mov 3gp webm m4v
- `AUDIO_EXT`: mp3 wav flac m4a aac ogg opus amr wma
- `IMG_EXT`: jpg jpeg png gif webp bmp
- `isApkFile`: 正则 `.*\.?apk(\.\d+)?`（兼容 `.apk.1` 这种下载残留后缀）

**`installApk(e)` 全流程** ⭐

```java
1. 权限闸门：!pm.canRequestPackageInstalls()
   → 弹"需要安装权限" → 点"去开启"跳 MANAGE_UNKNOWN_APP_SOURCES
2. 后台线程：
   shareDir = getExternalFilesDir(null)/share;  mkdirs();
   target   = shareDir/<e.name>;
   Engine.exec("rm -f <target>; cp -f <e.path> <target>" + handbackCmd(shareDir, target));
   ok = target.exists() && target.length()>0 && target.canRead();
3. 主线程：
   ok? → FileProvider.getUriForFile(ctx, pkg+".fileprovider", target)
       → Intent(ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
         .addFlags(0x10000001)   // FLAG_GRANT_READ_URI_PERMISSION | FLAG_ACTIVITY_NEW_TASK
       → startActivity
   否则 → "无法调起安装器…"
```

**`handbackCmd(dir, file)` — 修复"安装器打不开文件"的关键** ⭐

```java
String uid = String.valueOf(Process.myUid());
return "; OWN=$(stat -c %u:%g " + q(dir) + " 2>/dev/null || echo " + uid + ");"
     + " chown $OWN " + q(file) + " 2>/dev/null;"
     + " chmod 640 " + q(file) + " 2>/dev/null; true";
```
**问题**：`exec` 在 root 下走 `su`，`cp` 产物属主是 `root`、权限 `0600`；而 `FileProvider` 只是路径映射，系统安装器以 **App uid(如 u0_a270=10270)** 打开文件时会被拒绝 → "无法调起安装器"。
**修复**：复制后把属主交还给暂存目录的属主（即本应用）+ 补组读权限。实测修复后 `ls -la` 中 APK 属主由 `root`/`shell` 变为 **`u0_a270`**。

**分享** `sharePaths` 同理：暂存 → `handbackCmd` → FileProvider → `ACTION_SEND`。

**多选模式**：长按进入 → 顶部出现"全选 / 压缩 / 删除 / 完成"条 → 计数显示 `已选 N 项`；批量删除二次确认 `删除选中的 N 项？删除不可恢复！`。

**pick 模式**：`enterPickMode()` 后点脚本直接 `enqueue`（提示"点按脚本即可加入队列"），切页时 `resetPickMode()` 复位。

### 6.5 `PresetsFragment` — 方案库（740 行）

- `rebuild()`：读 `Engine.presets`，为每个方案 `presetCard(i, p)` 建卡。
- 卡片内容：方案名、脚本数量摘要、目标应用名；点卡片 → `showPresetSheet`。
- `appLabel(pkg)`：把包名转成应用显示名（`PackageManager.getApplicationLabel`，失败回退包名）。

**`showPresetSheet(i, p)`**：BottomSheetDialog，四个动作：

| 动作 | 行为 |
|---|---|
| **运行方案** | `runningNow`?→提示"正在执行中"；否则 `RunnerService.start()` → `Engine.log("")` → `Engine.runQueue(p.scripts, /*auto=*/true, p.targetApp)` → 切到队列页 |
| **目标应用** | `appPickerDialog`：列出所有 LAUNCHER 应用 + "不设置"项，选中写回 `p.targetApp` |
| **重命名** | `nameDialog` |
| **删除** | 确认框"删除「X」？删除后无法恢复。" |

**关键点**：**只有从方案运行才传 `auto=true`**，这也是"自动应答 + 录制不覆盖 + 跑完自动跳转"的唯一触发条件。

**保存方案**：把当前 `Engine.scripts`（含各自 `recordedInputs`）连同 `targetApp` 存成新 `Preset` → `savePresets`。

### 6.6 `EditorActivity` — 文本编辑器（703 行）

**二进制与字符集自动识别**（这是这个 Activity 最有技术含量的部分）：

```java
isBinary(raw): 采样检测 NUL 字节 / 不可打印字符比例 → 二进制则只读预览

detectCharset(raw):
  1) BOM 优先：
     EF BB BF → UTF-8
     FF FE    → UTF-16LE
     FE FF    → UTF-16BE
  2) 含 NUL 字节时，按奇/偶位置占比判断：
     even 位置 NUL 占比 > 30% → UTF-16BE
     odd  位置 NUL 占比 > 30% → UTF-16LE
  3) 严格 UTF-8 解码（REPORT/REPORT）
     成功 → UTF-8
     失败 → utf8NonAsciiRatio(raw) >= 0.6 ? UTF-8 : GB18030
```

`utf8NonAsciiRatio(raw)`：统计所有非 ASCII 起始字节，能成功按 2/3/4 字节 UTF-8 序列消费掉的比例（`valid / nonAscii`）。**这是"GBK 还是 UTF-8"的启发式判据**——真正的 GBK 中文几乎无法组成合法 UTF-8 序列，比值会很低。

**读写与保存**：文件读写经 `Engine.exec`（`cat` / 写回），需要 root 才能编辑 `/data/adb` 下的文件；保存失败提示 `保存失败（root 写回未成功），内容仍在界面未丢`，并做保存后校验（`verify_fail`）。

**其它**：`load()` 后台读，超 2MB 警告"编辑器可能卡顿"；`updateLines()` 行号；`applyHighlight()` 防抖后调 `SyntaxHighlighter`；`unsaved_mark`（● 未保存）；退出前 `confirmExit()` 三选（继续编辑/放弃修改/保存）。

### 6.7 `SyntaxHighlighter` — 语法高亮

- 语言判定 `detect(fileName)`：`.sh/.bash`→SHELL，`.py`→PYTHON，`.json`→JSON，`.ini/.conf/.cfg`→INI，否则 NONE。
- 上限 `MAX_BYTES = 262144`（256 KB），超限直接跳过。
- 实现：先移除所有旧 `ForegroundColorSpan`，再**逐行**用正则给关键字/内建命令/变量/数字/字符串/注释分别 `setSpan`。
- 每种语言一套正则表（如 SHELL 的 `if/then/fi/for/...` 关键字、`echo/printf/grep/...` 内建、`${VAR}`/`$1` 变量、`[frb]?"..."` 字符串、`#...` 注释）。

### 6.8 `MediaActivity` — 音视频/图片查看（714 行）

- 入口参数：`path / name / kind(video|audio|image) / size`。
- **大文件策略**：媒体文件常在 root 私有目录、MediaPlayer 拿不到，需先拷到 App 私有目录 `cacheDir/media_<name>`：
  - 小文件直接播
  - 大文件先弹 `文件较大（X），需先复制到应用缓存才能播放…继续？`
  - `copyThen(onReady)`：后台 `Engine.exec("cp <src> <cacheDir>/media_<name>")` → 成功后回调路径
- `startAudio(path)`：`MediaPlayer` + 播放/暂停切换 + 进度 + `fmt(ms)` 时长格式化。
- 失败提示 `无法打开该媒体文件` 并 `finish()`。

### 6.9 `ManualActivity` — 使用手册

`WebView` 加载 `file:///android_asset/manual.html`（1.8 MB 离线页面）。`onPageFinished` 时按当前明暗主题注入 `document.body.classList.add/remove('light')`。`onBackPressed` 支持页内后退，`onSaveInstanceState` 保存 WebView 状态。

### 6.10 `RunnerService` — 前台服务

- 常驻通知渠道 `runner`（重要度 2），`NOTI_ID = 1`。
- `onStartCommand`：
  - `ACTION_STOP` → `Engine.abort()` → `stopSelf()`（通知栏"停止"）
  - 否则 → `startForeground(1, buildNotification("执行中…"))`
- 监听 `Engine` 事件：
  - `"state"` → 找 `runningNow` 的脚本 → `updateNotification("执行中: <name>")`
  - `"done"` → `updateNotification(payload)` → `stopSelf()`
- **作用**：① 防后台被系统杀；② 给用户进度反馈；③ 提供"停止"入口。
- ⚠ **已知瑕疵**：`buildNotification` 用的是系统图标 `android.R.drawable.stat_sys_download`（见 10.1）。

---

## 7. 支撑组件层

| 组件 | 职责 | 关键实现 |
|---|---|---|
| **`Ui`** | 自定义 Toast | 手动拼 `LinearLayout`（品牌图标 `ic_brand_h` + 文字），`Toast.setGravity(BOTTOM, 0, dp(76))`，与系统 Toast 视觉区分 |
| **`SwitchFx`** | Tab/主题切换的过场遮罩 | 往 `decorView` 加一个全屏纯色 `View`；`run()` 0→1 淡入(260ms) 后执行动作，`consume()` 1→0 淡出(320ms) 后移除。`pendingColor` 静态传递，跨 Activity 也能衔接 |
| **`ThemeStore`** | 日夜模式 | `SharedPreferences("ui_prefs").night_mode`（默认 `-1` 跟随系统）；`AppCompatDelegate.setDefaultNightMode`；`App.onCreate` 里 `apply()` |
| **`Perms`** | 通知权限 | `notificationGranted()`：<33 恒真，否则 `areNotificationsEnabled()`；`openAppSettings()` 跳应用详情页 |
| **`Kt`** | Kotlin 标准库桥 | 手写 `contains/endsWith/startsWith/indexOf/replace/split/substringBefore(After)(Last)/joinToString`，**全部补齐 `$default` 的 mask 语义**（见 9.4） |
| **`ScriptAdapter`** | 队列列表适配器 | 3 种资源点：`dot_running` / `dot_done` / `dot_error` / `dot_idle`；行首 `index` 两位序号；`activeBar` 左缘竖条表示运行中；`btnMore` 弹 `menu_script_actions`（上移/下移/编辑/删除，删除项染 `error_text` 红） |
| **`FilesListView`** | 自定义 ListView | 加 `gestureLocked` 开关，锁定时 `onInterceptTouchEvent` 直接返回 false（用于多选时禁用滑动） |
| **`Item` / `Row`** | 弹窗条目模型 | `Item(title, danger, action)` 用于操作表；`Row(label, pkg, icon, isNone)` 用于应用选择器 |
| **`App`** | Application | 仅 `ThemeStore.apply(this)` |

---

## 8. 资源、权限与配置

### 8.1 `AndroidManifest.xml`

**权限**：

```xml
INTERNET                       <!-- 链接导入 -->
REQUEST_INSTALL_PACKAGES       <!-- 安装 APK -->
POST_NOTIFICATIONS             <!-- 13+ 通知 -->
FOREGROUND_SERVICE / _DATA_SYNC<!-- 前台服务 -->
READ/WRITE_EXTERNAL_STORAGE    <!-- maxSdkVersion=32 -->
MANAGE_EXTERNAL_STORAGE        <!-- 所有文件访问 -->
```

**组件**：

| 组件 | exported | 说明 |
|---|---|---|
| `SplashActivity` | true | LAUNCHER |
| `MainActivity` | true | `singleTask`，`adjustResize`；含 `VIEW` + `file/content` + `*/*` 的 intent-filter（接收"用其他应用打开"） |
| `EditorActivity` | false | `adjustResize` |
| `ManualActivity` / `MediaActivity` | false | |
| `RunnerService` | false | `foregroundServiceType="dataSync"` |
| `FileProvider` | false | authority `com.hazeroot.scriptrunner.fileprovider`，`grantUriPermissions=true` |

`application`：`allowBackup=true`、`extractNativeLibs=false`、`usesCleartextTraffic=true`（允许 http 导入）。

### 8.2 FileProvider 路径（`res/xml/file_paths.xml`）

```xml
<external-files-path name="share"          path="share/" />
<external-path       name="download_share" path="Download/HazeRoot_share/" />
```
即 APK 安装与分享的暂存区是 `Android/data/com.hazeroot.scriptrunner/files/share/`。

### 8.3 关键颜色

| 名称 | 白天 | 夜间 |
|---|---|---|
| `accent` | `#635BFF`（靛紫） | 同 |
| `bg_page` | `#F6F6F8` | `#0A0A0C` |
| `bg_card` | `#FFFFFF` | `#121214` |
| `label` | `#1C1C1E` | — |
| `label_tertiary` | `#98989E` | — |
| `error_text` | `#C50015` | — |

### 8.4 布局与菜单

- 布局：`activity_main` / `activity_editor` / `activity_manual` / `fragment_queue` / `fragment_files` / `fragment_presets` / `item_file` / `item_script` / `custom_dialog`
- 菜单：`bottom_nav` / `menu_script_actions`（上移/下移/编辑/删除）/ `menu_preset_actions`

### 8.5 字符串资源

`res/values/strings.xml` 共 **433 条**字符串（其中约 190 条为 Material/AppCompat 库自带，其余为自研文案，全中文，含 `%1$s`/`%1$d` 占位符）。`values-zh-rCN` 等 100+ 语言目录只含库字符串（反编译残留，无实际作用）。所有用户可见文案都走 `R.string`，便于后续国际化。

---

## 9. 关键实现细节与坑位清单

> 这一节记录重建过程中踩过的坑，是后续维护最有价值的部分。

### 9.1 jadx 反编译陷阱（4 类）

| 类型 | 表现 | 处置 |
|---|---|---|
| **① int→float 丢失** | `int/int` 变整数除法，动画数值全错 | 按 smali 补回浮点转换（如 `ConvergeSplashView` 的插值） |
| **② 方法彻底放弃** | `throw new UnsupportedOperationException("Method not decompiled: ...")` | 按 smali 手工回写（`Engine.runOne`，4680 行） |
| **③ 静默桩方法** | `{ return null; }` | 需与 smali 对照：`resolveAnswer` **原本就是 `return null`** → 死代码，保留 |
| **④ `$default` mask 丢失** | Kotlin 合成方法默认值语义丢失 → 出现 `null` 字面量 | 手写 `Kt` 补齐 mask 判断（见 9.4） |

**判断准则**：凡是 `UnsupportedOperationException` 或可疑 `return null`，都对照本地留存的 smali 素材逐行核对，**不要凭 Kotlin 语义猜测**。

### 9.2 KernelSU `su -c` 复合命令陷阱 ⭐（本次最关键根因）

`su -c "a; b"` / `su -c "a && b"` **只给第一条命令提权**，后续命令降回 shell(uid 2000)。必须 `su -c "sh -c 'a; b'"` 显式包裹。

实测证据：

| 形式 | `whoami` / `id -u` |
|---|---|
| `su -c "whoami; id -u"` | root / **2000** ✗ |
| `su -c "whoami && id -u"` | root / **2000** ✗ |
| `su -c "sh -c 'whoami; id -u'"` | root / **0** ✓ |

**影响面**：所有 `exec/su` 的复合命令（`cp`、`tar`、`chmod`、管道等）在 KernelSU 下都会静默以非 root 身份执行 → 失败或产物属主错误。

**修复位置**：`Engine.su()`、`Engine.execBytes()`。

> 这条陷阱会误导所有基于 `su -c` 的手工调试——用 adb 手敲同样的命令也会失败，容易误判为"App 的锅"。

### 9.3 FUSE `/sdcard` 权限特性

- `/sdcard` 走 FUSE：root 对**自己的**文件可以 `chown`（rc=0），但 root 直接 `chmod` **他人**文件会 `Operation not permitted`。
- `run-as` 创建的是**不带 App SELinux 上下文**的裸 uid 进程，用它 `cat /sdcard/Android/data/...` 报 Permission denied 属**测试假象**——真实 App 进程有 `MANAGE_EXTERNAL_STORAGE: allow` 且文件属主正确，是可读的。
- 结论：**不要用 `run-as` 验证文件读写权限**，要用 App 内实际路径或 `ls -la` 看属主。

### 9.4 Kotlin `$default` mask 语义

Kotlin 编译器为带默认值的参数生成 `$default` 合成方法：**mask 的对应位为 1 时，该参数取默认值，忽略调用方传入的值**。

反编译后 mask 语义丢失，导致显式传 `null` 的参数被 `StringBuilder.append` 转成字面 `"null"`。

**实例**：调用点 `Kt.joinToString(list, " · ", null, null, 0, null, transform, 30, null)` 输出 `nullnullnull`。

**修复**（`Kt.joinToString`）：

```java
if ((mask & 1)  != 0) sep = ", ";
if ((mask & 2)  != 0) prefix = "";
if ((mask & 4)  != 0) postfix = "";
if ((mask & 8)  != 0) limit = -1;
if ((mask & 16) != 0) truncated = "...";
if ((mask & 32) != 0) transform = null;
// 双保险：即使 mask 没置位，null 也兜底
if (sep == null) sep = ", ";  if (prefix == null) prefix = "";
if (postfix == null) postfix = "";  if (truncated == null) truncated = "...";
```

### 9.5 终端中文乱码（已定位并修复）

**现象**：终端输出 `ɂȂ肻 ǂ Ȃ ǂ 쐬...`，英文数字正常；v2.7.2 同机同脚本正常。

**根因**：重建版**跳过了"严格 UTF-8 先行解码"**，直接调 `hybridDecode`。UTF-8 的中文（`E4-E9` 开头 3 字节）被 GBK 分支吞掉 → 乱码。

**修复**：恢复两级解码策略——先用 `CharsetDecoder(REPORT)` 严格试解 UTF-8，失败才回退 `hybridDecode`。

**同时修正的边界错误**：`hybridDecode` 的 GBK 分支上界应为 `0xFF`（smali 中上界寄存器初值 `0xff` 且未改写），而非 `0xC2`；`0xC2` 属于下面"2 字节 UTF-8"分支的下界。

**验证**：修复后终端正确显示 `内核版本: 5.15.194-...`、`设备信息: Xiaomi 测试机 安卓17`、`请您输入"0/1/2"` 及特殊符号 `①②③ ★ ☆ 【】「」`。

### 9.6 APK 安装失败（已定位并修复）

见 6.4 的 `handbackCmd`。根因链：

```
installApk → exec("rm -f X; cp -f Y Z")
   ├ KernelSU 只提权 rm，cp 以 shell 跑 → 源在 root-only 目录时 cp 失败 → ok=false
   └ 即便 cp 成功，产物属主 root(0600) → FileProvider 以 App uid 打开 → EACCES
修复：① su 用 sh -c 包裹；② cp 后 chown $OWN + chmod 640
验证：APK 属主由 root/shell → u0_a270
```

### 9.7 其它实测环境注意事项

- **输入注入**：MIUI 下 `adb shell input tap/keyevent` 报 `INJECT_EVENTS` 拒绝，须 `adb shell su -c "input tap X Y"`；坐标应先用 `uiautomator dump` 取真实 `bounds`，不要靠截图目测。
- **沙箱**：`su -c "...; ..."` 含引号/分号时易被本地沙箱拦截，可用 base64 中转：先写脚本到 `/data/local/tmp`，再 `su -c "sh /data/local/tmp/x.sh"`。
- **adb push 路径**：Git Bash 会把 `/data/...` 当本地路径，须加 `MSYS_NO_PATHCONV=1`。
- **MIUI 输入法**：`adb ... input text '/data/adb/HazeRoot/x.sh'` 会把 `/` 当成拼音分隔符（变成 `、打他、adb、`）→ 需要输入路径的测试应改用"浏览文件"选路径，而非键入。

---

## 10. 已知问题与后续方向

### 10.1 已知瑕疵

| # | 问题 | 位置 | 影响 | 修复代价 |
|---|---|---|---|---|
| 1 | 通知栏图标是系统"下载"图标（MIUI 上形似漏斗） | `RunnerService:89` `setSmallIcon(android.R.drawable.stat_sys_download)` | 观感差、与品牌不符 | **1 行**：换成 `R.drawable.ic_brand_h`（或专用单色通知图标） |
| 2 | 通知不支持进度/暂停 | `RunnerService` | 只能看名字 + 停止 | 中等 |
| 3 | 运行态字段 `transient` 不持久化 | `ScriptItem` | 重启后队列状态重置 | 低（如需保留） |
| 4 | 提示匹配是精确文本 | `Engine.promptKey()` | 脚本改版即失配 | 中（可加正则/模糊） |
| 5 | 每个脚本一次 `su` 冷启动 | `Engine.runOne` | 队列长时累计慢 | 中（长期 shell 会话） |
| 6 | 无脚本间数据传递 | `Engine` | 只能线性执行 | 中高 |
| 7 | `values-*` 100+ 语言目录冗余 | `res/` | 包体略大 | 低（清理） |

### 10.2 发展方向（按性价比排序）

1. **回归测试自动化**：用 adb 写冒烟脚本（启动→入队→执行→断言 logcat→截图对比）。本次三个阻断级 bug（`runOne` 未反编译、中文乱码、APK 安装）都可在 10 秒内暴露。
2. **持久 root shell 会话**：把每次 `su` 冷启动换成常驻 shell（fd 复用），同时获得变量/环境共享能力，是"脚本编排"的地基。
3. **脚本包 + 在线源**：定义包格式（脚本 + 图标 + 元数据 + 校验和）+ 在线仓库 + 深链/二维码一键安装，从"单机工具"变"平台"。
4. **结构化输出协议**：约定 `@@PROGRESS:n` / `@@RESULT:k=v` 之类标记，把刷屏终端变成进度条 + 结果卡片。
5. **体验补齐**：通知品牌图标 + 进度/暂停、悬浮球、桌面快捷方式、定时任务。

---

## 10.3 去品牌化改造记录（v2.8.0-rename）

把该工程早期版本遗留的示例数据与外部品牌引用全部清除，统一为自有品牌。改造前已留存完整备份（含 `src/` 30 个源文件 + 全部 gradle 配置）。

### 改动清单

| # | 项目 | 改前 | 改后 |
|---|---|---|---|
| 1 | 包名 / namespace / applicationId | 早期品牌包名 | `com.hazeroot.scriptrunner` |
| 2 | 源码目录 | 早期包路径 | `java/com/hazeroot/scriptrunner/` |
| 3 | FileProvider authority | 早期包名 `.fileprovider` | `com.hazeroot.scriptrunner.fileprovider` |
| 4 | 脚本目录 `SCRIPT_DIR` | 早期群组目录 | `/data/adb/HazeRoot` |
| 5 | 默认队列 `defaultScripts()` | 若干示例脚本（指向早期专用目录） | **空列表** |
| 6 | 默认方案 `defaultPresets()` | 早期示例方案 | **空列表** |
| 7 | 根工程名 | `RootScriptRunner` | `HazeRoot` |
| 8 | 使用手册正文（`assets/manual.html`） | 含早期目录与脚本名示例 | 改为 `/data/adb/HazeRoot`、`MyLongScriptName_v1.0.sh` |

### 易漏点（改造时实际踩到）

1. **布局里的自定义 View 全限定类名**：`res/layout/fragment_files.xml` 中有 `<早期包名.FilesListView .../>`（左/右两窗）。**漏改会在运行时 inflate 崩溃（ClassNotFoundException）**，且编译器不会报错。
2. **`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`**：Manifest 中硬编码了 `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`，需同步改。
3. **手册 HTML 的正文**：按行 `grep -c` 会漏计（一行多次出现），需用 `grep -o | wc -l` 统计出现次数。
4. **新目录需自建**：`SCRIPT_DIR` 改名后新目录默认不存在，`MainActivity` 的「从链接导入」已在 `cp` 前补 `mkdir -p <SCRIPT_DIR>`，否则下载导入会静默失败。

### 改造后验证

```
clean assembleDebug → BUILD SUCCESSFUL
merged manifest: package="com.hazeroot.scriptrunner"
components: com.hazeroot.scriptrunner.{App,SplashActivity,MainActivity,EditorActivity,ManualActivity,MediaActivity,RunnerService}
authorities: com.hazeroot.scriptrunner.fileprovider / .androidx-startup
残留扫描（14 组关键词）→ 全部 0 次
```

> ⚠ **升级注意**：包名变更后签名与 identity 都变了，**已安装的旧版 App 无法覆盖升级**，必须先卸载（或装成并存的两个 App）。手机里原有的旧脚本目录也不会自动搬到 `/data/adb/HazeRoot`。

### 功能等价性

除包名/路径/示例数据外，**引擎与所有功能逻辑零改动**。逐文件 diff（对齐包名后）仅 3 个文件有实质差异，且均为上述清理项：

| 文件 | 差异行数 | 内容 |
|---|---|---|
| `MainActivity.java` | 8 | 脚本目录常量 + 3 处路径 + 下载前 `mkdir -p` |
| `QueueFragment.java` | 2 | 路径输入框提示 |
| `Store.java` | 6 | 两个默认数据方法改为返回空列表 |

**结论**：`runOne` / 队列调度 / 两级解码 / ANSI 状态机 / 自动应答 / 录制 / 看门狗 / 文件操作 / APK 安装等全部保持原样，脚本适配能力完整保留。

---

## 11. 附录：文件清单与职责速查

| 文件 | 行数 | 职责 |
|---|---|---|
| `Engine.java` | 1249 | **核心引擎**：root / 执行 / 队列 / 解码 / ANSI / 提示 / 录制 / 看门狗 |
| `FilesFragment.java` | 3351 | 双窗文件管理器 + 文件操作 + APK 安装 + 分享 |
| `QueueFragment.java` | 1675 | 队列面板 + 终端渲染 + 手动输入 + 执行门控 |
| `MainActivity.java` | 845 | Tab 壳 + 文件打开 + 链接导入 + 权限引导 |
| `PresetsFragment.java` | 740 | 方案库（CRUD + 运行 + 目标应用） |
| `MediaActivity.java` | 714 | 音视频/图片播放（大文件先拷贝） |
| `EditorActivity.java` | 703 | 文本编辑 + 二进制/字符集识别 + 语法高亮 + 保存校验 |
| `ConvergeSplashView.java` | 455 | 启动动画（12 碎片汇聚 → 方块 → 名称 → 放大） |
| `Kt.java` | 194 | Kotlin 标准库桥（补齐 `$default` mask 语义） |
| `ScriptAdapter.java` | 183 | 队列列表适配器 + 行内菜单 |
| `QueueFragment$uiListener$1.java` | 183 | 引擎事件 → UI 刷新 |
| `Json.java` | 159 | org.json 手写序列化 |
| `SyntaxHighlighter.java` | 157 | 4 种语言正则高亮 |
| `ScriptItem.java` | 152 | 队列项模型 |
| `RunnerService.java` | 127 | 前台服务通知 |
| `Preset.java` | 107 | 方案模型 |
| `ManualActivity.java` | 103 | WebView 使用手册 |
| `SplashActivity.java` | 96 | 启动页（薄壳） |
| `Row.java` | 92 | 应用选择器条目模型 |
| `Item.java` | 87 | 操作表条目模型 |
| `RecordedInput.java` | 85 | 问答对模型 |
| `SwitchFx.java` | 83 | 切页过场遮罩 |
| `Store.java` | 66 | SharedPreferences 持久化 |
| `QueueFragment$onViewCreated$3.java` | 59 | 队列项删除回调 |
| `PresetsFragment$showPresetSheet$items$4.java` | 59 | 方案删除回调 |
| `Ui.java` | 57 | 自定义 Toast |
| `FilesListView.java` | 55 | 锁定手势的 ListView |
| `ThemeStore.java` | 40 | 日夜模式 |
| `Perms.java` | 36 | 通知权限 |
| `App.java` | 16 | Application |

**总计**：30 个 Java 文件，11,928 行。

---

*文档基于 v2.8.0 源码逐文件核对生成。涉及源码复原的部分已与 v2.7.2 smali 对照。*
*作者：HazeRoot*
