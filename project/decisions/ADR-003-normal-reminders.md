# ADR-003: V1 提醒采用 Normal 非精确调度

- 状态：`ACCEPTED`
- 日期：`2026-11-04`
- 决策人：用户明确选择 Normal 提醒精度
- 相关任务：治理初始化

## Context
提醒需要多次每日触发，但产品优先考虑省电，不要求精确到分钟。

## Decision
使用 AlarmManager 非精确调度；不调用 `setExact()` 或 `setExactAndAllowWhileIdle()`，不申请 `SCHEDULE_EXACT_ALARM` 或 `USE_EXACT_ALARM`。Android 13+ 请求 `POST_NOTIFICATIONS`。

## Alternatives Considered
- 精确闹钟：需要额外权限/资格与用户预期管理，不符合已选 Normal。
- 仅 WorkManager：不适合作为每日指定时刻提醒的等价替代。

## Consequences
- 系统省电策略可能造成延迟；产品界面和测试报告不得承诺准点。
- 需测试重启、时区/时间变化、通知权限拒绝、强制停止等系统边界。
