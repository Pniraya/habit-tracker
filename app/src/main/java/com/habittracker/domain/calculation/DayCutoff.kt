package com.habittracker.domain.calculation

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Product decision ADR-016: a habit "day" runs from 04:00 to 03:59:59 local time.
 *
 * A completion at 01:30 belongs to the previous calendar date, so a user who stays up past
 * midnight neither breaks a streak nor double-counts a day. Every feature must obtain "today"
 * from here instead of `LocalDate.now()`; the rule lives in one object because a second
 * interpretation would make streak, statistics and archive dates disagree.
 */
object DayCutoff {

    const val CUTOFF_HOUR: Int = 4

    /** Logical date of a wall-clock instant: anything before 04:00 counts as the previous day. */
    fun logicalDate(at: LocalDateTime): LocalDate = at.minusHours(CUTOFF_HOUR.toLong()).toLocalDate()

    fun today(at: LocalDateTime = LocalDateTime.now()): LocalDate = logicalDate(at)

    fun logicalDateOf(nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        logicalDate(Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDateTime())

    /**
     * Milliseconds from [now] until the next 04:00 cutoff, i.e. when the displayed day changes.
     * Zone-aware so a DST transition is not turned into a 23- or 25-hour timer.
     */
    fun millisUntilNextCutoff(now: LocalDateTime, zone: ZoneId = ZoneId.systemDefault()): Long {
        val cutoff = logicalDate(now).plusDays(1).atTime(CUTOFF_HOUR, 0).atZone(zone)
        return Duration.between(now.atZone(zone), cutoff).toMillis()
    }
}
