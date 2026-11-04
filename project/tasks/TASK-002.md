# TASK-002: 准备 Android 构建环境

- 状态：`NOT_STARTED`
- 阶段：`Gate 0`（环境前置）
- 优先级：`P0`
- 依赖：无（可与 `TASK-001` 并行）
- 阻塞：需要用户批准安装与 JDK 决策
- 创建日期：`2026-11-04`

## 1. Objective
让本机具备可执行 `gradlew assembleDebug` 的条件：Android SDK（API 35 平台 + build-tools）、Gradle 可用，且确定 Gradle 运行 JDK。

## 2. Context
2026-11-04 实测：无 `ANDROID_HOME`、无 `%LOCALAPPDATA%\Android\Sdk`、无 `gradle`/`adb`、无 Android Studio；`JAVA_HOME=D:\javajdk21`，另有 `D:\javajdk17`；基线要求 JDK 17（`ARCHITECTURE.md` §2、`DEVELOPMENT.md` §2.1）。

## 3. Scope
- 安装 Android Studio 或 cmdline-tools + platform-tools + platforms;android-35 + build-tools。
- 接受 SDK licenses。
- 决定 Gradle 运行 JDK（推荐 17，与基线一致），并记录到 ADR 或任务记录。

## 4. Out of Scope
创建工程、写 Gradle 配置、引入依赖、修改基线文档。

## 5. Acceptance Criteria
- [ ] `sdkmanager --list` 可执行，且 `platforms;android-35` 已安装
- [ ] `adb version` 可执行
- [ ] Gradle 运行 JDK 已确定并记录（`java -version` 输出为证据）
- [ ] 未修改任何项目文档（除本任务记录）

## 6. Tests
| 命令 | 期望 |
|---|---|
| `sdkmanager --version` | 输出版本号 |
| `sdkmanager --list_installed` | 含 `platforms;android-35` |
| `adb version` | 输出版本号 |

## 7. Risks
- 安装体积与路径选择影响后续 `local.properties`；该文件已被 `.gitignore` 忽略。
- JDK 21 与 17 混用会造成 `Unsupported class file major version` 类错误，必须先定版本。

## 8. Implementation Log
（待执行）

## 9. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| NOT_RUN | NOT_RUN | 尚未安装 |
