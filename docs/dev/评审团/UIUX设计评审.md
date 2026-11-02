# HazeRoot UI/UX 与品牌设计评审报告

> 评审对象：HazeRoot「Android root 脚本执行器」
> 一手资料：`D:\DSH1\work\HazeRoot-UI视觉稿-v1.html`（视觉稿）、`colors.xml` + `values-night\colors.xml`、`layout\` 下 `activity_main.xml` / `fragment_queue.xml` / `item_file.xml` / `item_script.xml`、`docs\dev\` 两份文档、`SwitchFx.java`、`styles.xml` 主题与关键 drawable。
> 评审立场：UI/UX + 反「AI 味」。结论具体到色值 / dp / 组件，明确区分「视觉稿方向对错」与「现有代码布局对错」。

---

## 0. 一句话结论

**方向选对了，门面没落地。** 冷青蓝 + 琥珀 + 石墨的三元色、独立深色终端面板、单色极简底部导航——这套方向在视觉稿里是对的、是「有人味」的；但**现有代码实现与视觉稿严重脱节**：终端还是浅色贴纸而非深色设备面板、底部导航还留着被否掉的青蓝胶囊、色彩系统里紫色和 teal 残留没清干净。品牌锚点目前停留在「色值声明」层，没有贯穿到 logo / 图标 / 门面。

---

## 1. 视觉稿评审（方向判断）

### 1.1 方向判定：对

| 维度 | 视觉稿做法 | 判定 |
|---|---|---|
| 干净 | 中性石墨底 #F5F6F8 + 卡片白 + 1px 描边 #E3E6EC，去彩色胶囊 | ✅ 对 |
| 有质感 | 终端独立深色面板 #1E2028 + 描边 rgba(255,255,255,.06) | ✅ 方向对 |
| 无 AI 味 | 单色导航 + 选中亮/未选中灰 + 4px 品牌 dot | ✅ 大体对 |
| 冷暖巧思 | 琥珀 #E8A33D 只用于「运行态 + 提示行」两处 | ✅ 克制，对 |
| 品牌锚点 | brand-badge 26px 圆角方块 H 字母 + 冷青蓝 | ✅ 有雏形 |

**冷青蓝 + 琥珀 + 石墨的协调性：没问题。** #3B6FE0（hue≈218°，饱和度≈69%）与 #E8A33D（hue≈36°）是标准的「蓝橙互补但都压饱和」的克制用法，配中性石墨灰阶，不会打架。这是合格的、能被记住的品牌色组合。

### 1.2 视觉稿自身的「AI 味」残留（要警惕的地方）

1. **手机壳 + 右侧 legend 说明卡的排版**：`.phone` 375×812 圆角 44px + 外圈 10px 黑边 + 旁边并排一张「色值图例卡」，这是 AI 生成 UI 稿的典型排版范式（把 mockup 和图例贴一起）。作为内部稿可以，但**不要把这种「图例说明卡」的思维带进产品**——产品里不应该出现向用户解释色值的内容。
2. **示例文案是「AI 味」重灾区**：视觉稿里的脚本名「过电报验证.sh」「Fuzz_Cheat_v4.1.sh」「LinYuKernelSgame.Ver.4.9.sh」是演示占位。真实产品里这类「游戏外挂/电报验证」命名会立刻让 App 掉进「灰产工具」的气质，与「干净有质感」的目标背道而驰。**评审只针对 UI，但这一条必须提**：门面截图如果用了这种脚本名，品牌质感直接归零。
3. **`letter-spacing .1em` 的大写「TERMINAL」标签**：是通用终端套路，不算错，但配合 4px 小圆点、44px 手机壳，整体仍偏「模板感」。真正的差异化在终端面板的**内容质感**（ANSI 配色 + 等宽字体 + 光标），而不是这些装饰。
4. **深色模式品牌色 `--brand: #5B8DEF` 是「随手提亮」而非「系统化 tonal」**：视觉稿深色品牌色 #5B8DEF 与浅色 #3B6FE0 之间没有亮度/色相的系统关系，属于「手动加一档亮」的 AI 惯性。应该按 Material tonal palette 或至少固定「深色模式 brand 提亮 ~12% 亮度、微降饱和度」的规则派生。

### 1.3 视觉稿与代码实现的三处「致命脱节」

