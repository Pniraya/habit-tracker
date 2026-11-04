# DEVELOPMENT.md

> Habit Tracker V1 开发规范与验收清单\
> 开发前先阅读 `ARCHITECTURE.md` 和
> `DATABASE.md`。本文件规定实施顺序、质量门槛与 AI 编码协作方式。

## 1. 开发目标

交付一款可在 Android 15 真机稳定运行的本地习惯追踪应用。V1
的完成标准不是"页面能显示"，而是数据、日期规则、提醒生命周期和统计结果均可验证。

## 2. 环境与项目初始化

### 2.1 基线

-   Android Studio 稳定版。
-   JDK 17。
-   Kotlin。
-   compileSdk 35、targetSdk 35、minSdk 26。
-   单模块 Gradle 项目：`:app`。
-   Jetpack Compose + Material 3。
-   Room、DataStore、ViewModel、Navigation Compose、Coroutines/Flow。
-   WorkManager 仅在确有可延迟后台任务时加入。
-   版本由 Gradle Version Catalog（`libs.versions.toml`）集中管理。

### 2.2 初始化验收

-   项目可在 Android Studio 同步 Gradle。
-   Debug APK 可构建并安装到 Android 15 设备。
-   Java/Kotlin JVM target 统一为 17。
-   Room schema export 已开启并纳入版本控制。
-   Manifest 权限仅包含 V1 必要权限；Normal 提醒不申请精确闹钟权限。
-   不加入云服务 SDK、账号 SDK、广告 SDK 或无需求的依赖。

## 3. 实施阶段

### Phase 0 --- 工程骨架

任务： - 创建单模块项目。 - 配置 Compose、Material
3、Navigation、ViewModel、Coroutines。 - 建立 package 目录和主题
token。 - 建立基础导航壳：今日、统计、我的。

验收： - Debug 构建成功。 - 三个主页面可导航。 -
浅色主题使用概念稿颜色。 - 没有深色模式切换、小组件代码或后台常驻
Service。

### Phase 1 --- 数据层

任务： - 建立四张 Entity、DAO、Room Database。 - 编写 TypeConverter。 -
建立 Repository 接口与实现。 - 建立 DataStore 设置读写。 - 编写
Migration 基础设施。

验收： - 创建、查询、更新、归档习惯成功。 - 应用重启后数据仍存在。 -
唯一索引与外键行为正确。 - Room schema JSON 纳入版本控制。 - DAO
测试覆盖唯一约束和事务。

### Phase 2 --- 计划与记录业务

任务： - 实现 DAILY、WEEKDAYS、CUSTOM。 - 实现版本化计划更新。 - 实现
BOOLEAN 与 NUMERIC 日汇总。 - 实现未记录、进行中、已完成状态。 -
实现撤销打卡、数值增加和设置数值。

验收： - 周一至周五掩码为 31。 - 周一、三、五掩码为 21。 -
非计划日不可被误算为应执行日。 - 5/8 显示进行中，8/8 显示完成。 -
改目标不改变历史日志的 target_snapshot。 -
计划变更不修改过去的有效计划版本。 - 并发数值增量不会丢失更新。

### Phase 3 --- 今日与习惯管理 UI

任务： - 今日完成数与鼓励文案。 - 习惯卡片与快速打卡。 -
创建、编辑、归档习惯。 - 布尔型和数值型输入界面。 - 习惯详情页。

验收： - 空状态、加载状态、错误状态完整。 - 打卡后页面由 Room Flow
更新，不依赖手动刷新。 - 输入校验清晰。 -
触觉反馈与回弹动画轻量，不阻塞连续操作。 -
全部完成动画不会影响可访问性或性能。

### Phase 4 --- 连续天数与统计

任务： - 实现纯函数 `isScheduledDay`。 - 实现当前连续和最长连续。 -
实现周/月/年完成率。 - 实现月度日历热力图和本周趋势。 - 实现习惯详情的近
5 周热力图与累计统计。

