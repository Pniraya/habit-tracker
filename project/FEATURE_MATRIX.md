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
| F-003 | 创建/编辑习惯 | 已定义 | VERIFIED | VERIFIED | DEVICE_VERIFIED |
| F-004 | 归档/恢复习惯 | 已定义 | IN_PROGRESS | VERIFIED | DEVICE_VERIFIED |
| F-005 | DAILY/WEEKDAYS/CUSTOM 计划 | 已定义 | VERIFIED | VERIFIED | NOT_STARTED |
| F-006 | BOOLEAN 日记录 | 已定义 | DEVICE_VERIFIED | VERIFIED | DEVICE_VERIFIED |
| F-007 | NUMERIC 日累计记录 | 已定义 | DEVICE_VERIFIED | VERIFIED | DEVICE_VERIFIED |
| F-008 | 撤销/修改日记录 | 已定义 | IN_PROGRESS | VERIFIED | IN_PROGRESS |
| F-009 | 当前/最长连续天数 | 已定义 | DEVICE_VERIFIED | VERIFIED | DEVICE_VERIFIED |
| F-010 | 周/月/年统计 | 已定义 | IN_PROGRESS | VERIFIED | DEVICE_VERIFIED |
| F-011 | 日历热力图/趋势 | 已定义 | DEVICE_VERIFIED | VERIFIED | DEVICE_VERIFIED |
| F-012 | 多条 Normal 提醒 | 已定义 | NOT_STARTED | NOT_STARTED | NOT_STARTED |
| F-013 | 重启/时区后提醒恢复 | 已定义 | NOT_STARTED | NOT_STARTED | NOT_STARTED |
| F-014 | JSON 备份/恢复 | 已定义 | NOT_STARTED | NOT_STARTED | NOT_STARTED |
| F-015 | 离线核心功能 | 已定义 | VERIFIED | NOT_STARTED | NOT_STARTED |
| F-016 | 平均用时 | V1 不做（ADR-010） | OUT_OF_SCOPE | — | — |
| F-017 | 桌面小组件 | V1 不做 | OUT_OF_SCOPE | — | — |
| F-018 | 深色模式 | V1 不做 | OUT_OF_SCOPE | — | — |
| F-019 | 账号/云同步/服务端 | V1 不做 | OUT_OF_SCOPE | — | — |
| F-020 | 永久删除习惯 | `DATABASE.md` §9 | VERIFIED | VERIFIED | DEVICE_VERIFIED |

### 脚注（状态为部分完成的行）

