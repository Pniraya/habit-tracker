# TASK-016: 界面细节收尾

- 状态：`PARTIAL`（实现完成，等待独立评审；真机未验证）
- 阶段：`Gate 3`
- 优先级：`P2`
- 依赖：`TASK-013`（FAB 与数值输入框）、`TASK-014`（评审非阻塞项）
- 创建日期：`2026-11-05`
- 完成日期：`TBD`

## 1. Objective
收掉 `TASK-013`/`TASK-014` 两轮评审提出的细节问题：把没有依据的魔数变成可推导的量、让数值输入支持小数、修正把契约锁写成现实的注释、删除误导性的死 import。

## 2. Context
`TASK-013` 评审指出 `88.dp`/`20.dp`/FAB 默认 `56.dp` 三个数互不引用（余量仅 12dp，改动即遮挡）；`KeyboardType.Number` 不提供小数点而模型是 `Double`；加载态断言注释把不可达状态写成用户可见现实。
`TASK-014` 评审指出 `RoomHabitRepository.kt` 仍 `import HabitRules` 但已无引用，且 `theRepositoryItselfNoLongerValidates` 这个名字会让人误以为在断言 Room 实现。

## 3. Scope
- `TodayScreen`：`ListBottomPadding = FabSize + FabMargin + FabSafetyGap`（56 + 20 + 12 = 88，与原值相等，视觉不变），FAB 边距复用 `FabMargin`。
- 数值输入框改 `KeyboardType.Decimal`。
- `RoomHabitRepository` 删除死 import。
- 测试改名 `theFakeRepositoryStoresWhateverItIsGiven`，注释点明只断言测试替身。
- `TASK-014` §8 补「触发器只保证合法、不保证归一化」。

## 4. Out of Scope
- 统计页、我的页、图标/颜色选择器。
- 主题与暗色模式。

## 5. Expected Files
- 修改：`feature/today/TodayScreen.kt`、`data/repository/RoomHabitRepository.kt`、`CreateAndArchiveHabitUseCaseTest.kt`、`project/tasks/{TASK-014,TASK-016}.md`、状态文档

## 6. Acceptance Criteria
- [x] 底边留白由常量推导，不再是裸 `88.dp`（数值仍为 88，几何不变）
- [x] 数值输入框使用 `KeyboardType.Decimal`
- [x] 死 import 删除
- [x] 测试名与断言对象一致
- [ ] 真机验证能输入 `0.5` 并落库 —— **未完成**：设备休眠并锁屏（`mWakefulness=Asleep`、`mDreamingLockscreen=true`），`adb shell input keyevent 224` 未能唤醒，未强行操作

## 7. Tests
- `pwsh -NoProfile -File tools/run-unit-tests.ps1`
- `gradlew.bat assembleDebug`
- 真机小数输入：未执行（见 §6 最后一项）

## 8. Risks and Decisions
- **小数能力仍是未验证的**：改成 `Decimal` 只保证键盘允许输入 `.`，实际能否解析 `0.5` 到 `Double` 未经真机确认。数值输入的 `toDoubleOrNull()` 本就支持小数，但“键盘给了小数点”与“字段接受小数点”是两件事，不能混为一谈。
- `ListBottomPadding` 仍是编译期常量，没有测试锁住「FAB 变大后留白要跟着变」；它有推导过程与注释，但改 FAB 类型时仍需人工检查。
- 未引入 `Pester`/Robolectric，故本次改动只有编译与既有单测覆盖，没有新增自动化测试（纯布局与键盘类型，无可断言的行为）。

## 9. Implementation Log
- 2026-11-05：新增四个私有常量并替换两处魔数；键盘类型改 `Decimal`；删死 import；测试改名并改写注释；`TASK-014` §8 补触发器边界。
- 2026-11-05：真机安装被 vivo 安装器二次确认打断一次（与 `TASK-013` 相同），随后设备休眠锁屏，未继续强推。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/run-unit-tests.ps1` | **OK (63 tests)** | 无新增测试（纯布局/键盘常量） |
| `gradlew.bat assembleDebug` | **SUCCESSFUL**（25s） | 常量与键盘类型改动编译通过 |
| 真机：输入 `0.5` | **未运行** | 设备休眠 + 锁屏，adb 唤醒失败 |
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS**（exit 0） | 治理 21/21（0 警告）、单测 63 项、DAO SQL 21 项、构建通过 |

## 11. Completion Review
- [ ] 验收标准全部满足（4/5，真机小数输入未验证）→ 任务保持 `PARTIAL`
- [x] 相关测试实际运行（63 项）
- [x] `git diff --check` 通过
- [x] Diff 已审查：无新依赖、无 schema 改动
- [x] 文档/状态已更新
- [ ] 独立 Reviewer `APPROVE`

## 12. Handoff
- 下一步：评审后合入；真机小数输入待设备解锁后补验（或由用户在设备上手动确认一次）。
- 遗留：`ListBottomPadding` 无测试锁定；小数输入未真机验证。

## 13. Independent Review
- Reviewer record：`project/reviews/REVIEW-TASK-016.md`（待创建）
- 结论：`PENDING`

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-016
- 状态：`PARTIAL`
- 阶段：Gate 3
- 验收：通过 4 项 / 未通过 1 项
- 最近测试记录：2026-11-05 / **OK (63 tests)**
<!-- close-task:end -->
