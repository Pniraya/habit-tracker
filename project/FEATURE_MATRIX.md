# FEATURE_MATRIX.md

> 状态只反映已验证的实现，不反映“已讨论”或“已规划”。

状态定义：
- `NOT_STARTED`：没有实现证据。
- `IN_PROGRESS`：实现正在进行。
- `IMPLEMENTED_UNVERIFIED`：代码已写，但相关测试/构建尚未通过或未执行。
- `VERIFIED`：自动化测试/构建通过。
- `DEVICE_VERIFIED`：目标 Android 设备上完成验证。
- `BLOCKED`：有明确阻塞项。
- `OUT_OF_SCOPE`：明确不属于 V1。

| ID | 功能 | 需求/设计 | 实现状态 | 自动化验证 | 真机验证 |
|---|---|---|---|---|---|
| F-001 | Android 工程骨架 | 已定义 | VERIFIED | VERIFIED | NOT_STARTED |
| F-002 | 今日页与导航 | 已定义 | IMPLEMENTED_UNVERIFIED | NOT_STARTED | NOT_STARTED |
| F-003 | 创建/编辑习惯 | 已定义 | NOT_STARTED | NOT_STARTED | NOT_STARTED |
| F-004 | 归档/恢复习惯 | 已定义 | NOT_STARTED | NOT_STARTED | NOT_STARTED |
| F-005 | DAILY/WEEKDAYS/CUSTOM 计划 | 已定义 | NOT_STARTED | NOT_STARTED | NOT_STARTED |
| F-006 | BOOLEAN 日记录 | 已定义 | NOT_STARTED | NOT_STARTED | NOT_STARTED |
| F-007 | NUMERIC 日累计记录 | 已定义 | NOT_STARTED | NOT_STARTED | NOT_STARTED |
| F-008 | 撤销/修改日记录 | 已定义 | NOT_STARTED | NOT_STARTED | NOT_STARTED |
| F-009 | 当前/最长连续天数 | 已定义 | NOT_STARTED | NOT_STARTED | NOT_STARTED |
| F-010 | 周/月/年统计 | 已定义 | NOT_STARTED | NOT_STARTED | NOT_STARTED |
| F-011 | 日历热力图/趋势 | 已定义 | NOT_STARTED | NOT_STARTED | NOT_STARTED |
| F-012 | 多条 Normal 提醒 | 已定义 | NOT_STARTED | NOT_STARTED | NOT_STARTED |
| F-013 | 重启/时区后提醒恢复 | 已定义 | NOT_STARTED | NOT_STARTED | NOT_STARTED |
| F-014 | JSON 备份/恢复 | 已定义 | NOT_STARTED | NOT_STARTED | NOT_STARTED |
| F-015 | 离线核心功能 | 已定义 | NOT_STARTED | NOT_STARTED | NOT_STARTED |
| F-016 | 平均用时 | V1 不做（ADR-010） | OUT_OF_SCOPE | — | — |
| F-017 | 桌面小组件 | V1 不做 | OUT_OF_SCOPE | — | — |
| F-018 | 深色模式 | V1 不做 | OUT_OF_SCOPE | — | — |
| F-019 | 账号/云同步/服务端 | V1 不做 | OUT_OF_SCOPE | — | — |

## 更新规则

- 仅 Developer 在实现后可提出状态更新；Reviewer 应核对证据。
- `IMPLEMENTED_UNVERIFIED` 不能写成 `VERIFIED`。
- `VERIFIED` 不等于 `DEVICE_VERIFIED`。
- 每次状态变化应在任务记录中引用相关测试、构建或设备验证证据。
