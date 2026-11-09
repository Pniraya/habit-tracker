# TASK-030: MVP 收口（备份导出 + 真机巡检）

- 状态：`DONE`（独立 Reviewer `APPROVE`，见 §13）
- 阶段：`Gate 3`
- 优先级：`P0`（MVP 收口）
- 依赖：`TASK-029`
- 创建日期：`2026-11-05`
- 完成日期：`2026-11-06`

## 1. Objective
把 MVP 从「功能闭环」推到「可以交到用户手里」：加一个备份导出（数据搬得走），清掉未使用的权限，并做一次真机巡检。

## 2. Context
- 主路径已闭环（建 → 改 → 打卡 → 撤销 → 归档 → 恢复 → 统计），但数据只在设备里，卸载即丢。
- `POST_NOTIFICATIONS` 与 `RECEIVE_BOOT_COMPLETED` 已声明却无代码，属遗留声明。
- 设备在实现期间一度离线，真机项延后到设备恢复后补跑（2026-11-09）。

## 3. Scope
- 备份导出：按 `DATABASE.md` 的格式导出为 JSON，走 SAF（`ACTION_CREATE_DOCUMENT`），不申请存储权限。
  - 新增 `data/backup/`：`BackupDocument`（纯数据模型）、`BackupJson`（手写编码器，字段映射见 `ADR-018`）、`BackupExporter`（读四表 → JSON → SAF 输出）。
  - DAO 新增两条只读查询：`HabitLogDao.findAllForBackup()`、`HabitReminderDao.findAllForBackup()`；`verify-dao.mjs` 覆盖到 40 项。
  - 「我的」页新增「数据备份」卡片：点「导出备份」→ 系统文件选择器（默认名 `habit-tracker-backup-<日期>.json`）→ 成功显示「已导出 N 个习惯」。
- 清理未使用的权限声明（`POST_NOTIFICATIONS`、`RECEIVE_BOOT_COMPLETED`），提醒功能实现时再加回。
- 真机巡检一轮：今日页、统计页、我的页与导出流程。

## 4. Out of Scope
- 备份**导入**（`F-014` 的恢复方向）由 `TASK-039` 交付；提醒（`F-012`/`F-013`）推迟到 MVP 之后。
- 备份不含自定义图标的图片文件（只有 `custom:<file>` 字符串），见 `ADR-018` 的已知缺口。

## 5. Expected Files
- 新增：`data/backup/{BackupDocument,BackupJson,BackupExporter}.kt`、`app/src/test/java/com/habittracker/data/backup/BackupJsonTest.kt`、`project/decisions/ADR-018-json-backup-fields.md`
- 修改：`ProfileScreen.kt`、`HabitLogDao.kt`、`HabitReminderDao.kt`、`AndroidManifest.xml`、`tools/verify-dao.mjs`、状态文档

## 6. Acceptance Criteria
- [x] 导出的 JSON 能被同一 schema 的数据库回读：`.gradle/verify-backup.mjs` 用 `app/schemas/.../1.json` 建内存库，逐行插入 3 习惯 / 3 计划 / 5 日志 / 0 提醒，计数与行值校验通过；并与设备库计数一致
- [x] 未使用的权限被删除：`aapt2 dump permissions` 只剩 androidx 自生成权限，运行时权限为 0
- [x] 真机巡检记录写入 `TEST_REPORT`：今日页（11 月 9 日 · 周一，0/2，连续 3 天）、统计页（本月完成率 71%、最长连续 3 天、热力图当日描边）、我的页与导出成功提示

## 7. Tests
- `gradlew.bat compileDebugKotlin`：**BUILD SUCCESSFUL**
- `tools/run-unit-tests.ps1`：**OK (168 tests)**，新增 `BackupJsonTest` 6 项
- `node tools/verify-dao.mjs`：**DAO SQL PASS（40 项）**，覆盖两条备份查询的排序
- 真机：导出 → `已导出 3 个习惯`，文件 3,373 B 落 `Download/habit-tracker-backup-2026-11-09.json`
- `node .gradle/verify-backup.mjs`：**BACKUP VALID AND RE-READABLE**
- `tools/verify-all.ps1`：**VERIFY ALL PASS**

