package com.habittracker.feature.statistics

import com.habittracker.data.local.entity.LogStatus
import com.habittracker.domain.calculation.ScheduleRules
import com.habittracker.domain.model.DailyLog
import com.habittracker.domain.model.Habit
import com.habittracker.domain.model.HabitSchedule
import com.habittracker.domain.repository.HabitRepository
import com.habittracker.domain.usecase.GetStreakUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** One cell of the month heat map. [day] is null for the weekday padding before the 1st. */
data class MonthCell(
    val day: Int?,
    val level: Int = 0,
    val isToday: Boolean = false,
    val isFuture: Boolean = false,
)

/** One bar of the weekly trend. [fraction] is 0..1 of the tallest bar, so the view stays dumb. */
data class WeekBar(val label: String, val fraction: Float, val isToday: Boolean)

data class StatisticsUiState(
    val monthLabel: String = "",
    val rateText: String = "0%",
    val longestStreakText: String = "0",
    val cells: List<MonthCell> = emptyList(),
    val bars: List<WeekBar> = emptyList(),
    val hasData: Boolean = false,
    val isLoading: Boolean = true,
)

/**
 * Statistics page model (F-010 subset plus the design's heat map). Every rule lives here so the
 * layout stays a pure function of the state and the arithmetic is testable on the JVM.
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
        val date = today()
        val month = YearMonth.from(date)
        val habits = repository.observeActiveHabits().first()
        if (habits.isEmpty()) {
            return StatisticsUiState(
                monthLabel = monthLabel(month),
                cells = monthCells(month, date) { 0 },
                bars = weekBars(date) { 0 },
                isLoading = false,
            )
        }
        val ids = habits.map { it.id }
        val versions = repository.scheduleVersionsFor(ids)
        // Since the oldest version, so the longest streak is not clipped to this month.
        val earliest = minOf(
            versions.values.flatten().minOfOrNull { it.effectiveFrom } ?: month.atDay(1),
            month.atDay(1),
        )
        val logs = repository.logsInRangeFor(ids, earliest, date)

        val summaries = habits.map { habit ->
            summarize(
                versions = versions[habit.id].orEmpty(),
                logs = logs[habit.id].orEmpty(),
                from = month.atDay(1),
                to = date,
            )
        }
        val scheduledByDay = mutableMapOf<LocalDate, Int>()
        val completedByDay = mutableMapOf<LocalDate, Int>()
        summaries.forEach { summary ->
            summary.scheduledDates.forEach { day -> scheduledByDay[day] = (scheduledByDay[day] ?: 0) + 1 }
            summary.completedDates.forEach { day -> completedByDay[day] = (completedByDay[day] ?: 0) + 1 }
        }

        return StatisticsUiState(
            monthLabel = monthLabel(month),
            rateText = monthRateText(summaries),
            longestStreakText = summaries.maxOfOrNull { it.longestStreak }?.toString() ?: "0",
            cells = monthCells(month, date) { day ->
                level(completedByDay[day] ?: 0, scheduledByDay[day] ?: 0)
            },
            bars = weekBars(date) { day -> completedByDay[day] ?: 0 },
            hasData = scheduledByDay.isNotEmpty(),
            isLoading = false,
        )
    }

    companion object {
        /** Per habit inside the month-to-date window. Denominator is scheduled days, never calendar days. */
        data class HabitSummary(
            val scheduledDates: List<LocalDate>,
            val completedDates: List<LocalDate>,
            val longestStreak: Int,
        ) {
            val scheduledDays: Int get() = scheduledDates.size
            val completedDays: Int get() = completedDates.size
        }

        fun summarize(
            versions: List<HabitSchedule>,
            logs: List<DailyLog>,
            from: LocalDate,
            to: LocalDate,
        ): HabitSummary {
            val completedDates = logs.filter { it.status == LogStatus.COMPLETED }.map { it.date }.toSet()
            val scheduled = ArrayList<LocalDate>()
            val completed = ArrayList<LocalDate>()
            var day = from
            while (!day.isAfter(to)) {
                if (isScheduled(versions, day)) {
                    scheduled += day
                    if (completedDates.contains(day)) completed += day
                }
                day = day.plusDays(1)
            }
            // The streak window starts at the oldest version, not at `from`: a 20-day streak that
            // began last month must still read 20, not 6.
            val streak = GetStreakUseCase.fromLoaded(versions, logs, to)
            return HabitSummary(scheduled, completed, streak.longest)
        }

        fun isScheduled(versions: List<HabitSchedule>, day: LocalDate): Boolean {
            val version = versions.firstOrNull { v ->
                !day.isBefore(v.effectiveFrom) && (v.effectiveUntil == null || !day.isAfter(v.effectiveUntil))
            } ?: return false
            return ScheduleRules.isScheduledOn(version.frequencyType, version.weekdayMask, day)
        }

        /**
         * Five steps, matching the design's legend. Nothing scheduled or nothing done is level 0;
         * otherwise quartiles of the ratio, and 1-of-1 done is level 4 rather than level 1.
         */
        fun level(completed: Int, scheduled: Int): Int = when {
            scheduled <= 0 -> 0
            completed <= 0 -> 0
            completed >= scheduled -> 4
            completed * 4 <= scheduled -> 1
            completed * 2 <= scheduled -> 2
            completed * 4 <= scheduled * 3 -> 3
            else -> 4
        }

        fun monthLabel(month: YearMonth): String = month.monthValue.toString() + " 月"

        fun monthRateText(summaries: List<HabitSummary>): String {
            val scheduled = summaries.sumOf { it.scheduledDays }
            val completed = summaries.sumOf { it.completedDays }
            if (scheduled == 0) return "0%"
            return Math.round(completed * 100.0 / scheduled).toString() + "%"
        }

        /** Monday-first grid with leading blanks; [levelOf] is injected so the caller owns the colour rule. */
        fun monthCells(month: YearMonth, today: LocalDate, levelOf: (LocalDate) -> Int): List<MonthCell> {
            val first = month.atDay(1)
            val padding = first.dayOfWeek.value - 1
            val cells = ArrayList<MonthCell>(padding + month.lengthOfMonth())
            repeat(padding) { cells += MonthCell(day = null) }
            for (day in 1..month.lengthOfMonth()) {
                val date = month.atDay(day)
                cells += MonthCell(
                    day = day,
                    level = levelOf(date),
                    isToday = date == today,
                    // Days that have not happened yet are drawn lighter than a scheduled day with
                    // nothing done: they are not a failure, they are simply not measured yet.
                    isFuture = date.isAfter(today),
                )
            }
            return cells
        }

        /** Monday-first week containing [today]; future days stay 0 so the trend stops at today. */
        fun weekBars(today: LocalDate, completedOf: (LocalDate) -> Int): List<WeekBar> {
            val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
            val labels = listOf("一", "二", "三", "四", "五", "六", "日")
            val counts = (0..6).map { offset ->
                val day = monday.plusDays(offset.toLong())
                if (day.isAfter(today)) 0 else completedOf(day)
            }
            val max = counts.maxOrNull() ?: 0
            return (0..6).map { offset ->
                val day = monday.plusDays(offset.toLong())
                WeekBar(
                    label = labels[offset],
                    fraction = if (max <= 0) 0f else counts[offset].toFloat() / max,
                    isToday = day == today,
                )
            }
        }
    }
}