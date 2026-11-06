package com.habittracker.domain.calculation

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class DayCutoffTest {

    private fun at(text: String): LocalDateTime = LocalDateTime.parse(text)

    @Test
    fun everythingBeforeFourAmBelongsToThePreviousDay() {
        assertEquals(LocalDate.of(2026, 11, 5), DayCutoff.today(at("2026-11-05T23:59:59")))
        assertEquals(LocalDate.of(2026, 11, 5), DayCutoff.today(at("2026-11-06T00:00:00")))
        assertEquals(LocalDate.of(2026, 11, 5), DayCutoff.today(at("2026-11-06T03:59:59")))
    }

    @Test
    fun exactlyFourAmStartsTheNewDay() {
        assertEquals(LocalDate.of(2026, 11, 6), DayCutoff.today(at("2026-11-06T04:00:00")))
        assertEquals(LocalDate.of(2026, 11, 6), DayCutoff.today(at("2026-11-06T04:00:01")))
    }

    @Test
    fun theMillisHelperUsesTheSameCutoff() {
        // 03:59:59 -> one second to the cutoff.
        assertEquals(1_000L, DayCutoff.millisUntilNextCutoff(at("2026-11-06T03:59:59")))
        // 04:00:00 -> a whole habit day (24h) to the following cutoff.
        assertEquals(86_400_000L, DayCutoff.millisUntilNextCutoff(at("2026-11-06T04:00:00")))
        // 01:00 is four hours before the cutoff, not one day and four hours.
        assertEquals(3L * 3_600_000L, DayCutoff.millisUntilNextCutoff(at("2026-11-06T01:00:00")))
    }

    @Test
    fun epochMillisAndWallClockAgree() {
        val zone = ZoneId.of("Asia/Shanghai")
        val instant = at("2026-11-06T03:30:00").atZone(zone).toInstant()
        assertEquals(LocalDate.of(2026, 11, 5), DayCutoff.logicalDateOf(instant.toEpochMilli(), zone))
    }

    @Test
    fun aLeapDayIsAnOrdinaryDay() {
        assertEquals(LocalDate.of(2028, 2, 29), DayCutoff.today(at("2028-02-29T04:00:00")))
        assertEquals(LocalDate.of(2028, 2, 28), DayCutoff.today(at("2028-02-29T03:59:00")))
        // 2028-02-28 04:00 -> next cutoff is 2028-02-29 04:00, i.e. a full day.
        assertEquals(86_400_000L, DayCutoff.millisUntilNextCutoff(at("2028-02-28T04:00:00")))
    }
}
