# TASK-020: 离线守卫与检查器余留加固

- 状态：`PARTIAL`（三项加固已实现，探针 14 项全对，待独立评审）
- 阶段：`Gate 2`
- 优先级：`P3`
- 依赖：`TASK-017`（检查器加固）、`TASK-019`（F-015 状态依据）
- 创建日期：`2026-11-05`
- 完成日期：`TBD`

## 1. Objective
`FEATURE_MATRIX.md` 的 F-015「离线核心功能」标为 `VERIFIED`，依据是 manifest 没有 `INTERNET` 权限、依赖里没有网络库。这是人工核对的事实，**没有自动守卫**：任何人加一行权限或一个库，矩阵不会变红。

## 2. Context
`TASK-019` 评审认可「无 `INTERNET` = 结构性保证」，同时指出缺少守卫。`TASK-017` 收尾时把这项从它的范围里分出来，避免一个「检查器加固」提交混进两类改动。

## 3. Scope
1. 新增第 22 项守卫：`app/src/main/AndroidManifest.xml` 不得包含 `android.permission.INTERNET`；`gradle/libs.versions.toml` 不得出现 `okhttp`/`retrofit`/`ktor`/`firebase`；同步所有「治理 21 项」表述为 22 项。
2. 把探针脚本从 `.gradle/probes.ps1`（不入库）移到 `tools/replay-governance-probes.ps1` 并入库，使 P5–P15 可复现（`TASK-017` 评审第 1 条）。
3. 判定词锚定整格：结果列必须**整格**等于判定词或「未运行（任务未开始）」，避免备注引用被当成判定行（评审 P12）。
4. `TASK_TEMPLATE.md` §10 写明「结果列用英文判定词」与整格锚定要求（评审 P13：中文「通过」会被判无判定词，属可用性边界）。
5. 便宜的 `NOT_STARTED` 防规避：状态为 `NOT_STARTED` 但已勾选验收项、或 §9 有实现日志、或 §10 有真实结果行 → FAIL（`TASK-017` 评审第 3 条：改状态可同时绕过第 18/19 项）。

## 4. Out of Scope
- 不改应用代码；不新增网络能力。

## 5. Expected Files
- 修改：`tools/check-governance.ps1`；`README.md`、`project/PROJECT_STATUS.md`、`project/reports/TEST_REPORT.md`、`project/handoffs/LAST_HANDOFF.md`（计数 21 → 22）
- 新增：`project/tasks/TASK-020.md`

## 6. Acceptance Criteria
- [x] 新增检查通过时 `SUMMARY` 显示 22 项
- [x] 负向探针 P17：manifest 声明 `INTERNET` → FAIL
- [x] 负向探针 P18：`libs.versions.toml` 加 `okhttp` → FAIL
- [x] 负向探针 P19：`app/build.gradle.kts` 直接声明 okhttp（绕过 catalog）→ FAIL（评审攻击 A）
- [x] 探针脚本在脏树上拒绝运行（`[DIRTY]` + exit 1；评审攻击 G）
- [x] `tools/replay-governance-probes.ps1` 入库，一键重放 15 个探针（P5–P19）
- [x] 探针 P12：备注行结果列写整格「未运行（任务未开始）」→ 不改变判定（exit 0）
- [x] 探针 P16：`NOT_STARTED` 但已勾选验收 → FAIL
- [x] 探针 P13：结果列写中文「通过」→ FAIL（整格锚定 + 词表边界，已写入模板）
- [x] 还原后 22/22、0 警告

## 7. Tests
- 两个负向探针 + 还原后全量 `tools/verify-all.ps1`

## 8. Risks and Decisions
- 新增检查会让所有「21 项」的文档过期，必须一次性扫干净；漏一处就触发第 15 项之外的文档不一致风险。

## 9. Implementation Log
- 2026-11-05：新增第 22 项「offline guarantee」；第 21 项判定词改为**整格等值**（剥掉强调与括号后比较），并加 `NOT_STARTED` 防规避检测（已勾选验收 / §9 有日志 / §10 有真实判定 → 视为已开工）。
- 2026-11-05（评审第 1 轮 `REJECT` 后）：补 `TASK_TEMPLATE.md` §9/§10 的机器约束；`README`/`PROJECT_STATUS` 两处「21 项」改 22；第 22 项扫描范围扩到 `app/build.gradle.kts` 与根 `build.gradle.kts`（攻击 A）；探针脚本加脏树守卫（攻击 G）。
- 2026-11-05：探针脚本从 `.gradle/` 移到 `tools/replay-governance-probes.ps1`，原件用 `git show HEAD:` 取、还原用 `git checkout --`——此前从工作区读原件，脏状态会被当成基线（`TASK-017` 已因此产出过一次假 PASS）。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `tools/replay-governance-probes.ps1` | **PROBES: 15 run, 0 failed**；基线 `22 checks, 22 passed, 0 warnings` | P5–P19 逐个断言退出码与信息片段 |
| 探针 P17（manifest 加 `INTERNET`） | **exit 1** | `AndroidManifest declares the INTERNET permission` |
| 探针 P18（catalog 加 `okhttp`） | **exit 1** | `libs.versions.toml references a network library` |
| 探针 P19（`app/build.gradle.kts` 直接写 okhttp） | **exit 1** | `app/build.gradle.kts references a network library` |
| 脏树守卫（给 `TASK-019.md` 加未提交标记后跑探针） | **exit 1**，输出 `[DIRTY] ... M project/tasks/TASK-019.md` | 在改动任何文件之前中止 |
| 探针 P16（`NOT_STARTED` 但验收已勾选） | **exit 1** | `declares NOT_STARTED but shows work (ticked acceptance)` |
| 探针 P12（备注列写整格占位符） | **exit 0** | 整格锚定后不再被备注劫持 |
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS**（exit 0） | 治理 22/22、0 警告、单测 71 项、DAO 21 项、构建通过 |

## 11. Completion Review
- [x] 验收标准满足（10/10）
- [x] 测试实际运行（15 个探针 + 基线 + 全量门禁）
- [ ] 独立 Reviewer `APPROVE`

## 12. Handoff
- 之后回到功能开发：`F-009` 连续天数（矩阵里第一个未实现的产品功能）。
- 遗留（评审建议）：第 14 项只比对「当前任务」的任务号与阶段、不比对状态词——本轮 `PROJECT_STATUS` 写 `NOT_STARTED` 而其余三处写 `PARTIAL` 没被拦住，两轮才由人工发现。后续可让第 14 项比对状态词。

## 13. Independent Review
- Reviewer record：待创建
- 结论：`PENDING`

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-020
- 状态：`PARTIAL`
- 阶段：Gate 2
- 验收：通过 10 项 / 未通过 0 项
- 最近测试记录：2026-11-05 / **OK (71 tests)**
<!-- close-task:end -->
