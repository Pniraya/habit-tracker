# REVIEW-TASK-022: 独立评审记录（第 2 轮）

- 审查任务：`TASK-022`（Today 列表查询批量化，N+1 修复）
- 审查 commit：`0e0d5dbef5e5773710c5a35017af6c9ec882bf6a`（`git rev-parse HEAD`，分支 `feat/today-queries`，`main` = `bfb3150`）
- 审查范围：第 1 轮 `b73e03b`（已审）→ 本轮增量 `git diff b73e03b...0e0d5db`（13 文件 +620/−58；源码/测试 5 文件：`HabitRepository`、`GetStreakUseCase`、新增 `GetStreakUseCaseTest`、`TodayPresenterTest`，其余为文档）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现；全程只读核对 + 变异）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`
- 轮次：第 2 轮（第 1 轮结论 `REJECT`，3 条阻断）

工作区：本轮开始时 `git status --short` **为空**（第 1 轮遗留的两处未提交改动已随 `0e0d5db` 入库，属文档）。7 组变异逐组用 `git checkout -- <path>` 还原，结束时 `git status --short` 仍为空——本轮没有留下脏工作区。

## 1. Scope 与第 1 轮阻断的复核

**裁决：通过；3 条阻断里 2 条真正修好、1 条只修到一半（见 §6）。**

增量与 `TASK-022` §3/§5 一致：接口 4 个批量方法改抽象、`GetStreakUseCase` 由 `class`（带仓储）改为 `object`（只留 `fromLoaded`）、新增 `GetStreakUseCaseTest`、`TodayPresenterTest` 增 1 项归属用例、文档同步、`TASK-023` 立项。没有顺手重构、没有新依赖/权限/schema 改动、没有删既有测试。

DAO 与 Room 仓储文件相对 `b73e03b` **一字未动**（`git diff b73e03b...HEAD -- app/src/main/java/com/habittracker/data/` 为空），因此第 1 轮 D1/D2/D3 三条 DAO 变异结论继续有效，本轮未重复跑。

| 第 1 轮阻断 | 本轮实际状态 | 我怎么验的 |
|---|---|---|
| **F1** 接口默认实现 + Room 零覆盖 → 漏 override 静默 N+1 | **已修**：4 个方法去默认体、改抽象声明 | 变异 U2（删 `RoomHabitRepository.logsInRangeFor` 的 `override`）→ `e: Class 'RoomHabitRepository' is not abstract and does not implement abstract member 'logsInRangeFor'` + `RESULT: COMPILE FAILED (exit 1)`；第 1 轮同一变异是 86/86 全绿。顺带 U1（删 Fake 的 `activeSchedules`）也变成 COMPILE FAILED——§8 那条「fake 是绊线」可以退休了，现在的绊线是编译器 |
| **F2** 批量读的「按习惯归属」无测试 | **已修**：`everyHabitCountsItsOwnDays`（Steady 3 天 / Fresh 1 天，断言 3 与 1） | 变异 U4（`describe()` 喂 `logsSince.values.flatten()`）→ `Tests run: 90, Failures: 1`，红的正是 `everyHabitCountsItsOwnDays`；第 1 轮同一变异全绿 |
| **F3** 文档计数不同步（7 行） | **修了 6 行，漏 1 行 + 1 处过度声称** | 见 §5：`TEST_REPORT.md:86` 同一句里「27 项」与「（`verify-dao.mjs`，21 项）」并存 |

## 2. 实际命令与退出码

Windows / JDK 17 / Gradle 8.9，本机：

| 命令 | 退出码 | 输出摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | **0** | `RESULT: VERIFY ALL PASS`；治理 `22 checks, 22 passed, 0 warnings`（12s）、单测 `OK (90 tests)`（23s）、`verify-dao` `checks: 27, failed: 0`、`assembleDebug` `BUILD SUCCESSFUL in 17s`、`git diff --check` `no whitespace errors`；**无** "uncommitted changes" 行 |
| `pwsh -NoProfile -File tools/run-unit-tests.ps1`（基线） | **0** | `OK (90 tests)`（86 → 90，与 §10 一致） |
| `git rev-parse HEAD` | 0 | `0e0d5dbef5e5773710c5a35017af6c9ec882bf6a` |
| `git status --short`（开始前 / 结束后） | 0 | 空 / 空 |

声称的四个数字全部复现：90 单测、27 DAO、治理 22/22、exit 0；构建 17s，落在他说过的 17-21s 内。

## 3. 变异表

每组：改一处 → 跑真实 runner → `git checkout -- <path>` → 确认 `git status --short` 为空。

| # | 变异 | 文件 | 结果 | 打红的测试 | 判断 |
|---|---|---|---|---|---|
| U1 | 删 Fake 的 `activeSchedules` override | `FakeHabitRepository.kt:17` | **COMPILE FAILED**（exit 1） | — | ✅ 抽象化生效，接口层已无「循环兜底」 |
| U2 | 删 Room 的 `logsInRangeFor` override | `RoomHabitRepository.kt:21` | **COMPILE FAILED**（exit 1） | — | ✅ 第 1 轮 F1 关闭（第 1 轮同变异 86/86 全绿） |
| U3 | `snapshot()` 多做一次批量读 | `TodayPresenter.kt` | **FAILED**（Failures: 2）〔第 1 轮实测，本轮该处未改〕 | 两条计数断言 | ✅ 仍有效 |
| U4 | `describe()` 喂 `logsSince.values.flatten()` | `TodayPresenter.kt` | **FAILED**：`Tests run: 90, Failures: 1` | `everyHabitCountsItsOwnDays` | ✅ 第 1 轮 F2 关闭，且只红该红的一条 |
| U5 | 删 Room 空列表早退守卫 | `RoomHabitRepository.kt` | 全绿〔第 1 轮实测，本轮该文件未改〕 | 无 | ⚠️ 仍未覆盖；本轮 §8 已记为「不可达但保留」，接受 |
| U6 | `fromLoaded` 删掉日期钳位 `.filter { !it.date.isBefore(firstDay) && !it.date.isAfter(today) }` | `GetStreakUseCase.kt` | **PASS（90/90 全绿）** | 无 | ❌ 新增的 `rowsOutsideTheScheduleWindowAreIgnored` 抓不到它（F-新1） |
| U7 | `fromLoaded` 删掉 `.filter { it.status == LogStatus.COMPLETED }` | `GetStreakUseCase.kt` | **FAILED**：Failures: 2 | `onlyCompletedRowsCount`、`anInProgressDayDoesNotJoinTheStreak` | ✅ 新增的状态过滤测试真能红 |
| D1 | `findForDate` 去日期过滤 | `HabitLogDao.kt` | **FAILED** 1 项〔第 1 轮实测，本轮未改〕 | `…keeps only that date (expected 2) -> got 4` | ✅ |
| D2 | `findAllForHabits` 用 `IN (SELECT habit_id FROM habits)` 代替 id 表 | `HabitScheduleDao.kt` | **FAILED** 1 项〔同上〕 | `respects the id list` | ✅ |
| D3 | `findRangeForHabits` 去 `BETWEEN` | `HabitLogDao.kt` | **FAILED** 1 项〔同上〕 | `keeps both boundaries` | ✅ |

## 4. Findings

### F-新1（阻断）「3 项钳位测试」里钳位那一项不可能变红

`GetStreakUseCaseTest.rowsOutsideTheScheduleWindowAreIgnored` 的注释写「A completed day before the first version and one in the future must not inflate the count」，`TASK-022` §8 与 `LAST_HANDOFF.md:29` 也把本轮记为「+3 项 `GetStreakUseCase` 钳位测试」。

我删掉 `fromLoaded` 里那行钳位后：**90/90 全绿**（U6）。

原因不是断言写错，而是这行钳位**本身就是冗余的**：`StreakRules.compute` 的循环是 `day = firstDay` → `today`，窗口外的完成日 `completedDates.contains(day)` 根本不会被问到；`isAfter(today)` 那一半也没用，因为 presenter 传进来的 `logs` 来自 `logsInRangeFor(ids, …, date)`，SQL 已 `log_date BETWEEN :from AND :to`。删与不删，行为完全相同。

所以这不是「多写了一个测试」，而是**一条永远绿的新测试 + 一条把覆盖率说高了的记录**。本仓 Lessons 自己写着「别只写测试，要验证测试会失败」；第 1 轮我给的 F6 正是「钳位无覆盖」，现在只有「`invoke` 死代码」那半真的修好了。

修法（二选一，都是一行）：
- (a) 删掉 `fromLoaded` 里那行钳位，注释改成「`compute` 只遍历 `[firstDay, today]`、查询层已按 `[from, to]` 过滤」，测试名改成它真正证明的东西（断言不必变）；
- (b) 保留钳位当纵深防御，但把 §8/§10/`LAST_HANDOFF:29` 的「3 项钳位测试」改成「2 项过滤测试 + 1 项窗口外行不计」，并注明该行冗余、删掉不会红。

（另两项是真的：`onlyCompletedRowsCount` 被 U7 打红；`noScheduleVersionMeansNoStreak` 按代码读会因 `minOf` 在空列表上抛异常而红——这两项未单独跑变异，属代码推断。）

### F-新2（阻断，文档）`TEST_REPORT.md:86` 同一句里 27 与 21 并存

原文：

> 已覆盖：schema …；DAO 声明语句的 SQL 语义（`verify-dao.mjs`，**27 项**，含 6 项批量查询场景）——闭区间边界、CAS 更新…、级联删除、唯一索引与 `ON CONFLICT` 更新（`verify-dao.mjs`，**21 项**）。

第 1 轮报的就是这一行（当时全句 21），本轮在句首补了 27、句尾 21 没删。任务书要求 `TEST_REPORT` 与 27 项一致，这行仍自相矛盾。删掉句尾那个括号即可。

### F-新3（非阻断，提醒）本轮说明里「漏一天后即使今天完成 current 也是 0」与代码不符

消息里写「they assert 0 current / 1 longest, because a missed yesterday resets the run even when today is complete」。**0/1 没错，但理由不对**：两条新用例的「今天」都没有完成行（`rowsOutsideTheScheduleWindowAreIgnored` 只喂 10-31/11-01/11-04，今天 11-03 无日志；`onlyCompletedRowsCount` 也无 11-03），current=0 是「今天未完成 + 11-02 漏」的结果，不是「今天完成仍为 0」。

`StreakRules.compute` 的实际语义：`else if (day != today) run = 0`，今天完成会 `run++`。即**「昨天漏、今天补上」应当是 current=1**（与 `REQUIREMENTS` 93「只把过去到期的算失败」一致）。按那句话去「修」代码会把语义改错。建议补 `aCompletedTodayAfterAMissedYesterdayRestartsAtOne`（current=1/longest=1）钉住——全仓目前没有这个用例。

### F-新4（非阻断）`dailyLogs` 的归属仍无覆盖

`everyHabitCountsItsOwnDays` 只断言 `currentStreak`，而两个习惯**今天都完成了**，所以即使 `dailyLogs`/status 取错行，两行都是「已完成」，测试分辨不出。把 Fresh 今天改成未完成并断言 `isCompleted` 一真一假即可（1 行断言）。

### F-新5（非阻断）`GetStreakUseCase` 改成无依赖 `object` 后更像规则而非用例

现在它是「`StreakRules.compute` + 一段钳位」的薄包装，与 `StreakRules` 的边界变模糊。放 `domain/usecase` 不算错（`ARCHITECTURE` 只要求规则不散落到 UI/DAO）；若按 F-新1 选项 (a) 删掉钳位，它就只剩一次委托，届时应考虑并入 `StreakRules`。后续清理，不阻断。

### 第 1 轮非阻断项的处置复核

| 第 1 轮项 | 本轮处置 | 复核 |
|---|---|---|
| F4 行读取未减、窗口放宽 | 立 `TASK-023`（`NOT_STARTED`，P1：统计页 + 窗口收敛/ SQL 聚合，验收写「数值与今日页一致」） | ✅ 具体、有验收条件，不是「已备案」式空声称 |
| F5 空列表守卫不可达 | 保留守卫 + §8 记录不可达 | ✅ 合理（Room 对 `IN ()` 会报错，守卫仍必要）；U5 仍全绿，已如实记为不可达 |
| F6 `invoke` 死代码 | 整类改 `object`，只留 `fromLoaded` | ✅ 代码侧解决；钳位那半见 F-新1 |
| F7 主线程逐日推进 | 记入 §8 风险，注明未在真机测量 | ⚠️ 接受（登记为未测量，没伪装成已验证） |
| F8 精确断言 vs「上界」 | §8 改写为「精确值是 Fake 自身口径，真实路径还有未计数读取（约 8 SELECT + 1 写）」 | ✅ 自述与实现不再矛盾 |

## 5. 文档同步

**裁决：差一行。**

| 文件 | 状态 |
|---|---|
| `README.md:15` / `:62` | `90 项单测` ✅ / 命令注释 `27/27` ✅（第 1 轮是 21/21） |
| `PROJECT_STATUS.md:8/12/13` | `TASK-022（PARTIAL…待独立评审）` ✅ / `27/27` ✅ / `90 项` ✅ |
| `LAST_HANDOFF.md:9` | 分支 `feat/today-queries`、`main @ bfb3150` ✅（第 1 轮写的是 main @ f959f02） |
| `LAST_HANDOFF.md:29-30` | `OK (90 tests)` ✅；不再引用「84 + DAO 27」这个从未存在的组合 ✅ |
| `LAST_HANDOFF.md:42` | `N+1 已改为整表 4 次查询` + 行数未减的保留意见 ✅（第 1 轮是「N+1 未修」） |
| `LAST_HANDOFF.md:54-55` | Next Safe Action = 按第 1 轮三条阻断修复后复评 ✅（第 1 轮写 `NOT_STARTED`） |
| `TASK-022.md` | §8 记录 F4/F5/F6/F7/F8 ✅；§10 新增「90 tests」「U2 → COMPILE FAILED」「U4 → FAILED 1 项」✅ |
| `TEST_REPORT.md:9` | Latest Run `90 项 + DAO 27 项` ✅ |
| **`TEST_REPORT.md:86`** | **❌ 同一句 27 与 21 并存（F-新2）** |
| `TASK-022.md:74-75` | `Reviewer record：待创建 / 结论：PENDING` —— 第 1 轮记录已入库、本轮记录已覆盖，建议合并前改成「第 1 轮 REJECT / 第 2 轮 结论」。属收尾动作，不单列阻断 |

## 6. 结论

- 结论：`REJECT`

**先说清楚：第 1 轮两条技术阻断（F1、F2）我逐条复现，确认已真正修好**——U2/U1 现在是编译错误而不是性能事故，U4 现在会红且只红 `everyHabitCountsItsOwnDays`。**代码层面我不再阻塞合并**。剩下两条不是架构问题，各一行字，但都属于「记录说了、实际没做到」，按本仓既定标准（两次按文档/变异纪律 REJECT）不能放行：

1. **删掉 `TEST_REPORT.md:86` 句尾的「（`verify-dao.mjs`，21 项）」**或整句重写为 27 项（F-新2）。这是任务书点名要求一致的文件，同一句里不能有两个数字。
2. **把「3 项钳位测试」改成真话**（F-新1）：要么删掉 `fromLoaded` 里那行冗余钳位（相应改测试名/注释），要么在 §8/§10/`LAST_HANDOFF.md:29` 注明该行是冗余防御、删掉不会红。现状是「新测试永远绿 + 记录声称覆盖了钳位」，正是本仓 Lessons 点名的假证据形态。

建议（不阻断，可与上面两条一起提交）：补 `aCompletedTodayAfterAMissedYesterdayRestartsAtOne`（F-新3，别按「今天完成也是 0」改代码）；给 `everyHabitCountsItsOwnDays` 加一行 `isCompleted` 断言顺带覆盖 `dailyLogs` 归属（F-新4）。

复评时我只核对这几行字 + U6 是否能变红（或记录已改成「冗余防御」）；代码不必再动。
