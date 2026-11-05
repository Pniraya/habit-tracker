package com.habittracker.domain.usecase

import com.habittracker.data.local.entity.LogStatus
import com.habittracker.domain.calculation.StreakRules
import com.habittracker.domain.model.DailyLog
import com.habittracker.domain.model.HabitSchedule
import com.habittracker.domain.repository.HabitRepository
import java.time.LocalDate

/**
 * Loads what [StreakRules] needs and nothing else: the schedule versions, and the logs from the
 * first schedule version up to today. The rules stay pure so the date arithmetic can be tested on
 * the JVM without a database.
 */
class GetStreakUseCase(private val repository: HabitRepository) {

    suspend operator fun invoke(habitId: Long, today: LocalDate): StreakRules.Streak {
        val schedules = repository.scheduleVersions(habitId)
        if (schedules.isEmpty()) return StreakRules.Streak.EMPTY
        val from = schedules.minOf { it.effectiveFrom }
        if (from.isAfter(today)) return StreakRules.Streak.EMPTY
        val logs = repository.logsInRange(habitId, from, today)
        return fromLoaded(schedules, logs, today)
    }

    companion object {
        /**
         * Same rule over rows that are already loaded, so the Today list can fetch every habit in a
         * constant number of batch queries and still share one implementation of the rule.
         */
        fun fromLoaded(
            schedules: List<HabitSchedule>,
            logs: List<DailyLog>,
            today: LocalDate,
        ): StreakRules.Streak {
            if (schedules.isEmpty()) return StreakRules.Streak.EMPTY
            val firstDay = schedules.minOf { it.effectiveFrom }
            if (firstDay.isAfter(today)) return StreakRules.Streak.EMPTY
            val completed = logs
                .filter { it.status == LogStatus.COMPLETED && !it.date.isBefore(firstDay) && !it.date.isAfter(today) }
                .map { it.date }
                .toSet()
            return StreakRules.compute(schedules, completed, today)
        }
    }
}
