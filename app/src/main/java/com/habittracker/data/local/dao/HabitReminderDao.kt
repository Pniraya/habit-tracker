package com.habittracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.habittracker.data.local.entity.HabitReminderEntity

@Dao
interface HabitReminderDao {

    @Insert
    suspend fun insert(reminder: HabitReminderEntity): Long

    /** Only the restore path uses this (ADR-019). */
    @Query("DELETE FROM habit_reminders")
    suspend fun deleteAll()

    /** Whole table for the JSON backup (TASK-030); not used by any screen. */
    @Query("SELECT * FROM habit_reminders ORDER BY habit_id ASC, reminder_time ASC")
    suspend fun findAllForBackup(): List<HabitReminderEntity>

    @Query("SELECT * FROM habit_reminders WHERE habit_id = :habitId ORDER BY reminder_time ASC")
    suspend fun findForHabit(habitId: Long): List<HabitReminderEntity>

    /** Active set = enabled and habit not archived (ADR-008). */
    @Query(
        "SELECT r.* FROM habit_reminders r JOIN habits h ON h.id = r.habit_id" +
            " WHERE r.enabled = 1 AND h.archived_on IS NULL ORDER BY r.reminder_time ASC",
    )
    suspend fun findActiveForScheduling(): List<HabitReminderEntity>

    @Query("UPDATE habit_reminders SET enabled = :enabled, updated_at = :updatedAt WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean, updatedAt: Long): Int
}
