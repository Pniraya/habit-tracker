package com.habittracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * One aggregated row per habit per local day (DATABASE.md 4.3).
 *
 * [status] and [completedAt] are derived by domain rules and written together; the DAO must not
 * expose setters for them in isolation (ADR-006).
 */
@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["habit_id", "log_date"], unique = true),
        Index(value = ["habit_id", "log_date"], name = "index_habit_logs_habit_date"),
        Index(value = ["log_date"], name = "index_habit_logs_log_date"),
    ],
)
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "habit_id") val habitId: Long,
    @ColumnInfo(name = "log_date") val logDate: LocalDate,
    @ColumnInfo(name = "actual_value") val actualValue: Double,
    @ColumnInfo(name = "target_snapshot") val targetSnapshot: Double,
    @ColumnInfo(name = "status") val status: LogStatus,
    @ColumnInfo(name = "completed_at") val completedAt: Long? = null,
    /** V1 does not use this column (ADR-010); it exists for forward compatibility. */
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Int? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
