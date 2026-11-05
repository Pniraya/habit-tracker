# REVIEW-TASK-017: 独立评审记录

- 审查任务：`TASK-017`
- 审查 commit：`f01cfa190fda2467f315cfb73579eeb78a6b5e7c`
- 审查范围：`git diff main...HEAD`（`617482c`..`f01cfa1`，1 个提交，7 文件 +154/−36：`tools/check-governance.ps1` 两处判定逻辑 + `TASK-015`/`TASK-017`/`TASK-020`/状态文件）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`

评审时工作区：`git status --short` 为空（开始前）。本轮为只读核对 + 门禁复跑 + 独立重放 P5–P11 并追加 3 个自建探针；全部变异已用 `git checkout --` 还原，结束时工作区仅剩本评审文件。

## 1. Scope

**裁决：通过。**

- 只改两项检查（第 15、21 项）与文档，未改源码、依赖、schema；`TASK-020` 是新建任务文件，不是功能改动。
- 与 `TASK-017` §3 逐条对应：第 21 项改为「结果列判定词」，第 15 项只扫 `## Runs` 且缺计数即 FAIL，`NOT_STARTED` 容忍占位行。

## 2. Diff 核对

**裁决：通过。**

- `git diff --check`：**exit 0**；无冲突标记。
- 第 21 项：`$rows` 仍按行筛 `verify-all.ps1`，但只有 `$rowCells[2]` 命中 `VERIFY ALL (PASS|FAILED)|^PASS$|^FAILED$|未运行（任务未开始）` 的行才算证据；无判定词的行在任何位置都被忽略（P5/P6 同时修好）。
- 第 21 项：`$verdicts.Count -eq 0` 时只有 `NOT_STARTED` 放行；否则「最后一个判定行」不得写 `未运行|未执行|尚未`。`NOT_STARTED` 的判定用任务状态行，不再与代码注释矛盾。
- 第 15 项：`$runsBlock` 取自 `## Runs` 到下个 `## ` 之间；`Latest Run` 缺 `单测 N 项` 从「跳过」改为「FAIL」；`## Runs` 缺失或段内无 `OK (N tests)` 也各有独立失败信息。
- 新增 `TASK-020` 把「manifest 不得含 `INTERNET`」拆出去，本提交不含该项，与 §4 一致。

## 3. 架构与数据

**裁决：通过。**

- 无代码改动，权限、依赖、schema 未变；`AndroidManifest.xml` 仍只有 `POST_NOTIFICATIONS`、`RECEIVE_BOOT_COMPLETED`。
- 检查器仍保持「纯文本、无外部依赖」的定位，未引入 Pester 或网络调用；`TASK-020` 的拆分为单一职责，方向正确。

## 4. 验证证据（独立重放）

**裁决：通过——7 个声明探针全部复现，退出码与 §10 一致。**

每个探针都在**断言替换确实改变了文件内容**后才执行检查，结束后 `git checkout --` 还原并确认 `git status` 干净。

| 探针 | 操作 | 期望 | 实际 |
|---|---|---|---|
| P5 | `TASK-019` §10 证据行下方插入备注行（含命令名与「未运行」字样） | PASS | **exit 0** ✓ |
| P6 | 证据行结果列改「待补」 | FAIL | **exit 1**：`TASK-019 has no verify-all row whose result column states a verdict` ✓ |
| P7 | `Latest Run` 去掉「单测 71 项」 | FAIL | **exit 1**：`Latest Run does not state 单测 N 项, so the count cannot be checked` ✓ |
| P8 | `Known Test Gaps` 段写 `OK (59 tests)` | PASS | **exit 0** ✓ |
| P9 | `TASK-019` 改 `NOT_STARTED` + 占位证据行 | PASS | **exit 0** ✓ |
| P10 | 结果列改 `VERIFY ALL FAILED` | PASS | **exit 0** ✓ |
| P11 | `Latest Run` 计数改 70（实际 71） | FAIL | **exit 1**：`Latest Run says 单测 70 项 but the last OK row is 71 tests` ✓ |
| P12（本评审新增） | 真实行下方插入「结果列 = `未运行（任务未开始）`」的备注行 | FAIL（假阳性边界） | **exit 1**：被当成最后一个判定行 ✓（见 §6） |
| P13（本评审新增） | 结果列写中文「通过」 | 期望 FAIL（词表不含） | **exit 1**：`has no verify-all row whose result column states a verdict` ✓（行为符合实现，但是可用性边界，见 §6） |
| P14（本评审新增） | 删除 `## Runs` 标题 | FAIL | **exit 1**：`TEST_REPORT has no ## Runs section to compare the count against` ✓ |
| P15（本评审新增） | `Known Gaps` 写 `OK (59 tests)` 且 `Latest Run` 也改 59 | FAIL（证明比对锚在 Runs） | **exit 1**：`Latest Run says 单测 59 项 but the last OK row is 71 tests` ✓ |
| 基线 | 还原后 `check-governance.ps1` | 21/21、0 警告 | **exit 0** ✓ |

