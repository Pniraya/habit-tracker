# REVIEW-TASK-012: 独立评审记录

- 审查任务：`TASK-012`
- 审查 commit：`07b1aab559e7ac66d530a88850ecf5b616b4307c`（第 3 轮）
- 审查范围：`git diff main...HEAD`（`cc4fba5`..`07b1aab`，3 个提交 `1f633a8` + `ba96c55` + `07b1aab`，14 文件 +771/−41；本轮重点为 `ba96c55..07b1aab` 的 4 个文档文件）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`
- 轮次：第 3 轮。第 1 轮（`1f633a8`）`REJECT`（unit 状态泄漏 + 文档失真）；第 2 轮（`ba96c55`）`REJECT`（代码达标，仅计数不一致）。本轮核对计数、`TASK-012` §14 真机证据与收尾同步。

评审时工作区：`git status --short` 为空（开始前）。本轮为只读核对、门禁复跑、1 组变异复验与设备侧数据库只读取证；所有改动已还原，结束时工作区仅剩本评审文件。

## 1. Scope

**裁决：通过。**

- 第 3 轮提交 `07b1aab` 只改 4 个文档文件（`README.md`、`TEST_REPORT.md`、`TASK-012.md` 与本评审记录），**产品代码与第 2 轮完全一致**，因此第 2 轮的变异结论继续有效（本轮已复验 M-1）。
- 全量 diff 仍无 `build.gradle.kts` / `libs.versions.toml` / `AndroidManifest.xml` / schema / 迁移改动：无新依赖、无权限变更、无 schema 改动。
- `TASK-012` §14 只增加手动走查记录，未引入超范围功能；发现的 UX 缺口按 Scope 留在任务外并写入 §12。

## 2. Diff 核对

**裁决：通过。**

- `git diff --check`：**exit 0**；冲突标记扫描无匹配。
- `07b1aab` 的改动与第 2 轮要求逐条对应：README 58 项、`TEST_REPORT` 的 `Latest Run` 58 项与 Known Gaps「9 项」、§10 的 verify-all 行 58 项 + 两行变异证据（含「未被抓住」的 M-2 且明确不计为已验证防线）、§12 与 `LAST_HANDOFF` 的下一任务一致。
- 文档改动没有顺手改代码，也没有把「未抓住」的变异写成通过——M-2 一行写的是 `未被抓住（58 全绿）`，这是诚实记录，值得保留。

## 3. 架构与数据

**裁决：通过。**

- 分层与数据约束未变：规则仍在 `HabitRules.normalizeTarget`（`RoomHabitRepository` 调用），Presenter 只做即时反馈；`CreateHabitUseCase` 只校验 name 的分层问题已作为**下一个任务**写入 §12 与 `LAST_HANDOFF`，不在本任务内擅自重构。
- unit 修复落在 Presenter（`onTrackingTypeChange` 清 `unit` + `draft()` 对 BOOLEAN 兜底），数值对话框在 UI 层拒绝负值，均未把业务规则塞进 Composable 之外的新位置。
- 已知 UX 缺口（Today 仅在空态提供新建入口）与旋转丢表单都已明确记录为遗留，没有伪装成已满足。

## 4. 验证证据

**裁决：通过（含 1 项仅我能部分复核的手动证据，见下）。**

| 命令 / 变异 | 实际结果 | 退出码 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | 治理 21/21（0 警告）、单测 **OK (58 tests)**、DAO SQL 21 项 0 失败、`assembleDebug` BUILD SUCCESSFUL 21s、空白 0 项 | **0** |
| `pwsh -NoProfile -File tools/check-governance.ps1`（单独复跑） | `SUMMARY: 21 checks, 21 passed, 0 warnings, 0 failed` | 0 |
| `git diff --check` | `no whitespace errors` | 0 |
| 复验 M-1：去掉 `onTrackingTypeChange` 的 unit 清空 | `Tests run: 58, Failures: 1`，`switchingFromNumericToBooleanClearsTheUnit` 变红 | **1** |
| 还原后 `tools/run-unit-tests.ps1` | `OK (58 tests)` / `RESULT: UNIT TESTS PASS` | 0 |

**设备侧独立核对（只读）**：`adb devices` 显示 `10CDAK0B71005KU device`，与 §14 的序列号一致；`dumpsys package com.habittracker` 为 `versionName=0.1.0`、`lastUpdateTime=2026-11-05 15:02:21`；用 `adb exec-out run-as com.habittracker cat databases/...` 拉取 db/wal/shm（4096 / 86552 / 32768 字节，与设备 `ls -l` 一致），分别用 `node:sqlite` 与 Python `sqlite3` 读取：`habits`、`habit_schedules`、`habit_logs` 均为 **0 行**，`sqlite_sequence` 为空，`PRAGMA integrity_check` 为 `ok`，`page_count=18`，`android_metadata=('zh_CN')`，`room_master_table` 有 identity hash；db/wal 的 mtime 为 `2026-11-05 15:03:57 +0800`，而截图 `saved.png`/`done.png` 分别为 15:03:11、15:03:22（设备时区）。

结论：§14 的 **schema 级事实可复现**（表、locale、identity hash、user_version=1），但**行级事实无法复现**——当前设备上的库是 15:03:57 重建的空库，说明走查读库之后应用数据被清过（或重装）并已重建；§14 未记录这次重置，也没有保留 dump，所以那几行 `tracking_type=BOOLEAN / unit=NULL / COMPLETED` 我只能视为**当时的、不可复核的手动陈述**。这不影响代码结论（M-1 已由我在 JVM 侧独立证明），但证据链本身应补一句说明。

未验证项（如实声明）：Compose 渲染与交互仍无自动化（手动走查不能替代）；旋转丢表单未实机复现；统计页/我的页仍是占位；数值输入对话框的真机路径因中文 IME 丢字符未走通。

## 5. 文档与分支

**裁决：通过（附 4 处非阻塞修订建议）。**

- 计数现已全部一致：`README.md:15` 58 项、`TEST_REPORT.md:9` 58 项、`TEST_REPORT.md:69` 新建表单 9 项、`TASK-012.md` §10 58 项 + 两行变异、§11「58 项 + 2 组变异」与 §10 对应、自动摘要块 `OK (58 tests)`、`PROJECT_STATUS.md:13` 58 项、`LAST_HANDOFF` 57→58。第 2 轮指出的 5 处全部关闭。
- `TASK-012` §12 的下一任务已与 `LAST_HANDOFF` 对齐（先规则上移，再图标/颜色与编辑）；§13 如实列出第 1、2 轮 `REJECT` 与当前待第 3 轮。
- 分支 `feat/create-habit`、三个聚焦提交、未推送，符合 `AGENTS.md` §7；评审开始与结束时工作区均干净（仅本文件为修改）。
- 非阻塞：`TASK-012.md:40`（§7）仍写「新增 `CreateHabitPresenterTest` 8 项」，实际 9 项；`LAST_HANDOFF.md:8` 仍写「首轮独立评审 `REJECT` 已修复待复审」，而第 2 轮也 `REJECT` 过（标记 `DONE` 时会重写，可顺带修）；§14 的截图清单列了 7 个文件名，`.gradle/shots` 实际有 8 个（漏了 `numfilled.png`）；§14 写了设备序列号，建议只保留型号。

## 6. 结论

- 结论：`APPROVE`
- 证据：`verify-all` exit 0（58 项、治理 21/21、DAO 21 项、构建 21s、空白 0）；M-1 复验 1 红后还原全绿；计数与状态文档一致；`07b1aab` 未改代码，第 2 轮已证明的修复继续有效。
- 关于「Today 只在空态提供新建入口」：**同意不属于 TASK-012 的阻断项**——§6 验收条件字面只要求「空态可进入新建页」，§1 Objective 也是「加一个习惯」，实现与验收一致且已在 §12 记录。但它是产品级 P0 缺口（有习惯后无法再建第二个），建议立即建任务并在 `LAST_HANDOFF` 的下一任务列表里并列，避免只有任务文件里能找到。
- 关于 §14 这种证据形式：**合法，且是设备验证应有的写法**（标明手动、给出可复现步骤与命令、列出未验证项、截图不入库），前提是把它当**时间点证据**：读库结果要么保留 dump，要么注明设备数据何时被重置。本次缺了这一步，故我在 §4 按「不可复现」记录，而不是照单全收。
- 非阻塞建议（合入时一并处理即可）：
  1. `TASK-012` §14 补一句「读库后清过应用数据，当前设备库为空，dump 未保留」，或在 `project/` 下留一份查询结果文本；序列号可改为只写型号。
  2. §7 的「8 项」改为「9 项」。
  3. `LAST_HANDOFF` 的「首轮 `REJECT`」改为「第 1、2 轮 `REJECT`」；标记 `DONE` 时重写该文件。
  4. §14 截图清单补 `numfilled.png`，或删掉清单只保留目录说明。
  5. 把「有习惯后无法新建」的 P0 缺口写进 `LAST_HANDOFF` 的下一任务列表（目前只在 `TASK-012` §12）。
