# REVIEW-TASK-018: 独立评审记录

- 审查任务：`TASK-018`
- 审查 commit：`2845c169ef1373e17d12fd911218a35de336e99d`
- 审查范围：`git diff main...HEAD`（`52e89c1`..`2845c16`，1 个提交，10 文件 +194/−21）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`
- 轮次：第 1 轮

评审时工作区：`git status --short` 为空（开始前）。本轮为只读核对 + 门禁复跑 + 3 组变异；改动已还原，结束时工作区仅剩本评审文件。

## 1. Scope

**裁决：通过（范围），但不完整（见 §6）。**

- 改动与 §3/§5 一致：`CreateHabitPresenter`（`NO_UNIT` + `errorMessageFor`）、`TodayPresenter`（`progressText`/`formatQuantity`）、`TodayScreen`（`subtitle` 改用 presenter、逗号归一化）、两个测试文件与状态文档。
- 无依赖、无 schema、无权限改动；领域层 `HabitRules` 文案保持英文（§4 明确排除）。
- 但「小数显示」只修了列表：数值对话框仍有两处 `toLong()`（见 §6 阻断项 1），同一缺陷类别没有修完。

## 2. Diff 核对

**裁决：通过。**

- `git diff --check`：**exit 0**；无冲突标记。
- `errorMessageFor` 是顶层 `internal` 纯函数，映射 5 类消息 + `SAVE_FAILED` 兜底；`onFailure` 不再直出 `failure.message`。
- `progressText`/`formatQuantity` 用 `BigDecimal.valueOf().stripTrailingZeros().toPlainString()`，`0.5`→`0.5`、`2000.0`→`2000`；`subtitle()` 改为调用它，并去掉原来 `unit ?: ""` 造成的尾随空格。
- 对话框 `onValueChange = { text = it.replace(',', '.') }` 是单行改动，未影响 `parsed` 的 `takeIf { it >= 0.0 }` 守卫。

## 3. 架构与数据

**裁决：通过（含 1 项分层讨论）。**

- 映射与格式化都在 feature 层，未把展示需求下沉到 `domain/`；`HabitRules` 仍只抛英文 `require`，与 `TASK-014` 的领域/仓储分层一致。
- `progressText` 放在 `TodayPresenter.companion`：它是 UI 格式化，属于 feature 层而非领域层，放这里可被 JVM 直接测试，比留在 Composable 里好；若将来统计页也要用，再抽 `TodayFormatting` 对象也不迟。
- `errorMessageFor` 的映射是跨层「翻译层」，方向正确；但它是**字符串契约**，见 §6 非阻塞 1。

## 4. 验证证据

**裁决：通过（变异与门禁），但覆盖有缺口（§6 阻断项 2）。**

| 命令 / 变异 | 实际结果 | 退出码 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | 治理 21/21（0 警告）、单测 **OK (67 tests)**、DAO SQL 21 项 0 失败、`assembleDebug` BUILD SUCCESSFUL 21-22s、空白 0 项 | **0** |
| `pwsh -NoProfile -File tools/check-governance.ps1`（单独复跑） | `SUMMARY: 21 checks, 21 passed, 0 warnings, 0 failed` | 0 |
| `git diff --check` | `no whitespace errors` | 0 |
| 变异 A：`formatQuantity` → `toLong().toString()` | `Tests run: 67, Failures: 2`：`aRecordedDecimalIsShownAsADecimal`、`wholeNumbersKeepNoTrailingDecimalPoint` | **1** |
| 变异 B：`errorMessageFor` 的 `else` → 原始 `message` | `Tests run: 67, Failures: 2`：`domainRejectionsAreTranslatedForTheUser`、`anUnknownFailureKeepsAChineseMessage` | **1** |
| 变异 C（新增，评审自制）：`onFailure` 的 `error = messageFor(failure)` → `failure.message ?: SAVE_FAILED` | `OK (67 tests)`，**零失败** | 0 |
| 还原后 `tools/run-unit-tests.ps1` | `OK (67 tests)` / `RESULT: UNIT TESTS PASS` | 0 |

A/B 打红的正是对应测试，没有误伤；C 说明映射函数有测试，但**调用点没有**——把 `onFailure` 改回直出英文，67 项全绿，`TASK-012` 的原始缺陷可以原样回归。

未验证项：Compose 渲染与 IME 行为（真机走查由开发者完成，截图未入库）；对话框的显示值未纳入任何测试。

## 5. 文档与分支

**裁决：不通过（与 §6 阻断项 1 相关）。**

- 计数一致（README/TEST_REPORT/PROJECT_STATUS/handoff 均 67），分支 `fix/error-text`、单提交、未推送，符合 `AGENTS.md` §7。
- 🚫 §6 把 5 条验收全部勾为 `[x]`、§11 写「5/5」，其中第 3 条「`0.5` 显示为 `0.5`」只在列表成立；对话框仍把 `0.5` 显示成 `0`（见 §6）。这不是「未验证」，是**已知范围没修完却报为已完成**。
- 非阻塞：`LAST_HANDOFF.md:44` 仍把 `TASK-018` 列为「任务文件待建」的下一任务，与它已存在且正在评审矛盾。

## 6. 结论

- 结论：`REJECT`
- 证据：门禁 exit 0、两组官方变异各自打红正确测试；但评审自制变异 C 暴露调用点无测试，且 `TodayScreen.kt:131/139` 仍有 `toLong()` 截断——列表修好、对话框没修，缺陷只修了一半。
- 需修复项：
  1. **对话框仍截断小数（用户可见 + 会写坏数据）**：`TodayScreen.kt:131` 用 `habit.actualValue.toLong().toString()` 预填输入框，已记录 `0.5` 的习惯再次点开时显示 `0`；用户若直接按「记录」，`parsed = 0.0` 会把 0.5 **覆盖成 0**。`TodayScreen.kt:139` 的「今天的目标」也用 `habit.targetValue.toLong()`，目标 0.5 会显示成 `0`。修法：两处都改用 `TodayPresenter.formatQuantity(...)`；补一条断言（至少覆盖格式化函数，最好把对话框的预填逻辑抽成可测函数）。
  2. **`onFailure` 的翻译调用点没有测试**：变异 C 证明把 `error = messageFor(failure)` 改回 `failure.message` 后 67 项全绿。§6 第 2 条「任何英文消息都不会出现在界面上（有映射函数与测试）」目前只兑现了「映射函数有测试」。建议加一条 presenter 级测试：让 fake 仓储在 `createHabit` 抛 `IllegalArgumentException("NUMERIC habit needs a unit")`，断言 `form.error == NO_UNIT`。
  3. **逗号归一化把千分位静默变成小数**：`"1,234"` → `"1.234"`，`"10,000"` → `"10.0"`；§8 写的是「不支持千分位」，但实际行为不是拒绝而是**按另一种含义接受**，数值差 1000 倍且没有提示。建议：命中 `^\d{1,3}(,\d{3})+$` 的输入不归一化（视为非法/提示），或在对话框里实时显示解析结果（如「= 1.234 ml」）让用户看见将要写入的值。
- 非阻塞建议：
  1. 子串映射的取舍是**诚实的**（§8 写清了「防英文直出的兜底，不是强类型契约」，退化到中文 `SAVE_FAILED` 方向安全），不是合理化；但可再稳一点：把 `needs a unit` 等文案提为 `HabitRules` 的 `const val`，映射引用同一常量，领域改文案时至少编译期能看见。
  2. `progressText` 放 presenter companion 可接受：feature 层格式化、纯函数、可测；不建议下沉到领域层。
  3. `numericHabitWithoutUnitIsRefused` 改后确实更强（精确常量 + `createCalls == 0` + 无 id），但它现在测的是 presenter 的**本地**校验，不再覆盖 `onFailure` 的翻译路径——该路径需要第 2 条补的测试。
  4. `LAST_HANDOFF.md:44` 的「TASK-018 任务文件待建」应删除。
