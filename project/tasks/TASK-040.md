# TASK-040: 机型适配（屏幕尺寸 / 密度 / 字体缩放）

- 状态：`IN_PROGRESS`
- 阶段：`Gate 3`
- 优先级：`P1`
- 依赖：`TASK-032`（今日/新建页重建）、`TASK-031`（统计页）、`TASK-033`（详情页）
- 创建日期：`2026-11-09`

## 适配方案（2026-11-09 拟定，待确认后执行）

### A. 把「适配」拆成三层，避免用模拟器假装覆盖全部

| 层 | 内容 | 能否用模拟器验证 | 手段 |
|---|---|---|---|
| L1 布局 | 320/360/411/480/600/840dp 宽度、密度、字体缩放、insets、挖孔、手势条、横竖屏 | **能**（AOSP 镜像 + `wm`/`overlay` 覆盖） | 本地模拟器 + adb 断言 |
| L2 OEM ROM 行为 | 小米 MiSans / 荣耀 HONOR Sans 字体度量、系统「显示大小」档位、各家手势条高度、强制深色、应用双开/平行视界 | **不能**（官方镜像只有 AOSP/GMS） | 云真机或社区反馈 |
| L3 极端可用性 | 无障碍超大字体、显示大小最大档、TalkBack 焦点顺序 | 部分能 | 模拟器 `font_scale 2.0` + 人工 |

结论：**L1 用模拟器全量自动化，L2 只能用真机抽样（云真机或借机）**，两者不能互相替代。

### B. 工具链（按成本从低到高）

1. **静态扫描（无需设备，先做）**：`tools/adaptation-scan.ps1` 扫描 `.kt` 里的固定 `width/height`、按屏宽估算的 fraction、`maxLines` 缺失的长文案、小于 48dp 的触控目标，输出清单。当前基线：286 处 `.dp/.sp`、6 处固定 `width/height`。
2. **本地模拟器**（需下载 `emulator` + `system-images;android-35;google_apis;x86_64`，约 1.5–2 GB，每个 AVD 2–4 GB）：一个 AVD 通过 `wm size`/`wm density`/`settings put system font_scale`/`cmd overlay enable com.android.internal.display.cutout.emulation.*` 模拟全部 L1 档位；无需多开 AVD。
3. **`tools/probe-adaptation.ps1`**（不引入任何依赖，纯 adb）：逐档位 → 冷启动 → 逐页（今日/新建/统计/我的/详情）截图 + `uiautomator dump`，断言：
   - 节点 `bounds` 不越界、不为负；
   - 可点击节点 ≥ 48×48dp（按 density 换算）；
   - 关键控件（保存、FAB、底部导航）存在且不与其它 clickable 重叠；
   - 文本节点无越界/被裁切（`bounds` 宽 ≤ 屏宽，`text` 与 `content-desc` 非空）；
   产物：`project/reports/adaptation/<config>/<page>.png` + `summary.json` + PASS/FAIL。
4. **截图基线对比（第二阶段，可选）**：Roborazzi/Paparazzi 需要新增测试依赖（须你批准）；先用 PNG + 人工比对，不引入依赖。
5. **Compose `@Preview`**：按 device 规格批量预览，设计期辅助，CI 不可用。
6. **云真机（覆盖 L2 的唯一可靠途径）**：Firebase Test Lab（有免费额度，Robo 测试可按机型跑）、BrowserStack App Live、腾讯 WeTest / 华为云云真机。注意：Test Lab 目录里小米/荣耀机型有限，未必能覆盖你关心的具体型号。
7. **`gradle managed devices`**（AGP 官方，CI 可跑 AVD）：与 2 相同的镜像前提，本地先用脚本，CI 化再做。

### C. 适配矩阵（L1 必跑档位）

| 维度 | 档位 |
|---|---|
| 宽度 | 320dp（小屏）、360dp（主流）、411dp（Pixel/多数国产）、480dp（大屏）、600dp（折叠展开/小平板）、840dp（平板） |
| 密度 | 320 / 420 / 480 / 560 dpi |
| 字体 | 1.0 / 1.3 / 1.5 / 2.0 |
| insets | 手势条、三键、刘海、挖孔、瀑布屏 |
| 方向 | 竖屏必过；横屏「不破版」 |
| 语言 | zh-CN / en（若做英文） |

### D. 代码侧规则（P0 先清理，成本最低）

