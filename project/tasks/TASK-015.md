# TASK-015: 治理检查补计数一致性与中文占位符

- 状态：`DONE`（独立评审 `APPROVE` 于 `ebdee50`，第 1 轮；边界缺陷另立 `TASK-017`）
- 阶段：`Gate 3`
- 优先级：`P2`
- 依赖：`TASK-012`（暴露盲区）、`TASK-013`（评审建议）
- 创建日期：`2026-11-05`
- 完成日期：`2026-11-05`

## 1. Objective
让 `tools/check-governance.ps1` 能发现两类此前漏过的假证据：`TEST_REPORT` 的 `Latest Run` 计数与最后一次运行不一致；已开工的任务在 §10 仍写着「未运行」。

## 2. Context
`TASK-012` 第 2 轮评审发现：`Latest Run` 写「单测 57 项」、`Runs` 末行是 `OK (58 tests)`，而治理检查**依然通过**。第 15 项只比对日期与 `PASS`/`FAILED` 字样，不比对计数。
`TASK-013` 第 1 轮评审进一步指出：第 21 项的正则只认英文 `TBD|NOT_RUN`，中文「未运行」能直接通过。

## 3. Scope
- 第 15 项：从 `Latest Run` 摘要块抽出 `单测 N 项`，与 `Runs` 表中最后一次 `OK (N tests)` 的 N 比对，不一致即 `FAIL`。
- 第 21 项：`未运行|未执行|尚未` 也视为占位符，但**仅当任务状态不是 `NOT_STARTED`** 时才失败——未开工的任务本来就没有运行可言，把它判失败等于逼迫写假证据。

## 4. Out of Scope
- 不改变 `TEST_REPORT` / `close-task` 的输出格式。
- 不新增对整个仓库「所有数字互相一致」的通用检查（成本和误报都高）。

## 5. Expected Files
- 修改：`tools/check-governance.ps1`
- 新增：`project/tasks/TASK-015.md`；状态文档

## 6. Acceptance Criteria
- [x] 把 `Latest Run` 的计数改成 57 而末行是 63 时，检查失败且信息指出两个数字
- [x] 把某个 `DONE` 任务的 verify-all 行改成「未运行」时，检查失败
- [x] `NOT_STARTED` 任务写「未运行（任务未开始，无代码改动）」仍然通过（现状含 `TASK-016`）
- [x] 仓库现状 21/21 通过，无新增警告

## 7. Tests
- 负向探针 1：`TEST_REPORT` 的 `单测 63 项` → `单测 57 项`，检查必须 `FAIL`
- 负向探针 2：`TASK-014` 的 verify-all 行 → 「未运行（临时探针）」，检查必须 `FAIL`
- 正向对照：`TASK-016`（`NOT_STARTED`）的「未运行」行必须仍然通过
- `pwsh -NoProfile -File tools/verify-all.ps1`

## 8. Risks and Decisions
- **为什么按状态区分**：如果把中文「未运行」一律判失败，`NOT_STARTED` 任务只能编一个假结果；检查的本意是「做完要拿得出证据」，不是「不许承认没做」。
- **计数只取 `OK (N tests)`**：变异行写的是 `FAILED（N 项）`，不是同一语义，跳过它才能拿到「最后一次真实运行」的 N。
- 检查本身没有自动化测试（它是 PowerShell 脚本，本机没有 Pester），证据是三次人工探针；这一点如实记录，不写成「已自动化」。

### 边界补记（`TASK-017` 重放评审探针后）

- **P5 假阳性**：旧逻辑取「最后一个含命令名的行」，于是排在证据行下方的合法备注会被当成证据；若备注里恰好有「未运行」字样就更糟。现改为**只认结果列的判定词**。
- **P6 假阴性**：旧逻辑只要行内出现 `verify-all.ps1` 就算证据，结果列写「待补」也能过——**不运行也能通过**。这正是「有检查 ≠ 证据不可伪造」。
- **P7 假阴性**：`Latest Run` 不写「单测 N 项」时，计数比对被整段跳过。现改为缺失即 FAIL。
- **P8 假阳性**：计数原本扫全文，`Known Test Gaps` 里引用一句 `OK (59 tests)` 就会把比对打歪。现只取 `## Runs` 段内最后一个 `OK (N tests)`。
- **P9 注释与实现矛盾**：注释写「未开工的任务不需要证据」，代码却无条件让 `TBD|NOT_RUN` 失败。现已让 `NOT_STARTED` 容忍占位行。
- **仍然防不住的**：任务可以自我声明 `NOT_STARTED` 并写「未运行」同时提交代码，没有任何检查把状态与代码改动绑定。**本检查防事故，不防造假**；要防造假需要代码指纹或提交信息约束，属于另一个量级的设计。

