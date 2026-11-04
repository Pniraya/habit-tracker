# AGENTS.md

本文件是 Habit Tracker 仓库级 AI 协作规则。优先级低于用户明确指令和平台安全规则，高于临时聊天中的未经确认建议。

## 1. 项目身份与冻结基线

- 项目：Habit Tracker
- 平台：Android；V1 目标 Android 15（API 35）
- 语言：Kotlin
- UI：Jetpack Compose + Material 3
- 持久化：Room / SQLite；轻量设置使用 DataStore
- 架构：单 Gradle module（`:app`）的轻量分层架构
- 数据：本地优先；无账号、无服务器、无云同步
- 提醒：Normal 普通精度；允许系统延迟；不申请精确闹钟权限
- V1 不做：桌面小组件、深色模式、常驻前台服务、后端、微服务

技术细节以 `ARCHITECTURE.md`、`DATABASE.md`、`DEVELOPMENT.md` 为准。产品行为以 `project/REQUIREMENTS.md` 为准。若文件之间冲突，不得自行猜测或静默择一。

## 2. 必读文件

每次新会话、每项新任务开始前，按顺序检查：

1. `AGENTS.md`
2. `README.md`（若存在）
3. `project/PROJECT_STATUS.md`
4. `project/FEATURE_MATRIX.md`
5. `project/ROADMAP.md`
6. `project/REQUIREMENTS.md`
7. `ARCHITECTURE.md`
8. `DATABASE.md`
9. `DEVELOPMENT.md`
10. `project/handoffs/LAST_HANDOFF.md`
11. 当前任务文件 `project/tasks/TASK-xxx.md`

同时检查 `git status --short`、当前分支、最近提交，以及实际源码和构建配置。文档不能代替对代码现状的检查。

## 3. 信息可信度与冲突处理

按以下顺序判断项目事实：

1. 用户明确批准的最新决定；
2. 当前仓库中的已批准基线文档及 ADR；
3. 当前源码、构建配置、数据库 schema 和测试实际结果；
4. 任务文件与项目状态记录；
5. 聊天记录、模型记忆和推测。

注意：源码是“当前实现事实”，不自动代表“正确需求”。若源码与批准需求不一致，先报告差异，不要把现状当成需求。

发现以下情况时停止相关实现并报告：
- 产品需求与架构/数据库文档冲突；
- 数据库字段、历史数据语义或迁移策略不明确；
- 任务范围需要修改冻结基线；
- 工作区有用户未提交的改动且可能被覆盖；
- 测试失败原因不清，继续修改可能扩大影响。

提出最小修订方案，等待用户批准后再改动基线文件。获批的重大技术决策必须新增 ADR。

## 4. 任务执行规则

每次只执行一个明确的 `TASK-xxx`。开始前：

1. 确认任务状态、依赖任务和验收标准；
2. 检查 Git 工作区；不得覆盖或清理用户改动；
3. 阅读相关实现和测试，不假设文件不存在；
4. 给出简短实施计划、预计修改文件、风险和验证命令；
5. 若任务描述不足以安全实施，先提出一个最关键的澄清问题。

实现时：
- 严格限制在任务 Scope；额外重构另开任务。
- 不擅自增加依赖、Gradle module、网络权限、SDK、后台服务或产品功能。
- 不把业务规则散落在 Composable、Receiver 或 DAO 中。
- 不使用破坏性数据库迁移作为生产方案。
- 不在日志、报告或示例中暴露用户隐私数据。
- 不伪造代码、命令执行结果、测试结果或构建状态。

## 5. 验证与完成定义

任务完成前：
1. 运行任务文件要求的测试；
2. 运行适用的 Gradle 构建/测试命令；
3. 检查 `git diff --check` 和完整 diff；
4. 检查是否引入未批准依赖、权限、功能或无关文件；
5. 将实际命令、退出状态和失败摘要写入任务记录/测试报告。

只有验收标准全部满足，且验证结果有证据时，才能标记 `DONE`。如果环境不支持执行测试，标记 `BLOCKED` 或 `PARTIAL`，明确未验证项，不得写“测试通过”。

建议命令（按项目实际 Gradle Wrapper 和任务调整）：
- Windows：`gradlew.bat assembleDebug`、`gradlew.bat test`
- macOS/Linux：`./gradlew assembleDebug`、`./gradlew test`
- 检查：`git diff --check`、`git status --short`

## 6. 状态文件维护

任务结束时，根据真实结果更新：
- 当前 `TASK-xxx.md`：状态、变更文件、验收结果、测试结果；
- `project/PROJECT_STATUS.md`：当前阶段、当前任务、阻塞项、最后验证；
- `project/FEATURE_MATRIX.md`：仅更新有实现/测试证据的状态；
- `project/handoffs/LAST_HANDOFF.md`：下一会话可直接恢复的交接信息；
- `project/reports/TEST_REPORT.md`：重要测试记录；
- `project/reports/RELEASE_REPORT.md`：发布证据与已知限制（仅发布时填写）。

不要为了让进度看起来完整而批量勾选功能。规划完成、代码完成、测试通过、真机验证是不同状态。

## 7. Git 与提交规则

- `main` 保持可构建；每个独立任务优先使用 `feat/<short-name>` 或 `fix/<short-name>` 分支。
- 开始任务前检查工作区；不擅自 stash、reset、checkout 覆盖用户改动。
- 通过验收后建议提交一个聚焦的 commit；提交前展示摘要。
- 未经用户授权，不执行破坏性 Git 操作、强制推送、删除分支或重写历史。
- 数据库 schema、迁移、备份格式等高风险改动前创建可回退的 Git 检查点。
- 不自动推送远程仓库。

