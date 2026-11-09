package com.habittracker.data.backup

/**
 * Reads the JSON backup (ADR-018) and validates every row before anything touches the database
 * (DATABASE.md 11, ADR-019). Pure Kotlin: the JVM tests drive it, and a malformed file must fail
 * with a path such as "logs[2].log_date" instead of a stack trace.
 */
sealed interface Json {
    data class Obj(val values: Map<String, Json>) : Json
    data class Arr(val values: List<Json>) : Json
    data class Str(val value: String) : Json
    data class Num(val raw: String) : Json
    data class Bool(val value: Boolean) : Json
    object Null : Json
}

/** Minimal RFC 8259 reader: the backup shape is small and org.json is not a JVM test dependency. */
class JsonReader private constructor(private val text: String) {

    class JsonException(message: String) : Exception(message)

    private var position = 0

    companion object {
        fun parse(text: String): Json = JsonReader(text).run {
            skipWhitespace()
            val value = readValue()
            skipWhitespace()
            if (position < text.length) throw JsonException("trailing characters at " + position)
            value
        }
    }

    private fun skipWhitespace() {
        while (position < text.length && text[position].isWhitespace()) position++
    }

    private fun readValue(): Json {
        skipWhitespace()
        if (position >= text.length) throw JsonException("unexpected end of input")
        return when (val ch = text[position]) {
            '{' -> readObject()
            '[' -> readArray()
            '"' -> Json.Str(readString())
            't' -> readLiteral("true", Json.Bool(true))
            'f' -> readLiteral("false", Json.Bool(false))
            'n' -> readLiteral("null", Json.Null)
            else -> if (ch == '-' || ch.isDigit()) readNumber() else throw JsonException("unexpected character at " + position)
        }
    }

    private fun readLiteral(word: String, value: Json): Json {
        if (!text.startsWith(word, position)) throw JsonException("expected " + word + " at " + position)
        position += word.length
        return value
    }

    private fun readObject(): Json.Obj {
        position++ // '{'
        val values = LinkedHashMap<String, Json>()
        skipWhitespace()
        if (position < text.length && text[position] == '}') {
            position++
            return Json.Obj(values)
        }
        while (true) {
            skipWhitespace()
            if (position >= text.length || text[position] != '"') throw JsonException("expected key at " + position)
            val key = readString()
            skipWhitespace()
            if (position >= text.length || text[position] != ':') throw JsonException("expected ':' at " + position)
            position++
            values[key] = readValue()
            skipWhitespace()
            if (position >= text.length) throw JsonException("unterminated object")
            when (text[position]) {
                ',' -> position++
                '}' -> {
                    position++
                    return Json.Obj(values)
                }
                else -> throw JsonException("expected ',' or '}' at " + position)
            }
        }
    }

    private fun readArray(): Json.Arr {
        position++ // '['
        val values = ArrayList<Json>()
        skipWhitespace()
        if (position < text.length && text[position] == ']') {
            position++
            return Json.Arr(values)
        }
        while (true) {
            values.add(readValue())
            skipWhitespace()
            if (position >= text.length) throw JsonException("unterminated array")
            when (text[position]) {
                ',' -> position++
                ']' -> {
                    position++
                    return Json.Arr(values)
                }
                else -> throw JsonException("expected ',' or ']' at " + position)
            }
        }
    }

    private fun readString(): String {
        if (text[position] != '"') throw JsonException("expected string at " + position)
        position++
        val out = StringBuilder()
        while (true) {
            if (position >= text.length) throw JsonException("unterminated string")
            when (val ch = text[position]) {
                '"' -> {
                    position++
                    return out.toString()
                }
                '\\' -> {
                    position++
                    if (position >= text.length) throw JsonException("unterminated escape")
                    when (val esc = text[position]) {
                        '"', '\\', '/' -> out.append(esc)
                        'b' -> out.append('\b')
                        'f' -> out.append('\u000C')
                        'n' -> out.append('\n')
                        'r' -> out.append('\r')
                        't' -> out.append('\t')
                        'u' -> {
                            if (position + 4 >= text.length) throw JsonException("short unicode escape")
                            val hex = text.substring(position + 1, position + 5)
                            out.append(hex.toInt(16).toChar())
                            position += 4
                        }
                        else -> throw JsonException("bad escape at " + position)
                    }
                    position++
                }
                else -> {
                    out.append(ch)
                    position++
                }
            }
        }
    }

