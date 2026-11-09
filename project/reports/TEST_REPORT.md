# TEST_REPORT.md

> 只记录实际执行的命令和结果。没有执行就写 `NOT_RUN`。

## Latest Run
- 日期时间：`2026-11-06`
- 分支 / commit：`main`（`TASK-030` 合入；最终 commit 以 `git log -1` 为准）
- 环境：Windows；JDK 17.0.13（`JAVA_HOME=D:\javajdk17`）；Android SDK `D:\AndroidToolchain\sdk`；Gradle 8.9
- 结果：`tools/verify-all.ps1` `PASS`（治理 **22 项**检查、0 警告 + 单测 168 项 + DAO SQL 40 项 + `assembleDebug` + `git diff --check`）；`gradlew test` `FAILED`（沙箱禁止测试 worker，非代码失败）；真机 `PASS`（我的 → 导出备份 → 「已导出 3 个习惯」→ `node:sqlite` 按 schema v1 回读；今日/统计/我的三页巡检；`aapt2` 确认运行时权限为 0）

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
| 2026-11-05 | `node tools/verify-dao.mjs` | **DAO SQL PASS** | 31 项断言（新增 6 项批量查询场景）+ `MIN_CHECKS` 覆盖率兜底（防场景被静默删除）；真实 SQLite，8 个触发器在场 | TASK-010 |
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
| 2026-11-05 | `tools/run-unit-tests.ps1`（第 1 轮评审修复后） | **OK (90 tests)** | 新增每习惯归属与 3 项 `fromLoaded` 行为；U2 变异改为编译失败、U4 变异 1 项红 | TASK-022 |
| 2026-11-05 | `tools/run-unit-tests.ps1`（第 2 轮评审修复后） | **OK (91 tests)** | 删掉冗余钳位、补「昨天漏今天补 → 1」、归属用例加 `isCompleted` 断言 | TASK-022 |
| 2026-11-05 | `gradlew.bat connectedDebugAndroidTest`（第 1 次，设备锁屏） | **FAILED（环境）** | `INSTALL_FAILED_ABORTED: User rejected permissions`，失败在**安装阶段**；XML `tests="0"` 说明测试未启动。锁屏时 vivo 自动拒绝安装 | TASK-025 |
| 2026-11-05 | `gradlew.bat connectedDebugAndroidTest`（第 2 次，设备掉线） | **FAILED（环境）** | `Finished 0 tests`；`Android device (<device>) is not found`，UTP 启动前失败，与测试代码无关 | TASK-025 |
| 2026-11-05 | `tools/run-unit-tests.ps1`（接缝补洞） | **OK (105 tests)** | 转换器 7（含闰日、非法日期）、差分 1（3000 组随机对拍）、往返 3、翻日 1、接线规则 1、午夜算术 1；变异实测 M1=3、M2=1、M3=11、M4=5、ME=2、MH=1、MH2=1 | TASK-024 |
| 2026-11-05 | 真机：小数显示、对话框预填与中文校验提示 | **通过** | 列表 `0.5 / 2000 ml`；对话框预填 `0.5`；缺单位提示 `数值习惯需要填写单位` | TASK-018 |
| 2026-11-05 | `gradlew.bat connectedDebugAndroidTest`（唤醒 + `stayon usb` 后，第 3 次） | **PASS**（`BUILD SUCCESSFUL`，40s；另有 3m09s 一次） | `Tests 1/1 completed (0 skipped) (0 failed)`，XML `tests="1" failures="0"`；设备侧 runner 可用。由 Reviewer 独立复跑一次（43s）同形通过 | TASK-025 |
| 2026-11-05 | `tools/run-unit-tests.ps1`（撤销 + 归档） | **OK (110 tests)** | 新增 5 项：归档只移除目标行、重复归档报错、撤销回未完成、提示文案、静默 Flow 不等仓库 | TASK-027 |
| 2026-11-05 | `tools/run-unit-tests.ps1`（归档恢复） | **OK (115 tests)** | 新增 5 项：只列归档、恢复后回活动、重复恢复报错、静默 Flow、空态文案 | TASK-028 |
| 2026-11-05 | 变异 M-RESTORE：删掉 `restore()` 的本地重建 | **FAILED（1 项）**（预期） | `theRestoreRebuildDoesNotWaitForTheRepositoryFlow` 变红 | TASK-028 |
| 2026-11-05 | `node tools/verify-dao.mjs`（归档/恢复场景） | **DAO SQL PASS**（31 项） | 27 → 31；`MIN_CHECKS` 同步提升；恢复场景放在 `findById` 之后，否则会清掉被断言的 `archived_on` | TASK-028 |
| 2026-11-05 | 真机：建 → 归档 → 我的页 → 恢复 → 今日页 | **通过** | 两条归档恢复一条后，`Walk` 回到今日列表 | TASK-028 |
| 2026-11-05 | 变异 M-ARCH：删掉 `archive()` 的本地重建 | **FAILED（1 项）**（预期） | 修复前**同变异全绿**（测试证明的是 Flow 不是代码）；补「只发射一次」的假仓库后变红 | TASK-027 |
| 2026-11-05 | 真机：建习惯 → `⋮` → 归档 | **通过** | 列表回到空状态；`dumpsys activity top` 前台仍为 `com.habittracker` | TASK-027 |
| 2026-11-05 | 探针 A：虚构任务 `TASK-9XX` 标 `IMPLEMENTED` 但未登记待批审清单 | **FAILED（预期）** | `TASK-9XX is IMPLEMENTED but missing from the batched-review register`。**首次运行未变红**——新代码里 `$Matches[1]` 被后续 `-match` 覆盖，导致所有任务被 `continue` 跳过；修复后变红 | ADR-015 |
| 2026-11-05 | 探针 B：待批审清单写上不存在的 `TASK-9XX` | **FAILED（预期）** | `TASK-9XX is listed in the batched-review register but has no task file`；还原后 22/22 绿 | ADR-015 |
| 2026-11-05 | 变异：`SmokeTest` 断言改 `startsWith("com.nonexistent")` | **FAILED（预期）** | 1 项 `AssertionError`（`SmokeTest.kt:19`），`BUILD FAILED`；还原后复跑通过 → 断言有判别力。但只证明「安装 + runner attach」，不能当回归哨兵 | TASK-025 |
| 2026-11-06 | `tools/run-unit-tests.ps1`（今日页/新建页重建） | **OK (137 tests)** | 124 → 137：`HabitVisualsTest` 11（颜色解析与回退、环比例与越界钳制、文案、徽章口径、图标回退）、`CreateHabitPresenterTest` +2（图标/颜色落库、新表单默认值） | TASK-032 |
| 2026-11-06 | `pwsh -NoProfile -File tools/verify-all.ps1`（今日页/新建页重建） | **VERIFY ALL PASS** | 治理 22/22、单测 137、DAO 37、`assembleDebug` 13s、`git diff --check` 干净 | TASK-032 |
| 2026-11-06 | `tools/run-unit-tests.ps1`（统计滚动 + 导航隐藏 + 删除 + 归档分组） | **OK (145 tests)** | 137 → 145：`ArchiveGroupsTest` 5（月父/日子、倒序、同日按名、缺归档日忽略、空归档）、`DeleteHabitUseCaseTest` 3（删主表与子记录、已归档可删、缺失 id 拒绝）；首轮同日顺序断言写反，修正后通过 | TASK-034 |
| 2026-11-06 | `node tools/verify-dao.mjs`（删除语句） | **DAO SQL PASS**（38 项） | 新增 `HabitDao.deleteById` 场景：删除返回 1 行、三张子表级联清零、未知 id 返回 0；`MIN_CHECKS` 37 → 38 | TASK-034 |
| 2026-11-06 | 真机：统计页滚动 / 空白点击收导航栏 / 删除习惯 / 归档分组 | **PASS** | 趋势卡滚动后完整可见；空白点击后 UI 树无导航节点、再点恢复；建 `T` → 删除二次确认 → 列表回 3 条；归档后「2026 年 11 月 → 11 月 6 日」两级展示，随后恢复 | TASK-034 |
| 2026-11-06 | `gradlew.bat connectedDebugAndroidTest`（Room 契约） | **PASS**（40s） | 6 tests / 0 failures / 0 errors（`DatabaseContractTest` 5 + `SmokeTest`）：触发器 8 个在真实 `callback()` 路径注册且重开后仍拒绝非法行、闰日/REAL 精度/枚举/时区往返、`observeActive()` 失效、四表级联、`changeSchedule` 失败回滚旧版本；另实测确认跑完 App 被 AGP 卸载（`pm list packages` 为空），已用备份恢复数据 | TASK-026 |
| 2026-11-06 | `tools/run-unit-tests.ps1`（习惯详情页） | **OK (154 tests)** | 145 → 154：`HabitDetailRulesTest` 9（周一到周日边界、未计划日不算失败、首版本前不算失败、5 周周一对齐、完成率按应做天数、无应做日不除零、累计含窗口外历史、频率文案、版本区间）；首轮两处期望写错（周五的周六应为 FUTURE；5 周完成率分母是整窗口），修正后通过 | TASK-033 |
| 2026-11-06 | 真机：今日页行点击 → 详情 → 返回 | **PASS** | 行标题进详情（顶部返回/标题/编辑），打卡前 `0/1`、`0%`；勾选圈仍直接打卡 | TASK-033 |
| 2026-11-06 | 真机：详情页数据与编辑弹窗 | **PASS** | 打卡后重进详情：连续 1 天、本周 `1/1`、今天杏橙勾点、近 5 周 `100%`、累计/本月各 1；铅笔弹出「编辑习惯/名称/保存/取消」，两次返回回今日页 | TASK-033 |
| 2026-11-06 | 真机（iQOO Neo7 SE / Android 15）：安装 + 今日页 / 全部完成态 / 新建页 / 图标颜色落库对照 | **PASS** | 首次安装 `INSTALL_FAILED_ABORTED: User rejected permissions`（锁屏），唤醒 + `dismiss-keyguard` 后成功；今日页与设计稿一致；打卡后满环 +「连续 1 天」徽章 + 叶片；新建页曾有底部导航遮挡按钮，修复后复装确认无导航、按钮不遮挡；新建「水滴 + 雾蓝」习惯后列表显示水滴图标、打卡圈为雾蓝，归档后列表恢复 | TASK-032 |
| 2026-11-06 | 真机：日切改造后安装 + 今日页 + 统计页 | **PASS** | 今日页仍为 `11月6日 · 周五`、3 个习惯与打卡状态不变；统计页 UI 树只有「统计/本月/本周趋势」，`PeriodSwitch` 的「周/年」节点已消失 | TASK-035 |
| 2026-11-06 | 干净克隆 `.gradle/clone-verify` 后 `gradlew assembleDebug` | **PASS**（38s） | 验证「别人 clone 后能构建」；在 `%TEMP%` 克隆首次失败是 `D:\cpp_tmp` 的 ACL 问题（`stableIds.txt` 打开失败 13），换工作区内路径通过 | TASK-038 |
| 2026-11-06 | `gradlew.bat assembleDebug` + `aapt2 dump badging`（应用图标） | **PASS** | BUILD SUCCESSFUL 7s；`application-icon-120/160/240/320/480/640` 全指向 `res/mipmap-*/ic_launcher.png`；真机 `install -r` Success、`versionName=0.1.0`、今日页正常 | TASK-038 |
| 2026-11-06 | `tools/run-unit-tests.ps1`（日切点 04:00） | **OK (159 tests)** | 154 → 159：`DayCutoffTest` 5（03:59:59/04:00 边界、毫秒助手、epochMillis 时区、闰日、01:00 剩 3h）；`TodayPresenterTest` 的 4 条定时器断言改为 04:00 口径（23:59:59→4h1s、00:00→4h、闰日 16h、年末 5h） | TASK-035 |
| 2026-11-06 | 真机：自定义图标 Photo Picker → 保存 → 列表 | **PASS** | 选图后图标格预览；保存后今日页第 4 行显示圆形自定义图；`habits.icon='custom:952fcb26-...png'`，`files/icons/` 下 115,959 B | TASK-037 |
| 2026-11-06 | `tools/run-unit-tests.ps1`（自定义图标 + 内置线条图标） | **OK (162 tests)** | 159 → 162：自定义值保留、路径逃逸拒绝、11 个内置图标去重 | TASK-037 |
| 2026-11-06 | 真机：新建页 11 图标网格 / 滚动到底 | **PASS** | 两行 6+6（末格相册入口）等宽；频率卡与保存按钮之间补了间距 | TASK-037 |
| 2026-11-06 | `aapt2 dump badging`（自适应图标） | **PASS** | `application-icon-120/160/240/320/480/640/65534` 全指向 `res/mipmap-anydpi-v26/ic_launcher.xml` | TASK-038 |
| 2026-11-06 | 真机应用信息页图标（放大 4× 检查） | **PASS** | 绿底 + 完整插画，无白边；源图白边已裁掉 | TASK-038 |
| 2026-11-06 | `tools/verify-all.ps1`（自定义图标 + 自适应图标） | **VERIFY ALL PASS** | 治理 22/22、单测 162、DAO 38、`assembleDebug`、`git diff --check`（首次失败于 `CreateHabitScreen.kt` EOF 多一空行，已修） | TASK-037/038 |
| 2026-11-09 | `tools/verify-all.ps1`（备份导出） | **VERIFY ALL PASS** | 治理 22/22、单测 168、DAO **40**、`assembleDebug`、`git diff --check` | TASK-030 |
| 2026-11-06 | `gradlew.bat compileDebugKotlin`（备份导出） | **BUILD SUCCESSFUL** | 42s；`data/backup/` + SAF 接线编译通过 | TASK-030 |
| 2026-11-06 | `tools/run-unit-tests.ps1`（备份导出） | **OK (168 tests)** | 162 → 168：`BackupJsonTest` 6 项；首轮空表输出多行 `[ ]` 与期望 `[]` 不符，改为空表紧凑输出 | TASK-030 |
| 2026-11-06 | `aapt2 dump permissions`（权限清理后） | **PASS** | 仅 `com.habittracker.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`（androidx 生成）；`POST_NOTIFICATIONS`/`RECEIVE_BOOT_COMPLETED` 已删 | TASK-030 |
| 2026-11-09 | 真机：我的 → 导出备份 → 系统选择器保存 | **PASS** | 提示「已导出 3 个习惯」；`Download/habit-tracker-backup-2026-11-09.json` 3,373 B；导出后库计数不变 | TASK-030 |
| 2026-11-09 | `node .gradle/verify-backup.mjs`（按 schema v1 回读） | **PASS** | `format`/`schemaVersion`/日期/时间格式校验通过；内存库插入 3 习惯 / 3 计划 / 5 日志 / 0 提醒；与设备库计数一致；含 1 个归档习惯 | TASK-030 |
| 2026-11-09 | 真机巡检：今日 / 统计 / 我的 | **PASS** | 11 月 9 日 · 周一，今日 0/2、连续 3 天；统计本月 71%、最长连续 3 天、当日格描边；我的页数据备份卡片 + 归档项 | TASK-030 |

