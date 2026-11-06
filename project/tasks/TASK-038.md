# TASK-038: 应用图标与 GitHub 发布配套

- 状态：`IMPLEMENTED`（`ADR-015`：实现与自测完成，未经独立评审）
- 阶段：`Gate 3`
- 优先级：`P2`
- 依赖：`TASK-036`
- 创建日期：`2026-11-06`
- 完成日期：`2026-11-06`

## 1. Objective
把用户提供的图标接入应用，并按 `GITHUB-PUBLISH-GUIDE-2026-11-06.md` 补齐发布前缺的仓库卫生与社区文件，使仓库达到「可私有推送、检查后转公开」的状态。

## 2. Context
此前 `AndroidManifest.xml` 没有 `android:icon`，应用使用系统默认图标；`res/mipmap-*` 不存在。用户于 2026-11-06 提供 1254×1254 PNG（`docs/images/icon.png`）并附发布指南，要求按指南整理。

## 3. Scope
- 应用图标：从源图生成 mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi 五档 `ic_launcher.png`；Manifest 增加 `android:icon` 与 `android:roundIcon`。
- 仓库卫生：新增 `.gitattributes`（换行规范化、`gradlew` 强制 LF、二进制文件标记）；`git update-index --chmod=+x gradlew`；`.gitignore` 补 `.claude/settings.local.json`。
- 社区文件：`SECURITY.md`（GitHub 私有漏洞报告）、`CONTRIBUTING.md`（欢迎 Issue、暂不接受 PR 并说明原因）。
- README：加 4 张截图（裁掉状态栏）、通用构建命令（`assembleDebug`/`testDebugUnitTest`/`installDebug`）、隐私与 AI 说明、图标来源。
- 保存指南原文到 `project/reports/GITHUB-PUBLISH-GUIDE-2026-11-06.md`。

## 4. Out of Scope
- 未适配 Android 13 主题图标（monochrome 层）；源图是彩色插画，单色层需要另一份素材。
- 未做自适应图标（`mipmap-anydpi-v26`）：源图自带圆角绿底，直接作为 legacy 图标由启动器裁切，视觉可接受；后续可用「绿底 + 前景」重做。
- 英文 README、CODE_OF_CONDUCT、Issue/PR 模板（可选）。
- 推送远程、改提交身份、发布 Release。

## 5. Expected Files
- 新增：`app/src/main/res/mipmap-*/ic_launcher.png`（5）、`.gitattributes`、`SECURITY.md`、`CONTRIBUTING.md`、`docs/images/{icon,today,stats,detail,create}.png`、`project/tasks/TASK-038.md`、`project/reports/GITHUB-PUBLISH-GUIDE-2026-11-06.md`
- 修改：`AndroidManifest.xml`、`README.md`、`.gitignore`、`gradlew`（可执行位）、`PROJECT_STATUS.md`、`TEST_REPORT.md`、`LAST_HANDOFF.md`

## 6. Acceptance Criteria
- [x] `aapt2 dump badging` 显示 5 档 `application-icon-*` 全部指向 `res/mipmap-*/ic_launcher.png`
- [x] 安装后应用仍启动（今日页正常）
- [x] 截图裁掉状态栏（时间、通知图标不入库）
- [x] 当前文件与历史均无密钥；无 `local.properties`/`*.jks`/`*.apk` 被跟踪
- [x] `gradlew` 在索引中为可执行（`100755`）
- [x] README 含简介、截图、已实现/计划中、隐私、构建、许可证
- [x] `tools/verify-all.ps1` 全绿

## 7. Tests
- `gradlew.bat assembleDebug`：**BUILD SUCCESSFUL**（7s）
- `aapt2 dump badging`：5 档图标齐全
- 真机安装：`Success`，`versionName=0.1.0`，应用启动
- `git grep` 密钥扫描：只命中 `.gitignore`/`DEVELOPMENT.md` 等文字里的 "secret"/"token"，无真实凭据
- `tools/verify-all.ps1`：见证据表

## 8. Risks and Decisions
- **图标是位图**：缩小到 48dp 时细节会糊；若启动器显示效果差，下一步改为「纯绿底 + 前景图」自适应图标（需要重新排版素材）。
- **源图含白色贴纸描边**：作为 legacy 图标时，部分启动器会再加一层圆角遮罩，边缘可能出现双层白边；安装后未见明显异常，记录待观察。
- **不引入 `flutter_launcher_icons` 之类的工具**：一次性缩图用 `System.Drawing` 完成，不新增依赖。
- **不受支持的备选**：把 1254 原图直接放进 `res/drawable` 会让 APK 多 1.2 MB，且无任何好处。

## 9. Implementation Log
- 2026-11-06：从 1254×1254 源图生成 5 档 PNG（48/72/96/144/192），Manifest 接入；`aapt2` 与真机安装确认。
- 2026-11-06：截图用 `System.Drawing` 裁掉顶部 130px 状态栏后放入 `docs/images/`。
- 2026-11-06：补 `.gitattributes`、`gradlew` 可执行位、`.gitignore`、`SECURITY.md`、`CONTRIBUTING.md`，并按指南重写 README 的构建/隐私/发布章节。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `gradlew.bat assembleDebug` | **BUILD SUCCESSFUL** | 7s；图标资源打包成功 |
| `aapt2 dump badging app-debug.apk` | **PASS** | `application-icon-120/160/240/320/480/640` 均指向 mipmap `ic_launcher` |
| `adb install -r` + 启动 | **PASS** | `Success`；`versionName=0.1.0`；今日页正常 |
| `git grep` 密钥扫描 | **PASS** | 仅文档用词命中，无真实凭据；历史无 `keystore`/`local.properties` |
| `tools/verify-all.ps1` | **VERIFY ALL PASS** | 治理 22/22、单测 159、DAO 38、构建、`git diff --check` |

## 11. Completion Review
- [x] 验收标准满足
- [x] 测试实际运行
- [ ] 独立 Reviewer `APPROVE`（`ADR-015` 批量评审）

## 12. Handoff
- 待用户决定：是否要自适应/单色主题图标；提交身份；公开或私有；是否接受 PR。
- 推送后按指南 B3/C 检查文件树、About、Topics、Community Standards。

## 13. Independent Review
- Reviewer record：待创建（批量评审）
- 结论：`PENDING`（`IMPLEMENTED`）

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-038
- 状态：`IMPLEMENTED`
- 阶段：Gate 3
- 验收：通过 7 项 / 未通过 0 项
- 最近测试记录：2026-11-06 / **OK (159 tests)**
<!-- close-task:end -->
