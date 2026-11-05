# TASK-024: 接缝补洞（转换器、差分、翻日、往返）

- 状态：`DONE`
- 阶段：`Gate 3`
- 优先级：`P1`
- 依赖：`TASK-022`
- 创建日期：`2026-11-05`
- 完成日期：`2026-11-05`

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
- [x] 现有测试全绿（105 项）

## 7. Tests
- `tools/run-unit-tests.ps1`
- `pwsh -NoProfile -File tools/verify-all.ps1`

## 8. Risks and Decisions
- 差分测试**只抓单边漂移**（实现改了、oracle 没改）。它与实现仍共享 `HabitSchedule` 的数据含义、掩码位语义、`effectiveUntil` 闭区间约定。评审的 10 组变异里它从未是唯一红线。把 `WEEKDAYS_MASK` 从 31 改成 30 时它确实会红（实测 2 项），但**红的原因是 `ScheduleRules.isScheduledOn` 的 `require(mask == weekdayMask)` 抛异常**，属于实现内部的守卫，不是 oracle 独立判出差异——所以它仍然不是「第二双眼睛」，只是「同一双眼睛的第二条路径」。
- `refresh()` 会让今日页在回到前台时多一次整表查询（4 次），可接受；失败写入 `lastError` 而不是崩溃。
- 翻日覆盖：**后台→前台**（`ON_RESUME`）、**进入今日页**（日期不一致时立即刷新）、**停留前台跨 0 点**（定时器等下一次本地午夜）。**未覆盖**：前台手动改系统时间/时区；以及 DST——定时器用的是墙钟差值，在有夏令时的地区可能早/晚一小时触发（Asia/Shanghai 无 DST，属理论情形）。

## 9. Implementation Log
- 2026-11-05：由外部评审建议创建（`project/reports/EXTERNAL_REVIEW_BRIEF.md` 与对方答复），本任务先做四项接缝测试。
- 2026-11-05：实现。新增 `ConvertersTest`（5 项）、`StreakRulesDifferentialTest`（1 项，3000 组随机历史对拍）、`LogStatusRoundTripTest`（3 项）；`TodayPresenter.refresh()` + `TodayViewModel.refresh()` + `AppRoot` 的 `ON_RESUME` 观察者，测试 `crossingMidnightShowsTheNewDay`。单测 91 → 101。
- 2026-11-05：差分测试首次运行即通过（3000 组历史与朴素 oracle 完全一致），说明现有 `StreakRules` 在随机输入下没有口径偏差；这也是本仓第一次有「与实现不共享代码」的判定源。
- 2026-11-05：变异测量期间两次并发跑脚本，Gradle 锁互相干扰导致假结果（一次空输出、一次 `COMPILE FAILED`）；改为串行后重测，M4 实测 5 项红。这条记下来：**并发跑变异会制造假证据**。
- 2026-11-05（第 1 轮评审后）：接线规则抽成 `TodayPresenter.refreshesOn` 并单测；`refresh()` 补 `runCatching`；oracle 改为 epoch 天数 + 区间算术 + 字面量掩码；`verify-dao` 夹具时间改 `08:00:00`；`LAST_HANDOFF`/`TEST_REPORT` 重写；补建 `TASK-025`；把外部答复存入 `project/reports/` 并补来源声明。
- 2026-11-05（第 2 轮评审后）：抽 `millisUntilNextMidnight` 并单测（含闰日与跨年）；进入今日页时若日期不同立即刷新；M1/ME 在 HEAD 重测（3 红 / 2 红且差分红来自 `require` 异常）；证据表按实测更新。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/run-unit-tests.ps1` | **OK (105 tests)** | 91 → 105：转换器 7 项（含闰日、非法日期）、差分 1 项、往返 3 项、翻日 1 项、接线规则 1 项、午夜算术 1 项 |
| 变异 M1：日期格式改 `yyyy-M-d`（去零填充） | **FAILED（3 项）** | 三条都在 `ConvertersTest`（ISO 文本、往返、闰日）；早前记录的「2 项」是加闰日用例之前的旧值 |
| 变异 M2：`refresh()` 改空实现 | **FAILED（1 项）** | `crossingMidnightShowsTheNewDay` |
| 变异 M3：`>= targetSnapshot` 改 `>` | **FAILED（11 项）** | 状态规则与其下游 |
| 变异 M4：`else if (day != today)` 改 `else` | **FAILED（5 项）** | 差分也在其中，但**不是唯一红线**（旧测试同时红），与 §8 的定位一致 |
| 评审 ME：`WEEKDAYS_MASK` 31→30 | **FAILED（2 项）** | `weekdaysMaskIsMondayToFriday` + 差分；差分红是因为 `isScheduledOn` 的 `require(mask == weekdayMask)` 抛异常，**不是 oracle 判别** |
| 复评 R2-MH2：午夜算术 `plusDays(1)` 改 `plusDays(2)` | **FAILED（1 项）** | `theMidnightTimerAimsAtTheNextLocalMidnight`；本轮新增算术在抽出前的同一变异为 104 项全绿 |
| 复评变异 M-H：`refreshesOn` 的 `ON_RESUME` 改 `ON_CREATE` | **FAILED（1 项）** | `onlyReturningToTheForegroundRefreshes`；修复前同一变异 101 项全绿 |
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS** | 治理 22/22、0 警告；单测 105、DAO 27、构建 19-20s |

## 11. Completion Review
- [x] 验收标准满足（5/5）
- [x] 测试实际运行（105 项 + 7 组变异）
- [x] 独立 Reviewer `APPROVE`（第 4 轮，`4a0daef`）

## 12. Handoff
- 之后：`TASK-025` 试 `connectedDebugAndroidTest`。

## 13. Independent Review
- Reviewer record：`project/reviews/REVIEW-TASK-024.md`
- 第 1 轮：`REJECT`（`3b58062`，6 条：接线零覆盖、翻日过度声称、差分独立性、文档不同步、引用不存在文件、任务自相矛盾）
- 第 2 轮：`REJECT`（`f700d4b`，5 条：午夜算术零覆盖、证据未按 HEAD 重测、前台跨 0 点、handoff 旧口径、外部答复无出处）
- 第 3 轮：`REJECT`（`11facff`，2 条文档：证据表 104、handoff 第 1 轮口径）
- 第 4 轮：`APPROVE`（`4a0daef`，0 阻断；10 组变异全部命中正确断言）

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-024
- 状态：`DONE`
- 阶段：Gate 3
- 验收：通过 5 项 / 未通过 0 项
- 最近测试记录：2026-11-05 / **OK (105 tests)**
<!-- close-task:end -->
