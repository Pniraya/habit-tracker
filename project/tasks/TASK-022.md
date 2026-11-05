# TASK-022: Today 列表的查询批量化（N+1 与全量重算）

- 状态：`PARTIAL`
- 阶段：`Gate 3`
- 优先级：`P2`
- 依赖：`TASK-021`（连续天数引入第三个每习惯查询）
- 创建日期：`2026-11-05`
- 完成日期：待评审通过后

## 1. Objective
`TodayPresenter.describe()` 每个习惯要 4 次查询（活跃计划、当日日志、全部计划版本、区间日志），`snapshot()` 在每次 Flow 发射与每次 `record()` 后全量重跑；20 个习惯点按一次约 160 次查询。把列表加载改成常数次查询。

## 2. Context
`TASK-021` 第 2 轮评审指出：`record()` 里已显式重算一次，Flow 发射又重算一次，等于每次写入跑两遍全表。`StreakRules` 那 730 次纯计算不是瓶颈，**DB 往返才是**。当时决定不在功能任务里改仓储层，单独立此项。

## 3. Scope
- 仓储层提供批量读取：按习惯 id 集合取活跃计划、取最近的日志区间；避免 `describe()` 内的逐习惯查询。
- `TodayPresenter` 用一次批量加载构建列表；`record()` 后只做必要的一次刷新（去掉重复重算）。

## 4. Out of Scope
- 不改 schema；不改 `StreakRules` 的算法。
- 不引入 Flow 合并的数据库视图（如需再单独立项）。
- 不把 `observeActiveHabits` 换成 habit+log 的联表流；那会改变刷新语义，另开任务。

## 5. Expected Files
- 修改：`domain/repository/HabitRepository.kt`、`data/repository/RoomHabitRepository.kt`、`data/local/dao/HabitScheduleDao.kt`、`data/local/dao/HabitLogDao.kt`、`feature/today/TodayPresenter.kt`、`feature/today/TodayViewModel.kt`、`domain/usecase/GetStreakUseCase.kt`、`FakeHabitRepository.kt`、`TodayPresenterTest.kt`

## 6. Acceptance Criteria
- [x] 构建 20 个习惯的列表时，端到端查询次数为常数（用计数的 fake 证明），不再是每习惯 4 次
- [x] `record()` 后只刷新一次（用计数的 fake 证明）
- [x] 现有测试全绿（86 项），连续天数行为不变

## 7. Tests
- 给 `FakeHabitRepository` 加查询计数器（逐习惯原语与批量方法都计数）；断言 5 个习惯的列表加载固定 4 次查询
- 断言点按一次固定 5 次（写路径 1 次计划检查 + 整表重建 4 次），与习惯数无关
- 全量 `tools/verify-all.ps1`

## 8. Risks and Decisions
- 批量查询可能引入「一次取太多」的过度读取；按习惯 id 集合限制范围。
- 计数型断言与实现细节耦合，只能断言上界，不能断言精确 SQL 条数。
- 接口上的 4 个批量方法给了「逐习惯循环」的默认实现，这样其它实现仍能编译；但默认实现**并不解决 N+1**，所以 Room 与 Fake 都必须覆盖。Fake 的默认实现若被用到，计数会立刻从 4 跳到 20，测试会红——这是故意的绊线。
- `GetStreakUseCase` 拆出 `fromLoaded`：加载与规则分离后，列表可以一次取完再逐习惯算，规则只有一份实现。`TodayPresenter` 因此不再需要该 use case 实例，构造参数删除。

## 9. Implementation Log
- 2026-11-05：由 `TASK-021` 评审第 2 轮的技术债建议创建。
- 2026-11-05：实现。仓储新增 4 个批量读方法（`activeSchedules`/`dailyLogs`/`scheduleVersionsFor`/`logsInRangeFor`），Room 用 `IN (:ids)` 覆盖；`GetStreakUseCase.fromLoaded` 抽出纯规则；`snapshot()` 从「每习惯 4 次查询」变为「整表 4 次」，并把逐习惯的 `describe` 改成同步函数（数据已在外层取好）。
- 2026-11-05：实现期间两次被机器关机打断，工作区各留下一次变异（`associateBy` 逐习惯版），均 `git checkout`/手改还原并复跑 86 项确认后才继续。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/run-unit-tests.ps1` | **OK (86 tests)** | 新增 2 项查询计数断言；既有 84 项全绿 |
| 变异 MA：`snapshot()` 退回逐习惯 `activeSchedule` | **FAILED（2 项）** | `theWholeListLoadsInAConstantNumberOfQueries`、`aTapRefreshesTheListOnce` |
| 变异 MB：`snapshot()` 里多做一次批量读（旧重复刷新） | **FAILED（2 项）** | 同上两条，证明「只刷新一次」被断言住 |
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS**（exit 0） | 治理 22/22、单测 86、DAO 27、构建通过、`diff --check` 干净 |

## 11. Completion Review
- [x] 验收标准满足（3/3）
- [x] 测试实际运行（86 项 + 2 组变异）
- [ ] 独立 Reviewer `APPROVE`

## 12. Handoff
- 之后：`F-010`/`F-011` 统计页（同一套批量加载可复用）。

## 13. Independent Review
- Reviewer record：待创建
- 结论：`PENDING`

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-022
- 状态：`PARTIAL`
- 阶段：Gate 3
- 验收：通过 3 项 / 未通过 0 项
- 最近测试记录：2026-11-05 / **OK (86 tests)**
<!-- close-task:end -->
