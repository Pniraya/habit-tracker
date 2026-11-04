# PROJECT_STATUS.md

> 这是项目状态快照，不是计划书。AI 每次任务完成后必须按实际仓库状态更新。

## 当前状态

- 项目阶段：`Gate 0` — 治理与基线确认（已核实：仓库中**不存在** Android Studio 工程）
- 当前任务：`TASK-000` 审查完成（`DONE`，见 `project/tasks/TASK-000.md`）；下一任务 `TASK-001` 等待用户批准
- 总体状态：`IN_PROGRESS`
- 代码实现状态：`NOT_STARTED`（已由递归文件清点证明：无 `*.gradle*`、无 `*.kts`、无 `*.toml`、无 `*.kt`、无 `AndroidManifest.xml`）
- 最近验证构建：`NOT_RUN`（无工程、无 Android SDK）
- 最近验证测试：`NOT_RUN`
- 最后核对时间：`2026-11-04 23:55 (+09:00)`

## 已有设计资产

- [x] `ARCHITECTURE.md`：初始架构基线
- [x] `DATABASE.md`：初始数据库设计
- [x] `DEVELOPMENT.md`：开发规范与阶段计划
- [x] `project/REQUIREMENTS.md`：需求基线（本治理包新增）
- [x] Git 仓库初始化 + `.gitignore` + 首个检查点（`d0f470a`）
- [x] `project/tasks/TASK-000.md`：治理初始化审查记录
- [ ] 现有三份设计文档完成一致性修订（等待 ADR-004~007 批准）
- [ ] Android Studio 工程创建并通过首次构建（环境缺口见“阻塞项与风险”）

## 当前待办

1. 用户就 6 项待决问题给出答复：归档/创建日期的时区语义、`HabitLog.status` 策略、计划校验位置、归档与提醒 `enabled` 冲突、备份时间戳格式、平均用时去留。
2. 批准后执行 `TASK-001`：写入 ADR-004~007，并对三份设计文档做最小修订。
3. 安装 Android SDK / cmdline-tools（或 Android Studio），并确定 Gradle 运行 JDK（当前默认 21，基线写 17）。
4. 执行 `TASK-004`：创建 Android 单模块工程（`:app`），首次 Gradle Sync 与 `assembleDebug`。
5. 将 `TASK-001` 设为当前任务。

## 阻塞项与风险

- 现有数据库文档中 `archived_at` 使用时间戳，而统计规则使用归档日期；跨时区/跨日语义需要明确。
- “平均用时”有 UI 需求，但目前缺少可靠的数据采集流程定义。
- `HabitLog.status` 与 `actual_value`、`target_snapshot`、`completed_at` 存在冗余一致性要求，需定义唯一计算来源及写入约束。
- 计划区间不重叠无法仅靠普通 SQLite CHECK 约束保证，需在事务层强制校验并测试。
- 目前尚未从真实 Android 工程验证 SDK、Gradle、Kotlin、Compose 与 Room 版本组合。
- 环境缺口（2026-11-04 实测）：未安装 Android SDK / Android Studio / adb / Gradle CLI；`JAVA_HOME=D:\javajdk21` 与基线 JDK 17 不一致，需决策。
- 审查新增的文档冲突：`ARCHITECTURE.md` §5.2 让 Domain 流程触发提醒取消，与 §3.2“Domain 不依赖 Android”矛盾；`DATABASE.md` §9 归档“禁用提醒”与恢复“沿用 `enabled`”互相排斥；`DATABASE.md` §4.1/§4.2 的文字约束未在 SQL 中表达；备份时间戳 ISO 与 epoch milliseconds 两处不一致。
- `project/tasks/` 新增了 `TASK-000.md`，但项目尚无 `TASK-001` 记录；`README.md` 不存在而 `AGENTS.md` §2 必读清单引用了它。

## Git 状态

- 仓库是否已初始化：已初始化（`2026-11-04`；审查前确认**不存在**仓库）
- 当前分支：`main`（`git init` 默认 `master` 已重命名）
- 工作区状态：基线检查点后为干净状态；无远程、未推送
- 最近 commit：`d0f470a chore: import governance baseline and design references`（49 个跟踪文件）
- 注意：首个 commit 使用一次性身份 `-c user.name`/`-c user.email`，未写入仓库配置

以上字段已于 `2026-11-04` 用实际 `git status` / `git log` 结果替换；后续任务结束需继续维护。
