# REVIEW-TASK-021: 独立评审记录

- 审查任务：`TASK-021`
- 审查 commit：`5707e435944e38d909f432e97ac0f544ce458e31`
- 审查范围：`git diff main...HEAD`（`142c5ea`..`5707e43`，1 个提交，16 文件 +316/−19：`StreakRules`、`GetStreakUseCase`、仓储两方法、Today presenter/UI/状态、8 项规则测试 + 2 项 presenter 测试、状态文件）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`

评审时工作区：`git status --short` 为空（开始前）。本轮为只读核对 + 门禁复跑 + 7 组变异（M1–M6 复现开发者的，M8 为设计验证）；所有变异已还原，结束时工作区仅剩本评审文件。

## 1. Scope

**裁决：通过。**

- 与 `TASK-021` §3 一致：纯规则、仓储读取方法、UseCase、presenter 文案与行内渲染；未改 schema、未加依赖、未碰详情页与热力图。
- `REQUIREMENTS` 32/33/93 的三条口径都落到了代码注释与测试：按版本、非计划日跳过、按计划执行日。
- 今天未完成不算断的实现与 `REQUIREMENTS` 93「**过去**已到期的计划日缺失或未完成，视为失败」一致——不是临时 UX 偏好，规格本身只把「过去」判失败。

## 2. Diff 核对

**裁决：通过（2 处逻辑未被测试锁定，见 §4）。**

- `git diff --check`：**exit 0**；无冲突标记。
- `StreakRules.compute`（`StreakRules.kt:35-70`）：`due(day)` 由「版本覆盖 + 位掩码命中」决定；非计划日既不计数也不重置；`today` 未完成在两条循环里都显式跳过。
- `GetStreakUseCase`（`:20-23`）：只取 `from..today` 的日志并 `filter { status == COMPLETED }`；`RoomHabitRepository` 用既有的 `scheduleDao.findAll` / `logDao.findRange`。
- presenter（`TodayPresenter.kt:88`）在 `describe` 内调用 `streaks(habit.id, date)`；`TodayHabit` 加两个默认 0 的字段，旧构造点不受影响。
- UI（`TodayScreen.kt:181-189`）仅在 `streakText` 非 null 时渲染，0/0 不显示。

## 3. 架构与数据

**裁决：通过。**

- 规则在 `domain/calculation`，读模型在 `domain/usecase`，presenter 不碰 SQL；依赖方向正确。
- 未改 schema；`findRange` 是既有查询，`verify-dao.mjs` 已覆盖其边界与排序（`findRange includes both boundaries and sorts ascending`、`excludes rows outside the window`）。
- `Dispatchers.Main.immediate` 上执行的是「N 次 Room suspend 调用 + 逐日循环」：Room 自己把查询切到事务执行器，主线程不会被 SQL 阻塞；逐日循环的量级见 §6。

## 4. 验证证据

**裁决：通过（测试缺口见 §6 阻断 2）。**

| 命令 / 变异 | 实际结果 |
|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS（exit 0）**：治理 22/22、`OK (81 tests)`、DAO 21 项、BUILD SUCCESSFUL 18s、`no whitespace errors` |
| 基线 `tools/run-unit-tests.ps1` | `OK (81 tests)` / `RESULT: UNIT TESTS PASS` |
| M1 `due()` 恒真 | **Failures: 2**：`aDayThatWasNeverDueDoesNotBreakTheStreak`、`aScheduleChangeIsJudgedByTheVersionInForceOnThatDay` ✓ |
| M2 去掉两条循环里的 `day != today` | **Failures: 2**：`anIncompleteTodayDoesNotBreakTheStreak`、`theLongestRunSurvivesALaterBreak` ✓ |
| M3 presenter `currentStreak = 0` | **Failures: 1**：`theRowShowsTheStreakComputedFromCompletedDays` ✓ |
| M4（本轮新增）删掉 `GetStreakUseCase` 的 `filter { status == COMPLETED }` | **81 全绿** |
| M5（本轮新增）`from = today`（只查今天） | **Failures: 1**：`theRowShowsTheStreakComputedFromCompletedDays` ✓ |
| M6（本轮新增）`scheduleVersions(habitId).takeLast(1)`（只取最新版本） | **81 全绿** |
| M8（本轮新增，设计验证）把回溯循环换成 `current = run`（只留正向一遍） | **81 全绿** |

说明：M4 全绿意味着「只统计 COMPLETED」这条业务规则目前没有任何测试；`LogStatus` 有 `IN_PROGRESS`（数值习惯 0.5/2000 就落这种行），删掉过滤器后这些天会被算进连续天数。M6 全绿意味着「取全部计划版本」的接线没有测试，把仓储改成只返回最新版也不会红。M8 全绿说明 `StreakRules.kt:58-70` 的回溯循环与正向循环在现有语义下等价，属可删的重复实现（见 §6-1）。

未验证项：真机只观察到三个「连续 1 天」的习惯，`current != longest` 的多段历史未在设备上造过；Room entity→domain 映射与 UI 渲染仍无自动化测试（项目既有缺口）。

## 5. 文档与分支

**裁决：不通过（功能已落地，矩阵与状态摘要仍写未开始）。**

1. 🚫 `project/FEATURE_MATRIX.md:24` 的 F-009 仍是 `NOT_STARTED | NOT_STARTED | NOT_STARTED`；`FEATURE_MATRIX.md:52` 的证据索引仍写「F-008~F-014 … 无 `streak` … 引用」——现在明明有 `StreakRules.kt`、`GetStreakUseCase.kt` 与对应测试。
2. 🚫 `project/PROJECT_STATUS.md:22` 仍写「F-008~F-014 未开始」，F-009 已实现并有单测/真机证据。
3. 🚫 `project/handoffs/LAST_HANDOFF.md:49` 的「之后的功能顺序：F-009 连续天数 → …」把 F-009 当未来项，当前任务恰恰是它。
4. 其余一致：`TASK-021` §10 的 81 项与新增 10 项（8 规则 + 2 presenter）相符；§11 的 6/6 与 §6 六条相符；自动摘要 6 项；`README.md:12`、`PROJECT_STATUS.md:8`、`LAST_HANDOFF.md:8` 都是 `TASK-021` `PARTIAL`；分支 `feat/streak` 领先 `main`（`142c5ea`）1 个提交、未推送。

## 6. 结论

- 结论：`REJECT`
- 证据：门禁 exit 0（22/22、0 警告、81 项、DAO 21、构建 18s）；M1/M2/M3/M5 各自打红正确测试，核心日期算术与 presenter 接线成立；但业务规则「只算 COMPLETED」与「取全部计划版本」两条没有任何测试锁定（M4/M6 全绿），且矩阵/状态摘要仍把 F-009 写成未开始。
- 需修复项：
  1. **F-009 的状态同步**：`FEATURE_MATRIX.md:24` 改为与证据一致（实现 `VERIFIED`、自动化 `VERIFIED`、真机 `DEVICE_VERIFIED` 并加脚注「设备只看到 1 天连续；多段/最长未在真机造数据」）；`:52` 的证据索引拆出 F-009 一行（`StreakRules.kt`、`GetStreakUseCase.kt`、`StreakRulesTest`、`TodayPresenterTest`、真机记录）；`PROJECT_STATUS.md:22` 与 `LAST_HANDOFF.md:49` 同步。`TASK-019` 建立这条规矩才两周，功能落地却仍标未开始，等于把它又欠回去。
  2. **给 COMPLETED 过滤加一条测试**（M4）：在 `GetStreakUseCase` 层或 presenter 层造一条 `IN_PROGRESS` 日志（数值习惯 0.5/2000 即可），断言它不进连续天数。现在删掉这行过滤，81 项全绿——这正是「数值习惯当天进行中不应算连续」的回归口。
  3. **给「取全部计划版本」加一条接线测试**（M6）：用两段计划（DAILY 到某日、之后 CUSTOM）跑一次 use case 或 presenter，断言跨版本连续天数；现在把仓储换成只返回最新版也没有测试会红。规则层已有覆盖，缺的是接线。
- 对四个提问的回答：
  1. **重复的两条循环可以合并，而且合并是等价的**。M8 证明：把回溯循环换成 `current = run`（正向一遍结束时 `run` 的值），81 项全绿——因为「today 未完成不重置」这条规则让正向 `run` 在循环结束时恰好等于定义上的当前连续。建议合成一遍，`longest` 与 `current` 在同一个循环里产出，today 跳过规则只写一次；否则以后改语义要同时改两处（M2 就是两处一起改才没漏）。
  2. **730 次迭代不是问题，DB 往返才是**。逐日循环每步只是几次比较，两年 DAILY 也就 ~73 万次比较级别（实际远低），不值得为它加聚合投影；真正的成本在 `describe` 每习惯 4 次查询（见下条）。
  3. **N+1 应记为技术债，但不该在本任务修**。`describe` 现在每习惯 4 次 suspend 查询（`activeSchedule`、`dailyLog`、`scheduleVersions`、`logsInRange`），而 `snapshot` 在每次 Flow 发射和每次 `record` 成功后都会全量重跑（`record` 里还显式再算一次，实际是两遍）。20 个习惯 = 每次点按约 160 次查询。合理做法是另立任务：一次查全部活跃习惯的计划与日志（或让仓储返回 `Map<habitId, StreakInputs>`）。本任务里塞批量化会扩大 diff，不建议。
  4. **不需要新增 Room 级测试，缺的测试在别处**。`findRange` 的 SQL 语义（闭区间、区间外排除、升序）已由 `verify-dao.mjs` 在真实 SQLite 上验证；Room 的 entity→domain 映射是全项目都没有的覆盖，且本机 Gradle 测试 worker 不可用，单为它开例不现实。真正该补的是 §6-2/§6-3 那两条 JVM 测试。
