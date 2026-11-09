# REVIEW-TASK-037: 独立评审记录

- 审查任务：`TASK-037`（自定义习惯图标 / 相册自选）
- 审查 commit：`341a8995377e7c425af673b6aa73fa61a88c7ef0` + 本轮未提交的工作区改动（截至 2026-11-09，`git diff --numstat`：`CustomIcons.kt` +35/−6、`TASK-037.md` +7/−2）
- 审查范围：任务交付至今的累计代码。交付提交 `bf4145d`（12 文件 +272/−84）与 `78b40b0`（24 文件 +235/−78，含 11 图标/6+6 网格），并核对之后所有触碰图标渲染/存储的改动。
- Reviewer：独立评审 Agent（DeepSeek Harness 子会话，未参与实现）
- 独立性：`INDEPENDENT`
- 日期：`2026-11-09`
- 轮次：第 2 轮。第 1 轮判 `REJECT`，阻塞项为（1）`CustomIcons` 解码不处理 EXIF 方向、（2）任务文件的测试证据 161 vs 162 自相矛盾。本轮复审这两项，结论见 §7。

评审时工作区：HEAD 仍为 `341a899`，修复未提交；无设备、无 Android 运行时，因此 EXIF 路径的结论是**读码 + 主会话构建证据**，不是我实测。按指令**未运行** `gradlew` / `verify-all` / `connectedAndroidTest`；主会话报告的 `assembleDebug` SUCCESSFUL 与 `run-unit-tests` OK (177 tests) 非本轮实测。

## 1. Scope

**裁决：通过。**

- 本轮改动只落在两个阻塞项 + 已知缺口登记上：`CustomIcons.kt` +35/−6（新增 `applyExifRotation` 及调用）、`TASK-037.md` +7/−2（161→162、新增 §14）。无依赖、无权限、无 schema、无 `HabitVisuals` 契约改动。
- `android.media.ExifInterface` 是框架类（API 5 起存在，`ExifInterface(InputStream)` 自 API 24），`app/build.gradle.kts:15` `minSdk = 26`，因此不需要 `androidx.exifinterface`，与「不引入新依赖」一致；`git diff` 也无 `build.gradle.kts`/`libs.versions.toml`。
- 写入路径与文件名生成逻辑未改：仍是 `filesDir/icons` + `UUID.randomUUID()`，第 1 轮确认的路径与逃逸防护继续有效。

## 2. Diff 核对

**裁决：通过（EXIF 旋转的裁剪质量与 recycle 顺序均正确）。**

- `CustomIcons.kt:53-65`：解码后先 `applyExifRotation`，再按**旋转后**的 `upright.width/height` 取 `side` 居中裁方，最后缩到 256px——顺序正确（旋转 → 裁方 → 缩放），裁剪的是用户看到的直立画面，不会把裁切框错位。
- `CustomIcons.kt:68-89`：`ExifInterface(input)` 在 `runCatching` 内读取，异常/无法读取时 `getOrDefault(0)` 退回零旋转（保持旧行为，不崩）；`Matrix.postRotate(90/180/270)` 与 EXIF `ORIENTATION_ROTATE_*` 方向一致（顺时针），`filter = true` 保留插值质量。
- recycle 顺序逐步核对，**每一步只有一张存活位图、没有双重回收**：
  - `:86-88` 旋转产生新位图后回收原图，`it === bitmap` 时不动原图；
  - `:60-61` 裁方后若为新位图则回收 `upright`；`createBitmap` 在「整张图即目标正方形」时可能返回**同一个对象**，这里的 `!==` 判断正好挡住双重回收；
  - `:62-64` 缩到 256 后回收 `cropped`，同样带 `!==` 守卫；
  - `:28-31` 压缩完成后由 `save` 回收最终位图。
  - 失败路径：`save` 外层 `runCatching`（`:24`）会把任何异常（含旋转时 OOM）变成 `null`，此时尚未写文件，不会留下半写入的孤儿文件。
- `TASK-037.md:72` 的「单测 161」改为 **162**，`:94` 的自动摘要也从 161 改为 162，与 `:41`/`:45`/`:68`、`TEST_REPORT.md:117` 全部一致；`:96-100` 新增 §14 记录本轮修复，并把主线程解码与三类孤儿文件登记为已知缺口。
- `git diff --check` exit 0；无冲突标记。

## 3. 架构与数据

**裁决：通过。**

