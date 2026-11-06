# REVIEW-TASK-025: 独立评审记录（第 1–2 轮）

- 审查任务：`TASK-025`（真机 instrumented 测试可用性 spike）
- 审查 commit：第 1 轮 `82f67b0a7f47615263ab0676f8ac1147f2071669`；第 2 轮 `7b3cb62ecd1b7f941a149b0f9a6215892be44e55`（分支 `feat/androidtest-spike`，基线 `main` @ `74c3f06668291e51613d70c4f09e33332f483c01`）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，**未参与实现**，本轮只读 + 运行命令）
- 独立性：`INDEPENDENT`
- 环境：Windows；JDK 17.0.13（`JAVA_HOME=D:\javajdk17`）；Android SDK `D:\AndroidToolchain\sdk`；Gradle 8.9；真机 `V2238A - 15`（`<device>`，Android 15）
- 日期：`2026-11-05`
- 结论（第 1 轮，`82f67b0`）：**REJECT**（阻断 2 项，均为文档/状态口径；实现与测试证据本身通过）
- 结论（第 2 轮，`7b3cb62`）：**REJECT**（阻断 2 项，均为文档口径；技术证据与门禁全部通过，见 §12）
- 结论（第 3 轮，`49c51a8`）：`APPROVE`（阻断 0 项；技术证据、门禁、文档口径全部通过，见 §13）

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
  - 第二次失败记 `Android device (<device>) is not found` + `Finished 0 tests`，并明确标注「与测试代码无关」。这是 UTP 启动前的设备发现失败，性质判断正确，**没有把基础设施故障写成测试通过，也没有反过来把测试通过说成环境失败**。
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

`adb devices`：`<device>\tdevice`。运行结束后 `adb shell pm list packages com.habittracker` 为空（UTP 收尾卸载），说明本轮是**从零安装**成功，未出现安装确认阻塞。

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

## 12. 第 2 轮复审（`7b3cb62`，无代码改动）

第 1 轮的两条阻断按最小修复方案处理，我逐条复核了语义（不只是 grep 关键词），并重跑门禁。

### 12.1 本轮 diff 性质

| 项 | 实测 |
|---|---|
| `git show 7b3cb62 --stat` | 7 文件 +287/−23，**全部是文档**（含第 1 轮评审记录入库 190 行） |
| `git diff 82f67b0..7b3cb62 --stat -- app gradle` | 空 → 被测代码逐字节未变，第 1 轮的真机/变异结论在本 commit 继续有效 |
| `git diff --check` | exit 0 |
| 评审开始 `git status --short` | 空 |
| 提交的 `REVIEW-TASK-025.md` 与我第 1 轮写出的文件 | `git cat-file -s` = 20016 = 工作树大小 → **未被实现者改写** |

### 12.2 门禁重跑（HEAD=`7b3cb62`，串行）

| 步骤 | 实测 | 退出码 |
|---|---|---|
| `check-governance.ps1` | `SUMMARY: 22 checks, 22 passed, 0 warnings, 0 failed` | 0 |
| `run-unit-tests.ps1` | `OK (105 tests)` | 0 |
| `verify-dao.mjs` | `checks: 27, failed: 0` | 0 |
| `assembleDebug` | `BUILD SUCCESSFUL in 20s` | 0 |
| `git diff --check` | `no whitespace errors` | 0 |
| 合计 | `RESULT: VERIFY ALL PASS` | **0** |

### 12.3 第 1 轮阻断的落实情况

**B1（三份状态文件的 PARTIAL 原因）——已修复，语义正确。**
`README.md:12`、`PROJECT_STATUS.md:8`、`LAST_HANDOFF.md:8` 现为同一口径：「`PARTIAL`：真机 instrumented 测试**已验证可用**，等第 2 轮复审」。三处都不再声称「等设备授权安装」/「安装被设备拒绝」；剩余命中只在 `TASK-025` §9 的历史日志（`:45`，描述当时那次失败，属历史事实）与 §13 对第 1 轮阻断的复述（`:71`），都不是对当前状态的断言。**当前 PARTIAL 的原因不含任何与实测相反的内容。**

**B2（旧归因传播）——已修复，且没有反向过度修正。** 四处逐条看语义：

