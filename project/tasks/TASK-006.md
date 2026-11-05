# TASK-006: 治理系统状态一致性改造

- 状态：`DONE`
- 阶段：`Gate 2`（治理基础设施，不改变产品范围）
- 优先级：`P0`
- 依赖：无
- 创建日期：`2026-11-05`
- 完成日期：`2026-11-05`

## 1. Objective
消除“文件存在但状态不同步”：建立六份状态文档的一致性规则，把规则固化进 `tools/check-governance.ps1`，并让任务收尾半自动化。

## 2. 实施结果
- 新增 `ADR-012`（`ACCEPTED`）：状态唯一来源、`main` 稳定分支策略、Reviewer 环节、收尾自动化。
- `AGENTS.md` §5/§7/§10/§12 同步：收尾必须跑 `close-task` 与检查器；`main` 稳定分支；`PARTIAL` 需登记例外；Reviewer 按清单出结论。
- 检查器从 13 项扩到 **19 项**：README 与 PROJECT_STATUS 一致、ADR 状态一致（历史标注豁免）、发布报告不与工程状态矛盾、TEST_REPORT 摘要与 Runs 末行一致、`main` 上 `PARTIAL` 已登记、Reviewer 清单完整。
- 新增 `tools/close-task.ps1`：生成受标记保护的「状态摘要」块，`-Apply` 同步 README 与 PROJECT_STATUS 的阶段/当前任务行。
- 新增 `project/REVIEW_CHECKLIST.md`：六个维度、可判定勾选项、`APPROVE`/`REJECT` 结论。
- 修复现存漂移：README（Gate 1→Gate 2、当前任务、构建与测试结果、“没有 Android 工程”）、PROJECT_STATUS（ADR-011 状态、13→19 项、在册例外）、TEST_REPORT（Latest Run 停在 2026-11-04）、RELEASE_REPORT（“尚无 Android 工程”、`assembleDebug` 仍 NOT_RUN）、TASK-001/TASK-004 的 ADR 状态措辞。

## 3. Acceptance Criteria
- [x] 规则写入 `AGENTS.md` 并有 ADR-012 支撑
- [x] 检查器新增 6 项且全部通过（19/19）
- [x] 收尾脚本可生成摘要并同步状态行
- [x] Reviewer 清单覆盖 Diff / 架构 / 证据 / 文档同步 / 分支 / 结论
- [x] 四份过期文档已同步
- [x] 检查器能抓住真实漂移（首轮抓出 TASK-001/TASK-004 的 ADR 状态矛盾）

## 4. Verification Evidence
| 命令 | 结果 | 摘要 |
|---|---|---|
| `pwsh -File tools/check-governance.ps1` | **19/19 PASS** | 扩展后首轮 1 FAIL（ADR 状态漂移），修复后全通过 |
| `pwsh -File tools/close-task.ps1 -Task TASK-005 -Apply` | 摘要已写入并同步 | 验证脚本可用 |

## 5. 已知限制
- ADR 一致性检查基于“ADR-xxx 后 30 字符内出现状态词”的启发式；写法奇特的引用可能漏检，但历史标注（当时/历史记录）可豁免，不会误报。
- `close-task` 只自动同步阶段与当前任务两行，其余状态仍需人工确认——刻意如此，避免脚本编造未验证结论。

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-006
- 状态：`DONE`
- 阶段：Gate 2
- 验收：通过 6 项 / 未通过 0 项
- 最近测试记录：2026-11-05 / exit 0，19 PASS
<!-- close-task:end -->
