package com.habittracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.habittracker.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HabitDao {

    @Insert
    suspend fun insert(habit: HabitEntity): Long

    @Update
    suspend fun update(habit: HabitEntity)

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun findById(id: Long): HabitEntity?

    @Query("SELECT * FROM habits WHERE archived_on IS NULL ORDER BY created_at ASC")
    fun observeActive(): Flow<List<HabitEntity>>

    /** Archived rows, newest archive first, so the profile page can offer a restore (TASK-028). */
    @Query("SELECT * FROM habits WHERE archived_on IS NOT NULL ORDER BY archived_at DESC")
    fun observeArchived(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY created_at ASC")
    suspend fun findAll(): List<HabitEntity>

    /** Archive keeps every child row; reminders keep their enabled flag (ADR-008). */
    @Query(
        "UPDATE habits SET archived_at = :archivedAt, archived_on = :archivedOn," +
            " updated_at = :updatedAt WHERE id = :id AND archived_on IS NULL",
    )
    suspend fun archive(id: Long, archivedAt: Long, archivedOn: LocalDate, updatedAt: Long): Int

    @Query(
        "UPDATE habits SET archived_at = NULL, archived_on = NULL, updated_at = :updatedAt" +
            " WHERE id = :id",
    )
    suspend fun restore(id: Long, updatedAt: Long): Int

    /**
     * Edits the editable fields only (TASK-029). `tracking_type` is deliberately absent: switching
     * a habit between BOOLEAN and NUMERIC would reinterpret every historical log. The past logs keep
     * their own `target_snapshot`, which is why this statement never touches `habit_logs`.
     */
    @Query(
        "UPDATE habits SET name = :name, target_value = :targetValue, unit = :unit," +
            " updated_at = :updatedAt WHERE id = :id AND archived_on IS NULL",
    )
    suspend fun updateFields(
        id: Long,
        name: String,
        targetValue: Double,
        unit: String?,
        updatedAt: Long,
    ): Int
}
