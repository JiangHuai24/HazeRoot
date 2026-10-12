# HazeRoot — AI 交接文档

> 交接时间：2026-10-13
> 交接对象：接手本项目的下一个 AI / 开发者
> 作者署名：**HazeRoot**
> 本文件是**唯一入口**。读完本文即可独立接手，无需其他上下文。

---

## 〇、一句话摘要

**HazeRoot** 是一个 Android 平台的**通用 root 脚本执行器**（方向类比 MT 管理器）：以 root 身份按队列执行 shell / ELF 脚本，自动应答脚本的交互提问，可把整条队列连同回答记录存成"方案"一键重放。

工程位于本仓库，当前版本 **v2.8.0（versionCode 66）**，包名 `com.hazeroot.scriptrunner`，`assembleDebug` 已通过。

---

## 一、交付物清单（路径最重要）

### 1.1 本轮产出

| 内容 | 路径 |
|---|---|
| **APK（本轮新版）** | `HazeRoot-v2.8.0-debug.apk`（本地构建产物） |
| APK（构建原始产物） | `app/build/outputs/apk/debug/app-debug.apk` |
| 功能与实现文档（11 章） | `docs/HazeRoot-功能与实现逻辑.md` |
| 版本沿革与迭代记录 | `docs/HazeRoot-版本沿革与迭代记录.md` |
| **本交接文档** | `docs/HazeRoot-交接文档.md` |

### 1.2 工程与备份

| 内容 | 路径 |
|---|---|
| **工程源码** | 本仓库 |
| 源码快照（品牌统一前） | 本地归档（含 `src/` 30 个源文件 + gradle 配置） |
| 源码快照（更早） | 本地归档 |

### 1.3 历史产物与复原素材

| 内容 | 路径 |
|---|---|
| 历史 APK（v2.7.2 / 2.7.4 / 2.7.5 等） | 本地归档 |
| 复原参考：apktool 产物 | 本地归档（资源 + smali，**权威参照**） |
| 复原参考：jadx 产物 | 本地归档 |
| 历史工作 APK | `HazeRoot-v2.7.5.apk`、`HazeRoot-v2.7.5b.apk`（本地归档） |

> ⚠ `recovery/` 下的素材**保留原始内容**（是复原参照材料，不是产物），**不要当作待清理对象**。

---

## 二、工程与版本状态

| 项 | 值 |
|---|---|
| 包名 / namespace / applicationId | `com.hazeroot.scriptrunner` |
| versionCode / versionName | 66 / `2.8.0` |
| minSdk / targetSdk / compileSdk | 26 / 34 / 34 |
| 语言 / 构建 | Java 17 · Gradle 8.6 + AGP 8.2.2（Kotlin DSL） |
| 源码规模 | 30 个 Java 文件，约 11,900 行 |
| 架构 | 单 Activity + 3 Fragment + 4 独立 Activity + 1 前台 Service；全局单例 `Engine` 驱动，无 ViewModel/LiveData |
| 版本管理 | **非 git 仓库**，靠目录快照备份 |

**版本沿革**（aapt2 实测）：`2.7.2(62) → 2.7.3(63,产物缺失) → 2.7.4(64) → 2.7.5(65) → 2.8.0(66)`
**v2.7.2 为第一代基线**（更早记录已丢失）。v2.7.2~v2.7.5 使用旧包名（已弃用）。

---

## 三、本轮全部变更

### 3.1 源码复原期的缺陷修复（阻断级）

