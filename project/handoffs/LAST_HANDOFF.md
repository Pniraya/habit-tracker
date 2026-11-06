# LAST_HANDOFF.md

> 每次 AI 会话结束前更新。记录事实，不写愿望或推测。

## Snapshot
- 日期时间（含时区）：`2026-11-06`（Asia/Shanghai）
- 当前阶段：`Gate 3`（今日页 / 新建页 / 统计页 / 习惯详情页均已按设计稿实现；看板与详情页已真机对照）
- 当前任务：`TASK-033`（`IMPLEMENTED`：习惯详情页，154 项单测 + 真机走通，缺独立评审）；`TASK-032`/`TASK-034`/`TASK-031` 均已 `IMPLEMENTED` 合入待批量评审；下一条 `TASK-030`（MVP 收口：备份导出 + 权限清理）
- 当前 Git 分支：`main`（本地，无远程）；最近 commit 以 `git log -1 --oneline` 为准
- **会话状态：可继续**——评审按 `ADR-015` 批量后置；设备（`10CDAK0B71005KU`）在线，`TASK-033` 详情页四步交互已真机走通
- **设备测试怎么跑**：手机**唤醒** + `adb shell svc power stayon usb` + `gradlew.bat connectedDebugAndroidTest`。当前没有 UI 自动化用例，界面只能装 APK 手动对照任务 §6

## Completed This Session
- `TASK-033`（习惯详情页，`orgins/习惯详情-html/Detail.dc.html`）：
  - 新增纯规则 `feature/detail/HabitDetailRules.kt`：`DayState`（DONE/MISSED/NOT_DUE/FUTURE）、`week`（周一→周日，标记今天）、`fiveWeeks`（5 个周一对齐的行）、`stats`（累计/近 5 周完成率/本月次数）、`frequencyLabel`
  - 新增 `HabitDetailPresenter`：从**第一个计划版本**开始读日志（保证连续与累计不被 5 周窗口截断），组合 `GetStreakUseCase.fromLoaded` 与规则；编辑走 `UpdateHabitUseCase` 后重载
  - 新增 `HabitDetailScreen`：44dp 返回/编辑圆钮、128dp 杏橙连续卡（60sp 数字 + 72dp 叶片圆）、本周 7×40dp 圆点、近 5 周 5×7 网格与图例、三张 76dp 统计卡、计划行
  - 导航：`AppRoutes.HABIT_DETAIL = "habit/{habitId}"` + `habitDetail(id)`，`AppRoot` 用 `navArgument(LongType)` 建 presenter；详情为全屏路由（不显示底部导航）
  - 今日页行标题改为进入详情；勾选圈仍是打卡/数值录入（设计稿的语义）
- 设计取舍（已写进 `TASK-033` §8）：
  - 第三张统计卡改为「本月次数」（`duration_seconds` 从未写入，设计稿「每次平均 22 分钟」会是假数据）
  - 计划行不含提醒时间（`F-012` 未实现，避免假控件）
  - 5 周网格周一对齐（设计稿 35 格平铺在非周日会与表头错位）；未来日为浅色，不算失败

## Files Changed
- 新增：`feature/detail/HabitDetailRules.kt`、`HabitDetailPresenter.kt`、`HabitDetailScreen.kt`、`test/.../HabitDetailRulesTest.kt`、`res/drawable/ic_arrow_back.xml`、`ic_edit.xml`、`ic_calendar.xml`、`project/tasks/TASK-033.md`
- 修改：`navigation/AppRoutes.kt`、`AppRoot.kt`、`feature/today/TodayScreen.kt`、`FEATURE_MATRIX.md`（F-011 转 `DEVICE_VERIFIED`、脚注纠正）、`PROJECT_STATUS.md`、`README.md`、`TEST_REPORT.md`、本文件

## Verification Actually Run
| 命令 | 结果 | 备注 |
|---|---|---|
| `pwsh -NoProfile -File tools/run-unit-tests.ps1` | **OK (154 tests)** | 145 → 154：`HabitDetailRulesTest` 9 项 |
| `gradlew.bat compileDebugKotlin` | **BUILD SUCCESSFUL** | 13s |
| `gradlew.bat assembleDebug` | **BUILD SUCCESSFUL** | 安装包已装到真机 |
| 真机：行点击进详情 → 打卡 → 重进详情 | **PASS** | 打卡前 `0/1`、`0%`；打卡后连续 1 天、本周 `1/1`、今天杏橙勾点、近 5 周 `100%`、累计/本月各 1 |
| 真机：编辑铅笔 + 返回 | **PASS** | 弹窗「编辑习惯/名称/保存/取消」；返回今日页 |
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS**（exit 0） | 治理 22/22、单测 154、DAO 38、构建、`git diff --check` |

## Not Verified / Known Gaps
- **多周网格只有单测证据**：真机数据里每个习惯的计划都从今天生效，近 5 周实际只出现今天一格
- **跨周/跨月边界**（周一夜里、月初）未在真机造数，仅单测覆盖
- **详情页不可编辑历史记录**：`F-008` 的「修改某天记录」仍未实现
- **编辑弹窗不能改图标/颜色**（沿用 `UpdateHabitUseCase` 的字段范围）
- 安装坑：锁屏时 `adb install` 会 `INSTALL_FAILED_ABORTED: User rejected permissions`，先唤醒 + `dismiss-keyguard`
- 真机测试期间切换过 `Rread`/`Wa'l'k`/`Wa't'er` 的完成状态（测试习惯，非真实数据）
- 统计页周/年切换、图标替换仍是 `TASK-031` 的已知缺口
- **叶片动画不读系统减弱动效设置**；平板上 390×844 画板外的布局未实测

## Lessons Worth Keeping
- **日志窗口要覆盖连续天数**：详情页画 5 周，但连续/累计必须从第一个计划版本算起；只按可见窗口读日志会把历史变成假「漏做」
- **未来日不是漏做**：判定顺序必须 `FUTURE > NOT_DUE > DONE > MISSED`，否则周五看周六会显示失败
- **完成率的分母是「应做天数」**：设计稿只写百分比，分母来自计划版本，不能用总天数
- **测试期望先对日历**：两处失败都是「周五的周六应为 FUTURE」「5 周分母是整窗口」这类日历事实，不是实现错
- 设计稿里的控件要有对应实现才画：提醒行、平均用时都因后端缺失被替换并记录，而不是摆假数据

## Next Safe Action
1. 下一步实现 `TASK-030`（MVP 收口：备份导出 + 权限清理），先读 `TASK-030` §3 与 `DATABASE.md` 的备份格式
2. 或继续小步打磨：详情页空态/多周数据造数、跨周边界用例
3. 恢复开工前先跑 `tools/verify-all.ps1` 确认 `main` 仍绿；不要把 `TASK-033` 改成 `DONE`——它缺独立评审
