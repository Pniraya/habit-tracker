package com.habittracker.feature.today

import com.habittracker.data.local.entity.TrackingType
import com.habittracker.domain.calculation.ScheduleRules
import com.habittracker.domain.model.Habit
import com.habittracker.domain.repository.HabitRepository
import com.habittracker.domain.usecase.RecordHabitProgressUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Holds the Today screen state. Deliberately plain Kotlin: Compose only renders it and the JVM tests
 * can drive it without an Android ViewModel or a main dispatcher.
 *
 * The list is rebuilt after every write because the repository exposes habits, not a joined
 * habit+log stream; a future repository query can replace [refresh].
 */
class TodayPresenter(
    private val repository: HabitRepository,
    private val recordProgress: RecordHabitProgressUseCase,
    private val scope: CoroutineScope,
    private val today: () -> LocalDate = { LocalDate.now() },
    private val now: () -> Long = { System.currentTimeMillis() },
) {

    private val _state = MutableStateFlow(TodayUiState(today(), isLoading = true))
    val state: StateFlow<TodayUiState> = _state.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private var habits: List<Habit> = emptyList()

    init {
        scope.launch {
            repository.observeActiveHabits().collect { observed ->
                habits = observed
                _state.value = snapshot(observed)
            }
        }
    }

    /**
     * Only BOOLEAN rows toggle. A numeric habit toggled to 1.0 would silently record a wrong value,
     * so the presenter refuses instead of guessing; numeric input arrives through [record].
     */
    fun toggle(habitId: Long) {
        val habit = _state.value.habits.firstOrNull { it.id == habitId } ?: return
        if (habit.trackingType != TrackingType.BOOLEAN) {
            _lastError.value = NUMERIC_NEEDS_INPUT
            return
        }
        record(habitId, if (habit.isCompleted) 0.0 else 1.0)
    }

    fun record(habitId: Long, value: Double) {
        scope.launch {
            runCatching { recordProgress(habitId, today(), value, now()) }
                .onSuccess { _lastError.value = null; _state.value = snapshot(habits) }
                .onFailure { _lastError.value = it.message ?: "无法记录进度" }
        }
    }

    fun dismissError() {
        _lastError.value = null
    }

    private suspend fun snapshot(source: List<Habit>): TodayUiState {
        val date = today()
        return TodayUiState(
            date = date,
            habits = source.map { habit -> describe(habit, date) },
            isLoading = false,
        )
    }

    private suspend fun describe(habit: Habit, date: LocalDate): TodayHabit {
        val schedule = repository.activeSchedule(habit.id, date)
        val log = repository.dailyLog(habit.id, date)
        val scheduled = schedule != null &&
            ScheduleRules.isScheduledOn(schedule.frequencyType, schedule.weekdayMask, date)
        return TodayHabit(
            id = habit.id,
            name = habit.name,
            icon = habit.icon,
            color = habit.color,
            trackingType = habit.trackingType,
            targetValue = habit.targetValue,
            unit = habit.unit,
            isScheduledToday = scheduled,
            status = log?.status,
            actualValue = log?.actualValue ?: 0.0,
        )
    }

    companion object {
        const val NUMERIC_NEEDS_INPUT: String = "数值习惯需要输入数值，暂不支持一键完成"
    }
}
