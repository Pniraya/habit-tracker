# TASK-015: 治理检查补计数一致性

- 状态：`NOT_STARTED`
- 阶段：`Gate 3`
- 优先级：`P2`
- 依赖：`TASK-012`（领域与仓储现状）、`TASK-013`
- 创建日期：`2026-11-05`
- 完成日期：`TBD`

## 1. Objective
让 `tools/check-governance.ps1` 能发现「`TEST_REPORT` 的 `Latest Run` 计数与最后一条 `Runs` 行不一致」这类问题——`TASK-012` 就在这种不一致下通过过检查。

## 2. Context
`TASK-012` 第 2 轮评审发现：`Latest Run` 写 57、`Runs` 末行是 58，治理检查仍通过。第 15 项只比对日期与 `PASS`/`FAILED` 字样，不比对计数；第 21 项只查 verify-all 行是否存在。

## 3. Scope
- 解析 `Latest Run` 的 `单测 N 项`，与 `Runs` 表最后一条匹配行的 `OK (N tests)` 比对，不一致即失败。
- 对 `TASK-0xx` 的「最近测试记录（自动生成）」做同样比对。
- 新增检查需要正/反例自测，现有 59 项单测不受影响。

## 4. Out of Scope
- 修改 `TEST_REPORT` 或 `close-task` 的输出格式。

## 5. Expected Files
- 待实施时补充

## 6. Acceptance Criteria
- [ ] 人为把 `Latest Run` 改成 57 而末行是 59，检查必须失败
- [ ] 一致时检查通过

## 7. Tests
- 待实施时补充；至少一组变异证明断言可证伪

## 8. Risks and Decisions
- 尚未实施，无决策记录

## 9. Implementation Log
- 尚未开始

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | 未运行（任务未开始，无代码改动） | — |

## 11. Completion Review
- [ ] 验收标准全部满足
- [ ] 相关测试实际运行
- [ ] `git diff --check` 通过
- [ ] Diff 已审查
- [ ] 文档/状态已更新
- [ ] 独立 Reviewer `APPROVE`

## 12. Handoff
- 尚未开始。

## 13. Independent Review
- 尚未提交评审。
