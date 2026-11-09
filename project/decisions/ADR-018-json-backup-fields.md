# ADR-018: JSON 备份的字段映射

- 状态：`ACCEPTED`
- 日期：`2026-11-06`
- 相关任务：`TASK-030`
- 依据：`DATABASE.md` §11（备份格式）、`ADR-009`（时间戳语义）

## 背景

`DATABASE.md` §11 固定了备份的顶层结构（`format`/`schemaVersion`/`exportedAt`/四张表）、
日期与时间戳格式，但没有逐字段列出四张表的键名。实现必须先定下键名，否则同一个
`schemaVersion = 1` 会出现两种互不兼容的文件。

## 决定

1. **字段名等于数据库列名**（snake_case），不另造 camelCase 名称：
   `id`、`name`、`icon`、`color`、`tracking_type`、`target_value`、`unit`、`created_at`、
   `updated_at`、`archived_at`、`created_on`、`archived_on`；
   `habit_id`、`frequency_type`、`weekday_mask`、`effective_from`、`effective_until`；
   `log_date`、`actual_value`、`target_snapshot`、`status`、`completed_at`、`duration_seconds`；
   `reminder_time`、`enabled`。
   理由：导入时不需要第二张名字对照表，schema 与文件一眼对得上；`DATABASE.md` §11 只约束顶层与
   值格式，列名本来就是该格式的一部分。
2. **枚举写名字**（`BOOLEAN`/`NUMERIC`、`DAILY`/`WEEKDAYS`/`CUSTOM`、`IN_PROGRESS`/`COMPLETED`），
   与 `DATABASE.md` §5「按名字持久化」一致；不写序号。
3. **布尔写 JSON `true`/`false`**（数据库内仍是 0/1），日期 `yyyy-MM-dd`，本地时间 `HH:mm:ss`，
   时间戳为 epoch 毫秒 JSON number（`ADR-009`）。
4. **空表写 `[]`**，可空字段写 `null` 而不是省略键，导入端因此不必区分「缺失」与「为空」。
5. 顶层 `schemaVersion` 保持 `1`；将来字段变化时递增并另写 JSON 迁移器（`DATABASE.md` §11 已要求）。

## 影响

- 新增 `data/backup/`（编码器、导出器、纯数据模型）与 6 项 JVM 测试；不改 Room schema。
- 导入（`030b`）实现时按本 ADR 读取；字段名若变化必须走 `schemaVersion` 升级。

## 未决定

- 导入的冲突策略（`DATABASE.md` §11 的合并/覆盖）留到 `TASK-030b`。
- 自定义图标图片文件不在 JSON 内（只有 `custom:<file>` 字符串），导入后可能回退嫩芽；记录为已知缺口。
