# TASK-011: Today 页面的 ViewModel/Presenter 接线

- 状态：`PARTIAL`（实现与单测完成，等待独立评审）
- 阶段：`Gate 3`
- 优先级：`P0`
- 依赖：`TASK-008`（UseCase）、`TASK-010`（DAO SQL 验证）
- 创建日期：`2026-11-05`
- 完成日期：`TBD`

## 1. Objective
把已经验收的 UseCase 接到界面上：Today 页显示今天应做的习惯、读取当天日志、点击即可记录完成，且写入失败时把领域拒绝的原因显示出来而不是崩溃。

## 2. Context
`ARCHITECTURE.md` §5.2（ViewModel → UseCase → Repository）；`REQUIREMENTS.md` 的 Today 行为；`ADR-006`。

## 3. Scope
- 新增 `feature/today/TodayUiState.kt`、`TodayPresenter.kt`（纯 Kotlin 状态机）、`TodayViewModel.kt`（生命周期薄包装 + Room 工厂）。
- 改造 `TodayScreen` 渲染真实状态；`AppRoot` 注入 ViewModel。
- 用 `FakeHabitRepository` 写 JVM 单测覆盖加载、切换、数值记录、休息日、拒绝与归档。

## 4. Out of Scope
- 数值输入的完整 UI（本任务只暴露 `record(habitId, value)`）。
- 新建习惯页面、统计页、提醒、DataStore。
- Compose UI 测试（无 Robolectric/设备）。

## 5. Expected Files
- 新增：`app/src/main/java/com/habittracker/feature/today/{TodayUiState,TodayPresenter,TodayViewModel}.kt`、`app/src/test/java/com/habittracker/feature/today/TodayPresenterTest.kt`、`project/tasks/TASK-011.md`
- 修改：`feature/today/TodayScreen.kt`、`navigation/AppRoot.kt`、状态文档

## 6. Acceptance Criteria
- [x] Today 页显示当日习惯与计划状态（含"今天不在计划内"）
- [x] 点击布尔习惯写入 `COMPLETED`，再点一次清零而非累加
- [x] 数值习惯的写入走 `RecordHabitProgressUseCase` 并显示实存值
- [x] 被领域拒绝的写入显示消息，不抛到 UI
- [x] 归档习惯不出现在列表
- [x] 数值习惯不可一键完成（点击不写值，提示需输入数值）
- [x] `tools/run-unit-tests.ps1` 通过、`assembleDebug` 通过

## 7. Tests
- `pwsh -NoProfile -File tools/run-unit-tests.ps1`（新增 `TodayPresenterTest`，7 项）
- `gradlew.bat assembleDebug`

## 8. Risks and Decisions
- 逻辑放在纯 Kotlin 的 `TodayPresenter`，不放在 `ViewModel`：`lifecycle-viewmodel` 只有 `.aar`，JVM 测试 classpath 用不上，把状态机放进 ViewModel 会让它无法测试。ViewModel 只保留 `StateFlow` 转发与 `onCleared` 取消 scope。
- 写入后重建列表：Repository 目前只暴露习惯流，没有 habit+log 的联合查询；为此新增联合 Flow 属于 Repository 契约变更，留待需要时单独开任务。
- `TodayUiState` 复用 `data.local.entity` 的 `TrackingType`/`LogStatus`（Domain 已如此），枚举迁移仍为已知技术债。
- 工厂每次创建 `RoomHabitRepository`；Room 实例本身是单例，因此开销可忽略。

## 9. Implementation Log
- 2026-11-05：新增 Presenter/ViewModel/UiState 与页面接线；首次编译失败 `foundation.layout.weight` 为 internal import，删除后 47 项单测通过、构建 28s。
- 2026-11-05：独立评审首轮 `REJECT`：数值习惯 `toggle()` 会写入 1.0（真缺陷）；"负值拒绝"测试无法鉴别是否走了 use case；`TEST_REPORT`/`LAST_HANDOFF`/`Known Test Gaps` 未接上 Today。已修：`toggle` 只对 BOOLEAN 生效并提示，UI 对非 BOOLEAN 行禁用点击；新增"休息日写入被 use case 拒绝"测试（只有 use case 会拒绝该场景）。
- 2026-11-05：`TodayViewModel` 首次写成 `presenter.cancel()`，但 Presenter 不拥有 scope；改为 ViewModel 持有并取消自己的 scope，避免取消调用方传入的 scope。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/run-unit-tests.ps1` | **OK (49 tests)** | 新增 9 项：加载/切换/再切换/数值/休息日/拒绝/归档/数值不可点击/休息日写入被 use case 拒绝 |
| `gradlew.bat assembleDebug` | **SUCCESSFUL**（19-28s，冷热差异） | 首次失败：`androidx.compose.foundation.layout.weight` 为 internal，删除该 import |
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS**（exit 0） | 治理 21/21（0 警告）、单测 49 项、DAO SQL 21 项、构建 19-28s、空白干净 |

## 11. Completion Review
- [x] 验收标准全部满足（7/7）
- [x] 相关测试实际运行（49 项）
- [x] `git diff --check` 通过
- [x] Diff 已审查：无新依赖、无 schema 改动、无权限变更
- [x] 文档/状态已更新
- [ ] 独立 Reviewer `APPROVE`

## 12. Handoff
- 下一步：评审后合入 `main`；随后补新建习惯页面与数值输入 UI（任务文件尚未创建）。
- 遗留：UI 层无自动化测试；写入后整表重建，习惯多时可能成为性能问题。

## 13. Independent Review
- Reviewer record：`project/reviews/REVIEW-TASK-011.md`
- 结论：`PENDING`

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-011
- 状态：`PARTIAL`
- 阶段：Gate 3
- 验收：通过 7 项 / 未通过 0 项
- 最近测试记录：2026-11-05 / **OK (49 tests)**
<!-- close-task:end -->
