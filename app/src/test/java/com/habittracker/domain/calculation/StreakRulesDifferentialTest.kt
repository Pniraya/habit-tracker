package com.habittracker.domain.calculation

import com.habittracker.data.local.entity.FrequencyType
import com.habittracker.domain.model.HabitSchedule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

/**
 * A second, deliberately naive implementation of the same rule, compared against [StreakRules] on
 * randomised inputs (TASK-024, external review Q12).
 *
 * Why: unit tests written from the same reading of the spec as the code cannot catch a misreading -
 * the assertion inherits it. Mutation testing only proves the assertions bite *this* code. An
 * independent oracle that enumerates days instead of walking indices gives the suite a chance to
 * disagree with the implementation on its own.
 */
class StreakRulesDifferentialTest {

    private val oracleFirstDay = LocalDate.of(2025, 12, 1)

    /** Enumerates every day, asks each version whether it covers the day, then reads the runs. */
    private fun oracle(
        schedules: List<HabitSchedule>,
        completed: Set<LocalDate>,
        today: LocalDate,
    ): Pair<Int, Int> {
        if (schedules.isEmpty()) return 0 to 0
        val first = schedules.minOf { it.effectiveFrom }
        if (first.isAfter(today)) return 0 to 0

        // Deliberately NOT written the way StreakRules does it: no isScheduledOn, no weekdayMask
        // expression copied from the implementation. Days are addressed as epoch-day numbers and the
        // schedule is consulted through interval arithmetic on plain longs.
        val due = ArrayList<LocalDate>()
        var epoch = first.toEpochDay()
        val lastEpoch = today.toEpochDay()
        while (epoch <= lastEpoch) {
            val date = LocalDate.ofEpochDay(epoch)
            val weekdayIndex = date.dayOfWeek.value - 1
            // Literal masks rather than ScheduleRules.ALL_DAYS_MASK / WEEKDAYS_MASK: reusing those
            // constants is exactly how a wrong constant can slip past both sides (review ME).
            val matches = schedules.any { schedule ->
                val start = schedule.effectiveFrom.toEpochDay()
                val end = schedule.effectiveUntil?.toEpochDay() ?: Long.MAX_VALUE
                epoch >= start && epoch <= end && (schedule.weekdayMask.toLong() shr weekdayIndex) % 2L == 1L
            }
            if (matches) due.add(date)
            epoch += 1
        }

        var longest = 0
        var run = 0
        for (d in due) {
            if (completed.contains(d)) {
                run += 1
                if (run > longest) longest = run
            } else if (d != today) {
                run = 0
            }
        }

        // Current streak computed the other way round: walk back from the last due day.
        var current = 0
        var index = due.lastIndex
        if (index >= 0 && due[index] == today && !completed.contains(today)) index -= 1
        while (index >= 0 && completed.contains(due[index])) {
            current += 1
            index -= 1
        }
        return current to longest
    }

    private fun randomSchedules(random: Random, today: LocalDate): List<HabitSchedule> {
        val versionCount = random.nextInt(1, 4)
        val versions = ArrayList<HabitSchedule>()
        var cursor = today.minusDays(random.nextLong(1, 90))
        repeat(versionCount) { index ->
            val frequency = FrequencyType.values()[random.nextInt(FrequencyType.values().size)]
            val mask = when (frequency) {
                // Literal values on purpose: if a constant changes, this side must notice.
                FrequencyType.DAILY -> 127
                FrequencyType.WEEKDAYS -> 31
                FrequencyType.CUSTOM -> random.nextInt(1, 128)
            }
            val last = index == versionCount - 1
            val until = if (last && random.nextBoolean()) null else cursor.plusDays(random.nextLong(0, 40))
            // A gap before the next version (the store must keep working across a gap) is half the
            // time; a future start date is not allowed, because a habit's version history starts
            // when the habit exists.
            versions.add(
                HabitSchedule(
                    id = index.toLong() + 1,
                    habitId = 1,
                    frequencyType = frequency,
                    weekdayMask = mask,
                    effectiveFrom = cursor,
                    effectiveUntil = until,
                ),
            )
            val gap = if (random.nextBoolean()) random.nextLong(1, 20) else 1
            cursor = (until ?: today).plusDays(gap)
        }
        return versions.filter { !it.effectiveFrom.isAfter(today) }
    }

    @Test
    fun randomisedHistoriesAgreeWithTheNaiveImplementation() {
        val random = Random(20261105)
        var checked = 0
        repeat(3000) {
            val today = oracleFirstDay.plusDays(random.nextLong(0, 200))
            val schedules = randomSchedules(random, today)
            if (schedules.isEmpty()) return@repeat

            val first = schedules.minOf { it.effectiveFrom }
            val completed = HashSet<LocalDate>()
            var day = first
            while (!day.isAfter(today)) {
                if (random.nextInt(100) < 45) completed.add(day)
                day = day.plusDays(1)
            }
            // A completed day the habit was never due on must not count, in either implementation.
            completed.add(first.minusDays(1))

            val expected = oracle(schedules, completed, today)
            val actual = StreakRules.compute(schedules, completed, today)

            assertEquals(
                "current at case " + checked + " schedules=" + schedules + " today=" + today,
                expected.first,
                actual.current,
            )
            assertEquals(
                "longest at case " + checked + " schedules=" + schedules + " today=" + today,
                expected.second,
                actual.longest,
            )
            checked += 1
        }
        assertEquals(3000, checked)
    }
}
