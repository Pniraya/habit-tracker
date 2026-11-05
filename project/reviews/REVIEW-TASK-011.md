# REVIEW-TASK-011: 独立评审记录

- 审查任务：`TASK-011`
- 审查 commit：`362925fe64a0c15bb3bb0247d216a580e97e94e5`
- 审查范围：`git diff main...HEAD`（`5e99ece`..`362925f`，1 个提交，10 文件 +508/−19）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`

评审时工作区：`git status --short` 空（开始前）。所有变异先备份再改、跑完立即还原；结束时工作区仅剩本评审文件，`git diff --check` exit 0。

## 1. Scope

**裁决：通过（改动面本身干净），但有一处产品行为超出「最小 UI」的预期，见 §4 与 §6。**

- 改动文件与 TASK-011 §5 一致：新增 `TodayPresenter.kt`、`TodayUiState.kt`、`TodayViewModel.kt`、`TodayPresenterTest.kt`、`TASK-011.md`；修改 `TodayScreen.kt`、`AppRoot.kt`、`README.md`、`PROJECT_STATUS.md`、`TEST_REPORT.md`。
- `git diff main...HEAD --name-status` 中没有 `build.gradle.kts` / `libs.versions.toml` / `AndroidManifest.xml` / schema / 迁移：**无新依赖、无权限变更、无 schema 改动**；无 DataStore、无提醒调度代码。
- UI 维持最小：没有数值输入框，`TodayScreen` 只接收 `state` / `lastError` / 两个回调，自身不访问 Repository。
- 唯一越界：所有「今天应做」的行都可点击，`toggle()` 不区分 tracking type，导致数值习惯被点一下就写入 1.0（见 §4 探针与 §6）。

## 2. Diff 核对

**裁决：通过。**

- `git diff --check`：**exit 0**；无冲突标记、无行尾空白。
- 变更聚焦：一个提交完成「Presenter/UiState/ViewModel + 页面接线 + 7 项单测 + 状态文档」，没有顺手改动其他页面或构建脚本。
- `AppRoot` 只把 Today 路由从占位替换为 `viewModel(factory = ...)` + `collectAsState`，未改导航结构。

## 3. 架构与数据

**裁决：通过。**

- 分层符合 `ARCHITECTURE.md` §5.2：Compose → ViewModel → UseCase → Repository。Presenter 是纯 Kotlin，不引用 Android 类型；`TodayUiState` 复用 `data.local.entity` 的枚举（TASK-011 §8 已把枚举迁移记为已知技术债）。
- `TodayViewModel` 确实很薄：只转发 `state`/`lastError` 与三个方法，`onCleared()` 取消自己的 scope，工厂组装 `RoomHabitRepository` + `RecordHabitProgressUseCase`。
- 轮换/重复 create 的判断：ViewModel 在配置变更中存活，`presenter` 随 ViewModel 存活，状态不会因旋转丢失；若 `create()` 被调用两次（两个 ViewModelStore），会各自得到独立的 Repository 实例、scope 与 Presenter，无静态共享状态，**不会互相破坏**，只是重复订阅。`onCleared` 与工厂均无自动化测试（薄到可接受，见 §6 建议）。
- 无 schema/数据口径改动；UI 不直接依赖 Room。

## 4. 验证证据

**裁决：不通过。两条变异能被测试抓住，但「拒绝写入」这条测试无法区分 use case 与 repository，且数值习惯的点击行为没有任何测试。**

| 命令 | 我的实测 |
|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS**，exit **0**；治理 7s（21/21）、单测 23s（47 项）、DAO 0s（21 项）、构建 21s、空白 0s |
| `pwsh -NoProfile -File tools/check-governance.ps1` | `SUMMARY: 21 checks, 21 passed, 0 warnings, 0 failed` / `RESULT: OK`，exit **0** |
| `git diff --check` | exit **0** |

### 变异验证（全部亲自执行，已还原）

| # | 变异 | 结果 |
|---|---|---|
| M-a | `toggle()` 永远写 1.0（不再按 `isCompleted` 清零） | **预期失败被抓住**：`1) togglingAgainClearsTheDayInsteadOfStackingValues`，`Tests run: 47, Failures: 1`，exit **1** |
| M-b | `record()` 绕过 use case，直接调 `repository.recordDailyValue(-1.0)` | **没有被抓住**：`OK (47 tests)`，`RESULT: UNIT TESTS PASS`，exit **0**。原因：`LogStatusRules.evaluate` 同样以 `actual value must not be negative` 拒绝，测试断言的 `lastError.contains("negative")` 与「未被写入」在两个层级都成立，因此该测试**无法证明 Presenter 走了 `RecordHabitProgressUseCase`** |
| M-c | `describe()` 忽略 `activeSchedule`（`scheduled = true`） | **预期失败被抓住**：`1) aHabitScheduledForAnotherWeekdayIsMarkedAsResting`，`Tests run: 47, Failures: 1`，exit **1** |
| 探针 | 临时加一个测试：对 NUMERIC 习惯调用 `presenter.toggle(id)`，断言仓库里 `actualValue == 1.0` | **通过**（`OK (48 tests)`）→ 证实「点一下数值习惯会写入 1.0」；探针随即删除，`git status --short` 为空 |

- 还原后 `OK (47 tests)`、exit 0、工作区为空。
- 7 项测试覆盖了加载、布尔切换、再切换清零、数值写入、休息日、拒绝消息、归档过滤，与 TASK-011 §10 描述一致；但**没有**任何测试覆盖 NUMERIC 习惯的点击路径，也没有测试覆盖 `isLoading == true` 的初始态。

## 5. 文档与分支

**裁决：不通过（两处状态文档未跟上，一处覆盖边界未写进报告）。**

已同步、核对无误：
- `README.md`：阶段 `Gate 3`、当前任务 `TASK-011`、最近测试 47 项。
- `PROJECT_STATUS.md`：阶段 `Gate 3`、当前任务、代码实现状态写明「Today 页状态机；构建通过……UI 与设备未验证」——这一点是诚实的。
- `TASK-011` §10 的 21/21、47 项、DAO 21 项可复现；构建写 28s，我实测 20–21s（冷热构建差异，属噪声）。
- `TEST_REPORT.md` `Runs` 已追加 TASK-011 两行（47 项、构建 28s）。

不符合项（阻塞）：
1. **`TEST_REPORT.md` 的 `Latest Run` 仍是 TASK-010 的旧内容**：第 9 行写「单测 40 项……（`main` 上复跑）」，而第 7 行又说分支是 `feat/today-presenter`。当前分支是 47 项。检查器的「test report summary matches」抓不到这种滞后（它只比较 `Latest Run` 与最后一行是否同类，而最后一行恰是 TASK-010 的变异记录）。
2. **`LAST_HANDOFF.md` 未更新**：Snapshot 仍是 `Gate 2` / 当前任务 `TASK-010` / `main` @ `feeee61`，`Next Safe Action` 也停留在上一任务。`AGENTS.md` §6 与 §10 阶段 7 要求任务结束写交接，`REVIEW_CHECKLIST.md` §4 明确要求 Snapshot 已更新。
3. **`TEST_REPORT.md` 的 `Known Test Gaps` 没有 Today 的条目**：既没写「TodayPresenter 状态机已被 7 项 JVM 测试覆盖」，也没写「Compose 渲染与交互只编译、未验证（无 Compose 测试、无设备）」。TASK-010 已把该节改成「已覆盖 / 未覆盖」结构，本次新增功能没有接上。

## 6. 结论

- 结论：`REJECT`
- 判定说明：架构分层与 Page/Presenter 拆分是对的，M-a 与 M-c 证明状态机的核心分支确有可证伪测试。但 M-b 暴露了「拒绝写入」测试并不鉴别 use case 是否在路径上；数值习惯的点击会静默写 1.0，而这条路径没有任何测试；再加上 `Latest Run` 与交接文档未同步，本轮不满足「测试覆盖真正出货的逻辑」与文档同步两条要求。
- 证据（本轮实际命令与退出码）：
  - `pwsh -NoProfile -File tools/verify-all.ps1` → `RESULT: VERIFY ALL PASS`，exit **0**（治理 21/21、单测 47 项、DAO 21 项、构建 21s、空白 0s，5 步）
  - `pwsh -NoProfile -File tools/check-governance.ps1` → `SUMMARY: 21 checks, 21 passed, 0 warnings, 0 failed`，exit **0**
  - `git diff --check` → exit **0**
  - M-a（`toggle` 永远 1.0）→ `Failures: 1`，`togglingAgainClearsTheDayInsteadOfStackingValues`，exit **1**
  - M-b（绕过 use case）→ `OK (47 tests)`，exit **0**（未被抓住）
  - M-c（`scheduled = true`）→ `Failures: 1`，`aHabitScheduledForAnotherWeekdayIsMarkedAsResting`，exit **1**
  - 探针（NUMERIC 习惯 `toggle` 后 `actualValue == 1.0`）→ `OK (48 tests)`，行为即代码所示
  - 全部还原后 47/0、`git status --short` 为空
- 需修复项：
  1. 数值习惯的点击不得写入 1.0：在 `toggle()` 里只允许 `TrackingType.BOOLEAN`（其余直接 return），或让 `HabitRow` 对非 BOOLEAN 不可点击；并补一条测试覆盖「NUMERIC 行点击不产生日志」。
  2. 让「拒绝写入」测试真正鉴别 use case：改用只有 use case 会拒绝的场景（休息日写入、或归档习惯写入——repository 的 `recordDailyValue` 不校验这些），或注入一个会抛特定消息的 fake use case，断言该消息。
  3. 更新 `TEST_REPORT.md` 的 `Latest Run` 为本分支的实际运行（47 项、21 项检查、0 警告、DAO 21 项、构建耗时）。
  4. 更新 `LAST_HANDOFF.md` 的 Snapshot / Completed / Verification / Next Safe Action 到 TASK-011。
  5. 在 `Known Test Gaps` 增加 Today 条目：状态机 7 项 JVM 覆盖；Compose 渲染与交互「编译通过、未验证」。

### 非阻塞建议

1. `TodayPresenter.toggle()` 依赖 `_state.value.habits` 判断 `isCompleted`，两次快速点击可能都读到旧状态；当前 UI 无防抖，建议后续用单飞/串行化处理（本轮无测试）。
2. `isLoading == true` 的初始态没有测试；若 `observeActiveHabits` 迟迟不发首个值，界面会一直「读取中…」。
3. `TodayViewModel` 的 `onCleared` 与工厂没有测试。属薄包装可接受，但取消 scope 的行为一旦被删也不会变红。
4. `TodayScreen.subtitle` 用 `trackingType.name == "NUMERIC"` 做字符串比较，建议改枚举比较。
5. `collectAsState()` 非生命周期感知；若依赖允许，`collectAsStateWithLifecycle()` 更省资源。
6. `close-task` 生成的「最近测试记录：**SUCCESSFUL（28s）**」实际取的是构建行而非测试行，措辞误导（历史遗留）。
7. TASK-011 §10 的构建 28s 与实测 20–21s 属冷热构建差异，建议写成区间。
