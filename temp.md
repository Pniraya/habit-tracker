可以，而且这已经是 AI 软件工程中一个非常重要的研究方向。

你描述的问题实际上包含三个独立风险：

1. 上下文耗尽（Context Exhaustion）：对话越来越长，模型可用上下文有限，早期需求和决策可能被压缩或遗漏。
2. 状态漂移（State Drift）：AI 对项目当前状态的理解与实际代码逐渐不一致，可能重复实现、覆盖已有功能，甚至破坏原有架构。
3. 任务中断与恢复（Interruption & Recovery）：Token 额度耗尽、会话关闭、模型切换、工具报错，都可能让开发停在半成品状态。

我的判断是：不要试图让 AI 永远记住整个项目，而应该让 AI 每次开始工作时，都能从项目本身可靠地恢复状态。

这意味着需要把 AI 从一个依赖聊天记忆的编程助手，转变成一个遵循工程流程、读取项目状态、执行任务、验证结果、提交交接记录的开发代理。

这套思路可以做到相当稳定，但不能保证 AI 永远不犯错。真正的目标是：即使 AI 犯错或突然中断，也能发现问题、限制影响，并恢复到最后一个经过验证的状态。

## 一、截至 2026 年 11 月，研究已经发展到什么程度？

我查阅了 Anthropic 的长周期编程代理实践，以及 2026 年的上下文管理、项目状态维护研究。结论是：研究方向已经从单纯扩大上下文窗口，逐渐转向 Context Engineering（上下文工程）和 Harness Engineering（代理运行环境工程）。

这两个概念值得你了解。

### 1. Context Engineering：解决 AI 应该记住什么

它研究的不是简单地保存更多聊天记录，而是：

- 如何提取真正重要的项目事实。
- 如何区分长期有效的规则和临时任务状态。
- 如何在新会话中检索相关代码、决策和历史进展。
- 如何避免过期信息污染当前判断。

例如，AI 不需要每次重新阅读你所有的开发对话，而应该知道：

```
项目：Habit Tracker

当前技术栈：
Kotlin + Jetpack Compose + Room

已经完成：
- 工程初始化
- 数据库 Entity
- DAO

正在开发：
- 今日页面

尚未完成：
- 今日习惯列表
- 打卡交互
- 今日完成率

重要约束：
- 不允许引入云服务
- 不允许申请精确闹钟权限
- 不允许修改历史计划记录
```

这些信息比保留几百轮聊天记录有价值。

2026 年 10 月发表在 EMNLP 的研究 SWE-MeM，提出让编程代理根据任务进展和剩余上下文预算，主动决定何时、如何压缩记忆，而不是固定地定期总结。

ACL Anthology



另一项 2026 年 10 月的研究 StateHarness，则尝试维护持续变化的项目状态，包括当前任务、重要决策、代码之间的关系以及未完成事项。它在多个代码库级评估中改善了任务完成率和变更一致性，但也出现了回归增加的情况。

arXiv



### 2. Harness Engineering：解决 AI 如何可靠地工作

这是我认为对你最有价值的方向。

Harness 可以理解为：包围 AI 编程模型的一整套工作环境和管理机制。

它不只是提示词，还包括：

- 项目规则。
- 任务管理。
- 文件访问权限。
- Git 版本控制。
- 自动测试。
- 任务状态记录。
- 错误恢复。
- 工作过程审计。

2026 年 5 月的 AI Harness Engineering 研究将其拆分为任务规范、上下文选择、工具访问、项目记忆、任务状态、可观测性、故障归因、验证、权限等职责。

arXiv



这意味着：一个 AI 能否稳定开发软件，不仅取决于模型有多聪明，也取决于它工作的工程环境是否可靠。

## 二、已经有实际验证的工程方法

其中一个很有参考价值的案例来自 Anthropic。

他们在 2025 年 11 月公开了一套长周期编程代理实践，核心设计很值得借鉴。

Anthropic：长周期编程代理工作机制

1. 初始化代理（Initializer Agent）

   首次运行时建立项目基础环境、功能清单、初始化脚本和进度文件。

2. 增量开发代理（Coding Agent）

   每次会话只完成一个明确功能，不试图一次性开发整个软件。

3. 持久化进度

   每次完成工作，都将结果写入项目文件，并提交 Git。

4. 新会话恢复

   下一次代理读取项目进度和 Git 历史，继续未完成任务。

