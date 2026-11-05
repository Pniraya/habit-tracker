# REVIEW-TASK-022: 独立评审记录（第 3 轮）

- 审查任务：`TASK-022`（Today 列表查询批量化，N+1 修复）
- 审查 commit：`0f77faa7ee30067de13a146ad47dc4ed32250cf9`（`git rev-parse HEAD`，分支 `feat/today-queries`，`main` = `bfb3150`）
- 审查范围：本轮增量 `git diff 0e0d5db...0f77faa`（9 文件 +128/−111，源码 3 文件：`GetStreakUseCase.kt`、`GetStreakUseCaseTest.kt`、`TodayPresenterTest.kt`，其余为文档）；累计 `git diff bfb3150...0f77faa`
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现；全程只读核对 + 变异）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`
- 轮次：第 3 轮（第 1 轮 `REJECT`：3 条阻断；第 2 轮 `REJECT`：代码已可合，剩 2 条文档/覆盖声称）

工作区：开始与结束均为 `git status --short` 空。2 组变异逐组 `git checkout -- <path>` 还原，结束复核为空。

## 1. Scope

**裁决：通过。**

- 本轮只做第 2 轮点名的两件事 + 两条建议：删掉 `fromLoaded` 里那行冗余钳位（并把测试改名、注释改成真话）、`TEST_REPORT.md` 句尾重复计数删除、补 `aCompletedTodayAfterAMissedYesterdayRestartsAtOne`、`everyHabitCountsItsOwnDays` 改数据并加 `isCompleted` 断言。无顺手重构、无新依赖/权限/schema 改动、无删测试。
- 与 `TASK-022` §3/§4 一致：`StreakRules` 语义未动（红线守住），接口/DAO/Room 仓储本轮一字未改。
- 第 2 轮两条阻断的复核：

| 第 2 轮阻断 | 本轮状态 | 我怎么验的 |
|---|---|---|
| F-新2 `TEST_REPORT` 同一句 27 与 21 并存 | **已修** | `TEST_REPORT.md:87` 现为「（`verify-dao.mjs`，27 项，含 6 项批量查询场景）……唯一索引与 `ON CONFLICT` 更新。」——句尾那个「21 项」括号已消失，全文再无 21 项残留 |
| F-新1 「3 项钳位测试」是假覆盖 | **已修（采取选项 a）** | `fromLoaded` 里的 `!it.date.isBefore(firstDay) && !it.date.isAfter(today)` 已删除，`completed` 只剩 `COMPLETED` 过滤；测试改名 `rowsOutsideTheComputedWindowDoNotChangeTheCount`，注释明写「防的是将来改成逐行遍历，不是当前风险」；`TASK-022` §8/§10、`LAST_HANDOFF` 的「3 项钳位」措辞已全部改掉 |

## 2. 实际命令与退出码

Windows / JDK 17 / Gradle 8.9，本机（**串行执行**，不与变异并发，避免 Gradle 锁互相干扰）：

| 命令 | 退出码 | 输出摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | **0** | `RESULT: VERIFY ALL PASS`；治理 `22 checks, 22 passed, 0 warnings`（12s）、单测 `OK (91 tests)`、`verify-dao` `checks: 27, failed: 0`、`assembleDebug` `BUILD SUCCESSFUL`、`git diff --check` 干净 |
| `pwsh -NoProfile -File tools/run-unit-tests.ps1`（基线，含在上条） | **0** | `OK (91 tests)`（90 → 91：+1 重启用例） |
| `git rev-parse HEAD` | 0 | `0f77faa7ee30067de13a146ad47dc4ed32250cf9` |
| `git status --short`（开始前 / 结束后） | 0 | 空 / 空 |

声称的四个数字全部复现：**91 单测、27 DAO、治理 22/22、exit 0**。

## 3. 变异表

本轮两个 live 变异 + 前两轮结论的适用性说明。

| # | 变异 | 文件 | 结果 | 打红的测试 | 判断 |
|---|---|---|---|---|---|
| U7 | 删掉 `fromLoaded` 的 `.filter { it.status == LogStatus.COMPLETED }` | `GetStreakUseCase.kt` | **FAILED**：`Tests run: 91, Failures: 2` | `onlyCompletedRowsCount`、`anInProgressDayDoesNotJoinTheStreak` | ✅ 这是钳位删掉之后 `fromLoaded` 仅存的行为过滤，仍被两条测试钉住 |
| U4 | `describe()` 喂 `logsSince.values.flatten()`（跨习惯串日志） | `TodayPresenter.kt` | **FAILED**：`Tests run: 91, Failures: 1` | `everyHabitCountsItsOwnDays` | ✅ 且该用例现在同时断言 `isCompleted` 一真一假，归属错会从两条线索暴露 |
| U6（上轮） | 删 `fromLoaded` 的日期钳位 | — | **变异目标已不存在**：该行本轮被删除 | — | ✅ 这正是本轮要求的处置；不再有「删了也不红」的代码。等价 live 检查即 U7 |
| U1/U2（上轮） | 删 Fake / Room 的批量读 override | `HabitRepository.kt` 未变 | 仍为 **COMPILE FAILED**（接口 4 个方法仍是抽象声明，本轮零改动） | — | ✅ 结论沿用，未重跑 |
| U3/U5（上轮） | `snapshot` 多一次批量读 / 删空列表守卫 | 相关文件本轮未变 | 沿用上轮结论（2 红 / 全绿） | — | ✅ 同上 |
| D1–D3（上轮） | DAO 日期过滤 / id 表 / `BETWEEN` | `app/src/main/.../dao` 本轮未变 | 沿用（各 1 红） | ✅ |

`rowsOutsideTheComputedWindowDoNotChangeTheCount` 现在**明确不是** live 风险的守卫（注释已自陈）：它断言的性质由 `StreakRules.compute` 的 `[firstDay, today]` 遍历范围保证，将来若有人把 `compute` 改成逐行遍历、或把 `fromLoaded` 的日期过滤加回来当「唯一防线」，这条用例才有意义。这样记录是诚实的，我接受。

## 4. Findings

**无阻断项。** 三条不阻断的小问题：

1. **`TASK-022` §10:59 与 `LAST_HANDOFF:29` 的「新增 5 项」枚举里多算了 2 项**：括号里列的是「两个查询计数、每习惯归属、`fromLoaded` 的 3 项」= 6 项，而标签写 5 项。实际增量是 86 → 91 = 5 项（每习惯归属 1 + `fromLoaded` 3 + 重启 1），两个查询计数属于 86 那一档、不在本次增量里。改标签或删掉枚举里的「两个查询计数」即可。数字本身（91）在三份文档里都正确，不影响结论。
2. **`TASK-022` §13「第 3 轮：待复审」**：合并前应写成「第 3 轮：`APPROVE`（`0f77faa`）」，否则任务记录与本评审记录对不上。
3. **上轮遗留、已登记为接受的项**：`RoomHabitRepository` 的空 id 早退守卫仍无测试（presenter 在更外层 return，不可达；`verify-dao` 的 binder 对空列表主动抛错，DAO 层也测不到）；F7 主线程逐日推进仍未在真机测量。两者都写在 `TASK-022` §8 风险里，属「已知未测」，不是隐瞒。

## 5. 文档同步

**裁决：通过。**

| 文件 | 91 / 27 / PARTIAL |
|---|---|
| `README.md:15` / `:62` | `91 项单测` ✅ / `27/27` ✅ |
| `PROJECT_STATUS.md:8/12/13` | `TASK-022（PARTIAL…待独立评审）` ✅ / `27/27` ✅ / `91 项` ✅ |
| `project/reports/TEST_REPORT.md:9` / `:87` | Latest Run `91 项 + DAO 27 项` ✅ / Known Gaps `27 项`（21 已彻底消失）✅ |
| `project/handoffs/LAST_HANDOFF.md:29-30` | `OK (91 tests)` ✅ / DAO 27 ✅ / 分支 `feat/today-queries`、`main @ bfb3150` ✅ |
| `project/tasks/TASK-022.md` §10/§13 | `OK (91 tests)`、U2→COMPILE FAILED、U4→FAILED 1 项、钳位处置 ✅ / 待补第 3 轮结论（见 §4-2） |
| 治理检查 | `22 checks, 22 passed`，含 `test report summary matches` ✅ |

## 6. 结论

- 结论：`APPROVE`

三条阻断项在三个轮次里逐条落地并有我亲手复现的证据：

1. **F1（N+1 在生产侧无人看守）**：接口 4 个批量方法已抽象化——删 Room 或 Fake 的任一 `override` 现在是 `COMPILE FAILED`，不是静默 N+1（第 1 轮同一变异 86/86 全绿）。
2. **F2（批量读缺习惯归属测试）**：`everyHabitCountsItsOwnDays` 打到跨习惯串数据（U4 → 1 红），并额外用 `isCompleted` 一真一假覆盖 `dailyLogs` 归属。
3. **F3（文档计数不同步）**：五份文档现在一致写 91 项 / 27 项 / `PARTIAL` 待评审；第 2 轮点出的两处（`TEST_REPORT` 21 与 27 并存、「3 项钳位测试」）都已改掉，钳位按选项 (a) 删除并在代码注释里写明为什么不再需要。

合并到 `main` 前后请顺手做两件收尾（不阻断，属 housekeeping）：把 `TASK-022` §13 的第 3 轮写成 `APPROVE`（`0f77faa`）、把「新增 5 项」的枚举改成 5 项或删掉其中两个查询计数；合并后按 §6 更新 `PROJECT_STATUS`/`HANDOFF` 的当前任务与阶段。

未验证项（如实声明）：真机上 20 个习惯时长列表的首帧耗时（F7 已登记未测）、`RoomHabitRepository` 空 id 守卫、Room 运行时 SQL 映射（`verify-dao` 验的是语句语义，不是 Room 生成代码）。
