package com.habittracker.domain.usecase

import com.habittracker.data.local.entity.LogStatus
import com.habittracker.domain.calculation.StreakRules
import com.habittracker.domain.model.DailyLog
import com.habittracker.domain.model.HabitSchedule
import java.time.LocalDate

/**
 * Turns already-loaded rows into a streak. Pure, so the date clamping (logs before the first
 * schedule version, or in the future, never count) is testable on the JVM.
 *
 * It used to be a class that also loaded the rows; the Today list now loads everything through
 * batch queries, which left that loader with no caller (TASK-022 review, F6). Loading lives in the
 * repository; the rule lives here.
 */
object GetStreakUseCase {

    fun fromLoaded(
        schedules: List<HabitSchedule>,
        logs: List<DailyLog>,
        today: LocalDate,
    ): StreakRules.Streak {
        if (schedules.isEmpty()) return StreakRules.Streak.EMPTY
        val firstDay = schedules.minOf { it.effectiveFrom }
        if (firstDay.isAfter(today)) return StreakRules.Streak.EMPTY
        val completed = logs
            .filter { it.status == LogStatus.COMPLETED }
            .filter { !it.date.isBefore(firstDay) && !it.date.isAfter(today) }
            .map { it.date }
            .toSet()
        return StreakRules.compute(schedules, completed, today)
    }
}
