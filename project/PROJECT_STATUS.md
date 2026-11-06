# PROJECT_STATUS.md

> 这是项目状态快照，不是计划书。AI 每次任务完成后必须按实际仓库状态更新。

## 当前状态

- 项目阶段：`Gate 3` — Today 与新建习惯两页均已接真实数据，并在真机走通「建习惯 → 今日出现 → 打卡 → 落库」（`TASK-012` §14 / `DEVICE_DUMP-2026-11-05.md`）；Gate 1 的自动设备验证仍缺（`TASK-003` `PARTIAL`）
- 当前任务：`TASK-026`（`IMPLEMENTED`：设备侧 Room 契约测试 5 项，真机 6/6 通过，待批量评审）；`TASK-033`（习惯详情页）已 `IMPLEMENTED` 合入；`TASK-034`（统计页滚动、导航栏自动隐藏、删除习惯、归档按月日分组）已 `IMPLEMENTED` 合入；`TASK-032`（今日页/新建页重建）已 `IMPLEMENTED` 合入；`TASK-031`（统计页重建）已 `IMPLEMENTED` 合入；下一条按外部评审（`ADVICE-RESPONSE-2026-11-06.md`）先做窄口径里程碑评审（7 个 `IMPLEMENTED` 一起），再把 `TASK-030` 拆成 `030a 导出`（先做）与 `030b 恢复`；`TASK-024` 已 `DONE` 合入 `main`；`TASK-022` 已 `DONE` 合入 `main`；`TASK-012`~`TASK-022` 已 `DONE`（第 3 轮独立评审 `APPROVE` 于 `07b1aab`，合入 `main` @ `62199e9`）；`TASK-011` 已 `DONE`；`TASK-010`、`TASK-009` 已 `DONE`；`TASK-008`、`TASK-007`、`TASK-006` 已 `DONE`；`TASK-005`、`TASK-004`、`TASK-003` 为 `PARTIAL`（main 上的在册例外）
- 总体状态：`IN_PROGRESS`
- 代码实现状态：`DEVICE_VERIFIED`（单模块 `:app` + 三页导航 + Room schema v1 + Domain/Repository/UseCase + Today 页与新建习惯页状态机；构建通过，schema/约束/DAO 语句经真实 SQLite 验证，主路径已在 `iQOO Neo7 SE` 走通；逐页巡检与 UI 自动化仍缺，见 `FEATURE_MATRIX.md` 的脚注）
- 最近验证构建：`PASS`（`gradlew assembleDebug`，2026-11-06，13s，APK 10,559,569 B；与 `README` 同口径）
- 最近验证数据层：`PASS`（`node tools/verify-schema.mjs` 40/40 含 schema parity；`node tools/verify-dao.mjs` 38/38 验证 DAO 的 SQL 语义、CAS 更新与级联删除（含 `deleteById`））
- 最近验证测试：`PASS`（**154 项**单测 + **6 项设备侧 instrumented**，`connectedDebugAndroidTest`：触发器注册/重开、Converter 往返、Flow 失效、级联删除、事务回滚）；`gradlew test` 仍因沙箱阻断测试 worker 而失败，已改用直接 runner
- 最近治理一致性检查：`PASS`（2026-11-05，**22 项**检查 + 15 个探针 0 失败；含独立评审记录、verify-all 证据、离线守卫 `offline guarantee`。历史任务/评审记录里的「21 项」是当时的真实值，不回改）
- 最后核对时间：`2026-11-06`

## 已有设计资产与实现

