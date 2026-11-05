package com.habittracker.domain.usecase

import com.habittracker.data.local.entity.FrequencyType
import com.habittracker.data.local.entity.LogStatus
import com.habittracker.domain.model.DailyLog
import com.habittracker.domain.model.HabitSchedule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Rows in, streak out: the clamping rules that the batch path relies on. */
class GetStreakUseCaseTest {

    private val start = LocalDate.of(2026, 11, 1)
    private val today = LocalDate.of(2026, 11, 3)

    private fun schedule() = HabitSchedule(
        id = 1,
        habitId = 1,
        frequencyType = FrequencyType.DAILY,
        weekdayMask = 127,
        effectiveFrom = start,
        effectiveUntil = null,
    )

    private fun log(date: LocalDate, status: LogStatus = LogStatus.COMPLETED) =
        DailyLog(1, date, 1.0, 1.0, status, if (status == LogStatus.COMPLETED) 1L else null)

    @Test
    fun rowsOutsideTheComputedWindowDoNotChangeTheCount() {
        // Not a live risk today: compute() only walks [firstVersion, today], so this asserts the
        // property survives a future rewrite that iterates the rows themselves.
        val streak = GetStreakUseCase.fromLoaded(
            schedules = listOf(schedule()),
            logs = listOf(log(start.minusDays(1)), log(start), log(today.plusDays(1))),
            today = today,
        )

        assertEquals(0, streak.current)
        assertEquals(1, streak.longest)
    }

    @Test
    fun aCompletedTodayAfterAMissedYesterdayRestartsAtOne() {
        // The mixed path: yesterday was due and missed, today is done. REQUIREMENTS 93 counts only
        // past due days as failures, so today's completion starts a fresh run of one.
        val streak = GetStreakUseCase.fromLoaded(
            schedules = listOf(schedule()),
            logs = listOf(log(start), log(today)),
            today = today,
        )

        assertEquals(1, streak.current)
        assertEquals(1, streak.longest)
    }

    @Test
    fun onlyCompletedRowsCount() {
        val streak = GetStreakUseCase.fromLoaded(
            schedules = listOf(schedule()),
            logs = listOf(log(start), log(start.plusDays(1), LogStatus.IN_PROGRESS)),
            today = today,
        )

        // 11-02 stayed IN_PROGRESS, which breaks the run just like a missing day would.
        assertEquals(0, streak.current)
        assertEquals(1, streak.longest)
    }

    @Test
    fun noScheduleVersionMeansNoStreak() {
        assertEquals(0, GetStreakUseCase.fromLoaded(emptyList(), emptyList(), today).current)
    }
}
