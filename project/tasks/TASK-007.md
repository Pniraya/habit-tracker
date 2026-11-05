# TASK-007: 独立评审证据与治理脚本可移植性

- 状态：`DONE`
- 阶段：`Gate 2`（治理基础设施，不改变产品范围）
- 优先级：`P0`
- 依赖：`TASK-006`
- 创建日期：`2026-11-05`
- 完成日期：`2026-11-05`

## 1. Objective
让“Reviewer 已核对”成为可审计的事实，并让治理脚本默认作用于当前仓库，而不是固定机器路径。

## 2. Context
- `ADR-012` 已要求独立 Reviewer，但原检查器只确认清单模板存在。
- 审查发现 `close-task.ps1` 与 `run-unit-tests.ps1` 的默认 `Repo` 都是 `D:\AndroidStudio`。

## 3. Scope
- 新增 ADR-013、评审记录模板与 TASK-006 的独立评审记录。
- 为 TASK-007 及后续 `DONE` 任务检查独立 `APPROVE` 记录。
- 让两项 PowerShell 工具从脚本位置推导默认仓库路径。
- 同步治理规则、状态、交接和测试记录。

## 4. Out of Scope
- Android 产品功能、Gradle 依赖、数据库 schema、历史任务的追溯性评审。

## 5. Expected Files
- 修改：`AGENTS.md`、`README.md`、`PROJECT_STATUS.md`、`TASK_TEMPLATE.md`、治理脚本。
- 新增：ADR-013、`project/reviews/**`、本任务记录。

## 6. Acceptance Criteria
- [x] `TASK-007` 起的 `DONE` 任务缺独立 `APPROVE` 记录时，治理检查失败。
- [x] TASK-006 有独立评审记录，且检查器能读取其结构。
- [x] `close-task.ps1` 与 `run-unit-tests.ps1` 不含默认仓库绝对路径。
- [x] 治理检查、脚本 dry run、空白检查全部通过。
- [x] 本任务有后续独立 Reviewer 记录后才可标记 `DONE` 并合入 `main`（记录：`project/reviews/REVIEW-TASK-007.md`，`APPROVE`）。

## 7. Tests
- 治理检查：`pwsh -NoProfile -File tools/check-governance.ps1`。
- 脚本 dry run：`pwsh -NoProfile -File tools/close-task.ps1 -Task TASK-007`。
- 回归：`pwsh -NoProfile -File tools/run-unit-tests.ps1`。
- 检查：`git diff --check`。

## 8. Risks and Decisions
- 需要改动受保护的 `AGENTS.md`，因此由 ADR-013 记录用户授权、原因和最小影响。
- 实现者不能为自己的 TASK-007 写独立评审结论；该任务只能留在功能分支等待 Reviewer。

## 9. Implementation Log
- 2026-11-05：发现 Reviewer 清单未形成审计证据，且两项仓库脚本写死路径；创建 ADR-013 与本任务。
- 2026-11-05：首次运行单测脚本时，系统临时目录拒绝创建编译日志；改为写入已忽略的 `.gradle` 目录。
- 2026-11-05：受控负向验证将本任务暂改为 `DONE`；检查器如预期因缺少独立评审记录失败，随后立即恢复为 `PARTIAL`。
- 2026-11-05：首次独立评审结论 `REJECT`，三项问题：`close-task` 把 TASK 内所有勾选框当作验收项（显示 9/2，实际 4/1）、README 仍写检查器 19 项、交接两处仍写 19 项且 README 把当前分支硬编码为 `main`。已改为只统计 Acceptance Criteria 章节、同步 20 项、README 分支表述改为通用规则；重新运行检查器 20/20、单测 27 项、`git diff --check` exit 0。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/close-task.ps1 -Task TASK-007` | PASS | 使用脚本位置推导仓库根目录，成功生成状态摘要 |
| `pwsh -NoProfile -File tools/check-governance.ps1` | PASS | 20/20 通过；功能分支不要求将 TASK-007 登记为 main 例外 |
| `pwsh -NoProfile -File tools/run-unit-tests.ps1` | PASS | 27 项通过；首次因系统 TEMP 拒绝写日志失败，改用 `.gradle` 后重试成功 |
| `git diff --check` | PASS | 无空白错误 |
| 受控负向验证：TASK-007 暂改为 `DONE` 后运行治理检查 | 预期 FAILED | 当时尚无评审记录，`done tasks have review evidence` 报缺少 `REVIEW-TASK-007.md`；随后恢复为 `PARTIAL` |

## 11. Completion Review
- [x] 验收标准全部满足（独立 Reviewer `APPROVE`）
- [x] 相关测试实际运行
- [x] `git diff --check` 通过
- [x] Diff 已审查，无无关改动
- [x] 文档/状态已更新
- [x] 未完成项已明确记录

## 12. Handoff
- 当前剩余工作：独立 Reviewer 按 `project/reviews/REVIEW_TEMPLATE.md` 审查本分支并给出结论。
- 下一条安全操作：独立会话运行验证命令，写入 `project/reviews/REVIEW-TASK-007.md`；只有 `APPROVE` 后才可标记 `DONE` 并合入 main。
- Git 分支/提交：`feat/governance-review-evidence` @ `b630537`；以 `--no-ff` 合入 `main`，合并提交 `c10f67d`。

## 13. Independent Review
- Reviewer record：`project/reviews/REVIEW-TASK-007.md`
- 结论：`APPROVE`（审查 commit `b63053762010fff0afa8394e1597014e27bf8b0c`；首次复审因四项文档漂移 `REJECT`，已修复）

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-007
- 状态：`DONE`
- 阶段：Gate 2
- 验收：通过 5 项 / 未通过 0 项
- 最近测试记录：2026-11-05 / exit 0，20 PASS
<!-- close-task:end -->
