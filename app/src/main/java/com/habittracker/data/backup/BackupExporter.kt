package com.habittracker.data.backup

import android.content.Context
import android.net.Uri
import com.habittracker.data.local.HabitDatabase
import com.habittracker.data.local.entity.HabitEntity
import com.habittracker.data.local.entity.HabitLogEntity
import com.habittracker.data.local.entity.HabitReminderEntity
import com.habittracker.data.local.entity.HabitScheduleEntity
import java.time.format.DateTimeFormatter

/**
 * Reads every table and writes the JSON backup through a SAF Uri (TASK-030). No storage permission
 * is needed: the document is created by the system picker and the app only receives a write handle.
 */
class BackupExporter(
    private val database: HabitDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    /** Writes the backup and returns how many habits were exported. */
    suspend fun export(context: Context, uri: Uri): Int {
        val document = build()
        val json = BackupJson.encode(document)
        val stream = context.contentResolver.openOutputStream(uri)
            ?: error("无法打开所选文件")
        stream.bufferedWriter(Charsets.UTF_8).use { it.write(json) }
        return document.habits.size
    }

    suspend fun build(): BackupDocument {
        val habits = database.habitDao().findAll()
        val habitIds = habits.map { it.id }
        // Room renders an empty IN () as invalid SQL on some SQLite builds; skip the query.
        val schedules = if (habitIds.isEmpty()) {
            emptyList()
        } else {
            database.habitScheduleDao().findAllForHabits(habitIds)
        }
        val logs = database.habitLogDao().findAllForBackup()
        val reminders = database.habitReminderDao().findAllForBackup()
        return BackupDocument(
            exportedAt = clock(),
            habits = habits.map(::toHabit),
            schedules = schedules.map(::toSchedule),
            logs = logs.map(::toLog),
            reminders = reminders.map(::toReminder),
        )
    }

    private fun toHabit(habit: HabitEntity) = BackupHabit(
        id = habit.id,
        name = habit.name,
        icon = habit.icon,
        color = habit.color,
        trackingType = habit.trackingType.name,
        targetValue = habit.targetValue,
        unit = habit.unit,
        createdAt = habit.createdAt,
        updatedAt = habit.updatedAt,
        archivedAt = habit.archivedAt,
        createdOn = habit.createdOn.format(DATE),
        archivedOn = habit.archivedOn?.format(DATE),
    )

    private fun toSchedule(schedule: HabitScheduleEntity) = BackupSchedule(
        id = schedule.id,
        habitId = schedule.habitId,
        frequencyType = schedule.frequencyType.name,
        weekdayMask = schedule.weekdayMask,
        effectiveFrom = schedule.effectiveFrom.format(DATE),
        effectiveUntil = schedule.effectiveUntil?.format(DATE),
        createdAt = schedule.createdAt,
    )

    private fun toLog(log: HabitLogEntity) = BackupLog(
        id = log.id,
        habitId = log.habitId,
        logDate = log.logDate.format(DATE),
        actualValue = log.actualValue,
        targetSnapshot = log.targetSnapshot,
        status = log.status.name,
        completedAt = log.completedAt,
        durationSeconds = log.durationSeconds,
        createdAt = log.createdAt,
        updatedAt = log.updatedAt,
    )

    private fun toReminder(reminder: HabitReminderEntity) = BackupReminder(
        id = reminder.id,
        habitId = reminder.habitId,
        // DATABASE.md 11 fixes HH:mm:ss even though LocalTime would drop zero seconds.
        reminderTime = reminder.reminderTime.format(TIME),
        enabled = reminder.enabled,
        createdAt = reminder.createdAt,
        updatedAt = reminder.updatedAt,
    )

    companion object {
        private val DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

        /** Name offered to the system picker; dated so repeated exports do not collide. */
        fun fileName(today: String): String = "habit-tracker-backup-" + today + ".json"
    }
}
