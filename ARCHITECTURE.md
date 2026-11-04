# ARCHITECTURE.md

> Habit Tracker V1 --- Android 15 / Kotlin / Jetpack Compose\
> 文档状态：V1 基线；本文件与 `DATABASE.md`、`DEVELOPMENT.md` 配套使用。

## 1. 产品边界

### 1.1 V1 目标

构建一款运行于单台 Android
设备的离线习惯追踪器。用户可以创建和管理习惯、按计划打卡、查看连续记录与统计，并为同一习惯设置多条每日提醒。

### 1.2 明确不做

-   不提供账号、登录、云端服务或云同步。
-   不开发桌面小组件。
-   不开发深色模式。
-   不运行常驻前台服务。
-   V1 不记录每一次操作的独立时间轴；数值记录按日汇总。
-   不引入 Spring Boot、微服务或独立后端。

### 1.3 已确定的产品规则

-   追踪类型：布尔型与数值型混合。
-   计划：每日、工作日、自定义星期。
-   连续天数：按计划执行日计算，不按连续自然日计算。
-   提醒：Normal 普通精度，允许系统延迟，优先省电；不申请精确闹钟权限。
-   提醒可为每个习惯配置多条每日时间。
-   数据完全保存在本机。

## 2. 技术基线

  项目           选型
  -------------- ------------------------------------------
  语言           Kotlin
  UI             Jetpack Compose
  组件规范       Material 3 + 项目自定义 Design System
  持久化         Room / SQLite
  轻量配置       DataStore Preferences
  异步           Kotlin Coroutines + Flow
  页面状态       ViewModel + StateFlow
  导航           Navigation Compose
  提醒           AlarmManager 非精确闹钟
  通知           NotificationManager
  延迟后台任务   WorkManager（仅在确有延迟任务时使用）
  编译基线       compileSdk 35 / targetSdk 35 / minSdk 26
  JVM            Java 17

版本号应由项目初始化时选定的稳定 Android Gradle Plugin、Kotlin、Compose
BOM 和 Room 版本统一管理；不要在不同模块或文件中随意混用版本。V1 使用单
Gradle module（`:app`）。

## 3. 总体架构

采用轻量分层架构。分层用于约束依赖与职责，不以增加抽象层数量为目标。

``` text
Compose UI
   │ 用户操作 / 收集 UI State
   ▼
ViewModel
   │ 调用业务用例
   ▼
Domain（UseCase、规则计算、Repository 接口）
   ▲
   │ Repository 实现
Data（Room DAO、Entity、DataStore）

Android System Integration
AlarmManager / BroadcastReceiver / NotificationManager
   │
   └── 调用 Repository 查询必要数据，再执行提醒规则
```

### 3.1 Presentation

负责： - Compose 页面、可复用 UI 组件和导航。 - 将用户输入转换为
ViewModel 方法调用。 - 通过 `StateFlow<UiState>` 暴露页面状态。 -
显示加载、空状态、校验错误和操作结果。

不负责： - 直接访问 DAO。 - 在 Composable 中计算连续天数或完成率。 -
直接操作 Room Entity 作为页面状态模型。 - 在 Composable 中创建或调度
AlarmManager 闹钟。

### 3.2 Domain

负责纯业务规则： - 习惯是否应在某日执行。 -
布尔型/数值型完成状态判定。 - 连续天数计算。 - 统计区间与完成率计算。 -
习惯计划变更规则。 - 提醒是否仍然有效的业务判断。

建议的 UseCase： - `ObserveTodayHabitsUseCase` -
`RecordHabitProgressUseCase` - `CreateHabitUseCase` -
`UpdateHabitUseCase` - `ArchiveHabitUseCase` -
`GetHabitStatisticsUseCase` - `CalculateStreakUseCase` -
`ShouldSendReminderUseCase`

Domain 不依赖 Android UI、Activity、BroadcastReceiver 或 Room
的具体实现。日期与时间规则使用 `java.time`。

### 3.3 Data

负责： - Room Entity、DAO、Database、TypeConverter。 - Repository
实现。 - DataStore 配置读写。 - 备份数据的序列化与导入导出。 -
将持久化模型映射为 Domain Model。

Repository 接口放在 Domain；实现放在 Data。不要让 ViewModel 直接依赖
DAO。

### 3.4 Android System Integration

