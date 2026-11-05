# REVIEW-TASK-024: 独立评审记录

- 审查任务：`TASK-024`（接缝补洞：转换器、差分、翻日、往返）
- 审查 commit：`3b5806290faddbd7962e54a4414f56c716f88802`（分支 `feat/seam-hardening`，基线 `main` @ `427e7cc0345b83425ef39b12f34a9991798eda62`）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现；只读核对 + 自造变异）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`

评审开始与结束时 `git status --short` 均为空；10 组变异逐组 `git checkout -- <path>` 还原，每组还原后当场复查工作区为空，结束时 `git status --short` 为空。所有门禁与变异**串行**执行，未与任何 Gradle 任务并发。

## 1. Scope

- [x] 改动与任务 Scope 一致（存储格式测试、差分测试、翻日、数值往返）。
- [x] 无新依赖、无权限、无 Gradle module、无后台服务、无 schema/迁移改动（`git diff --stat` 12 文件，只有 `app/**` 与 `project/**`）。
- [ ] **§5 Expected Files 与实际改动不符**：实际改了 `navigation/AppRoot.kt`（`TASK-024.md:5` 的 Expected Files 没列它），而列出的 `TodayScreen.kt` 一个字都没改。功能上 AppRoot 的接线是必要的，但任务记录的文件范围与 diff 不一致（`REVIEW_CHECKLIST` §1 要求「每个改动文件都能对应到 TASK」）。

## 2. Diff 核对

- [x] `git diff --check` 无空白错误；无冲突标记（`verify-all` 第 5 步 `no whitespace errors`）。
- [x] 没有删除或绕过既有测试（101 = 90 + 11，无删除）。
- [x] 没有把业务规则写进 DAO / Receiver。
- [x] 生产代码改动只有 3 处：`TodayPresenter.refresh()`（4 行）、`TodayViewModel.refresh()`（1 行）、`AppRoot` 的 `ON_RESUME` 观察者（12 行）；没有顺手重构。
- [ ] `AppRoot.kt:75` 用了 `androidx.compose.ui.platform.LocalLifecycleOwner`。实测编译告警：`w: ...AppRoot.kt:75:38 'val LocalLifecycleOwner ...' is deprecated. Moved to lifecycle-runtime-compose library in androidx.lifecycle.compose package.`（`gradlew compileDebugKotlin --rerun-tasks`，exit 0）。BOM 2024.10.01 下这是废弃 API；替换品需要新增 `lifecycle-runtime-compose` 依赖（本仓未声明，加依赖需批准）。**功能等价**（单 Activity，值相同），不阻断，但属于「新增一处废弃调用且未记录」。

## 3. 架构与数据

- [x] `TodayPresenter` 仍然是纯 Kotlin：时钟是注入的 `() -> LocalDate`，没有引入 Android API；生命周期观察只存在于 `AppRoot`。
- [x] `Converters`、schema、DAO 一行未改；DATABASE.md 4.2 的 `effective_from`/`effective_until` 双闭区间与 `StreakRules` 一致（`DATABASE.md:106-107`）。
- [ ] **「oracle 与实现不共享判定代码」不成立**：`StreakRulesDifferentialTest.kt:36-40` 的版本覆盖谓词与 `StreakRules.kt:36-38` 逐字相同（`!day.isBefore(effectiveFrom) && (until == null || !day.isAfter(until))`），只有掩码位那一行换成了内联位移；且 oracle 的掩码数据直接取自 `ScheduleRules.ALL_DAYS_MASK`/`WEEKDAYS_MASK`（`:80,82`）。任务 §8（`TASK-024.md:42`）的「不共享判定代码」是夸大。
- [ ] `verify-dao.mjs:197` 手写 `reminder_time = "08:00"`，而 `Converters.localTimeToString` 产出 `"08:05:00"`（`Converters.kt:48`，DATABASE.md 2 要求 ISO `HH:mm:ss`）。新增的 `ConvertersTest` 断言后者，却与工具链的 fixture 形状相反——日期那一半（`"2026-01-05"`）确实对得上，时间这一半对不上，且没人对齐。

## 4. 验证证据

### 4.1 门禁（`pwsh -NoProfile -File tools/verify-all.ps1`，串行执行）

| 步骤 | 实测 | 退出码 |
|---|---|---|
| `check-governance.ps1` | `SUMMARY: 22 checks, 21 passed, 1 warnings, 0 failed`；`RESULT: OK` | 0 |
| `run-unit-tests.ps1` | `OK (101 tests)` | 0 |
| `verify-dao.mjs` | `checks: 27, failed: 0` | 0 |
| `assembleDebug` | `BUILD SUCCESSFUL in 19s` | 0 |
| `git diff --check` | `no whitespace errors` | 0 |
| 合计 | `RESULT: VERIFY ALL PASS` | **0** |

计数结论：**101 单测 ✔、DAO 27 ✔、门禁 exit 0 ✔**；**「治理 22/22、0 警告」✘**——实测 21 passed + 1 warning：

```
[WARN] task/adr references  unwritten or misspelled: project/tasks/TASK-024.md -> TASK-025
```

该 warning 正是本任务 §12（`TASK-024.md:67`）提前引用尚不存在的 `TASK-025` 造成的；`TEST_REPORT.md:9` 却写「0 警告」。

### 4.2 作者声称的 4 组变异：复现结果

| 声明 | 实测 | 判定 |
|---|---|---|
| M1 日期格式 `yyyy-M-d` → 2 红 | 2 红：`ConvertersTest.localDateIsStoredAsIsoText`、`ConvertersTest.aStoredDateParsesBackToTheSameDay` | 数字对，**归因错**：`TASK-024.md:55` 写「转换器与往返用例变红」，但 `LogStatusRoundTripTest` 根本不经过转换器，两条红都在同一个文件里 |
| M2 `refresh()` 空实现 → 1 红 | 1 红：`TodayPresenterTest.crossingMidnightShowsTheNewDay` | ✔ |
| M3 `>=` → `>` → 11 红 | 11 红（`LogStatusRulesTest` 3、`LogStatusRoundTripTest` 2、`TodayPresenterTest` 5、其他 1） | ✔ |
| M4 `else if (day != today)` → `else` → 5 红（含差分） | 5 红，含 `randomisedHistoriesAgreeWithTheNaiveImplementation` | ✔ |

### 4.3 自造变异（本轮，逐组还原）

| # | 变异 | 文件 | 红 | 打红的测试 | 差分是否红 | 差分是否唯一防线 |
|---|---|---|---|---|---|---|
| M-A | `effectiveUntil` 改开区间（`!isAfter` → `isBefore`） | `StreakRules.kt:37` | 3 | 差分 + `StreakRulesTest.aScheduleChange…` + `TodayPresenterTest.everyScheduleVersionFeeds…` | 是 | 否 |
| M-B | `bitFor` 位移旋转（`value % 7`） | `ScheduleRules.kt:31` | 7 | 差分 + `ScheduleRulesTest` 2 + `StreakRulesTest` 3 + `CreateHabitPresenterTest` | 是 | 否 |
| M-C | today 规则去掉 | `StreakRules.kt:54` | 5 | 差分 + `StreakRulesTest` 2 + `LogStatusRoundTrip` + `TodayPresenterTest` | 是 | 否 |
| M-D | `firstDay` `minOf` → `maxOf` | `StreakRules.kt:32` | 3 | 差分 + 上述两条旧测试 | 是 | 否 |
| **M-E** | `WEEKDAYS_MASK` 31 → 30 | `ScheduleRules.kt:16` | **1** | 只有 `ScheduleRulesTest.weekdaysMaskIsMondayToFriday` | **否（全绿）** | — |
| M-G | 时间格式 `HH:mm:ss` → `HH:mm` | `Converters.kt:48` | 1 | 只有 `ConvertersTest.localTimeIsStoredWithSeconds` | — | 新增测试是唯一防线（这一条有价值） |
| **M-H** | `ON_RESUME` → `ON_CREATE`（即接线失效） | `AppRoot.kt:78` | **0** | 无：`OK (101 tests)` 全绿 | — | **接线零覆盖** |

结论：
1. 差分测试**能失败**（M-A/M-B/M-C/M-D 都红），但**在这 10 组变异里没有一次是唯一红的那条**——每条它抓到的缺陷，旧测试也抓到了。它目前的边际判别力是 0，代价是 3000 组随机历史。
2. 差分测试**看不见掩码常量**：M-E 改坏 `WEEKDAYS_MASK` 后它全绿。因为 oracle 直接拿 `ScheduleRules.WEEKDAYS_MASK` 当输入数据，实现和 oracle 一起错。这与 DATABASE.md 的 CHECK（`WEEKDAYS` 必须 31，`DATABASE.md:124`）是同一事实的三处副本，而唯一能抓的是那条老的单点测试。
3. **M-H 是最严重的一条**：把 `ON_RESUME` 换成 `ON_CREATE`（等于`回到前台不再刷新`），101 项全绿。本任务要补的「UI↔presenter 接缝」恰恰是唯一没有测试的地方，`crossingMidnightShowsTheNewDay` 只测了 presenter 的一个方法。

## 5. 文档与分支

- [x] 分支策略：`PARTIAL` 任务留在 `feat/seam-hardening`，没有进 `main`；
- [x] `README.md:12/15` 与 `PROJECT_STATUS.md:8/13` 的 `TASK-024 PARTIAL` / 101 项一致；`TASK-024` 自动摘要 `OK (101 tests)` 与实测一致。
- [ ] **`LAST_HANDOFF.md` 只改了一行，其余全是旧事实**（直接阻碍下一会话恢复）：
  - `:9` 当前分支写 `feat/today-queries（TASK-022 修复中）`、`main @ bfb3150` —— 实际是 `feat/seam-hardening`、`main @ 427e7cc`；
  - `:10` 最近 commit 写 `b73e03b` —— 实际 HEAD `3b58062`；
  - `:11` 会话状态写「下一条从 `TASK-022` 开始」—— `TASK-022` 早已 `APPROVE` 并入 `main`，与同一文件 `:8` 自相矛盾；
  - `:29` 验证表仍写 `OK (91 tests)` —— 本任务产出 101 项，交接里查不到；
  - `:56` Next Safe Action 仍要求「`TASK-022` 按第 1 轮阻断修复后复评」。
  治理检查的 `handoff snapshot filled` 只校验字段非空，抓不到这种「字段在、值是旧的」。
- [ ] **`TEST_REPORT.md` 头部未同步**：`:7` 仍是 `分支 / commit：feat/today-queries（TASK-022，基线 main @ bfb3150）`，而 `:83` 新增的却是 `TASK-024` 在 `feat/seam-hardening` 上的运行；`:9` 的「0 警告」与实测 1 warning 冲突。
- [ ] **`TASK-024.md` 内部自相矛盾**：§6 五条验收（`:31-35`）全部 `[ ]` 未勾，§11（`:62`）却写 `[x] 验收标准满足（5/5）`，自动摘要（`:79`）写「通过 0 项 / 未通过 5 项」。机器可读的结论与人写的结论相反。
- [ ] **`TASK-024.md:14`/`:46` 引用的 `project/reports/EXTERNAL_REVIEW_ANSWERS.md` 在仓库里不存在**（`project/reports/` 只有 `BASELINE_REVIEW`、`RELEASE_REPORT`、`DEVICE_DUMP-2026-11-05`、`EXTERNAL_REVIEW_BRIEF`、`TEST_REPORT`）。整个任务的外部依据不可核对——这正是 `LAST_HANDOFF.md:52` 自己总结过的「声称必须可核对」。
- [ ] `TASK-024.md:48` 的「差分测试首次运行即通过」是不可核对的自我报告（无日志、无历史提交），只能记为 `UNKNOWN`。

## 6. 结论

- 结论：`REJECT`
- 证据：门禁 `VERIFY ALL PASS` 且 `OK (101 tests)` / `checks: 27`（exit 0）；4 组声明变异中 M2/M3/M4 复现一致、M1 数字对但归因错；7 组自造变异中 M-E（掩码常量）与 M-H（ON_RESUME 接线）暴露真实盲区；文档三处未同步、一处悬空引用、一处自相矛盾。
- 需修复项（按严重度编号）：

1. **新增的 `ON_RESUME` 接线零测试覆盖（M-H：改成 `ON_CREATE` 后 101 项全绿）**。`TASK-024` §6 第 3 条「有测试」只对 `TodayPresenter.refresh()` 成立，对真正让翻日生效的那一段（`AppRoot.kt:76-82`）不成立；本任务要补的正是这种接缝。最小修法（与 `TASK-013` 的先例一致）：把「哪个 lifecycle 事件该触发刷新」抽成纯函数（如 `TodayRefresh.shouldRebuild(event)`）并在 JVM 单测里断言 `ON_RESUME→true`、`ON_CREATE/ON_START→false`、以及 `AppRoot` 确实调用它；做不到就在 §11 与 `TEST_REPORT` 的 Known Gaps 里明确写「UI 接线未覆盖」，不要把「有测试」写成已满足。

2. **翻日只覆盖「后台→前台」，前台跨午夜仍然显示昨天**。`AppRoot.kt:72-74` 的注释与 `TodayPresenter.refresh()` 的 KDoc（`TodayPresenter.kt:75-79`）都声称修好了「app left open across midnight」，但代码只在 `ON_RESUME` 触发：用户在前台看着今日页跨 00:00、或停在别的 tab 跨 00:00 再切回来、或手动改时间/时区（`ACTION_TIMEZONE_CHANGED`/`TIME_SET` 都不会产生 `ON_RESUME`），都仍是旧日期。最小修法：在今日页加一个「睡到下一个本地午夜再 `refresh()`」的 `LaunchedEffect`（`delay` 到次日 00:00:01），保留 `ON_RESUME` 作为兜底；或者把注释/KDoc 改成「后台返回时刷新」，并在 Known Gaps 登记前台跨午夜与时区变更未覆盖。

3. **`LAST_HANDOFF.md` 未同步**（`:9` 分支/main、`:10` 最近 commit、`:11` 会话状态、`:29` `OK (91 tests)`、`:56` Next Safe Action 全部停留在 `TASK-022`）。按 `AGENTS.md` §6/§10 重写 Snapshot、Verification、Next Safe Action，使 101 项、`feat/seam-hardening` @ `3b58062`、`TASK-024` 待评审成为可直接恢复的状态。

4. **`TEST_REPORT.md` 头部与警告数不实**：`:7` 的分支/commit 仍是 `feat/today-queries / TASK-022 / bfb3150`（本轮运行在 `feat/seam-hardening` @ `3b58062`）；`:9` 的「0 警告」应改为实测的「22 项检查、21 passed、1 warning」，并说明该 warning 是 `TASK-024` 引用尚未创建的 `TASK-025`（要么创建 `TASK-025`，要么把 §12 改成不产生悬空引用的写法）。

5. **`TASK-024.md` 记录自相矛盾且含错误归因**：①§6 五条验收 `[ ]` 与 §11 `[x] 5/5`、自动摘要「通过 0 项 / 未通过 5 项」三者取一，按真实状态同步；②§5 Expected Files 漏 `navigation/AppRoot.kt`、多列未改动的 `TodayScreen.kt`；③`:55` 的 M1 归因改为「两条都在 `ConvertersTest`（`localDateIsStoredAsIsoText`、`aStoredDateParsesBackToTheSameDay`），`LogStatusRoundTripTest` 不经过转换器」；④`:14`/`:46` 引用的 `EXTERNAL_REVIEW_ANSWERS.md` 不存在，改为真实存在的 `EXTERNAL_REVIEW_BRIEF.md` 或注明「答复仅在会话中、不入库」；⑤`:48` 的「首次运行即通过」无法核对，要么附日志，要么写「无独立记录」。

6. **差分测试的独立性被夸大，且已实测出盲区**：oracle 的版本覆盖谓词与 `StreakRules` 逐字相同（`StreakRulesDifferentialTest.kt:36-40` vs `StreakRules.kt:36-38`），掩码直接复用 `ScheduleRules` 的常量（`:80,82`）；因此它只对「实现单边漂移」有判别力，对「实现与 oracle 共同误读规格」无判别力——而后者正是引入 oracle 的理由。实测 `WEEKDAYS_MASK` 31→30 时差分全绿（M-E），且 10 组变异中它从未是唯一红的那条。最小修法：①掩码用字面量（`127`/`31`/`random 1..127`）而非常量，让「常量被改」这一侧至少有一处独立副本；②把版本覆盖谓词按 DATABASE.md 4.2 的文字重写成不同形状（例如先算 `[from, until]` 的闭区间再 `contains`，或把 `until == null` 展开成两个显式分支），避免逐字复制；③在 §8 把「不共享判定代码」降级为「遍历与读数逻辑独立，版本覆盖谓词与掩码常量仍共享」，并把「M-E / M-H 打不中」写进 Risks。若不做 ①②，则本任务不能声称拿到了「第一个独立 oracle」。

### 6.1 非阻断建议（本轮已实测或已定位）

- `TodayPresenter.refresh()`（`TodayPresenter.kt:80-82`）没有 `record()` 那样的 `runCatching`：查询抛异常时协程异常会冒到 ViewModel scope，等于新增了一条「回到前台就崩」的路径；同时它与 Flow collector 并发写 `_state`（`habits` 是无同步的普通 `var`），后完成的旧快照可以覆盖新快照。建议 `runCatching` + 失败走 `_lastError`，并考虑用 `conflate`/序号避免旧覆盖新。另外每次 `ON_RESUME` 都会做 4 次批量查询 + 全量重算，与 `LAST_HANDOFF.md:42-43` 已记的「2 万行/次」叠加。
- `ConvertersTest.kt:24-28` 三条断言逐字相同（`"2026-01-05"` × 3），注释说「Zero padding matters」但第三条与前两条是同一个用例；真正的边界（`2024-02-29`、`9999-12-31`、`stringToLocalDate("2026-1-5")` 应拒绝、`stringToLocalTime(null)`）一个都没测。`stringToLocalTime` 的 null 分支缺断言。
- `ConvertersTest` 只能证明「转换器与自己一致」：它没有、也无法证明 Room 真的把该列绑到这个转换器（`Converters` 是 `@TypeConverter` 且 schema 已导出，契约成立，但没有运行时证据）。它唯一钉住的外部事实是日期文本形状；时间格式反而与 `verify-dao.mjs:197` 的 `"08:00"` 相反。
- `LogStatusRoundTripTest` 名不副实：三次 `LogStatusRules.evaluate` 全在内存里，没有经过 `RecordHabitProgressUseCase`/`RoomHabitRepository`/`Converters`，所以它抓不到「状态变了但 `completed_at` 没清」「写库后读回不一致」这类真正的往返缺陷；第三条断言（`current = 2`）成立的原因是「今天未完成不算断」，与往返无关。建议至少让序列走 `RecordHabitProgressUseCase`，或改名为 `LogStatusSequenceTest`。
- 差分测试的输入分布比实现契约窄：版本永远连续相邻（`randomSchedules` 里 `cursor = until + 1`），因此「两个版本重叠」「版本之间有空档」永远不出现；`effectiveFrom > today` 的版本分支（`StreakRules.kt:33`）与 `versions.filter { !from.isAfter(today) }`（`StreakRulesDifferentialTest.kt:96`）都不可达；`completed` 里没有晚于 `today` 的日期。`assertEquals(true, checked > 2000)`（`:126`）弱于 `assertEquals(3000, checked)`，无法分辨 3000 与 2001。
- `AppRoot.kt:75` 的 `androidx.compose.ui.platform.LocalLifecycleOwner` 已废弃（编译实测 `w: …:75:38 … is deprecated`）。替换需要 `lifecycle-runtime-compose` 依赖（未声明、需批准），因此本轮不阻断，但应在任务里记一句「沿用废弃 API 的理由」，否则下一轮还会被当成新发现。
