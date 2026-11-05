# REVIEW-TASK-018: 独立评审记录

- 审查任务：`TASK-018`
- 审查 commit：`b5bd10fd72dc17a6773f15602905c3f3f77d89e7`（第 3 轮）
- 审查范围：`git diff main...HEAD`（`52e89c1`..`b5bd10f`，3 个提交 `2845c16` + `fe958fc` + `b5bd10f`，12 文件 +361/−28；本轮重点为 `fe958fc..b5bd10f` 的 4 个代码/测试/任务文件）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`
- 轮次：第 3 轮。第 1 轮（`2845c16`）`REJECT`：对话框截断小数、`onFailure` 无测试、千分位被改写；第 2 轮（`fe958fc`）`REJECT`：任务记录仍写 67 项、预填无回归测试。本轮核对这两项。

评审时工作区：`git status --short` 为空（开始前）。本轮为只读核对 + 门禁复跑 + 5 组变异；改动已还原，结束时工作区仅剩本评审文件。

## 1. Scope

**裁决：通过。**

- 修复对准第 2 轮两条：`TodayPresenter.prefillText` 抽取 + `theNumericDialogPrefillsTheRecordedDecimal`；`TASK-018.md` 的 §7/§10/§11 计数与证据行更新。
- 额外按第 2 轮非阻塞建议收紧了千分位正则（`^[1-9]\d{0,2}(,\d{3})+$`），并补 `0,123 → 0.123` 断言。
- 无依赖、schema、权限改动；`HabitRules` 仍不承担展示文案。

## 2. Diff 核对

**裁决：通过。**

- `git diff --check`：**exit 0**；无冲突标记。
- `prefillText` 是纯函数（`actual > 0.0` 时 `formatQuantity`，否则空串），`TodayScreen` 只调用它——与 `progressText` 的处置一致，规则不再埋在 Composable 里。
- 正则只在全串匹配时原样返回：`1,234` 保持不可解析、`记录` 保持禁用；`0,123` 走逗号→点号，得到 `0.123`。
- 新增 1 项测试（71 = 70 + 1），§10 补了 C/D/E 三行变异证据。

## 3. 架构与数据

**裁决：通过。**

- 格式化与输入规则都在 feature 层，依赖方向未变；数据层未动。
- 构造函数仍是第 2 轮讨论过的 `suspend (NewHabit, LocalDate, Long) -> Long`。开发者选择「先保留函数类型，把 `FakeHabitRepository.createFailure` 的改造单开任务」，理由成立（共享 fake 的改动不该混进文档修复提交）；本轮不因此阻断，但该跟进任务应当建。

## 4. 验证证据

**裁决：通过——5 组变异全部打红正确测试。**

| 命令 / 变异 | 实际结果 | 退出码 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | 治理 21/21（0 警告）、单测 **OK (71 tests)**、DAO SQL 21 项 0 失败、`assembleDebug` BUILD SUCCESSFUL 19s、空白 0 项 | **0** |
| `pwsh -NoProfile -File tools/check-governance.ps1`（单独复跑） | `SUMMARY: 21 checks, 21 passed, 0 warnings, 0 failed` | 0 |
| `git diff --check` | `no whitespace errors` | 0 |
| 变异 A：`formatQuantity` → `toLong().toString()` | `Tests run: 71, Failures: 3`：`aRecordedDecimalIsShownAsADecimal`、`theNumericDialogPrefillsTheRecordedDecimal`、`wholeNumbersKeepNoTrailingDecimalPoint` | **1** |
| 变异 B：`errorMessageFor` 的 `else` → 原文 | `Tests run: 71, Failures: 2`：`domainRejectionsAreTranslatedForTheUser`、`anUnknownFailureKeepsAChineseMessage` | **1** |
| 变异 C：`onFailure` → `failure.message` | `Tests run: 71, Failures: 1`：`aDomainRejectionIsTranslatedAtTheCallSite` | **1** |
| 变异 D：`prefillText` 的 `formatQuantity` → `toLong()` | `Tests run: 71, Failures: 1`：**`theNumericDialogPrefillsTheRecordedDecimal`** | **1** |
| 变异 E：去掉千分位保护 | `Tests run: 71, Failures: 1`：`aThousandsGroupIsNotRewrittenIntoADecimal` | **1** |
| 还原后 `tools/run-unit-tests.ps1` | `OK (71 tests)` / `RESULT: UNIT TESTS PASS` | 0 |

第 2 轮的变异 D 现在确实只有 1 红（新测试），第 1 轮的调用点变异 C 仍只有 1 红；A 从 2 红变成 3 红，是因为 `prefillText` 复用了 `formatQuantity`，属预期收敛，不是误伤。

未验证项：Compose 渲染与 IME 行为（真机走查由开发者完成，截图不入库）；`1,234` 被拒时界面只说「按钮禁用」，没有解释文案。

## 5. 文档与分支

**裁决：通过（1 处需在标记 `DONE` 前修正）。**

- 计数已一致：`TASK-018.md` §7「新增 8 项 / 共 71 项」、§10 首行 `OK (71 tests)`、verify-all 行 71、§10 新增 C/D/E 三行；`README.md:15`、`TEST_REPORT.md:9`、`PROJECT_STATUS.md:13`、`LAST_HANDOFF.md:27` 均为 71；自动摘要 `OK (71 tests)`。
- §11 写「验收标准全部满足（**9/9**）」，但 §6 实际是 **8** 条、自动摘要也是「通过 8 项 / 未通过 0 项」。这不是「少验一条」（8 条全部勾选且有证据），而是分母写错；请在标记 `DONE` 前改成 8/8，避免合并后的记录自相矛盾。
- 分支 `fix/error-text`、三个提交、未推送，符合 `AGENTS.md` §7；工作区干净。

## 6. 结论

- 结论：`APPROVE`
- 证据：`verify-all` exit 0（71 项、治理 21/21、DAO 21 项、构建 19s、空白 0）；A/B/C/D/E 五组变异各自打红对应测试（D 只 1 红，与开发者声明一致）；第 1、2 轮的阻断项全部关闭。
- 需在 `DONE` 前修正（不阻断合并）：`TASK-018.md` §11 的「9/9」改为「8/8」（§6 是 8 条，自动摘要也是 8）。
- 非阻塞建议：
  1. 跟进任务：把 `CreateHabitPresenter` 的构造参数恢复为 `CreateHabitUseCase`，用 `FakeHabitRepository.createFailure` 注入失败——测试仍能覆盖 `onFailure`，生产签名也不必放宽；当前函数类型可接受，但既然要单开任务，建议把它列进 `TASK-017` 或另建一个后续任务。
  2. `1,234` 被拒时只有「记录」按钮禁用，没有提示；建议在对话框加一行「无法解析的数值」或在输入时显示解析结果，避免用户以为界面卡住。
  3. 千分位正则只覆盖「1–3 位非零首组」：`12,345` 会被拒（它也可能是 12.345），这是安全方向，但值得在 §8 写一句「歧义写法一律拒绝」。
