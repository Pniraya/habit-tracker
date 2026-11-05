package com.habittracker.feature.create

import com.habittracker.data.local.entity.FrequencyType
import com.habittracker.data.local.entity.TrackingType
import com.habittracker.domain.calculation.ScheduleRules
import com.habittracker.domain.model.NewHabit
import com.habittracker.domain.model.NewSchedule
import com.habittracker.domain.usecase.CreateHabitUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Form state machine for creating a habit. The use case stays the single source of truth for the
 * rules; the presenter only produces immediate feedback and never calls the repository itself.
 */
class CreateHabitPresenter(
    // A function rather than the use case itself so a test can inject a failing implementation and
    // cover the onFailure path; production wires CreateHabitUseCase::invoke.
    private val createHabit: suspend (NewHabit, LocalDate, Long) -> Long,
    private val scope: CoroutineScope,
    private val today: () -> LocalDate = { LocalDate.now() },
    private val now: () -> Long = { System.currentTimeMillis() },
) {

    private val _form = MutableStateFlow(CreateHabitForm())
    val form: StateFlow<CreateHabitForm> = _form.asStateFlow()

    fun onNameChange(value: String) = _form.update { it.copy(name = value, error = null) }

    fun onTrackingTypeChange(type: TrackingType) = _form.update {
        // Switching to BOOLEAN hides the unit field, so the unit must be cleared here: otherwise the
        // form would be unsaveable (normalizeTarget rejects a BOOLEAN with a unit) and the user
        // would have no visible field to fix it.
        val keepNumeric = type == TrackingType.NUMERIC
        it.copy(
            trackingType = type,
            error = null,
            targetValue = if (keepNumeric) it.targetValue else "",
            unit = if (keepNumeric) it.unit else "",
        )
    }

    fun onTargetValueChange(value: String) = _form.update { it.copy(targetValue = value, error = null) }

    fun onUnitChange(value: String) = _form.update { it.copy(unit = value, error = null) }

    fun onFrequencyChange(frequency: FrequencyType) = _form.update {
        it.copy(frequency = frequency, error = null, weekdays = if (frequency == FrequencyType.CUSTOM) it.weekdays else emptySet())
    }

    fun onWeekdayToggle(day: DayOfWeek) = _form.update {
        val next = if (it.weekdays.contains(day)) it.weekdays - day else it.weekdays + day
        it.copy(weekdays = next, error = null)
    }

    /** Validates locally, then hands the draft to the use case; a rejection keeps the form open. */
    fun save() {
        val form = _form.value
        if (form.isSaving) return

        val error = validate(form)
        if (error != null) {
            _form.update { it.copy(error = error) }
            return
        }

        _form.update { it.copy(isSaving = true, error = null) }
        scope.launch {
            runCatching { createHabit(draft(form), today(), now()) }
                .onSuccess { id -> _form.update { it.copy(isSaving = false, createdHabitId = id) } }
                .onFailure { failure ->
                    // Never surface a raw domain message: HabitRules speaks English.
                    _form.update { it.copy(isSaving = false, error = messageFor(failure)) }
                }
        }
    }

    private fun validate(form: CreateHabitForm): String? = when {
        form.name.isBlank() -> BLANK_NAME
        form.trackingType == TrackingType.NUMERIC && parseTarget(form) == null -> BAD_TARGET
        form.trackingType == TrackingType.NUMERIC && form.unit.isBlank() -> NO_UNIT
        form.frequency == FrequencyType.CUSTOM && form.weekdays.isEmpty() -> NO_WEEKDAYS
        else -> null
    }

    private fun messageFor(failure: Throwable): String = errorMessageFor(failure)

    private fun parseTarget(form: CreateHabitForm): Double? =
        form.targetValue.trim().toDoubleOrNull()?.takeIf { it > 0.0 }

    private fun maskFor(form: CreateHabitForm): Int? = when (form.frequency) {
        FrequencyType.CUSTOM -> form.weekdays.fold(0) { mask, day -> mask or ScheduleRules.bitFor(day) }
        FrequencyType.DAILY -> ScheduleRules.ALL_DAYS_MASK
        FrequencyType.WEEKDAYS -> ScheduleRules.WEEKDAYS_MASK
    }

    private fun draft(form: CreateHabitForm): NewHabit = NewHabit(
        name = form.name.trim(),
        icon = DEFAULT_ICON,
        color = DEFAULT_COLOR,
        trackingType = form.trackingType,
        targetValue = if (form.trackingType == TrackingType.NUMERIC) parseTarget(form) ?: 1.0 else 1.0,
        unit = if (form.trackingType == TrackingType.NUMERIC) form.unit.trim().ifBlank { null } else null,
        initialSchedule = NewSchedule(frequencyType = form.frequency, weekdayMask = maskFor(form)),
    )

    companion object {
        const val BLANK_NAME: String = "名称不能为空"
        const val BAD_TARGET: String = "目标值必须是大于 0 的数字"
        const val NO_WEEKDAYS: String = "自定义频率至少要选一天"
        const val NO_UNIT: String = "数值习惯需要填写单位"
        const val NO_UNIT_UNEXPECTED: String = "「完成 / 未完成」的习惯不能填单位"
        const val SAVE_FAILED: String = "保存失败，请检查输入"
        const val DEFAULT_ICON: String = "star"
        const val DEFAULT_COLOR: String = "#5B8C6A"
    }
}

/**
 * Maps a domain rejection to something the user can read. The domain speaks English (`require`
 * messages from HabitRules), so `Throwable.message` must never reach the UI directly; an unmapped
 * failure falls back to a generic line. Top-level and internal so it is testable on the JVM.
 */
internal fun errorMessageFor(failure: Throwable): String {
    val raw = failure.message.orEmpty()
    return when {
        raw.contains("must not be blank") -> CreateHabitPresenter.BLANK_NAME
        raw.contains("needs a unit") -> CreateHabitPresenter.NO_UNIT
        raw.contains("must not have a unit") -> CreateHabitPresenter.NO_UNIT_UNEXPECTED
        raw.contains("must be positive") -> CreateHabitPresenter.BAD_TARGET
        raw.contains("must be exactly 1") -> CreateHabitPresenter.BAD_TARGET
        else -> CreateHabitPresenter.SAVE_FAILED
    }
}
