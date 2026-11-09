# ADR-019: 备份导入的策略与校验

- 状态：`ACCEPTED`
- 日期：`2026-11-09`
- 相关任务：`TASK-039`
- 依据：`DATABASE.md` §11（导入步骤）、`ADR-018`（字段映射）

## 背景

`DATABASE.md` §11 规定 V1 提供「清空后恢复」与「合并导入」两种操作，并给出校验顺序：
限大小 → 校验结构/枚举/日期/引用/范围 → 内存中全量校验 → 单事务写入 → 失败回滚。
但两种操作同时实现会让 V1 的界面与测试面翻倍，而「清空后恢复」已经覆盖真实场景
（换机、重装、误删恢复）。

## 决定

1. **V1 只做「清空后恢复」**：先删除四张表的全部数据，再按文件内容写回；不做合并导入。
   合并导入（按 ID 去重、冲突让用户选）推迟到后续任务，届时新增 `ADR` 与新的确认流程。
2. **二次确认**：选择文件并校验通过后，先弹出摘要（N 个习惯 / M 条日志 / 覆盖当前全部数据），
   用户点「覆盖并恢复」才执行；取消则什么都不做。校验失败只显示错误，不进入确认。
3. **先全量校验、后写库**：解析与校验在内存中完成，任何一条不合法就整体失败，
   错误提示包含字段路径（如 `logs[2].log_date`），不产生部分写入。
4. **保留原始 ID**：导入按文件里的 `id` 写回，习惯、计划、日志、提醒之间的引用不变；
   导入后 SQLite 自增序列自然从最大 ID 继续。
5. **单事务**：删除 + 写入在 `withTransaction` 内完成，中途异常回滚到导入前状态。
6. **大小上限 8 MB**：超过直接拒绝，避免把超大文件读进内存。
7. **提醒**：V1 无提醒调度，导入只写 `habit_reminders` 行，不重建闹钟；`F-012` 实现时再补「事务成功后重建」。
8. **图标图片不在备份里**：`custom:<file>` 字符串按原样写回，文件缺失时界面回退嫩芽（`ADR-018`）。

## 校验项（与 `DATABASE.md` §11 对应）

- 顶层：`format = habit-tracker-backup`、`schemaVersion = 1`、`exportedAt` 为 number。
- 行内类型：字符串/数字/布尔/可空字段的类型必须正确；日期 `yyyy-MM-dd`、时间 `HH:mm:ss`。
- 枚举：`BOOLEAN|NUMERIC`、`DAILY|WEEKDAYS|CUSTOM`、`IN_PROGRESS|COMPLETED`。
- 引用：计划/日志/提醒的 `habit_id` 必须存在于 `habits`。
- 唯一性：`habits.id`、`habit_schedules.id`、`habit_logs.id`、`habit_reminders.id` 不重复；
  `(habit_id, log_date)` 不重复；`(habit_id, reminder_time)` 不重复。
- 范围：`target_value > 0`、`actual_value >= 0`、`duration_seconds` 为空或 >= 0；
  `weekday_mask` 在 1..127；`DAILY` 必须 127、`WEEKDAYS` 必须 31（ADR-007）。
- 一致性：`status = COMPLETED` 必须有 `completed_at`，`IN_PROGRESS` 必须没有。

## 影响

- 新增 `data/backup/BackupParser.kt`（纯 Kotlin，含最小 JSON 解析器）与 `BackupRestorer.kt`（Android）。
- 四个 DAO 增加 `deleteAll()`；`verify-dao.mjs` 补 4 个场景。
- 「我的」页新增「从备份恢复」入口：`OpenDocument("application/json")` → 校验 → 摘要确认 → 写库。

## 未决定

- 合并导入的冲突交互（`DATABASE.md` §11 第 5 条）留待后续。
- 导入后是否重建提醒（等 `F-012`）。
## 修订（2026-11-09）

- 用户决定：**合并导入不再实现**，V1 只保留覆盖式恢复。`DATABASE.md` §11 的「合并导入」条款因此失效；按 `ADR-004` 基线文件不由 AI 直接改写，以本修订为准。
- 影响：`TASK-039` 的已知缺口「合并导入未做」不再是缺口，改为「已决定不做」。
- 依据：用户在会话中明确指示「合并导入不需要了」。
