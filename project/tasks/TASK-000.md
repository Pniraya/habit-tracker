# TASK-000: 建立 AI 项目治理文件并核对既有基线

- 状态：`DONE`
- 阶段：`Gate 0`
- 优先级：`P0`
- 依赖：`None`
- 创建日期：`2026-11-04`
- 完成日期：`2026-11-04`

## 1. Objective
在不改动任何冻结基线的前提下，核对仓库真实状态（Git、Android 工程、Gradle 配置、工具链），逐项核实 `project/reports/BASELINE_REVIEW.md` 的结论，列出文档冲突与未决设计问题，并判定是否具备开始 Android 工程初始化的条件。

## 2. Context
- `AGENTS.md` §2、§3、§5
- `ARCHITECTURE.md`、`DATABASE.md`、`DEVELOPMENT.md`
- `project/REQUIREMENTS.md`、`project/reports/BASELINE_REVIEW.md`、`project/ROADMAP.md`（Gate 0）
- `README_GOVERNANCE.md`「给 Codex 的首次指令」

## 3. Scope
只读审查；新增审查记录；按 `AGENTS.md` §6/§8 更新状态与交接文件；初始化 Git 仓库并创建首个可回退检查点。

## 4. Out of Scope
业务代码、任何 Gradle/Android 工程文件、数据库 schema 变更、`ARCHITECTURE.md`/`DATABASE.md`/`DEVELOPMENT.md`/`REQUIREMENTS.md` 修订、新增依赖、安装 SDK/Studio。

## 5. Expected Files
- 新增：`project/tasks/TASK-000.md`、`.gitignore`
- 更新：`project/PROJECT_STATUS.md`、`project/reports/TEST_REPORT.md`、`project/handoffs/LAST_HANDOFF.md`

## 6. Acceptance Criteria
- [x] 三份设计文档与 `project/` 下全部治理文件已读
- [x] Git 分支、工作区、提交已查（结论：审查前无仓库）
- [x] Android 工程目录、Gradle 配置、依赖、源码已查（结论：全部不存在）
- [x] `BASELINE_REVIEW.md` 的 9 项（A1–A4、B1–B5）逐项核实并给出证据
- [x] 文档冲突与未决设计问题成表
- [x] 已明确判定是否具备开始工程初始化的条件
- [x] 未写业务代码、未改 schema、未改基线、未加依赖、未把未验证项标记为完成

## 7. Tests
- 单元测试：无（无工程）
- 集成测试：无
- 构建/静态检查：`git diff --check`、`git status --short`（见 §10）
- 真机验证：不适用

## 8. Risks and Decisions
- A 类设计问题（归档与创建日期的时区语义、`HabitLog.status` 冗余、计划版本校验位置、归档与提醒 `enabled` 冲突、备份时间戳格式）在 ADR 获批前不得实现。
- 环境缺口（无 SDK/Studio、JDK 版本未定）比设计缺口更早阻断 Gate 1。
- 首个 commit 使用一次性身份（`-c user.name`/`-c user.email`），未写入仓库配置；后续若需要真实作者信息，应在提交前配置。
- 本次未修改任何冻结基线；`FEATURE_MATRIX.md` 未改动，因为没有任何实现或测试证据。

## 9. Implementation Log
- 2026-11-04：完成只读审查；`git init` 并将默认分支重命名为 `main`；新增 `.gitignore`；创建基线检查点 `d0f470a`（49 个跟踪文件）；写入本任务记录、状态、测试报告与交接。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `git rev-parse --is-inside-work-tree`（审查前） | exit 1 | `fatal: not a git repository`：审查前无仓库，因此不存在会被覆盖的用户改动 |
| 递归搜索 `*.gradle*`、`*.kts`、`*.toml`、`gradlew*`、`*.kt`、`AndroidManifest.xml` | 0 命中 | 无 Android 工程、无 Gradle 配置、无源码 |
| `java -version` | exit 0，`21.0.2` | 默认 JDK 21（`JAVA_HOME=D:\javajdk21`），另有 `D:\javajdk17`；基线要求 JDK 17，需决策 |
| `$env:ANDROID_HOME`；`Test-Path "$env:LOCALAPPDATA\Android\Sdk"`；`Get-Command gradle,adb` | 空；`False`；未找到 | 未安装 Android SDK / adb / Gradle CLI |
| `grep` 概念稿色值与字体统计 | 20+ 色值；`Noto Sans SC` + `Nunito` | 基线只定义 5 个品牌色且未收录 Nunito，token 清单不完整 |
| `git init` → `git branch -M main` → `git add -A` → `git commit` | exit 0 | `d0f470a`，49 个跟踪文件，`git status --short` 为空 |
| `gradlew.bat assembleDebug`；`gradlew.bat test` | NOT_RUN | 无 Wrapper、无 SDK，无法执行；不得视为通过 |

## 11. Completion Review
- [x] 验收标准全部满足
- [x] 相关检查实际运行
- [x] `git diff --check` 通过（提交前执行，见 §10）
- [x] Diff 已审查，无无关改动（仅新增 `.gitignore`/任务记录，更新三份治理记录）
- [x] 文档/状态已更新
- [x] 未完成项已明确记录

## 12. Handoff
- 当前剩余工作：`TASK-001`（ADR-005~009 与三份设计文档的最小修订，8 项决策待裁决）、`TASK-002`（环境）；见 `project/tasks/TASK-001.md`。
- 下一条安全操作：用户就 `TASK-001` §5 的 8 项决策答复后执行 `TASK-001`。
- Git 分支/提交：`main`，基线检查点 `d0f470a`；本记录随第二个 commit 入库。