来源：Anthropic, Effective harnesses for long-running agents, 2025-11-26。

Anthropic



Anthropic 在 2026 年 3 月进一步提出了一个重要观点：

上下文压缩（Compaction）不一定等于上下文重置（Context Reset）。

压缩是在原有会话中缩短历史；重置则是创建一个干净的新会话，通过结构化交接文件恢复工作。

他们的实践表明，在长周期开发中，单纯压缩可能仍然无法消除上下文混乱，因此有时应该主动结束会话，让新的代理接手。

Anthropic



这恰好对应你担心的问题。

# 三、我建议为 Habit Tracker 建立一套项目管理系统

不要仅仅依赖目前的三份 Markdown 文档。

它们解决的是工程知识的记录问题，但还没有完整解决 AI 的任务管理、状态恢复和变更审查问题。

我建议把整个项目升级成：



推荐方案

Repository-Centered AI Development System（以代码仓库为中心的 AI 开发管理系统）



它有一个基本原则：

> 项目仓库是唯一可信的项目状态来源。聊天记录不是项目数据库，AI 的记忆也不是项目数据库。

## 1. 建立完整的项目文件体系

建议在现有三份文档基础上增加以下文件。

```
HabitTracker/
│
├── AGENTS.md
│
├── README.md
│
├── ARCHITECTURE.md
├── DATABASE.md
├── DEVELOPMENT.md
│
├── project/
│   ├── PROJECT_STATUS.md
│   ├── ROADMAP.md
│   ├── REQUIREMENTS.md
│   ├── FEATURE_MATRIX.md
│   │
│   ├── decisions/
│   │   ├── ADR-001-architecture.md
│   │   ├── ADR-002-database.md
│   │   └── ADR-003-reminder.md
│   │
│   ├── tasks/
│   │   ├── TASK-001.md
│   │   ├── TASK-002.md
│   │   └── TASK-003.md
│   │
│   ├── handoffs/
│   │   └── LAST_HANDOFF.md
│   │
│   └── reports/
│       ├── TEST_REPORT.md
│       └── RELEASE_REPORT.md
│
├── app/
│   ├── build.gradle.kts
│   └── src/
│
├── gradle/
│   └── libs.versions.toml
│
└── .gitignore
```

这些文件不是为了让 AI 阅读更多内容，而是为了让每种信息都有固定的位置。

## 2. 每份文件有严格职责

| 文件                | 管理内容              | 更新频率     |
| ------------------- | --------------------- | ------------ |
| `AGENTS.md`         | AI 必须遵守的开发规则 | 低           |
| `ARCHITECTURE.md`   | 系统架构与模块职责    | 低           |
| `DATABASE.md`       | 数据库结构与数据规则  | 低           |
| `DEVELOPMENT.md`    | 开发规范和验收规则    | 低           |
| `REQUIREMENTS.md`   | 产品需求及功能定义    | 中           |
| `ROADMAP.md`        | 项目开发阶段          | 中           |
| `FEATURE_MATRIX.md` | 功能完成情况          | 每次功能验收 |
| `PROJECT_STATUS.md` | 当前项目实际状态      | 每次任务结束 |
| `TASK-xxx.md`       | 单项开发任务          | 每个任务     |
| `LAST_HANDOFF.md`   | 最近一次工作交接      | 每次会话结束 |
| `ADR-xxx.md`        | 重要技术决策及原因    | 有新决策时   |

其中最重要的是四个文件：

- `AGENTS.md`
- `PROJECT_STATUS.md`
- `FEATURE_MATRIX.md`
- `LAST_HANDOFF.md`

下面解释它们如何协作。

## 3. 建立 AI 的规则系统：AGENTS.md

这是项目级 AI 开发规范。

如果使用支持 `AGENTS.md` 的 Codex 工作环境，可以通过这个文件向代理提供项目指令。

例如：

```
# AGENTS.md

## 1. Project Identity

Project: Habit Tracker
Platform: Android 15
Language: Kotlin

## 2. Mandatory Reading

Before implementation, read:

- ARCHITECTURE.md
- DATABASE.md
- DEVELOPMENT.md
- project/PROJECT_STATUS.md
- project/FEATURE_MATRIX.md

For a specific task, also read its TASK file.

## 3. Development Rules

- Follow the approved architecture.
- Do not invent new product requirements.
- Do not modify unrelated files.
- Do not silently change database schemas.
- Do not remove existing tests to make builds pass.
- Do not introduce unapproved dependencies.

## 4. Task Execution

Every task must have:

1. Explicit scope.
2. Acceptance criteria.
3. Implementation.
4. Tests.
5. Verification report.

## 5. Completion Rules

A task is not completed merely because code was generated.

It must pass its required checks.

## 6. Interruption Recovery

At the end of every session:

- Update PROJECT_STATUS.md.
- Update FEATURE_MATRIX.md if needed.
- Update LAST_HANDOFF.md.
- Record unfinished work.
- Record actual test results.
- Preserve uncommitted changes explicitly.
```

