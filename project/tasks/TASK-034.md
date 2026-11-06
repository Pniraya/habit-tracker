# TASK-034: 统计页滚动、导航栏自动隐藏、删除习惯、归档按月日分组

- 状态：`IMPLEMENTED`（`ADR-015`：实现与真机自测完成，未经独立评审）
- 阶段：`Gate 3`
- 优先级：`P0`
- 依赖：`TASK-032`
- 创建日期：`2026-11-06`
- 完成日期：`2026-11-06`

## 1. Objective
修掉统计页被底部导航挡住「本周趋势」的问题；点空白处让导航栏下滑隐藏；新增永久删除习惯（二次确认）；「我的」页把归档习惯按「月 → 日」父子级展示。

## 2. Context
- 统计页是固定高度 `Column`，热力图 + 趋势卡在 390×844 画板上超出可视区，最后一张卡被裁掉，看起来像被导航栏遮挡。
- 用户要求「单击任意位置导航栏下拉至消失」，属可选交互增强。
- `DATABASE.md` §9 已定义永久删除：显式确认后在单一 transaction 内删除子表与主表（`ON DELETE CASCADE` 已建）。此前只有归档，没有删除入口。
- 归档列表此前是平铺一行一条；用户要求「归档后的习惯存储结构为月份/日期的父子级」。
- `F-012` 的「每日提醒」开关仍未做，本任务不涉及。

## 3. Scope
- 统计页改为可滚动，底部留 16dp，趋势卡完整可见。
- 底部导航：仅在三个顶层路由显示；点击内容区空白处（非交互子组件）切换显示，带下滑/收起动画；切回顶层 tab 时自动恢复。
- 删除习惯：`HabitDao.deleteById` + `HabitRepository.deleteHabit` + `DeleteHabitUseCase` + 今日页 `⋮ → 删除` + 二次确认弹窗（说明记录不可恢复，并提示可用归档）。
- 归档列表：新增纯函数 `ArchiveGroups.group`，按「年-月 → 日」两级分组，月份/日期倒序、同日内按名称排序；「我的」页按父/子标题渲染。
- 单测：`ArchiveGroupsTest`（5 项）、`DeleteHabitUseCaseTest`（3 项）。

## 4. Out of Scope
- **数据库结构改成月/日父表**：`DATABASE.md` 8.3 规定 `archived_on` 是统计区间上界的唯一依据，归档不迁移子记录；把月/日做成物理父表会重复派生数据并需要迁移与 ADR。本任务按「展示层父子级」实现，数据仍由 `archived_on` 派生。
- 删除前的回收站/撤销：`DATABASE.md` §9 明确 V1 不做软删除回收站。
- 提醒相关清理（`F-012` 未实现，无 `PendingIntent` 需要取消）。
- 统计页周/年切换（`TASK-031` 已知缺口）。

## 5. Expected Files
- 新增：`domain/usecase/DeleteHabitUseCase.kt`、`feature/profile/ArchiveGroups.kt`、`ArchiveGroupsTest.kt`、`DeleteHabitUseCaseTest.kt`、本文件
- 修改：`HabitDao.kt`、`HabitRepository.kt`、`RoomHabitRepository.kt`、`FakeHabitRepository.kt`、`TodayPresenter.kt`、`TodayViewModel.kt`、`TodayScreen.kt`、`AppRoot.kt`、`StatisticsScreen.kt`、`ProfileScreen.kt`、`Color.kt`、`FEATURE_MATRIX.md`、`PROJECT_STATUS.md`、`README.md`、`TEST_REPORT.md`、`LAST_HANDOFF.md`

## 6. Acceptance Criteria
- [x] 统计页可滚动，滚到底时「本周趋势」完整显示在导航栏之上
- [x] 点内容区空白处导航栏下滑收起，再点一次恢复；切 tab 自动恢复
- [x] 新建页等非顶层路由不显示导航栏（沿用 `TASK-032`）
- [x] `⋮` 菜单有「删除」，点击先弹二次确认；确认后行立即消失
- [x] 删除走 `ON DELETE CASCADE`，习惯的日志/计划/提醒一并删除（单条 `DELETE` 原子完成）
- [x] 归档页按「2026 年 11 月 → 11 月 6 日 → 习惯」两级父子结构展示，空态文案不变
- [x] 单测 145 项通过（+8），构建通过
- [x] 真机走通：滚动统计页、导航栏收起/恢复、删除临时习惯、归档分组展示