- [x] `ARCHITECTURE.md`、`DATABASE.md`、`DEVELOPMENT.md`：已按 `ADR-005~010` 修订
- [x] `project/REQUIREMENTS.md`：TBD 第 1/5/6 项已关闭
- [x] `project/decisions/ADR-001`~`ADR-015`：全部 `ACCEPTED`（`ADR-015` 评审批量后置，交付 MVP 期间生效）（`ADR-011` 触发器方案、`ADR-012` 治理一致性、`ADR-013` 评审证据、`ADR-014` 单一验证入口 `tools/verify-all.ps1`）
- [x] `project/FEATURE_MATRIX.md`：F-001/F-002/F-003/F-004/F-006/F-007/F-009/F-011/F-020 已 `DEVICE_VERIFIED`；F-005 `VERIFIED`（三种频率有单测，真机只建过 `DAILY`）；F-008 `PARTIAL`（撤销可，改历史记录未做）；F-010 `DEVICE_VERIFIED`（进度条/7 天完成率）；F-012~F-014 未开始；F-015 `VERIFIED`（无 `INTERNET` 权限与网络库，无自动化断言）；F-016~F-019 `OUT_OF_SCOPE`（`TASK-019` 校正）
- [x] Git 仓库 + `.gitignore` + 基线检查点（`d0f470a`）
- [x] 治理层：`README.md`、`RELEASE_REPORT.md`、`TASK-000~009`、`tools/check-governance.ps1`（22 项）、`tools/close-task.ps1`、`tools/verify-all.ps1`、`project/REVIEW_CHECKLIST.md`
- [x] 构建环境：`D:\AndroidToolchain`（`sdk` + `gradle` + `env.ps1` + `verify.ps1`），JDK 17 在 `D:\javajdk17`
- [x] Android 工程骨架：Wrapper、Version Catalog、`:app`、主题 token、三页导航
- [x] 数据层 schema v1：四表 + 外键 + 索引 + Converter + schema export（`TASK-004`）；DAO 语句经 `verify-dao.mjs` 在真实 SQLite 上验证（`TASK-010`）
- [x] 独立评审证据：`ADR-013` + `project/reviews/REVIEW-TASK-006/007`（`TASK-007` 的 `APPROVE` 记录随 `c10f67d` 合入 `main`）
- [x] Domain 纯规则 + Repository + 计划版本写入器（`TASK-005`）+ UseCase 层（`TASK-008`），40 项单测通过
- [x] DAO/Repository 运行时测试（`TASK-026`：`DatabaseContractTest` 5 项在真机通过——生产 `callback()` 的触发器注册与重开、Converter/实体往返、`withTransaction` 回滚、Flow 失效、四表级联；`gradlew test` 的单元测试 worker 仍被禁）
- [x] 真机安装与三页导航验证（`TASK-012`/`TASK-016`）；模拟器不可用（无 WHPX），此项不再作为缺口

## 当前待办

1. 补 `UseCase` 层（`RecordHabitProgressUseCase` 等）与 `DataStore`。
2. ~~为 Room 事务/DAO 补集成测试~~ → `TASK-026` 已交付（`DatabaseContractTest` 5 项真机通过）；剩余：UI 自动化（外部评审 `N6`，≤8 用例）与迁移测试底座。
3. 接真机或装模拟器完成 `TASK-003` 的设备验证，再关闭 Gate 1。
4. 为 TASK-007 取得独立 Reviewer 的 `APPROVE` 记录后合入 `main`；其后每任务结束运行 `tools/close-task.ps1` 与 `tools/check-governance.ps1`，保持六份状态文档同步。

## 阻塞项与风险

- 无模拟器：`emulator`/`system-images` 未安装，`HypervisorPresent=False`（无 WHPX/Hyper-V），x86_64 镜像无法启动。真机可用但需手动连接；`TASK-012`/`TASK-016` 已在 `iQOO Neo7 SE`（Android 15）上手动走查。
- 沙箱内 `gradlew test` 永远失败（测试 worker 管道被禁）；纯 JVM 测试用 `tools/run-unit-tests.ps1` 运行。**设备侧** instrumented 测试可用（`connectedDebugAndroidTest`，`TASK-025` 实测通过），因此 Room/Compose 并非「无法测」，而是「还没有用例」。
- 依赖经腾讯镜像获取（官方 SHA-1 只用在校验 SDK 包上，Maven 依赖未逐包校验），镜像不可用时回退到 `google()` 会极慢。
- Android SDK 许可文件由 AI 按标准哈希写入，等同于代表用户接受条款，需用户复核。
- 仍存在的设计缺口：`habits` 的 BOOLEAN 约束未在 SQL 表达；数值型“撤销”UI 语义未定；恢复默认策略未定。
- ~~DAO/Repository 仍无运行时测试~~ → `TASK-026` 补齐：`DatabaseContractTest` 证明 `DatabaseConstraints.callback()` 在真实 `Room` 打开路径上注册了 8 个触发器（含重开）、实体/Converter 往返、`withTransaction` 回滚与 Flow 失效。仍未覆盖：迁移、Cursor 到实体的**全量**字段映射（只抽查关键字段）、并发写入。
- `ADR-011` 已接受并落地；触发器是等价实现而非字面 `CHECK`，若将来改用预建库方案需新 ADR。

