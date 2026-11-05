# Habit Tracker

本地优先的 Android 习惯追踪器。Kotlin + Jetpack Compose + Material 3 + Room，单 Gradle module（`:app`），目标 Android 15（API 35），minSdk 26。

> 仓库已包含可构建的 Android 工程（`:app`）、Room schema v1 与 Domain/Repository 层。所有状态只反映真实执行结果：没有证据的项一律写 `NOT_RUN`/`PARTIAL`。

## 1. 当前状态

| 项目 | 状态 |
|---|---|
| 阶段 | `Gate 3` — 首个功能页（Today）已接线，设备验证未完成 |
| 当前任务 | `TASK-012`（`PARTIAL`，新建习惯页与数值输入完成，待独立评审） |
| 代码实现 | `IMPLEMENTED_UNVERIFIED`（单模块 `:app`、三页导航、Room schema v1、Repository、UseCase） |
| 最近构建 | `PASS` — `gradlew assembleDebug`，2026-11-05，38s，APK 9.4 MB |
| 最近测试 | `PASS` — 57 项单测（`tools/run-unit-tests.ps1`）；`gradlew test` 在沙箱内不可用 |
| 最近数据层校验 | `PASS` — schema 40/40（`verify-schema.mjs`）+ DAO SQL 21/21（`verify-dao.mjs`） |
| ADR | `ADR-001`~`ADR-014` 全部 `ACCEPTED` |
| Git | `main` 为稳定分支；治理任务在 `feat/*` 经独立评审后合入；本地仓库，无远程；基线检查点 `d0f470a` |

## 2. 文档地图

| 文件 | 职责 | 更新频率 | AI 权限 |
|---|---|---|---|
| `AGENTS.md` | AI 必须遵守的开发规则 | 低 | 受保护（需批准 + ADR） |
| `ARCHITECTURE.md` | 系统架构、分层与工程目录 | 低 | 受保护（需批准 + ADR） |
| `DATABASE.md` | 表结构、数据规则、统计口径 | 低 | 受保护（需批准 + ADR） |
| `DEVELOPMENT.md` | 开发规范、阶段计划、验收清单 | 低 | 受保护（需批准 + ADR） |
| `project/REQUIREMENTS.md` | 产品需求与范围 | 中 | 受保护（需批准 + ADR） |
| `project/ROADMAP.md` | 阶段与 Gate | 中 | 可更新（需依据） |
| `project/PROJECT_STATUS.md` | 项目真实状态快照 | 每任务结束 | 可更新（需依据） |
| `project/FEATURE_MATRIX.md` | 功能完成矩阵 | 每次验收 | 可更新（仅限有证据项） |
| `project/tasks/TASK-xxx.md` | 单项任务的范围与验收 | 每任务 | 可更新 |
| `project/handoffs/LAST_HANDOFF.md` | 会话交接 | 每次会话结束 | 可更新 |
| `project/reports/TEST_REPORT.md` | 实际执行的测试记录 | 每次验证 | 可更新（禁止补写未执行项） |
| `project/reports/RELEASE_REPORT.md` | 发布证据与已知限制 | 发布时 | 可更新（需证据） |
| `project/reports/BASELINE_REVIEW.md` | 基线静态审查结论 | 低 | 只读 |
| `project/REVIEW_CHECKLIST.md` | Reviewer 独立评审清单 | 规则变化时 | 可修改 |
| `project/reviews/REVIEW-TASK-xxx.md` | 已完成任务的独立评审结论 | 每次拟合入 `main` 前 | 可新增 |
| `project/decisions/ADR-xxx.md` | 技术决策记录 | 有新决策时 | 只增不改（改状态除外） |
| `tools/check-governance.ps1` | 治理一致性检查脚本（只读） | 规则变化时 | 可修改（不得降低检查强度） |
| `tools/close-task.ps1` | 任务收尾：生成状态摘要并同步 README/状态快照 | 每任务结束 | 可修改 |
| `orgins/**` | 视觉概念稿（HTML 参考） | 只读 | 不得当作生产代码 |