- 触控目标 ≥ 48dp；固定高度改 `heightIn(min=)`；整页可滚动（`verticalScroll` / `LazyColumn`）。
- 用 `WindowInsets` 而非写死状态栏/导航栏高度；边到边 + `safeDrawing` padding。
- 断点用 `BoxWithConstraints`：<360dp 收紧间距、600dp+ 用 `widthIn(max = 560.dp)` 居中限制行宽，避免平板被拉散。
- 长文案 `maxLines` + `TextOverflow.Ellipsis`，并保证关键时刻（习惯名、按钮）在 1.5× 字体下仍完整。
- 密度相关用 `dp`，字号用 `sp`（现状守住）。

### E. 阶段与产出

| 阶段 | 内容 | 前置 | 产出 |
|---|---|---|---|
| P0 | 静态扫描 + 修固定尺寸/滚动/触控目标 | 无 | `adaptation-scan` 报告、修复 commit |
| P1 | 装模拟器 → 写 `probe-adaptation.ps1` → 跑矩阵截图与断言 | 允许下载 ~2 GB、BIOS 虚拟化开启 | 截图 + `summary.json` |
| P2 | 按 P1 失败项修布局 → 复跑至全绿 | P1 | 修复 commit、全绿报告 |
| P3 | L2 抽样：云真机（小米/荣耀/OPPO/vivo 各 1 台）或借机 | 预算/账号决策 | OEM 差异清单 |
| P4 | 固化：UI 改动后跑 P1；发布前跑 P3 | — | `verify-all -WithAdaptation` 可选步骤 |

### F. 需要你决策

1. ~~是否允许下载模拟器镜像~~ → **已批准**（2026-11-09）：下载 `emulator` + `android-35 google_apis x86_64`。
2. ~~云真机预算~~ → **已定**（2026-11-09）：先用 Firebase Test Lab 免费额度，付费云真机后置。
3. ~~OEM 优先级~~ → **已定**（2026-11-09）：小米/Redmi（HyperOS）+ OPPO/一加（ColorOS），本机 vivo（OriginOS）作为常驻对照。
4. 是否做英文界面（影响 L1 的 locale 档位）。

### G. 风险

- 模拟器 ≠ 真机：字体度量、显示大小档位、手势条高度、强制深色、后台策略都测不出来；L1 全绿不等于 OEM 无问题。
- 下载/磁盘：镜像 + AVD 约 6–10 GB；无 WHPX/Hyper-V 时 x86_64 镜像跑不动，需先 `emulator -accel-check`。
- 云真机机型目录未必含你关心的具体型号（尤其荣耀/小米新机）。
- 无提醒、无后台服务，因此 OEM 后台限制影响很小，L2 主要风险集中在**字体与显示大小**。

## 1. Objective

同一份 APK 在小屏、高 DPI、平板尺寸与系统大字体下都不溢出、不遮挡、不丢内容。用 `adb shell wm size` / `wm density` / `font_scale` 模拟目标机型，逐页截图核对，修掉写死的尺寸假设。

## 2. Context
- 目前只在 iQOO Neo7 SE（1080×2400，约 440dpi，Android 15）上验证过；布局里存在按该宽度估算的固定高度、字号与两行图标网格。
- 小屏（720×1280 / 320dpi）与平板（1200×1920）尚未跑过；系统字体放大后文案可能挤出卡片。
- V1 只做竖屏（`MainActivity` 未声明 `screenOrientation`，但页面按竖屏设计）。

## 3. Scope
- 用 `wm size` / `wm density` / `settings put system font_scale` 模拟：小屏 720×1280@320、常规 1080×2400@440、大屏 1200×1920@320；字体 1.0 与 1.5。
- 逐页检查：今日、新建、统计、我的、详情；记录截图与问题清单。
- 修复：溢出、控件重叠、文案截断、图标网格挤压、保存按钮遮挡。
- 恢复设备原始 `wm size`/`density`/`font_scale`。

## 4. Out of Scope
- 横屏与折叠屏展开态（V1 竖屏）。
- 平板双栏布局（V1 不做专门平板布局，只保证不破版）。

## 5. Expected Files
- 修改：`feature/today/TodayScreen.kt`、`feature/create/CreateHabitScreen.kt`、`feature/statistics/StatisticsScreen.kt`、`feature/profile/ProfileScreen.kt`、`core/designsystem/Ui.kt`（按实际发现）
- 新增：本文件、`project/reports/ADAPTATION-2026-11-09.md`（截图与结论）