| 位置 | 新文本 | 我的判定 |
|---|---|---|
| `PROJECT_STATUS.md:30` | 「通道已打通，缺的是用例（`TASK-026`）；`gradlew test` 的单元测试 worker 仍被禁，这条限制仍然成立」 | 正确：撤销旧归因的同时**保留**了本任务没推翻的限制；条目仍是 `[ ]`，没有假装已有运行时测试 |
| `PROJECT_STATUS.md:43` | 「设备侧 instrumented 可用……Room/Compose 并非『无法测』，而是『还没有用例』」 | 正确：没有扩写成「已经测过」 |
| `PROJECT_STATUS.md:47` | 「SQL 层由 `verify-schema`/`verify-dao` 覆盖，Room 回调与实体映射尚无设备侧用例」，并点出 `DatabaseConstraints.callback()` 零覆盖 | 正确：`HabitDatabase.kt:55` 确实有 `.addCallback(DatabaseConstraints.callback())`，所以「是否真的注册」是**未来的回归风险**而非现状断言；「零覆盖」属实 |
| `LAST_HANDOFF.md:45` | 「无自动化验证，但通道已经有了……`TASK-026` 接手」 | 正确：保留缺口，只更新可行性 |

- [x] 结论句「只对 Gradle 单元测试 worker 成立」仍在（`TASK-025` §9、`TEST_REPORT.md:92`），没有被反向过度修正成「所有 Android 测试都能跑」。`TEST_REPORT.md:92` 写「解锁」、`LAST_HANDOFF.md:12` 写「唤醒」，是同一事实的不同粒度，不构成矛盾（非阻断 nit）。

**N1–N7 抽查（非阻断，已实质落地）**：失败轮补了两行；`Latest Run` 改本分支（`TASK-025`，基线 `74c3f06`）；APK 9.67 MB（与我的 10,137,249 B 一致）；`TASK-025` §6 第一条改为「1 次安装被拒、1 次设备掉线、2 次通过」；`LAST_HANDOFF.md:12` 改成「首次授权 + 授权粘性」并引用我的实测；`TASK-026` 已建。**N1 的根治没有做**，见 B3 与 N8。

### 12.4 本轮新发现（阻断 2 项，均为文档）

**B3（阻断）— `TASK-025.md:73` 声称做了一件没做的事。**
原文：「非阻断建议 N1–N7 已全部处理：……、**新增一条治理探针（见下）**」。
- 实测：`git diff 82f67b0..7b3cb62 -- tools` 为空，`tools/check-governance.ps1` 未被修改，仓库里不存在这条探针；「（见下）」在本文件没有所指（探针只作为**提案**出现在 `TASK-026.md` §8）。
- 这与实现者汇报里的说法（「N1 的根治留作新任务……我没有在本 commit 改 `check-governance.ps1`」）**直接相反**，属对已完成工作的不实声称（`AGENTS.md` §4/§5；本项目既有教训「声称必须可核对」）。
- 最小修复：改为「N1 的根治**未在本任务做**，已作为 `TASK-026` §8 的输入；其余 N2–N7 已处理」，删掉「新增一条治理探针（见下）」。

**B4（阻断）— `LAST_HANDOFF.md` 内部口径相反，且 `Next Safe Action` 未更新。**
- `:11`「会话状态：可继续——`TASK-022`、`TASK-024`、`TASK-025` 已 `DONE`」与 `:8`「`TASK-025`（`PARTIAL`……等第 2 轮复审）」在**同一文件里互相矛盾**；且此刻 `TASK-025` 没有任何 `APPROVE` 记录，按 `AGENTS.md` §7 它不能是 `DONE`——这正是「先 APPROVE 再置 DONE」顺序的立论，写反就把立论作废。
- `:57`（Next Safe Action 1）与 `:60`（第 4 条）仍指示「`TASK-025`：先加最小 `androidTest` 与依赖，在真机上跑一次」「其后的候选是 `TASK-025` 与 `TASK-023`」，与 `:11`「下一步是 `TASK-026`」及 Snapshot 相反。下一会话只读 Next Safe Action 会**重做一遍本任务**。
- 最小修复：`:11` 改回 `PARTIAL`（或写「第 2 轮复审通过后再置 `DONE`」）；`:57`/`:60` 的当前动作改成 `TASK-026` 并去掉 `TASK-025` 候选。三处，无代码。

### 12.5 非阻断

