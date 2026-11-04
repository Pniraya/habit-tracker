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

## 3. 实际做法
- 用官方 `repository2-3.xml` 取四个包的精确文件名与 SHA-1，从腾讯镜像下载后逐个校验（Google 直连仅 17 KB/s，不可用）。
- 全部工具集中到 **`D:\AndroidToolchain`**：`sdk\`（原 `D:\AndroidSdk`）、`gradle\gradle-8.9\`（原 `D:\Gradle`），并附 `env.ps1`、`verify.ps1`、`README.md`。
- Gradle Wrapper 的 `distributionUrl` 指向腾讯镜像，换机首次构建不会卡在 Google CDN。
- Gradle 运行 JDK 定为 **17**（`D:\javajdk17`，17.0.13）。该 JDK 早于本项目存在，可能有其他工具引用，因此**没有**移入工具目录，由 `env.ps1` 指向。
- 新版 `sdkmanager` 已废弃 `--licenses`，但 AGP 仍校验 `licenses/` 目录，故按标准哈希写入 `android-sdk-license` 与 `android-sdk-preview-license`。**等同于代表用户接受 Google SDK 条款**，需复核。

## 4. Acceptance Criteria
- [x] `platforms;android-35` 已安装（`android.jar` 存在）
- [x] `build-tools;35.0.0` 已安装（`aapt2.exe`、`d8.bat`）
- [x] `adb` 可执行（1.0.41）
- [x] Gradle 运行 JDK 已确定并验证（Gradle 8.9 + JDK 17.0.13）
- [x] 工具集中在单一目录，`D:\AndroidSdk` 与 `D:\Gradle` 已不存在
- [x] 未修改任何基线文档

## 5. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| 四包 SHA-1 比对官方 `repository2-3.xml` | 4/4 VERIFIED | commandlinetools / platform-35 / build-tools / platform-tools |
| `adb version` | `1.0.41` | platform-tools 解包正确 |
| `Test-Path D:\AndroidToolchain\sdk\platforms\android-35\android.jar` | True | 迁移后路径有效 |
| `Test-Path D:\AndroidToolchain\gradle\gradle-8.9\bin\gradle.bat` | True | Gradle 迁移后可用 |
| 迁移后 `verify.ps1`（`gradlew assembleDebug`） | 见 `TEST_REPORT` | 端到端复验 |
| SDK 占用 | 0.41 GB | 工具目录总计约 0.54 GB |

## 6. Risks
- 许可文件由 AI 写入：用户应自行复核 Google SDK 条款。
- 使用腾讯镜像：仅下载通道变化，SDK 包经官方 SHA-1 校验。
- 工具目录在仓库之外，换机需重装；`local.properties` 已 gitignore。
