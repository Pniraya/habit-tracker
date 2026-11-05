# REVIEW-TASK-024: 独立评审记录（第 2 轮）

- 审查任务：`TASK-024`（接缝补洞：转换器、差分、翻日、往返）
- 审查 commit：`f700d4ba0e2722591b360a90f3409432f934da23`（分支 `feat/seam-hardening`，基线 `main` @ `427e7cc0345b83425ef39b12f34a9991798eda62`；第 1 轮审查的是 `3b5806290faddbd7962e54a4414f56c716f88802`）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现；只读核对 + 自造变异）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`

评审开始 `git status --short` 为空。9 组变异串行执行，逐组 `git checkout -- <path>` 还原并当场复查工作区；结束时工作区只剩本评审文件。门禁与变异**串行**，未与任何 Gradle 任务并发。

## 1. Scope

- [x] 第 2 轮改动（`3b58062..f700d4b`，14 文件）与第 1 轮 6 条阻断逐条对应：接线规则、午夜定时器、差分重写与降级、文档同步、任务文件矛盾、外部答复文件。
- [x] `tools/verify-dao.mjs` 的夹具时间改为 `08:00:00` 属于同一接缝（存储格式）的修复，未越界。
- [x] 新建 `project/tasks/TASK-025.md` 是为了消除 `TASK-024` §12 造成的悬空引用（治理唯一 warning），本身不产生代码改动，可接受。
- [x] 无新依赖、无权限、无 Gradle module、无 schema/迁移改动；`refresh()` 加 `runCatching` 与 `record()` 对齐。
- [ ] `TASK-024.md` §10 与本轮新增测试不一致（见 §4）：新增的 `leapDaysSurviveTheRoundTrip` 改变了 M1 的红数，但证据表没重测。

## 2. Diff 核对

- [x] `git diff --check` 干净；无冲突标记；无删除测试（104 = 101 + 3）。
- [x] `@Suppress("DEPRECATION")` 生效：`gradlew compileDebugKotlin --rerun-tasks` 实测 **0 warning**（第 1 轮的 `w: :75:38 …deprecated` 已消失），并有注释说明替换品需要未声明的依赖。第 1 轮该非阻断项**已关闭**。
- [x] `TodayPresenter.refreshesOn(event: String)` 把「哪个事件触发刷新」搬出 Composable，符合 `AGENTS.md` §4 与 `TASK-013` 先例；用 `String` 而不是 `Lifecycle.Event` 保持了 presenter 无 Android 依赖，可接受。
- [ ] 午夜定时器被**内联写在 Composable 里**（`AppRoot.kt` 的 `LaunchedEffect`：`LocalDateTime.now()` → `plusDays(1).atStartOfDay()` → `Duration…toMillis() + 1000`）。这是本轮新增的生产算术，且不可被 JVM 测试触及（见 §4 的 R2-MH2）。

## 3. 架构与数据

- [x] 无 schema 变更；`DATABASE.md` 2 的「`LocalTime` → TEXT ISO `HH:mm:ss`」现在与 `Converters`、与 `verify-dao.mjs` 夹具、与 `ConvertersTest` 四处一致——第 1 轮指出的「夹具写 `08:00`」是真实接缝缺陷，已修。
- [x] 差分 oracle 已改为 epoch-day 区间算术 + 字面量掩码（`127`/`31`），不再复用 `ScheduleRules` 的常量，也不再逐字复制 `isAfter/isBefore` 谓词。它在**形状**上独立了。
- [ ] 但它的**语义**仍与实现共享：`epoch >= start && epoch <= end` 与实现的闭区间是同一约定，掩码位仍是 Monday=bit0。这一点 `TASK-024` §8 已如实承认（「只抓单边漂移」「不是第二双眼睛」），本轮不再作为阻断。
- [ ] §8 的降级句里有一处**实测已不成立**的说法：「把 `WEEKDAYS_MASK` 从 31 改成 30 时它全绿」。在 HEAD 上实测是 2 红，且差分也红——原因是字面量 `31` 与常量 `30` 触发 `ScheduleRules.isScheduledOn` 的 `require(mask == weekdayMask)`，属于一致性守卫抛异常，**不是** oracle 的判别力（见 §4 R2-ME）。

## 4. 验证证据

### 4.1 门禁（`pwsh -NoProfile -File tools/verify-all.ps1`，串行）

| 步骤 | 实测 | 退出码 |
|---|---|---|
| `check-governance.ps1` | `SUMMARY: 22 checks, 22 passed, 0 warnings, 0 failed` | 0 |
| `run-unit-tests.ps1` | `OK (104 tests)` | 0 |
| `verify-dao.mjs` | `checks: 27, failed: 0` | 0 |
| `assembleDebug` | `BUILD SUCCESSFUL in 18s` | 0 |
| `git diff --check` | `no whitespace errors` | 0 |
| 合计 | `RESULT: VERIFY ALL PASS` | **0** |

作者声明的三个数字全部复现：**104 单测、27 DAO、治理 22/22 · 0 警告、exit 0**。第 1 轮的治理 warning（悬空 `TASK-025`）已随 `TASK-025.md` 的创建消失。

### 4.2 第 1 轮变异在 HEAD 复跑

| # | 变异 | 位置 | 红 | 结论 |
|---|---|---|---|---|
| R2-M2 | `refresh()` 空实现 | TodayPresenter | 1 | `crossingMidnightShowsTheNewDay`，与作者声明一致 |
| R2-M3 | `>=` → `>` | LogStatusRules:31 | 11 | 与声明一致 |
| R2-M4 | `else if (day != today)` → `else` | StreakRules:54 | 5 | 含差分，与声明一致 |

### 4.3 本轮新造变异（全部在 HEAD 实测）

| # | 变异 | 位置 | 红 | 打红的测试 | 判断 |
|---|---|---|---|---|---|
| R2-M1 | 日期格式改 `yyyy-M-d` | Converters:18 | **3**（作者仍写 2） | `localDateIsStoredAsIsoText`、`aStoredDateParsesBackToTheSameDay`、**`leapDaysSurviveTheRoundTrip`** | 证据表过期：新增闰日测试也抓到这个变异 |
| R2-ME | `WEEKDAYS_MASK` 31→30 | ScheduleRules:16 | **2**（作者写「差分全绿」） | `weekdaysMaskIsMondayToFriday`、**差分**（因 `require(mask == weekdayMask)` 抛异常） | 结论方向对（oracle 无语义判别力），但引用的是第 1 轮旧测量；HEAD 行为已变 |
| R2-MH | `refreshesOn` 的 `ON_RESUME` → `ON_CREATE` | TodayPresenter | 1 | `onlyReturningToTheForegroundRefreshes` | **第 1 轮阻断 1 已关闭**：规则现在有测试，call site 仍靠真机（作者已声明） |
| **R2-MH2** | 午夜定时器 `plusDays(1)` → `plusDays(2)`（少睡一天也不刷） | AppRoot `LaunchedEffect` | **0** | 无，`OK (104 tests)` | **新增生产代码零覆盖**：定时器算错一天没有任何测试会红 |
| R2-MA | `effectiveUntil` 改开区间 | StreakRules:37 | 3 | 差分 + 2 条旧测试 | 差分仍能抓单边漂移，仍非唯一 |
| R2-MB | `bitFor` 位移旋转 | ScheduleRules:31 | 7 | 差分 + 6 | 同上 |

小结：第 1 轮 6 条阻断中 5 条实质关闭，但**新增了一条同类问题**（午夜定时器零覆盖），且**证据表没有在改完之后重测**（M1 与 ME 两处数字已过期）。

## 5. 文档与分支

- [x] `README.md:15`、`PROJECT_STATUS.md:13`、`TEST_REPORT.md:9`（Latest Run）与 `TEST_REPORT` 最后一行、`TASK-024` 自动摘要：**104 项**一致；DAO 27/27 一致；治理 22 项一致。
- [x] `TEST_REPORT.md:7` 头部已改为 `feat/seam-hardening`（`TASK-024`，基线 `main` @ `427e7cc`）。
- [x] `TASK-024` §5 已列 `AppRoot.kt`、去掉未改动的 `TodayScreen.kt`；§6 五条全部 `[x]`，与 §11「5/5」、自动摘要「通过 5 项 / 未通过 0 项」一致；§10 M1 归因已改为「两条都在 `ConvertersTest`」（现应为三条）。
- [x] `project/reports/EXTERNAL_REVIEW_ANSWERS.md` 现在存在（209 行），`TASK-024` §2/§9 的引用可解析。
- [ ] **`LAST_HANDOFF.md` 描述的是提交前的状态**：`:10` 最近 commit 写 `3b58062` 且「`TASK-024` 的修复尚未提交」——在被审的 `f700d4b` 上修复**已经提交**；`:31` 指向「下方『已知警告』」——该小节不存在，且写的「21 passed / 1 warning」是 `TASK-025` 创建之前的旧值（HEAD 实测 22/0）；`:56` Next Safe Action 仍是「按 6 条阻断修复」，实际应等第 2 轮评审。按它恢复会去找未提交改动和一个不存在的小节。
- [ ] **`project/reports/EXTERNAL_REVIEW_ANSWERS.md` 无出处**：没有作者/模型/日期/会话标识，只有页首「依据：仅 `EXTERNAL_REVIEW_BRIEF.md`」。它在「被指为悬空引用」的同一轮由实现者加入，而实现者与代码是同一个模型。内容本身具体、且对本项目不客气（不是自我辩护），但作为证据它只能证明「仓库里有这样一份文档」，不能证明「一个独立外部模型说过这些」。
- [ ] `TASK-024` §10 M4 行的措辞「差分测试能独立判错，不是摆设」与 §8 的新结论（「从未是唯一红线」「不是第二双眼睛」）自相矛盾，应统一。

## 6. 结论

- 结论：`REJECT`
- 证据：门禁 `RESULT: VERIFY ALL PASS`（exit 0，`22/22 · 0 warnings`、`OK (104 tests)`、`checks: 27`）；第 1 轮 6 条阻断中 5 条已实质关闭；但 HEAD 上仍有 2 处验证证据是旧测量、1 处新增生产代码零覆盖、1 处新增覆盖声明超出实际、交接文件停留在提交前状态、外部答复文件无出处。
- 需修复项（按严重度编号）：

1. **新增的午夜定时器零测试覆盖**：把 `AppRoot` 的 `LaunchedEffect` 里 `plusDays(1)` 改成 `plusDays(2)`（定时器晚一天）后 **104 项全绿**（R2-MH2）。这是本轮新加的生产算术，与第 1 轮阻断 1 同类。最小修法：把 `millisUntilNextMidnight(now: LocalDateTime): Long`（或等价纯函数）抽到 `TodayPresenter`/独立 object，断言 23:59:59 → 约 1_001ms、00:00:00 → 约 86_401_000ms、闰日与年末仍指向次日 00:00；Composable 只保留 `delay` + `refresh()`。若判定无法抽，就在 §8 与 `TEST_REPORT` 的 Known Gaps 明确写「定时器算术未覆盖」，不要把 §6 第 3 条写成已由测试覆盖。

2. **验证证据没有在 HEAD 重测**：①M1 现在是 3 红（`leapDaysSurviveTheRoundTrip` 也抓到 `yyyy-M-d`），`TASK-024` §10 仍写「FAILED（2 项）」、`TEST_REPORT` 行仍写「4 组变异各 2/1/11/5 红」；②§8 写「`WEEKDAYS_MASK` 31→30 时差分全绿」，HEAD 实测是 2 红且差分因 `require(mask == weekdayMask)` 抛异常而红。要求：按 `f700d4b` 重跑并更新 §8/§10 与 `TEST_REPORT`，并把「差分被一致性守卫带红」与「差分独立判错」分开表述，否则 §8 的诚实降级本身变成了不准确的证据。

3. **前台跨 0 点仍有一个洞，§8 的覆盖声明比代码宽**：`LaunchedEffect` 只在今日页处于组合中时运行，且进入时**不先刷新**。用户在「统计/我的」占位页跨过 0 点再切回今日页（同一 Activity，不触发任何 lifecycle 事件），看到的仍是昨天，直到下一次午夜或下一次 `ON_RESUME`。§8 写「前台跨 0 点」已覆盖，实际只覆盖「一直停在今日页」。最小修法：effect 开头先 `refresh()`（或 `if (state.date != LocalDate.now()) refresh()`），一行；并在 §8 把「从别的页面切回今日页」写清已覆盖/未覆盖。

4. **`LAST_HANDOFF.md` 仍是提交前快照**：`:10` 的 `3b58062` 与「修复尚未提交」、`:31` 指向不存在的「已知警告」小节且写 21 passed / 1 warning、`:56` 的 Next Safe Action 仍要求修 6 条阻断。按 `f700d4b` 重写这三处（commit、治理 22/0、下一步=等第 2 轮评审结果）。

5. **`EXTERNAL_REVIEW_ANSWERS.md` 缺出处**：加一行来源（谁/哪个模型、何时、原样粘贴还是事后整理、是否经第三方核实），或明确标注「由开发者整理/自答，非独立外部意见」。否则第 1 轮「引用不存在的文件」只是换成了「引用不可核对的文件」，而这份文档正是 `TASK-024` 的立项依据。

### 6.1 第 1 轮阻断的关闭情况（供下一轮核对）

| 第 1 轮阻断 | 状态 | 证据 |
|---|---|---|
| 1. `ON_RESUME` 接线零覆盖 | **关闭**（call site 仍靠真机，已声明） | R2-MH 1 红；`onlyReturningToTheForegroundRefreshes` |
| 2. 翻日只覆盖后台→前台 | **部分关闭**：加了定时器，但见本轮阻断 1/3 | `AppRoot` `LaunchedEffect`；R2-MH2 0 红 |
| 3. `LAST_HANDOFF` 未同步 | 未关闭（换了新的过期点） | 见本轮阻断 4 |
| 4. `TEST_REPORT` 头部/警告数 | **关闭** | `:7` 已改；治理实测 22/0 |
| 5. `TASK-024` 自相矛盾/归因/悬空引用 | **关闭** | §5/§6/§11/自动摘要一致；`EXTERNAL_REVIEW_ANSWERS.md` 已存在（出处待补） |
| 6. 差分独立性被夸大 | **关闭**（已降级；但降级句需按本轮阻断 2 修正） | §8 新表述 |

### 6.2 非阻断（已接受/仅记录）

- `refresh()` 仍与 Flow collector 竞争（last-writer-wins）；`LogStatusRoundTripTest` 仍是内存内序列；差分生成器仍不产生重叠版本或未来版本。三者都已在任务文件声明，本轮**不**作为阻断，但「重叠版本」是 `schedules.any` 语义唯一可能与他人理解不同的地方，建议在 `DATABASE.md` 或 `TASK-023` 里显式写「版本区间不得重叠」，让它从「未测」变成「不变量」。
- `refresh()` 的 `runCatching` 会吞掉 `CancellationException`（`record()` 早就是这样），属既有风格，不新增问题。
- `refreshesOn(event: String)` 字符串化牺牲了编译期校验；换来 presenter 无 Android 依赖，可接受。真正未覆盖的是 `AppRoot` 里 `event.name` 的传参，作者已声明。
