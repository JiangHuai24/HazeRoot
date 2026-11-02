# HazeRoot 版本管理与灾难恢复方案

> 核心命题：解决「版本号迭代太快 + 推 GitHub 太频繁，但又怕本地丢失」的矛盾。
> 根因：**把「备份/防丢失」和「发版」耦合在一起了**。每次小修都升版本号 + push，导致版本号膨胀、提交历史全是 `vX.Y.Z` 噪声。

---

## 一、三层解耦（核心思想）

| 层 | 动作 | 频率 | 目的 | 动版本号？ |
|---|---|---|---|---|
| **① 保存** | 本地 `git commit` | 每改完一个点就 commit（可以很频繁） | 记录变更、可回滚 | ❌ 不动 |
| **② 备份** | `git push` + 本地定时快照 | 每次收工前 / 每日 | 防丢失、能找回 | ❌ 不动 |
| **③ 发版** | 升 `versionCode`/`versionName` + 打 tag + 写 Release | 一个稳定里程碑一次 | 标记"可交付版本" | ✅ 才动 |

**一句话**：`commit` 和 `push` 是「保险」，`tag` 和版本号才是「发版」。两者彻底分开，互不绑架。

---

## 二、Commit 约定（Conventional Commits）

不再在 commit message 里写 `v2.8.4`，改用「类型 + 描述」：

```
<type>: <描述>

类型：
feat     新功能
fix      修 bug
refactor 重构（不改行为）
style    样式/UI（不影响逻辑）
docs     文档
chore    杂项（构建/脚本/依赖）
perf     性能
test     测试
wip      未完成的中间态（后续 squash 掉）
```

示例：
```
feat: 文件页长文件名改两行折行 + 长按看全名
fix: 终端增量渲染在 buffer 裁剪后错位
refactor: 拆分 Engine 的 ANSI 状态机为独立类
```

**为什么**：commit message 描述"改了什么"，版本号只由 tag 承载。这样 GitHub 提交历史干净、可检索、可 cherry-pick。

---

## 三、版本号何时升（发版判据）

**只有同时满足以下条件，才升版本号 + 打 tag：**

1. 累积了一批功能/修复（不是一个单点改动）；
2. **真机自测通过**（启动 / 队列执行 / 终端中文 / 文件页 / APK 安装 / 方案重放 至少跑过一遍）；
3. 用户确认"这一版可以交付"。

**版本号规则（SemVer 简化版）：**

| 变更 | versionName | versionCode |
|---|---|---|
| 修 bug（不新增功能） | 2.8.5（修订+1） | +1 |
| 新增功能（向后兼容） | 2.9.0（次+1，修订归0） | +1 |
| 大重构/不兼容 | 3.0.0（主+1） | +1 |

**关键点**：`versionCode` 是 Android 安装器判断"能否覆盖安装"的字段，必须**单调递增**；`versionName` 是给人看的，按语义走。**两者都只在发版时改，测试期间 `adb install -r` 可覆盖同 versionCode 的 debug 包，不必为每次测试升号。**

---

## 四、Git 工作流（个人 + AI 协作，单 main 最省事）

```
日常开发（随时）：
    git add -A
    git commit -m "fix: xxx"        ← ① 保存，频繁无妨

收工前 / 每天（防丢失）：
    git push origin main            ← ② 备份，频繁无害（但不动版本号）

真正发版（低频）：
    1. 改 app/build.gradle.kts 的 versionCode/versionName
    2. 真机自测通过
    3. git commit -m "chore: 发版 v2.9.0"
    4. git tag -a v2.9.0 -m "v2.9.0：新增文件页长文件名折行 + 终端渲染修复"
    5. git push origin main --tags   ← ③ 发版，低频
    6. 写 Release Notes（可选：附 APK + 源码 tar 作为双保险）
```

**为什么不建议多分支**：个人开发 + AI 协作场景，多分支（dev/main）会增加合并负担，且 AI 容易在错误分支上改。单 `main` + tag 发版，配合「commit 随意、push 频繁、tag 低频」已经足够。

---

## 五、灾难恢复强化（git 之外再加两层保险）

git push 已经是远端备份，但为防「误删 commit / 强推 / 本地和远端同时损坏」，再加：

### 5.1 本地定时快照（自动化）
用 `scripts/backup.ps1`（已落地）：
- 打包「源码（排除 build/.gradle）+ docs + APK 产物 + README/LICENSE」为带时间戳的 zip；
- 输出到 `D:\WorkBuddy\backup-auto\HazeRoot-YYYYMMDD-HHmmss.zip`；
- 保留最近 N 份，自动清理更早的；
- 可挂 Windows 计划任务每日自动执行。

### 5.2 发版时附产物（GitHub Release 双保险）
发版 tag 时，把 `app-debug.apk` 和源码 zip 作为 Release 附件上传。这样即使某天 git 历史被重写，Release 附件里的 APK 和源码包仍在。

---

## 六、落地清单

| 项 | 状态 |
|---|---|
| 方案文档（本文件） | ✅ |
| `scripts/backup.ps1` 自动快照脚本 | ✅ 已落地 |
| commit 约定（Conventional Commits） | ✅ 本文档定义 |
| 发版判据 + 版本号规则 | ✅ 本文档定义 |
| 当前 git 状态 | main 分支，2 提交，1 tag `v2.8.4`，versionCode 70 / 2.8.4（无需立即动） |

---

## 七、给你的一句话决策规则

> **改完就 `commit`，收工就 `push`，自测通过且要交付才「升版本号 + 打 tag」。**
> 版本号只回答一个问题：「这是不是一个新的、可交付的版本？」——不是的话，别动它。

这样：版本号不再膨胀、GitHub 历史干净、本地丢了随时能从 GitHub + 自动快照 + Release 附件三处找回。
