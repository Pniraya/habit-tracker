# LAST_HANDOFF.md

> 每次 AI 会话结束前更新。记录事实，不写愿望或推测。

## Snapshot
- 日期时间（含时区）：`2026-11-05`
- 当前阶段：`Gate 2`（数据层 schema v1 已建；Gate 1 设备验证仍缺）
- 当前任务：`TASK-004`（`PARTIAL`）；`TASK-003`（`PARTIAL`）；`TASK-001`/`TASK-002` `DONE`
- 当前 Git 分支：`main`（本地，无远程）
- 最近 commit：以 `git log -1 --oneline` 为准；基线检查点 `d0f470a`

## Completed This Session
- Room schema v1：4 Entity（`habits`/`habit_schedules`/`habit_logs`/`habit_reminders`）、4 DAO、TypeConverter、`HabitDatabase`（version 1，无破坏性回退）、schema export 入库
- `DATABASE.md` §4 的 CHECK 以 8 个触发器实现（`DatabaseConstraints`，onCreate + onOpen 幂等安装），并写下 `ADR-011`（`PROPOSED`）
- `tools/verify-schema.mjs`：用 `node:sqlite` 在真实 SQLite 上验证 40 项（28 约束行为 + 12 schema parity），全 PASS
- `assembleDebug` 含 KSP/Room 通过（2m 32s），APK 仍 9.4 MB

## Files Changed
- 新增：`app/src/main/java/com/habittracker/data/local/**`（4 entity、4 dao、`Converters`、`DatabaseConstraints`、`HabitDatabase`）、`app/schemas/com.habittracker.data.local.HabitDatabase/1.json`、`tools/verify-schema.mjs`、`project/tasks/TASK-004.md`、`project/decisions/ADR-011.md`
- 修改：`gradle/libs.versions.toml`（room 2.6.1、ksp 2.0.21-1.0.28）、`build.gradle.kts`、`app/build.gradle.kts`（KSP 插件、`room.schemaLocation`）、`project/PROJECT_STATUS.md`、`project/reports/TEST_REPORT.md`、`project/handoffs/LAST_HANDOFF.md`

## Verification Actually Run
| 命令 | 结果 | 备注 |
|---|---|---|
| `gradlew assembleDebug`（KSP/Room） | SUCCESSFUL 2m 32s | schema JSON 12 187 bytes |
| `node tools/verify-schema.mjs` | **40/40 PASS** | 约束行为 + schema parity |
| `gradlew test` | FAILED（环境） | 测试 worker 管道被沙箱阻断 |
| `adb devices` | 空 | 无设备 |
| `tools/check-governance.ps1` | 见提交结果 | 13 项 |

## Incomplete Work
- `Repository`、`HabitScheduleWriter`（同事务版本切换）、`upsertDailyLog` 的 Domain 计算、DataStore
- Migration v1→v2 与迁移测试（`MigrationTestHelper` 需要测试 worker）
- DAO/Repository 运行时测试、设备端持久化验证
- `TASK-003` 真机导航验证

## Known Issues / Risks
- `ADR-011` 未批准：触发器方案若被否决，需改 `createFromAsset`，且 `DATABASE.md` §4 需相应修订
- 触发器对 `habit_logs` 反查 `habits.tracking_type`：导入顺序必须“习惯在前”
- 无设备、无 WHPX：设备验证短期无法自证
- 依赖走腾讯镜像；SDK 许可文件由 AI 写入，需用户复核

## Next Safe Action
1. 用户裁决 `ADR-011`。
2. 实现 `Repository` 与 `HabitScheduleWriter`，在事务内校验区间不重叠并关闭旧版本。
3. 在非沙箱环境跑 `gradlew test`；接真机跑安装与三页导航。

## Recovery Notes
- 先核对：`git log --oneline -3`、`git status --short`、`tools/check-governance.ps1`、`node tools/verify-schema.mjs`、`adb devices`
- 不要重复：不要重装 SDK；不要改 `DATABASE.md` 除非 ADR 获批；不要把 `temp.md` 入库；不要用 `fallbackToDestructiveMigration`
