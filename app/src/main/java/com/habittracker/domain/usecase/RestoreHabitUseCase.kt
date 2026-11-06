package com.habittracker.domain.usecase

import com.habittracker.domain.repository.HabitRepository

/** Undoes an archive. History is untouched: only the archive flags are cleared (ADR-008). */
class RestoreHabitUseCase(private val repository: HabitRepository) {

    suspend operator fun invoke(habitId: Long, now: Long) {
        val habit = requireNotNull(repository.getHabit(habitId)) { "habit not found" }
        require(habit.archivedOn != null) { "habit is not archived" }
        repository.restoreHabit(habitId, now)
    }
}