# TASK-023: 统计页（F-010/F-011）与日志读取窗口收敛

- 状态：`NOT_STARTED`
- 阶段：`Gate 4`
- 优先级：`P1`
- 依赖：`TASK-022`（批量读）、`TASK-021`（连续天数规则）
- 创建日期：`2026-11-05`
- 完成日期：`TBD`

## 1. Objective
实现统计页：每个习惯的完成率、当前/最长连续、热力图（近 N 天）。同时收敛日志读取窗口——`TASK-022` 的批量读仍取「全局最早计划版本 → 今天」的全部日志行（20 习惯 × 3 年 ≈ 2 万行/次），统计页会把同一模式放大，必须一并处理。

## 2. Context
`TASK-022` 第 1 轮评审的 F4：批量读减少了往返，没有减少读取行数，而且窗口从「每习惯首个版本」放宽到「全局最早版本」。评审建议单独立项，与统计页一起做。

## 3. Scope
- 统计页数据来源：按习惯、按区间聚合，避免把全部原始行读进内存再算。
- 日志窗口收敛：只取计算所需的最小回溯窗口，或改由 SQL 聚合（`COUNT`/`SUM`/按日分组）。
- 热力图仅展示近 N 天（N 由需求决定），不加载全历史。

## 4. Out of Scope
- 不改 schema；不改 `StreakRules` 语义。
- 不做导出/备份（`F-014`）。

## 5. Expected Files
- 新增：`feature/statistics/*`、相应 DAO 聚合查询、presenter 测试
- 修改：`HabitRepository` 与其实现、`FakeHabitRepository`、导航与占位页

## 6. Acceptance Criteria
- [ ] 统计页展示完成率与连续天数，数值与今日页一致
- [ ] 20 个习惯的统计加载有可断言的行数/查询数上界，不再是「全历史全行」
- [ ] 现有测试全绿，连续天数行为不变

## 7. Tests
- presenter/规则层单测；计数型 fake 断言加载规模
- `pwsh -NoProfile -File tools/verify-all.ps1`

## 8. Risks and Decisions
- SQL 聚合与规则纯函数之间存在"两处计算"的风险；聚合结果必须由测试钉住与规则一致。
- 热力图的时间窗口若与"连续天数"窗口不同，会出现同一屏两套口径，需在文案上避免误导。

## 9. Implementation Log
- 尚未开始（由 `TASK-022` 评审 F4 创建；开工后在此记录）。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | 未运行（任务未开始） | 开工后补真实结果 |

## 11. Completion Review
- [ ] 验收标准满足
- [ ] 测试实际运行
- [ ] 独立 Reviewer `APPROVE`

## 12. Handoff
- 之后：`F-012`/`F-013` 提醒。

## 13. Independent Review
- Reviewer record：待创建
- 结论：`PENDING`
