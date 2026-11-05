# TASK-013: 今日页的新建入口

- 状态：`PARTIAL`（实现与单测完成，等待独立评审）
- 阶段：`Gate 3`
- 优先级：`P0`
- 依赖：`TASK-011`（Today 页）、`TASK-012`（新建页）
- 创建日期：`2026-11-05`
- 完成日期：`TBD`

## 1. Objective
有至少一个习惯之后，用户仍能新建第二个习惯；把这个入口的判断规则放进可测代码，而不是散在 Composable 里。

## 2. Context
`TASK-012` 的真机走查发现：Today 页只在**空态**渲染「新建习惯」按钮，一旦列表非空，界面上再无新建入口。原验收标准（§6「空态可进入新建页」）按字面成立，因此当时没有作为阻塞项，但作为产品路径是断的。

## 3. Scope
- `TodayScreen`：非空且非读取中时，在右下角显示 `FloatingActionButton`（`Icons.Filled.Add`）；空态仍保留原来的按钮。
- 列表加 `contentPadding(bottom = 88.dp)`，避免最后一行被按钮遮住。
- 判断规则移至 `TodayPresenter.showsCreateAction(state)`，并在 JVM 单测覆盖。
- 数值输入框补 `KeyboardType.Number`（原来弹出的是全键盘）。

## 4. Out of Scope
- 统计页、我的页、习惯编辑与删除、图标/颜色选择器。
- Compose UI 测试（本机无 Robolectric/Robolectric-free 方案）。

## 5. Expected Files
- 修改：`app/src/main/java/com/habittracker/feature/today/{TodayScreen,TodayPresenter}.kt`、`app/src/test/java/com/habittracker/feature/today/TodayPresenterTest.kt`
- 新增：`project/tasks/TASK-013.md`；状态文档

## 6. Acceptance Criteria
- [x] 列表非空时出现可点击的新建入口，点击进入新建页
- [x] 空态仍可用原按钮进入新建页（不重复出现两个入口）
- [x] 读取中不出现浮动入口
- [x] 规则在 `TodayPresenter.showsCreateAction` 中，且单测能证伪
- [x] `tools/run-unit-tests.ps1` 通过、`assembleDebug` 通过

## 7. Tests
- `pwsh -NoProfile -File tools/run-unit-tests.ps1`（新增 1 项 `theCreateActionAppearsOnlyOnceTheListHasRows`）
- 变异：把 `showsCreateAction` 的 `!state.isLoading &&` 去掉，加载态断言必须变红
- 真机走查：先建 1 个习惯 → 出现 `+` → 点击进入新建页 → 建第 2 个习惯 → 两项同列

## 8. Risks and Decisions
- **为什么把规则搬出 Composable**：`AGENTS.md` §4 要求业务规则不散落在 Composable；同时本机无法做 Compose 测试，只有搬进纯 Kotlin 才可被 JVM 测试与变异验证覆盖。
- 浮动按钮与底部导航之间留 20dp；列表底部留 88dp，避免最后一行被遮挡（真机截图确认无遮挡）。
- 未引入 `Scaffold` 的 `floatingActionButton` 槽位，因为 `AppRoot` 已有 Scaffold 承载底部导航，会话级嵌套会让按钮跑到导航栏下方；改用 `Box` 叠加。

## 9. Implementation Log
- 2026-11-05：`TodayScreen` 外层改 `Box`，加 `FloatingActionButton`；列表加底部 `contentPadding`；数值输入框键盘类型改为数字。
- 2026-11-05：先把判断写成 `!state.isLoading && state.habits.isNotEmpty()` 的内联条件，随后上移到 `TodayPresenter.showsCreateAction` 以便测试。
- 2026-11-05：首次写的加载态断言（空列表 + `isLoading=true`）不可证伪（去掉 `!isLoading` 后仍为假）；改为「加载中且有行」的构造，变异后测试变红。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/run-unit-tests.ps1` | **OK (59 tests)** | 新增 `theCreateActionAppearsOnlyOnceTheListHasRows` |
| 变异：`showsCreateAction` 去掉 `!state.isLoading &&` | **FAILED（1 项）**（预期） | 加载态断言变红；还原后 59 项全绿 |
| `gradlew.bat assembleDebug` | **SUCCESSFUL**（43s） | 首次缺 `PaddingValues`/`KeyboardOptions` 导入，已补 |
| 真机走查（`iQOO Neo7 SE`，Android 15） | **通过** | 建 1 项 → `+` 出现 → 点击进新建页 → 建第 2 项 → 列表 2 行；空名保存被拦（「名称不能为空」） |
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS**（exit 0） | 治理 21/21（0 警告）、单测 59 项、DAO SQL 21 项、`assembleDebug` 27s、空白 0s |

## 11. Completion Review
- [x] 验收标准全部满足（5/5）
- [x] 相关测试实际运行（59 项 + 1 组变异）
- [x] `git diff --check` 通过
- [x] Diff 已审查：无新依赖、无 schema 改动、无权限变更
- [x] 文档/状态已更新
- [ ] 独立 Reviewer `APPROVE`

## 12. Handoff
- 下一步：评审 `APPROVE` 后合入 `main`；之后的首选任务是 `TASK-014`（领域规则上移到 UseCase）。
- 遗留：数值输入对话框仍未在真机走通（`adb shell input` 在中文输入法下丢字符）。

## 13. Independent Review
- Reviewer record：`project/reviews/REVIEW-TASK-013.md`（待创建）
- 结论：`PENDING`

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-013
- 状态：`PARTIAL`
- 阶段：Gate 3
- 验收：通过 5 项 / 未通过 0 项
- 最近测试记录：2026-11-05 / **OK (59 tests)**
<!-- close-task:end -->
