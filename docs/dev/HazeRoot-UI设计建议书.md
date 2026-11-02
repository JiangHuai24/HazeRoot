# HazeRoot UI/UX 专业建议书（权威完整版）

> 面向：非设计师出身的 Android 开发者 · 对象：HazeRoot「通用 root 脚本执行器」
> 技术约束：Java 17 + AppCompat + Material 1.11 + ConstraintLayout（**无 Compose**）
> 作者审美锚点：**Apple 原生 App 式克制质感**（方向没错，执行没到位）· 干净 · 有质感 · 符合人类使用直觉 · **无 AI 味** · 内敛的"技术炫技"作品牌记忆锚点

---

## 第一层：全网调研结论

> 说明：本轮 web_search 因缺少 API key 不可用，全部采用 web_fetch 直采权威源。

### 1.1 2024–2026 工具类/开发者类 App 的 UI 趋势

| 趋势 | 内容 | 对 HazeRoot 的启示 |
|---|---|---|
| **Material 3 成为 Android 基线** | Color Roles（token）+ Tonal Palette + Shape scale 三位一体 | 现状用 M3 组件但没用 M3 token 体系，这是风格漂移的技术根因 |
| **Material 3 Expressive（2025）** | 异形形状/弹性动效/情绪化组件，面向消费级 | HazeRoot 是生产力工具，取 M3 的 token 纪律、克制 Expressive |
| **iOS 26 Liquid Glass（2025）** | 折射/光晕玻璃质感，强依赖 Metal | AppCompat 无法低成本复刻，只借鉴"层级用透明度+描边区分"的思路 |
| **终端/代码类 App 的"设备感"** | Termux/iSH/a-Shell：终端区自成深色面板，与 App 外壳明显区隔 | 终端门面应独立设计，不被 app 主色污染 |
| **文件管理器"双窗为专业"范式** | MT/ES：左右双窗 + 路径栏 + 类型图标，专业感来自信息密度+选区高亮 | 双窗方向正确，问题在多选/操作菜单的视觉呈现 |

**来源：**
- Material 3 Color System：https://m3.material.io/styles/color/system/overview
- Material 3 Shape Scale：https://m3.material.io/styles/shape/shape-scale-tokens
- Material 3 Design Tokens：https://m3.material.io/foundations/design-tokens/overview
- Material 3 Motion：https://m3.material.io/styles/motion/overview
- Apple HIG：https://developer.apple.com/design/human-interface-guidelines/typography
- Termux：https://github.com/termux/termux-app · https://wiki.termux.com/wiki/Terminal_Settings
- iSH：https://github.com/ish-app/ish · a-Shell：https://github.com/holzschu/a-shell

### 1.2 「移动端终端怎么做才好看」的专业做法

**核心方法论：终端是一块"设备"，不是一页"界面"。**

1. **等宽字体是门面第一要素**：首选 Sarasa Mono SC（原生覆盖 CJK），或 JetBrains Mono；字号 13sp、行高 1.5、左右内边距 8dp。
2. **ANSI 配色用终端专用调色板，不借 app 主色**：Dracula（深色）`#282A36` 底 / `#F8F8F2` 前景；Nord（克制蓝灰）也适用。
3. **明暗双模式都做对比度校验**：正文 ≥4.5:1、大字/图标 ≥3:1（WCAG AA）；深色前景别用纯白（眩光），浅色前景别太灰。
4. **终端窗口高级感来自"面板化"**：独立底色 + 1dp 描边 + 圆角，像嵌入的硬件 console。

**来源：** Dracula：https://draculatheme.com/spec · Nord：https://www.nordtheme.com/ · MDN WCAG 对比度：https://developer.mozilla.org/en-US/docs/Web/Accessibility/Guides/Understanding_WCAG/Perceivable/Color_contrast · iTerm2：https://github.com/gnachman/iTerm2

### 1.3 文件管理器「双窗/多选/手势」交互范式

| 范式 | 规范做法 | HazeRoot 落地 |
|---|---|---|
| 双窗 | 主窗+副窗，中缝可拖调比例，当前窗用背景亮度区分 | 左右窗+中缝手柄，当前窗高 1 档背景 |
| 路径栏 | 面包屑可点回跳优于纯文本框 | 路径栏+上一级，长路径截断为"…/末级" |
| 多选 | 长按进入，顶部 Contextual Action Bar，左滑逐项勾选 | 顶部操作条而非整屏宫格覆盖 |
| 操作菜单 | bottom sheet / contextual menu 不遮挡主体 | 高频 6 项进宫格，其余进"更多" |
| 类型图标 | 颜色+图标双重编码 | 文件夹=主题色、脚本=.sh 徽标、APK=应用图标 |