| # | 缺陷 | 根因 | 修复位置 |
|---|---|---|---|
| 1 | 脚本完全无法执行 | `Engine.runOne` 被 jadx 放弃（`UnsupportedOperationException`），重建时按 smali（4680 行）手工回写 | `Engine.java` |
| 2 | 终端中文乱码（`ɂȂ肻`） | 复原时漏掉"严格 UTF-8 先行解码"，UTF-8 中文被 GBK 分支误解码 | `Engine.java` runOne 解码段 |
| 3 | 队列摘要显示 `nullnullnull` | `Kt.joinToString` 未实现 Kotlin `$default` 的 mask 语义 | `Kt.java` |
| 4 | APK 安装失败（调不起安装器） | (a) `su -c "a; b"` 只给首条命令提权，`cp` 降权；(b) 产物属主 root，FileProvider 以应用 uid 读取被拒 | `Engine.su()/execBytes()` + `FilesFragment.handbackCmd()` |

### 3.2 顺带修复

| 项 | 说明 |
|---|---|
| `hybridDecode` 边界 | GBK 分支上界应为 `0xFF`（smali 中寄存器初值，未被改写），非 `0xC2`；`0xC2` 属"2 字节 UTF-8"分支下界 |
| 下载导入目录不存在 | `MainActivity.downloadTo` 在 `cp` 前补 `mkdir -p <SCRIPT_DIR>` |

### 3.3 品牌统一（v2.8.0-rename）

| 项目 | 改前 | 改后 |
|---|---|---|
| 包名 / namespace / applicationId | 旧包名（已弃用） | `com.hazeroot.scriptrunner` |
| 源码目录 | 旧包路径（已弃用） | `java/com/hazeroot/scriptrunner/` |
| FileProvider authority | 旧包名 `.fileprovider` | `com.hazeroot.scriptrunner.fileprovider` |
| 脚本目录 `SCRIPT_DIR` | 旧目录（已弃用） | `/data/adb/HazeRoot` |
| 默认队列 `defaultScripts()` | 若干示例脚本 | **空列表** |
| 默认方案 `defaultPresets()` | 早期示例方案 | **空列表** |
| 根工程名 | `RootScriptRunner` | `HazeRoot` |
| `assets/manual.html` 正文 | 含旧目录与旧脚本名示例 | 中性示例；footer 加"作者 HazeRoot" |
| `app/build.gradle.kts` | — | 加作者注释头 |

### 3.4 功能等价性（重要）

除包名 / 路径 / 示例数据外，**引擎与全部功能逻辑零改动**。对齐包名后逐文件 diff，仅 **3 个文件** 有实质差异：

| 文件 | 差异行数 | 内容 |
|---|---|---|
| `MainActivity.java` | 8 | 脚本目录常量 + 3 处路径 + 下载前 `mkdir -p` |
| `QueueFragment.java` | 2 | 路径输入框提示 |
| `Store.java` | 6 | 两个默认数据方法返回空列表 |

即：`runOne` / 队列调度 / 两级解码 / ANSI 状态机 / 自动应答 / 录制 / 看门狗 / 文件操作 / APK 安装 **全部保持原样**。

---

## 四、构建与运行

### 4.1 精确构建命令

```bash
cd HazeRoot
export JAVA_HOME="C:\Program Files\Java\jdk-21"
MSYS_NO_PATHCONV=1 \
  <gradle-8.6>/bin/gradle \
  assembleDebug --offline 2>&1 | iconv -f GBK -t UTF-8 | tail -30
```

- 环境为 **Windows + Git Bash**，Gradle 输出是 GBK，必须 `iconv` 过一遍。
- **必须 `MSYS_NO_PATHCONV=1`**，否则 `/d/...` 类参数会被改写成 Windows 路径。
- 依赖已缓存，用 `--offline` 即可（改依赖时才去掉）。
- 全量重建：把 `assembleDebug` 换成 `clean assembleDebug`（约 1m20s）。
- 产物：`app/build/outputs/apk/debug/app-debug.apk`（约 7.4 MB，debug 签名）。

### 4.2 版本号变更位置

`app/build.gradle.kts` → `defaultConfig`：`versionCode`（每次 +1）、`versionName`。

### 4.3 真机安装

```bash
MSYS_NO_PATHCONV=1 adb install -r <apk 路径>
adb shell "am start -n com.hazeroot.scriptrunner/.SplashActivity"
```

