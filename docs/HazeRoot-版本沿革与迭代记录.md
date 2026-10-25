# HazeRoot 版本沿革与迭代记录

> 作者：**HazeRoot**
> 文档用途：项目介绍 + 版本迭代档案
> 说明：更早期的开发记录已丢失，本项目以 **v2.7.2 作为第一代基线版本** 起算。

---

## 一、项目简介

**HazeRoot** 是一个 Android 平台的**通用 root 脚本执行器**（方向类比 MT 管理器）。

核心能力：

| 能力 | 说明 |
|---|---|
| **脚本执行** | 以 root 身份运行 shell 脚本（`.sh`）或 ELF 可执行文件，支持队列按顺序执行 |
| **交互应答** | 脚本提问时在终端给出橙色提示行，用户在下方输入框回答；支持自动录制回答 |
| **一键方案** | 把整条队列连同"回答记录"存成"方案"，之后一键重放，同提示自动回填答案 |
| **终端** | 完整 ANSI 颜色解析 + UTF-8/GBK 双编码自动识别，中文不乱码 |
| **文件管理** | 双窗文件管理（浏览 / 复制 / 移动 / 压缩 / 解压 / 权限 / 重命名 / 删除 / 分享 / APK 安装） |
| **编辑与预览** | 内置脚本编辑器（语法高亮 + 字符集自动识别）、媒体播放、图文使用手册 |

**定位**：把"每次手动敲一堆回答"变成"点一下跑完"。面向需要反复在 root 环境执行同一套脚本的用户。

**技术底座**：Java 17 · minSdk 26 / targetSdk 34 · AppCompat + Material（无 Compose）· 全局单例 `Engine` 驱动，无 ViewModel/LiveData。

---

## 二、版本代号约定

| 术语 | 含义 |
|---|---|
| **第一代** | v2.7.2（versionCode 62）—— 目前可追溯的最早版本，作为基线 |
| 版本号 | `主.次.修订`（如 `2.8.0`）；每次发版 `versionCode` 递增 1 |
| 当前版本 | **v2.8.4**（versionCode 70） |

> 早期开发记录（v2.7.2 之前的迭代过程）已丢失，故以 v2.7.2 为第一代重新建立档案。

---

## 三、版本时间线

| 版本 | versionCode | 包名 | 产物归档 | 状态 |
|---|---|---|---|---|
| **v2.7.2（第一代）** | 62 | 旧包名（已弃用） | `HazeRoot-v2.7.2.apk`（本地归档） | 基线，旧包名 |
| v2.7.3 | 63 | — | **产物缺失** | 记录缺失（按 versionCode 推断存在） |
| v2.7.4 | 64 | 旧包名（已弃用） | `app_v274.apk`（本地归档） | 旧包名 |
| v2.7.5 | 65 | 旧包名（已弃用） | `HazeRoot-v2.7.5.apk` / `HazeRoot-v2.7.5b.apk` | 旧包名 |
| **v2.8.0** | 66 | `com.hazeroot.scriptrunner` | `app/build/outputs/apk/debug/app-debug.apk` | **源码复原 + 去品牌化** |
| v2.8.1 | 67 | `com.hazeroot.scriptrunner` | 同上 | 修复双窗同目录点击无反应 |
| v2.8.2 | 68 | `com.hazeroot.scriptrunner` | 同上 | 修复 root 误判与乱弹窗 |
| v2.8.3 | 69 | `com.hazeroot.scriptrunner` | 同上 | su 重试 + 错误文案修正 |
| **v2.8.4（当前）** | 70 | `com.hazeroot.scriptrunner` | `HazeRoot-v2.8.4.apk` | su 串行化，文件页稳定 |

---

## 四、当前版本（v2.8.0）迭代说明

v2.8.0 是"源码复原 + 品牌统一"的合并版本，主要工作分两部分。

### 4.1 源码复原

早期源码丢失，基于自有的 v2.7.2 版本 APK 复原出完整可编译工程（apktool + jadx），并对 jadx 复原失败的方法按 smali 逐行手工回写。

复原期修复的阻断级缺陷：

| # | 缺陷 | 根因 | 影响 |
|---|---|---|---|
| 1 | 脚本无法执行 | `Engine.runOne` 被 jadx 放弃（`UnsupportedOperationException`） | 点执行直接抛异常，脚本完全不跑 |
| 2 | 终端中文乱码 | 复原时漏掉"严格 UTF-8 先行解码"，UTF-8 中文被 GBK 分支误解码 | 中文显示为 `ɂȂ肻` 类乱码 |
| 3 | 队列摘要显示 `nullnullnull` | `Kt.joinToString` 未实现 Kotlin `$default` 的 mask 语义 | 折叠态摘要行错误 |
| 4 | APK 安装失败 | (a) `su -c` 复合命令只给首条命令提权；(b) 产物属主为 root，FileProvider 以应用 uid 读取被拒 | 调不起系统安装器 |

### 4.2 品牌统一（v2.8.0-rename）

清理早期版本遗留的示例数据与外部品牌引用，统一为自有品牌：

