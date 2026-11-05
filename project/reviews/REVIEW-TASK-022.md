# REVIEW-TASK-022: 独立评审记录

- 审查任务：`TASK-022`（Today 列表查询批量化，N+1 修复）
- 审查 commit：`b73e03b22d94ad97f5f0155e19ddb1963be01ec0`（`git rev-parse HEAD`，分支 `feat/today-queries`，`main` = `bfb3150`）
- 审查范围：`git diff bfb3150...b73e03b`——15 文件 +346/−48
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现；全程只读核对 + 变异）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`

工作区：开始时 `git status --short` 并非为空，有**两处不是本次提交、也不是我产生的改动**——`M project/tasks/TASK-021.md`（状态行改成「第 2 轮 APPROVE」）与 `?? project/reports/EXTERNAL_REVIEW_BRIEF.md`（18 KB 外部评审简报）。两者都不在 `b73e03b` 内，我未触碰；所有变异只改 5 个源码文件，逐组用 `git checkout -- <path>` 还原，结束时 `git status --short` 与开始时**逐字节一致**（同样只有上列两项）。合并前必须把这两项从合并集里摘出去，别顺手带进 `main`。

## 1. Scope

**裁决：通过。**

- 与 `TASK-022` §3/§5 一致：仓储 4 个批量读（接口 + Room `IN (:ids)` + DAO）、`GetStreakUseCase.fromLoaded`、`TodayPresenter.snapshot()`/`describe()`、`TodayViewModel` 构造参数、`FakeHabitRepository` 计数器、`TodayPresenterTest` 2 项、`tools/verify-dao.mjs` 6 个批量场景。文档同步属 `AGENTS.md` §6 强制项。
- 未碰 schema、未加依赖/权限/Gradle module、未改 `StreakRules` 算法、未把 `observeActiveHabits` 换联表流（§4 Out of Scope 守住）。
- 没有删除或绕过既有测试：`TodayPresenterTest` 只删掉 `streaks =` 构造实参，其余为新增；`verify-dao.mjs` 的 `MIN_CHECKS` 21 → 27，与新增 6 个 `equal(...)` 场景数一致（逐条数过）。
- 业务规则位置正确：计划/连续天数口径仍在 `domain/`；`describe()` 只做装配；DAO 无规则。

## 2. 实际命令与退出码

本机 Windows / JDK 17 / Gradle 8.9：

| 命令 | 退出码 | 输出摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | **0** | `RESULT: VERIFY ALL PASS`；治理 `22 checks, 22 passed, 0 warnings`（13s）、单测 `OK (86 tests)`（29s）、`verify-dao` `checks: 27, failed: 0`、`assembleDebug` `BUILD SUCCESSFUL in 25s`、`git diff --check` `no whitespace errors` |
| `pwsh -NoProfile -File tools/run-unit-tests.ps1`（基线） | **0** | `RESULT: UNIT TESTS PASS`（86 项） |
| `node tools/verify-dao.mjs`（基线） | **0** | `checks: 27, failed: 0` |
| `git rev-parse HEAD` | 0 | `b73e03b22d94ad97f5f0155e19ddb1963be01ec0` |
| `git status --short`（开始前） | 0 | **2 项**（`M project/tasks/TASK-021.md`、`?? project/reports/EXTERNAL_REVIEW_BRIEF.md`），均非本次提交 |
| `git status --short`（结束后） | 0 | 同样 2 项，无源码残留 |

声称的三个数字**全部复现**：86 单测、27 DAO 检查、exit 0。

## 3. 变异表

每组：改一处 → 跑真实 runner → `git checkout -- <path>` → 确认 `git status --short` 回到基线。

| # | 变异 | 结果 | 打红的测试 | 打中的是不是「正确的那条」 |
|---|---|---|---|---|
| U1 | `FakeHabitRepository` 去掉 `activeSchedules` 覆盖，退回接口逐习惯默认实现 | **FAILED**（`Tests run: 86, Failures: 2`） | `theWholeListLoadsInAConstantNumberOfQueries`、`aTapRefreshesTheListOnce` | ✅ 正是两条计数断言。与任务记录的变异 MA 同形、同 2 项，声称可复现 |
| U2 | **`RoomHabitRepository`** 去掉 `logsInRangeFor` 覆盖，退回接口逐习惯默认实现（= 生产代码 N+1 复活） | **PASS（86/86 全绿）** | 无 | ❌ **一条都没红**。生产实现退化成 N+1，测试完全看不见 |
| U3 | `snapshot()` 多做一次批量读（模拟旧的重复全量重算） | **FAILED**（Failures: 2） | 同上两条 | ✅ 与任务记录的变异 MB 一致，「只刷新一次」确被断言住 |
| U4 | `describe()` 把**所有**习惯的日志都喂进去（`logsSince.values.flatten()`，id 归属被打散） | **PASS（86/86 全绿）** | 无 | ❌ 跨习惯串数据无效。多习惯列表的连续天数算错也不会有测试红 |
| U5 | `RoomHabitRepository.logsInRangeFor` 删掉空列表早退守卫 | **PASS（86/86 全绿）** | 无 | ❌ 守卫无覆盖（presenter 在更早就 return，永不触达；`verify-dao` 的 binder 反而**主动对空列表抛错**，这条路径两层都测不到） |
| U6 | `fromLoaded` 删掉 `!it.date.isBefore(firstDay)` 日期钳位 | **PASS（86/86 全绿）** | 无 | ❌ 钳位是纯防御代码，没有测试钉住 |
| D1 | `HabitLogDao.findForDate` 去掉 `AND log_date = :date` | **FAILED**（`checks: 27, failed: 1`） | `findForDate spans habits and keeps only that date (expected 2) -> got 4` | ✅ 任务记录里「删日期过滤即红」的声称成立 |
| D2 | `findAllForHabits` 把 id 列表换成 `IN (SELECT habit_id FROM habits)`（扫全表代替用 id 表） | **FAILED**（1 项） | `findAllForHabits respects the id list (expected [2]) -> got [1,1,2]` | ✅ id 过滤在 SQL 层有守卫 |
| D3 | `findRangeForHabits` 删掉 `log_date BETWEEN :from AND :to` | **FAILED**（1 项） | `findRangeForHabits keeps both boundaries and filters by id` | ✅ 区间边界有守卫 |

**读法**：DAO 层（D1–D3、D2 的 id 过滤）守得住；repository/presenter 层只守住「次数」和「窗口次数」，**守不住「哪一个习惯的数据」和「Room 到底有没有覆盖」**。U2/U4 是本次评审的核心证据。

## 4. Findings（按严重度）

### F1（阻断）接口默认实现 + Room 零覆盖 = 本任务的核心不变量无人守

`HabitRepository` 给 4 个批量方法写了「逐习惯循环」的默认实现；这正是本任务要消灭的 N+1。任务记录 §8 说这是「故意的绊线」——但绊线只装在 `FakeHabitRepository` 上。全仓 **8 个测试类没有任何一个构造 `RoomHabitRepository`**（`app/src/androidTest` 为空；`grep RoomHabitRepository` 在 `app/src/test` 下 0 命中）。证据就是 U2：把 `RoomHabitRepository.logsInRangeFor` 的 `override` 去掉，生产代码立刻退回每习惯一次查询，而 86 项测试**全绿**。

`verify-dao.mjs` 拦不住这个：它用正则从 Kotlin 源码里抠 `@Query` 文本，验的是 **SQL 字符串**，不是「Room 仓储有没有调它」。所以「Room 漏写一个 override」「`activeSchedules` 忘了传 ids」这类错误可以静默上线，而本任务的唯一交付就是「生产环境常数次查询」。

最小修法（二选一）：(a) 4 个方法在接口上改 `abstract`（无默认实现），编译期强制每个实现表态——这才是这类不变量该用的机制；(b) 保留默认值，但加一条断言「`RoomHabitRepository` 的 4 个方法声明类 ≠ `HabitRepository`」（反射或 arch 测试），或直接给 Room 加一条 instrumentation/Robolectric 测试。任务 §8 的理由（「其它实现仍能编译」）代价高于收益：仓库里只有 2 个实现，而默认实现把「忘记覆盖」从编译错误降级成了性能事故。

### F2（阻断）批量读的「按习惯归属」没有测试

U4 把所有习惯的日志混在一起喂给 `describe()`，86 项仍全绿。现有 streak 用例全是单习惯，2 项计数用例里根本没有日志。于是 `logsInRangeFor`/`dailyLogs` 的分组键一旦写错（`associate` 键错、`groupBy` 漏字段），多习惯列表的连续天数会串号，而测试与 `verify-dao`（只验 SQL 行内容）都不会红。D2 证明 SQL 层有 id 过滤，但那是**另一个层**。

最小修法：加一条 2 习惯用例——A 连续 3 天完成、B 只有 1 天，断言两条 row 的 `currentStreak` 分别为 3 和 1。这条同时能锁住 U4 和「`activeSchedules` 拿错 habit 的计划」两类错误。

### F3（阻断）文档同步不成立（检查单 §4 与任务书的明确要求）

四处过期断言，都不是「历史记录保持原样」能解释的，因为它们描述的是**当前状态**：

| 位置 | 现状 | 应为 |
|---|---|---|
| `project/handoffs/LAST_HANDOFF.md:29` | `OK (84 tests)` | 86 |
| `project/handoffs/LAST_HANDOFF.md:30` | 同一行写「单测 84、DAO 27」——这个组合从未存在过（84 那次是 DAO 21） | 单测 86、DAO 27 |
| `project/handoffs/LAST_HANDOFF.md:42` | `**N+1 未修**（TASK-022）…约 160 次` | 已修 |
| `project/handoffs/LAST_HANDOFF.md:55` | 下一步写 `**TASK-022**（P2，NOT_STARTED）` | `PARTIAL`，待评审 |
| `project/handoffs/LAST_HANDOFF.md:9` | 分支 `main @ f959f02`，工作区干净 | `feat/today-queries @ b73e03b`，（且实际有 2 项未提交） |
| `README.md:62` | `node tools/verify-dao.mjs  # 已验证：21/21` | 27/27（同文件第 16 行的表格已写 27/27，自相矛盾） |
| `project/reports/TEST_REPORT.md:85` | Known Test Gaps 仍写「`verify-dao.mjs`，21 项」 | 27 项 |

