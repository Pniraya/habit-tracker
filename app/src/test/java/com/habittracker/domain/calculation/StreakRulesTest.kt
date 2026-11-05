package com.habittracker.domain.calculation

import com.habittracker.data.local.entity.FrequencyType
import com.habittracker.domain.model.HabitSchedule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakRulesTest {

    private val today = LocalDate.of(2026, 11, 5) // Thursday

    private fun daily(from: LocalDate = LocalDate.of(2026, 10, 1), until: LocalDate? = null) =
        HabitSchedule(1, 1, FrequencyType.DAILY, ScheduleRules.ALL_DAYS_MASK, from, until)

    private fun monWedFri(from: LocalDate = LocalDate.of(2026, 10, 1)) =
        HabitSchedule(1, 1, FrequencyType.CUSTOM, 21, from, null)

    private fun dates(vararg days: LocalDate) = days.toSet()

    @Test
    fun aDayThatWasNeverDueDoesNotBreakTheStreak() {
        // Mon/Wed/Fri habit: Tuesday is not due. Mon and Wed completed, Thursday(today) not yet done.
        val mon = LocalDate.of(2026, 11, 2)
        val wed = LocalDate.of(2026, 11, 4)
        val streak = StreakRules.compute(listOf(monWedFri()), dates(mon, wed), today)

        assertEquals(2, streak.current)
        assertEquals(2, streak.longest)
    }

    @Test
    fun aMissedDueDayBreaksTheCurrentStreak() {
        // Wednesday was due and missed, so Monday no longer continues the streak.
        val mon = LocalDate.of(2026, 11, 2)
        val streak = StreakRules.compute(listOf(monWedFri()), dates(mon), today)

        assertEquals(0, streak.current)
        assertEquals(1, streak.longest)
    }

    @Test
    fun anIncompleteTodayDoesNotBreakTheStreak() {
        val yesterday = today.minusDays(1)
        val streak = StreakRules.compute(listOf(daily()), dates(today.minusDays(2), yesterday), today)

        assertEquals(2, streak.current)
        assertEquals(2, streak.longest)
    }

    @Test
    fun aCompletedTodayJoinsTheStreak() {
        val streak = StreakRules.compute(
            listOf(daily()),
            dates(today.minusDays(2), today.minusDays(1), today),
            today,
        )

        assertEquals(3, streak.current)
        assertEquals(3, streak.longest)
    }

    @Test
    fun theLongestRunSurvivesALaterBreak() {
        val a = LocalDate.of(2026, 10, 26)
        val b = LocalDate.of(2026, 10, 27)
        val c = LocalDate.of(2026, 10, 28)
        // Three in a row, a gap, then one recent completion.
        val streak = StreakRules.compute(listOf(daily()), dates(a, b, c, LocalDate.of(2026, 11, 4)), today)

        assertEquals(1, streak.current)
        assertEquals(3, streak.longest)
    }

    @Test
    fun aScheduleChangeIsJudgedByTheVersionInForceOnThatDay() {
        // Daily until the 2nd, then Mon/Wed/Fri only. The 3rd and 4th are no longer due.
        val before = HabitSchedule(1, 1, FrequencyType.DAILY, ScheduleRules.ALL_DAYS_MASK, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 2))
        val after = HabitSchedule(2, 1, FrequencyType.CUSTOM, 21, LocalDate.of(2026, 11, 3), null)
        val streak = StreakRules.compute(
            listOf(before, after),
            dates(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 4)),
            today,
        )

        assertEquals(2, streak.current)
    }

    @Test
    fun missingYesterdayAndCompletingTodayGivesOne() {
        // The mixed path the single forward pass relies on: a missed yesterday resets the run, then
        // today's completion starts a new one and only that one is current.
        val streak = StreakRules.compute(listOf(daily()), dates(today), today)

        assertEquals(1, streak.current)
        assertEquals(1, streak.longest)
    }

    @Test
    fun noScheduleMeansNoStreak() {
        assertEquals(StreakRules.Streak.EMPTY, StreakRules.compute(emptyList(), dates(today), today))
    }

    @Test
    fun aHabitCreatedTodayWithNoLogsHasNoStreak() {
        val streak = StreakRules.compute(listOf(daily(from = today)), emptySet(), today)

        assertEquals(0, streak.current)
        assertEquals(0, streak.longest)
    }
}