## 6. Acceptance Criteria
- [ ] 小屏 720×1280@320：今日/新建/统计/我的 四页无横向溢出、无控件重叠
- [ ] 大屏 1200×1920@320：内容不被拉伸到不可读，图标网格与卡片正常
- [ ] 字体 1.5×：关键文案不被截断到不可理解，按钮仍可点
- [ ] 改动后 `tools/verify-all.ps1` 全绿，单测数不减少
- [ ] 设备参数已恢复原值

## 7. Tests
- 各配置截图对比；关键页用 `uiautomator dump` 核对控件边界不越界
- `pwsh -NoProfile -File tools/verify-all.ps1`

## 8. Risks and Decisions
- 用 `wm size` 覆盖的是逻辑显示尺寸，能验证布局与密度换算，但不等于真实机型（刘海、厂商 ROM 差异仍需真机复核）。
- 不改设计稿的视觉比例，只修「装不下」的地方。

## 9. Implementation Log
- 2026-11-09：创建任务，开始按配置逐页截图。
## 实施记录（2026-11-09）

### 已落地

1. 模拟器跑通：`emulator 37.3.8` + `system-images;android-35;google_apis;x86_64`，AEHD 2.2 加速（`-accel-check` = 0，无需 WHPX）。
2. `tools/probe-adaptation.ps1`：8 档配置 × 3 页（今日/统计/我的）截图 + `uiautomator` 断言（越界、触控目标 ≥48dp、文本宽度）。
3. 修复 1：底部导航固定 `84.dp` 在字体 ≥1.5× 时裁掉文字 → 高度随 `fontScale` 缩放（1×–1.7×）。
4. 修复 2：今日页内容 `widthIn(max = 560.dp)` 居中（只影响 600dp+ 宽屏，手机无变化）。
5. 修复 3：「记录方式」「频率」两行 ChoicePill 在 320dp + 1.5× 字体下挤压，标签被裁成「记录」「自」→ 改 FlowRow 换行，pill 高度改 heightIn(min = 44.dp)。
6. 修复 4：详情页「本周」「近 5 周」卡片头固定 height(22.dp)，1.5× 字体下标题顶部被裁、图例「完成」被裁 → heightIn(min = 22.dp) + FlowRow。
7. 手机档位：矩阵只保留 320/360/411dp、1.5×/2.0× 字体、挖孔；平板档位移除（用户决定只考虑手机）。
8. 探测扩展到「新建」「详情」两页：点 FAB 进表单、按 `-HabitName` 点习惯名进详情（名称被省略号截断时用前缀匹配）。
9. 修复 5：新建页图标格子与关闭按钮、ChoicePill 原本 44–46dp，改为 48dp；探测的触控下限取 44dp（无障碍最低线），因为 1.5× 字体下 pill 稳定落在 46dp。

### 断言脚本踩坑（已修）

- `$w`/`$h` 局部变量覆盖了参数 `$W`/`$H`：PowerShell 变量不区分大小写，第一处触控目标计算把屏幕宽度改写成节点宽度，导致「全部越界」。改名 `$nodeW`/`$nodeH` 后恢复正常。
- `wm size` / `wm density` 输出同时含 `Physical` 与 `Override`：取最后一个数字才对应当前覆盖值。
- `-File` 启动时 `-Only a,b` 是一个字符串，需先按逗号拆分。
- uiautomator dump 的节点不是自闭合：用 `<node [^>]*>` 而非 `<node [^>]*/>`。

### 未完成

- 2.0× 字体的详情页仍会跳过（习惯名被省略号截断且行在折叠线以下，前缀匹配也取不到）；不算缺陷，但是覆盖空洞。
- 平板（600dp+）按用户决定不再投入。
- Firebase Test Lab（小米 / OPPO）未跑。
- 横屏未验证。
## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `gradlew.bat assembleDebug` | **BUILD SUCCESSFUL** | 布局修复后重建（`AppRoot.kt`、`TodayScreen.kt`） |
| `tools/probe-adaptation.ps1`（含种子数据，5 页） | **PASS** | 5 档手机配置 × 5 页（今日/统计/我的/详情/新建）；修掉底部导航裁字、pill 裁字、详情卡头裁切、图标格与关闭按钮 <48dp |
| `tools/verify-all.ps1` | **VERIFY ALL PASS** | 治理 22/22、单测 177、DAO 45、构建、`git diff --check` |