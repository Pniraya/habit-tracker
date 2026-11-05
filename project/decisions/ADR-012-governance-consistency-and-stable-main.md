# ADR-012: 状态一致性规则与 main 稳定分支策略

- 状态：`ACCEPTED`
- 批准方式：用户于 `2026-11-05` 直接指示“完善治理系统、解决文档存在但状态不同步”，并列出四项检查与三项流程要求；本 ADR 记录该指示与实现范围。
- 日期：`2026-11-05`
- 关联：`AGENTS.md` §5、§7、§10、§12；`README.md`；`project/PROJECT_STATUS.md`；`project/REVIEW_CHECKLIST.md`；`tools/check-governance.ps1`；`tools/close-task.ps1`

## 背景
治理文件齐全，但多次出现“文档存在、状态不同步”：`README.md` 长期停留在 Gate 1 与“没有 Android 工程”，`PROJECT_STATUS.md` 把已接受的 `ADR-011` 仍写成待批准，`TEST_REPORT.md` 顶部摘要停留在 2026-11-04 的 `NOT_RUN`，`RELEASE_REPORT.md` 仍写“尚无 Android 工程”。这些都是人工同步遗漏，靠提醒无法根治。

## 决策

### 1. 状态一致性规则（同一事实只允许一个来源）
- 阶段与当前任务：以 `project/PROJECT_STATUS.md` 为唯一来源，`README.md` 必须与之一致。
- ADR 状态：以 `project/decisions/ADR-xxx-*.md` 的“状态”行为唯一来源；任何文档引用 ADR 且写了状态词，必须与文件一致；每个存在的 ADR 必须在 `PROJECT_STATUS.md` 被覆盖（区间写法如 `ADR-001`~`ADR-012` 视为覆盖）。
- 最近验证结果：以 `TEST_REPORT.md` 的 `Runs` 表最后一行为唯一来源；`Latest Run` 摘要必须与之一致。
- 发布证据：`RELEASE_REPORT.md` 的构建证据不得在工程已可构建时仍写 `NOT_RUN`，也不得出现“尚无 Android 工程”。
- 以上四项由 `tools/check-governance.ps1` 强制，失败即视为治理缺陷。

### 2. 任务收尾半自动化
- `tools/close-task.ps1` 从 TASK 文件抽取状态、阶段、验收勾选数与最近测试记录，生成受标记保护的“状态摘要”块并写回；`-Apply` 时同步 `README.md` 与 `PROJECT_STATUS.md` 的阶段/当前任务行。
- 人工仍需确认的内容（未验证项、风险、Release 证据）由脚本打印为待办，不自动编造。

### 3. main 为稳定分支
- 只有构建通过、适用测试通过、任务验收完成、Reviewer 核对的改动才能进入 `main`。
- `PARTIAL`/`BLOCKED` 任务默认留在 `feat/<short-name>`；若因环境限制必须留在 `main`，必须在 `PROJECT_STATUS.md` 的「在册例外」逐条登记，否则检查失败。

### 4. 独立 Reviewer
- 新增 `project/REVIEW_CHECKLIST.md`，覆盖 Diff、架构一致性、验证证据、文档同步、分支与提交。
- Reviewer 结论只有 `APPROVE` 与 `REJECT` 两种，必须附证据链接或命令输出。

## 后果
- `AGENTS.md` §5/§7/§10/§12 与 `README.md` 同步修订；检查器从 13 项扩展到 19 项。
- 违反一致性时构建仍可能通过，但治理检查会失败，因此“提交前必跑检查器”成为硬要求。
- 现存 `TASK-003`/`TASK-004`/`TASK-005` 的 `PARTIAL` 状态登记为在册例外，不回写历史。
