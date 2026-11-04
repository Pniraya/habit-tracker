# ADR-007: 计划版本与频率约束在 Repository 事务内校验

- 状态：`ACCEPTED`（2026-11-05 用户批准）
- 日期：`2026-11-05`
- 决策人：用户（2026-11-05 明确批准 ADR-005~010 全部内容）
- 相关任务：`TASK-001`
- 关闭问题：`BASELINE_REVIEW` A4、审查新增的 mask 与 frequency 不一致问题

## Context
同一习惯的计划版本区间不能重叠、最多一条开区间版本，这类跨行规则 SQLite CHECK 无法表达；`frequency_type` 与 `weekday_mask` 的对应关系（DAILY=127、WEEKDAYS=31、CUSTOM≠0）目前只写在文字里，SQL 只限制 mask 在 0–127，因此 `DAILY` 配 `mask=0` 也能写进去。

## Decision
1. 跨行规则（区间不重叠、单条开区间）由 Repository `HabitScheduleWriter` 在**同一事务内**校验：查该习惯全部版本 → 计算新区间 → 校验 → 关闭旧版本 → 插入新版本。SQLite 单写事务已提供串行化，不需要额外锁。
2. 可表达规则下沉到 schema：
   - `CHECK (weekday_mask BETWEEN 1 AND 127)`
   - `CHECK ((frequency_type = 'DAILY' AND weekday_mask = 127) OR (frequency_type = 'WEEKDAYS' AND weekday_mask = 31) OR (frequency_type = 'CUSTOM'))`
3. CUSTOM 的 `weekday_mask != 0` 由 `BETWEEN 1 AND 127` 覆盖；mask 与用户所选星期的映射由 Domain 计算，UI 不得自行拼 mask。
4. 读取侧：同一日期出现多个有效版本时视为数据完整性错误，抛出明确异常，不静默挑一条。

## Alternatives Considered
- 只靠 UI 校验：已被明确否定。
- 用触发器强制区间不重叠：SQLite 可写，但逻辑分散在 SQL 里，难测试、难迁移，且与 Room schema 管理冲突。

## Consequences
- 正面：非法频率/掩码组合在写库时即失败；跨行规则有唯一实现位置。
- 成本：所有计划写入必须经过 Writer，DAO 不直接插入。
- 迁移：schema v1 直接带 CHECK。
- 影响文件：`DATABASE.md` §4.2 SQL 与规则段、§6。

## Implementation and Verification
- 批准后改动：`DATABASE.md` §4.2 替换 mask CHECK 并补 frequency CHECK；规则段写明校验位置。
- 必须增加的测试：周一至周五=31、周一三五=21、CUSTOM≠0、DAILY+mask=21 被拒绝、区间重叠被拒绝、同日切换、未来生效、两条开区间被拒绝。

## Supersedes
无。