## 3. 工作流

1. 新会话先按 `AGENTS.md` §2 的顺序读取文件，并核对 `git status --short`、当前分支与 `git log -1`。
2. 一次只执行一个 `TASK-xxx`，遵循 `AGENTS.md` §10 的七步生命周期（Bootstrap → Inspect → Plan → Implement → Verify → Persist → Handoff）。
3. 只有存在真实构建/测试证据时，才能把功能标记为 `VERIFIED`；`VERIFIED` ≠ `DEVICE_VERIFIED`。
4. 会话结束必须更新 `PROJECT_STATUS.md`、`TEST_REPORT.md`、`LAST_HANDOFF.md`，并提交一个聚焦 commit。

## 4. 快速命令

```powershell
# 本机工具链集中在一个目录（不在仓库内，换机需重装）
. D:\AndroidToolchain\env.ps1        # 设置 ANDROID_HOME / JAVA_HOME / PATH
pwsh -NoProfile -File D:\AndroidToolchain\verify.ps1   # 一键构建并校验 APK

pwsh -NoProfile -File tools/verify-all.ps1       # 收尾统一入口：治理检查 + 单测 + 构建 + 空白检查
gradlew.bat assembleDebug                        # 已验证：SUCCESSFUL
pwsh -NoProfile -File tools/run-unit-tests.ps1   # 已验证：40 项通过（绕过 Gradle 测试 worker）
node tools/verify-schema.mjs                     # 已验证：40/40（schema + 约束）
node tools/verify-dao.mjs                        # 已验证：21/21（DAO 的 SQL 语义，不含 Room 映射）
adb devices                                      # 当前为空：无真机、未装模拟器
pwsh -NoProfile -File tools/close-task.ps1 -Task TASK-xxx -Apply
pwsh -NoProfile -File tools/check-governance.ps1
```

## 5. 验证

```powershell
pwsh -NoProfile -File tools/check-governance.ps1   # exit 0 = OK, 1 = 有 FAIL
```

脚本只读，检查 21 项（含「`TASK-009` 起必须有 `verify-all` 证据」）：必备文件、状态文件残留 `UNKNOWN`、`TASK/ADR` 引用、功能矩阵状态词表与证据、交接快照是否填写、Markdown 相对链接、冲突标记与行尾空白、本地草稿是否误入库、`.gitignore` 覆盖、发布报告是否与工程状态矛盾、**改受保护基线是否同时带 ADR**（仅空白改动豁免），以及六项状态同步检查：

- `README` 的当前任务/阶段必须与 `PROJECT_STATUS` 一致；
- 每个 ADR 必须在 `PROJECT_STATUS` 中被覆盖，且引用处写的状态词必须与 ADR 文件一致；
- 有 Gradle 工程时 `RELEASE_REPORT` 不得再写“尚无 Android 工程”，`assembleDebug` 也不得仍为 `NOT_RUN`；
- `TEST_REPORT` 的 `Latest Run` 必须与 `Runs` 表最后一行（日期与结果）一致；
- `main` 上的 `PARTIAL` 任务必须在 `PROJECT_STATUS` 的「在册例外」登记；
- `project/REVIEW_CHECKLIST.md` 必须存在且含六个评审小节；`TASK-007` 起的 `DONE` 任务必须有独立 Reviewer 的 `APPROVE` 记录。

它只判断「文档是否自洽」，不判断「代码是否正确」——后者只能靠构建与测试。

## 6. 下一步

Gate 2 收尾：补 `UseCase` 层与 `DataStore`，并为 Room 事务补集成测试（Robolectric 或真机）；设备验证仍需 `emulator` + `system-images;android-35;google_apis` 或一台真机。详见 `project/tasks/TASK-005.md` 与 `project/PROJECT_STATUS.md`。
