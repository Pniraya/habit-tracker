# REVIEW-TASK-014: 独立评审记录

- 审查任务：`TASK-014`
- 审查 commit：`621186dccff47d36176b5ba41190eb05d282dd35`
- 审查范围：`git diff main...HEAD`（`421fb21`..`621186d`，1 个提交，10 文件 +142/−47）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`
- 轮次：第 1 轮

评审时工作区：`git status --short` 为空（开始前）。本轮为只读核对 + 门禁复跑 + 1 组变异；改动已还原，结束时工作区仅剩本评审文件。

## 1. Scope

**裁决：通过。**

- 改动与 §3/§5 一致：`CreateHabitUseCase.kt`（`validateName` + `normalizeTarget` + `draft.copy`）、`HabitRepository.kt`（契约 KDoc）、`RoomHabitRepository.kt`（删两处校验与 `trim`）、`FakeHabitRepository.kt`（退回哑实现）、`CreateAndArchiveHabitUseCaseTest.kt`（+4 项）与状态文档。
- 无 `build.gradle.kts` / `libs.versions.toml` / `AndroidManifest.xml` / schema / 迁移改动：**无新依赖、无权限变更、无 schema 改动**；未触碰 `RecordHabitProgressUseCase` 的规则位置。
- 行为变化（`createHabit` 现在要求已归一化输入）在 §8 明确记录，没有藏在新 KDoc 里。

## 2. Diff 核对

**裁决：通过（1 处非阻塞清理）。**

- `git diff --check`：**exit 0**；无冲突标记。
- 迁移是完整的：UseCase 增加 `validateName` + `normalizeTarget` 并 `copy(name.trim(), target, unit)`；Room 仓储删掉两处校验与 `name.trim()`；接口 KDoc 写明「输入已校验归一化，实现不得自行加校验」；fake 注释改为「故意不校验」。
- 唯一遗留：`RoomHabitRepository.kt` 第 10 行仍 `import com.habittracker.domain.calculation.HabitRules`，但文件里已无任何 `HabitRules` 引用——死 import，不影响编译，但与「数据层不再知道规则」的结论在字面上矛盾，建议删除。

## 3. 架构与数据

**裁决：通过。**

- 规则的唯一执行点是 `CreateHabitUseCase`（纯 Kotlin、无 Android 依赖）；仓储只持久化，符合 `ARCHITECTURE.md` §5.2 与 `AGENTS.md` §4。
- **生产路径核对**：`app/src/main` 中 `createHabit(` 只出现三处定义 + 一处调用——`CreateHabitPresenter.kt:73` 调用的是构造注入的 `CreateHabitUseCase`，而 `AppRoot.kt:83` 组装为 `CreateHabitPresenter(CreateHabitUseCase(repository), scope)`。**没有任何生产代码直接调用 `repository.createHabit`**，所以「Room 不再 trim」目前没有可达的绕过路径。
- **数据库兜底**：`DatabaseConstraints` 的 `trg_habits_insert` / `trg_habits_update` 在 `WHEN` 里拒绝 `BOOLEAN` 带 unit、`target_value <> 1`、`NUMERIC` 无 unit/空 unit、空名（`length(trim(NEW.name)) = 0`）、`target_value <= 0` 与非法 `tracking_type`；`node tools/verify-schema.mjs` **40/40 PASS（exit 0）**，其中就包含 `habits: BOOLEAN with unit rejected` 与 `habits: NUMERIC without unit rejected` 两条真实 SQLite 断言。删掉 Kotlin 检查不是数据完整性回归。
- 需要说清的边界：触发器只保证「合法」，不保证「归一化」——它们不 trim 存储值。若将来真出现绕过 UseCase 的写入，SQLite 会接受 `name = '  Water  '` 或 `unit = ' ml '`（合法但未归一化）。§8 已把「新实现必须经 UseCase」写成契约，建议把这一点也点明。

## 4. 验证证据

**裁决：通过。**

| 命令 / 变异 | 实际结果 | 退出码 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | 治理 21/21（0 警告）、单测 **OK (63 tests)**、DAO SQL 21 项 0 失败、`assembleDebug` BUILD SUCCESSFUL 17-18s、空白 0 项 | **0** |
| `pwsh -NoProfile -File tools/check-governance.ps1`（单独复跑） | `SUMMARY: 21 checks, 21 passed, 0 warnings, 0 failed` | 0 |
| `node tools/verify-dao.mjs` | `checks: 21, failed: 0` / `RESULT: DAO SQL PASS` | 0 |
| `node tools/verify-schema.mjs`（补充核对触发器） | `checks: 40, failed: 0`，含 `BOOLEAN with unit rejected` / `NUMERIC without unit rejected` | 0 |
| `git diff --check` | `no whitespace errors` | 0 |
| 变异：`normalizeTarget(...)` → `HabitRules.NormalizedTarget(draft.targetValue, draft.unit)` | `Tests run: 63, Failures: 4` | **1** |
| 还原后 `tools/run-unit-tests.ps1` | `OK (63 tests)` / `RESULT: UNIT TESTS PASS` | 0 |

变异打红的 4 项与判别力：`aNumericHabitWithoutAUnitIsRefusedByTheUseCase`、`aBooleanHabitCarryingAUnitIsRefusedByTheUseCase`（`normalizeTarget` 的两个分支）、`theUseCaseTrimsTheUnitBeforeStoring`（抓「调用了但忽略返回值」）、`CreateHabitPresenterTest.numericHabitWithoutUnitIsRefused`（抓错误向 UI 的传播）。四条都真实变红，没有一条是靠同义反复通过；其中第 4 条是第 1 条的高层重复，但覆盖的是错误回显路径，保留合理。

**`createCalls == 0` 的语义变化**：fake 不再可能抛异常，`createHabit` 只在被调用时自增，因此 UseCase 级测试里的 `assertEquals(0, repository.createCalls)` 现在**只能**由 `validateName`/`normalizeTarget` 抛错来解释——它真的证明「UseCase 拒绝了，仓库没被触达」。在 `TASK-012` 里这条断言是含混的（fake 自己会在自增前抛 `normalizeTarget`），现在不再含混；presenter 级测试仍可能由 presenter 或 UseCase 拦截，但那里比对的是具体错误文案（`BLANK_NAME` vs `habit name must not be blank`），判别力没有下降。

未验证项：`RoomHabitRepository` 去掉校验后的运行时行为（无 instrumentation/Robolectric，只有源码级证据与 SQLite 触发器兜底）；§8 已如实声明。

## 5. 文档与分支

**裁决：通过。**

- 计数一致：`README.md:15` 63 项、`TEST_REPORT.md:9` 63 项与 Runs 末两行（63 项 + 变异 4 红）、`TASK-014.md` §10 63 项 + 变异行、自动摘要 63 项、`PROJECT_STATUS.md:13` 63 项、`LAST_HANDOFF` 63 项。
- §8 对 `theRepositoryItselfNoLongerValidates` 的证据强度写得准确（只证明 fake、不证明 Room、未作为验收依据），§12 也把这条列为遗留；没有把「源码级证据」冒充运行时证明。
- 分支 `feat/domain-validation`、单提交、未推送，符合 `AGENTS.md` §7；工作区干净。

## 6. 结论

- 结论：`APPROVE`
- 证据：`verify-all` exit 0（63 项、治理 21/21、DAO 21 项、构建 17-18s、空白 0）；`verify-schema` 40/40 证明数据库兜底仍在；变异 4 红后还原全绿；生产代码无绕过 UseCase 的 `createHabit` 调用。
- 非阻塞建议：
  1. 删除 `RoomHabitRepository.kt` 第 10 行未使用的 `HabitRules` import——它现在会让人误以为数据层仍参与规则。
  2. `theRepositoryItselfNoLongerValidates` 的断言对象其实是 `FakeHabitRepository`，名字容易被读成「Room 仓储不校验」。建议改名（如 `theFakeRepositoryStoresWhateverItIsGiven`）或加一行注释点明「本条只针对测试替身」；§8 已说明局限，改名只是让名字与事实一致。
  3. §8 可补一句：触发器保证「合法」但不 trim，绕过 UseCase 的写入仍可能存下未归一化的空白（合法但脏）；这样「数据库兜底」的边界就完整了。
  4. `verify-schema.mjs` 已能证明 BOOLEAN+unit 被拒，§8 引用「DATABASE.md 的约束触发器」时可顺手写上这条可执行的证据。
