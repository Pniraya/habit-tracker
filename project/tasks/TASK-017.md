# TASK-017: 治理检查加固（P5–P8）

- 状态：`NOT_STARTED`
- 阶段：`Gate 3`
- 优先级：`P2`
- 依赖：`TASK-015`（被本任务加固的检查）
- 创建日期：`2026-11-05`
- 完成日期：`TBD`

## 1. Objective
堵上 `TASK-015` 评审用探针找出的四个边界：两个假阴性（可以不跑却通过）与两个假阳性（合法写法被误判）。

## 2. Context
`TASK-015` 第 1 轮评审构造 8 个探针，P1–P4 有效，P5–P8 暴露缺陷：

| 探针 | 操作 | 现状 | 后果 |
|---|---|---|---|
| P5 | 合法备注排在证据行**下方** | FAIL | 假阳性：正常补充说明被拦住 |
| P6 | 证据行含 `verify-all.ps1`、结果列写「待补」 | PASS | **假阴性：不运行也能过** |
| P7 | `Latest Run` 不写「单测 N 项」 | PASS | 假阴性：计数比对被静默跳过 |
| P8 | `Known Gaps` 里写一句 `OK (59 tests)` | FAIL | 假阳性：计数扫全报告被污染 |

评审给出的正确锚点：**判定词必须在结果列**，而不是「行内出现命令名」——后者分不清引用与结果。

## 3. Scope
- 第 21 项：只认「第 2 列（结果列）匹配判定词」的行（`VERIFY ALL PASS` / `FAILED` / `未运行（任务未开始）`），不再用「行内含命令名」作为证据判据。
- 第 15 项：仅在 `## Runs` 段内取最后一个 `OK (N tests)`（消除 P8 的全报告扫描）。
- 第 15 项：`Latest Run` 缺「单测 N 项」时直接 FAIL（模板本来就要求）。
- 在 `TASK-015` §8 补 P5–P8 四条边界，明确「有检查 ≠ 证据不可伪造」。

## 4. Out of Scope
- 引入 Pester 或其它测试框架。
- 用提交信息/代码指纹把「是否改过代码」与任务状态绑定（纯文本检查做不到，见 §8）。

## 5. Expected Files
- 修改：`tools/check-governance.ps1`、`project/tasks/TASK-015.md`（§8 补边界）
- 新增：`project/tasks/TASK-017.md`

## 6. Acceptance Criteria
- [ ] P6 重放：结果列写「待补」的证据行必须 FAIL
- [ ] P5 重放：证据行下方追加合法备注必须 PASS
- [ ] P7 重放：`Latest Run` 缺「单测 N 项」必须 FAIL
- [ ] P8 重放：`Known Gaps` 写 `OK (59 tests)` 必须 PASS（不污染计数）
- [ ] 现有仓库 21/21 通过，无新增警告

## 7. Tests
- 重放 P5–P8 四个探针，逐一记录命令与退出码
- `pwsh -NoProfile -File tools/verify-all.ps1`

## 8. Risks and Decisions
- 尚未实施。
- 已知边界（将写入 `TASK-015` §8）：纯文本检查**无法**阻止有意造假——任务可以自我声明 `NOT_STARTED` 并写「未运行」同时提交代码，当前没有检查把状态与代码改动绑定。治理检查的定位是**防事故**（忘记更新、数字漂移），不是防欺诈。

## 9. Implementation Log
- 尚未开始

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | 未运行（任务未开始，无代码改动） | — |

## 11. Completion Review
- [ ] 验收标准全部满足
- [ ] 相关测试实际运行
- [ ] `git diff --check` 通过
- [ ] Diff 已审查
- [ ] 文档/状态已更新
- [ ] 独立 Reviewer `APPROVE`

## 12. Handoff
- 尚未开始。

## 13. Independent Review
- 尚未提交评审。