| 视觉稿说 | 代码实际 | 后果 |
|---|---|---|
| 终端 = 独立深色设备面板 #1E2028 | `bg_term` 浅色 #F1F1F3 / 深色 #101114，且未做「独立面板」描边+圆角分层 | **门面没建**，终端看起来是普通灰底卡片 |
| 终端 ANSI = Dracula 深色系（cyan #8BE9FD / green #50FA7B） | `term_ansi_*` 是浅色主题暗色系（#0A5FCC 蓝 / #157A3A 绿） | 色板自相矛盾，深色终端若上线会错乱 |
| 底部导航 = 单色极简（选中亮/未选中灰 + 4px dot） | 代码仍用 `accent_tint14` 青蓝胶囊 `bg_tab_pill` | **被否掉的彩色胶囊还活着** |

---

## 2. 现有布局评审（代码层，具体到 dp）

### 2.1 fragment_queue.xml（队列页）—— 密度失衡 + 按钮层级混乱

**排版不良好的具体点：**

1. **根布局 `LinearLayout` + `animateLayoutChanges`**：整页竖向线性堆叠，队列卡片区 / 终端区 / 输入区 / 执行区全靠 `layout_weight=1` 挤。队列脚本多时，终端会被压到只剩 100dp（`termScroll minHeight=100dp`），门面忽大忽小。
2. **`appTitle` 24sp 过大**：视觉稿标题 19sp，代码 24sp。24sp 是「大标题」，配 28dp 的 H 图标（`bg_brand` 纯黑底）比例失衡——图标 28dp 撑不起 24sp 标题。
3. **品牌徽标 `bg_brand` 是纯黑 #0B0B0B，不是品牌青蓝**：H 图标 28dp 用黑底，品牌色没有出现在「品牌标识」本身上，锚点失效（详见 §6）。
4. **`rootBadge` 高度 24dp、`rootDot` 7dp**：24dp 是触控禁区（<48dp），7dp 圆点几乎不可见。Root 状态是这类工具的核心信任标识，却做得最弱。
5. **`btnAdd`（48dp，accent_tint8 淡青蓝底 + accent 文字）与 `btnRun`（48dp，实心青蓝）同屏并列**：两个「主按钮级」视觉权重，用户分不清「添加脚本」和「执行全部」谁是主行动。次级按钮应该降为描边或文字按钮。
6. **`btnStop` 44dp 圆形 + `alpha=0.35` 常驻显示**：停止按钮永远可见只是半透明。正确做法是**运行/停止互斥切换**（执行中只出现停止，空闲只出现执行），而不是用 alpha 表达「不可用」。
7. **终端区缺「门面行」**：`termScroll` 只有一条 `TERMINAL` 文字标签 + `清空`，缺少视觉稿要求的「当前脚本名 + 状态灯」这一行——它是把「终端」和「队列执行态」绑在一起的关键门面。
8. **输入框 46dp + 发送按钮 44dp 用 `selectableItemBackgroundBorderless`**：发送按钮是裸的矢量图标，无容器，视觉重量不足，且 44dp < 48dp 触控标准。

### 2.2 item_file.xml（文件列表项）—— 信息层级缺失 + 滑动按钮混乱

1. **`FrameLayout` 右侧 4 个等宽 48dp 竖条按钮（入队/复制/移动/删除）**：四个操作权重完全相同，只有颜色区分。破坏性操作「删除」被排在最后一个但用 `error_tint` 淡红，与前三个 `accent_tint8`/`bg_soft` 混在一起，**操作层级没有「危险 vs 常规」的视觉隔离**。MT 管理器的做法是左滑只出「删除 + 更多」，其余进宫格/更多菜单。
2. **主行只有 1 行 + 副行 1 行，但间距过挤**：`paddingTop=6dp / paddingBottom=6dp`，`fName` 14sp bold + `fMeta` 10sp monospace，没有 `minHeight`。长文件名会直接 ellipsize=end 截断，且**没有「完整名兜底」**（无点按/长按弹全名），违背交互规格 B 节。
3. **图标 `fBadge` 24dp 偏小**：文件类型图标（目录/SH/ELF）24dp 是「状态点」大小，不是「列表主图标」大小。MT 是 40dp。图标小 → 整行视觉重心不稳，密度全靠文字撑。
4. **`fMeta` 10sp**：副行 10sp 已经接近「不可读」，在 5 寸屏上信息等于没有。

### 2.3 item_script.xml（队列脚本项）—— 结构对但层级弱

