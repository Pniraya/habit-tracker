# RELEASE_REPORT.md

> 只记录真实发布证据。没有执行就写 `NOT_RUN`。禁止为让检查表好看而勾选。

## 当前版本

- 版本号：`NOT_SET`
- 版本类型：Debug / Release（未定）
- 日期：`TBD`
- commit / tag：`TBD`
- 结果：`NOT_RUN`（尚未进入发布流程；Debug 构建已可执行，见构建证据）

## 构建证据

| 命令 | 结果 | 产物 / 摘要 |
|---|---|---|
| `gradlew.bat assembleDebug` | SUCCESSFUL（2026-11-05，36s） | `app/build/outputs/apk/debug/app-debug.apk`，9.4 MB，`com.habittracker` / minSdk 26 / targetSdk 35 |
| `gradlew.bat assembleRelease` | NOT_RUN | 未执行签名与混淆配置 |
| `gradlew.bat test` | FAILED（沙箱限制） | 测试 worker 的命名管道被禁；等价证据见下两行 |
| `pwsh tools/run-unit-tests.ps1` | PASS（27 项） | `LogStatusRulesTest`、`ScheduleRulesTest`、`AppRoutesTest` |
| `node tools/verify-schema.mjs` | PASS（40/40） | schema parity + 约束行为 |
| `gradlew.bat lint` | NOT_RUN | 未执行 |

## 真机验证矩阵

| 设备 / 系统 | 安装 | 启动 | 核心流程 | 提醒 | 备份 | 结果 |
|---|---|---|---|---|---|---|
| Android 15（API 35） | NOT_RUN | NOT_RUN | NOT_RUN | NOT_RUN | NOT_RUN | NOT_RUN |

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