## Known Test Gaps
- 已知限制：`gradlew test` 在本机沙箱内必然失败（Gradle 测试 worker 的命名管道被禁），只能由 `tools/run-unit-tests.ps1` 直接跑 JUnit；这不是代码通过或失败的证据。
- **重要更新（`TASK-025`）**：`connectedDebugAndroidTest` 在真机上**可用**（`Tests 1/1 completed`，两次通过）。此前「本机无法跑 Android 侧测试」只对 Gradle 单元测试 worker 成立。约束有两条：安装需要**解锁**设备；运行期间需 `adb shell svc power stayon usb` 保持唤醒，否则 UTP 会报 `Android device ... is not found`。
- **已覆盖**：schema 与 8 条约束触发器（`verify-schema.mjs`，40 项）；DAO 声明语句的 SQL 语义（`verify-dao.mjs`，31 项，含 6 项批量查询场景与归档/恢复 4 项）——闭区间边界、CAS 更新（`archive`/`closeVersion`）、级联删除、唯一索引与 `ON CONFLICT` 更新。
- **未覆盖（UI）**：Today 状态机 9 项、新建表单状态机 9 项 JVM 覆盖；Compose 渲染与交互未由 Robolectric 或设备自动化验证（手动真机走查记录见 `TASK-012` §14，非自动化）。
- **部分已覆盖（`TASK-026`，设备侧）**：触发器在生产 `callback()` 路径上的注册与重开、实体/Converter 往返（闰日/REAL 精度/可空 unit/枚举/时区）、`observeActive()` 失效、四表级联、`changeSchedule` 回滚。**仍未覆盖**：迁移（schema 仍 v1）、Cursor 到实体的全量字段映射（只抽查关键字段）、并发写入、Room 由 `@Upsert`/`@Insert` **生成的** SQL 与唯一索引选择。
- 无模拟器（`HypervisorPresent=False`，无 WHPX/Hyper-V）；真机验证为手动走查，非自动化：`TASK-012`（建习惯/打卡/落库）与 `TASK-016`（小数输入 `0.5` 落库）。设备侧行级证据见 `project/reports/DEVICE_DUMP-2026-11-05.md`。
- **日切点 04:00（`TASK-035`）**：边界由 5 项 `DayCutoffTest` 覆盖；真机只能在凌晨手动确认一次真实跨日，当前真机证据仅为「日间不回归」。
- 数值型“撤销”语义、恢复默认策略仍未定（见 `project/reports/BASELINE_REVIEW.md`）。
