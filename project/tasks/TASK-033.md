# TASK-033: 习惯详情页（按设计稿 Detail.dc.html）

- 状态：`NOT_STARTED`
- 阶段：`Gate 4`
- 优先级：`P1`
- 依赖：`TASK-032`
- 创建日期：`2026-11-06`
- 完成日期：`TBD`

## 1. Objective
新增习惯详情页，按 `orgins/习惯详情-html/Detail.dc.html` 展示单个习惯的连续天数、最近记录与编辑入口。

## 2. Context
设计稿已存在，但今日页的行点击只做打卡/数值记录，没有任何进入详情的入口；`F-008`（修改历史记录）与 `F-011`（热力图）也因此缺少落点。`TASK-032` 已建立设计系统（`core/designsystem/Ui.kt`）与图标资产，可以直接复用。

## 3. Scope
- 新增 `feature/detail`：Presenter + Screen，读取单个习惯的日志历史。
- 今日页行标题点击进入详情（勾选圈仍直接打卡）。
- 详情页复用 `SoftCard`/`ProgressRing`/`StreakBadge`；返回按钮样式同新建页。

## 4. Out of Scope
- 修改历史某天的记录（`F-008` 剩余部分）：先在详情页只读展示。
- 统计页的周/年切换与热力图合并。

## 5. Expected Files
- 新增：`feature/detail/HabitDetailScreen.kt`、`HabitDetailPresenter.kt`、`HabitDetailUiState.kt`、对应单测、本文件
- 修改：`navigation/AppRoutes.kt`、`AppRoot.kt`、`TodayScreen.kt`、`FEATURE_MATRIX.md`（`F-011`）、`PROJECT_STATUS.md`、`README.md`、`TEST_REPORT.md`、`LAST_HANDOFF.md`

## 6. Acceptance Criteria
- [ ] 详情页展示习惯名、当前/最长连续、最近 30 天记录
- [ ] 空历史与已归档习惯有明确状态
- [ ] 今日页可进入详情并返回，返回后列表状态不丢
- [ ] 单测覆盖 presenter 的聚合规则；构建通过

## 7. Tests
- 单元测试：presenter 聚合规则
- 构建/静态检查：`assembleDebug`、`tools/run-unit-tests.ps1`、`tools/verify-all.ps1`
- 真机验证：接设备后手动对照设计稿（当前 `adb devices` 为空）

## 8. Risks and Decisions
- 设计稿的「修改历史记录」按钮若无法实现编辑，需显式禁用并说明，不摆假入口。
- 日志读取需避免 N+1：参考 `TASK-022` 的批量读做法。
- 设计稿为只读资产（`AGENTS.md` §11），只作视觉参考。

## 9. Implementation Log

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | 尚未开始（NOT_STARTED） | 任务未开工，无证据 |

## 11. Completion Review
- [ ] 验收标准全部满足
- [ ] 测试实际运行
- [ ] `git diff --check` 通过
- [ ] Diff 已审查，无无关改动
- [ ] 文档/状态已更新
- [ ] 未完成项已明确记录

## 12. Handoff
- 当前剩余工作：全部
- 下一条安全操作：先跑 `tools/verify-all.ps1` 确认 `main` 绿，再读设计稿 `Detail.dc.html`
- Git 分支/提交：无

## 13. Independent Review
- Reviewer record：未开始
- 结论：`PENDING`
