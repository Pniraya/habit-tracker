# BATCH_REVIEW.md（ADR-015 批量评审模板）

> 项目完成时使用。一次覆盖所有 `IMPLEMENTED` 任务；每个任务仍必须写出独立的 `project/reviews/REVIEW-TASK-xxx.md`，因为治理第 18 项只认 per-task 记录。

## 评审范围
- 里程碑/版本：
- 基线 commit（`git rev-parse HEAD`）：
- 覆盖任务（`IMPLEMENTED` 清单）：
- 未覆盖任务及原因：

## 一次性机械核对
| 项 | 实测 | 备注 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | | 退出码必填 |
| `tools/run-unit-tests.ps1` | | |
| `node tools/verify-dao.mjs` | | |
| `connectedDebugAndroidTest`（真机） | | 唤醒 + `stayon usb` |
| `git status --short` | | 必须为空 |

## 逐任务结论
| 任务 | 结论 | 阻断项 | 之后动作 |
|---|---|---|---|
| TASK-xxx | APPROVE / REJECT | | |

## 跨任务发现
- 结构性缺陷（多任务共有）：
- 文档口径漂移：
- 缺失的闸门/探针：

## 结论
- 结论：`APPROVE` / `REJECT`
- 允许的后续动作：
- 遗留风险：