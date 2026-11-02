# HazeRoot 工程化重构看板

> 依据：PRD + ARCHITECTURE.md + 工程化重构方案
> 说明：任务卡格式 `[ ] 卡号 标题【角色】依赖：... AC：... DoD：...`

---

## 里程碑 M0 — 工程地基（进行中）

**目标**：装上「彻底摒弃走一步看一步」的强制闸门——CI + 静态检查 + 测试骨架，不碰业务代码。

**验收标准（M0 完成定义）**：
- [x] 基线 `assembleDebug` 可编译
- [x] GitHub Actions CI 工作流（lint + assembleDebug）
- [x] 本地行数门禁脚本（类行数 ≤300，存量 God Object 豁免 8 个）—— 零依赖版，替代离线不可用的 Checkstyle
- [ ] JUnit 测试骨架（离线环境无 junit 依赖缓存，改由 CI 云端 `testDebugUnitTest` 承担；本地先以 lint-size.py 门禁）
- [x] `.gitignore` 覆盖 build/.gradle/backup-auto

### 任务卡

- [x] M0-1 建立 CI 工作流 `.github/workflows/ci.yml`【发布工程师】
      依赖：—　AC：push 触发 lint+assembleDebug　DoD：CI 配置落地且可解析
- [x] M0-2 接入本地行数门禁（>300 行类）【架构师→工程师】
      依赖：—　AC：新增类超限被拦　DoD：lint-size.py + 文档 + 豁免清单登记
- [ ] M0-3 建立 JUnit 测试骨架【工程师】（离线无依赖，改由 CI testDebugUnitTest 承担）
      依赖：—　AC：CI 云端 test 0 失败　DoD：CI 纳入 testDebugUnitTest
- [x] M0-4 补 `.gitignore`（build/.gradle/backup-auto）【工程师】
      依赖：—　AC：构建产物不入库　DoD：git status 无 build 产物

### 已额外完成（UI 落地 v1，作者已确认「紫色换掉」）

- [x] FR-7 部分：品牌色 紫→冷青蓝 #3B6FE0（浅色/夜间两套）
- [x] FR-7 部分：底部导航去彩色胶囊 → 单色极简（选中亮/未选中灰）
- [x] FR-7 部分：提示行 + 运行态 → 琥珀暖色 #E8A33D
- [x] 视觉稿 v1（HTML，可交互，供作者确认方向）

### 风险登记

| 风险 | 等级 | 缓解 | 状态 |
|---|---|---|---|
| 现 3 个 God Object 超 300 行触发门禁 | 高 | M0 只装门禁「只对新增类生效」，存量类豁免清单登记 | 待处理 |

---

## 里程碑 M1 — 插接缝（待 M0）

- [ ] M1-1 引入 SuGateway 接口 + 转发【架构师→工程师】
- [ ] M1-2 补特性测试锁解码/应答/状态行为【工程师】
- [ ] M1-3 引入 ProcessExecutor 接缝【工程师】

## 里程碑 M2 — 拆 Engine（待 M1）

（见 ARCHITECTURE.md §8 完整映射，此处省略占位）

## 里程碑 M3 — 终端渲染专项（待 M2）

## 里程碑 M4 — 文件交互专项（待 M2，可与 M3 并行）

## 里程碑 M5 — 页面联动（待 M2）

## 里程碑 M6 — 队列/方案/录制闭环（待 M5）

## 里程碑 M7 — UI 换肤 + 收尾（待作者确认 D1–D3）
