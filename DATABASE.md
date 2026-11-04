# DATABASE.md

> Habit Tracker V1 数据库规范\
> 对应 `ARCHITECTURE.md` 的分层与日期策略；开发验收见 `DEVELOPMENT.md`。

## 1. 数据库设计原则

1.  Room / SQLite 是习惯业务数据的唯一可信来源。
2.  V1
    使用四张核心表：`habits`、`habit_schedules`、`habit_logs`、`habit_reminders`。
3.  每个习惯每天最多一条汇总日志；不保存每次操作的时间轴。
4.  计划采用版本化记录，历史计划不可被新计划覆盖。
5.  删除习惯默认使用归档；历史统计不得因归档而丢失。
6.  所有多表写操作使用 Room transaction。
7.  数据库 schema 版本与 JSON 备份格式版本分别管理。

## 2. 类型与存储约定

  Kotlin 类型   SQLite 类型       规范
  ------------- ----------------- -----------------------
  `Long` ID     `INTEGER`         主键；本地生成
  `String`      `TEXT`            UTF-8 文本
  `Boolean`     `INTEGER`         0=false，1=true
  `Double`      `REAL`            数值进度与目标
  `LocalDate`   `TEXT`            ISO `yyyy-MM-dd`
  `LocalTime`   `TEXT`            ISO `HH:mm:ss`
  `Instant`     `INTEGER`         epoch milliseconds
  可空字段      对应类型 / NULL   不用空字符串冒充 NULL

`LocalDate` 和 `LocalTime` 必须使用固定 ISO
格式，确保日期文本可按字典序排序。时间戳统一 epoch
milliseconds，不得在不同表中混用秒与毫秒。

## 3. 表关系

``` text
habits 1 ─── N habit_schedules
habits 1 ─── N habit_logs
habits 1 ─── N habit_reminders
```

外键均引用
`habits.id`。习惯归档不删除子记录。用户明确永久删除习惯时，使用
transaction 删除关联提醒、日志、计划，再删除习惯；删除前必须二次确认。

## 4. SQL Schema

以下 SQL 表达逻辑 schema。Room Entity
的列名、默认值、索引和外键必须与之保持一致。

### 4.1 habits

``` sql
CREATE TABLE habits (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  name TEXT NOT NULL,
  icon TEXT NOT NULL,
  color TEXT NOT NULL,
  tracking_type TEXT NOT NULL
    CHECK (tracking_type IN ('BOOLEAN', 'NUMERIC')),
  target_value REAL NOT NULL DEFAULT 1.0,
  unit TEXT,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL,
  archived_at INTEGER,
  CHECK (length(trim(name)) > 0),
  CHECK (target_value > 0)
);
```

字段说明：

  字段              含义
  ----------------- --------------------------------------------
  `id`              本地稳定 ID
  `name`            习惯名称
  `icon`            V1 图标标识符，不保存任意 UI 对象
  `color`           颜色 token 或规范化 HEX
  `tracking_type`   `BOOLEAN` / `NUMERIC`
  `target_value`    数值型目标；布尔型固定为 1
  `unit`            数值型单位，如 ml、页、分钟；布尔型为 NULL
  `created_at`      创建时间
  `updated_at`      最近修改时间
  `archived_at`     归档时间；NULL 表示未归档

约束： - `BOOLEAN`：`target_value=1`、`unit IS NULL`；日志实际值规范为 0
或 1。 - `NUMERIC`：`target_value>0`；`unit`
必须非空且去除首尾空格后不为空。 - 追踪类型变更属于语义变更。V1
不允许直接把已有历史数据的习惯从 BOOLEAN 改成 NUMERIC
或反向修改；应新建习惯，或在未来设计显式迁移流程。 -
名称允许重复，不设置名称唯一约束。

### 4.2 habit_schedules

每次计划配置形成一个版本。`effective_from` 含当日，`effective_until`
含当日；NULL 表示持续有效。