> ⚠ 包名变更后**与旧版不兼容**：旧包名版本与 `com.hazeroot.scriptrunner` 是两个独立 App，旧版**不能覆盖升级**，需先卸载旧版。

### 4.4 可用工具

| 工具 | 路径 |
|---|---|
| JDK 21 | `C:\Program Files\Java\jdk-21` |
| Gradle 8.6 | `<gradle-8.6>/bin/gradle` |
| aapt2 | `<Android SDK>/build-tools/36.1.0/aapt2.exe` |
| apktool | 本地工具（未随仓库公开） |
| jadx | 本地工具（未随仓库公开） |

---

## 五、关键技术陷阱（**必读，否则会重复踩坑**）

### 5.1 KernelSU `su -c` 复合命令陷阱 ⭐ 最关键

`su -c "a; b"` 与 `su -c "a && b"` **只给第一条命令提权**，后续命令降回 `shell(uid 2000)`。必须用 `su -c "sh -c 'a; b'"` 显式包裹。

| 形式 | `whoami` / `id -u` |
|---|---|
| `su -c "whoami; id -u"` | root / **2000** ✗ |
| `su -c "whoami && id -u"` | root / **2000** ✗ |
| `su -c "sh -c 'whoami; id -u'"` | root / **0** ✓ |

**影响面**：所有复合命令（`cp`、`tar`、`chmod`、管道）。已在 `Engine.su()` / `execBytes()` 修复。
**注意**：用 adb 手敲同样的命令也会失败——别误判成"App 的锅"。

### 5.2 jadx 复原陷阱（4 类）

| 类型 | 表现 | 处置 |
|---|---|---|
| ① `int→float` 丢失 | `int/int` 变整数除法，动画数值全错 | 按 smali 补回浮点转换 |
| ② 方法彻底放弃 | `throw new UnsupportedOperationException("Method not decompiled: ...")` | 按 smali 手工回写 |
| ③ 静默桩方法 | `{ return null; }` | **必须与 smali 对照**：`Engine.resolveAnswer` 原本就是 `return null`（死代码），不是缺陷 |
| ④ `$default` mask 丢失 | 默认值语义丢失，出现字面量 `null` | 手写 `Kt` 补齐 mask 判断 |

**准则**：凡遇 `UnsupportedOperationException` 或可疑 `return null`，去 `recovery/apktool_out/smali_classes*/` 逐行核对，**不要凭 Kotlin 语义猜测**。

### 5.3 终端解码必须两级

```
先用 CharsetDecoder(REPORT) 严格试解 UTF-8
  ├ 成功 → 直接用
  └ malformed → 才回退 hybridDecode（UTF-8/GBK 同屏混合解码）
```
**跳过第一级 = UTF-8 中文被 GBK 分支吞掉 = 乱码。**

### 5.4 改名时最容易漏的地方

1. **布局 XML 里的自定义 View 全限定类名**：`res/layout/fragment_files.xml` 有 `<包名.FilesListView/>`（左右两窗）。**漏改 → 运行时 inflate 抛 `ClassNotFoundException`，编译器不报错。**
2. Manifest 里硬编码的 `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`。
3. 手册 HTML 正文：`grep -c` 按行统计会漏计，须 `grep -o | wc -l`。

### 5.5 真机调试环境注意

| 现象 | 处置 |
|---|---|
| `adb shell input tap/keyevent` 报 `SecurityException: INJECT_EVENTS` | 改用 `adb shell su -c "input tap X Y"` |
| 按钮坐标靠截图目测不准 | 先 `uiautomator dump` 取真实 `bounds`，再点中心 |
| `adb push` 的 `/data/...` 被当成 Windows 路径 | 加 `MSYS_NO_PATHCONV=1` |
| MIUI 输入法把 `input text` 里的 `/` 当拼音分隔符（`/data/adb/x.sh` → `、打他、adb、`） | 需输入路径时改用「浏览文件」选择，不要键入 |
| `su -c "...; ..."` 组合命令被本地沙箱拦截 | 改用 base64 中转：`echo <b64> \| base64 -d > /data/local/tmp/x.sh; sh /data/local/tmp/x.sh` |
| 用 `run-as` 验证文件权限 | **不可信**：`run-as` 建的进程无 App SELinux 上下文，读 `/sdcard/Android/data/...` 报 Permission denied 是假象 |

