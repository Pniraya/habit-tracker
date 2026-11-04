# LAST_HANDOFF.md

> 每次 AI 会话结束前更新。记录事实，不写愿望或推测。

## Snapshot
- 日期时间（含时区）：`2026-11-05 01:25 +09:00`
- 当前阶段：`Gate 0` 已关闭 → `Gate 1` 准备（环境）
- 当前任务：`TASK-002`（`NOT_STARTED`，等安装批准）；`TASK-001`、`TASK-000` 已 `DONE`
- 当前 Git 分支：`main`（本地，无远程）
- 最近 commit：以 `git log -1 --oneline` 为准；基线检查点 `d0f470a`
- `git status --short` 摘要：干净（`temp.md` 被 `.gitignore` 排除，仍在磁盘）

## Completed This Session
- 用户批准 `ADR-005`~`ADR-010`，6 份全部改为 `ACCEPTED`
- 按各 ADR 的 Implementation 段落实修订：
  - `DATABASE.md`：`habits` 增 `created_on`/`archived_on`；计划表 mask 下界改 1 并加 frequency CHECK；日志表加两条 status 不变量 CHECK；`duration_seconds` 标注 V1 未启用；§8.3 收敛为单一口径；§9 归档不再改写 `enabled`；§11 时间戳统一 epoch ms
  - `ARCHITECTURE.md`：§5.2 取消提醒移至 Presentation；§6 增加本地日期冻结；§8.3 不应提醒时只重排不补发；§9 备份示例改 epoch ms
  - `REQUIREMENTS.md`：今日分母明确；移除平均用时；TBD 第 1/5/6 项关闭
  - `FEATURE_MATRIX.md`：F-016 → `OUT_OF_SCOPE`
- `TASK-001` 置 `DONE` 并填验收与证据；`PROJECT_STATUS` 改为 Gate 1 准备

## Files Changed
- 修改：`DATABASE.md`、`ARCHITECTURE.md`、`project/REQUIREMENTS.md`、`project/FEATURE_MATRIX.md`、`project/decisions/ADR-005~010`（状态）、`project/tasks/TASK-001.md`、`project/PROJECT_STATUS.md`、`project/reports/TEST_REPORT.md`、`project/handoffs/LAST_HANDOFF.md`
- 未改动：`DEVELOPMENT.md`、`AGENTS.md`、`README_GOVERNANCE.md`、`orgins/**`、`tools/check-governance.ps1`

## Verification Actually Run
| 命令 | 结果 | 备注 |
|---|---|---|
| `pwsh -NoProfile -File tools/check-governance.ps1` | exit 0 | 13 项全 PASS |
| `Select-String` 复核（禁用提醒/mask/exportedAt/平均用时） | 符合预期 | 见 `TEST_REPORT` |
| `git diff --check`、`git status --short` | exit 0 / 空 | 提交前检查 |
| `gradlew assembleDebug` / `test` | NOT_RUN | 无工程、无 SDK |

## Incomplete Work
- `TASK-002`：SDK / Studio 未安装（网络已验证）
- `TASK-003`：无 `app/`、无 Version Catalog、无 Wrapper

## Known Issues / Risks
- `habits` 的 BOOLEAN 约束仍未在 SQL 表达（本次批准的 ADR 未覆盖，需单独决定是否加 CHECK）
- 数值型“撤销”的 UI 语义、恢复默认策略仍未定
- `ADR-006` 的两条 CHECK 依赖 SQLite 把布尔表达式当 0/1，建库后需在真实 Room 上验证一次
- 检查器只验证文档自洽与提交纪律，不验证代码正确性

## Decisions Made
- 用户批准 `ADR-005`~`ADR-010`（2026-11-05）
- 之前已接受：`ADR-001`~`ADR-004`

## Next Safe Action
1. `TASK-002`：安装 Android SDK（platforms;android-35、build-tools、platform-tools），`JAVA_HOME` 指向 `D:\javajdk17`。
2. 装完后跑 `sdkmanager --list_installed` 与 `adb version`，把输出写进 `TASK-002` 证据表。
3. 再进 `TASK-003`：单模块骨架 + Wrapper + Version Catalog，首次 `assembleDebug`。

## Recovery Notes
- 下一会话先核对：`git log --oneline -3`、`git status --short`、`pwsh -NoProfile -File tools/check-governance.ps1`、根目录是否出现 `app/`
- 不要重复：不要再次 `git init`；不要改 `orgins/**`；改三份基线必须同 commit 带 ADR（检查器会拦）；不要把 `temp.md` 入库