- 规则仍在正确层：路径与文件名净化在 `core/designsystem/HabitVisuals`（纯 Kotlin、可 JVM 测试），编解码/EXIF/IO 在 `core/storage/CustomIcons`（Android 侧），没有把逻辑写进 Composable 或 DAO；`habits.icon` 仍是字符串键，老值 `star` 仍回退嫩芽。
- 外设契约不变：`PickVisualMedia` 授权只在回调内有效，三次 `openInputStream`（`bounds` / 解码 / EXIF）都在回调内完成，复制后与相册解耦；manifest 仍无任何 `uses-permission`。
- 新增的 EXIF 读取不改变落盘格式（PNG，丢弃 EXIF），方向在像素上被真正烘焙进去，渲染侧 `HabitIcon`（`Ui.kt:192-207`）无需感知方向。
- 未处理的边界（记录，不阻塞）：EXIF 的**镜像类**方向 `ORIENTATION_FLIP_HORIZONTAL`(2)、`FLIP_VERTICAL`(4)、`TRANSPOSE`(5)、`TRANSVERSE`(7) 仍落到 `else -> 0`（`CustomIcons.kt:80`），前摄自拍或编辑过的图会保持镜像；普通后摄照片的 1/3/6/8 已覆盖。

## 4. 验证证据

**裁决：通过（第 1 轮两项阻塞均已闭合；EXIF 路径无运行时证据，已如实标注）。**

| # | 验收标准（TASK-037 §6） | 本轮核对 | 结论 |
|---|---|---|---|
| 1 | 创建页可选相册图片，预览并描边 | `CreateHabitScreen.kt:93-99`、`:176-205`、`:355-395` | 通过（读码，第 1 轮已确认，本轮未改） |
| 2 | 保存后今日页行内显示自定义图（圆形） | `TodayScreen.kt:398` → `Ui.kt:177-203` | 通过（读码） |
| 3 | 存 `custom:<uuid>.png`，文件在 `filesDir/icons/` | `CustomIcons.kt:26-32` + `TEST_REPORT.md:112` | 通过 |
| 4 | 无存储权限，用系统 Photo Picker | Manifest 无 `uses-permission`；`CreateHabitScreen.kt:93-94` | 通过 |
| 5 | 畸形/丢失的自定义值回退嫩芽，不崩溃 | `HabitVisuals.kt:51-57`、`Ui.kt:192-207`、`HabitVisualsTest.kt:108-114` | 通过 |
| 6 | 11 个内置单色线条图标，两行 6+6 | `HabitVisuals.kt:29-32`、`CreateHabitScreen.kt:178-204`、`HabitVisualsTest.kt:81` | 通过 |
| 7 | 单测 162 项通过 | `TASK-037.md:41/45/68/72/94` 与 `TEST_REPORT.md:117` 现已全部为 **162**；当前全仓 177 项（`TEST_REPORT.md:9/127`） | **本轮成立** |
| 8 | 真机「选图 → 保存 → 列表显示」 | `TEST_REPORT.md:112/114`、`TASK-037.md:47`（2026-11-06 旧证据）；**EXIF 修复本身没有真机或仪器测试证据** | 旧证据有效；新路径未运行时验证 |

- EXIF 修复可验证性的说明：框架 `ExifInterface` 无法在 JVM 单测里驱动，本轮只跑了构建与既有单测，所以「竖拍照片不再横躺」目前是**代码评审结论**，不是实测结论。可降风险的依据有两条：`minSdk = 26` 保证 API 可用；`runCatching` 兜底保证任何失败都退回「不旋转」的旧行为，不会崩。

## 5. 文档与分支

**裁决：部分通过。**

- 一致：`PROJECT_STATUS.md:71`（待批审清单登记 `IMPLEMENTED` 与孤儿文件缺口）、`FEATURE_MATRIX.md:40`、`README.md:106`、`TASK-037.md:96-100`（§14 修复记录 + 主线程/孤儿文件缺口）。
- 小的形式问题（不阻塞）：① `TASK-037.md:94` 是「自动生成，勿手改」块，本轮是手改成 162 的，规范做法是重跑 `tools/close-task.ps1`，否则下次生成会覆盖回旧数；② `## 14`（`:96`）直接贴在 `<!-- close-task:end -->`（`:95`）后面且没有空行，且排在自动摘要之后，读起来像附录；建议把 §14 移到 §13 之后、或让 `close-task` 重新生成。
- 分支与提交：修复仍在工作区未提交；第 1 轮指出的「`78b40b0` 把 TASK-037 与 TASK-038 混在一个 commit」是既成事实，不要求改写历史，但本轮修复建议单独一个聚焦 commit（`CustomIcons.kt` + `TASK-037.md`）。

