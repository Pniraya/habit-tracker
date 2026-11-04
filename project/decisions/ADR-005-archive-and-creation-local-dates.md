# ADR-005: 归档日期与创建日期使用本地日期列

- 状态：`PROPOSED`（等待批准；批准后改 `ACCEPTED`）
- 日期：`2026-11-05`
- 决策人：用户（待批准）
- 相关任务：`TASK-001`
- 关闭问题：`BASELINE_REVIEW` A1、审查新增的 `created_at` 时区问题、`DATABASE.md` §8.3 自相矛盾

## Context
`habits.created_at`、`habits.archived_at` 是 epoch milliseconds，而连续天数与统计按本地日期计算：创建日是连续起点，归档日是统计有效期终点。若在读取时从 Instant 现算 LocalDate，用户改时区后同一个习惯的“创建日/归档日”会漂移，归档日冻结的统计也会跟着漂移。`DATABASE.md` §8.3 同一段里还并存两种归档日口径。

## Decision
1. `habits` 增加 `created_on TEXT NOT NULL`、`archived_on TEXT`（ISO `yyyy-MM-dd`，本地日期）。
2. 写入时机：创建/归档时用**当时的设备 ZoneId**把当前 Instant 转成本地日期写入；恢复时把 `archived_on` 与 `archived_at` 都置 NULL（V1 不保留归档历史）。
3. 统计、连续天数、有效期判断**只读** `created_on` / `archived_on`，永不在读取时从 `*_at` 现算本地日期。
4. `created_at` / `archived_at` 降级为审计时间戳，不参与业务判定。
5. 归档日口径：统计上界 = `min(今天, archived_on)`；归档当日只看“是否为计划日”和“归档前是否已有完成日志”，归档不新建、不修改当日日志。

## Alternatives Considered
- 不新增列，一律按当前 ZoneId 解释 Instant：零 schema 成本，但跨时区后历史日期漂移，归档冻结日可能错一天。
- 只存 Instant 并缓存创建时的 ZoneId：仍需可读的本地日期列，等价但更绕。

## Consequences
- 正面：历史日期与归档冻结不依赖当前时区；统计可直接按 TEXT 日期比较。
- 成本：schema 多两列，归档/恢复多写一个字段。
- 迁移：仓库尚无数据库，直接进入 schema v1，无需 Migration；一旦 Gate 2 建库后才改，就必须写 Migration。
- 影响文件：`DATABASE.md` §2、§4.1、§6、§7、§8.3、§9；`ARCHITECTURE.md` §6。

## Implementation and Verification
- 批准后改动：`DATABASE.md` §4.1 SQL 增列并补字段说明；§8.3 收敛为单一口径；`ARCHITECTURE.md` §6 增加“本地日期在写入时冻结”。
- 必须增加的测试：23:55 归档的跨日场景、归档后改时区重算统计、恢复后 `archived_on IS NULL`、以 `created_on` 为起点的连续天数。
- 验收：统计查询中不出现把 `*_at` 转日期。

## Supersedes
无。
