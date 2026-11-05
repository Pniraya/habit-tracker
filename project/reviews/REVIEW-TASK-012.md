# REVIEW-TASK-012: 独立评审记录

- 审查任务：`TASK-012`
- 审查 commit：`1f633a85125d6d3f41bf2492f640d9b5d480ce55`
- 审查范围：`git diff main...HEAD`（`cc4fba5`..`1f633a8`，1 个提交，12 文件 +605/−17）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`
- 轮次：第 1 轮

评审时工作区：`git status --short` 为空（开始前）。本轮为只读核对 + 门禁复跑 + 3 组变异 + 1 个诊断探针；所有改动已还原，结束时工作区仅剩本评审文件。

## 1. Scope

**裁决：通过。**

- 实现范围与 TASK-012 §3/§5 一致：新增 `feature/create/{CreateHabitForm,CreateHabitPresenter,CreateHabitScreen}.kt`、`CreateHabitPresenterTest.kt`、任务文件；修改 `TodayScreen.kt`、`AppRoot.kt`、`AppRoutes.kt`、`FakeHabitRepository.kt` 与状态文档。
- 无 `build.gradle.kts` / `libs.versions.toml` / `AndroidManifest.xml` / schema / 迁移改动：`git diff main...HEAD --name-status` 中没有这些路径，**无新依赖、无权限变更、无 schema 改动**。
- 无编辑/删除习惯、统计页、提醒、DataStore、图标选择器等 Out of Scope 内容。
- 未做 Compose UI 测试，与 §4 声明一致。

## 2. Diff 核对

**裁决：通过。**

- `git diff --check`：**exit 0**，无行尾空白。
- 冲突标记扫描（`git grep -E '^(<<<<<<<|=======|>>>>>>>)'`）：无匹配，exit 1（grep 无匹配语义）。
- 单提交 `1f633a8` 职责集中，未夹带无关重构。
- 新增文件的 mask 计算复用 `ScheduleRules.bitFor/ALL_DAYS_MASK`，未另写一份位运算硬编码；`RoomHabitRepository` 侧仍会用 `ScheduleRules.maskFor` 复算一次，属幂等的双重保障，不构成规则分叉。

## 3. 架构与数据

**裁决：通过（含 1 项需后续跟进的设计问题）。**

- 分层符合 `ARCHITECTURE.md` §5.2：Compose → Presenter → UseCase → Repository；`CreateHabitPresenter` 为纯 Kotlin，不引用 Android 类型，`today`/`now` 可注入。
- 数值输入链路完整：`TodayScreen` 的 `HabitRow` 在 `isScheduledToday` 时可点击，BOOLEAN 走 `onToggle`，其余走 `onRecord` 打开 `NumericInputDialog` → `TodayViewModel.record` → `TodayPresenter.record` → `RecordHabitProgressUseCase`。没有静默不可交互的数值行；未排期行不可点属既定设计。
- 对话框不做写入前校验，但负值会被 use case 拒绝并以 `lastError` 回显，不会被静默写入；代价是关框丢输入（见 §6 非阻塞 2）。
- 表单持有方式（`remember { }`，旋转丢内容）已在 §8 与 §12 明确记为已知缺陷，未隐藏在验收项里——符合诚实性要求。
- 规则位置：`HabitRules.normalizeTarget` 只在 `RoomHabitRepository` 调用，`CreateHabitUseCase` 只校验 name；fake 改为调用同一个函数是**本任务内合理的最小修复**（测试必须与生产同规则，否则会出现测试绿、生产红），且 §8/§9 与测试注释都写明了发现过程，并把规则上移明确留给后续任务，没有假装架构已对齐。但仓库接口 `HabitRepository.createHabit` 仍未声明该契约，规则仍只由数据层实现强制执行（见 §6 非阻塞 1）。

## 4. 验证证据

**裁决：部分通过——门禁与变异证据真实，但任务文件记录的变异只覆盖 1 组，且文档计数与自动摘要不一致。**

| 命令 | 实际结果 | 退出码 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | 治理 21/21（0 警告）、单测 **OK (57 tests)**、DAO SQL 21 项 0 失败、`assembleDebug` BUILD SUCCESSFUL 19s、空白 0 项 | **0** |
| `pwsh -NoProfile -File tools/check-governance.ps1`（经 verify-all） | `SUMMARY: 21 checks, 21 passed, 0 warnings, 0 failed` | 0 |
| `git diff --check` | `no whitespace errors` | 0 |
| 变异 a：`form.name.isBlank()` → `isEmpty()` | `Tests run: 57, Failures: 1`，`blankNameIsRejectedWithoutTouchingTheRepository` 变红 | **1** |
| 变异 b：`maskFor` 的 CUSTOM 分支固定为 `ALL_DAYS_MASK` | `Tests run: 57, Failures: 1`，`customWeekdaysProduceTheMask` 变红 | **1** |
| 变异 c：删除 `save()` 的本地校验（`val error = validate(form)` → `null`） | `Tests run: 57, Failures: 4`：`blankName…`、`numericTargetMustBeANumber`、`numericTargetMustBePositive`、`customFrequencyNeedsAtLeastOneDay` 全红 | **1** |
| 诊断探针：NUMERIC 填 unit 后切回 BOOLEAN 再 `save()` | `Tests run: 58, Failures: 1`，`probeSwitchingFromNumericToBooleanKeepsAUnit` 变红（见 §6 需修复 1） | **1** |
| 还原后 `tools/run-unit-tests.ps1` | `OK (57 tests)` / `RESULT: UNIT TESTS PASS` | 0 |

变异 a/b/c 与探针均在独立备份后回写原文件，还原后 57 项全绿；`git status --short` 为空。

未验证项（如实声明）：Compose 渲染与交互（无 Robolectric/设备测试，`NumericInputDialog` 与 `CreateHabitScreen` 只经编译）；`AppRoot` 的路由跳转与返回栈行为；旋转后表单丢失（代码可读，未实机复现）。

## 5. 文档与分支

**裁决：不通过。**

- `README.md`（57 项、当前任务 TASK-012）、`TEST_REPORT.md` 的 `Latest Run`（57 项、`feat/create-habit`）与 Known Test Gaps（新建表单 8 项 JVM 覆盖、Compose 未验证）**一致且准确**。
- 🚫 `project/handoffs/LAST_HANDOFF.md` 仍是 TASK-011 的交接：写着「单测 47→49 项」，并把「新建习惯页面 + 数值输入 UI」列为「下一个新任务（尚未创建）」——而本任务正是它，已完成实现与单测。违反 `AGENTS.md` §5/§10（Persist/Handoff）与 §6。
- 🚫 `project/tasks/TASK-012.md` §状态摘要（自动生成块）写「最近测试记录：**OK (49 tests)**」，实际 57 项。根因是 `TEST_REPORT.md` 的 `Runs` 表行序错乱：TASK-012 的 57 项行在第 57-58 行，而 TASK-011 的 49 项行（第 60 行）与 TASK-010 的行（第 62-63 行）被追加在它后面，`tools/close-task.ps1` 取「最后一条匹配行」时命中了 49 项。这不是摘要生成器单独出错，而是表格追加顺序错误 + 摘要未重新生成，两者都要修。
- 🚫 `project/PROJECT_STATUS.md` 第 13 行仍为「最近验证测试：`PASS`（49 项…在 `main` @ `f5d4dad` 上经 `verify-all` 复跑）」，与 `README.md` 的 57 项、本分支实际 57 项矛盾。
- 分支 `feat/create-habit`、单提交、未推送，符合 §7；评审开始与结束时工作区均干净（仅本文件为新增）。

## 6. 结论

- 结论：`REJECT`
- 证据：`verify-all` exit 0（57 项、治理 21/21、DAO 21 项、构建 19s）；变异 a/b/c 分别 exit 1 且精确打红目标测试；诊断探针证明表单存在 unit 状态泄漏（§4）。实现主体可用，阻断项是 1 个真实用户可复现的缺陷与 3 处文档/计数失真，修复量小。
- 需修复项：
  1. **表单 unit 状态泄漏（产品缺陷）**：`onTrackingTypeChange(NUMERIC→BOOLEAN)` 只清 `targetValue`，不清 `unit`；`draft()` 又无条件把 `form.unit` 传给 `NewHabit`。用户在 NUMERIC 填入单位后切回「完成/未完成」，BOOLEAN 模式下单位输入框被隐藏、无法清空，保存必被 `HabitRules.normalizeTarget` 以 `BOOLEAN habit must not have a unit` 拒绝，且用户无路可退（只能来回切类型）。探针 `probeSwitchingFromNumericToBooleanKeepsAUnit` 已实测变红。修法：切到 BOOLEAN 时一并清空 `unit`（与 `targetValue` 同处），或在 `draft()` 里对 BOOLEAN 强制 `unit = null`；并补一条回归测试。
  2. **`LAST_HANDOFF.md` 未更新**：按 §5/§10 改写为 TASK-012 的当前状态、验证结果（57 项）、未验证项与下一安全操作。
  3. **TASK-012 自动摘要的 49 项**：先修正 `TEST_REPORT.md` `Runs` 表的追加顺序（TASK-012 行放到 TASK-011/TASK-010 行之后），再重跑 `tools/close-task.ps1` 让摘要显示 57 项。
  4. **`PROJECT_STATUS.md` 第 13 行 49 项**：与 README/实际对齐为 57 项（并写清分支与 commit）。
- 非阻塞建议：
  1. fake 与生产共用 `normalizeTarget` 本身正确，但规则的执行点仍在数据层：`CreateHabitUseCase` 只校验 name，任何新的 `HabitRepository` 实现都可能漏掉 target/unit 规则，而 use case 不会兜底。建议后续任务把 `normalizeTarget`（纯函数，无 Android 依赖）上移到 use case，或在 `HabitRepository.createHabit` 上写明「实现必须归一化 target/unit」的契约并加一条针对接口的契约测试。另外 `FakeHabitRepository.createCalls += 1` 位于 `normalizeTarget` 之后，因此「不触达仓库」的 `assertEquals(0, createCalls)` 在绕过本地校验时也恒为真——变异 c 的红是来自错误文案相等断言，不是来自调用计数；若想让「不触达仓库」真正可证伪，应在 use case 前加计数或断言具体错误文案。
  2. `NumericInputDialog` 只用 `toDoubleOrNull()` 判断可提交，负值可提交后关框，再由 use case 拒绝、错误显示在页面顶部，用户输入丢失。建议在对话框内加 `value >= 0` 的本地判断并保持对话框打开。
  3. `CreateHabitPresenter` 的 `onFailure` 分支（保存失败保持表单打开）没有测试覆盖。
  4. `AppRoot.kt` 的 import 未排序（`androidx.*` 排在 `com.habittracker.*` 之后），属风格问题，不影响构建。
