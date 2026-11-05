# REVIEW-TASK-009: 独立评审记录

- 审查任务：`TASK-009`
- 审查 commit：`2e7777b24b610faf6d4c1403f88034844b2e8701`
- 审查范围：`git diff main...HEAD`（`14eb69d`..`2e7777b`，2 个提交：`c15da03` + `2e7777b`，8 文件 +209/−10）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`

评审时工作区状态：`git status --short` 空（评审前），`git log --oneline -3` = `2e7777b` / `c15da03` / `14eb69d`。评审过程中只做受控变异并立即还原，最终 `git status --short` 仅显示本文件。

## 1. Scope

**裁决：通过（文件范围符合），但 Scope 中一处承诺未兑现（见 §5）。**

- 改动文件与 TASK-009 §5 Expected Files 对应：新增 `tools/verify-all.ps1`、`ADR-014`、`TASK-009.md`；修改 `AGENTS.md`、`TASK_TEMPLATE.md`、`check-governance.ps1`、`README.md`、`PROJECT_STATUS.md`。
- 无顺手重构、无产品代码、无依赖、无 Gradle/module、无权限、无 schema（与 §4 Out of Scope 一致）。
- 唯一不符：§5 把 `project/handoffs/LAST_HANDOFF.md` 列为修改文件，实际 diff 未触及，详见 §5。

## 2. Diff 核对

**裁决：通过。**

- 每个文件都能对应到验收项：脚本对应 §6 第 1 项，第 21 项对应第 3 项，AGENTS/TEMPLATE 对应第 4 项，README/STATUS 对应 §3 最后一行。
- `tools/check-governance.ps1` 的改动是纯追加（`+11/−0`），未改动既有 20 项判断逻辑，未删除或绕过任何测试。
- `git diff --check`：**exit 0**，无空白错误；检查器「冲突标记」项 PASS。
- 第 21 项切点 `[int]$Matches[1] -lt 9` 与第 18 项的 `-lt 7`（`check-governance.ps1:330`）写法一致；豁免在 `ADR-014` 决策 4 明文记录（`TASK-001`~`TASK-008` 为历史记录），并在 `README.md:73`、`TASK_TEMPLATE.md:42` 复述。**属于有意设计且已有文档，不是漏掉的硬编码。**

## 3. 架构与数据

**裁决：通过（不涉及架构/数据面），受保护基线的授权判定为「依据成立但偏薄」。**

- 不触及 `ARCHITECTURE.md`/`DATABASE.md` 的行为约束、分层、schema、迁移；无 Room/Composable/DAO 改动。
- `AGENTS.md` 属于 §11 受保护基线：本 commit 内同时带 ADR，检查器「baseline changes carry an ADR」PASS。
- 授权链核对：`ADR-014` 记录了用户原话「你来决定便是」，并说明该指令紧接 AI「补 `verify-all.ps1`」的提议。这满足 §11 的书面要求（ADR + 指令来源），且上下文就是本脚本，**不构成越权**。
- 但 ADR 对提议内容只是转述（「补 `verify-all.ps1`，约 30 分钟……」），没有记录该提议同时包含「改 `AGENTS.md` §5 与 `TASK_TEMPLATE.md` §10」。读者无法从 ADR 本身确认用户批准了受保护文件的修改。判定为**非阻塞的薄弱点**（见 §6 建议 4）。

## 4. 验证证据

**裁决：通过（脚本行为与记录数字经实测复核）。**

| 复核项 | 我实际执行 | 结果 |
|---|---|---|
| 治理检查 | `pwsh -NoProfile -File tools/check-governance.ps1` | 21/21 PASS，exit **0** |
| 统一入口 | `pwsh -NoProfile -File tools/verify-all.ps1` | VERIFY ALL PASS，exit **0**；步进 7s/29s/25s，构建 24s，单测 `OK (40 tests)` |
| 空白检查 | `git diff --check` | exit **0** |
| 单步失败是否传递 | 删掉 `TASK-009` 中 `verify-all` 字样后跑 verify-all | 治理步 exit **1**（`[FAIL] verify-all evidence  TASK-009 has no verify-all evidence`），其余三步 exit 0，汇总 `RESULT: VERIFY ALL FAILED (check-governance.ps1)`，进程 exit **1** |
| 后失败是否掩盖先失败 | 同时注入治理违规 + 行尾空白 | 治理 exit 1、空白 exit 2，汇总同时列出两步，`uncommitted changes: 2 file(s)`，进程 exit **1**；后一步没有掩盖前一步 |
| 第 21 项是否空转 | 仅保留 §5 Scope 里的 `tools/verify-all.ps1` 提及，删除 §10 证据行与其余字样 | 仍 **21/21 PASS**（详见 §6 建议 1） |
| 变异还原 | — | 两次变异后 `git status --short` 均恢复为空 |

- TASK-009 §10 声称的「21 项检查」「40 项单测」**可精确复现**；「`assembleDebug` 22s」我实测 21s/24s/25s（三次），**秒数不可精确复现**，量级正确，不作为缺陷。
- 「首次 FAIL 于 `ADR-014 not covered by PROJECT_STATUS`」这一历史陈述无法在不回退 `PROJECT_STATUS.md` 的前提下复现；对应当前状态已修复，不记为伪造。
- 构建失败、单测失败这两条分支未直接注入：两者与治理步使用同一 `$LASTEXITCODE` 机制，治理步已实测通过；作为「未直接验证项」记录。

## 5. 文档与分支

**裁决：不通过（两份状态文档与实际不符）。**

已同步、核对无误的部分：
- `README.md:12` 当前任务 `TASK-009`；`:58` 新增 `verify-all` 命令行；`:73` 检查数 21 项——与实际 21 项一致。
- `PROJECT_STATUS.md` 当前任务 `TASK-009`、ADR 列表 `ADR-001`~`ADR-014`、工具列表含 `check-governance.ps1`（21 项）与 `tools/verify-all.ps1`、最近治理检查 21 项 PASS——均与实际一致。
- 分支：`TASK-009` 为 `PARTIAL` 且留在 `feat/verify-all-gate`，检查器「partial tasks registered」PASS；两个 commit 信息与内容相符。

不符合项（阻塞）：
1. **`project/handoffs/LAST_HANDOFF.md` 未更新。** Snapshot 仍是「当前任务 `TASK-008`（`DONE`…）」「`main` @ `d7998ad`」「`check-governance.ps1` 20/20 PASS」，`Next Safe Action` 仍写着「独立复审 `REVIEW-TASK-008.md`」。而 TASK-009 §5 明确把该文件列为修改项、§11 已勾选「文档/状态已更新」。下个会话按 §8 读交接会被直接误导回 TASK-008。
2. **`project/reports/TEST_REPORT.md` 未记录本次运行。** `Latest Run` 仍是 `main` @ `d7998ad`、治理 20/20，`Runs` 表没有 `verify-all` 行；这与 `PROJECT_STATUS.md:14` 的「21 项检查 PASS」直接矛盾，也不满足 `REVIEW_CHECKLIST.md` §3 第 1 条。
3. 由 1、2 派生：TASK-009 §11「文档/状态已更新」的勾选与事实不符（应改为未完成，或在补完后再勾）。

补充说明：这两处恰好是 21 项检查抓不到的盲区——第 21 项只看 TASK 文件里有没有 `verify-all` 字样，交接与测试报告不在其覆盖范围。ADR-014 把该信任边界交给 Reviewer，本次即按此判定。

## 6. 结论

- 结论：`REJECT`
- 判定说明：门禁工具本身可用、可失败、不掩盖失败，第 21 项也确实有真实效果；但本次变更同时声称完成了交接与测试报告同步，实际两份文档仍停留在 `TASK-008`，属于 `REVIEW_CHECKLIST.md` §3/§4 的明确未通过项，且会被下一次会话直接采信。修复范围仅为文档，不需要改动脚本。
- 证据（命令与实测退出码）：
  - `pwsh -NoProfile -File tools/check-governance.ps1` → `SUMMARY: 21 checks, 21 passed, 0 warnings, 0 failed` / `RESULT: OK` / exit **0**
  - `pwsh -NoProfile -File tools/verify-all.ps1` → `RESULT: VERIFY ALL PASS` / exit **0**（治理 7s、单测 29s `OK (40 tests)`、构建 24s `BUILD SUCCESSFUL`、空白 0s）
  - `git diff --check` → 无输出 / exit **0**
  - 负向验证 1（仅治理违规）：verify-all 治理步 exit **1**，其余 3 步 exit 0，进程 exit **1** —— 早失败不会被后续 PASS 掩盖
  - 负向验证 2（治理 + 行尾空白）：治理 exit 1、空白 exit 2，`RESULT: VERIFY ALL FAILED (check-governance.ps1, git diff --check)`，进程 exit **1** —— 两步失败都被汇总
  - 负向验证 3（第 21 项）：删掉 TASK-009 的 `verify-all` 字样 → `[FAIL] verify-all evidence`，21 项中 1 failed，exit **1**；还原后 21/21 PASS
  - 变异均以临时副本还原，复核后 `git status --short` 为空（仅剩本评审文件）
- 需修复项：
  1. 更新 `project/handoffs/LAST_HANDOFF.md`：Snapshot 的当前任务/阶段/分支改为 `TASK-009`（`PARTIAL`，`feat/verify-all-gate`）、治理 21/21、`verify-all` 入口；`Verification Actually Run` 增加 `verify-all` 与 21/21 行；`Next Safe Action` 改为「复审通过后合入 `main`」。
  2. 在 `project/reports/TEST_REPORT.md` 增加 `TASK-009` 的 `Runs` 行（`verfy-all` 全通过、21 项治理、40 项单测、构建耗时、负向验证），并把 `Latest Run` 更新为本次结果，消除与 `PROJECT_STATUS.md:14` 的 20/21 矛盾。
  3. 上述两项完成后修正 TASK-009 §11「文档/状态已更新」的勾选，或保持未勾选直至补完。

### 非阻塞建议（不计入需修复项）

1. 第 21 项是「字样存在」检查，不是「证据」检查：我保留 `TASK-009.md:17` 的 `tools/verify-all.ps1`（Scope 行），删掉 §10 证据行与其余全部字样后，检查器仍 21/21 PASS。建议锚定 §10 表格行（例如匹配 `^\|\s*`pwsh -NoProfile -File tools/verify-all\.ps1`\s*\|\s*(?!TBD)`）或要求出现带退出码/结论的行；至少把 ADR-014 与 TASK-009 §8 的措辞从「存在证据」改为「出现该命令字样」，避免高估这项检查。
2. `verify-all.ps1:52-55` 的 `git diff --check` 失败时不打印定位信息：步骤体的管道输出被 `$whitespace = Invoke-Step ...` 吞掉（我注入行尾空白时，页面只剩 `exit 2`，具体 `文件:行 trailing whitespace` 不可见），且该步不像前 3 步那样写日志。建议改为 `git diff --check 2>&1 | Tee-Object -FilePath .gradle/logs/verify-whitespace.log`。
3. `verify-all.ps1:26` 的 `$LASTEXITCODE` 可能是上一步残留：若某步体内没有原生命令（例如 `pwsh` 不存在），会复用它之前的值而误判。建议在 `& $Body` 前置 `$LASTEXITCODE = 0`。
4. ADR-014「用户授权来源」建议逐字引用当轮提议（含「同步修改 `AGENTS.md` §5 与 `TASK_TEMPLATE.md` §10」），而非转述脚本部分，使 §11 要求的「依据与来源」可独立核验。按本次会话记录，「你来决定便是」紧接该提议，判定为授权成立、非越权。
5. 构建秒数口径不统一：`README.md:14`「38s」、TASK-009 §10「22s」，我实测 21s/24s/25s。建议文档统一用「约 20–25s」或注明测量时机，避免后续会话把某个秒数当验收阈值。
6. 未直接注入构建失败/单测失败两条分支（机制与已实测的治理步相同）；如需完全闭环，可在临时克隆里破坏一个测试再跑一次 verify-all。
