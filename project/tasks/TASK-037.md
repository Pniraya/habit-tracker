# TASK-037: 自定义习惯图标（相册自选）

- 状态：`DONE`（独立 Reviewer `APPROVE`，见 §13）
- 阶段：`Gate 3`
- 优先级：`P1`
- 依赖：`TASK-032`（图标/颜色选择器）、`TASK-038`（应用图标）
- 创建日期：`2026-11-06`
- 完成日期：`2026-11-06`

## 1. Objective
让用户从相册选一张图作为习惯图标，不引入图片加载库、不申请存储权限、不改数据库 schema。

## 2. Context
原实现只有 6 个内置矢量图标，`habits.icon` 存字符串键，未知值回退嫩芽。用户于 2026-11-06 要求「自定义图标」并授权在需要素材时索要文件。相册自选方案不需要用户提供素材，因此先做；「用户素材包」作为后续可选任务（需要另提供 PNG/SVG 与风格决定）。

## 3. Scope
- `HabitVisuals`：`CUSTOM_PREFIX = "custom:"`、`isCustom`、`customFileName`（拒绝空名、`/`、`\\`、`..`，纯 Kotlin，可 JVM 测试）。
- 新增 `core/storage/CustomIcons`：`PickVisualMedia` 返回的 content URI → 解码 → 中心裁方 → 缩到 256px → PNG 存 `filesDir/icons/<uuid>.png` → 返回 `custom:<uuid>.png`；`file()` 解析路径。
- `HabitIcon`：`custom:` 值优先按文件渲染（圆形裁切、不做 tint），文件丢失回退嫩芽。
- 创建页：图标行新增「+」入口 → 系统 Photo Picker；选中后格子内直接预览，并以选中态描边。
- 内置图标扩到 11 个（新增 `sun`/`coffee`/`dumbbell`/`pen`/`music`，与原有 6 个同一套 24dp 单色线条风格），选择器改为两行 6+6，第 12 格为相册入口。
- 单测：`HabitVisualsTest` 新增 3 项（自定义值保留、畸形值不能越出目录、11 个内置图标互不重复）。

## 4. Out of Scope
- 用户素材包（B 方案）：需要用户提供 PNG/SVG 与「单色线条 vs 彩色原图」决定，另立任务。
- 编辑已有习惯的图标（当前编辑弹窗只改名称/目标/单位）。
- 自定义图标的清理：删除习惯不删文件；图标约 100 KB，先按「保留」实现。
- 备份导出包含图片文件：`TASK-030a` 的 JSON 只含 `icon` 字符串。

## 5. Expected Files
- 新增：`core/storage/CustomIcons.kt`、本文件更新
- 修改：`HabitVisuals.kt`、`Ui.kt`（`HabitIcon`）、`CreateHabitScreen.kt`、`HabitVisualsTest.kt`、`PROJECT_STATUS.md`、`README.md`、`TEST_REPORT.md`、`FEATURE_MATRIX.md`、`LAST_HANDOFF.md`

## 6. Acceptance Criteria
- [x] 创建页可选相册图片，选中后图标格显示预览并描边
- [x] 保存后今日页行内显示自定义图（圆形）
- [x] 数据库存 `custom:<uuid>.png`，文件在 `filesDir/icons/`
- [x] 无存储权限；使用系统 Photo Picker
- [x] 畸形/丢失的自定义值回退嫩芽，不崩溃
- [x] 11 个内置单色线条图标，两行网格等宽排列（6 + 6，含相册入口）
- [x] 单测 162 项通过（159 + 3）
- [x] 真机走通「选图 → 保存 → 列表显示」

## 7. Tests
- `tools/run-unit-tests.ps1`：**162 项通过**
- `HabitVisualsTest` 新增：`aCustomIconKeepsItsFileNameAndIsNotReplacedByTheSprout`、`aMalformedCustomIconCannotEscapeTheIconsDirectory`、`theBuiltInPickerOffersElevenDistinctLineIcons`
- 真机（iQOO Neo7 SE）：Photo Picker 选中图 → 图标格显示该图 → 命名保存 → 今日页第 4 行显示圆形自定义图
- 数据库核对：`habits.icon = 'custom:952fcb26-c4d5-4258-9fc0-43bbb32887e5.png'`；`files/icons/` 下 115,959 B 的 PNG

