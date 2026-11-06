# REVIEW-TASK-025: 独立评审记录（第 1 轮）

- 审查任务：`TASK-025`（真机 instrumented 测试可用性 spike）
- 审查 commit：`82f67b0a7f47615263ab0676f8ac1147f2071669`（分支 `feat/androidtest-spike`，基线 `main` @ `74c3f06668291e51613d70c4f09e33332f483c01`）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，**未参与实现**，本轮只读 + 运行命令）
- 独立性：`INDEPENDENT`
- 环境：Windows；JDK 17.0.13（`JAVA_HOME=D:\javajdk17`）；Android SDK `D:\AndroidToolchain\sdk`；Gradle 8.9；真机 `V2238A - 15`（`10CDAK0B71005KU`，Android 15）
- 日期：`2026-11-05`
- 结论：**REJECT**（阻断 2 项，均为文档/状态口径；实现与测试证据本身通过）

评审开始与结束 `git status --short` 均为空（唯一改动是本评审文件本身，未入库）。所有 Gradle 任务串行执行，未与其他 Gradle 任务并发。按任务要求未使用 `gradlew.bat test`。

## 1. 前置

- [x] Reviewer 是独立会话，非实现者
- [x] 已读 `AGENTS.md` §5/§7/§11/§12、`project/REVIEW_CHECKLIST.md`、`project/tasks/TASK-025.md`、`project/reports/TEST_REPORT.md`、`project/handoffs/LAST_HANDOFF.md`、`README.md`、`project/PROJECT_STATUS.md`
- [x] `git log --oneline -6`：`82f67b0` / `36c7e95` / `74c3f06`（merge TASK-024）/ `17ea84c` / `4a0daef` / `11facff`
- [x] `git status --short` 为空；`git diff --check` exit 0，无空白错误、无冲突标记
- [x] `git diff main...HEAD --stat`：8 个文件、+65/−11，全部可对应到本任务（见 §2）

## 2. Diff 与 Scope（`74c3f06..82f67b0`）

| 文件 | 改动 | 判定 |
|---|---|---|
| `gradle/libs.versions.toml` | +2 版本号、+2 library（`androidx.test:runner:1.6.2`、`androidx.test.ext:junit:1.2.1`） | 在 Scope（§3 明写 `androidx.test.ext:junit`、`androidx.test:runner`） |
| `app/build.gradle.kts` | + `testInstrumentationRunner`（注释声明仅测试用）、+ 两条 `androidTestImplementation` | 在 Scope（§5 Expected Files 含本文件） |
| `app/src/androidTest/java/com/habittracker/SmokeTest.kt`（新增 21 行） | 1 条 instrumented 用例 | 在 Scope（§3「写一个最小测试」） |
| `project/tasks/TASK-025.md`、`TEST_REPORT.md`、`PROJECT_STATUS.md`、`README.md`、`LAST_HANDOFF.md` | 状态/证据/交接 | 在 Scope（§3 明写更新 TEST_REPORT） |

- [x] 无顺手重构、无删除既有测试、无 Gradle module / 后台服务 / 产品功能变化
- [x] 未触碰 `app/src/main`、schema、DAO、Composable 业务逻辑
- [x] 受保护基线文件（`AGENTS.md`/`ARCHITECTURE.md`/`DATABASE.md`/`DEVELOPMENT.md`/`REQUIREMENTS.md`/`README_GOVERNANCE.md`）**未被改动**，无需 ADR

## 3. A. 任务记录的「通过/失败」是否与真实输出一致

**结论：三次运行的因果链与失败性质记载属实、未粉饰；但 §6 验收项的文字口径自相矛盾（非阻断，见 N4）。**

