# HazeRoot 工程化重构看板

> 依据：PRD + ARCHITECTURE.md + 工程化重构方案
> 说明：任务卡格式 `[ ] 卡号 标题【角色】依赖：... AC：... DoD：...`

---

## 里程碑 M0 — 工程地基（进行中）

**目标**：装上「彻底摒弃走一步看一步」的强制闸门——CI + 静态检查 + 测试骨架，不碰业务代码。

**验收标准（M0 完成定义）**：
- [x] 基线 `assembleDebug` 可编译（构建中验证）
- [ ] GitHub Actions CI 工作流（lint + assembleDebug）
- [ ] Checkstyle 接入（类行数/方法行数硬门禁）
- [ ] 测试骨架（JUnit 单测可跑）
- [ ] `.gitignore` 覆盖 build/.gradle/backup 产物

### 任务卡

- [ ] M0-1 建立 CI 工作流 `.github/workflows/ci.yml`【发布工程师】
      依赖：—　AC：push 触发 lint+assembleDebug　DoD：CI 配置落地且可解析
- [ ] M0-2 接入 Checkstyle 门禁（>300 行类 / >40 行方法）【架构师→工程师】
      依赖：—　AC：新类超限被拦　DoD：checkstyle 配置 + gradle 接入 + 文档
- [ ] M0-3 建立 JUnit 测试骨架【工程师】
      依赖：—　AC：`gradlew test` 可跑 0 失败　DoD：测试目录 + 示例测试 + CI 纳入 test
- [ ] M0-4 补 `.gitignore`（build/.gradle/backup-auto）【工程师】
      依赖：—　AC：构建产物不入库　DoD：git status 无 build 产物

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
