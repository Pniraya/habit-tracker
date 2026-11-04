# TASK-001: 关闭 Gate 0 —— 固化基线决策并做最小修订

- 状态：`DONE`
- 阶段：`Gate 0`
- 优先级：`P0`
- 依赖：`TASK-000`（已完成）
- 创建日期：`2026-11-04`
- 完成日期：`2026-11-05`
- 批准：用户于 `2026-11-05` 明确批准 `ADR-005~010` 全部内容

## 1. Objective
把 `project/reports/BASELINE_REVIEW.md` 的 A 类问题与本次审查新增的冲突，用 ADR + 文档最小修订一次关闭，使数据库 schema 与统计口径在进入 Gate 2 之前无未决语义。

## 2. Context
- `project/reports/BASELINE_REVIEW.md`（A1–A4、B1–B5）
- `TASK-000` 审查新增冲突：分层职责、归档/恢复矛盾、SQL 未表达文字约束、`created_at` 时区、备份时间戳冲突
- `AGENTS.md` §3 冲突处理、§11 文件分级、ADR-004

## 3. Scope
- 新增 ADR-005~010。
- 修订 `ARCHITECTURE.md`、`DATABASE.md`、`project/REQUIREMENTS.md` 中**与决策直接相关的最少行**。
- 同步 `project/PROJECT_STATUS.md`、`project/FEATURE_MATRIX.md`（仅 F-016）。

## 4. Out of Scope
任何 Kotlin/Gradle 源码、Room Entity 实现、Migration 实现、依赖引入、环境安装、`orgins/` 修改。

## 5. 待裁决决策（推荐默认）

| # | 决策 | 推荐默认 | 落点 |
|---|---|---|---|
| 1 | 归档/创建日期的时区语义 | `habits` 增加 `created_on`、`archived_on`（TEXT `yyyy-MM-dd`），与 `*_at` 并存；无数据库，零迁移成本 | ADR-005 + DATABASE §4.1 |
| 2 | `HabitLog.status` 策略 | 保留持久化 + Domain 唯一写入口 + DAO 不暴露任意字段更新 + 1 条不变量测试 | ADR-006 + DATABASE §4.3 |
| 3 | 计划版本校验位置 | Repository/UseCase 事务内校验；SQL 仅补可表达的 `frequency_type`↔`weekday_mask` CHECK | ADR-007 + DATABASE §4.2 |
| 4 | 归档与提醒 `enabled` 冲突 | 归档不写 `enabled=0`，仅取消 PendingIntent 并由归档态抑制触发；恢复重排 `enabled=1` | ADR-008 + DATABASE §9 |
| 5 | 备份时间戳格式 | 统一 epoch milliseconds，修正 `ARCHITECTURE.md` §9 示例 | ADR-009 + ARCHITECTURE §9 |
| 6 | 平均用时 | V1 移除该指标；`duration_seconds` 保留但 V1 不使用 | REQUIREMENTS §4.3/§10 + FEATURE_MATRIX F-016 |
| 7 | 今日完成数分母 | 仅统计今日应执行的习惯 | REQUIREMENTS §4.1 + DATABASE §8.2 注 |
| 8 | 时区变更后的历史日志 | 保留 `log_date` 文本，不按新时区重算 | DATABASE §2 注 |

## 6. Acceptance Criteria
- [x] 8 项决策各在文档中有可执行、无歧义的一句话规则
- [x] 每条被修订的受保护文件改动都能指向对应 ADR 编号（ADR-005~010）
- [x] 修订后不存在新的自相矛盾（同一规则只出现一种口径）
- [x] `git diff` 仅包含预期文件，无无关改动
- [x] F-016 状态与产品范围一致（`OUT_OF_SCOPE`），且未出现无证据的 `VERIFIED`

## 7. Tests
- 文档类任务：`git diff --check`；对修订后的关键词（`archived_at`、`archived_on`、`status`、`epoch`、`平均用时`）逐条 grep 复核一致性。
- 不涉及单元/集成测试；不产生构建结论。

## 8. Risks and Decisions
- 决策 1 若选择“加列”，必须在 Gate 2 建库前完成，否则要写 Migration。
- 决策 2 的“自检/修复”策略若不明确，损坏数据无恢复路径。
- 未经批准不得先改文档后补 ADR。

## 9. Implementation Log
- 2026-11-05：把 §5 的 8 项决策写成 `ADR-005~010`（状态 `PROPOSED`），每份含决策、替代方案、影响文件、必需测试与批准后要执行的具体改动。基线文档本次未修改，等批准后再动。
- 2026-11-05：用户批准全部 6 份 ADR，状态改为 `ACCEPTED`；按各 ADR 的 Implementation 段完成 `DATABASE.md`、`ARCHITECTURE.md`、`REQUIREMENTS.md` 的修订，并把 `FEATURE_MATRIX` F-016 改为 `OUT_OF_SCOPE`。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/check-governance.ps1` | exit 0 | 13 项全 PASS，含“改基线须带 ADR”（本次改动与 ADR 同 commit） |
| `Select-String DATABASE.md -Pattern 'exportedAt'` | 1793750400000 | 备份示例已改为 epoch milliseconds（ADR-009） |
| `Select-String DATABASE.md -Pattern '禁用提醒'` | 0 命中 | 归档不再改写 `enabled`（ADR-008） |
| `Select-String DATABASE.md -Pattern 'weekday_mask BETWEEN'` | `BETWEEN 1 AND 127` | 掩码下界修正，配合 frequency CHECK（ADR-007） |
| `Select-String REQUIREMENTS.md -Pattern '平均用时'` | 仅剩“V1 不提供”说明 | 平均用时已从范围移除（ADR-010） |
| `git diff --check` / `git status --short` | exit 0 / 空 | 无空白错误，无遗留改动 |
| 构建/单元测试 | NOT_RUN | 仍无 Android 工程与 SDK，不产生构建结论 |

## 11. Completion Review
- [x] 验收标准全部满足
- [x] `git diff --check` 通过
- [x] 文档/状态已更新
- [x] 未完成项已记录（构建与单元测试未执行）

## 12. Handoff
- 当前剩余工作：无（Gate 0 关闭）；下一步是 `TASK-002` 环境准备
- 下一条安全操作：安装 Android SDK 并把 `JAVA_HOME` 指向 `D:\javajdk17`，然后进 `TASK-003`
- Git 分支/提交：`main`，本次改动随批准提交入库