负责把系统事件接入应用： - `ReminderReceiver` 接收闹钟广播。 -
`BootReceiver` 在设备重启后恢复未来提醒。 - `NotificationManager`
发布通知。 - `ReminderScheduler` 注册、取消和重建非精确闹钟。

系统接入层只负责调度和平台
API；是否应发送提醒由可测试的业务规则决定。不要启动常驻 Service。

## 4. 建议工程目录

``` text
app/src/main/java/<package>/
├── MainActivity.kt
├── core/
│   ├── common/
│   ├── designsystem/
│   ├── database/
│   │   ├── AppDatabase.kt
│   │   └── converter/
│   └── util/
├── data/
│   ├── local/
│   │   ├── entity/
│   │   └── dao/
│   ├── mapper/
│   ├── repository/
│   └── backup/
├── domain/
│   ├── model/
│   ├── repository/
│   ├── usecase/
│   └── calculation/
├── feature/
│   ├── today/
│   ├── statistics/
│   ├── profile/
│   ├── habitdetail/
│   └── habiteditor/
├── reminder/
│   ├── ReminderScheduler.kt
│   ├── ReminderReceiver.kt
│   ├── ReminderManager.kt
│   └── BootReceiver.kt
├── navigation/
└── settings/
    └── SettingsDataStore.kt
```

目录是职责边界，不要求每个目录一开始就创建空文件。保持单模块，只有在真实构建复杂度要求时才考虑拆分
Gradle module。

## 5. 核心数据流

### 5.1 今日页面读取

1.  `TodayViewModel` 调用 `ObserveTodayHabitsUseCase`。
2.  UseCase 获取今天的习惯、有效计划和当天日志。
3.  Domain 计算每个习惯的状态（未记录、进行中、已完成）。
4.  ViewModel 映射为 `TodayUiState`。
5.  Compose 只渲染状态，不重复实现业务计算。

### 5.2 打卡写入

1.  用户点击打卡或增加数值。
2.  ViewModel 调用 `RecordHabitProgressUseCase`。
3.  UseCase 校验习惯是否有效、当天是否为计划日、输入是否符合追踪类型。
4.  Repository 在 Room transaction 中插入或更新当天唯一日志。
5.  更新完成状态后，提醒调度器取消该习惯当天剩余提醒。
6.  Room Flow 发出变化，页面自动刷新。

同一习惯同一日期最多一条
`HabitLog`。数值型进度采用日累计值；具体的撤销和覆盖语义见
`DATABASE.md`。

### 5.3 统计读取

1.  页面选择周、月或年区间。
2.  UseCase 取得区间内的有效计划版本与日志。
3.  先计算计划执行日集合，再与完成日志匹配。
4.  分母只包含统计区间内、截至今天且已经到期的计划执行日。
5.  Domain 输出完成率、连续天数、每日汇总等结果。

## 6. 数据与日期规则

统一采用： - 日期：`LocalDate`，数据库保存 ISO-8601 `TEXT`，格式
`yyyy-MM-dd`。 - 本地提醒时间：`LocalTime`，数据库保存 ISO-8601
`TEXT`，格式 `HH:mm:ss`。 - 实际时间戳：`Instant`，数据库保存 epoch
milliseconds `INTEGER`。 - 时区：运行时使用设备当前 `ZoneId`；不把固定
UTC 偏移量当作时区。

计划版本以 `effectiveFrom`（含）和
`effectiveUntil`（含，可空）定义有效区间。更新计划必须结束旧版本并插入新版本，不得改写已经生效的历史计划。详细字段与约束见
`DATABASE.md`。

## 7. 连续天数与统计的业务口径

-   只遍历习惯计划要求执行的日期。
-   非计划日既不增加连续次数，也不打断连续次数。
-   未来日期不参与统计。
-   今天若为计划日，在当天结束前不因尚未完成而打断当前连续记录。
-   对过去已经到期的计划日，缺失日志视为未完成。
-   补录历史日志后，重新计算受影响的当前连续与最长连续。
-   计划变更按各日期当时有效的计划版本判断，不追溯套用新计划。
-   完成率 = 已完成的到期计划次数 / 到期计划次数 × 100%；分母为 0
    时显示"---"，不显示 0%。

建议统一由 Domain 的纯函数实现并通过单元测试验证，UI 不自行推导。

## 8. 提醒设计（Normal）

### 8.1 调度策略

-   使用 `AlarmManager.set()` 或适合当前 API 行为的非精确调度
    API；不得使用 `setExact()`、`setExactAndAllowWhileIdle()` 或
    `USE_EXACT_ALARM` 来实现 Normal 提醒。
