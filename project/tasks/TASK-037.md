# TASK-037: 自定义习惯图标（含用户提供素材）

- 状态：`NOT_STARTED`（等待用户提供图标文件与风格选择）
- 阶段：`Gate 3`
- 优先级：`P1`
- 依赖：`TASK-032`（图标/颜色选择器）
- 创建日期：`2026-11-06`
- 完成日期：—

## 1. Objective
让用户使用**自己的图标**，而不是固定 6 个内置图标：既能从相册选一张图，也能使用用户提供的整套图标素材。

## 2. Context
当前 `habits.icon` 存字符串键（`sprout/water/book/breathe/moon/heart`，旧数据可能为 `star`），`HabitVisuals.drawableFor` 映射到 6 个内置矢量，未知键回退嫩芽。创建页/编辑页只有这 6 个选择。用户于 2026-11-06 明确要求「自定义图标」，并授权在需要素材时主动索要文件。

## 3. Scope（建议拆成两步）
- **A. 相册自选图标**（不需要用户文件）：`ActivityResultContracts.PickVisualMedia` → 复制到 `filesDir/icons/<uuid>.<ext>` → `habit.icon = "custom:<fileName>"`；渲染时用 `BitmapFactory` 解码（不引入 Coil 等新依赖）；删除/归档不删文件，删除习惯时清理由单独任务处理。
- **B. 用户素材包**（需要文件）：用户提供的图标放进 `res/drawable-nodpi/`（PNG）或 `res/drawable/`（SVG 转 vector），在 `HabitVisuals.ICONS` 注册名字与资源，创建页选择器自动出现。
- 选择器：内置 6 个 + 「自定义」入口；详情/编辑保持只读展示。

## 4. Out of Scope
- 图标包在线下载（V1 无网络）。
- 用户自定义颜色的任意取色器（当前 3 个预设色）。
- 图标文件的云同步/备份（备份导出是否包含自定义图标另定）。

## 5. 需要用户提供的文件（用户已授权「到时候主动索要」）
请提供以下任一组即可开工：
1. **图标素材**：PNG（透明背景，建议 96×96 或 192×192）或 SVG；数量与命名；是否希望它们替换/补充现有 6 个。
2. **风格决定**：单色线条（跟随习惯颜色 tint，与现有图标一致）还是彩色原图（不做 tint）。
3. **是否需要相册自选**（A）：若只要素材包，B 单独实现即可，无存储权限、无文件拷贝。

## 6. Expected Files（预估）
- 新增：`feature/create/CustomIconPicker.kt`（或复用 `CreateHabitScreen` 的对话框）、`core/designsystem/CustomIcon.kt`（文件路径 → ImageBitmap）、`test/.../CustomIconNamingTest.kt`
- 修改：`HabitVisuals.kt`（`custom:` 前缀解析与回退）、`CreateHabitPresenter.kt`/`CreateHabitForm.kt`、`CreateHabitScreen.kt`、`HabitIconTile`/`HabitIcon` 渲染、`README`（素材说明）
- 可能涉及：`DATABASE.md` 对 `habits.icon` 的取值范围说明（**不需要 schema 变更**，仍是 TEXT）

## 7. Acceptance Criteria（草案）
- [ ] 创建页可选自定义图标，选择后列表/详情/今日页一致显示
- [ ] 未知或丢失的文件回退到嫩芽，不崩溃
- [ ] 相册选择不申请任何存储权限（系统 Photo Picker）
- [ ] `habits.icon` 仍为字符串，schema 不变；旧数据（`star`）继续回退
- [ ] 单测覆盖名称解析与回退；真机验证选图、重启、归档后展示
- [ ] 素材包方案下，图标资源可被 `verify-all` 的构建与治理检查通过

## 8. Risks and Decisions
- **不引入图片加载库**：单张本地文件用 `BitmapFactory` 足够；Coil 会新增依赖，违反 `AGENTS.md` 的依赖约束。
- **图标文件生命周期**：先按「不删除」实现（文件很小），删除清理另立任务；否则删除习惯会顺带删用户素材，风险更高。
- **备份导出**：`TASK-030a` 的 JSON 只含 `icon` 字符串；自定义图标的文件本身默认不进 JSON（体积与格式问题），需要单独决定是否打包。
- **尺寸与 tint**：彩色原图不做 tint；单色线条按现有规则 tint，需要用户先定风格。

## 9. Implementation Log
- 2026-11-06：建立任务，等待用户提供素材与风格决定。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| — | `NOT_RUN` | 任务未开始，取决于用户提供的文件 |

## 11. Completion Review
- [ ] 验收标准满足
- [ ] 测试实际运行
- [ ] 独立 Reviewer `APPROVE`

## 12. Handoff
- 用户提供文件后：先做 A 或 B（按用户选择），再补单测与真机验证。
- 素材版权：用户提供的图标需确认可再分发（公开仓库会包含它们）。

## 13. Independent Review
- Reviewer record：待创建
- 结论：`NOT_STARTED`