- 我无法重放 `36c7e95` 之前的历史运行，只能核对**记载本身的内部一致性 + 我的独立复现**：
  - 第一次失败的签名 `INSTALL_FAILED_ABORTED: User rejected permissions` 是 vivo 的 USB 安装确认被拒，**发生在安装阶段**；`TASK-025` §10 同时记 `TEST-...xml` 为 `tests="0"`，即测试根本没跑。把这次写成「BUILD FAILED」而不是「测试失败」是正确的区分。
  - 第二次失败记 `Android device (10CDAK0B71005KU) is not found` + `Finished 0 tests`，并明确标注「与测试代码无关」。这是 UTP 启动前的设备发现失败，性质判断正确，**没有把基础设施故障写成测试通过，也没有反过来把测试通过说成环境失败**。
  - 第三次 `adb shell svc power stayon usb` 后通过，并把「设备必须保持连接与唤醒」写成通过前提，属如实归因。
- 我的独立复现（同一设备、锁屏已唤醒、`stayon usb`、`connectedDebugAndroidTest`）**一次通过**，输出与记载的第三次同形：`Starting 1 tests on V2238A - 15` → `Tests 1/1 completed. (0 skipped) (0 failed)` → `BUILD SUCCESSFUL in 43s`。
- 差异点：**我这次安装没有出现「继续安装」的人工确认**（见 N5），因此「安装被设备拒绝」不应继续作为当前状态的原因（见阻断 B1）。

## 4. B. 新增依赖是否超 Scope / 是否破坏离线保证

**结论：不超 Scope；`androidTestImplementation` 不进应用 APK 运行时；未引入网络权限或网络库。离线保证（治理第 22 项）成立。**

我的实测证据（`assembleDebug` 之后）：

| 证据 | 实测 |
|---|---|
| `app/build/outputs/apk/debug/app-debug.apk` | 10 137 249 bytes（9.67 MB），ZIP 条目 160 |
| app APK 内匹配 `androidx/test`、`espresso`、`okhttp`、`retrofit` 的条目 | **0 条** |
| `aapt2 dump badging` app APK | `package: com.habittracker`；权限仅 `POST_NOTIFICATIONS`、`RECEIVE_BOOT_COMPLETED`、`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`；**无 `INTERNET`**；**无 `instrumentation` 节点** |
| 合并后的主变体 manifest（`merged_manifest/debug`、`merged_manifests/release` 等） | 无 `instrumentation`、无 `INTERNET` |
| `:app:dependencies --configuration debugRuntimeClasspath` | 680 行，匹配 `androidx\.test` **0 行** |
| `:app:dependencies --configuration debugAndroidTestRuntimeClasspath` | 仅测试配置里有 `androidx.test:runner:1.6.2` / `ext:junit:1.2.1` / `monitor` / `core` / `services:storage`，无 okhttp/retrofit |
| `app-debug-androidTest.apk` | 357 393 bytes（0.34 MB），`package: com.habittracker.test`，权限仅 `REORDER_TASKS` |

结论维度：
- `testInstrumentationRunner` 写在 `defaultConfig` 里只作用于 **test manifest**（`processDebugAndroidTestManifest`），app APK 的 badging 中不存在 `instrumentation`，已实测确认。
- 依赖隔离由 Gradle 配置语义保证（`androidTestImplementation` 只进 `debugAndroidTestRuntimeClasspath`），我用 classpath 与 APK 双重验证，不是只看文档。
- 治理第 22 项 `offline guarantee` 在本 commit 上实测 `[PASS]`（见 §6.1）。
- 唯一可指摘处：`TASK-025` §8 写「仍需确认不含网络库」但没有任何记录显示做过这条确认；实际结论正确，属「说了没做」的措辞问题（非阻断，建议改为实测结论）。

## 5. C. `SmokeTest` 是否有判别力

**结论：不是 `assertTrue(true)`，但它证明的是「双向 APK 安装 + runner 启动 + targetContext 可取」这一件事，对应用行为零判别力。对 spike 而言够用，但不构成「冒烟测试」。**

