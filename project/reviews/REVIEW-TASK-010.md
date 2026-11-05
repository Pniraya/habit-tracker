# REVIEW-TASK-010: 独立评审记录

- 审查任务：`TASK-010`
- 审查 commit：`e01a10322d1a1e41a4db0d2ed2fb08f814d156d8`
- 审查范围：`git diff main...HEAD`（`18360bc`..`e01a103`，5 个提交 `69c280e` + `16aeb1c` + `4774f81` + `44ff0af` + `e01a103`，8 文件 +473/−16）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`
- 轮次：第 4 轮。第 1 轮（`16aeb1c`）因报告未同步判 `REJECT`；第 2/3 轮（`44ff0af`）复审后因 §10 归因错误且自带警告修正为 `REJECT`；本轮针对 `e01a103` 复审，结论见 §7。

评审时工作区：`git status --short` 空（开始前）。所有变异先备份再改、跑完立即还原；结束时工作区仅剩本评审文件，`git diff --check` exit 0。

## 1. Scope

**裁决：通过。**

- 与 TASK-010 §3/§5 一致：新增 `tools/verify-dao.mjs`、`TASK-010.md`；修改 `tools/verify-all.ps1`（第 5 步）、`TEST_REPORT.md`、`PROJECT_STATUS.md`、`LAST_HANDOFF.md`、`README.md`。
- `git diff main...HEAD --name-status` 只有 markdown 与 `tools/`：**无 `app/**` 产品代码、无 `gradle/**`、无依赖、无 schema/迁移改动**（变异只发生在本地且已还原，工作区最终为空）。
- 无顺手重构；`verify-schema.mjs` 未被改动。

## 2. Diff 核对

**裁决：通过。**

- `git diff --check`：**exit 0**；无冲突标记、无行尾空白。
- `verify-all.ps1` 单步插入在单测与构建之间，日志写 `.gradle/logs/verify-dao.log`；实测汇总 5 步。
- 提交聚焦：`69c280e` harness + 接线；其余四个提交只改文档与 harness 的守卫/场景，未混入产品代码。

## 3. 架构与数据

**裁决：通过。**

- 不触碰 schema、实体、Converter、迁移；harness 只读消费 `1.json` DDL 与 `DatabaseConstraints.kt` 触发器文本。实测 `triggers installed: 8`。
- @Upsert 用 `ON CONFLICT` 替身：判定为「可接受但有明确上限」——证明 `(habit_id, log_date)` 唯一索引存在、该冲突列上 `DO UPDATE` 为更新；不能证明 Room 生成的 SQL 一定选用该冲突列。上限已写入 `TEST_REPORT.md` 的 Known Test Gaps 未覆盖项第 5 条，与 §8 一致。

## 4. 验证证据

**裁决：通过（21 项、数字可复现、失败路径有汇总）。**

| 命令 | 我的实测 | 与文档对照 |
|---|---|---|
| `node tools/verify-dao.mjs` | `triggers installed: 8`、`checks: 21, failed: 0`、`RESULT: DAO SQL PASS`，exit **0** | 21 项 / 8 触发器 **可复现** |
| `pwsh -NoProfile -File tools/verify-all.ps1` | `RESULT: VERIFY ALL PASS`，exit **0**；治理 7s、单测 22s（40 项）、DAO 0s（21 项）、构建 19s、空白 0s | 5 步与顺序一致 |
| `pwsh -NoProfile -File tools/check-governance.ps1` | `SUMMARY: 21 checks, 21 passed, 0 warnings, 0 failed` / `RESULT: OK`，exit **0** | 一致 |
| `git diff --check` | exit **0** | 一致 |

### 破坏性验证（本轮亲自执行，全部还原）

| # | 变异 | 结果 |
|---|---|---|
| M1 | 把 `fun findVersionsOn(` 改名 | 守卫先报 `FAIL every expected DAO query was parsed out of the Kotlin sources -> not parsed: HabitScheduleDao.findVersionsOn; unexpected @Query not covered by a scenario: HabitScheduleDao.findVersionsOnRenamed`；随后场景块内抛出并被捕获为 `FAIL uncaught: query is missing: the DAO function name was not found in the Kotlin source`；**仍打印 `checks: 13, failed: 2` 与 `RESULT: DAO SQL FAILED`**，exit **1** |
| M2 | 从 expected 注册表删掉 `observeRange` | `FAIL ... -> unexpected @Query not covered by a scenario: HabitLogDao.observeRange`，`checks: 21, failed: 1`，`RESULT: DAO SQL FAILED`，exit **1** —— 集合相等守卫双向生效 |
| M3 | 只删掉 `observeRange` 的场景、保留注册 | `checks: 20, failed: 0`、`RESULT: DAO SQL PASS`，exit **0** —— 见 §7 建议 1（残余弱点） |

- 还原后 `checks: 21, failed: 0`、exit 0、`git status --short` 空。
- 前几轮已实测且代码未变的变异（`BETWEEN` 改开区间 → 2 项失败；闭区间上/下界改 `<` → 各 1 项失败；`observeActive` 去归档过滤 → 1 项失败）仍然有效。
- 小结：解析、绑定、边界、CAS、级联删除都能被打红，且**中止时也有 FAIL 行与 checks/failed 汇总**，不再只有裸堆栈。

## 5. 文档与分支

**裁决：通过。**

- 计数全仓一致为 21：`TASK-010` §9/§10/§11、`TEST_REPORT.md` 的 `Latest Run` 与 Runs 行及 Known Gaps、`README.md:16`/`:62`、`PROJECT_STATUS.md:12`、`LAST_HANDOFF.md:19/35/37`。
- `TEST_REPORT.md` 的 `Latest Run` 现为「治理 21 项检查、0 警告 + 单测 40 项 + DAO SQL 21 项 + `assembleDebug` 20s + `git diff --check`」，与实测一致。
- `Known Test Gaps` 维持「已覆盖 / 未覆盖」两段，`@Upsert` 上限写明。
- `TASK-010` §8 指向 Known Test Gaps 具体条目；§10 不再包含任何未创建任务编号；§12 只写"任务文件尚未创建"。
- `README.md` ADR 范围 `ADR-001`~`ADR-014`；命令列表含 `verify-dao.mjs`。
- 全仓扫描所有已跟踪 `.md`：**没有任何 `TASK-0NN`（NN ≥ 011）引用**，检查器「task/adr references」PASS、0 警告。
- 分支：`TASK-010` 仍为 `PARTIAL` 留在 `feat/dao-sql-verification`，检查器「partial tasks registered」PASS。

## 6. 第 3 轮阻塞项闭合情况

| 上轮阻塞项 | 本轮证据 | 状态 |
|---|---|---|
| §10 证据行自带未创建任务编号，使"21/21"与"非本任务文件"同时失真 | 该编号已删除；检查器 `21 checks, 21 passed, 0 warnings`；全仓扫描无残留 | **已闭合** |
| §11 与 handoff 的「19 项」过期计数 | 全部改为 21（§11、`LAST_HANDOFF.md:37`） | **已闭合** |

上一轮的非阻塞建议也落实了：集合相等守卫（并借此发现 `observeRange` 未覆盖、已补场景）、中止时输出 FAIL + 汇总、`Latest Run` 改为「21 项检查、0 警告」。

## 7. 结论

- 结论：`APPROVE`
- 证据（本轮实际命令与退出码）：
  - `node tools/verify-dao.mjs` → `checks: 21, failed: 0` / `RESULT: DAO SQL PASS`，exit **0**（8 个触发器）
  - `pwsh -NoProfile -File tools/verify-all.ps1` → `RESULT: VERIFY ALL PASS`，exit **0**（治理 7s、单测 22s/40 项、DAO 0s/21 项、构建 19s、空白 0s，共 5 步）
  - `pwsh -NoProfile -File tools/check-governance.ps1` → `SUMMARY: 21 checks, 21 passed, 0 warnings, 0 failed` / `RESULT: OK`，exit **0**
  - `git diff --check` → exit **0**
  - M1（`fun` 改名）→ `checks: 13, failed: 2` + FAIL 行，exit **1**；M2（删注册）→ `checks: 21, failed: 1`，exit **1**；M3（只删场景）→ `checks: 20, failed: 0`，exit **0**（见建议 1）
  - 还原后 21/0、工作区为空
- 结论说明：harness 真的执行从 Kotlin 源码解析出的 DAO SQL，断言可证伪，覆盖与不覆盖都写明；计数全仓一致、无悬空任务编号、门禁 5 步全绿。前几轮的阻塞项全部闭合。

### 非阻塞建议

1. **场景数量没有和注册表闭环**：M3 只删场景、保留注册，运行仍是 20/20 PASS。建议加一条硬断言（例如 `if (checks !== 21) throw`），或为每个注册函数记录"已执行"标记并在结尾核对，避免覆盖率被静默削减。
2. M1 这类致命错误会让后续场景停跑（`checks: 13` 而非 21）。汇总已如实反映，属合理取舍；若想一次列出全部问题，可把致命错误降级为"跳过该 DAO 的剩余场景"。
3. `LAST_HANDOFF.md:37` 的备注「变异分别变红 1 项与 2 项」只描述两组，而 TASK-010 §11 已写"5 组变异"，两处口径可统一。
4. `TEST_REPORT.md` 的 Runs 行建议补一行本轮的破坏性验证（`fun` 改名 / 删注册 / 只删场景），与 TASK-010 §11 的"5 组变异"对应。
