package com.habittracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.habittracker.data.local.entity.HabitLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Deliberately has no `setStatus` / `setCompletedAt` (DATABASE.md 4.3, ADR-006): the log is
 * written as a whole record computed by the domain layer, so status and completed_at cannot
 * drift apart.
 */
@Dao
interface HabitLogDao {

    @Upsert
    suspend fun upsert(log: HabitLogEntity): Long

    @Query("SELECT * FROM habit_logs WHERE habit_id = :habitId AND log_date = :date")
    suspend fun findByDate(habitId: Long, date: LocalDate): HabitLogEntity?

    @Query(
        "SELECT * FROM habit_logs WHERE habit_id = :habitId" +
            " AND log_date BETWEEN :from AND :to ORDER BY log_date ASC",
    )
    suspend fun findRange(habitId: Long, from: LocalDate, to: LocalDate): List<HabitLogEntity>

    @Query(
        "SELECT * FROM habit_logs WHERE log_date BETWEEN :from AND :to ORDER BY log_date ASC",
    )
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<HabitLogEntity>>
}
