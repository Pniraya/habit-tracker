# LAST_HANDOFF.md

> 每次 AI 会话结束前更新。记录事实，不写愿望或推测。

## Snapshot
- 日期时间（含时区）：`2026-11-06`（Asia/Shanghai）
- 当前阶段：`Gate 3`（出口按外部评审重定义为：`TASK-026` 完成 + 窄口径里程碑评审 `R1` 通过）
- 当前任务：`TASK-026`（`IMPLEMENTED`：设备侧 Room 契约测试 5 项，真机 6/6）；`TASK-027/028/029/031/032/033/034` 均 `IMPLEMENTED` 待批量评审；下一条 `R1` 评审，然后 `TASK-030a` 导出
- 当前 Git 分支：`main`（本地，无远程）；最近 commit 以 `git log -1 --oneline` 为准
- **会话状态：可继续**——外部评审 `project/reports/ADVICE-RESPONSE-2026-11-06.md` 已收到并采纳主线；设备（`10CDAK0B71005KU`）在线且数据已恢复
- **设备测试怎么跑**：手机**唤醒** + `adb shell svc power stayon usb` + `gradlew.bat connectedDebugAndroidTest`；**跑前必须备份 DB**（见下）

## Completed This Session
- 收到外部评审（`ADVICE-RESPONSE-2026-11-06.md`，374 行），采纳其主线：先 `TASK-026`，再窄口径评审，再拆备份。
- `TASK-026`（设备侧 Room 契约测试）：
  - 新增 `app/src/androidTest/.../DatabaseContractTest.kt`：每用例独立临时文件 DB，使用与生产完全相同的 `DatabaseConstraints.callback()`
  - 5 个用例：触发器在生产打开路径注册 + 重开后仍生效；Converter/实体往返（闰日、REAL 精度、可空 unit、枚举、UTC 切换）；Flow 失效；四表级联删除；`changeSchedule` 在关闭旧版本后抛错仍回滚
  - 真机 `connectedDebugAndroidTest`：**6 tests / 0 failures**（40s）
- 数据安全（外部评审 R1）**实测确认并处置**：
  - 跑设备测试前备份 `databases/habit-tracker.db{,-wal,-shm}` 到 `.gradle/db-backup-20261106`
  - 跑完 `pm list packages com.habittracker` **为空**——AGP 确实卸载了 App，数据被清空
  - 重装 + `adb push` + `run-as cp` 恢复，今日页重新出现 `Rread`/`Wa'l'k`/`Wa't'er`
- 代码事实核对（回应评审的「需确认」项）：
  - `AndroidManifest.xml` 已经是 `allowBackup="false"`，无 `dataExtractionRules`（R1 的云备份风险已排除）
  - `HabitDatabase` 没有 `fallbackToDestructiveMigration()`，`exportSchema = true`
  - `verify-schema.mjs`/`verify-dao.mjs` 读的是 `app/schemas/.../1.json` + `DatabaseConstraints.kt` 真实文本，**不是手抄 DDL**（评审 B3 的漂移疑虑不成立）
  - 无签名配置、无 `applicationIdSuffix`（R1 的 keystore 与「测试安装清数据」风险仍成立）

## Files Changed
- 新增：`app/src/androidTest/java/com/habittracker/data/local/DatabaseContractTest.kt`、`project/reports/ADVICE-RESPONSE-2026-11-06.md`
- 修改：`project/tasks/TASK-026.md`（重写为 `IMPLEMENTED`）、`PROJECT_STATUS.md`、`README.md`、`TEST_REPORT.md`、`FEATURE_MATRIX.md`（数据层契约脚注）、本文件

## Verification Actually Run
| 命令 | 结果 | 备注 |
|---|---|---|
| `gradlew.bat assembleDebug assembleDebugAndroidTest` | **BUILD SUCCESSFUL** | 6s |
| `gradlew.bat connectedDebugAndroidTest` | **PASS**（40s） | `Starting 6 tests` / `Finished 6 tests`；XML `tests="6" failures="0" errors="0"` |
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS**（exit 0） | 治理 22/22、单测 154、DAO 38、构建、`git diff --check` |
| 数据备份/恢复 | **PASS** | 备份 3 个文件 → 测试后 App 被卸载 → 重装 + `run-as cp` → 今日页 3 个习惯恢复 |

## Not Verified / Known Gaps
- **设备测试会卸载 App（已实测）**：每次设备测试前备份 DB；长期方案（`applicationIdSuffix` 或专用测试机）未定
- **未覆盖迁移**：schema 仍 v1，无 `MigrationTestHelper` 模板；第一次 v2 前要建
- **Cursor → 实体只抽查关键字段**，不是全量字段映射；并发写入未测
- **UI 层仍无自动化**（外部评审 `N6`，≤8 用例，依赖本任务底座）
- **评审债 7 项未还**：外部评审要求由**新上下文**做窄口径评审（偏差表 + 写路径/规则边界 + 变异抽查），不能用本会话自查
- **真机数据仍是单日计划版本**：多周/跨月、跨年边界只有单测
- 待用户拍板：归档层级语义、日切点（0 点 vs 4 点）、提醒是否进 MVP、恢复只做覆盖、第三张卡口径、SDK 许可复核

## Lessons Worth Keeping
- **`connectedAndroidTest` 会卸载 App**：不是「可能」，是实测；跑前 `force-stop` + `run-as cat` 备份 `db/-wal/-shm`，跑后重装 + `push`/`cp` 恢复
- **`withTransaction` 的回滚要挑「已经写了一步」的路径测**：`changeSchedule` 先关旧版本再抛错，才能证明回滚而不是证明「异常之前没写」
- **触发器测试必须重开数据库**：只测首次打开漏掉「恢复/导入的库」这条真实路径
- **验证工具读的是导出 schema 与真实源码**：评审质疑「SQL 是手抄副本」时，先看工具入口再改测试

## Next Safe Action
1. **`R1` 窄口径里程碑评审**：由不带实现上下文的新会话执行；按 `ADVICE-RESPONSE` Q1 的 4 步（偏差表、写路径/规则边界、变异抽查约 10 个、产出 1 份 review），把 7 个 `IMPLEMENTED` 一起转 `DONE` 或列阻塞项
2. 与 `R1` 并行：`N3` 偏差 ADR + `project/DESIGN-DELTA.md` + 归档语义用户确认
3. 之后：`N5` 提醒数据模型 ADR（先于备份格式）→ `TASK-030a` JSON 导出（只读）
4. 恢复开工前先跑 `tools/verify-all.ps1` 确认 `main` 仍绿；不要把 `TASK-026` 改成 `DONE`——它缺独立评审