## 8. 会话中断与恢复

新会话不能只依赖聊天摘要。必须读取 handoff 和任务文件，再用 Git 与源码核对。

如果上次中断：
1. 识别最后一个完成的验证点；
2. 检查是否存在未提交改动；
3. 将 handoff 中的说法与源码、Git 状态核对；
4. 继续未完成任务，不重复覆盖已完成工作；
5. 对无法确认的内容标记 `UNKNOWN`，不要猜测。

结束会话前必须写交接记录。即使 Token 即将耗尽，也优先保存：当前任务、已改文件、未完成步骤、测试实际结果、Git 状态和下一条安全操作。

## 9. 输出格式

每次任务结束，用简短结构报告：
- 状态：DONE / PARTIAL / BLOCKED
- 实现内容
- 修改文件
- 验收标准逐项结果
- 实际运行的测试与结果
- 未解决问题/风险
- Git 分支与工作区状态
- 下一项建议任务

不要把建议、推测或尚未执行的操作写成已完成事实。

## 10. AI 会话生命周期

每次会话按固定顺序执行，上一阶段没有产出就不进入下一阶段：

| # | 阶段 | 动作 | 必须留下的证据 |
|---|---|---|---|
| 1 | Bootstrap | 按 §2 顺序读取规则、状态、路线、需求、设计文档、交接与当前 TASK 文件 | 已读清单与核对到的 Git 状态 |
| 2 | Inspect | 核对源码、构建配置、依赖、数据库 schema、测试实际结果；确认文档与仓库无冲突 | 实际命令与输出摘要 |
| 3 | Plan | 明确本次范围、预计文件清单、验收条件、风险、验证命令 | 简短计划（可写在 TASK 文件 §9） |
| 4 | Implement | 只完成当前 TASK 的 Scope | 变更文件清单 |
| 5 | Verify | 构建、测试、静态检查、完整 diff 审查 | 命令、退出状态、失败摘要 |
| 6 | Persist | 更新 TASK 记录、`PROJECT_STATUS.md`、`FEATURE_MATRIX.md`（仅限有证据项）、`TEST_REPORT.md` | 更新后的文件 |
| 7 | Handoff | 写 `LAST_HANDOFF.md`；提交聚焦 commit，或明确记录未提交原因 | commit 摘要与下一安全操作 |

规则：
- 未完成阶段 5 不得进入阶段 6；没有验证结果时只能写 `NOT_RUN`/`PARTIAL`/`BLOCKED`。
- 任一阶段发现 §3 所列冲突，立即停止相关实现，输出最小修订方案等待批准。

## 11. 文件分级与保护清单

AI 对文件的修改权限按三级划分（依据 ADR-004）：

| 级别 | 文件 | AI 权限 |
| --- | --- | --- |
| 产品与架构基线 | `AGENTS.md`、`ARCHITECTURE.md`、`DATABASE.md`、`DEVELOPMENT.md`、`project/REQUIREMENTS.md`、`README_GOVERNANCE.md` | 只能提出修改建议；必须经用户批准并新增 ADR 后才可改动 |
| 任务状态与进度 | `project/PROJECT_STATUS.md`、`project/FEATURE_MATRIX.md`、`project/ROADMAP.md`、`project/tasks/**`、`project/handoffs/**`、`project/reports/**` | 可以更新，但必须有实际依据；不得为让进度好看而勾选 |
| 源代码与测试 | `app/**`、`gradle/**`、`*.gradle.kts` | 在当前 TASK 范围内可以修改 |

- 只读资产：`orgins/**`（视觉概念稿）、`project/reports/BASELINE_REVIEW.md`。不得将其作为生产代码或验收依据。
- 修改受保护文件的最小流程：指出冲突与理由 → 提交最小修订方案 → 用户批准 → 新增 ADR → 修改文档 → 同步状态文件。
- 即使用户在会话中口头指示修改，也必须留下 1 份 ADR 记录依据与该指示的来源。
- 数据库 schema、备份格式、迁移策略属于高风险改动，改动前必须存在可回退的 Git 检查点。

## 12. 角色分工与验证顺序

- `Planner`：理解需求、拆任务、定文件范围与验收条件。
- `Developer`：实现代码与测试，更新文档。
- `Reviewer`：独立核对 Diff、架构一致性、测试证据与回归风险。建议在 Developer 完成后用新会话执行，避免自评过宽。

验证顺序不可颠倒：编译器 → 单元/集成测试 → 静态检查 → Diff 审查 → 独立评审。任何 AI 的“看起来没问题”都不能替代测试结果；未经验证的代码不得合并到 `main`。

## 13. Git 事件与操作

| 事件 | 操作 |
| --- | --- |
| 完成一个小任务 | 一个聚焦 commit |
| 功能开发完成并通过验收 | 合并到 `main` |
| 高风险数据库/迁移/备份改动前 | 创建可回退检查点（commit 或 tag） |
| AI 修改导致异常 | 先审查 diff 与测试结果，再回滚 |
| 发布 V1 | 打 tag，并填写 `RELEASE_REPORT.md` |

禁止项：未经授权 `push --force`、重写历史、删除分支、自动推送远程、`reset --hard` 丢弃用户改动。