**来源：** Refactoring UI（hierarchy is everything）：https://refactoringui.com/

### 1.4 Design Token 落地到「无 Compose」AppCompat

**token 是"语义层"不是"组件库"，完全可在 XML 资源里做。**

| Token 层 | AppCompat 落点 |
|---|---|
| 参考 token（原语） | `values/colors.xml`（`ref_palette_*`） |
| 语义 token | `values/colors.xml` + `values-night/colors.xml` |
| 组件 token | `styles.xml`/`themes.xml`（ThemeOverlay + TextAppearance） |
| 形状 token | `shapeAppearance*` + CardView 圆角 |
| 字阶 token | `TextAppearance.*` 覆写 |
| 间距 token | `dimens.xml`（space_2/4/8/12/16/24） |
| 动效 token | `MotionSpec`/`AnimationUtils` 常量 |

**三条铁律：** ① 永远引用语义 token，不引用原语色值；② 明暗两套同名 key 放 `values` 与 `values-night`；③ 新增组件先建 style 后写布局，禁止 layout 里硬编码色值。

### 1.5 五种主流设计风格诊断

| 风格 | 设计惯性 | 适合 HazeRoot？ |
|---|---|---|
| ① Material 3 | 色调阶梯、圆角卡片、语义色、涟漪 | ✅ 作为骨架与 token 底座 |
| ② iOS HIG / Liquid Glass | 大圆角、毛玻璃、居中标题、Inset Grouped | ❌ 仅借鉴"透明度分层+留白" |
| ③ 极简 monochrome | 单色灰、少边框、靠留白分区 | ✅ 中性底 + 语义色点缀 |
| ④ 开发者极客/深色代码风 | 深底、等宽字、ANSI 高亮、terminal 隐喻 | ✅ **主方向**（终端区） |
| ⑤ 杂志化/编辑部风 | 大标题、衬线字、大留白 | ❌ 与工具高效操作冲突 |

**最优解 = ④ 终端/代码质感做门面 + ① token 纪律做骨架 + ③ 中性底做外壳 + 单一克制品牌色。**

---

## 第二层：「教会用户 UI 设计」进阶教学大纲

### 2.1 UI 设计决策的 10 大核心维度

1. **布局层级**：每页 1 个主角，其余降级。
2. **信息密度**：工具类允许高密度，但要有"呼吸缝"。
3. **视觉重心**：眼动先落点=当前最重要动作/状态，用颜色/大小/对比引导。
4. **色彩系统**：先中性灰阶，再 1 品牌色+语义色，明暗各一套。
5. **字阶**：固定 4–5 档，字号比例阶梯，行高随字号反比。
6. **留白**："留白不够=廉价感"的 90% 来源，先给多再收紧。
7. **对比度**：正文 4.5:1、大字 3:1。
8. **动效**：只服务"状态变化解释"与"空间关系提示"，150–300ms，decelerate。
9. **可访问性**：颜色不唯一承载信息，触控目标 ≥48dp。
10. **状态表达**：运行中/成功/失败/等待/空状态必须全局一致。

### 2.2 「向 AI 精确描述 UI」填空式框架（一次说清，减少返工）

```
【风格锚点】像哪个 App？（如"像 Linear 的克制 + Termux 的终端"）
【情绪关键词】3 个形容词（如"克制/专业/有设备感"）
【产品定位】工具型/内容型/消费型
【密度偏好】高密度(MT) ↔ 中密度 ↔ 低密度(iOS设置)
【色彩倾向】主色色相+饱和度 / 中性色偏冷偏暖 / 是否默认深色
【字阶偏好】标题大小 / 正文 13–15sp / 代码必须等宽
【留白偏好】紧凑 ↔ 宽松
【动效偏好】克制 ↔ 适度 ↔ 丰富
【可容忍复杂度】一次性投入 / 低成本改
【明暗策略】默认暗/默认明/跟随系统/双模
【已否掉的方案】哪些颜色/风格明确不要
```

---

## 第三层：HazeRoot 新 UI 专业建议（核心交付）

### 3.1 当前 UI 核心问题诊断

