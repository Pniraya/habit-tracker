package com.habittracker.domain.usecase

import com.habittracker.domain.calculation.HabitRules
import com.habittracker.domain.repository.HabitRepository

/**
 * The single write path for edits (TASK-029). Validation is shared with creation, so a name or a
 * target that the create form would refuse cannot sneak in through the edit dialog.
 */
class UpdateHabitUseCase(private val repository: HabitRepository) {

    suspend operator fun invoke(habitId: Long, name: String, targetValue: Double, unit: String?, now: Long) {
        val habit = requireNotNull(repository.getHabit(habitId)) { "habit not found" }
        require(habit.archivedOn == null) { "archived habits cannot be edited" }
        HabitRules.validateName(name)
        val target = HabitRules.normalizeTarget(habit.trackingType, targetValue, unit)
        val updated = repository.updateHabit(habitId, name.trim(), target.targetValue, target.unit, now)
        check(updated) { "habit not found" }
    }
}