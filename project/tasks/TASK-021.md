# TASK-021: F-009 当前/最长连续天数

- 状态：`PARTIAL`（实现与真机验证完成，等待独立评审）
- 阶段：`Gate 3`
- 优先级：`P1`
- 依赖：`TASK-008`（UseCase 层）、`TASK-011`（Today 状态机）、`TASK-014`（领域规则上移）
- 创建日期：`2026-11-05`
- 完成日期：`TBD`

## 1. Objective
实现 `F-009`：按计划执行日计算每个习惯的**当前连续**与**最长连续**完成天数，并在今日列表显示。

## 2. Context
`REQUIREMENTS.md` 的约束：
- 第 32 行：连续天数按每个日期**当时有效**的计划计算。
- 第 33 行：非计划日不计入应执行次数，**也不打断**连续次数。
- 第 93 行：按计划执行日计算，不是自然日。

因此不能用「今天减昨天一直往回数自然日」，必须逐日判断 `HabitSchedule` 是否覆盖该日且位掩码命中。

## 3. Scope
- `domain/calculation/StreakRules.kt`：纯函数，输入 `List<HabitSchedule>`、已完成日期集合、`today`，输出 `Streak(current, longest)`。
- `HabitRepository` 增加读取某习惯的全部计划版本与已完成日志日期的方法；`RoomHabitRepository` 与 `FakeHabitRepository` 实现。
- `RecordHabitProgressUseCase` 之后新增 `GetStreakUseCase`（或在 Today 加载路径内计算，二者择一，见 §8）。
- `TodayPresenter`：行副标题追加 `连续 N 天 · 最长 M 天`。

## 4. Out of Scope
- 详情页（`REQUIREMENTS.md` 第 56 行的完整统计）；本任务只在今日列表显示两个数字。
- 日历热力图、月/周统计。

## 5. Expected Files
- 新增：`domain/calculation/StreakRules.kt`、`StreakRulesTest.kt`
- 修改：`domain/repository/HabitRepository.kt`、`data/repository/RoomHabitRepository.kt`、`domain/usecase/GetStreakUseCase.kt`、`feature/today/TodayPresenter.kt`、`TodayUiState.kt`、`TodayViewModel.kt`、`TodayScreen.kt`、`TodayPresenterTest.kt`、`FakeHabitRepository.kt`

## 6. Acceptance Criteria
- [x] 非计划日不计数也不打断：周一/三/五习惯，周一、周三完成、周四（今天）未完成 → 连续仍为 2
- [x] 今天未完成不打断已有连续（`anIncompleteTodayDoesNotBreakTheStreak`）
- [x] 计划变更后按**每个日期当时有效**的版本判定（`aScheduleChangeIsJudgedByTheVersionInForceOnThatDay`）
- [x] 最长连续跨越多段，取历史最大值（`theLongestRunSurvivesALaterBreak`：3 最长，当前 1）
- [x] 无计划、无日志时返回 `(0, 0)` 而不是崩溃
- [x] 今日列表显示 `连续 N 天`；`连续 3 天 · 最长 7 天` 由 `streakText` 覆盖

## 7. Tests
- `pwsh -NoProfile -File tools/run-unit-tests.ps1`
- 变异：把「跳过非计划日」改成「按自然日回退」，连续天数测试必须变红
- 真机：造一个多日数据后查看列表显示

## 8. Risks and Decisions
- **今天未完成是否算断**：REQUIREMENTS 未规定。决定：不算断（用户白天还没打卡不该被判失守），因此当前连续从「今天或昨天」开始回溯。
- **性能**：逐日回溯到最早计划日；习惯创建日期通常很近，量级可接受。若数据量增长再改为从日志反向聚合。
- 计划版本读取需要仓储方法；若 `HabitScheduleDao` 没有现成查询，本任务内补一条只读查询，不改 schema。

## 9. Implementation Log
- 2026-11-05：`StreakRules.compute` + `StreakRulesTest`（8 项）；`HabitRepository.scheduleVersions`/`logsInRange` 与两个实现；`GetStreakUseCase`；`TodayHabit` 增加 `currentStreak`/`longestStreak`；`TodayPresenter.streakText` 与行内渲染。
- 2026-11-05：测试里 `createHabit` 原本固定用 `monday` 作为创建日，导致在更早日期记录时抛 `no schedule version covers 2026-10-31`；改为可按日期创建。
- 2026-11-05：真机首次截图没看到连续天数——实际是屏幕休眠时截到了旧进程画面，重新安装并唤醒后显示 `连续 1 天`。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/run-unit-tests.ps1` | **OK (81 tests)** | 新增 10 项：8 项 `StreakRules` + 2 项 presenter（行内连续天数、`streakText`） |
| 变异 M1：`due(day)` 恒真（忽略计划） | **FAILED（2 项）**（预期） | 非计划日与计划变更两条断言变红 |
| 变异 M2：去掉「今天未完成不断」判断 | **FAILED（2 项）**（预期） | `anIncompleteTodayDoesNotBreakTheStreak` 等变红 |
| 变异 M3：presenter 把连续数写死为 0 | **FAILED（1 项）**（预期） | 行内连续天数断言变红 |
| 真机（`iQOO Neo7 SE`） | **通过** | 三个已完成习惯显示 `连续 1 天`；进行中的数值习惯不显示 |
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS**（exit 0） | 治理 22/22、单测 81 项、DAO 21 项、构建通过 |

## 11. Completion Review
- [x] 验收标准满足（6/6）
- [x] 测试实际运行（81 项 + 3 组变异 + 真机）
- [ ] 独立 Reviewer `APPROVE`

## 12. Handoff
- 之后：`F-010`/`F-011` 统计页与热力图。

## 13. Independent Review
- Reviewer record：待创建
- 结论：`PENDING`

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-021
- 状态：`PARTIAL`
- 阶段：Gate 3
- 验收：通过 6 项 / 未通过 0 项
- 最近测试记录：2026-11-05 / **OK (81 tests)**
<!-- close-task:end -->
