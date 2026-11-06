# LAST_HANDOFF.md

> 每次 AI 会话结束前更新。记录事实，不写愿望或推测。

## Snapshot
- 日期时间（含时区）：`2026-11-06`（Asia/Shanghai）
- 当前阶段：`Gate 3`（出口 = `TASK-026` + 窄口径评审 `R1`；两者均未完成评审）
- 当前任务：`TASK-035`（`IMPLEMENTED`：日切点 04:00、设计偏差 ADR/追踪表、移除统计页死控件，159 项单测 + 真机自测）；8 个 `IMPLEMENTED` 待批量评审
- 当前 Git 分支：`main`（本地，无远程）；最近 commit 以 `git log -1 --oneline` 为准
- **会话状态：可继续**——设备（`10CDAK0B71005KU`）在线，数据完好（`TASK-026` 跑完被卸载后已从备份恢复）
- **设备测试怎么跑**：唤醒 + `adb shell svc power stayon usb` + `gradlew.bat connectedDebugAndroidTest`；**跑前必须备份 DB**（AGP 结束会卸载 App）

## Completed This Session
- 收到并归档外部评审 `ADVICE-RESPONSE-2026-11-06.md`（374 行），采纳主线：先 `TASK-026` → 窄口径评审 → 备份拆分。
- `TASK-026`（设备侧 Room 契约测试）：`DatabaseContractTest` 5 项，真机 6/6；并实测确认「跑完 App 被卸载」→ 备份/恢复流程走通。
- 用户拍板：**日切点为凌晨 04:00**；其余「你看着办」→ 落成 `ADR-017` 与 `DESIGN-DELTA.md`：
  - 归档保持展示层树（`archived_on` 单一来源），不建物理月/日父表
  - 提醒进 MVP，排在备份之后；提醒数据模型 ADR 先于备份格式
  - 第三张卡保留「本月次数」；编辑不改图标/颜色/类型
  - 统计周/年切换延后，死控件已删除（改为静态「本月」）
- `TASK-035`（日切改造）：
  - `domain/calculation/DayCutoff`：`logicalDate / today / logicalDateOf / millisUntilNextCutoff`（Zone 感知）
  - 今日/新建/详情/统计四个 Presenter 的 `today` 默认值与 `AppRoot` 跨日定时器全部改走 `DayCutoff`
  - `TodayPresenter.millisUntilNextMidnight` → `millisUntilNextDay`（04:00 口径），4 条断言改写
  - 新增 `DayCutoffTest` 5 项；单测 154 → **159**
  - 删除统计页 `PeriodSwitch`（周/月/年死控件）

## Files Changed
- 新增：`domain/calculation/DayCutoff.kt`、`test/.../DayCutoffTest.kt`、`project/decisions/ADR-016-day-cutoff-four-am.md`、`ADR-017-v1-design-deltas.md`、`project/DESIGN-DELTA.md`、`project/tasks/TASK-035.md`、`app/src/androidTest/.../DatabaseContractTest.kt`、`project/reports/ADVICE-RESPONSE-2026-11-06.md`
- 修改：四个 Presenter、`AppRoot.kt`、`StatisticsScreen.kt`、`TodayPresenterTest.kt`、`TASK-026.md`、`PROJECT_STATUS.md`、`README.md`、`TEST_REPORT.md`、`FEATURE_MATRIX.md`、本文件

## Verification Actually Run
| 命令 | 结果 | 备注 |
|---|---|---|
| `gradlew.bat assembleDebug assembleDebugAndroidTest` | **BUILD SUCCESSFUL** | `TASK-026` 测试 APK |
| `gradlew.bat connectedDebugAndroidTest` | **PASS**（40s） | 6 tests / 0 failures；跑完 App 被卸载，已恢复数据 |
| `tools/run-unit-tests.ps1` | **OK (159 tests)** | 154 + `DayCutoffTest` 5；定时器断言改为 04:00 |
| 真机：日切改造后今日页 + 统计页 | **PASS** | `11月6日 · 周五`、3 个习惯不变；统计页无「周/年」节点 |
| `tools/verify-all.ps1` | **VERIFY ALL PASS** | 治理 22/22、单测 159、DAO 38、构建、`git diff --check` |

## Not Verified / Known Gaps
- **04:00 真实跨日未在真机验证**：需要凌晨手测或 root 改时钟；当前只有 5 项 JVM 测试
- **设备测试会卸载 App**：跑前备份；长期方案（`applicationIdSuffix`/专用测试机）未定
- **迁移测试未建**：schema 仍 v1，`MigrationTestHelper` 留到第一次 v2
- **UI 自动化仍为零**（评审 `N6`，≤8 用例，依赖 `TASK-026` 底座）
- **评审债 8 项**：必须由新上下文做 `R1`，本会话自查不算
- 真机数据仍是单日计划版本：多周/跨月/跨年边界只有单测
- 待办：`N5` 提醒数据模型 ADR → `TASK-030a` JSON 导出 → `030b` 覆盖恢复

## Lessons Worth Keeping
- **「今天」必须只有一个来源**：四个 Presenter 各自 `LocalDate.now()` 时，日切改一处就会漏三处；`DayCutoff` 收口后只改一处
- **测试先写边界**：04:00 的 23:59:59/00:00/01:00 三组断言比「跑一遍界面」更早抓住改口径时的遗漏
- **死控件直接删**：变淡的「周/年」仍会让用户点，删掉后统计页语义更清楚（`ADR-017`）
- **`connectedAndroidTest` 会卸载 App**：备份 `db/-wal/-shm` 是硬前置，不是建议

## Next Safe Action
1. **`R1` 窄口径里程碑评审**（新上下文）：偏差表 + 写路径/规则边界 + 约 10 个变异抽查，覆盖 8 个 `IMPLEMENTED`（`TASK-027/028/029/031/032/033/034/035`，外加 `TASK-026`）
2. **`N5` 提醒数据模型 ADR**（是否 schema v2；先于备份格式）
3. **`TASK-030a` JSON 导出**（只读、低风险）→ `030b` 覆盖恢复（依赖 `TASK-026`）
4. 恢复开工前先跑 `tools/verify-all.ps1`；不要把任何 `IMPLEMENTED` 改成 `DONE`——缺独立评审