`TASK-022` §10、`PROJECT_STATUS.md`、`README.md` 表格、`TEST_REPORT.md` 的 Latest Run 四处**是一致的**（86 / 27 / PARTIAL）。治理检查 22/22 也过了——因为第 15 项只比对 `Latest Run` 与 `Runs` 表，不读 handoff 的正文断言。这正是评审要人肉兜住的那部分。

附带：`TEST_REPORT.md` 的两条 TASK-022 记录插在表中倒数第二的位置，最后一行仍是 TASK-018 的真机记录，因此检查单 §4「Latest Run 与 Runs 表最后一行一致」按字面不成立（治理检查对此宽松）。

### F4（设计）批量读没有减少行读取，只是减少了往返；且这次把窗口放宽了

`snapshot()` 取 `firstDay = versions.values.flatten().minOfOrNull { it.effectiveFrom }`（**全局**最早计划版本），然后 `logsInRangeFor(ids, minOf(firstDay, date), date)` 把这个区间的**所有日志行**装进内存，再由 `fromLoaded` 在 Kotlin 里按习惯钳位、按状态过滤。

- 行量级不变：仍是 O(习惯数 × 历史天数)。20 个习惯 × 3 年 ≈ 2 万行/次快照，每次点按都来一遍。`index_habit_logs_habit_date` 有索引，SQL 代价可控，但内存与 GC 是线性的。
- 比改动前**略差一点**：旧代码每个习惯查 `[该习惯首个版本, 今天]`；现在统一用全局最早日期，个别习惯会把「自身首个版本之前」的日志也捞回来（`fromLoaded` 再滤掉）。行为等价（U6 的钳位就是干这个的，但没人测），读取面变宽。
- `StreakRules.compute` 仍是「从首个版本逐日步进到今天」× 每个习惯 × 每次快照。TASK-021 评审已认定纯计算不是瓶颈，这里不改也合理。

