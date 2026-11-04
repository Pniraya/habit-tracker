# Habit Tracker

本地优先的 Android 习惯追踪器。Kotlin + Jetpack Compose + Material 3 + Room，单 Gradle module（`:app`），目标 Android 15（API 35），minSdk 26。

> 本仓库当前**没有任何 Android 源码或 Gradle 配置**，处于治理层（Gate 0）。构建与测试状态一律是 `NOT_RUN`，直到真实执行才可改写。

## 1. 当前状态

| 项目 | 状态 |
|---|---|
| 阶段 | `Gate 1` — 骨架已构建，真机安装与导航未验证 |
| 当前任务 | `TASK-003`（`PARTIAL`，待设备验证） |
| 代码实现 | `IMPLEMENTED_UNVERIFIED`（单模块 `:app` + 三页导航壳） |
| 最近构建 | `PASS` — `gradlew assembleDebug`，2026-11-05，2m 38s，APK 9.4 MB |
| 最近测试 | `SUCCESS` 但全部 `NO-SOURCE`（尚无测试用例） |
| Git | `main`，本地仓库，无远程；基线检查点 `d0f470a` |

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
| `project/decisions/ADR-xxx.md` | 技术决策记录 | 有新决策时 | 只增不改（改状态除外） |
| `tools/check-governance.ps1` | 治理一致性检查脚本（只读） | 规则变化时 | 可修改（不得降低检查强度） |
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

gradlew.bat assembleDebug   # 已验证：SUCCESSFUL
gradlew.bat test            # 已验证：SUCCESSFUL，但当前无测试用例（NO-SOURCE）
adb devices                 # 当前为空：无真机、未装模拟器
pwsh -NoProfile -File tools/check-governance.ps1
```

## 5. 验证

```powershell
pwsh -NoProfile -File tools/check-governance.ps1   # exit 0 = OK, 1 = 有 FAIL
```

脚本只读，检查 13 项：必备文件、状态文件残留 `UNKNOWN`、`TASK/ADR` 引用、功能矩阵状态词表与证据、交接快照是否填写、Markdown 相对链接、冲突标记与行尾空白、本地草稿是否误入库、`.gitignore` 覆盖、发布报告是否在无工程时声称 `PASS`、**改受保护基线是否同时带 ADR**（仅空白改动豁免）。它只判断「文档是否自洽」，不判断「代码是否正确」——后者只能靠构建与测试。

## 6. 下一步

`TASK-003` 收尾：补 `emulator` 与 `system-images;android-35;google_apis`（或接真机），完成“Android 15 设备可安装并启动、三页可导航”的验证，然后关闭 Gate 1 进入 Gate 2（Room 数据层）。详见 `project/tasks/TASK-003.md`。
