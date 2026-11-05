# TASK-010: DAO 层的 SQL 运行时验证

- 状态：`PARTIAL`（harness 完成并自证可证伪，等待独立评审）
- 阶段：`Gate 2`
- 优先级：`P0`
- 依赖：`TASK-004`（schema v1）、`TASK-005`（Repository/DAO）
- 创建日期：`2026-11-05`
- 完成日期：`TBD`

## 1. Objective
在没有 Robolectric、没有设备的前提下，用真实 SQLite 执行 DAO 声明的 SQL，验证查询语义、CAS 更新与级联删除确实成立；把"DAO 只是写出来了"推进到"DAO 的语句行为被验证过"。

## 2. Context
`DATABASE.md` §4.2（计划版本追加式写入）、§4.3（日志整条写入）；`ARCHITECTURE.md` §4；`ADR-006`、`ADR-011`；`TEST_REPORT.md` 的 Known Test Gaps。

## 3. Scope
- 新增 `tools/verify-dao.mjs`：从 DAO 源码解析 `@Query`，在 Room 导出的 DDL + `DatabaseConstraints` 触发器上执行并断言。
- 接入 `tools/verify-all.ps1` 作为独立一步。

## 4. Out of Scope
- Room 的 Cursor 映射、TypeConverter 往返、Flow 失效通知（需 Robolectric 或设备）。
- 迁移 v1→v2、`MigrationTestHelper`。
- 任何产品代码或 schema 改动。

## 5. Expected Files
- 新增：`tools/verify-dao.mjs`、`project/tasks/TASK-010.md`、`project/reviews/REVIEW-TASK-010.md`
- 修改：`tools/verify-all.ps1`、`project/reports/TEST_REPORT.md`、`project/PROJECT_STATUS.md`、`project/handoffs/LAST_HANDOFF.md`

## 6. Acceptance Criteria
- [x] harness 从 DAO 源码解析 `@Query`，而不是另抄一份 SQL
- [x] 覆盖 `@Upsert` 冲突语义、`archive`/`closeVersion` 的 CAS、`BETWEEN` 边界、级联删除
- [x] 变异验证：改错 DAO 的 WHERE 条件后 harness 变红
- [x] 接入 `verify-all` 并作为独立步骤报告结果
- [x] 明确的覆盖边界写入任务与报告（不含 Room 映射与 Flow）

## 7. Tests
- `node tools/verify-dao.mjs`
- 变异 1：`observeActive` 去掉 `archived_on IS NULL` → 应变红
- 变异 2：`findRange` 的 `BETWEEN` 改为开区间 → 应变红
- `pwsh -NoProfile -File tools/verify-all.ps1`

## 8. Risks and Decisions
- 复制了 `verify-schema.mjs` 的建库逻辑（约 20 行）而没有抽取共享模块：避免改动一个已 40/40 通过的脚本；后续若出现第三处再抽。
- 解析器只处理 `@Query` 的字符串拼接，不处理运行期拼接的 SQL（DAO 中不存在）。
- `@Upsert` 的 SQL 由 Room 生成，harness 无法直接取得，因此验证的是"该冲突列上等价 upsert 语句的行为"以及唯一索引确实存在——这是本任务最弱的一环，其上限已写入 `TEST_REPORT.md` 的 Known Test Gaps（未覆盖项第 5 条）。

## 9. Implementation Log
- 2026-11-05：新增 `verify-dao.mjs`，用括号深度扫描解析 `@Query`，命名参数按出现顺序绑定为位置参数。
- 2026-11-05：21 项断言全部通过（含"预期 @Query 集合与已解析集合相等"守卫）；变异 1（`observeActive` 去掉归档过滤）→ 1 项失败；变异 2（`findRange` 改开区间）→ 2 项失败；还原后全绿。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `node tools/verify-dao.mjs` | **DAO SQL PASS** | 21 项断言、8 个触发器 |
| 变异 1：`HabitDao.observeActive` 去掉 `archived_on IS NULL` | **FAILED（1 项）** | `observeActive after archive expected 1, got 2` |
| 变异 2：`HabitLogDao.findRange` 的 `BETWEEN` 改开区间 | **FAILED（2 项）** | 两条边界断言均 `got []` |
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS**（exit 0） | 治理 21 项全部通过、0 警告；单测 40 项、DAO SQL 21/21、`assembleDebug` 20s、空白 0s |

## 11. Completion Review
- [x] 验收标准全部满足（5/5）
- [x] 相关测试实际运行（21 项 + 5 组变异）
- [x] `git diff --check` 通过
- [x] Diff 已审查：无产品代码、无依赖、无 schema 改动
- [x] 文档/状态已更新
- [ ] 独立 Reviewer `APPROVE`

## 12. Handoff
- 下一步：评审后合入 `main`；随后开始 ViewModel 层（任务文件尚未创建）。
- 遗留：Room 映射与 Flow 通知仍无自动化验证。

## 13. Independent Review
- Reviewer record：`project/reviews/REVIEW-TASK-010.md`
- 结论：`PENDING`

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-010
- 状态：`PARTIAL`
- 阶段：Gate 2
- 验收：通过 5 项 / 未通过 0 项
- 最近测试记录：2026-11-05 / **VERIFY ALL PASS**（exit 0）
<!-- close-task:end -->
