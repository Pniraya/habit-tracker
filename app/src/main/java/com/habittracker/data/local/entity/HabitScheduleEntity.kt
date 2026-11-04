package com.habittracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "habit_schedules",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["habit_id"], name = "index_habit_schedules_habit_id"),
        Index(
            value = ["habit_id", "effective_from", "effective_until"],
            name = "index_habit_schedules_habit_effective",
        ),
    ],
)
data class HabitScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "habit_id") val habitId: Long,
    @ColumnInfo(name = "frequency_type") val frequencyType: FrequencyType,
    /** ISO weekday bit mask, Monday = bit 0. */
    @ColumnInfo(name = "weekday_mask") val weekdayMask: Int,
    @ColumnInfo(name = "effective_from") val effectiveFrom: LocalDate,
    @ColumnInfo(name = "effective_until") val effectiveUntil: LocalDate? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
