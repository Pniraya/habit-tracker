# LAST_HANDOFF.md

> 每次 AI 会话结束前更新。记录事实，不写愿望或推测。

## Snapshot
- 日期时间（含时区）：`2026-11-05`
- 当前阶段：`Gate 3`（Today 与新建习惯两页接真实数据，主路径已在真机走通）
- 当前任务：无（`TASK-013` 已 `DONE`，第 1 轮独立评审 `APPROVE` 于 `cf9dad0`，合入 `main` @ `b3810c5`）；`TASK-012` 已 `DONE`；`TASK-003`/`TASK-004`/`TASK-005` 为 `PARTIAL`，已在 `PROJECT_STATUS.md` 的「在册例外」登记
- 当前 Git 分支：`main`（本地，无远程）@ `b3810c5`；已合并分支 `feat/*` / `fix/*` 保留未删
- 最近 commit：以 `git log -1 --oneline` 为准；基线检查点 `d0f470a`

## Completed This Session
- `TASK-007`~`TASK-013` 全部 `DONE` 合入 `main`：治理加固、`verify-all` 单一入口、DAO SQL 验证、Today 页状态机、新建习惯页、今日页新建入口
- `TASK-012` 三轮评审：第 1 轮 `REJECT`（unit 状态泄漏真缺陷 + 三份文档未同步）、第 2 轮 `REJECT`（仅计数不一致）、第 3 轮 `APPROVE`
- 真机首次接入：`iQOO Neo7 SE`（`V2238A`，Android 15），走通「建习惯 → 今日出现 → 打卡 → 落库」，行级证据在 `project/reports/DEVICE_DUMP-2026-11-05.md`；读库后执行过 `adb shell pm clear`，故该 dump 是唯一行级证据
- `TASK-013`：Today 页非空时显示 `+` 浮动按钮（空态仍用原按钮），判断规则上移到 `TodayPresenter.showsCreateAction` 并加单测；数值输入框改数字键盘；真机验证可建第 2 个习惯
- 发现并记录：`target`/`unit` 规则在 `RoomHabitRepository`（`HabitRules.normalizeTarget`）而非 UseCase；`FakeHabitRepository` 已对齐同一规则，规则上移留给 `TASK-014`

## Files Changed
`TASK-013`：
- 修改：`feature/today/TodayScreen.kt`（`Box` + FAB、列表底部 padding、数字键盘）、`feature/today/TodayPresenter.kt`（`showsCreateAction`）、`TodayPresenterTest.kt`（+1 项）
- 新增：`project/tasks/TASK-013.md`；状态文档（`README.md`、`PROJECT_STATUS.md`、`TEST_REPORT.md`、本文件）

## Verification Actually Run
| 命令 | 结果 | 备注 |
|---|---|---|
| `pwsh -NoProfile -File tools/run-unit-tests.ps1` | **OK (59 tests)** | 新增今日页新建入口规则测试 |
| 变异：`showsCreateAction` 去掉 `!state.isLoading &&` | **FAILED（1 项）** | 加载态断言变红；还原后全绿 |
| `gradlew.bat assembleDebug` | **SUCCESSFUL**（43s） | 缺 `PaddingValues`/`KeyboardOptions` 导入，已补 |
| 真机走查 | **通过** | 建 1 项 → `+` 出现 → 进新建页 → 建第 2 项 → 列表 2 行 |
| `pwsh -NoProfile -File tools/verify-all.ps1` | 见任务 §10 | 合入前在分支上运行 |

## Not Verified / Known Gaps
- **UI 无自动化测试**：Compose 渲染与交互仅由真机手动走查覆盖（截图在 `.gradle/shots/`，不入库）
- **数值输入对话框未在真机走通**：`adb shell input` 在中文输入法下丢字符，无法稳定填表
- Room 的 Cursor 映射、Converter、Flow 通知、`withTransaction` 边界：无自动化验证
- 表单由 `remember` 持有，旋转会丢失输入
- 统计页、我的页仍为占位
- `check-governance` 第 15/21 项不比对 `OK (N tests)` 的计数，`TASK-012` 出现过「Latest Run 57 / 末行 58」仍过检的盲区

## Next Safe Action
1. **TASK-014（P1）**：把 `validateName`/`normalizeTarget` 上移到 `CreateHabitUseCase`，并在 `HabitRepository` 接口写明契约
2. **TASK-015（P2）**：给 `check-governance` 增加「`OK (N tests)` 计数一致」与中文占位符检测
3. **TASK-016（P2）**：界面细节收尾（FAB 魔数推导、`KeyboardType.Decimal`、加载态注释口径）
4. 之后：图标/颜色选择器、习惯编辑与删除、提醒调度