``` sql
CREATE TABLE habit_schedules (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  habit_id INTEGER NOT NULL,
  frequency_type TEXT NOT NULL
    CHECK (frequency_type IN ('DAILY', 'WEEKDAYS', 'CUSTOM')),
  weekday_mask INTEGER NOT NULL,
  effective_from TEXT NOT NULL,
  effective_until TEXT,
  created_at INTEGER NOT NULL,
  FOREIGN KEY (habit_id) REFERENCES habits(id) ON DELETE CASCADE,
  CHECK (effective_until IS NULL OR effective_until >= effective_from),
  CHECK (weekday_mask BETWEEN 0 AND 127)
);

CREATE INDEX index_habit_schedules_habit_id
  ON habit_schedules(habit_id);

CREATE INDEX index_habit_schedules_habit_effective
  ON habit_schedules(habit_id, effective_from, effective_until);
```

星期掩码定义（ISO weekday）： - Monday=1 对应 bit 0（值 1）。 -
Tuesday=2 对应 bit 1（值 2）。 - Wednesday=3 对应 bit 2（值 4）。 -
Thursday=4 对应 bit 3（值 8）。 - Friday=5 对应 bit 4（值 16）。 -
Saturday=6 对应 bit 5（值 32）。 - Sunday=7 对应 bit 6（值 64）。

例如周一、三、五：`1 + 4 + 16 = 21`。

频率规则： - `DAILY`：每天执行，`weekday_mask=127`。 -
`WEEKDAYS`：周一至周五，`weekday_mask=31`。 - `CUSTOM`：按掩码执行，必须
`weekday_mask != 0`。

计划版本规则： 1. 创建习惯时必须创建一条初始计划。 2. 修改计划时，在同一
transaction 中将旧版本 `effective_until` 设为新版本 `effective_from`
前一天，再插入新版本。 3. 不允许版本日期区间重叠。 4.
不允许删除已参与历史统计的计划版本。 5. 对同一习惯，最多只能有一条
`effective_until IS NULL` 的当前版本；Room transaction 内检查并维护。 6.
若新计划从未来日期生效，旧计划在生效日前仍然有效。

SQLite CHECK 无法简单表达同一习惯的跨行区间不重叠；必须由
Repository/UseCase 在 transaction
中校验。并发更新也必须在同一事务中完成。

### 4.3 habit_logs

一条记录表示某习惯在一个自然日内的汇总进度。

``` sql
CREATE TABLE habit_logs (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  habit_id INTEGER NOT NULL,
  log_date TEXT NOT NULL,
  actual_value REAL NOT NULL,
  target_snapshot REAL NOT NULL,
  status TEXT NOT NULL
    CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
  completed_at INTEGER,
  duration_seconds INTEGER,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL,
  FOREIGN KEY (habit_id) REFERENCES habits(id) ON DELETE CASCADE,
  UNIQUE (habit_id, log_date),
  CHECK (actual_value >= 0),
  CHECK (target_snapshot > 0),
  CHECK (duration_seconds IS NULL OR duration_seconds >= 0)
);

CREATE INDEX index_habit_logs_habit_date
  ON habit_logs(habit_id, log_date);

CREATE INDEX index_habit_logs_log_date
  ON habit_logs(log_date);
```

字段说明： - `actual_value`：当天累计值。BOOLEAN 只允许 0 或 1；NUMERIC
为非负累计数值。 -
`target_snapshot`：写入该日记录时使用的目标快照，避免未来修改目标后改变历史完成判定。 -
`status`：由业务规则推导并持久化，允许
`IN_PROGRESS`、`COMPLETED`。未记录不是一条日志，而是没有对应行。 -
`completed_at`：首次达到目标的实际时间；撤销达标后置
NULL，再次达标时更新为新的达标时刻。 -
`duration_seconds`：可选的当日耗时，不作为完成判断依据。 - `created_at`
/ `updated_at`：记录创建与最后修改时间。

完成状态规则： - BOOLEAN：`actual_value=0` 为未完成；`actual_value=1`
为完成。 - NUMERIC：`actual_value < target_snapshot`
为进行中；`actual_value >= target_snapshot` 为完成。 -
达标后继续增加数值，仍保持完成。 -
数值型允许超额完成；统计只判定是否达到目标。 - BOOLEAN 的输入只允许 0 或
1。 - `status` 和 `completed_at` 必须由同一个 Domain 规则计算，不允许 UI
独立写入互相矛盾的值。

