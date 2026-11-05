# REVIEW-TASK-020: 独立评审记录

- 审查任务：`TASK-020`
- 审查 commit：`4b286aa227c34394b9c99e0f2c1937bb4d624b74`（第 2 轮）
- 审查范围：`git diff main...HEAD`（`a8232a5`..`4b286aa`，2 个提交 `95641c0` + `4b286aa`，10 文件 +260/−26；本轮重点为 `95641c0..4b286aa` 的 9 个文件）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`
- 轮次：第 2 轮。第 1 轮（`95641c0`）`REJECT`：模板未改、`README`/`PROJECT_STATUS` 残留「21 项」、探针脚本会覆盖未提交改动。

评审时工作区：`git status --short` 为空（开始前）。本轮为只读核对 + 门禁复跑 + 运行 15 探针 + 2 个独立攻击；所有改动已还原，结束时工作区仅剩本评审文件。

## 1. Scope

**裁决：通过。**

- 第 1 轮的三项全部落地：`TASK_TEMPLATE.md` 真的改了（§9 未开工不写日期行、§10 机器契约与英文判定词）；`README.md:74` 与 `PROJECT_STATUS.md:24` 改 22；探针脚本加脏树守卫。
- 第 22 项按评审攻击 A 扩到 `app/build.gradle.kts` 与根 `build.gradle.kts`，并新增 P19；`FEATURE_MATRIX` F-015 脚注写明覆盖边界（挡不住库 manifest 合并与手写 socket），主张收窄为「本仓库未声明网络能力」。
- 未夹带功能改动：仍只动治理脚本、探针脚本与文档。

## 2. Diff 核对

**裁决：通过。**

- `git diff --check`：**exit 0**；无冲突标记。
- 第 21 项逻辑未回退：整格等值 + 剥强调/尾括号 + `NOT_STARTED` 三种信号（勾选验收、§9 日期行、真实判定行）。
- 第 22 项：manifest 全文匹配，构建文件用同一组子串（`okhttp|retrofit|ktor|firebase`）；`Test-Path` 缺失即跳过。
- 探针脚本：脏树守卫在所有写操作之前（`git status --porcelain -- $paths` 非空即 `[DIRTY]` + exit 1），`git show HEAD:` 取原件、`finally` 还原、结束时再断言一次干净。

## 3. 架构与数据

**裁决：通过。**

- 无源码、依赖、schema 改动；manifest 与 catalog 内容未变。
- 第 22 项仍是纯文本检查；探针脚本只依赖 git 与 pwsh，且现在拒绝在脏树上运行，不会成为新的数据丢失源。

## 4. 验证证据

**裁决：通过。**

| 命令 | 结果 |
|---|---|
| `pwsh -NoProfile -File tools/replay-governance-probes.ps1` | **PROBES: 15 run, 0 failed**，P5–P19 全部 `[OK]`；基线 `22 checks, 22 passed, 0 warnings`，exit 0；跑完 `git status` 为空 |
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS（exit 0）**：治理 22/22、`OK (71 tests)`、DAO 21 项、BUILD SUCCESSFUL 19s、`no whitespace errors` |
| `pwsh -NoProfile -File tools/check-governance.ps1` | `SUMMARY: 22 checks, 22 passed, 0 warnings, 0 failed` |

自建攻击（均先确认变异生效，再还原）：

| 攻击 | 操作 | 实际 | 判定 |
|---|---|---|---|
| A2（第 1 轮攻击 A 的复测） | `app/build.gradle.kts` 直接写 `com.squareup.okhttp3:okhttp` | **exit 1**：`app/build.gradle.kts references a network library: ...` | 漏检已补上 ✓ |
| G2（脏树守卫复测） | 给 `TASK-019.md` 追加未提交标记后跑探针脚本 | **exit 1**，输出 `[DIRTY] ...`，**标记行仍在**（一个文件都没被写） | 第 1 轮的数据丢失风险已消除 ✓ |

未验证项：第 22 项仍看不到库 manifest 合并后的 `INTERNET`、也看不到手写 socket/URL；这一点已在 F-015 脚注写明，属已声明的边界，不是隐瞒。

## 5. 文档与分支

**裁决：不通过（两处状态/计数不一致）。**

1. 🚫 `PROJECT_STATUS.md:8` 写「当前任务：`TASK-020`（**`NOT_STARTED`**，离线守卫与检查器余留加固）」，而 `README.md:12`、`LAST_HANDOFF.md:8` 与 `TASK-020.md:3` 都是 `PARTIAL`（待独立评审）。三份状态文档对同一字段给出两个值；第 14 项检查只比任务号与阶段，不比状态词，所以没拦住。
2. 🚫 `TASK-020.md:67` §11 仍写「测试实际运行（**14 个探针** + 基线 + 全量门禁）」，而 §6、§10、探针脚本输出都是 15（P5–P19）。
3. 其余一致：模板 §9/§10 已写入机器契约与英文判定词；`FEATURE_MATRIX` F-015 脚注含覆盖边界；自动摘要 10/10 与 §6 的 10 条相符；分支 `chore/offline-guard` 领先 `main`（`a8232a5`）2 个提交、未推送。

## 6. 结论

- 结论：`REJECT`
- 证据：门禁 exit 0（22/22、0 警告、71 项、DAO 21、构建 19s）；15 个探针全部通过；攻击 A2 与 G2 都证明第 1 轮的两处缺陷已修；但 `PROJECT_STATUS` 仍把当前任务写成 `NOT_STARTED`，任务 §11 仍写 14 个探针。
- 需修复项（均为一行）：
  1. `PROJECT_STATUS.md:8` 的 `NOT_STARTED` 改为 `PARTIAL`，与 `README`/`LAST_HANDOFF`/任务文件一致。
  2. `TASK-020.md:67` 的「14 个探针」改「15 个探针（P5–P19）」。
  3. 建议（非阻塞，可并入本任务的后续或单列）：第 14 项「readme matches status」目前只比任务号与阶段，建议同时比对状态词——本轮这个矛盾正是从这条缝里漏过去的。
- 对四个提问的回答：
  1. **「shows work」现在可以接受**：模板 §9 已写明「未开工不要写日期行」并给出原因，触发项也在失败信息里列出；「开工前先跑一次基线 verify-all」会被 `real verdict row` 命中，但那种任务本来也不该长期挂 `NOT_STARTED`。
  2. **英文闭集正确，且这次把约定写进了模板**：中文变体（通过/已完成/成功）一旦开收就收不住，机器列必须封闭；模板 §10 已明确「用英文判定词」，P13 从「暗坑」变成「文档化边界」。
  3. **子串守卫值得有，但不是「绝对离线」的证明**：它现在覆盖 manifest、catalog 与两个 build 文件，能把「加一行依赖/权限」变成红灯；F-015 脚注把「库 manifest 合并」「手写 socket」写成明确边界，并把主张收窄为「本仓库未声明网络能力」——这是诚实且可维护的定位，不是安全剧场。
  4. **放 `tools/` 可以了**：脏树守卫在写之前中止，攻击 G2 证明未提交改动会被保住；不必再复制临时仓库。
