# REVIEW-TASK-039: 独立评审记录（第 4 轮）

- 审查任务：`TASK-039`
- 审查 commit：`4d3b5c95410e846aa44ea6f3f5549cb0957ba6ac`（HEAD；前序 `fb767de` 交付、`59f70a3` 第 1 轮修复、`8d4312d` 第 2/3 轮文档）
- 审查范围：任务交付至今的累计代码（`fb767de`..`4d3b5c9`）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-09`
- 轮次：第 4 轮。历史：第 1 轮 `REJECT`（校验/区间/文档）→ 第 2 轮 `REJECT`（`PROJECT_STATUS.md:13`/`:70`、`ADR-019` 表述）→ 第 3 轮 `REJECT`（`README.md:21`/`:26`、`PROJECT_STATUS.md:8`/`:21`）。
- 评审时工作区：写评审时并行会话正在跑 `TASK-030` 的收尾（`git status` 显示 `README.md`、`project/PROJECT_STATUS.md`、`project/tasks/TASK-030.md` 未提交修改）。本评审以 **commit `4d3b5c9`** 为准，并用 `git show HEAD:` 读取已提交内容；工作区那部分见 §5.1 的注意事项。

## 1. Scope

**裁决：通过。**

- `4d3b5c9` 只改文档：`.gitignore`（+2）、`README.md`、`project/PROJECT_STATUS.md`、`project/reports/RELEASE_REPORT.md`；无 `app/**` 代码与测试改动。
- 与第 3 轮的 4 项要求一一对应，并额外处理了 3 条非阻塞建议（`.gitignore`、构建产物尺寸、发布证据）。

## 2. Diff 核对

**裁决：通过。**

- 治理检查「no conflict markers」「no trailing whitespace」通过（`check-governance.ps1` 22/22，exit 0）。
- 未删除或绕过测试；单测的 Test 注解静态计数仍为 **182**。
- 说明（非阻塞）：提交信息说 `8d4312d`「只含 build.gradle.kts + README 发布小节」，实际 `git show --stat 8d4312d` 仍是 5 个文件（另含 `PROJECT_STATUS.md`、`ADR-019`、`TASK-039.md`）。历史没有被改写（提交哈希未变，这点正确），只是这句描述不准确；不影响本轮裁定。

## 3. 架构与数据

**裁决：通过（代码自 `59f70a3` 起未变）。**

- 第 2 轮核对过的解析期校验（`BackupParser.kt:242-246`、`:262-277`、`:279-312`、`:314-332`、`:378-381`）与流式 8 MB 判断本次未改，结论继续成立。
- 第 2 轮 4 条残余缺口已在 `TASK-039.md` §14 登记为 Known Gaps，本轮不修，属已声明项。

## 4. 验证证据

**裁决：通过。**

| 项目 | 本轮实测 | 结论 |
|---|---|---|
| `check-governance.ps1` | 22 checks / 22 passed / 0 warnings / 0 failed，exit **0** | 与任务记录一致 |
| `verify-dao.mjs` / `verify-schema.mjs` | 45/45、40/40（第 3 轮实测，代码未变） | 与文档一致 |
| Test 注解静态计数 | **182** | 与 `README.md:24`/`:71` 一致 |
| 构建产物 | `app/build/outputs/apk/debug/app-debug.apk` = **11,237,263 B**、`release/app-release.apk` = **7,668,838 B**（2026-11-09 19:13 / 19:22） | 与 `README.md:23`、`PROJECT_STATUS.md:11` 的两个数字**逐字节一致** |
| `.gitignore` | `*.jks`、`*.keystore`、`*.p12`、`*.bks`、`keystore.properties` | 非阻塞建议已落实 |
| `gradlew assembleDebug/assembleRelease` / 单测执行 | **本轮未跑**（主会话要求不跑 gradlew） | 采信 `RELEASE_REPORT.md` 与 `TEST_REPORT.md:133` |

## 5. 文档同步

**裁决：通过（第 3 轮 4 项全部闭合，按已提交的 `4d3b5c9` 核对）。**

| 第 3 轮要求 | `git show HEAD:` 实测 | 状态 |
|---|---|---|
| `README.md:21` 当前任务改回 `TASK-040` | `TASK-040`（描述「机型适配」），名实一致 | 已闭合 |
| `PROJECT_STATUS.md:8` 当前任务改回 `TASK-040` | `TASK-040`（描述「机型适配」） | 已闭合 |
| `PROJECT_STATUS.md:21` ADR 范围到 019 | `ADR-001`~`ADR-019`，括注含「`ADR-019` 覆盖式备份导入」 | 已闭合 |
| `README.md:26` ADR 范围到 019 | `ADR-001`~`ADR-019`，括注含 `ADR-018`/`ADR-019` | 已闭合 |

- 已闭合的其它项：`README.md:23`/`PROJECT_STATUS.md:11` 的构建证据为 2026-11-09 的 debug 11,237,263 B + release 7,668,838 B（已按磁盘产物逐字节核对）；`RELEASE_REPORT.md` 的当前版本改为 0.1.0 / 已签名 / `PARTIAL` 并补了 lint 行；`TEST_REPORT.md` 的 Latest Run 与 Runs 末行一致（治理检查 PASS）。

### 5.1 收尾时必须守住这一点（不是本 commit 的问题，但会立刻复发）

- 写评审时，收尾 `TASK-030` 的 `tools/close-task.ps1 -Apply` 正在把两处「当前任务」改写成 **`TASK-030`**，而描述仍是 `TASK-040` 的「机型适配」；这就是第 3 轮那处错误的**同一个根因**：`tools/close-task.ps1:69`（README）与 `:76`（PROJECT_STATUS）的正则只替换 `TASK-` 加三位数字这个 token，不动同一行的描述。
- 因此：如果把 `TASK-030`/`TASK-039` 转 `DONE` 的收尾提交原样提交，最终仓库里「当前任务」会是「`TASK-030`（机型适配…）」——错误会再次进入 `main`。**提交收尾前请把 `README.md:21` 与 `PROJECT_STATUS.md:8` 改回 `TASK-040`（描述不动）**，或先修 `close-task.ps1`（要求显式传入描述 / 只写占位符），然后再跑最终 `verify-all`。
- 治理检查抓不到这一类错误：「readme matches status」只比对任务 id，「adr status consistent」只要求 ADR 被提及，所以绿灯不等于文档正确。

## 6. 分支与提交

**裁决：通过（附建议）。**

- `4d3b5c9` 只含文档，信息与改动一致；`8d4312d` 的「发布签名 + 任务文档」混合见 §2 说明，属既往提交，不建议为了它改写历史。
- 任务在 `PROJECT_STATUS.md:70` 的待批审清单中，本轮 `APPROVE` 后可按 `ADR-015` 转 `DONE`。

## 7. 结论

- 结论：`APPROVE`
- 理由：
  1. 第 1 轮的两条代码阻断（解析期校验、计划版本区间）已在 `59f70a3` 闭合，本轮静态复核仍在位：`BackupParser.kt:239-246` 的校验插在「有错即返回」（`:252`）之前，`:262-277` 列级约束、`:279-312` 日历真解析、`:314-332` 区间与多条开放版本、`:378-381` 的 unit 类型，错误均带字段路径；8 MB 上限改为流式判断。
  2. 第 2、3 轮的文档阻断已全部闭合：`PROJECT_STATUS.md:13`/`:70` 的 182、`ADR-019` 修订段、`README.md:21`/`:26` 与 `PROJECT_STATUS.md:8`/`:21` 的当前任务与 ADR 范围、构建证据的两处产物尺寸（与磁盘上的 11,237,263 B / 7,668,838 B 逐字节一致）。
  3. 可复现证据：`check-governance` 22/22（exit 0）、Test 注解 182、`verify-dao` 45/45、`verify-schema` 40/40；残余缺口已作为 Known Gaps 登记在 `TASK-039.md` §14，不属隐藏风险。
  4. 未验证项已如实声明：本轮未跑 gradlew/verify-all、未跑真机导入；`RELEASE_REPORT` 的签名与 lint 证据采信作者记录（不属本任务验收面）。
- 需修复项：无（本任务范围内）。
- 收尾注意项（务必执行，见 §5.1）：转 `DONE` 的收尾提交会经 `close-task.ps1` 把「当前任务」改写成 `TASK-030`/`TASK-039` 并保留「机型适配」描述；提交前把 `README.md:21` 与 `PROJECT_STATUS.md:8` 改回 `TASK-040`，再跑最终 `verify-all`。
- 非阻塞建议：
  1. `project/reports/RELEASE_REPORT.md:17` 的 `assembleDebug` 行仍是「2026-11-05、9.4 MB」，建议补 2026-11-09 / 11,237,263 B 一行，与 README 口径统一。
  2. 修 `tools/close-task.ps1:69`/`:76`：收尾时应替换整行描述或只写占位符，避免每个任务收尾都把「当前任务」写成上一任务的描述。