- 断言 `InstrumentationRegistry.getInstrumentation().targetContext.packageName.startsWith("com.habittracker")`：`targetContext` 只有在 app APK 与 test APK 都装上、instrumentation 成功 attach 之后才存在；若安装被拒/runner 未启动，用例根本到不了这一行（现实中表现为 `Finished 0 tests`）。所以它编码的正是本任务要测的那个属性。
- **变异实测（我自造，跑在真机上，非文档转抄）**：

```
assertTrue(context.packageName.startsWith("com.nonexistent"))
com.habittracker.SmokeTest > theAppUnderTestIsInstalled[V2238A - 15] FAILED
  java.lang.AssertionError  ... at com.habittracker.SmokeTest.theAppUnderTestIsInstalled(SmokeTest.kt:19)
Tests on V2238A - 15 failed: There was 1 failure(s).
BUILD FAILED in 48s
XML: <testsuite name="com.habittracker.SmokeTest" tests="1" failures="1" errors="0" ...>
```

  → 断言是活的，会红。
- 还原：`git checkout -- app/src/androidTest` 后复跑 `BUILD SUCCESSFUL in 40s`，`Tests 1/1 completed (0 failed)`，`git status --short` 为空；文件第 19 行已确认恢复为 `startsWith("com.habittracker")`。
- **判别力边界（必须写清）**：即使 app 启动即崩溃、没有 Activity、Room 映射全错、Compose 白屏，这条也照样绿。它等价于「harness 通了」，不等价于「app 能用」。`TASK-025` §4 Out of Scope 已声明「通过后再另立任务写冒烟用例」，因此**不算缺陷**；但后续任务不能把它当回归哨兵，建议下一条用例至少 `ActivityScenario.launch(MainActivity::class.java)` 或 `onNodeWithText(...)` 断言一个真实节点。
- 用例名 `theAppUnderTestIsInstalled` 与它实际证明的东西相符，没有夸大命名。

## 6. 我的实测数字（全部本轮亲自执行）

### 6.1 门禁 `pwsh -NoProfile -File tools/verify-all.ps1`（HEAD=`82f67b0`，串行）

| 步骤 | 实测输出 | 退出码 | 耗时 |
|---|---|---|---|
| `check-governance.ps1` | `SUMMARY: 22 checks, 22 passed, 0 warnings, 0 failed` / `RESULT: OK` | 0 | 12s |
| `run-unit-tests.ps1` | `OK (105 tests)` / `RESULT: UNIT TESTS PASS` | 0 | 24s |
| `verify-dao.mjs` | `checks: 27, failed: 0` / `RESULT: DAO SQL PASS` | 0 | 0s |
| `assembleDebug` | `BUILD SUCCESSFUL in 19s` | 0 | 20s |
| `git diff --check` | `no whitespace errors` | 0 | 0s |
| 合计 | `RESULT: VERIFY ALL PASS` | **0** | — |

### 6.2 真机 `gradlew.bat connectedDebugAndroidTest --console=plain`

| 轮次 | 命令/条件 | 实测结果 |
|---|---|---|
| 我的第 1 次 | `adb shell input keyevent 224` + `wm dismiss-keyguard` + `svc power stayon usb` 后 | `Starting 1 tests on V2238A - 15` → `Tests 1/1 completed. (0 skipped) (0 failed)` → `BUILD SUCCESSFUL in 43s`；XML `tests="1" failures="0" errors="0" time="11.045"`，`<testcase ... time="0.027">` |
| 变异 | 断言改 `com.nonexistent` | `BUILD FAILED in 48s`，1 项 `AssertionError`（`SmokeTest.kt:19`）；XML `failures="1"` |
| 还原后复跑 | `git checkout -- app/src/androidTest` | `BUILD SUCCESSFUL in 40s`，`Tests 1/1 completed (0 failed)`；`git status --short` 空 |

`adb devices`：`10CDAK0B71005KU\tdevice`。运行结束后 `adb shell pm list packages com.habittracker` 为空（UTP 收尾卸载），说明本轮是**从零安装**成功，未出现安装确认阻塞。

## 7. D. 文档同步