| 项目 | 改前 | 改后 |
|---|---|---|
| 包名 / namespace / applicationId | 旧品牌包名 | `com.hazeroot.scriptrunner` |
| FileProvider authority | 旧包名 `.fileprovider` | `com.hazeroot.scriptrunner.fileprovider` |
| 脚本目录 `SCRIPT_DIR` | 旧群组目录 | `/data/adb/HazeRoot` |
| 默认队列 / 默认方案 | 若干示例脚本与方案 | **空列表**（首次启动走空状态引导） |
| 根工程名 | `RootScriptRunner` | `HazeRoot` |
| 使用手册正文 | 含旧目录与旧脚本名示例 | 改为中性示例 |

**功能等价性**：除包名 / 路径 / 示例数据外，**引擎与全部功能逻辑零改动**。逐文件 diff 仅 `MainActivity`(8 行) / `QueueFragment`(2 行) / `Store`(6 行) 三处实质差异，均为上述清理项。

> ⚠ **升级注意**：v2.8.0 包名变更后签名与身份都变了，**旧版本无法覆盖升级**，需先卸载再安装。手机里原有的旧脚本目录也不会自动迁移。

### 4.3 v2.8.1 – v2.8.4 稳定性修复

| 版本 | 缺陷 | 根因 | 修复 |
|---|---|---|---|
| v2.8.1 | 左右两窗打开同一目录（或左窗为右窗父目录）时，点右窗行无反应 / 误弹「新建」 | 用「路径前缀 startsWith」判定行属于哪个窗格，右窗的行命中了左窗 → getPositionForView 返回 -1 → 取到 null → 走空白菜单 | 改用 getPositionForView 逐窗探测；pos < 0 时直接忽略 |
| v2.8.2 | 明明已授权却提示无 root、乱弹「需要 root」引导窗 | rootCache 只写不失效，冷启动时 KernelSU 授权未就绪的一次误判被永久钉死；checkRoot 无重试无超时 | checkRoot 3 次重试 + 3s 超时并写缓存；缓存分极性（true 长期 / false 15s TTL）；新增 refreshRoot；回前台与弹窗前强制重探 |
| v2.8.3 | 文件页偶发「无法读取目录（无 root / 命令失败）」 | 两窗并行加载 + ELF 批量探测同时发起多个 su 进程，KernelSU 高并发下拒绝/超时 | su / execBytes 增加 3 次重试；错误文案改为「读取失败，请重试」 |
| v2.8.4 | 上述并发问题仍偶发 | 重试不能解决并发本身 | 所有 su 调用加全局串行锁 SU_LOCK，彻底消除并发风暴 |

**压测结论（v2.8.4）**：连续 5 次冷启动进入文件页，5/5 正常（Root Active、目录正常列出、零错误提示、零多余弹窗）；双窗同目录 / 父子目录点击回归均通过。

---

## 五、迭代规范

### 5.1 版本号变更位置

`app/build.gradle.kts`：

```kotlin
defaultConfig {
    applicationId = "com.hazeroot.scriptrunner"
    versionCode = 66          // 每次发版 +1
    versionName = "2.8.0"     // 主.次.修订
}
```

### 5.2 构建流程

```bash
export JAVA_HOME="C:\Program Files\Java\jdk-21"
MSYS_NO_PATHCONV=1 <gradle-8.6>/bin/gradle assembleDebug --offline
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

### 5.3 发版检查清单

- [ ] `versionCode` 递增、`versionName` 更新
- [ ] `clean assembleDebug` 通过
- [ ] `aapt2 dump badging` 确认包名 / 版本号正确
- [ ] 真机回归：队列执行 / 终端中文 / 文件页操作 / APK 安装 / 方案重放
- [ ] APK 归档 + 源码备份

### 5.4 归档位置

| 内容 | 路径 |
|---|---|
| 工程源码 | 本仓库 |
| 源码备份（品牌统一前） | 本地归档 |
| 历史 APK | 本地归档 |
| 复原参考素材 | 本地归档（apktool / jadx 产物） |

### 5.5 技术文档

功能与底层实现的完整说明见：`HazeRoot-功能与实现逻辑.md`（11 章，含引擎算法、ANSI 状态机、编码识别、坑位清单）。

---

## 六、后续迭代方向（待定）

按性价比排序：

1. **回归测试自动化** —— adb 冒烟脚本（启动→入队→执行→断言 logcat→截图对比），把回归从"手动点"变成"一条命令"。
2. **持久 root shell 会话** —— 把每个脚本一次 `su` 冷启动换成常驻 shell 复用，同时获得变量/环境共享能力。
3. **脚本包 + 在线源** —— 定义包格式（脚本 + 元数据 + 校验和）+ 在线仓库 + 深链一键安装。
4. **结构化输出协议** —— 约定 `@@PROGRESS:n` / `@@RESULT:k=v` 标记，把终端刷屏变成进度条与结果卡片。
5. **体验补齐** —— 通知栏品牌图标（当前用的是系统"下载"图标）、进度/暂停、悬浮球、定时任务。

---

*文档随版本迭代更新。最近更新：v2.8.0*
*作者：HazeRoot*
