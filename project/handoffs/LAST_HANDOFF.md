# LAST_HANDOFF.md

> 每次 AI 会话结束前更新。记录事实，不写愿望或推测。

## Snapshot
- 日期时间（含时区）：`2026-11-05`
- 当前阶段：`Gate 2`（数据层 + Domain/Repository/UseCase；治理系统已加固，新增单一验证入口）
- 当前任务：`TASK-010`（`PARTIAL`，DAO SQL 运行时验证完成，待独立评审）；`TASK-009` 已 `DONE` 合入 `main`（`7c8b074`）；`TASK-003`/`TASK-004`/`TASK-005` 为 `PARTIAL`，已在 `PROJECT_STATUS.md` 的「在册例外」登记
- 当前 Git 分支：`feat/dao-sql-verification`（本地，无远程）；`main` @ `18360bc`
- 最近 commit：以 `git log -1 --oneline` 为准；基线检查点 `d0f470a`

## Completed This Session
- 治理任务 `TASK-007` 合入 `main`（`c10f67d`）；`TASK-008` UseCase 层经「首轮 `REJECT` → 变异验证 → 复审 `APPROVE`」合入（`d7998ad`）
- `TASK-008` 首轮评审发现两条测试是同义反复（fake 自己抛异常、断言 fake 计算的状态）；改为宽松 fake + 断言仓库实存值 + arrange 在期望之外，7 组变异验证全部变红
- `TASK-009`：新增 `tools/verify-all.ps1`（治理 + 单测 + 构建 + 空白检查，任一步失败即非零退出），检查器扩为 21 项
- `ADR-014` 记录该决策与用户授权来源；`AGENTS.md` §5 的建议命令改为单入口要求
- 首轮 `verify-all` 运行即在治理步 FAIL（`ADR-014 not covered by PROJECT_STATUS`），补登记后通过——门禁当场拦住了真实遗漏
- `TASK-009` 首轮独立评审 `REJECT`：`LAST_HANDOFF`/`TEST_REPORT` 未记录本次运行（正是本任务要防的失败），已补
- `TASK-010`：新增 `tools/verify-dao.mjs`，从 DAO 源码解析 `@Query` 并在真实 SQLite 上执行，19 项断言覆盖 upsert 冲突形状、`archive`/`closeVersion` 的 CAS、`BETWEEN` 闭区间、每天最多一个计划版本、级联删除
- 变异验证：去掉 `archived_on IS NULL` → 1 项失败；`BETWEEN` 改开区间 → 2 项失败；还原后全绿
- `verify-all` 现为 5 步（治理 / 单测 / DAO SQL / 构建 / 空白）；检查器第 21 项已禁止占位行（`TBD`/`NOT_RUN`）通过

## Files Changed
本任务（TASK-009）：
- 新增：`tools/verify-all.ps1`、`project/decisions/ADR-014-single-verification-entry.md`、`project/tasks/TASK-009.md`、`project/reviews/REVIEW-TASK-009.md`
- 修改：`AGENTS.md`（§5，受保护文件，依据 `ADR-014`）、`project/tasks/TASK_TEMPLATE.md`（§10）、`tools/check-governance.ps1`（第 21 项）、`tools/close-task.ps1`、`README.md`、`project/PROJECT_STATUS.md`、`project/reports/TEST_REPORT.md`、`project/tasks/TASK-009.md`

历史（本会话早前完成，不属于 TASK-009）：
- TASK-008 产品源码：`app/src/main/java/com/habittracker/domain/{repository,usecase}/*`、`app/src/test/java/com/habittracker/domain/usecase/*`、`tools/run-unit-tests.ps1`
- `project/tasks/TASK-008.md`、`project/reviews/REVIEW-TASK-008.md`

## Verification Actually Run
| 命令 | 结果 | 备注 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS**（exit 0） | 治理 7s（21/21）、单测 25-31s（40 项）、构建 21-24s、空白检查 0s |
| `pwsh -NoProfile -File tools/check-governance.ps1` | **21/21 PASS**，0 警告 | 分支与 `main` 均通过；曾拦下 TASK-010 的占位证据行 |
| `node tools/verify-dao.mjs` | **DAO SQL PASS** | 19 项断言；变异 1/2 分别变红 1 项与 2 项 |
| `pwsh -NoProfile -File tools/run-unit-tests.ps1` | **OK (40 tests)** | 27 项既有 + 13 项 UseCase |
| 受控负向验证：删除 `RecordHabitProgressUseCase` 归档校验 | **FAILED（1 项）** | 恢复后 40 项全绿 |
| 受控负向验证：删除 `TASK-009` 的 verify-all 证据 | **FAILED（第 21 项）** | 还原后 21/21 PASS |
| `gradlew.bat assembleDebug` | **SUCCESSFUL**（21-24s） | 无产品代码改动 |
| `git diff --check` | exit 0 | 无空白错误 |
| `gradlew test` | **FAILED（环境）** | Gradle 测试 worker 被沙箱禁止，不能作为通过证据 |

## Not Verified / Known Gaps
- Room 的 Cursor 映射、TypeConverter 往返、Flow 失效通知：仍无自动化验证（`verify-dao.mjs` 只覆盖 SQL 语义与约束）
- 事务运行时行为（`withTransaction` 边界、并发写入）：无验证
- 迁移 v1→v2 与 `MigrationTestHelper` 未开始
- 检查器第 18/21 项验证的是"证据存在"，不验证证据新鲜度；由 Reviewer 复核

## Next Safe Action
1. 独立评审 `REVIEW-TASK-010.md`（通过则 `DONE`、`verify-all`、合入 `main`）
2. 之后的新任务（尚未创建）：ViewModel 层 + Compose 页面接线，复用已验收的 UseCase；提醒调度与 DataStore 在其后
