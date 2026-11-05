# FEATURE_MATRIX.md

> 状态只反映已验证的实现，不反映“已讨论”或“已规划”。

状态定义：
- `NOT_STARTED`：没有实现证据。
- `IN_PROGRESS`：实现正在进行（含“部分子功能完成、部分未开始”，见下方脚注）。
- `IMPLEMENTED_UNVERIFIED`：代码已写，但相关测试/构建尚未通过或未执行。
- `VERIFIED`：自动化测试/构建通过。
- `DEVICE_VERIFIED`：目标 Android 设备上完成验证。
- `BLOCKED`：有明确阻塞项。
- `OUT_OF_SCOPE`：明确不属于 V1。

| ID | 功能 | 需求/设计 | 实现状态 | 自动化验证 | 真机验证 |
|---|---|---|---|---|---|
| F-001 | Android 工程骨架 | 已定义 | DEVICE_VERIFIED | VERIFIED | DEVICE_VERIFIED |
| F-002 | 今日页与导航 | 已定义 | DEVICE_VERIFIED | VERIFIED | DEVICE_VERIFIED |
| F-003 | 创建/编辑习惯 | 已定义 | IN_PROGRESS | VERIFIED | DEVICE_VERIFIED |
| F-004 | 归档/恢复习惯 | 已定义 | IN_PROGRESS | VERIFIED | NOT_STARTED |
| F-005 | DAILY/WEEKDAYS/CUSTOM 计划 | 已定义 | VERIFIED | VERIFIED | NOT_STARTED |
| F-006 | BOOLEAN 日记录 | 已定义 | DEVICE_VERIFIED | VERIFIED | DEVICE_VERIFIED |
| F-007 | NUMERIC 日累计记录 | 已定义 | DEVICE_VERIFIED | VERIFIED | DEVICE_VERIFIED |
| F-008 | 撤销/修改日记录 | 已定义 | NOT_STARTED | NOT_STARTED | NOT_STARTED |
| F-009 | 当前/最长连续天数 | 已定义 | NOT_STARTED | NOT_STARTED | NOT_STARTED |
| F-010 | 周/月/年统计 | 已定义 | NOT_STARTED | NOT_STARTED | NOT_STARTED |
| F-011 | 日历热力图/趋势 | 已定义 | NOT_STARTED | NOT_STARTED | NOT_STARTED |
| F-012 | 多条 Normal 提醒 | 已定义 | NOT_STARTED | NOT_STARTED | NOT_STARTED |
| F-013 | 重启/时区后提醒恢复 | 已定义 | NOT_STARTED | NOT_STARTED | NOT_STARTED |
| F-014 | JSON 备份/恢复 | 已定义 | NOT_STARTED | NOT_STARTED | NOT_STARTED |
| F-015 | 离线核心功能 | 已定义 | VERIFIED | NOT_STARTED | NOT_STARTED |
| F-016 | 平均用时 | V1 不做（ADR-010） | OUT_OF_SCOPE | — | — |
| F-017 | 桌面小组件 | V1 不做 | OUT_OF_SCOPE | — | — |
| F-018 | 深色模式 | V1 不做 | OUT_OF_SCOPE | — | — |
| F-019 | 账号/云同步/服务端 | V1 不做 | OUT_OF_SCOPE | — | — |

### 脚注（状态为部分完成的行）

- **F-003**：创建已实现并真机验证；**编辑习惯未开始**（无编辑入口、无 `updateHabit` 调用路径）。`VERIFIED` 只覆盖创建。
- **F-004**：`ArchiveHabitUseCase` 与 `HabitRepository.restoreHabit` 已实现并有单测；**界面没有归档/恢复入口**，故真机列为 `NOT_STARTED`。
- **F-005**：三种频率的模型、`ScheduleRules` 与创建页选择器都已实现并有单测；真机只验证过 `DAILY`（现存的 3 个习惯都是每天），`WEEKDAYS`/`CUSTOM` 未在设备上建过，故真机列保持 `NOT_STARTED`，不按 `DEVICE_VERIFIED` 记。
- **F-015**：`AndroidManifest.xml` 只声明 `POST_NOTIFICATIONS` 与 `RECEIVE_BOOT_COMPLETED`，没有 `INTERNET`；`libs.versions.toml` 无任何网络库。核心功能不依赖网络即成立。自动化列无专门断言，故记 `NOT_STARTED`。

