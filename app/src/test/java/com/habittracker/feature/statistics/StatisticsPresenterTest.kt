package com.habittracker.feature.statistics

import com.habittracker.data.local.entity.FrequencyType
import com.habittracker.data.local.entity.LogStatus
import com.habittracker.data.local.entity.TrackingType
import com.habittracker.domain.model.DailyLog
import com.habittracker.domain.model.Habit
import com.habittracker.domain.model.HabitSchedule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * The denominator rule is the whole point of this page: unscheduled days must not count against the
 * user. These tests pin that with the calendar, not with a mock.
 */
class StatisticsPresenterTest {

    private val monday = LocalDate.of(2026, 11, 2)
    private val sunday = monday.plusDays(6)

    private fun habit(id: Long = 1) = Habit(
        id = id,
        name = "Read",
        icon = "book",
        color = "#5B8C6A",
        trackingType = TrackingType.BOOLEAN,
        targetValue = 1.0,
        unit = null,
        createdOn = monday,
        archivedOn = null,
    )

    private fun schedule(mask: Int, type: FrequencyType = FrequencyType.WEEKDAYS) = HabitSchedule(
        id = 1,
        habitId = 1,
        frequencyType = type,
        weekdayMask = mask,
        effectiveFrom = monday,
        effectiveUntil = null,
    )

    private fun completed(date: LocalDate) = DailyLog(
        habitId = 1,
        date = date,
        actualValue = 1.0,
        targetSnapshot = 1.0,
        status = LogStatus.COMPLETED,
        completedAt = 100L,
    )

    @Test
    fun aWeekdayHabitIsNotPunishedForTheWeekend() {
        // Mon-Fri only: 5 scheduled days in the window, 3 completed.
        val stat = StatisticsPresenter.summarize(
            habit = habit(),
            versions = listOf(schedule(mask = 31)),
            logs = listOf(completed(monday), completed(monday.plusDays(1)), completed(monday.plusDays(2))),
            from = monday,
            to = sunday,
        )

        assertEquals(5, stat.scheduledDays)
        assertEquals(3, stat.completedDays)
        assertEquals(0.6, stat.rate, 0.0001)
        assertEquals("3 / 5 · 60%", StatisticsPresenter.rateText(stat))
    }

    @Test
    fun aDailyHabitCountsEveryDay() {
        val stat = StatisticsPresenter.summarize(
            habit = habit(),
            versions = listOf(schedule(mask = 127, type = FrequencyType.DAILY)),
            logs = listOf(completed(monday), completed(sunday)),
            from = monday,
            to = sunday,
        )

        assertEquals(7, stat.scheduledDays)
        assertEquals(2, stat.completedDays)
    }

    @Test
    fun aCompletionOnARestDayDoesNotInflateTheRate() {
        val stat = StatisticsPresenter.summarize(
            habit = habit(),
            versions = listOf(schedule(mask = 31)),
            logs = listOf(completed(monday), completed(monday.plusDays(5))),
            from = monday,
            to = sunday,
        )

        // Saturday is not scheduled, so it is not counted at all: 1 / 5, never 2 / 5.
        assertEquals(5, stat.scheduledDays)
        assertEquals(1, stat.completedDays)
    }

    @Test
    fun daysWithoutAnyScheduleVersionAreNotCounted() {
        val stat = StatisticsPresenter.summarize(
            habit = habit(),
            versions = emptyList(),
            logs = listOf(completed(monday)),
            from = monday,
            to = sunday,
        )

        assertEquals(0, stat.scheduledDays)
        assertEquals(0.0, stat.rate, 0.0001)
        // No division by zero, and the text still reads sanely.
        assertEquals("0 / 0 · 0%", StatisticsPresenter.rateText(stat))
    }

    @Test
    fun theWindowIsSevenDaysInclusive() {
        assertEquals(7L, StatisticsPresenter.WINDOW_DAYS)
        // 2026-11-06 minus 6 days is 2026-10-31: the window must not silently become 8 or 6.
        val to = LocalDate.of(2026, 11, 6)
        assertEquals(LocalDate.of(2026, 10, 31), to.minusDays(StatisticsPresenter.WINDOW_DAYS - 1))
    }
}