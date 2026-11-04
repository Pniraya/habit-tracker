package com.habittracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.habittracker.data.local.entity.HabitScheduleEntity
import java.time.LocalDate

/**
 * Schedule versions are append-only. Callers must go through the schedule writer that closes the
 * previous version in the same transaction (DATABASE.md 4.2); this DAO only exposes primitives.
 */
@Dao
interface HabitScheduleDao {

    @Insert
    suspend fun insert(schedule: HabitScheduleEntity): Long

    /** Returns every version valid on [date]; more than one row is a data integrity error. */
    @Query(
        "SELECT * FROM habit_schedules WHERE habit_id = :habitId" +
            " AND effective_from <= :date" +
            " AND (effective_until IS NULL OR :date <= effective_until)",
    )
    suspend fun findVersionsOn(habitId: Long, date: LocalDate): List<HabitScheduleEntity>

    @Query("SELECT * FROM habit_schedules WHERE habit_id = :habitId ORDER BY effective_from ASC")
    suspend fun findAll(habitId: Long): List<HabitScheduleEntity>

    @Query("SELECT * FROM habit_schedules WHERE habit_id = :habitId AND effective_until IS NULL")
    suspend fun findOpenVersion(habitId: Long): HabitScheduleEntity?

    @Query("UPDATE habit_schedules SET effective_until = :until WHERE id = :id AND effective_until IS NULL")
    suspend fun closeVersion(id: Long, until: LocalDate): Int
}