## 待批审清单（ADR-015）

> `IMPLEMENTED` 任务（实现完成、未经独立评审）必须在此逐条登记；转 `DONE` 后从此处移除。`check-governance.ps1` 第 18 项双向校验。

- `TASK-027`（今日页撤销 + 归档）：真机自测通过，缺独立评审。归档无二次确认
- `TASK-028`（归档恢复入口）：真机自测通过（建 → 归档 → 我的页 → 恢复 → 今日页），缺独立评审
- `TASK-029`（编辑习惯 + 统计）：真机自测通过，缺独立评审
- `TASK-031`（统计页按设计稿重建）：真机自测通过（打卡前后两张截图），缺独立评审。**已知缺口**：周/年切换未实现（标签显式变淡）、图标仍为实心图标
- `TASK-032`（今日页 + 新建页按设计稿重建）：构建与单测通过；真机对照今日页/全部完成态/新建页通过，图标颜色落库已在设备确认；缺独立评审
- `TASK-034`（统计页滚动 + 导航栏隐藏 + 删除 + 归档分组）：145 项单测与构建通过，真机四项走通；缺独立评审。已知缺口：归档分组只有单月单日真机数据、删除未对带日志习惯做真机级联验证
- `TASK-033`（习惯详情页）：154 项单测与构建通过，真机进入详情/打卡后 1/1 与 100%/编辑弹窗/返回走通；缺独立评审。已知缺口：真机只有单日一个计划日，多周网格与「未计划日」仅有单测证据
- `TASK-026`（设备侧 Room 契约测试）：5 个新用例 + SmokeTest 共 6 项真机通过；已知缺口：未覆盖迁移与 Cursor 全量映射。另实测确认 `connectedAndroidTest` 结束会卸载 App，跑前必须备份 DB

## 在册例外（main 上的未完成任务）

`AGENTS.md` §7 要求 `PARTIAL` 任务不进 `main`；以下任务因环境限制（无设备、沙箱禁止 Gradle 测试 worker）必须留在 `main`，按 ADR-012 登记为例外：

- `TASK-003`：`PARTIAL` —— 构建通过；三页导航与安装现已在真机手动走查（`TASK-012`/`TASK-016`），但 `TASK-003` 的验收项本身尚未按自动化方式关闭。
- `TASK-004`：`PARTIAL` —— schema 与约束已用 `node:sqlite` 验证 40/40，Room 运行时未验证。
- `TASK-005`：`PARTIAL` —— 27 项纯 JVM 单测通过，Room 事务集成与设备持久化未验证。

新增例外必须写在这里，否则 `tools/check-governance.ps1` 失败。

## Git 状态

- 仓库：已初始化，无远程、未推送；`main` 为稳定分支，治理任务在 `feat/*` 分支评审后合入（当前分支以 `git branch --show-current` 为准）
- 工作区：构建产物、`local.properties`、`.gradle/`、`.kotlin/` 均已忽略
- 最近 commit：以 `git log -1 --oneline` 为准；基线检查点 `d0f470a`

以上字段已于 `2026-11-05` 按实际构建结果、`node tools/verify-schema.mjs`、`adb devices` 与检查器结果更新。
