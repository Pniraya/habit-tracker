# TASK-012: 新建习惯页与数值输入

- 状态：`PARTIAL`（实现与单测完成，等待独立评审）
- 阶段：`Gate 3`
- 优先级：`P0`
- 依赖：`TASK-011`（Today 页接线）
- 创建日期：`2026-11-05`
- 完成日期：`TBD`

## 1. Objective
让用户能真正加一个习惯并在当天使用：新建表单（名称/类型/目标/单位/频率）、保存后回到 Today；数值习惯可以在 Today 页输入实际值。

## 2. Context
`REQUIREMENTS.md` 的习惯创建与记录行为；`ARCHITECTURE.md` §5.2；`DATABASE.md` §4.2（首个计划版本）。

## 3. Scope
- 新增 `feature/create/{CreateHabitForm,CreateHabitPresenter,CreateHabitScreen}.kt`。
- Today 页：数值习惯点击弹出输入对话框；空态提供「新建习惯」入口。
- `AppRoutes.CREATE_HABIT` 路由与 `AppRoot` 接线。
- 单测覆盖表单校验、mask 计算与保存路径。

## 4. Out of Scope
- 图标/颜色选择器（暂用默认值）。
- 编辑与删除习惯、统计页、提醒、DataStore。
- Compose UI 测试。

## 5. Expected Files
- 新增：`feature/create/*`、`app/src/test/java/com/habittracker/feature/create/CreateHabitPresenterTest.kt`、`project/tasks/TASK-012.md`
- 修改：`feature/today/TodayScreen.kt`、`navigation/{AppRoot,AppRoutes}.kt`、`app/src/test/java/com/habittracker/domain/usecase/FakeHabitRepository.kt`（与生产规则对齐）、状态文档

## 6. Acceptance Criteria
- [x] 表单可输入名称并选择类型/频率，自定义频率可选周几
- [x] 空名、非数字目标、目标 ≤ 0、自定义未选日均在本地拦截，且不触达仓库
- [x] 保存成功返回新建的习惯 id，并生成对应的首个计划版本（mask 正确）
- [x] 数值习惯在 Today 页点击弹出输入框并写入实存值
- [x] Today 空态可进入新建页
- [x] `tools/run-unit-tests.ps1` 通过、`assembleDebug` 通过

## 7. Tests
- `pwsh -NoProfile -File tools/run-unit-tests.ps1`（新增 `CreateHabitPresenterTest` 8 项）
- 变异：把 `isBlank()` 改成 `isEmpty()`，空名测试必须变红
- `gradlew.bat assembleDebug`

## 8. Risks and Decisions
- **发现**：`target`/`unit` 的校验在 `RoomHabitRepository`（`HabitRules.normalizeTarget`），不在 UseCase；因此 `FakeHabitRepository` 原本不校验，测试与生产行为不一致。已让 fake 调用同一个 `normalizeTarget`，保持两边一致。把规则的执行点上移到 UseCase 属于领域重构，另开任务。
- `CreateHabitPresenter` 由 `remember { }` 持有，配置变更（旋转）会丢失表单内容；未引入 `SavedStateHandle`，因为那需要 ViewModel 且本机无法测试。已记录为已知缺陷。
- 图标与颜色暂用固定默认值（`star` / `#5B8C6A`），选择器留待专门任务。

## 9. Implementation Log
- 2026-11-05：新增表单状态、Presenter、页面与路由；Today 页增加数值对话框与新建入口。
- 2026-11-05：首次编译失败 `NewSchedule(frequency = ...)`——参数名是 `frequencyType`，已修正。
- 2026-11-05：独立评审首轮 `REJECT`：`onTrackingTypeChange` 从数值切回布尔时未清 `unit`，而布尔下单位框隐藏 → 表单无法保存且用户无路可退（真缺陷）；另有三份文档未同步、`TEST_REPORT` 行序错乱导致 `close-task` 摘要取到 49 项。
- 2026-11-05：修复：切换布尔时同时清空 `unit`，`draft()` 对布尔强制 `unit = null`；补回归测试 `switchingFromNumericToBooleanClearsTheUnit`；`NumericInputDialog` 本地拒绝负值（原先会关框丢输入）；`TEST_REPORT` 行序改为时间序。
- 2026-11-05：首次测试失败：`numericHabitWithoutUnitIsRefusedByTheUseCase` 断言错误为空——查出 fake 不校验 unit，遂让 fake 与 `RoomHabitRepository` 共用 `normalizeTarget`；测试改名为 `numericHabitWithoutUnitIsRefused` 并去掉「只由 use case 拒绝」的错误声明。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/run-unit-tests.ps1` | **OK (58 tests)** | 新增 `CreateHabitPresenterTest` 9 项（含"数值→布尔切换清空单位"回归） |
| 变异：`isBlank()` → `isEmpty()` | **FAILED（1 项）** | 空名测试变红，证明本地校验被真的执行 |
| `gradlew.bat assembleDebug` | **SUCCESSFUL**（29s） | 首次失败：`NewSchedule` 参数名错误 |
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS**（exit 0） | 治理 21/21（0 警告）、单测 57 项、DAO SQL 21 项、`assembleDebug` 22s、空白 0s |

## 11. Completion Review
- [x] 验收标准全部满足（6/6）
- [x] 相关测试实际运行（58 项 + 2 组变异）
- [x] `git diff --check` 通过
- [x] Diff 已审查：无新依赖、无 schema 改动、无权限变更
- [x] 文档/状态已更新
- [ ] 独立 Reviewer `APPROVE`

## 12. Handoff
- 下一步：评审后合入 `main`；之后补图标/颜色选择与习惯编辑（任务文件尚未创建）。
- 遗留：旋转丢失表单；UI 无自动化测试。

## 13. Independent Review
- Reviewer record：`project/reviews/REVIEW-TASK-012.md`
- 结论：`PENDING`

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-012
- 状态：`PARTIAL`
- 阶段：Gate 3
- 验收：通过 6 项 / 未通过 0 项
- 最近测试记录：2026-11-05 / **OK (58 tests)**
<!-- close-task:end -->
