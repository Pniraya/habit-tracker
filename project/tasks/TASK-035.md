# TASK-035: 日切点 04:00 与设计偏差登记

- 状态：`IMPLEMENTED`（`ADR-015`：实现与真机自测完成，未经独立评审）
- 阶段：`Gate 3`
- 优先级：`P1`
- 依赖：`TASK-026`
- 创建日期：`2026-11-06`
- 完成日期：`2026-11-06`

## 1. Objective
把用户拍板的「日切点为凌晨 04:00」落成唯一日期来源，并按外部评审 Q9 建立设计偏差登记（ADR + 追踪表），同时移除统计页不可用的周/年死控件。

## 2. Context
外部评审 `ADVICE-RESPONSE-2026-11-06.md` 把「日切点 0 点还是 4 点」列为必须用户拍板的问题；用户 2026-11-06 明确「日切点为凌晨 4 点」。此前各 Presenter 默认 `LocalDate.now()`、前台跨日定时器指向 0 点，凌晨打卡会记错日期并打断连续。

## 3. Scope
- 新增 `domain/calculation/DayCutoff`：逻辑日、epochMillis 版本、到下一个 04:00 的毫秒数（Zone 感知）。
- 今日 / 新建 / 详情 / 统计四个 Presenter 的 `today` 默认值改走 `DayCutoff`；`AppRoot` 的跨日判断与定时器改为 04:00。
- `TodayPresenter.millisUntilNextMidnight` → `millisUntilNextDay`（04:00 口径）。
- `ADR-016`（日切）、`ADR-017`（设计偏差 + 归档语义）、`project/DESIGN-DELTA.md`。
- 统计页删除 `PeriodSwitch`（周/月/年），改为静态「本月」标签。

## 4. Out of Scope
- 提醒（`F-012/013`）实现：另行排期。
- 可配置日切点设置项。
- 时区迁移既有日志。
- `REQUIREMENTS.md` 原文改写（ADR-017 只登记偏差与状态标注）。

## 5. Expected Files
- 新增：`domain/calculation/DayCutoff.kt`、`test/.../DayCutoffTest.kt`、`project/decisions/ADR-016-day-cutoff-four-am.md`、`ADR-017-v1-design-deltas.md`、`project/DESIGN-DELTA.md`、本文件
- 修改：四个 Presenter、`AppRoot.kt`、`StatisticsScreen.kt`、`TodayPresenterTest.kt`、`PROJECT_STATUS.md`、`README.md`、`TEST_REPORT.md`、`FEATURE_MATRIX.md`、`LAST_HANDOFF.md`

## 6. Acceptance Criteria
- [x] 03:59:59 属于前一天，04:00:00 属于新一天（单测）
- [x] 「今天」只有一个来源；业务代码不再用 `LocalDate.now()` 作日期
- [x] 前台跨日刷新在 04:00 后触发，而非 0 点
- [x] 统计页没有不可点击的周/年控件
- [x] ADR-016/017 与 `DESIGN-DELTA.md` 已提交，偏差有唯一登记处
- [x] 单测 159 项通过（154 + 5 `DayCutoffTest`，定时器用例改写）
- [x] 真机安装后日期、打卡、统计不回归

## 7. Tests
- `tools/run-unit-tests.ps1`：**159 项通过**
- `DayCutoffTest` 5 项：04:00 边界、毫秒助手、epochMillis 时区、闰日、同日 01:00 的剩余时间
- `TodayPresenterTest.theDayTimerAimsAtTheNextFourAmCutoff`：23:59:59 → 4h1s，00:00 → 4h，闰日 16h，年末 5h
- 真机：安装后今日页 `11月6日 · 周五`、3 个习惯；统计页只显示「本月」，UI 树无「周/年」节点

## 8. Risks and Decisions
- **04:00 跨日无法在真机自动验证**：需要改设备时钟（adb shell date 需 root）或等到凌晨。当前只由 5 项纯 JVM 测试覆盖，真机验证限于「日间行为不回归」。已在 `TEST_REPORT.md` 的 Known Gaps 记录。
- **归档日期也走逻辑日**：凌晨归档归到前一天，与打卡一致；`archived_on` 语义不变（最近一次归档）。
- **周起始固定周一**：今日/统计/详情的周视窗都用周一，外部评审 R2 的「三处各算各的」风险已有统一实现，未新增设置项。
- **统计页死控件直接删除**：外部评审 Q10 认为死控件比没有控件更伤体验；周/年切换延后到 V1 之后。

## 9. Implementation Log
- 2026-11-06：先加 `DayCutoff` + 5 项测试，再改四个 Presenter 默认值与 `AppRoot` 定时器；`millisUntilNextMidnight` 改名并改口径，原 4 条断言全部改写。
- 2026-11-06：编译通过，单测 159 项一次通过。
- 2026-11-06：删除统计页 `PeriodSwitch`，改为静态「本月」；真机复装确认今日页日期与统计页标签。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `gradlew.bat compileDebugKotlin` | **BUILD SUCCESSFUL** | 4–6s |
| `tools/run-unit-tests.ps1` | **OK (159 tests)** | 154 → 159：`DayCutoffTest` 5；定时器用例改写 |
| 真机（iQOO Neo7 SE）：安装 + 今日页 + 统计页 | **PASS** | 今日页 `11月6日 · 周五`、3 个习惯不变；统计页 UI 树只有「统计/本月/本周趋势」，无「周/年」 |
| `tools/verify-all.ps1` | **VERIFY ALL PASS** | 治理 22/22、单测 159、DAO 38、构建、`git diff --check` |

## 11. Completion Review
- [x] 验收标准满足
- [x] 测试实际运行
- [ ] 独立 Reviewer `APPROVE`（`ADR-015` 批量评审）

## 12. Handoff
- 下一条：`R1` 窄口径评审（现在有 8 个 `IMPLEMENTED`），或 `N5` 提醒数据模型 ADR。
- 04:00 真实跨日需要在凌晨手动确认一次；这是目前唯一无法自动化的日切证据。

## 13. Independent Review
- Reviewer record：待创建（批量评审）
- 结论：`PENDING`（`IMPLEMENTED`）

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-035
- 状态：`IMPLEMENTED`
- 阶段：Gate 3
- 验收：通过 7 项 / 未通过 0 项
- 最近测试记录：2026-11-06 / **OK (159 tests)**
<!-- close-task:end -->