- **F-002**：`TASK-032` 按 `orgins/首页 · 今日-html` 重建今日页与底部导航（进度环、连续徽章、66dp 行、绿色 FAB、白底 28dp 圆角导航）。137 项单测与构建通过；iQOO Neo7 SE（Android 15）上安装后对照通过：列表、点勾后满环 +「今天也做到了」+「连续 1 天」徽章 + 叶片飘落、导航胶囊。真机列恢复为 `DEVICE_VERIFIED`（范围：渲染与打卡状态切换，不含滚动/平板）。
- **F-003**：`TASK-037` 增加自定义图标（系统 Photo Picker → 256px 存 `filesDir/icons` → `icon = custom:<file>`，真机走通，未知/丢失文件回退嫩芽）；内置单色线条图标由 6 个扩到 11 个（两行 6+6 网格）。创建（`TASK-012`/`TASK-016`/`TASK-018`）与编辑（`TASK-029`：`updateFields` + 编辑弹窗 + 历史快照不被改写）均已实现；`TASK-032` 按 `orgins/新建习惯-html` 重建创建页并新增图标/颜色选择（写入 `habits.icon`/`habits.color`）。真机已确认：修复版创建页无底部导航、按钮不遮挡；「选水滴图标 + 雾蓝 → 保存 → 列表显示水滴图标 → 打卡后勾选圈为雾蓝」走通，`icon`/`color` 落库获设备证据；归档后列表恢复。真机列恢复为 `DEVICE_VERIFIED`。
- **F-010**：只做了**最近 7 天完成率**（`TASK-029`，真机 `0 / 1 · 0%`）。周/月/年切换与热力图（`F-011`）未做，因此实现列为 `IN_PROGRESS`，不是 `VERIFIED` 的完整功能。
- **F-004**：`TASK-027` 补齐归档入口（今日页 `⋮ → 归档`），`TASK-028` 补齐恢复入口（我的页「恢复」）。真机走通「建 → 归档 → 我的页 → 恢复 → 今日页」，故为 `DEVICE_VERIFIED`。`TASK-034` 把我的页归档列表改为「年-月 → 日」两级分组（展示层，数据仍由 `archived_on` 派生），真机单月单日走通。
- **F-020**：`TASK-034` 新增永久删除：`⋮ → 删除` + 二次确认（提示可用归档），`HabitDao.deleteById` 单条 `DELETE`，子表由 `ON DELETE CASCADE` 清理；`DeleteHabitUseCase` 校验存在性。真机删除的是无日志的临时习惯；带历史日志的级联由 `verify-schema.mjs`/`verify-dao.mjs` 覆盖，未在真机造数。
- **F-008**：布尔习惯点两次即撤销（`TASK-027`，真机未逐项走查撤销这一步，只验证了归档）；数值习惯要重新输入 0 才算撤销；修改历史某天的记录仍未实现。故 `PARTIAL`。
- **F-009**：真机只验证了「今天完成 → 显示连续 1 天」；多段历史、最长连续、计划变更这三类需要跨日数据，**未在真机造数验证**，仅由 `StreakRulesTest` 与 `TodayPresenterTest` 覆盖。
- **F-005**：三种频率的模型、`ScheduleRules` 与创建页选择器都已实现并有单测；真机只验证过 `DAILY`（现存的 3 个习惯都是每天），`WEEKDAYS`/`CUSTOM` 未在设备上建过，故真机列保持 `NOT_STARTED`，不按 `DEVICE_VERIFIED` 记。
- **F-015**：`AndroidManifest.xml` 只声明 `POST_NOTIFICATIONS` 与 `RECEIVE_BOOT_COMPLETED`，没有 `INTERNET`；`libs.versions.toml` 无任何网络库。核心功能不依赖网络即成立。自动化列无专门断言，故记 `NOT_STARTED`。

## 证据索引

