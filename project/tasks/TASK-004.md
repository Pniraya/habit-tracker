# TASK-004: Gate 2 —— Room 数据层骨架

- 状态：`PARTIAL`（schema 与约束已用真实 SQLite 验证；DAO/Repository 运行时未验证）
- 阶段：`Gate 2`
- 优先级：`P0`
- 依赖：`TASK-003`（骨架，`PARTIAL`，不阻塞本任务）
- 创建日期：`2026-11-05`
- 完成日期：`TBD`

## 1. Objective
按 `DATABASE.md` §4/§5 建立 Room schema v1：四张表、外键、索引、TypeConverter、DAO 原语、schema export 与约束兜底。本任务不含 Repository、DataStore、Domain 规则。

## 2. Context
`DATABASE.md` §2/§4/§5/§10/§12；`ARCHITECTURE.md` §5 分层；`ROADMAP.md` Gate 2。

## 3. 实施结果
- 四个 Entity：`habits`、`habit_schedules`、`habit_logs`、`habit_reminders`；列名、可空性、默认值、索引名、外键（`ON DELETE CASCADE`）与 `DATABASE.md` §4 逐项对齐。
- `Converters`：`LocalDate` ↔ ISO `yyyy-MM-dd`、`LocalTime` ↔ `HH:mm:ss`、三个枚举按名字存储（不用 ordinal）。
- 四个 DAO 只暴露原语：`HabitLogDao` **没有** `setStatus`/`setCompletedAt`（ADR-006），写日志只能整条 upsert。
- `HabitDatabase`：`version = 1`、`exportSchema = true`、无 `fallbackToDestructiveMigration()`；schema JSON 已纳入版本控制（`app/schemas/.../1.json`，12 187 bytes）。
- **CHECK 约束的实现方式**：Room 的 `@Entity` 无法生成 CHECK，因此 `DATABASE.md` §4 的约束以 `BEFORE INSERT/UPDATE` 触发器落地（`DatabaseConstraints`，8 个），并在 `onCreate` 与 `onOpen` 幂等安装。这使“直接 UPDATE 违反不变量会被拒绝”（§12 最后几条）仍然成立。见 `ADR-011`（`PROPOSED`）。
- 新增 `tools/verify-schema.mjs`：用 `node:sqlite` 加载 Room 导出的 DDL + 触发器 DDL，在真实 SQLite 上跑 40 项断言。

## 4. 未完成 / 未验证
- **DAO 运行时未验证**：`gradlew test` 在本会话仍因沙箱禁止 Gradle 测试 worker 的管道而失败（`ClassNotFoundException: worker.org.gradle.process.internal.worker.GradleWorkerMain`）。没有 Robolectric/仪器测试证据。
- 未实现 `Repository`、`HabitScheduleWriter`、`upsertDailyLog` 的 Domain 计算、DataStore；这些属于 Gate 2 后半与 Gate 3。
- 没有 Migration：当前 `version = 1`，只有“初始创建”，尚无 v1→v2 的真实迁移与迁移测试。
- 未在设备上验证“重启后数据持久化”。

## 5. 与计划的偏差
- 计划中的“Migration 基础设施”只完成了一部分：配置了 schema export 与无破坏性回退，缺少 `MigrationTestHelper` 测试（依赖测试 worker）。
- 触发器方案需要用户批准（`ADR-011`）；若被否决，需要改用 `createFromAsset` 预建库或接受约束缺失。

## 6. Acceptance Criteria
- [x] 四张表按 `DATABASE.md` §4 建立（列/可空/默认值/索引名/外键一致）
- [x] `UNIQUE (habit_id, log_date)` 与 `UNIQUE (habit_id, reminder_time)` 生效
- [x] 外键 `ON DELETE CASCADE` 生效
- [x] `status`/`completed_at` 一致性、`DAILY=127`/`WEEKDAYS=31`、`weekday_mask` 范围、`enabled ∈ {0,1}` 被拒绝
- [x] schema JSON 导出并纳入版本控制
- [ ] DAO/Repository 自动化测试通过 —— **未验证：测试 worker 无法在沙箱启动**
- [ ] 重启后数据持久化 —— **未验证：无设备**

## 7. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `gradlew.bat assembleDebug`（含 KSP/Room） | SUCCESSFUL（2m 32s） | `app/schemas/.../1.json` 生成 |
| `node tools/verify-schema.mjs` | **40/40 PASS** | 28 项行为断言 + 12 项 schema parity |
| 约束拒绝示例 | `habits: invalid row`、`habit_logs: invalid log`、`UNIQUE constraint failed`、`FOREIGN KEY constraint failed` | 触发器与索引均生效 |
| `gradlew.bat test` | FAILED（环境） | Gradle 测试 worker 管道被沙箱阻断，见 `TASK-003` §4 |

## 8. Risks
- 触发器是“等价实现”，与 `DATABASE.md` 字面 `CHECK` 不同；未获批准前不能当作基线已达成。
- 触发器依赖 `NEW.habit_id` 反查 `habits.tracking_type`：删除习惯时级联删除日志，不会触发误判；但导入时若先写日志后写习惯，插入顺序必须先习惯后日志。
- 触发器在 `onOpen` 每次重装：开销小，但若历史上手工删除了触发器，会在下次打开时恢复。
- 索引 `index_habit_logs_habit_date` 与唯一索引列完全相同，属 `DATABASE.md` 原样保留的冗余，未擅自删除。

## 9. Handoff
- 下一步（Gate 2 后半）：`Repository` + `HabitScheduleWriter`（同事务关闭旧版本）、`upsertDailyLog` 的 Domain 计算、外键与唯一约束的 DAO 集成测试。
- 需要用户决定：批准或否决 `ADR-011`。
