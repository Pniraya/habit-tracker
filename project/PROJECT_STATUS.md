# PROJECT_STATUS.md

> 这是项目状态快照，不是计划书。AI 每次任务完成后必须按实际仓库状态更新。

## 当前状态

- 项目阶段：`Gate 0` — 治理与基线确认（已核实：仓库中**不存在** Android Studio 工程）
- 当前任务：`TASK-001` — 关闭 Gate 0（`NOT_STARTED`，等待用户裁决 8 项决策）；上一任务 `TASK-000` 已 `DONE`
- 总体状态：`IN_PROGRESS`
- 代码实现状态：`NOT_STARTED`（已由递归文件清点证明：无 `*.gradle*`、无 `*.kts`、无 `*.toml`、无 `*.kt`、无 `AndroidManifest.xml`）
- 最近验证构建：`NOT_RUN`（无工程、无 Android SDK）
- 最近验证测试：`NOT_RUN`
- 最近治理一致性检查：`PASS`（2026-11-05，`tools/check-governance.ps1`：13 项检查，13 PASS / 0 WARN / 0 FAIL）
- 最后核对时间：`2026-11-04 23:55 (+09:00)`

## 已有设计资产

- [x] `ARCHITECTURE.md`：初始架构基线
- [x] `DATABASE.md`：初始数据库设计
- [x] `DEVELOPMENT.md`：开发规范与阶段计划
- [x] `project/REQUIREMENTS.md`：需求基线（本治理包新增）
- [x] Git 仓库初始化 + `.gitignore` + 首个检查点（`d0f470a`）
- [x] `project/tasks/TASK-000.md`：治理初始化审查记录
- [x] `README.md`：仓库入口与文件地图
- [x] `project/reports/RELEASE_REPORT.md`：发布证据模板
- [x] `project/tasks/TASK-001~003.md`：Gate 0 关闭 / 环境准备 / 工程骨架
- [x] `project/decisions/ADR-004`：文件分级与保护清单；`AGENTS.md` §10–§13
- [x] `tools/check-governance.ps1`：13 项治理一致性检查（含双向反向测试验证）
- [x] `project/decisions/ADR-005~010`：6 份 `PROPOSED` 决策草案（覆盖 8 项待决问题，等待批准）
- [ ] 现有三份设计文档完成一致性修订（等待 ADR-005~010 批准）
- [ ] Android Studio 工程创建并通过首次构建（环境缺口见“阻塞项与风险”）

## 当前待办

1. 用户审阅 `ADR-005~010`（已按推荐默认写成 `PROPOSED`）并批准或否决；批准后只需把状态改为 `ACCEPTED` 并执行文档改动。
2. 批准后执行 `TASK-001`：写入 ADR-005~010，并对 `ARCHITECTURE.md`、`DATABASE.md`、`project/REQUIREMENTS.md` 做最小修订。
3. 执行 `TASK-002`：安装 Android SDK / cmdline-tools（或 Android Studio），确定 Gradle 运行 JDK（当前默认 21，基线写 17）。
4. 执行 `TASK-003`：创建 Android 单模块工程（`:app`），首次 Gradle Sync 与 `assembleDebug`。
5. 每个任务结束按 `AGENTS.md` §10 生命周期更新状态、测试报告与交接。

## 阻塞项与风险

- 现有数据库文档中 `archived_at` 使用时间戳，而统计规则使用归档日期；跨时区/跨日语义需要明确。
- “平均用时”有 UI 需求，但目前缺少可靠的数据采集流程定义。
- `HabitLog.status` 与 `actual_value`、`target_snapshot`、`completed_at` 存在冗余一致性要求，需定义唯一计算来源及写入约束。
- 计划区间不重叠无法仅靠普通 SQLite CHECK 约束保证，需在事务层强制校验并测试。
- 目前尚未从真实 Android 工程验证 SDK、Gradle、Kotlin、Compose 与 Room 版本组合。
- 环境缺口（2026-11-05 实测）：未安装 Android SDK / Android Studio / adb / Gradle CLI；`JAVA_HOME=D:\javajdk21` 与基线 JDK 17 不一致，但 `D:\javajdk17`（17.0.13）已存在，可直接指定。
- 网络与磁盘已确认可用：`dl.google.com`（AGP/SDK）、`repo1.maven.org`、`services.gradle.org` 均返回 200；D 盘剩余 98.6 GB。因此 `TASK-002` 只差安装动作，不存在网络阻断。
- 审查新增的文档冲突：`ARCHITECTURE.md` §5.2 让 Domain 流程触发提醒取消，与 §3.2“Domain 不依赖 Android”矛盾；`DATABASE.md` §9 归档“禁用提醒”与恢复“沿用 `enabled`”互相排斥；`DATABASE.md` §4.1/§4.2 的文字约束未在 SQL 中表达；备份时间戳 ISO 与 epoch milliseconds 两处不一致。
- 仓库根目录的 `temp.md`（用户提供的治理方案草稿，709 行）已写入 `.gitignore`：它是输入材料，不作为基线、验收或实现依据。
- 新增的 `TASK-001~003` 均为 `NOT_STARTED`；`TASK-001` 依赖用户裁决，`TASK-002/003` 依赖环境安装。

## Git 状态

- 仓库是否已初始化：已初始化（`2026-11-04`；审查前确认**不存在**仓库）
- 当前分支：`main`（`git init` 默认 `master` 已重命名）
- 工作区状态：基线检查点后为干净状态；无远程、未推送
- 最近 commit：以 `git log -1 --oneline` 为准；基线检查点 `d0f470a`（49 个文件），治理层提交 `82a8bff`、`dc2885f`
- 注意：首个 commit 使用一次性身份 `-c user.name`/`-c user.email`，未写入仓库配置

以上字段已于 `2026-11-04` 用实际 `git status` / `git log` 结果替换；后续任务结束需继续维护。
