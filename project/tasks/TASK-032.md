# TASK-032: 今日页与新建页按设计稿重建

- 状态：`IMPLEMENTED`（`ADR-015`：实现与自测完成，未经独立评审）
- 阶段：`Gate 3`
- 优先级：`P0`（MVP 视觉收口）
- 依赖：`TASK-031`
- 创建日期：`2026-11-06`
- 完成日期：`2026-11-06`（实现完成）

## 1. Objective
把今日页与新建习惯页从 Material 默认样式改成设计稿 `orgins/首页 · 今日-html/Main.dc.html`、`orgins/新建习惯-html/New.dc.html` 的样子（进度环、连续徽章、66dp 习惯行、绿色 FAB、白底圆角导航、卡片表单、图标与颜色选择），并让图标/颜色真正落库。

## 2. Context
用户要求「界面 UI 需和 origins 文件夹里的 HTML 效果一致」。当前统计页已按设计稿重建（`TASK-031`），但今日页仍是默认 `Card` + 文字标题，新建页仍是 `OutlinedTextField` + `FilterChip`，与设计稿差距最大。`habits` 表已有 `icon`/`color` 列（`DATABASE.md`），但 `CreateHabitPresenter.draft()` 把两者写死成默认值，设计稿的图标/颜色选择因此没有落库路径。

## 3. Scope
- 新增 `core/designsystem/Ui.kt`：`SoftCard`、`ProgressRing`、`StreakBadge`、`CheckCircle`、`HabitIconTile`、`PillButton`、`ChoicePill`、`FallingLeaves` 等设计系统组件。
- 新增 `core/designsystem/HabitVisuals.kt`：颜色解析、进度环比例、文案、图标/颜色回退等纯规则（不依赖 Compose，可 JVM 测试）。
- 新增 13 个矢量图标（`res/drawable`），路径值直接取自设计稿 SVG。
- `TodayScreen` 按设计稿重建；`AppRoot` 底部导航改为设计稿样式；`CreateHabitScreen` 按设计稿重建。
- `CreateHabitForm`/`CreateHabitPresenter` 增加 `icon`/`color` 字段与 `onIconChange`/`onColorChange`。
- 单测：`HabitVisualsTest`（11 项）+ `CreateHabitPresenterTest` 新增 2 项。

## 4. Out of Scope
- **习惯详情页**（`orgins/习惯详情-html/Detail.dc.html`）：需要日志历史、热力图与编辑入口，另立任务。
- **设计稿的「每日提醒」开关**：`F-012` 未实现，渲染一个不工作的开关属于假功能，本轮不做。
- **编辑弹窗改图标/颜色**：`UpdateHabitUseCase` 只更新名称/目标/单位，扩展它超出本任务。
- 真机视觉走查的**扩展范围**：本轮只对照今日页、全部完成态与新建页渲染；新建页保存后图标/颜色是否落库的设备复验视设备可用性，见 §7。
- 通知权限清理与备份（`TASK-030`）。

## 5. Expected Files
- 新增：`core/designsystem/Ui.kt`、`core/designsystem/HabitVisuals.kt`、`res/drawable/ic_*.xml`（13 个）、`HabitVisualsTest.kt`、本文件
- 修改：`TodayScreen.kt`、`CreateHabitScreen.kt`、`CreateHabitForm.kt`、`CreateHabitPresenter.kt`、`AppRoot.kt`、`AppDestination.kt`、`Color.kt`、`CreateHabitPresenterTest.kt`、`FEATURE_MATRIX.md`、`PROJECT_STATUS.md`、`README.md`、`TEST_REPORT.md`、`LAST_HANDOFF.md`

## 6. Acceptance Criteria
- [x] 今日页有「M月d日 · 周X」14sp + 「今天」32sp 标题、96dp 进度环与 132dp 进度卡、右上「连续 N 天」徽章
- [x] 习惯行为 66dp 白卡（20dp 圆角），左侧 44dp 勾选圈（未完成 2dp 描边 / 完成实心 + 勾），右侧 36dp 图标底
- [x] 全部完成时进度环满格并出现叶片飘落层，不弹窗
- [x] 底部导航为白底、28dp 上圆角、选中项绿色胶囊（`#E4EDE6`/`#3F6B4D`）、图标为设计稿线性图标
- [x] 新建页为卡片表单：名称、记录方式、每日目标（数值时）、6 个图标、3 个颜色、频率与自定义星期、底部全圆「开始养成」
- [x] 图标/颜色选择写入 `NewHabit`（不写死默认值），并有单测证明
- [x] 纯规则（颜色解析、环比例、文案、回退）有 JVM 单测
- [x] `compileDebugKotlin` 与 `assembleDebug` 通过；`run-unit-tests.ps1` 137 项通过
- [x] 真机（iQOO Neo7 SE / Android 15）安装后对照：今日页进度卡/徽章/行/FAB/导航、全部完成态（满环 + 徽章 + 叶片）与设计稿一致；新建页渲染一致
- [x] 真机发现的问题（新建页底部导航未隐藏，压住「开始养成」）已修复：非顶层路由不再显示底部导航

## 7. Tests
- 单元测试：`HabitVisualsTest` 11 项（颜色解析/回退、环比例、文案、徽章口径、图标回退）；`CreateHabitPresenterTest` +2（图标颜色落库、新表单默认值）
- 构建/静态检查：`compileDebugKotlin`、`assembleDebug`、`tools/run-unit-tests.ps1`、`tools/verify-all.ps1`
- 真机验证（iQOO Neo7 SE / Android 15，1080×2400 / 480dpi）：**今日页与全部完成态通过**（`today.png`/`done2.png`）；**新建页渲染通过**（`create2.png`），并暴露底部导航遮挡按钮的缺陷，已修复。修复后的复装因设备掉线未完成，**图标/颜色落库的设备复验未做**