    private fun readNumber(): Json.Num {
        val start = position
        if (position < text.length && text[position] == '-') position++
        while (position < text.length && text[position].isDigit()) position++
        if (position < text.length && text[position] == '.') {
            position++
            while (position < text.length && text[position].isDigit()) position++
        }
        if (position < text.length && (text[position] == 'e' || text[position] == 'E')) {
            position++
            if (position < text.length && (text[position] == '+' || text[position] == '-')) position++
            while (position < text.length && text[position].isDigit()) position++
        }
        if (position == start) throw JsonException("expected number at " + start)
        return Json.Num(text.substring(start, position))
    }
}

/** Everything the importer needs to know about a file, or the reasons it is unusable. */
sealed interface BackupParseResult {
    data class Ready(val document: BackupDocument) : BackupParseResult
    data class Invalid(val errors: List<String>) : BackupParseResult
}

object BackupParser {

    const val MAX_BYTES: Int = 8 * 1024 * 1024

    private val DATE = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    private val TIME = Regex("^\\d{2}:\\d{2}:\\d{2}$")

    fun parse(text: String): BackupParseResult {
        val root = try {
            JsonReader.parse(text)
        } catch (error: JsonReader.JsonException) {
            return BackupParseResult.Invalid(listOf("文件不是合法的 JSON：" + error.message))
        }
        val errors = ArrayList<String>()
        val document = readDocument(root, errors)
        return if (errors.isEmpty() && document != null) {
            BackupParseResult.Ready(document)
        } else {
            BackupParseResult.Invalid(errors.ifEmpty { listOf("文件内容不完整") })
        }
    }

    private fun readDocument(root: Json, errors: MutableList<String>): BackupDocument? {
        if (root !is Json.Obj) {
            errors.add("顶层必须是对象")
            return null
        }
        if (root.string("format") != BackupJson.FORMAT) errors.add("format 必须是 " + BackupJson.FORMAT)
        val version = root.long("schemaVersion")
        if (version != BackupJson.SCHEMA_VERSION.toLong()) {
            errors.add("schemaVersion 必须是 " + BackupJson.SCHEMA_VERSION)
        }
        val exportedAt = root.long("exportedAt") ?: run {
            errors.add("exportedAt 必须是数字")
            null
        }

        val habits = readRows(root, "habits", errors) { obj, path -> readHabit(obj, path, errors) }
        val schedules = readRows(root, "schedules", errors) { obj, path -> readSchedule(obj, path, errors) }
        val logs = readRows(root, "logs", errors) { obj, path -> readLog(obj, path, errors) }
        val reminders = readRows(root, "reminders", errors) { obj, path -> readReminder(obj, path, errors) }
        if (habits == null || schedules == null || logs == null || reminders == null) return null

        val habitIds = habits.map { it.id }.toSet()
        if (habitIds.size != habits.size) errors.add("habits.id 有重复")
        val logKeys = HashSet<String>()
        logs.forEach { log ->
            if (log.habitId !in habitIds) errors.add("logs 引用了不存在的 habit_id " + log.habitId)
            if (!logKeys.add(log.habitId.toString() + "@" + log.logDate)) {
                errors.add("logs 中 habit " + log.habitId + " 在 " + log.logDate + " 有重复记录")
            }
        }
        schedules.forEach { schedule ->
            if (schedule.habitId !in habitIds) errors.add("schedules 引用了不存在的 habit_id " + schedule.habitId)
            validateMask(schedule, errors)
        }
        val reminderKeys = HashSet<String>()
        reminders.forEach { reminder ->
            if (reminder.habitId !in habitIds) errors.add("reminders 引用了不存在的 habit_id " + reminder.habitId)
            if (!reminderKeys.add(reminder.habitId.toString() + "@" + reminder.reminderTime)) {
                errors.add("reminders 中 habit " + reminder.habitId + " 在 " + reminder.reminderTime + " 有重复提醒")
            }
        }
        if (schedules.map { it.id }.toSet().size != schedules.size) errors.add("schedules.id 有重复")
        if (logs.map { it.id }.toSet().size != logs.size) errors.add("logs.id 有重复")
        if (reminders.map { it.id }.toSet().size != reminders.size) errors.add("reminders.id 有重复")

        if (errors.isNotEmpty()) return null
        return BackupDocument(
            exportedAt = exportedAt ?: 0L,
            habits = habits,
            schedules = schedules,
            logs = logs,
            reminders = reminders,
        )
    }