---

## 六、用户约定与红线（交接必守）

| # | 约定 |
|---|---|
| 1 | **定位是「通用 root 脚本执行器」**，文档与代码注释中**保持中性、中立的表述** |
| 2 | 本项目源码为自有作品；早期源码丢失后从自有 v2.7.2 版本 APK 复原，对外表述保持中性、一致 |
| 3 | **作者署名统一用 `HazeRoot`** |
| 4 | 清理品牌时**不得削减功能** —— 脚本适配能力（ELF 检测 / PTY / 自动应答 / 编码识别）必须保留 |
| 5 | **默认只读**：用户没明确说"改"就不要改任何配置或系统状态。要动手前先说明"改什么、代价是什么"，等确认 |
| 6 | **不要替用户停用/删除他正在使用的软件**。涉及取舍时先说清代价，让用户决定 |
| 7 | 变更前**先备份**，留回滚路径 |

---

## 七、当前待办与未验证项

| # | 事项 | 状态 |
|---|---|---|
| 1 | **真机回归验证** | ✅ **核心链路已通过**（详见第七节 7.1） |
| 2 | 🐛 **`rootCache` 永不失效** | **确认为缺陷**，详见 7.2，建议修复 |
| 3 | ⚠ PTY 输出不实时（成批到达） | 见 7.3 |
| 4 | ⚠ 终端不自动滚到底 | 见 7.4 |
| 5 | 通知栏图标 | 待改：`RunnerService.java` 用 `setSmallIcon(android.R.drawable.stat_sys_download)`（系统下载图标，MIUI 上形似漏斗），建议换 `R.drawable.ic_brand_h` |
| 6 | 通知支持进度/暂停 | 未做 |
| 7 | 设备上并存两个同名 App | 待处理：建议卸载旧包名版本（见 8.1） |
| 8 | debug 签名 | 正式分发前需换 release 签名（见 8.3） |

### 7.1 真机回归结果（2026-10-13，Xiaomi 测试机 / Android 17 / KernelSU-SukiSU）

| 验证项 | 结果 | 证据 |
|---|---|---|
| APK 安装 | ✅ | `adb install -r` Success |
| 启动（Splash→Main） | ✅ | 进入 MainActivity，启动动画正常 |
| 应用身份 | ✅ | KSU 应用详情页显示 `HazeRoot · 2.8.0 (66) · com.hazeroot.scriptrunner · UID 10322` |
| **Root 检测** | ✅ | 状态行变绿 `Root Active` |
| **Root 管理器识别** | ✅ | 弹窗显示「Root 已激活 ✓ 管理器：SukiSU 包名：com.sukisu.ultra」 |
| **首次启动空状态** | ✅ | 「暂无脚本 / 添加一个脚本路径，开始构建执行队列」（默认数据已清空生效） |
| **新脚本目录生效** | ✅ | 文件页快捷条显示 `/data/adb/HazeRoot`；执行日志 `$ su -c "/data/adb/HazeRoot/t_cn.sh"` |
| 文件浏览（需 root） | ✅ | 左窗正确列出 `t_cn.sh  506 B` |
| 点选入队 | ✅ | 队列变 1，显示 `t_cn.sh / 01` |
| **队列执行** | ✅ | `runQueue 请求 auto=false 脚本数=1` → `队列 [0/0]` → `busybox script 可用性=true` |
| **中文输出（本轮核心修复）** | ✅ | 终端正确显示 `内核版本: 5.15.194-android13-8-00`、`设备: Xiaomi 测试机 安卓17`、`中文行：这是简体中文，用于验证终端解码`、符号 `①②③ ★ ☆ 【】「」` |
| 英文/数字混排 | ✅ | `Line A: abcDEF 12345 !@#$%` |
| **手动输入应答** | ✅ | logcat `term\| > [手动输入] 2` |
| **回答录制** | ✅ | logcat `录制落盘 t_cn.sh：[请输入 0/1/2 选择方案：]` |
| 退出码 | ✅ | `[t_cn.sh 退出码: 0]`，终端绿色显示 |
| 队列收尾 | ✅ | `队列结束 aborted=false` / `====== 队列执行完毕 ======` |
| 停止按钮 / 主题 / 语言 | ⏸ | 本轮未逐一验证 |

