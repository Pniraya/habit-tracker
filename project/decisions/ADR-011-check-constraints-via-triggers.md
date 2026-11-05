# ADR-011: 用触发器实现 DATABASE.md §4 的 CHECK 约束

- 状态：`ACCEPTED`
- 批准方式：用户于 `2026-11-05` 在本会话指示“你帮我做”，授权 AI 代为决定并实施该实现级决策。
- 日期：`2026-11-05`
- 关联：`DATABASE.md` §4、§12；`ADR-006`、`ADR-007`；`TASK-004`

## 背景
`DATABASE.md` §4 用 SQL `CHECK` 表达多处不变量（`status` 与 `completed_at` 一致、`DAILY=127`、`weekday_mask` 范围、`enabled ∈ {0,1}` 等），§12 还要求“违反 `status` 不变量的直接 UPDATE 被 CHECK 拒绝”。

Room 的 `@Entity` 注解无法生成 `CHECK` 约束：Room 只生成列、主键、唯一索引、外键与默认值。schema export 的 `createSql` 中也不会出现 `CHECK`。因此“Room Entity 与 §4 保持一致”在不引入额外机制时无法完整成立。

## 决策
用 `BEFORE INSERT` / `BEFORE UPDATE` 触发器实现与 `CHECK` 等价的拒绝语义，在 `RoomDatabase.Callback` 的 `onCreate` 与 `onOpen` 中幂等安装（`CREATE TRIGGER IF NOT EXISTS`）。

理由：
1. 对**任何**写入者生效，包括原始 `UPDATE`，满足 §12 的测试意图；
2. 不依赖测试能否运行，也不改变 Room 的实体定义与迁移机制；
3. 与 `CHECK` 一样在数据库内、事务内失败并回滚；
4. 幂等安装可覆盖“恢复导入的数据库文件”。

## 备选方案
- **A. 接受约束缺失，仅在 Domain/Repository 校验**：最省事，但直接 `UPDATE` 可以绕过，§12 的测试项无法满足。
- **B. `createFromAsset` 预建数据库**：可以让 DDL 带 `CHECK`，但需要在构建期生成并维护二进制数据库文件，且 Room 的 identity hash 必须匹配，升级成本高。
- **C. 自定义 `SupportSQLiteOpenHelper` 覆写建表 SQL**：技术上可行，但要重写 Room 的建表与校验流程，风险高于收益。

## 后果
- `DATABASE.md` 字面仍写 `CHECK`；若本 ADR 获批，后续基线修订应在 §4 增加一句“CHECK 以等价触发器实现”，并同步 §12 用词。**在获批前不改动 `DATABASE.md`。**
- 触发器名统一 `trg_<table>_<insert|update>`；新增约束时必须同时更新 `DatabaseConstraints` 与 `tools/verify-schema.mjs`。
- 导入流程必须“先习惯、后子表”，否则日志触发器反查不到 `tracking_type`（不会误拒，但会漏掉布尔 0/1 校验）。
