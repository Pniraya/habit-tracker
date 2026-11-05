# TASK-005: Repository 与计划版本写入器

- 状态：`PARTIAL`（纯规则 27 项单测通过；Room 事务集成与设备验证未做）
- 阶段：`Gate 2` / `Gate 3` 交界
- 优先级：`P0`
- 依赖：`TASK-004`（schema v1）
- 创建日期：`2026-11-05`
- 完成日期：`TBD`

## 1. Objective
按 `ARCHITECTURE.md` §3/§4 的分层补上 Domain 规则与 Repository：纯计算放 `domain/calculation`，接口放 `domain/repository`，实现放 `data/repository`；计划版本切换必须在单事务内完成。

## 2. 实施结果
- `domain/model`：`Habit`、`HabitSchedule`、`DailyLog`、`NewHabit`、`NewSchedule`、`ScheduleVersion`。
- `domain/calculation/HabitRules`：名称非空、BOOLEAN 必须 `target=1` 且 `unit=null`、NUMERIC 必须 `target>0` 且单位非空。
- `domain/calculation/LogStatusRules`：`status` 与 `completedAt` 成对计算，保留首次达标时刻，回落后清空，再次达标取新时刻。
- `domain/calculation/ScheduleRules`：掩码一致性、ISO 星期位、计划日判定、`planVersionChange`（把旧版本关到新版本前一天）、`findVersionOn`（多版本重叠即完整性错误）。
- `domain/repository/HabitRepository`：接口不泄漏 Room 类型。
- `data/repository/RoomHabitRepository`：`withTransaction` 包裹建习惯、改计划、写日志。
- `data/repository/HabitScheduleWriter`：唯一写计划版本的组件，在调用方事务内关闭旧版本并插入新版本。
- `data/mapper/HabitMappers`：实体 ↔ Domain。
- `tools/run-unit-tests.ps1`：绕过被沙箱禁止的 Gradle 测试 worker，直接定位缓存 jar 用 `java -cp` 启动 `JUnitCore`。

## 3. 未完成 / 未验证
- **Room 事务行为没有运行时证据**：`withTransaction`、`@Upsert`、`closeVersion` 的实际执行未经测试（需 Android 运行时或 Robolectric）。
- 未实现 `UseCase` 层、`DataStore`、`ReminderScheduler`。
- 未在设备上验证“重启后数据持久化”。

## 4. 与计划的偏差
- `AppDestinationTest` 改为 `AppRoutesTest`：`AppDestination` 枚举在类初始化时加载 Compose 图标，纯 JVM 无法构造。路由常量抽到 `AppRoutes`，让导航契约可被单测覆盖。

## 5. Acceptance Criteria
- [x] 纯规则有自动化测试覆盖（27 项通过）
- [x] 计划版本切换逻辑（关闭旧版本、拒绝回退/重叠/双开版本）有测试
- [x] Repository 接口不泄漏 Room 类型
- [x] 写日志只有一个入口，`status`/`completedAt` 由纯函数成对产出
- [ ] Room 事务/DAO 集成测试 —— **未验证**
- [ ] 设备端持久化 —— **未验证：无设备**

## 6. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -File tools/run-unit-tests.ps1` | **OK (27 tests)** | `LogStatusRulesTest`、`ScheduleRulesTest`、`AppRoutesTest` |
| `gradlew assembleDebug` | 见 `TEST_REPORT` | 重构后重新打包验证 |
| `gradlew test` | 仍 FAILED（环境） | 测试 worker 管道被沙箱阻断；改用上面的 runner |

## 7. Risks
- Repository 的原子性只依赖 `withTransaction` 语义，未实测；并发写入未压测。
- `recordDailyValue` 用 `habit.targetValue` 作快照；若产品要求“当天目标可单独覆盖”，需要扩展入参。
- 归档与写入并发时无显式锁，依赖事务串行化。

## 8. Handoff
- 下一步：`UseCase` 层与 DataStore；补 Room 集成测试（Robolectric 或真机）。
- `ADR-011` 已 `ACCEPTED`，`DATABASE.md` §4/§12 措辞已同步。

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-005
- 状态：`PARTIAL`
- 阶段：Gate 2` / `Gate 3
- 验收：通过 4 项 / 未通过 2 项
- 最近测试记录：2026-11-05 / exit 0，19 PASS
<!-- close-task:end -->
