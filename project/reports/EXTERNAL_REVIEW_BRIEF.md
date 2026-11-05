# 外部评审简报：Habit Tracker（Android · 本地优先）

> 目的：把项目现状压缩成一份**外部模型不需要访问仓库**就能给出有用建议的文档。
> 写法约定：只写有证据的事；无法核实的标 `UNKNOWN`；不写"应该会通过"这类推测。
> 生成时间：2026-11-05；对应 commit `b73e03b`（`main` 为 `bfb3150` + 分支 `feat/today-queries`）。

## 0. 给外部模型的使用说明

1. 你没有仓库访问权，本文件是你的唯一事实来源。需要细节时按 §8 的摘录推断，不要假设存在未列出的东西。
2. 请**不要**重复本文已写明的结论（例如"要加测试"）。要指出的是：**本文的判断哪里错了、哪里过度、哪里遗漏**。
3. §7 是我真正想问的 18 个问题，按编号回答；答不了的说"信息不足"，不要编。
4. 如果你认为某个约束（§2）本身不合理，请说明代价与替代方案，而不是直接假设它可以改——它是冻结基线，改动需要 ADR 与用户批准。

## 1. 一句话

一个个人习惯追踪 Android 应用：记录每天是否/多少完成，展示今日列表与连续天数；**完全本地、无账号、无网络**。目前处于 V1 的前中期：核心记录链路可用并在真机验证，统计/提醒/备份等尚未开始。

## 2. 冻结的硬约束（不可自行放宽）

| 项 | 值 |
|---|---|
| 平台 | Android 15（API 35）为目标，`minSdk 26`、`compileSdk 35` |
| 语言/UI | Kotlin 2.0.21、Jetpack Compose（BOM 2024.10.01）、Material 3 |
| 持久化 | Room 2.6.1 + KSP，schema v1 已导出 |
| 模块 | **单 Gradle 模块** `:app`，轻量分层 |
| 数据 | 本地优先；无账号、无服务器、无云同步、无网络权限 |
| 提醒 | 只用 Normal 精度，不申请精确闹钟权限，允许系统延迟 |
| V1 不做 | 桌面小组件、深色模式、常驻前台服务、后端、微服务 |
| 构建 | JVM 17；Gradle 8.9 |

## 3. 产品范围与当前状态

功能矩阵共 15 项（`F-001`~`F-015`），状态含义：
`NOT_STARTED` → `IMPLEMENTED_UNVERIFIED` → `VERIFIED`（有自动化断言）→ `DEVICE_VERIFIED`（真机看过）。

| 功能 | 实现 | 自动化 | 真机 | 备注 |
|---|---|---|---|---|
| F-001 工程骨架 | ✅ | ✅ | ✅ | 仅安装/启动验证 |
| F-002 今日页与导航 | ✅ | ✅ | ✅ | |
| F-003 创建习惯 | ✅ | ✅ | ✅ | **编辑**未做 |
| F-004 归档/恢复 | ✅ | ✅ | ❌ | 无界面入口，只有 UseCase |
| F-005 计划频率（每天/工作日/自定义） | ✅ | ✅ | ⚠️ | 真机只建过"每天" |
| F-006 布尔打卡 | ✅ | ✅ | ✅ | |
| F-007 数值记录（如 2000 ml） | ✅ | ✅ | ✅ | 含小数与中文输入法处理 |
| F-008 撤销/修改 | ❌ | — | — | |
| F-009 连续天数（当前/最长） | ✅ | ✅ | ⚠️ | 真机只验证"1 天"；多段与最长仅单测 |
| F-010/F-011 统计与热力图 | ❌ | — | — | |
| F-012/F-013 提醒与重启恢复 | ❌ | — | — | 权限已在 manifest 声明但无代码（遗留声明） |
| F-014 备份/恢复 | ❌ | — | — | 导出格式未定 |
| F-015 离线保证 | ✅ | ✅ | — | 治理检查第 22 项强制：manifest 不得含 `INTERNET`，依赖不得含 okhttp/retrofit/ktor/firebase |

