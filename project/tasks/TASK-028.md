# TASK-028: 归档恢复 + 编辑习惯 + 最小统计

- 状态：`NOT_STARTED`
- 阶段：`Gate 3`
- 优先级：`P0`（MVP 收口）
- 依赖：`TASK-027`
- 创建日期：`2026-11-05`
- 完成日期：`TBD`

## 1. Objective
把 MVP 剩下的三处硬缺口补齐：归档之后**找得回来**、习惯建错之后**改得动**、以及一个能看的最小统计。

## 2. Context
- `TASK-027` 给了归档入口，但没有恢复入口：误触一次，习惯就从列表里消失，用户只能卸载重装。这是当前最严重的可用性问题。
- `F-003` 编辑仍未实现，`HabitRepository` 没有更新习惯字段的方法。
- `StatisticsScreen` 仍是占位文字（`F-010`/`F-011` 未开始）。

## 3. Scope
- 「我的」页列出已归档习惯，提供恢复（`restoreHabit` 已有）。
- 新增 `updateHabit`（名称/目标值/单位）与 `verify-dao.mjs` 场景；`HabitRules` 校验复用。
- 今日页菜单增加「编辑」，复用新建表单的校验逻辑。
- 最小统计：近 7 天「计划天数 / 完成天数 / 完成率」，按习惯列出。

## 4. Out of Scope
- 热力图（`F-011` 完整版）。
- 提醒与备份（`F-012`~`F-014`）。

## 5. Expected Files
- `data/local/dao/HabitDao.kt`（新增 `@Query`）、`tools/verify-dao.mjs`（新场景）
- `domain/repository/HabitRepository.kt`、`data/repository/RoomHabitRepository.kt`、`domain/usecase/UpdateHabitUseCase.kt`
- `feature/profile/ProfileScreen.kt`、`feature/today/*`、`feature/statistics/StatisticsScreen.kt`

## 6. Acceptance Criteria
- [ ] 归档后能在「我的」页看到并一键恢复，恢复后重新出现在今日页
- [ ] 编辑名称/目标/单位后，今日页立即反映新值，历史日志的 `target_snapshot` 不被改写
- [ ] 统计页显示最近 7 天的完成率，且分母只算「计划日」
- [ ] 每条新断言都有能被变异打红的证据

## 7. Risks
- `updateHabit` 会让 `target_snapshot` 语义变敏感：已存在的日志必须保持旧快照，否则历史完成率会被追溯性改写。

## 8. Notes
- 开工前先把 `TASK-026` §2 的假设（删 `addCallback` 后仍全绿）实测掉，避免在一个未验证的前提上建新功能。

## 9. Implementation Log
- 尚未开始。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | 未运行（任务未开始） | 开工后补真实结果 |

## 11. Completion Review
- [ ] 验收标准满足
- [ ] 测试实际运行
- [ ] 独立 Reviewer `APPROVE`

## 12. Handoff
- 之后：提醒 `F-012`/`F-013`，备份 `F-014`。

## 13. Independent Review
- Reviewer record：待创建
- 结论：`PENDING`