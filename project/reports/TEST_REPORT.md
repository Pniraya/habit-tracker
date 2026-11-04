# TEST_REPORT.md

> 只记录实际执行的命令和结果。没有执行就写 `NOT_RUN`。

## Latest Run
- 日期时间：`2026-11-04 23:55 (+09:00)`
- 分支 / commit：`main`（审查执行时尚无仓库，故无 commit）→ 证据入库于基线检查点 `d0f470a`
- 环境：Windows；JDK 21.0.2（`JAVA_HOME=D:\javajdk21`）；**无 Android SDK / Android Studio / Gradle CLI**
- 结果：构建与单元测试 `NOT_RUN`（无工程）；治理审查检查见 `Runs` 表

## Runs

| 日期 | 命令/测试 | 结果 | 摘要 | 关联任务 |
|---|---|---|---|---|
| 2026-11-04 | `git rev-parse --is-inside-work-tree` | exit 1 | 审查前无 Git 仓库：`fatal: not a git repository` | TASK-000 |
| 2026-11-04 | 递归搜索 `*.gradle*`、`*.kts`、`*.toml`、`gradlew*`、`*.kt`、`AndroidManifest.xml` | 0 命中 | 无 Android 工程、无构建配置、无源码 | TASK-000 |
| 2026-11-04 | `java -version`；`$env:ANDROID_HOME`；`Test-Path %LOCALAPPDATA%\Android\Sdk`；`Get-Command gradle,adb` | 21.0.2 / 空 / False / 未找到 | 环境不具备构建条件 | TASK-000 |
| 2026-11-04 | `gradlew.bat assembleDebug`；`gradlew.bat test` | NOT_RUN | 无 Wrapper、无 SDK，无法执行；不得记为通过 | TASK-000 |
| 2026-11-05 | `pwsh -NoProfile -File tools/check-governance.ps1` | exit 0 | 12 项检查 11 PASS / 1 WARN（ADR-005~009 尚未创建，属计划内）/ 0 FAIL | TASK-000 |
| 2026-11-05 | 同上，在临时克隆中注入 2 处缺陷后运行 | exit 1 | 期望失败：状态文件残留 `UNKNOWN`、无测试证据的 `VERIFIED` 行；证明检查器非空转 | TASK-000 |
| 2026-11-05 | HTTPS HEAD：`dl.google.com/dl/android/maven2`、`repo1.maven.org`、`services.gradle.org` | 200 / 200 / 200 | 依赖与 Wrapper 下载可行；环境只缺安装，不缺网络 | TASK-002 |
| 2026-11-05 | `D:\javajdk17\bin\java.exe -version` | exit 0 / 17.0.13 | 基线要求的 JDK 17 本机已存在，可作 Gradle 运行 JDK | TASK-002 |

## Known Test Gaps
- Android 工程创建前无法运行 Gradle 构建；本机连 Android SDK 与 Gradle Wrapper 都不存在。
- 归档日期、创建日期的时区语义、日志状态不变量、平均用时和数值撤销语义尚待明确（详见 `project/reports/BASELINE_REVIEW.md` 与本会话新增冲突项）。
- 归档日期、日志状态不变量、平均用时和数值撤销语义尚待明确。
