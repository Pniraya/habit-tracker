package com.habittracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalTime

@Entity(
    tableName = "habit_reminders",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["habit_id"], name = "index_habit_reminders_habit_id"),
        Index(value = ["habit_id", "reminder_time"], unique = true),
    ],
)
data class HabitReminderEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "habit_id") val habitId: Long,
    /** Local wall-clock time, stored as ISO HH:mm:ss. */
    @ColumnInfo(name = "reminder_time") val reminderTime: LocalTime,
    @ColumnInfo(name = "enabled", defaultValue = "1") val enabled: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
