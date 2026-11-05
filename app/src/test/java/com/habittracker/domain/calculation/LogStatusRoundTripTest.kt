package com.habittracker.domain.calculation

import com.habittracker.data.local.entity.FrequencyType
import com.habittracker.data.local.entity.LogStatus
import com.habittracker.data.local.entity.TrackingType
import com.habittracker.domain.model.DailyLog
import com.habittracker.domain.model.HabitSchedule
import com.habittracker.domain.usecase.GetStreakUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * "2000 -> 1000 -> 2000" on a numeric habit (TASK-024, external review Q16): status is derived from
 * the day's total, so dropping below the target clears the completion and the streak follows without
 * any rollback code of its own.
 */
class LogStatusRoundTripTest {

    private val target = 2000.0
    private val today = LocalDate.of(2026, 11, 3)

    private fun schedule() = HabitSchedule(
        id = 1,
        habitId = 1,
        frequencyType = FrequencyType.DAILY,
        weekdayMask = ScheduleRules.ALL_DAYS_MASK,
        effectiveFrom = LocalDate.of(2026, 11, 1),
        effectiveUntil = null,
    )

    private fun log(date: LocalDate, value: Double, status: LogStatus, completedAt: Long?) =
        DailyLog(1, date, value, target, status, completedAt)

    @Test
    fun droppingBelowTheTargetClearsTheCompletion() {
        val reached = LogStatusRules.evaluate(TrackingType.NUMERIC, 2000.0, target, null, 100L)
        assertEquals(LogStatus.COMPLETED, reached.status)
        assertEquals(100L, reached.completedAt)

        val dropped = LogStatusRules.evaluate(TrackingType.NUMERIC, 1000.0, target, reached.completedAt, 200L)
        assertEquals(LogStatus.IN_PROGRESS, dropped.status)
        assertNull(dropped.completedAt)
    }

    @Test
    fun reachingTheTargetAgainStampsANewCompletion() {
        val third = LogStatusRules.evaluate(TrackingType.NUMERIC, 2000.0, target, null, 300L)
        assertEquals(LogStatus.COMPLETED, third.status)
        assertEquals(300L, third.completedAt)
        assertNotNull(third.completedAt)
    }

    @Test
    fun theStreakFollowsTheDerivedStatusWithoutItsOwnRollback() {
        // 11-01 and 11-02 completed, 11-03 started but later dropped below the target.
        val logs = listOf(
            log(today.minusDays(2), 2000.0, LogStatus.COMPLETED, 10L),
            log(today.minusDays(1), 2000.0, LogStatus.COMPLETED, 11L),
            log(today, 1000.0, LogStatus.IN_PROGRESS, null),
        )

        val streak = GetStreakUseCase.fromLoaded(listOf(schedule()), logs, today)

        // Today is still pending, so it must not break the run; the two finished days count.
        assertEquals(2, streak.current)
        assertEquals(2, streak.longest)
    }
}