1. **`name` 16sp bold + `index` 11sp monospace + `dot` 7dp + `btnMore` 44dp**：结构（状态点/名称/序号/更多）是对的，但 `index` 11sp 太弱、`dot` 7dp 太小、`paddingRight=4dp` 配 44dp 的 more 按钮导致文字被压。
2. **`activeBar` 3dp 宽 accent 色**：选中/运行态的侧边指示条，但 `visibility=gone`，只在运行时用。**运行态/选中态应该用「状态点颜色 + 侧边条」双重编码**，而不是仅侧边条。
3. **无副行**：脚本项没有「参数摘要/耗时/错误摘要」副行，执行完成后用户无法在列表直接看到结果（要进终端翻）。

### 2.4 activity_main.xml（底部导航）—— 实现与视觉稿直接冲突

1. **手写 `LinearLayout` 三 tab，`bg_nav_island` 只有 `solid` + 20dp 圆角，无 1px 描边**：视觉稿是「1px border + 8dp 阴影」，代码只有 `elevation=8dp`（AppCompat DayNight 下 elevation 阴影可能不渲染）。**外壳没有视觉稿的「浮岛描边」质感。**
2. **`bg_tab_pill` 仍用 `accent_tint14`（透明青蓝胶囊）**：这是视觉稿明确「去掉彩色胶囊」后还留在代码里的残留。选中态应是「图标亮 + 4px dot」，不是青蓝药丸背景。
3. **label 文字没有显式 textColor**：`labelQueue/labelFiles/labelPresets` 没设 `textColor`，靠 Theme 默认，但 icon 用了 `bottom_nav_item` selector（选中 accent / 未选中 label_tertiary）。**图标和文字两套选中逻辑不一致**，可能图标变色文字不变或反之。
4. **`marginBottom=8dp` + `marginLeft/Right=12dp`**：视觉稿是 12dp 左右 / 20dp 底。小屏 + 手势导航下 8dp 底距会贴底，视觉压迫。

---

## 3. 色彩系统评审（colors.xml）

### 3.1 色值是否统一：不统一，三套体系并存

代码里同时存在**三套互不相干的色彩体系**，这是「风格漂移」的代码级证据：

1. **HazeRoot 自定义色**（`accent`/`bg_page`/`label`/`term_*`/`running`…）—— 这是实际在用的。
2. **Material 默认紫**：`design_default_color_primary #6200ee`、`design_dark_default_color_primary #ba86fc`、`design_default_color_primary_dark #3700b3`。
3. **M3 参考紫 palette 整套**：`m3_ref_palette_primary40 #6750a4`、`primary80 #d0bcff` 等一整套。

`Theme.App` 显式覆盖了 `colorPrimary=@color/accent`，所以主色不会漏紫；**但 `colorSecondary` 没有在 Theme.App 里覆盖**，AppCompat 默认 secondary 是 teal 青绿（`material_deep_teal_500 #008577`）。这意味着 **Switch 轨道、Checkbox、部分 secondary 组件会用 teal 而非青蓝**——这是「蓝绿混搭」的隐蔽残留。

### 3.2 紫色残留清单（直接证据）

| 位置 | 值 | 说明 |
|---|---|---|
| `ic_script.xml` | `#ff5147e8` | **脚本图标仍是靛紫**，最直接的紫色残留 |
| `design_default_color_primary` | `#ff6200ee` | Material 默认紫，未清 |
| `design_dark_default_color_primary` | `#ffba86fc` | 同上（深色） |
| `m3_ref_palette_primary40/80` | `#ff6750a4` / `#ffd0bcff` | M3 紫 palette 原样保留 |
| `term_ansi_35/95` | `#ffa626a4` / `#ffc13fd6`（light） | 终端 ANSI magenta 仍是紫红，未纳入青蓝体系 |

> 说明：M3 palette 若只是「Material 库默认资源」可以留着不动，但 `design_default_color_primary` 与 `ic_script` 的紫色是**产品内可感知**的，必须清。

### 3.3 语义色是否清晰：运行/提示撞色 + 成功/失败不一致

| 语义 | light | night | 问题 |
|---|---|---|---|
| running | #E8A33D | #E8A33D | ✅ 一致 |
| warning | #E8A33D | #E8A33D | ✅ 但与 running 同色 |
| prompt_text | #E8A33D | #E8A33D | ⚠️ 提示行也吃琥珀 |
| success | #248A3D | #5DD37A | ⚠️ 明暗不是同一色相亮度阶梯（视觉稿是 #2E9E5B） |
| error | #C50015 | #FF625C | ⚠️ 同上 |
| term_err | #C50015 | #FF625C | 与 error 一致 ✅ |