| 命令 | 结果 |
|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS（exit 0）**：治理 21/21、`OK (71 tests)`、DAO 21 项、BUILD SUCCESSFUL 19s、`no whitespace errors` |
| `pwsh -NoProfile -File tools/check-governance.ps1` | `SUMMARY: 21 checks, 21 passed, 0 warnings, 0 failed`，exit 0 |

未验证项：`TASK-017` 的检查器自身没有自动化测试（本机无 Pester），证据只有探针；本评审也是重放探针，不是永久回归测试。

## 5. 文档与分支

**裁决：通过（1 处计数需在标 `DONE` 前改）。**

- `TASK-015` §8 新增「边界补记」，P5–P9 与「防事故不防造假」都写在明处，与本轮探针结果一致。
- `TASK-020` 建档齐全（Objective/Scope/AC），`NOT_STARTED`，其 §10 占位行「尚未开始（`NOT_STARTED`）」在新检查下合法（P9 设计）。
- `README.md:12`、`PROJECT_STATUS.md:8`、`LAST_HANDOFF.md:8` 都写 `TASK-017` `PARTIAL` 待评审，与分支状态一致；`main` 在 `TASK-019` 合入后为 `617482c`，分支领先 1 个提交。
- 残留：`TASK-017.md` §11 写「验收标准全部满足（**7/7**）」，但 §6 实际 **8** 条（P6/P5/P7/P8/P9/P11/P10/21-21），自动摘要也是「通过 8 项」。请标 `DONE` 前改成 8/8。这是摘要计数，不影响 §10 的证据行，故按本轮标准列为「必须修、不阻断合并」。

## 6. 结论

- 结论：`APPROVE`
- 证据：`verify-all` exit 0（治理 21/21、0 警告、71 项、DAO 21 项、构建 19s、空白 0）；P5–P11 七个声明的探针全部独立复现且退出码一致；自建 P12–P15 进一步确认比对锚点正确、失败信息可读。
- 需在 `DONE` 前修正（不阻断合并）：`TASK-017.md` §11 的「7/7」改「8/8」。
- 对我所提四个问题的回答：
  1. **证据可信，但有一个流程弱点**。我逐条断言了「替换改变了文件」再跑检查，7 个探针的退出码与 §10 完全一致；第一次脚本静默不改却报 PASS 的教训本身也被写进了 §8。弱点是探针脚本 `.gradle/probes.ps1` 不入库，别人只能按 §10 重建——建议把它放进 `tools/`（或在 `TASK-015`/`TASK-017` 里保存探针全文），否则这份证据会随时间不可复现。
  2. **判定词用固定小词表是对的**，因为它是「结果列必须表达结论」的封闭词表，而不是自由文本匹配；比「非占位即可」强得多（P6 就是后者放过的）。但两个可用性边界应写进模板或 §8：中文「通过」会被判为无判定词（P13），以及 `未运行（任务未开始）` 未加锚定，备注行若原样引用该串会被当成最后判定行（P12）。建议在 `TASK_TEMPLATE` §10 明确「结果列必须使用 `VERIFY ALL PASS`/`PASS`/`FAILED` 等英文判定词」，并把占位符匹配锚定到整格。
  3. **`NOT_STARTED` 的豁免是诚实的，不是借口**——纯文本检查无法把状态与代码改动绑定，这一点 §8 写得很清楚。但洞比描述的更宽：把任务改成 `NOT_STARTED` 同时也绕过了第 18 项（`DONE` 需评审）与第 19 项（`PARTIAL` 登记）。有一个便宜的检测可以缩小它：**`NOT_STARTED` 却已勾选验收标准、或 §9 有实现日志、或 §10 有真实结果行 → FAIL**。建议作为 `TASK-017` 的后续项（或并入 `TASK-020`），不必在本提交做。
  4. **拆分是对的**。`INTERNET` 守卫是新增检查项，会改「治理 N 项」的计数并牵动一批文档；混进「检查器加固」提交会让两类风险绑在一起回滚。`TASK-020` 已建档且 `NOT_STARTED`，拆分成立。
