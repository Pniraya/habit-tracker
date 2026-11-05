# LAST_HANDOFF.md

> 每次 AI 会话结束前更新。记录事实，不写愿望或推测。

## Snapshot
- 日期时间（含时区）：`2026-11-05`
- 当前阶段：`Gate 3`（Today + 新建习惯两页接真实数据；连续天数已上线并真机确认）
- 当前任务：`TASK-022`（`PARTIAL`，Today 查询批量化，待独立评审）；`TASK-021`（`F-009` 连续天数）已 `DONE` 合入 `main`；`TASK-012`~`TASK-021` 全部 `DONE`
- 当前 Git 分支：`main`（本地，无远程）@ `f959f02`，工作区干净；`feat/streak` 已合并、保留未删（另有 14 个历史分支同样保留）
- 最近 commit：`f959f02 merge: TASK-021 streaks`（功能 `5707e43`/`3104352`，收尾文档 `45b7425`）
- **会话状态：可继续**——`TASK-021` 已闭环（独立评审第 2 轮 `APPROVE`），下一条从 `TASK-022` 开始

## Completed This Session
- `TASK-021`（F-009 当前/最长连续天数）：`StreakRules` 纯函数一次正向遍历同时给出当前与最长；`GetStreakUseCase` 取全部计划版本 + 区间内 `COMPLETED` 日志；`TodayPresenter.streakText` 决定文案（`0/0` 不显示、`0/7` 显示「最长 7 天」、`3/7` 显示「连续 3 天 · 最长 7 天」）
- 语义决策：非计划日不计数也不打断；**今天未完成不算断**（依据 `REQUIREMENTS` 93「过去已到期的计划日」才是失败），已写入任务 §8 并由评审确认符合规格
- 评审第 1 轮 `REJECT`（`5707e43`）三条阻断：矩阵/状态仍写 F-009 未开始、删 `COMPLETED` 过滤全绿、只取最新计划版本全绿。第 2 轮 `APPROVE`（`3104352`，0 阻断，7 组自造变异全打红正确断言，另用独立模型穷举 34.4 万例验证 `current = run` 等价）
- 评审带来的实际改进：两个循环合并为一次正向遍历（today 规则不再重复）；`FakeHabitRepository` 改为保存计划历史（此前只存一条，`changeSchedule` 直接覆盖，所以「只取最新版本」这种缺陷测不出来）
- 评审非阻断建议全部落地：§10 计数 81→83→84 修正、矩阵重复的 F-009 条目合并、真机限定脚注移入脚注块、N+1 立 `TASK-022`（此前只在聊天里说「已备案」，仓库查不到，属不可核对的声称）
- 中断恢复：第 2 轮评审进程因网络中断死亡，工作区遗留变异 `from = today`；已 `git checkout --` 还原并复跑测试确认，然后另起新评审进程

## Files Changed
`TASK-021`（已合入 `main`）：
- 新增：`domain/calculation/StreakRules.kt`、`domain/usecase/GetStreakUseCase.kt`、`StreakRulesTest.kt`（9 项）、`project/tasks/TASK-021.md`、`project/tasks/TASK-022.md`、`project/reviews/REVIEW-TASK-021.md`
- 修改：`HabitRepository.kt`、`RoomHabitRepository.kt`（`scheduleVersions`、`logsInRange`）、`FakeHabitRepository.kt`（计划历史）、`TodayPresenter.kt`/`TodayScreen.kt`/`TodayUiState.kt`/`TodayViewModel.kt`、`TodayPresenterTest.kt`（+4 项）、`FEATURE_MATRIX.md`、`PROJECT_STATUS.md`、`README.md`、`TEST_REPORT.md`、本文件