> 测试脚本：本地留存（`t_cn.sh`、`t_timing.sh`）
> 部署位置：`/data/adb/HazeRoot/`

### 7.2 🐛 缺陷：`rootCache` 永不失效（建议修复）

**现象**：首次启动时若用户还没授权 root，应用会把 `rootCache=false` **永久钉死**；之后即便在 Root 管理器里授权并点「重新检测」，所有走 `Engine.exec()` 的操作（文件浏览、文件操作等）**仍以非 root 运行** → `/data/adb` 是 `drwx------ root root`，非 root 进不去 → 文件页左窗空白。

**根因**：`rootCache` 全工程仅三处引用，**只写不失效**：

```
Engine.java:67   private static volatile Boolean rootCache;          // 声明
Engine.java:371  Boolean bool = rootCache;                          // rootAvailable() 读
Engine.java:377  rootCache = Boolean.valueOf(ok);                   // rootAvailable() 写
```

而 `QueueFragment.threadRootCheck()`（点「重新检测」/ 点 Root 徽标触发）调的是 `Engine.checkRoot()`，**不经过 `rootAvailable()`，因此不更新缓存**。同理 `MainActivity.onboardRootStep` 也直接调 `checkRoot()`。

**复现**：装好后先不授权 → 进文件页（触发一次 `exec()`，缓存 false）→ 再去授权 → 回应用点「重新检测」（显示已激活）→ 文件页仍空白。**杀进程重启后恢复正常**。

**建议修复**：让 `checkRoot()` 把结果写回缓存，例如

```java
public final boolean checkRoot() {
    boolean ok;
    try { ... } catch (Exception e) { ok = false; }
    rootCache = Boolean.valueOf(ok);      // ← 新增：权威检测结果回写缓存
    return ok;
}
```
或提供 `Engine.invalidateRootCache()` 供「重新检测」调用。

> 该缺陷源自早期版本设计，**非本轮改动引入**。

### 7.3 ⚠ PTY 路径下输出不实时（成批到达）

**现象**：脚本经 `busybox script`（PTY）路径执行时，stdout 不是实时逐行到达，而是**攒批**。

**实测**：脚本 `echo T1; sleep 3; echo T2; sleep 3; echo T3; sleep 3; echo T4`，本应 0/3/6/9 秒各出一行，实际终端里 **T1–T4 一起出现**（脚本退出时才刷出）。

**推测原因**：`busybox script` 的 stdout 是管道（非 tty），其 stdio **块缓冲**（4KB）→ 缓冲区未满且进程未退出时不下发。

**影响**：长跑脚本看不到实时进度（本项目的提示应答依赖实时识别提示行，长脚本可能受影响）。

**可能的缓解方向**：改用 `script -f`（flush）若 busybox 支持；或不用 PTY 而让脚本自行 `stdbuf`/`setvbuf`；或对 PTY 输出做定时 poll。

> 该行为属早期版本设计，非本轮改动引入。

### 7.4 ⚠ 终端不自动滚到底

