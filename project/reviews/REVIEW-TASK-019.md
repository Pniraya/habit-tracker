# REVIEW-TASK-019: 独立评审记录

- 审查任务：`TASK-019`
- 审查 commit：`1eb9fedab0b9f97f3f4b624ad6bf50a9c043c132`
- 审查范围：`git diff main...HEAD`（`b64326c`..`1eb9fed`，3 个提交 `b64326c` 之后为 `9ebb646`、`1eb9fed`；`git diff main...HEAD` 实际为 `b64326c`..`1eb9fed`，5 文件 +54/−21：`README.md`、`project/FEATURE_MATRIX.md`、`project/PROJECT_STATUS.md`、`project/handoffs/LAST_HANDOFF.md`、`project/tasks/TASK-019.md`）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`

评审时工作区：`git status --short` 为空（开始前）。本轮为只读核对 + 门禁复跑 + 对矩阵每行的证据抽查；结束时工作区仅剩本评审文件。

## 1. Scope

**裁决：通过。**

- 只改矩阵状态、证据与状态文件，未改 `REQUIREMENTS.md`、未实现功能、未引依赖，符合 `TASK-019` §3/§4。
- 低报修正是有依据的：F-002/F-003/F-005/F-006/F-007 的代码、单测与真机记录都可点开核对（逐项见 §4）。
- `IN_PROGRESS` 已在矩阵词表与 `check-governance.ps1` 的 `$allowed` 中，未新增词汇。

## 2. Diff 核对

**裁决：不通过（矩阵自相矛盾，见 §5-2）。**

- `git diff --check`：**exit 0**；无冲突标记；矩阵每行仍 ≥7 个管道单元格，治理词表检查通过。
- 矩阵新增脚注与证据索引，方向正确；但脚注与新增的「更新规则」对 F-003 的处置互相矛盾（§5-2）。
- 状态文件的改动只覆盖「当前任务」一行，`代码实现状态` / `FEATURE_MATRIX.md` 摘要 / handoff 的分支快照仍是旧值（§5-1、§5-4）。

## 3. 架构与数据

**裁决：通过。**

- 本次无代码改动，依赖方向、schema、权限均未动（`AndroidManifest.xml` 仍只有 `POST_NOTIFICATIONS` 与 `RECEIVE_BOOT_COMPLETED`，无 `INTERNET`）。
- 矩阵把「实现」「自动化」「真机」拆成三列，比单一状态更诚实；问题不在结构，而在个别单元格与规则不一致。

## 4. 验证证据（逐项抽查）

**裁决：通过（3 处计数/措辞需修正，见 §5）。**

| 检查项 | 实际命令/文件 | 结果 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | 治理 + 单测 + DAO + 构建 + 空白 | **VERIFY ALL PASS（exit 0）**：治理 21/21、`OK (71 tests)`、DAO 21 项、BUILD SUCCESSFUL 19s、`no whitespace errors` |
| `pwsh -NoProfile -File tools/check-governance.ps1` | 21 项 | `SUMMARY: 21 checks, 21 passed, 0 warnings, 0 failed`，exit 0 |
| F-005 自动化 | `ScheduleRulesTest.kt` 逐条读 | `DAILY → 127`、`WEEKDAYS → 31`、`CUSTOM(21) → 21`、`CUSTOM(0)/CUSTOM(128)` 抛错、`bitFor` 周一/周二/周日、`CUSTOM` 逐日判定——三种频率都被钉住，**`VERIFIED` 成立** |
| F-005 实现 | `CreateHabitScreen.kt` | 三个频率 chip（`DAILY`/`WEEKDAYS`/`CUSTOM`）+ 星期选择器存在 |
| F-004 代码 | `ArchiveHabitUseCase.kt`、`HabitRepository.restoreHabit:34`、`RoomHabitRepository.restoreHabit:82`、`FakeHabitRepository:80`、`CreateAndArchiveHabitUseCaseTest`（9 项） | 存在；main 源码里除 `ArchiveHabitUseCase.kt` 外**无任何 `archive`/`restore` 引用**，证实「无界面入口」 |
| F-009~F-014 缺席 | 47 个 `app/src` Kotlin 文件全量 grep | `streak` 0、`backup` 0、`AlarmManager` 0、`WorkManager` 0、`HttpURLConnection`/`okhttp`/`retrofit` 0 |
| F-010 缺席 | 同 grep | ⚠️ 命中 `feature/statistics/StatisticsScreen.kt`（占位页，仅两行文字）与 `AppDestination.Statistics` 导航项；`NOT_STARTED` 仍成立（`TASK-019` §6 允许「仅占位」），但 §10 的「无统计实现」措辞不准确 |
| F-015 | `AndroidManifest.xml`、`libs.versions.toml` | 仅两个权限、无 `INTERNET`；依赖只有 androidx/room/junit/ksp，无网络库 |
| F-001 真机 | `project/reports/DEVICE_DUMP-2026-11-05.md:46` | 「15:02:xx 安装 APK、启动、进入新建页」——矩阵索引未引用该文件 |
| 独立复跑 | 本体（无关） | — |

**未验证项**：真机截图未入库（`.gradle/shots/` 被忽略），故 F-002/F-006/F-007 的真机列只能核到任务 §10 的文字记录；本次不重跑真机。

## 5. 文档与状态文件

**裁决：不通过。**

1. 🚫 `project/PROJECT_STATUS.md:10` 仍写「代码实现状态：`IMPLEMENTED_UNVERIFIED`（…**UI 与设备未验证**）」，`README.md:13` 同为 `IMPLEMENTED_UNVERIFIED`；而 `PROJECT_STATUS.md:6` 自己就写着真机走通「建习惯 → 今日出现 → 打卡 → 落库」。矩阵刚把 F-001/F-002/F-003/F-006/F-007 标为 `DEVICE_VERIFIED`，状态快照却说设备未验证——同一提交内互相否认。
2. 🚫 `project/PROJECT_STATUS.md:22` 仍写「`FEATURE_MATRIX.md`：F-001 构建验证通过、**F-002 未验证**」，F-002 已是 `DEVICE_VERIFIED`。
3. 🚫 `FEATURE_MATRIX.md:59` 新增规则「复合功能只有在**全部子功能**完成时才能整体记 `VERIFIED`/`DEVICE_VERIFIED`；否则用 `IN_PROGRESS`」，但 `FEATURE_MATRIX.md:18` 的 F-003 是 `IN_PROGRESS` + **自动化 `VERIFIED` + 真机 `DEVICE_VERIFIED`**，而编辑习惯完全没做。`TASK-019` §6 第 5 条（「不整体记 `DEVICE_VERIFIED`」）因此是**错误地勾选**。要么把规则限定为「实现状态列用 `IN_PROGRESS`；自动化/真机列只按已验证的子功能记，且脚注必须限定范围」，要么改 F-003 的列值。
4. 🚫 `FEATURE_MATRIX.md:47` 写 `CreateHabitPresenterTest`（**9 项**），实际 **12 个 `@Test`**（`TASK-018` 加了 3 项）。证据索引里的计数必须可核对。
5. 🚫 `project/handoffs/LAST_HANDOFF.md:9` 写「当前 Git 分支：`main` @ `b64326c`，工作区干净」，实际在 `docs/feature-matrix` @ `1eb9fed`（领先 `main` 3 个提交）；同文件 `:46` 仍写 `TASK-019`「`NOT_STARTED`」，任务已是 `PARTIAL`。

非阻塞（措辞/加固）：

- `TASK-019` §10 第 4 行「无 streak / 统计 / 提醒调度 / 备份实现」建议改成「统计仅有占位页（`StatisticsScreen.kt`），无数据逻辑；其余无实现」，与 §6 第 2 条的「文件不存在或仅占位」对齐。
- 「证据索引」只覆盖 F-001~F-007 与 F-015，F-008~F-014 没有一行；若 §6 第 4 条的「每行」指全部行，建议补一行「F-008~F-014：无实现（复核见 §10）」或把验收词改成「每个变更行」。

## 6. 结论

- 结论：`REJECT`
- 证据：门禁全绿（治理 21/21、71 项、DAO 21 项、构建 19s、空白 0）；F-005 的 `VERIFIED`、F-004 的「有实现无界面」、F-009~F-014 的缺席都经抽查成立；但矩阵新增规则与 F-003 行自相矛盾，`CreateHabitPresenterTest` 计数过期，且三份状态文件仍停留在「设备未验证 / F-002 未验证 / main @ b64326c」。
- 需修复项：
  1. `PROJECT_STATUS.md:10` 与 `README.md:13` 的 `IMPLEMENTED_UNVERIFIED`/「UI 与设备未验证」改成与矩阵一致的状态（至少注明 F-001/F-002/F-003/F-006/F-007 已真机验证）。
  2. `PROJECT_STATUS.md:22` 的「F-002 未验证」按矩阵更新。
  3. `FEATURE_MATRIX.md:59` 的复合功能规则与 `:18` 的 F-003 行二选一改到一致（推荐改规则措辞，保留 F-003 已有证据的列值）。同时修正 `TASK-019` §6 第 5 条的勾选理由。
  4. `FEATURE_MATRIX.md:47` 的「9 项」改为 12 项（或去掉计数）。
  5. `LAST_HANDOFF.md:9` 的分支/commit 改成 `docs/feature-matrix` @ `1eb9fed`，`:46` 改成 `PARTIAL`。
- 对四个提问的回答：
  1. **`IN_PROGRESS` 是对的，不要拆行**。F-003/F-004 的 id 来自 `REQUIREMENTS`，拆成 a/b 会让需求、矩阵、任务三方编号漂移；`IN_PROGRESS` + 脚注已经能表达「创建完成、编辑未开始」，词表也已有该值。要改的是规则措辞（见阻断项 3），不是拆行。
  2. **F-005 的自动化 `VERIFIED` 成立**。`ScheduleRulesTest` 不只是测一个分支：DAILY=127、WEEKDAYS=31、CUSTOM=21、非法掩码抛错、`bitFor` 三个星期、CUSTOM 逐日判定都在；创建页三个 chip 也在。真机列记 `NOT_STARTED`（只建过 DAILY）是准确的。
  3. **F-015 保持 `VERIFIED`，不要降成 `IMPLEMENTED_UNVERIFIED`**。Android 没有 `INTERNET` 权限就打不开 socket，加上无任何网络库，「核心功能不依赖网络」是结构性成立，比一条「离线可用」的测试更强；矩阵词表本身把 `VERIFIED` 定义为「自动化测试/构建通过」，构建确实通过，降级反而变成假话。但这条结论**没有任何自动化守卫**：建议在 `check-governance.ps1` 加一项「manifest 不得出现 `INTERNET`」，否则以后有人加一行权限，矩阵会静默过期。
  4. **F-001 记 `DEVICE_VERIFIED` 偏薄但不算高报**。工程骨架的主张就是「能构建、能安装、能启动、主题能渲染」，`DEVICE_DUMP-2026-11-05.md:46` 有「安装 APK、启动」的观察，足以支撑这一行；导航行为属于 F-002。建议索引里直接引用该 dump 并写明「仅安装/启动，未逐页巡检」，把「薄」写在明处，而不是留在读者心里。
- 其它非阻塞：`AndroidManifest.xml` 的 `POST_NOTIFICATIONS`/`RECEIVE_BOOT_COMPLETED` 当前无对应代码（无 Receiver、无通知实现，F-012/F-013 为 `NOT_STARTED`），属遗留声明；不在本任务范围，建议后续单独清理或在矩阵脚注里注明「为提醒预留」。
