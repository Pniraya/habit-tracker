# TASK-030: MVP 收口（备份导出 + 真机巡检）

- 状态：`PARTIAL`（导出已实现并通过 6 项 JVM 测试与权限清理检查；真机导出与巡检因设备离线 `NOT_RUN`）
- 阶段：`Gate 3`
- 优先级：`P0`（MVP 收口）
- 依赖：`TASK-029`
- 创建日期：`2026-11-05`
- 完成日期：`TBD`

## 1. Objective
把 MVP 从「功能闭环」推到「可以交到用户手里」：再加一个备份导出（数据搬得走），并做一次完整的真机巡检。

## 2. Context
- 主路径已闭环（建 → 改 → 打卡 → 撤销 → 归档 → 恢复 → 统计），但数据只在设备里，卸载即丢。
- `POST_NOTIFICATIONS` 与 `RECEIVE_BOOT_COMPLETED` 已声明却无代码，属遗留声明，收口时要么实现提醒要么删除。

## 3. Scope
- 备份导出：按 `DATABASE.md` 的格式导出为 JSON，走 SAF（`ACTION_CREATE_DOCUMENT`），不申请存储权限。
  - 新增 `data/backup/`：`BackupDocument`（纯数据模型）、`BackupJson`（手写编码器，字段映射见 `ADR-018`）、`BackupExporter`（读四表 → JSON → SAF 输出）。
  - DAO 新增两条只读查询：`HabitLogDao.findAllForBackup()`、`HabitReminderDao.findAllForBackup()`。
  - 「我的」页新增「数据备份」卡片：点「导出备份」→ 系统文件选择器（默认名 `habit-tracker-backup-<日期>.json`）→ 成功显示「已导出 N 个习惯」。
- 清理未使用的权限声明（`POST_NOTIFICATIONS`、`RECEIVE_BOOT_COMPLETED`），提醒功能实现时再加回。
- 真机巡检一轮：三个页面 + 空态 + 归档恢复 + 统计。

## 4. Out of Scope
- 备份**导入**（`F-014` 的恢复方向）推迟到 `TASK-030b`；提醒（`F-012`/`F-013`）推迟到 MVP 之后。
- 备份不含自定义图标的图片文件（只有 `custom:<file>` 字符串），见 `ADR-018` 的已知缺口。

## 5. Expected Files
- 新增：`data/backup/{BackupDocument,BackupJson,BackupExporter}.kt`、`app/src/test/java/com/habittracker/data/backup/BackupJsonTest.kt`、`project/decisions/ADR-018-json-backup-fields.md`
- 修改：`ProfileScreen.kt`、`HabitLogDao.kt`、`HabitReminderDao.kt`、`AndroidManifest.xml`、状态文档

## 6. Acceptance Criteria
- [ ] 导出的 JSON 能被同一 schema 的数据库回读（有测试或脚本验证）——**未完成**：设备离线，文件未真机产出
- [x] 未使用的权限被删除：`POST_NOTIFICATIONS`、`RECEIVE_BOOT_COMPLETED` 已删，`aapt2 dump permissions` 只剩 androidx 的自定义权限
- [ ] 真机巡检记录写入 `TEST_REPORT`——**未完成**：设备离线

## 7. Tests
- `gradlew.bat compileDebugKotlin`：**BUILD SUCCESSFUL**（42s）
- `tools/run-unit-tests.ps1`：**OK (168 tests)**，新增 `BackupJsonTest` 6 项
- `aapt2 dump permissions`：运行时权限 0 个
- 真机导出与巡检：`NOT_RUN`（`adb devices` 为空）

## 8. Risks and Decisions
- **格式先行**：字段名一旦发布就难改，先用 `ADR-018` 把「列名即键名」「枚举写名字」「布尔 true/false」「空表 `[]`、可空写 null」钉死，再由测试守住。
- **手写 JSON 编码器**：org.json 不在 JVM 测试类路径，引入序列化库只为一个文件格式不划算；转义与控制字符由单测覆盖。
- **SAF 而非存储权限**：`CreateDocument` 由系统创建文件，应用只拿到写句柄。
- **空习惯表跳过 `IN ()`**：部分 SQLite 构建对空 `IN` 报语法错，导出器显式短路。
- **删权限的影响**：安装更新不涉及权限撤销提示；提醒实现时按 `DATABASE.md` 重新声明这两个权限即可。

## 9. Implementation Log
- 2026-11-06：定下字段映射（`ADR-018`，列名即键名），先写 6 项 JVM 测试驱动编码器；首轮空表按多行数组输出，测试期望 `[]`，改为空表紧凑输出后通过。
- 2026-11-06：`BackupExporter` 读四表并按 ISO 日期 / `HH:mm:ss` / epoch 毫秒写出；空习惯表时跳过 `IN ()` 查询。
- 2026-11-06：「我的」页接 SAF（`CreateDocument("application/json")`），结果文案区分成功与失败。
- 2026-11-06：删除未使用的 `POST_NOTIFICATIONS` 与 `RECEIVE_BOOT_COMPLETED`，注释说明随提醒功能回归；`aapt2 dump permissions` 确认。
- 2026-11-06：真机验证被阻：`adb devices` 为空（设备已断开），导出文件与巡检未跑，任务记 `PARTIAL`。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `gradlew.bat compileDebugKotlin` | **BUILD SUCCESSFUL** | 42s |
| `tools/run-unit-tests.ps1` | **OK (168 tests)** | 162 → 168：`BackupJsonTest` 6 项（空表头、列名与 ISO 日期、null 不省略、转义、提醒时间/布尔、文件名） |
| `aapt2 dump permissions` | **PASS** | 只剩 `com.habittracker.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`（androidx 生成），运行时权限为 0 |
| 真机：导出文件 → node 解析/回读 | **NOT_RUN** | `adb devices` 为空，设备离线；恢复连接后补跑 |
| `tools/verify-all.ps1` | **VERIFY ALL PASS** | 治理 22/22、单测 168、DAO 40、构建、`git diff --check`；首轮 3 项治理失败（README 任务名、ADR-018 未登记、缺 verify-all 证据行）与 DAO 未覆盖新查询，均已修 |

## 11. Completion Review
- [ ] 验收标准满足（设备项未完成）
- [x] 已运行的测试有证据
- [ ] 独立 Reviewer `APPROVE`

## 12. Handoff
- 设备恢复后：安装本分支 APK → 我的 → 导出备份 → 系统选择器保存到 Downloads → `adb pull` → `node -e` 解析校验 → 与数据库行数对比 → 巡检三页（补进 `TEST_REPORT`），通过后转 `IMPLEMENTED` 并合入 `main`。
- 之后：`F-012`/`F-013` 提醒、`F-014` 导入、`F-011` 热力图。

## 13. Independent Review
- Reviewer record：待创建
- 结论：`PENDING`

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-030
- 状态：`PARTIAL`
- 阶段：Gate 3
- 验收：通过 1 项 / 未通过 2 项
- 最近测试记录：2026-11-06 / **OK (168 tests)**
<!-- close-task:end -->
