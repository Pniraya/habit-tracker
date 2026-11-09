# TASK-026: 设备侧 Room 契约测试

- 状态：`DONE`（独立 Reviewer `APPROVE`，见 §13）
- 阶段：`Gate 3`
- 优先级：`P0`（外部评审 `ADVICE-RESPONSE-2026-11-06.md` 建议「现在最该做的一件事」）
- 依赖：`TASK-025`（设备侧通道）
- 创建日期：`2026-11-06`
- 完成日期：`2026-11-06`

## 1. Objective
用 `TASK-025` 打通的设备侧通道，补上「Room 真的把数据存成了什么样」的自动化证据：触发器在`生产打开路径`上是否注册、实体/转换器往返、`withTransaction` 回滚、`Flow` 失效通知，以及外键级联。

## 2. Context
`tools/verify-schema.mjs`（40 项）与 `tools/verify-dao.mjs`（38 项）验证的是 **Room 导出的 schema JSON + `DatabaseConstraints.kt` 里的触发器文本**，用 `node:sqlite` 执行。它们证明 SQL 语义，但不证明：
- `DatabaseConstraints.callback()` 在真实 `Room.databaseBuilder` 打开时被调用；
- Room 的实体/Converter 映射往返正确；
- `withTransaction` 真的回滚；
- `Flow` 在写入后失效。

外部评审把这条列为 P0，因为「数据库保证不变量」目前只有 SQL 副本证据。

## 3. Scope
- 新增 `app/src/androidTest/java/com/habittracker/data/local/DatabaseContractTest.kt`：每个用例独立临时文件 DB，用与生产完全相同的 `DatabaseConstraints.callback()`。
- 5 个用例：
  1. **触发器在生产路径上生效**：查 `sqlite_master` 断言 8 个触发器；非法 `habits` 行被拒（`SQLiteException` 含 `invalid`）；关闭后重新打开仍存在且仍拒绝（覆盖「恢复/导入的库」）。
  2. **Converter + 实体往返**：闰日 `2028-02-29`、`NUMERIC` 的 REAL 精度（`0.1 + 0.2` 精确相等）、可空 `unit`、枚举、`completed_at` 配对；`TimeZone` 切到 UTC 后日期不漂移。
  3. **Flow 失效**：`observeActive()` 先挂起等待，写入后 `first { it.isNotEmpty() }` 在 10s 内返回正确行。
  4. **级联删除**：习惯 + 计划 + 日志 + 提醒各 1 行，`deleteHabit` 后四张表全 0。
  5. **真实事务回滚**：`changeSchedule` 先关闭旧版本、再用非法掩码触发异常；断言旧版本 `effective_until` 仍为 NULL（没有回滚的话旧版本会被关掉）。

## 4. Out of Scope
- UI 自动化（外部评审列为 `N6`，靠后）。
- 迁移测试（schema 仍为 v1，无迁移可测；`MigrationTestHelper` 模板留到第一次 `schema v2`）。
- 把测试底座抽成独立模块（当前是测试类内的私有 helper；抽模块等第二个调用方出现再说）。

## 5. Expected Files
- 新增：`app/src/androidTest/java/com/habittracker/data/local/DatabaseContractTest.kt`、本文件更新
- 修改：`PROJECT_STATUS.md`、`README.md`、`TEST_REPORT.md`、`LAST_HANDOFF.md`、`FEATURE_MATRIX.md`（脚注）

## 6. Acceptance Criteria
- [x] 5 个用例在真机上通过，连同 `SmokeTest` 共 6 项
- [x] 用例使用生产 `DatabaseConstraints.callback()`，而不是手写 DDL
- [x] 触发器断言覆盖「首次打开」与「重开后」
- [x] 事务回滚用例走真实 `RoomHabitRepository.changeSchedule`，不是玩具事务
- [x] 每个用例独立 DB 文件，互不依赖，跑完即删
- [x] 单测 154 项与构建仍通过
- [x] 测试运行前后的设备数据有备份/恢复证据

