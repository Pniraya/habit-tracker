# REVIEW-TASK-013: 独立评审记录

- 审查任务：`TASK-013`
- 审查 commit：`cf9dad0591564b3b376cef18d1a1fe6a4782a6f8`
- 审查范围：`git diff main...HEAD`（`d016442`..`cf9dad0`，1 个提交，10 文件 +324/−61）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`
- 轮次：第 1 轮

评审时工作区：`git status --short` 为空（开始前）。本轮为只读核对 + 门禁复跑 + 2 组变异 + 设备侧只读抽查；所有改动已还原，结束时工作区仅剩本评审文件。

## 1. Scope

**裁决：通过。**

- 改动与 §3/§5 一致：`TodayPresenter.kt`（新增 `showsCreateAction`）、`TodayScreen.kt`（`Box` + `FloatingActionButton`、`contentPadding(bottom = 88.dp)`、数值框 `KeyboardType.Number`）、`TodayPresenterTest.kt`（+1 项）、`project/tasks/TASK-013.md` 与状态文档。
- 新增 `TASK-014`、`TASK-015` 两个任务文件属于评审结论的后续落项，不是产品代码；无 `build.gradle.kts` / `libs.versions.toml` / `AndroidManifest.xml` / schema / 迁移改动：**无新依赖、无权限变更、无 schema 改动**。
- 未触碰统计页、我的页、编辑/删除、图标/颜色选择器。

## 2. Diff 核对

**裁决：通过。**

- `git diff --check`：**exit 0**；无冲突标记。
- FAB 的显示条件只有一个来源：`if (TodayPresenter.showsCreateAction(state))`；空态按钮仍在 `state.habits.isEmpty()` 分支内，两者互斥，不会同时出现两个入口（空态时 `showsCreateAction` 为 false）。
- `contentPadding` 加在 `LazyColumn` 上而不是外层 `Column`，符合「最后一行可滚出按钮遮挡」的意图。
- `TodayPresenter.showsCreateAction` 是 `companion object` 里的纯函数，没有引入状态或 Android 依赖。

## 3. 架构与数据

**裁决：通过。**

- 把显示规则从 Composable 移到 Presenter 符合 `AGENTS.md` §4（业务规则不散落在 Composable），也让本机可在 JVM 上证伪；Composable 只读 `state` 与纯函数。
- 未用 `Scaffold.floatingActionButton` 而用 `Box` 叠加：`AppRoot` 的 `Scaffold` 已通过 `innerPadding` 把 `NavHost` 抬到底部导航之上，再嵌套一个 Scaffold 的 FAB 槽位确实会落到导航栏下方；当前实现下 FAB 位于内容区右下角，截图中位于导航栏上方。
- 数值输入框的负值守卫仍完整：`text.trim().toDoubleOrNull()?.takeIf { it >= 0.0 }`（`TodayScreen.kt` 第 126 行），确认按钮仍以 `parsed != null` 为 `enabled`，`KeyboardType.Number` 只影响键盘类型，不影响该校验。

## 4. 验证证据

**裁决：通过。**

| 命令 / 变异 | 实际结果 | 退出码 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | 治理 21/21（0 警告）、单测 **OK (59 tests)**、DAO SQL 21 项 0 失败、`assembleDebug` BUILD SUCCESSFUL 23s、空白 0 项 | **0** |
| `pwsh -NoProfile -File tools/check-governance.ps1`（单独复跑） | `SUMMARY: 21 checks, 21 passed, 0 warnings, 0 failed` | 0 |
| `git diff --check` | `no whitespace errors` | 0 |
| 变异 M-1：`showsCreateAction` 去掉 `!state.isLoading &&` | `Tests run: 59, Failures: 1`，`theCreateActionAppearsOnlyOnceTheListHasRows` 变红 | **1** |
| 变异 M-2：`showsCreateAction` 去掉 `state.habits.isNotEmpty()` | `Tests run: 59, Failures: 1`，同一测试变红 | **1** |
| 还原后 `tools/run-unit-tests.ps1` | `OK (59 tests)` / `RESULT: UNIT TESTS PASS` | 0 |

**加载态断言的鉴別力**：M-1 变红说明「加载中且有行」这条断言确实咬住了 `!isLoading`，不是不可证伪的摆设。但要如实说明：该状态**当前不可达**——`TodayPresenter` 的初始 `_state` 是 `isLoading = true` + 空列表，之后唯一的状态入口是 `snapshot()`，它在同一次赋值里给出 `habits` 并把 `isLoading` 置为 false；没有任何路径会「保留旧行再进入 loading」。所以它是一条**契约锁**（防止将来加 `refresh()` 时旧行在加载期闪出 FAB），不是对真实用户状态的回归测试。测试注释「the list is not real yet」把不可达状态写成了现实，建议改注释或在 §8 写明。

**设备侧抽查（只读）**：`adb devices` 显示 `<device>` 在线；拉取 `databases/habit-tracker.db` + `-wal` 用 `node:sqlite` 读取：`habits` 2 行（`R`、`e'a'w'l`，均 `BOOLEAN`、`unit=NULL`）、`habit_schedules` 2 行、`habit_logs` 2 行，与「建了第 2 个习惯」一致；`.gradle/shots/fab-done.png` 中两行习惯与右下角 `+` 按钮同时可见，按钮在底部导航之上、无遮挡。名称乱码与 §14/§10 记录的「中文 IME 丢字符」相符，不是产品缺陷。