- **N8** 第 15 项仍未解析到真正的通过行。我复刻脚本逻辑确认：`Runs` 最后一行现为「变异……**FAILED（预期）**」→ `expected='FAILED'`，而 `Latest Run` 摘要里恰好有「`gradlew test` `FAILED`」，于是 `[PASS]` 又是因为**另一个** FAILED 字样成立；那条 `**PASS**` 行是倒数第二行，从未被读。把通过行放到表尾解决不了（表尾必然是最新的运行记录），**只有探针能修**——`TASK-026` §8 的方向对，不要用「调行序」代替它。
- **N9** `TASK-026.md` 的 `## 8. Notes`（`:67`）排在 `## 13` 之后，编号乱序；建议移到 §7 与 §9 之间或改成 §14。
- **N10** `PROJECT_STATUS.md:11` 仍写「合入 `main` 后 21-23s」，`README.md:14` 已改 20s（我本轮实测 20–21s）；两者都对但口径不一，下次统一。
- **N11** `LAST_HANDOFF.md:9` 仍写「`main` …… @ `427e7cc`（`TASK-022` 已合入）」，实际 `main` 已到 `74c3f06`（`TASK-024` 已合入）。顺手改，别写死哈希。

### 12.6 关于「先 APPROVE 再置 DONE」的顺序

**这个顺序我认可，不驳回。** 实测依据：治理第 17 项（`tools/check-governance.ps1:359-360`）要求 `DONE` 任务的评审文件里存在一条 APPROVE 结论行（反引号包住 `APPROVE`），先把 `TASK-025` 置 `DONE` 必然打红门禁，形成「没有 APPROVE 不能 DONE、没 DONE 又没得审」的死结。可执行的顺序只能是：**评审先把 APPROVE 落到本文件 → 再置 DONE → 门禁转绿 → 同一次提交带上两者**。

边界（第 3 轮请照此执行，否则本次 APPROVE 不覆盖）：置 `DONE` 的那次提交只允许改 `TASK-025.md` 的状态行 / §11 / §13 结论 / 自动摘要，加 `README.md:12`、`PROJECT_STATUS.md:8`、`LAST_HANDOFF.md:8` 的 `PARTIAL`→`DONE` 字样；**不得**夹带 `app/**`、`gradle/**`、`tools/**` 或其他文档内容改动。

### 12.7 第 2 轮逐项结论

| # | 项 | 结论 |
|---|---|---|
| 1 | diff 只对应本任务、无顺手重构 | APPROVE（纯文档 + 评审记录入库） |
| 1 | 无新依赖/权限/module | APPROVE（`app/**`、`gradle/**` 零改动） |
| 1 | `git diff --check` 干净 | APPROVE |
| 3 | TEST_REPORT 有命令/退出码/失败摘要（失败轮已补齐） | APPROVE（但通过行仍不被第 15 项读到 → N8） |
| 3 | 未执行的测试未写成通过 | APPROVE（两次环境失败标 `FAILED（环境）`，性质正确） |
| 4 | README 与 PROJECT_STATUS 一致 | APPROVE（`TASK-025`/`Gate 3` 一致） |
| 4 | 不含过期断言 | **REJECT**（B4 的 `Next Safe Action`；`LAST_HANDOFF:9` 哈希 → N11） |
| 4 | LAST_HANDOFF Snapshot 无矛盾 | **REJECT**（`:11` 说 `DONE`，`:8` 说 `PARTIAL`） |
| 4 | TEST_REPORT `Latest Run` 与 Runs 末行一致 | APPROVE（计数 105 一致；结果词巧合成立 → N8） |
| 5 | 分支策略 | APPROVE（`feat/androidtest-spike`，治理第 16 项 PASS） |
| 5 | 任务记录不伪造工作 | **REJECT**（B3「新增一条治理探针」） |
| 6 | 结论 | **REJECT**（阻断 2 项：B3、B4） |

### 12.8 第 2 轮结论与下一步

- 结论：**REJECT**（阻断 2 项，均文档口径，共约 4 行文字；技术内容、依赖隔离、真机证据、门禁全部通过，第 1 轮阻断确已修复）。
- 第 3 轮只需复核 `TASK-025.md:73`、`LAST_HANDOFF.md:11`、`:57`、`:60` 四处；若这四处之外还有 `app/**`/`gradle/**`/`tools/**` 改动，将重新完整评审。届时按 §12.6 边界写入 APPROVE 结论行。
- 未验证项：`TASK-026` §2「删掉 `addCallback` 后 105 项单测与 27 项 DAO 检查仍全绿」这一**假设**未实测（属 TASK-026 开工前提，不在本任务 Scope）；我只核对了 `HabitDatabase.kt:55` 当前确实注册了回调。

## 13. 第 3 轮复审（`49c51a8`）→ APPROVE

按第 2 轮划定的范围复核（`TASK-025` §13 的 `73–75`、`LAST_HANDOFF` `:11/:57/:60`），外加 N9/N10/N11 与整轮 diff 抽查。

