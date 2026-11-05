# REVIEW-TASK-008: 独立评审记录

- 审查任务：`TASK-008`
- 审查 commit：`6fa68f6cd597cec75fd2d0ef790f91945d44f2bd`（复审；分支 `feat/usecase-layer`，父提交 `af4a167`，merge-base `402dfc7`，对照 `main` = `c10f67d`）
- 首轮评审 commit：`af4a1674266746f1177e4c7373f5a3fb116e0f6d`（结论 `REJECT`，本文件首版）
- Reviewer：独立评审 Agent（只读评审会话，未参与 TASK-008 实现）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`（沿用仓库任务时间线）
- 复审起始状态：HEAD `6fa68f6`，`git status --short` 为空

## 0. 复审概要

首轮 11 项问题（A1–A7 文档/证据、B8–B11 测试强度）**全部关闭**，关闭依据见下表；B8–B11 用**变异验证**（改坏被测代码后看测试是否变红）判定，不看提交说明。

| 编号 | 首轮问题 | 复审证据 | 状态 |
|---|---|---|---|
| A1 | TEST_REPORT `Latest Run` 未更新 | `TEST_REPORT.md:7` 分支改为 `feat/usecase-layer`（TASK-008）；`:9` 单测 40 项、构建 38s | **CLOSED** |
| A2 | Runs 表顺序：TASK-008 行夹在 TASK-007 行之前 | TASK-008 三行现位于表尾（`:44`、`:46`、`:47`），在其后无更早任务行，时间顺序恢复 | **CLOSED**（残留建议 R1） |
| A3 | PROJECT_STATUS 未同步 | `:8` 当前任务 TASK-008；`:10` 代码实现含 UseCase；`:11` 构建 38s；`:13` 单测 40 项；`:24` `TASK-000~008`；`:29` "…+ UseCase 层（TASK-008），40 项单测通过" | **CLOSED** |
| A4 | README 未同步、且 TASK-007 状态与 PROJECT_STATUS 矛盾 | `:12` 当前任务 TASK-008（`PARTIAL`，首轮 REJECT 已修复待复审）；`:13` 含 UseCase；`:15`/`:59` 40 项；TASK-007 的 "PARTIAL 待评审" 矛盾已随该行改写消失 | **CLOSED** |
| A5 | LAST_HANDOFF 未同步 | Snapshot `:8` TASK-008 / `:9` 分支 `feat/usecase-layer`；`Verification Actually Run` 表含 `OK (40 tests)`、负向验证、`git diff --check` | **CLOSED** |
| A6 | TASK-008 §7 引用不存在的测试类 | `TASK-008.md:38` 改为 `RecordHabitProgressUseCaseTest`、`CreateAndArchiveHabitUseCaseTest` | **CLOSED** |
| A7 | 自动摘要把治理检查当测试记录 + 反引号未闭合 | `TASK-008.md:84` `阶段：Gate 2`（无残留反引号）、`:85`-`:87` "最近测试记录 … OK (40 tests)"；根因 `tools/close-task.ps1` 两处已修（见 §2） | **CLOSED** |
| B8 | 缺失习惯/未知习惯测试靠 fake 自身抛异常才绿 | fake 改为"缺 id 不抛"（`FakeHabitRepository.kt:69-73` 归档 no-op、`:110` `error()` 改抛 ISE）；测试断言异常消息。变异 M1/M2/M3 全部变红 | **CLOSED** |
| B9 | 断言 `status`/`completedAt` 是 fake 用 `LogStatusRules` 算出的，属镜像 | 改为断言仓库实存值（`RecordHabitProgressUseCaseTest.kt:70-73,86-91` 的 `actualValue`/`targetSnapshot`）。变异 M4（转发固定 1.0）变红 | **CLOSED** |
| B10 | arrange 混进 `@Test(expected)`，准备步骤异常可冒充被测行为 | 改为 `expectRejection(...)`（`:57-70`）+ arrange 在块外。变异 M5（归档恒抛 IAE）导致 4 项失败 | **CLOSED** |
| B11 | "不触碰仓库" 只写在测试名里 | `CreateAndArchiveHabitUseCaseTest.kt:60-62` 现为 `assertEquals(0, repository.createCalls)`。变异 M6（先写库后校验）`expected:<0> but was:<1>` | **CLOSED** |

## 1. Scope

- [x] 改动与任务 Scope 一致。**PASS**
  - 修复 commit `6fa68f6` 变更 10 个文件：仅测试源码（3）、文档（README / PROJECT_STATUS / TEST_REPORT / LAST_HANDOFF / TASK-008 / 本评审记录）与 `tools/close-task.ps1`；**主源码（`domain/usecase/*`、`HabitRepository`、`RoomHabitRepository`）零改动**（`git diff af4a167..HEAD -- app/src/main` 为空）。
  - 无新依赖、无 UI/Compose、无提醒/Receiver、无 schema/migration、无 `build.gradle.kts`/`gradle/` 改动。
- [x] 超出 TASK-008 §3 字面 Scope 的 `tools/close-task.ps1`（+4/−2）可追溯且必要。**PASS（附记录建议 R3）**
  - 它直接修复 A7 的两个生成缺陷：(a) `Get-Field $text '阶段：' ('Gate [0-9][^' + $tick + '\r\n]*')` —— 原写法在参数模式下 `+` 不被求值，模式被截断；(b) Runs 行过滤排除 `check-governance` 行并要求 `run-unit-tests|verify-schema|assembleDebug`，避免把治理检查当作"最近测试记录"。
  - 实测结果：生成的自动摘要 `阶段：Gate 2`、`最近测试记录：2026-11-05 / OK (40 tests)`，与我独立检查 TEST_REPORT 的"最后一条测试类行"（`:46`，OK (40 tests)）一致。修复方向正确，未降低任何检查强度（该脚本只生成摘要）。
- 结论：Scope 维度 **PASS**。

## 2. Diff 核对

- [x] 文件范围合理，无无关重构。**PASS**
  - 主源码不变；测试只做"断言加强"，未删除任何用例（13 项新增保持 13 项，总数仍 40）。
  - 未触碰受保护基线（`AGENTS.md`/`ARCHITECTURE.md`/`DATABASE.md`/`DEVELOPMENT.md`/`REQUIREMENTS.md`/`README_GOVERNANCE.md`）。
- [x] `git diff --check` 与冲突标记检查通过。**PASS**
  - `git diff --check` exit 0（复跑）；`git diff --check af4a167..HEAD` exit 0；治理检查器 `no conflict markers` / `no trailing whitespace` PASS。
- [x] 没有删除或绕过既有测试。**PASS**
  - 5 个测试类全在；`tools/run-unit-tests.ps1` 未改（本次 commit 只改 `close-task.ps1`）。
- 结论：Diff 维度 **PASS**。

## 3. 架构与数据

- [x] "计划日"规则仍在 domain/calculation，未被搬进 UseCase 或 DAO。**PASS**
  - `ScheduleRules.kt:33-37` 实现判定；`RecordHabitProgressUseCase.kt:24-26` 调用；DAO/Repository 无重复实现（`HabitScheduleDao.kt:20-25` 只按 interval 选版本行）。
- [x] 分层与 ADR 关系不变。**PASS**
  - 修复 commit 未新增 Domain→Android/Room 依赖；`RecordHabitProgressUseCase.kt:3` 的 `data.local.entity.TrackingType` 仍是既有约定（同 `HabitRepository.kt:3`、`Habit.kt:3-5`），后续可另开任务迁到 `domain/model`。
- [x] 数据约束未变。**PASS** —— 无 schema/触发器/索引/外键改动，无 migration。
- [x] 测试辅助类的"宽松化"没有削弱真实约束。**PASS**
  - `FakeHabitRepository` 的归档对未知 id 变 no-op、`recordDailyValue` 对未知 id 抛 `IllegalStateException`——这是刻意的"只有 UseCase 的校验能让测试变红"，且已由 M1–M3 变异证明有效；真实 `RoomHabitRepository` 行为未改。
- 结论：架构与数据维度 **PASS**。

## 4. 验证证据

### 4.1 命令与退出码（本 Reviewer 复跑）

| # | 命令 | 实际输出摘要 | 退出码 |
|---|---|---|---|
| 1 | `pwsh -NoProfile -File tools/check-governance.ps1` | `SUMMARY: 20 checks, 20 passed, 0 warnings, 0 failed` / `RESULT: OK` | **0** |
| 2 | `pwsh -NoProfile -File tools/run-unit-tests.ps1` | `OK (40 tests)` / `RESULT: UNIT TESTS PASS` | **0** |
| 3 | `git diff --check` | 无输出 | **0** |
| 4 | `git status --short`（开始与结束） | 空 | — |
| 5 | `git diff --stat`（结束） | 空（变异全部还原） | — |

计数：治理 20 项 / 测试 40 项；40 = 27 既有 + 13 新增，与 `TEST_REPORT.md:46`、TASK-008 §6 一致。

### 4.2 变异验证（决定性证据）

方法：逐条用编辑器改坏被测代码 → 运行 `tools/run-unit-tests.ps1`（该脚本先执行 `gradlew compileDebugUnitTestKotlin`，编译失败会 exit 1，因此排除了陈旧 class 的干扰）→ 记录失败用例 → `git checkout -- <file>` 还原 → 确认 `git status --short` 为空。全部还原后复跑，40 项全绿。

| 变异 | 位置 | 结果 | 失败用例 |
|---|---|---|---|
| **M1** 删除存在性校验（未知 id 直接回落委托） | `RecordHabitProgressUseCase.kt:18` | exit 1，Tests run 40 / Failures 1 | `missingHabitIsRejected`（`IllegalStateException: fake repository: habit not found`） |
| **M2** 删除 Archive 存在性校验 | `ArchiveHabitUseCase.kt:10` | exit 1，1 failure | `archivingAnUnknownHabitIsRejectedByTheUseCase`（`expected IllegalArgumentException but nothing was thrown`） |
| **M3** 删除"已归档"校验 | `ArchiveHabitUseCase.kt:11` | exit 1，1 failure | `archivingTwiceIsRejectedByTheUseCase`（同上） |
| **M4** 转发值与入参不一致（固定传 `1.0`） | `RecordHabitProgressUseCase.kt:33` | exit 1，1 failure | `numericProgressIsStoredWithItsSnapshot`（`expected:<800.0> but was:<1.0>`） |
| **M5** 归档恒抛 IAE（arrange 阶段失败） | `ArchiveHabitUseCase.kt:9` | exit 1，**4 failures** | `archivingTwiceIsRejectedByTheUseCase`、`archivingAnUnknownHabitIsRejectedByTheUseCase`、`archivingStampsTheLocalDate`、`archivedHabitsRejectWrites`（首轮结构下前两者会是绿的） |
| **M6** 先写库再校验名字 | `CreateHabitUseCase.kt:12-13` | exit 1，1 failure | `blankNamesNeverReachTheRepository`（`expected:<0> but was:<1>`） |
| **M7** 删除归档校验（复核开发者记载的负向验证） | `RecordHabitProgressUseCase.kt:19` | exit 1，1 failure | `archivedHabitsRejectWrites` |

说明：**没有一次变异保持全绿**；M1/M4/M5/M6 正是首轮"同义反复"与"镜像断言"的判别点。M7 复现了 `TEST_REPORT.md:47` 记录的那次受控负向验证（"删除归档校验 → 1 项失败"），该行记录属实。

### 4.3 测试质量结论

- 首轮的 2 条镜像断言（`status`/`completedAt`）已改为断言**仓库实存值**；`status` 相关覆盖仍在 `LogStatusRulesTest`，职责不再重复。
- 所有拒绝用例都断言**异常消息**（`expectRejection`），异常类型与来源可区分。
- `@Test(expected=...)` 已全部移除（`RecordHabitProgressUseCaseTest`、`CreateAndArchiveHabitUseCaseTest` 中均无），arrange 失败会直接报错而不是被吞成"预期异常"。
- 仍未覆盖（可接受，属 TASK-008 §4 Out of Scope）：Room/DAO 事务运行时、`+Infinity` 数值、并发归档竞态。
- `TEST_REPORT.md:5-9` 的 `Latest Run`（40 项 PASS）我已亲手复现；`:47` 的负向行含义正确。残留建议见 R1（最后一行是"预期失败"行，绿色复跑未单独成行）。

### 4.4 检查器是否被"喂答案"

- `test report summary matches` 仍 PASS，但机制偏宽松：末行结果 `FAILED` → 期望摘要含 `FAILED`，而 `Latest Run` 里的 `gradlew test` `FAILED` 子句恰好满足匹配。**这是检查器既有缺陷，非本 commit 引入**；本次未利用它就虚假内容（`Latest Run` 的 40 项 PASS 独立可复现）。建议后续把该检查改为按"单测条数 + 结果词"匹配，或要求摘要的测试行与末条测试类行一致（见 R1）。

## 5. 文档与分支

- [x] 分支与提交符合 `AGENTS.md` §7。**PASS** —— 单 commit `6fa68f6` 位于 `feat/usecase-layer`（`PARTIAL` 任务留在 feature 分支）；未推送、无破坏性操作、工作区干净。
- [x] `README.md` / `PROJECT_STATUS.md` 一致且指向 TASK-008。**PASS** —— `README.md:12` 与 `PROJECT_STATUS.md:8` 任务与状态一致；测试计数 40 两处一致；TASK-007 的 DONE/APPROVE 表述不再矛盾。
- [x] `project/reports/TEST_REPORT.md` 记录了本次真实命令、结果与失败摘要。**PASS（附 R1）**
- [x] `project/handoffs/LAST_HANDOFF.md` Snapshot 已更新。**PASS（附 R4 轻微不准确）** —— 分支、当前任务、验证表、下一步均正确。
- [x] `project/tasks/TASK-008.md` 记录诚实。**PASS** —— `:68` 的 `[ ] 独立 Reviewer APPROVE` 与 `:77` 的 `复审：PENDING` 在复审前是准确的；本次结论落盘后应改为 `APPROVE` 并置 `DONE`（收尾步骤，非缺陷）。
- [x] `FEATURE_MATRIX.md` 未变可接受。**PASS** —— F-003/F-004/F-005 仍缺 UI/设备证据，保持 `NOT_STARTED`。
- 结论：文档与分支维度 **PASS**。

## 6. 结论

- 结论：**`APPROVE`**
- 证据：
  - `pwsh -NoProfile -File tools/check-governance.ps1` → 20 checks / 20 passed / 0 failed，**exit 0**
  - `pwsh -NoProfile -File tools/run-unit-tests.ps1` → `OK (40 tests)`，**exit 0**（变异后还原复跑仍为 40 OK）
  - `git diff --check` → **exit 0**；`git status --short` → 空；`git diff --stat` → 空（变异全部还原，无残留改动）
  - 变异矩阵 M1–M7：**7/7 变红**，无一次保持全绿（详见 §4.2）
  - 主源码零改动：`git diff af4a167..HEAD -- app/src/main` 为空；仅测试与文档变更
  - 首轮 A1–A7、B8–B11 全部关闭（详见 §0）
- 首轮 11 项问题全部关闭，无新增阻塞项；复审未发现功能性缺陷、越界改动或虚假证据。

### 未验证项（如实记录）
- Room/DAO/`@Upsert`/`withTransaction` 的运行时行为：本机无 Robolectric、无设备（`HypervisorPresent=False`），与 `TEST_REPORT.md` Known Test Gaps 一致，本次不改变该结论。
- `gradlew test` 仍因沙箱禁止测试 worker 失败，未作为通过证据。
- `assembleDebug` 本次未由 Reviewer 重跑（首轮评审时已在同源码基线上复核为 `BUILD SUCCESSFUL in 59s`，exit 0；本次 commit 未改主源码，故未重复）。

### 非阻塞建议（不影响本次 APPROVE）
- **R1**：`TEST_REPORT.md:47` 的"受控负向验证"是 Runs 表最后一行（结果 FAILED，预期），而绿色复跑没单独成行；`Latest Run` 因此只在宽松匹配下与末行"一致"。建议补一行"恢复后 `run-unit-tests.ps1` → OK (40 tests)"，或把负向行放在绿色行之前。
- **R2**：`README.md:14` 仍写构建 36s，`PROJECT_STATUS.md:11` 写 38s（不同次构建，均真实）；建议统一口径。
- **R3**：`tools/close-task.ps1` 的修复属 TASK-008 §3 字面 Scope 之外（虽必要），建议在 `TASK-008.md` §9 Implementation Log 补一行说明，避免"工具改动只出现在 handoff 里"。
- **R4**：`LAST_HANDOFF.md` 的 Files Changed 把 `REVIEW-TASK-006/007` 列为"新增"，实为 TASK-007 期间新增；属历史清单混写。
- **R5**：`RecordHabitProgressUseCaseTest.kt` 中 `mondayKey = monday` 是冗余别名；`assertEquals(2000.0, stored.targetSnapshot)` 仍由 fake 依据 habit 推导（有意义的是 `actualValue` 断言），可简化。
- **R6**：首轮已列的长期项仍在（`HabitRepository.dailyLog` 暂无生产调用方；`TrackingType` 等枚举建议迁至 `domain/model`；runner 的 coroutines jar 建议固定版本）。
