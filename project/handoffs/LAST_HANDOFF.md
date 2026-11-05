# LAST_HANDOFF.md

> 每次 AI 会话结束前更新。记录事实，不写愿望或推测。

## Snapshot
- 日期时间（含时区）：`2026-11-05`
- 当前阶段：`Gate 2`（数据层 + Domain/Repository/UseCase；治理系统已加固并合入 `main`）
- 当前任务：`TASK-008`（`PARTIAL`，UseCase 层完成、40 项单测通过；首轮独立评审 `REJECT`，已修复待复审）；`TASK-003`/`TASK-004`/`TASK-005` 为 `PARTIAL`，已在 `PROJECT_STATUS.md` 的「在册例外」登记
- 当前 Git 分支：`feat/usecase-layer`（本地，无远程）；`main` = `402dfc7`（`TASK-007` 以 `c10f67d` 合入后的状态提交）
- 最近 commit：以 `git log -1 --oneline` 为准；基线检查点 `d0f470a`

## Completed This Session
- 治理任务 `TASK-007` 走完「实现 → 首轮 `REJECT` → 修复 → 复审 `APPROVE` → `--no-ff` 合入 `main`（`c10f67d`）」
- Reviewer 对门禁做负向验证：`DONE` 缺记录、`INDEPENDENT`+`REJECT`、`NOT_INDEPENDENT`+`APPROVE` 均 exit 1
- 新建分支 `feat/usecase-layer` 与 `TASK-008`：Repository 增 `activeSchedule`/`dailyLog`，新增三层 UseCase 与 13 项单测（总计 40 项）
- 首轮独立评审 `REJECT`（`project/reviews/REVIEW-TASK-008.md`）：4 份文档未同步 + 两条测试是同义反复（fake 自己抛异常）+ 断言的是 fake 计算的状态 + arrange 混进 `@Test(expected)`
- 修复：`FakeHabitRepository` 改为宽松（缺 id 不抛），测试改为「先 arrange → 断言异常消息 → 断言仓库实存值」，并补受控负向验证
- `close-task.ps1` 两处缺陷修复：PowerShell 参数拼接导致 `Gate 2` 值吞掉反引号、摘要误把治理检查行当「最近测试记录」

## Files Changed
- 新增：`app/src/main/java/com/habittracker/domain/usecase/{CreateHabitUseCase,RecordHabitProgressUseCase,ArchiveHabitUseCase}.kt`、`app/src/test/java/com/habittracker/domain/usecase/{FakeHabitRepository,RecordHabitProgressUseCaseTest,CreateAndArchiveHabitUseCaseTest}.kt`、`project/tasks/TASK-008.md`、`project/reviews/REVIEW-TASK-006.md`、`project/reviews/REVIEW-TASK-007.md`、`project/reviews/REVIEW-TASK-008.md`
- 修改：`app/src/main/java/com/habittracker/domain/repository/HabitRepository.kt`、`app/src/main/java/com/habittracker/data/repository/RoomHabitRepository.kt`、`tools/close-task.ps1`、`tools/run-unit-tests.ps1`、`README.md`、`project/PROJECT_STATUS.md`、`project/reports/TEST_REPORT.md`、`project/handoffs/LAST_HANDOFF.md`

## Verification Actually Run
| 命令 | 结果 | 备注 |
|---|---|---|
| `pwsh -NoProfile -File tools/check-governance.ps1` | **20/20 PASS** | `main` 与 `feat/usecase-layer` 均通过 |
| `pwsh -NoProfile -File tools/run-unit-tests.ps1` | **OK (40 tests)** | 27 项既有 + 13 项新增 |
| 受控负向验证：删除 `RecordHabitProgressUseCase` 归档校验 | **FAILED（1 项）** | 恢复后 40 项全绿，证明该测试可证伪 |
| `gradlew.bat assembleDebug` | **SUCCESSFUL**（38s） | UseCase 层进入主源码集 |
| `git diff --check` | exit 0 | 无空白错误 |
| `gradlew test` | **FAILED（环境）** | Gradle 测试 worker 被沙箱禁止，不能作为通过证据 |

## Not Verified / Known Gaps
- Room/DAO/事务运行时行为：无 Robolectric、无设备（`HypervisorPresent=False`），`node tools/verify-schema.mjs` 只覆盖 SQL 层
- 迁移 v1→v2 与 `MigrationTestHelper` 测试未开始
- DataStore、ReminderScheduler、ViewModel/Compose 页面未开始

## Next Safe Action
1. 独立复审 `REVIEW-TASK-008.md`（新会话）：通过则置 `DONE`、`close-task`、`check-governance`、提交并合入 `main`
2. 之后的新任务（尚未创建任务文件）：Room/DAO 集成测试，Robolectric 优先；不可用则扩展 `verify-schema.mjs` 并明确标注「SQL 层覆盖、非 Room 运行时」
