# REVIEW-TASK-010: 独立评审记录

- 审查任务：`TASK-010`
- 审查 commit：`16aeb1c07d21c9c22573fcf810d7b0969462ef6e`
- 审查范围：`git diff main...HEAD`（`18360bc`..`16aeb1c`，2 个提交 `69c280e` + `16aeb1c`，7 文件 +323/−11）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`

评审时工作区：`git status --short` 空。所有变异都先备份源文件、跑完立即还原；结束时 `git status --short` 仅剩本评审文件，`git diff --check` exit 0。

## 1. Scope

**裁决：通过。**

- 改动与 TASK-010 §3/§5 一致：新增 `tools/verify-dao.mjs`、`TASK-010.md`；修改 `tools/verify-all.ps1`（第 5 步）、`TEST_REPORT.md`、`PROJECT_STATUS.md`、`LAST_HANDOFF.md`、`README.md`。
- `git diff main...HEAD --name-status` 只有 markdown 与 `tools/`：**无 `app/**` 产品代码、无 `gradle/**`、无依赖、无 schema/迁移改动**。
- 无顺手重构：`verify-dao.mjs` 自带约 20 行建库逻辑而非改动已通过的 `verify-schema.mjs`，改动集中在单一新步骤（理由见 TASK-010 §8）。

## 2. Diff 核对

**裁决：通过。**

- `git diff --check`：**exit 0**；检查器「冲突标记」「no trailing whitespace」均无 FAIL。
- `verify-all.ps1` 只插入一个 `Invoke-Step 'verify-dao.mjs'`，位置在单测之后、构建之前，日志写 `.gradle/logs/verify-dao.log`，与既有步骤同构（Tee + Select-String + `$LASTEXITCODE`）。实测汇总为 5 步，DAO 步 exit 0 / 0s。
- 提交聚焦：`69c280e` 是 harness + 接线；`16aeb1c` 只同步文档，未混入脚本行为改动。

## 3. 架构与数据

**裁决：通过（含对 `@Upsert` 替身的判断）。**

- 不触碰 schema、实体、Converter、迁移；harness 读的是 Room 导出的 `1.json` DDL 和 `DatabaseConstraints.kt` 的触发器文本，属只读消费。
- 触发器数量：实测 `triggers installed: 8`，与报告一致。
- **`@Upsert` 用 `ON CONFLICT` 手写语句代替 Room 生成的 SQL**：判定为「可接受但有明确上限」，不是毫无意义，也不是等价验证。它证明了 (a) `habit_logs` 上 `(habit_id, log_date)` 唯一索引真实存在；(b) 该冲突目标上的 `DO UPDATE` 语义是更新而非重复插入。它**不能**证明 Room 对 `@Upsert` 注解生成的 SQL 一定命中这个冲突列——若注解被误写成普通 `@Insert`，本断言依然会绿。任务 §8 已把这一点写成本任务最弱的一环，判断合理。
- 须纠正的是「写进报告」的声明：TASK-010 §8 称「已写入 `TEST_REPORT.md` Known Gaps」，但该提交没有改 `Known Test Gaps` 一节，且现存文字与结论相反（见 §5）。边界本身诚实，记录不诚实。

## 4. 验证证据

**裁决：不通过。harness 的结论可复现，但记录的数字与运行事实不符。**

实测（全部为本轮亲自执行）：

| 命令 | 我的实测 | 与文档对照 |
|---|---|---|
| `node tools/verify-dao.mjs` | `triggers installed: 8`、`checks: 19, failed: 0`、`RESULT: DAO SQL PASS`，exit **0** | 19 项 / 8 触发器 **可复现** |
| `pwsh -NoProfile -File tools/verify-all.ps1` | `RESULT: VERIFY ALL PASS`，exit **0**；治理 7s、单测 21s（`OK (40 tests)`）、DAO 0s、构建 20s、空白 0s | 步骤与顺序一致；单测耗时 21s（文档 26s，属波动） |
| `pwsh -NoProfile -File tools/check-governance.ps1` | `SUMMARY: 21 checks, 20 passed, 1 warnings, 0 failed` / `RESULT: OK`，exit **0**；WARN 为 `task/adr references  unwritten or misspelled: project/tasks/TASK-010.md -> TASK-011` | **文档写「治理 21/21」，实际 20 通过 + 1 警告** |
| `git diff --check` | exit **0** | 一致 |

- 该 WARN 由本任务的 `TASK-010.md:68`（`TASK-011` 尚未创建）触发，是 TASK-009 曾用 `2e7777b`「drop reference to an uncreated task file」专门修掉的同一类问题，本任务又引入了一次。
- 其余数字（`assembleDebug` 20s、空白 0s、DAO 0s、40 项单测）均可复现或落在测量噪声内。

### 变异验证（全部亲自执行，已还原）

| # | 变异 | 结果 |
|---|---|---|
| M1 | `HabitScheduleDao.findVersionsOn` 下界 `effective_from <= :date` → `<` | `FAIL ... on the first day of the open version (expected 1) -> got 0`，1 项失败，exit **1** |
| M2 | `HabitLogDao.findRange` 的 `BETWEEN` → 开区间 | `FAIL includes both boundaries ... -> got []`、`FAIL excludes rows outside the window ... -> got []`，**2 项失败**，exit **1**（与文档一致） |
| M3 | 把 `fun findVersionsOn(` 改名 | `TypeError: Cannot read properties of undefined (reading 'matchAll')`，exit **1** —— 证明 SQL 确实按函数名从 Kotlin 源码解析，不是另抄一份 |
| M4 | `findVersionsOn` 的参数 `:date` 改名为 `:day` | `Error: unbound parameters: day, day`，exit **1** —— 绑定失败会响亮失败，不会静默跳过 |
| M5 | `HabitDao.observeActive` 去掉 `archived_on IS NULL`（改用 `WHERE 1 = 1`） | `FAIL ... observeActive after archive (expected 1) -> got 2`，1 项失败，exit **1**（与文档一致） |
| M6 | `findVersionsOn` 上界 `:date <= effective_until` → `<` | `FAIL ... inside the closed version (expected 1) -> got 0`，1 项失败，exit **1** |

结论：**断言确实有鉴别力，不是空转**。闭区间在上下两个边界各有一个可证伪的断言（M1 打中下界、M6 打中上界），`BETWEEN` 的 2 项失败与文档一致，CAS 与归档过滤也都被独立验证。还原后 `checks: 19, failed: 0`、`git status --short` 空。

## 5. 文档与分支

**裁决：不通过（报告未同步且与任务结论矛盾）。**

已同步、核对无误：
- `README.md:12` 当前任务 `TASK-010`；`:16` 数据层校验行含 DAO SQL 19/19；`:62` 命令行含 `node tools/verify-dao.mjs`。
- `PROJECT_STATUS.md` 当前任务 `TASK-010`、最近验证数据层含 `verify-dao.mjs` 19/19、数据层清单加了 DAO 语句验证。
- `LAST_HANDOFF.md` Snapshot 改为 `feat/dao-sql-verification` / `main @ 18360bc`，`Not Verified` 正确列出 Room 映射 / Converter / Flow / 事务，`Next Safe Action` 指向评审与 ViewModel。
- `TASK-010` §10 的 19 项、8 触发器、两组变异（1 项 / 2 项）均与我的实测一致。

不符合项（阻塞）：
1. **`TEST_REPORT.md` 的 `Known Test Gaps` 仍写着「Room 事务、DAO 查询、`@Upsert` 行为没有运行时证据，需要 Robolectric 或真机」。** 本任务之后「DAO 查询」的 SQL 语义已有 19 项运行时证据，而 `@Upsert` 恰恰是被 `ON CONFLICT` 替身部分覆盖、需要写明上限的那一项。这句话与任务自身结论相反，且没有记录 upsert 替身这个最弱环节。
2. **TASK-010 §8「已写入 `TEST_REPORT.md` Known Gaps」不成立**：该提交只往 `Runs` 追加了 3 行，`Known Test Gaps` 一字未改。验收项 5 要求「覆盖边界写入任务与**报告**」，任务文件写了、报告没写，勾选 `[x]` 与实际不符。
3. **`TEST_REPORT.md` 的 `Latest Run` 过期**：仍是 `main` @ `7c8b074`，结果行只提治理/单测/构建/空白，不含新增的 `verify-dao` 步骤；而 `PROJECT_STATUS.md` 已宣称数据层验证包含 19/19。检查器的「test report summary matches」只比较 `Latest Run` 与最后一行是否同类，抓不到这种滞后。
4. **TASK-010 §10 的「治理 21/21」不实**：实测 `21 checks, 20 passed, 1 warnings`，警告由本任务 §12 引用未创建的 `TASK-011` 造成。既然 `2e7777b` 已专门修过同类问题，本任务不应再引入。

## 6. 结论

- 结论：`REJECT`
- 判定说明：**harness 本体通过**——SQL 真从 DAO 源码解析、断言有鉴别力、绑定失败会响亮报错、失败退出码正确，TASK-010 的核心目标成立。阻塞点全部在记录诚实性：任务声称「覆盖边界已写入报告」而报告写的是相反结论；`Latest Run` 未更新；`§10` 的治理数字与实测不符并夹带一个新警告。按 `AGENTS.md` §4「不得伪造…测试结果」与 `REVIEW_CHECKLIST.md` §3/§4，这些必须先修。
- 证据（本轮实际命令与退出码）：
  - `node tools/verify-dao.mjs` → `triggers installed: 8` / `checks: 19, failed: 0` / `RESULT: DAO SQL PASS`，exit **0**
  - `pwsh -NoProfile -File tools/verify-all.ps1` → `RESULT: VERIFY ALL PASS`，exit **0**（治理 7s、单测 21s、DAO 0s、构建 20s、空白 0s，共 5 步）
  - `pwsh -NoProfile -File tools/check-governance.ps1` → `SUMMARY: 21 checks, 20 passed, 1 warnings, 0 failed` / `RESULT: OK`，exit **0**
  - `git diff --check` → exit **0**
  - 变异 M1/M2/M3/M4/M5/M6 → 全部 exit **1**，失败项分别为 1 / 2 / TypeError / unbound params / 1 / 1；还原后 19/0、exit 0、工作区为空
- 需修复项：
  1. 重写 `TEST_REPORT.md` 的 `Known Test Gaps`：明确「`verify-dao.mjs` 覆盖 SQL 语义、CAS、闭区间、唯一索引、级联删除」；「不覆盖 Room 的 Cursor 映射、TypeConverter 往返、Flow 失效通知、`withTransaction`、Room 生成的 `@Upsert` SQL（harness 用 `ON CONFLICT` 替身，只能证明唯一索引与冲突形状）」。
  2. 更新 `TEST_REPORT.md` 的 `Latest Run` 为本次分支与本次运行（含 `verify-dao` 19/19 与治理的实际计数）。
  3. 修正 TASK-010 §10 的「治理 21/21」为实测值；删除 §12 的 `TASK-011` 引用（或先创建任务文件），让治理回到 21/21、0 警告。
  4. 让 §8「已写入 `TEST_REPORT.md` Known Gaps」为真（随第 1 项一起完成，或删掉该句）。

### 非阻塞建议（不影响 harness 结论）

1. `README.md:17` 仍写「ADR `ADR-001`~`ADR-013` 全部 `ACCEPTED`」，但 `ADR-014` 早已在 `main` 上。这是 TASK-009 合入时遗留（当时的评审也没抓到），建议一并改为 `ADR-014`。
2. 诊断信息可以更好：M3 改名、M4 改参数名、以及任何 SQL 语法错误都会以未捕获异常中止整个脚本，没有 `FAIL` 行、没有 `checks:` 汇总（我最初一次把 `observeActive` 改成非法 SQL，只看到 `SqliteError` 与 exit 1）。建议把「查不到查询/参数绑不上」包进 `ok()`，输出 `FAIL  <查询名> -> missing/unbound`，比堆栈更可定位。
3. `queryOne()`（`verify-dao.mjs:147`）没有 `query()` 里的 unbound 参数守卫，参数改名时会落到 `node:sqlite` 的 `TypeError: Provided value cannot be bound`；建议两个入口复用一个绑定函数。
4. `closeVersion` 断言写死 `{ id: 2 }`，依赖 fixture 的插入顺序；建议改用 `lastInsertRowid` 一类变量，避免以后加 fixture 时静默错位。
5. `LAST_HANDOFF.md` 说「19 项断言覆盖……每天最多一个计划版本」：schema 没有该约束，harness 证明的是「给定 fixture 下每个日期恰好命中一行」，不是「DB 会拒绝第二个重叠版本」。建议措辞收窄。
6. `LAST_HANDOFF.md` 的 `Verification Actually Run` 未给出 `verify-dao` 的耗时与 5 步汇总，与 TASK-010 §10 的粒度不一致；下次同步时可补。