## Verification Actually Run
| 命令 | 结果 | 备注 |
|---|---|---|
| `pwsh -NoProfile -File tools/run-unit-tests.ps1` | **OK (84 tests)** | 较上一存档 +13 项：9 项 `StreakRules` + 4 项 presenter（行内连续、`streakText`、IN_PROGRESS 不计入、多计划版本） |
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS**（exit 0） | 分支 `45b7425` 与合入后 `main` `f959f02` 各跑一次；治理 22/22、单测 84、DAO 27、构建 ~27s、`diff --check` 干净 |
| `pwsh -NoProfile -File tools/check-governance.ps1` | **22 / 22 passed，0 警告** | 收尾时先红过一次：`TASK-022` 的 `NOT_STARTED` 与 §9 日期行冲突、README/STATUS 无当前任务 id，已修 |
| 变异 M1 `due()` 恒真 | **FAILED（2 项）** | `aDayThatWasNeverDue…`、`aScheduleChange…` |
| 变异 M2 去「今天不算断」 | **FAILED（2 项）** | 首次跑未生效（正则未匹配），已重跑并如实记录 |
| 变异 M3 presenter 写 0 | **FAILED（1 项）** | 行内连续断言 |
| 变异 M4 删 `COMPLETED` 过滤 | **FAILED（1 项）** | 补测试后；补之前 81 项全绿（缺陷） |
| 变异 M6 只取最新计划版本 | **FAILED（1 项）** | 补测试后；补之前全绿（缺陷） |
| 真机（`iQOO Neo7 SE`，Android 15） | **通过（范围有限）** | 三个已完成习惯显示 `连续 1 天`；进行中的数值习惯不显示。多段历史/最长/计划变更**未在真机造数**，仅单测覆盖 |

## Not Verified / Known Gaps
- **UI 无自动化测试**：Compose 渲染与交互仅靠真机手动走查（截图在 `.gradle/shots/`，不入库）
- **连续天数的真机证据弱**：只有「今天完成 → 1 天」文字叙述，`project/reports` 下无 dump/截图（`F-006`/`F-007` 有 `DEVICE_DUMP`）
- **N+1 未修**（`TASK-022`）：`describe()` 每习惯 4 次查询，20 个习惯点按一次约 160 次；`record()` 后 Flow 与显式重算各跑一遍
- Room 的 Cursor 映射、Converter、Flow 通知、`withTransaction` 边界：无自动化验证（本机跑不了 Gradle 测试）
- 表单由 `remember` 持有，旋转丢输入
- 统计页、我的页仍为占位；`POST_NOTIFICATIONS` 与 `RECEIVE_BOOT_COMPLETED` 已声明但无代码，属遗留声明待清理

## Lessons Worth Keeping
- **变异测试的价值在「改坏代码看测试会不会红」**：M4/M6 两个全绿变异证明功能错了而 83 项测试没反应。别只写测试，要验证测试会失败。
- **探针/变异脚本必须自己验证自己改了东西**：M2 第一次未生效却报「通过」，是假证据。
- **改文档要一次改完**：两轮 `REJECT` 都是文档不同步（改了 A 忘了 B），不是逻辑错误。
- **声称必须可核对**：「N+1 已备案」在仓库查不到任何记录，评审直接指出。要么建任务，要么别说。
- **子进程变异可能留下脏工作区**：评审死亡时留下 `from = today`，直接提交会合入缺陷。恢复后必须复跑测试确认。

## Next Safe Action
1. **`TASK-022`（P2，`NOT_STARTED`）**：Today 列表查询批量化——去 N+1、去掉 `record()` 后的重复重算；验收用计数型 fake 断言查询次数上界
2. 之后功能顺序：`F-010`/`F-011` 统计页与热力图 → `F-012`/`F-013` 提醒 → `F-014` 备份恢复 → 图标/颜色选择器 → 习惯编辑与删除
3. 遗留重构（未立任务）：`CreateHabitPresenter` 构造参数为函数类型，评审建议改为「保留 `CreateHabitUseCase` 形参 + `FakeHabitRepository.createFailure`」
4. 恢复开工前先跑 `tools/verify-all.ps1` 确认 `main` 仍绿，再读 `TASK-022`。
