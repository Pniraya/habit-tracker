# LAST_HANDOFF.md

> 每次 AI 会话结束前更新。记录事实，不写愿望或推测。

## Snapshot
- 日期时间（含时区）：`2026-11-05`
- 当前阶段：`Gate 3`（Today 与新建习惯两个页面已接真实数据）
- 当前任务：无（`TASK-012` 已 `DONE`，第 3 轮独立评审 `APPROVE` 于 `07b1aab`，合入 `main`）；`TASK-003`/`TASK-004`/`TASK-005` 为 `PARTIAL`，已在 `PROJECT_STATUS.md` 的「在册例外」登记
- 当前 Git 分支：`feat/create-habit`（本地，无远程）；`main` @ `cc4fba5`
- 最近 commit：以 `git log -1 --oneline` 为准；基线检查点 `d0f470a`

## Completed This Session
- `TASK-007`~`TASK-011` 全部 `DONE` 合入 `main`：治理加固、`verify-all` 单一入口、DAO SQL 验证、Today 页状态机
- `TASK-012`：新建习惯表单（名称/类型/目标/单位/频率/自定义周几）+ Presenter + 路由；Today 页数值输入对话框与空态入口
- 三轮评审：第 1 轮 `REJECT`（unit 状态泄漏真缺陷 + 三份文档未同步）、第 2 轮 `REJECT`（仅计数不一致）、第 3 轮 `APPROVE`
- 首轮评审抓到真缺陷：数值习惯填了单位再切回布尔，单位残留且字段隐藏 → 表单永远保存失败。已修（切换时清空 + `draft` 强制 `null`）并补回归测试
- 首轮评审还发现 `TEST_REPORT` 的 Runs 行序错乱，导致 `close-task` 摘要取到过期的 49 项；已改为时间序并在末端补 58 项行
- 发现并记录：`target`/`unit` 规则在 `RoomHabitRepository`（`HabitRules.normalizeTarget`）而非 UseCase；`FakeHabitRepository` 已对齐同一规则，规则上移留给后续任务

## Files Changed
本任务（TASK-012）：
- 新增：`feature/create/{CreateHabitForm,CreateHabitPresenter,CreateHabitScreen}.kt`、`app/src/test/java/com/habittracker/feature/create/CreateHabitPresenterTest.kt`、`project/tasks/TASK-012.md`、`project/reviews/REVIEW-TASK-012.md`
- 修改：`feature/today/TodayScreen.kt`（数值对话框、空态入口）、`navigation/{AppRoot,AppRoutes}.kt`、`app/src/test/java/com/habittracker/domain/usecase/FakeHabitRepository.kt`（与生产规则对齐）、`README.md`、`project/PROJECT_STATUS.md`、`project/reports/TEST_REPORT.md`、`project/handoffs/LAST_HANDOFF.md`

## Verification Actually Run
| 命令 | 结果 | 备注 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS** | 治理 21/21（0 警告）、单测 57→58 项、DAO SQL 21 项、构建 21-29s、空白 0s |
| `gradlew.bat assembleDebug` | **SUCCESSFUL**（29s） | 首次失败：`NewSchedule(frequency=...)` 参数名应为 `frequencyType` |
| 变异：`isBlank()` → `isEmpty()` | **FAILED（1 项）** | 空名测试变红 |
| 变异：CUSTOM mask 固定 `ALL_DAYS_MASK` | **FAILED（1 项）** | 自定义周几测试变红 |
| 变异：删除 `save()` 本地校验 | **FAILED（4 项）** | 四条本地校验测试全红 |
| `gradlew test` | **FAILED（环境）** | 测试 worker 被沙箱禁止 |

## Not Verified / Known Gaps
- **UI 无自动化验证**：Today/新建两页的状态机共有 18 项 JVM 测试；Compose 渲染与交互仅编译进 APK
- **无设备/模拟器**：`HypervisorPresent=False`（虚拟化未启用），`adb devices` 为空；`DEVICE_VERIFIED` 全程未达成
- Room 的 Cursor 映射、TypeConverter、Flow 通知、`withTransaction` 边界：无自动化验证
- 表单由 `remember` 持有，旋转会丢失输入
- `target`/`unit` 校验仍在数据层；`HabitRepository.createHabit` 的契约未在接口上声明

## Next Safe Action
1. **TASK-013（P0，任务文件待建）**：Today 页有习惯后没有新建入口（只有空态有按钮），用户无法添加第二个习惯。修法：非空态也给出入口（顶栏 + 或 FAB），并补 Presenter 测试。
2. **TASK-014（P1，任务文件待建）**：把 `validateName`/`normalizeTarget` 上移到 `CreateHabitUseCase`，并在 `HabitRepository` 接口写明契约——`TASK-012` 暴露的分层问题（仓库层校验，换实现必漏）。
3. **TASK-015（P2，任务文件待建）**：`check-governance` 第 15 项只比对日期与 `PASS`/`FAILED` 字样、不比对 `OK (N tests)` 的 N，导致「Latest Run 57 / 末行 58」这类不一致能过检；第 21 项只查是否存在 verify-all 行、不查计数。补计数一致性检查。
4. 之后：图标/颜色选择器、习惯编辑与删除、提醒调度（`ReminderScheduler` + DataStore）。
