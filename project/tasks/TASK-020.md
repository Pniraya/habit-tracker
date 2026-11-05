# TASK-020: 离线守卫与检查器余留加固

- 状态：`NOT_STARTED`
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
- [ ] 新增检查通过时 `SUMMARY` 显示 22 项
- [ ] 负向探针：manifest 加一行 `INTERNET` → 该检查 FAIL
- [ ] 负向探针：`libs.versions.toml` 加一行 `okhttp` → 该检查 FAIL
- [ ] `tools/replay-governance-probes.ps1` 入库，能一键重放 P5–P15
- [ ] 探针 P12 重放：备注行结果列写整格「未运行（任务未开始）」→ 不改变判定
- [ ] 探针 P16：`NOT_STARTED` 但已勾选验收 → FAIL
- [ ] 还原后 22/22、0 警告

## 7. Tests
- 两个负向探针 + 还原后全量 `tools/verify-all.ps1`

## 8. Risks and Decisions
- 新增检查会让所有「21 项」的文档过期，必须一次性扫干净；漏一处就触发第 15 项之外的文档不一致风险。

## 9. Implementation Log
- 尚未开始

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | 尚未开始（`NOT_STARTED`） | 开工后补真实结果 |

## 11. Completion Review
- [ ] 验收标准满足
- [ ] 测试实际运行
- [ ] 独立 Reviewer `APPROVE`

## 12. Handoff
- 之后回到功能开发：`F-009` 连续天数（矩阵里第一个未实现的产品功能）。

## 13. Independent Review
- Reviewer record：待创建
- 结论：`PENDING`

