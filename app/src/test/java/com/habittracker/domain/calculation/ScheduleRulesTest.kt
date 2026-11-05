package com.habittracker.domain.calculation

import com.habittracker.data.local.entity.FrequencyType
import com.habittracker.domain.model.ScheduleVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ScheduleRulesTest {

    private val monday = LocalDate.of(2026, 11, 2)

    @Test
    fun dailyMaskIsAllDays() {
        assertEquals(127, ScheduleRules.maskFor(FrequencyType.DAILY, null))
    }

    @Test
    fun weekdaysMaskIsMondayToFriday() {
        assertEquals(31, ScheduleRules.maskFor(FrequencyType.WEEKDAYS, null))
    }

    @Test
    fun customMaskIsUsedAsGiven() {
        assertEquals(21, ScheduleRules.maskFor(FrequencyType.CUSTOM, 21))
    }

    @Test(expected = IllegalArgumentException::class)
    fun customMaskZeroIsRejected() {
        ScheduleRules.maskFor(FrequencyType.CUSTOM, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun customMaskOutOfRangeIsRejected() {
        ScheduleRules.maskFor(FrequencyType.CUSTOM, 128)
    }

    @Test
    fun isoWeekdayBitsMatchTheMaskDefinition() {
        assertEquals(1, ScheduleRules.bitFor(DayOfWeek.MONDAY))
        assertEquals(2, ScheduleRules.bitFor(DayOfWeek.TUESDAY))
        assertEquals(64, ScheduleRules.bitFor(DayOfWeek.SUNDAY))
    }

    @Test
    fun customMaskSelectsOnlyItsDays() {
        val mask = 21 // Monday, Wednesday, Friday
        assertTrue(ScheduleRules.isScheduledOn(FrequencyType.CUSTOM, mask, monday))
        assertFalse(ScheduleRules.isScheduledOn(FrequencyType.CUSTOM, mask, monday.plusDays(1)))
        assertTrue(ScheduleRules.isScheduledOn(FrequencyType.CUSTOM, mask, monday.plusDays(4)))
        assertFalse(ScheduleRules.isScheduledOn(FrequencyType.CUSTOM, mask, monday.plusDays(5)))
    }

    @Test
    fun dailyRunsOnWeekends() {
        assertTrue(ScheduleRules.isScheduledOn(FrequencyType.DAILY, 127, monday.plusDays(6)))
    }

    @Test
    fun changingScheduleClosesTheOpenVersionTheDayBefore() {
        val versions = listOf(
            ScheduleVersion(id = 1, effectiveFrom = LocalDate.of(2026, 1, 1), effectiveUntil = null),
        )
        val change = ScheduleRules.planVersionChange(versions, LocalDate.of(2026, 11, 10))
        assertEquals(1L, change.closeVersionId)
        assertEquals(LocalDate.of(2026, 11, 9), change.closeVersionUntil)
    }

    @Test(expected = IllegalArgumentException::class)
    fun newVersionMustStartAfterTheLatestOne() {
        val versions = listOf(
            ScheduleVersion(id = 1, effectiveFrom = LocalDate.of(2026, 1, 1), effectiveUntil = null),
        )
        ScheduleRules.planVersionChange(versions, LocalDate.of(2026, 1, 1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun twoOpenVersionsAreAIntegrityError() {
        val versions = listOf(
            ScheduleVersion(id = 1, effectiveFrom = LocalDate.of(2026, 1, 1), effectiveUntil = null),
            ScheduleVersion(id = 2, effectiveFrom = LocalDate.of(2026, 2, 1), effectiveUntil = null),
        )
        ScheduleRules.planVersionChange(versions, LocalDate.of(2026, 3, 1))
    }

    @Test
    fun findVersionOnRespectsClosedIntervals() {
        val versions = listOf(
            ScheduleVersion(id = 1, effectiveFrom = LocalDate.of(2026, 1, 1), effectiveUntil = LocalDate.of(2026, 2, 28)),
            ScheduleVersion(id = 2, effectiveFrom = LocalDate.of(2026, 3, 1), effectiveUntil = null),
        )
        assertEquals(1L, ScheduleRules.findVersionOn(versions, LocalDate.of(2026, 2, 28))?.id)
        assertEquals(2L, ScheduleRules.findVersionOn(versions, LocalDate.of(2026, 3, 1))?.id)
        assertNull(ScheduleRules.findVersionOn(versions, LocalDate.of(2025, 12, 31)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun overlappingVersionsAreRejected() {
        val versions = listOf(
            ScheduleVersion(id = 1, effectiveFrom = LocalDate.of(2026, 1, 1), effectiveUntil = LocalDate.of(2026, 3, 1)),
            ScheduleVersion(id = 2, effectiveFrom = LocalDate.of(2026, 2, 1), effectiveUntil = null),
        )
        ScheduleRules.findVersionOn(versions, LocalDate.of(2026, 2, 15))
    }
}
