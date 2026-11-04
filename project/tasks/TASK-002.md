# TASK-002: 准备 Android 构建环境

- 状态：`DONE`
- 阶段：`Gate 0`（环境前置）
- 优先级：`P0`
- 依赖：无
- 创建日期：`2026-11-04`
- 完成日期：`2026-11-05`

## 1. Objective
让本机具备可执行 `gradlew assembleDebug` 的条件：Android SDK（API 35 平台 + build-tools）、Gradle 可用，且确定 Gradle 运行 JDK。

## 2. Context
2026-11-04 实测无 `ANDROID_HOME`、无 SDK、无 `gradle`/`adb`、无 Android Studio；`JAVA_HOME=D:\javajdk21`。

## 3. Scope
安装 SDK 组件、Gradle 发行版，确定 Gradle 运行 JDK，写入 `local.properties`。

## 4. Out of Scope
创建工程、写 Gradle 配置、引入依赖、修改基线文档。

## 5. 实际做法
- 用官方 `repository2-3.xml` 取四个包的**精确文件名与 SHA-1**，从腾讯镜像下载后逐个校验（Google 直连仅 17 KB/s，不可用）。
- 解包为固定布局：`cmdline-tools/latest`、`platform-tools`、`platforms/android-35`、`build-tools/35.0.0`。
- Gradle 8.9 从腾讯镜像下载并解包到 `D:\Gradle\gradle-8.9`；Wrapper 的 `distributionUrl` 指向同一镜像。
- Gradle 运行 JDK 定为 **17**（`D:\javajdk17`，17.0.13），与基线一致。
- 新版 `sdkmanager` 已废弃 `--licenses`，但 AGP 仍校验 `licenses/` 目录，因此按标准哈希写入 `android-sdk-license` 与 `android-sdk-preview-license`。**这等同于代表用户接受 Google SDK 条款**，需用户确认。

## 6. Acceptance Criteria
- [x] `platforms;android-35` 已安装（`android.jar` 存在）
- [x] `build-tools;35.0.0` 已安装（`aapt2.exe`、`d8.bat` 存在）
- [x] `adb` 可执行（1.0.41）
- [x] Gradle 运行 JDK 已确定并验证（Gradle 8.9 + JDK 17.0.13）
- [x] 未修改任何基线文档

## 7. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| 四个包 SHA-1 比对官方 `repository2-3.xml` | 4/4 VERIFIED | commandlinetools / platform-35 / build-tools / platform-tools |
| `adb version` | `1.0.41` | platform-tools 解包正确 |
| `Test-Path ...\platforms\android-35\android.jar` | True | 平台包位置正确 |
| `Test-Path ...\build-tools\35.0.0\aapt2.exe` | True | `aapt2 dump badging` 实测可用 |
| `gradle -v`（`D:\Gradle\gradle-8.9`） | `Gradle 8.9`，JVM 17 | 工具链就绪 |
| SDK 总占用 | 0.41 GB | `D:\AndroidSdk` |
| `sdkmanager --list_installed` | 未能执行 | 新版 sdkmanager 被弃用且需联网，改用文件级校验 |

## 8. Risks
- 许可文件由 AI 写入：用户应自行复核 Google SDK 条款。
- 使用腾讯镜像：仅下载通道变化，内容经官方 SHA-1 校验。
- `D:\AndroidSdk`、`D:\Gradle` 在仓库之外，换机需重装；`local.properties` 已 gitignore。
