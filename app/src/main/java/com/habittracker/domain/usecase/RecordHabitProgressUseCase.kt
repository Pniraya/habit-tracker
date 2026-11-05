package com.habittracker.domain.usecase

import com.habittracker.data.local.entity.TrackingType
import com.habittracker.domain.calculation.ScheduleRules
import com.habittracker.domain.model.DailyLog
import com.habittracker.domain.repository.HabitRepository
import java.time.LocalDate

/**
 * The single write path for daily progress (ARCHITECTURE.md 5.2): checks that the habit exists,
 * is not archived and that the date really is a scheduled day before delegating the write.
 *
 * Status and completedAt are never passed in; the repository derives both from LogStatusRules.
 */
class RecordHabitProgressUseCase(private val repository: HabitRepository) {

    suspend operator fun invoke(habitId: Long, date: LocalDate, value: Double, now: Long): DailyLog {
        val habit = requireNotNull(repository.getHabit(habitId)) { "habit not found" }
        require(habit.archivedOn == null) { "archived habits cannot record progress" }

        val schedule = requireNotNull(repository.activeSchedule(habitId, date)) {
            "no schedule version covers " + date
        }
        require(ScheduleRules.isScheduledOn(schedule.frequencyType, schedule.weekdayMask, date)) {
            date.toString() + " is not a scheduled day for this habit"
        }

        require(value >= 0.0) { "value must not be negative" }
        if (habit.trackingType == TrackingType.BOOLEAN) {
            require(value == 0.0 || value == 1.0) { "a BOOLEAN habit only accepts 0 or 1" }
        }

        return repository.recordDailyValue(habitId, date, value, now)
    }
}
