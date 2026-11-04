# Habit Tracker — AI Governance Pack

本目录包含项目 AI 开发治理文件。将本包内容合并到 Android 项目仓库根目录（不要把整个 `HabitTracker_Governance` 文件夹嵌套进项目，除非你有意如此）。

## 合并方式

1. 将 `AGENTS.md` 放到仓库根目录。
2. 将 `project/` 整个目录放到仓库根目录。
3. 保留原有根目录 `ARCHITECTURE.md`、`DATABASE.md`、`DEVELOPMENT.md`。
4. 对照 `project/reports/BASELINE_REVIEW.md`，不要在未确认产品语义前擅自改数据库设计。
5. 首次接入时，让 Codex 检查真实文件、Git 状态和 Android 工程；把 `PROJECT_STATUS.md` 中的 `UNKNOWN` 更新为实际事实。

## 推荐首次 Git 操作

在确认没有需要保留的现有改动后：

```bash
git init
git add .
git commit -m "chore: establish project baseline and AI governance"
```

如果仓库已经初始化，不要重复 `git init`；先检查 `git status` 和现有历史。

## 给 Codex 的首次指令

请先只执行项目审查，不要编写业务代码，也不要修改冻结基线。读取根目录 `AGENTS.md`、三份设计文档和 `project/` 下的治理文件；检查 Git 状态、工程目录、Gradle 配置和现有源码。对照 `project/reports/BASELINE_REVIEW.md` 验证审查项，输出：
1. 实际仓库状态；
2. 文档与源码冲突；
3. 阻断后续实现的未决事项；
4. 建议的最小修订；
5. 下一项任务草案。
未经用户批准，不修改 `ARCHITECTURE.md`、`DATABASE.md`、`DEVELOPMENT.md` 或 `REQUIREMENTS.md`。
