package com.habittracker.data.backup

/**
 * Serialises a [BackupDocument] to the JSON shape fixed by DATABASE.md 11.
 *
 * Field names are the database column names, so the mapping stays one-to-one and a future reader
 * does not need a second name table. Dates are ISO yyyy-MM-dd, local times ISO HH:mm:ss, timestamps
 * epoch milliseconds (ADR-009). Hand-rolled on purpose: org.json is not on the JVM test classpath
 * and the shape is small enough that a serialisation library would be a dependency for no benefit.
 */
object BackupJson {

    const val FORMAT: String = "habit-tracker-backup"
    const val SCHEMA_VERSION: Int = 1

    fun encode(document: BackupDocument): String {
        val out = StringBuilder(512)
        out.append("{\n")
        line(out, 1, "\"format\": " + quote(FORMAT) + ",")
        line(out, 1, "\"schemaVersion\": " + SCHEMA_VERSION + ",")
        line(out, 1, "\"exportedAt\": " + document.exportedAt + ",")
        if (document.habits.isEmpty()) {
            line(out, 1, "\"habits\": [],")
        } else {
            line(out, 1, "\"habits\": [")
        document.habits.forEachIndexed { index, habit ->
            out.append("    {\n")
            line(out, 3, "\"id\": " + habit.id + ",")
            line(out, 3, "\"name\": " + quote(habit.name) + ",")
            line(out, 3, "\"icon\": " + quote(habit.icon) + ",")
            line(out, 3, "\"color\": " + quote(habit.color) + ",")
            line(out, 3, "\"tracking_type\": " + quote(habit.trackingType) + ",")
            line(out, 3, "\"target_value\": " + number(habit.targetValue) + ",")
            line(out, 3, "\"unit\": " + quoteOrNull(habit.unit) + ",")
            line(out, 3, "\"created_at\": " + habit.createdAt + ",")
            line(out, 3, "\"updated_at\": " + habit.updatedAt + ",")
            line(out, 3, "\"archived_at\": " + (habit.archivedAt?.toString() ?: "null") + ",")
            line(out, 3, "\"created_on\": " + quote(habit.createdOn) + ",")
            line(out, 3, "\"archived_on\": " + quoteOrNull(habit.archivedOn))
            out.append(if (index == document.habits.lastIndex) "    }\n" else "    },\n")
        }
            line(out, 1, "],")
        }
        if (document.schedules.isEmpty()) {
            line(out, 1, "\"schedules\": [],")
        } else {
            line(out, 1, "\"schedules\": [")
        document.schedules.forEachIndexed { index, schedule ->
            out.append("    {\n")
            line(out, 3, "\"id\": " + schedule.id + ",")
            line(out, 3, "\"habit_id\": " + schedule.habitId + ",")
            line(out, 3, "\"frequency_type\": " + quote(schedule.frequencyType) + ",")
            line(out, 3, "\"weekday_mask\": " + schedule.weekdayMask + ",")
            line(out, 3, "\"effective_from\": " + quote(schedule.effectiveFrom) + ",")
            line(out, 3, "\"effective_until\": " + quoteOrNull(schedule.effectiveUntil) + ",")
            line(out, 3, "\"created_at\": " + schedule.createdAt)
            out.append(if (index == document.schedules.lastIndex) "    }\n" else "    },\n")
        }
            line(out, 1, "],")
        }
        if (document.logs.isEmpty()) {
            line(out, 1, "\"logs\": [],")
        } else {
            line(out, 1, "\"logs\": [")
        document.logs.forEachIndexed { index, log ->
            out.append("    {\n")
            line(out, 3, "\"id\": " + log.id + ",")
            line(out, 3, "\"habit_id\": " + log.habitId + ",")
            line(out, 3, "\"log_date\": " + quote(log.logDate) + ",")
            line(out, 3, "\"actual_value\": " + number(log.actualValue) + ",")
            line(out, 3, "\"target_snapshot\": " + number(log.targetSnapshot) + ",")
            line(out, 3, "\"status\": " + quote(log.status) + ",")
            line(out, 3, "\"completed_at\": " + (log.completedAt?.toString() ?: "null") + ",")
            line(out, 3, "\"duration_seconds\": " + (log.durationSeconds?.toString() ?: "null") + ",")
            line(out, 3, "\"created_at\": " + log.createdAt + ",")
            line(out, 3, "\"updated_at\": " + log.updatedAt)
            out.append(if (index == document.logs.lastIndex) "    }\n" else "    },\n")
        }
            line(out, 1, "],")
        }
        if (document.reminders.isEmpty()) {
            line(out, 1, "\"reminders\": []")
        } else {
            line(out, 1, "\"reminders\": [")
        document.reminders.forEachIndexed { index, reminder ->
            out.append("    {\n")
            line(out, 3, "\"id\": " + reminder.id + ",")
            line(out, 3, "\"habit_id\": " + reminder.habitId + ",")
            line(out, 3, "\"reminder_time\": " + quote(reminder.reminderTime) + ",")
            line(out, 3, "\"enabled\": " + reminder.enabled + ",")
            line(out, 3, "\"created_at\": " + reminder.createdAt + ",")
            line(out, 3, "\"updated_at\": " + reminder.updatedAt)
            out.append(if (index == document.reminders.lastIndex) "    }\n" else "    },\n")
        }
            line(out, 1, "]")
        }
        out.append("}\n")
        return out.toString()
    }

    private fun line(out: StringBuilder, depth: Int, text: String) {
        repeat(depth) { out.append("  ") }
        out.append(text).append("\n")
    }

    private fun number(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

    private fun quoteOrNull(value: String?): String = if (value == null) "null" else quote(value)

    /** Escapes quotes, backslashes and control characters; names are user input. */
    fun quote(value: String): String {
        val out = StringBuilder(value.length + 2)
        out.append('"')
        value.forEach { ch ->
            when (ch) {
                '"' -> out.append("\\\"")
                '\\' -> out.append("\\\\")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                else -> if (ch < ' ') out.append("\\u").append(String.format("%04x", ch.code)) else out.append(ch)
            }
        }
        out.append('"')
        return out.toString()
    }
}