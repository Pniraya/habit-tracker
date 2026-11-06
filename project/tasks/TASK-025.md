# TASK-025: 真机 instrumented 测试可用性 spike

- 状态：`PARTIAL`
- 阶段：`Gate 3`
- 优先级：`P1`
- 依赖：无（但设备必须连接）
- 创建日期：`2026-11-05`
- 完成日期：待设备授权后

## 1. Objective
确认 `connectedDebugAndroidTest` 能否在本机 + USB 真机上跑通。若可，Room 映射、迁移、Compose 冒烟测试一次性获得落点；若不可，记录实际报错，改为 `adb shell uiautomator dump` 的脚本断言或明确接受缺口。

## 2. Context
外部评审指出：失败的是 Gradle **单元测试 worker**（命名管道被禁），而 instrumented 测试走设备侧 runner，可能不受影响。这是当前最大的未验证假设，卡住了 UI 与 Room 的自动化。

## 3. Scope
- 加 `androidTestImplementation`（`androidx.test.ext:junit`、`androidx.test:runner`、必要时 compose ui-test），写一个最小测试（如启动 Activity 后断言某文本存在）。
- 在真机上执行 `gradlew.bat connectedDebugAndroidTest`，记录实际结果。
- 按结果更新 `TEST_REPORT` 的「Known Test Gaps」。

## 4. Out of Scope
- 不追求覆盖率；通过后再另立任务写冒烟用例。
- 不改 `verify-all`（设备测试不放进默认门禁）。

## 5. Expected Files
- 修改：`app/build.gradle.kts`、`gradle/libs.versions.toml`
- 新增：`app/src/androidTest/...` 最小用例

## 6. Acceptance Criteria
- [x] 给出 `connectedDebugAndroidTest` 在本机的真实结果（失败原文已记录）
- [x] 若通过：至少 1 条设备侧用例稳定通过 2 次（run1 3m09s、run3 40s）
- [x] 若失败：记录报错（第二次为设备掉线，已记录原文）

## 7. Tests
- `gradlew.bat connectedDebugAndroidTest`
- `pwsh -NoProfile -File tools/verify-all.ps1`（确认未破坏既有门禁）

## 8. Risks and Decisions
- 新增测试依赖属于 `androidTestImplementation`，不影响运行时包体与离线保证；仍需确认不含网络库。
- 设备测试慢且依赖插线，不放入默认门禁。

## 9. Implementation Log
- 2026-11-05：开工。加 `androidTestImplementation`（`androidx.test:runner` 1.6.2、`androidx.test.ext:junit` 1.2.1）与 `testInstrumentationRunner`，写最小用例 `SmokeTest.theAppUnderTestIsInstalled`。
- 2026-11-05：**关键结论——runner 路径本身可用**。`gradlew.bat connectedDebugAndroidTest` 完成了 66 个任务（编译、打包 androidTest APK、连接设备、尝试安装），说明设备侧 runner 不受「Gradle 单元测试 worker 被禁」影响。外部评审的推断成立。
- 2026-11-05：**但安装被设备拒绝**：`INSTALL_FAILED_ABORTED: User rejected permissions`；`adb install` 手动安装 app 与 androidTest 两个 APK 同样失败。截图显示设备处于**锁屏**状态（`00:16`）。vivo/iQOO 在锁屏或未授权「USB 安装」时会自动拒绝。这不是沙箱限制，是设备策略 + 需要人工确认。
- 2026-11-05：解锁设备后重跑，**通过**：`adb install` 返回 `Success`，`Starting 1 tests on V2238A - 15` → `Tests 1/1 completed (0 failed)` → `BUILD SUCCESSFUL in 3m 9s`；XML `tests="1" failures="0"`。
- 2026-11-05：第二次重跑**失败，但与测试无关**：`Android device (10CDAK0B71005KU) is not found`，即设备在中途掉线/休眠；`Finished 0 tests`。
- 2026-11-05：执行 `adb shell svc power stayon usb` 保持唤醒后第三次重跑**通过**（`BUILD SUCCESSFUL in 40s`）。因此「稳定通过」成立的前提是：**设备保持连接与唤醒**，否则 UTP 会在启动前就报告设备不存在。
- 结论：`connectedDebugAndroidTest` 在本机可用。此前「本机无法跑 Android 侧测试」的结论只对 **Gradle 单元测试 worker** 成立，对设备侧 runner 不成立。Room 映射、迁移、Compose 冒烟测试现在有落点。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `gradlew.bat connectedDebugAndroidTest` | **BUILD FAILED**（2m41s，66 tasks） | 失败在安装阶段而非测试阶段：`INSTALL_FAILED_ABORTED: User rejected permissions`；`TEST-V2238A - 15-_app-.xml` 记录 `tests="0"` |
| `adb install -r -t` app APK / androidTest APK | **均失败** | 同上错误；设备锁屏时 vivo 自动拒绝 |
| 设备屏幕截图 | 锁屏，`00:16` | 解释拒绝原因；也证明设备时间已跨午夜 |
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS** | 治理 22/22、单测 105、DAO 27；新增 androidTest 依赖不影响离线保证（第 22 项仍过） |
| `gradlew.bat connectedDebugAndroidTest`（第二次，设备掉线） | **BUILD FAILED**（45s） | `Finished 0 tests`；`Android device (10CDAK0B71005KU) is not found`，与测试代码无关 |
| `adb shell svc power stayon usb` 后第三次重跑 | **BUILD SUCCESSFUL**（40s） | `Tests 1/1 completed (0 failed)` |

## 11. Completion Review
- [ ] 验收标准满足
- [ ] 测试实际运行
- [ ] 独立 Reviewer `APPROVE`

## 12. Handoff
- 之后：若通过，用设备侧测试补 Room 映射与 Compose 冒烟；统计页见 `TASK-023`。

## 13. Independent Review
- Reviewer record：待创建
- 结论：`PENDING`

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-025
- 状态：`PARTIAL`
- 阶段：Gate 3
- 验收：通过 3 项 / 未通过 0 项
- 最近测试记录：2026-11-05 / **OK (105 tests)**
<!-- close-task:end -->
