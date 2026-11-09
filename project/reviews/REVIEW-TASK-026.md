# REVIEW-TASK-026: 独立评审记录

- 审查任务：`TASK-026`
- 审查 commit：`341a8995377e7c425af673b6aa73fa61a88c7ef0`（当前 HEAD；任务交付 commit `7540d36`）
- 审查范围：任务交付至今的累计代码（`7540d36`..`341a899`。交付提交 8 文件 +748/−85：`DatabaseContractTest.kt` 259 行 + 状态文档 + 外部评审报告；此后无产品代码变动）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-09`
- 轮次：第 1 轮
- 评审时工作区：评审期间有并行会话的未提交改动（`app/src/main/java/com/habittracker/core/storage/CustomIcons.kt`、`app/src/main/java/com/habittracker/feature/profile/ProfileScreen.kt`、`project/FEATURE_MATRIX.md`、`project/PROJECT_STATUS.md`、`project/decisions/ADR-019-backup-import.md`、`project/tasks/TASK-039.md`，以及未跟踪的 `project/reviews/REVIEW-TASK-035.md`、`REVIEW-TASK-037.md`）。本评审一律以 HEAD `341a899` 的已提交内容为准，不评判这些未提交改动。

## 1. Scope

**裁决：通过。**

- 交付物与 §3 的 5 个用例一一对应（`DatabaseContractTest.kt`）：触发器 + 重开（:100-136）、Converter/实体往返（:138-187）、Flow 失效（:189-200）、四表级联（:202-227）、真实事务回滚（:229-249）。
- 无产品代码改动：`git show --stat 7540d36` 只有该测试文件 + `README.md`/`FEATURE_MATRIX.md`/`PROJECT_STATUS.md`/`LAST_HANDOFF.md`/`TEST_REPORT.md`/`TASK-026.md` + 外部评审报告；**无 `app/schemas`、无 `DatabaseConstraints.kt`、无 DAO、无依赖/权限/Gradle 改动**。
- 与 §4 Out of Scope 一致：没有 UI 自动化、没有 `MigrationTestHelper`、测试底座仍留在测试类内（私有 helper :75-88、:251-258）。

## 2. Diff 核对

**裁决：通过。**

- `git diff --check`：**exit 0**；治理检查「no conflict markers」「no trailing whitespace」均 PASS。
- 未删除或绕过既有测试：`SmokeTest` 保留；`app/src/androidTest` 现有 `@Test` 计数 = **6**（本类 5 + `SmokeTest` 1），与 §6 第 1 项「连同 `SmokeTest` 共 6 项」一致。
- 断言有判别力，逐项核对后无「恒真断言」：
  - 触发器清单按名字**全等**比较（:102-112、:130-135），不是 `contains`；
  - 非法行用 `assertRejected`（:251-258）要求 `SQLiteException` 且 message 含 `invalid`，并断言 `count("habits") == 0`（:127、:135），证明是拒绝而不是静默丢弃；
  - 回滚用例确实能证伪事务：`HabitScheduleWriter.replaceSchedule` 先 `closeVersion`（`HabitScheduleWriter.kt:26`），随后在构造实体时调用 `ScheduleRules.maskFor`（:32）才抛异常，而 `changeSchedule` 外面包了 `withTransaction`（`RoomHabitRepository.kt:156-158`）。若事务失效，旧版本 `effective_until` 会残留，:247 的 `assertNull` 立刻变红。

## 3. 架构与数据

**裁决：通过。**

- 用生产打开路径：测试构造 `Room.databaseBuilder(context, HabitDatabase::class.java, name).addCallback(DatabaseConstraints.callback()).build()`（:69-73），与生产的 `HabitDatabase.getInstance` → `build()`（`HabitDatabase.kt:53-56`）逐行等价，只换文件名；满足 §6 第 2 项「用生产 `DatabaseConstraints.callback()`，而不是手写 DDL」。
- 隔离：每个用例在 `@Before` 用 `"contract-" + System.nanoTime() + ".db"` 新建文件库（:58），`@After` close + `deleteDatabase`（:64-67），互不依赖、跑完即删，满足 §6 第 5 项。
- 时区用例保存并还原进程默认时区（:140-141、:184-186），不会污染同进程其它用例。
- 未声明行为变化：无。schema 仍 v1，`app/schemas/.../1.json` 与 `DatabaseConstraints.kt` 的最后一次改动仍是 `eaed879`；`node tools/verify-schema.mjs` 40/40 通过（schema parity 未变）。
- 边界如实登记：迁移、Cursor 到实体的全量字段映射、并发写入未覆盖，写在任务 §4/§8 与 `TEST_REPORT.md:139`，与实现一致。

## 4. 验证证据

**裁决：通过（设备侧本轮未独立复跑，已如实标注）。**

| 项目 | 本轮实测/核对 | 结论 |
|---|---|---|
| `node tools/verify-schema.mjs` | `checks: 40, failed: 0`，`triggers installed: 8`，exit **0** | 与文档一致 |
| `node tools/verify-dao.mjs` | `checks: 45, failed: 0`，`RESULT: DAO SQL PASS`，exit **0** | 当前值（交付时为 38，见 §5） |
| `pwsh -NoProfile -File tools/check-governance.ps1` | `SUMMARY: 22 checks, 22 passed, 0 warnings, 0 failed`，exit **0** | 与 §10 一致 |
| `git diff --check` | exit **0** | 无空白错误 |
| `@Test` 静态计数 | 单测 **177**、androidTest **6** | §6 第 1 项成立 |
| 设备数据备份证据 | `.gradle/db-backup-20261106/` 下 `habit-tracker.db`（73,728 B）+ `-wal` + `-shm` 均在盘 | §6 第 7 项有实物证据 |
| `connectedDebugAndroidTest` | **本轮未运行**（主会话要求不跑 gradlew/instrumented 测试） | 采用 `TEST_REPORT.md:103` 的记录：`6 tests / 0 failures / 0 errors`、`Finished 6 tests`、XML `tests="6"` |

- 未独立复跑的那一项**不写成通过**：本评审确认的是「证据链自洽」（XML 计数 + 备份文件 + 断言与实现对应），不是「我亲眼看它跑绿」。
- 计数口径：任务 §10 的「单测 154 项」与 `TEST_REPORT.md:104`（`TASK-033` 达 154）同期，属当时真实值，不是过期断言。

## 5. 文档同步

**裁决：通过。**

- `7540d36` 同步更新了 `README.md`（当前任务 + 「最近测试」154 项 + 6 项 instrumented）、`PROJECT_STATUS.md`（:12 数据层 38/38、:13 测试 154、:30/:53/:67 契约测试条目）、`FEATURE_MATRIX.md` 脚注、`TEST_REPORT.md:103`、`LAST_HANDOFF.md`。
- 这些读数在交付当时与 `verify-dao.mjs` 的 38 项、DAO 38 项一致（`git show 7540d36` 的 diff 可见）；当前 `README.md:24` 的 159 与 `:25`/`:73` 的 38/38 已过期，但那是后续 `TASK-030`（应为 168/40）与 `TASK-039`（应为 177/45）遗留，记在那两份评审，不回溯扣本任务的分。
- 任务状态文件自洽：§6 七项全部 `[x]`、§10 四行证据、§11 只有「独立 Reviewer `APPROVE`」未勾选、自动摘要块「154 tests」与 §10 一致。

## 6. 分支与提交

**裁决：通过。**

- 交付提交 `7540d36` 聚焦（测试 + 状态文档 + 外部评审报告），提交信息与实际改动一致，无混入无关文件。
- 任务保持 `IMPLEMENTED`，并已在 `PROJECT_STATUS.md:67` 的「待批审清单」登记；治理检查第 18 项 PASS（15 个待批审），符合 `ADR-015` 的「先合入 main、里程碑批量评审」。
- 无 push、无分支破坏、无历史重写。

## 7. 结论

- 结论：`APPROVE`
- 理由：5 个用例与 §3 逐条对应；用生产 `callback()` 打开真实文件库；触发器断言覆盖首次打开与重开；回滚用例走的是真实 `RoomHabitRepository.changeSchedule` 且事务失效时必然变红；每例独立库文件、跑完即删；证据链（`TEST_REPORT.md:103` + `.gradle/db-backup-20261106/`）可核对，未覆盖项与文档口径一致。唯一无法由本轮独立证实的是真机 instrumented 复跑，已在 §4 明确标注，不构成阻塞。
- 需修复项：无。
- 非阻塞建议：
  1. `aWriteInvalidatesTheObservedFlow` 用 `delay(500)` 等 observer 注册（:195），属时间竞态（有 10s 超时兜底）。建议改成「先收到空列表初值再写入」，把等待变成事件驱动。
  2. 该用例只断言 8 个触发器的**存在**，不校验其 `WHEN` 语义；语义由 `verify-schema.mjs` 保证，两层证据合起来才完整，可在 §2 或 `TEST_REPORT.md` 明示这一分工（现由 `TEST_REPORT.md:139` 部分覆盖）。
