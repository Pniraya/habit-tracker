# TASK-030: MVP 收口（备份导出 + 真机巡检）

- 状态：`NOT_STARTED`
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
- 清理未使用的权限声明，或说明保留理由。
- 真机巡检一轮：三个页面 + 空态 + 归档恢复 + 统计。

## 4. Out of Scope
- 备份**导入**（`F-014` 的恢复方向）推迟；提醒（`F-012`/`F-013`）推迟到 MVP 之后。

## 5. Expected Files
- `feature/profile/ProfileScreen.kt`、`data/backup/BackupExporter.kt`、`app/src/main/AndroidManifest.xml`

## 6. Acceptance Criteria
- [ ] 导出的 JSON 能被同一 schema 的数据库回读（有测试或脚本验证）
- [ ] 未使用的权限被删除，或删除不了时在文档里写清原因
- [ ] 真机巡检记录写入 `TEST_REPORT`

## 7. Risks
- 备份格式一旦发布就难改，`DATABASE.md` 的格式约束必须先读，不能自创字段名。

## 9. Implementation Log
- 尚未开始。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | 未运行（任务未开始） | 开工后补真实结果 |

## 11. Completion Review
- [ ] 验收标准满足
- [ ] 测试实际运行
- [ ] 独立 Reviewer `APPROVE`

## 12. Handoff
- 之后：`F-012`/`F-013` 提醒、`F-014` 导入、`F-011` 热力图。

## 13. Independent Review
- Reviewer record：待创建
- 结论：`PENDING`