## 7. Tests
- `gradlew.bat connectedDebugAndroidTest`：6 tests / 0 failures / 0 errors（40s，V2238A）
- `tools/run-unit-tests.ps1`：154 项（未受本次改动影响）
- `tools/verify-all.ps1`：全绿

## 8. Risks and Decisions
- **AGP 在 `connected*AndroidTest` 结束后会卸载被测 App（已实测确认）**：跑完后 `pm list packages com.habittracker` 为空，设备数据随之清空。本次运行前已备份 DB，运行后重装并恢复，3 个测试习惯回到今日页。以后跑设备测试必须先备份；长期方案（独立 `applicationIdSuffix` 或固定测试设备）见 `ADVICE-RESPONSE` R1。
- **测试底座暂不抽模块**：5 个用例共享的是「打开临时库 + 计数」十几行代码；过早抽公共模块会让测试读起来更绕。
- **临时文件 DB 而非内存 DB**：重开用例必须落到文件；内存库无法证明「恢复/导入后仍受约束」。
- `DatabaseContractTest` 依赖 `RoomHabitRepository` 的具体行为（`changeSchedule` 先关闭后插入），这是有意为之：测的就是生产写路径。

## 9. Implementation Log
- 2026-11-06：先读 `HabitDatabase`/`DatabaseConstraints`/`Converters`/`RoomHabitRepository` 与四个 DAO，确认可测接缝；确认 `verify-*.mjs` 读的是导出 schema JSON，不是手抄 DDL。
- 2026-11-06：写 5 个用例 → `assembleDebugAndroidTest` 编译通过 → 真机一次跑绿（6/6）。
- 2026-11-06：跑完发现 App 被卸载（`pm list packages` 为空）——即外部评审 R1 警告的路径；按「force-stop → run-as 拉 db/wal/shm → 重装 → push + run-as cp → 启动」恢复，今日页重新出现 3 个习惯。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `gradlew.bat assembleDebug assembleDebugAndroidTest` | **BUILD SUCCESSFUL**（6s） | 测试 APK 与 App APK 均产出 |
| `gradlew.bat connectedDebugAndroidTest` | **PASS**（40s） | `Starting 6 tests on V2238A - 15` / `Finished 6 tests`；XML `tests="6" failures="0" errors="0"` |
| 数据保护 | **PASS** | 运行前备份至 `.gradle/db-backup-20261106`；运行后 App 被卸载 → 重装 + `run-as cp` 恢复；截图今日页 `Rread`/`Wa'l'k`/`Wa't'er` 均在 |
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS** | 治理 22/22、单测 154、DAO 38、`assembleDebug`、`git diff --check` |

## 11. Completion Review
- [x] 验收标准满足
- [x] 测试实际运行
- [x] 独立 Reviewer `APPROVE`（`REVIEW-TASK-026`，0 阻断）

## 12. Handoff
- 下一条安全操作：外部评审建议的 `N2` 窄口径里程碑评审（7 个 `IMPLEMENTED` 一起转 `DONE` 或列阻塞项），由**不带实现上下文的新会话**执行。
- `TASK-030` 按外部评审拆为 `030a 导出`（只读，可先做）与 `030b 恢复`（依赖本任务）。
- 跑设备测试前必须备份 DB（见 §8）。

## 13. Independent Review
- Reviewer record：[`REVIEW-TASK-026`](../reviews/REVIEW-TASK-026.md)
- 结论：`APPROVE`（第 1 轮独立评审，0 阻断）。评审者未参与实现，逐项复核了 数据层契约测试（触发器/回滚） 的 diff、测试证据与文档同步。

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-026
- 状态：`DONE`
- 阶段：Gate 3
- 验收：通过 7 项 / 未通过 0 项
- 最近测试记录：2026-11-09 / **OK (182 tests)**，`verify-all` 全绿
<!-- close-task:end -->
