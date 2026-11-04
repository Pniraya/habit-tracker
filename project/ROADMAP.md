# ROADMAP.md

> 路线图定义交付顺序；每个阶段必须通过验收后才能进入下一阶段。具体工作拆到 `project/tasks/`。

## Gate 0 — 治理与基线确认
- 合并治理文件。
- 审查 `ARCHITECTURE.md`、`DATABASE.md`、`DEVELOPMENT.md` 一致性。
- 明确需求 TBD。
- 建立 Git 仓库和初始检查点。
- Gate：冻结基线无未解决的高风险冲突；治理文件可被 Codex 读取。

## Gate 1 — Android 工程骨架
- 创建单模块 Android 工程。
- 配置 Gradle Wrapper、Version Catalog、Kotlin、Compose、Material 3。
- 建立基础主题、导航壳与目录。
- Gate：Gradle Sync、`assembleDebug` 成功；Android 15 设备可安装启动。

## Gate 2 — 数据层
- 建立 Room Entity、DAO、Database、TypeConverter。
- 建立 Repository 和 DataStore。
- 配置 schema export、Migration 基础设施。
- Gate：DAO/Repository 测试通过；重启后数据持久化；外键/唯一约束正确。

## Gate 3 — Domain 业务规则
- 计划日判断、版本化计划更新。
- 布尔型/数值型日汇总。
- 打卡、撤销、目标快照。
- 连续天数和完成率计算。
- Gate：纯函数单元测试覆盖边界；业务规则不依赖 Android UI。

## Gate 4 — 核心 UI
- 今日页。
- 习惯创建/编辑/归档。
- 习惯详情。
- 统计页与热力图。
- Gate：核心流程可操作；UI 状态由 ViewModel 驱动；真机基础体验验收。

## Gate 5 — 提醒系统
- 通知权限和通知渠道。
- Normal 非精确提醒调度。
- 多提醒独立 PendingIntent。
- 完成后抑制当天后续提醒。
- 重启、时区/时间变化、计划变更、归档后的重排。
- Gate：Android 15 真机测试通过；明确系统延迟与强制停止限制。

## Gate 6 — 备份恢复
- JSON 导出。
- 全量校验。
- 合并导入/清空后恢复。
- 事务回滚和提醒重建。
- Gate：往返导入导出、非法数据、冲突和失败回滚测试通过。

## Gate 7 — 发布候选
- 全量回归、性能与异常日志检查。
- Release 构建。
- 发布说明与已知限制。
- Gate：所有 V1 发布检查项有证据；无未解决的阻断级缺陷。

## 排序原则
- 先实现可测试的 Domain 规则，再接 UI 和系统 API。
- 先建立数据库迁移能力，再开始高风险 schema 演进。
- 不为“架构完整”提前引入多模块、复杂 DI、网络层或 Agent 编排平台。