这样，AI 不再需要你每次都重新解释项目规则。

## 4. 建立任务系统：TASK 文件

这是控制 AI 开发范围最重要的措施。

不要给 AI 一个任务：

> 把 Habit Tracker 的今日页面开发完成。

这太宽泛了。

应该拆分成可以独立验证的任务。

例如：

TASK-001

示例任务

实现今日习惯列表的数据读取

任务目标：

在今日页面展示当天应该执行的习惯。

范围：

- 查询有效习惯。
- 查询当天有效计划。
- 查询当天日志。
- 映射为 UI State。

不在范围内：

- 不实现打卡按钮。
- 不实现统计页面。
- 不实现提醒功能。

验收条件：

- 只显示有效习惯。
- 非计划日习惯不显示在今日待办列表。
- 已归档习惯不显示。
- 没有日志时正确显示未记录状态。
- Room Flow 数据变化能够刷新 UI。

测试：

- Domain 单元测试。
- Repository 测试。
- 页面状态测试。

AI 每次只处理一个这样的任务。

这有三个好处：

1. 限制上下文负载。
2. 降低跨文件修改范围。
3. 任务失败时可以精准恢复。

## 5. 建立状态系统：PROJECT_STATUS.md

它记录整个项目当前真实进展。

例如：

```
# PROJECT STATUS

Last Updated: 2026-11-04

## Current Phase

Phase 1 - Data Layer

## Completed

- [x] Android project initialized
- [x] Gradle configuration
- [x] Room Database
- [x] HabitEntity

## In Progress

- [ ] HabitScheduleEntity

## Pending

- [ ] HabitLogEntity
- [ ] HabitReminderEntity
- [ ] DAO tests

## Build Status

Last Build: PASS

Last Test: PASS

## Known Issues

- Schedule overlap validation not implemented.

## Next Task

TASK-004: Implement HabitSchedule DAO.
```

注意：

这里的 `Build Status: PASS` 必须有真实的构建结果支撑，不能让 AI 根据自己的主观判断填写。

最好在后续加入自动化脚本生成构建和测试报告。

# 四、最重要的机制：AI 会话交接与恢复

这是防止 Token 耗尽导致项目失控的核心。

我建议每次 AI 开发会话都遵循固定流程。

AI 开发生命周期

1. 启动（Bootstrap）

   读取 `AGENTS.md`、项目状态、功能矩阵、当前任务和 Git 状态。

2. 核实（Inspect）

   检查实际代码、依赖、构建结果。确认文档与仓库没有明显冲突。

3. 计划（Plan）

   明确本次修改范围、文件清单、验收条件和风险。

4. 执行（Implement）

   只完成当前 TASK，不擅自扩展范围。

5. 验证（Verify）

   执行编译、测试、静态检查，检查 Git Diff。

6. 持久化（Persist）

   更新项目状态、功能矩阵、任务结果。

7. 交接（Handoff）

   写入 `LAST_HANDOFF.md`，提交 Git 或明确记录未提交的修改。

### 一个会话结束时，必须留下什么？

例如：

```
# LAST HANDOFF

## Completed This Session

- Implemented HabitEntity.
- Implemented HabitDao.
- Added DAO unit tests.

## Modified Files

- HabitEntity.kt
- HabitDao.kt
- HabitDaoTest.kt

## Verification

- Gradle Build: PASS
- DAO Tests: PASS

## Current Git State

- Commit: abc1234
- Working Tree: Clean

## Unfinished Work

- HabitScheduleEntity not implemented.

## Known Problems

- None identified.

## Next Action

Start TASK-004.

## Important Notes

- Do not modify existing HabitEntity fields.
- Follow DATABASE.md.
```

这样即使 AI 下一秒突然失去上下文，新的会话也不需要依赖旧聊天记录来推测项目进展。

# 五、Git 是整个系统的安全网

