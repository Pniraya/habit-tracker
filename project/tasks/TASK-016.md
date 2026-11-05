# TASK-016: 界面细节收尾

- 状态：`NOT_STARTED`
- 阶段：`Gate 3`
- 优先级：`P2`
- 依赖：`TASK-013`（FAB 与数值输入框）、`TASK-012`
- 创建日期：`2026-11-05`
- 完成日期：`TBD`

## 1. Objective
收掉 `TASK-013` 评审提出的四个非阻塞问题：把看不到依据的魔数变成可推导的量、让数值输入支持小数、修正一处把契约锁写成现实状态的注释、扩大治理检查的占位符词表。

## 2. Context
`TASK-013` 第 1 轮评审（`APPROVE`）逐条给出：
- `TodayScreen` 里 `88.dp`（列表底部留白）、`20.dp`（FAB 边距）与 FAB 默认 `56.dp` 三个数互不引用，余量只有 12dp；换 large FAB 或加标签就会压住最后一行，且没有测试会红。
- `KeyboardType.Number` 通常不提供小数点，而 `TodayHabit.actualValue` 是 `Double`（例如 0.5 升、0.25 小时无法输入）。
- 加载态断言的注释写「the list is not real yet」，但该状态当前不可达，实质是防未来 `refresh()` 的契约锁。
- `check-governance` 第 21 项的正则只认 `TBD|NOT_RUN`，中文「未运行」能通过——与 `TASK-015` 同源。

## 3. Scope
- 用 `FloatingActionButton` 的默认尺寸推导底边留白（或提为常量），让改动时几何关系可读。
- 数值输入改为 `KeyboardType.Decimal`，并在真机确认能输入 `0.5`。
- 修正加载态断言注释与其在 §8 的口径（契约锁，不是可达状态）。
- 与 `TASK-015` 合并处理中文占位符检测（若 `TASK-015` 先做，则本条改为复核）。

## 4. Out of Scope
- 主题、暗色模式、动画、统计页与我的页。

## 5. Expected Files
- `app/src/main/java/com/habittracker/feature/today/TodayScreen.kt`、`TodayPresenterTest.kt`、`tools/check-governance.ps1`（或并入 `TASK-015`）

## 6. Acceptance Criteria
- [ ] 底边留白由 FAB 尺寸 + 边距推导或共用常量，不再出现无依据的 `88.dp`
- [ ] 真机能输入 `0.5` 并落库为 `0.5`
- [ ] 加载态测试注释与实际语义一致（不可达/契约锁）
- [ ] 治理检查能拦住中文「未运行」占位符

## 7. Tests
- 待实施时补充；数值路径至少一条真机走查 + 一条计数断言

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
