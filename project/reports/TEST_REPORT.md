# TEST_REPORT.md

> 只记录实际执行的命令和结果。没有执行就写 `NOT_RUN`。

## Latest Run
- 日期时间：`2026-11-05`
- 分支 / commit：`feat/today-queries`（`TASK-022`，基线 `main` @ `bfb3150`）
- 环境：Windows；JDK 17.0.13（`JAVA_HOME=D:\javajdk17`）；Android SDK `D:\AndroidToolchain\sdk`；Gradle 8.9
- 结果：`tools/verify-all.ps1` `PASS`（治理 **22 项**检查、0 警告 + 单测 90 项 + DAO SQL 27 项 + `assembleDebug` 约 20-28s + `git diff --check`）；`gradlew test` `FAILED`（沙箱禁止测试 worker，非代码失败）

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
| 2026-11-05 | `gradlew.bat assembleDebug`（UseCase 层） | **SUCCESSFUL**（38s） | UseCase 进入主源码集 | TASK-008 |
| 2026-11-05 | `pwsh -File tools/run-unit-tests.ps1`（UseCase 层，测试加强后） | **OK (40 tests)** | 13 项新增；断言改为校验仓库实存值，不再依赖 fake 计算的状态 | TASK-008 |
| 2026-11-05 | 受控负向验证：删除 `RecordHabitProgressUseCase` 的归档校验后运行单测 | **FAILED，1 项失败**（预期） | 证伪 `archivedHabitsRejectWrites`：删除校验才变红 | TASK-008 |
| 2026-11-05 | 恢复校验后复跑 `tools/run-unit-tests.ps1` | **OK (40 tests)** | 变异全部还原，`git diff --stat` 为空 | TASK-008 |
| 2026-11-05 | `pwsh -NoProfile -File tools/verify-all.ps1`（首次运行） | **FAILED（exit 1）** | 治理步拦下 `ADR-014 not covered by PROJECT_STATUS`；单测/构建/空白三步本身通过 | TASK-009 |
| 2026-11-05 | 受控负向验证：删除 `TASK-009` 的 verify-all 证据 | **FAILED（第 21 项）**（预期） | `TASK-009 has no verify-all evidence`；还原后 21/21 PASS | TASK-009 |
| 2026-11-05 | 补登记 ADR-014 后复跑 `tools/verify-all.ps1` | **VERIFY ALL PASS**（exit 0） | 治理 21/21（7s）、单测 40 项（25s）、`assembleDebug` 21s、空白 0s | TASK-009 |
| 2026-11-05 | 合入 `main` 后复跑 `tools/verify-all.ps1` | **VERIFY ALL PASS**（exit 0） | 治理 7s、单测 25s、构建 23s、空白 0s；`main` 上确认 | TASK-009 |
| 2026-11-05 | `node tools/verify-dao.mjs` | **DAO SQL PASS** | 27 项断言（新增 6 项批量查询场景）+ `MIN_CHECKS` 覆盖率兜底（防场景被静默删除）；真实 SQLite，8 个触发器在场 | TASK-010 |
| 2026-11-05 | 变异：`HabitDao.observeActive` 去掉 `archived_on IS NULL` | **FAILED（1 项）**（预期） | `observeActive after archive expected 1, got 2` | TASK-010 |
| 2026-11-05 | 变异：`HabitLogDao.findRange` 的 `BETWEEN` 改开区间 | **FAILED（2 项）**（预期） | 两条边界断言均 `got []`；还原后全绿 | TASK-010 |
| 2026-11-05 | `pwsh -NoProfile -File tools/run-unit-tests.ps1`（Today presenter） | **OK (47 tests)** | 新增 `TodayPresenterTest` 7 项 | TASK-011 |
| 2026-11-05 | 变异（复审）：`toggle` 永远写 1.0 / `describe` 忽略 schedule / 绕过 use case 直写 | **FAILED（前两项）/ 未被抓住（第三项）** | 第三项证明"负值拒绝"测试无法鉴别是否走了 use case | TASK-011 |
| 2026-11-05 | 复审修复后 `tools/run-unit-tests.ps1` | **OK (49 tests)** | 新增"数值习惯点击不写入"与"休息日写入被 use case 拒绝"；后者只在走 use case 时成立 | TASK-011 |
| 2026-11-05 | `gradlew.bat assembleDebug`（Today 接线） | **SUCCESSFUL**（28s） | 首次失败：`foundation.layout.weight` 为 internal import | TASK-011 |
| 2026-11-05 | 破坏性验证（复审）：`fun` 改名 / 删 `observeRange` 注册 / 只删场景保留注册 | **FAILED（各 1-2 项）**（预期） | 改名仍打印 `checks/failed` 汇总并 exit 1；删注册触发集合守卫；只删场景暴露"覆盖率可静默削减"，已加 `MIN_CHECKS` 兜底 | TASK-010 |
| 2026-11-05 | 变异：`findVersionsOn` 下界/上界的 `<=` 改 `<`、`fun` 改名、参数改名 | **FAILED（各 1 项）**（预期） | 上下闭区间各有可证伪断言；改名触发 `query is missing`；参数改名触发 `unbound parameters` | TASK-010 |
| 2026-11-05 | `pwsh -NoProfile -File tools/run-unit-tests.ps1`（新建习惯） | **OK (57 tests)** | 新增 `CreateHabitPresenterTest` 8 项 | TASK-012 |
| 2026-11-05 | 变异：`isBlank()` → `isEmpty()` | **FAILED（1 项）**（预期） | 空名测试变红；还原后 57 项全绿 | TASK-012 |
| 2026-11-05 | 复审修复后 `tools/run-unit-tests.ps1` | **OK (58 tests)** | 新增「数值→布尔切换清空单位」回归测试；对话框本地拒绝负值 | TASK-012 |
| 2026-11-05 | `pwsh -NoProfile -File tools/run-unit-tests.ps1`（今日页新建入口） | **OK (59 tests)** | 新增 `theCreateActionAppearsOnlyOnceTheListHasRows` | TASK-013 |
| 2026-11-05 | 变异：`showsCreateAction` 去掉 `!state.isLoading &&` | **FAILED（1 项）**（预期） | 加载态断言变红；还原后 59 项全绿 | TASK-013 |
| 2026-11-05 | `pwsh -NoProfile -File tools/run-unit-tests.ps1`（规则上移 UseCase） | **OK (63 tests)** | 新增 4 项：无单位数值被拒、带单位布尔被拒、单位 trim、fake 不再校验 | TASK-014 |
| 2026-11-05 | 变异：UseCase 的 `normalizeTarget` → 直接构造 `NormalizedTarget` | **FAILED（4 项）**（预期） | 拒绝与 trim 断言全部变红；还原后 63 项全绿 | TASK-014 |
| 2026-11-05 | 负向探针：`Latest Run` 计数 63 → 57 | **FAILED（治理第 15 项）**（预期） | `Latest Run says 单测 57 项 but the last OK row is 63 tests` | TASK-015 |
| 2026-11-05 | 负向探针：`TASK-014` 证据行 → 「未运行」 | **FAILED（治理第 21 项）**（预期） | 已开工任务不得写未运行 | TASK-015 |
| 2026-11-05 | `pwsh -NoProfile -File tools/run-unit-tests.ps1`（错误文案与小数显示） | **OK (71 tests)** | 新增 8 项：小数显示、整数去 `.0`、领域消息映射、未知错误回退、`onFailure` 调用点、逗号小数、千分位不被改写、对话框预填 | TASK-018 |
| 2026-11-05 | 变异：`formatQuantity` → `toLong().toString()` | **FAILED（2 项）**（预期） | 小数被截断的断言变红 | TASK-018 |
| 2026-11-05 | 变异：`errorMessageFor` 未知分支返回原文 | **FAILED（2 项）**（预期） | 英文原文回显被抓住 | TASK-018 |
| 2026-11-05 | 变异：`onFailure` 改写回 `failure.message`（评审第 1 轮） | **FAILED（1 项）**（修复后） | 新增调用点测试变红；修复前全绿，属真实回归缺口 | TASK-018 |
| 2026-11-05 | 变异：`prefillText` 改用 `toLong()`（评审第 2 轮） | **FAILED（1 项）**（修复后） | 预填测试变红；修复前全绿 | TASK-018 |
| 2026-11-05 | 变异：去掉千分位保护（评审第 2 轮） | **FAILED（1 项）** | 千分位断言变红 | TASK-018 |
| 2026-11-05 | `tools/run-unit-tests.ps1`（连续天数） | **OK (84 tests)** | 新增 13 项；5 组变异各 2/2/1/1/1 红；第 2 轮评审 7 组变异全打红 | TASK-021 |
| 2026-11-05 | `tools/replay-governance-probes.ps1`（15 个探针） | **0 failed**；基线 22/22 | P5–P19：离线守卫 P17/P18/P19（含绕过 catalog）、`NOT_STARTED` 防规避 P16、脏树守卫 | TASK-020 |
| 2026-11-05 | `tools/run-unit-tests.ps1`（Today 查询批量化） | **OK (86 tests)** | 新增 2 项查询计数断言：5 个习惯列表固定 4 次查询、点按固定 5 次 | TASK-022 |
| 2026-11-05 | 变异：`snapshot()` 退回逐习惯读 / 多做一次批量读 | **FAILED（各 2 项）**（预期） | 两条计数断言同时变红；还原后 86 项全绿 | TASK-022 |
| 2026-11-05 | `tools/run-unit-tests.ps1`（评审阻断项修复后） | **OK (90 tests)** | 新增每习惯归属与 3 项钳位；U2 变异改为编译失败、U4 变异 1 项红 | TASK-022 |
| 2026-11-05 | 真机：小数显示、对话框预填与中文校验提示 | **通过** | 列表 `0.5 / 2000 ml`；对话框预填 `0.5`；缺单位提示 `数值习惯需要填写单位` | TASK-018 |

