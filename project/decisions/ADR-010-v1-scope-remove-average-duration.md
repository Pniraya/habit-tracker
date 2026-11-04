# ADR-010: V1 移除“平均用时”，并确定今日完成数的分母

- 状态：`PROPOSED`（等待批准；批准后改 `ACCEPTED`）
- 日期：`2026-11-05`
- 决策人：用户（待批准）
- 相关任务：`TASK-001`
- 关闭问题：`BASELINE_REVIEW` A3、B5；`REQUIREMENTS.md` §10 待确认第 1、5 项

## Context
概念稿的习惯详情页展示了“每次平均 22 分钟”，`REQUIREMENTS.md` 也把它写进详情页，但基线里没有任何采集流程（`habit_logs.duration_seconds` 只是可选列，没有开始/结束计时或手动录入交互）。在没有数据来源的情况下实现该指标，只能造假数据。另一个小口径问题是今日页“3/5”的分母：应执行的习惯数，还是全部未归档习惯数。

## Decision
1. V1 **不实现**“平均用时”：习惯详情页不显示该指标，不做计时交互、不做手动录入、不做统计。
2. `habit_logs.duration_seconds` 列**保留在 schema v1 中**（避免以后加列要写 Migration），但 V1 不写入、不读取、不展示；在 `DATABASE.md` 中标注“V1 未启用”。
3. 今日完成数分母 = **当日为计划执行日且未归档**的习惯数；分子 = 其中已完成的习惯数。非计划日习惯不进入分母，归档习惯不进入分母。
4. `FEATURE_MATRIX` 中 F-016“平均用时”状态改为 `OUT_OF_SCOPE`，并在备注注明依据本 ADR。

## Alternatives Considered
- 手动录入用时：多一个输入字段和校验，但用户几乎不会填，数据质量低。
- 开始/结束计时：需要前台计时体验与后台存续策略，与 V1 单页轻量定位冲突，且明显扩大范围。
- 保留指标但显示“—”：界面出现永远为空的字段，属于产品噪音。

## Consequences
- 正面：V1 不出现无法验证的数字；详情页范围收窄。
- 成本：概念稿里该字段不落地，需要在 README 或验收记录中说明差异来源。
- 影响文件：`REQUIREMENTS.md` §4.3、§10 第 1 与第 5 项；`FEATURE_MATRIX.md` F-016；`DATABASE.md` §4.3 字段说明加一句“V1 未启用”。

## Implementation and Verification
- 批准后改动：`REQUIREMENTS.md` §4.3 删除“平均用时”、§10 关闭第 1 与第 5 项；`DATABASE.md` §4.3 标注未启用；`FEATURE_MATRIX.md` F-016 改 `OUT_OF_SCOPE`。
- 必须增加的测试：详情页不存在平均用时字段；今日分母排除非计划日与归档习惯。

## Supersedes
无。
