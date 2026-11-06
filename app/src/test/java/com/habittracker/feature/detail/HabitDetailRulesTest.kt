package com.habittracker.feature.detail

import com.habittracker.data.local.entity.FrequencyType
import com.habittracker.domain.model.HabitSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class HabitDetailRulesTest {

    private val today = LocalDate.of(2026, 11, 6) // Friday

    private fun schedule(
        from: LocalDate = LocalDate.of(2026, 1, 1),
        until: LocalDate? = null,
        frequency: FrequencyType = FrequencyType.DAILY,
        mask: Int = 127,
    ) = HabitSchedule(
        id = 1L,
        habitId = 1L,
        frequencyType = frequency,
        weekdayMask = mask,
        effectiveFrom = from,
        effectiveUntil = until,
    )

    @Test
    fun theWeekRunsMondayToSundayAndOnlyDueDaysCanBeMissed() {
        val week = HabitDetailRules.week(
            today = today,
            versions = listOf(schedule()),
            completed = setOf(LocalDate.of(2026, 11, 2)),
        )

        assertEquals(7, week.days.size)
        assertEquals(DayOfWeek.MONDAY, week.days.first().date.dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, week.days.last().date.dayOfWeek)
        assertEquals(DayState.DONE, week.days[0].state)
        assertEquals(DayState.MISSED, week.days[1].state)
        assertEquals(DayState.MISSED, week.days[4].state)
        assertEquals(DayState.FUTURE, week.days[5].state)
        assertEquals(DayState.FUTURE, week.days[6].state)
        assertEquals(1, week.done)
        assertEquals(5, week.due)
        assertTrue(week.days[4].isToday)
    }

    @Test
    fun daysOutsideThePlanAreNotDueAndAreNotCountedAsMissed() {
        // Sunday, so Saturday and Sunday are already past: WEEKDAYS must make them rest days
        // rather than failures.
        val sunday = LocalDate.of(2026, 11, 8)
        val week = HabitDetailRules.week(
            today = sunday,
            versions = listOf(schedule(frequency = FrequencyType.WEEKDAYS, mask = 31)),
            completed = emptySet(),
        )

        assertEquals(DayState.MISSED, week.days[4].state)
        assertEquals(DayState.NOT_DUE, week.days[5].state)
        assertEquals(DayState.NOT_DUE, week.days[6].state)
        assertEquals(5, week.due)
    }

    @Test
    fun aDayBeforeTheFirstScheduleVersionIsNotDue() {
        val week = HabitDetailRules.week(
            today = today,
            versions = listOf(schedule(from = LocalDate.of(2026, 11, 4))),
            completed = emptySet(),
        )

        assertEquals(DayState.NOT_DUE, week.days[0].state)
        assertEquals(DayState.MISSED, week.days[4].state)
    }

    @Test
    fun theFiveWeekGridIsMondayAlignedAndEndsWithTheCurrentWeek() {
        val weeks = HabitDetailRules.fiveWeeks(
            today = today,
            versions = listOf(schedule()),
            completed = emptySet(),
        )

        assertEquals(5, weeks.size)
        assertTrue(weeks.all { it.size == 7 })
        assertEquals(DayOfWeek.MONDAY, weeks.first().first().date.dayOfWeek)
        assertEquals(LocalDate.of(2026, 10, 5), weeks.first().first().date)
        assertEquals(DayOfWeek.MONDAY, weeks.last().first().date.dayOfWeek)
        val todayCell = weeks.last().first { it.isToday }
        assertEquals(today, todayCell.date)
        assertEquals(DayState.MISSED, todayCell.state)
        assertEquals(DayState.FUTURE, weeks.last()[5].state)
    }

    @Test
    fun theFiveWeekRateDividesByDueDaysOnly() {
        // The habit starts on Monday, so the window holds exactly five due days (Mon..Fri).
        val completed = setOf(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 3))
        val stats = HabitDetailRules.stats(
            today,
            listOf(schedule(from = LocalDate.of(2026, 11, 2))),
            completed,
        )

        // 5 due days, 2 done => 40%.
        assertEquals(2, stats.totalCompletions)
        assertEquals(40, stats.fiveWeekRatePercent)
        assertEquals(2, stats.monthCompletions)
    }

    @Test
    fun aWindowWithoutDueDaysReportsZeroInsteadOfDividingByZero() {
        val stats = HabitDetailRules.stats(
            today = today,
            versions = listOf(schedule(from = today.plusDays(1))),
            completed = emptySet(),
        )

        assertEquals(0, stats.fiveWeekRatePercent)
        assertEquals(0, stats.totalCompletions)
    }

    @Test
    fun theStreakUsesTheWholeHistoryNotOnlyTheVisibleWindow() {
        // A completion far outside the five-week grid still counts as "total", which is why the
        // presenter loads logs from the first schedule version instead of from the grid window.
        val completed = setOf(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 11, 2))
        val stats = HabitDetailRules.stats(today, listOf(schedule()), completed)

        assertEquals(2, stats.totalCompletions)
        assertEquals(1, stats.monthCompletions)
    }

    @Test
    fun theFrequencyLabelMatchesTheSchedule() {
        assertEquals("每天", HabitDetailRules.frequencyLabel(schedule()))
        assertEquals(
            "工作日",
            HabitDetailRules.frequencyLabel(schedule(frequency = FrequencyType.WEEKDAYS, mask = 31)),
        )
        assertEquals(
            "每周 3 天",
            HabitDetailRules.frequencyLabel(schedule(frequency = FrequencyType.CUSTOM, mask = 0b10101)),
        )
        assertEquals("暂无计划", HabitDetailRules.frequencyLabel(null))
    }

    @Test
    fun isDueRespectsClosedVersions() {
        val versions = listOf(
            schedule(from = LocalDate.of(2026, 1, 1), until = LocalDate.of(2026, 10, 31)),
            schedule(from = LocalDate.of(2026, 11, 1)),
        )

        assertTrue(HabitDetailRules.isDue(versions, LocalDate.of(2026, 11, 6)))
        assertFalse(HabitDetailRules.isDue(versions, LocalDate.of(2025, 12, 31)))
    }
}
