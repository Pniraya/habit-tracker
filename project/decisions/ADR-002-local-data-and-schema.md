# ADR-002: 本地 Room 数据作为业务数据唯一可信来源

- 状态：`ACCEPTED`
- 日期：`2026-11-04`
- 决策人：用户已确认的项目产品边界（如后续发现并非明确批准，应改为 PROPOSED）
- 相关任务：治理初始化

## Context
V1 不提供账号、云服务或同步；习惯、计划、日志和提醒配置需要在应用重启后保留。

## Decision
Room/SQLite 保存习惯业务数据；DataStore 仅保存轻量应用设置。计划版本化，日志按习惯与自然日唯一汇总。JSON 备份格式版本独立于 Room schema version。

## Alternatives Considered
- 云端数据库：不符合本地优先和无账号需求。
- 将所有数据存 DataStore：不适合关系查询、唯一约束和事务。

## Consequences
- 应用离线可用。
- 需要认真维护 Room Migration、schema export 和备份导入校验。
- 数据库和备份格式必须分别演进。