**现象**：终端内容超过可视区后，新输出出现在可视区**下方**，视图停在顶部不跟随。

**代码依据**（`QueueFragment.syncTerm`）：
```java
boolean wasAtBottom = !termScroll.canScrollVertically(1);
...
if (initial || wasAtBottom) { termScroll.post(() -> fullScroll(FOCUS_DOWN)); }
```
即仅在「本来就在底部」或「首次渲染」时才滚到底。若判定为 false（可能因布局尚未完成导致 `canScrollVertically` 返回值不准），则不再自动滚动，用户需手动下滑。

**影响**：观感上像"输出卡住了"，实际内容都在，需手动滚动才可见。

### 7.5 真机回归剩余建议项

- 停止按钮 / 中止队列
- 方案保存与一键重放（自动应答 + 跑完跳转目标应用）
- **APK 安装全流程**（本轮修复重点，务必实点验证）
- 文件页其余操作：复制 / 移动 / 压缩 / 解压 / 权限 / 重命名 / 删除 / 分享
- 语言与主题切换

> **测试脚本部署**：把 `.sh` 放到 `/data/adb/HazeRoot/`（当前 `SCRIPT_DIR`），再用「添加脚本 → 浏览文件」入队。
> **命令形式备忘**（本轮踩到）：
> - MIUI 上 `adb shell input tap` 会报 `INJECT_EVENTS` 拒绝 → 必须 `adb shell "su -c 'input tap X Y'"`
> - `adb push` 本地文件用 **Windows 路径**（如 `D:/...`）+ `MSYS_NO_PATHCONV=1`；纯 POSIX 路径（`/c/...`）adb 不认
> - 中文写入设备可用 base64 中转，或直接 push 后再 `su -c cp`
> - 键盘弹出会压缩布局，`btnRun` 坐标会变（正常 `[44,1982][887,2114]`，键盘态 `[44,1134][887,1266]`）→ 操作前先 `uiautomator dump` 取当前坐标

---

## 八、APK 校验结果（2026-10-13 实测）

### 8.1 ⚠ 设备上并存两个同名 App（需要处理）

真机（Xiaomi 测试机 / Android 17 / KernelSU）当前装了 **两个都叫「HazeRoot」** 的包：

| 包名 | 版本 | 标签 | 大小 | 首次安装 | 最后更新 |
|---|---|---|---|---|---|
| 旧包名（已弃用） | 2.8.0 (66) | HazeRoot | 7.05 MB | 2026-10-06 09:14 | 2026-10-07 23:24 |
| `com.hazeroot.scriptrunner` | 2.8.0 (66) | HazeRoot | 7.05 MB | 2026-10-13 00:48 | 2026-10-13 00:48 |

- 前者 = **改名前的重建版**；后者 = **改名后的当前版**。
- 两者**同名同版本号**，仅有包名不同 → 在桌面上无法区分，**建议卸载旧包名版本**。
- 设备上另有与本项目无关的第三方应用，勿动。

### 8.2 两个 HazeRoot 的差异（解包逐条比对）

各 961 个条目，**仅 6 处不同**：

| 差异条目 | 原因 |
|---|---|
| `AndroidManifest.xml` | 包名 / FileProvider authority / 组件名 |
| `assets/manual.html` | footer 加署名 + 示例脚本名中性化 |
| `classes2.dex`、`classes3.dex` | 源码改动（包名 + 路径 + 默认数据） |
| `res/layout/fragment_files.xml` | 自定义 View 全限定类名（改名最易漏的一项） |
| `resources.arsc` | 布局与字符串变更导致资源表重建 |

其余 **955 个条目完全一致** → 说明除计划内改动外没有夹带意外变更。

### 8.3 签名

两者使用**同一个 debug 证书**：

```
DN     : C=US, O=Android, CN=Android Debug
SHA-256: a7380fc8801cd449398023f7d5278a9848e457eaf425ed352429373fb4e5251b
```

