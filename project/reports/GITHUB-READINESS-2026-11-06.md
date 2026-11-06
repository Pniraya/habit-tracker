# GitHub 预上传评估（2026-11-06）

- 结论：**可以进入「预上传整理」，整理已完成；但不宜直接以「正式发布」姿态公开。**
- 基线：`main` @ `dc7e37c`；仓库 241 个受版本控制的文件，`git count-objects` 约 0.94 MiB（pack），无远程。
- 依据：两次外部评审结论 + 实际扫描；本文只写可复现的事实。

## 1. 结论摘要

| 维度 | 状态 | 说明 |
|---|---|---|
| 代码与构建 | 可公开 | `assembleDebug`、159 项单测、schema 40/40、DAO 38/38 全绿；设备侧 6 项契约测试通过 |
| 仓库卫生 | 通过 | 未跟踪 `local.properties`、`*.jks`、`*.apk`、`.gradle/`、截图或数据库备份；`.gitignore` 覆盖签名、密钥、构建产物 |
| 隐私 | 已处理 | 文档中的设备序列号已脱敏为 `<device>`（8 个文件）；工具链绝对路径改为环境变量优先 |
| 可复现性 | 提升 | 新增 `.github/workflows/android.yml`：Ubuntu + JDK 17 + Android SDK + Node 22，跑 `assembleDebug test` 与 schema/DAO 校验 |
| 许可证 | **阻塞** | 无 `LICENSE`。未选择即默认「保留所有权利」，公开仓库会被视为专有代码 |
| 质量门槛 | **阻塞（可标注为 WIP 绕过）** | 8 个任务为 `IMPLEMENTED`（`TASK-026/027/028/029/031/032/033/034/035`），缺 `ADR-015` 要求的独立评审 |
| 提交身份 | 待确认 | 135 次提交的作者均为 `Habit Tracker Baseline <baseline@localhost>`，GitHub 不会把它关联到任何账号 |
| Release 构建 | 未验证 | 只有 `assembleDebug`；`assembleRelease`（R8/资源压缩）与 `lint` 从未运行（外部评审 R6） |
| 远程 | 无 | `git remote -v` 为空；本仓库不会自动推送（`AGENTS.md` §7/§13） |

## 2. 本次已完成的整理

1. **设备序列号脱敏**：`10CDAK0B71005KU` → `<device>`，涉及 8 个文件（评审记录、任务、测试报告、交接）。评审结论与命令输出不变，只去掉设备标识。
2. **工具链路径可移植**：`tools/run-unit-tests.ps1`、`tools/verify-all.ps1` 不再写死 `D:\AndroidToolchain\env.ps1`；优先使用已设置的 `ANDROID_HOME`/`JAVA_HOME`，其次 `HABIT_ENV_SCRIPT`，最后尝试同级 `../AndroidToolchain/env.ps1`；都缺失时明确报错而不是静默跳过。
3. **README 发布前置说明**：新增「发布状态（未发布 WIP）」「开源/发布前检查」两节，并修正过期的命令说明（`40 项` → `159 项`）与 2026-11-05 的旧「下一步」。
4. **CI**：`.github/workflows/android.yml`。选择 Ubuntu 而非 Windows，因为本地唯一不可用的 `gradlew test` 在 GitHub Runner 上正常，159 项单测因此有了第二处执行环境。
5. **本报告**：把阻塞项与已处理项分开记录，避免「看起来准备好了」。

## 3. 公开前必须由所有者决定的事

1. **许可证**：MIT / Apache-2.0 / 其他，或明确「source-available, all rights reserved」。建议 MIT（个人 Android 应用最常见的宽松许可），但这是法律决定，不由 AI 代选。
2. **提交身份**：是否把作者改为自己的 GitHub 邮箱/用户名。若保留现有身份，可在 `.mailmap` 里映射，但 GitHub 仍不会关联贡献图。
3. **仓库形态**：公开 or 私有；是否把 `project/` 的治理文档一并公开（当前设计是全部公开，便于复现）。
4. **发布定位**：作为 WIP 仓库公开（README 已标注）还是等 `R1` 评审把 8 个任务转 `DONE` 后再公开。
5. **`assembleRelease`/lint**：公开前是否先各跑一次并把结果写进 `RELEASE_REPORT.md`。

## 4. 若决定公开，建议顺序

1. 选定许可证 → 新增 `LICENSE`（同 commit 更新 README 第 7 节，把「未选定」改为实际许可证）。
2. 统一提交身份（`git config user.name/user.email`，或首次推送前 `rebase`/new commits only）。
3. `git remote add origin <url>` → `git push -u origin main`（由所有者执行）。
4. 推送后确认 GitHub Actions 第一次运行：`./gradlew assembleDebug test` + `verify-schema.mjs` + `verify-dao.mjs` 全绿。
5. 打 `v0.1.0-wip` tag 并在 `RELEASE_REPORT.md` 写「未发布、无提醒/备份」的范围声明（可选；正式版等提醒与备份落地）。

## 5. 未解决风险（不阻塞公开，但会被外部看到）

- 8 个 `IMPLEMENTED` 任务与 3 个 `PARTIAL` 例外：状态文件已如实标注，评审债不是隐瞒。
- 无 UI 自动化；界面证据只有手测记录与截图（截图未入库）。
- `gradlew test` 在本机失败是环境限制（Gradle 测试 worker 被沙箱禁），CI 与 `tools/run-unit-tests.ps1` 是替代证据。
- 依赖走腾讯镜像；公开仓库的构建脚本未启用 dependency verification（外部评审 R9）。
