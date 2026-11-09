# RELEASE_REPORT.md

> 只记录真实发布证据。没有执行就写 `NOT_RUN`。禁止为让检查表好看而勾选。

## 当前版本

- 版本号：`0.1.0`（`versionCode 1`）
- 版本类型：Release，**已签名**（正式密钥，非 debug key）
- 日期：`2026-11-09`（本地构建）
- commit / tag：`TBD`（首个公开 tag 待用户建仓后打）
- 结果：`PARTIAL` —— 本地 `assembleRelease` 成功并签名，release 包能在模拟器安装；GitHub Release、商店分发与 release 包真机首屏验证尚未执行

## 构建证据

| 命令 | 结果 | 产物 / 摘要 |
|---|---|---|
| `gradlew.bat assembleDebug` | SUCCESSFUL（2026-11-09） | `app-debug.apk`，11,237,263 B（debug 含 Compose 工具链，比 release 大） |
| `gradlew.bat assembleDebug`（历史） | SUCCESSFUL（2026-11-05，36s） | 9.4 MB，`com.habittracker` / minSdk 26 / targetSdk 35 |
| `gradlew.bat assembleRelease` | SUCCESSFUL（2026-11-09） | `app-release.apk`，7.7 MB，RSA-4096 密钥签名，`apksigner verify` 通过，`SHA-256 33AC…CE9E` |
| `gradlew.bat lintDebug` | SUCCESSFUL（2026-11-09） | 0 error / 10 warning（`app/build/reports/lint-results-debug.txt`） |
| `gradlew.bat test` | FAILED（沙箱限制） | 测试 worker 的命名管道被禁；等价证据见下两行 |
| `pwsh tools/run-unit-tests.ps1` | PASS（27 项） | `LogStatusRulesTest`、`ScheduleRulesTest`、`AppRoutesTest` |
| `node tools/verify-schema.mjs` | PASS（40/40） | schema parity + 约束行为 |
| `gradlew.bat lint` | NOT_RUN | 未执行 |

## 真机验证矩阵

| 设备 / 系统 | 安装 | 启动 | 核心流程 | 提醒 | 备份 | 结果 |
|---|---|---|---|---|---|---|
| Android 15（API 35）模拟器（`adapt` AVD，1080×2400 @420dpi） | **PASS**（`adb install app-release.apk` → Success，正式签名） | **NOT_RUN**（系统 UI 自身 ANR，焦点停在 `systemui`，未取到首屏） | NOT_RUN | NOT_RUN | NOT_RUN | **PARTIAL** |
| iQOO Neo7 SE（Android 15，真机） | PASS（debug 包） | PASS | PASS（建习惯/打卡/备份导出导入/详情页） | NOT_RUN（未实现） | PASS（导出 + 导入） | **PARTIAL**（release 包未装机） |

## V1 发布检查表

以 `DEVELOPMENT.md` §8 为准，逐项在此登记证据链接（TASK 文件、测试报告、截图路径）。未通过项必须写在此处，不得隐藏。

- [ ] 全部 17 项检查项有证据
- [ ] 已知限制已写入发布说明

## 已知限制（发布说明素材）

- Normal 非精确提醒：系统省电策略可能造成延迟，不承诺准点。
- 应用被强制停止后无法保证按时提醒。
- V1 不含桌面小组件、深色模式、账号与云同步。

## 回滚方案

- 发布前必须存在可回退的 Git tag 检查点；数据库 schema 变更需附带 Migration 与回滚说明。
