package com.habittracker.domain.usecase

import com.habittracker.domain.repository.HabitRepository

/**
 * Permanent delete (DATABASE.md 9). Archive is the default way to drop a habit; this exists for the
 * explicit "delete" action and is only reachable behind a second confirmation in the UI.
 */
class DeleteHabitUseCase(private val repository: HabitRepository) {

    suspend operator fun invoke(habitId: Long) {
        requireNotNull(repository.getHabit(habitId)) { "habit not found" }
        repository.deleteHabit(habitId)
    }
}