| # | 症状 | 根因 |
|---|---|---|
| 1 | 风格漂移（iOS玻璃→绿终端→中性） | 无 token 层，每次改组件不改体系 |
| 2 | "勉强能看能用" | 密度失衡：队列页过密、方案库过疏 |
| 3 | 绿色终端被否后"退回中性"无替代方案 | 终端区与 UI 区共用色板 |
| 4 | 层级靠"按钮数量"表达 | 卡片上一排 4–5 个等权小按钮 |
| 5 | 阴影/描边混用无规则 | 无统一层级表达策略 |
| 6 | 空状态被否多次（像素猫/ASCII猫/横幅） | 用"装饰"解决"空状态"而非"结构" |
| 7 | 日夜双模无对比度校验 | 终端浅色模式沿用深色高饱和色 |

### 3.2 推荐主风格 + 完整 Design Token

**主方向：「开发者控制台」风格（Developer Console Aesthetic）**
= 中性单色底 + 单一克制品牌色（靛紫收敛）+ 终端区独立深色"设备面板" + 语义色只用于状态。

**理由**：HazeRoot = "脚本执行器 + 文件管理"，目标用户是开发者/进阶玩家，对"终端质感/设备感"有天然认同；用"控制台"隐喻统一："App 是外壳，终端是嵌入的设备"。

#### A. 中性色阶（外壳 UI）

| Token | Light | Night | 用途 |
|---|---|---|---|
| color_surface | #FFFFFF | #121317 | 页面底 |
| color_surface_variant | #F2F3F6 | #1A1C22 | 卡片/分组底 |
| color_surface_container | #FFFFFF | #22242C | 卡片 |
| color_on_surface | #1C1E24 | #E8EAF0 | 主文本 |
| color_on_surface_variant | #5C6070 | #9AA0AE | 次文本 |
| color_outline | #C8CCD4 | #3A3E48 | 描边 |
| color_outline_variant | #E6E8EE | #2A2D36 | 细分隔线 |

#### B. 品牌色（保留 #635BFF，收敛为 tonal）

| Token | Light | Night | 用途 |
|---|---|---|---|
| color_primary | #4F46E5 | #8B87FF | 主按钮/FAB/选中 |
| color_on_primary | #FFFFFF | #1C1B2E | 主色上文字 |
| color_primary_container | #E4E2FF | #3A3668 | 选中卡片底/胶囊导航 |
| color_on_primary_container | #1C1B2E | #E0DEFF | container 上文字 |

#### C. 语义色（全局状态）

| Token | Light | Night | 用途 |
|---|---|---|---|
| color_success | #15803D | #4ADE80 | 成功 |
| color_error | #DC2626 | #F87171 | 失败/停止 |
| color_warning | #B45309 | #FBBF24 | 提示行/警告 |
| color_info | #2563EB | #60A5FA | 信息 |
| color_running | #0EA5E9 | #38BDF8 | **运行中**（独立色） |

> 「运行中」必须有独立色，不能复用信息蓝/成功绿。

#### D. 终端区 ANSI 调色板（与 UI 区完全分离）

**深色终端（默认，Dracula）：** 背景 #282A36 / 前景 #F8F8F2；red #FF5555、green #50FA7B、yellow #F1FA8C、blue #BD93F9、magenta #FF79C6、cyan #8BE9FD；bright 系列 #FF6E6E/#69FF94/#FFFFA5/#D6ACFF/#FF92DF/#A4FFFF。

**浅色终端（Alucard）：** 背景 #FFFBEB（纸感米白，避免纯白刺眼）/ 前景 #1F1F1F；green #14710A、red #CB3A2A、yellow #846E15、blue #644AC9、magenta #A3144D、cyan #036A96。

#### E. 形状/间距/字阶 token

| Token | 值 |
|---|---|
| 圆角 small/medium/large/pill | 8 / 12 / 16 / 999dp |
| 间距 | 2/4/8/12/16/24dp 阶梯 |
| 标题/小标题/正文/辅助/终端 | 22sp·w600·1.3 / 16sp·w600·1.4 / 14sp·1.5 / 12sp·1.4 / 13sp·mono·1.5 |

#### F. 阴影/描边策略（告别混用）

| 场景 | 规则 |
|---|---|
| 卡片（同层） | 只 1dp outline_variant 描边，不用 elevation |
| 浮层（sheet/dialog/FAB） | elevation 8–16dp + 阴影 |
| 当前窗/选中卡片 | primary_container 底 + primary 1.5dp 描边 |
| 终端面板 | 独立底色 + 1dp 描边 + 12dp 圆角（嵌入感） |

### 3.3 逐页改造

#### ① 启动动画
保留"H 图标 → 品牌名"识别性，**删掉"放大穿越进主界面"**（iOS 老套路+小屏易晕）。改为：H 碎片收敛 → 静置 400ms → 图标淡出 + 底部淡入队列页（crossfade 200ms）。总时长 ≤1.2s。