## 4. 架构（简要）

```
app/src/main/java/com/habittracker/
  domain/
    calculation/     ScheduleRules, LogStatusRules, StreakRules   ← 纯函数，无 IO、无时钟
    usecase/         CreateHabit, RecordHabitProgress, GetStreak, ArchiveHabit
    repository/      HabitRepository（接口，不泄漏 Room 类型）
    model/           Habit, HabitSchedule, DailyLog, NewHabit
  data/
    local/           Room entities / DAO / 迁移 / schema
    repository/      RoomHabitRepository（唯一实现）
  feature/
    today/           TodayPresenter（纯 Kotlin 状态机）+ TodayScreen（只渲染）
    create/          CreateHabitPresenter + CreateHabitScreen
  navigation/        AppRoot（3 个目的地，其中"统计/我的"是占位）
```

两条贯穿性规则：
- **业务规则不写在 Composable / DAO / Receiver 里**。状态与判断放在 pure presenter 或 domain 对象中，因此可以在 JVM 上直接测，不需要 Robolectric。
- **写入路径唯一**：状态与完成时间由 `LogStatusRules` 计算，DAO 不提供 `setStatus`，数据库触发器兜底拒绝不一致的行。

## 5. 质量体系（这是本项目最不寻常的部分）

### 5.1 三重门禁

`tools/verify-all.ps1` 一条命令跑完，任一失败即非零退出：

| 步骤 | 内容 | 当前结果 |
|---|---|---|
| 治理检查 | 自研 `check-governance.ps1`，**22 项** | 22/22，0 警告 |
| 单元测试 | `tools/run-unit-tests.ps1`（见 5.4） | **86 项**通过 |
| DAO SQL | `tools/verify-dao.mjs`，真实 SQLite 跑 DAO 的 SQL 文本 | **27 项** |
| Schema | `tools/verify-schema.mjs` | 40 项 |
| 构建 | `assembleDebug` | 通过 |
| 空白 | `git diff --check` | 干净 |

### 5.2 治理检查在查什么（22 项节选）

- 每个 `DONE` 任务必须有独立评审记录，且记录里写明**被审查的 commit 与独立性**；
- `main` 上的 `PARTIAL` 任务必须在状态文件的「在册例外」逐条登记；
- 任务证据表的结果列必须是**判定词整格锚定**（`VERIFY ALL PASS` / `PASS` / `FAILED` / `未运行（任务未开始）`），不允许"待补""基本通过"；
- 声明 `NOT_STARTED` 的任务不得出现开工痕迹（勾选、带日期的日志行、真实判定词），否则判定为"谎报未开始"；
- `README`/`PROJECT_STATUS`/`TEST_REPORT` 的计数必须与测试报告最后一行一致；
- 离线保证：manifest 不得有 `INTERNET`，依赖不得有网络库。

另有 `tools/replay-governance-probes.ps1`：15 个"负向探针"（P5–P19），每次把仓库改成该被抓住的样子，断言检查器确实红；探针先断言自己真的改到了文本，再断言检查器失败。

### 5.3 流程

```
TASK-xxx 文件（目标/范围/验收标准/证据表）
   → 实现（只改 Scope 内文件）
   → 自己跑门禁 + 变异测试
   → 独立 Reviewer 子进程评审（另一份上下文，不看我的解释，自己跑命令）
        → REJECT：改；APPROVE：合入 main
```

现状：`TASK-000`~`TASK-022` 共 23 个任务文件，**16 份评审记录**，95 个 commit，50 个 Kotlin 文件。
评审轮次：多数 1 轮通过；`TASK-012`/`TASK-018`/`TASK-020` 各 3 轮，`TASK-019` 2 轮，`TASK-021` 2 轮。

### 5.4 一个环境限制导致的架构选择

本机沙箱里 `gradlew test` **必然失败**（Gradle 测试 worker 的命名管道被禁，`ClassNotFoundException: GradleWorkerMain`）。所以：

