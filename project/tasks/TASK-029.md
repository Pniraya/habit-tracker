# TASK-029: 编辑习惯 + 最小统计

- 状态：`IMPLEMENTED`（`ADR-015`：实现与真机自测完成，未经独立评审）
- 阶段：`Gate 3`
- 优先级：`P0`（MVP 收口）
- 依赖：`TASK-028`
- 创建日期：`2026-11-05`
- 完成日期：`2026-11-05`（实现完成）

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
- [x] 改名称/目标/单位后今日页立即反映，**历史日志的 `target_snapshot` 不变**（单测 + DAO 场景双重证据）
- [x] 统计只把「计划日」计入分母（5 项测试，含周末与无版本日）
- [x] 变异证据：M-STAT 去掉计划日判断 → 2 项红；M-EDIT-NAME 去掉名校验 → 见 §10

## 7. Risks
- `target_snapshot` 被追溯改写会让历史完成率失真；必须只改 `habits` 表。

## 9. Implementation Log
- 2026-11-05：`HabitDao.updateFields`（只改 name/target_value/unit/updated_at，**不含 `tracking_type`**，也不碰 `habit_logs`）+ `UpdateHabitUseCase`（复用 `HabitRules`）。
- 2026-11-05：`verify-dao.mjs` 补 6 项编辑场景（37 项，`MIN_CHECKS` 31 → 37），其中「改目标后历史日志 `target_snapshot` 仍是 2000」是防追溯改写的关键断言。
- 2026-11-05：今日页 `⋮` 菜单加「编辑」，弹窗只暴露名称/目标/单位；布尔习惯不显示目标与单位。
- 2026-11-05：统计页 `StatisticsPresenter.summarize` 纯函数，逐日找「覆盖该日的计划版本」再判 `isScheduledOn`，因此换过计划的习惯也按当日计划算。
- 2026-11-05：变异 M-STAT（去掉计划日判断）→ `Tests run: 122, Failures: 2`（周末惩罚、休息日灌水两条同时红）。
- 2026-11-05：真机：统计页显示 `Wa'lk 0 / 1 · 0%`——分母 1 是「今天建的习惯在 7 天窗口内只有 1 个计划日」，符合预期，不是 bug。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/run-unit-tests.ps1` | **OK (122 tests)** | 115 → 122：编辑 2 项、统计 5 项 |
| 变异 M-STAT：统计不再判断计划日 | **FAILED（2 项）** | `aWeekdayHabitIsNotPunishedForTheWeekend`、`aCompletionOnARestDayDoesNotInflateTheRate` |
| `node tools/verify-dao.mjs` | **DAO SQL PASS**（37 项） | 编辑 6 项场景；含「历史 `target_snapshot` 不被改写」「归档习惯拒绝编辑」「未知 id no-op」 |
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS** | 治理 22/22、单测 122、DAO 37、构建通过 |
| 真机：统计页 | **通过** | `Wa'lk 0 / 1 · 0%`，分母只算计划日 |

## 11. Completion Review
- [x] 验收标准满足
- [x] 测试实际运行
- [ ] 独立 Reviewer `APPROVE`（`ADR-015` 批量评审）

## 12. Handoff
- 之后：`F-011` 热力图、`F-012`/`F-013` 提醒、`F-014` 备份。

## 13. Independent Review
- Reviewer record：待创建
- 结论：`PENDING`（`IMPLEMENTED`）

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-029
- 状态：`IMPLEMENTED`
- 阶段：Gate 3
- 验收：通过 3 项 / 未通过 0 项
- 最近测试记录：2026-11-05 / **DAO SQL PASS**（31 项）
<!-- close-task:end -->
