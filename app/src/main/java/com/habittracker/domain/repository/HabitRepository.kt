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

    suspend fun getHabit(habitId: Long): Habit?

    /** Archive keeps every child row and never rewrites habit_reminders.enabled (ADR-008). */
    suspend fun archiveHabit(habitId: Long, today: LocalDate, now: Long)

    suspend fun restoreHabit(habitId: Long, now: Long)

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

    /** Writes the single daily row with status and completedAt computed by the domain rule. */
    suspend fun recordDailyValue(habitId: Long, date: LocalDate, value: Double, now: Long): DailyLog
}
