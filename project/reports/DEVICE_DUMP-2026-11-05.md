# DEVICE DUMP — 2026-11-05

> 真机走查的时间点快照。设备此后被 `adb shell pm clear` 重置过，因此本文件是**唯一**的当时行级证据。

## 设备
- 型号：`iQOO Neo7 SE`（`V2238A`），Android 15（API 35），`versionName=0.1.0`
- 连接方式：USB 调试（序列号已省略）
- APK：`app-debug.apk`，`adb install -r` → `Success`

## 采集方式
```
adb exec-out run-as com.habittracker cat databases/habit-tracker.db     > habit-tracker.db
adb exec-out run-as com.habittracker cat databases/habit-tracker.db-wal > habit-tracker.db-wal
adb exec-out run-as com.habittracker cat databases/habit-tracker.db-shm > habit-tracker.db-shm
node query.mjs   # node:sqlite，WAL 模式下必须同时拉 -wal，否则只能读到 1 页
```

## 当时的查询输出（原样）
```json
tables: ["android_metadata","habit_logs","habit_reminders","habit_schedules","habits","room_master_table","sqlite_sequence"]
--- android_metadata
[{"locale":"zh_CN"}]
--- habit_logs
[{"id":1,"habit_id":1,"log_date":"2026-11-05","actual_value":1,"target_snapshot":1,"status":"COMPLETED","completed_at":1793862198279,"duration_seconds":null,"created_at":1793862198279,"updated_at":1793862198279}]
--- habit_reminders
[]
--- habit_schedules
[{"id":1,"habit_id":1,"frequency_type":"DAILY","weekday_mask":127,"effective_from":"2026-11-05","effective_until":null,"created_at":1793862187352}]
--- habits
[{"id":1,"name":"Wa't'er","icon":"star","color":"#5B8C6A","tracking_type":"BOOLEAN","target_value":1,"unit":null,"created_at":1793862187352,"updated_at":1793862187352,"archived_at":null,"created_on":"2026-11-05","archived_on":null}]
--- room_master_table
[{"id":42,"identity_hash":"cbb57b09d85478fb41458c2508ca2d28"}]
--- sqlite_sequence
[{"name":"habits","seq":1},{"name":"habit_schedules","seq":1},{"name":"habit_logs","seq":1}]
```

## 这些行证明什么
- `habits.unit = null` 且 `tracking_type = BOOLEAN`：**单位泄漏缺陷的修复在真实写入路径上生效**（此前该保存会被 `normalizeTarget` 拒绝）
- `habit_schedules.weekday_mask = 127` + `frequency_type = DAILY`：保存时确实生成了首个计划版本
- `habit_logs.status = COMPLETED`、`target_snapshot = 1`：打卡走完仓储与事务
- `room_master_table.identity_hash` 存在：Room 校验哈希写入

## 时间线（设备时区）
| 时间 | 事件 |
|---|---|
| 15:02:xx | 安装 APK、启动、进入新建页 |
| 15:03:11 | `saved.png`：保存成功，Today 出现该习惯 |
| 15:03:22 | `done.png`：点击卡片变「今天已完成」 |
| 15:03:57 | 拉取并查询数据库（即上文输出） |
| 15:04:xx | **`adb shell pm clear com.habittracker`** —— 为验证「数值习惯输入对话框」重置应用数据；该操作删除了上述行 |

## 已知不可复核之处
评审在 `07b1aab` 上复核时读到的是重置后的空库（`habits`/`habit_schedules`/`habit_logs` 均 0 行，`integrity_check=ok`）。
**因此上文行级事实仅以本文件的当时输出为准，不能由当前设备重新导出。**
后续真机走查应遵循：先导出 dump 入库，再重置数据。
