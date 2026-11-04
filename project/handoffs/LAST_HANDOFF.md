# LAST_HANDOFF.md

> 每次 AI 会话结束前更新。记录事实，不写愿望或推测。

## Snapshot
- 日期时间（含时区）：`2026-11-05 03:05 (+09:00)`
- 当前阶段：`Gate 1` — 骨架可构建；设备与单测验证未完成
- 当前任务：`TASK-003`（`PARTIAL`）；`TASK-001`、`TASK-002` 已 `DONE`
- 当前 Git 分支：`main`（本地，无远程）
- 最近 commit：以 `git log -1 --oneline` 为准；基线检查点 `d0f470a`
- `git status --short` 摘要：提交后应为干净

## Completed This Session
- 工具集中到 `D:\AndroidToolchain`：`sdk\`（原 `D:\AndroidSdk`）、`gradle\gradle-8.9\`（原 `D:\Gradle`），附 `env.ps1`、`verify.ps1`、`README.md`；旧目录已删除
- 迁移后 `clean assembleDebug` SUCCESSFUL（50s，36 tasks executed）→ 新路径端到端可用
- 新增首个单测 `AppDestinationTest` 与 `junit 4.13.2` 依赖
- 探明模拟器不可行：`HypervisorPresent=False`（未启用 WHPX），且沙箱禁止 Gradle 测试 worker 的命名管道

## Files Changed
- 新增：`app/src/test/java/com/habittracker/navigation/AppDestinationTest.kt`
- 修改：`gradle/libs.versions.toml`（junit）、`app/build.gradle.kts`（testImplementation）、`README.md`、`local.properties`（已忽略）、`project/tasks/TASK-002.md`、`project/tasks/TASK-003.md`、`project/PROJECT_STATUS.md`、`project/reports/TEST_REPORT.md`、`project/handoffs/LAST_HANDOFF.md`
- 仓库外：`D:\AndroidToolchain\{env.ps1,verify.ps1,README.md}` 与迁移后的 sdk/gradle

## Verification Actually Run
| 命令 | 结果 | 备注 |
|---|---|---|
| `clean assembleDebug`（迁移后） | SUCCESSFUL（50s） | APK 9.4 MB |
| `verify.ps1` | BUILD OK 9.4 MB | 一键脚本可用 |
| `gradlew test` | FAILED（环境） | worker 管道被沙箱阻断 |
| `adb devices` | 空 | 无设备 |
| `tools/check-governance.ps1` | 见提交前结果 | 13 项 |

## Incomplete Work
- `TASK-003` 真机/设备验证
- 单测实际执行（需非沙箱环境）
- Gate 2（Room、schema export、Migration）

## Known Issues / Risks
- 单测未跑通，不能声称测试通过
- 无设备且无 WHPX：设备验证短期无法自证
- 依赖走腾讯镜像，Maven 依赖未逐包校验
- SDK 许可文件由 AI 写入，需用户复核条款
- 设计遗留：`habits` BOOLEAN 约束未在 SQL 表达；数值撤销 UI、恢复策略未定

## Decisions Made
- 工具统一放 `D:\AndroidToolchain`；JDK 17 保留在 `D:\javajdk17`（早于本项目，可能被其他工具引用），由 `env.ps1` 指向
- 新增 junit 测试依赖（标准测试依赖，非产品依赖）

## Next Safe Action
1. 在非沙箱环境执行 `pwsh -File D:\AndroidToolchain\verify.ps1` 与 `gradlew test`，把结果补进 `TASK-003`。
2. 接真机：`adb install -r app\build\outputs\apk\debug\app-debug.apk`，走三个页面。
3. 通过后关闭 Gate 1，进 Gate 2（Room）。

## Recovery Notes
- 下一会话先核对：`git log --oneline -3`、`git status --short`、`tools/check-governance.ps1`、`adb devices`、`D:\AndroidToolchain` 是否存在
- 不要重复：不要重装 SDK；不要再次 `git init`；改基线必须同 commit 带 ADR；不要把 `temp.md` 入库
