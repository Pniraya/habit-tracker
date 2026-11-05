# TASK-XXX: <简明任务名称>

- 状态：`NOT_STARTED` / `IN_PROGRESS` / `PARTIAL` / `BLOCKED` / `DONE`
- 阶段：`Gate N`
- 优先级：`P0` / `P1` / `P2`
- 依赖：`TASK-xxx` 或 `None`
- 创建日期：`YYYY-MM-DD`
- 完成日期：`TBD`

## 1. Objective
描述本任务唯一、可验证的目标。

## 2. Context
相关需求、架构、数据库文档章节和 ADR。

## 3. Scope
- 本任务允许完成的内容。

## 4. Out of Scope
- 明确不在本任务内的内容。

## 5. Expected Files
- 预计新增/修改的文件；实施前可修订并说明原因。

## 6. Acceptance Criteria
- [ ] 可观察、可判定的验收条件 1
- [ ] 可观察、可判定的验收条件 2

## 7. Tests
- 单元测试：
- 集成测试：
- 构建/静态检查：
- 真机验证（如适用）：

## 8. Risks and Decisions
列出风险；需要改变基线时先提交 ADR 并等待批准。

## 9. Implementation Log
按日期记录关键变更，不复制冗长聊天记录。**未开工就不要写日期行**：带日期的行表示真的动过东西，治理检查会据此判定任务已经开工（声明 `NOT_STARTED` 却带日期行的任务会 FAIL）。计划写在 §2 或 §8。

## 10. Verification Evidence
必须包含一行 `tools/verify-all.ps1` 的实际运行结果（`TASK-009` 起；治理检查第 21 项强制）。

结果列的写法有机器约束（`TASK-020` 起生效，治理检查第 21 项）：

- **判定词必须是整格内容**，只能是 `VERIFY ALL PASS`、`VERIFY ALL FAILED`、`PASS`、`FAILED`、`未运行（任务未开始）` 之一；允许加粗与一个尾随括号说明（如 `**VERIFY ALL PASS**（exit 0）`），比较前会被剥掉。
- **用英文判定词**，不要写「通过」「已完成」等中文描述：中文不在词表内，会被判成「该行没有判定词」，进而让整个任务失去证据。
- 只是提到命令名、或结果列写「待补」「见上」的行**不算证据**；备注可以写在证据行下方，但结果列不要长得像判定词。
- 任务尚未开工时，可写 `尚未开始（NOT_STARTED）` 作为占位；一旦任务有实质进展（勾了验收、§9 有日期行、§10 有真实判定），必须换成真实结果。

| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | TBD | TBD |

## 11. Completion Review
- [ ] 验收标准全部满足
- [ ] 相关测试实际运行
- [ ] `git diff --check` 通过
- [ ] Diff 已审查，无无关改动
- [ ] 文档/状态已更新
- [ ] 未完成项已明确记录

## 12. Handoff
- 当前剩余工作：
- 下一条安全操作：
- Git 分支/提交：

## 13. Independent Review
- Reviewer record：`project/reviews/REVIEW-TASK-xxx.md`
- 结论：`PENDING` / `APPROVE` / `REJECT`