    private fun validateMask(schedule: BackupSchedule, errors: MutableList<String>) {
        if (schedule.weekdayMask !in 1..127) {
            errors.add("schedules[" + schedule.id + "].weekday_mask 必须在 1..127")
            return
        }
        when (schedule.frequencyType) {
            "DAILY" -> if (schedule.weekdayMask != 127) errors.add("DAILY 的 weekday_mask 必须是 127")
            "WEEKDAYS" -> if (schedule.weekdayMask != 31) errors.add("WEEKDAYS 的 weekday_mask 必须是 31")
        }
    }

    private fun <T> readRows(
        root: Json.Obj,
        name: String,
        errors: MutableList<String>,
        read: (Json.Obj, String) -> T?,
    ): List<T>? {
        val array = root.values[name]
        if (array !is Json.Arr) {
            errors.add(name + " 必须是数组")
            return null
        }
        val rows = ArrayList<T>(array.values.size)
        array.values.forEachIndexed { index, value ->
            val path = name + "[" + index + "]"
            if (value !is Json.Obj) {
                errors.add(path + " 必须是对象")
                return@forEachIndexed
            }
            read(value, path)?.let(rows::add)
        }
        return rows
    }

    private fun readHabit(obj: Json.Obj, path: String, errors: MutableList<String>): BackupHabit? {
        val id = obj.long("id") ?: return wrong(errors, path + ".id", "数字")
        val name = obj.string("name") ?: return wrong(errors, path + ".name", "字符串")
        if (name.isBlank()) errors.add(path + ".name 不能为空")
        val icon = obj.string("icon") ?: return wrong(errors, path + ".icon", "字符串")
        val color = obj.string("color") ?: return wrong(errors, path + ".color", "字符串")
        val tracking = obj.string("tracking_type") ?: return wrong(errors, path + ".tracking_type", "字符串")
        if (tracking !in setOf("BOOLEAN", "NUMERIC")) errors.add(path + ".tracking_type 不是已知枚举")
        val target = obj.double("target_value") ?: return wrong(errors, path + ".target_value", "数字")
        if (target <= 0) errors.add(path + ".target_value 必须大于 0")
        val unit = obj.string("unit")
        val createdAt = obj.long("created_at") ?: return wrong(errors, path + ".created_at", "数字")
        val updatedAt = obj.long("updated_at") ?: return wrong(errors, path + ".updated_at", "数字")
        var mark = errors.size
        val archivedAt = obj.optionalLong("archived_at", path + ".archived_at", errors)
        if (errors.size > mark) return null
        val createdOn = obj.string("created_on") ?: return wrong(errors, path + ".created_on", "字符串")
        if (!DATE.matches(createdOn)) errors.add(path + ".created_on 必须是 yyyy-MM-dd")
        mark = errors.size
        val archivedOn = obj.optionalString("archived_on", path + ".archived_on", errors)
        if (errors.size > mark) return null
        if (archivedOn != null && !DATE.matches(archivedOn)) errors.add(path + ".archived_on 必须是 yyyy-MM-dd")
        return BackupHabit(id, name, icon, color, tracking, target, unit, createdAt, updatedAt, archivedAt, createdOn, archivedOn)
    }

    private fun readSchedule(obj: Json.Obj, path: String, errors: MutableList<String>): BackupSchedule? {
        val id = obj.long("id") ?: return wrong(errors, path + ".id", "数字")
        val habitId = obj.long("habit_id") ?: return wrong(errors, path + ".habit_id", "数字")
        val frequency = obj.string("frequency_type") ?: return wrong(errors, path + ".frequency_type", "字符串")
        if (frequency !in setOf("DAILY", "WEEKDAYS", "CUSTOM")) errors.add(path + ".frequency_type 不是已知枚举")
        val mask = obj.long("weekday_mask")?.toInt() ?: return wrong(errors, path + ".weekday_mask", "数字")
        val from = obj.string("effective_from") ?: return wrong(errors, path + ".effective_from", "字符串")
        if (!DATE.matches(from)) errors.add(path + ".effective_from 必须是 yyyy-MM-dd")
        val mark = errors.size
        val until = obj.optionalString("effective_until", path + ".effective_until", errors)
        if (errors.size > mark) return null
        if (until != null && !DATE.matches(until)) errors.add(path + ".effective_until 必须是 yyyy-MM-dd")
        val createdAt = obj.long("created_at") ?: return wrong(errors, path + ".created_at", "数字")
        return BackupSchedule(id, habitId, frequency, mask, from, until, createdAt)
    }

