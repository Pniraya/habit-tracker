# TASK-027: 今日页操作闭环（撤销 + 归档）

- 状态：`IMPLEMENTED`（`ADR-015` 批量评审期：实现与真机自测完成，未经独立评审）
- 阶段：`Gate 3`
- 优先级：`P0`（MVP 主路径）
- 依赖：`TASK-021`、`TASK-022`、`TASK-024`
- 创建日期：`2026-11-05`
- 完成日期：`2026-11-05`（实现完成）

## 1. Objective
让今日页从「只能新增和打卡」变成「能撤销、能归档」，这是 MVP 里用户每天都会用到的两个动作。

## 2. Context
- `ArchiveHabitUseCase` 与 `HabitRepository.archiveHabit` 早已存在并有 DAO 场景覆盖，但**没有任何 UI 入口**：习惯一旦建错就永远留在列表里（`F-004` 长期 `IN_PROGRESS`）。
- 撤销在数据层本来就能表达（`record 0.0`），但没有任何提示告诉用户「再点一下可以撤销」。

## 3. Scope
- `TodayPresenter.archive(habitId)`：调用 `ArchiveHabitUseCase`，成功后本地重建列表，不等待仓库 Flow。
- `TodayPresenter.undoHint(isCompleted)`：完成态才显示「再点一下撤销」。
- `TodayViewModel` 暴露 `archive`，工厂注入 `ArchiveHabitUseCase`。
- `TodayScreen`：每行右侧 `⋮` → 下拉菜单「归档」；完成态副标题改为提示撤销。

## 4. Out of Scope
- 编辑习惯名称/目标（`F-003`，需新增 `UPDATE` 语句与 `verify-dao` 场景）。
- 归档后的「已归档」列表与恢复入口（`restoreHabit` 已有，UI 未做）。
- 数值习惯「清零」的专门按钮（现在通过重新输入 0 实现）。

## 5. Expected Files
- `feature/today/TodayPresenter.kt`、`TodayViewModel.kt`、`TodayScreen.kt`、`navigation/AppRoot.kt`
- `TodayPresenterTest.kt`、`FakeHabitRepository.kt`（改为 `open` + `snapshotHabits()`）

## 6. Acceptance Criteria
- [x] 点 `⋮ → 归档` 后该习惯从今日列表消失，其他习惯不受影响
- [x] 重复归档同一习惯会上报错误，而不是静默成功
- [x] 已完成的布尔习惯再点一次回到未完成（撤销），副标题提示「再点一下撤销」
- [x] 「不等待仓库 Flow」这一行为有**能被变异打红**的测试（见 §10 变异 M-ARCH）
- [x] 真机走查：建习惯 → 归档 → 列表回到空状态

## 7. Risks
- 归档没有二次确认：误触会直接让习惯消失（数据仍在库里，但没有恢复入口）。**这是当前最大的可用性缺口**，恢复入口列在 `TASK-028`。
- 数值习惯的「撤销」不直观：要重新输入 0。

## 8. Notes
- 真机第一次安装后列表为空，是因为 `connectedDebugAndroidTest` 结束时会卸载应用并清空数据——不是数据丢失缺陷。

## 9. Implementation Log
- 2026-11-05：`TodayPresenter` 增加 `archive()`；**首次实现只依赖仓库 Flow 移除行**，变异 M-ARCH（删掉本地重建）**没有变红**，说明测试证明的是 Flow 而不是我写的代码。
- 2026-11-05：补 `theArchiveRebuildDoesNotWaitForTheRepositoryFlow`：用一个「只发射一次」的假仓库，使本地重建成为唯一能让行消失的路径。再跑 M-ARCH，`Tests run: 110, Failures: 1` —— 变红。
- 2026-11-05：真机（iQOO Neo7 SE / Android 15）走查：建 `Read` → `⋮` 菜单出现「归档」→ 点击后列表回到空状态；截图在 `.gradle/shots/`（不入库）。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/run-unit-tests.ps1` | **OK (110 tests)** | 105 → 110：归档移除、重复归档报错、撤销、提示文案、静默 Flow 各 1 |
| 变异 M-ARCH：删掉 `archive()` 里的本地重建 | **FAILED（1 项）** | `theArchiveRebuildDoesNotWaitForTheRepositoryFlow`；修复前同变异**全绿**，证明原测试无判别力 |
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS** | 治理 22/22、单测 110、DAO 27、构建通过 |
| 真机：建习惯 → 归档 | **通过** | 菜单「归档」→ 列表回空；活动栈显示前台仍是 `com.habittracker`（`dumpsys activity top` 抓到的其它条目是系统后台进程） |

## 11. Completion Review
- [x] 验收标准满足
- [x] 测试实际运行
- [ ] 独立 Reviewer `APPROVE`（`ADR-015` 批量评审时补）

## 12. Handoff
- 之后：`TASK-028` 归档恢复入口 + 编辑习惯（`F-003`）+ 统计页最小版（`F-010`）。

## 13. Independent Review
- Reviewer record：批量评审时创建（`project/reviews/BATCH_REVIEW.md`）
- 结论：`PENDING`（`IMPLEMENTED` 状态，未评审）

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-027
- 状态：`IMPLEMENTED`
- 阶段：Gate 3
- 验收：通过 5 项 / 未通过 0 项
- 最近测试记录：2026-11-05 / **OK (110 tests)**
<!-- close-task:end -->