- [x] 计数一致：`README.md:15` 105、`PROJECT_STATUS.md:13` 105、`TEST_REPORT.md:9` 105、`TASK-025` §10 / 自动摘要 105；治理 22 项、DAO 27 项一致；均与本轮实测相符。
- [x] `TEST_REPORT` 新增行（第 83 行）的 verdict **没有被第 15 项解析到**，但这不是本 commit 的破坏：该行是**插在中间的**，第 15 项只取 `## Runs` 内最后一个 `^| 20` 行（现为第 85 行 `TASK-018` 的「**通过**」）。
- [!] **第 15 项在这棵树上实际是空转的**（我复刻脚本逻辑实测）：最后一行 verdict 是中文「**通过**」，脚本把它映射成 `expected = 'NOT_RUN'`，而 `## Runs` 之前的 `Latest Run` 摘要块里恰好有表头说明「没有执行就写 `NOT_RUN`」，于是 `-notmatch 'NOT_RUN'` 恒不成立，检查通过。也就是说：**结果词这一半检查恒真**；真正有效的只有「`OK (N tests)` 计数」那一半（最后一条 105 = `Latest Run` 的 105）。
- [!] 由此可推出的实质风险：本任务最有价值的那条证据行（`BUILD SUCCESSFUL`（3m09s 与 40s 各一次））**不受任何治理校验保护**，写错也没人拦。属既有检查器弱点（`TASK-015` 时代的探针只覆盖了「计数」与「NOT_RUN 文案」两条），不是本 commit 引入，但本 commit 依赖它作为交付证据，建议补一条探针或把该行放到表尾并改用 `SUCCESS` 关键字（见 N1）。
- [!] `TEST_REPORT` 的 `Latest Run` 仍写「分支 / commit：`feat/seam-hardening`（`TASK-024`，基线 `main` @ `427e7cc`）」，与本分支 `feat/androidtest-spike` @ `82f67b0` 不符（N2）。
- [x] `README.md:12` 与 `PROJECT_STATUS.md:8` 对「当前任务」的表述彼此一致（都写 `PARTIAL` + 「等设备授权安装」），但**与同分支的任务记录相反** → 阻断 B1。
- [x] `LAST_HANDOFF` Snapshot 无空字段；但同为「安装被设备拒绝」→ 同上。
- [x] ADR 覆盖完整（`PROJECT_STATUS.md:21` 的 ADR-001~014 全是 `ACCEPTED`，无新增 ADR 需求）。

## 8. E. 结论层面的诚实性（推翻旧结论的适用范围）

**结论：任务记录的结论句范围限定正确，但状态文件没有跟着改口径，出现了「只改一处」的半传播。**

- `TASK-025` §9 末句与 `TEST_REPORT.md:89` 都明确写「此前『本机无法跑 Android 侧测试』**只对 Gradle 单元测试 worker 成立**」，没有扩写成「所有 Android 测试都能跑」。这是正确的、**没有反向过度修正**。
- 但同一分支里仍在说旧话（我 grep 实测）：
  - `PROJECT_STATUS.md:43`：「Room/Compose 相关测试仍无法在本机执行」——已被本任务推翻（应为「无设备侧用例」而非「无法执行」）。
  - `PROJECT_STATUS.md:47`：「DAO/Repository 无运行时测试……只能靠 `verify-schema.mjs` 验证 SQL 层」——同上，现在还多了一条可行的设备侧路径。
  - `PROJECT_STATUS.md:30`：「DAO/Repository 运行时测试（沙箱内无法运行 Gradle 测试 worker）」——同一旧归因。
  - `LAST_HANDOFF.md:45`：「Room 的 Cursor 映射……无自动化验证（本机跑不了 Gradle 测试）」——括号里的归因已失效。
- 这正是本评审第 2 个阻断项 B2：结论写在任务文件里，**没有传播到状态文件的「阻塞项/已知缺口」**，下一会话读到 `PROJECT_STATUS` 仍会以为设备侧测试不可行。

