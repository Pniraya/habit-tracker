# LAST_HANDOFF.md

> 每次 AI 会话结束前更新。记录事实，不写愿望或推测。

## Snapshot
- 日期时间（含时区）：`2026-11-06`（Asia/Shanghai）
- 当前阶段：`Gate 3`（今日页 / 新建页 / 统计页均已按设计稿重建；无设备，重建后的界面未真机复验）
- 当前任务：`TASK-034`（`IMPLEMENTED`：统计页可滚动、点空白处收起导航栏、删除习惯、归档按月日分组，145 项单测 + 真机走通，缺独立评审）；`TASK-032`/`TASK-031` 均已 `IMPLEMENTED` 合入待批量评审；下一条 `TASK-030`（MVP 收口：备份导出 + 权限清理）
- 当前 Git 分支：`main`（本地，无远程）；最近 commit 以 `git log -1 --oneline` 为准
- **会话状态：可继续**——评审按 `ADR-015` 批量后置；设备（`10CDAK0B71005KU`）在线，`TASK-034` 四项交互已真机走通
- **设备测试怎么跑**：手机**唤醒** + `adb shell svc power stayon usb` + `gradlew.bat connectedDebugAndroidTest`。当前没有 UI 自动化用例，界面只能装 APK 手动对照 `TASK-032` §6

## Completed This Session
- `TASK-032`（`Main.dc.html` + `New.dc.html` 视觉重建）：
  - 新增设计系统 `core/designsystem/Ui.kt`：`SoftCard`（24/20 圆角 + 暖色阴影）、`ProgressRing`（96dp，描边 = 尺寸 × 12/120，完成时半透明光晕弧）、`StreakBadge`、`CheckCircle`（44dp 触点 / 40dp 圆点）、`HabitIconTile`（36dp 暖白圆）、`PillButton`、`ChoicePill`、`FallingLeaves`（6 片叶，6s 循环，相位错开）
  - 新增 `HabitVisuals`（纯 Kotlin，无 Compose 依赖）：`parseColor`、`ringSweep`、`todayMessage`、`bestStreak`、`badgeText`、`iconKey`/`colorKey` 回退
  - 新增 13 个 `res/drawable` 矢量，`pathData` 直接取自设计稿 SVG（不用 `material-icons-extended`）
  - `TodayScreen` 重写：日期行 + 「今天」32sp、进度卡、连续徽章、66dp 习惯行、56dp 绿色 FAB、全部完成叶片层；编辑/归档菜单收成 18dp 的安静「更多」按钮（设计稿无此入口，功能不能丢，写在 §8）
  - `AppRoot` 底部导航改为 `Surface` + 自定义 `Row`（84dp、28dp 上圆角、选中绿色胶囊），并补 `navigationBars` 内边距（targetSdk 35 强制 edge-to-edge）
  - `CreateHabitScreen` 重写为卡片表单；`CreateHabitForm`/`CreateHabitPresenter` 新增 `icon`/`color` 与 `onIconChange`/`onColorChange`，`draft()` 不再写死默认值
- 语义决策：标题徽章的数字取「今日计划内习惯的最大当前连续」（`bestStreak`），0 时隐藏——设计稿只有一个数字，而连续天数按习惯计算（`F-009`），不编造全局连续
- 明确不做的假控件：设计稿的「每日提醒」开关（`F-012` 未实现）；设计稿的「习惯详情」页（`Detail.dc.html`）另立任务

## Files Changed
- 新增：`core/designsystem/Ui.kt`、`core/designsystem/HabitVisuals.kt`、`res/drawable/ic_habit_*.xml`（6）、`ic_check/ic_leaf/ic_close/ic_plus`、`ic_tab_today/ic_tab_stats/ic_tab_profile`、`test/.../HabitVisualsTest.kt`、`project/tasks/TASK-032.md`
- 修改：`TodayScreen.kt`、`CreateHabitScreen.kt`、`CreateHabitForm.kt`、`CreateHabitPresenter.kt`、`AppRoot.kt`、`AppDestination.kt`（图标改为 `@DrawableRes`）、`Color.kt`、`CreateHabitPresenterTest.kt`、`FEATURE_MATRIX.md`、`PROJECT_STATUS.md`、`README.md`、`TEST_REPORT.md`、本文件