这里我建议你把 Git 的地位提高到项目基础设施，而不只是代码上传工具。

AI 每完成一个经过验证的任务，就应该形成一个可追溯的提交。

例如：

```
main
 │
 ├── feat/project-initialization
 │
 ├── feat/room-database
 │
 ├── feat/habit-schedule
 │
 ├── feat/habit-logging
 │
 └── feat/today-screen
```

对于你目前的个人项目，不需要复杂的 Git Flow。

可以采用：

- `main`：始终保持可构建的稳定版本。
- `feat/*`：独立功能开发。
- `fix/*`：缺陷修复。

建议执行以下规则：

| 事件                 | Git 操作        |
| -------------------- | --------------- |
| 完成一个小任务       | Commit          |
| 一个功能开发完成     | Merge 到 main   |
| 开始高风险数据库修改 | 创建检查点      |
| AI 修改导致异常      | Diff 检查、回滚 |
| 发布 V1              | Git Tag         |

关键原则：未经验证的代码，不得直接合并到稳定分支。

# 六、是否需要多个 AI 代理协同？

可以，但我不建议一开始就部署复杂的多代理系统。

更适合你的，是三个逻辑角色：

Planner（规划者）

任务设计

负责理解需求、拆分任务、明确文件范围和验收标准。

Developer（开发者）

代码实现

负责实际修改代码、编写测试和更新文档。

Reviewer（审查者）

独立验证

负责检查代码 Diff、架构一致性、测试结果和潜在回归。

这三个角色不一定要使用三个不同模型或三个并行会话。

你完全可以让同一个 AI 在不同阶段扮演不同角色，甚至在 Developer 完成后开启一个全新的 Reviewer 会话。

Anthropic 在 2026 年的长周期开发实验中，也采用了 Planner、Generator、Evaluator 三角色结构。其研究同时指出，AI 自我评价容易过于宽松，独立评估有助于形成更可靠的反馈。

Anthropic



不过，编译器、单元测试和静态检查仍然应该是第一道验证关卡，不能用另一个 AI 的肯定代替测试。

# 七、针对你的项目，我建议最终采用的技术组合

这里不需要购买一堆复杂的 AI Agent 平台。

推荐的 V1 AI 开发管理栈