验收： - 连续天数按计划执行日，而非自然日。 -
非计划日跳过；到期未完成计划日打断连续。 -
今天尚未结束且未完成时，不提前判失败。 - 补录历史记录后统计正确重算。 -
完成率分母仅含到期计划执行日。 - 分母为 0 时显示"---"。 -
归档后的日期不继续产生计划次数。

### Phase 5 --- 多次普通提醒

任务： - 实现 `ReminderScheduler`。 - 实现 `ReminderReceiver`。 -
实现通知渠道和通知权限请求。 - 每条提醒使用稳定唯一 PendingIntent
标识。 - 实现重启、时区变化、计划变更、归档后的重排。 -
完成打卡后取消当天剩余提醒。

验收： - 同一习惯可创建多条不同时间提醒。 - 不同提醒不会互相覆盖。 -
已完成习惯不再收到当天后续提醒。 - 归档、禁用或删除提醒后不再触发。 -
重启后从数据库重建未来闹钟。 - Normal 模式不调用精确闹钟
API、不申请精确闹钟权限。 - Android 13+
通知权限拒绝时应用核心功能仍可用。 -
测试报告明确普通闹钟可能延迟，不承诺精确触发时间。

### Phase 6 --- 本地备份恢复与稳定性

任务： - JSON 导出。 - JSON 全量校验。 - 合并导入与清空后恢复。 - 数据库
transaction。 - 导入成功后重建提醒。

验收： - 导出文件可被重新导入。 - 不支持的 schemaVersion
给出明确错误。 - 非法数据不会写入数据库。 - 导入失败不留下部分记录。 -
冲突不会被静默覆盖。 - 恢复后习惯、历史计划、日志和提醒一致。

### Phase 7 --- 发布前验证

任务： - Android 15 真机完整回归。 -
检查权限、通知、省电、重启、时区变化。 -
检查启动速度、列表滚动、内存和异常日志。 - 输出版本说明与已知限制。

验收： - Release 构建成功。 - 无崩溃级问题。 - 核心数据重启不丢失。 -
离线状态下全部核心功能可用。 - 备份恢复通过。 -
权限拒绝与系统限制有明确降级行为。

## 4. 代码规范

### 4.1 Kotlin

-   使用 Kotlin 惯用写法；避免无必要的 Java 风格样板代码。
-   使用不可变 `val`，仅在必要时使用 `var`。
-   公共 API 显式声明可见性与返回类型。
-   不使用 `!!` 规避空值处理。
-   时间逻辑使用 `java.time`，禁止用字符串截取或手写日期运算。
-   不使用 enum ordinal 持久化。

### 4.2 Compose

-   Composable 保持声明式、可预览、尽量无副作用。
-   页面状态由 ViewModel 提供。
-   使用 `collectAsStateWithLifecycle()` 收集 Flow。
-   用户操作通过回调传递给 ViewModel。
-   不在 Composable 中访问 DAO、AlarmManager 或 NotificationManager。
-   列表使用稳定 key。
-   颜色、间距、圆角、字体统一从 Design System 获取。

### 4.3 ViewModel

-   暴露不可变 UI State。
-   业务操作放在 UseCase/Domain，不把复杂业务塞入 ViewModel。
-   使用 `viewModelScope` 管理协程。
-   对用户可见错误转换为明确 UI 状态。
-   不保存可由数据库重新计算的冗余事实。

### 4.4 Repository 与数据库

-   UI 不直接依赖 DAO。
-   多表修改使用 transaction。
-   数值增量必须原子化。
-   Entity 不直接穿透到 UI。
-   生产构建不使用 destructive migration。
-   所有数据库变更同步更新 schema export 和 Migration 测试。

### 4.5 命名

-   类型与函数：`PascalCase` / `camelCase`。
-   常量：`UPPER_SNAKE_CASE`。
-   数据库表与列：`snake_case`。
-   UseCase 以动作或查询语义命名，例如 `RecordHabitProgressUseCase`。
-   不使用含糊名称，如 `Manager2`、`DataHelper`、`doWork`。

## 5. 测试策略

### 5.1 Domain 单元测试

