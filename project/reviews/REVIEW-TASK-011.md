# REVIEW-TASK-011: 独立评审记录

- 审查任务：`TASK-011`
- 审查 commit：`110d3b4fcf3bb39dfd452c9db5fa6570a55e395a`
- 审查范围：`git diff main...HEAD`（`5e99ece`..`110d3b4`，2 个提交 `362925f` + `110d3b4`，12 文件 +680/−54）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`
- 轮次：第 2 轮。第 1 轮针对 `362925f` 判 `REJECT`（数值习惯点击写 1.0、拒绝测试不鉴别 use case、报告/交接未同步）；本轮针对 `110d3b4` 复审，结论见 §6。

评审时工作区：`git status --short` 空（开始前）。所有变异先备份再改、跑完立即还原；结束时工作区仅剩本评审文件，`git diff --check` exit 0。

## 1. Scope

**裁决：通过。**

- 改动仍限于 TASK-011 §5 列出的文件加 `LAST_HANDOFF.md`、`README.md`、`TEST_REPORT.md`；`git diff main...HEAD --name-status` 中没有 `build.gradle.kts` / `libs.versions.toml` / `AndroidManifest.xml` / schema / 迁移：**无新依赖、无权限变更、无 schema 改动**；无 DataStore、无提醒调度。
- 本轮新增的修复正是首轮指出的越界点：`toggle()` 只对 BOOLEAN 生效，`HabitRow` 对非 BOOLEAN 行禁用点击；数值输入仍按 §4 留给后续任务。
- `TodayScreen` 依旧只接收 `state` / `lastError` / 两个回调，自身不访问 Repository，渲染的是给定状态。

## 2. Diff 核对

**裁决：通过。**

- `git diff --check`：**exit 0**；无冲突标记、无行尾空白。
- 本轮 diff 聚焦：`TodayPresenter`（BOOLEAN 守卫 + 常量）、`TodayScreen`（enabled 条件 + 枚举比较）、测试 +2 项、三份状态文档；没有借机重构其他页面。
- 2 个提交职责清晰：`362925f` 实现接线，`110d3b4` 只修首轮问题。

## 3. 架构与数据

**裁决：通过。**

- 分层符合 `ARCHITECTURE.md` §5.2：Compose → ViewModel → UseCase → Repository；Presenter 纯 Kotlin，不引用 Android 类型。
- `TodayViewModel` 依旧很薄：转发 `state`/`lastError` 与三个方法，`onCleared()` 取消自己的 scope，工厂组装 Repository + UseCase。轮换时 ViewModel 存活、Presenter 随之存活；重复 `create()` 只会得到互相独立的实例，无共享静态状态。
- 守卫放在 Presenter（有测试覆盖）而不是只靠 UI 禁用点击：即使别的调用方直接调 `toggle()`，也不会写出错误值——两层都拦，方向正确。
- 无 schema/数据口径改动。

## 4. 验证证据

**裁决：通过（首轮两处测试缺陷均已闭合，且我复跑了同样的破坏性实验）。**

| 命令 | 我的实测 |
|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | **VERIFY ALL PASS**，exit **0**；治理 7s（21/21）、单测 21s（49 项）、DAO 0s（21 项）、构建 20s、空白 0s |
| `pwsh -NoProfile -File tools/check-governance.ps1` | `SUMMARY: 21 checks, 21 passed, 0 warnings, 0 failed` / `RESULT: OK`，exit **0** |
| `git diff --check` | exit **0** |

### 变异验证（本轮亲自执行，已还原）

| # | 变异 / 探针 | 结果 |
|---|---|---|
| M-b | `record()` 绕过 use case，直接 `repository.recordDailyValue(...)` | **现在被抓住**：`1) aRestDayWriteIsRefusedByTheUseCasePath`，`Tests run: 49, Failures: 1`，exit **1**。休息日写入只有 use case 会拒绝，fake 仓库会照写——这正是首轮缺失的鉴别力 |
| 探针 | 临时加测试：NUMERIC 习惯 `toggle()` 后断言仓库 `actualValue == 1.0` | **现在失败**：`1) probeNumericTapWritesOne`，`java.lang.NullPointerException`（根本没有日志行），`Tests run: 50, Failures: 1`，exit **1**。证实数值习惯不再被写入 1.0 |
| 回归 | 全部还原后 | `OK (49 tests)`、exit 0、`git status --short` 空 |

- 首轮已实测的两条（`toggle` 永远 1.0 → 再切换清零测试变红；`describe()` 忽略 schedule → 休息日测试变红）对应代码路径本轮未变，结论继续成立。
- 新增 `togglingANumericHabitWritesNothing` 同时断言无日志行与提示文案，比只断言文案更强。

## 5. 文档与分支

**裁决：不通过（计数未随新增的 2 项测试更新，共 4 处）。**

已同步、核对无误：
- `TEST_REPORT.md` 的 `Latest Run`：分支 `feat/today-presenter`、单测 **49** 项、治理 21 项 0 警告、DAO 21 项、构建 20–28s ✓。
- `TEST_REPORT.md` 的 `Known Test Gaps` 已新增 UI 条目：状态机 JVM 覆盖、Compose 渲染/交互仅编译未验证 ✓（覆盖数仍写 7，见下）。
- `LAST_HANDOFF.md` 已整体改写：`Gate 3` / `TASK-011` / `feat/today-presenter`、5 步 verify-all、四组变异（含未被抓住的那条）、`Not Verified` 写明 UI 与 Room 运行时未验证、`Next Safe Action` 指向复审 ✓。
- `PROJECT_STATUS.md` 的 `Gate 3` 与 UI 未验证表述 ✓。
- `TASK-011` §6 增补第 7 条验收、§9 记录首轮 `REJECT` 的修复过程 ✓。

不符合项（阻塞，均为数字未更新）：
1. `README.md:15` 仍写「最近测试 PASS — **47** 项单测」，实际 49 项——属于 `REVIEW_CHECKLIST.md` §4 的过期断言。
2. `TASK-011` §10 的 `verify-all` 证据行仍写「单测 **47** 项」，而同一张表上一行已写 49 项，自相矛盾。
3. `TASK-011` §11 仍写「相关测试实际运行（**47** 项）」。
4. `TEST_REPORT.md` 的 `Known Test Gaps` UI 条目写「Today 页的状态机有 **7** 项 JVM 覆盖」，实际 `TodayPresenterTest` 已有 9 项（新增 2 项）。

## 6. 结论

- 结论：`REJECT`
- 判定说明：首轮五项阻塞中，代码与测试四项已实质闭合（数值点击不再写值、拒绝测试有了鉴别力、Latest Run 与交接已重写、Known Gaps 有了 UI 条目），架构与范围也保持干净；**唯一剩余的是 4 处测试计数仍停留在 47/7**。都属于一行数字的修正，不涉及逻辑。
- 证据（本轮实际命令与退出码）：
  - `pwsh -NoProfile -File tools/verify-all.ps1` → `RESULT: VERIFY ALL PASS`，exit **0**（治理 21/21、单测 49 项、DAO 21 项、构建 20s、空白 0s，5 步）
  - `pwsh -NoProfile -File tools/check-governance.ps1` → `SUMMARY: 21 checks, 21 passed, 0 warnings, 0 failed`，exit **0**
  - `git diff --check` → exit **0**
  - M-b（绕过 use case）→ `Failures: 1`、`aRestDayWriteIsRefusedByTheUseCasePath`，exit **1**（首轮为 `OK (47 tests)`）
  - 探针（NUMERIC toggle 写 1.0）→ `Failures: 1`、`NullPointerException`，exit **1**（首轮为通过）
  - 还原后 `OK (49 tests)`、`git status --short` 空
- 需修复项：
  1. `README.md:15`：47 → 49。
  2. `TASK-011` §10 的 verify-all 行：单测 47 项 → 49 项。
  3. `TASK-011` §11：（47 项）→ 49 项。
  4. `TEST_REPORT.md` 的 `Known Test Gaps`：Today 状态机覆盖 7 项 → 9 项。

### 非阻塞建议

1. `TASK-011` §10 的构建耗时写 28s，我本轮实测 19–21s（冷热构建差异）；建议写成区间，与 `TEST_REPORT.md` 的「约 20–28s」一致。
2. `close-task` 生成的「最近测试记录：SUCCESSFUL（28s）」取的仍是构建行而非测试行，措辞误导（历史遗留，与本任务无关）。
3. `TodayViewModel` 的 `onCleared` 与工厂仍无测试（薄包装可接受）；一旦取消 scope 的行为被删，测试不会变红。
4. `isLoading == true` 的初始态与快速连点竞态仍无覆盖，可在后续 UI 任务一并处理。