[OpenAI Codex 重磅登场：开启“AI代理指挥中心”时代 薅羊毛！限时免费体验！](https://images.openai.com/static-rsc-4/MsrLz9aU2xpW-JPeX7SsnM5o9MC1AIAK5tm12gQUD3hBnNwoMcbCO6shODK1uhgr_IAmgVH7eUN3aGogtvENwe1b0FqyIMBiBct9lpOwCY6cwzUameeW-vgzje-gpopgM7DhDhzoKk79akOgEyVw4PcxIdDegrXGJl6h4e30uPQ?purpose=inline)

Codex

负责理解任务、修改代码、运行测试

[Code Yellow Software](https://images.openai.com/static-rsc-4/AcXiSoCUlMsiC5tbnBYxhvYNizaL2lqCgEZgMsy61wLWAJVcFl6nl-h-Rq_jGyCAi50fBuZMjCbTjnfWk1HDHraj0WY6nLTgPufy8T05jLC4qNBuPTqzwvVu55URKsO30O-SOf-8vmXOjyCblXhOwa8lS0UVp9eEOCfn6dJuSxk?purpose=inline)

Git

版本控制、提交记录、回滚与检查点

[Evimes | Agencia de Diseño Web y Marketing Digital en Madrid](https://images.openai.com/static-rsc-4/1-pJs617cI8xOfJdw6jsyBc9EV20AqyVXnmZMQ1RSdBsAkPKRQqxcfkTu3KhasPHam-S_s0-oXllmA6oZzAwiOPwUJRkMewon7J5JArmuRGwD-YB-to2zSdY_VTYppdUqf0DP8kZcLKhbKlyis7V9AnY0SLm9ZJEafZh_Ye-w1A?purpose=inline)

GitHub

Issues、任务清单、代码审查与 CI

[Yashwant | Portfolio](https://images.openai.com/static-rsc-4/W4lS-gFnZroBv7dpYKUmkpy4zwgEpLImTsz_2SyTxSXJmMn23bdxymWfButxV42Kb5ATCNz6VVzys32sOY0copVIvFeBVmJcdyxbVS_zt2nidojBoxd3DP9nhhJvXgVMnh-37G_dLeyOOtAtJBm5PuEpDogy_-w1_-llY4BrC2w?purpose=inline)

Android Studio

Android 构建、模拟器、真机调试

对应关系：

| 系统           | 负责什么       |
| -------------- | -------------- |
| Codex          | 执行开发任务   |
| AGENTS.md      | 管理 AI 行为   |
| Markdown 文档  | 保存项目知识   |
| Git            | 保存代码状态   |
| GitHub Issues  | 管理任务       |
| GitHub Actions | 自动构建和测试 |
| Android Studio | 真实运行和调试 |

GitHub Issues 并不是必须的，初期也可以完全使用仓库里的 `TASK-xxx.md` 管理任务。

# 八、还需要防止一个容易被忽略的问题：AI 修改项目规范本身

假设 AI 开发数周后，认为把 Room 改成其他数据库更方便，于是修改了 `DATABASE.md`。

这就出现了一个严重问题：

AI 不仅可以修改代码，还可以修改约束自己的规则。

因此需要设置权限边界。

我建议将文件分为三类：

| 文件类型       | AI 权限                          |
| -------------- | -------------------------------- |
| 产品与架构基线 | 只能提出修改建议，必须经过你批准 |
| 任务状态与进度 | 可以更新，但必须有实际依据       |
| 源代码与测试   | 在任务范围内可以修改             |

例如，以下文件应列入保护清单：

```
ARCHITECTURE.md
DATABASE.md
REQUIREMENTS.md
AGENTS.md
```

如果开发过程中确实需要修改，必须先创建 ADR（Architecture Decision Record），记录：

- 为什么需要修改？
- 原有设计有什么问题？
- 新方案是什么？
- 会影响哪些模块？
- 是否需要数据库迁移？
- 如何验证没有破坏已有功能？

这能防止 AI 为了解决眼前的小问题，逐渐破坏整个项目的设计。

# 九、这套系统能解决到什么程度？

我把实际效果分成三类：

| 风险               | 可以采取的措施                   | 可控程度     |
| ------------------ | -------------------------------- | ------------ |
| Token 额度耗尽     | 任务切分、会话交接、Git 检查点   | 高           |
| 上下文记忆丢失     | 项目状态文件、规则文件、任务记录 | 高           |
| AI 理解错误        | 测试、Diff 审查、独立 Reviewer   | 中高         |
| AI 修改错误架构    | 文档保护、ADR 审批               | 高           |
| AI 修改后产生回归  | 自动测试、CI、稳定分支           | 中高         |
| AI 反复犯同类错误  | 失败记录、回归测试、故障分析     | 中高         |
| 模型本身的能力不足 | 人工介入、任务降级、换模型       | 无法完全消除 |

这里的“高”是指能够建立明确的发现、阻止和恢复机制，并不代表不会出错。

2026 年 5 月微软研究团队提出的 PROBE，也在研究失败后如何将运行证据、故障诊断和有边界的恢复指导组织起来。其结果显示，失败恢复能力可以通过结构化机制改善，但准确诊断并不自动意味着恢复成功。

Microsoft Research



# 十、我的最终建议：先建设管理系统，再开始大规模编码

你目前已经有：

- `ARCHITECTURE.md`
- `DATABASE.md`
- `DEVELOPMENT.md`

这是不错的起点，但还不是一套完整的 AI 项目管理体系。

我建议下一步先完成一个独立的 AI Development Governance（AI 开发治理层），不要急着让 Codex 开始写大量业务代码。

具体新增：

1. `AGENTS.md`：全局 AI 开发规则。
2. `PROJECT_STATUS.md`：项目真实状态。
3. `ROADMAP.md`：阶段性开发路线。
4. `REQUIREMENTS.md`：产品需求基线。
5. `FEATURE_MATRIX.md`：功能完成矩阵。
6. `TASK_TEMPLATE.md`：标准任务模板。
7. `LAST_HANDOFF.md`：会话交接文件。
8. `decisions/`：架构决策记录。

之后再为 Codex 设计一套固定的任务执行、验证、提交、交接流程。

最终目标不是让 AI 拥有无限记忆，而是让它不需要依赖记忆，也能可靠地管理项目。

这也是我认为最适合你这个 Android 项目的路线：先建立轻量、可审计、可恢复的工程治理体系，再逐步增加自动化，而不是一开始就堆叠多个 Agent、向量数据库和复杂编排框架。