# REVIEW-TASK-009: 独立评审记录

- 审查任务：`TASK-009`
- 审查 commit：`1d6d3edd8a1fe1f8a2e3d222d51db4dbcad744de`
- 审查范围：`git diff main...HEAD`（`14eb69d`..`1d6d3ed`，3 个提交：`c15da03` + `2e7777b` + `1d6d3ed`，11 文件 +343/−32）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`
- 轮次：第 2 轮（复审）。第 1 轮针对 `2e7777b` 的结论为 `REJECT`，仅因 `LAST_HANDOFF.md` / `TEST_REPORT.md` 未记录本次运行；该记录已被本次提交覆盖。

评审时工作区：`git status --short` 空；`git log --oneline -3` = `1d6d3ed` / `2e7777b` / `c15da03`。评审中的变异均用临时副本还原，复核后 `git status --short` 复为空。

## 1. Scope

**裁决：通过。**

- 文件与 TASK-009 §5 Expected Files 全部对应：新增 `tools/verify-all.ps1`、`ADR-014`、`TASK-009.md`；修改 `AGENTS.md`（§5）、`TASK_TEMPLATE.md`（§10）、`tools/check-governance.ps1`（第 21 项）、`README.md`、`PROJECT_STATUS.md`、`LAST_HANDOFF.md`、`TEST_REPORT.md`、`TASK-009.md`，以及本次提交纳入的 `REVIEW-TASK-009.md`。
- 无产品代码、无依赖、无 module、无权限、无 schema 改动（`git diff main...HEAD --name-only` 只含 markdown 与 `tools/*.ps1`）。
- 无顺手重构：`check-governance.ps1` 仅追加第 21 项及其配套注释，既有 20 项未改。

## 2. Diff 核对

**裁决：通过。**

- `git diff --check`：**exit 0**，无空白错误；检查器「冲突标记」项 PASS。
- 提交聚焦：`c15da03` 实现门禁 + 基线修订（同 commit 带 `ADR-014`）；`2e7777b` 只删一句指向未创建任务文件的引用；`1d6d3ed` 只补证据与检查器锚定。没有把不相关改动塞进同一提交。
- 第 21 项锚定在 §10 表格行，而非全文任意提及（见 §4 变异 A）：`check-governance.ps1:351-354` 先按 `## 10. Verification Evidence` 切段，再要求 `(?m)^\|[^\r\n]*verify-all[^\r\n]*\|`。
- 切点 `[int]$Matches[1] -lt 9` 与第 18 项 `-lt 7` 同构；豁免在 `ADR-014` 决策 4、`README.md:73`、`TASK_TEMPLATE.md:42` 三处记录，属有意设计。

## 3. 架构与数据

**裁决：通过。**

- 不触及 `ARCHITECTURE.md`/`DATABASE.md` 的分层、schema、迁移、日期口径；无 Room/Composable/DAO/Receiver 改动。
- `AGENTS.md` 是 §11 受保护基线：同 commit 内带 `ADR-014`，检查器「baseline changes carry an ADR」PASS。
- 授权链：`ADR-014` 记录用户原话「你来决定便是」及其紧接的 AI 提议；本轮新增「受保护文件的最小改动」一节，逐条写明 `AGENTS.md` §5 删 3 行/增 4 行、`TASK_TEMPLATE.md` §10 增 1 条要求与示例，并声明其余基线文档未动。经与 `git diff main...HEAD -- AGENTS.md` 核对，行数与范围**一致**。授权依据成立，不构成越权。
- 未改任何产品行为，故不涉及 ADR 与架构一致性冲突。

## 4. 验证证据

**裁决：通过（脚本行为、失败传播与第 21 项均经变异实测）。**

| 复核项 | 实际执行 | 结果 |
|---|---|---|
| 统一入口 | `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS**，exit **0**；治理 7s / 单测 25s `OK (40 tests)` / 构建 21s `BUILD SUCCESSFUL` / 空白 0s |
| 治理检查 | `pwsh -NoProfile -File tools/check-governance.ps1` | `SUMMARY: 21 checks, 21 passed, 0 warnings, 0 failed` / `RESULT: OK`，exit **0** |
| 空白检查 | `git diff --check` | exit **0** |
| 变异 A：保留 §5 的 `verify-all` 提及，替换 §10 证据行 | 改动后运行 checker | `[FAIL] verify-all evidence  TASK-009 has no verify-all evidence row in section 10`，20 passed / 1 failed，exit **1**；此时文件内仍有 12 处 `verify-all` 字样——**锚定生效，不再被任意提及骗过** |
| 变异 B：§10 行改成 `| verify-all | TBD | TBD |` | 改动后运行 checker | 仍 21/21 PASS（见 §7 建议 1，非阻塞） |
| 变异还原 | 从临时副本恢复 | `git status --short` 空，`git diff --check` exit 0 |
| 门禁失败传播（第 1 轮已实测，代码未改） | 仅治理违规 / 治理+空白双违规 | 前者 exit 1 且后三步仍 exit 0，汇总 `FAILED (check-governance.ps1)`；后者汇总两步、exit 1——早失败不被掩盖，后失败不掩盖先失败 |

- `assembleDebug` 秒数：本轮实测 20s / 21s，第 1 轮 21/24/25s；TASK-009 §10 写的 22s 属同一量级，非精确可复现，不作为缺陷。
- 未直接注入构建失败/单测失败分支（与已实测的治理步共用 `$LASTEXITCODE` 机制），如实记为未直接验证项。

## 5. 文档与分支

**裁决：通过（上轮两项阻塞已闭合）。**

- `README.md`：当前任务 `TASK-009`、命令表含 `verify-all`、检查器「21 项」——与实际 21 项一致。
- `PROJECT_STATUS.md`：当前任务 `TASK-009`、ADR 列表 `ADR-001`~`ADR-014`、工具列表含 `verify-all.ps1` 与「`check-governance.ps1`（21 项）」、最近治理检查 21 项 PASS——一致。
- `LAST_HANDOFF.md`：Snapshot 已改为 `TASK-009` / `feat/verify-all-gate` / `main` @ `14eb69d`；`Verification Actually Run` 首行为 `verify-all` 并给出 21/21 与分步耗时；`Next Safe Action` 已改为「复审 `REVIEW-TASK-009.md` → Room/DAO 运行时验证」。检查器「handoff snapshot filled」PASS。
- `TEST_REPORT.md`：`Latest Run` 改为 `tools/verify-all.ps1` PASS（治理 21/21 + 40 项 + 构建 + 空白）；`Runs` 追加 3 行（首次 FAILED / 第 21 项负向验证 / 补登记后 PASS），最后一行为 PASS，故「test report summary matches」PASS——实测确认。
- `TASK-009`：§11「文档/状态已更新」现已成立；`close-task` 摘要更新为 `VERIFY ALL PASS`。
- 分支与提交：`TASK-009` 仍为 `PARTIAL` 留在 `feat/verify-all-gate`，检查器「partial tasks registered」PASS；三个提交信息与内容相符。

## 6. 第 2 轮复审：上轮阻塞项闭合情况

| 上轮阻塞项 | 本轮证据 | 状态 |
|---|---|---|
| `LAST_HANDOFF.md` 仍停留在 `TASK-008` / `main @ d7998ad` / 20/20 | Snapshot 三项均改写为 TASK-009 / `feat/verify-all-gate` / 21/21；新增 `verify-all` 证据行与新 Next Safe Action（见 `git diff 2e7777b..1d6d3ed`） | **已闭合** |
| `TEST_REPORT.md` 无本次运行记录，且与 `PROJECT_STATUS.md` 的 20/21 矛盾 | `Latest Run` 改为 verify-all PASS、治理 21/21；`Runs` 增 3 行；最后一行为 PASS，汇总检查 PASS | **已闭合** |
| TASK-009 §11「文档/状态已更新」与实际不符 | 上述两文件补齐后该勾选成立 | **已闭合** |
| （非阻塞建议）第 21 项可被任意提及骗过 | 已改为按 §10 表格行锚定；变异 A 实测 FAIL，消息为 `no verify-all evidence row in section 10` | **已采纳** |
| （非阻塞建议）ADR 未逐字记录授权到哪些基线改动 | 新增「受保护文件的最小改动」一节，行数与实际 diff 一致 | **已采纳** |

## 7. 结论

- 结论：`APPROVE`
- 证据（本轮实际命令与退出码）：
  - `pwsh -NoProfile -File tools/verify-all.ps1` → `RESULT: VERIFY ALL PASS`，exit **0**（治理 7s、单测 25s `OK (40 tests)`、构建 21s、空白 0s）
  - `pwsh -NoProfile -File tools/check-governance.ps1` → `SUMMARY: 21 checks, 21 passed, 0 warnings, 0 failed` / `RESULT: OK`，exit **0**
  - `git diff --check` → 无输出，exit **0**
  - 变异 A（§10 证据行去掉 `verify-all`，§5 提及保留）→ `[FAIL] verify-all evidence  TASK-009 has no verify-all evidence row in section 10`，20 passed / 1 failed，exit **1**
  - 变异 B（§10 行改为 `| verify-all | TBD | TBD |`）→ 21/21 PASS，exit **0**（残余弱点，见建议 1）
  - 还原后 `git status --short` 空、`git diff --check` exit **0**
- 结论说明：门禁可用、可失败、不掩盖失败；第 21 项已从「字样存在」收紧为「§10 表格行存在」并经变异验证；受保护基线的 ADR 记录已具体到行数；上轮两项文档阻塞均已闭合，无新阻塞项。

### 非阻塞建议（不影响本结论）

1. 第 21 项仍不校验行内容：变异 B 中 `| verify-all | TBD | TBD |` 依然 PASS。建议再加一道廉价约束，例如该行不得同时出现 `TBD`/`NOT_RUN`，或要求含 `PASS` 字样。这与 `ADR-014`「不验证证据新鲜度」的信任边界一致，但 `TBD` 连「证据」都不是。
2. `ADR-014`「`TASK_TEMPLATE.md` §10：增加 1 行要求与 1 行示例表头」措辞不准：实际是新增 1 行要求 + 替换 1 行示例数据行，表头未变。建议改为「示例行」。
3. `LAST_HANDOFF.md` 的 `Files Changed` 把 `TASK-008` 的产品源码（`app/src/main/...`、`app/src/test/...`）与 `tools/close-task.ps1`、`run-unit-tests.ps1`、`TASK-008.md` 一并列入本次「修改」。虽可解释为跨会话累计，但容易被读成 TASK-009 动过产品代码。建议分成「本任务（TASK-009）」与「历史（TASK-008）」两段。
4. `tools/verify-all.ps1:52-55` 的 `git diff --check` 失败时看不到定位信息：步骤体输出被 `$whitespace = Invoke-Step ...` 吞掉（第 1 轮注入行尾空白时只剩 `exit 2`），且该步不写日志。建议 `git diff --check 2>&1 | Tee-Object -FilePath .gradle/logs/verify-whitespace.log`。
5. `tools/verify-all.ps1:26` 的 `$LASTEXITCODE` 可能是上一步残留；建议 `& $Body` 前重置为 0。
6. `TASK-009` §5 Expected Files 未列 `project/reviews/REVIEW-TASK-009.md`（它随后被提交进来）；纯文档整洁问题。
7. `TASK-009` §10 的「`assembleDebug` 22s」与实测 20–21s 有 1–2 秒差；建议统一写成区间（如「约 20–25s」）。

### 提交提醒

本评审记录覆盖的是已入库的 `REVIEW-TASK-009.md`（第 1 轮内容），本次 `APPROVE` 为工作区改动，需随任务一起提交；TASK-009 置 `DONE` 后第 18 项会读取本文件的 `INDEPENDENT` 与 `APPROVE`，请勿在提交前再改写结论字段。