## 证据索引

- **F-001** 工程骨架 — 代码：`MainActivity.kt`、`core/designsystem/*`；自动化：`assembleDebug` 通过；真机：应用安装并启动。
- **F-002** 今日页与导航 — 代码：`feature/today/TodayScreen.kt`、`TodayPresenter.kt`、`TodayViewModel.kt`、`navigation/AppRoot.kt`；自动化：`TodayPresenterTest`、`AppRoutesTest`；真机：`TASK-013`/`TASK-016`/`TASK-018` §10（空态、非空列表、FAB 均截图确认）。
- **F-003** 创建习惯 — 代码：`feature/create/CreateHabitScreen.kt`、`CreateHabitPresenter.kt`、`CreateHabitUseCase.kt`；自动化：`CreateHabitPresenterTest`（9 项）、`CreateAndArchiveHabitUseCaseTest`；真机：`TASK-012`/`TASK-016`/`TASK-018` §10（建布尔习惯、建数值习惯 `nu 2000 ml`）。编辑习惯无代码。
- **F-004** 归档/恢复 — 代码：`domain/usecase/ArchiveHabitUseCase.kt`、`RoomHabitRepository.restoreHabit`；自动化：`CreateAndArchiveHabitUseCaseTest`；真机：无（无界面入口）。
- **F-005** 计划类型 — 代码：`domain/calculation/ScheduleRules.kt`、`entity/DataTypes.kt`、`CreateHabitScreen` 星期选择器；自动化：`ScheduleRulesTest`、`CreateHabitPresenterTest`（`NO_WEEKDAYS` 分支）；真机：仅建过 `DAILY` 习惯。
- **F-006** BOOLEAN 记录 — 代码：`RecordHabitProgressUseCase.kt`、`LogStatusRules.kt`、`TodayScreen` 点击切换；自动化：`LogStatusRulesTest`、`RecordHabitProgressUseCaseTest`；真机：`project/reports/DEVICE_DUMP-2026-11-05.md`、`TASK-013` §10。
- **F-007** NUMERIC 记录 — 代码：`RecordHabitProgressUseCase`、`TodayScreen.NumericInputDialog`、`TodayPresenter.progressText`/`prefillText`；自动化：`RecordHabitProgressUseCaseTest`、`TodayPresenterTest`（小数显示、预填、逗号归一化）；真机：`TASK-016`/`TASK-018` §10（`0.5 / 2000 ml`、对话框预填 `0.5`）。
- **F-015** 离线 — 代码：`app/src/main/AndroidManifest.xml`（仅 `POST_NOTIFICATIONS`、`RECEIVE_BOOT_COMPLETED`，无 `INTERNET`）、`gradle/libs.versions.toml`（无 `okhttp`/`retrofit`/`ktor`/`firebase`）；自动化：无专门断言；真机：无。

## 更新规则

- 仅 Developer 在实现后可提出状态更新；Reviewer 应核对证据。
- `IMPLEMENTED_UNVERIFIED` 不能写成 `VERIFIED`。
- `VERIFIED` 不等于 `DEVICE_VERIFIED`：真机列必须有设备上的实际观察记录。
- 复合功能只有在**全部子功能**完成时才能整体记 `VERIFIED`/`DEVICE_VERIFIED`；否则用 `IN_PROGRESS` 并在脚注写清哪部分没做。
- 每次状态变化应在任务记录中引用相关测试、构建或设备验证证据。