**核心问题**：琥珀 #E8A33D 同时承担了「运行中」和「提示/警告」两个语义。视觉稿的设计意图是「琥珀做运行/提示暖色小巧思」，但 UX 上「运行中（进行时）」与「警告（需注意）」是两种不同的注意力等级，共用一色会让「这条黄色到底是脚本在跑、还是它在等我输卡密」无法一眼区分。**建议**：运行态用品牌青蓝的呼吸态，琥珀只留给「等待输入/提示/警告」，两者分离。

### 3.4 终端区色板：与视觉稿「独立深色面板」矛盾

`bg_term` light=#F1F1F3、night=#101114。视觉稿要求的独立深色面板 #1E2028（light 模式也应是深色，因为它模拟「硬件 console」）在代码里变成了「浅色模式浅灰底」。这是**门面落差的最大来源**。若坚持「终端跟随明暗」，也必须：
- light 终端用「米白纸感」（如 #FFFBEB）而非 #F1F1F3（与卡片白 #FFFFFF 几乎无区分度）；
- night 终端 #101114 与 night 卡片 #121214 几乎同色，同样没有「面板感」。

---

## 4. 信息密度与层级

| 页面 | 现状 | 失衡点 |
|---|---|---|
| 队列页 | 标题 24sp + 脚本卡 + 终端 + 输入 + 执行，全挤一屏 | **过密**：终端作为门面却被 `weight=1` 压缩，视觉重心落在「添加/执行」两个按钮而非终端 |
| 文件页 | 单窗，24dp 图标 + 10sp 副行 | **密度虚高、层级虚低**：字小图标小，但没副行补全/没全名兜底 |
| 方案库 | 视觉稿只有 2 张占位卡 + 终端占位 | **过疏**：没做卡片信息架构，方案「日常启动/清理+备份」只有名称无摘要无状态 |

**视觉重心**：队列页第一眼落在 24sp 大字标题和两个青蓝按钮上，**不是落在「终端输出」这个产品灵魂上**。工具类 App 的视觉重心应该是「正在做的事」（终端/队列状态），标题和按钮都要往后退。

**留白**：item_file 的 6dp 上下 padding、rootBadge 的 2dp 上边距，都是「舍不得给留白」的表现；留白不够是廉价感的主要来源。

---

## 5. 动效评审（SwitchFx + animateLayoutChanges）

### 5.1 SwitchFx 全屏遮罩：该改，不该删

`SwitchFx.run()` 的流程：全屏纯色 `scrim` alpha 0→1（260ms）→ 执行切主题动作 → `consume()` 同色 alpha 1→0（320ms）。

**问题：**
1. **这不是 crossfade，是「先全黑再变」**。切主题时用户看到的是「屏幕被一个纯色盖住 → 变暗到完全不透明 → 再掀开」，视觉上是「黑屏闪一下」，比直接切更突兀。切主题的正确做法是 `windowBackground` 换色 + 内容 alpha 淡入淡出，或至少遮罩到 ~60% 就执行动作、再淡出。
2. **`pendingColor` 是 static volatile**：跨 Activity 全局状态，快速连续切换（或旋转屏幕）时 `pendingColor` 可能残留错色，遮罩颜色和实际主题不符。
3. **fade in 时长 260ms + out 320ms 合计 ~580ms**：偏慢，切主题应 ≤300ms 总时长。

**该删/该改**：改 `run()` 为「遮罩淡入到 ~0.6 即回调 → 主题应用 → 遮罩淡出」，去掉 static pendingColor（改为每次调用传入色值并本地持有）；总时长压到 ≤320ms。

### 5.2 animateLayoutChanges

`fragment_queue.xml` 根用了 `android:animateLayoutChanges="true"`。默认的 autoTransition 会对**所有子 View 的增删**做位移动画，包括 RecyclerView 内脚本项变化引发的 `scriptsBody` 高度变化。脚本增删/运行态切换时可能出现「整块上下跳动」。**建议**：只在「脚本卡片折叠/展开」「提示行出现/消失」这类明确的容器上局部动画，根布局关掉，改用精确的 `Transition`。

---

## 6. 品牌印象（品牌锚点是否成立）

### 6.1 色组合：锚点成立了一半

冷青蓝 #3B6FE0 + 琥珀 #E8A33D + 石墨中性，**这是一组能被记住的颜色**，方向正确。但品牌锚点 = 色值 × 载体，目前「载体」没接上：

