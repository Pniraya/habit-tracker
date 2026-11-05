# TASK-014: 领域规则上移到 UseCase

- 状态：`DONE`（独立评审 `APPROVE` 于 `621186d`，第 1 轮）
- 阶段：`Gate 3`
- 优先级：`P1`
- 依赖：`TASK-012`（暴露该问题）、`TASK-013`
- 创建日期：`2026-11-05`
- 完成日期：`2026-11-05`

## 1. Objective
把 `validateName` 与 `normalizeTarget` 从 `RoomHabitRepository` 上移到 `CreateHabitUseCase`，并在 `HabitRepository.createHabit` 的接口契约里写明调用方需保证什么。

## 2. Context
`TASK-012` 的一条测试失败暴露了这个分层错位：规则在数据层，UseCase 只校验 `name`，于是任何非 Room 实现都必须自己复述规则，漏一个就静默偏差——`FakeHabitRepository` 当时就漏了，一批测试跑在「规则更宽松的世界」里。第 1 轮评审确认这是真实问题，并明确要求单开任务，不要在 `TASK-012` 里顺手改。

## 3. Scope
- `CreateHabitUseCase`：`validateName` + `normalizeTarget`，把归一化后的 draft（含 `name.trim()` 与规范化 `unit`）传给仓库。
- `RoomHabitRepository.createHabit`：删除两处校验，直接用 `draft.targetValue` / `draft.unit` 持久化。
- `HabitRepository.createHabit`：KDoc 写明契约——输入已校验归一化，实现不得自行加校验。
- `FakeHabitRepository`：退回哑实现，不再调用 `HabitRules`。
- 单测：UseCase 拒绝无单位数值、拒绝带单位布尔、trim 单位；另有一条记录「fake 不再校验」的结构性断言。

## 4. Out of Scope
- 图标/颜色选择、编辑/删除、提醒调度。
- `RecordHabitProgressUseCase` 的规则位置（它的校验本就在 UseCase）。

## 5. Expected Files
- 修改：`domain/usecase/CreateHabitUseCase.kt`、`domain/repository/HabitRepository.kt`、`data/repository/RoomHabitRepository.kt`、`app/src/test/java/com/habittracker/domain/usecase/{FakeHabitRepository,CreateAndArchiveHabitUseCaseTest}.kt`
- 新增：`project/tasks/TASK-014.md`；状态文档

## 6. Acceptance Criteria
- [x] `CreateHabitUseCase` 调用 `validateName` 与 `normalizeTarget`，仓库只持久化
- [x] `HabitRepository.createHabit` 的契约写入接口 KDoc
- [x] `FakeHabitRepository` 不再重复实现校验
- [x] 删除 UseCase 的归一化会让单测变红（4 项）
- [x] 现有单测（63 项）与 DAO 21 项全部通过

## 7. Tests
- `pwsh -NoProfile -File tools/run-unit-tests.ps1`（新增 4 项，共 63 项）
- 变异：把 `normalizeTarget` 换成直接构造 `NormalizedTarget`，必须变红
- `gradlew.bat assembleDebug`

## 8. Risks and Decisions
- **数据库触发器仍在**：这次只移动「谁能给出友好错误」，`DATABASE.md` 的约束触发器仍是最后防线，非法数据依旧进不了库。
- **`theRepositoryItselfNoLongerValidates` 的证据强度有限**：它只能证明 `FakeHabitRepository` 不再校验；`RoomHabitRepository` 的等价行为需要 instrumentation/Robolectric，本机没有。因此这条测试被写成「结构性说明」而非「生产行为证明」，验收标准也没有依赖它。
- **行为变化**：`createHabit` 现在要求调用方传入已归一化的 draft。若将来新增实现（缓存层、内存仓库）直接接收 UI 输入，必须经过 UseCase——接口 KDoc 已写明。
- `name.trim()` 随之上移；仓库不再裁剪，避免两处都改。

## 9. Implementation Log
- 2026-11-05：`CreateHabitUseCase` 加 `normalizeTarget` 与 `draft.copy(...)`；`RoomHabitRepository` 删两处校验；接口补 KDoc；fake 退回哑实现并移除未用 import。
- 2026-11-05：首次编译失败——测试里在 `runBlocking` 外调用 `suspend` 的 `repository.getHabit`，已把断言包进 `runBlocking`。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/run-unit-tests.ps1` | **OK (63 tests)** | 新增 4 项：无单位数值被拒、带单位布尔被拒、单位被 trim、fake 不再校验 |
| 变异：`normalizeTarget` → 直接构造 `NormalizedTarget` | **FAILED（4 项）**（预期） | 两条拒绝测试与 trim 测试变红；还原后 63 项全绿 |
| `gradlew.bat assembleDebug` | **SUCCESSFUL** | 见下一行汇总 |
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS**（exit 0） | 治理 21/21（0 警告）、单测 63 项、DAO SQL 21 项、构建通过 |

## 11. Completion Review
- [x] 验收标准全部满足（5/5）
- [x] 相关测试实际运行（63 项 + 1 组变异）
- [x] `git diff --check` 通过
- [x] Diff 已审查：无新依赖、无 schema 改动、无权限变更
- [x] 文档/状态已更新
- [x] 独立 Reviewer `APPROVE`（`621186d`，第 1 轮）

## 12. Handoff
- 下一步：评审 `APPROVE` 后合入 `main`；之后 `TASK-015`（治理计数一致性）或 `TASK-016`（界面细节）。
- 遗留：`RoomHabitRepository` 去掉校验后的行为只有源码级证据，没有运行时测试。

## 13. Independent Review
- Reviewer record：`project/reviews/REVIEW-TASK-014.md`
- 结论：`APPROVE`（第 1 轮）
- 评审独立确认：`app/src/main` 中 `repository.createHabit` 只有 `CreateHabitPresenter` 通过注入的 `CreateHabitUseCase` 一处调用，没有生产代码绕过；触发器仍拒绝非法行（`verify-schema.mjs` 40/40，含 BOOLEAN 带单位、NUMERIC 无单位两条真实 SQLite 断言）
- 评审指出 `createCalls == 0` 的语义在本任务后变强：fake 不再能抛错，0 只能由 UseCase 拒绝解释
- 非阻塞项并入 `TASK-016`：`RoomHabitRepository.kt` 的死 import、`theRepositoryItselfNoLongerValidates` 应改名、§8 补「触发器不 trim」与可执行证据出处

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-014
- 状态：`DONE`
- 阶段：Gate 3
- 验收：通过 5 项 / 未通过 0 项
- 最近测试记录：2026-11-05 / **OK (63 tests)**
<!-- close-task:end -->