**结论：可接受，但不是「修好了」**。它兑现了任务书的目标（DB 往返常数化，160 → 4/5），把行量级与逐日计算留成了明确的后续债。建议单独立项（而不是在本任务里扩 diff）：统计页（F-010/F-011）必然要同一批数据，届时用「近 N 天窗口 + 聚合」一次解决。

### F5（设计）空 id 列表早退：对 Room 是必需的，位置也对，但两层都测不到

Room 把 `IN (:ids)` 展开成占位符，空集合会生成 `IN ()`，SQLite 直接语法错。所以 `if (habitIds.isEmpty()) return emptyMap()` 是**正确且必要**的。三点补充：(1) 守卫只在 `RoomHabitRepository`，DAO 自身没有，未来直接调 DAO 的人会踩；(2) presenter 在更外层就 `if (source.isEmpty()) return` 了，这 4 个守卫在生产路径里不可达（U5 全绿即证）；(3) `verify-dao.mjs` 的新 binder 对空列表**主动抛错**（`empty list bound to :${name} would not match Room`），于是「空列表」这个真实风险在 DAO 校验层被永久排除，谁也不会去测。建议：把守卫下沉到 DAO 调用点或加一条 binder 层用例，至少留一句注释说明「本路径不可由 presenter 触达」。