### 13.1 本轮 diff 性质

| 项 | 实测 |
|---|---|
| `git show 49c51a8 --stat` | 5 文件 +109/−13，**全部落在 `project/**`** |
| `git diff 7b3cb62..49c51a8 --stat -- app gradle tools` | **空** → 无代码、无依赖、无治理脚本改动 |
| `git diff 7b3cb62..49c51a8 --name-only` | `PROJECT_STATUS.md`、`LAST_HANDOFF.md`、`REVIEW-TASK-025.md`、`TASK-025.md`、`TASK-026.md` |
| `REVIEW-TASK-025.md` 的改动 | 正是我第 2 轮写在**工作区未提交**的 §12 与表头（+99/−5），blob 与我的内容一致，**实现者未改写评审记录** |
| `git diff --check` | exit 0（实现者报过的 `TASK-026` EOF 空行已修掉） |
| `git status --short` | 空 |

### 13.2 门禁重跑（HEAD=`49c51a8`，串行）

| 步骤 | 实测 | 退出码 |
|---|---|---|
| `check-governance.ps1` | `SUMMARY: 22 checks, 22 passed, 0 warnings, 0 failed` | 0 |
| `run-unit-tests.ps1` | `OK (105 tests)` | 0 |
| `verify-dao.mjs` | `checks: 27, failed: 0` | 0 |
| `assembleDebug` | `BUILD SUCCESSFUL in 19s` | 0 |
| `git diff --check` | `no whitespace errors` | 0 |
| 合计 | `RESULT: VERIFY ALL PASS` | **0** |

与实现者自查的差异只有构建耗时（19–20s，属正常抖动），其余数字逐项相同。

### 13.3 B3 / B4 复核（逐条看语义）

**B3 —— 已修复，而且修得比我要求的多。**
`TASK-025.md:73` 现为第 2 轮阻断的自述：「B3 本条曾写『新增一条治理探针（见下）』，**不实**：`git diff 82f67b0..7b3cb62 -- tools` 为空，探针不存在」。`:74` 把 N 项拆开：「N2–N7 已处理……**N1 根治未在本任务做**，探针会改变治理检查项数（22→23）并牵动 8 处计数，已作为输入写进 `TASK-026` §8；N8 已确认调行序无助于第 15 项……不再靠排序规避」。
- 我核对：`project/` 内「新增一条治理探针」只剩 `TASK-025:73` 这一处**引述并否定**它的文本，以及本评审 §12.4 的原始记录；**没有任何地方再声称探针已存在**。
- 保留自我否定（而不是删掉原句）是正确做法：仓库里既有的教训是「声称必须可核对」，把错误留在记录里比隐去更容易被下一会话识别。这一点我明确记为正例。

**B4 —— 已修复。**
- `:11` 现为「`TASK-022`、`TASK-024` 已 `DONE` 合入 `main`；`TASK-025` 为 `PARTIAL`（通道已验证可用，**等第 2 轮复审 → 第 3 轮**，尚无 `APPROVE`，按 §7 不得视为 `DONE`）」→ 不再声称 `DONE`，且显式引用 §7，与 `:8` 的 `PARTIAL` 一致。
- `:57` 现为「`TASK-025` 已做完，只剩复审……**不要重跑 spike**」→ 下一会话不会再重做。
- `:60` 现为「再读 `TASK-026`（设备侧 Room 契约测试，核心是「删掉 `addCallback` 必须让测试变红」）；其后是 `TASK-023`」→ 候选里已无 `TASK-025`。

**N9/N10/N11 —— 已确认。** `TASK-026` 的 §8 已移到 §9 之前（章节恢复升序，原末尾副本删除）；`PROJECT_STATUS.md:11` 改为「20s；与 `README` 同口径」；`LAST_HANDOFF.md:9` 改为「`feat/androidtest-spike`（`TASK-025`，未合入）；`main` @ `74c3f06`（`TASK-024` 已合入）」——与我实测的 `main`=`74c3f06`、当前分支一致，N11 关闭。

### 13.4 非阻断（不影响本轮 APPROVE，建议在置 `DONE` 的那次提交里顺手处理）

