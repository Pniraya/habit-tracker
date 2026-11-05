# REVIEW-TASK-010: 独立评审记录

- 审查任务：`TASK-010`
- 审查 commit：`44ff0af2641803027f6c19021d2bb74efbec4642`
- 审查范围：`git diff main...HEAD`（`18360bc`..`44ff0af`，4 个提交 `69c280e` + `16aeb1c` + `4774f81` + `44ff0af`，8 文件 +454/−15）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`
- 轮次：第 3 轮（第 2 轮复审的修订版）。第 1 轮针对 `16aeb1c` 的结论为 `REJECT`（记录问题），第 2 轮针对 `44ff0af` 起初判为通过，复查检查器输出后发现残留问题，本记录为最终结论。

评审时工作区：`git status --short` 空（开始前）。变异均先备份 DAO 源文件、跑完立即还原；结束时工作区仅剩本评审文件，`git diff --check` exit 0。

## 1. Scope

**裁决：通过。**

- 与 TASK-010 §3/§5 一致：新增 `tools/verify-dao.mjs`、`TASK-010.md`；修改 `tools/verify-all.ps1`（第 5 步）、`TEST_REPORT.md`、`PROJECT_STATUS.md`、`LAST_HANDOFF.md`、`README.md`。
- `git diff main...HEAD --name-status` 只有 markdown 与 `tools/`：**无 `app/**` 产品代码、无 `gradle/**`、无依赖、无 schema/迁移改动**（变异只发生在本地并已还原）。
- 无顺手重构；`verify-schema.mjs` 未被改动。

## 2. Diff 核对

**裁决：通过。**

- `git diff --check`：**exit 0**；无冲突标记、无行尾空白。
- `verify-all.ps1` 单步插入在第 2、3 步之间（单测之后、构建之前），日志写 `.gradle/logs/verify-dao.log`；实测汇总为 5 步。
- 提交聚焦：`69c280e` harness + 接线；其余三个提交只改文档，未在改文档的同时改断言。

## 3. 架构与数据

**裁决：通过。**

- 不触碰 schema、实体、Converter、迁移；harness 只读消费 `1.json` DDL 与 `DatabaseConstraints.kt` 触发器文本。实测 `triggers installed: 8`。
- `@Upsert` 的 `ON CONFLICT` 替身：判定为「可接受但有明确上限」——证明 `(habit_id, log_date)` 唯一索引存在、该冲突列上 `DO UPDATE` 为更新；不能证明 Room 生成的 SQL 一定选用该冲突列。该上限已写入 `TEST_REPORT.md` 的 Known Test Gaps 未覆盖项第 5 条，与 §8 一致。

## 4. 验证证据

**裁决：通过（数字可复现，断言有鉴别力）。**

| 命令 | 我的实测 | 与文档对照 |
|---|---|---|
| `node tools/verify-dao.mjs` | `triggers installed: 8`、`checks: 20, failed: 0`、`RESULT: DAO SQL PASS`，exit **0** | 20 项 / 8 触发器 **可复现** |
| `pwsh -NoProfile -File tools/verify-all.ps1` | `RESULT: VERIFY ALL PASS`，exit **0**；治理 7s、单测 22s（40 项）、DAO 0s、构建 19s、空白 0s | 5 步与顺序一致 |
| `pwsh -NoProfile -File tools/check-governance.ps1` | `SUMMARY: 21 checks, 20 passed, 1 warnings, 0 failed`，exit **0**；WARN 的来源见 §5 | 见 §5 |
| `git diff --check` | exit **0** | 一致 |

### 变异验证（本轮亲自执行，已还原）

| # | 变异 | 结果 |
|---|---|---|
| M1 | 把 `fun findVersionsOn(` 改名 | 新增守卫先报 `FAIL every expected DAO query was parsed out of the Kotlin sources -> not parsed: HabitScheduleDao.findVersionsOn`，随后 `Error: query is missing: the DAO function name was not found in the Kotlin source`，exit **1** |
| M2 | `HabitLogDao.findRange` 的 `BETWEEN` 改开区间 | `FAIL includes both boundaries ... -> got []`、`FAIL excludes rows outside the window ... -> got []`，**2 项失败**，exit **1** |
| M3 | `findVersionsOn` 的参数 `:date` 改名 `:day` | `Error: unbound parameters: day, day`，exit **1** |

- 还原后 `checks: 20, failed: 0`、exit 0、`git status --short` 空。
- 第 1 轮已实测的下界/上界 `<=` 改 `<`、归档过滤删除，对应代码路径本轮未变，仍有效（闭区间上下两端各有一个可证伪断言）。
- 小结：解析、绑定、边界、CAS、级联删除都能被打红，失败信息可定位。

## 5. 文档与分支

**裁决：不通过（仅剩一处，但会持续制造警告并作出错误归因）。**

已核对无误：
- `TEST_REPORT.md` `Latest Run` 已改为 `feat/dao-sql-verification` 且结果行含 DAO SQL 20 项。
- `Known Test Gaps` 已重写为「已覆盖 / 未覆盖」，含 Cursor 映射、TypeConverter、Flow、`withTransaction` 与并发、Room 生成的 `@Upsert`/`@Insert` SQL 及其原因。
- TASK-010 §8 改为指向该条目的具体位置；§12 已删除未创建任务编号的引用。
- `README.md` 当前任务、数据层行（20 项）、ADR 范围 `014`、命令列表均正确。
- `PROJECT_STATUS.md` 与 `LAST_HANDOFF.md` 的当前任务/分支/数据层行已同步；handoff 的"一天最多一个有效版本"已改为"由写入器与事务保证，schema 无此约束"。

不符合项（阻塞）：
- **TASK-010 §10 的证据行自身仍写着那个未创建的任务编号**（`TASK-010.md:57`：该行括号内直接写有那个未创建任务编号的字面量（本记录不复述该编号）。检查器的引用扫描会遍历所有已跟踪的 `.md`，因此**这一行本身**就是当前唯一触发源，检查器输出也明确写着 `project/tasks/TASK-010.md -> <该编号>`。§10 括号里"非本任务文件"的归因与实测相反。
- 我已把自己的评审记录里对该编号的引用删除（本记录不再出现该编号）。删完后重新运行检查器，警告没有消失，仓库内该编号只剩 `TASK-010.md:57` 一处（`grep -r` 全仓验证）。因此这不是评审记录造成的问题，也不是"已在 §12 修掉"就能清除的：只要这行文字保留，项目就会长期带着 1 个 WARN，`TEST_REPORT.md` `Latest Run` 里"治理 21/21"也就不成立。

## 6. 第 2 轮复审：上轮阻塞项闭合情况

| 上轮阻塞项 | 本轮证据 | 状态 |
|---|---|---|
| Known Test Gaps 与结论矛盾 | 重写为"已覆盖 / 未覆盖"，含 `@Upsert` 生成 SQL 的上限 | **已闭合** |
| §8「已写入 Known Gaps」不成立 | 改为指向具体条目 | **已闭合** |
| `Latest Run` 过期 | 改为 `feat/dao-sql-verification`，含 DAO 20 项 | **已闭合** |
| §10「治理 21/21」不实、且 WARN 来自本任务 | §12 的编号引用已删；但 §10 的解释句又把该编号写了回来，WARN 仍在 | **未闭合（见 §5）** |

上轮非阻塞项落实：README ADR 范围改 `014` ✓；缺失查询报 `query is missing`、参数报 `unbound parameters` ✓；新增"所有预期 `@Query` 均被解析"守卫（20 项）✓；`closeVersion` 改用 `lastInsertRowid` ✓；handoff 的版本措辞收窄 ✓。

## 7. 结论

- 结论：`REJECT`
- 判定说明：**harness 与覆盖说明都已达标**，本轮唯一不通过的是 TASK-010 §10 的那句归因：它既与检查器输出相反，又因为把编号写进了自己的文档而成为警告的唯一来源，使"21/21"和"非本任务文件"两个说法同时失真。修改量是一行文字加两个计数，不涉及脚本行为。
- 证据（本轮实际命令与退出码）：
  - `node tools/verify-dao.mjs` → `checks: 20, failed: 0` / `RESULT: DAO SQL PASS`，exit **0**（8 个触发器）
  - `pwsh -NoProfile -File tools/verify-all.ps1` → `RESULT: VERIFY ALL PASS`，exit **0**（治理 7s、单测 22s/40 项、DAO 0s/20 项、构建 19s、空白 0s，共 5 步）
  - `pwsh -NoProfile -File tools/check-governance.ps1` → `SUMMARY: 21 checks, 20 passed, 1 warnings, 0 failed` / `RESULT: OK`，exit **0**；WARN 唯一来源 `project/tasks/TASK-010.md`
  - `git diff --check` → exit **0**
  - 变异 M1/M2/M3 → 全部 exit **1**（守卫 `not parsed` + `query is missing`；2 项边界失败；`unbound parameters`），还原后 20/0
- 需修复项：
  1. 删除 TASK-010 §10 证据行中的未创建任务编号（改成「另一处对尚未创建编号的引用」之类不带编号的描述），使检查器回到 `21 checks, 21 passed, 0 warnings`；随后 `TEST_REPORT.md` `Latest Run` 的「治理 21/21」才成立。
  2. 顺手把两处过期计数改为 20：TASK-010 §11「（19 项 + 2 组变异）」、`LAST_HANDOFF.md` `Verification Actually Run` 表里的「19 项断言」。

### 非阻塞建议（不影响 harness 结论）

1. 解析守卫是「预期集合 ⊆ 已解析集合」：DAO 新增 `@Query` 而没人补断言时不会提示。建议改为集合相等（或对多出的查询输出提示），让新查询无法静默漏测。
2. 守卫 FAIL 之后仍会因未捕获的 `query is missing` 中止，没有 `checks:` 汇总（exit 1 正确、消息可读）。建议让 `query()`/`queryOne()` 在守卫未通过时返回空结果，由 `equal` 报错，使一次运行列出全部问题。
3. `TEST_REPORT.md` `Latest Run` 建议写成「21 项检查、0 警告」，避免把"检查数"与"通过数"混成一个分数。
4. `LAST_HANDOFF.md` 的 `Verification Actually Run` 未列出 5 步耗时与 DAO 步的 0s，粒度与 TASK-010 §10 不一致。