- 测试用 `tools/run-unit-tests.ps1` 直接 `java -cp ...` 跑 JUnit，**绕开 Gradle worker**；
- 没有模拟器（`HypervisorPresent=False`，无 WHPX），真机验证靠 USB 手动走查 + `adb` 截图；
- 因此**业务逻辑被迫放在纯 Kotlin 层**（presenter、domain 对象），否则一行都测不了。这个限制反而塑造了架构。

## 6. 已知弱点（诚实清单）

1. **自动化测试从未自己发现过缺陷。** 23 个任务里，真实缺陷全部来自：独立评审、真机走查、变异测试。到目前为止没有一次是"测试跑红了，于是发现 bug"。这是我认为最值得外部意见的信号。
2. **UI 无自动化**：Compose 的渲染与交互只有手动真机走查（截图不入库）。presenter 有 86 项覆盖，Composable 是 0。
3. **Room 层薄**：Cursor 映射、TypeConverter 往返、`Flow` 失效通知、`withTransaction` 边界、Room 生成的 upsert SQL——都没有运行时测试（受 5.4 限制）。只有 DAO 的 SQL 文本在真实 SQLite 上验证。
4. **文档同步反复失败**：最近 3 次 `REJECT` 里，主要阻断项都是文档不一致（矩阵写"未开始"、计数写旧值、声称"已备案"但仓库里没有），不是逻辑错误。说明"改 A 忘 B"是这套流程的结构性弱点。
5. **自我评审风险**：实现与评审是同一个模型（不同上下文），评审真的独立吗？目前证据是"每轮都能抓出真问题"，但这不等于独立性成立。
6. **性能债刚动了一半**：`TASK-022` 把今日列表从"每习惯 4 次查询"改成"整表 4 次"，但批量读仍然是**取每个习惯从首个计划版本到今天的全部日志，在 Kotlin 里过滤**。
7. **未定语义**：数值习惯的"撤销"（从 2000 退回 1000 是否算未完成？）、备份导出格式、`TASK-003/004/005` 三个 `PARTIAL` 长期挂在 `main`（已登记为例外）。
8. **声明但无代码**：`POST_NOTIFICATIONS`、`RECEIVE_BOOT_COMPLETED` 在 manifest 里，但没有提醒功能——属于遗留声明。

## 7. 我想问的 18 个问题

### A. 架构与性能

**Q1.** 今日列表的批量读策略是"一次取全部习惯的全部历史日志 + 计划版本，然后在内存里逐习惯算"。习惯数 20、每个 2 年 DAILY 的话，一次加载约 20×730 = 1.5 万行日志。这个量级在手机上算问题吗？应该改成 SQL 聚合（例如只取"当前连续所需的最大回溯窗口"），还是保持简单？
**Q2.** `HabitRepository` 的 4 个批量方法在接口上给了"逐习惯循环"的默认实现，Room 与 Fake 覆盖它们。默认实现的好处是其它实现仍能编译，坏处是忘了覆盖就悄悄退回 N+1。你倾向：接口改成抽象方法（强制覆盖）、还是保留默认但加运行时断言、还是别的？
**Q3.** `GetStreakUseCase` 被拆成"加载"与纯函数 `fromLoaded` 两半，列表用纯函数那半。这样规则只有一份，但"加载"那半现在有两个调用点（单习惯路径、批量路径）。这种拆分方式你认可吗？有没有更干净的形状？
**Q4.** 统计页（F-010/011）会是同样的读取模式（多习惯 × 长区间）。你会现在就把"批量读"抽象成一个可复用的 `HabitStatisticsSource`，还是等统计页真正动手时再抽？

### B. 治理与流程

