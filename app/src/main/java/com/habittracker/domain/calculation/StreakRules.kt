package com.habittracker.domain.calculation

import com.habittracker.domain.model.HabitSchedule
import java.time.LocalDate

/**
 * Streak rules (REQUIREMENTS 32/33/93): a streak is measured in *scheduled* days, so a day the
 * habit was never due neither adds to the count nor breaks it. Walking back over natural days would
 * turn a Mon/Wed/Fri habit into a false miss every Tuesday.
 *
 * Pure: no database, no clock. The caller supplies the schedule versions that were effective in the
 * past, the dates that were completed, and the date treated as today.
 */
object StreakRules {

    data class Streak(val current: Int, val longest: Int) {
        companion object {
            val EMPTY = Streak(current = 0, longest = 0)
        }
    }

    /**
     * [completedDates] must already mean "this habit counted as done on that date"; how a numeric
     * habit reaches that state is LogStatusRules' business, not this one's.
     */
    fun compute(
        schedules: List<HabitSchedule>,
        completedDates: Set<LocalDate>,
        today: LocalDate,
    ): Streak {
        if (schedules.isEmpty()) return Streak.EMPTY
        val firstDay = schedules.minOf { it.effectiveFrom }
        if (firstDay.isAfter(today)) return Streak.EMPTY

        fun due(date: LocalDate): Boolean = schedules.any { schedule ->
            !date.isBefore(schedule.effectiveFrom) &&
                (schedule.effectiveUntil == null || !date.isAfter(schedule.effectiveUntil)) &&
                ScheduleRules.isScheduledOn(schedule.frequencyType, schedule.weekdayMask, date)
        }

        // Longest: sweep forward once over every day the habit existed.
        var longest = 0
        var run = 0
        var day = firstDay
        while (!day.isAfter(today)) {
            if (due(day)) {
                if (completedDates.contains(day)) {
                    run++
                    if (run > longest) longest = run
                } else if (day != today) {
                    // Today is not over yet, so an incomplete today does not break the run.
                    run = 0
                }
            }
            day = day.plusDays(1)
        }

        // Current: walk back from today (or from the last scheduled day) until a due day is missed.
        var current = 0
        day = today
        while (!day.isBefore(firstDay)) {
            if (due(day)) {
                if (completedDates.contains(day)) {
                    current++
                } else if (day != today) {
                    break
                }
            }
            day = day.minusDays(1)
        }

        return Streak(current = current, longest = longest)
    }
}
