# TASK-018: 错误文案中文化与小数显示

- 状态：`PARTIAL`（实现与真机验证完成，等待独立评审）
- 阶段：`Gate 3`
- 优先级：`P1`
- 依赖：`TASK-012`（表单）、`TASK-014`（规则上移到 UseCase）、`TASK-016`（小数输入）
- 创建日期：`2026-11-05`
- 完成日期：`TBD`

## 1. Objective
修掉两个用户能直接看见的缺陷：中文界面弹出英文领域错误；数值习惯的小数被截断成整数显示。

## 2. Context
`TASK-016` 真机补验时先后暴露：
1. 数值习惯不填单位，界面显示 `NUMERIC habit needs a unit` —— `HabitRules.normalizeTarget` 的英文 `require` 原文被 `onFailure { error = failure.message }` 直接贴到屏幕上。
2. 输入 `0.5` 后数据库写入正确（`actual_value = 0.5`），但列表显示 `0 / 2000 ml` —— `subtitle()` 里 `habit.actualValue.toLong()` 把小数截断了。

## 3. Scope
- `TodayPresenter`：新增 `progressText()` / `formatQuantity()`，用 `BigDecimal.stripTrailingZeros()` 保留小数、去掉整数的 `.0`；`TodayScreen.subtitle()` 改为调用它。
- `CreateHabitPresenter`：`validate()` 增加 `NO_UNIT`；`onFailure` 不再直出 `failure.message`，改走顶层 `internal fun errorMessageFor()` 映射，未知错误给 `SAVE_FAILED`。
- 数值对话框：预填值与「今天的目标」都改用 `formatQuantity()`——原来 `toLong()` 把已记录的 `0.5` 显示成 `0`，用户一点「记录」就把 0.5 覆盖成 0。
- 输入归一化抽成 `normaliseQuantityInput()`：`,` 转 `.`，但 `^\d{1,3}(,\d{3})+$` 千分位原样保留（转成小数会差 1000 倍）。

## 4. Out of Scope
- 统计页、我的页、图标选择器、提醒。
- 领域层消息改中文（`HabitRules` 的 `require` 文案保持英文，作为开发可读的契约；用户侧由映射兜底）。

## 5. Expected Files
- 修改：`feature/today/{TodayPresenter,TodayScreen}.kt`、`feature/create/CreateHabitPresenter.kt`、`TodayPresenterTest.kt`、`CreateHabitPresenterTest.kt`
- 新增：`project/tasks/TASK-018.md`；状态文档

## 6. Acceptance Criteria
- [x] 数值习惯缺单位时提示为中文，且不触达仓库
- [x] 任何领域层英文消息都不会出现在界面上（有映射函数与测试）
- [x] `0.5` 显示为 `0.5`，整数不带 `.0`
- [x] 逗号小数 `0,5` 被归一化为 `0.5`
- [x] 已记录的 `0.5` 打开对话框时预填为 `0.5`，不会把存储值覆盖成 `0`
- [x] 千分位输入（`1,234`）不被静默改写成小数
- [x] `onFailure` 的翻译调用点有测试覆盖（变异 C 见 §10）
- [x] 真机确认：列表 `0.5 / 2000 ml`、对话框预填 `0.5`、缺单位提示 `数值习惯需要填写单位`

## 7. Tests
- `pwsh -NoProfile -File tools/run-unit-tests.ps1`（新增 8 项，共 71 项）
- 变异 A：`formatQuantity` 改回 `toLong().toString()`，必须变红
- 变异 B：`errorMessageFor` 未知分支改为返回 `failure.message`，必须变红
- 真机：输入 `0.5` 后查看列表；缺单位保存查看提示