**Q5.** 22 项自研治理检查 + 15 个负向探针，维护成本已经不低（每次改文档都可能踩到计数一致性）。对单人项目而言，这套机器是不是**过度工程**？如果是，你会砍掉哪几项、保留哪几项？
**Q6.** 文档不同步是最近 3 次 REJECT 的主因。根因我判断是"同一事实写在 5 个文件里"（README / PROJECT_STATUS / TEST_REPORT / LAST_HANDOFF / TASK 文件）。你会怎么消掉这种重复——单一事实源 + 生成？还是把检查器做成"自动改写"而不是"报错"？
**Q7.** 独立评审由同一个模型扮演（不同上下文、不看我的解释、自己跑命令）。你认为这种"同模型异构上下文"的评审有多少价值？有没有更便宜但更有效的替代（例如只做变异测试、或让评审只看 diff 不看任务描述）？
**Q8.** 我要求评审必须给出"变异表"：每条变异改坏一处代码，看多少测试变红。这个做法在小项目里值得常态化吗？什么情况下它会变成仪式？

### C. 测试策略

**Q9.** 在没有模拟器、`gradlew test` 不可用的前提下，Compose UI 的自动化测试有什么现实路径？（Robolectric 需要 Gradle；截图测试同样需要构建链。是否有纯 JVM 的替代？）
**Q10.** Room 的映射与 `Flow` 通知没有运行时覆盖，只有 DAO 的 SQL 文本在 SQLite 上验证。这个缺口在 V1 可接受吗？如果不接受，最便宜的补法是什么？
**Q11.** 我用"给 Fake 加读取计数器，断言 5 个习惯的列表固定 4 次查询"来防 N+1 回归。这类"断言实现细节"的测试，你认为是好设计还是脆弱的伪测试？
**Q12.** 现在 86 项测试里，断言多是"值等于什么"，消息断言很少。有没有哪类测试是你认为我明显缺失的（例如并发、时间边界、时区/夏令时、闰年）？`StreakRules` 用 `LocalDate` 逐日推进，跨时区或 DST 会不会出问题？

### D. 数据与迁移

**Q13.** schema v1 已导出，触发器有 8 个，禁止破坏性迁移。V1 之后如果要改 `habit_logs` 的语义（例如数值习惯引入"撤销"），你建议怎么设计迁移才能不破坏历史数据？
**Q14.** 备份/恢复（F-014）还没设计。本地优先、无账号的 Android 应用，你觉得导出成什么格式最稳（JSON + 版本号？SQLite 文件？），恢复时如何避免覆盖更新版本的数据？

### E. 产品语义

**Q15.** 连续天数的规则是：**只数计划日；今天还没完成不算断**。依据是需求文档"过去已到期的计划日缺失或未完成才是失败"。你认同这个语义吗？有没有常见产品（Duolingo、Streaks 等）的反例值得参考？
**Q16.** 数值习惯（2000 ml 目标）的"撤销"该怎么定义？从 2000 改成 1000 之后，当天算"未完成"还是"部分完成"，连续天数是否回退？

### F. 节奏

**Q17.** 当前节奏是"一次一个任务 + 独立评审 + 门禁"，一个功能任务平均 1–3 轮评审。对单人和 AI 协作，这个节奏偏慢吗？如果要提速，你会在哪里让步（评审？门禁？真机验证？），哪里绝不让步？
**Q18.** 如果只能给这个项目一条建议，你会说什么？

## 8. 关键代码摘录

### 8.1 连续天数（纯函数，全项目的风格样板）

```kotlin
object StreakRules {
    data class Streak(val current: Int, val longest: Int)

    fun compute(schedules: List<HabitSchedule>, completedDates: Set<LocalDate>, today: LocalDate): Streak {
        if (schedules.isEmpty()) return Streak.EMPTY
        val firstDay = schedules.minOf { it.effectiveFrom }
        if (firstDay.isAfter(today)) return Streak.EMPTY

        fun due(date: LocalDate): Boolean = schedules.any { schedule ->
            !date.isBefore(schedule.effectiveFrom) &&
                (schedule.effectiveUntil == null || !date.isAfter(schedule.effectiveUntil)) &&
                ScheduleRules.isScheduledOn(schedule.frequencyType, schedule.weekdayMask, date)
        }

        // 一次正向遍历同时得到两个数：today 未完成不重置 run，所以循环结束时 run 就是当前连续。
        var longest = 0
        var run = 0
        var day = firstDay
        while (!day.isAfter(today)) {
            if (due(day)) {
                if (completedDates.contains(day)) { run++; if (run > longest) longest = run }
                else if (day != today) { run = 0 }
            }
            day = day.plusDays(1)
        }
        return Streak(current = run, longest = longest)
    }
}
```

