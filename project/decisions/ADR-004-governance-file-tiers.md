# ADR-004: 建立 AI 开发治理层的文件分级与保护清单

- 状态：`ACCEPTED`
- 日期：`2026-11-04`
- 决策人：用户（以“按 temp.md 方案修改”的明确指令批准）
- 相关任务：`TASK-000`（前置审查）

## Context
仓库已有 `AGENTS.md` 与 `project/` 治理包，但没有明确“AI 能改哪些文件”。若 AI 可以自行修改 `DATABASE.md` 或 `ARCHITECTURE.md`，它就能修改约束自己的规则，长周期开发中会逐步破坏设计基线。需要一份显式的文件分级、受保护清单和改动流程。

## Decision
1. 文件分三级：**产品与架构基线**（AI 只能建议）、**任务状态与进度**（可更新，必须有实际依据）、**源代码与测试**（TASK 范围内可改）。
2. 受保护清单：`AGENTS.md`、`ARCHITECTURE.md`、`DATABASE.md`、`DEVELOPMENT.md`、`project/REQUIREMENTS.md`、`README_GOVERNANCE.md`。
3. 修改受保护文件的流程：提出冲突与理由 → 给最小修订方案 → 用户批准 → 新增 ADR → 改文档 → 同步状态。用户在当前会话明确指示修改时，仍必须留下 1 份 ADR 记录依据。
4. `orgins/**` 为只读视觉参考，不得作为生产代码或验收依据。
5. 规则写入 `AGENTS.md` §11。

## Alternatives Considered
- 仅依赖 `AGENTS.md` §3 的文字约束：无法区分“改代码”和“改规则”，也没有清单化的可审查边界。
- 使用 Git 权限/CODEOWNERS 强制：个人项目成本过高，且无法约束同仓库内的 AI 会话。
- 所有文件都需要 ADR：会把任务记录也变成审批负担，反而降低记录质量。

## Consequences
- 正面：基线文件的任何改动都有 ADR 与批准痕迹，可在 Diff 中审计。
- 成本：每次基线修订多一次 ADR 写入；文档类任务也必须走批准流程。
- 影响：`AGENTS.md` 新增 §10–§13；新增 `README.md` 作为入口与文件地图。
- 验证：任何提交中若出现受保护文件改动，必须能指向对应 ADR 与用户批准记录。