必须覆盖： - 每日、工作日、自定义星期的计划日判断。 -
星期掩码边界（周一、周日、全周、空掩码）。 - BOOLEAN 0/1。 - NUMERIC
小于目标、等于目标、大于目标。 - 当前连续、最长连续。 - 非计划日跳过。 -
今天未到期。 - 过去计划日缺失日志。 - 补录历史记录。 -
计划变更前后连续计算。 - 周/月/年区间边界。 - 分母为 0 的完成率。

### 5.2 DAO / Repository 集成测试

必须覆盖： - CRUD 与归档。 - `(habit_id, log_date)` 唯一约束。 -
`(habit_id, reminder_time)` 唯一约束。 - 计划版本不重叠校验。 - 多表
transaction 回滚。 - 数值并发增量。 - target_snapshot 历史稳定性。 -
数据库 Migration。

### 5.3 Android 仪器测试 / 真机测试

必须覆盖： - Android 15 通知权限允许与拒绝。 -
多提醒创建、编辑、禁用和删除。 - 打卡完成后抑制当天剩余提醒。 -
设备重启后的提醒恢复。 - 时区和系统时间变化。 - Doze /
省电模式下普通提醒可能延迟。 - 应用强制停止后的行为说明。 - 离线使用。 -
JSON 导入导出。

### 5.4 测试原则

-   日期计算测试使用可注入的 `Clock`，不要依赖真实系统当前时间。
-   测试数据使用固定日期和时区。
-   Domain 算法尽可能保持纯函数。
-   测试名称描述场景与预期结果。
-   修复 bug 时增加能复现问题的回归测试。

## 6. AI 编码助手协作规范

AI 生成代码必须遵守：

1.  编码前先阅读三份规范，不能擅自更改已冻结的产品决策。
2.  每次只处理一个可验收的垂直任务；先说明涉及文件和设计，再实施。
3.  不允许为了"架构完整"提前引入多模块、DI 框架、网络层或服务端。
4.  不允许自行加入小组件、深色模式、账号、云同步或精确闹钟。
5.  不得把业务规则散落在 Composable、Receiver 和 DAO 中；规则应集中在
    Domain。
6.  不得用 WorkManager 冒充精确时间提醒；Normal 提醒采用非精确
    AlarmManager。
7.  不得使用 `fallbackToDestructiveMigration()` 作为生产迁移策略。
8.  不得只提供伪代码或省略关键文件；若任务要求实现，必须给出可编译代码和必要配置。
9.  每次改动后运行相关测试和构建；报告实际执行的命令与结果，不得声称未执行的测试已通过。
10. 若发现规范冲突，先指出冲突并提出最小修订，不得静默选择一种解释。
11. 不引入未说明的依赖；新增依赖必须说明用途、替代方案和维护成本。
12. 不将用户数据、备份内容或设备标识发送到网络。

## 7. Definition of Done

一个任务只有同时满足以下条件才算完成： -
功能符合产品需求与两份设计文档。 -
有必要的输入校验、错误状态和边界处理。 - 业务逻辑有自动化测试。 -
数据持久化与迁移行为符合约定。 - 不破坏离线运行。 -
不引入未批准的功能或依赖。 - Debug 构建通过；涉及发布时 Release
构建通过。 - 变更文档与实际实现一致。 - 明确记录未完成事项和已知限制。

## 8. V1 发布检查表

-   [ ] Android 15 真机安装、启动和导航正常
-   [ ] 创建、编辑、归档习惯
-   [ ] BOOLEAN 打卡与撤销
-   [ ] NUMERIC 累计、设置、撤销
-   [ ] DAILY / WEEKDAYS / CUSTOM 计划
-   [ ] 历史计划版本保留
-   [ ] 当前连续与最长连续正确
-   [ ] 周 / 月 / 年统计正确
-   [ ] 多条 Normal 提醒独立工作
-   [ ] 完成后跳过当天后续提醒
-   [ ] 重启和时区变化后重排提醒
-   [ ] 通知权限拒绝时功能降级合理
-   [ ] 本地 JSON 备份与恢复
-   [ ] 离线可用
-   [ ] 数据库 Migration 测试通过
-   [ ] Release 构建成功
-   [ ] 无小组件、无深色模式、无精确闹钟权限申请