无记录与 0 值： - 无日志行：当天尚未产生记录。 - 有日志且
`actual_value=0`：用户显式记录了
0；展示上仍可视为未完成，但可保留"已记录"语义。 - 未打卡不能自动插入 0
值日志。

写入与撤销： - BOOLEAN 打卡：将值设为 1；撤销：设为 0。 - NUMERIC
增量：在 transaction
内读取现值并增加；设置具体值：以新值覆盖当天累计值。 -
达标后撤销或减少到目标以下：状态变为
`IN_PROGRESS`，`completed_at=NULL`，并重新安排当天仍有效的提醒（如果提醒时间尚未过去）。 -
同一日期重复写入采用
upsert，但数值"增加"必须是原子增量，不能用先读后写的无事务流程。 -
同时写入时使用 Room transaction；数值增量优先使用 SQL
原子更新或在事务内读改写。

### 4.4 habit_reminders

每个习惯允许多条每日提醒。

``` sql
CREATE TABLE habit_reminders (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  habit_id INTEGER NOT NULL,
  reminder_time TEXT NOT NULL,
  enabled INTEGER NOT NULL DEFAULT 1,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL,
  FOREIGN KEY (habit_id) REFERENCES habits(id) ON DELETE CASCADE,
  CHECK (enabled IN (0, 1)),
  UNIQUE (habit_id, reminder_time)
);

CREATE INDEX index_habit_reminders_habit_id
  ON habit_reminders(habit_id);
```

规则： - 同一习惯不允许两条相同时间的提醒。 -
不同习惯可以使用相同时间。 - `reminder_time`
保存本地墙上时间；实际下一次触发日期根据设备当前时区和当前日期计算。 -
禁用提醒保留记录，不物理删除。 - 删除提醒时取消对应 PendingIntent。 -
Normal 模式使用非精确 AlarmManager，不申请精确闹钟权限。

## 5. Room Entity 与 TypeConverter

建议 Entity： - `HabitEntity` - `HabitScheduleEntity` -
`HabitLogEntity` - `HabitReminderEntity`

建议 Converter： - `LocalDate` ↔ ISO `yyyy-MM-dd` TEXT。 - `LocalTime` ↔
ISO `HH:mm:ss` TEXT。 - `Instant` ↔ epoch milliseconds Long。 - Enum ↔
稳定字符串值。

不要将 Kotlin `Set<Int>` 直接存成 SQLite 列；星期掩码使用 Int。不要依赖
enum 的 ordinal，因为调整枚举顺序会破坏已有数据。

## 6. 计划日查询规则

给定习惯和日期 D： 1. 找到满足 `effective_from <= D`
且（`effective_until IS NULL` 或
`D <= effective_until`）的唯一计划版本。 2.
若不存在版本，则该日不是计划日。 3. 按 frequency_type 与 weekday_mask
判断： - DAILY：true。 - WEEKDAYS：ISO weekday 1..5。 - CUSTOM：对应 bit
是否为 1。

如果数据库中出现多个有效版本，视为数据完整性错误，不能任意挑选一条继续统计。

## 7. 连续天数算法口径

### 7.1 输入集合

对一个习惯，生成从创建日期至今天的计划执行日期序列。每个日期使用当日生效的计划版本判断。

### 7.2 单日判定

-   计划日且日志状态为 COMPLETED：成功。
-   计划日且日期早于今天、日志缺失或未完成：失败。
-   今天是计划日且尚未完成：当天尚未到期，暂不作为失败。
-   非计划日：跳过，不成功也不失败。
-   未来日期：完全忽略。

### 7.3 当前连续次数

从最近一个已到期的计划日向过去回溯： -
今天是计划日：若今天已完成，则从今天开始回溯；若今天未完成，则从最近一个过去的计划日开始回溯，今天不打断当前连续。 -
今天不是计划日：从最近一个过去的计划日开始回溯。 -
遇到第一个失败的到期计划日即停止。 -
成功一个计划日，计数加一；非计划日跳过。

该定义意味着当前连续次数在今天尚未完成时仍可保持昨天的结果。若用户今天后来补录完成，则当前连续次数包含今天。

### 7.4 最长连续次数

