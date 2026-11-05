# LAST_HANDOFF.md

> 每次 AI 会话结束前更新。记录事实，不写愿望或推测。

## Snapshot
- 日期时间（含时区）：`2026-11-05`
- 当前阶段：`Gate 2`（数据层与 Domain 已建；治理系统已加固）
- 当前任务：`TASK-007`（`PARTIAL`，实现验证完成，待独立 Reviewer）；`TASK-003`/`TASK-004`/`TASK-005` 为 `PARTIAL`，已在 `PROJECT_STATUS.md` 的「在册例外」登记
- 当前 Git 分支：`feat/governance-review-evidence`（本地，无远程）
- 最近 commit：以 `git log -1 --oneline` 为准；基线检查点 `d0f470a`

## Completed This Session
- 新增 ADR-013：要求 `TASK-007` 起的 DONE 任务保留独立 Reviewer 记录；TASK-006 已补独立 `APPROVE` 记录
- `close-task.ps1`、`run-unit-tests.ps1` 默认从脚本位置推导仓库根目录；单测日志移至已忽略的 `.gradle`
- 检查器扩展为 20 项：功能分支不将 PARTIAL 误判为 main 例外，并验证后续 DONE 任务有独立 `APPROVE`
- `ADR-012`：状态唯一来源、`main` 稳定分支、Reviewer 环节、收尾自动化（`ACCEPTED`）
- `AGENTS.md` §5/§7/§10/§12 同步为新的同步要求与分支策略
- 检查器 13 → **20 项**：README 与状态一致、ADR 状态一致（历史标注豁免）、发布报告不与工程矛盾、TEST_REPORT 摘要与末行一致、`PARTIAL` 在册例外、Reviewer 清单完整、`DONE` 任务必须有独立 `APPROVE` 记录
- 新增 `tools/close-task.ps1`（生成「状态摘要」块 + `-Apply` 同步阶段/当前任务）与 `project/REVIEW_CHECKLIST.md`
- 修复四份过期文档（README、PROJECT_STATUS、TEST_REPORT、RELEASE_REPORT）并纠正 TASK-001/TASK-004 的 ADR 状态措辞

## Files Changed
- 新增：`project/decisions/ADR-013-review-evidence-and-portable-tools.md`、`project/reviews/REVIEW_TEMPLATE.md`、`project/reviews/REVIEW-TASK-006.md`、`project/tasks/TASK-007.md`
- 修改：`AGENTS.md`、`README.md`、`project/PROJECT_STATUS.md`、`project/reports/TEST_REPORT.md`、`project/tasks/TASK_TEMPLATE.md`、`tools/check-governance.ps1`、`tools/close-task.ps1`、`tools/run-unit-tests.ps1`
- 新增：`project/decisions/ADR-012-governance-consistency-and-stable-main.md`、`project/REVIEW_CHECKLIST.md`、`project/tasks/TASK-006.md`、`tools/close-task.ps1`
- 修改：`AGENTS.md`、`README.md`、`project/PROJECT_STATUS.md`、`project/reports/TEST_REPORT.md`、`project/reports/RELEASE_REPORT.md`、`project/tasks/TASK-001.md`、`project/tasks/TASK-004.md`、`project/tasks/TASK-005.md`、`tools/check-governance.ps1`、`project/handoffs/LAST_HANDOFF.md`

## Verification Actually Run
| 命令 | 结果 | 备注 |
|---|---|---|
| `pwsh tools/check-governance.ps1` | **20/20 PASS** | 首轮 1 FAIL（ADR 漂移）、复审 1 次 REJECT（统计漂移），修复后全通过 |
| `pwsh -NoProfile -File tools/check-governance.ps1` | **20/20 PASS** | TASK-007：独立评审记录、功能分支 PARTIAL 规则；负向验证缺记录时 exit 1 |
| `pwsh tools/close-task.ps1 -Task TASK-005 -Apply` | 摘要写入 + 状态行同步 | `-Apply` 路径已验证 |
| `pwsh -NoProfile -File tools/run-unit-tests.ps1` | **OK (27 tests)** | TASK-007：日志改至 `.gradle` 后通过 |
| `pwsh tools/run-unit-tests.ps1` | OK (27 tests) | 代码未改，沿用上一轮结果 |
| `node tools/verify-schema.mjs` | 40/40 PASS | 同上 |
| `adb devices` | 空 | 无设备 |

## Incomplete Work
- `TASK-007` 的独立 Reviewer 记录与合入 main
- `TASK-003`/`TASK-004`/`TASK-005` 的设备与 Room 运行时验证
- `UseCase` 层、`DataStore`、`ReminderScheduler`
- Migration v1→v2 与迁移测试

## Known Issues / Risks
- ADR 一致性检查是启发式（30 字符窗口）；历史记录必须写“当时/历史记录”才豁免。
- `main` 上仍有 3 个 `PARTIAL` 任务，属登记例外；新增例外必须写进 `PROJECT_STATUS.md`，否则检查失败。
- 无设备、无 WHPX：设备验证短期无法自证。
- `gradlew test` 在沙箱内必失败，替代 runner 只覆盖纯 JVM 测试。

## Next Safe Action
1. 新会话的 Reviewer 审查 `feat/governance-review-evidence`，将结论写入 `project/reviews/REVIEW-TASK-007.md`。
2. `APPROVE` 后将 TASK-007 标为 DONE，再合入 main。
3. 然后写 `UseCase` 层并接 ViewModel。

## Recovery Notes
- 先核对：`git log --oneline -3`、`git status --short`、`tools/check-governance.ps1`（20 项）、`adb devices`
- 不要重复：不要重装 SDK；改受保护基线必须同 commit 带 ADR；不要把 `temp.md` 入库；不要用 `fallbackToDestructiveMigration`
