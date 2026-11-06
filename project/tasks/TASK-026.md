# TASK-026: 设备侧 Room 契约测试

- 状态：`NOT_STARTED`
- 阶段：`Gate 3`
- 优先级：`P1`
- 依赖：`TASK-025`（真机 instrumented 测试已可用）
- 创建日期：`2026-11-05`
- 完成日期：`TBD`

## 1. Objective
用 `TASK-025` 打通的设备侧通道，补上「Room 真的把数据存成了什么样」的自动化证据：实体/转换器往返、`withTransaction` 原子性、以及 `DatabaseConstraints` 触发器**在 Room 打开数据库时是否真的被注册**。

## 2. Context
现状是：SQL 层由 `verify-schema.mjs`（40 项）与 `verify-dao.mjs`（27 项）覆盖，两者都在**独立 SQLite 进程**里执行语句。这留下了三个真实空洞：

1. `DatabaseConstraints.callback()` 是 `RoomDatabase.Callback`，`verify-schema.mjs` 永远看不到它。若 `HabitDatabase.build()` 忘了 `.addCallback(...)`，**全部 105 项单测与 27 项 DAO 检查仍然全绿**，而生产库没有任何触发器。
2. `Converters` 在真实 Room 写入路径上未被验证（现有 `ConvertersTest` 是直接调函数，绕过了 Room 的注册与取值）。
3. `withTransaction` 的原子性没有任何测试。

## 3. Scope
- `app/src/androidTest` 下新增测试：用 `Room.inMemoryDatabaseBuilder` + 真实 `Converters`/实体，验证
  - 插入后读回：`LocalDate`/`LocalTime`/三个枚举值往返不变；
  - `sqlite_master` 里存在 8 个 `trg_*` 触发器（证明 `onOpen`/`onCreate` 回调真的跑了）；
  - 非法行（如 `target_value <= 0`、`WEEKDAYS` 配 `mask=127`）被触发器以 `SQLiteConstraintException` 拒绝；
  - `withTransaction` 中途抛异常时全部回滚。
- 关键：用例必须走 `HabitDatabase` 的**同一个** `build` 配置（或抽出可注入的 builder 配置），否则测的是另一条装配路径。

## 4. Out of Scope
- UI/Compose 冒烟（另立任务）。
- 迁移测试（`v1` 是当前唯一版本，无迁移可测；等 v2 出现再写）。
- 性能/主线程耗时测量。

## 5. Expected Files
- `app/src/androidTest/java/com/habittracker/data/local/HabitDatabaseContractTest.kt`（新增）
- 可能需在 `app/build.gradle.kts` 加 `androidx.test:core` 或 `room-testing`（**需先确认是否真的必需**，不加未用的依赖）

## 6. Acceptance Criteria
- [ ] 至少 4 条设备侧用例，且每条都能被一个具体变异打红（附变异实测输出）
- [ ] 「`addCallback` 被删」这一变异**必须**让测试变红（这是本任务的核心价值）
- [ ] `verify-all.ps1` 与 `check-governance.ps1` 继续全绿；设备侧测试**不**加入默认门禁（慢、依赖插线）
- [ ] `TEST_REPORT` 记录真机实测输出

## 7. Risks
- 设备侧测试要插线、要唤醒，不能作为快速反馈回路；只作为「发布前证据」。
- `inMemoryDatabaseBuilder` 与生产 `databaseBuilder` 的配置差异可能掩盖问题；需要显式确认 callback 注册路径共享。

## 9. Implementation Log
- 尚未开始（由 `TASK-025` 评审 §5「判别力边界」与 §12 建议创建；开工后在此记录）。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | 未运行（任务未开始） | 开工后补真实结果 |

## 11. Completion Review
- [ ] 验收标准满足
- [ ] 测试实际运行
- [ ] 独立 Reviewer `APPROVE`

## 12. Handoff
- 之后：Compose 冒烟（另立任务）、统计页 `TASK-023`。

## 13. Independent Review
- Reviewer record：待创建
- 结论：`PENDING`

## 8. Notes
- `TASK-025` 评审的 N1 建议顺带处理：`TEST_REPORT` 第 15 项在「最后一行 verdict 是中文」时结果词检查恒真，建议补一条治理探针（最后一行 verdict 必须是 `PASS|FAILED|NOT_RUN` 之一）。
