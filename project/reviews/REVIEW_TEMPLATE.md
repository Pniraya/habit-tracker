# REVIEW-TASK-XXX: 独立评审记录

- 审查任务：`TASK-xxx`
- 审查 commit：`<full commit SHA>`
- Reviewer：`<name or agent>`
- 独立性：`INDEPENDENT` / `NOT_INDEPENDENT`
- 日期：`YYYY-MM-DD`

## 1. Scope
- [ ] 改动与任务 Scope 一致。

## 2. Diff 核对
- [ ] 文件范围合理；没有无关重构、依赖、权限或功能。
- [ ] `git diff --check` 与冲突标记检查通过。

## 3. 架构与数据
- [ ] 分层、数据约束和 ADR 关系符合基线。

## 4. 验证证据
- [ ] 命令、退出码和未验证项如实记录。

## 5. 文档与分支
- [ ] 状态、交接、测试和发布文档同步。
- [ ] 分支与提交符合 `AGENTS.md`。

## 6. 结论
- 结论：`APPROVE` / `REJECT`
- 证据：
- 需修复项：
