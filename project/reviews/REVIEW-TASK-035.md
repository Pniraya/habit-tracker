# REVIEW-TASK-035: 独立评审记录

- 审查任务：`TASK-035`（日切点 04:00 与设计偏差登记）
- 审查 commit：`341a8995377e7c425af673b6aa73fa61a88c7ef0` + 本轮未提交的工作区改动（截至 2026-11-09，`git diff --numstat`：`ProfileScreen.kt` +4/−2、`ADR-016` +8/−1）
- 审查范围：任务交付至今的累计代码。起点 `dc7e37c`（`TASK-035` 的 merge，18 文件 +344/−99），并核对之后所有涉及「今天/日期」的改动（`TASK-030` 备份、`TASK-039` 导入）。日期相关全量 grep 覆盖 `app/src/main`、`app/src/test`、`DATABASE.md`、`ARCHITECTURE.md`、`project/REQUIREMENTS.md`。
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-09`
- 轮次：第 2 轮。第 1 轮针对同一 commit 判 `REJECT`，阻塞项为（1）`ProfileScreen.kt:146` 仍用 `LocalDate.now()` 作业务日期、（2）`DATABASE.md §4.3`/`ADR-002` 的「自然日」与 `ADR-016` 的「逻辑日」并存且 `Supersedes` 写「无」。本轮复审这两项的修复，结论见 §7。

评审时工作区：HEAD 仍为 `341a899`，修复未提交（`M CustomIcons.kt`、`M ProfileScreen.kt`、`M ADR-016`、`M TASK-037.md` 及 4 个他人文档改动）；未触碰任何文件，只更新本评审文件。`git diff --check` exit **0**。按评审指令**未运行** `gradlew` / `verify-all` / `connectedAndroidTest`；主会话报告的 `assembleDebug` SUCCESSFUL 与 `run-unit-tests` OK (177 tests) **非本轮实测**。

## 1. Scope

**裁决：通过。**

- 本轮改动严格落在第 1 轮两个阻塞项上：`ProfileScreen.kt`（+4/−2，换日期来源）、`ADR-016`（+8/−1，补 `Supersedes`）。没有顺带改架构、依赖、权限、schema 或无关文件。
- `DayCutoff.kt` 与四个 Presenter 在本轮**未改动**（`git diff --name-only` 只有上述两文件属本任务），第 1 轮确认的「单点实现 + 五个入口一致」仍然成立，不需要重新推导。
- 无新增依赖、无权限、无受保护基线文件改动：`DATABASE.md`/`ADR-002` 按 `ADR-004` 保护清单保持原样，改由 `ADR-016` 的 `Supersedes` 收窄措辞（这正是第 1 轮给的备选方案）。

## 2. Diff 核对

**裁决：通过。**

- 修复 1（`feature/profile/ProfileScreen.kt`）：
  - `:42-46` 删除 `import java.time.LocalDate`，新增 `import com.habittracker.domain.calculation.DayCutoff`；
  - `:148` `BackupExporter.fileName(DayCutoff.today().toString())`，并在 `:146-147` 写明理由；
  - 全量 grep `LocalDate\.now`（`app/src/main` 递归）→ **0 命中**；`ProfileScreen.kt` 内 grep `LocalDate` → 0 命中（`:45` 是 `DayCutoff` 的 import，`:148` 是唯一使用点），因此不存在「删 import 但余留引用」的编译隐患（主会话亦报 `assembleDebug` SUCCESSFUL）。
- 修复 2（`project/decisions/ADR-016-day-cutoff-four-am.md:34-42`）：
  - 明确写出「本 ADR 收窄以下两处措辞，基线文件本身不改（`ADR-004` 保护清单）」；
  - 点名 `DATABASE.md` §4.3 的 `log_date`「自然日」→「逻辑日」，以及 `ADR-002` 第 12 行同类表述；
  - 保留与 `ADR-005` 的衔接，并登记「该偏差由 `TASK-035` 的实现与 `TASK-035` 评审（2026-11-09）发现」。
  - 这是合法的 ADR 后置收窄：既不越过 `ADR-004` 改基线，又让唯一登记处（ADR）具备可检索的「谁改了哪句」。
- `git diff --check` exit 0；无冲突标记；无空白错误。

## 3. 架构与数据

**裁决：通过。**

- 「一条日志代表哪一天」现在只有一个权威答案：`ADR-016:13-15`（`log_date` = 逻辑日）+ `:34-42`（点名收窄 `DATABASE.md §4.3` 与 `ADR-002` 的「自然日」措辞）。基线文件按 `ADR-004` 不动，读者按 §11 的优先级（最新批准决定 > 基线文字）得到唯一解释，第 1 轮指出的「两个事实来源」消失。
- 分层与数据约束不变：`DayCutoff` 仍在 `domain/calculation`（纯 `java.time`），Presentation 通过注入的 `today: () -> LocalDate` 取日期；数据库列、可空性、索引、触发器均未触碰。
- 备份文件名改用逻辑日后，导出文件名的日期与文件内 `log_date`/`created_on`/`archived_on` 同源（00:00–03:59 不再出现「文件名明天、数据昨天」）。

## 4. 验证证据

**裁决：通过（第 1 轮不成立的验收项已成立；本轮代码仅有构建证据）。**

| # | 验收标准（TASK-035 §6） | 本轮核对 | 结论 |
|---|---|---|---|
| 1 | 03:59:59 属前一天，04:00:00 属新一天（单测） | `DayCutoffTest.kt:14-24`；第 1 轮用等价实现复算的 5 个边界值（`11-06T03:59:59→11-05`、`04:00:00→11-06`、`00:00/01:00→11-05`、`2028-02-29T03:59→02-28`）本轮代码未变，结论沿用 | 通过 |
| 2 | 「今天」只有一个来源；业务代码不再用 `LocalDate.now()` 作日期 | `app/src/main` 全量 grep `LocalDate.now` → **0 命中**；`ProfileScreen.kt:148` 已改用 `DayCutoff.today()` | **本轮成立** |
| 3 | 前台跨日刷新在 04:00 后触发 | `AppRoot.kt:160/164-167` + `TodayPresenter.kt:218-219`；`TodayPresenterTest.kt:77-87` 的 4 条断言（第 1 轮复算一致），本轮未改动 | 通过 |
| 4 | 统计页没有不可点击的周/年控件 | `StatisticsScreen.kt:77-79` 静态「本月」，`PeriodSwitch` 已删，无残留定义 | 通过 |
| 5 | ADR-016/017 与 DESIGN-DELTA 已提交，偏差有唯一登记处 | `ADR-016:34-42` 现明确列出被收窄的两处措辞与发现来源；`DESIGN-DELTA.md:13`、`ADR-017` 仍在 | 通过 |
| 6 | 单测 159 项通过 | 任务记录 `TEST_REPORT.md:111`；当前全仓为 177 项（`TEST_REPORT.md:9/127`，主会话实测），本轮仅改一行调用与 ADR 文本，不影响计数 | 记录一致 |
| 7 | 真机安装后日期/打卡/统计不回归 | `TEST_REPORT.md:108`；04:00 真实跨日缺口仍登记在 `TEST_REPORT.md:141`/`ADR-016:32`。本轮 `ProfileScreen` 一行改动未做真机复测 | 仅记录，未复核 |

## 5. 文档与分支

**裁决：部分通过（有一项 DONE 前置条件，不阻塞本轮结论）。**

- 一致：`PROJECT_STATUS.md:32-33`/`:68`、`README.md:26`、`FEATURE_MATRIX.md:59-60`、`DESIGN-DELTA.md:13`、`ADR-016` 全链路口径统一；`ADR-016:36` 的「按 `ADR-004` 不改基线」与 `AGENTS.md` §11 一致。
- 未落地：第 1 轮要求「改完在 `TASK-035` §10 增加一行『全仓 grep `LocalDate.now` = 0』的可复核证据」，`project/tasks/TASK-035.md` 本轮**仍未改动**（`git diff` 无该文件），§13 也还是 `PENDING`。这是一条记录缺项，不是技术缺陷（该 grep 证据已写在本评审 §4），但转 `DONE` 前必须补上，否则任务记录与评审证据脱节。
- 分支与提交：修复在工作区未提交，仍与 `TASK-036`/`038`/`039` 的文档改动同树；建议本任务单独收一个聚焦 commit（`ProfileScreen` + `ADR-016` + `TASK-035.md`），避免与其它任务的未提交改动混在一起。

## 6. 第 1 轮阻塞项闭合情况

| 第 1 轮阻塞项 | 本轮证据 | 状态 |
|---|---|---|
| `ProfileScreen.kt:146` 用 `LocalDate.now()` 命名备份文件，00:00–03:59 与逻辑日差一天 | `ProfileScreen.kt:45` 换 import、`:148` 改 `DayCutoff.today()`；全仓 `LocalDate.now` 0 命中；主会话 `assembleDebug` SUCCESSFUL（我未复跑） | **已闭合** |
| 「自然日」与「逻辑日」并存，`ADR-016:34` `Supersedes` 写「无」 | `ADR-016:34-42` 点名 `DATABASE.md §4.3` 与 `ADR-002` 第 12 行，声明按 `ADR-004` 不改基线，并登记发现来源 | **已闭合** |

第 1 轮的非阻塞建议中，第 1 条（`today()` 默认值无测试固定）、第 2 条（`logicalDateOf` 死 API）、第 3 条（统计/详情页停 04:00 不自刷新）本轮未处理，仍作为后续建议保留。

## 7. 结论

- 结论：`APPROVE`
- 证据（本轮实际命令与结果）：
  - `git log -1 --format=%h` → `341a899`；`git status --short` → 2 个代码文件 + `ADR-016` + `TASK-037.md` 及其它任务的文档改动（未触碰）
  - `git diff --numstat` → `CustomIcons.kt` +35/−6、`ProfileScreen.kt` +4/−2、`ADR-016` +8/−1、`TASK-037.md` +7/−2
  - grep `LocalDate\.now`（`app/src/main` 递归）→ **0 命中**；grep `LocalDate|DayCutoff`（`ProfileScreen.kt`）→ `:45` import、`:148` 使用，无残留
  - `git diff --check` → exit **0**
  - 未运行：`gradlew`、`tools/run-unit-tests.ps1`、`tools/verify-all.ps1`、`connectedAndroidTest`（按评审指令交由主会话统一执行）
- 结论说明：两项阻塞都按最小改动闭合——日期来源收口到 `DayCutoff.today()` 且全仓再无业务日期用 `LocalDate.now()`；日期语义的冲突用「`ADR-016` 收窄措辞、基线按 `ADR-004` 不动」的方式解决，既没违反保护清单，也没留下第二个事实来源。核心日切逻辑（单点实现、五个入口、00:00/01:00/03:59:59/04:00 边界、04:00 定时器相位）在第 1 轮已被读码 + 复算确认，本轮未改动。

### 需修复项

无（两项阻塞均已闭合）。

### 转 `DONE` 前置条件（非技术阻塞）

1. 在 `project/tasks/TASK-035.md` §10 补一行本轮修复的可复核证据（`全仓 grep LocalDate.now = 0`、`ProfileScreen.kt:148`），§13 填本轮 `APPROVE` 与评审文件路径，状态由 `IMPLEMENTED` 改为 `DONE` 时同步 `PROJECT_STATUS.md:68` 的待批审清单（`check-governance` 第 18 项双向校验）。
2. 建议 `ProfileScreen.kt` + `ADR-016` + `TASK-035.md` 单独一个聚焦 commit，别和其它任务的未提交文档改动一起提交。

### 非阻塞建议（沿用第 1 轮，未处理）

1. 四个 Presenter 的 `today()` 默认值（`TodayPresenter.kt:38` 等）无测试固定，回归时可被无痛改回 `LocalDate.now()`；建议加一条守卫测试或 grep 门禁。
2. `DayCutoff.logicalDateOf`（`DayCutoff.kt:26`）主代码零调用，属未使用 API：用起来或删掉。
3. 统计页/详情页只在 `load()` 时取一次 `today()`，只有今日页有 04:00 定时器（`AppRoot.kt:157-169`）；前台停在这两页跨 04:00 会停在旧日，建议补进已知缺口。
4. `ADR-016:17` 仍引用 `DATABASE.md` 的「日期是本地日历日」作为「一致」的依据，与新的 `:38` 收窄表述并列时读起来像自相矛盾；措辞可统一为「与 `DATABASE.md` 的日期冻结口径一致」。
