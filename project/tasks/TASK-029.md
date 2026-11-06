# TASK-029: 编辑习惯 + 最小统计

- 状态：`NOT_STARTED`
- 阶段：`Gate 3`
- 优先级：`P0`（MVP 收口）
- 依赖：`TASK-028`
- 创建日期：`2026-11-05`
- 完成日期：`TBD`

## 1. Objective
MVP 最后两块：习惯建错能改（`F-003`），以及一个能看的最小统计（`F-010` 子集）。

## 2. Context
- 编辑功能完全缺失：`HabitRepository` 没有更新习惯字段的方法，建错了只能归档重建，历史全丢。
- 统计页仍是占位文字。

## 3. Scope
- `HabitDao.updateFields` + `HabitRepository.updateHabit` + `UpdateHabitUseCase`（复用 `HabitRules` 校验）；`verify-dao` 补场景。
- 今日页菜单增加「编辑」，弹窗改名称/目标值/单位。
- 统计页：每个习惯最近 7 天的「完成天数 / 计划天数 / 完成率」。

## 4. Out of Scope
- 热力图完整版、提醒、备份。

## 5. Expected Files
- `data/local/dao/HabitDao.kt`、`tools/verify-dao.mjs`、`domain/repository/HabitRepository.kt`、`RoomHabitRepository.kt`、`domain/usecase/UpdateHabitUseCase.kt`
- `feature/today/*`、`feature/statistics/StatisticsScreen.kt`、`feature/statistics/StatisticsPresenter.kt`

## 6. Acceptance Criteria
- [ ] 改名称/目标/单位后今日页立即反映，**历史日志的 `target_snapshot` 不变**（有测试）
- [ ] 统计只把「计划日」计入分母（有测试，含周计划与停工日）
- [ ] 每条新断言都有能被变异打红的证据

## 7. Risks
- `target_snapshot` 被追溯改写会让历史完成率失真；必须只改 `habits` 表。

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
- 之后：`F-011` 热力图、`F-012`/`F-013` 提醒、`F-014` 备份。

## 13. Independent Review
- Reviewer record：待创建
- 结论：`PENDING`