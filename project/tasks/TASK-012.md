# TASK-012: 新建习惯页与数值输入

- 状态：`DONE`（独立评审 `APPROVE` 于 `07b1aab`，第 3 轮）
- 阶段：`Gate 3`
- 优先级：`P0`
- 依赖：`TASK-011`（Today 页接线）
- 创建日期：`2026-11-05`
- 完成日期：`2026-11-05`

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
- `pwsh -NoProfile -File tools/run-unit-tests.ps1`（新增 `CreateHabitPresenterTest` 9 项）
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
| 变异：去掉 `onTrackingTypeChange` 的 unit 清空 | **FAILED（1 项）**（预期） | `switchingFromNumericToBooleanClearsTheUnit` 变红，证明修复被回归测试锁住 |
| 变异：去掉 `draft()` 的 BOOLEAN `unit = null` 兜底 | **未被抓住（58 全绿）** | 先清 unit 后该行不可达，属不可证伪的纵深防御，不计为已验证防线 |
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS**（exit 0） | 治理 21/21（0 警告）、单测 58 项、DAO SQL 21 项、`assembleDebug` 19-22s、空白 0s |

## 11. Completion Review
- [x] 验收标准全部满足（6/6；其中「数值习惯弹框写入实存值」仅由 Presenter 测试与真机手动走查支撑，无自动化 UI 测试）
- [x] 相关测试实际运行（58 项 + 2 组变异）
- [x] `git diff --check` 通过
- [x] Diff 已审查：无新依赖、无 schema 改动、无权限变更
- [x] 文档/状态已更新
- [x] 独立 Reviewer `APPROVE`（`07b1aab`，第 3 轮）

## 12. Handoff
- 下一步：评审 `APPROVE` 后合入 `main`；之后的首选任务是**把 `validateName`/`normalizeTarget` 上移到 `CreateHabitUseCase` 并在 `HabitRepository` 接口写明契约**（本任务暴露的分层问题，任务文件尚未创建），其次才是图标/颜色选择与习惯编辑。
- 遗留：旋转丢失表单；UI 无自动化测试。
- 已知 UX 缺口（真机走查发现，未修）：Today 页**仅在空态**提供「新建习惯」入口，有至少一个习惯后无法再新建；修复应另开任务。

## 13. Independent Review
- Reviewer record：`project/reviews/REVIEW-TASK-012.md`
- 第 1 轮（`1f633a8`）：`REJECT` —— 抓到 unit 状态泄漏真缺陷 + 三份文档未同步
- 第 2 轮（`ba96c55`）：`REJECT` —— 代码达标，仅剩计数不一致（README/TEST_REPORT/TASK 证据行）；M-2 兜底行未被变异抓住
- 第 3 轮（`07b1aab`）：`APPROVE` —— 计数全部一致、M-1 复验有效；补记设备 dump 与数据重置说明后合入

## 14. Device Verification（手动走查，非自动化）
- 设备：`iQOO Neo7 SE`（型号 `V2238A`），Android 15（API 35），USB 调试（序列号不记录）
- APK：`app/build/outputs/apk/debug/app-debug.apk`，`adb install -r` 输出 `Success`
- 走查 1（修复前遗留路径的修复后行为）：新建「数值 + 单位 ml + 目标 2000」→ 切回「完成/未完成」→ 单位框隐藏且保存成功 → Today 出现该习惯 → 点击卡片变为「今天已完成」
- 走查 2（落库核对）：`adb exec-out run-as com.habittracker cat databases/habit-tracker.db`（含 `-wal`/`-shm`）后用 `node:sqlite` 读取：
  - `habits`：`tracking_type=BOOLEAN`、`target_value=1`、`unit=NULL`（证明单位被清空，未被拒）
  - `habit_schedules`：`frequency_type=DAILY`、`weekday_mask=127`、`effective_from=2026-11-05`
  - `habit_logs`：`status=COMPLETED`、`actual_value=1`、`target_snapshot=1`
  - `room_master_table` 身份哈希存在；`android_metadata.locale=zh_CN`
- 截图：`.gradle/shots/{today,create,numeric,filled,switched,saved,done,numfilled}.png`（未纳入版本库）
- **行级证据已入库**：`project/reports/DEVICE_DUMP-2026-11-05.md`（含当次原样查询输出与时间线）
- **重要限制**：读库之后为验证数值输入对话框执行过 `adb shell pm clear`，设备上的库已重建为空；因此上述行级事实**只能以该 dump 为准，无法从当前设备重新导出**（评审第 3 轮复核时读到的即是空库）
- **未验证**：数值习惯的输入对话框未在真机走通（`adb shell input` 在中文 IME 下丢字符，无法稳定填表）；统计页、我的页仍为占位；旋转未测
- 结论：真机验证了「能建习惯、能出现在今日、能打卡、单位不会残留」这条主路径；它不等于 UI 自动化，也不覆盖异常路径

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-012
- 状态：`DONE`
- 阶段：Gate 3
- 验收：通过 6 项 / 未通过 0 项
- 最近测试记录：2026-11-05 / **OK (58 tests)**
<!-- close-task:end -->