## 9. Implementation Log
- 2026-11-05：第 15 项加计数比对；第 21 项加中文占位符识别，并以任务状态 `NOT_STARTED` 作为豁免条件。
- 2026-11-05：两个负向探针各失败一次并打印出可读信息；还原后 21/21 通过。
- 2026-11-05：**新检查先误伤了自己**——`TASK-015` §10 里引用了报错原文（含 verify-all 与「未运行」），且排在真实证据行之前，旧选择逻辑取第一行即报错。已改为要求行内出现 `verify-all.ps1` 并取最后一行为准；改后复跑两个负向探针仍各自失败。
- 2026-11-05：与 `TASK-011` 的告警事故同源（正文引用任务号触发检查）。规则：**引用检查报错原文时，不要连命令名一起抄进表格**。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| 负向探针 1：`Latest Run` 63 → 57 | **FAILED（第 15 项）**（预期） | `Latest Run says 单测 57 项 but the last OK row is 63 tests` |
| 负向探针 2：`TASK-014` 证据行 → 「未运行」 | **FAILED（第 21 项）**（预期） | `TASK-014 verify-all evidence row still says it never ran, but the task is past NOT_STARTED` |
| 正向对照：`TASK-016`（`NOT_STARTED`）保留「未运行」行 | **PASS** | 未开工任务不被误判 |
| `pwsh -NoProfile -File tools/check-governance.ps1` | **21/21 passed, 0 warnings** | 还原探针后复跑；证据行改为取最后一条含命令名的行 |
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS**（exit 0） | 治理 21/21、单测 63 项、DAO SQL 21 项、构建通过 |

## 11. Completion Review
- [x] 验收标准全部满足（4/4）
- [x] 相关检查实际运行（2 个负向探针 + 1 个正向对照）
- [x] `git diff --check` 通过
- [x] Diff 已审查：只改治理脚本，无产品代码、无依赖变化
- [x] 文档/状态已更新
- [x] 独立 Reviewer `APPROVE`（`ebdee50`，第 1 轮）

## 12. Handoff
- 下一步：评审 `APPROVE` 后合入 `main`；之后 `TASK-016`（界面细节与 `TASK-014` 的 3 条非阻塞项）。
- 遗留：治理脚本本身无自动化测试（无 Pester），只能靠人工探针。

## 13. Independent Review
- Reviewer record：`project/reviews/REVIEW-TASK-015.md`
- 结论：`APPROVE`（第 1 轮）
- 评审用 8 个探针验证，确认 P1/P2/P3 有效，同时找出 4 个边界缺陷并另立 `TASK-017`：
  - P5 假阳性：合法的「以后还要再跑」备注排在证据行**下方**会被误判
  - P6 假阴性：写一条含 `verify-all.ps1`、结果列是「待补」的行即可无运行通过
  - P7 假阴性：`Latest Run` 删掉「单测」二字后计数比对被静默跳过
  - P8 假阳性：`OK (N tests)` 扫描全报告，`Known Gaps` 里写一句就会污染计数
- 评审给的正确锚点：**结果列必须是机器可判的判定词**（`VERIFY ALL PASS` / `FAILED（N 项）`），或由 `close-task` 写入机器标记；「行内出现命令名」本质上分不清引用与结果
- 评审同时指出 `NOT_STARTED` 豁免可被滥用（没有任何检查把「状态」与「是否已改代码」绑定）——检查应被当作**防事故**而非防造假

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-015
- 状态：`DONE`
- 阶段：Gate 3
- 验收：通过 4 项 / 未通过 0 项
- 最近测试记录：2026-11-05 / **OK (63 tests)**
<!-- close-task:end -->
