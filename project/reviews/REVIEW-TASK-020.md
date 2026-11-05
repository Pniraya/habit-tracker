# REVIEW-TASK-020: 独立评审记录

- 审查任务：`TASK-020`
- 审查 commit：`4f4f5f589cc80d86d21d1e40e0ce7c9d712037ee`（第 3 轮）
- 审查范围：`git diff main...HEAD`（`a8232a5`..`4f4f5f5`，3 个提交 `95641c0` + `4b286aa` + `4f4f5f5`，10 文件 +261/−26；本轮重点为 `4b286aa..4f4f5f5` 的 3 个文件）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`
- 轮次：第 3 轮。第 1 轮（`95641c0`）`REJECT`：模板未改、两处「21 项」残留、探针脚本覆盖未提交改动；第 2 轮（`4b286aa`）`REJECT`：`PROJECT_STATUS` 当前任务写 `NOT_STARTED`、§11 写 14 个探针。

评审时工作区：`git status --short` 为空（开始前）。本轮为只读核对 + 门禁与探针全量复跑；结束时工作区仅剩本评审文件。

## 1. Scope

**裁决：通过。**

- 本轮只改两处：`PROJECT_STATUS.md:8` 的状态词、`TASK-020.md` §11 的探针计数，外加 §12 记录第 14 项检查的后续项；没有夹带新的规则或代码。
- 与第 2 轮的两条阻断项一一对应，改动范围与任务 §3 一致。

## 2. Diff 核对

**裁决：通过。**

- `git diff --check`：**exit 0**；无冲突标记。
- `PROJECT_STATUS.md:8`：`TASK-020`（`NOT_STARTED`）→（`PARTIAL`，…待独立评审），与 `README.md:12`、`LAST_HANDOFF.md:8`、`TASK-020.md:3` 一致（四份文档已复核）。
- `TASK-020.md:67`：「14 个探针」→「15 个探针」，与 §6、§10、脚本输出一致。
- `TASK-020.md` §12 新增一条遗留：第 14 项只比任务号与阶段、不比状态词——本轮矛盾的来源被记录下来，没有假装已经解决。

## 3. 架构与数据

**裁决：通过。**

- 无源码、依赖、schema 改动；`check-governance.ps1` 与探针脚本本轮未动，第 21/22 项逻辑与第 2 轮评审时相同。
- 探针脚本的脏树守卫、`git show HEAD:` 取原件、`finally` 还原均保持不变。

## 4. 验证证据

**裁决：通过。**

| 命令 | 结果 |
|---|---|
| `pwsh -NoProfile -File tools/replay-governance-probes.ps1` | **PROBES: 15 run, 0 failed**（P5–P19 全 `[OK]`），基线 `22 checks, 22 passed, 0 warnings`，exit 0；跑完 `git status` 为空 |
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS（exit 0）**：治理 22/22、`OK (71 tests)`、DAO 21 项、BUILD SUCCESSFUL 19s、`no whitespace errors` |
| `pwsh -NoProfile -File tools/check-governance.ps1` | `SUMMARY: 22 checks, 22 passed, 0 warnings, 0 failed`，exit 0 |

第 2 轮的攻击 A2（直接 Gradle 依赖 → FAIL）与 G2（脏树 → `[DIRTY]` 且未提交标记保留）在 `4b286aa` 上已实测通过；本轮未改检查器与脚本，结论沿用。

未验证项：第 22 项仍看不到库 manifest 合并进来的 `INTERNET` 与手写 socket/URL——已在 `FEATURE_MATRIX` F-015 脚注声明为边界；本轮不重跑真机（与治理改动无关）。

## 5. 文档与分支

**裁决：通过。**

- 四份状态文档对 `TASK-020` 的状态一致为 `PARTIAL`；`TASK-020.md` §11 的 10/10 与 §6 的 10 条、自动摘要「通过 10 项」一致。
- `TASK-020.md:67` 的探针计数已改为 15，与 §10 的「PROBES: 15 run, 0 failed」和脚本输出一致。
- §12 记录的遗留（第 14 项不比对状态词）是对本轮问题的诚实归档；建议另立后续任务时把它做成独立检查项并配负向探针，不要混进文档提交。
- 分支 `chore/offline-guard` 领先 `main`（`a8232a5`）3 个提交、未推送；`git status --short` 在评审开始时为空。

## 6. 结论

- 结论：`APPROVE`
- 证据：`verify-all` exit 0（治理 22/22、0 警告、71 项、DAO 21 项、构建 19s、空白 0）；15 个探针全过且基线 22/22；第 2 轮的两条阻断（状态词、探针计数）都已按最小改动修好，四份文档重新一致。
- 无阻断项。
- 非阻塞建议：
  1. 第 14 项增加「状态词比对」时，请连同负向探针一起做（例如把 `PROJECT_STATUS` 的 `PARTIAL` 改成 `NOT_STARTED`，期望 FAIL），否则又是一条没有证据的规则；这正是 §12 记录的那条遗留。
  2. `/tools/replay-governance-probes.ps1` 目前是唯一让治理规则可复现的手段，建议在 `DEVELOPMENT.md` 或 `README` 的命令表里提一句它的存在与「会改动探针路径、脏树会拒绝运行」，避免下一个人看到 `git checkout` 时误解。
