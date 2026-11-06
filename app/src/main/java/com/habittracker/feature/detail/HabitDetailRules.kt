package com.habittracker.feature.detail

import com.habittracker.domain.calculation.ScheduleRules
import com.habittracker.domain.model.HabitSchedule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/** State of one day in the detail page's week strip / five-week grid. */
enum class DayState { DONE, MISSED, NOT_DUE, FUTURE }

data class DetailDay(val date: LocalDate, val state: DayState, val isToday: Boolean)

/** The current Monday-to-Sunday strip: done/due are the two numbers in its header. */
data class DetailWeek(val days: List<DetailDay>, val done: Int, val due: Int)

/** The three small cards under the grid. */
data class DetailStats(val totalCompletions: Int, val fiveWeekRatePercent: Int, val monthCompletions: Int)

/**
 * Pure projections for the habit detail page: which days were due, which were done, and how the
 * five-week window is aligned. No database access, so the JVM tests can pin every boundary.
 */
object HabitDetailRules {

    /** True when a schedule version covers [date] and that version marks it as a plan day. */
    fun isDue(versions: List<HabitSchedule>, date: LocalDate): Boolean {
        val version = versions.firstOrNull { candidate ->
            !date.isBefore(candidate.effectiveFrom) &&
                (candidate.effectiveUntil == null || !date.isAfter(candidate.effectiveUntil))
        } ?: return false
        return ScheduleRules.isScheduledOn(version.frequencyType, version.weekdayMask, date)
    }

    fun state(
        date: LocalDate,
        today: LocalDate,
        versions: List<HabitSchedule>,
        completed: Set<LocalDate>,
    ): DayState = when {
        date.isAfter(today) -> DayState.FUTURE
        !isDue(versions, date) -> DayState.NOT_DUE
        date in completed -> DayState.DONE
        else -> DayState.MISSED
    }

    private fun mondayOfWeek(today: LocalDate): LocalDate =
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun week(today: LocalDate, versions: List<HabitSchedule>, completed: Set<LocalDate>): DetailWeek {
        val monday = mondayOfWeek(today)
        val days = (0L..6L).map { offset ->
            val date = monday.plusDays(offset)
            DetailDay(date = date, state = state(date, today, versions, completed), isToday = date == today)
        }
        return DetailWeek(
            days = days,
            done = days.count { it.state == DayState.DONE },
            due = days.count { it.state == DayState.DONE || it.state == DayState.MISSED },
        )
    }

    /**
     * Five Monday-aligned rows ending with the current week. The last row can contain future days
     * (rendered as FUTURE), which is what keeps the weekday columns honest; the mockup's flat
     * 35-cell strip would put "today" under the wrong weekday.
     */
    fun fiveWeeks(
        today: LocalDate,
        versions: List<HabitSchedule>,
        completed: Set<LocalDate>,
    ): List<List<DetailDay>> {
        val firstMonday = mondayOfWeek(today).minusWeeks(4)
        return (0 until 5).map { weekIndex ->
            (0L..6L).map { dayIndex ->
                val date = firstMonday.plusDays(weekIndex * 7L + dayIndex)
                DetailDay(date = date, state = state(date, today, versions, completed), isToday = date == today)
            }
        }
    }

    fun stats(
        today: LocalDate,
        versions: List<HabitSchedule>,
        completed: Set<LocalDate>,
    ): DetailStats {
        val firstMonday = mondayOfWeek(today).minusWeeks(4)
        val windowEnd = today
        var due = 0
        var done = 0
        var date = firstMonday
        while (!date.isAfter(windowEnd)) {
            if (isDue(versions, date)) {
                due += 1
                if (date in completed) done += 1
            }
            date = date.plusDays(1)
        }
        val month = YearMonth.from(today)
        return DetailStats(
            totalCompletions = completed.size,
            fiveWeekRatePercent = if (due == 0) 0 else Math.round(100f * done / due),
            monthCompletions = completed.count { YearMonth.from(it) == month },
        )
    }

    /** 2000.0 -> "2000", 0.5 -> "0.5": the dialog and the schedule row share one format. */
    fun formatAmount(value: Double): String =
        if (value == Math.floor(value)) value.toLong().toString() else value.toString()

    /** "每天" / "工作日" / "每周 3 天" for the schedule row. */
    fun frequencyLabel(version: HabitSchedule?): String = when {
        version == null -> "暂无计划"
        version.frequencyType == com.habittracker.data.local.entity.FrequencyType.DAILY -> "每天"
        version.frequencyType == com.habittracker.data.local.entity.FrequencyType.WEEKDAYS -> "工作日"
        else -> "每周 " + Integer.bitCount(version.weekdayMask) + " 天"
    }
}
