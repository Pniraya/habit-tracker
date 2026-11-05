# TASK-022: Today 列表的查询批量化（N+1 与全量重算）

- 状态：`NOT_STARTED`
- 阶段：`Gate 3`
- 优先级：`P2`
- 依赖：`TASK-021`（连续天数引入第三个每习惯查询）
- 创建日期：`2026-11-05`
- 完成日期：`TBD`

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

## 5. Expected Files
- 修改：`domain/repository/HabitRepository.kt`、`data/repository/RoomHabitRepository.kt`、`data/local/dao/HabitDao.kt`（或 `HabitLogDao`）、`feature/today/TodayPresenter.kt`、`FakeHabitRepository.kt`、`TodayPresenterTest.kt`

## 6. Acceptance Criteria
- [ ] 构建 20 个习惯的列表时，端到端查询次数为常数（用计数的 fake 证明），不再是每习惯 4 次
- [ ] `record()` 后只刷新一次（用计数的 fake 证明）
- [ ] 现有 83 项测试全绿，连续天数行为不变

## 7. Tests
- 给 `FakeHabitRepository` 加查询计数器；断言点击一次后的查询次数上界
- 全量 `tools/verify-all.ps1`

## 8. Risks and Decisions
- 批量查询可能引入「一次取太多」的过度读取；按习惯 id 集合限制范围。
- 计数型断言与实现细节耦合，只能断言上界，不能断言精确 SQL 条数。

## 9. Implementation Log
- 尚未开始（由 `TASK-021` 评审第 2 轮的技术债建议创建；开工后在此记录）。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | 未运行（任务未开始） | 开工后补真实结果 |

## 11. Completion Review
- [ ] 验收标准满足
- [ ] 测试实际运行
- [ ] 独立 Reviewer `APPROVE`

## 12. Handoff
- 之后：`F-010`/`F-011` 统计页（同一套批量加载可复用）。

## 13. Independent Review
- Reviewer record：待创建
- 结论：`PENDING`

