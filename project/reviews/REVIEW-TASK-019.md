# REVIEW-TASK-019: 独立评审记录

- 审查任务：`TASK-019`
- 审查 commit：`8184d34cdc262c7e2ec24468dbc8f33954ed2387`（第 2 轮）
- 审查范围：`git diff main...HEAD`（`9ebb646`..`8184d34`，2 个提交 `1eb9fed` + `8184d34`，6 文件 +147/−29；本轮重点为 `1eb9fed..8184d34` 的 5 个文档文件）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`
- 轮次：第 2 轮。第 1 轮（`1eb9fed`）`REJECT`，5 项文档不一致：状态文件仍写「UI 与设备未验证」、`F-002 未验证`、复合功能规则与 F-003 行矛盾、`CreateHabitPresenterTest` 计为 9 项、handoff 仍写 `main @ b64326c`。

评审时工作区：`git status --short` 为空（开始前）。本轮为只读核对 + 门禁复跑；结束时工作区仅剩本评审文件。

关于 commit 号：开发者消息先写成 `3c8b1ab`，随后更正为 `8184d34`；以 `git rev-parse HEAD` 为准，**实际 HEAD 是 `8184d34`**，`3c8b1ab` 在仓库中不存在。

## 1. Scope

**裁决：通过。**

- 仍是文档-only：只改 `FEATURE_MATRIX.md`、`PROJECT_STATUS.md`、`README.md`、`LAST_HANDOFF.md`、`TASK-019.md`，未动源码、依赖、schema。
- 第 1 轮的 5 项全部落在本提交里，没有顺手夹带功能改动。

## 2. Diff 核对

**裁决：通过。**

- `git diff --check`：**exit 0**；矩阵每行仍 ≥7 个管道单元格，治理词表检查通过（21/21）。
- `FEATURE_MATRIX.md:18` 的 F-003 保持 `IN_PROGRESS` / `VERIFIED` / `DEVICE_VERIFIED`，规则改为「实现状态列在子功能未完成时记 `IN_PROGRESS`；自动化/真机列按已验证子功能如实记录，脚注限定范围」——规则与行一致了；`TASK-019` §6 第 5 条同步改写，不再声称「不整体记 `DEVICE_VERIFIED`」。
- `FEATURE_MATRIX.md:47` 的 `CreateHabitPresenterTest` 由 9 项改为 **12 项**，与 `@Test` 计数一致。
- `README.md:13` 与 `PROJECT_STATUS.md:10` 改为 `DEVICE_VERIFIED` 并保留「逐页巡检与 UI 自动化仍缺」的限定，不再与 `PROJECT_STATUS.md:6` 的真机记录打架。
- `PROJECT_STATUS.md:22` 重写为按矩阵分档；`LAST_HANDOFF.md:9` 改为 `docs/feature-matrix`、`main @ 9ebb646`（已核对：`git rev-parse main` = `9ebb646`，分支在其上领先 2 个提交）。

## 3. 架构与数据

**裁决：通过。**

- 无代码改动；`AndroidManifest.xml` 仍只有 `POST_NOTIFICATIONS`、`RECEIVE_BOOT_COMPLETED`，无 `INTERNET`；依赖未变。
- 矩阵的三列语义（实现 / 自动化 / 真机）在规则里写清后自洽：F-003、F-004 用 `IN_PROGRESS` 表达「部分子功能未做」，其余列只记录已验证部分。

## 4. 验证证据

**裁决：通过。**

| 检查项 | 命令/文件 | 结果 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | 治理 + 单测 + DAO + 构建 + 空白 | **VERIFY ALL PASS（exit 0）**：治理 21/21、`OK (71 tests)`、DAO 21 项、BUILD SUCCESSFUL 19s、`no whitespace errors` |
| `pwsh -NoProfile -File tools/check-governance.ps1` | 21 项 | `SUMMARY: 21 checks, 21 passed, 0 warnings, 0 failed`，exit 0 |
| 矩阵抽查 | `FEATURE_MATRIX.md:16-22` | F-001/F-002/F-003/F-006/F-007 `DEVICE_VERIFIED`，F-004/F-005 `IN_PROGRESS`/`VERIFIED` 真机 `NOT_STARTED`，F-008~F-014 全 `NOT_STARTED`，F-016~F-019 `OUT_OF_SCOPE` |
| 测试计数 | `CreateHabitPresenterTest.kt` | 12 个 `@Test`，与索引一致 |
| 占位页 | `feature/statistics/StatisticsScreen.kt`、`feature/profile/ProfileScreen.kt` | 均存在且只有占位文字；`TASK-019` §10 已按此措辞 |
| F-001 真机出处 | `DEVICE_DUMP-2026-11-05.md:46` | 「15:02:xx 安装 APK、启动、进入新建页」，索引已引用并注明「仅安装/启动，未逐页巡检」 |
| handoff 分支事实 | `git rev-parse main` / `git log main..HEAD` | `main` = `9ebb646`；分支领先 2 个提交（`1eb9fed`、`8184d34`） |

未验证项：真机截图不入库，F-002/F-006/F-007 的真机列只能核到任务 §10 文字；本次不重跑真机。

## 5. 文档与状态文件

**裁决：通过（2 处措辞可在标 `DONE` 前顺手改，不阻断合并）。**

- 第 1 轮 5 项全部关闭：`PROJECT_STATUS.md:10`、`README.md:13`、`:22`、`FEATURE_MATRIX.md:47`、`LAST_HANDOFF.md:9` 都已对齐；`TASK-019` §10 的统计措辞、证据索引的 F-008~F-014 行、F-001 的 dump 引用也都补上。
- 残留（非阻塞）：`TASK-019.md:19` 的 §2 发现表仍写「`CreateHabitPresenterTest` 9 项」，实际 12 项——这张表记录的是盘点时的证据，应在标 `DONE` 前一并改成 12（或去掉计数），否则同一文件里 47 行的 12 与 19 行的 9 自相矛盾。
- 残留（非阻塞）：`LAST_HANDOFF.md:11` 写「最近 commit：`1eb9fed`」，而本文件所属的提交是 `8184d34`；handoff 无法引用包含自身的那次提交，属已知写法问题，但下一次会话可能误以为 HEAD 是 `1eb9fed`，建议改成「撰写时 HEAD」或在下一次交接时刷新。
- 残留（非阻塞）：`PROJECT_STATUS.md:22` 的分档列举了 F-001/F-002/F-006/F-007、F-003/F-004、F-008~F-014、F-016~F-019，**漏了 F-005（`VERIFIED`，真机 `NOT_STARTED`）与 F-015（`VERIFIED`，无自动化断言）**；补一句更完整，但不构成矛盾。
- 分支 `docs/feature-matrix`、2 个提交、未推送，符合 `AGENTS.md` §7；工作区干净。

## 6. 结论

- 结论：`APPROVE`
- 证据：`verify-all` exit 0（治理 21/21、0 警告、71 项、DAO 21 项、构建 19s、空白 0）；第 1 轮 5 项阻断全部关闭；F-003 的规则/行已自洽，计数 12 与 `@Test` 一致，handoff 的分支与 `main` 哈希经 `git` 核对属实。
- 需在 `DONE` 前修正（不阻断合并）：`TASK-019.md:19` 的「9 项」改 12（与 `FEATURE_MATRIX.md:47` 一致）。
- 非阻塞建议：handoff 的「最近 commit」写法（`1eb9fed` vs `8184d34`）在下一次交接时刷新；`PROJECT_STATUS.md:22` 补上 F-005、F-015 两档。
- 关于四问的最终结论（与第 1 轮一致，未因改文档而改变）：`IN_PROGRESS` 是正确 token 且不应拆行；F-005 的自动化 `VERIFIED` 由 `ScheduleRulesTest` 的三种频率断言支撑；F-015 保持 `VERIFIED`（无 `INTERNET` 权限是结构性保证，降级反而失真），但「manifest 不得出现 `INTERNET`」的自动守卫应随 `TASK-017` 落地；F-001 的 `DEVICE_VERIFIED` 偏薄但成立，且现已在索引里写明「仅安装/启动」，属于把薄弱点写在明处。
