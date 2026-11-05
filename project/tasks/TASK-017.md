# TASK-017: 治理检查加固（P5–P9）

- 状态：`PARTIAL`（检查器已加固，探针全部重放，待独立评审）
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
- 第 21 项：`TBD|NOT_RUN` 占位判断要尊重 `NOT_STARTED`——当前代码注释写「未开工的任务不需要证据」，但第 370 行无条件 FAIL，注释与实现矛盾（`TASK-019` 建档时撞到）。
- 在 `TASK-015` §8 补 P5–P8 四条边界，明确「有检查 ≠ 证据不可伪造」。

## 4. Out of Scope
- 引入 Pester 或其它测试框架：探针以「临时改文件 → 跑检查 → 还原」的脚本重放（`.gradle/probes.ps1`，不入库）。
- 用提交信息/代码指纹把「是否改过代码」与任务状态绑定（纯文本检查做不到，见 §8）。
- 新增第 22 项「manifest 不得含 `INTERNET`」检查：属新增检查项，另立 `TASK-020`。

## 5. Expected Files
- 修改：`tools/check-governance.ps1`、`project/tasks/TASK-015.md`（§8 补边界）
- 新增：`project/tasks/TASK-017.md`

## 6. Acceptance Criteria
- [x] P6 重放：结果列写「待补」的证据行必须 FAIL
- [x] P5 重放：证据行下方追加合法备注必须 PASS
- [x] P7 重放：`Latest Run` 缺「单测 N 项」必须 FAIL
- [x] P8 重放：`Known Gaps` 写 `OK (59 tests)` 必须 PASS（不污染计数）
- [x] 新增探针 P9：`NOT_STARTED` 任务写占位证据行必须 PASS
- [x] 正向对照 P11：`Latest Run` 计数写错（70 vs 71）必须 FAIL
- [x] 探针 P10：结果列写 `VERIFY ALL FAILED` 必须 PASS（不误伤真实失败记录）
- [x] 现有仓库 21/21 通过，无新增警告

## 7. Tests
- 重放 P5–P8 四个探针，逐一记录命令与退出码
- `pwsh -NoProfile -File tools/verify-all.ps1`

## 8. Risks and Decisions
- **只认结果列的判定词**，不认「行内出现命令名」：引用命令的备注不是证据。代价是判定词列表要维护（`VERIFY ALL PASS` / `VERIFY ALL FAILED` / `PASS` / `FAILED` / `未运行（任务未开始）`）。
- **`NOT_STARTED` 允许占位证据行**：有意留的洞，见下条边界。
- 探针脚本第一次运行时静默没改到文件（PowerShell 里双反引号被当成字面量），却报告 PASS。已给每个探针加「改动是否真的生效」断言——**探针自己不验证自己，就会变成假证据**。
- 已知边界（已写入 `TASK-015` §8）：纯文本检查**无法**阻止有意造假——任务可以自我声明 `NOT_STARTED` 并写「未运行」同时提交代码，当前没有检查把状态与代码改动绑定。治理检查的定位是**防事故**（忘记更新、数字漂移），不是防欺诈。

## 9. Implementation Log
- 2026-11-05：第 15 项改为只在 `## Runs` 段取计数，缺「单测 N 项」直接 FAIL；第 21 项改为只认结果列判定词，并让 `NOT_STARTED` 容忍占位行。
- 2026-11-05：P5–P11 七个探针全部重放，结果见 §10。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS**（exit 0） | 治理 21/21、单测 71 项、DAO SQL 21 项、构建通过 |
| P5：证据行下方追加合法备注（含命令名与「未运行」字样） | **exit 0**（PASS，符合预期） | 加固前该备注被当成证据行 → 假阳性 |
| P6：证据行结果列改「待补」 | **exit 1**（FAIL，符合预期） | `TASK-019 has no verify-all row whose result column states a verdict`；加固前 PASS |
| P7：`Latest Run` 去掉「单测 71 项」 | **exit 1**（FAIL，符合预期） | `Latest Run does not state 单测 N 项, so the count cannot be checked`；加固前静默跳过 |
| P8：`Known Test Gaps` 段写 `OK (59 tests)` | **exit 0**（PASS，符合预期） | 计数只取 `## Runs` 段；加固前被污染成 FAIL |
| P9：`TASK-019` 改 `NOT_STARTED` + 占位证据行 | **exit 0**（PASS，符合预期） | 注释与实现的矛盾已消除；加固前无条件 FAIL |
| P10：结果列改 `VERIFY ALL FAILED` | **exit 0**（PASS，符合预期） | 真实失败记录不被误判为「无判定词」 |
| P11：`Latest Run` 计数改 70（实际 71） | **exit 1**（FAIL，符合预期） | `Latest Run says 单测 70 项 but the last OK row is 71 tests` |
| 还原后基线 | **exit 0**，21/21、0 警告 | 每次探针后 `git status --short` 只剩 `tools/check-governance.ps1` 一处改动 |

## 11. Completion Review
- [x] 验收标准全部满足（7/7）
- [x] 相关测试实际运行（P5–P11 七个探针 + 基线复跑）
- [x] `git diff --check` 通过
- [x] Diff 已审查：只改 `tools/check-governance.ps1` 两处判定逻辑
- [x] 文档/状态已更新
- [ ] 独立 Reviewer `APPROVE`

## 12. Handoff
- 下一步：评审 `APPROVE` 后合入 `main`；之后 `TASK-020`（manifest `INTERNET` 守卫）。
- 遗留：判定词为硬编码列表，出现新写法需同步维护。

## 13. Independent Review
- Reviewer record：待创建
- 结论：`PENDING`

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-017
- 状态：`PARTIAL`
- 阶段：Gate 3
- 验收：通过 8 项 / 未通过 0 项
- 最近测试记录：2026-11-05 / **OK (71 tests)**
<!-- close-task:end -->
