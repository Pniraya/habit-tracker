# PROJECT_STATUS.md

> 这是项目状态快照，不是计划书。AI 每次任务完成后必须按实际仓库状态更新。

## 当前状态

- 项目阶段：`Gate 0` 已关闭 → 进入 `Gate 1` 准备（`TASK-002` 环境）
- 当前任务：`TASK-002` — 准备 Android 构建环境（`NOT_STARTED`，等安装批准）；`TASK-001`、`TASK-000` 均已 `DONE`
- 总体状态：`IN_PROGRESS`
- 代码实现状态：`NOT_STARTED`（递归清点：无 `*.gradle*`、`*.kts`、`*.toml`、`*.kt`、`AndroidManifest.xml`）
- 最近验证构建：`NOT_RUN`（无工程、无 Android SDK）
- 最近验证测试：`NOT_RUN`
- 最近治理一致性检查：`PASS`（2026-11-05，`tools/check-governance.ps1`：13 项检查，13 PASS / 0 WARN / 0 FAIL）
- 最后核对时间：`2026-11-05 01:20 (+09:00)`

## 已有设计资产

- [x] `ARCHITECTURE.md`、`DATABASE.md`、`DEVELOPMENT.md`：三份基线，已完成与 ADR 一致的修订
- [x] `project/REQUIREMENTS.md`：需求基线，TBD 第 1/5/6 项已关闭
- [x] `project/decisions/ADR-001`~`ADR-010`：全部 `ACCEPTED`
- [x] `project/FEATURE_MATRIX.md`：F-016 平均用时 → `OUT_OF_SCOPE`（ADR-010）
- [x] Git 仓库 + `.gitignore` + 基线检查点（`d0f470a`）
- [x] `README.md`、`project/reports/RELEASE_REPORT.md`、`project/tasks/TASK-000~003.md`
- [x] `tools/check-governance.ps1`：13 项治理检查（含「改基线须带 ADR」，双向验证）
- [ ] Android Studio 工程创建并通过首次构建（环境缺口见下）

## 当前待办

1. 执行 `TASK-002`：安装 Android SDK / cmdline-tools（或 Android Studio），把 `JAVA_HOME` 指向 `D:\javajdk17`。
2. 执行 `TASK-003`：创建 Android 单模块工程（`:app`）、Version Catalog、Wrapper，完成首次 `assembleDebug`。
3. 每个任务结束按 `AGENTS.md` §10 生命周期更新状态、测试报告与交接。

## 阻塞项与风险

- 已由 `ADR-005~010` 关闭：归档/创建日期时区、平均用时无数据源、`HabitLog.status` 冗余、计划区间与频率校验、归档/恢复提醒冲突、备份时间戳冲突、今日完成数分母。
- 仍存在的设计缺口：`habits` 的 BOOLEAN 约束（`target_value=1` 且 `unit IS NULL`）尚未在 SQL 表达；数值型“撤销”的 UI 语义未定；恢复默认策略（合并/清空）未定。
- 环境缺口（2026-11-05 实测）：未安装 Android SDK / Android Studio / adb / Gradle CLI；`JAVA_HOME=D:\javajdk21` 与基线 JDK 17 不一致，但 `D:\javajdk17`（17.0.13）已存在。
- 网络与磁盘可用：`dl.google.com`、`repo1.maven.org`、`services.gradle.org` 均返回 200；D 盘剩余 98.6 GB。`TASK-002` 只差安装动作。
- 尚未从真实 Android 工程验证 SDK、Gradle、Kotlin、Compose 与 Room 的版本组合。
- `temp.md` 为用户输入草稿，已由 `.gitignore` 排除，不作为基线或验收依据。

## Git 状态

- 仓库是否已初始化：已初始化（`2026-11-04`；审查前确认**不存在**仓库）
- 当前分支：`main`（`git init` 默认 `master` 已重命名）
- 工作区状态：干净；无远程、未推送
- 最近 commit：以 `git log -1 --oneline` 为准；基线检查点 `d0f470a`
- 注意：提交使用一次性身份 `-c user.name`/`-c user.email`，未写入仓库配置

以上字段已于 `2026-11-05` 按实际 `git status` / `git log` / 检查器结果更新；后续任务结束需继续维护。
