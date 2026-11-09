# LAST_HANDOFF.md

> 每次 AI 会话结束前更新。记录事实，不写愿望或推测。

## Snapshot
- 日期时间（含时区）：`2026-11-09`（Asia/Shanghai）
- 当前阶段：`Gate 3` 收尾 —— 高风险任务已完成批量评审，发布链路已打通
- 当前任务：`TASK-040`（`IMPLEMENTED`：机型适配，待批量评审）
- 用户决定：备份 = **覆盖**；**不做提醒**；**只考虑手机**；**Spark 即可**；**合并导入不做**；**发布用正式签名**
- 已 `DONE`（有独立 `APPROVE`）：`TASK-026`、`TASK-030`、`TASK-035`、`TASK-037`
- 待批审：`TASK-027/028/029/031/032/033/034/036/038/040` + `TASK-039`（第 4 轮复审中，前 3 轮已把代码与文档阻断全部闭合）；`TASK-003/004/005` 为 `PARTIAL` 在册例外

## 当前可交付状态
- 功能：今日/新建/统计/我的/详情五页；04:00 日切；自定义图标（含 EXIF 方向）；JSON 导出 + 覆盖式导入（解析期全量校验）；单色线条内置图标 + 自适应启动图标。
- 质量门：`tools/verify-all.ps1` 全绿（治理 22、单测 **182**、DAO 45、schema 40、构建、`git diff --check`）。
- 适配：`tools/probe-adaptation.ps1` 覆盖 5 档手机配置 × 5 页；模拟器环境 `.gradle\emu-sdk`（AEHD）。
- 发布：`assembleRelease` 成功并已用**正式密钥**签名，`apksigner verify` 通过；`lintDebug` 0 error / 10 warning。

## 签名（重要）
- 密钥：`D:\HabitTracker-release\release.jks`（PKCS12，RSA 4096，30 年）+ 仓库根 `keystore.properties`（均已被 `.gitignore` 排除）。
- 证书 `SHA-256`：`33acf8b78a895e59452275e3726905868a45b1596e14478bd2ec3f38c908ce9e`。
- **丢失无法升级已发布版本**；需离线备份。换新密钥必须换 `applicationId`。
- 无 `keystore.properties` 时仍构建出 `app-release-unsigned.apk`。

## Not Verified / Known Gaps
- **OEM 真机（小米/OPPO）未验证**：用户决定不再投入；`tools/testlab-run.ps1` 从未执行。
- release 包只在模拟器验证到「安装成功」；首屏未取到（模拟器 systemui 自身 ANR）。真机 release 未装。
- 2.0× 字体下详情页探测跳过；横屏未验证。
- 提醒功能未做（`F-012/F-013`）；合并导入已决定不做。
- EXIF 镜像类方向（FLIP/TRANSPOSE）未处理；自定义图标解码仍在主线程；孤儿图标文件不清理。
- CI（`.github`）从未在 GitHub 实跑。

## Next Safe Action
1. 等 `TASK-039` 第 4 轮 `APPROVE` → 转 `DONE`、移出待批审清单、跑一次 `verify-all`、提交。
2. 用户建 GitHub 仓库后：给出 `git remote add` / `push` 命令；先 Private，再用 `app-release.apk` 建首个 Release（tag `v0.1.0`）。
3. 剩余可做项优先级：提醒（`N5`）> CI 实跑 > 图标素材包（等用户提供文件）。