## 6. 第 1 轮阻塞项闭合情况

| 第 1 轮阻塞项 | 本轮证据 | 状态 |
|---|---|---|
| `CustomIcons.kt:37-49` 用 `BitmapFactory` 解码、不处理 EXIF 方向 | `CustomIcons.kt:53-55` 新增旋转调用、`:68-89` `applyExifRotation`（`ExifInterface` + `Matrix.postRotate` + `runCatching` 兜底）；旋转在裁方之前，recycle 顺序无重复回收 | **已闭合（读码 + 构建；无真机证据）** |
| `TASK-037.md:72` 的 161 与 `TEST_REPORT.md:117` 的 162 矛盾 | `:72` 改为 162，`:94` 同步为 162，全文件再无 161 | **已闭合** |

第 1 轮的非阻塞建议中，「主线程解码 + 合成失败静默」「孤儿文件三种来源」已被写进 `TASK-037.md:100` §14 作为已知缺口；「解码峰值不受 256px 约束」与「`HabitIcon` 组合期同步解码」本轮未处理，保留为后续建议。

## 7. 结论

- 结论：`APPROVE`
- 证据（本轮实际命令与结果）：
  - `git log -1 --format=%h` → `341a899`；`git diff --numstat` → `CustomIcons.kt` +35/−6、`TASK-037.md` +7/−2（`git diff` 未含 `build.gradle.kts`/`libs.versions.toml`，无新依赖）
  - 读 `CustomIcons.kt:24-89` 逐行核对旋转/裁剪/缩放/recycle；读 `TASK-037.md:41/45/68/72/94/96-100`
  - `git diff --check` → exit **0**
  - 未运行：`gradlew`、`tools/run-unit-tests.ps1`、`tools/verify-all.ps1`、`connectedAndroidTest`（按评审指令交由主会话统一执行）
- 结论说明：第 1 轮两项阻塞都以最小改动闭合——EXIF 方向在裁方前烘焙进位图，失败退回旧行为、不会崩，且没有引入依赖；测试证据 161/162 已统一。安全面（写入固定 `filesDir/icons`、文件名由 `UUID` 生成、`customFileName` 拒绝分隔符与 `..`、缺文件回退嫩芽、零权限）在第 1 轮已逐项确认，本轮改动没有触碰这些路径。

### 需修复项

无（两项阻塞均已闭合）。

### 转 `DONE` 前置条件（非技术阻塞）

1. `project/tasks/TASK-037.md` 用 `tools/close-task.ps1` 重新生成自动摘要（而不是手改），把 §14 挪到 `close-task` 块之前，§13 填本轮 `APPROVE` 与评审文件路径。
2. 转 `DONE` 时同步 `PROJECT_STATUS.md:71` 的待批审清单（`check-governance` 第 18 项双向校验），并把 §14 的两条已知缺口（主线程解码、孤儿文件）保留在清单里，别在转 `DONE` 时丢。
3. 建议补一次真机确认：用一张**竖拍相机原图**（EXIF `Orientation=6`）走「选图 → 保存 → 今日页」，验证图标直立。这是当前唯一没有运行证据的改动。

### 非阻塞建议

1. **镜像方向未处理**：`CustomIcons.kt:76-81` 只识别 3/6/8，`FLIP_HORIZONTAL`(2)/`FLIP_VERTICAL`(4)/`TRANSPOSE`(5)/`TRANSVERSE`(7) 落到 `else -> 0`；前摄自拍或编辑过的图会保持镜像。补 `postScale(-1f, 1f)`/`postScale(1f, -1f)` 并在 §14 记一条，或明确登记该限制。
2. **解码峰值仍不受 256px 约束**（沿用第 1 轮）：`CustomIcons.kt:44-47` 要求两个维度同时 `/(sample*2) >= 256`，极端长宽比会以 `sample=1` 全尺寸解码（复算 `4000×300` ≈ 4.58 MiB、`20000×300` ≈ 22.9 MiB）；加了旋转后峰值再乘约 2。建议按短边或长边夹取。
3. **主线程解码**（已登记为缺口）：`CreateHabitScreen.kt:93-99` 在 ActivityResult 回调里同步做 3 次 `openInputStream` + 解码 + 旋转 + 压缩，云盘 URI 有 ANR 风险，失败还是静默。做的时候建议 `withContext(Dispatchers.IO)` + 失败提示。
4. `HabitIcon`（`Ui.kt:189-196`）在组合期 `remember(icon)` 同步 `decodeFile`，列表滚动每行一次磁盘解码；可加一层内存缓存。