### 8.2 今日列表的批量加载（TASK-022 后）

```kotlin
private suspend fun snapshot(source: List<Habit>): TodayUiState {
    val date = today()
    if (source.isEmpty()) return TodayUiState(date = date, habits = emptyList(), isLoading = false)

    val ids = source.map { it.id }
    val schedulesToday = repository.activeSchedules(ids, date)      // 1 次
    val logsToday = repository.dailyLogs(ids, date)                 // 1 次
    val versions = repository.scheduleVersionsFor(ids)              // 1 次
    val firstDay = versions.values.flatten().minOfOrNull { it.effectiveFrom } ?: date
    val logsSince = repository.logsInRangeFor(ids, minOf(firstDay, date), date)  // 1 次

    return TodayUiState(date = date, habits = source.map { habit -> describe(...) }, isLoading = false)
}
```

### 8.3 Room 侧的批量查询

```kotlin
@Query("SELECT * FROM habit_logs WHERE habit_id IN (:habitIds)" +
       " AND log_date BETWEEN :from AND :to ORDER BY habit_id ASC, log_date ASC")
suspend fun findRangeForHabits(habitIds: List<Long>, from: LocalDate, to: LocalDate): List<HabitLogEntity>
```

### 8.4 任务文件的形状（每个任务都长这样）

```
1. Objective   目标 + 为什么现在做
2. Context     上游依据（需求/ADR/上一任务）
3. Scope       明确要做什么
4. Out of Scope 明确不做什么
5. Expected Files
6. Acceptance Criteria  可勾选的、能判定的
7. Tests       实际要跑的命令
8. Risks and Decisions  含被否决的方案与理由
9. Implementation Log   带日期的实际过程（含失败与重试）
10. Verification Evidence  命令 → 实际结果 → 失败摘要
11. Completion Review  验收/测试/独立 APPROVE
12. Handoff
13. Independent Review 记录路径与结论
```

## 9. 指标（可对照判断）

| 指标 | 值 |
|---|---|
| commit 数 | 95 |
| 任务文件 | 23（`DONE` 18、`PARTIAL` 5，其中 3 个是历史例外） |
| 评审记录 | 16 |
| 单元测试 | 86 |
| DAO SQL 断言 | 27 |
| Schema 断言 | 40 |
| 治理检查 | 22 |
| 治理负向探针 | 15 |
| 变异测试组数（累计） | 约 40 组，每组都记录"多少项变红 + 是否打中正确断言" |
| 真机验证 | 1 台（iQOO Neo7 SE，Android 15），手动走查 + 截图 |
| 自动化测试独立发现的缺陷 | **0** |

```
你是我的外部技术顾问。附件是我一个 Android 单人项目的现状简报（含架构、测试体系、
已知弱点和 18 个具体问题）。我没有仓库访问权限可以提供，请只依据这份简报回答。

要求：
1. 按 Q1–Q18 编号回答；答不了的直接说"信息不足"并说明还缺什么，不要编造项目细节。
2. 每条建议给"结论 / 代价 / 最小可行做法"，三句话内。
3. 明确指出你认为简报里判断错误的结论（尤其是"自动化测试从未独立发现缺陷"这条
   信号意味着什么），不要只顺着我说。
4. 不要泛泛建议"多写测试""加强文档"——那种建议我已经有 22 项检查在管。
   我要的是：这个体系哪里过度、哪里漏、哪里顺序错了。

如果你只能回答一个问题，请回答 Q18。
```

## 11. 我期待的答复形式

- 按 Q1–Q18 编号回答，允许一题多段。
- 每条建议请给：**结论 / 代价 / 最小可行做法**，三句话内说清。
- 明确区分"这是硬错误"与"这是口味偏好"。
- 如果某题信息不足，直接说不足，并说明还需要什么证据。