## 8. Risks and Decisions
- **映射靠子串匹配**（`"needs a unit"` 等），领域层改文案会静默退化成 `SAVE_FAILED`。这是防「英文直出」的兜底，不是强类型契约；`SAVE_FAILED` 本身仍然是中文可读的，退化方向安全。
- **领域消息保持英文**：`HabitRules` 的 `require` 文案是给开发者看的契约，且已被 `TASK-014` 的单测断言；把用户文案下沉到领域层会让领域依赖展示需求。
- `progressText` 放在 `TodayPresenter.companion`：它是纯函数、无状态，放这里能被 JVM 测试直接调用，也避免再出现「格式化写在 Composable 里、无法测试」。
- 逗号归一化区分两种含义：`0,5` 视为小数分隔符转成 `0.5`；`1,234` 命中千分位模式则原样保留，导致解析失败、按钮禁用——**拒绝**比**按另一种含义接受**安全。多小数点同样解析失败。代价是千分位写法不能作为合法输入（本应用不需要）。

## 9. Implementation Log
- 2026-11-05：`progressText`/`formatQuantity` 与 `errorMessageFor` 落地，`validate()` 加 `NO_UNIT`，对话框加逗号归一化。
- 2026-11-05（评审第 1 轮 `REJECT` 后）：对话框预填/目标显示改为 `formatQuantity()`；`CreateHabitPresenter` 构造参数改成函数类型以注入失败实现，新增 `aDomainRejectionIsTranslatedAtTheCallSite` 覆盖 `onFailure`；`normaliseQuantityInput()` 区分小数逗号与千分位。
- 2026-11-05：原测试 `numericHabitWithoutUnitIsRefused` 断言的是英文子串 `needs a unit`，改动后必须同步改为断言中文常量并补 `createCalls == 0`——否则它测的是旧行为。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/run-unit-tests.ps1` | **OK (71 tests)** | 新增 8 项：小数显示、整数去 `.0`、映射 5 条、未知错误回退、`onFailure` 调用点、逗号小数、千分位不被改写、对话框预填 |
| 变异 A：`formatQuantity` → `toLong().toString()` | **FAILED（2 项）**（预期） | 小数与整数格式断言变红 |
| 变异 B：`errorMessageFor` 未知分支返回原文 | **FAILED（2 项）**（预期） | 映射与中文回退断言变红 |
| 变异 C：`onFailure` 改写回 `failure.message`（评审第 1 轮提出） | **FAILED（1 项）**（修复后） | `aDomainRejectionIsTranslatedAtTheCallSite` 变红；修复前全绿 |
| 变异 D：`prefillText` 改用 `toLong()`（评审第 2 轮提出） | **FAILED（1 项）**（修复后） | `theNumericDialogPrefillsTheRecordedDecimal` 变红；修复前全绿 |
| 变异 E：去掉千分位保护（评审第 2 轮） | **FAILED（1 项）** | `aThousandsGroupIsNotRewrittenIntoADecimal` 变红 |
| 真机：已记录 `0.5` 的习惯打开对话框 | **通过** | 预填 `0.5`（修复前为 `0`，点「记录」即覆盖存储值） |
| 真机：列表显示已记录的小数 | **通过** | `0.5 / 2000 ml`（修复前为 `0 / 2000 ml`） |
| 真机：数值习惯缺单位保存 | **通过** | 提示 `数值习惯需要填写单位`（修复前为 `NUMERIC habit needs a unit`） |
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS**（exit 0） | 治理 21/21、单测 71 项、DAO SQL 21 项、构建通过 |

## 11. Completion Review
- [x] 验收标准全部满足（9/9）
- [x] 相关测试实际运行（71 项 + 4 组变异 + 3 项真机）
- [x] `git diff --check` 通过
- [x] Diff 已审查：无新依赖、无 schema 改动
- [x] 文档/状态已更新
- [ ] 独立 Reviewer `APPROVE`

## 12. Handoff
- 下一步：评审 `APPROVE` 后合入 `main`；之后 `TASK-017`（治理检查加固 P5–P8）。
- 遗留：映射为子串匹配，领域文案变更会退化为通用提示；千分位写法被拒绝而非解析（本应用不需要该量级）。

## 13. Independent Review
- Reviewer record：`project/reviews/REVIEW-TASK-018.md`（待创建）
- 结论：`PENDING`

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-018
- 状态：`PARTIAL`
- 阶段：Gate 3
- 验收：通过 8 项 / 未通过 0 项
- 最近测试记录：2026-11-05 / **OK (71 tests)**
<!-- close-task:end -->