| 品牌载体 | 现状 | 问题 |
|---|---|---|
| 品牌徽标 H | `bg_brand` 纯黑 #0B0B0B | **没用品牌青蓝**，锚点断裂 |
| Launcher 背景 | `ic_launcher_background` 纯黑 #0B0B0B | 同上 |
| 脚本类型图标 | `ic_script` #5147E8 紫色 | 用的是被否掉的紫 |
| 文件类型图标 | `ic_elf` #1E7A32 绿 / `ic_file` #0E7C8C 青 | 五颜六色，没有统一到品牌色系 |
| 终端面板 | 浅灰 #F1F1F3 | 不是「门面」的深色设备面板 |

**结论**：品牌色目前只活在 `accent` 这个按钮色里，没有出现在「能被一眼认出是 HazeRoot」的标识、图标、门面上。品牌锚点**不成立**，需要把青蓝贯穿到 logo/图标/选中态，把琥珀贯穿到「运行/等待」这一唯一暖色点。

### 6.2 终端面板是合格的「门面」吗？—— 不是（当前代码）

视觉稿的终端面板（深色 #1E2028 + 独立圆角 + 等宽字 + ANSI 高亮 + 闪烁光标）**是合格的、能成为门面的设计**。但代码里的终端是浅灰 `ScrollView + TextView`，没有：
- 独立深色面板（light 模式也是）
- 顶部「当前脚本名 + 状态灯」行
- 真正的 ANSI 分段染色（`term_ansi_*` 存在但和视觉稿 Dracula 色板冲突）
- 闪烁光标（视觉稿有 `.cursor`，代码无）

**门面要落地，必须先做终端面板重做（P0）。**

---

## 7. UI 待改清单（按「最影响观感」排序）

| # | 优先级 | 改造项 | 具体做法 | 影响 |
|---|---|---|---|---|
| 1 | P0 | **终端面板重做为独立深色设备面板** | light/night 都用深色（#1E2028 或 Dracula #282A36）+ 1dp 描边 + 12dp 圆角 + 等宽字 + 顶部「脚本名 + 状态灯」行 + ANSI 深色色板 + 闪烁光标 | 门面，最高 |
| 2 | P0 | **清紫色/teal 残留** | `ic_script` #5147E8→青蓝；Theme.App 补 `colorSecondary=@color/accent`；清理 `design_default_color_primary #6200ee` | 色系纯度 |
| 3 | P0 | **底部导航对齐视觉稿** | 去掉 `accent_tint14` 青蓝胶囊，改「单色 + 选中亮/未选中灰 + 4dp dot」；`bg_nav_island` 补 1px 描边 | 全局第一眼 |
| 4 | P1 | **统一语义色 + 分离运行/提示** | success/error 明暗成对（如 light #2E9E5B/#C73E3A）；运行态用青蓝呼吸，琥珀只留「等待输入/提示」 | 状态可读 |
| 5 | P1 | **品牌锚点贯穿** | `bg_brand`/launcher 改品牌青蓝；文件/脚本类型图标统一到青蓝系 + 语义色点缀 | 品牌记忆 |
| 6 | P1 | **队列页按钮层级** | add 降为描边次级，run 为唯一主按钮，run/stop 互斥切换（去 alpha 0.35） | 视觉重心 |
| 7 | P1 | **文件项三区布局** | 图标 40dp + 主名 15sp + 副行（大小·时间）12sp，长名 ellipsize=middle + 点按弹全名；滑动操作只剩「删除+更多」 | 文件页可用性 |
| 8 | P2 | **SwitchFx 改 crossfade** | 遮罩淡入到 ~0.6 即切主题再淡出，总时长 ≤320ms，去 static pendingColor | 动效舒适 |
| 9 | P2 | **信息密度微调** | appTitle 24sp→19sp；rootBadge 24dp→32dp 且触控 ≥48dp；item_script 补副行（状态/耗时摘要） | 观感 |
| 10 | P3 | **空状态统一 + 启动动画去「穿越」** | 线性图标 48dp + 一句话 + 主行动按钮，禁插画/ASCII；启动改「H 收敛→静置→crossfade」 | 收尾 |

---

## 附：视觉稿方向 vs 代码实现的「一句话对照」

- **视觉稿方向（对）**：冷青蓝+琥珀+石墨、独立深色终端、单色导航、克制动效。
- **代码实现（没跟上）**：终端是浅色贴纸、导航是青蓝胶囊、图标是紫色、语义色撞车、品牌 logo 是纯黑。
- **结论**：先做 P0 三项（终端重做 + 清残留 + 导航对齐），品牌质感即可从「勉强能看」跃迁到「有产品感」。
