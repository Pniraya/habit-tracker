# TASK-014: 领域规则上移到 UseCase

- 状态：`NOT_STARTED`
- 阶段：`Gate 3`
- 优先级：`P1`
- 依赖：`TASK-012`（领域与仓储现状）、`TASK-013`
- 创建日期：`2026-11-05`
- 完成日期：`TBD`

## 1. Objective
把 `validateName` 与 `normalizeTarget`（`target`/`unit` 校验）从 `RoomHabitRepository` 上移到 `CreateHabitUseCase`，并在 `HabitRepository.createHabit` 的接口契约里写明调用方需保证什么。

## 2. Context
`TASK-012` 的一条测试失败暴露：规则在数据层，UseCase 只校验 `name`，于是任何非 Room 实现都必须自己复述规则，漏一个就静默偏差（`FakeHabitRepository` 当时就漏了）。评审同意这是真实的分层问题，并明确要求单开任务。

## 3. Scope
- `CreateHabitUseCase` 调用 `HabitRules.validateName` 与 `HabitRules.normalizeTarget`，仓库只负责持久化。
- 在 `HabitRepository` 接口用 KDoc 声明契约（是否要求已归一化的输入）。
- `FakeHabitRepository` 恢复为不校验、只记录的哑实现，让 UseCase 的校验成为唯一防线。

## 4. Out of Scope
- 图标/颜色选择、编辑/删除、提醒调度。

## 5. Expected Files
- 待实施时补充

## 6. Acceptance Criteria
- [ ] 删除 UseCase 的校验会让单测变红（而不只是仓库层变红）
- [ ] `FakeHabitRepository` 不再重复实现校验
- [ ] 现有 59 项单测与 DAO 21 项全部通过

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