-   普通闹钟可能被 Doze、省电策略或系统负载延迟；界面不承诺准点。
-   不因 Normal 模式申请 `SCHEDULE_EXACT_ALARM` 或 `USE_EXACT_ALARM`。
-   Android 13（API 33）及以上在发布通知前请求 `POST_NOTIFICATIONS`
    运行时权限。
-   通知权限被拒绝时，保留习惯记录功能，并在设置页说明通知未开启。

### 8.2 多条提醒与 PendingIntent 唯一性

每条提醒必须有稳定的 `reminderId`。PendingIntent 使用明确的
receiver、action 和唯一 requestCode（由稳定 ID 映射），并通过 extras
传递 `habitId`、`reminderId` 和目标日期。不要只依赖 extras 区分
PendingIntent，因为 extras 本身不参与 PendingIntent 匹配。

编辑提醒时先取消旧
PendingIntent，再注册新时间。一个习惯的多条提醒不得互相覆盖。

### 8.3 触发校验

Receiver 收到广播后： 1. 读取对应习惯、提醒和当天有效计划。 2.
若习惯已归档、提醒已禁用、今天不是计划日，则不发通知。 3.
若当天已达标，则不发通知。 4. 否则发布通知。 5.
计算并注册该提醒下一次触发时间。

完成打卡后取消该习惯当天剩余提醒。多个提醒之间互不覆盖。

### 8.4 生命周期重排

以下情况必须取消旧闹钟并重排： - 新建、编辑、禁用或删除提醒。 -
修改习惯计划。 - 习惯归档或恢复。 - 设备重启（`BOOT_COMPLETED`）。 -
设备时区或系统时间变化。

数据库是提醒配置的唯一可信来源；AlarmManager
中注册的闹钟是可重建的系统状态。启动恢复时从数据库重新计算，不依赖内存状态。

### 8.5 权限与边界

-   Android 13+：`POST_NOTIFICATIONS`。
-   Normal 模式不申请精确闹钟权限。
-   不创建常驻 Service。
-   不保证应用被强制停止后仍能按时提醒；强制停止后的系统行为应在产品说明中明确。
-   真机测试 Doze、省电模式、重启、时区变更和通知权限拒绝。

## 9. 备份与恢复

V1 提供本地 JSON 导出/导入，不依赖服务器。

建议顶层结构：

``` json
{
  "format": "habit-tracker-backup",
  "schemaVersion": 1,
  "exportedAt": "2026-11-04T12:00:00Z",
  "habits": [],
  "schedules": [],
  "logs": [],
  "reminders": []
}
```

要求： - `schemaVersion` 用于备份格式演进，与 Room
数据库版本分开管理。 -
导入前完整解析并校验；校验失败不得写入部分数据。 - 导入写入必须放在 Room
transaction 中。 - V1 默认采用"恢复前预览 +
明确确认"；导入策略选择合并时，以稳定 ID 去重，遇到同 ID
内容冲突时提示用户，不静默覆盖。 - 恢复后重建全部未来提醒。 -
不导入系统权限状态；通知权限仍由 Android 管理。

## 10. 视觉与交互约束

遵循产品概念稿： - 暖白：`#FAF7F2` - 苔绿：`#5B8C6A` - 杏橙：`#F2A65A` -
雾蓝：`#8FAFC4` - 炭灰：`#1C1B1A` - 字体优先思源黑体 / HarmonyOS
Sans；字体不可用时使用系统 sans-serif。 - 卡片圆角
20dp；主要按钮全圆。 - 打卡提供轻量触觉反馈与约 0.3 秒回弹。 -
全部完成时使用轻量叶片动画，不阻塞交互。 - 页面切换淡入并轻微上移（约
12dp）。 - 文案采用温和、具体的正向反馈，不制造连续打卡压力。 - V1
仅实现浅色主题，不加入深色主题切换入口。

## 11. 官方平台参考

-   Android 15 / API
    35：<https://developer.android.com/about/versions/15>
-   AlarmManager：<https://developer.android.com/develop/background-work/services/alarms>
-   通知运行时权限：<https://developer.android.com/develop/ui/views/notifications/notification-permission>
-   Room：<https://developer.android.com/training/data-storage/room>
-   DataStore：<https://developer.android.com/topic/libraries/architecture/datastore>
-   WorkManager：<https://developer.android.com/topic/libraries/architecture/workmanager>
