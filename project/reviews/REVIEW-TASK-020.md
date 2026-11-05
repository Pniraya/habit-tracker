# REVIEW-TASK-020: 独立评审记录

- 审查任务：`TASK-020`
- 审查 commit：`95641c0ac0ed73b79531b5064bff6732c49193b2`
- 审查范围：`git diff main...HEAD`（`a8232a5`..`95641c0`，1 个提交，7 文件 +155/−22：`tools/check-governance.ps1` 第 21 项收紧 + 新增第 22 项、`tools/replay-governance-probes.ps1` 入库、`TASK-020.md`、状态文件）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`

评审时工作区：`git status --short` 为空（开始前）。本轮为只读核对 + 门禁复跑 + 运行已入库的 14 探针 + 3 个自建攻击；所有变异已还原，结束时工作区仅剩本评审文件。

## 1. Scope

**裁决：不通过（Scope 第 4 条未落地）。**

- 第 22 项、整格锚定、`NOT_STARTED` 防规避、探针入库都已实现，与 §3 的 1/2/3/5 条对应。
- 🚫 §3 第 4 条要求「`TASK_TEMPLATE.md` §10 写明结果列用英文判定词与整格锚定」，但本提交**没有改 `TASK_TEMPLATE.md`**（`git diff main...HEAD --name-only` 的 7 个文件里没有它），模板 §10 仍是 `| ... | TBD | TBD |`。§6 第 7 条「已写入模板」因此不成立。

## 2. Diff 核对

**裁决：通过。**

- `git diff --check`：**exit 0**；无冲突标记。
- 第 21 项：判定词改为整格比较，先剥 `*` 与中/英文括号段，再与 `$verdictWords` 做 `-contains`；P12 的劫持路径被切断。
- `NOT_STARTED`：只有状态为 `NOT_STARTED` 时才检查「已勾选验收 / §9 有日期行 / 有真实判定行」，命中后置 `$notStarted = $false` 并记 FAIL。
- 第 22 项：manifest 全文匹配 `android.permission.INTERNET`；`libs.versions.toml` 用 `Select-String` 查四个库名；缺 manifest 也有独立失败信息。
- `tools/replay-governance-probes.ps1` 用 `git show HEAD:<path>` 取原件、`git checkout --` 还原、`finally` 包裹、结束时断言探针路径干净——修掉了 TASK-017 那次「脏状态当基线」的根因。

## 3. 架构与数据

**裁决：通过。**

- 无源码、依赖、schema 改动；`AndroidManifest.xml` 与 `libs.versions.toml` 未变。
- 第 22 项仍是纯文本检查，不引入网络或新依赖；探针脚本只依赖 git 与 pwsh。

## 4. 验证证据

**裁决：通过（14 探针全对），另有 1 个探针运行安全缺陷（§5-3）。**

| 命令 | 结果 |
|---|---|
| `pwsh -NoProfile -File tools/replay-governance-probes.ps1` | **14 run, 0 failed**，每项均 `[OK]`（P5/P6/P7/P8/P10/P11/P12/P13/P14/P15/P9/P16/P17/P18），基线 `22 checks, 22 passed, 0 warnings`，exit 0 |
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS（exit 0）**：治理 22/22、`OK (71 tests)`、DAO 21 项、BUILD SUCCESSFUL 19s、`no whitespace errors` |
| `pwsh -NoProfile -File tools/check-governance.ps1` | `SUMMARY: 22 checks, 22 passed, 0 warnings, 0 failed`，exit 0 |

自建攻击（均先断言变异生效，再还原）：

| 攻击 | 操作 | 实际 | 判定 |
|---|---|---|---|
| A | 在 `app/build.gradle.kts` 直接写 `implementation("com.squareup.okhttp3:okhttp:...")`（**不经 catalog**） | **exit 0**，`[PASS] offline guarantee` | 第 22 项漏检（见 §6-4） |
| C | `TASK-020` 改 `NOT_STARTED` + 取消勾选，保留 §9 的日期行 | **exit 1**：`declares NOT_STARTED but shows work (implementation log, real verdict row)` | 防规避生效；日期行即触发（见 §6-2） |
| G | 给 `TASK-019.md` 追加一行未提交标记，再跑探针脚本 | 运行后**标记消失**（`git checkout --` 覆盖） | 探针脚本会静默丢弃未提交改动（见 §6-3） |

未验证项：真机与 UI 未涉及；第 22 项没有覆盖 manifest 合并器引入的权限（见 §6-4）。

## 5. 文档与分支

**裁决：不通过（两处计数未扫干净）。**

1. 🚫 `README.md:74` 仍写「脚本只读，检查 **21 项**」——这是对当前脚本的陈述，不是历史记录，应为 22。
2. 🚫 `PROJECT_STATUS.md:24` 仍写「`tools/check-governance.ps1`（**21 项**）」——同为当前清单，应为 22；`PROJECT_STATUS.md:14` 的「历史记录里的 21 项不回改」不能覆盖这两处。
3. 🚫 `TASK_TEMPLATE.md` 未改（见 §1），而 `TASK-020` §3.4/§6 第 7 条都说改了。
4. 其余一致：`TASK-020` §11 的 8/8 与 §6 的 8 条相符，自动摘要 8 项；`TEST_REPORT.md:9` 已写「治理 **22 项**」；`LAST_HANDOFF` 与 `README` 的当前任务为 `TASK-020` `PARTIAL`；分支 `chore/offline-guard` 领先 `main`（`a8232a5`）1 个提交、未推送。

## 6. 结论

- 结论：`REJECT`
- 证据：门禁 exit 0（22/22、0 警告、71 项、DAO 21、构建 19s）；14 个探针全部通过且断言了消息片段；第 21 项的整格锚定、`NOT_STARTED` 防规避、第 22 项的正向行为都经复跑确认。但 Scope 第 4 条未做、两处「21 项」未同步，且探针脚本会覆盖用户未提交改动。
- 需修复项：
  1. 更新 `TASK_TEMPLATE.md` §10：写明结果列必须使用英文判定词（`VERIFY ALL PASS`/`VERIFY ALL FAILED`/`PASS`/`FAILED`）且整格锚定、可带一个结尾括号（如 `（exit 0）`），并给出一行合格示例；否则 §3.4 与 §6 第 7 条是空头承诺，P13 的边界也没有落到模板上。
  2. `README.md:74` 与 `PROJECT_STATUS.md:24` 的「21 项」改 22（§3.1 要求一次性扫干净）。
  3. `tools/replay-governance-probes.ps1` 加**干净工作区守卫**：脚本开头检查 `git status --porcelain -- $paths`，非空就打印 `[DIRTY] ... abort` 并 `exit 1`，什么都不改。现在它在第 47 行先 `Restore-All`，等于在 dirty 树上静默 `git checkout --`，会丢掉别人未提交的改动（我实测一个标记行被抹掉）。这条同时防住 TASK-017 那次「脏状态当基线」的复发。
- 对四个提问的回答：
  1. **「shows work」偏严，但方向对**。§9 在模板里是「按日期记录关键变更」，日期行确实等于「我改过东西」；真正的计划应写在 §2/§8。建议保留现规则，只在消息里说明触发项，并在模板 §9 加一句「未开工的任务不写日期行；计划写 §2/§8」。另注意「real verdict row」信号会误伤「开工前先跑一次基线 verify-all」的合法记录——这类行若存在，任务其实也无法自称 `NOT_STARTED`，可接受。
  2. **保持英文闭集是对的**。这一列是给机器判定的枚举，不是给人看的自由文本；中文同义词会立刻引出「通过／已通过／成功」的无穷变体。关键是把它写进模板（本轮缺口）而不是暗中约定。
  3. **子串守卫不是安全剧场，但也不能叫「offline guarantee」的全部**。它精确对应 F-015 引用的两条证据（无 `INTERNET` 权限、无网络库），能把「有人加一行」变成红灯，这已经比人工核对强；但它挡不住直接 Gradle 依赖（攻击 A 已证明 PASS）、也挡不住库 manifest 合并进来的 `INTERNET`。建议：把 `app/build.gradle.kts`、根 `build.gradle.kts` 一并纳入同一组子串；并在检查注释或 `FEATURE_MATRIX` F-015 的脚注里写明「守卫覆盖声明与 catalog，不覆盖合并后的 manifest 与源码内 URL」。
  4. **放在 `tools/` 可以，但必须拒绝在脏树上运行**。`git checkout --` 的还原方式本身没错（它能把探针路径恢复到 HEAD），错在没有先确认这些路径没有别人的未提交改动；一个「测试脚本」不该有静默丢改动的副作用。加 3 行守卫即可，不必复制临时仓库。
