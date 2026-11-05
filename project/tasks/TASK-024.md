# TASK-024: 接缝补洞（转换器、差分、翻日、往返）

- 状态：`PARTIAL`
- 阶段：`Gate 3`
- 优先级：`P1`
- 依赖：`TASK-022`
- 创建日期：`2026-11-05`
- 完成日期：待评审通过后

## 1. Objective
补上最便宜、又最可能自发现缺陷的接缝测试：存储格式往返、连续天数的差分校验、跨午夜翻日、数值撤销往返。

## 2. Context
外部评审（`project/reports/EXTERNAL_REVIEW_ANSWERS.md`）指出：现有 90 项测试与变异测试全都围着纯逻辑层转，真实缺陷却出在接缝上；且"测试与实现由同一模型写"会导致共因失效，需要独立 oracle。

## 3. Scope
- `Converters` 往返测试，断言精确存储格式（ISO 日期、`HH:mm:ss`、枚举名）。
- `StreakRules` 差分测试：朴素 oracle + 随机计划/完成集合。
- 跨午夜翻日：`TodayPresenter` 暴露 `refresh()`，界面在回到前台时调用；测试用可控时钟推进一天。
- `2000 → 1000 → 2000` 往返：状态、完成时间、连续天数。

## 4. Out of Scope
- 不做 `connectedDebugAndroidTest`（另立任务）。
- 不改连续天数语义；不改 schema。

## 5. Expected Files
- 新增：`app/src/test/java/com/habittracker/data/local/ConvertersTest.kt`、`app/src/test/java/com/habittracker/domain/calculation/StreakRulesDifferentialTest.kt`、`app/src/test/java/com/habittracker/domain/calculation/LogStatusRoundTripTest.kt`
- 修改：`TodayPresenter.kt`、`TodayViewModel.kt`、`AppRoot.kt`、`TodayPresenterTest.kt`、`tools/verify-dao.mjs`（夹具时间格式）

## 6. Acceptance Criteria
- [x] 转换器往返断言精确存储值（含闰日与拒绝非法日期）
- [x] 差分测试 3000 组随机历史，与 oracle 完全一致
- [x] 时钟跨过午夜后，今日页显示新的一天（presenter 测试 + 回到前台/午夜定时器接线；接线规则由 `TodayPresenter.refreshesOn` 单测覆盖）
- [x] 数值撤销往返后状态与连续天数符合预期（有测试）
- [x] 现有测试全绿（104 项）

## 7. Tests
- `tools/run-unit-tests.ps1`
- `pwsh -NoProfile -File tools/verify-all.ps1`

## 8. Risks and Decisions
- 差分测试**只抓单边漂移**（实现改了、oracle 没改）。它与实现仍共享 `HabitSchedule` 的数据含义、掩码位语义、`effectiveUntil` 闭区间约定。评审做了 10 组变异，它从未是唯一红线；把 `WEEKDAYS_MASK` 从 31 改成 30 时它全绿。所以它不是「第二双眼睛」，只是「同一双眼睛的第二条路径」。
- `refresh()` 会让今日页在回到前台时多一次整表查询（4 次），可接受；失败写入 `lastError` 而不是崩溃。
- 翻日覆盖：**后台→前台**（`ON_RESUME`）与**前台跨 0 点**（定时器等下一次本地午夜）。**未覆盖**：前台手动改时间/时区。

## 9. Implementation Log
- 2026-11-05：由外部评审建议创建（`project/reports/EXTERNAL_REVIEW_BRIEF.md` 与对方答复），本任务先做四项接缝测试。
- 2026-11-05：实现。新增 `ConvertersTest`（5 项）、`StreakRulesDifferentialTest`（1 项，3000 组随机历史对拍）、`LogStatusRoundTripTest`（3 项）；`TodayPresenter.refresh()` + `TodayViewModel.refresh()` + `AppRoot` 的 `ON_RESUME` 观察者，测试 `crossingMidnightShowsTheNewDay`。单测 91 → 101。
- 2026-11-05：差分测试首次运行即通过（3000 组历史与朴素 oracle 完全一致），说明现有 `StreakRules` 在随机输入下没有口径偏差；这也是本仓第一次有「与实现不共享代码」的判定源。
- 2026-11-05：变异测量期间两次并发跑脚本，Gradle 锁互相干扰导致假结果（一次空输出、一次 `COMPILE FAILED`）；改为串行后重测，M4 实测 5 项红。这条记下来：**并发跑变异会制造假证据**。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/run-unit-tests.ps1` | **OK (104 tests)** | 91 → 104：转换器 7 项（含闰日、非法日期）、差分 1 项、往返 3 项、翻日 1 项、接线规则 1 项 |
| 变异 M1：日期格式改 `yyyy-M-d`（去零填充） | **FAILED（2 项）** | 两条都在 `ConvertersTest`（往返用例不碰转换器） |
| 变异 M2：`refresh()` 改空实现 | **FAILED（1 项）** | `crossingMidnightShowsTheNewDay` |
| 变异 M3：`>= targetSnapshot` 改 `>` | **FAILED（11 项）** | 状态规则与其下游 |
| 变异 M4：`else if (day != today)` 改 `else` | **FAILED（5 项）** | 含 `randomisedHistoriesAgreeWithTheNaiveImplementation`——差分测试能独立判错，不是摆设 |
| 复评变异 M-H：`refreshesOn` 的 `ON_RESUME` 改 `ON_CREATE` | **FAILED（1 项）** | `onlyReturningToTheForegroundRefreshes`；修复前同一变异 101 项全绿 |
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS** | 治理 22/22、0 警告；单测 104、DAO 27、构建 ~25s |

## 11. Completion Review
- [x] 验收标准满足（5/5）
- [x] 测试实际运行（104 项 + 5 组变异）
- [ ] 独立 Reviewer `APPROVE`

## 12. Handoff
- 之后：`TASK-025` 试 `connectedDebugAndroidTest`。

## 13. Independent Review
- Reviewer record：待创建
- 结论：`PENDING`

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-024
- 状态：`PARTIAL`
- 阶段：Gate 3
- 验收：通过 5 项 / 未通过 0 项
- 最近测试记录：2026-11-05 / **OK (104 tests)**
<!-- close-task:end -->
