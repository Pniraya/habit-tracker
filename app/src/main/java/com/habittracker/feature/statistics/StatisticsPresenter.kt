package com.habittracker.feature.statistics

import com.habittracker.data.local.entity.LogStatus
import com.habittracker.domain.calculation.ScheduleRules
import com.habittracker.domain.model.DailyLog
import com.habittracker.domain.model.Habit
import com.habittracker.domain.model.HabitSchedule
import com.habittracker.domain.repository.HabitRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/** One habit's last-N-days summary. The denominator is scheduled days, never calendar days. */
data class HabitStat(
    val habitId: Long,
    val name: String,
    val scheduledDays: Int,
    val completedDays: Int,
) {
    val rate: Double get() = if (scheduledDays == 0) 0.0 else completedDays.toDouble() / scheduledDays
}

data class StatisticsUiState(
    val from: LocalDate = LocalDate.MIN,
    val to: LocalDate = LocalDate.MIN,
    val stats: List<HabitStat> = emptyList(),
    val isLoading: Boolean = true,
)

/**
 * Last-7-days completion per habit (F-010 subset). Pure Kotlin so the JVM tests can pin the
 * denominator rule: a WEEKDAYS habit must not be punished for Saturday, and an unscheduled day is
 * neither a success nor a failure.
 */
class StatisticsPresenter(
    private val repository: HabitRepository,
    private val scope: CoroutineScope,
    private val today: () -> LocalDate = { LocalDate.now() },
) {

    private val _state = MutableStateFlow(StatisticsUiState())
    val state: StateFlow<StatisticsUiState> = _state.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        scope.launch {
            runCatching { load() }
                .onSuccess { _lastError.value = null; _state.value = it }
                .onFailure { _lastError.value = it.message ?: "无法统计" }
        }
    }

    private suspend fun load(): StatisticsUiState {
        val to = today()
        val from = to.minusDays(WINDOW_DAYS - 1L)
        val habits = repository.observeActiveHabits().first()
        if (habits.isEmpty()) return StatisticsUiState(from, to, emptyList(), isLoading = false)
        val ids = habits.map { it.id }
        val versions = repository.scheduleVersionsFor(ids)
        val logs = repository.logsInRangeFor(ids, from, to)
        return StatisticsUiState(
            from = from,
            to = to,
            stats = habits.map { habit ->
                summarize(
                    habit = habit,
                    versions = versions[habit.id].orEmpty(),
                    logs = logs[habit.id].orEmpty(),
                    from = from,
                    to = to,
                )
            },
            isLoading = false,
        )
    }

    companion object {
        const val WINDOW_DAYS: Long = 7L

        /**
         * Counts only the days the habit was actually scheduled between [from] and [to]. A completed
         * log on a non-scheduled day is ignored, because it cannot make the rate exceed 100%.
         */
        fun summarize(
            habit: Habit,
            versions: List<HabitSchedule>,
            logs: List<DailyLog>,
            from: LocalDate,
            to: LocalDate,
        ): HabitStat {
            val completedDates = logs.filter { it.status == LogStatus.COMPLETED }.map { it.date }.toSet()
            var scheduled = 0
            var completed = 0
            var day = from
            while (!day.isAfter(to)) {
                val version = versions.firstOrNull { v ->
                    !day.isBefore(v.effectiveFrom) && (v.effectiveUntil == null || !day.isAfter(v.effectiveUntil))
                }
                if (version != null &&
                    ScheduleRules.isScheduledOn(version.frequencyType, version.weekdayMask, day)
                ) {
                    scheduled += 1
                    if (completedDates.contains(day)) completed += 1
                }
                day = day.plusDays(1)
            }
            return HabitStat(habit.id, habit.name, scheduled, completed)
        }

        /** "5 / 7 · 71%". No wording that scolds: REQUIREMENTS 71 forbids pressure language. */
        fun rateText(stat: HabitStat): String =
            stat.completedDays.toString() + " / " + stat.scheduledDays + " · " +
                (stat.rate * 100).toInt() + "%"
    }
}