按全部已到期计划日的时间顺序扫描；成功计划日增加当前段长度，失败计划日将当前段归零，非计划日跳过。最长值为扫描期间的最大段长度。今天未到期时不作为失败，也不计入最长值，除非今天已经完成。

### 7.5 补录与计划修改

-   补录历史日志后重新计算整个相关区间。
-   计划修改不改写过去计划版本。
-   计划变更前后相邻的成功计划日可以属于同一连续段，只要两日之间没有失败的计划执行日。
-   归档后保留历史最长连续；当前连续按归档日之前最后一个到期计划日计算，归档后的日期不再产生应执行次数。

## 8. 统计口径

### 8.1 区间

所有区间使用闭区间 `[startDate, endDate]`。周、月、年由 `LocalDate`
计算自然区间边界。

### 8.2 完成率

-   分子：区间内已完成的计划执行日数量。
-   分母：区间内截至今天、已经到期的计划执行日数量。
-   未来日期排除。
-   今天的计划日若未完成，因尚未到期而不计入分母；今天已完成则计入分子与分母。
-   非计划日不进入分母。
-   分母为 0 时返回空值，由 UI 显示"---"。

### 8.3 归档

归档习惯仍可查看历史统计；统计日期不得晚于归档日期。归档日是否计入取决于当日是否为计划日，且归档操作发生前用户是否完成：已有日志保留；没有完成日志的归档当日不应在未来被继续提醒。产品统计实现应将归档日期作为该习惯有效期的最后日期，并在归档时冻结当天状态。

## 9. 删除、归档与恢复

-   归档：设置
    `archived_at`，保留所有计划、日志和提醒；禁用提醒并取消所有
    PendingIntent。
-   恢复：清空 `archived_at`；提醒是否重新启用应沿用各条 reminder 的
    enabled 字段，并重新调度未来触发。
-   永久删除：显式确认后在单一 transaction 内删除子表与主表记录。
-   V1 不做软删除字段以外的回收站机制。

## 10. 数据库迁移

-   `@Database(version = N)` 每次 schema 变化递增。
-   每个版本提供明确 `Migration`；禁止生产构建使用
    `fallbackToDestructiveMigration()`。
-   使用 Room schema export，将 JSON schema 文件纳入版本控制。
-   Migration 测试必须覆盖至少一个真实旧版本数据库。
-   不得重用已删除字段名表达不同含义。

## 11. JSON 备份格式

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

导出： - 包含归档习惯及其历史计划、日志、提醒配置。 - 不包含 Android
系统权限、PendingIntent 或设备闹钟状态。 - 数值使用 JSON
number；日期与时间使用上述 ISO 格式；时间戳使用 ISO-8601 UTC
字符串或统一 epoch milliseconds，整个格式版本内必须固定。本 V1
推荐时间戳统一 epoch milliseconds number。

导入： 1. 读取文件并限制合理文件大小。 2. 校验
`format`、`schemaVersion`、必填字段、枚举、日期格式、引用完整性和数值范围。
3. 先在内存中完成全量校验，再进入数据库 transaction。 4. V1
提供"合并导入"和"清空后恢复"两种明确操作；清空后恢复必须二次确认。 5.
合并导入按原始 ID 去重；相同 ID 且内容一致则跳过；相同 ID
内容不同则报告冲突并要求用户选择，不静默覆盖。 6.
事务成功后重建未来提醒；事务失败则回滚，不留下部分导入数据。

备份 `schemaVersion` 与 Room `version`
不要求相同。未来格式升级应提供独立 JSON 迁移器。

## 12. 必须覆盖的数据库测试

-   同一习惯同一天插入第二条日志被唯一约束拒绝或 upsert 正确处理。
-   同一习惯同一提醒时间重复添加失败。
-   不同习惯相同提醒时间允许。
-   计划版本不能重叠。
-   Monday=1、Sunday=7 的掩码映射正确。
-   BOOLEAN 只接受 0/1。
-   NUMERIC 的部分完成与超额完成判定正确。
-   修改目标后旧日志仍使用 target_snapshot。
-   归档保留历史数据并禁用提醒。
-   永久删除时关联数据按预期删除。
-   JSON 导入校验失败不产生部分写入。