### F6（设计）去掉 `GetStreakUseCase` 构造参数：规则仍可测，但 `invoke` 成了生产死代码

- 规则可测：`fromLoaded` 是纯函数，presenter 只调它，逻辑与 `invoke` 共用一份，没有第二份实现。✅
- 但 `GetStreakUseCase.invoke` 现在**没有任何生产调用方**（`grep GetStreakUseCase`：只有类自身与 `TodayPresenter.kt:116` 的静态调用）。它靠自己的测试活着，是一条没人走的门。要么删掉 `invoke`（连带 `HabitRepository.logsInRange` 也可议），要么在注释里写明「留给统计页/详情页」，否则下一个人会以为列表用的是它。
- 另一处未被测的路径：`invoke` 的 `from = schedules.minOf` 与新路径的「全局 min + Kotlin 钳位」是两种取窗方式，目前只在 U6 那条无覆盖的钳位上等价。若钳位被当成冗余删掉（U6 证明无人拦），二者才会分叉。

### F7（设计）`describe()` 失去 suspend 边界：主线程上多了一段不可打断的循环

`TodayViewModel.factory` 用 `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)`，所以 `snapshot()` 整个跑在主线程。改动前 `describe()` 是 suspend，每个习惯夹 4 次可让出线程的 DB 调用；改动后 4 次批量读之后是一段**纯同步**的 `source.map { describe(...) }`，中途没有任何挂起点，里面每个习惯都要跑 `StreakRules.compute`（逐日步进）。20 个习惯 × 3 年 ≈ 2 万次迭代 + 2 万个 `LocalDate` 分配，一次性连续占住主线程。

规模不大，但这是本次改动**新增**的阻塞形态（以前被 I/O 挂起切碎），而 JVM 测试用 `Dispatchers.Unconfined`，永远不会暴露它。建议：把 `map` 包进 `withContext(Dispatchers.Default)`，或在任务书里显式记为已接受的风险（真机 20 习惯下跑一次首帧并记录）。

