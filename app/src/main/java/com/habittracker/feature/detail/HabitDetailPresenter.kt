package com.habittracker.feature.detail

import com.habittracker.data.local.entity.LogStatus
import com.habittracker.data.local.entity.TrackingType
import com.habittracker.domain.model.Habit
import com.habittracker.domain.repository.HabitRepository
import com.habittracker.domain.usecase.GetStreakUseCase
import com.habittracker.domain.usecase.UpdateHabitUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HabitDetailUiState(
    val habit: Habit? = null,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val monthCompletions: Int = 0,
    val week: DetailWeek? = null,
    val weeks: List<List<DetailDay>> = emptyList(),
    val stats: DetailStats = DetailStats(0, 0, 0),
    val frequencyLabel: String = "",
    val targetLabel: String = "",
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
)

/**
 * Loads one habit plus everything the detail page draws. Plain Kotlin like the other presenters,
 * so [HabitDetailRulesTest] can exercise the page without Android or a clock.
 */
class HabitDetailPresenter(
    private val habitId: Long,
    private val repository: HabitRepository,
    private val updateHabit: UpdateHabitUseCase,
    private val scope: CoroutineScope,
    private val today: () -> LocalDate = { LocalDate.now() },
    private val now: () -> Long = { System.currentTimeMillis() },
) {

    private val _state = MutableStateFlow(HabitDetailUiState())
    val state: StateFlow<HabitDetailUiState> = _state.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    init {
        load()
    }

    fun load() {
        scope.launch { loadNow() }
    }

    private suspend fun loadNow() {
        val day = today()
        val habit = repository.getHabit(habitId)
        if (habit == null) {
            _state.value = HabitDetailUiState(isLoading = false, notFound = true)
            return
        }
        val versions = repository.scheduleVersions(habitId)
        // The streak walks every day since the first plan version, so the log window must start
        // there; a five-week window would turn older history into fake "missed" days.
        val firstDay = versions.minOfOrNull { it.effectiveFrom } ?: day
        val logs = repository.logsInRange(habitId, firstDay, day)
        val completed = logs.filter { it.status == LogStatus.COMPLETED }.map { it.date }.toSet()
        val streak = GetStreakUseCase.fromLoaded(versions, logs, day)
        val stats = HabitDetailRules.stats(day, versions, completed)
        val currentVersion = versions.firstOrNull {
            !day.isBefore(it.effectiveFrom) && (it.effectiveUntil == null || !day.isAfter(it.effectiveUntil))
        }
        _state.value = HabitDetailUiState(
            habit = habit,
            currentStreak = streak.current,
            longestStreak = streak.longest,
            monthCompletions = stats.monthCompletions,
            week = HabitDetailRules.week(day, versions, completed),
            weeks = HabitDetailRules.fiveWeeks(day, versions, completed),
            stats = stats,
            frequencyLabel = HabitDetailRules.frequencyLabel(currentVersion),
            targetLabel = if (habit.trackingType == TrackingType.NUMERIC) {
                "目标 " + HabitDetailRules.formatAmount(habit.targetValue) + " " + (habit.unit ?: "")
            } else {
                "完成 / 未完成"
            },
            isLoading = false,
        )
    }

    /** The edit pencil: same editable fields as the Today dialog, then reload the page. */
    fun edit(name: String, targetValue: Double, unit: String?) {
        scope.launch {
            runCatching { updateHabit(habitId, name, targetValue, unit, now()) }
                .onSuccess {
                    _lastError.value = null
                    loadNow()
                }
                .onFailure { _lastError.value = it.message ?: "无法保存" }
        }
    }

    fun dismissError() {
        _lastError.value = null
    }
}
