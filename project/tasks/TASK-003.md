# TASK-003: Gate 1 —— Android 单模块工程骨架

- 状态：`PARTIAL`（构建与清理构建验证通过；单测与真机验证未完成）
- 阶段：`Gate 1`
- 优先级：`P0`
- 依赖：`TASK-001`、`TASK-002`（均 `DONE`）
- 创建日期：`2026-11-04`
- 完成日期：`TBD`

## 1. Objective
创建可同步、可构建的单模块 Android 工程骨架：`:app`、Version Catalog、Compose + Material 3、浅色主题 token、三页导航壳。不含业务逻辑与 Room。

## 2. Context
`ARCHITECTURE.md` §2、§4、§10；`DEVELOPMENT.md` §2.2；`ROADMAP.md` Gate 1；视觉参考 `orgins/`（只读）。

## 3. 实施结果
- 版本组合固定在 `gradle/libs.versions.toml`：AGP 8.7.3、Kotlin 2.0.21、Compose BOM 2024.10.01、Navigation 2.8.3、lifecycle 2.8.6、activity-compose 1.9.3、core-ktx 1.13.1、junit 4.13.2。
- `compileSdk 35`、`targetSdk 35`、`minSdk 26`、`buildToolsVersion 35.0.0`、JVM target 17。
- Manifest 仅 `POST_NOTIFICATIONS`、`RECEIVE_BOOT_COMPLETED`；无精确闹钟权限、无网络权限。
- 主题：仅浅色，色值取自概念稿；`AppDestination` + `NavHost` + `NavigationBar` 三页骨架，页面文案明写“尚未实现”。
- 新增首个单元测试 `AppDestinationTest`（路由唯一性、路由契约、标签非空），并加入 `junit` 测试依赖。

## 4. 未完成 / 未验证
- **真机或模拟器安装与导航**：`adb devices` 为空；`HypervisorPresent=False`，本机未启用 WHPX/Hyper-V，x86_64 模拟器无法运行；未安装 `emulator`/`system-images`。
- **单元测试未能在本会话执行**：Gradle 测试执行器需要 fork 带管道的 worker 进程，本会话沙箱禁止命名管道，报 `java.io.IOException: 管道正在被关闭` 与 `ClassNotFoundException: worker.org.gradle.process.internal.worker.GradleWorkerMain`。该失败与测试代码无关，但也没有证据表明测试会通过。

## 5. 与计划的偏差
- “Room schema export 目录占位”未做：Room 留到 Gate 2 一起配置，避免无用依赖。
- 依赖源：`settings.gradle.kts` 把腾讯镜像放首位（Google Maven 直连 17 KB/s），官方源作回退。
- `buildToolsVersion` 显式固定 35.0.0：AGP 8.7.3 默认要 34.0.0，本机只装 35.0.0。

## 6. Acceptance Criteria
- [x] `gradlew.bat assembleDebug` 成功，产出 APK（9.4 MB）
- [x] `gradlew.bat clean assembleDebug` 在新工具目录下成功（50s，36 tasks executed）
- [ ] Android 15 设备可安装并启动、三页可导航 —— **未验证：无设备、模拟器不可用**
- [x] Manifest 无 `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`
- [x] 无网络权限、无广告/账号/云 SDK
- [x] 主题仅浅色，色值集中在 Design System
- [x] `FEATURE_MATRIX` F-001/F-002 状态与证据一致

## 7. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `gradlew.bat assembleDebug` | SUCCESSFUL（2m 38s，35 tasks） | APK 9.4 MB |
| `gradlew.bat clean assembleDebug`（迁移后） | SUCCESSFUL（50s，36 tasks executed） | 新工具目录端到端可用 |
| `aapt2 dump badging` | `com.habittracker`，min 26，target 35 | launcher `com.habittracker.MainActivity` |
| `gradlew.bat test` | FAILED（环境） | `ClassNotFoundException: worker.org.gradle.process.internal.worker.GradleWorkerMain`，沙箱禁止 worker 管道 |
| `adb devices` | 空 | 无设备 |
| 模拟器可行性 | `HypervisorPresent=False` | 无 WHPX/Hyper-V，x86_64 镜像无法启动 |

## 8. Risks
- 设备验证缺失：Compose 运行时问题、返回栈、边缘布局都可能在设备上暴露。
- 单测只写未跑：当前无通过证据。
- 镜像仓库失效会回退到 `google()`（很慢）。

## 9. Handoff
- 剩余工作：真机验证；或在非沙箱环境跑一次 `gradlew test`。
- 下一条安全操作：接真机 → `adb install -r app\build\outputs\apk\debug\app-debug.apk` → 走三个页面。