### F8（低）计数断言与 §8 自相矛盾，且「5 次」不是真实 SQL 次数

- `TASK-022` §8 写「计数型断言…只能断言上界，不能断言精确 SQL 条数」，实现却写 `assertEquals(4, readCalls)` / `assertEquals(5, readCalls)`（**精确相等**）。将来任何一次合法预取都会变红，把「防 N+1」变成「防改动」。要么改成 `assertTrue(readCalls <= 4)`，要么把 §8 那句删掉。
- 「点按一次正好 5 次」只在 fake 的计数口径里成立。真实点按路径是 `getHabit`(1) + `findVersionsOn`(1) + 事务内 `habitDao.findById`(1) + `logDao.findByDate`(1) + `upsert`(写) + 4 次批量读 = **8 次 SELECT + 1 次写**。`FakeHabitRepository` 对 `getHabit`/`recordDailyValue` 内部读不计数，所以注释里「1 次写路径计划检查」少算了 3 次读。结论「与习惯数无关」仍然成立，但数字本身是计数口径的产物，注释应按口径写。

## 5. 文档同步与分支

**裁决：不通过（见 F3）。**

- `main` 稳定分支策略：改动留在 `feat/today-queries`，`TASK-022` 为 `PARTIAL`，无需「在册例外」登记。✅
- 单一 commit `b73e03b`，信息 `perf(today): load the list in four queries` 与内容相符。✅
- 受保护基线文件（`AGENTS.md`/`ARCHITECTURE.md`/`DATABASE.md`/`REQUIREMENTS.md`）未被改动，无需 ADR。✅
- 工作区：审查开始就不干净（2 项非本次提交），已在文首记录；我全程未触碰，结束复核一致。

## 6. 结论

- 结论：`REJECT`

阻断项（按修复成本排序，前 3 项都必须先修）：

1. **接口的 4 个批量方法改为抽象（或补一条「Room 必须覆盖」的测试）。** 证据：变异 U2 把 `RoomHabitRepository.logsInRangeFor` 的 override 去掉——生产代码退回 N+1——86 项测试全绿。当前 `verify-dao` 只验 SQL 文本，仓库里没有任何测试构造 `RoomHabitRepository`，本任务的核心不变量因此在生产侧无人看守。
2. **补一条 2 习惯的归属用例**（A 连续 3 天、B 只有 1 天，断言两行各算各的）。证据：变异 U4 让 `describe()` 拿到所有习惯的日志，86 项全绿；现有 streak 用例全是单习惯，`logsInRangeFor`/`dailyLogs` 的分组键写错不会有任何测试变红。
3. **修文档同步（F3 表格 7 行）**：`LAST_HANDOFF.md` 的 84 测试 / `main @ f959f02` / 「N+1 未修」/ 下一步仍写 `NOT_STARTED`；`README.md:62` 的 `21/21`；`TEST_REPORT.md:85` 的「21 项」。合并集里还要摘掉不属于本提交的 `project/tasks/TASK-021.md` 与未跟踪的 `project/reports/EXTERNAL_REVIEW_BRIEF.md`。
4. （建议，不单独阻断，随 #1/#2 一起处理最省事）F7 主线程连续计算：把 `snapshot` 的逐习惯装配放到 `Dispatchers.Default`，或明确记为已接受并给真机数据；F4 行读取与 F6 的 `invoke` 死代码登记为后续任务。

非阻断但值得记录：F4（行量级未变、窗口被放宽）、F5（空列表守卫两层都测不到）、F8（精确相等断言与 §8 自述矛盾、「5 次」是计数口径而非 SQL 次数）。

复评时我会重点看：U2、U4 两条变异是否能红，以及 F3 那 7 行是否真的改了（不改文档就再 REJECT 一次，和 `TASK-009` 第 1 轮同样的理由）。