## 7. Tests
- 单元测试：`ArchiveGroupsTest` 5 项（月为父/日为子、倒序、同日按名排序、缺归档日忽略、空归档）；`DeleteHabitUseCaseTest` 3 项（删除主表与子记录、已归档仍可删、缺失 id 拒绝）
- 构建/静态检查：`compileDebugKotlin`、`compileDebugUnitTestKotlin`、`assembleDebug`、`tools/run-unit-tests.ps1`、`tools/verify-all.ps1`
- 真机（iQOO Neo7 SE / Android 15）：统计页滚动前后截图；空白处点击后导航栏消失；创建 `T` → 删除 → 列表恢复 3 条；归档 `Wa't'er` → 「我的」显示月份/日期两级 → 恢复

## 8. Risks and Decisions
- **「父子级存储」的语义**：按展示层树实现（`ArchiveGroups` 纯函数）。若要改成物理父表，需要 ADR + schema 迁移，且与 `DATABASE.md` 8.3「`archived_on` 唯一依据」冲突，不在本任务内。
- **点空白处收起的可发现性**：交互不显眼，但按用户要求实现；恢复路径是「再点一次空白处」或切换 tab 自动恢复。
- **父级 `clickable` 的副作用**：`AppRoot` 在 `NavHost` 外层加无涟漪 `clickable`，子组件会先消费点击，因此卡片/按钮行为不变；滚动手势由子级列表消费。已在真机验证点击行、FAB、tab 均正常。
- **删除真机验证的是无日志的临时习惯**：级联删除由 schema 的 `ON DELETE CASCADE` 与 `verify-schema/verify-dao` 覆盖，未在真机对带历史日志的习惯做删除。
- 统计页未处理周/年标签（显式变淡），与 `TASK-031` 一致。

## 9. Implementation Log
- 2026-11-06：统计页由固定 `Column` 改为 `verticalScroll`，底部留 16dp；真机滚到底后趋势卡完整可见。
- 2026-11-06：`AppRoot` 用 `AnimatedVisibility(slideOutVertically + shrinkVertically + fadeOut)`，由外层无涟漪 `clickable` 切换；`LaunchedEffect(currentRoute)` 保证切 tab 时恢复。
- 2026-11-06：`HabitDao.deleteById` 单条 `DELETE`，子表靠 `ON DELETE CASCADE`；`DeleteHabitUseCase` 先校验存在性，UI 弹二次确认并建议优先归档。
- 2026-11-06：归档分组实现为纯函数 + 展示层标题，不动物理 schema；单据「11 月 6 日 → Wa't'er」真机可见。
- 2026-11-06：第一次写 `ArchiveGroupsTest` 时把同日顺序断言成插入顺序，测试按名称排序失败，改为断言名称顺序（实现是对的）。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/run-unit-tests.ps1` | **PASS** | `OK (145 tests)`：137 → 145（分组 5、删除 3） |
| `gradlew.bat assembleDebug` | **PASS** | `BUILD SUCCESSFUL`，10s |
| 真机：统计页滚动 | **PASS** | 滚动后「本周趋势」完整显示，未被导航栏遮挡（`stats_top.png`/`stats_bottom.png`） |
| 真机：空白处点击收起导航栏 | **PASS** | 点击后 UI 树中无「今日/统计/我的」节点，再点恢复（`bar_hidden2.png`） |
| 真机：删除习惯 | **PASS** | 建 `T` → 菜单「删除」→ 二次确认「删除「T」？」→ 列表回到 3 条（`delete_dialog.png`/`after_delete.png`） |
| 真机：归档按月日分组 | **PASS** | 归档 `Wa't'er` 后「我的」显示「2026 年 11 月 / 1 个」→「11 月 6 日」→ 行（`profile_grouped.png`），随后恢复 |
| `node tools/verify-dao.mjs` | **DAO SQL PASS**（38 项） | 触发 `unexpected @Query` 告警后补 `HabitDao.deleteById` 场景：删除 1 行、三张子表级联清零、未知 id 0 行；`MIN_CHECKS` 37 → 38 |
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS**（exit 0） | 治理 22/22、单测 145、DAO 38、构建、`git diff --check` |

## 11. Completion Review
- [x] 验收标准满足
- [x] 测试实际运行
- [ ] 独立 Reviewer `APPROVE`（`ADR-015` 批量评审）

## 12. Handoff
- 当前剩余工作：归档分组的真机证据只覆盖单月单日；多月份数据未造。
- 下一条安全操作：`TASK-033`（习惯详情页）或 `TASK-030`（备份 + 权限清理）。
- Git 分支/提交：`feat/ui-interactions`（`IMPLEMENTED` 合入，`ADR-015`）。

## 13. Independent Review
- Reviewer record：待创建（批量评审）
- 结论：`PENDING`（`IMPLEMENTED`）

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-034
- 状态：`IMPLEMENTED`
- 阶段：Gate 3
- 验收：通过 8 项 / 未通过 0 项
- 最近测试记录：2026-11-06 / **OK (145 tests)**
<!-- close-task:end -->