## 8. Risks and Decisions
- **不引入 Coil/Glide**：单张本地文件用 `BitmapFactory` 足够，符合「不擅自加依赖」的约束。
- **256px 中心裁方**：一个 12 MP 照片会占 10 MB+；裁方后再缩到 256px，单图标约 100 KB，行内显示足够。
- **不申请存储权限**：`PickVisualMedia` 只授予选中项的一次性读权限，复制后即与相册解耦。
- **删除习惯不删图**：先避免「删习惯顺带删用户素材」；孤儿文件清理另立任务。
- **安全校验**：`customFileName` 拒绝路径分隔符与 `..`，防止手改数据库把渲染指向 `filesDir` 之外。
- **未做备份**：JSON 导出只含字符串；导入后若图片文件不存在会回退嫩芽（可接受，记录为后续）。

## 9. Implementation Log
- 2026-11-06：先写 `HabitVisuals` 的纯解析与 2 项测试，再写 `CustomIcons` 存储与 `HabitIcon` 渲染，最后接创建页 Photo Picker。
- 2026-11-06：首次编译失败于 `asImageBitmap` 的包函数调用（其实是扩展函数），改为 import + 扩展调用后通过。
- 2026-11-06：真机上 `adb push` 一张图触发 MediaScanner，Photo Picker 可选中；保存后列表显示自定义图。
- 2026-11-06：按用户「单色线条」决定补 5 个内置线条图标（`sun`/`coffee`/`dumbbell`/`pen`/`music`），选择器改两行 6+6；顺手补了表单底部间距（图标多一行后最后一张卡会贴住保存按钮）。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `gradlew.bat compileDebugKotlin` | **BUILD SUCCESSFUL** | 首次 `asImageBitmap` 报 unresolved，修正后 5s |
| `tools/run-unit-tests.ps1` | **OK (162 tests)** | 159 → 162：自定义值保留、路径逃逸拒绝、11 个内置图标去重 |
| 真机：新建页图标网格 | **PASS** | 11 个线条图标 + 相册格，两行等宽；滚动到底后频率卡与保存按钮不再贴住 |
| 真机：Photo Picker → 保存 → 今日页 | **PASS** | 截图 `picked.png`（图标格预览）、`custom_created.png`（行内圆形图） |
| 真机数据库/文件核对 | **PASS** | `icon='custom:952fcb26-...png'`；`files/icons/952fcb26-...png` 115,959 B |
| `tools/verify-all.ps1` | **VERIFY ALL PASS** | 治理 22/22、单测 **162**、DAO 38、构建、`git diff --check` |

## 11. Completion Review
- [x] 验收标准满足
- [x] 测试实际运行
- [x] 独立 Reviewer `APPROVE`（`REVIEW-TASK-037`，第 2 轮，0 阻断）

## 12. Handoff
- 素材包方案（B）需要用户提供文件与风格决定；相册方案已可用。
- 后续可做：编辑已有习惯的图标、删除习惯时清理孤儿文件、备份导出包含图标文件。

## 13. Independent Review
- Reviewer record：[`REVIEW-TASK-037`](../reviews/REVIEW-TASK-037.md)
- 结论：`APPROVE`（第 2 轮独立评审，0 阻断）。评审者未参与实现，逐项复核了 自定义图标（含 EXIF 修复） 的 diff、测试证据与文档同步。

## 14. 评审修复（2026-11-09）

- **EXIF 方向**：`CustomIcons.decodeSquare` 现在用 `android.media.ExifInterface`（框架自带，无新依赖）读 Orientation，旋转 90/180/270 后再居中裁剪；否则相机竖拍会横躺落盘。
- **证据矛盾**：`TASK-037` 里 `verify-all` 的「单测 161」与其余「162」不一致，已统一为 162。
- 未改动：解码仍在主线程（慢 provider 有 ANR 风险）、孤儿文件（删除习惯 / 取消新建 / 重选图）仍未清理，均登记为已知缺口。

<!-- close-task:begin -->
## 状态摘要（自动生成，勿手改）

- 任务：TASK-037
- 状态：`DONE`
- 阶段：Gate 3
- 验收：通过 8 项 / 未通过 0 项
- 最近测试记录：2026-11-09 / **OK (182 tests)**，`verify-all` 全绿
<!-- close-task:end -->