## 8. Risks and Decisions
- **格式先行**：字段名一旦发布就难改，先用 `ADR-018` 把「列名即键名」「枚举写名字」「布尔 true/false」「空表 `[]`、可空写 null」钉死，再由测试守住。
- **手写 JSON 编码器**：org.json 不在 JVM 测试类路径，引入序列化库只为一个文件格式不划算；转义与控制字符由单测覆盖。
- **SAF 而非存储权限**：`CreateDocument` 由系统创建文件，应用只拿到写句柄；导出的文件位于系统「下载」目录。
- **空习惯表跳过 `IN ()`**：部分 SQLite 构建对空 `IN` 报语法错，导出器显式短路。
- **删权限的影响**：安装更新不涉及权限撤销提示；提醒实现时按 `DATABASE.md` 重新声明这两个权限即可。
- **导出不改数据**：只读四表；真机导出后数据库计数不变。

## 9. Implementation Log
- 2026-11-06：定下字段映射（`ADR-018`，列名即键名），先写 6 项 JVM 测试驱动编码器；首轮空表按多行数组输出，测试期望 `[]`，改为空表紧凑输出后通过。
- 2026-11-06：`BackupExporter` 读四表并按 ISO 日期 / `HH:mm:ss` / epoch 毫秒写出；空习惯表时跳过 `IN ()` 查询。
- 2026-11-06：「我的」页接 SAF（`CreateDocument("application/json")`），结果文案区分成功与失败。
- 2026-11-06：删除未使用的 `POST_NOTIFICATIONS` 与 `RECEIVE_BOOT_COMPLETED`，注释说明随提醒功能回归；`aapt2 dump permissions` 确认。
- 2026-11-06：`verify-dao.mjs` 首轮报「未覆盖 `findAllForBackup`」，补提醒 DAO 解析与两条排序场景，38 → 40 项。
- 2026-11-09：设备恢复，安装分支 APK → 我的 → 导出备份 → 下载目录保存成功；拉回 JSON 用 `node:sqlite` 按 schema v1 回读并比对设备库；巡检今日/统计/我的三页。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `gradlew.bat compileDebugKotlin` | **BUILD SUCCESSFUL** | 42s |
| `tools/run-unit-tests.ps1` | **OK (168 tests)** | 162 → 168：`BackupJsonTest` 6 项 |
| `node tools/verify-dao.mjs` | **DAO SQL PASS（40 项）** | 新增 `findAllForBackup` 排序场景；`MIN_CHECKS` 38 → 40 |
| `aapt2 dump permissions` | **PASS** | 仅 `com.habittracker.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`，运行时权限 0 |
| 真机：我的 → 导出备份 → 保存 | **PASS** | 提示「已导出 3 个习惯」；`Download/habit-tracker-backup-2026-11-09.json` 3,373 B |
| `node .gradle/verify-backup.mjs` | **PASS** | 3 习惯（含 1 归档）/ 3 计划 / 5 日志 / 0 提醒；回读与设备库计数一致 |
| 真机巡检：今日 / 统计 / 我的 | **PASS** | 11 月 9 日 · 周一；今日 0/2、连续 3 天；统计本月 71%、最长连续 3 天、当日格描边；我的页数据备份卡片 |
| `tools/verify-all.ps1` | **VERIFY ALL PASS** | 治理 22/22、单测 168、DAO 40、构建、`git diff --check` |

## 11. Completion Review
- [x] 验收标准满足
- [x] 测试实际运行
- [x] 独立 Reviewer `APPROVE`（`REVIEW-TASK-030`，第 2 轮，0 阻断）

## 12. Handoff
- 导入（`TASK-030b`）按 `ADR-018` 读取；`DATABASE.md` §11 的合并/覆盖策略待定。
- 之后：`F-012`/`F-013` 提醒（先 `N5` ADR）、`F-011` 热力图。

## 13. Independent Review
- Reviewer record：[`REVIEW-TASK-030`](../reviews/REVIEW-TASK-030.md)
- 结论：`APPROVE`（第 2 轮独立评审，0 阻断）。第 1 轮只挑出文档计数；第 2 轮复核导出字段与 `ADR-018` 逐键一致、计数已按实测同步。

## 14. 评审修复（2026-11-09，第 2 轮）

- 评审阻断项只有文档计数：`README.md`（159 → 182、38/38 → 45/45）、`PROJECT_STATUS.md`（38/38 → 45/45）、`TEST_REPORT.md`（31 项 → 45 项）已按实际结果同步 182 单测 / 45 DAO 校验。
- 代码未改：导出字段与 `ADR-018` 逐键一致，评审已用真实导出文件反查。

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-030
- 状态：`DONE`
- 阶段：Gate 3
- 验收：通过 3 项 / 未通过 0 项
- 最近测试记录：2026-11-09 / **OK (182 tests)**，`verify-all` 全绿
<!-- close-task:end -->