## 8. Risks and Decisions
- **无真机**：布局在 390×844 画板外（平板/大屏）按弹性宽度展开，圆环 96dp、条形高度固定，未实测。
- **设计稿的「图标/颜色」语义**：勾选圈使用用户选择的颜色（默认苔绿，与设计稿一致）；行右侧图标底色固定暖白、描边色按图标固定，与设计稿一致。两者分工写入本文件，避免同一颜色来源两种解释。
- **连续徽章口径**：设计稿只有一个数字，而连续天数按习惯记录（`F-009`）。徽章取「今日计划内习惯的最大当前连续」，0 时隐藏；不编造全局连续。规则落在 `HabitVisuals.bestStreak`。
- **叶片动画未处理系统减弱动效**：设计稿有 `prefers-reduced-motion` 分支，Android 侧需要读 `Settings.Global.ANIMATOR_DURATION_SCALE`，属额外系统读取，未做。
- **旧数据图标**：`TASK-012` 起创建的 `icon="star"` 无对应图形，`HabitVisuals.iconKey` 统一回退到嫩芽；未做数据迁移。
- 设计稿为只读资产（`AGENTS.md` §11），本轮按用户要求作为**视觉参考**使用，验收依据是 §6 的可测断言。

## 9. Implementation Log
- 2026-11-06：先取设计稿内联样式数值（背景 `#FAF7F2`、卡白 `#FFFFFF`、圆角 24/20、环 96dp/12 单位描边、徽章 `#FCEBD5`/`#8A4B0C`、导航 84dp/28dp 圆角/胶囊 `#E4EDE6`、按钮 `#4A7A59`），不靠目测。
- 2026-11-06：图标用 `res/drawable` 矢量，`pathData` 直接抄设计稿 SVG（含 `s`/`a`/`c` 命令），避免引 `material-icons-extended` 新依赖。
- 2026-11-06：发现 `draft()` 写死 `DEFAULT_ICON/DEFAULT_COLOR`，先加表单字段与 `onIconChange/onColorChange`，再写选择器；单测用「保存后的 NewHabit」断言，防止选择器与落库再次脱节。
- 2026-11-06：导航栏不能再用 `NavigationBar`（无法表达 28dp 上圆角与胶囊尺寸），改为 `Surface` + 自定义 `Row`，并补 `navigationBars` 内边距（targetSdk 35 强制 edge-to-edge）。
- 2026-11-06：设计稿的「每日提醒」开关与「习惯详情」页没有对应实现，明确写在 §4，不用假控件凑齐画面。
- 2026-11-06：设备接入后安装并截图。首次 `adb install` 报 `INSTALL_FAILED_ABORTED: User rejected permissions`（锁屏时 vivo 自动拒绝安装）；`input keyevent KEYCODE_WAKEUP` + `wm dismiss-keyguard` 后安装成功。
- 2026-11-06：真机截图暴露缺陷——新建页仍显示底部导航，「开始养成」压住频率卡。改为仅在三个顶层路由显示导航栏；同时给名称输入框加占位文案「例如：晨跑」。
- 2026-11-06：复装时设备从 `adb devices` 掉线（`offline` → 消失），修复后的新建页截图与图标/颜色落库复验**未完成**，如实记入 §7。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/run-unit-tests.ps1` | **PASS** | `OK (137 tests)`：124 → 137（`HabitVisualsTest` 11、`CreateHabitPresenterTest` +2） |
| `gradlew.bat assembleDebug` | **PASS** | `BUILD SUCCESSFUL`，13s，APK 10,559,569 B |
| 真机：安装 + 今日页 / 全部完成态 / 新建页对照 | **PASS**（范围有限） | 今日页与设计稿一致；点勾选后满环 + 「今天也做到了」+「连续 1 天」徽章 + 叶片；新建页渲染一致。截图在 `.gradle/shots/`（不入库） |
| 真机：修复后复装 | `NOT_RUN` | 设备掉线（`adb devices` 为空），修复后的新建页与图标/颜色落库复验未做 |
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS**（exit 0） | 见下（治理 + 单测 + 构建 + `git diff --check`） |

## 11. Completion Review
- [x] 验收标准满足（§6 中「真机视觉」一项按 §7 记为 NOT_RUN，不主张已验证）
- [x] 测试实际运行
- [ ] 独立 Reviewer `APPROVE`（`ADR-015` 批量评审）

## 12. Handoff
- 当前剩余工作：真机视觉走查（进度环、叶片、图标线性度、导航胶囊），以及设计稿里的习惯详情页。
- 下一条安全操作：接设备后 `gradlew.bat connectedDebugAndroidTest` 不适用（无 UI 用例），改为安装 APK 手动逐项对照 §6。
- Git 分支/提交：`main` 上直接实现（`IMPLEMENTED` 合入，`ADR-015`）。

## 13. Independent Review
- Reviewer record：待创建（批量评审）
- 结论：`PENDING`（`IMPLEMENTED`）

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-032
- 状态：`IMPLEMENTED`
- 阶段：Gate 3
- 验收：通过 9 项 / 未通过 0 项
- 最近测试记录：2026-11-06 / **OK (137 tests)**
<!-- close-task:end -->
