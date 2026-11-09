# REVIEW-TASK-039: 独立评审记录

- 审查任务：`TASK-039`
- 审查 commit：`341a8995377e7c425af673b6aa73fa61a88c7ef0`（当前 HEAD；任务交付 commit `fb767de`）
- 审查范围：任务交付至今的累计代码（`fb767de`..`341a899`。交付提交 17 文件 +924/−45：`BackupParser.kt` 398 行、`BackupRestorer.kt` 85 行、`BackupParserTest.kt`（9 项）、四个 DAO 的 `deleteAll()`、`ProfileScreen.kt` 恢复入口、`ADR-019`、`tools/verify-dao.mjs`、状态文档）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-09`
- 轮次：第 1 轮
- 评审时工作区：评审期间有并行会话的未提交改动（`CustomIcons.kt`、`ProfileScreen.kt`、`FEATURE_MATRIX.md`、`PROJECT_STATUS.md`、`ADR-019-backup-import.md` 的「修订（2026-11-09）」段、`TASK-039.md` 的 §4 措辞，以及未跟踪的 `REVIEW-TASK-035/037.md`）。本评审以 HEAD `341a899` 为准；§5 会指出「ADR-019 的修订段当前不在 HEAD」这一事实。

## 1. Scope

**裁决：通过。**

- 交付物与 §3/§5 一致：`BackupParser.kt`（最小 JSON 读取器 + 校验）、`BackupRestorer.kt`、`BackupParserTest.kt`、`ADR-019`、四个 DAO 的 `deleteAll()`、`ProfileScreen.kt` 的 `OpenDocument` → 摘要确认 → 恢复、`verify-dao.mjs` 的 4 个删除场景。`git show fb767de` 无 schema/迁移文件、无 `DatabaseConstraints.kt` 改动、无新依赖。
- 与 §4 Out of Scope 一致：没有合并导入、没有提醒调度重建。

## 2. Diff 核对

**裁决：通过。**

- `git diff --check`：**exit 0**；无冲突标记。
- 未删除既有测试：`BackupParserTest` 为新增；单测总数 168 → 177（静态 `@Test` 计数 = **177**，与该类 9 项相加吻合）。
- `verify-dao.mjs` 只新增 4 个 `deleteAll` 场景与全表清空断言，`MIN_CHECKS` 40 → 45（`git show fb767de -- tools/verify-dao.mjs`）。
- `ProfileScreen.kt` 的改动集中在新卡片与两个 launcher，未见顺手重构。

## 3. 架构与数据（数据安全）

**裁决：不通过。**

### 3.1 已成立的部分

- **先全量校验、后写库**：`ProfileScreen.kt:71-100` 先 `BackupParser.parse`，只有 `Ready` 才 `pendingRestore = document`（:86-89）；`Invalid` 只把前 3 条错误写进 `restoreError`（:90-93），不进入写库。
- **二次确认**：`ProfileScreen.kt:171-205`，文案含习惯数、日志数与「此操作不可撤销」（:177-178），确认按钮才调用 `restorer.restore`（:186）。
- **单事务**：`BackupRestorer.kt:24-36`，四条 `deleteAll` 与四组插入都在 `database.withTransaction` 内；`restore` 抛异常时整段回滚（删除也一并回滚），不会留下「已清空、未写入」的半状态。
- **保留原始 ID**：`toEntity()` 直接写 `id`/`habit_id`（:39-52、:54-62、:64-75、:77-84），引用不需重映射。
- **删除顺序**：先子表后主表（:27-30），不依赖 `foreign_keys` pragma 是否开启，意图清晰。
- **未声明的行为变化**：无 schema/迁移改动（`node tools/verify-schema.mjs` 40/40，含 schema parity)；四个 `deleteAll()` 在 §3 里已声明。

### 3.2 阻塞项 A：校验不完整，坏文件仍会走到写库阶段（与 §6 第 1 项不符）

§6 第 1 项写「任何错误整体失败并**显示字段路径**」，`ADR-019` :21-22 也要求「任何一条不合法就整体失败，错误提示包含字段路径」。但解析器只覆盖了 `ADR-019` :30-40 列出的子集，下列四类输入能通过 `BackupParser.parse` 返回 `Ready`，直到 `insert` 时才被数据库触发器拒绝（数据因为单事务而**不会损坏**，但用户看到的是原始 `SQLiteException` 文本，不是 `logs[2].xxx`）：

| 输入 | 解析器为何放行 | 在哪一步炸 |
|---|---|---|
| `status=COMPLETED` 但 `actual_value < target_snapshot`；或 `status=IN_PROGRESS` 但 `actual_value >= target_snapshot` | `BackupParser.kt:343-344` 只校验 `completed_at` 配对 | `DatabaseConstraints.kt:77` 的 `((NEW.status = 'COMPLETED') <> (NEW.actual_value >= NEW.target_snapshot))` → `habit_logs: invalid log` |
| `effective_until < effective_from` | `BackupParser.kt:318-320` 只校验两个日期各自的格式 | `DatabaseConstraints.kt:48` → `habit_schedules: invalid schedule` |
| `tracking_type=BOOLEAN` 且 `target_value<>1` 或 `unit` 非空；`NUMERIC` 且 `unit` 为空/空白 | `BackupParser.kt:290-294` 只校验枚举名与 `target_value>0` | `DatabaseConstraints.kt:22-23` → `habits: invalid row` |
| 日历上不存在的日期/时间，例如 `"log_date":"2026-02-30"`、`"reminder_time":"25:00:00"` | `BackupParser.kt:177-178` 只有 `^\d{4}-\d{2}-\d{2}$` / `^\d{2}:\d{2}:\d{2}$` | `BackupRestorer.kt:50/59/67/80` 的 `LocalDate.parse`/`LocalTime.parse` 在事务内抛 `DateTimeParseException`，回滚后 UI 显示 `Invalid date 'FEBRUARY 30'` |

- 复现步骤（不需要构建）：把 `.gradle/backup/export.json` 里任一条 `"log_date":"2026-11-06"` 改成 `"2026-02-30"`，其余不动 → `BackupParser.parse` 返回 `Ready`（`DATE` 正则匹配 `2026-02-30`）→ 确认恢复 → 事务内 `LocalDate.parse` 抛异常。`BackupParserTest.kt:101-102` 只测了 `"08/11/2026"`（格式错），没有测「格式对但日历上不存在」。
- 同类：`BackupParser.kt:294` 的 `val unit = obj.string("unit")` 会把 `"unit": 123`（类型错）**静默变成 `null`** 而不报错 —— `ADR-019` :33 要求「可空字段的类型必须正确」，这里是一条可绕过的类型校验。

### 3.3 阻塞项 B：计划版本区间不校验，能写入「数据库没有约束可挡」的坏数据（数据完整性缺口）

- `DATABASE.md` §4.2 规则 3/5：同一习惯的计划版本**不允许区间重叠**，且**最多一条 `effective_until IS NULL`**；§4.2 :154-162 明确「SQLite CHECK 无法表达跨行不重叠，必须由写入端在 transaction 内校验」，而唯一写入端是 `HabitScheduleWriter`（`DATABASE.md` :158-160）。
- 但 `BackupRestorer.kt:33` 直接 `database.habitScheduleDao().insert(...)`，**绕过了 `HabitScheduleWriter`**；`BackupParser.kt:225-228`/`:250-259` 只校验 `habit_id` 存在与掩码，**不校验区间**。
- 复现步骤：在 `.gradle/backup/export.json` 的 `schedules` 里复制一行，改 `id` 为未占用值、`effective_from` 提前 10 天、`effective_until` 保持 `null` → 解析 `Ready`、单事务写入**成功**（无触发器覆盖此规则）→ 之后打开今日/统计页时 `RoomHabitRepository.kt:73`/`:93` 的 `check(versions.size <= 1)` 抛 `IllegalStateException: overlapping schedule versions on ...`。这一次不是「回滚后报错」，而是**坏数据留在库里**，用户只能再导入一次或清数据。
- 这属于 `REVIEW_CHECKLIST` §3 的「数据约束被绕过」，也是本任务最重的一条。

### 3.4 非阻塞观察

- **8 MB 上限的检查顺序**：`ProfileScreen.kt:78-80` 先 `readBytes()` 把整个文件读进内存，再比较 `bytes.size > MAX_BYTES`。上限本身生效（超限拒绝），但 `ADR-019` :26 的理由是「避免把超大文件读进内存」，实现没有达到这个目的。建议先用 `OpenableColumns.SIZE` 预判，或用限长流读。
- **空数组**：`habits/schedules/logs/reminders` 全为 `[]` 时解析返回 `Ready`，确认后会把库清空且写入 0 行（弹窗显示「0 个习惯、0 条日志」）。`ADR-019` 没有禁止，二次确认也在，属可接受；但建议在 0 习惯时把确认文案改成更强的警示。
- **logs 用 `upsert`，其余用 `insert`**（`BackupRestorer.kt:34` 对 :32/:33/:35）：解析器已挡掉重复 `logs.id` 与重复 `(habit_id, log_date)`，所以语义等价；统一成 `insert` 更能表达「按原 ID 写回、重名即失败」。
- **图标图片**：备份不含图片文件，导入后回退嫩芽 —— 与 `ADR-018` :38、`TASK-039` §4 一致，不算缺口。

## 4. 验证证据

**裁决：通过（本轮未复跑 gradle/真机，已注明）。**

| 项目 | 本轮实测/核对 | 结论 |
|---|---|---|
| `node tools/verify-dao.mjs` | `checks: 45, failed: 0`，`RESULT: DAO SQL PASS`，exit **0**；末尾 5 条即 4 个 `deleteAll` + 「every table is empty after the restore wipes」 | 复现 §10 第 3 行（45 项） |
| `node tools/verify-schema.mjs` | `checks: 40, failed: 0`，`triggers installed: 8`，exit **0** | 无 schema 变动 |
| `@Test` 静态计数 | 单测 **177**（`BackupParserTest` = 9）、androidTest 6 | 复现 §6「177 项」 |
| `pwsh -NoProfile -File tools/check-governance.ps1` | `SUMMARY: 22 checks, 22 passed`，exit **0** | 与 §10 第 5 行一致 |
| `gradlew compileDebugKotlin` / 真机导入 | **本轮未运行**（主会话要求不跑 gradlew；无设备） | 采信 `TEST_REPORT.md:120`（用户手动确认）与 `:121` |

- 明确未验证的部分：`BackupParserTest` 9 项的实际通过状态、真机导入流程。本评审对解析器的判断来自**逐行代码核对**（并给出可复现的手工步骤），不是执行结果。

## 5. 文档同步

**裁决：不通过（阻塞）。**

- `README.md:24`「**159** 项单测」、`:25`「DAO SQL **38/38**」、`:71`「已验证：**159** 项通过」、`:73`「已验证：**38/38**」—— 而本任务 §10 是 177/45。交付提交 `fb767de` 把同一张表的「当前任务」行写成「177 项单测 / DAO 45 项」，与这两行**在同一个 commit 内自相矛盾**；HEAD 上「当前任务」已被 `TASK-040` 覆盖（`README.md:21`），但计数两行仍是旧值。
- `project/PROJECT_STATUS.md:12` 仍是「`verify-dao.mjs` **38/38**」；同文件 `:13` 已是 177 项单测 → 同一文件自相矛盾。
- `project/reports/TEST_REPORT.md:137`（Known Test Gaps「已覆盖」）仍写 `verify-dao.mjs`「**31 项**」。
- 任务文件自身的自动摘要块仍未更新：`TASK-039.md:92`「最近测试记录：2026-11-06 / **OK (168 tests)**」，与 §10 / §6 的 177 项冲突（`close-task.ps1` 需重跑）。
- **基线口径未在 HEAD 内解决**：`DATABASE.md:384-388`（§11 第 4/5 条）仍把「合并导入」写成 V1 必做项，而 `ADR-019` :17 决定 V1 只做覆盖式恢复。`ADR-019` 的「修订（2026-11-09）」段落当前**只存在于工作区、未提交**（`git status` 显示 `M project/decisions/ADR-019-backup-import.md`），因此在被审查的 commit 上，已批准 ADR 与冻结基线对 V1 范围的表述不一致，`ADR-019` 也只写了「留待后续」而未援引 `ADR-004` 说明基线为何不改。`AGENTS.md` §3/§11 要求这类冲突显式记录并经用户批准 —— 建议把修订段连同「基线以 ADR 修订为准」的说明一起提交。
- `check-governance.ps1` 不校验这些计数与措辞（22/22 PASS），但 `REVIEW_CHECKLIST` §4 与 `AGENTS.md` §5.6 要求成立。

## 6. 分支与提交

**裁决：通过。**

- 交付提交 `fb767de` 聚焦（解析器 + 恢复器 + 4 个 `deleteAll` + UI + 测试 + ADR-019 + 文档），中间没有夹带其它任务。
- 任务为 `IMPLEMENTED`，已登记在 `PROJECT_STATUS.md:73` 的「待批审清单」；治理检查第 18 项 PASS，符合 `ADR-015`。
- 无 push、无破坏性操作。

## 7. 结论

- 结论：`REJECT`
- 理由：
  1. **§6 第 1 项只部分成立**：解析器覆盖了 `ADR-019` 列出的检查，但「任何错误都在内存里失败、带字段路径」不成立 —— `status`/`actual_value` 一致性、`effective_until >= effective_from`、`BOOLEAN` 的 `target_value=1`/`unit` 空、日历非法日期与时间这四类输入都能先通过校验，再在写库时抛原始数据库/解析异常（单事务保证不损坏数据，但错误没有字段路径，见 §3.2）。
  2. **存在一条会把坏数据真正写进库的路径**：计划版本区间重叠/多条 open 版本不校验，`BackupRestorer` 又绕过唯一写入端 `HabitScheduleWriter`，数据库无约束可挡，导入成功后读取会抛 `overlapping schedule versions`（见 §3.3）。
  3. **文档同步未闭合**：`README.md:24/25/73`、`PROJECT_STATUS.md:12`、`TEST_REPORT.md:137`、`TASK-039.md:92` 与任务自身证据冲突；`DATABASE.md` §11 与 `ADR-019` 的基线口径差异，其修订段尚未进入 HEAD（见 §5）。
- 需修复项（按优先级）：
  1. `BackupParser` 补校验：`(status=COMPLETED) == (actual_value >= target_snapshot)`（`BackupParser.kt:343` 附近）、`effective_until >= effective_from`（:318 附近）、`BOOLEAN ⇒ target_value=1 且 unit IS NULL` / `NUMERIC ⇒ unit 非空白`（:291 附近）、日期与时间做真实解析（提到解析阶段或用严格校验），并把 `unit` 改成 `optionalString` 以免类型错被静默吞掉（:294）。`BackupParserTest` 各补 1 项。
  2. `BackupParser` 校验计划版本区间：同一 `habit_id` 的 `[effective_from, effective_until]` 不重叠、最多一条 `effective_until IS NULL`；或让恢复器改走 `HabitScheduleWriter` 的校验分支。`BackupParserTest` 补「重叠版本被拒绝」。
  3. 文档：`README.md:24/25/71/73` → 177/45；`PROJECT_STATUS.md:12` → 45/45；`TEST_REPORT.md:137` → 45 项；重跑 `tools/close-task.ps1` 修正 `TASK-039.md` 的自动摘要块；提交 `ADR-019` 的修订段并注明基线以该修订为准（`ADR-004` 流程）。
  4. 建议同批修掉 8 MB 的「先读后判」（`ProfileScreen.kt:78-80`），让它符合 `ADR-019` :26 的理由。
- 非阻塞建议：
  1. `BackupRestorer.kt:34` 的 `upsert` 改成 `insert`，让「按原 ID 写回、重复即失败」的语义与其它三表一致。
  2. 全空备份（0 习惯 / 0 日志）时确认弹窗用更强措辞（例如标题改为「将清空全部数据」），降低误触代价。
