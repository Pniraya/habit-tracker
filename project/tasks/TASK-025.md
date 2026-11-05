# TASK-025: 真机 instrumented 测试可用性 spike

- 状态：`NOT_STARTED`
- 阶段：`Gate 3`
- 优先级：`P1`
- 依赖：无（但设备必须连接）
- 创建日期：`2026-11-05`
- 完成日期：`TBD`

## 1. Objective
确认 `connectedDebugAndroidTest` 能否在本机 + USB 真机上跑通。若可，Room 映射、迁移、Compose 冒烟测试一次性获得落点；若不可，记录实际报错，改为 `adb shell uiautomator dump` 的脚本断言或明确接受缺口。

## 2. Context
外部评审指出：失败的是 Gradle **单元测试 worker**（命名管道被禁），而 instrumented 测试走设备侧 runner，可能不受影响。这是当前最大的未验证假设，卡住了 UI 与 Room 的自动化。

## 3. Scope
- 加 `androidTestImplementation`（`androidx.test.ext:junit`、`androidx.test:runner`、必要时 compose ui-test），写一个最小测试（如启动 Activity 后断言某文本存在）。
- 在真机上执行 `gradlew.bat connectedDebugAndroidTest`，记录实际结果。
- 按结果更新 `TEST_REPORT` 的「Known Test Gaps」。

## 4. Out of Scope
- 不追求覆盖率；通过后再另立任务写冒烟用例。
- 不改 `verify-all`（设备测试不放进默认门禁）。

## 5. Expected Files
- 修改：`app/build.gradle.kts`、`gradle/libs.versions.toml`
- 新增：`app/src/androidTest/...` 最小用例

## 6. Acceptance Criteria
- [ ] 给出 `connectedDebugAndroidTest` 在本机的真实结果（通过或具体报错原文）
- [ ] 若通过：至少 1 条设备侧用例稳定通过 2 次
- [ ] 若失败：记录报错，并在 `TEST_REPORT` 写明替代方案与缺口

## 7. Tests
- `gradlew.bat connectedDebugAndroidTest`
- `pwsh -NoProfile -File tools/verify-all.ps1`（确认未破坏既有门禁）

## 8. Risks and Decisions
- 新增测试依赖属于 `androidTestImplementation`，不影响运行时包体与离线保证；仍需确认不含网络库。
- 设备测试慢且依赖插线，不放入默认门禁。

## 9. Implementation Log
- 尚未开始（由外部评审 Q9 与 `TASK-024` 评审建议创建）。

## 10. Verification Evidence
| 命令/测试 | 实际结果 | 证据/失败摘要 |
|---|---|---|
| `gradlew.bat connectedDebugAndroidTest` | 未运行（任务未开始） | 开工后补真实报错或结果 |

## 11. Completion Review
- [ ] 验收标准满足
- [ ] 测试实际运行
- [ ] 独立 Reviewer `APPROVE`

## 12. Handoff
- 之后：若通过，用设备侧测试补 Room 映射与 Compose 冒烟；统计页见 `TASK-023`。

## 13. Independent Review
- Reviewer record：待创建
- 结论：`PENDING`
