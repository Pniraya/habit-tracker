package com.habittracker.feature.today

import com.habittracker.data.local.entity.TrackingType
import com.habittracker.domain.calculation.DayCutoff
import com.habittracker.domain.calculation.ScheduleRules
import com.habittracker.domain.model.DailyLog
import com.habittracker.domain.model.Habit
import com.habittracker.domain.model.HabitSchedule
import com.habittracker.domain.repository.HabitRepository
import com.habittracker.domain.usecase.ArchiveHabitUseCase
import com.habittracker.domain.usecase.DeleteHabitUseCase
import com.habittracker.domain.usecase.GetStreakUseCase
import com.habittracker.domain.usecase.RecordHabitProgressUseCase
import com.habittracker.domain.usecase.UpdateHabitUseCase
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
    private val archiveHabit: ArchiveHabitUseCase,
    private val updateHabit: UpdateHabitUseCase,
    private val scope: CoroutineScope,
    // Defaults to the real use case so existing call sites (and tests) keep working; the
    // ViewModel passes it explicitly for readability.
    private val deleteHabit: DeleteHabitUseCase = DeleteHabitUseCase(repository),
    private val today: () -> LocalDate = { DayCutoff.today() },
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

    /**
     * Archives the habit and rebuilds the list. The repository's Flow also removes the row, but the
     * snapshot is rebuilt here so the UI does not wait for an emission: a test can assert the row is
     * gone right after the call returns.
     */
    fun archive(habitId: Long) {
        scope.launch {
            runCatching { archiveHabit(habitId, today(), now()) }
                .onSuccess {
                    _lastError.value = null
                    habits = habits.filterNot { it.id == habitId }
                    _state.value = snapshot(habits)
                }
                .onFailure { _lastError.value = it.message ?: "无法归档" }
        }
    }

    /**
     * Permanently deletes the habit and rebuilds the list, same local-first pattern as [archive].
     * The UI asks for confirmation before calling this (DATABASE.md 9).
     */
    fun delete(habitId: Long) {
        scope.launch {
            runCatching { deleteHabit(habitId) }
                .onSuccess {
                    _lastError.value = null
                    habits = habits.filterNot { it.id == habitId }
                    _state.value = snapshot(habits)
                }
                .onFailure { _lastError.value = it.message ?: "无法删除" }
        }
    }

    /** Edits the fields the dialog exposes; validation lives in `UpdateHabitUseCase`. */
    fun edit(habitId: Long, name: String, targetValue: Double, unit: String?) {
        scope.launch {
            runCatching { updateHabit(habitId, name, targetValue, unit, now()) }
                .onSuccess {
                    _lastError.value = null
                    habits = habits.map { if (it.id == habitId) it.copy(name = name.trim(), targetValue = targetValue, unit = unit) else it }
                    _state.value = snapshot(habits)
                }
                .onFailure { _lastError.value = it.message ?: "无法保存" }
        }
    }

    fun dismissError() {
        _lastError.value = null
    }

    /**
     * Rebuilds the list against the current date and clock. The date is read when the snapshot is
     * taken, so an app left open across midnight would otherwise keep showing yesterday: nothing
     * changes in the database at 00:00, so no Flow emission arrives to trigger a rebuild.
     */
    fun refresh() {
        scope.launch {
            runCatching { snapshot(habits) }
                .onSuccess { _state.value = it }
                .onFailure { _lastError.value = it.message ?: "无法刷新" }
        }
    }

    /**
     * Four batch queries per snapshot, whatever the number of habits. It used to be four *per
     * habit*, plus a second full rebuild after every write (TASK-022).
     */
    private suspend fun snapshot(source: List<Habit>): TodayUiState {
        val date = today()
        if (source.isEmpty()) return TodayUiState(date = date, habits = emptyList(), isLoading = false)

        val ids = source.map { it.id }
        val schedulesToday = repository.activeSchedules(ids, date)
        val logsToday = repository.dailyLogs(ids, date)
        val versions = repository.scheduleVersionsFor(ids)
        val firstDay = versions.values.flatten().minOfOrNull { it.effectiveFrom } ?: date
        val logsSince = repository.logsInRangeFor(ids, minOf(firstDay, date), date)

        return TodayUiState(
            date = date,
            habits = source.map { habit ->
                describe(
                    habit = habit,
                    date = date,
                    schedule = schedulesToday[habit.id],
                    log = logsToday[habit.id],
                    versions = versions[habit.id].orEmpty(),
                    logs = logsSince[habit.id].orEmpty(),
                )
            },
            isLoading = false,
        )
    }

    private fun describe(
        habit: Habit,
        date: LocalDate,
        schedule: HabitSchedule?,
        log: DailyLog?,
        versions: List<HabitSchedule>,
        logs: List<DailyLog>,
    ): TodayHabit {
        val scheduled = schedule != null &&
            ScheduleRules.isScheduledOn(schedule.frequencyType, schedule.weekdayMask, date)
        val streak = GetStreakUseCase.fromLoaded(versions, logs, date)
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
            currentStreak = streak.current,
            longestStreak = streak.longest,
        )
    }

    companion object {
        const val NUMERIC_NEEDS_INPUT: String = "数值习惯需要输入数值，暂不支持一键完成"

        /** True when the row is finished, so a second tap should undo rather than repeat. */
        fun undoHint(isCompleted: Boolean): String? = if (isCompleted) "再点一下撤销" else null

        /**
         * Which lifecycle event rebuilds the list. The decision lives here, not in the composable,
         * so it can be tested on the JVM: the wiring in AppRoot can otherwise be broken without a
         * single test noticing (TASK-024 review, M-H).
         */
        fun refreshesOn(event: String): Boolean = event == "ON_RESUME"

        /**
         * Milliseconds until the next day change after [now]. The day changes at 04:00, not at
         * midnight (ADR-016), so the header must not flip while the user is still logging "yesterday".
         */
        fun millisUntilNextDay(now: java.time.LocalDateTime): Long =
            DayCutoff.millisUntilNextCutoff(now)

        /**
         * The create action is always reachable. The empty state renders its own button inside the
         * body, so the floating action only appears once there is a list to sit above; the rule lives
         * here rather than in the composable so it can be tested on the JVM.
         */
        fun showsCreateAction(state: TodayUiState): Boolean = !state.isLoading && state.habits.isNotEmpty()

        /**
         * "0.5 / 2000 ml". Formatting used to sit in the composable and cast to Long, so a recorded
         * 0.5 rendered as "0"; it lives here now so the JVM tests can pin the decimal behaviour.
         */
        fun progressText(actual: Double, target: Double, unit: String?): String {
            val suffix = if (unit.isNullOrBlank()) "" else " " + unit
            return formatQuantity(actual) + " / " + formatQuantity(target) + suffix
        }

        /**
         * "连续 3 天 · 最长 7 天". Wording stays factual: REQUIREMENTS 71 forbids wording that
         * pressures the user, so a missed streak is simply absent rather than announced.
         */
        fun streakText(current: Int, longest: Int): String? = when {
            current <= 0 && longest <= 0 -> null
            current <= 0 -> "最长 " + longest + " 天"
            current >= longest -> "连续 " + current + " 天"
            else -> "连续 " + current + " 天 · 最长 " + longest + " 天"
        }

        /** Drops the trailing ".0" for whole numbers and keeps decimals otherwise. */
        fun formatQuantity(value: Double): String =
            java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

        /**
         * What the numeric dialog puts in the field when it opens. Casting to Long here showed 0.5
         * as "0" and one tap on 记录 then overwrote the stored 0.5 with 0.
         */
        fun prefillText(actual: Double): String =
            if (actual > 0.0) formatQuantity(actual) else ""
    }
}

// Leading group is 1-3 digits and non-zero: "0,123" is a legitimate 0.123, not a thousands group.
private val THOUSANDS_SEPARATOR = Regex("^[1-9]\\d{0,2}(,\\d{3})+$")

/**
 * Some IMEs emit a comma for the decimal key, so a comma has to become a dot before
 * [toDoubleOrNull] sees it. A value that looks like a thousands group (1,234) is left alone:
 * rewriting it to 1.234 would silently divide the number by a thousand.
 */
internal fun normaliseQuantityInput(raw: String): String =
    if (THOUSANDS_SEPARATOR.matches(raw)) raw else raw.replace(',', '.')
