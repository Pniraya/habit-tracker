# TASK-009: 单一验证入口 verify-all

- 状态：`PARTIAL`（实现与验证完成，等待独立评审）
- 阶段：`Gate 2`
- 优先级：`P1`
- 依赖：无
- 创建日期：`2026-11-05`
- 完成日期：`TBD`

## 1. Objective
把"治理检查 + 单元测试 + 构建 + 空白检查"收敛为一条命令，使交付门禁不再依赖执行者记忆；用检查器强制新任务留下该命令的证据。

## 2. Context
`ADR-014`；`AGENTS.md` §5；`check-governance.ps1` 当前只做静态校验，不执行测试。

## 3. Scope
- 新增 `tools/verify-all.ps1`。
- 新增 `ADR-014`（受保护文件 `AGENTS.md` 的修改依据）。
- 修改 `AGENTS.md` §5 的建议命令段、`TASK_TEMPLATE.md` §10、检查器新增第 21 项（`TASK-009` 起生效）。
- 同步 `README.md`、`PROJECT_STATUS.md`、`LAST_HANDOFF.md` 中的命令与计数。

## 4. Out of Scope
- CI、git hook（见 `ADR-014` 的否决理由）。
- 任何产品代码、依赖、schema 改动。

## 5. Expected Files
- 新增：`tools/verify-all.ps1`、`project/decisions/ADR-014-single-verification-entry.md`、`project/tasks/TASK-009.md`
- 修改：`AGENTS.md`、`project/tasks/TASK_TEMPLATE.md`、`tools/check-governance.ps1`、`README.md`、`project/PROJECT_STATUS.md`、`project/handoffs/LAST_HANDOFF.md`

## 6. Acceptance Criteria
- [x] `tools/verify-all.ps1` 依次跑治理检查、单测、构建、空白检查，任一步失败时退出码非零
- [x] 记录每步耗时与未提交文件数
- [x] 检查器新增第 21 项，且 `TASK-009` 缺少 verify-all 证据时确实 FAIL（负向验证）
- [x] `AGENTS.md` §5 与 `TASK_TEMPLATE.md` §10 指向该脚本
- [x] 在 `main` 上实际运行一次 `verify-all` 并通过

## 7. Tests
- `pwsh -NoProfile -File tools/verify-all.ps1`
- 受控负向验证：临时删掉 `TASK-009` 中的 `verify-all` 字样后运行检查器，应 FAIL
- `git diff --check`

## 8. Risks and Decisions
- 单次约 90 秒；失败时按 `.gradle/logs/verify-*.log` 定位。
- 检查器第 21 项验证的是"存在证据"，不验证证据的新鲜度——与第 18 项同等信任边界，由 Reviewer 在"验证证据"维度复核。

## 9. Implementation Log
- 2026-11-05：新增 `verify-all.ps1`；检查器加第 21 项；`AGENTS.md` §5 改为单入口要求（ADR-014）。
- 2026-11-05：首次运行 gate 时治理检查 FAIL（`ADR-014 not covered by PROJECT_STATUS`）——新增 ADR 未在状态文档登记；补登记后 21/21 通过。
- 2026-11-05：负向验证第 21 项：把 `TASK-009` 中的 `verify-all` 字样替换后检查器 FAIL（`TASK-009 has no verify-all evidence`），还原后 PASS。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS**（首次 FAIL） | 首次运行在治理检查步 FAIL：`ADR-014 not covered by PROJECT_STATUS`（新 ADR 未登记），已补登记后重跑全通过；单测 40 项、`assembleDebug` 22s、`git diff --check` 干净 |

## 11. Completion Review
- [x] 验收标准全部满足（5/5）
- [x] 相关测试实际运行（`verify-all`：21 项检查、40 项单测、构建、空白检查）
- [x] `git diff --check` 通过
- [x] Diff 已审查：无产品代码、无依赖、无 schema 改动
- [x] 文档/状态已更新
- [x] 未完成项已明确记录（CI、hook 按 ADR-014 否决）
- [ ] 独立 Reviewer `APPROVE`

## 12. Handoff
- 下一步：独立复审后合入 `main`；随后开新任务做 Room/DAO 集成测试（任务文件尚未创建）。

## 13. Independent Review
- Reviewer record：`project/reviews/REVIEW-TASK-009.md`
- 结论：`PENDING`

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-009
- 状态：`PARTIAL`
- 阶段：Gate 2
- 验收：通过 5 项 / 未通过 0 项
- 最近测试记录：2026-11-05 / **OK (40 tests)**
<!-- close-task:end -->
