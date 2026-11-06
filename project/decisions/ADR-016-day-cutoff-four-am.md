# ADR-016: 日切点为凌晨 04:00

- 状态：`ACCEPTED`（2026-11-06 用户明确批准「日切点为凌晨4点」）
- 日期：`2026-11-06`
- 决策人：用户
- 相关任务：`TASK-026` 之后的日切改造（本 ADR 随代码同提交）
- 关闭问题：外部评审 `ADVICE-RESPONSE-2026-11-06.md` R2「日切点是 0 点还是凌晨 4 点」

## Context
习惯类 App 的用户常在 00:00 之后才打卡。若按自然日 0 点切换，「凌晨 1 点完成昨天」会被记成今天：连续天数被打断、统计多算一天、归档日期也可能差一天。外部评审把这条列为需要用户拍板的产品语义，用户于 2026-11-06 决定：**日切点为凌晨 04:00**。

## Decision
1. **逻辑日** `logicalDate = (本地时间 - 4 小时).toLocalDate()`。00:00–03:59:59 属于前一天；04:00 起进入新的一天。
2. 所有「今天」必须来自 `domain/calculation/DayCutoff`：今日页、新建页、详情页、统计页的 `today` 默认值，以及前台跨日刷新定时器。禁止再出现 `LocalDate.now()` 作为业务日期。
3. 数据库不变：`habit_logs.log_date`、`habit_schedules.effective_from/until`、`habits.created_on/archived_on` 存的都是**逻辑日**的 ISO 日期；0 点切日不是数据库概念，而是应用层的日期解释。
4. 前台刷新：`TodayPresenter.millisUntilNextDay` 指向下一个 04:00（原 `millisUntilNextMidnight`）；`AppRoot` 的跨日重建随之改到 04:00 后 1 秒。
5. 时区：逻辑日按设备当前时区计算；时区变化后既有 `log_date` 不迁移（与 `DATABASE.md` 的「日期是本地日历日」一致）。

## Alternatives Considered
- **00:00 切日**：实现最简单，但会惩罚熬夜用户，与「连续天数反映真实坚持」冲突。
- **可配置切日点（0/3/4 点）**：多一个设置项、多一倍边界测试，V1 无此需求。
- **按 24 小时滚动窗口**：与「今天」的日历语义冲突，统计/归档日期都会变得难解释。

## Consequences
- 正面：凌晨打卡不再打断连续；日期语义统一到一处，避免各页各算。
- 成本：04:00 前后的行为必须单测覆盖；前台跨日刷新定时器改为 4 小时相位；文档需要说明「今天」不是自然日。
- 影响文件：`DayCutoff.kt`、四个 Presenter 的 `today` 默认值、`AppRoot.kt`、`TodayPresenter.millisUntilNextDay`、`TodayPresenterTest`、新增 `DayCutoffTest`。

## Implementation and Verification
- 已实现：`DayCutoff.logicalDate/today/logicalDateOf/millisUntilNextCutoff`；四个 Presenter 默认值与 `AppRoot` 定时器接入。
- 测试：`DayCutoffTest` 5 项（03:59:59 / 04:00 边界、毫秒助手、epochMillis、闰日）；`TodayPresenterTest` 的定时器断言改为 04:00 口径；`tools/run-unit-tests.ps1` = 159 项通过。
- 真机：安装后今日页日期、打卡、详情/统计读数不回归（04:00 真实跨日行为无法在不改设备时钟的前提下自动化，记为已知缺口）。

## Supersedes
无。与 `ADR-005`（归档/创建使用本地日期）一致：那里说的「本地日期」现在明确为「逻辑日」。
