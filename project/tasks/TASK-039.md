# TASK-039: 备份导入（覆盖式恢复）

- 状态：`IMPLEMENTED`（`ADR-015`：实现与验证完成，未经独立评审）
- 阶段：`Gate 3`
- 优先级：`P0`
- 依赖：`TASK-030`（导出 + `ADR-018` 字段映射）
- 创建日期：`2026-11-09`
- 完成日期：`2026-11-09`

## 1. Objective
让用户把导出的 JSON 重新写回应用：选择文件 → 校验 → 二次确认 → 单事务覆盖。数据安全优先于功能数量，因此 V1 只做「清空后恢复」。

## 2. Context
- `TASK-030` 已交付导出（`data/backup/`、`ADR-018`、真机导出 3 个习惯并被 `node:sqlite` 回读）。
- `DATABASE.md` §11 要求导入先全量校验、再单事务写入；同时列出「合并导入」与「清空后恢复」两种操作。用户已确认 V1 采用**覆盖**方式，合并导入推迟。

## 3. Scope
- `ADR-019`：V1 只做覆盖式恢复、二次确认、先校验后写库、保留原始 ID、8 MB 上限。
- `data/backup/BackupParser.kt`：最小 JSON 读取器 + 全量校验（格式/版本/类型/枚举/日期/引用/唯一性/范围/状态一致性），错误带路径。
- `data/backup/BackupRestorer.kt`：`withTransaction` 内先删四表再按原 ID 写回。
- 四个 DAO 增加 `deleteAll()`；`verify-dao.mjs` 覆盖删除语句（38 → 45 项）。
- 「我的」页「从备份恢复」：`OpenDocument` → 解析 → 摘要确认弹窗（习惯数/日志数、不可撤销）→ 恢复 → 结果文案。
- 文件选择用 `*/*`：从电脑拷入的 JSON 常无 MIME 类型，按内容校验更可靠。

## 4. Out of Scope
- 合并导入（按 ID 去重、冲突选择）留待后续任务。
- 提醒调度重建（V1 无提醒）。
- 自定义图标的图片文件不在 JSON 内，导入后文件缺失则回退嫩芽。

## 5. Expected Files
- 新增：`data/backup/BackupParser.kt`、`data/backup/BackupRestorer.kt`、`app/src/test/java/com/habittracker/data/backup/BackupParserTest.kt`、`project/decisions/ADR-019-backup-import.md`、本文件
- 修改：`ProfileScreen.kt`、`HabitDao.kt`、`HabitScheduleDao.kt`、`HabitLogDao.kt`、`HabitReminderDao.kt`、`tools/verify-dao.mjs`、状态文档

## 6. Acceptance Criteria
- [x] 校验失败不写库：解析/校验在内存完成，任何错误整体失败并显示字段路径
- [x] 覆盖前二次确认：弹窗写明习惯数、日志数与「不可撤销」
- [x] 单事务：删除与写入在 `withTransaction` 内，异常回滚
- [x] 保留原始 ID 与引用，导入后自增序列从最大 ID 继续
- [x] 8 MB 上限；`*/*` 选择器
- [x] 单测 177 项、DAO 校验 45 项通过
- [x] 真机：用户（设备所有者）确认导出与导入均正常，覆盖式更新

## 7. Tests
- `gradlew.bat compileDebugKotlin`：**BUILD SUCCESSFUL**
- `tools/run-unit-tests.ps1`：**OK (177 tests)**，新增 `BackupParserTest` 9 项
- `node tools/verify-dao.mjs`：**DAO SQL PASS（45 项）**
- 真机：`adb` 驱动进入「我的 → 从备份恢复」能打开系统文件选择器；完整选择流程由用户手动完成并确认成功

## 8. Risks and Decisions
- **只做覆盖**：合并导入的冲突交互复杂，V1 先保证「能恢复」；`ADR-019` 记录推迟理由。
- **手写 JSON 解析器**：org.json 不在 JVM 测试类路径；解析器只支持 RFC 8259 子集，但备份格式固定，测试覆盖转义与控制字符。
- **校验先于写库**：避免用户点了「覆盖」才发现文件损坏，数据已被清空。
- **ID 保留**：日志与计划的 `habit_id` 直接可用，不需要重映射。
- **8 MB**：正常备份只有几 KB，上限防止恶意/误选大文件。
- **`*/*` 选择器**：JSON 从电脑拷入时常无 MIME，按扩展名过滤会「看不到文件」；改为接受任意文件 + 内容校验。

## 9. Implementation Log
- 2026-11-09：先写 `ADR-019` 定策略，再写解析器与 9 项单测；`wrong()` 的返回类型首轮写成 `Nothing` 导致编译失败，改回 `Nothing?`。
- 2026-11-09：可选字段用「错误数是否增加」判断，避免把 JSON `null` 当成解析失败。
- 2026-11-09：`verify-dao.mjs` 首轮 `deleteAll` 的 changes 断言失败——被级联删掉的提醒行已不存在；改为先插入一条再删。
- 2026-11-09：真机文件选择器只显示文件夹、看不到 JSON（MIME 缺失），把过滤器改为 `*/*` 后可见；完整恢复流程由用户在真机确认成功。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `gradlew.bat compileDebugKotlin` | **BUILD SUCCESSFUL** | 解析器、恢复器、SAF 接线编译通过 |
| `tools/run-unit-tests.ps1` | **OK (177 tests)** | 168 → 177：`BackupParserTest` 9 项（往返、格式/版本、悬空引用、枚举/日期、状态一致性、DAILY 掩码、坏 JSON、转义、未知键） |
| `node tools/verify-dao.mjs` | **DAO SQL PASS（45 项）** | 4 个 `deleteAll` + 全表清空断言；`MIN_CHECKS` 40 → 45 |
| 真机：我的 → 从备份恢复 → 系统选择器 | **PASS（用户手动）** | 用户确认「导出和导入功能正常，数据更新方式为覆盖」；AI 侧只验证到选择器打开（下载目录文件过多，未定位到备份文件） |
| `tools/verify-all.ps1` | **VERIFY ALL PASS** | 治理 22/22、单测 177、DAO 45、构建、`git diff --check` |

## 11. Completion Review
- [x] 验收标准满足
- [x] 测试实际运行
- [ ] 独立 Reviewer `APPROVE`（`ADR-015` 批量评审）

## 12. Handoff
- 合并导入（`DATABASE.md` §11 第 5 条）未做；需要时另立任务并补 ADR。
- 导入后若图标图片缺失，行内回退嫩芽；备份不含图片文件是有意为之（`ADR-018`）。

## 13. Independent Review
- Reviewer record：待创建（批量评审）
- 结论：`PENDING`（`IMPLEMENTED`）

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-039
- 状态：`IMPLEMENTED`
- 阶段：Gate 3
- 验收：通过 7 项 / 未通过 0 项
- 最近测试记录：2026-11-06 / **OK (168 tests)**
<!-- close-task:end -->
