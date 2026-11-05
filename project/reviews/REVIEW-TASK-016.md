# REVIEW-TASK-016: 独立评审记录

- 审查任务：`TASK-016`
- 审查 commit：`bf8634be89f5ce3b70dcf8d3e5639f2e7367a2be`
- 审查范围：`git diff main...HEAD`（`79ab62e`..`bf8634b`，1 个提交，9 文件 +71/−47）
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-05`
- 轮次：第 1 轮

评审时工作区：`git status --short` 为空（开始前）。本轮为只读核对 + 门禁复跑 + 2 组变异 + 设备状态抽查；改动已还原，结束时工作区仅剩本评审文件。

## 1. Scope

**裁决：通过。**

- 改动与 §3/§5 一致：`TodayScreen.kt`（`FabMargin/FabSize/FabSafetyGap/ListBottomPadding`、FAB 边距复用、`KeyboardType.Decimal`）、`RoomHabitRepository.kt`（删死 import）、`CreateAndArchiveHabitUseCaseTest.kt`（改名 + 注释）、`TASK-014.md` §8（触发器边界）与状态文档。
- 无 `build.gradle.kts` / schema / 权限改动；未触碰统计页、我的页、图标/颜色选择器。
- 第 5 项（真机小数输入）**未做**，任务因此保持 `PARTIAL`——这与 §6/§8/§10/§11 的写法一致，不是漏写。

## 2. Diff 核对

**裁决：通过。**

- `git diff --check`：**exit 0**；无冲突标记。
- 常量替换是保值的：56 + 20 + 12 = 88，与原 `88.dp` 相等，视觉不变；FAB 的 `padding(20.dp)` 也改为复用 `FabMargin`，避免两处各写一个 20。
- 键盘只改了 `keyboardType`；负值守卫那一行（`toDoubleOrNull()?.takeIf { it >= 0.0 }`）与 `enabled = parsed != null` 未被触碰。
- `RoomHabitRepository` 只删 1 行 import，无其它改动；测试改名同时改了注释，明确「断言的是测试替身」。

## 3. 架构与数据

**裁决：通过。**

- 常量属于纯渲染层，未引入状态或依赖；无分层变化，无数据约束变化。
- `TASK-014` §8 的补充与事实相符：触发器只保证合法、不 trim；`app/src/main` 中也没有绕过 UseCase 的 `createHabit` 调用（`TASK-014` 评审已核对，本轮复核 data 层已无任何 `HabitRules` 引用）。

## 4. 验证证据

**裁决：通过（含 1 项未验证，如实记录）。**

| 命令 / 变异 | 实际结果 | 退出码 |
|---|---|---|
| `pwsh -NoProfile -File tools/verify-all.ps1` | 治理 21/21（0 警告）、单测 **OK (63 tests)**、DAO SQL 21 项 0 失败、`assembleDebug` BUILD SUCCESSFUL 27-28s、空白 0 项 | **0** |
| `pwsh -NoProfile -File tools/check-governance.ps1`（单独复跑） | `SUMMARY: 21 checks, 21 passed, 0 warnings, 0 failed` | 0 |
| `git diff --check` | `no whitespace errors` | 0 |
| 变异 M-1：`FabSafetyGap = 12.dp` → `0.dp` | `OK (63 tests)`，**没有任何测试变红** | 0 |
| 变异 M-2：`KeyboardType.Decimal` → `Number` | `OK (63 tests)`，**没有任何测试变红** | 0 |
| 还原后 `tools/run-unit-tests.ps1` | `OK (63 tests)` / `RESULT: UNIT TESTS PASS` | 0 |

两个变异都通过，正好印证 §8 的自我说明：几何常量与键盘类型都没有自动化测试锁定；M-1 说明 `FabSafetyGap` 目前只是「可读的推导」，不是「被验证的约束」。

**真机状态抽查**：`adb devices` 显示 `10CDAK0B71005KU device`，`dumpsys power` 为 `mWakefulness=Awake`。也就是说 §6/§9 记录的「设备休眠锁屏、唤醒失败」在评审时**已不成立**（设备后来醒了），这项验收现在具备补验条件；评审不代做该操作，但可以确认「未验证」不是永久环境限制。

未验证项：真机输入 `0.5` 并落库（`TASK-016` §6 最后一项，按事实保持未勾选）；Compose 渲染与键盘布局无自动化。

## 5. 文档与分支

**裁决：通过（附 2 条非阻塞修正）。**

- 诚实性核对通过：`TASK-016` §6 第 5 项是 `[ ]` 且写明「**未完成**」与失败原因；§8 明说「小数能力仍是未验证的」「键盘给了小数点与字段接受小数点是两件事」；§10 真机行为 `未运行`；§11 写「4/5 → 保持 PARTIAL」；自动摘要为「通过 4 项 / 未通过 1 项」；`README.md:12`、`PROJECT_STATUS.md:8`、`LAST_HANDOFF.md:8` 都标注 `PARTIAL` + 真机小数输入未验证。**全文没有任何一处声称「小数输入已验证/可用」**（对 `0.5`、`小数`、`Decimal` 的全仓检索只命中上述未验证表述与代码本身）。
- 计数一致：`README.md:15`、`TEST_REPORT.md`、`PROJECT_STATUS.md:13`、`LAST_HANDOFF.md` 均为 63。
- 分支 `feat/ui-polish`、单提交、未推送，符合 `AGENTS.md` §7；工作区干净。
- 非阻塞：`LAST_HANDOFF.md:42` 仍写「设备当前休眠锁屏」，评审时已是 `Awake`，属时间点陈述，补验时应一并更新；`README.md:63`、`PROJECT_STATUS.md:42`、`PROJECT_STATUS.md:54` 仍写「`adb devices` 为空 / 无真机」，与 `TASK-012` §14、`TASK-013`、`DEVICE_DUMP` 的真机记录矛盾，是更早阶段遗留的陈述，建议顺手改成「沙箱无模拟器；真机需手动连接」。

## 6. 结论

- 结论：`APPROVE`
- 证据：`verify-all` exit 0（63 项、治理 21/21、DAO 21 项、构建 27-28s、空白 0）；两个变异都通过，证明本次改动无测试锁定（与 §8 一致）；§6/§8/§10/§11 对「未验证」的表述一致，无夸大；工作区干净。
- 关于问题 1（常量推导是否只是装饰）：**一半是装饰**。它把「88 从哪来」变成了可读的加法，这是真的改进；但 `FabSize = 56.dp` 是从 Material3 规格抄来的假设，不是测量值——Material 改尺寸时它不会跟着变，也没有测试会失败（M-1 已证明几何无锁）。§8 把「无测试锁定、改 FAB 类型需人工检查」写清楚了，所以算**诚实的文档化假设**，不算隐瞒。若要更强，只能在运行时用 `onGloballyPositioned` 量 FAB 实际高度再动态算 padding，代价是状态与重组，对这个页面不值得；建议在注释里补一句「56dp 对应 Material3 当前规格，升级 Material 时需复核」。
- 关于问题 2（Decimal 是否改变负值守卫）：**没有改变**。守卫仍是 `toDoubleOrNull()?.takeIf { it >= 0.0 }`，`enabled = parsed != null`；`KeyboardType.Decimal` 只是让键盘出现小数点，通常不含负号，粘贴进来的负数仍被 `takeIf` 拦下。新增的真实风险是**小数分隔符的 locale 差异**：部分输入法在 `Decimal` 下会打出 `,`，而 Kotlin 的 `toDoubleOrNull()` 按 `.` 解析，`0,5` 会解析失败、确认按钮保持禁用。若目标用户可能用逗号小数，建议在 `onValueChange` 里把 `,` 归一化为 `.`（或明确只支持 `.`）。
- 关于问题 3（其它魔数/死 import）：`feature/today` 里仍有 `20.dp/4.dp/8.dp/12.dp/10.dp/16.dp`，但它们是常规间距，不是「有隐含几何关系却互不引用」的那一类，暂不需要常量化；死 import 方面，`TodayScreen` 的 import 全部在用（启发式把 `getValue/setValue` 标成未使用是误报，属性委托不出现标识符），`data` 层已无 `HabitRules` 引用。
- 关于问题 4（`PARTIAL` 是否正确）：**正确**。`AGENTS.md` §5 明确「环境不支持时标记 `BLOCKED`/`PARTIAL`，写明未验证项，不得写测试通过」；这里代码完成、验收 4/5、真机项未做，`PARTIAL` 是唯一诚实的标签。两点后续：① 设备现在已是 `Awake`，补验成本很低，做完可转 `DONE`；② 若急于把本分支合入 `main`，按 §7 必须把 `TASK-016` 登记进 `PROJECT_STATUS.md` 的「在册例外」（目前只有 `TASK-003/004/005`），否则就是未登记的部分完成进 `main`。
- 非阻塞建议：
  1. 注释补「56dp 对应 Material3 当前规格，升级 Material 时复核」；或在 `TASK-017` 里评估运行时测量。
  2. 考虑把 `,` 归一化为 `.`，或注明只支持点号小数。
  3. 补验真机 `0.5` 后同步更新 `TASK-016` §6/§10/§11、`LAST_HANDOFF` 的「设备休眠」表述。
  4. 修正 `README.md:63`、`PROJECT_STATUS.md:42/54` 的「无真机」旧陈述。
