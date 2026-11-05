# PROJECT_STATUS.md

> 这是项目状态快照，不是计划书。AI 每次任务完成后必须按实际仓库状态更新。

## 当前状态

- 项目阶段：`Gate 2` — 数据层进行中；Gate 1 的设备验证仍缺（`TASK-003` `PARTIAL`）
- 当前任务：`TASK-005`（`PARTIAL`）；`TASK-004`、`TASK-003` 同为 `PARTIAL`；`TASK-001`、`TASK-002` 已 `DONE`
- 总体状态：`IN_PROGRESS`
- 代码实现状态：`IMPLEMENTED_UNVERIFIED`（单模块 `:app` + 三页导航壳 + Room schema v1；构建通过，约束经真实 SQLite 验证，DAO/Repository/真机未验证）
- 最近验证构建：`PASS`（`gradlew assembleDebug`，2026-11-05，重构后 36s）
- 最近验证数据层：`PASS`（`node tools/verify-schema.mjs`，40/40，含 schema parity）
- 最近验证测试：`PASS`（27 项，`tools/run-unit-tests.ps1`）；`gradlew test` 仍因沙箱阻断测试 worker 而失败，已改用直接 runner
- 最近治理一致性检查：`PASS`（2026-11-05，13 项检查）
- 最后核对时间：`2026-11-05`

## 已有设计资产与实现

- [x] `ARCHITECTURE.md`、`DATABASE.md`、`DEVELOPMENT.md`：已按 `ADR-005~010` 修订
- [x] `project/REQUIREMENTS.md`：TBD 第 1/5/6 项已关闭
- [x] `project/decisions/ADR-001`~`ADR-010`：全部 `ACCEPTED`
- [x] `project/FEATURE_MATRIX.md`：F-001 构建验证通过、F-002 未验证、F-016 `OUT_OF_SCOPE`
- [x] Git 仓库 + `.gitignore` + 基线检查点（`d0f470a`）
- [x] 治理层：`README.md`、`RELEASE_REPORT.md`、`TASK-000~003`、`tools/check-governance.ps1`
- [x] 构建环境：`D:\AndroidToolchain`（`sdk` + `gradle` + `env.ps1` + `verify.ps1`），JDK 17 在 `D:\javajdk17`
- [x] Android 工程骨架：Wrapper、Version Catalog、`:app`、主题 token、三页导航
- [x] 数据层 schema v1：四表 + 外键 + 索引 + Converter + schema export（`TASK-004`）
- [x] Domain 纯规则 + Repository + 计划版本写入器（`TASK-005`），27 项单测通过
- [ ] DAO/Repository 运行时测试（沙箱内无法运行 Gradle 测试 worker）
- [ ] 真机/模拟器安装与三页导航验证

## 当前待办

1. 用户裁决 `ADR-011`（CHECK → 触发器）；否决则改用 `createFromAsset` 预建库方案。
2. 补 `Repository` + `HabitScheduleWriter` + `upsertDailyLog` 的 Domain 计算，并补 DAO 集成测试（需非沙箱环境执行）。
3. 接真机完成 `TASK-003` 的设备验证，再关闭 Gate 1。
4. 每个任务结束按 `AGENTS.md` §10 更新状态、测试报告与交接。

## 阻塞项与风险

- 无真机、无模拟器：`adb devices` 为空，`emulator`/`system-images` 未安装，Gate 1 的“设备可安装启动”无法验证。
- 沙箱内 `gradlew test` 永远失败（测试 worker 管道被禁）；纯 JVM 测试用 `tools/run-unit-tests.ps1` 运行，Room/Compose 相关测试仍无法在本机执行。
- 依赖经腾讯镜像获取（官方 SHA-1 只用在校验 SDK 包上，Maven 依赖未逐包校验），镜像不可用时回退到 `google()` 会极慢。
- Android SDK 许可文件由 AI 按标准哈希写入，等同于代表用户接受条款，需用户复核。
- 仍存在的设计缺口：`habits` 的 BOOLEAN 约束未在 SQL 表达；数值型“撤销”UI 语义未定；恢复默认策略未定。
- DAO/Repository 无运行时测试：`gradlew test` 在沙箱内必失败，只能靠 `tools/verify-schema.mjs` 验证 SQL 层。
- `ADR-011`（CHECK → 触发器）待批准：获批前不能宣称 `DATABASE.md` §4 已完全落地。

## Git 状态

- 仓库：已初始化（`main`，无远程、未推送）
- 工作区：构建产物、`local.properties`、`.gradle/`、`.kotlin/` 均已忽略
- 最近 commit：以 `git log -1 --oneline` 为准；基线检查点 `d0f470a`

以上字段已于 `2026-11-05` 按实际构建结果、`node tools/verify-schema.mjs`、`adb devices` 与检查器结果更新。
