# ADR-006: HabitLog 状态由 Domain 单一入口写入，并用数据库 CHECK 兜底

- 状态：`ACCEPTED`（2026-11-05 用户批准）
- 日期：`2026-11-05`
- 决策人：用户（2026-11-05 明确批准 ADR-005~010 全部内容）
- 相关任务：`TASK-001`
- 关闭问题：`BASELINE_REVIEW` A2

## Context
`habit_logs.status` 与 `completed_at` 都能由 `actual_value`、`target_snapshot` 推导，文档要求三者一致，却没有规定唯一写入入口，也没规定数据库层如何兜底。没有兜底时，一次错误的 DAO 调用就能留下 `status=COMPLETED` 但 `actual_value<target` 的坏行，而统计会静默把它计入。

## Decision
1. `status` 保留为持久化列（统计需要按状态过滤）。
2. 唯一写入口：Domain 纯函数计算状态 → Repository 单一方法 `upsertDailyLog(habitId, date, value, targetSnapshot)`；DAO 只暴露 upsert 与查询，不暴露 `setStatus` / `setCompletedAt`。
3. `completed_at` 仅在“非 COMPLETED → COMPLETED”时写入当时 Instant；回退到 IN_PROGRESS 时置 NULL。
4. 数据库兜底 CHECK（SQLite 可表达，且不跨表）：
   - `CHECK ((status = 'COMPLETED') = (actual_value >= target_snapshot))`
   - `CHECK ((status = 'COMPLETED') = (completed_at IS NOT NULL))`
5. 跨表规则（BOOLEAN 只允许 0/1、BOOLEAN 的 `target_snapshot=1`）无法用 CHECK 表达，由 Domain + Repository 强制，并必须有不变量测试。

## Alternatives Considered
- 不持久化 `status`，查询时派生：一致性最好，但统计与列表过滤要全表计算，且索引失效。
- 只靠 Domain 约束、不加 CHECK：当前文档就是这样，已被证明不足。

## Consequences
- 正面：坏数据写不进去；自检与修复有明确 SQL 依据。
- 成本：写入路径被收窄，后续功能必须走同一入口。
- 迁移：schema v1 直接带 CHECK，无 Migration。
- 影响文件：`DATABASE.md` §4.3 SQL 与规则段；`DEVELOPMENT.md` §4.4 可加一句“状态只能由 Repository 单入口写”。

## Implementation and Verification
- 批准后改动：`DATABASE.md` §4.3 增加两条 CHECK，并写明 DAO 不暴露状态写方法。
- 必须增加的测试：BOOLEAN 0/1、NUMERIC 小于/等于/大于目标、达标后回退、超额完成、直接执行非法 UPDATE 被 CHECK 拒绝。
- 验收：不存在绕过 CHECK 的写入路径。

## Supersedes
无。
