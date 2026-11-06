# LAST_HANDOFF.md

> 每次 AI 会话结束前更新。记录事实，不写愿望或推测。

## Snapshot
- 日期时间（含时区）：`2026-11-06`（Asia/Shanghai）
- 当前阶段：`Gate 3`（出口 = `TASK-026` 完成 + `R1` 窄口径评审；评审仍未做）
- 当前任务：`TASK-036`（`IMPLEMENTED`：GitHub 预上传整理，159 项单测 + `verify-all` 通过）；`TASK-026/027/028/029/031/032/033/034/035` 均 `IMPLEMENTED` 待批量评审
- 当前 Git 分支：`main`（本地，无远程）；最近 commit 以 `git log -1 --oneline` 为准
- **会话状态：可继续**——设备数据完好；按用户要求**跳过** 04:00 真机跨日验证（默认成功，仅 JVM 证据）
- **发布状态**：未发布 WIP；无 `LICENSE`；无远程；评估见 `project/reports/GITHUB-READINESS-2026-11-06.md`

## Completed This Session
- `TASK-035`：日切点改为 **04:00**（`DayCutoff` 单一来源，四个 Presenter + `AppRoot` 定时器），单测 154 → 159；`ADR-016` 记录用户决定；`ADR-017` + `DESIGN-DELTA.md` 登记 7 项设计偏差；删除统计页周/年死控件。
- `TASK-036`（GitHub 预上传整理）：
  - 扫描 `git ls-files`：无 `local.properties`/`*.jks`/`*.apk`/`build/`/`.gradle/`/截图/DB 备份入库；pack 约 0.94 MiB
  - 设备序列号脱敏为 `<device>`（8 个文件，`git grep` 复验 0）
  - `tools/run-unit-tests.ps1` 与 `tools/verify-all.ps1` 去掉写死的 `D:\AndroidToolchain\env.ps1`，改为「已设置的环境变量 → `HABIT_ENV_SCRIPT` → 同级候选 → 明确报错」
  - README 增加「发布状态（未发布 WIP）」「开源/发布前检查」，修正过期的 40 项 → 159 项说明与旧「下一步」
  - 新增 `.github/workflows/android.yml`（Ubuntu + JDK 17 + Android SDK + Node 22：`assembleDebug test` + schema/DAO 校验）
  - 新增 `project/reports/GITHUB-READINESS-2026-11-06.md`

## Files Changed
- 新增：`.github/workflows/android.yml`、`project/reports/GITHUB-READINESS-2026-11-06.md`、`project/tasks/TASK-036.md`
- 修改：`README.md`、`tools/run-unit-tests.ps1`、`tools/verify-all.ps1`、8 个脱敏文档、`PROJECT_STATUS.md`、`TEST_REPORT.md`、本文件

## Verification Actually Run
| 命令 | 结果 | 备注 |
|---|---|---|
| `git ls-files` 敏感模式扫描 | **0 命中** | 无签名/密钥/构建产物/真实数据 |
| `git grep 10CDAK0B71005KU` | **0 命中** | 脱敏完成 |
| `tools/run-unit-tests.ps1` | **OK (159 tests)** | 可移植性改造后复跑 |
| `tools/verify-all.ps1` | **VERIFY ALL PASS** | 治理 22/22、单测 159、DAO 38、构建、`git diff --check` |
| GitHub Actions | **未运行（无远程）** | 首次推送后验证；语法按 actions 标准 |

## Not Verified / Known Gaps
- **`LICENSE` 未选**：公开前必须由所有者决定（默认保留所有权利）
- **提交身份**：135 次提交均为 `Habit Tracker Baseline <baseline@localhost>`，GitHub 不会关联账号
- **`assembleRelease`/lint 从未运行**（外部评审 R6）
- **8 个 `IMPLEMENTED` 评审债**：必须由新上下文做 `R1`
- **04:00 跨日**：用户明确跳过真机验证，默认成功；仅 `DayCutoffTest` 5 项证据
- 依赖走腾讯镜像，未启用 dependency verification；真机 UI 无自动化

## Lessons Worth Keeping
- **公开前的第一道检查是「受版本控制的文件」，不是工作区**：`git ls-files` 才能证明没有密钥/备份入库
- **工具脚本不要写死本机路径**：写死路径在公开仓库里等于拒绝外部复现；用环境变量 + 明确报错
- **CI 选 Linux**：本地被禁的 `gradlew test` 在 Runner 上正常，等于给 159 项单测增加第二处执行环境
- **评估报告要把「已处理」和「阻塞」分开写**，否则「整理完成」会被误读成「可以发布」

## Next Safe Action
1. 所有者决定许可证与提交身份 → 新增 `LICENSE`、更新 README 第 7 节
2. 可选：先做 `R1` 评审把 8 个 `IMPLEMENTED` 转 `DONE`，再公开
3. `git remote add origin <url>` + 首次 push（由所有者执行；本仓库不自动推送）
4. 推送后确认 GitHub Actions 首跑全绿；再考虑 `v0.1.0-wip` tag
