# TASK-033: 习惯详情页（按设计稿 Detail.dc.html）

- 状态：`IMPLEMENTED`（`ADR-015`：实现与真机自测完成，未经独立评审）
- 阶段：`Gate 3`
- 优先级：`P1`
- 依赖：`TASK-032`
- 创建日期：`2026-11-06`
- 完成日期：`2026-11-06`

## 1. Objective
新增习惯详情页，按 `orgins/习惯详情-html/Detail.dc.html` 展示单个习惯的当前/最长连续、本周完成、近 5 周网格、三张统计卡与计划信息，并提供编辑入口。

## 2. Context
设计稿已存在，但今日页的行点击只做打卡/数值记录，没有任何进入详情的入口；`F-011` 的每习惯热力图因此没有落点。`TASK-032` 已建立设计系统（`SoftCard`/`ProgressRing`/图标资产），可直接复用。

## 3. Scope
- 新增 `feature/detail`：`HabitDetailRules`（纯规则）、`HabitDetailPresenter`、`HabitDetailScreen`。
- 导航：`AppRoutes.HABIT_DETAIL = "habit/{habitId}"` + `habitDetail(id)`；详情页为全屏路由，不显示底部导航（沿用 `TASK-032` 的规则）。
- 今日页行标题点击进入详情；**勾选圈仍是打卡/数值录入入口**（行为调整）。
- 编辑铅笔：弹出与今日页同字段的编辑弹窗，保存后刷新详情。
- 单测：`HabitDetailRulesTest` 9 项。

## 4. Out of Scope
- 修改历史某天的记录（`F-008` 剩余部分）：详情页只读展示。
- 提醒时间（`F-012` 未实现）：设计稿的「提醒 21:30」不做，计划行只显示频率与目标。
- 统计页周/年切换（`TASK-031` 已知缺口）。

## 5. Expected Files
- 新增：`feature/detail/HabitDetailRules.kt`、`HabitDetailPresenter.kt`、`HabitDetailScreen.kt`、`HabitDetailRulesTest.kt`、`res/drawable/ic_arrow_back.xml`、`ic_edit.xml`、`ic_calendar.xml`、本文件
- 修改：`navigation/AppRoutes.kt`、`AppRoot.kt`、`feature/today/TodayScreen.kt`、`FEATURE_MATRIX.md`、`PROJECT_STATUS.md`、`README.md`、`TEST_REPORT.md`、`LAST_HANDOFF.md`

## 6. Acceptance Criteria
- [x] 今日页点行标题进入详情，返回后列表状态不丢；勾选圈仍直接打卡
- [x] 连续卡：当前连续大数字 + 「天」、最长/本月次数、72dp 杏橙叶片圆
- [x] 本周卡：周一到周日 7 个 40dp 圆点（完成 = 杏橙 + 勾，未完成 = 暖灰），今天描边、标签变「今」
- [x] 近 5 周卡：周一开头的 5×7 网格、图例、今天描边；未来日与「未计划」日不显示为失败
- [x] 三张统计卡：累计次数、近 5 周完成率、本月次数
- [x] 计划行：频率 + 目标（每天 · 完成 / 未完成）
- [x] 编辑铅笔弹窗可保存并刷新页面
- [x] 习惯不存在时有明确状态，不崩溃
- [x] 单测 154 项通过（+9），构建通过
- [x] 真机（iQOO Neo7 SE）：进入详情、打卡后 1/1 与 100%、编辑弹窗、返回均走通

## 7. Tests
- 单元测试：`HabitDetailRulesTest` 9 项（周边界、未计划日、首版本前、5 周对齐、完成率按应做天数、零除、全历史累计、频率文案、版本区间）
- 构建/静态检查：`compileDebugKotlin`、`assembleDebug`、`tools/run-unit-tests.ps1`、`tools/verify-all.ps1`
- 真机：行点击进入详情；打卡后满格/勾点/100%；编辑弹窗字段齐全；返回今日页

## 8. Risks and Decisions
- **第三张卡改为「本月次数」**：设计稿是「每次平均 22 分钟」，但 `habit_logs.duration_seconds` 从未被写入，展示会变成假数据；改用与连续卡一致的「本月 N 次」。
- **计划行不含提醒时间**：`F-012` 未实现，没有真实提醒可显示，故去掉铃铛与时间。
- **5 周网格按周一对齐**：设计稿是「35 个格子平铺 + 今天描边」，在非周日会与星期表头错位；实现改为 5 个完整周，最后一行含未来日（浅色，不算失败）。
- **行点击语义变更**：设计稿里勾选圈是唯一记录动作；行点击改为进入详情。数值习惯仍通过勾选圈弹输入框。
- 详情页可滚动（设计稿为固定 844 画板）。
- 真机数据只有今天一个计划日，因此「本周 0/1、近 5 周仅今天」是数据事实，不是渲染缺陷。

## 9. Implementation Log
- 2026-11-06：先写纯规则（`HabitDetailRules`）与 9 项测试，再接 presenter / 页面；规则只依赖 `HabitSchedule` + 完成日期集合，不碰数据库。
- 2026-11-06：presenter 从**第一个计划版本**开始读日志，保证连续天数与累计次数不被五周窗口截断（`HabitDetailRulesTest.theStreakUsesTheWholeHistoryNotOnlyTheVisibleWindow`）。
- 2026-11-06：首次测试两处失败——「周六在周五之后应为 FUTURE 而非 NOT_DUE」、「5 周完成率分母是整窗口应做天数而不是本周 5 天」，均为测试期望写错，规则实现正确，修正期望。
- 2026-11-06：真机进入详情看到 `0/1`（习惯今天才创建，计划从今天生效）；打卡后重进为 `1/1`、100%、累计 1，验证数据链路。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/run-unit-tests.ps1` | **PASS** | `OK (154 tests)`：145 → 154（`HabitDetailRulesTest` 9） |
| `gradlew.bat assembleDebug` | **PASS** | `BUILD SUCCESSFUL` |
| 真机：今日页 → 详情 → 打卡 → 详情 | **PASS** | `detail_top.png`（0/1、0%）、`detail_done.png`（1/1、100%、累计 1、今天勾点） |
| 真机：编辑弹窗 + 返回 | **PASS** | 弹窗含「名称/保存/取消」；两次返回回到今日页（`detail_edit.png`） |
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS**（exit 0） | 治理 22/22、单测 154、DAO 38、构建、`git diff --check` |

## 11. Completion Review
- [x] 验收标准满足
- [x] 测试实际运行
- [ ] 独立 Reviewer `APPROVE`（`ADR-015` 批量评审）

## 12. Handoff
- 当前剩余工作：`F-008` 修改历史记录仍只读；详情页的真机数据只有单日一个计划日，多周网格仅有单测证据。
- 下一条安全操作：`TASK-030`（备份导出 + 权限清理）。
- Git 分支/提交：`feat/habit-detail`（`IMPLEMENTED` 合入，`ADR-015`）。

## 13. Independent Review
- Reviewer record：待创建（批量评审）
- 结论：`PENDING`（`IMPLEMENTED`）

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-033
- 状态：`IMPLEMENTED`
- 阶段：Gate 3
- 验收：通过 10 项 / 未通过 0 项
- 最近测试记录：2026-11-06 / **OK (154 tests)**
<!-- close-task:end -->
