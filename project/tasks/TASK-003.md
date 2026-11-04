# TASK-003: Gate 1 —— Android 单模块工程骨架

- 状态：`NOT_STARTED`
- 阶段：`Gate 1`
- 优先级：`P0`
- 依赖：`TASK-001`、`TASK-002`
- 创建日期：`2026-11-04`

## 1. Objective
创建可同步、可构建的单模块 Android 工程骨架：`:app`、Version Catalog、Compose + Material 3、浅色主题 token、三页导航壳（今日 / 统计 / 我的）。不含任何业务逻辑与 Room。

## 2. Context
`ARCHITECTURE.md` §2、§4、§10；`DEVELOPMENT.md` §2.2、Phase 0；`project/ROADMAP.md` Gate 1；视觉参考 `orgins/`（只读，不作生产代码）。

## 3. Scope
- `settings.gradle.kts`、根 `build.gradle.kts`、`gradle/libs.versions.toml`、Gradle Wrapper。
- `app/build.gradle.kts`：compileSdk 35、targetSdk 35、minSdk 26、JVM target 17、Compose、M3、Navigation、ViewModel、Coroutines。
- `AndroidManifest.xml`：仅 V1 必需权限（`POST_NOTIFICATIONS`、`RECEIVE_BOOT_COMPLETED`）；不申请精确闹钟权限。
- 主题 token（5 个品牌色 + 概念稿的文本/描边/tint）、`MainActivity`、三页导航壳与空状态。
- Room schema export 目录占位（不建表）。

## 4. Out of Scope
Room Entity/DAO、Repository、UseCase、提醒调度、备份、统计计算、深色模式、小组件。

## 5. Acceptance Criteria
- [ ] `gradlew.bat assembleDebug` 成功，产出 APK
- [ ] Android 15 设备可安装并启动，三个页面可导航
- [ ] Manifest 无 `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`
- [ ] 无网络权限、无广告/账号/云 SDK
- [ ] 主题仅浅色，颜色来自 Design System，不散落硬编码
- [ ] `FEATURE_MATRIX` F-001/F-002 状态与实际证据一致

## 6. Tests
- `gradlew.bat assembleDebug`、`gradlew.bat test`（此时可无业务测试，但要能执行）
- 真机：安装启动 + 导航回归

## 7. Risks
- 无 SDK 时无法编译；必须先完成 `TASK-002`。
- 版本组合（AGP/Kotlin/Compose BOM/Room）需一次定死并写入 Version Catalog，避免后期漂移。
- 不得为“跑通”引入未批准依赖或降级 SDK。

## 8. Implementation Log
（待执行）

## 9. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| NOT_RUN | NOT_RUN | 尚未创建工程 |
