# REVIEW-TASK-012: 独立评审记录

- 审查任务：`TASK-012`
- 审查 commit：`ba96c55663249a998fb1cfd6d30f1bc4db4086b2`（第 2 轮）
- 审查范围：`git diff main...HEAD`（`cc4fba5`..`ba96c55`，2 个提交 `1f633a8` + `ba96c55`，14 文件 +753/−41；本轮重点为 `1f633a8..ba96c55` 的 8 文件）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`
- 轮次：第 2 轮。第 1 轮针对 `1f633a8` 判 `REJECT`（unit 状态泄漏 + 4 处文档/计数失真）；本轮核对 `ba96c55` 的修复与文档同步。

评审时工作区：`git status --short` 为空（开始前）。本轮为只读核对 + 门禁复跑 + 2 组变异 + 1 个探针；所有改动已还原，结束时工作区仅剩本评审文件。

## 1. Scope

**裁决：通过。**

- 修复提交 `ba96c55` 只动 8 个文件：`CreateHabitPresenter.kt`（清 unit + draft 兜底）、`TodayScreen.kt`（负值本地拒绝）、`CreateHabitPresenterTest.kt`（新增回归测试）、`PROJECT_STATUS.md`、`LAST_HANDOFF.md`、`TEST_REPORT.md`、`TASK-012.md` 与本评审记录；无越界改动。
- 无 `build.gradle.kts` / `libs.versions.toml` / `AndroidManifest.xml` / schema / 迁移改动：**无新依赖、无权限变更、无 schema 改动**。
- 任务 Scope 仍与 §3/§5 一致，未夹带图标/颜色/编辑等 Out of Scope 内容。

## 2. Diff 核对

**裁决：通过。**

- `git diff --check`：**exit 0**；无冲突标记（`git grep -E '^(<<<<<<<|=======|>>>>>>>)'` 无匹配）。
- 两处修复方向正确且互补：`onTrackingTypeChange` 在非 NUMERIC 时清 `unit`（并保留原有 `targetValue` 清理），`draft()` 对非 NUMERIC 强制 `unit = null` 作为第二道防线。
- 新增回归测试 `switchingFromNumericToBooleanClearsTheUnit` 断言三层：form.unit 为空、form.targetValue 为空、落库后 `unit == null` 且 `targetValue == 1.0`——覆盖的是用户真实操作序列，而不是内部字段。
- 对话框改动仅 1 行：`toDoubleOrNull()?.takeIf { it >= 0.0 }`，即解析失败或负数时 `parsed == null`，确认按钮保持禁用，对话框不关闭。

## 3. 架构与数据

**裁决：通过。**

- 分层未变：Compose → Presenter → UseCase → Repository；`CreateHabitPresenter` 仍为纯 Kotlin，`today`/`now` 可注入。
- 第 1 轮发现的 unit 泄漏已按要求修在 Presenter（UI 层），未把规则挪进 Composable；领域规则仍在 `HabitRules.normalizeTarget`，本任务不改变分层结论。
- 数值输入链路仍完整：非 BOOLEAN 行 → `NumericInputDialog` → `onRecord` → `TodayViewModel.record` → `presenter.record` → `RecordHabitProgressUseCase`；负值现在在 UI 就被拦下，不再出现「关框丢输入」。
- 遗留（已记录、非本任务）：规则执行点仍在数据层，`CreateHabitUseCase` 只校验 name；`LAST_HANDOFF.md` 已将「上移 `validateName`/`normalizeTarget` 并在接口写明契约」列为下一个新任务。

## 4. 验证证据

**裁决：通过。**

| 命令 / 变异 | 实际结果 | 退出码 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | 治理 21/21（0 警告）、单测 **OK (58 tests)**、DAO SQL 21 项 0 失败、`assembleDebug` BUILD SUCCESSFUL 19s、空白 0 项 | **0** |
| `pwsh -NoProfile -File tools/check-governance.ps1`（经 verify-all） | `SUMMARY: 21 checks, 21 passed, 0 warnings, 0 failed` | 0 |
| `git diff --check` | `no whitespace errors` | 0 |
| 变异 M-1：`onTrackingTypeChange` 去掉 `unit` 清空（`unit = it.unit`） | `Tests run: 58, Failures: 1`，`switchingFromNumericToBooleanClearsTheUnit` 变红 | **1** |
| 变异 M-2：`draft()` 去掉 BOOLEAN 的 `unit = null` 兜底 | `OK (58 tests)` —— **未被抓住**（见 §6 非阻塞 1） | 0 |
| 探针（第 1 轮变红的同一段代码）：NUMERIC 填 `ml` → 切 BOOLEAN → `save()` | `OK (59 tests)`，探针断言「无错误且已创建」全部通过 | 0 |
| 还原后 `tools/run-unit-tests.ps1` | `OK (58 tests)` / `RESULT: UNIT TESTS PASS` | 0 |

说明：M-2 的结果说明 `draft()` 的 `else null` 在当前「先清 unit」的前提下是死代码；它是合理的纵深防御，但没有测试能独立证明它，删掉也不会有测试变红——不应把它算作已验证的防线。

未验证项（如实声明）：Compose 渲染与交互（`NumericInputDialog` 的负值禁用按钮只经代码核对 + 编译，无 Robolectric/设备测试）；旋转后表单丢失（代码可读，未实机复现）；路由跳转与返回栈。

## 5. 文档与分支

**裁决：不通过（仅剩计数与同步问题，非代码问题）。**

- ✅ `PROJECT_STATUS.md` 第 13 行已改为「58 项 … `feat/create-habit`」；`LAST_HANDOFF.md` 已按 TASK-012 重写（Gate 3、分支、57→58、变异、未验证项、下一任务为规则上移）；TASK-012 自动摘要块为 `OK (58 tests)`；`TEST_REPORT.md` 的 `Runs` 行序已改为时间序，末行为 58 项。
- 🚫 `README.md` 第 15 行仍为「`PASS` — **57 项**单测」，与本分支实际 58 项不符。
- 🚫 `TEST_REPORT.md` 第 9 行 `Latest Run` 仍为「单测 **57 项**」，而同文件末行 `Runs` 已是 58 项，同一文件自相矛盾。
- 🚫 `TEST_REPORT.md` 第 69 行 `Known Test Gaps` 仍写「新建表单状态机 **8 项** JVM 覆盖」，实际 `CreateHabitPresenterTest` 现为 9 项。
- 🚫 `TASK-012.md` §10 的 `verify-all` 行仍为「单测 **57 项** … `assembleDebug` 22s」（同表首行已是 58 项）；§11 写「58 项 + **2 组变异**」，但 §10 只记录了 `isBlank` 一组，unit 清空那组没有证据行。
- 分支 `feat/create-habit`、两个聚焦提交、未推送，符合 `AGENTS.md` §7；评审开始与结束时工作区均干净（仅本文件为修改）。

## 6. 结论

- 结论：`REJECT`
- 证据：代码修复经变异证明有效（M-1 → 1 红；第 1 轮探针由红转绿），门禁 `verify-all` exit 0（58 项、治理 21/21、DAO 21 项、构建 19s）。阻断原因仅为 5 处计数/证据同步，全部是文档改动，不需要再动产品代码。
- 需修复项：
  1. `README.md` 第 15 行：`57 项` → `58 项`。
  2. `TEST_REPORT.md` 第 9 行 `Latest Run`：`单测 57 项` → `58 项`（与末行 58 一致）。
  3. `TEST_REPORT.md` 第 69 行：新建表单状态机 `8 项` → `9 项`。
  4. `TASK-012.md` §10 `verify-all` 行的 `单测 57 项` → `58 项`；补一行 unit 清空变异的证据（或在 §11 去掉「2 组变异」的表述，使 §10/§11 一致）。
  5. 可选但推荐：`TASK-012.md` §12「下一步」与 `LAST_HANDOFF.md` 的「下一个新任务＝规则上移」对齐，避免两份文档给出不同的后继任务。
- 非阻塞建议：
  1. `draft()` 里 BOOLEAN 的 `unit = null` 目前是死代码（M-2 删除后 58 项全绿）。保留作为纵深防御可以，但不要声称它被测试覆盖；若要它可证伪，需要一个绕过 `onTrackingTypeChange`（直接构造非 NUMERIC + 非空 unit 的 form）的测试，而 Presenter 目前没有这样的入口。
  2. `tools/check-governance.ps1` 的第 15 项检查（`test report summary matches`）只比对日期与 `PASS`/`FAILED` 字样，不比对**计数**，所以本次「Latest Run 57 / 末行 58」能通过检查。这是一个真实的检查盲区，建议单开任务让它比对 `OK (N tests)` 的 N。
  3. `FakeHabitRepository.createCalls += 1` 位于 `normalizeTarget` 之后，四条本地校验测试的 `assertEquals(0, createCalls)` 在绕过 Presenter 校验时也恒为真；变异 c 的红来自错误文案相等断言。若要让「不触达仓库」真正可证伪，应在 use case 调用前加计数，或断言具体文案。
  4. `CreateHabitPresenter` 的 `onFailure` 分支（保存失败保持表单打开）仍无测试覆盖。
  5. `AppRoot.kt` 的 import 未排序（`androidx.*` 在 `com.habittracker.*` 之后），风格问题，不影响构建。
