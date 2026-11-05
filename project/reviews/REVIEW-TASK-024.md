# REVIEW-TASK-024: 独立评审记录（第 3 轮）

- 审查任务：`TASK-024`（接缝补洞：转换器、差分、翻日、往返）
- 审查 commit：`11facff6ba5ba487522a98e3806609a7e1444aa7`（分支 `feat/seam-hardening`，基线 `main` @ `427e7cc0345b83425ef39b12f34a9991798eda62`；第 1 轮 `3b58062`、第 2 轮 `f700d4b`）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现；只读核对 + 自造变异）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`

评审开始 `git status --short` 为空。4 组变异串行执行，逐组 `git checkout -- <path>` 还原并当场复查；结束时工作区只剩本评审文件。门禁与变异**串行**，未与任何 Gradle 任务并发。

## 1. Scope

- [x] 本轮改动（`f700d4b..11facff`，10 文件）与第 2 轮 5 条阻断逐条对应：午夜算术抽出并单测、证据按 HEAD 重测、进入今日页也刷新、交接重写、外部答复补来源。
- [x] 无新依赖、无权限、无 Gradle module、无 schema/迁移改动；生产改动只有 `TodayPresenter.millisUntilNextMidnight`（纯函数）与 `AppRoot` 的 6 行接线。
- [x] `TASK-023` 承接「计划版本区间不得重叠」的不变量（不改 `DATABASE.md` 冻结基线）是正确处置；若之后要改基线，必须走 ADR，本轮不要求。
- [ ] `TASK-024.md:63` 的 verify-all 证据行仍写「单测 104」。

## 2. Diff 核对

- [x] `git diff --check` 干净；无冲突标记；无删除测试（105 = 104 + 1）。
- [x] `millisUntilNextMidnight(now: LocalDateTime): Long` 是纯函数：不读时钟、不碰 Android，`Duration.between(now, 次日 atStartOfDay())` 无时区依赖，结果与设备时区无关（只随墙上时钟变化）。
- [x] Composable 只剩「睡到点 + 刷新」，算术在 presenter 里——与 `TASK-013`/`refreshesOn` 的处置一致。
- [x] 进入今日页的补刷（`AppRoot` 的 `if (state.value.date != LocalDate.now())`）逻辑正确：首次组合、进程重建、后台返回、跨日切回四种情况都不会误刷或漏刷。
- [ ] 该补刷比较是 UI 接线，无测试（见 §6.2，非阻断）。

## 3. 架构与数据

- [x] 无 schema 变更；存储格式四处（`DATABASE.md` 2、`Converters`、`verify-dao.mjs` 夹具、`ConvertersTest`）仍一致。
- [x] `TASK-024` §8 现在准确描述差分 oracle 的定位：只抓单边漂移；`WEEKDAYS_MASK` 变异会红但原因是 `require(mask == weekdayMask)` 抛异常，不是 oracle 判别。§10 M4 的措辞也与 §8 一致了。
- [x] `EXTERNAL_REVIEW_ANSWERS.md` 补了来源块：逐字粘贴、作者/模型版本/会话 id 未保留、仓库内无法独立核实、只能作设计参考、不能当作「独立第三方已验证」。这句话把该文件的证据地位降到了正确的高度——第 1、2 轮关于「不可核对的外部依据」的阻断到此真正闭合。

## 4. 验证证据

### 4.1 门禁（`pwsh -NoProfile -File tools/verify-all.ps1`，串行）

| 步骤 | 实测 | 退出码 |
|---|---|---|
| `check-governance.ps1` | `SUMMARY: 22 checks, 22 passed, 0 warnings, 0 failed` | 0 |
| `run-unit-tests.ps1` | `OK (105 tests)` | 0 |
| `verify-dao.mjs` | `checks: 27, failed: 0` | 0 |
| `assembleDebug` | `BUILD SUCCESSFUL in 19s` | 0 |
| `git diff --check` | `no whitespace errors` | 0 |
| 合计 | `RESULT: VERIFY ALL PASS` | **0** |

三个数字全部复现：**105 单测、27 DAO、治理 22/22 · 0 警告、exit 0**。

### 4.2 本轮变异（HEAD 实测，逐组还原）

| # | 变异 | 位置 | 红 | 打红的测试 | 与作者声明 |
|---|---|---|---|---|---|
| R3-MH2 | 午夜算术 `plusDays(1)` → `plusDays(2)` | `TodayPresenter.millisUntilNextMidnight` | **1** | `theMidnightTimerAimsAtTheNextLocalMidnight` | 一致；该变异在抽出前是 105 全绿 |
| R3-M1 | 日期格式 `yyyy-M-d` | `Converters:18` | **3** | `localDateIsStoredAsIsoText`、`aStoredDateParsesBackToTheSameDay`、`leapDaysSurviveTheRoundTrip` | 一致（已从过期的 2 修正为 3） |
| R3-ME | `WEEKDAYS_MASK` 31→30 | `ScheduleRules:16` | **2** | `weekdaysMaskIsMondayToFriday` + 差分（`require` 抛异常） | 一致（§8 现已写明红的原因不是 oracle 判别） |
| R3-ENTRY | 进入页补刷条件 `!=` 取反 | `AppRoot` | **0** | 无，`OK (105 tests)` | UI 接线，见 §6.2（非阻断） |

新增的 4 条断言（23:59:59→1000、00:00→86_400_000、闰日 12h、年末 1h）确实钉住了算术：`plusDays(2)`、`atStartOfDay()→atTime(1,0)` 一类改动都会被至少一条打红，且断言不依赖设备时区（`Duration.between(LocalDateTime, LocalDateTime)` 是墙上时钟差）。

## 5. 文档与分支

- [x] `README.md:15` / `PROJECT_STATUS.md:13` / `TEST_REPORT.md:9` 与 Runs 末行 / `TASK-024` 自动摘要：**105 项**一致；DAO 27/27、治理 22 项一致；`TEST_REPORT` 的变异汇总（M1=3、M2=1、M3=11、M4=5、ME=2、MH=1、MH2=1）与我的实测逐一相符。
- [x] `TEST_REPORT.md:7` 头部是 `feat/seam-hardening`（`TASK-024`，基线 `main` @ `427e7cc`）。
- [x] `TASK-024` §5/§6/§8/§11 与自动摘要一致；§10 的 M1（3 项）、M4（不是唯一红线）、ME（require 抛异常）、MH2 四行都与实测一致。
- [x] `LAST_HANDOFF.md:10` 改为「以 `git log -1 --oneline` 为准」，避免再次被「写死哈希」钉死；`:31` 改为 22/22 · 0 警告并说明 TASK-025 消除了第 1 轮 warning；`:56` 改为等待第 3 轮复评。
- [ ] **`LAST_HANDOFF.md:9`/`:11` 仍是第 1 轮口径**：`:9` 写「`TASK-024` 第 1 轮 `REJECT` 后修复中，旧 HEAD `3b58062`」（实际已到 `11facff`，且第 2 轮阻断已修）；`:11` 写「正在修 6 条阻断」，与同一文件 `:56` 的「第 2 轮 5 条已修，等待第 3 轮复评」直接冲突。下一会话按 Snapshot 恢复会重做第 1 轮的活。
- [ ] **`TASK-024.md:63` 的 verify-all 行仍写「单测 104」**（其余 8 处都是 105）。本轮的主题就是「证据按 HEAD 重测」，遗漏的恰是汇总行；同行的「构建 ~25s」实测为 19–20s（量级无妨，可一并改）。
- [ ] `TASK-024` §9（Implementation Log）没有第 2/3 轮的条目，只有 §10 证据行；不属于硬性要求，但与「Implementation Log 记录实际过程（含失败与重试）」的模板有落差。

## 6. 结论

- 结论：`REJECT`
- 证据：门禁 `VERIFY ALL PASS`（exit 0，`22/22 · 0 warnings`、`OK (105 tests)`、`checks: 27`）；第 2 轮 5 条阻断已关闭（R3-MH2 打红新测试、M1=3/ME=2 与重测值一致、进入页补刷已加、交接与外部答复已改）；**剩余仅 2 处文档事实错误**，各一行，不涉及代码或测试。
- 需修复项（按严重度编号）：

1. **`TASK-024.md:63` 证据行写「单测 104」**：本轮其余八处（`README:15`、`PROJECT_STATUS:13`、`TEST_REPORT:9`、Runs 末行、§6、§10 首行、§11、自动摘要）都是 105，只有 verify-all 汇总行没跟上。改成 105（顺带把「构建 ~25s」改成实测的 19–20s）。这是本任务第 2 轮自己立的规矩——「证据必须按 HEAD 重测」——的最后一次违反。

2. **`LAST_HANDOFF.md:9` 与 `:11` 仍是第 1 轮口径**：`:9` 的「第 1 轮 `REJECT` 后修复中，旧 HEAD `3b58062`」应改为「第 2 轮 5 条阻断已修，HEAD `11facff`，等待第 3 轮复评」；`:11` 的「正在修 6 条阻断」与 `:56` 冲突，应统一为第 2 轮之后的真实状态。`LAST_HANDOFF` 是下一会话唯一入口，Snapshot 与 Next Safe Action 必须说同一件事。

两条都不需要重跑测试或变异：改完即可提交，第 4 轮只需核对这两行与门禁。

### 6.1 三轮阻断的关闭情况

| 轮次/阻断 | 状态 | 证据 |
|---|---|---|
| R1-1 `ON_RESUME` 接线零覆盖 | 关闭 | R2-MH 1 红；`refreshesOn` 单测 |
| R1-2 翻日只覆盖后台→前台 | 关闭 | 午夜定时器 + 进入页补刷；R3-MH2 打红 |
| R1-3 `LAST_HANDOFF` 未同步 | **本轮仍未关闭**（换了新的过期点） | 见阻断 2 |
| R1-4 `TEST_REPORT` 头部/警告数 | 关闭 | `:7`、治理实测 22/0 |
| R1-5 `TASK-024` 自相矛盾/归因/悬空引用 | 关闭（除 §10 汇总行的 104） | §5/§6/§8/§11 一致；外部答复已存在且已标注可信度 |
| R1-6 差分独立性被夸大 | 关闭 | §8 已写明「只抓单边漂移」「红来自 require 守卫」 |
| R2-1 午夜算术零覆盖 | 关闭 | R3-MH2 1 红 |
| R2-2 证据未按 HEAD 重测 | **大部分关闭**，剩 §10 汇总行 | 见阻断 1 |
| R2-3 前台跨 0 点仍有洞 | 关闭 | `AppRoot` 进入页比较 + 定时器 |
| R2-4 `LAST_HANDOFF` 提交前快照 | **部分关闭**（`:9`/`:11` 未改） | 见阻断 2 |
| R2-5 外部答复无出处 | 关闭 | 来源块明确「无法独立核实、只能作设计参考」 |

### 6.2 非阻断（记录，不再要求）

- **进入页补刷比较无测试**（R3-ENTRY：把 `!=` 取反后 105 全绿）。它与 `AppRoot` 的 `event.name` 传参同属已声明的 UI 接线缺口；若将来再动这块，可照样抽一个 `shouldRefreshOnEntry(stateDate, today)` 纯函数，但本轮不要求——代码本身按四种进入路径逐条核对是正确的。
- **墙上时钟 Duration 的 DST 边界**：`millisUntilNextMidnight` 用 `LocalDateTime` 差，跨夏令时切换日会多睡/少睡最多 1 小时（提前刷或晚 1 小时刷，仍落在同一天，不会显示错日期）。目标用户时区 `Asia/Shanghai` 无夏令时；若要严谨，可在 §8 的「未覆盖」里加一句「夏令时切换日定时器可偏差 ≤1h」。
- `refresh()` 与 Flow collector 的 last-writer-wins、`LogStatusRoundTripTest` 的内存内序列、差分生成器不含重叠/未来版本：三项已声明；「版本区间不得重叠」已由 `TASK-023` 承接，不再要求本任务处理。
