# REVIEW-TASK-021: 独立评审记录

- 审查任务：`TASK-021`（F-009 当前/最长连续天数）
- 审查 commit：`3104352721424e3b557fe877161a4c53cdd6593b`（`git rev-parse HEAD`，分支 `feat/streak`，`main` = `281667b03884f83215b54e00bb1138835ae41130`）
- 审查范围：`git diff 281667b...3104352`——18 文件 +453/−42，含 `5707e43`（第 1 轮被拒的实现）与 `3104352`（本轮修复）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现，本轮全程只读核对 + 变异）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`

工作区：开始时 `git status --short` 为空（上一轮遗留的 `from = today` 已不在文件中，`GetStreakUseCase.kt:18` 为 `schedules.minOf { it.effectiveFrom }`）。7 组变异逐组用 `git checkout -- <path>` 还原并当场确认 `git status --short` 为空；结束时工作区只有本评审文件一处改动。

## 1. Scope

**裁决：通过。**

- 与 `TASK-021` §3/§5 一致：`StreakRules`（纯规则）、`HabitRepository` 两个只读方法 + `RoomHabitRepository`/`FakeHabitRepository` 实现、`GetStreakUseCase`、`TodayPresenter`/`TodayUiState`/`TodayViewModel`/`TodayScreen`、规则与 presenter 测试；文档同步属 `AGENTS.md` §6 的强制项。未碰 schema、未加依赖/权限、未做详情页与热力图（§4 Out of Scope 守住）。
- 第 1 轮的三条拒绝理由逐条复核：

| 第 1 轮阻断项 | 本轮实际状态 | 我的验证方式 |
|---|---|---|
| (a) `FEATURE_MATRIX`/`PROJECT_STATUS`/`LAST_HANDOFF` 仍写 F-009 未开始 | 已改：矩阵 `F-009` = `DEVICE_VERIFIED / VERIFIED / DEVICE_VERIFIED`（`FEATURE_MATRIX.md:24`），证据索引新增 F-009 两条（`:46`、`:53`），`PROJECT_STATUS.md:8/22`、`LAST_HANDOFF.md:8/49`、`README.md:12` 均为 `TASK-021 PARTIAL` | 读文件 + `check-governance.ps1` 的 `readme matches status`/`feature matrix vocabulary`/`handoff snapshot filled` 全绿 |
| (b) 删掉 `COMPLETED` 过滤仍全绿 | 已补测试：`TodayPresenterTest.anInProgressDayDoesNotJoinTheStreak`（数值习惯 1/2000 ml = `IN_PROGRESS`；断言 `current=0, longest=0` 且该日确为 `IN_PROGRESS`） | 我的 M1 变异只打红这一条（`Failures: 1`） |
| (c) 只取最新计划版本仍全绿 | 已补接线测试：`TodayPresenterTest.everyScheduleVersionFeedsTheStreak`（DAILY 到 11/4、11/5 起 CUSTOM 周一三五，日志 11/2–11/4，断言 `current=3`）；`FakeHabitRepository.changeSchedule` 现在会像 `HabitScheduleWriter` 一样关闭旧版本 | 我的 M2 变异只打红这一条（`Failures: 1`） |

## 2. Diff 核对

**裁决：通过。**

- `git diff --check`：**exit 0**，无空白错误、无冲突标记（`check-governance` 的 `no conflict markers`、`no trailing whitespace` 亦通过）。
- 18 个文件全部能对应到 §5 Expected Files 或 `AGENTS.md` §6 的文档同步义务；没有顺手重构、没有新依赖/权限/Gradle module、没有新增产品功能。
- 业务规则位置正确：日期与计划口径只在 `domain/calculation/StreakRules.kt`；`TodayScreen` 只做渲染（`:181-188` 调 `TodayPresenter.streakText`），DAO 未出现规则。
- 未删除或绕过既有测试：`TodayPresenterTest` 只把 `createHabit` 的创建日参数化（默认值仍为 `monday`，旧用例行为不变），其余为新增。
- 第 2 个 commit 相对第 1 个的净变化与提交信息 `fix(streak): merge the loops and cover the wiring` 相符：合并两条循环、补两条接线测试、补文档。

## 3. 架构、数据与设计判断

**裁决：通过（I/O 量记为后续任务，见 §6-3）。**

- 分层：规则在 `domain/calculation`，读模型在 `domain/usecase`，仓储方法 `scheduleVersions`/`logsInRange` 只返回 domain 模型，presenter 不碰 SQL。`GetStreakUseCase` 引用 `data.local.entity.LogStatus` 属仓库既有形态（`domain/model/Habit.kt:34` 的 `DailyLog.status` 本就是该枚举，`TASK-011` §8 已登记为已知技术债），非本任务新增。
- 数据：未改 schema、未改列/可空性/索引；`scheduleVersions` 走既有的 `HabitScheduleDao.findAll`（`ORDER BY effective_from ASC`，与接口注释「oldest first」一致），`logsInRange` 走既有的 `HabitLogDao.findRange`（闭区间语义已由 `verify-dao.mjs` 21/21 覆盖）。
- **单次正向遍历 + `current = run` 对所有情况成立**。不变量：只有「应执行日且未完成且**不是今天**」才把 `run` 清零，其余日期不改变 `run`；因此循环结束时 `run` 恰好等于「从今天（今天是应执行日且已完成）或今天之前最后一个应执行日往回数、连续已完成的应执行日数」，与「从今天/昨天反向扫描」的定义相同。
  - 另做了一次**独立模型穷举**（不在仓库内，防自证）：84 组计划配置（DAILY/WEEKDAYS/CUSTOM、多版本边界、`effectiveUntil` 开闭）× 12 天窗口的全部 4096 种完成子集 = **344,064 例，正向 `run` 与反向扫描不一致 0 例**；另抽样 49,224 例验证 `current ≤ longest` 恒成立。这是对设计命题的佐证，不替代 JVM 测试。
  - 残余风险（不阻断）：等价性依赖 `day != today` 一个条件。若以后加「宽限日/补签/今天算断」之类规则，`current = run` 会静默失效，因为现有 8 条规则测试里没有「昨天漏、今天补 → 1」这种混合用例（`theLongestRunSurvivesALaterBreak` 覆盖的是「过去漏、今天未完成」）。建议补一条 `aMissedYesterdayWithACompletedTodayRestartsAtOne`。
- **今天未完成时当前连续允许为 0，也允许不为 0**：`REQUIREMENTS.md:96`「今天尚未结束时，未完成不提前判失败」使 `today` 不清零；若今天之前的应执行日已漏，`current` 就该是 0（第 93 行「过去已到期…视为失败」）。此语义与 `streakText` 的表现一致：`0/0 → null`（新习惯不显示「连续 0 天」）、`0/7 → 「最长 7 天」`，不违反第 71 行「不施压文案」。
- **UseCase 的工作量：算得对，但读得偏多**。每个习惯每次 `describe` 现在是 4 次 suspend 查询（`activeSchedule`、`dailyLog`、`scheduleVersions`、`logsInRange`），`logsInRange` 的区间是「首个计划版本 → 今天」，即把该习惯全部历史日志读进内存再在 Kotlin 里过滤；`snapshot` 在每次习惯 Flow 发射与每次 `record` 成功后都会对所有习惯重跑。功能正确、区间有界（不会读到未来），当前规模（个位数习惯、月级历史）可接受，但随历史增长是 O(习惯数 × 历史天数) 的行读取。属性能债，不该在本任务里以扩大 diff 的方式修（见 §6-3）。

## 4. 验证证据

**裁决：通过。**

实际命令与退出码（Windows / JDK 17 / Gradle 8.9，本机）：

| 命令 | 退出码 | 输出摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | **0** | `RESULT: VERIFY ALL PASS`：`check-governance` exit 0（14s）、`run-unit-tests` exit 0（26s，`OK (83 tests)`）、`verify-dao` exit 0（21 checks, 0 failed）、`assembleDebug` exit 0（`BUILD SUCCESSFUL in 22s`）、`git diff --check` exit 0（`no whitespace errors`） |
| `pwsh -NoProfile -File tools/check-governance.ps1` | **0** | `SUMMARY: 22 checks, 22 passed, 0 warnings, 0 failed` |
| `git status --short`（开始前） | 0 | 空 |
| `git rev-parse HEAD` | 0 | `3104352721424e3b557fe877161a4c53cdd6593b` |
| 基线 `tools/run-unit-tests.ps1` | 0 | `OK (83 tests)`；= 71 + 新增 12（`StreakRulesTest` 8 项 + `TodayPresenterTest` 4 项），与 `TASK-021` §10、`TEST_REPORT.md` 一致 |

变异表（每组：改一处 → `run-unit-tests.ps1` → `git checkout -- <path>` → 确认 `git status --short` 为空）：

| # | 变异 | 文件 | 结果 | 打红的测试 | 是否打中正确的那条 |
|---|---|---|---|---|---|
| M1 | 删掉 `.filter { it.status == LogStatus.COMPLETED }` | `GetStreakUseCase.kt:21` | **FAILED（exit 1）**：`Tests run: 83, Failures: 1` | `anInProgressDayDoesNotJoinTheStreak` | ✅ 第 1 轮阻断 (b) 的回归口已锁 |
| M2 | `scheduleVersions(habitId).takeLast(1)`（只取最新版本） | `GetStreakUseCase.kt:16` | **FAILED**：`Failures: 1` | `everyScheduleVersionFeedsTheStreak` | ✅ 第 1 轮阻断 (c) 的回归口已锁 |
| M3 | `due()` 恒真（`schedules.isNotEmpty()`） | `StreakRules.kt:35-39` | **FAILED**：`Failures: 2` | `aDayThatWasNeverDueDoesNotBreakTheStreak`、`aScheduleChangeIsJudgedByTheVersionInForceOnThatDay` | ✅ 两条都是「按计划执行日」的正主 |
| M4 | 删掉「今天不算断」：`else if (day != today)` → `else` | `StreakRules.kt:54` | **FAILED**：`Failures: 2` | `anIncompleteTodayDoesNotBreakTheStreak`、`theLongestRunSurvivesALaterBreak` | ✅ 主目标是第一条，第二条属连带（该用例的今天也未完成） |
| M5 | presenter 把 `currentStreak`/`longestStreak` 写死 0 | `TodayPresenter.kt:100-101` | **FAILED**：`Failures: 2` | `theRowShowsTheStreakComputedFromCompletedDays`、`everyScheduleVersionFeedsTheStreak` | ✅ 行内渲染那条正是主目标 |
| M6 | 去掉 `due()` 的 `effectiveUntil` 上界 | `StreakRules.kt:37` | **FAILED**：`Failures: 1` | `aScheduleChangeIsJudgedByTheVersionInForceOnThatDay` | ✅ 版本边界有测试守着 |
| M7 | `if (run > longest) longest = run` → `longest = run` | `StreakRules.kt:53` | **FAILED**：`Failures: 1` | `theLongestRunSurvivesALaterBreak` | ✅ |

未验证项（如实记录）：
- 真机仍是**只有 TASK-021 §10 的文字叙述**（三个已完成习惯显示 `连续 1 天`，进行中的数值习惯不显示），`project/reports` 下没有对应截图/dump（F-006/F-007 有 `DEVICE_DUMP-2026-11-05.md`）。沿用本仓库既有惯例可接受，但比那两条弱；`current != longest`、计划变更、跨多段历史这三类从未在设备上造数。
- Room 的 entity→domain 映射、Compose 渲染仍无自动化测试（全项目既有缺口，非本任务引入）。
- 我只在 JVM 上验证；本机 `gradlew test` 因沙箱禁用测试 worker 不可用，结论以 `run-unit-tests.ps1` 为准（与 `AGENTS.md` §5 一致）。

## 5. 文档与分支

**裁决：通过（3 处瑕疵，均不阻断合并）。**

- `FEATURE_MATRIX.md:24` 的 F-009 三列与证据相符；`更新规则` 第 62 行允许「真机列记 `DEVICE_VERIFIED` + 脚注限定范围」（F-003 先例）。`FEATURE_MATRIX.md:53` 明确写了**只验证了「今天完成 → 连续 1 天」；多段历史、最长连续、计划变更未在真机造数**——设备只证明了一天这条**是诚实的**，没有把单测当成设备证据。
- `README.md:12`、`PROJECT_STATUS.md:8`、`LAST_HANDOFF.md:8` 均为 `TASK-021 PARTIAL / Gate 3` 且 `check-governance` 的 `readme matches status` 通过；`TEST_REPORT.md` 的 `Latest Run` 与 `Runs` 末行一致（83 项）。
- 分支：`feat/streak` 领先 `main`（`281667b`）2 个提交，未推送，工作区干净；`PARTIAL` 任务留在 feature 分支，符合 `AGENTS.md` §7，`partial tasks registered` 检查通过。
- 瑕疵（见 §6 需修复项 1–3）：`TASK-021` §10 内部数字自相矛盾、`FEATURE_MATRIX` 有两处 F-009 条目、`TASK-021` §13 仍是 `PENDING`（合并前由 Developer 指向本记录并勾 §11，属合并动作的一部分）。

## 6. 结论

- 结论：`APPROVE`
- 证据：
  - 门禁：`tools/verify-all.ps1` **exit 0**（治理 **22/22**、0 警告、单测 **83 项**、DAO **21 项**、`BUILD SUCCESSFUL in 22s`、`git diff --check` 干净）；`tools/check-governance.ps1` 单独复跑 **exit 0**、22/22。
  - 第 1 轮三条阻断全部在**本轮独立复现的变异**下被正确测试捕获（M1 打红 `anInProgressDayDoesNotJoinTheStreak`、M2 打红 `everyScheduleVersionFeedsTheStreak`、F-009 的矩阵/状态/handoff 已如实同步）。
  - 设计成立：单次正向遍历 `current = run` 与反向扫描等价（本机穷举 344,064 例 0 不符，且 `current ≤ longest` 恒成立）；今天未完成不清零、允许为 0 的口径与 `REQUIREMENTS.md:93/96` 一致。
  - 额外 4 组自造变异（M3/M4/M6/M7）各自打红正确断言，说明核心日期算术已被测试锁定。
- 阻断项：**无**。
- 需修复项（编号，均非阻断；建议在状态更新/合并时顺手处理）：
  1. `project/tasks/TASK-021.md` §10 的 `verify-all` 行仍写「单测 81 项」，同表首行与 `TEST_REPORT.md` 是 83——同一张证据表里的数字自相矛盾，应改成 83。
  2. `project/FEATURE_MATRIX.md` 有两处 F-009 证据条目（`:46` 与 `:53`），且「真机只验证一天」的限定写在证据索引里而不是文件自带的「脚注（状态为部分完成的行）」块（F-003/F-005 都在脚注块）。内容诚实，仅排版重复，建议合并为一条并放进脚注块。
  3. 「N+1/每习惯 4 次查询已备案」在仓库里**没有任何记录**：`project/tasks` 的最大编号就是 `TASK-021`，没有为它新开任务，且 `TASK-021` §8 的「性能」条目只谈逐日循环、未提查询次数。请落一条任务或在 §8 补一句，否则该说法无法核对。
  4. （测试建议，下个任务）补 `aMissedYesterdayWithACompletedTodayRestartsAtOne`：现有 8 条规则测试没有「昨天漏、今天补」的混合用例，而 `current = run` 的正确性正依赖这条路径。
- 对四个提问的回答：
  1. **单次正向遍历 + `current = run` 对所有情况都正确**（不变量见 §3，穷举佐证见 §4）。合并本身是对的：它消掉了第 1 轮指出的两处重复 today 规则，且没有改变语义。
  2. **今天未完成时当前连续可以为 0，也应该允许**：`REQUIREMENTS.md:93` 只把「过去已到期」判失败，第 96 行明确今天不提前判失败；因此今天不计数也不清零。0 的展示由 `streakText` 处理成「不显示」或「最长 N 天」，不产生施压文案（第 71 行）。
  3. **UseCase 算得对，读得偏多**：每习惯 4 次查询 + 全历史日志读入 + 每次 `record`/Flow 发射全量重算。当前规模可接受，属性能债，另立任务批量化（一次查全部活跃习惯的计划与日志），不要在本任务扩大 diff。
  4. **F-009 的矩阵记录是诚实的**：真机列写 `DEVICE_VERIFIED` 但证据索引明确限定「只验证了今天完成 → 连续 1 天，多段/最长/计划变更未在真机造数」，符合 `FEATURE_MATRIX.md` 第 62 行的先例；唯一问题是条目重复与摆放位置（§6-2）。