## 9. REVIEW_CHECKLIST 逐项

| # | 项 | 结论 | 依据 |
|---|---|---|---|
| 0 | Reviewer 独立、已读任务、`git log`/`status` 已核对 | APPROVE | §1 |
| 1 | 每个改动可对应验收项、无顺手重构 | APPROVE | §2 |
| 1 | 无未批准依赖/权限/module/后台服务/功能 | APPROVE | §4（APK badging + classpath 实测） |
| 1 | 业务规则未写进 Composable/Receiver/DAO | APPROVE | 未触碰 main 源码 |
| 1 | `git diff --check` 干净、无冲突标记 | APPROVE | exit 0 |
| 1 | 未删除或绕过既有测试 | APPROVE | 仅新增 androidTest 源集 |
| 2 | 分层符合 `ARCHITECTURE.md` | APPROVE | 测试源集不涉生产分层 |
| 2 | `DATABASE.md` schema/索引/外键未变 | APPROVE | 无 schema 与迁移改动 |
| 2 | 日期/时区/掩码/快照口径一致 | APPROVE | 本改动不涉 |
| 3 | TEST_REPORT 有实际命令、退出码、失败摘要 | **REJECT** | 成功轮有，但两次失败轮只存在于 `TASK-025` §10，Runs 表未记（N3）；且该行不被治理解析（§7） |
| 3 | 未执行的测试未写成通过；`gradlew test` 限制有记录并用替代 runner | APPROVE | 本轮未用 `gradlew test`；105 项走 `run-unit-tests.ps1`，归档与实测一致 |
| 3 | 声称 `VERIFIED`/`DEVICE_VERIFIED` 有证据 | APPROVE（本任务未提升任何功能状态） | 无 `FEATURE_MATRIX` 改动 |
| 3 | 构建产物可复核 | APPROVE | APK 路径/字节数/XML 均见 §4、§6.2 |
| 4 | README 阶段/当前任务与 PROJECT_STATUS 一致 | **REJECT** | 两者一致但**都错**（「等设备授权安装」与实测相反）→ B1 |
| 4 | PROJECT_STATUS 覆盖所有 ADR、ADR 状态一致 | APPROVE | ADR-001~014 全 `ACCEPTED` |
| 4 | 无过期断言（如「尚无 Android 工程」） | **REJECT** | `PROJECT_STATUS.md:30/43/47`、`LAST_HANDOFF.md:45` 仍持被本任务推翻的旧结论 → B2 |
| 4 | TEST_REPORT `Latest Run` 与 Runs 末行一致 | APPROVE（计数一致）/ 非阻断 | 计数 105 一致；但 `Latest Run` 的分支/commit 仍指 `feat/seam-hardening`（N2） |
| 4 | LAST_HANDOFF Snapshot 已更新、无空字段 | **REJECT** | `:8` 的 PARTIAL 原因与 `:11` 的「已证明可用」自相矛盾 → B1 |
| 5 | 分支策略 / PARTIAL 在 feature 分支 | APPROVE | `feat/androidtest-spike`，治理第 16 项 `[PASS]` |
| 5 | commit 信息聚焦且与改动一致 | APPROVE | `36c7e95`、`82f67b0` 两条均聚焦 |
| 5 | 受保护基线改动带 ADR | APPROVE（无改动） | §2 |
| 6 | **结论** | **REJECT** | 见 §10 |

## 10. 阻断项与最小修复（均为文档，无代码改动）

**B1（阻断）— PARTIAL 的原因与实测相反，三份状态文件自相矛盾。**
`README.md:12`、`PROJECT_STATUS.md:8`、`LAST_HANDOFF.md:8` 都写「等设备授权安装」/「安装被设备拒绝」，而 `TASK-025` §9 记录了解锁后 `adb install` 返回 `Success`、两次 `Tests 1/1` 通过，我本轮又从零安装通过了一次。**任务状态 `PARTIAL` + 三项验收全部 `[x]` + 「spike 结论已得出」三者不自洽**：spike 的 Objective（能否跑通）已经回答，剩余工作不在本任务内。

