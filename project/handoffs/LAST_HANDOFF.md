# LAST_HANDOFF.md

> 每次 AI 会话结束前更新。记录事实，不写愿望或推测。

## Snapshot
- 日期时间（含时区）：`2026-11-05`
- 当前阶段：`Gate 3`（Today + 新建习惯两页接真实数据；连续天数已上线并真机确认）
- 当前任务：`TASK-025`（`PARTIAL`，真机 instrumented 测试 spike；runner 可用，安装被设备拒绝）；`TASK-024`（接缝补洞）已 `DONE` 合入 `main`（第 4 轮 `APPROVE`）；`TASK-022` 已 `DONE` 合入 `main`（第 3 轮 `APPROVE`）；`TASK-021`（`F-009` 连续天数）已 `DONE` 合入 `main`；`TASK-012`~`TASK-021` 全部 `DONE`
- 当前 Git 分支：`main`（本地，无远程），`feat/seam-hardening` 已合并、保留未删；`main`（本地，无远程）@ `427e7cc`（`TASK-022` 已合入）；`feat/today-queries`、`feat/streak` 已合并、保留未删
- 最近 commit：以 `git log -1 --oneline` 为准（交接记录与代码修复同一次提交时，写死哈希必然立刻过期）
- **会话状态：可继续**——`TASK-022`、`TASK-024` 均已 `DONE` 合入 `main`；下一条从 `TASK-025` 开始（真机 instrumented 测试 spike）

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
| `pwsh -NoProfile -File tools/run-unit-tests.ps1` | **OK (105 tests)** | `TASK-021` +13；`TASK-022` +5；`TASK-024` +14（转换器 7、差分 1、往返 3、翻日 1、接线规则 1、午夜算术 1） |
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS**（exit 0） | `TASK-021` 合入时一次（`f959f02`）、`TASK-022` 实现时一次（`b73e03b`）；治理 22/22、DAO 27、构建 ~21-27s、`diff --check` 干净 |
| `pwsh -NoProfile -File tools/check-governance.ps1` | **22 / 22 passed，0 警告** | 第 1 轮评审的 1 条 warning（引用未创建的 `TASK-025`）已补建任务消除 |
| 变异 M1 `due()` 恒真 | **FAILED（2 项）** | `aDayThatWasNeverDue…`、`aScheduleChange…` |
| 变异 M2 去「今天不算断」 | **FAILED（2 项）** | 首次跑未生效（正则未匹配），已重跑并如实记录 |
| 变异 M3 presenter 写 0 | **FAILED（1 项）** | 行内连续断言 |
| 变异 M4 删 `COMPLETED` 过滤 | **FAILED（1 项）** | 补测试后；补之前 81 项全绿（缺陷） |
| 变异 M6 只取最新计划版本 | **FAILED（1 项）** | 补测试后；补之前全绿（缺陷） |
| 真机（`iQOO Neo7 SE`，Android 15） | **通过（范围有限）** | 三个已完成习惯显示 `连续 1 天`；进行中的数值习惯不显示。多段历史/最长/计划变更**未在真机造数**，仅单测覆盖 |

## Not Verified / Known Gaps
- **UI 无自动化测试**：Compose 渲染与交互仅靠真机手动走查（截图在 `.gradle/shots/`，不入库）
- **连续天数的真机证据弱**：只有「今天完成 → 1 天」文字叙述，`project/reports` 下无 dump/截图（`F-006`/`F-007` 有 `DEVICE_DUMP`）
- **N+1 已改为整表 4 次查询**（`TASK-022`，待评审）：但批量读仍取「全局最早版本 → 今天」的全部日志行，只减少往返、不减少行数（20 习惯 × 3 年 ≈ 2 万行/次），留待统计页一并收敛
- **主线程计算**：批量读仍是挂起调用，其后的逐习惯「逐日推进」是纯计算，跑在 `Dispatchers.Main.immediate` 上；最坏情况约 2 万次迭代，未实测耗时
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
1. **`TASK-025`**：`connectedDebugAndroidTest` spike——先加最小 `androidTest` 与依赖，在真机上跑一次，记录真实结果；通过则补 Room 映射与 Compose 冒烟，失败则记录报错并改走 `uiautomator dump`
2. 之后功能顺序：`F-010`/`F-011` 统计页与热力图 → `F-012`/`F-013` 提醒 → `F-014` 备份恢复 → 图标/颜色选择器 → 习惯编辑与删除
3. 遗留重构（未立任务）：`CreateHabitPresenter` 构造参数为函数类型，评审建议改为「保留 `CreateHabitUseCase` 形参 + `FakeHabitRepository.createFailure`」
4. 恢复开工前先跑 `tools/verify-all.ps1` 确认 `main` 仍绿，再读 `TASK-024`；其后的候选是 `TASK-025`（真机 instrumented 测试 spike）与 `TASK-023`（统计页）。
