# TASK-008: UseCase 层与 Repository 查询补全

- 状态：`PARTIAL`（实现与单测完成，等待独立评审）
- 阶段：`Gate 2` / `Gate 3`
- 优先级：`P0`
- 依赖：`TASK-005`（Repository 已建，`PARTIAL` 在册例外）
- 创建日期：`2026-11-05`
- 完成日期：`TBD`

## 1. Objective
在 Repository 之上补最小可用 UseCase 层，让"记录进度"遵循"计划日 + 追踪类型"规则，并提供可被 ViewModel 直接调用的入口；用纯 JVM 单测覆盖这些规则。

## 2. Context
`ARCHITECTURE.md` §3.2（Domain 不得依赖 Room）、§5.2（ViewModel → UseCase → Repository）；`DATABASE.md` §4.3、§6；`ADR-006`。

## 3. Scope
- Repository 增补两个只读查询：`activeSchedule(habitId, date)`、`dailyLog(habitId, date)`。
- 新增 UseCase：`CreateHabitUseCase`、`RecordHabitProgressUseCase`、`ArchiveHabitUseCase`。
- 用 fake repository 写纯 JVM 单测，覆盖计划日判定、布尔/数值校验、归档拒绝写入。

## 4. Out of Scope
- ViewModel、Compose 页面、提醒调度、DataStore。
- 连续天数与统计（Gate 3 后续任务）。
- Robolectric / 仪器测试。

## 5. Expected Files
- 修改：`domain/repository/HabitRepository.kt`、`data/repository/RoomHabitRepository.kt`
- 新增：`domain/usecase/*.kt`、`app/src/test/java/com/habittracker/domain/usecase/*`

## 6. Acceptance Criteria
- [x] 记录进度在非计划日被拒绝
- [x] 布尔习惯只接受 0/1，数值习惯接受非负值
- [x] 归档习惯拒绝写入当天日志
- [x] 三个 UseCase 均有单测且通过（`tools/run-unit-tests.ps1`，40 项）
- [x] `assembleDebug` 通过

## 7. Tests
- 单元测试：`CreateHabitUseCaseTest`、`RecordHabitProgressUseCaseTest`、`ArchiveHabitUseCaseTest`
- 构建：`gradlew.bat assembleDebug`

## 7.5 Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/run-unit-tests.ps1` | **OK (40 tests)** | 新增 `RecordHabitProgressUseCaseTest`、`CreateAndArchiveHabitUseCaseTest`；首次编译因 `HabitRepository` 缺 `HabitSchedule` import 失败，已修 |
| `gradlew.bat assembleDebug` | **SUCCESSFUL**（38s） | UseCase 层进入主源码集 |

## 8. Risks and Decisions
- 计划日判定复用 `ScheduleRules`，不新增第二套实现。
- UseCase 只做编排与校验，业务计算仍留在 `domain/calculation`。

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-008
- 状态：`PARTIAL`
- 阶段：Gate 2` / `Gate 3
- 验收：通过 5 项 / 未通过 0 项
- 最近测试记录：2026-11-05 / exit 0，20 PASS
<!-- close-task:end -->