#### ② 队列页（重点：终端门面）
自上而下：工具行（标题+Root徽标+日夜切换，40dp）→ 脚本卡片（可折叠，12dp圆角+描边）→ **终端命令窗面板（深色 #282A36 + 1dp描边 + Sarasa Mono 13sp/1.5行高 + 左右margin 8dp + 圆角12dp，顶部一行"当前脚本名+状态灯"）** → 输入框 + 执行FAB/停止按钮。
- 执行按钮改右下角 FAB（primary 圆角 16dp ▶），停止用次级胶囊；两者不同时出现。
- 橙色提示行改"顶部 warning 细横幅可收起"。
- 卡片默认隐藏上下移/编辑/删除小按钮，长按进入编辑模式才出现。

#### ③ 文件页
主窗+副窗，中缝 4dp 可拖手柄；当前窗 surface_container、非当前窗 surface_variant（用背景亮度区分，不描边）。路径栏面包屑式。多选用顶部 Contextual Action Bar。宫格菜单只留高频 6 项，删除/分享/APK 安装进"更多"。

#### ④ 方案库页
卡片 12dp 圆角+1dp 描边；底部弹层精简为 3 主动作（运行/目标应用/更多），删除用 error 色永远放"更多"里。

#### ⑤ 底部导航（推荐）
**胶囊式浮动导航（Floating Pill NavigationBar）**：primary_container 底 + 1dp primary 描边 + elevation 8dp，内放三 item（图标+11sp标签）；选中 primary 图标+圆点指示，未选中 on_surface_variant 图标。AppCompat 用 CardView 包 NavigationBar 即可，成本低。

### 3.4 动效与过渡

| 场景 | 建议 | 时长/缓动 |
|---|---|---|
| 切页 | crossfade 交叉淡入淡出，不用横向滑动 | 200ms / decelerate |
| 执行中 | 终端顶部状态灯呼吸闪烁 alpha 0.4↔1.0 | 800ms / ease-in-out 循环 |
| 终端滚动 | 平滑滚入 + 结束时吸底 + 末行高亮 300ms | 150ms / decelerate |
| 徽标状态变化 | 填充色过渡 | 150ms |
| 卡片折叠/展开 | 高度动画 + 摘要淡入 | 200ms / decelerate |
| 多选进入/退出 | 顶部操作条下滑进入 | 200ms |
| 浮层弹出 | sheet 默认 behavior + FAB scale 0.9→1.0 | 250ms |

### 3.5 空状态统一规范（根治被否循环）

**公式：线性图标（48dp，outline 色）+ 一句话（为什么空）+ 一个主行动按钮 + 可选次级说明**
- 队列空：线性"终端窗口"+"还没有脚本，添加第一个脚本开始自动化"+[添加脚本]
- 文件空：线性"文件夹"+"当前目录为空"+[刷新/粘贴]
- 方案库空：线性"收藏"+"保存常用脚本组合为方案"+[新建方案]
- **禁用插画/ASCII 艺术**（装饰性过强、与工具气质冲突，是被否掉的共同原因）

### 3.6 AppCompat 可实现 vs 需 Compose

| 能力 | 现状可做 |
|---|---|
| 全部 token / 胶囊导航 / 终端面板+ANSI配色 / 等宽字体 / 卡片描边 / 折叠动画 / 底部sheet / 多选CAB / 启动动画 / 状态呼吸 | ✅ 直接 |
| Expressive 异形组件 / Liquid Glass 折射 / 复杂 shared-element 联动 | ❌ 需 Compose（且非必要） |

**结论：85% 可在现有 AppCompat 项目直接落地，无需迁移 Compose。**

---

## 落地优先级

1. **P0**：建立 token 层（colors.xml + values-night + 语义色 + 终端独立色板）
2. **P0**：终端门面重做（深色面板 + 等宽字 + ANSI 色板）
3. **P1**：队列页结构重组（FAB 执行/停止、折叠卡片、徽章统一、提示行收纳）
4. **P1**：底部导航改胶囊
5. **P2**：文件页双窗 + 多选 CAB + 操作菜单精简
6. **P2**：方案库弹层精简 + 全局空状态规范
7. **P3**：动效统一（crossfade、状态呼吸、滚动吸底）
8. **P3**：启动动画去"穿越"

> 每步独立可交付、可回滚。先做 P0/P1，即可让"勉强能用"跃迁到"有产品感"。