- **F-001** 工程骨架 — 代码：`MainActivity.kt`、`core/designsystem/*`；自动化：`assembleDebug` 通过；真机：`DEVICE_DUMP-2026-11-05.md` 记录了安装 APK 与启动（**仅安装/启动，未逐页巡检**）。
- **F-009** 连续天数 — 代码：`domain/calculation/StreakRules.kt`、`domain/usecase/GetStreakUseCase.kt`、`TodayPresenter.streakText`；自动化：`StreakRulesTest`（8 项）、`TodayPresenterTest`（行内连续天数、IN_PROGRESS 不计入、多计划版本）；真机：三个已完成习惯显示 `连续 1 天`（`TASK-021` §10）。
- **F-002** 今日页与导航 — 代码：`feature/today/TodayScreen.kt`、`TodayPresenter.kt`、`TodayViewModel.kt`、`navigation/AppRoot.kt`；自动化：`TodayPresenterTest`、`AppRoutesTest`；真机：`TASK-013`/`TASK-016`/`TASK-018` §10（空态、非空列表、FAB 均截图确认）。
- **F-003** 创建习惯 — 代码：`feature/create/CreateHabitScreen.kt`、`CreateHabitPresenter.kt`、`CreateHabitUseCase.kt`；自动化：`CreateHabitPresenterTest`（14 项）、`CreateAndArchiveHabitUseCaseTest`；真机：`TASK-012`/`TASK-016`/`TASK-018` §10（建布尔习惯、建数值习惯 `nu 2000 ml`），**`TASK-032` 重建后的界面未复验**。
- **F-004** 归档/恢复 — 代码：`domain/usecase/ArchiveHabitUseCase.kt`、`RoomHabitRepository.restoreHabit`；自动化：`CreateAndArchiveHabitUseCaseTest`；真机：无（无界面入口）。
- **F-005** 计划类型 — 代码：`domain/calculation/ScheduleRules.kt`、`entity/DataTypes.kt`、`CreateHabitScreen` 星期选择器；自动化：`ScheduleRulesTest`、`CreateHabitPresenterTest`（`NO_WEEKDAYS` 分支）；真机：仅建过 `DAILY` 习惯。
- **F-006** BOOLEAN 记录 — 代码：`RecordHabitProgressUseCase.kt`、`LogStatusRules.kt`、`TodayScreen` 点击切换；自动化：`LogStatusRulesTest`、`RecordHabitProgressUseCaseTest`；真机：`project/reports/DEVICE_DUMP-2026-11-05.md`、`TASK-013` §10。
- **F-007** NUMERIC 记录 — 代码：`RecordHabitProgressUseCase`、`TodayScreen.NumericInputDialog`、`TodayPresenter.progressText`/`prefillText`；自动化：`RecordHabitProgressUseCaseTest`、`TodayPresenterTest`（小数显示、预填、逗号归一化）；真机：`TASK-016`/`TASK-018` §10（`0.5 / 2000 ml`、对话框预填 `0.5`）。
- **F-011**：月度热力图与本周柱状趋势由 `TASK-031` 实现并在真机对照；`TASK-033` 补齐每习惯的「本周 + 近 5 周」网格（详情页）。故三列均为已验证。限制：统计页的周/年切换未实现，`TASK-035` 已删除死控件，改为静态「本月」标签（`ADR-017` §6）。
- **日期语义**：`TASK-035` 起所有「今天」来自 `DayCutoff`（04:00 逻辑日，`ADR-016`）；跨过 0 点的打卡仍属于前一天。
- **F-012~F-014** 提醒、提醒恢复、备份 — 全量 grep（`AlarmManager`、`WorkManager`、`backup`、网络库）无相关实现；`ProfileScreen.kt` 已承载归档列表，但没有提醒或备份逻辑。故仍为 `NOT_STARTED`。
- **F-008** 的「修改历史记录」仍未实现：`TASK-033` 详情页只读展示历史，编辑只在当前习惯字段上，故 `F-008` 实现列保持 `PARTIAL`。
- **数据层契约（不占功能行）**：`TASK-026` 新增设备侧 `DatabaseContractTest` 5 项——生产 `DatabaseConstraints.callback()` 的 8 个触发器注册与「重开后仍生效」、实体/Converter 往返（闰日、REAL 精度、可空 unit、枚举、时区切换）、`observeActive()` 失效、四表级联删除、`changeSchedule` 失败回滚。真机 6/6（含 `SmokeTest`）通过；仍未覆盖迁移与 UI。
- **F-015** 离线 — **覆盖范围**：治理检查第 22 项扫 `AndroidManifest.xml` 的 `INTERNET` 权限，以及 `gradle/libs.versions.toml`、`app/build.gradle.kts`、根 `build.gradle.kts` 的 `okhttp`/`retrofit`/`ktor`/`firebase` 字样。**挡不住**：第三方库 manifest 合并进来的 `INTERNET`、手写 socket/URL 代码。`VERIFIED` 主张的是「本仓库未声明网络能力」，不是「应用绝对无法联网」。 代码：`app/src/main/AndroidManifest.xml`（仅 `POST_NOTIFICATIONS`、`RECEIVE_BOOT_COMPLETED`，无 `INTERNET`）、`gradle/libs.versions.toml`（无 `okhttp`/`retrofit`/`ktor`/`firebase`）；自动化：无专门断言；真机：无。

## 更新规则

- 仅 Developer 在实现后可提出状态更新；Reviewer 应核对证据。
- `IMPLEMENTED_UNVERIFIED` 不能写成 `VERIFIED`。
- `VERIFIED` 不等于 `DEVICE_VERIFIED`：真机列必须有设备上的实际观察记录。
- 复合功能（如「创建/编辑」「归档/恢复」）的**实现状态**列在子功能未全部完成时记 `IN_PROGRESS`，并在脚注写清哪部分没做；**自动化验证/真机验证**列按已验证的子功能如实记录（例：F-003 创建部分已真机通过，故真机列为 `DEVICE_VERIFIED`，脚注限定范围为「仅创建」）。
- 每次状态变化应在任务记录中引用相关测试、构建或设备验证证据。