    private fun readLog(obj: Json.Obj, path: String, errors: MutableList<String>): BackupLog? {
        val id = obj.long("id") ?: return wrong(errors, path + ".id", "数字")
        val habitId = obj.long("habit_id") ?: return wrong(errors, path + ".habit_id", "数字")
        val date = obj.string("log_date") ?: return wrong(errors, path + ".log_date", "字符串")
        if (!DATE.matches(date)) errors.add(path + ".log_date 必须是 yyyy-MM-dd")
        val actual = obj.double("actual_value") ?: return wrong(errors, path + ".actual_value", "数字")
        if (actual < 0) errors.add(path + ".actual_value 不能为负")
        val target = obj.double("target_snapshot") ?: return wrong(errors, path + ".target_snapshot", "数字")
        if (target <= 0) errors.add(path + ".target_snapshot 必须大于 0")
        val status = obj.string("status") ?: return wrong(errors, path + ".status", "字符串")
        if (status !in setOf("IN_PROGRESS", "COMPLETED")) errors.add(path + ".status 不是已知枚举")
        var mark = errors.size
        val completedAt = obj.optionalLong("completed_at", path + ".completed_at", errors)
        if (errors.size > mark) return null
        mark = errors.size
        val duration = obj.optionalLong("duration_seconds", path + ".duration_seconds", errors)
        if (errors.size > mark) return null
        if (duration != null && duration < 0) errors.add(path + ".duration_seconds 不能为负")
        if (status == "COMPLETED" && completedAt == null) errors.add(path + " 已完成但缺少 completed_at")
        if (status == "IN_PROGRESS" && completedAt != null) errors.add(path + " 未完成但有 completed_at")
        val createdAt = obj.long("created_at") ?: return wrong(errors, path + ".created_at", "数字")
        val updatedAt = obj.long("updated_at") ?: return wrong(errors, path + ".updated_at", "数字")
        return BackupLog(id, habitId, date, actual, target, status, completedAt, duration?.toInt(), createdAt, updatedAt)
    }

    private fun readReminder(obj: Json.Obj, path: String, errors: MutableList<String>): BackupReminder? {
        val id = obj.long("id") ?: return wrong(errors, path + ".id", "数字")
        val habitId = obj.long("habit_id") ?: return wrong(errors, path + ".habit_id", "数字")
        val time = obj.string("reminder_time") ?: return wrong(errors, path + ".reminder_time", "字符串")
        if (!TIME.matches(time)) errors.add(path + ".reminder_time 必须是 HH:mm:ss")
        val enabled = obj.boolean("enabled") ?: return wrong(errors, path + ".enabled", "布尔")
        val createdAt = obj.long("created_at") ?: return wrong(errors, path + ".created_at", "数字")
        val updatedAt = obj.long("updated_at") ?: return wrong(errors, path + ".updated_at", "数字")
        return BackupReminder(id, habitId, time, enabled, createdAt, updatedAt)
    }

    private fun wrong(errors: MutableList<String>, path: String, type: String): Nothing? {
        errors.add(path + " 必须是" + type)
        return null
    }

    private fun Json.Obj.string(key: String): String? = (values[key] as? Json.Str)?.value

    private fun Json.Obj.boolean(key: String): Boolean? = (values[key] as? Json.Bool)?.value

    private fun Json.Obj.long(key: String): Long? {
        val number = values[key] as? Json.Num ?: return null
        return number.raw.toLongOrNull() ?: number.raw.toDoubleOrNull()?.toLong()
    }

    private fun Json.Obj.double(key: String): Double? = (values[key] as? Json.Num)?.raw?.toDoubleOrNull()

    private fun Json.Obj.optionalString(key: String, path: String, errors: MutableList<String>): String? {
        val value = values[key] ?: return null
        if (value is Json.Null) return null
        if (value !is Json.Str) {
            errors.add(path + " 必须是字符串或 null")
            return null
        }
        return value.value
    }

    private fun Json.Obj.optionalLong(key: String, path: String, errors: MutableList<String>): Long? {
        val value = values[key] ?: return null
        if (value is Json.Null) return null
        if (value !is Json.Num) {
            errors.add(path + " 必须是数字或 null")
            return null
        }
        val parsed = value.raw.toLongOrNull() ?: value.raw.toDoubleOrNull()?.toLong()
        if (parsed == null) errors.add(path + " 必须是数字或 null")
        return parsed
    }
}