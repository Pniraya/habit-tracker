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

    /** Only the restore path uses this (ADR-019). */
    @Query("DELETE FROM habit_logs")
    suspend fun deleteAll()

    /** Whole table for the JSON backup (TASK-030); not used by any screen. */
    @Query("SELECT * FROM habit_logs ORDER BY habit_id ASC, log_date ASC")
    suspend fun findAllForBackup(): List<HabitLogEntity>

    @Query("SELECT * FROM habit_logs WHERE habit_id = :habitId AND log_date = :date")
    suspend fun findByDate(habitId: Long, date: LocalDate): HabitLogEntity?

    @Query(
        "SELECT * FROM habit_logs WHERE habit_id = :habitId" +
            " AND log_date BETWEEN :from AND :to ORDER BY log_date ASC",
    )
    suspend fun findRange(habitId: Long, from: LocalDate, to: LocalDate): List<HabitLogEntity>

    /** Batch form of [findByDate]: the whole Today list in one query. */
    @Query("SELECT * FROM habit_logs WHERE habit_id IN (:habitIds) AND log_date = :date")
    suspend fun findForDate(habitIds: List<Long>, date: LocalDate): List<HabitLogEntity>

    /** Batch form of [findRange]: streaks for the whole list in one query. */
    @Query(
        "SELECT * FROM habit_logs WHERE habit_id IN (:habitIds)" +
            " AND log_date BETWEEN :from AND :to ORDER BY habit_id ASC, log_date ASC",
    )
    suspend fun findRangeForHabits(
        habitIds: List<Long>,
        from: LocalDate,
        to: LocalDate,
    ): List<HabitLogEntity>

    @Query(
        "SELECT * FROM habit_logs WHERE log_date BETWEEN :from AND :to ORDER BY log_date ASC",
    )
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<HabitLogEntity>>
}
