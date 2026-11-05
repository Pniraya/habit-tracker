# REVIEW-TASK-018: 独立评审记录

- 审查任务：`TASK-018`
- 审查 commit：`fe958fc64a8d7831ff60545d6c473fa8f53a860a`（第 2 轮）
- 审查范围：`git diff main...HEAD`（`52e89c1`..`fe958fc`，2 个提交 `2845c16` + `fe958fc`，12 文件 +335/−28；本轮重点为 `2845c16..fe958fc` 的 6 个代码/测试文件与任务记录）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`
- 轮次：第 2 轮。第 1 轮针对 `2845c16` 判 `REJECT`（对话框仍截断小数、`onFailure` 调用点无测试、千分位被静默改写）；本轮核对这三项的修复。

评审时工作区：`git status --short` 为空（开始前）。本轮为只读核对 + 门禁复跑 + 5 组变异；改动已还原，结束时工作区仅剩本评审文件。

## 1. Scope

**裁决：通过。**

- 修复严格对应第 1 轮的三项：`TodayScreen` 预填/目标改用 `formatQuantity`；`CreateHabitPresenter` 构造参数改为函数类型并新增调用点测试；`normaliseQuantityInput` 区分小数逗号与千分位。
- 无依赖、schema、权限改动；域层 `HabitRules` 文案仍为英文（§4 既有限制）。
- 新增 3 项测试（70 = 67 + 3），与实现一一对应。

## 2. Diff 核对

**裁决：通过。**

- `git diff --check`：**exit 0**；无冲突标记。
- 对话框两处 `toLong()` 都已替换为 `TodayPresenter.formatQuantity(...)`；`normaliseQuantityInput` 不再内联在 Composable 里，成为可测的 `internal` 顶层函数。
- `AppRoot` 改为 `CreateHabitPresenter(CreateHabitUseCase(repository)::invoke, scope)`，生产路径仍走真实 UseCase。
- 千分位保护用的是「匹配则原样返回」，`1,234` 保持不可解析、`记录` 保持禁用——拒绝而不是换一种含义接受，方向正确。

## 3. 架构与数据

**裁决：通过（含 1 项设计取舍，见 §6 非阻塞 1）。**

- `normaliseQuantityInput` 与 `formatQuantity` 都在 feature 层，未下沉到 `domain/`；依赖方向未变。
- 构造函数从 `CreateHabitUseCase` 放宽为 `suspend (NewHabit, LocalDate, Long) -> Long`：签名本身没有变弱（参数与返回值都在），但丢掉了「这是一个领域 UseCase」的类型信息，`::invoke` 的可读性也差一点。
- 数据约束未变；千分位拒绝策略不会产生错误数值。

## 4. 验证证据

**裁决：通过（含 1 处未覆盖，见 §6 阻断项 2）。**

| 命令 / 变异 | 实际结果 | 退出码 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | 治理 21/21（0 警告）、单测 **OK (70 tests)**、DAO SQL 21 项 0 失败、`assembleDebug` BUILD SUCCESSFUL 19s、空白 0 项 | **0** |
| `pwsh -NoProfile -File tools/check-governance.ps1`（单独复跑） | `SUMMARY: 21 checks, 21 passed, 0 warnings, 0 failed` | 0 |
| `git diff --check` | `no whitespace errors` | 0 |
| 变异 A：`formatQuantity` → `toLong().toString()` | `Tests run: 70, Failures: 2`：`aRecordedDecimalIsShownAsADecimal`、`wholeNumbersKeepNoTrailingDecimalPoint` | **1** |
| 变异 B：`errorMessageFor` 的 `else` → 原文 | `Tests run: 70, Failures: 2`：`domainRejectionsAreTranslatedForTheUser`、`anUnknownFailureKeepsAChineseMessage` | **1** |
| 变异 C：`onFailure` 的 `error = messageFor(failure)` → `failure.message` | `Tests run: 70, Failures: 1`：**`aDomainRejectionIsTranslatedAtTheCallSite`** | **1** |
| 变异 D（本轮新增）：对话框预填 `TodayPresenter.formatQuantity(habit.actualValue)` → `habit.actualValue.toLong().toString()` | `OK (70 tests)`，**零失败** | 0 |
| 变异 E（本轮新增）：`normaliseQuantityInput` 去掉千分位保护 | `Tests run: 70, Failures: 1`：`aThousandsGroupIsNotRewrittenIntoADecimal` | **1** |
| 还原后 `tools/run-unit-tests.ps1` | `OK (70 tests)` / `RESULT: UNIT TESTS PASS` | 0 |

A/B/C/E 打红的都是对应测试，没有误伤；第 1 轮的阻断项 2（调用点）已被变异 C 证明修好——现在把 `onFailure` 改回直出英文，`aDomainRejectionIsTranslatedAtTheCallSite` 会变红。

**变异 D 的缺口**：把对话框预填改回 `toLong()`，70 项全绿。也就是说第 1 轮那个「点记录会把 0.5 覆盖成 0」的缺陷目前只有真机证据，没有回归测试；代码是对的，但它可以在无人察觉的情况下被改回去。

未验证项：Compose 渲染（对话框预填只经真机走查）；IME 在真实键盘上的逗号行为未自动化。

## 5. 文档与分支

**裁决：不通过（任务记录自相矛盾）。**

- `README.md:15`、`TEST_REPORT.md:9`、`PROJECT_STATUS.md:13`、`LAST_HANDOFF.md:27` 都已是 **70 项 / 新增 7 项**；自动摘要块也是「通过 8 项 / OK (70 tests)」。
- 🚫 `TASK-018.md` §7 仍写「新增 4 项，共 67 项」；§10 第一行仍写 `OK (67 tests)` + 「新增 4 项」，verify-all 行仍写「单测 67 项」；§11 仍写「验收标准全部满足（**5/5**）」与「67 项 + 2 组变异 + 2 项真机」。实际是 8 条验收、70 项测试、3 组变异（A/B/C/A-E 中记录在案的 A/B/C）、3 项真机。§10 也没有 70 项那次运行的记录行。
- `LAST_HANDOFF.md` 已不再把 `TASK-018` 写成「任务文件待建」，第 1 轮的非阻塞项已修。
- 分支 `fix/error-text`、两个提交、未推送，符合 `AGENTS.md` §7；工作区干净。

## 6. 结论

- 结论：`REJECT`
- 证据：三项修复在代码层面都成立（变异 A/B/C/E 各自打红正确测试，D 暴露唯一剩余缺口）；门禁 exit 0（70 项、治理 21/21、DAO 21 项、构建 19s、空白 0）；但任务记录仍停留在 67 项/5 条验收，与自动摘要和四份状态文档矛盾。
- 需修复项：
  1. **`TASK-018.md` 的计数与验收条数过期**：§7「4 项/67」、§10 两处 67、§11「5/5」「67 项 + 2 组变异 + 2 项真机」全部改成实际值（7 项新增 / 70 项、8/8、3 组变异、3 项真机），并在 §10 补一行 70 项运行记录。这是本轮唯一的硬性阻断（文档诚实性），与第 1 轮同类。
  2. **对话框预填没有回归测试**（变异 D 全绿）：建议把这段抽成 `TodayPresenter.prefillText(actualValue: Double): String`（内部就是 `if (actual > 0.0) formatQuantity(actual) else ""`），补一条 JVM 断言；成本是几行代码，收益是「0.5 被覆盖成 0」不会再悄悄回来。若坚持只保留真机证据，则必须在 §8 明写「预填逻辑无 JVM 测试、只有真机走查」，不能像现在这样既不测也不声明。
- 非阻塞建议：
  1. **函数类型构造参数是可行但更弱的取舍**。它确实让 `onFailure` 可测，但把「依赖领域 UseCase」降级成「任意同签名 lambda」，`AppRoot` 里的 `::invoke` 也降低了可读性。更干净的两种做法：① 保留 `CreateHabitUseCase` 形参，给 `FakeHabitRepository` 加一个 `createFailure: Throwable?` 注入点——测试仍能走真实的 `Presenter → UseCase → Repository` 链路，且不用改生产签名；② 定义 `fun interface CreateHabit { suspend operator fun invoke(...) }` 作为具名类型。当前实现不算错，但如果后续还有别的测试缝隙，建议优先用 ①。
  2. 千分位正则 `^\d{1,3}(,\d{3})+$` 会误伤 `0,123`（合法的 0.123）：它匹配「0 + 千分位组」，被原样保留后解析失败、按钮禁用。建议改成 `^[1-9]\d{0,2}(,\d{3})+$`，或以「整数部分为 0 时视为小数」特判。
  3. 子串映射（第 1 轮已讨论）仍可接受：§8 的「兜底而非类型化契约、退化方向安全」是诚实的；若想更稳，把文案提成 `HabitRules` 的 `const val` 供映射引用。
  4. `normaliseQuantityInput` 与 `THOUSANDS_SEPARATOR` 放在 `TodayPresenter.kt` 末尾可以，但它不属于 presenter 状态机；若后续再加输入规则，建议单独成文件。
