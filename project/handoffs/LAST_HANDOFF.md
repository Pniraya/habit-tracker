# LAST_HANDOFF.md

> 每次 AI 会话结束前更新。记录事实，不写愿望或推测。

## Snapshot
- 日期时间（含时区）：`2026-11-05`
- 当前阶段：`Gate 3`（Today 与新建习惯两页接真实数据，主路径已在真机走通）
- 当前任务：`TASK-020`（`NOT_STARTED`，离线守卫与检查器余留加固）；`TASK-012`~`TASK-019` 已 `DONE` 合入 `main`；`TASK-003`/`TASK-004`/`TASK-005` 为 `PARTIAL`，已在 `PROJECT_STATUS.md` 的「在册例外」登记
- 当前 Git 分支：`chore/governance-probes`（本地，无远程）；`main` @ `617482c`；已合并分支 `fix/error-text`、`docs/feature-matrix` 保留未删
- 最近 commit：以 `git log -1 --oneline` 为准（本文件提交时即最新）；基线检查点 `d0f470a`
- **会话状态：进行中**（`TASK-019` 已实现，评审第 1 轮 `REJECT` 的 5 项文档不一致已修，等待第 2 轮）

## Completed This Session
- `TASK-007`~`TASK-018` 全部 `DONE` 合入 `main`：治理加固、`verify-all` 单一入口、DAO SQL 验证、Today 页状态机、新建习惯页、今日页新建入口、领域规则上移到 UseCase、治理计数检查、界面细节、错误文案中文化与小数显示
- `TASK-018` 三轮评审：第 1 轮 `REJECT`（对话框仍把 `0.5` 预填成 `0`、`onFailure` 调用点无测试、逗号把千分位变小数）、第 2 轮 `REJECT`（任务记录计数自相矛盾、预填无回归测试）、第 3 轮 `APPROVE`（`b5bd10f`）
- 真实缺陷清单（全部由评审或真机发现，无一是自动化测试自发现）：单位状态泄漏、Today 非空无新建入口、英文领域错误直出、`toLong()` 截断小数、对话框预填覆盖数据、逗号千分位误判
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
| `pwsh -NoProfile -File tools/run-unit-tests.ps1` | **OK (71 tests)** | `TASK-018` 新增 8 项：小数显示、整数去 `.0`、领域消息映射、未知错误回退、`onFailure` 调用点、逗号小数、千分位不被改写、对话框预填 |
| 真机（`iQOO Neo7 SE`） | **通过** | 列表 `0.5 / 2000 ml`；对话框预填 `0.5`；缺单位提示 `数值习惯需要填写单位` |
| 变异：`showsCreateAction` 去掉 `!state.isLoading &&` | **FAILED（1 项）** | 加载态断言变红；还原后全绿 |
| `gradlew.bat assembleDebug` | **SUCCESSFUL**（43s） | 缺 `PaddingValues`/`KeyboardOptions` 导入，已补 |
| 真机走查 | **通过** | 建 1 项 → `+` 出现 → 进新建页 → 建第 2 项 → 列表 2 行 |
| `pwsh -NoProfile -File tools/verify-all.ps1` | 见任务 §10 | 合入前在分支上运行 |

## Not Verified / Known Gaps
- **UI 无自动化测试**：Compose 渲染与交互仅由真机手动走查覆盖（截图在 `.gradle/shots/`，不入库）
- **数值输入对话框未在真机走通**：`adb shell input` 在中文输入法下丢字符，无法稳定填表
- Room 的 Cursor 映射、Converter、Flow 通知、`withTransaction` 边界：无自动化验证
- 表单由 `remember` 持有，旋转会丢失输入
- 统计页、我的页仍为占位；`POST_NOTIFICATIONS` 与 `RECEIVE_BOOT_COMPLETED` 已在 manifest 声明但无对应代码，属遗留声明，待清理
- `TASK-015` 已让 `check-governance` 比对 `OK (N tests)` 计数与中文占位符；但评审用 P5–P8 探针证明仍有边界缺陷（结果列无判定词也能过、摘要缺计数即跳过、计数扫全文被污染），已立 `TASK-017`

## Next Safe Action
1. **TASK-019（P1）**：校正 `FEATURE_MATRIX.md`——已完成实现，待第 2 轮评审（第 1 轮 `REJECT` 的 5 项文档打架已修）
2. **TASK-017（P2）**：治理检查加固——P5–P8 四个洞，加上本轮新发现的 P9（`TBD|NOT_RUN` 占位判断没尊重 `NOT_STARTED`，与代码注释矛盾）
3. **遗留重构（未立任务）**：`CreateHabitPresenter` 构造参数为函数类型以便注入失败实现；评审建议改为「保留 `CreateHabitUseCase` 形参 + `FakeHabitRepository.createFailure`」，属独立重构
4. 之后的功能顺序：F-009 连续天数 → F-010/011 统计页 → F-012/013 提醒 → F-014 备份恢复 → 图标/颜色选择器 → 习惯编辑与删除
