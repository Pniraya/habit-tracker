# LAST_HANDOFF.md

> 每次 AI 会话结束前更新。记录事实，不写愿望或推测。

## Snapshot
- 日期时间（含时区）：`2026-11-05`
- 当前阶段：`Gate 2` 收尾 / `Gate 3` 起点（Domain 规则与 Repository 已建）
- 当前任务：`TASK-005`（`PARTIAL`）；`TASK-003`、`TASK-004` 同为 `PARTIAL`
- 当前 Git 分支：`main`（本地，无远程）
- 最近 commit：以 `git log -1 --oneline` 为准；基线检查点 `d0f470a`

## Completed This Session
- `ADR-011` 转 `ACCEPTED`（用户指示“你帮我做”授权），`DATABASE.md` §4/§12 同步为“CHECK 以等价触发器实现”
- Domain 层：`HabitRules`、`LogStatusRules`、`ScheduleRules`（纯 Kotlin，无 Android 依赖）
- Repository 层：`HabitRepository` 接口 + `RoomHabitRepository`（`withTransaction`）+ `HabitScheduleWriter`（唯一写计划版本者）+ `HabitMappers`
- 测试可执行化：`tools/run-unit-tests.ps1` 绕过沙箱禁止的 Gradle 测试 worker，直接 `java -cp` 跑 `JUnitCore`
- 27 项单测全通过（`LogStatusRulesTest` 11、`ScheduleRulesTest` 13、`AppRoutesTest` 3）
- 导航路由常量抽到 `AppRoutes`，使导航契约可在纯 JVM 测试

## Files Changed
- 新增：`app/src/main/java/com/habittracker/domain/**`（model、calculation、repository）、`app/src/main/java/com/habittracker/data/repository/**`、`app/src/main/java/com/habittracker/data/mapper/HabitMappers.kt`、`app/src/main/java/com/habittracker/navigation/AppRoutes.kt`、`app/src/test/java/com/habittracker/domain/calculation/*`、`app/src/test/java/com/habittracker/navigation/AppRoutesTest.kt`、`tools/run-unit-tests.ps1`、`project/tasks/TASK-005.md`
- 修改：`DATABASE.md`（§4 说明、§12 §措辞，受 `ADR-011` 保护）、`ADR-011`（状态）、`app/build.gradle.kts`、`project/PROJECT_STATUS.md`、`project/reports/TEST_REPORT.md`、`project/handoffs/LAST_HANDOFF.md`
- 删除：`app/src/test/java/com/habittracker/navigation/AppDestinationTest.kt`（改为 `AppRoutesTest`）

## Verification Actually Run
| 命令 | 结果 | 备注 |
|---|---|---|
| `tools/run-unit-tests.ps1` | **OK (27 tests)** | 直接 JUnit runner |
| `gradlew assembleDebug` | 见提交前结果 | 重构后重新打包 |
| `node tools/verify-schema.mjs` | 40/40 PASS | 上一轮结果，schema 未变 |
| `gradlew test` | FAILED（环境） | 测试 worker 管道被沙箱阻断 |
| `adb devices` | 空 | 无设备 |

## Incomplete Work
- Room 事务/DAO 集成测试（需 Android 运行时或 Robolectric）
- `UseCase` 层、`DataStore`、`ReminderScheduler`
- `TASK-003` 真机导航验证；`TASK-004` 设备端持久化验证
- Migration v1→v2 与迁移测试

## Known Issues / Risks
- `recordDailyValue` 以 `habit.targetValue` 作快照，不支持当天单独覆盖目标
- 归档与写入并发依赖事务串行化，未压测
- 无设备、无 WHPX：真机验证短期无法自证
- 依赖走腾讯镜像；SDK 许可文件由 AI 写入，需用户复核

## Next Safe Action
1. 写 `UseCase`（`RecordHabitProgressUseCase`、`CreateHabitUseCase` 等）并接 ViewModel。
2. 补 Room 集成测试（Robolectric）或接真机跑一次完整流程。
3. 之后进 Gate 3：连续天数与完成率计算（纯函数 + 单测）。

## Recovery Notes
- 先核对：`git log --oneline -3`、`git status --short`、`tools/check-governance.ps1`、`tools/run-unit-tests.ps1`、`adb devices`
- 不要重复：不要重装 SDK；改基线必须同 commit 带 ADR；不要把 `temp.md` 入库；不要 `fallbackToDestructiveMigration`
