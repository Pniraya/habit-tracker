package com.habittracker.domain.repository

import com.habittracker.data.local.entity.FrequencyType
import com.habittracker.domain.model.DailyLog
import com.habittracker.domain.model.Habit
import com.habittracker.domain.model.HabitSchedule
import com.habittracker.domain.model.NewHabit
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Domain-facing persistence contract (ARCHITECTURE.md 3.3): domain defines it, data implements it,
 * and no Room type leaks through the interface.
 */
interface HabitRepository {

    /**
     * Persists the habit and its first schedule version.
     *
     * Contract: [draft] must already be validated and normalised by `CreateHabitUseCase`
     * (`HabitRules.validateName` + `HabitRules.normalizeTarget`). Implementations persist it as
     * given and must not add their own validation: a rule duplicated here drifts from the use case
     * and silently changes behaviour for other implementations.
     */
    suspend fun createHabit(draft: NewHabit, today: LocalDate, now: Long): Long

    fun observeActiveHabits(): Flow<List<Habit>>

    /** Archived habits, so an accidental archive can be undone from the profile page. */
    fun observeArchivedHabits(): Flow<List<Habit>>

    suspend fun getHabit(habitId: Long): Habit?

    /**
     * Edits name/target/unit. Contract: [name] and [target] are already validated and normalised by
     * the use case. Never rewrites `habit_logs.target_snapshot`, so past completion rates stay true.
     * Returns false when the habit does not exist or is archived.
     */
    suspend fun updateHabit(habitId: Long, name: String, targetValue: Double, unit: String?, now: Long): Boolean

    /** Archive keeps every child row and never rewrites habit_reminders.enabled (ADR-008). */
    suspend fun archiveHabit(habitId: Long, today: LocalDate, now: Long)

    suspend fun restoreHabit(habitId: Long, now: Long)

    /**
     * Permanent delete: the habit row plus its schedules, logs and reminders go away through
     * ON DELETE CASCADE (DATABASE.md 9). Callers must have confirmed with the user first.
     */
    suspend fun deleteHabit(habitId: Long)

    /** Closes the open version and opens a new one in a single transaction (DATABASE.md 4.2). */
    suspend fun changeSchedule(
        habitId: Long,
        frequencyType: FrequencyType,
        weekdayMask: Int?,
        effectiveFrom: LocalDate,
        now: Long,
    )

    /** Returns the schedule version valid on [date], or null when the date is not covered. */
    suspend fun activeSchedule(habitId: Long, date: LocalDate): HabitSchedule?

    suspend fun dailyLog(habitId: Long, date: LocalDate): DailyLog?

    /** Every schedule version of the habit, oldest first: streaks must use the version in force then. */
    suspend fun scheduleVersions(habitId: Long): List<HabitSchedule>

    /** Logs of one habit in a closed date range, for streaks and later for statistics. */
    suspend fun logsInRange(habitId: Long, from: LocalDate, to: LocalDate): List<DailyLog>

    /**
     * Batch forms of the four per-habit reads above, for the Today list.
     *
     * Each one answers for every id at once so a list of 20 habits costs a constant number of
     * queries instead of 4 per habit. Keys are habit ids; a missing key means "no row", which is
     * why the maps do not use a nullable value type.
     *
     * Deliberately **abstract**: a loop-shaped default here would silently restore the N+1 a
     * future implementation forgot to override, and nothing in this project can construct the Room
     * implementation in a JVM test, so no test could notice. A missing override must be a compile
     * error instead (TASK-022 review, F1).
     */
    suspend fun activeSchedules(habitIds: List<Long>, date: LocalDate): Map<Long, HabitSchedule>

    suspend fun dailyLogs(habitIds: List<Long>, date: LocalDate): Map<Long, DailyLog>

    suspend fun scheduleVersionsFor(habitIds: List<Long>): Map<Long, List<HabitSchedule>>

    suspend fun logsInRangeFor(
        habitIds: List<Long>,
        from: LocalDate,
        to: LocalDate,
    ): Map<Long, List<DailyLog>>

    /** Writes the single daily row with status and completedAt computed by the domain rule. */
    suspend fun recordDailyValue(habitId: Long, date: LocalDate, value: Double, now: Long): DailyLog
}
