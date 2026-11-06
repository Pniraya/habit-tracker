package com.habittracker.feature.statistics

import com.habittracker.data.local.entity.FrequencyType
import com.habittracker.data.local.entity.LogStatus
import com.habittracker.domain.model.DailyLog
import com.habittracker.domain.model.HabitSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * The denominator rule and the five-step scale are the whole point of this page: an unscheduled day
 * must not count against the user, and 1-of-1 done must read as the darkest cell, not the lightest.
 */
class StatisticsPresenterTest {

    private val monday = LocalDate.of(2026, 11, 2)
    private val sunday = monday.plusDays(6)

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
        val summary = StatisticsPresenter.summarize(
            versions = listOf(schedule(mask = 31)),
            logs = listOf(completed(monday), completed(monday.plusDays(1)), completed(monday.plusDays(2))),
            from = monday,
            to = sunday,
        )

        assertEquals(5, summary.scheduledDays)
        assertEquals(3, summary.completedDays)
    }

    @Test
    fun aCompletionOnARestDayDoesNotInflateTheCount() {
        val summary = StatisticsPresenter.summarize(
            versions = listOf(schedule(mask = 31)),
            logs = listOf(completed(monday), completed(monday.plusDays(5))),
            from = monday,
            to = sunday,
        )

        assertEquals(5, summary.scheduledDays)
        assertEquals(1, summary.completedDays)
    }

    @Test
    fun theFiveStepScaleIsMonotonicAndHitsBothEnds() {
        assertEquals(0, StatisticsPresenter.level(0, 0))
        assertEquals(0, StatisticsPresenter.level(0, 4))
        assertEquals(1, StatisticsPresenter.level(1, 4))
        assertEquals(2, StatisticsPresenter.level(2, 4))
        assertEquals(3, StatisticsPresenter.level(3, 4))
        assertEquals(4, StatisticsPresenter.level(4, 4))
        // One habit, one day, done: the darkest cell. Getting this wrong was the visual bug the
        // design would show immediately (a fully done day looking almost empty).
        assertEquals(4, StatisticsPresenter.level(1, 1))
    }

    @Test
    fun theMonthGridStartsWithTheRightWeekdayAndHasNoTrailingBlanks() {
        // 2026-11-01 is a Sunday, so a Monday-first grid needs 6 blanks before day 1.
        val november = YearMonth.of(2026, 11)
        val cells = StatisticsPresenter.monthCells(november, LocalDate.of(2026, 11, 6)) { 0 }

        assertEquals(6, cells.count { it.day == null })
        assertEquals(30, cells.count { it.day != null })
        assertEquals(1, cells.first { it.day != null }.day)
        assertEquals(30, cells.last().day)
        assertTrue(cells.first { it.isToday }.day == 6)
        // Days after today are flagged so the view can draw them lighter; day 6 itself is not future.
        assertTrue(cells.first { it.day == 7 }!!.isFuture)
        assertTrue(!cells.first { it.day == 6 }!!.isFuture)
    }

    @Test
    fun theWeekBarsStopAtTodayAndScaleToTheTallestDay() {
        val today = LocalDate.of(2026, 11, 6) // Friday
        val bars = StatisticsPresenter.weekBars(today) { day ->
            when (day.dayOfWeek.value) {
                1 -> 2
                5 -> 1
                else -> 0
            }
        }

        assertEquals(7, bars.size)
        assertEquals("一", bars[0].label)
        assertEquals(1f, bars[0].fraction, 0.0001f)
        assertEquals(0.5f, bars[4].fraction, 0.0001f)
        assertTrue(bars[4].isToday)
        // Saturday and Sunday are after today: zero, not a phantom bar.
        assertEquals(0f, bars[5].fraction, 0.0001f)
        assertEquals(0f, bars[6].fraction, 0.0001f)
    }

    @Test
    fun theRateIsRoundedFromTotalsNotAveragedPerHabit() {
        val a = StatisticsPresenter.summarize(listOf(schedule(127, FrequencyType.DAILY)), listOf(completed(monday)), monday, sunday)
        val b = StatisticsPresenter.summarize(listOf(schedule(127, FrequencyType.DAILY)), emptyList(), monday, sunday)

        // 1 of 14, not (14% + 0%) / 2.
        assertEquals("7%", StatisticsPresenter.monthRateText(listOf(a, b)))
    }

    @Test
    fun anEmptyRangeReportsZeroWithoutDividingByZero() {
        val summary = StatisticsPresenter.summarize(emptyList(), listOf(completed(monday)), monday, sunday)
        assertEquals(0, summary.scheduledDays)
        assertEquals("0%", StatisticsPresenter.monthRateText(listOf(summary)))
    }
}