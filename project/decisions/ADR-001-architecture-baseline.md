# ADR-001: V1 采用单模块轻量分层架构

- 状态：`ACCEPTED`
- 日期：`2026-11-04`
- 决策人：用户已确认的项目技术方向（如后续发现并非明确批准，应改为 PROPOSED）
- 相关任务：治理初始化

## Context
V1 是单设备本地习惯追踪器，功能规模有限，不需要服务端或复杂模块拆分。

## Decision
V1 使用单 Gradle module `:app`，Kotlin、Jetpack Compose、Material 3、Room、DataStore、ViewModel、Coroutines/Flow，并按 Presentation、Domain、Data 和 Android System Integration 划分职责。

## Alternatives Considered
- 多 Gradle module：当前规模下增加构建与依赖管理成本。
- 引入后端：与本地离线、无账号、无云同步需求冲突。

## Consequences
- 结构简单，适合个人项目和逐步交付。
- 需要通过 package 边界和代码审查维持分层，不依赖 Gradle module 强制隔离。
- 只有真实构建复杂度出现时才重新评估模块拆分。
