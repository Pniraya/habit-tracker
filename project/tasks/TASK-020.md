# TASK-020: 离线守卫检查（manifest 不得含 INTERNET）

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
- `tools/check-governance.ps1` 新增第 22 项：`app/src/main/AndroidManifest.xml` 不得包含 `android.permission.INTERNET`；`gradle/libs.versions.toml` 不得出现 `okhttp`/`retrofit`/`ktor`/`firebase`。
- 同步所有写「治理 21 项」的文档为 22 项。

## 4. Out of Scope
- 不改应用代码；不新增网络能力。

## 5. Expected Files
- 修改：`tools/check-governance.ps1`；`README.md`、`project/PROJECT_STATUS.md`、`project/reports/TEST_REPORT.md`、`project/handoffs/LAST_HANDOFF.md`（计数 21 → 22）
- 新增：`project/tasks/TASK-020.md`

## 6. Acceptance Criteria
- [ ] 新增检查通过时 `SUMMARY` 显示 22 项
- [ ] 负向探针：manifest 加一行 `INTERNET` → 该检查 FAIL
- [ ] 负向探针：`libs.versions.toml` 加一行 `okhttp` → 该检查 FAIL
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
- 之后回到功能开发：F-009 连续天数。

## 13. Independent Review
- Reviewer record：待创建
- 结论：`PENDING`

