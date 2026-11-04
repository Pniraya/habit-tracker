# TASK-003: Gate 1 —— Android 单模块工程骨架

- 状态：`PARTIAL`（构建通过；真机安装与导航尚未验证）
- 阶段：`Gate 1`
- 优先级：`P0`
- 依赖：`TASK-001`（已完成）、`TASK-002`（已完成）
- 创建日期：`2026-11-04`
- 完成日期：`2026-11-05`（构建部分）

## 1. Objective
创建可同步、可构建的单模块 Android 工程骨架：`:app`、Version Catalog、Compose + Material 3、浅色主题 token、三页导航壳（今日 / 统计 / 我的）。不含业务逻辑与 Room。

## 2. Context
`ARCHITECTURE.md` §2、§4、§10；`DEVELOPMENT.md` §2.2、Phase 0；`project/ROADMAP.md` Gate 1；视觉参考 `orgins/`（只读）。

## 3. 实施结果
- 版本组合固定在 `gradle/libs.versions.toml`：AGP 8.7.3、Kotlin 2.0.21、Compose BOM 2024.10.01、Navigation 2.8.3、lifecycle 2.8.6、activity-compose 1.9.3、core-ktx 1.13.1。
- `compileSdk 35`、`targetSdk 35`、`minSdk 26`、`buildToolsVersion 35.0.0`、JVM target 17。
- Manifest 仅 `POST_NOTIFICATIONS`、`RECEIVE_BOOT_COMPLETED`；无精确闹钟权限、无网络权限。
- 主题：仅浅色，色值取自概念稿（`#FAF7F2`、`#5B8C6A`、`#F2A65A`、`#8FAFC4`、`#1C1B1A` + 文本/描边/surface token）。
- 导航：`AppDestination` 三入口 + `NavHost` + `NavigationBar`，三个占位页面，文案明确写“尚未实现”，不伪造业务数据。

## 4. Out of Scope（未做）
Room Entity/DAO、Repository、UseCase、提醒调度、备份、统计计算、深色模式、小组件、任何测试用例。

## 5. 与计划的偏差
- 计划写“Room schema export 目录占位”：实际未加入 Room，留到 Gate 2 与 Room 一起配置，避免无用依赖。
- 计划未提依赖源：Google Maven 直连仅 17 KB/s，`settings.gradle.kts` 把腾讯镜像放首位，`google()`/`mavenCentral()` 作为回退。属本地网络适配，可移除。
- 计划未提 `buildToolsVersion`：AGP 8.7.3 默认要 34.0.0，本机只装 35.0.0，故显式固定。

## 6. Acceptance Criteria
- [x] `gradlew.bat assembleDebug` 成功，产出 APK（9.4 MB）
- [ ] Android 15 设备可安装并启动、三页可导航 —— **未验证：无真机、无模拟器镜像**
- [x] Manifest 无 `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`
- [x] 无网络权限、无广告/账号/云 SDK
- [x] 主题仅浅色，色值集中在 Design System
- [x] `FEATURE_MATRIX` F-001/F-002 状态与证据一致

## 7. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `gradlew.bat assembleDebug` | SUCCESSFUL（2m 38s，35 tasks） | `app/build/outputs/apk/debug/app-debug.apk`，9.4 MB |
| `aapt2 dump badging` | `com.habittracker`，minSdk 26，targetSdk 35，compileSdk 35 | launcher = `com.habittracker.MainActivity` |
| `gradlew.bat test` | BUILD SUCCESSFUL，全部 NO-SOURCE | 尚无测试用例，不得记为“测试通过” |
| `adb devices` | 空列表 | 无设备；`emulator` 与 `system-images` 未安装 |
| 真机安装 / 导航 | NOT_RUN | 缺少设备与模拟器 |

## 8. Risks
- 未做真机验证：Compose 运行时崩溃、返回栈、边缘布局都可能在设备上暴露问题。
- 无任何单元测试，`test` 的 SUCCESS 只代表任务可执行。
- 镜像仓库若失效会回退到 `google()`（很慢）而非失败。

## 9. Handoff
- 剩余工作：设备/模拟器验证；补 `system-images;android-35;google_apis` 与 `emulator` 包后可自证。
- 下一条安全操作：有设备时 `adb install -r app-debug.apk` 并手动走三个页面。