**大字号风险（如实说明，未实测）**：FAB 固定 56dp + 20dp 边距 = 76dp，列表底部留 88dp，余量 12dp；`fontScale` 不改变 FAB 尺寸，所以几何上不会因字号变大而压住最后一行。真正的风险是 88dp/20dp/56dp 是三个互不引用的魔数：若以后换成 large FAB、加文字标签或调整内边距，余量会被吃掉，而没有任何测试会失败。建议把 88dp 由 `56.dp + 20.dp + 12.dp` 推导或提为共享常量并注释来源。

未验证项：Compose 渲染与交互仍无自动化（FAB 点击、遮挡只能靠真机走查）；字体放大、横屏、三键导航未实测；`KeyboardType.Number` 在不同 IME 的实际键盘布局未验证。

## 5. 文档与分支

**裁决：通过（附 3 项非阻塞建议）。**

- 计数一致：`README.md:15` 59 项、`TEST_REPORT.md:9` 59 项、`TEST_REPORT` Runs 末两行 59 项与其变异行、`TASK-013.md` §10 59 项 + 变异行 + 真机走查行、自动摘要块 `OK (59 tests)`、`PROJECT_STATUS.md:13` 59 项、`LAST_HANDOFF` 59 项。
- `TASK-013` §12 与 `LAST_HANDOFF` 的下一任务均为 `TASK-014`，`TASK-015` 在 `LAST_HANDOFF` 的后续列表里，顺序合理。
- 分支 `fix/today-create-entry`、单提交、未推送，符合 `AGENTS.md` §7；工作区干净。
- `TASK-014` / `TASK-015` 为 `NOT_STARTED`，§10 的 verify-all 行写「未运行（任务未开始，无代码改动）」：这是诚实的空证据，不违反门禁意图（门禁要防的是 `DONE` 任务没有真实证据，而不是禁止未开始任务存在）。但检查 21 的占位符正则只有 `TBD|NOT_RUN`，中文「未运行」能通过——同一份文件若哪天被标成 `DONE` 也不会被抓住，建议把「未运行/未执行」纳入 `TASK-015` 的检查范围。

## 6. 结论

- 结论：`APPROVE`
- 证据：`verify-all` exit 0（59 项、治理 21/21、DAO 21 项、构建 23s、空白 0）；M-1 / M-2 各 1 红后还原全绿；真机数据库与截图交叉印证「两个习惯 + 右下角 `+`」；计数与状态文档一致。
- 非阻塞建议：
  1. 加载态断言对应的状态当前不可达：改测试注释或在 §8 注明它是「为将来的 refresh() 预留的契约锁」，避免读者以为它是用户可见状态的回归测试。
  2. 88dp / 20dp / 56dp 三个魔数互不引用；建议用 `FAB_SIZE + FAB_MARGIN + extra` 推导 `contentPadding`，防止将来换 large FAB 或调整边距后最后一行被遮。
  3. `KeyboardType.Number` 通常不提供小数点，而 `targetValue`/`actualValue` 是 `Double`；如果允许记录 0.5 这类零碎值，应改用 `KeyboardType.Decimal`，否则应在 §3 里写明「只支持整数实际值」。
  4. `TASK-015` 建议把 `未运行`/`未执行` 也纳入占位符检测，避免 `DONE` 任务用中文占位行绕过检查 21。
