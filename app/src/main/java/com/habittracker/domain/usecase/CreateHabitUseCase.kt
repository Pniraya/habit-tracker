package com.habittracker.domain.usecase

import com.habittracker.domain.calculation.HabitRules
import com.habittracker.domain.model.NewHabit
import com.habittracker.domain.repository.HabitRepository
import java.time.LocalDate

/** Creates a habit together with its first schedule version (DATABASE.md 4.2). */
class CreateHabitUseCase(private val repository: HabitRepository) {

    suspend operator fun invoke(draft: NewHabit, today: LocalDate, now: Long): Long {
        HabitRules.validateName(draft.name)
        return repository.createHabit(draft, today, now)
    }
}
