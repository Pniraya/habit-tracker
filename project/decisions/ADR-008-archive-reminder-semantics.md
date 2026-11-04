# ADR-008: 归档不改写提醒开关，并明确取消提醒的职责层

- 状态：`PROPOSED`（等待批准；批准后改 `ACCEPTED`）
- 日期：`2026-11-05`
- 决策人：用户（待批准）
- 相关任务：`TASK-001`
- 关闭问题：审查新增的“归档禁用提醒”与“恢复沿用 enabled”互斥；`ARCHITECTURE.md` §5.2 与 §3.2 分层矛盾

## Context
`DATABASE.md` §9 说归档时“禁用提醒并取消所有 PendingIntent”，又说恢复时“沿用各条 reminder 的 enabled 字段”。如果归档把 `enabled` 写成 0，恢复后就无法区分“用户主动关闭”与“因归档而关闭”，用户原来的提醒会被静默清掉。另外 `ARCHITECTURE.md` §5.2 把“取消当天剩余提醒”写进了打卡写入的 UseCase 流程，而 §3.2 规定 Domain 不依赖 Android。

## Decision
1. 归档**不修改** `habit_reminders.enabled`，只做两件事：写 `archived_on`（见 ADR-005）、取消该习惯的全部 PendingIntent。
2. “有效提醒集合”统一定义为：`enabled = 1` 且 `habits.archived_on IS NULL`。Receiver 触发时先按此判定，再决定是否发通知。
3. 恢复时清空 `archived_on`，并按数据库中 `enabled = 1` 的记录重排未来提醒；用户原先关闭的提醒保持关闭。
4. 分层职责：Domain 只回答“是否应当提醒”（纯函数），取消/注册 PendingIntent 由 `ReminderScheduler`（系统接入层）执行。打卡完成后取消当天剩余提醒，由 Presentation 在 UseCase 返回后调用调度器，不在 Domain 内调用。
5. 提醒迟到/重复触发的处理：触发时若判定“不应提醒”（已归档、已停用、非计划日、当日已达标），只重排下一次，不补发通知；同一 `reminderId` + 同一目标日期只允许一次通知（用已发标记的去重表或内存+数据库双写判定，实现时在 TASK 内定）。

## Alternatives Considered
- 归档写 `enabled = 0` 并另加 `disabled_by_archive` 列：能保留差异，但多一列、多一套恢复逻辑，收益不足。
- 归档物理删除提醒：与“保留历史配置”冲突，恢复后用户配置丢失，直接否决。

## Consequences
- 正面：用户提醒配置不会因归档而丢失；分层职责不再自相矛盾。
- 成本：所有提醒路径都必须额外判断 `archived_on`，不能只依赖 `enabled`。
- 影响文件：`DATABASE.md` §9；`ARCHITECTURE.md` §5.2、§8.3、§8.4。

## Implementation and Verification
- 批准后改动：`DATABASE.md` §9 删除“禁用提醒”，改为“不改写 enabled”；`ARCHITECTURE.md` §5.2 第 5 步改为“Presentation 在 UseCase 返回后调用 ReminderScheduler”；§8.3 增加“不应提醒时只重排不补发”。
- 必须增加的测试：归档后不再触发、恢复后原启用提醒重排、原关闭提醒保持关闭、打卡完成取消当天剩余提醒、重复广播只通知一次。

## Supersedes
无。
