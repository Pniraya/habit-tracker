package com.habittracker.data.backup

import androidx.room.withTransaction
import com.habittracker.data.local.HabitDatabase
import com.habittracker.data.local.entity.FrequencyType
import com.habittracker.data.local.entity.HabitEntity
import com.habittracker.data.local.entity.HabitLogEntity
import com.habittracker.data.local.entity.HabitReminderEntity
import com.habittracker.data.local.entity.HabitScheduleEntity
import com.habittracker.data.local.entity.LogStatus
import com.habittracker.data.local.entity.TrackingType
import java.time.LocalDate
import java.time.LocalTime

/**
 * Overwrites the database with a validated backup inside one transaction (ADR-019).
 *
 * The caller must have parsed the file with [BackupParser] first: a failure here would otherwise
 * leave the user with a wiped database and a bad file.
 */
class BackupRestorer(private val database: HabitDatabase) {

    suspend fun restore(document: BackupDocument) {
        database.withTransaction {
            // Children first: habits cascade would delete them anyway, but explicit deletes keep the
            // intent readable and do not depend on foreign-key pragma settings.
            database.habitReminderDao().deleteAll()
            database.habitLogDao().deleteAll()
            database.habitScheduleDao().deleteAll()
            database.habitDao().deleteAll()

            document.habits.forEach { database.habitDao().insert(it.toEntity()) }
            document.schedules.forEach { database.habitScheduleDao().insert(it.toEntity()) }
            document.logs.forEach { database.habitLogDao().upsert(it.toEntity()) }
            document.reminders.forEach { database.habitReminderDao().insert(it.toEntity()) }
        }
    }

    private fun BackupHabit.toEntity() = HabitEntity(
        id = id,
        name = name,
        icon = icon,
        color = color,
        trackingType = TrackingType.valueOf(trackingType),
        targetValue = targetValue,
        unit = unit,
        createdAt = createdAt,
        updatedAt = updatedAt,
        archivedAt = archivedAt,
        createdOn = LocalDate.parse(createdOn),
        archivedOn = archivedOn?.let(LocalDate::parse),
    )

    private fun BackupSchedule.toEntity() = HabitScheduleEntity(
        id = id,
        habitId = habitId,
        frequencyType = FrequencyType.valueOf(frequencyType),
        weekdayMask = weekdayMask,
        effectiveFrom = LocalDate.parse(effectiveFrom),
        effectiveUntil = effectiveUntil?.let(LocalDate::parse),
        createdAt = createdAt,
    )

    private fun BackupLog.toEntity() = HabitLogEntity(
        id = id,
        habitId = habitId,
        logDate = LocalDate.parse(logDate),
        actualValue = actualValue,
        targetSnapshot = targetSnapshot,
        status = LogStatus.valueOf(status),
        completedAt = completedAt,
        durationSeconds = durationSeconds,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun BackupReminder.toEntity() = HabitReminderEntity(
        id = id,
        habitId = habitId,
        reminderTime = LocalTime.parse(reminderTime),
        enabled = enabled,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