最小修复（任选其一，二选一即可，但三份文件必须同一口径）：
1. 若本任务已完成 → 把 `TASK-025` 状态改 `DONE`（`§11` 三项勾选、`§13` 填 `APPROVE`），`README.md:12` / `PROJECT_STATUS.md:8` / `LAST_HANDOFF.md:8` 改为「`DONE`：真机 instrumented 可用，两次通过」。
2. 若要保留 `PARTIAL` → 必须写出真实剩余项（例如「等第二条设备侧用例」），并删掉「等设备授权安装」「安装被设备拒绝」。

**B2（阻断）— 被推翻的旧结论没有传播到状态文件。**
`PROJECT_STATUS.md:30`、`:43`、`:47` 与 `LAST_HANDOFF.md:45` 仍把「本机无法跑 Android 侧 / Room 测试」当作现行限制。

最小修复：把这四处改成「设备侧 instrumented 测试可行（需解锁 + `stayon usb`），缺的是用例而非通道」，保留「`gradlew test` 的单元测试 worker 仍被禁用」这条真实限制——**不要**顺手删掉它，那才是本任务没有推翻的部分。

### 非阻断建议

- **N1** `TEST_REPORT`：把 `TASK-025` 行移到表尾（最后一行的位置），或给检查器补一条「最后一行 verdict 必须是 `PASS|FAILED|NOT_RUN` 之一」的探针，否则中文 verdict 会让结果词检查恒真（§7）。
- **N2** `TEST_REPORT` `Latest Run` 的分支/commit 更新为本分支/本 commit，或在 `Runs` 表里加一列 commit。
- **N3** 把两次失败轮补成 Runs 行（`**BUILD FAILED（环境）**`，摘要写安装被拒 / 设备掉线），与 `TASK-025` §10 对称；现在 Runs 表只有成功轮，失败只存在于任务文件的表格里。
- **N4** `TASK-025` §6 第一条验收被改写成「（失败原文已记录）」，与最终通过的事实错位；建议写成「记录三次运行的真实结果（含 1 次安装被拒、1 次设备掉线）」，不要用「失败」概括整段历史。
- **N5** `LAST_HANDOFF.md:12` 写「解锁状态下安装会问一次『继续安装』」。我本轮实测：包在运行前不存在、UTP 从零安装，**没有出现任何确认**（安装授权是粘性的，不是逐次确认）。建议改为「首次需人工授权 USB 安装；之后仅需唤醒设备」。
- **N6** `README.md:14` 仍写「APK 9.4 MB」，实测 `app-debug.apk` 为 10 137 249 bytes（9.67 MB）。属更早任务遗留，本任务改 README 时顺手更新即可。
- **N7** `SmokeTest` 请勿被后续任务当作回归哨兵；下一条设备侧用例至少要启动 Activity 或断言一个真实节点（§5）。

## 11. 未验证 / 说明

- 历史运行（`36c7e95` 与更早）的原始控制台输出我无法重放：`.gradle/logs` 是忽略目录，仓库内无可核对的原始日志；我只能核对记载的内部一致性与失败签名的性质，并用自己的同条件复现作支撑。
- 安装被拒时引用的设备截图（`00:16` 锁屏）在 `.gradle/shots/`，不入库，**不可独立核对**；这属项目既有做法（`TASK-021` 的真机证据同样弱），本次不据此判分。
- 我未运行 `gradlew.bat test`（按任务纪律与本项目既定限制），因此「单元测试 worker 仍被禁用」这条我没有重新测量，仅沿用既有记录，未据此给分。
- 我未跨分支切换去对比 `main` 上的 APK 大小；「依赖没有进 APK」的结论用的是 `debugRuntimeClasspath`（0 条 `androidx.test`）+ APK 条目扫描（0 条）+ badging（无 `instrumentation`）三重证据，不依赖体积差值。
