# REVIEW-TASK-008: 独立评审记录

- 审查任务：`TASK-008`
- 审查 commit：`af4a1674266746f1177e4c7373f5a3fb116e0f6d`（分支 `feat/usecase-layer`，merge-base `402dfc7`，对照 `main` = `c10f67d`）
- Reviewer：独立评审 Agent（只读评审会话，未参与 TASK-008 实现）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`（沿用仓库当前任务时间线）
- 工作区：`git status --short` 为空；评审期间只新增本文件

## 1. Scope

- [x] 改动与任务 Scope 一致。**PASS**
  - 变更 11 个文件：`HabitRepository.kt`(+6)、`RoomHabitRepository.kt`(+10)、3 个 UseCase(+64)、3 个测试文件(+316)、`project/tasks/TASK-008.md`(+59)、`TEST_REPORT.md`(+2)、`tools/run-unit-tests.ps1`(+4/-1)，与 TASK-008 §3/§5 对应。
  - 无 ViewModel、Compose、提醒/Receiver/AlarmManager、DataStore、备份、`build.gradle.kts`、`gradle/`、`app/schemas/**` 改动；`git diff --stat main...HEAD` 中不存在上述路径。
- [x] 超出 §5 预期清单的 `tools/run-unit-tests.ps1` 属必要修正。**PASS（接受）**
  - UseCase 是 `suspend` 函数，runner 用 `java -cp ... JUnitCore` 直接反射加载测试类，运行期必须能解析 `kotlinx.coroutines`；否则 `NoClassDefFoundError`。
  - 该改动**只影响 runner 进程的 `-cp`**（`tools/run-unit-tests.ps1:34,47`），不进 `build.gradle.kts`、不进 APK、不改变应用依赖。**"只加到测试 classpath"这一说法属实**（更准确说：运行 JUnit 的临时 classpath，含 main+test 编译产物）。
  - 附带风险（非阻塞）：`Find-Jar "kotlinx-coroutines-core-jvm-*.jar"` + `Select-Object -First 1`（`tools/run-unit-tests.ps1:10-15,34`）在缓存存在多版本时选取不确定，建议后续固定版本号（与其它 jar 一致）。
- 结论：Scope 维度 **PASS**，无产品功能/依赖/权限蔓延。

## 2. Diff 核对

- [x] 文件范围合理，没有无关重构。**PASS**
  - 未改动 `AGENTS.md`/`ARCHITECTURE.md`/`DATABASE.md`/`DEVELOPMENT.md`/`REQUIREMENTS.md` 等受保护基线，无 ADR 要求。
  - 未新增依赖声明；未新增权限；未新增 Gradle module；未新增后台服务。
- [x] `git diff --check` 与冲突标记检查通过。**PASS**
  - `git diff --check` exit 0；`git diff --check main...HEAD` exit 0；`check-governance.ps1` 的 `no conflict markers` / `no trailing whitespace` 均 PASS。
- [x] 没有删除或绕过既有测试。**PASS**
  - 既有 3 个测试类（`LogStatusRulesTest`、`ScheduleRulesTest`、`AppRoutesTest`）未改；测试数 27 → 40（+13）。runner 仍是"先编译、再跑全部 `*Test.class`"，无白名单收窄。
  - 说明：`tools/run-unit-tests.ps1:22` 会先执行 `gradlew compileDebugUnitTestKotlin` 并在失败时 `exit 1`，因此 40 项确实由本 commit 的源码编译得出，不是陈旧 class 文件。
- 结论：Diff 维度 **PASS**。

## 3. 架构与数据

- [x] "计划日"规则落在 domain/calculation，而非 UseCase 内联或 DAO。**PASS**
  - 判定实现在 `app/src/main/java/com/habittracker/domain/calculation/ScheduleRules.kt:33-37`（`isScheduledOn`），UseCase 只调用：`RecordHabitProgressUseCase.kt:24-26`。
  - 数据层没有重复实现该规则：`HabitScheduleDao.kt:20-25` 只按 `effective_from/effective_until` 选版本行，`HabitLogDao`/`RoomHabitRepository.recordDailyValue`（`RoomHabitRepository.kt:101-133`）不做计划日判断，符合 `ARCHITECTURE.md` §5.2「UseCase 校验计划日，Repository 只负责事务内写入」。
- [x] 分层符合基线。**PASS（含 1 条历史遗留观察）**
  - Domain 不依赖 Android API：UseCase 只 import `java.time`、`domain.*` 与 `data.local.entity` 的纯枚举；无 Android/Room 类。
  - 观察（非本次引入、不阻塞）：`RecordHabitProgressUseCase.kt:3` 从 `data.local.entity` 取 `TrackingType`，即 Domain 反向依赖 Data 包中的枚举；同一模式在 `HabitRepository.kt:3`（`FrequencyType`）、`Habit.kt:3-5`、`ScheduleRules.kt:3` 已存在。枚举无 Room 注解，属既有约定；若要彻底消解应另开任务把枚举移到 `domain/model`，超出 TASK-008 Scope。
- [x] `DATABASE.md` 的列名、可空性、索引、外键、状态不变量未被改变。**PASS**
  - 无 schema / migration / 触发器 / `app/schemas` 改动；新增的两个查询是只读，复用既有 DAO 方法。
- [x] 异常路径与数据层兜底一致。**PASS（1 条风险记录）**
  - `RoomHabitRepository.recordDailyValue`（:107-108）在事务内再次校验"习惯存在 + 未归档"，因此 UseCase 的 check-then-act（先 `getHabit` 再 `recordDailyValue`）即使被并发归档也不会写脏数据。
  - 风险（不阻塞，无文档要求）：事务内**没有**复检"计划日"，若两次调用之间计划被改版，写入可能落在非计划日；当前无并发写入口（无 ViewModel/Service），记录为后续 Gate 3 注意项。
- 观察（非阻塞）：`HabitRepository.dailyLog`（`HabitRepository.kt:40`）在 main 与 test 源码中**无任何调用方**（仅 fake/真实实现各一处），属 TASK-008 §3 明确要求的查询，但当前是死 API；建议下一个 UseCase 落地时再使用或删除。
- 结论：架构与数据维度 **PASS**。

## 4. 验证证据

### 4.1 命令与退出码（本 Reviewer 实跑，非转抄）

| # | 命令 | 实际输出摘要 | 退出码 |
|---|---|---|---|
| 1 | `pwsh -NoProfile -File tools/check-governance.ps1` | `SUMMARY: 20 checks, 20 passed, 0 warnings, 0 failed` / `RESULT: OK` | **0** |
| 2 | `pwsh -NoProfile -File tools/run-unit-tests.ps1` | `tests: LogStatusRulesTest, ScheduleRulesTest, CreateAndArchiveHabitUseCaseTest, RecordHabitProgressUseCaseTest, AppRoutesTest` → `OK (40 tests)` / `RESULT: UNIT TESTS PASS` | **0** |
| 3 | `git diff --check` | 无输出 | **0** |
| 4 | `git diff --check main...HEAD`（补充） | 无输出 | **0** |
| 5 | `gradlew.bat assembleDebug --console=plain --no-daemon`（补充复核） | `BUILD SUCCESSFUL in 59s` | **0** |

计数核对：40 = 27（既有）+ 13（新增：`RecordHabitProgressUseCaseTest` 8 + `CreateAndArchiveHabitUseCaseTest` 5），与 TEST_REPORT:40、TASK-008 §6 一致；"20 项检查"与检查器实际输出一致。**数字本身属实。**

### 4.2 测试是否真的断言行为（重点审查）

**PASS 的部分**（删掉对应用例检查后这些测试会失败，是有效断言）：
- `customScheduleRejectsAnUnscheduledWeekday`（`RecordHabitProgressUseCaseTest.kt:70-88`）——fake 不校验计划日，只有 UseCase 的 `ScheduleRules.isScheduledOn` 能抛错。
- `aDateOutsideEveryScheduleVersionIsRejected`（:90-99）——只有 `requireNotNull(activeSchedule)` 能阻止后续 NPE。
- `booleanHabitRejectsHalfValues`（:101-107）、`negativeValuesAreRejected`（:109-115）——fake 不校验值域。
- `archivedHabitsRejectWrites`（:117-124）——fake 不校验 `archivedOn`，可检出归档检查被删。
- `archivingTwiceIsRejected`（`CreateAndArchiveHabitUseCaseTest.kt:57-64`）、`blankNamesAreRejectedBeforeTouchingTheRepository`（:43-46）——分别覆盖 `ArchiveHabitUseCase.kt:11` 与 `CreateHabitUseCase.kt:12` 的前置校验。
- `creatingAHabitAlsoCreatesItsFirstScheduleVersion`（:31-41）可检出 UseCase 传错日期（fake 用传入的 `today` 建版本）。

**FAIL 的部分（tautology / 镜像实现）**：
1. `RecordHabitProgressUseCaseTest.kt:56-57` 与 `:66-67` 断言的 `status`/`completedAt` 由 `FakeHabitRepository.recordDailyValue`（`FakeHabitRepository.kt:105-112`）调用 `LogStatusRules.evaluate` 产生，是 fake 的输出，不是 UseCase 的输出。**若把 UseCase 退化成纯转发、甚至固定转发 `value = 1.0`，这两条断言仍然全绿**（800.0→1.0 对 target 2000 仍是 `IN_PROGRESS`/`completedAt = null`）。它们检出不了"UseCase 篡改参数"这类缺陷，实际是在重复 `LogStatusRulesTest` 的覆盖。
2. `RecordHabitProgressUseCaseTest.kt:126-129`（`missingHabitIsRejected`）与 `CreateAndArchiveHabitUseCaseTest.kt:66-69`（`archivingAnUnknownHabitIsRejected`）之所以通过，是因为 `FakeHabitRepository.kt:103`/`:67` 自己用 `requireNotNull` 抛 `IllegalArgumentException`。**删掉 `RecordHabitProgressUseCase.kt:18` 和 `ArchiveHabitUseCase.kt:10` 的存在性校验，这两条测试依然通过。**
3. `@Test(expected = IllegalArgumentException::class)` 把"准备步骤"也纳入了预期异常范围：
   - `RecordHabitProgressUseCaseTest.kt:121` 的归档调用在 expected 块内——若归档本身抛 `IllegalArgumentException`，测试就在被测断言之前"通过"；
   - `:94-96` 的 `requireNotNull(repository.storedSchedule(id))`（抛 IAE）同理。
   建议把 arrange 移到 expected 之外，或改用 `try/catch` + 断言异常消息。
4. 其它命名/语义小问题：`blankNamesAreRejectedBeforeTouchingTheRepository`（:44）并未断言"仓库未被触碰"；`FakeHabitRepository.archiveHabit`（:66-69）用的是 `requireNotNull`（IAE），真实 `RoomHabitRepository.archiveHabit`（:79-82）用 `check`（ISE），fake 与真实实现的异常类型不一致（本用例下由 UseCase 兜住，未暴露）。

结论：测试**不是全盘镜像**，6 条有效、2 条为镜像断言、2 处 arrange 混入 expected。以"三 UseCase 均有单测"的验收口径勉强成立，但按 `REVIEW_CHECKLIST` §3 "声称有测试证据需要能证伪实现"的标准，测试证据强度不足。

### 4.3 记录真实性

- [x] TASK-008 §7.5 的两行证据（40 tests / assembleDebug SUCCESSFUL）与实跑一致。**PASS**
- [ ] `project/reports/TEST_REPORT.md:5-9`（`Latest Run`）仍是 `main @ c10f67d`、单测 27 项，未反映本分支 `feat/usecase-layer @ af4a167` 的 40 项。**FAIL**
- [ ] `project/reports/TEST_REPORT.md:40-41`（TASK-008 的两行，均为 2026-11-05）被插在 `:42-46`（TASK-007 的行）**之前**，表格不再按时间递增；检查器 `test report summary matches` 只比对"摘要 vs 最后一列行"，因此恰好仍 PASS，但文档语义上最新一次运行（40 项）不在最后一行。**FAIL（记录顺序 + 摘要未更新）**

## 5. 文档与分支

- [x] 分支与提交符合 `AGENTS.md` §7。**PASS**
  - 单个聚焦 commit `af4a167` 位于 `feat/usecase-layer`，基线 `main`=`c10f67d`；`PARTIAL` 任务留在 feature 分支（检查器项 `partial tasks registered` PASS），未合入 `main`、未推送、无破坏性操作。
  - commit message 与改动一致；受保护基线文件未被改动，无需 ADR。
- [ ] `project/PROJECT_STATUS.md` 未同步。**FAIL**
  - `:8` 当前任务仍写 `TASK-007`（`DONE`），全文无 `TASK-008`；
  - `:13` 最近验证测试仍写"27 项"；
  - `:24` 治理资产清单仍写 `TASK-000~007`。
- [ ] `README.md` 未同步且与 `PROJECT_STATUS` 实质矛盾。**FAIL**
  - `:12` 「当前任务 `TASK-007`（`PARTIAL`，…待独立评审）」 vs `PROJECT_STATUS.md:8`「`TASK-007`（`DONE`，独立评审 `APPROVE`，已合入 `main`）」——同一事实两种状态；
  - `:15`、`:59` 仍写 27 项单测。
  - 溯源：该矛盾在 `c10f67d` 已存在（`git show main:README.md` 同样文字），不是本 commit 引入；但本分支承担"同步状态文档"的收尾职责（`AGENTS.md` §5.6、§10 阶段 6、`README.md:49`），必须在此一并修正。
- [ ] `project/handoffs/LAST_HANDOFF.md` 未同步。**FAIL**
  - `:8` 当前任务 `TASK-007`；`:9` 当前分支 `feat/governance-review-evidence`（实际为 `feat/usecase-layer`）；`Verification Actually Run` 表无 40 项记录。
- [ ] `project/tasks/TASK-008.md:48-49` 引用了不存在的测试类 `CreateHabitUseCaseTest`、`ArchiveHabitUseCaseTest`（实际为 `CreateAndArchiveHabitUseCaseTest` + `RecordHabitProgressUseCaseTest`，§7.5 写法正确，§7 与 §7.5 自相矛盾）。**FAIL**
- [ ] `project/tasks/TASK-008.md:53-58` 的自动摘要把治理检查当测试记录：`最近测试记录：2026-11-05 / exit 0，20 PASS`（20 是治理检查项数，测试是 40 项），且 `:8`/`:55` 的 `阶段：Gate 2` / Gate 3` 反引号未闭合（`close-task.ps1` 生成缺陷）。**FAIL（需在 TASK 文件里改正/注明，避免"20 PASS"被误读为测试结论）**
- 观察（不阻塞）：`project/FEATURE_MATRIX.md` 未变。F-003（创建/编辑）、F-004（归档/恢复）、F-005（计划）本次只落 UseCase/规则，无 UI 与设备证据，保持 `NOT_STARTED` 可接受；若后续把 UseCase 计为实现证据，应统一口径并同任务登记。

## 6. 结论

- 结论：**`REJECT`**
- 定级说明：**代码与测试本身可接受（无功能性缺陷、无越界改动、构建/测试/静态检查全部通过），REJECT 仅因 §4.2 测试证据强度不足 + §4.3/§5 文档同步失败**。修复项均为文档与测试加强，不需要改产品代码语义。

- 证据（命令、退出码、文件）：
  - `pwsh -NoProfile -File tools/check-governance.ps1` → 20 checks / 20 passed / 0 failed，`RESULT: OK`，**exit 0**
  - `pwsh -NoProfile -File tools/run-unit-tests.ps1` → `OK (40 tests)`，`RESULT: UNIT TESTS PASS`，**exit 0**（脚本内部先跑 `gradlew compileDebugUnitTestKotlin`，编译失败即 exit 1）
  - `git diff --check` → 无输出，**exit 0**；`git diff --check main...HEAD` → **exit 0**
  - `gradlew.bat assembleDebug --console=plain --no-daemon` → `BUILD SUCCESSFUL in 59s`，**exit 0**（复核 TASK-008 §7.5 的构建声明）
  - `git status --short` → 空；`git log --oneline main..HEAD` → 仅 `af4a167`；`git merge-base main HEAD` → `402dfc7`
  - 代码路径：`app/src/main/java/com/habittracker/domain/usecase/*.kt`、`domain/calculation/ScheduleRules.kt:33-37`、`data/repository/RoomHabitRepository.kt:70-77`

- 需修复项（阻塞合并，均不含业务语义改动）：

  **A. 文档同步（必须）**
  1. `project/reports/TEST_REPORT.md:5-9`：`Latest Run` 改为 `feat/usecase-layer @ af4a167`，单测 40 项、构建 59s 级、治理 20/20。
  2. `project/reports/TEST_REPORT.md:40-41`：把 TASK-008 两行移到表尾（`:46` 之后），恢复时间递增顺序，并让 `Latest Run` 与真正的最后一行一致。
  3. `project/PROJECT_STATUS.md:8,13,24`：当前任务/阶段改为 `TASK-008`（`PARTIAL`，待独立评审），最近测试 40 项，治理资产写 `TASK-000~008`。
  4. `README.md:12,15,59`：当前任务改 `TASK-008`；同时修掉 `TASK-007`「`PARTIAL` 待评审」与 `PROJECT_STATUS.md:8`「`DONE` 已合入」的矛盾（改为 `DONE`/`APPROVE`），测试计数 27 → 40。
  5. `project/handoffs/LAST_HANDOFF.md:8-9`：当前任务 `TASK-008`、当前分支 `feat/usecase-layer`；`Verification Actually Run` 增补 40 项与治理 20/20 两行。
  6. `project/tasks/TASK-008.md:48-49`：测试类名改为实际文件（`RecordHabitProgressUseCaseTest`、`CreateAndArchiveHabitUseCaseTest`）。
  7. `project/tasks/TASK-008.md:53-58`：自动摘要的「最近测试记录 … 20 PASS」改为真实口径（治理 20/20、单测 40），并修 `:8`/`:55` 未闭合的反引号；如属 `close-task.ps1` 生成缺陷，在任务记录中注明并另开任务修工具。

  **B. 测试加强（必须，改动仅限 test 源码）**
  8. `app/src/test/java/com/habittracker/domain/usecase/RecordHabitProgressUseCaseTest.kt:126-129`：不要依赖 fake 的 `requireNotNull`——改为断言 UseCase 抛出的具体消息（`"habit not found"`）或用不含该兜底的 fake 变体；同理 `CreateAndArchiveHabitUseCaseTest.kt:66-69`。
  9. `RecordHabitProgressUseCaseTest.kt:50-68`：`status`/`completedAt` 由 fake 计算，属镜像断言。改为断言 UseCase 真正转交的入参落库，例如用 `repository.dailyLog(habitId, monday)` 断言 `actualValue == 800.0`、`targetSnapshot == 2000.0`（fake 已实现 `dailyLog`），使"UseCase 篡改 value/target"这类缺陷可被检出。
  10. `RecordHabitProgressUseCaseTest.kt:90-99,117-124`：把 arrange（`replaceSchedule`、`ArchiveHabitUseCase(...)`）移出 `@Test(expected=...)` 块，改为 `assertThrows` 风格 + 消息断言，避免准备步骤的异常冒充被测行为。
  11. `CreateAndArchiveHabitUseCaseTest.kt:44`：要么断言"仓库未被触碰"（如 fake 增加写入计数），要么改掉测试名，避免名不副实。

  **C. 建议（不阻塞）**
  12. `tools/run-unit-tests.ps1:34`：`kotlinx-coroutines-core-jvm-*.jar` 用固定版本号替换通配 + `Select-Object -First 1`，避免多版本缓存时 runner 行为不确定。
  13. `HabitRepository.kt:40` 的 `dailyLog` 目前无调用方，建议在下一个 UseCase（今日页读取）落地时一并使用，或删除以免死 API。
  14. 长期：把 `TrackingType`/`FrequencyType`/`LogStatus` 从 `data.local.entity` 迁到 `domain/model`，消除 Domain→Data 的包依赖（另开任务，本次不要求）。

- Reviewer 未验证项（如实记录）：
  - Room/DAO/事务的真实运行时行为（`@Upsert`、`withTransaction`、`findVersionsOn` 的 SQL）——本机无 Robolectric/设备，属 `TEST_REPORT.md:48-52` 已登记的 Known Test Gap，本次未改变该结论。
  - `gradlew test` 仍会在沙箱内因测试 worker 管道被禁而失败，未以此作为通过证据。