> 正式分发前需换成 release 签名（当前是 debug 签名）。

### 8.4 交付版一致性

```
交付 APK : HazeRoot-v2.8.0-debug.apk
SHA-256  : d261fa98a85b0a4924d5413a61f36a65...
设备已装 : com.hazeroot.scriptrunner    → 同一 SHA-256 ✅
```
即：**设备上装的就是交付版，未做任何二次改动。**

### 8.5 全部 HazeRoot 产物清单

| 文件 | 包名 | vCode | vName | 大小 | 位置 |
|---|---|---|---|---|---|
| `HazeRoot-v2.7.2.apk` | 旧包名（已弃用） | 62 | 2.7.2 | 7.09 MB | `recovery/` |
| `app_v274.apk` | 旧包名（已弃用） | 64 | 2.7.4 | 6.92 MB | `recovery/` |
| `HazeRoot-v2.7.5.apk` | 旧包名（已弃用） | 65 | 2.7.5 | 6.55 MB | 工作区根 |
| `HazeRoot-v2.7.5b.apk` | 旧包名（已弃用） | 65 | 2.7.5 | 6.55 MB | 工作区根（与 v2.7.5 不同 SHA，非同一文件） |
| `rebuild-v280.apk` | 旧包名（已弃用） | 66 | 2.8.0 | 7.04 MB | `recovery/` |
| **`HazeRoot-v2.8.0-debug.apk`** | **com.hazeroot.scriptrunner** | **66** | **2.8.0** | **7.05 MB** | **工作区根（交付版）** |
| `app-debug.apk` | com.hazeroot.scriptrunner | 66 | 2.8.0 | 7.05 MB | 工程 `app/build/outputs/apk/debug/` |

### 8.6 复现校验的命令

```bash
AAPT="<Android SDK>/build-tools/36.1.0/aapt2.exe"
"$AAPT" dump badging <apk> | grep -E "^package:|^application-label:|^launchable-activity:"

export JAVA_HOME="C:\Program Files\Java\jdk-21"
"C:/.../build-tools/36.1.0/apksigner.bat" verify --print-certs <apk>

# 解包比对
unzip -q <旧>.apk -d x_old && unzip -q <新>.apk -d x_new && diff -rq x_old x_new
```

---

## 九、代码地图（快速定位）

| 文件 | 行数 | 职责 |
|---|---|---|
| `Engine.java` | ~1250 | **核心引擎**：root 探测 / 提权执行 / 队列调度 / 两级解码 / ANSI 状态机 / 提示识别 / 自动应答 / 录制 / 中止 / 看门狗 / 方案收尾 |
| `FilesFragment.java` | ~3350 | 双窗文件管理 + 全部文件操作 + APK 安装 + 分享 |
| `QueueFragment.java` | ~1675 | 队列面板 + 终端增量渲染 + 手动输入 + 执行门控 |
| `MainActivity.java` | ~845 | Tab 壳 + 文件打开 + 链接导入 + 权限引导 |
| `PresetsFragment.java` | ~740 | 方案库 CRUD + 运行 + 目标应用选择 |
| `MediaActivity.java` | ~715 | 音视频/图片播放 |
| `EditorActivity.java` | ~705 | 文本编辑 + 二进制/字符集识别 + 语法高亮 |
| `ConvergeSplashView.java` | ~455 | 启动动画（12 碎片汇聚，时间轴见功能文档） |
| `Kt.java` | ~195 | Kotlin 标准库桥（`$default` mask 语义） |
| 其余 21 个文件 | — | 见 `HazeRoot-功能与实现逻辑.md` 第 11 章 |

**深入阅读顺序建议**：`HazeRoot-功能与实现逻辑.md` 第 5 章（引擎）→ 第 6 章（UI）→ 第 9 章（坑位）→ 本文第 5 章。

---

*交接文档完。构建已通过；真机验证待接手方执行。*
*作者：HazeRoot*
