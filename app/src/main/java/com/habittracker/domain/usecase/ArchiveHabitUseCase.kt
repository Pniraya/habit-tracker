package com.habittracker.domain.usecase

import com.habittracker.domain.repository.HabitRepository
import java.time.LocalDate

/** Archives a habit; history stays intact and reminders keep their enabled flag (ADR-008). */
class ArchiveHabitUseCase(private val repository: HabitRepository) {

    suspend operator fun invoke(habitId: Long, today: LocalDate, now: Long) {
        val habit = requireNotNull(repository.getHabit(habitId)) { "habit not found" }
        require(habit.archivedOn == null) { "habit is already archived" }
        repository.archiveHabit(habitId, today, now)
    }
}
