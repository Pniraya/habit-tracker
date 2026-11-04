# PROJECT_STATUS.md

> 这是项目状态快照，不是计划书。AI 每次任务完成后必须按实际仓库状态更新。

## 当前状态

- 项目阶段：治理层初始化（尚未确认 Android Studio 工程已创建）
- 当前任务：`TASK-000` — 建立 AI 项目治理文件并核对既有基线
- 总体状态：`IN_PROGRESS`
- 代码实现状态：`NOT_STARTED`（除非实际仓库检查证明已有源码）
- 最近验证构建：`NOT_RUN`
- 最近验证测试：`NOT_RUN`

## 已有设计资产

- [x] `ARCHITECTURE.md`：初始架构基线
- [x] `DATABASE.md`：初始数据库设计
- [x] `DEVELOPMENT.md`：开发规范与阶段计划
- [x] `project/REQUIREMENTS.md`：需求基线（本治理包新增）
- [ ] 现有三份设计文档完成一致性修订
- [ ] Android Studio 工程创建并通过首次构建

## 当前待办

1. 将本治理包文件合并进 Android 项目仓库。
2. 阅读 `project/reports/BASELINE_REVIEW.md`，确认需要修订的设计问题。
3. 用户批准后修订冻结基线并记录 ADR。
4. 创建 Android 单模块工程。
5. 执行首次 Gradle Sync 与 Debug 构建。
6. 将 `TASK-001` 设为当前任务并开始工程初始化。

## 阻塞项与风险

- 现有数据库文档中 `archived_at` 使用时间戳，而统计规则使用归档日期；跨时区/跨日语义需要明确。
- “平均用时”有 UI 需求，但目前缺少可靠的数据采集流程定义。
- `HabitLog.status` 与 `actual_value`、`target_snapshot`、`completed_at` 存在冗余一致性要求，需定义唯一计算来源及写入约束。
- 计划区间不重叠无法仅靠普通 SQLite CHECK 约束保证，需在事务层强制校验并测试。
- 目前尚未从真实 Android 工程验证 SDK、Gradle、Kotlin、Compose 与 Room 版本组合。

## Git 状态

- 仓库是否已初始化：`UNKNOWN`
- 当前分支：`UNKNOWN`
- 工作区状态：`UNKNOWN`
- 最近 commit：`UNKNOWN`

首次接入项目时必须以实际 `git status` 和 `git log` 结果替换以上 UNKNOWN。
