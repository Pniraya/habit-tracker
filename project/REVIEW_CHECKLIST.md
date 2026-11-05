# REVIEW_CHECKLIST.md

> Reviewer 使用。逐项打勾或写明理由；结论只有 `APPROVE` / `REJECT`。Developer 不得自己勾选本表。

## 0. 前置
- [ ] Reviewer 是独立会话（或独立 Agent），不是实现者本人
- [ ] 已读 `project/tasks/TASK-xxx.md`，确认本次变更没有超出 Scope
- [ ] 已运行 `git log --oneline -3`、`git status --short`，确认工作区干净或改动已声明

## 1. Diff 核对
- [ ] 每个改动文件都能对应到 TASK 的验收项，没有顺手重构
- [ ] 没有引入未批准的依赖、权限、Gradle module、后台服务或产品功能
- [ ] 没有把业务规则写进 Composable / Receiver / DAO
- [ ] `git diff --check` 无空白错误；无冲突标记
- [ ] 没有删除或绕过既有测试

## 2. 架构一致性
- [ ] 分层符合 `ARCHITECTURE.md`：Presentation 不直接依赖 Room；Domain 不依赖 Android API
- [ ] `DATABASE.md` 的列名、可空性、索引、外键、状态不变量未被改变；若改变则有 Migration 与 ADR
- [ ] 日期、时区、掩码、快照等口径与 `DATABASE.md` / `ARCHITECTURE.md` 一致

## 3. 验证证据
- [ ] `project/reports/TEST_REPORT.md` 有本次实际执行的命令、退出码与失败摘要
- [ ] 未执行的测试没有被写成通过；`gradlew test` 的沙箱限制有记录且用了替代 runner
- [ ] 声称 `VERIFIED` 的功能有对应测试或构建产物；`DEVICE_VERIFIED` 有设备证据
- [ ] 构建产物可复核（路径、大小、`aapt2` 输出或截图）

## 4. 文档同步
- [ ] `README.md` 的阶段与当前任务与 `PROJECT_STATUS.md` 一致
- [ ] `PROJECT_STATUS.md` 覆盖所有存在的 ADR，且 ADR 状态与 ADR 文件一致
- [ ] `README.md` / `PROJECT_STATUS.md` / `RELEASE_REPORT.md` 不含过期断言（如“尚无 Android 工程”）
- [ ] `TEST_REPORT.md` 的 `Latest Run` 与 `Runs` 表最后一行一致
- [ ] `LAST_HANDOFF.md` 的 Snapshot 已更新，未留空字段

## 5. 分支与提交
- [ ] 改动不违反 `main` 稳定分支策略；`PARTIAL` 任务要么在 feature 分支，要么已在 `PROJECT_STATUS.md` 的「在册例外」登记
- [ ] commit 信息聚焦且与实际改动一致
- [ ] 受保护基线文件的改动在同一 commit 内带有 ADR

## 6. 结论
- 结论：`APPROVE` / `REJECT`
- 证据（命令输出、文件路径或截图）：
- 需修复项（REJECT 时必填）：
