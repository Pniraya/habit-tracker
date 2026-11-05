package com.habittracker.domain.usecase

import com.habittracker.domain.calculation.HabitRules
import com.habittracker.domain.model.NewHabit
import com.habittracker.domain.repository.HabitRepository
import java.time.LocalDate

/**
 * Creates a habit together with its first schedule version (DATABASE.md 4.2).
 *
 * Validation and normalisation live here, not in the repository: a rule kept in the data layer has
 * to be repeated by every implementation, and one implementation missing it is a silent behaviour
 * difference. The repository receives a draft that already satisfies the contract (see
 * [HabitRepository.createHabit]); the database triggers remain the final backstop.
 */
class CreateHabitUseCase(private val repository: HabitRepository) {

    suspend operator fun invoke(draft: NewHabit, today: LocalDate, now: Long): Long {
        HabitRules.validateName(draft.name)
        val target = HabitRules.normalizeTarget(draft.trackingType, draft.targetValue, draft.unit)
        return repository.createHabit(
            draft.copy(
                name = draft.name.trim(),
                targetValue = target.targetValue,
                unit = target.unit,
            ),
            today,
            now,
        )
    }
}
