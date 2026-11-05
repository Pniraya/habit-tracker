# ADR-014: 单一验证入口与交付门禁

- 状态：`ACCEPTED`
- 日期：`2026-11-05`
- 决策者：用户（本轮授权"你来决定便是"），由 AI 执行
- 关联：`ADR-012`（治理一致性）、`ADR-013`（评审证据）、`AGENTS.md` §5

## 背景

`tools/check-governance.ps1` 只校验 Markdown 字段与文件存在性：它不编译、不执行测试。因此"20/20 PASS"可以与"单元测试全红"同时成立，交付门禁依赖执行者记得手动跑每一条命令。`TASK-008` 期间实际发生过的两次返工（文档未同步、测试恒真）都说明"记得跑"不是可靠机制。

同时，仓库没有 CI（本地仓库、无远程），也没有 git hook，因此唯一的自动化机会就是一条能一次跑完全部验证的脚本。

## 决策

1. 新增 `tools/verify-all.ps1`：依次执行治理检查、单元测试、`assembleDebug`、`git diff --check`，任一失败即非零退出，并打印每步耗时与未提交文件数。
2. 该脚本成为**收尾唯一入口**：`AGENTS.md` §5 的建议命令由逐条命令改为"先跑 `verify-all`，失败再单独定位"。
3. `project/tasks/TASK_TEMPLATE.md` §10 要求记录一行 `verify-all` 的实际结果。
4. `check-governance.ps1` 新增第 21 项：`TASK-009` 及以后的任务文件必须出现 `verify-all` 证据，否则 FAIL；`TASK-001`~`TASK-008` 作为历史记录豁免（与 `ADR-013` 的 TASK-007 切点同一处理方式）。

## 被否决的方案

- **CI**：仓库无远程、无 GitHub Actions 运行环境；本地跑 CI 等价于脚本。
- **git pre-commit hook**：会拖慢每次提交（约 90 秒），且 hook 不随仓库分发、容易在换机后静默失效；改为流程要求 + 检查器兜底。
- **把测试塞进 `check-governance.ps1`**：会让"静态一致性"与"执行验证"混成一个数字，失败时难以区分是文档漂移还是代码回归。

## 影响与风险

- 单次收尾从"若干条命令"变为约 90 秒的固定成本；可用 `--console=plain` 日志在 `.gradle/logs/` 中定位失败步骤。
- `verify-all` 依赖本机环境（`ANDROID_HOME`/`JAVA_HOME`）；脚本在环境变量缺失时会尝试 `D:\AndroidToolchain\env.ps1`，否则该步失败并明确报错，不会静默跳过。
- 检查器第 21 项只验证"有 verify-all 证据"，不验证该证据是本次运行的结果——与第 18 项同等的信任边界，已在 `REVIEW_CHECKLIST.md` 的"验证证据"维度要求 Reviewer 核对。

## 用户授权来源

本轮用户指令："你来决定便是。"在此之前的一轮中，AI 明确提议"补 `verify-all.ps1`，约 30 分钟，消灭'忘记跑测试'这类失败"，用户以该指令表示同意执行。