- **N12** `TASK-025.md:75–76` 有**两条**「结论：」行，内容几乎重复（「待第 3 轮复审」/「待第 3 轮复审（第 3 轮送审范围仅……三处）」）。置 `DONE` 时请把**两条都**替换成一条 APPROVE 结论，别只改一条留下「待复审」。另：该行说「三处」，实际送审范围是 `TASK-025:73` 加 `LAST_HANDOFF:11/57/60`（四处）再加你顺手修的 N9/N10，措辞偏窄。
- **N12b** `LAST_HANDOFF.md:11` 的「等第 2 轮复审 → 第 3 轮」略滞后：第 2 轮已经 `REJECT` 并修完，现在等的是第 3 轮。建议写「第 2 轮 `REJECT` 已修，等第 3 轮 `APPROVE`」。语义不假，只是口径滞后。
- **N12c** `TASK-025.md:74` 的「牵动 8 处计数」是估计：我在 4 份状态文档（`README`/`PROJECT_STATUS`/`LAST_HANDOFF`/`TEST_REPORT`）里就数到 18 行提到「22」，改成 23 时涉及的位置明显多于 8。建议写「多处」，别给一个可被数出来的数字。
- **N13（治理盲区，新发现，建议与 N8 一起给 `TASK-026` §8）** 治理第 18 项（`check-governance.ps1:359`）用正则 `- 结论：\s*\*{0,2}\s*`APPROVE`` 在整个评审文件里找 APPROVE，**引述这个格式本身就会命中**。实证：`git show 49c51a8:project/reviews/REVIEW-TASK-025.md` 上该正则命中 **2 次**，两次都是我第 2 轮正文里引述「要求评审文件里存在该结论行」的句子——也就是说，**在没有任何 APPROVE 的情况下，只要评审文本提到这个格式，`TASK-025` 一置 `DONE` 第 18 项就会绿**。我已把这两处引述改写成「一条 APPROVE 结论行」，本文件现在只由 §13.5 的真实结论行命中。这与 N8（第 15 项结果词恒真）是同一类问题：**检查器验证的是字符串出现，不是语义**。建议 `TASK-026` §8 的探针同时覆盖两处（或另立一个小任务）。

### 13.5 第 3 轮逐项结论

| # | 项 | 结论 |
|---|---|---|
| 1 | 本轮 diff 只含 `project/**` 文档、无顺手改动 | APPROVE |
| 1 | 无新依赖/权限/module/治理脚本改动 | APPROVE（`app`、`gradle`、`tools` 零改动） |
| 1 | `git diff --check` 干净 | APPROVE |
| 3 | 门禁实测与记录一致 | APPROVE（22/22、105、27、19–20s、exit 0） |
| 4 | B3 不实声称已撤回，且不再有未做事项的声称 | APPROVE |
| 4 | `LAST_HANDOFF` Snapshot 与 Next Safe Action 自洽 | APPROVE（N12b 仅措辞滞后） |
| 4 | README / PROJECT_STATUS / 待办口径一致 | APPROVE（`TASK-025` `PARTIAL` 三处同口径，`TASK-026` 已入 Next Safe Action） |
| 5 | 分支策略 | APPROVE（`feat/androidtest-spike`；置 `DONE` 前必须先有本 APPROVE） |
| 6 | 结论 | **APPROVE**（阻断 0 项） |

### 13.6 评审结论

- 结论：`APPROVE`
- 覆盖范围：commit `49c51a8e0a4f15f043674e31cb8dfa48a8857d6a`（分支 `feat/androidtest-spike`，基线 `main` @ `74c3f06`）。第 1–3 轮共 4 条阻断（B1–B4）全部关闭，0 条遗留。
- 本 APPROVE 允许的后续动作（边界，超出即失效）：置 `DONE` 的那次提交只允许改 `TASK-025.md` 的状态行 / §11 / §13 结论（含 N12 的两条结论行合并）/ 自动摘要，以及 `README.md:12`、`PROJECT_STATUS.md:8`、`LAST_HANDOFF.md:8` 的 `PARTIAL`→`DONE` 字样；**不得**夹带 `app/**`、`gradle/**`、`tools/**` 或其他文档内容。
- 合入前请再跑一次 `pwsh -NoProfile -File tools/verify-all.ps1`（置 `DONE` 后、合入 `main` 前），确认第 18 项在真实 APPROVE 下转绿；合入 `main` 后在 `main` 上再跑一次，与 `TASK-009`/`TASK-024` 的做法保持一致。
- 遗留（不阻塞本任务，已记录给 `TASK-026`）：N1/N8（第 15 项结果词恒真）、N13（第 18 项可被引述命中）、`TASK-026` §2 的「删 `addCallback` 后仍全绿」假设未实测、Compose 冒烟仍无设备侧用例。
