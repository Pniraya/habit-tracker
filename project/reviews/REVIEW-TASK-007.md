# REVIEW-TASK-007: 独立评审证据与治理脚本可移植性

- 审查任务：`TASK-007`
- 审查 commit：`489a318` 之上的未提交差异（`feat/governance-review-evidence`）
- Reviewer：`Codex /root/review_task007`
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`

## 1. Scope
- [x] 变更限于 ADR-013 所定义的评审证据、治理检查和脚本路径可移植性。

## 2. Diff 核对
- [x] 未发现 Android 产品功能、Gradle 依赖、权限或数据库 schema 改动。
- [x] `git diff --check` 退出码 0；未发现冲突标记。

## 3. 架构与数据
- [x] 受保护的 `AGENTS.md` 改动有同一待提交范围内的 ADR-013 支撑。
- [x] 没有改变架构分层或数据语义。

## 4. 验证证据
- [x] `pwsh -NoProfile -File tools/check-governance.ps1`：20/20 PASS。
- [x] `pwsh -NoProfile -File tools/run-unit-tests.ps1`：27 项通过。
- [x] 受控负向验证证明：缺少本评审记录时，将 TASK-007 改为 `DONE` 会使治理检查 exit 1。

## 5. 文档与分支
- [ ] 收尾摘要把整份 TASK 中所有勾选框当作验收项：当前显示“通过 9 项 / 未通过 2 项”，而 §6 实际是 4 项通过、1 项未完成。`tools/close-task.ps1:47` 与 `TASK-007.md:84` 需要只统计“Acceptance Criteria”章节。
- [ ] README 仍称检查器有“19 项”，实际执行结果为 20 项；`README.md:72` 需要同步。
- [ ] `LAST_HANDOFF.md:18` 与 `LAST_HANDOFF.md:57` 仍写 19 项，需同步为 20 项。README 的 Git 摘要也仍将当前状态写作 `main`，而实际审查分支为 `feat/governance-review-evidence`。

## 6. 结论
- 结论：`REJECT`
- 证据：`git diff --check`（exit 0）；`pwsh -NoProfile -File tools/check-governance.ps1`（20/20 PASS）；`pwsh -NoProfile -File tools/run-unit-tests.ps1`（27 tests PASS）；上述文档与摘要不一致的行号。
- 需修复项：仅修复 §5 的三项状态与统计漂移；修复后重新运行 `close-task.ps1`、治理检查和差异检查，再请求独立复审。
