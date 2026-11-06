# TASK-036: GitHub 预上传整理

- 状态：`IMPLEMENTED`（`ADR-015`：实现与自测完成，未经独立评审）
- 阶段：`Gate 3`
- 优先级：`P2`
- 依赖：`TASK-035`
- 创建日期：`2026-11-06`
- 完成日期：`2026-11-06`

## 1. Objective
按用户要求评估「能否进入预上传 GitHub 的工作」，并把可以安全完成的整理一次做完：隐私脱敏、工具可移植、README 发布说明、CI、评估报告。

## 2. Context
仓库一直是无远程的本地仓库（`AGENTS.md` §7 禁止自动推送）。外部评审提出 R9（依赖供应链）、R10（无远端=单点）与 R6（Release 从未验证）。用户明确问的是**能否进入预上传工作**，不是「立即公开」。

## 3. Scope
- 扫描受版本控制的文件，确认无签名、密钥、构建产物、真实用户数据入库。
- 设备序列号脱敏；工具链绝对路径改为环境变量优先。
- README 增加发布状态与开源前检查；修正过期命令说明与「下一步」。
- 新增 `.github/workflows/android.yml`（Ubuntu + JDK 17 + Android SDK + Node 22）。
- 新增 `project/reports/GITHUB-READINESS-2026-11-06.md`：结论、阻塞项、公开顺序。

## 4. Out of Scope
- 选择许可证（法律决定，留给所有者）。
- 修改提交身份、创建远程、推送、打 tag。
- `assembleRelease`/lint/release 构建验证（外部评审 R6 仍是未解决项）。
- 依赖校验和（R9）。

## 5. Expected Files
- 新增：`.github/workflows/android.yml`、`project/reports/GITHUB-READINESS-2026-11-06.md`、本文件
- 修改：`README.md`、`tools/run-unit-tests.ps1`、`tools/verify-all.ps1`、8 个含设备序列号的文档

## 6. Acceptance Criteria
- [x] 仓库内无 `local.properties`、`*.jks`、`*.apk`、`.gradle/`、数据库备份或截图被跟踪
- [x] 文档中不再出现设备序列号（`git grep` 0 命中）
- [x] 工具链路径不再写死；缺环境时明确报错
- [x] README 如实标注「未发布 / 无许可证 / 8 个 `IMPLEMENTED`」
- [x] CI 文件就位（语法按 actions 标准，首次推送后在 GitHub 验证）
- [x] 单测 159 项与 `verify-all` 仍通过

## 7. Tests
- `tools/run-unit-tests.ps1`：**159 项通过**（改动后复跑，确认可移植性改造没有破坏入口）
- `tools/verify-all.ps1`：**VERIFY ALL PASS**（治理 22/22、单测 159、DAO 38、构建、`git diff --check`）
- `git grep 10CDAK0B71005KU`：0 命中
- CI：**未在 GitHub 上运行**（无远程）；首次推送后验证

## 8. Risks and Decisions
- **脱敏影响评审证据？** 只替换设备标识，命令、输出、结论不变；评审记录的其他内容未动。
- **CI 选 Ubuntu 而不是 Windows**：本地唯一不可用的 `gradlew test` 在 GitHub Runner 上正常，因此 CI 用标准 Gradle 测试任务，不依赖 Windows 专用的 `tools/run-unit-tests.ps1`。
- **LICENSE 不代选**：默认「保留所有权利」与开源定位冲突，必须由所有者决定。
- **提交身份**：135 次提交均为 `Habit Tracker Baseline <baseline@localhost>`；公开后 GitHub 不会关联到账号，报告里列为待确认项。
- **不推送**：`AGENTS.md` §7/§13 禁止自动推送；本次只做本地整理。

## 9. Implementation Log
- 2026-11-06：`git ls-files` + 扩展名/路径扫描 → 无敏感文件；`git count-objects` 约 0.94 MiB。
- 2026-11-06：`git grep` 找到 8 个含设备序列号的文件并脱敏；`git grep` 复验 0。
- 2026-11-06：两个工具脚本改为「环境变量 → `HABIT_ENV_SCRIPT` → 同级候选」，缺环境时输出明确错误。
- 2026-11-06：README 增补发布状态与检查清单；新增 CI 与评估报告。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `git ls-files` 敏感模式扫描 | **0 命中** | 无 `local.properties`/`*.jks`/`*.apk`/`build/`/`.gradle/` |
| `git grep 10CDAK0B71005KU` | **0 命中** | 脱敏 8 个文件 |
| `tools/run-unit-tests.ps1` | **OK (159 tests)** | 可移植性改造后复跑 |
| `tools/verify-all.ps1` | **VERIFY ALL PASS** | 治理 22/22、单测 159、DAO 38、构建、空白检查 |

## 11. Completion Review
- [x] 验收标准满足
- [x] 测试实际运行
- [ ] 独立 Reviewer `APPROVE`（`ADR-015` 批量评审）

## 12. Handoff
- 公开前需要所有者决定：许可证、提交身份、公开/私有、是否等 `R1` 评审。
- 公开后第一件事：确认 GitHub Actions 首次运行通过。
- `assembleRelease`/lint 仍未验证（R6）。

## 13. Independent Review
- Reviewer record：待创建（批量评审）
- 结论：`PENDING`（`IMPLEMENTED`）

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-036
- 状态：`IMPLEMENTED`
- 阶段：Gate 3
- 验收：通过 6 项 / 未通过 0 项
- 最近测试记录：2026-11-06 / **OK (159 tests)**
<!-- close-task:end -->
