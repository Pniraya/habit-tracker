# LAST_HANDOFF.md

> 每次 AI 会话结束前更新。记录事实，不写愿望或推测。

## Snapshot
- 日期时间（含时区）：`2026-11-06`（Asia/Shanghai）
- 当前阶段：`Gate 3`（出口 = `TASK-026` 完成 + `R1` 窄口径评审；评审仍未做）
- 当前任务：`TASK-037`（`IMPLEMENTED`：自定义习惯图标——相册自选，161 项单测 + 真机走通）；`TASK-026/027/028/029/031/032/033/034/035/036/038` 均 `IMPLEMENTED` 待批量评审
- 当前 Git 分支：`main`（本地，无远程）；最近 commit 以 `git log -1 --oneline` 为准
- **发布状态**：MIT（`LICENSE`）；未发布 WIP；评估见 `project/reports/GITHUB-READINESS-2026-11-06.md`，操作指南存档 `project/reports/GITHUB-PUBLISH-GUIDE-2026-11-06.md`
- **提交身份**：仓库局部 `user.name=xuy65787`、`user.email=xuy65787@gmail.com`；新增 `.mailmap` 把历史 `Habit Tracker Baseline <baseline@localhost>` 映射到同一身份（未改写历史，文档引用的 commit hash 保持有效）
- **设备**：iQOO Neo7 SE 在线，数据完好（3 条原习惯 + 1 条自定义图标测试习惯 `Cu's'to'mi'con`）；按用户要求跳过 04:00 真机跨日验证（默认成功）

## Completed This Session
- `TASK-038` 收尾：MIT `LICENSE`、`.mailmap`、Git 身份改为所有者邮箱。
- `TASK-037`（本轮，相册自选方案）：
  - `HabitVisuals`：`CUSTOM_PREFIX="custom:"`、`isCustom`、`customFileName`（拒绝空名、`/`、`\\`、`..`），`iconKey` 保留自定义值
  - 新增 `core/storage/CustomIcons`：系统 Photo Picker → 解码 → 中心裁方 → 256px PNG → `filesDir/icons/<uuid>.png` → 返回 `custom:<file>`；无存储权限、无新依赖
  - `HabitIcon`：自定义值圆形渲染（不 tint），文件丢失回退嫩芽
  - 创建页图标行第 7 格「+」→ Photo Picker；选中后预览 + 描边
  - 单测 +2（自定义值保留、路径逃逸拒绝）
- 用户提供素材包的 B 方案仍未做（需要文件与「单色线条 vs 彩色原图」决定）

## Files Changed
- 新增：`app/src/main/java/com/habittracker/core/storage/CustomIcons.kt`、`.mailmap`
- 修改：`HabitVisuals.kt`、`Ui.kt`、`CreateHabitScreen.kt`、`HabitVisualsTest.kt`、`project/tasks/TASK-037.md`、`PROJECT_STATUS.md`、`FEATURE_MATRIX.md`、`TEST_REPORT.md`、`README.md`、本文件

## Verification Actually Run
| 命令 | 结果 | 备注 |
|---|---|---|
| `gradlew.bat compileDebugKotlin` | **BUILD SUCCESSFUL** | 首次 `asImageBitmap` 报 unresolved，改为扩展调用后 5s |
| `tools/run-unit-tests.ps1` | **OK (161 tests)** | 159 → 161：`aCustomIconKeepsItsFileNameAndIsNotReplacedByTheSprout`、`aMalformedCustomIconCannotEscapeTheIconsDirectory` |
| 真机：Photo Picker → 预览 → 保存 → 今日页 | **PASS** | 图标格显示所选图并描边；保存后第 4 行圆形自定义图 |
| 真机数据库/文件核对 | **PASS** | `habits.icon='custom:952fcb26-c4d5-4258-9fc0-43bbb32887e5.png'`；`files/icons/` 下 115,959 B |
| `tools/verify-all.ps1` | **VERIFY ALL PASS** | 治理 22/22、单测 161、DAO 38、构建、`git diff --check` |

## Not Verified / Known Gaps
- **图标未做自适应/单色主题图标**：彩色位图直接作 legacy 图标，桌面缩放清晰度与白边表现待观察
- **CI 未在 GitHub 实跑**（无远程）
- **`assembleRelease`/lint 未运行**
- **10 个 `IMPLEMENTED` 评审债**（`027/028/029/031/032/033/034/035/036/038` + `037`），必须由新上下文做 `R1`
- **04:00 跨日**：用户跳过真机验证，仅 `DayCutoffTest` 5 项
- **自定义图标未覆盖**：删除习惯不清理孤儿文件、编辑旧习惯不能换图标、备份导出不含图片文件、图片损坏时只由单测兜底（未真机注入坏文件）
- **adb `input text` 在该机输入法下会插入多余单引号**（`CustomIcon` → `Cu's'to'mi'con`），非产品缺陷

## Lessons Worth Keeping
- **扩展函数不能用包名当普通函数调**：`androidx.compose.ui.graphics.asImageBitmap(bitmap)` 编译失败，必须 import 后 `bitmap.asImageBitmap()`
- **Photo Picker 无需权限**：`PickVisualMedia` 返回一次性 content URI，复制进 `filesDir` 后即与相册解耦
- **图标先看 `aapt2 dump badging`**：资源放进 `res/mipmap-*` 不等于 APK 用上了
- **改历史作者用 `.mailmap`**：改写历史会让文档里的 commit hash 失效，映射即可让 GitHub 归因
- **截图要裁状态栏**：时间与通知会泄露个人信息，公开仓库尤其
