# TASK-019: 校正功能矩阵与现状

- 状态：`NOT_STARTED`
- 阶段：`Gate 2`
- 优先级：`P1`
- 依赖：无（`TASK-012`~`TASK-018` 已 `DONE` 合入）
- 创建日期：`2026-11-05`
- 完成日期：`TBD`

## 1. Objective
`project/FEATURE_MATRIX.md` 落后于实现：已有代码、单测与真机证据的功能仍标 `NOT_STARTED`，属**低报**。按 AGENTS §6「仅更新有实现/测试证据的状态」逐项对齐。

## 2. Context
`TASK-018` 收尾时只读盘点发现：

| ID | 矩阵当前 | 实际证据 |
|---|---|---|
| F-002 今日页与导航 | `IMPLEMENTED_UNVERIFIED` | 多次真机验证（列表、状态、空状态、FAB） |
| F-003 创建习惯 | `NOT_STARTED` | `CreateHabitScreen` + `CreateHabitPresenterTest` 9 项 + 真机 |
| F-005 DAILY/WEEKDAYS/CUSTOM | `NOT_STARTED` | 创建页三种频率可选，`ScheduleRulesTest` 有断言 |
| F-006 BOOLEAN 日记录 | `NOT_STARTED` | 点击切换 + `LogStatusRulesTest` + 真机 |
| F-007 NUMERIC 日累计 | `NOT_STARTED` | 数值对话框、小数累计、真机验证 |

确未实现、保持 `NOT_STARTED`：F-004（有 `ArchiveHabitUseCase` 但无界面）、F-008、F-009、F-010、F-011、F-012、F-013、F-014。

## 3. Scope
- 只改 `project/FEATURE_MATRIX.md` 的状态与证据列。
- 每行状态必须能指向代码文件、测试名或真机记录。

## 4. Out of Scope
- 不改 `project/REQUIREMENTS.md`；不实现任何功能。

## 5. Expected Files
- 修改：`project/FEATURE_MATRIX.md`
- 更新：`project/PROJECT_STATUS.md`、`README.md`、`project/reports/TEST_REPORT.md`、`project/handoffs/LAST_HANDOFF.md`

## 6. Acceptance Criteria
- [ ] F-002/F-003/F-005/F-006/F-007 状态与实际证据一致
- [ ] 仍为 `NOT_STARTED` 的行确有「无实现」证据（文件不存在或仅占位）
- [ ] 表格无 `TBD`、无空证据格

## 7. Tests
- 本次只改文档，无代码改动；跑 `tools/verify-all.ps1` 确认治理检查与构建未受影响

## 8. Risks and Decisions
- 风险：为了让矩阵「好看」而提前提升状态。对策：每行证据必须可点开核对。
- `F-015 离线核心功能` 需要重新表述（本地库本就离线），本次一并处理。

## 9. Implementation Log
- 2026-11-05：由 `TASK-018` 收尾盘点创建，尚未开始。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | 尚未开始（`NOT_STARTED`） | 任务未开工，开工后补真实结果 |

## 11. Completion Review
- [ ] 验收标准满足
- [ ] 测试实际运行
- [ ] 独立 Reviewer `APPROVE`

## 12. Handoff
- 完成后下一项：`TASK-017`（治理检查加固 P5–P8）。

## 13. Independent Review
- Reviewer record：待创建
- 结论：`PENDING`

