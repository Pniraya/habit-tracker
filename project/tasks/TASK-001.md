# TASK-001: 关闭 Gate 0 —— 固化基线决策并做最小修订

- 状态：`NOT_STARTED`
- 阶段：`Gate 0`
- 优先级：`P0`
- 依赖：`TASK-000`（已完成）
- 阻塞：等待用户对 §5 的 8 项决策给出答复
- 创建日期：`2026-11-04`
- 完成日期：`TBD`

## 1. Objective
把 `project/reports/BASELINE_REVIEW.md` 的 A 类问题与本次审查新增的冲突，用 ADR + 文档最小修订一次关闭，使数据库 schema 与统计口径在进入 Gate 2 之前无未决语义。

## 2. Context
- `project/reports/BASELINE_REVIEW.md`（A1–A4、B1–B5）
- `TASK-000` 审查新增冲突：分层职责、归档/恢复矛盾、SQL 未表达文字约束、`created_at` 时区、备份时间戳冲突
- `AGENTS.md` §3 冲突处理、§11 文件分级、ADR-004

## 3. Scope
- 新增 ADR-005~009。
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
- [ ] 8 项决策各在文档中有可执行、无歧义的一句话规则
- [ ] 每条被修订的受保护文件改动都能指向对应 ADR 编号
- [ ] 修订后不存在新的自相矛盾（同一规则只出现一种口径）
- [ ] `git diff` 仅包含预期文件，无无关改动
- [ ] F-016 状态与产品范围一致，且不出现无证据的 `VERIFIED`

## 7. Tests
- 文档类任务：`git diff --check`；对修订后的关键词（`archived_at`、`archived_on`、`status`、`epoch`、`平均用时`）逐条 grep 复核一致性。
- 不涉及单元/集成测试；不产生构建结论。

## 8. Risks and Decisions
- 决策 1 若选择“加列”，必须在 Gate 2 建库前完成，否则要写 Migration。
- 决策 2 的“自检/修复”策略若不明确，损坏数据无恢复路径。
- 未经批准不得先改文档后补 ADR。

## 9. Implementation Log
（待执行）

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| TBD | NOT_RUN | 尚未执行 |

## 11. Completion Review
- [ ] 验收标准全部满足
- [ ] `git diff --check` 通过
- [ ] 文档/状态已更新
- [ ] 未完成项已记录

## 12. Handoff
- 当前剩余工作：等待 8 项决策答复
- 下一条安全操作：用户确认后写 ADR-005~009，再改文档
- Git 分支/提交：`main`
