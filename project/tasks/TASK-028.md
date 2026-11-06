# TASK-028: 归档恢复 + 编辑习惯 + 最小统计

- 状态：`IMPLEMENTED`（`ADR-015`：实现与真机自测完成，未经独立评审）
- 阶段：`Gate 3`
- 优先级：`P0`（MVP 收口）
- 依赖：`TASK-027`
- 创建日期：`2026-11-05`
- 完成日期：`2026-11-05`（实现完成）

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
- [x] 归档后能在「我的」页看到并一键恢复，恢复后重新出现在今日页（真机走通）
- [ ] 编辑名称/目标/单位（**本切片未做**，留给 `TASK-029`）
- [ ] 统计页最近 7 天完成率（**本切片未做**，留给 `TASK-029`）
- [x] 每条新断言都有能被变异打红的证据：M-RESTORE 删掉本地重建 → `theRestoreRebuildDoesNotWaitForTheRepositoryFlow` 变红

## 7. Risks
- `updateHabit` 会让 `target_snapshot` 语义变敏感：已存在的日志必须保持旧快照，否则历史完成率会被追溯性改写。

## 8. Notes
- 开工前先把 `TASK-026` §2 的假设（删 `addCallback` 后仍全绿）实测掉，避免在一个未验证的前提上建新功能。

## 9. Implementation Log
- 2026-11-05：本切片只做「归档恢复」——它是 `TASK-027` 留下的单向门（误触即消失，只能重装）。编辑与统计拆到 `TASK-029`。
- 2026-11-05：`HabitDao.observeArchived()` + `HabitRepository.observeArchivedHabits()` + `RoomHabitRepository` 实现；`verify-dao.mjs` 补 4 项场景（27 → 31），`MIN_CHECKS` 同步 27 → 31。
- 2026-11-05：`RestoreHabitUseCase` 要求「存在且已归档」，因此重复恢复会报错而不是静默成功；`ProfilePresenter.restore` 成功后本地重建列表。
- 2026-11-05：变异 M-RESTORE（删掉本地重建）→ `Tests run: 115, Failures: 1`，测试有判别力。
- 2026-11-05：真机走查：建 `Walk` → 归档 → 「我的」列出两条归档（`Walk`、`Read`）→ 点 `Walk` 的恢复 → 「我的」只剩 `Read` → 「今日」重新出现 `Walk`。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/run-unit-tests.ps1` | **OK (115 tests)** | 110 → 115：只列归档、恢复后回到活动、重复恢复报错、静默 Flow、空态文案 |
| 变异 M-RESTORE：删掉 `restore()` 的本地重建 | **FAILED（1 项）** | `theRestoreRebuildDoesNotWaitForTheRepositoryFlow` |
| `node tools/verify-dao.mjs` | **DAO SQL PASS**（31 项） | 新增归档/恢复场景，`MIN_CHECKS` 27 → 31 |
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS** | 治理 22/22、单测 115、DAO 31、构建通过 |
| 真机：归档 → 我的页 → 恢复 → 今日页 | **通过** | 两条归档只剩一条，`Walk` 回到今日列表 |

## 11. Completion Review
- [x] 归档恢复切片验收满足
- [x] 测试实际运行
- [ ] 编辑与统计（拆到 `TASK-029`）
- [ ] 独立 Reviewer `APPROVE`（`ADR-015` 批量评审）

## 12. Handoff
- 之后：提醒 `F-012`/`F-013`，备份 `F-014`。

## 13. Independent Review
- Reviewer record：待创建
- 结论：`PENDING`（`IMPLEMENTED`，仅覆盖归档恢复切片）

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-028
- 状态：`IMPLEMENTED`
- 阶段：Gate 3
- 验收：通过 2 项 / 未通过 2 项
- 最近测试记录：2026-11-05 / **DAO SQL PASS**（31 项）
<!-- close-task:end -->