## Verification Actually Run
| 命令 | 结果 | 备注 |
|---|---|---|
| `pwsh -NoProfile -File tools/run-unit-tests.ps1` | **OK (137 tests)** | 124 → 137：`HabitVisualsTest` 11、`CreateHabitPresenterTest` +2（图标/颜色落库） |
| `gradlew.bat compileDebugKotlin` | **BUILD SUCCESSFUL** | 首次编译 1m17s；出现一次增量缓存告警后回退全量编译，非失败 |
| `gradlew.bat assembleDebug` | **BUILD SUCCESSFUL** | 13s，APK 10,559,569 B |
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS** | 治理 22/22、单测 137、DAO 37、构建、`git diff --check`；首次运行被治理第 18 项拦下（`TASK-032` 未登记在待批审清单），补登记后复跑通过 |
| 真机（iQOO Neo7 SE / Android 15） | **PASS**（范围有限） | 安装后对照：今日页与设计稿一致；打卡后满环 +「连续 1 天」徽章 + 叶片；新建页渲染一致。截图 `.gradle/shots/today.png`、`done2.png`、`create2.png`（不入库） |
| 真机：统计页滚动 / 空白点击收导航栏 / 删除习惯 / 归档按月日分组 | **PASS** | 趋势卡滚到底完整可见；点击空白后 UI 树无导航节点；删除「T」经二次确认后列表回 3 条；「我的」显示「2026 年 11 月 → 11 月 6 日」两级，随后恢复。截图 `stats_bottom.png`/`bar_hidden2.png`/`delete_dialog.png`/`profile_grouped.png` |
| 真机：修复版复装 + 新建「水滴 + 雾蓝」+ 打卡 + 归档 | **PASS** | 新建页无底部导航、按钮不遮挡；列表显示水滴图标；打卡圈为雾蓝 `#8FAFC4`；归档后列表恢复。截图 `create_final.png`/`today_after_create.png`/`water_done.png`/`after_archive.png`（`.gradle/shots/`，不入库） |

## Not Verified / Known Gaps
- **今日页与新建页均已真机对照**（`F-002`/`F-003` 均为 `DEVICE_VERIFIED`）；未覆盖：滚动、平板宽度、减弱动效设置
- **安装坑**：锁屏时 `adb install` 会 `INSTALL_FAILED_ABORTED: User rejected permissions`，先 `input keyevent KEYCODE_WAKEUP` + `wm dismiss-keyguard` 再装
- **归档分组只有单月单日真机数据**：多月份/跨月排序只由 `ArchiveGroupsTest` 覆盖
- **删除的级联只对无日志的临时习惯做过真机验证**：带历史日志的删除未在真机造数，级联由 schema 与 `verify-schema`/`verify-dao` 覆盖
- **点空白处收导航栏**：交互可发现性低，且没有 UI 自动化；恢复靠再点一次或切 tab
- 真机测试期间为造数据切换过 `Rread`/`Wa'l'k` 的完成状态，未逐条还原（测试习惯，非真实用户数据）
- **习惯详情页未实现**：`orgins/习惯详情-html/Detail.dc.html` 有完整设计，但需要日志历史/热力图/编辑入口，未立任务
- **编辑弹窗不能改图标/颜色**：`UpdateHabitUseCase` 只更新名称/目标/单位
- **旧数据 `icon="star"`** 无对应图形，统一回退嫩芽；未做数据迁移（不需要 schema 变更的话也应在编辑页补选项）
- **叶片动画不读系统减弱动效设置**：设计稿有 `prefers-reduced-motion` 分支，Android 侧需读 `ANIMATOR_DURATION_SCALE`
- 平板上 390×844 画板外的弹性布局未实测；`FallingLeaves` 在列表滚动时的绘制开销未测
- 统计页 `TASK-031` 的周/年切换、图标替换仍为已知缺口（见 `PROJECT_STATUS.md` 待批审清单）

## Lessons Worth Keeping
- **选择器必须用「保存后的对象」断言**：`draft()` 写死默认值时，选择器能点、界面能变，但数据库收不到——只测 presenter 的 UI 状态抓不到这类断链
- **设计稿里的控件要有对应实现才画**：提醒开关、详情页入口都没有后端，宁可明确记 `Out of Scope`，也不摆假控件
- **`NavigationBar` 表达不了设计稿的圆角与胶囊尺寸**：Material 组件的样式参数有边界，超出就自己画
- **沙箱与 Gradle 缓存**：本机 `C:\Users\<user>\.gradle` 需要写锁文件，受限沙箱下 `gradlew` 会以 `FileNotFoundException ... .lck` 失败；那是环境拒绝，不是构建错误

## Next Safe Action
1. `TASK-034` 已四项真机走通（`F-020` 新增为 `DEVICE_VERIFIED`）。下一步可做 `TASK-033`（习惯详情页，设计稿 `Detail.dc.html`）或 `TASK-030`（备份导出 + 权限清理）
2. 若继续打磨归档页：补多月份归档数据或给 `ArchiveGroupsTest` 增加跨年用例即可，不需要改 schema
2. 视觉通过后：`project/tasks/TASK-033.md`（习惯详情页，设计稿 `orgins/习惯详情-html/Detail.dc.html`）或 `TASK-030`（备份导出 + 权限清理）
3. 恢复开工前先跑 `tools/verify-all.ps1` 确认 `main` 仍绿；不要把 `TASK-032` 直接改成 `DONE`——它缺独立评审
