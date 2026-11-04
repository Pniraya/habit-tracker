# ADR-009: 备份 JSON 的时间戳统一为 epoch milliseconds

- 状态：`PROPOSED`（等待批准；批准后改 `ACCEPTED`）
- 日期：`2026-11-05`
- 决策人：用户（待批准）
- 相关任务：`TASK-001`
- 关闭问题：`BASELINE_REVIEW` B4 的直接冲突

## Context
`ARCHITECTURE.md` §9 的备份示例里 `exportedAt` 是 ISO-8601 UTC 字符串，`DATABASE.md` §11 又推荐“时间戳统一 epoch milliseconds”，两份文档给同一份备份格式两种写法。备份格式一旦发布就不能随意改，必须在 V1 之前定死。

## Decision
1. 备份 JSON 中所有**时间戳**字段（含 `exportedAt`、各类 `*_at`）一律为 epoch milliseconds 的 JSON number。
2. 所有**日期**仍为 ISO `yyyy-MM-dd` 字符串，**本地时间**为 `HH:mm:ss` 字符串，与数据库一致；这是时间点与墙上时间的区别，不混用。
3. `format = "habit-tracker-backup"`、`schemaVersion = 1`，与 Room `version` 各自独立演进。
4. 备份文件不包含 PendingIntent、闹钟状态、通知权限等系统状态。

## Alternatives Considered
- 全部使用 ISO-8601 UTC 字符串：可读性好，但与数据库存储不一致，导入导出要多一层转换，且时区解析更易出错。
- 混用（时间戳用 ISO，其余用数字）：正是当前冲突的来源，直接否决。

## Consequences
- 正面：备份与数据库同构，导入几乎不需要格式转换；比较与排序简单。
- 成本：人工阅读备份文件时可读性差，需要在导出说明里注明单位。
- 影响文件：`ARCHITECTURE.md` §9 示例；`DATABASE.md` §11 措辞（消解“或”）。

## Implementation and Verification
- 批准后改动：`ARCHITECTURE.md` §9 JSON 示例改为 `"exportedAt": 1793750400000`，并注明单位为毫秒；`DATABASE.md` §11 去掉“ISO-8601 UTC 字符串或 epoch milliseconds”的二选一表述。
- 必须增加的测试：导出→导入往返后所有时间戳等值；非法时间戳（字符串）被校验拒绝；`schemaVersion` 不支持时报明确错误。

## Supersedes
无。
