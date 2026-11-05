# LAST_HANDOFF.md

> 每次 AI 会话结束前更新。记录事实，不写愿望或推测。

## Snapshot
- 日期时间（含时区）：`2026-11-05`
- 当前阶段：`Gate 2`（数据层与 Domain 已建；治理系统已加固）
- 当前任务：`TASK-006`（`DONE`）；`TASK-003`/`TASK-004`/`TASK-005` 为 `PARTIAL`，已在 `PROJECT_STATUS.md` 的「在册例外」登记
- 当前 Git 分支：`main`（稳定分支，本地，无远程）
- 最近 commit：以 `git log -1 --oneline` 为准；基线检查点 `d0f470a`

## Completed This Session
- `ADR-012`：状态唯一来源、`main` 稳定分支、Reviewer 环节、收尾自动化（`ACCEPTED`）
- `AGENTS.md` §5/§7/§10/§12 同步为新的同步要求与分支策略
- 检查器 13 → **19 项**：README 与状态一致、ADR 状态一致（历史标注豁免）、发布报告不与工程矛盾、TEST_REPORT 摘要与末行一致、`PARTIAL` 在册例外、Reviewer 清单完整
- 新增 `tools/close-task.ps1`（生成「状态摘要」块 + `-Apply` 同步阶段/当前任务）与 `project/REVIEW_CHECKLIST.md`
- 修复四份过期文档（README、PROJECT_STATUS、TEST_REPORT、RELEASE_REPORT）并纠正 TASK-001/TASK-004 的 ADR 状态措辞

## Files Changed
- 新增：`project/decisions/ADR-012-governance-consistency-and-stable-main.md`、`project/REVIEW_CHECKLIST.md`、`project/tasks/TASK-006.md`、`tools/close-task.ps1`
- 修改：`AGENTS.md`、`README.md`、`project/PROJECT_STATUS.md`、`project/reports/TEST_REPORT.md`、`project/reports/RELEASE_REPORT.md`、`project/tasks/TASK-001.md`、`project/tasks/TASK-004.md`、`project/tasks/TASK-005.md`、`tools/check-governance.ps1`、`project/handoffs/LAST_HANDOFF.md`

## Verification Actually Run
| 命令 | 结果 | 备注 |
|---|---|---|
| `pwsh tools/check-governance.ps1` | **19/19 PASS** | 扩展首轮 1 FAIL（真实 ADR 漂移），修复后全通过 |
| `pwsh tools/close-task.ps1 -Task TASK-005 -Apply` | 摘要写入 + 状态行同步 | `-Apply` 路径已验证 |
| `pwsh tools/run-unit-tests.ps1` | OK (27 tests) | 代码未改，沿用上一轮结果 |
| `node tools/verify-schema.mjs` | 40/40 PASS | 同上 |
| `adb devices` | 空 | 无设备 |

## Incomplete Work
- `TASK-003`/`TASK-004`/`TASK-005` 的设备与 Room 运行时验证
- `UseCase` 层、`DataStore`、`ReminderScheduler`
- Migration v1→v2 与迁移测试

## Known Issues / Risks
- ADR 一致性检查是启发式（30 字符窗口）；历史记录必须写“当时/历史记录”才豁免。
- `main` 上仍有 3 个 `PARTIAL` 任务，属登记例外；新增例外必须写进 `PROJECT_STATUS.md`，否则检查失败。
- 无设备、无 WHPX：设备验证短期无法自证。
- `gradlew test` 在沙箱内必失败，替代 runner 只覆盖纯 JVM 测试。

## Next Safe Action
1. 写 `UseCase` 层并接 ViewModel（纯 JVM 可测，继续用 `tools/run-unit-tests.ps1` 取证）。
2. 为 Room 事务补 Robolectric 集成测试。
3. 每任务结束：`close-task.ps1` → 手动同步清单 → `check-governance.ps1` → 单 commit。

## Recovery Notes
- 先核对：`git log --oneline -3`、`git status --short`、`tools/check-governance.ps1`（19 项）、`adb devices`
- 不要重复：不要重装 SDK；改受保护基线必须同 commit 带 ADR；不要把 `temp.md` 入库；不要用 `fallbackToDestructiveMigration`
