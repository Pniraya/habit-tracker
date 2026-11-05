# PROJECT_STATUS.md

> 这是项目状态快照，不是计划书。AI 每次任务完成后必须按实际仓库状态更新。

## 当前状态

- 项目阶段：`Gate 3` — 首个功能页（Today）已接线；Gate 1 的设备验证仍缺（`TASK-003` `PARTIAL`）
- 当前任务：`TASK-011`（`PARTIAL`，Today 页 Presenter/ViewModel 接线完成，待独立评审）；`TASK-010`、`TASK-009` 已 `DONE`；`TASK-008`、`TASK-007`、`TASK-006` 已 `DONE`；`TASK-005`、`TASK-004`、`TASK-003` 为 `PARTIAL`（main 上的在册例外）
- 总体状态：`IN_PROGRESS`
- 代码实现状态：`IMPLEMENTED_UNVERIFIED`（单模块 `:app` + 三页导航 + Room schema v1 + Domain/Repository/UseCase + Today 页状态机；构建通过，schema/约束/DAO 语句经真实 SQLite 验证，UI 与设备未验证）
- 最近验证构建：`PASS`（`gradlew assembleDebug`，2026-11-05，合入 `main` 后 23s）
- 最近验证数据层：`PASS`（`node tools/verify-schema.mjs` 40/40 含 schema parity；`node tools/verify-dao.mjs` 21/21 验证 DAO 的 SQL 语义、CAS 更新与级联删除）
- 最近验证测试：`PASS`（40 项，`tools/run-unit-tests.ps1`，在 `main` @ `feeee61` 上经 `verify-all` 复跑）；`gradlew test` 仍因沙箱阻断测试 worker 而失败，已改用直接 runner
- 最近治理一致性检查：`PASS`（2026-11-05，21 项检查；含独立评审记录与 verify-all 证据检查）
- 最后核对时间：`2026-11-05`

## 已有设计资产与实现

- [x] `ARCHITECTURE.md`、`DATABASE.md`、`DEVELOPMENT.md`：已按 `ADR-005~010` 修订
- [x] `project/REQUIREMENTS.md`：TBD 第 1/5/6 项已关闭
- [x] `project/decisions/ADR-001`~`ADR-014`：全部 `ACCEPTED`（`ADR-011` 触发器方案、`ADR-012` 治理一致性、`ADR-013` 评审证据、`ADR-014` 单一验证入口 `tools/verify-all.ps1`）
- [x] `project/FEATURE_MATRIX.md`：F-001 构建验证通过、F-002 未验证、F-016 `OUT_OF_SCOPE`
- [x] Git 仓库 + `.gitignore` + 基线检查点（`d0f470a`）
- [x] 治理层：`README.md`、`RELEASE_REPORT.md`、`TASK-000~009`、`tools/check-governance.ps1`（21 项）、`tools/close-task.ps1`、`tools/verify-all.ps1`、`project/REVIEW_CHECKLIST.md`
- [x] 构建环境：`D:\AndroidToolchain`（`sdk` + `gradle` + `env.ps1` + `verify.ps1`），JDK 17 在 `D:\javajdk17`
- [x] Android 工程骨架：Wrapper、Version Catalog、`:app`、主题 token、三页导航
- [x] 数据层 schema v1：四表 + 外键 + 索引 + Converter + schema export（`TASK-004`）；DAO 语句经 `verify-dao.mjs` 在真实 SQLite 上验证（`TASK-010`）
- [x] 独立评审证据：`ADR-013` + `project/reviews/REVIEW-TASK-006/007`（`TASK-007` 的 `APPROVE` 记录随 `c10f67d` 合入 `main`）
- [x] Domain 纯规则 + Repository + 计划版本写入器（`TASK-005`）+ UseCase 层（`TASK-008`），40 项单测通过
- [ ] DAO/Repository 运行时测试（沙箱内无法运行 Gradle 测试 worker）
- [ ] 真机/模拟器安装与三页导航验证

## 当前待办

1. 补 `UseCase` 层（`RecordHabitProgressUseCase` 等）与 `DataStore`。
2. 为 Room 事务/DAO 补集成测试（Robolectric 或真机），补齐 `TASK-004`/`TASK-005` 的验收缺口。
3. 接真机或装模拟器完成 `TASK-003` 的设备验证，再关闭 Gate 1。
4. 为 TASK-007 取得独立 Reviewer 的 `APPROVE` 记录后合入 `main`；其后每任务结束运行 `tools/close-task.ps1` 与 `tools/check-governance.ps1`，保持六份状态文档同步。

## 阻塞项与风险

- 无真机、无模拟器：`adb devices` 为空，`emulator`/`system-images` 未安装，Gate 1 的“设备可安装启动”无法验证。
- 沙箱内 `gradlew test` 永远失败（测试 worker 管道被禁）；纯 JVM 测试用 `tools/run-unit-tests.ps1` 运行，Room/Compose 相关测试仍无法在本机执行。
- 依赖经腾讯镜像获取（官方 SHA-1 只用在校验 SDK 包上，Maven 依赖未逐包校验），镜像不可用时回退到 `google()` 会极慢。
- Android SDK 许可文件由 AI 按标准哈希写入，等同于代表用户接受条款，需用户复核。
- 仍存在的设计缺口：`habits` 的 BOOLEAN 约束未在 SQL 表达；数值型“撤销”UI 语义未定；恢复默认策略未定。
- DAO/Repository 无运行时测试：`gradlew test` 在沙箱内必失败，只能靠 `tools/verify-schema.mjs` 验证 SQL 层。
- `ADR-011` 已接受并落地；触发器是等价实现而非字面 `CHECK`，若将来改用预建库方案需新 ADR。

## 在册例外（main 上的未完成任务）

`AGENTS.md` §7 要求 `PARTIAL` 任务不进 `main`；以下任务因环境限制（无设备、沙箱禁止 Gradle 测试 worker）必须留在 `main`，按 ADR-012 登记为例外：

- `TASK-003`：`PARTIAL` —— 构建通过，设备安装与三页导航未验证（无真机、无 WHPX）。
- `TASK-004`：`PARTIAL` —— schema 与约束已用 `node:sqlite` 验证 40/40，Room 运行时未验证。
- `TASK-005`：`PARTIAL` —— 27 项纯 JVM 单测通过，Room 事务集成与设备持久化未验证。

新增例外必须写在这里，否则 `tools/check-governance.ps1` 失败。

## Git 状态

- 仓库：已初始化，无远程、未推送；`main` 为稳定分支，治理任务在 `feat/*` 分支评审后合入（当前分支以 `git branch --show-current` 为准）
- 工作区：构建产物、`local.properties`、`.gradle/`、`.kotlin/` 均已忽略
- 最近 commit：以 `git log -1 --oneline` 为准；基线检查点 `d0f470a`

以上字段已于 `2026-11-05` 按实际构建结果、`node tools/verify-schema.mjs`、`adb devices` 与检查器结果更新。
