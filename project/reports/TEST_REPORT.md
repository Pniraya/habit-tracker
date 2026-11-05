# TEST_REPORT.md

> 只记录实际执行的命令和结果。没有执行就写 `NOT_RUN`。

## Latest Run
- 日期时间：`2026-11-05`
- 分支 / commit：`main` @ `c10f67d`（治理任务 `TASK-007` 合入后的合并提交）
- 环境：Windows；JDK 17.0.13（`JAVA_HOME=D:\javajdk17`）；Android SDK `D:\AndroidToolchain\sdk`；Gradle 8.9
- 结果：治理检查 `PASS`（20/20）；单测 `PASS`（27 项，`tools/run-unit-tests.ps1`）；构建 `PASS`（`assembleDebug` 36s）；schema `PASS`（40/40）；`gradlew test` `FAILED`（沙箱禁止测试 worker，非代码失败）

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
| 2026-11-05 | `check-governance.ps1`（13 项，新增「改基线须带 ADR」） | exit 0 | 13 PASS / 0 WARN / 0 FAIL；首轮曾抓出 `235941f` 仅改 AGENTS.md 尾空行，已用「忽略空白后比对 blob」精确豁免 | TASK-001 |
| 2026-11-05 | 临时克隆注入：改 `DATABASE.md` 且无 ADR | FAIL | `[FAIL] baseline changes carry an ADR  77d9246: DATABASE.md`，exit 1 | TASK-001 |
| 2026-11-05 | 临时克隆注入：同样改动 + 同 commit 新增 ADR | PASS | `[PASS] baseline changes carry an ADR`，RESULT OK，exit 0 | TASK-001 |
| 2026-11-05 | 按 ADR-005~010 修订 `DATABASE.md`/`ARCHITECTURE.md`/`REQUIREMENTS.md` 后运行检查器 | exit 0 | 13 项全 PASS | TASK-001 |
| 2026-11-05 | `Select-String` 一致性复核（`禁用提醒`、`weekday_mask BETWEEN`、`exportedAt`、`平均用时`） | 均符合预期 | 归档不再改 `enabled`；掩码下界 1；时间戳为数字；平均用时仅剩“V1 不提供” | TASK-001 |
| 2026-11-05 | `gradlew assembleDebug` | **SUCCESSFUL**（2m 38s，35 tasks） | APK 9.4 MB；`aapt2 dump badging`：`com.habittracker`、minSdk 26、targetSdk 35 | TASK-003 |
| 2026-11-05 | `gradlew test` | BUILD SUCCESSFUL / 全部 `NO-SOURCE` | 无测试用例，不得记为“测试通过” | TASK-003 |
| 2026-11-05 | `adb devices` | 空列表 | 无设备；`emulator`/`system-images` 未安装，真机验证 NOT_RUN | TASK-003 |
| 2026-11-05 | 四个 SDK 包 SHA-1 vs 官方 `repository2-3.xml` | 4/4 VERIFIED | 腾讯镜像下载 + 官方哈希校验 | TASK-002 |
| 2026-11-05 | 工具迁移到 `D:\AndroidToolchain` 后 `clean assembleDebug` | **SUCCESSFUL**（50s，36 tasks） | 新 `ANDROID_HOME` 端到端可用 | TASK-002 |
| 2026-11-05 | `gradlew test`（含首个单测） | **FAILED（环境）** | `ClassNotFoundException: worker.org.gradle.process.internal.worker.GradleWorkerMain`；沙箱禁止 worker 管道，非代码失败 | TASK-003 |
| 2026-11-05 | `HypervisorPresent` / `VirtualizationFirmwareEnabled` | False / True | 未启用 WHPX，x86_64 模拟器不可行 | TASK-003 |
| 2026-11-05 | `gradlew assembleDebug`（含 KSP/Room 2.6.1） | **SUCCESSFUL**（2m 32s） | 导出 `app/schemas/.../1.json`（12 187 bytes） | TASK-004 |
| 2026-11-05 | `node tools/verify-schema.mjs`（真实 SQLite，40 项） | **40/40 PASS** | 28 项约束行为 + 12 项 schema parity（列/索引/外键） | TASK-004 |
| 2026-11-05 | 违反 `status` 不变量的直接 `UPDATE` | 拒绝（`habit_logs: invalid log`） | 触发器兜底，满足 §12 该项 | TASK-004 |
| 2026-11-05 | `pwsh -File tools/run-unit-tests.ps1`（JUnit4 直接运行，绕过测试 worker） | **OK (27 tests)** | `LogStatusRulesTest`、`ScheduleRulesTest`、`AppRoutesTest` | TASK-005 |
| 2026-11-05 | `pwsh -File tools/check-governance.ps1`（扩展至 19 项） | exit 0，19 PASS | 首轮 1 FAIL：`TASK-001`/`TASK-004` 的 ADR 状态与 ADR 文件不一致；修复后通过 | TASK-006 |
| 2026-11-05 | `pwsh -NoProfile -File tools/run-unit-tests.ps1` | FAILED（环境） | 系统 TEMP `D:\cpp_tmp` 拒绝写入编译日志；随后将日志改至已忽略的 `.gradle` | TASK-007 |
| 2026-11-05 | `pwsh -NoProfile -File tools/run-unit-tests.ps1` | PASS（27 项） | 修复日志路径后直接 JUnit runner 通过 | TASK-007 |
| 2026-11-05 | `pwsh -NoProfile -File tools/check-governance.ps1` | exit 0，20 PASS | 包含独立评审记录与功能分支 PARTIAL 判断 | TASK-007 |
| 2026-11-05 | 受控负向验证：TASK-007 暂改为 `DONE` 后运行治理检查 | exit 1（预期） | `done tasks have review evidence` 正确拒绝缺少 `REVIEW-TASK-007.md` 的 DONE 任务 | TASK-007 |
| 2026-11-05 | 恢复 TASK-007 为 `PARTIAL` 后运行治理检查 | exit 0，20 PASS | 功能分支不会被要求登记为 main 上的 PARTIAL 例外 | TASK-007 |

## Known Test Gaps
- `gradlew test` 在本机沙箱内必然失败（Gradle 测试 worker 的命名管道被禁），只能由 `tools/run-unit-tests.ps1` 直接跑 JUnit；这不是代码通过或失败的证据。
- Room 事务、DAO 查询、`@Upsert` 行为没有运行时证据，需要 Robolectric 或真机。
- 无真机、无模拟器：`adb devices` 为空且 `HypervisorPresent=False`，设备验证全部 `NOT_RUN`。
- 数值型“撤销”语义、恢复默认策略仍未定（见 `project/reports/BASELINE_REVIEW.md`）。