## Known Test Gaps
- 已知限制：`gradlew test` 在本机沙箱内必然失败（Gradle 测试 worker 的命名管道被禁），只能由 `tools/run-unit-tests.ps1` 直接跑 JUnit；这不是代码通过或失败的证据。
- **已覆盖**：schema 与 8 条约束触发器（`verify-schema.mjs`，40 项）；DAO 声明语句的 SQL 语义（`verify-dao.mjs`，27 项，含 6 项批量查询场景）——闭区间边界、CAS 更新（`archive`/`closeVersion`）、级联删除、唯一索引与 `ON CONFLICT` 更新（`verify-dao.mjs`，21 项）。
- **未覆盖（UI）**：Today 状态机 9 项、新建表单状态机 9 项 JVM 覆盖；Compose 渲染与交互未由 Robolectric 或设备自动化验证（手动真机走查记录见 `TASK-012` §14，非自动化）。
- **未覆盖**：Room 的 Cursor/实体映射；TypeConverter 往返；`Flow` 失效通知；`withTransaction` 边界与并发写入；Room 由 `@Upsert`/`@Insert` **生成的** SQL（harness 验证的是同一冲突列上等价语句的语义与唯一索引存在，不能证明 Room 会选用该冲突列）。
- 无模拟器（`HypervisorPresent=False`，无 WHPX/Hyper-V）；真机验证为手动走查，非自动化：`TASK-012`（建习惯/打卡/落库）与 `TASK-016`（小数输入 `0.5` 落库）。设备侧行级证据见 `project/reports/DEVICE_DUMP-2026-11-05.md`。
- 数值型“撤销”语义、恢复默认策略仍未定（见 `project/reports/BASELINE_REVIEW.md`）。
