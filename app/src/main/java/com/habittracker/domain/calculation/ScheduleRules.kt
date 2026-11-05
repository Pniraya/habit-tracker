package com.habittracker.domain.calculation

import com.habittracker.data.local.entity.FrequencyType
import com.habittracker.domain.model.ScheduleVersion
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Pure schedule rules (DATABASE.md 4.2, 6; ADR-007): mask consistency, plan-day matching and the
 * "close the old version, open a new one" algebra. No database access here - the transaction and the
 * cross-row overlap check are wired by the repository.
 */
object ScheduleRules {

    const val ALL_DAYS_MASK: Int = 127
    const val WEEKDAYS_MASK: Int = 31
    private const val MIN_MASK: Int = 1

    data class VersionChange(val closeVersionId: Long, val closeVersionUntil: LocalDate)

    fun maskFor(frequencyType: FrequencyType, customMask: Int?): Int = when (frequencyType) {
        FrequencyType.DAILY -> ALL_DAYS_MASK
        FrequencyType.WEEKDAYS -> WEEKDAYS_MASK
        FrequencyType.CUSTOM -> {
            val mask = requireNotNull(customMask) { "CUSTOM schedule needs an explicit weekday mask" }
            require(mask in MIN_MASK..ALL_DAYS_MASK) { "weekday mask must be within 1..127" }
            mask
        }
    }

    fun bitFor(dayOfWeek: DayOfWeek): Int = 1 shl (dayOfWeek.value - 1)

    fun isScheduledOn(frequencyType: FrequencyType, weekdayMask: Int, date: LocalDate): Boolean {
        val mask = maskFor(frequencyType, if (frequencyType == FrequencyType.CUSTOM) weekdayMask else null)
        require(mask == weekdayMask) { "frequency type and mask disagree" }
        return (weekdayMask and bitFor(date.dayOfWeek)) != 0
    }

    /**
     * Computes how to close the currently open version so that [newFrom] starts a new one.
     * Throws when the change would overlap history or leave a gap.
     */
    fun planVersionChange(versions: List<ScheduleVersion>, newFrom: LocalDate): VersionChange {
        require(versions.isNotEmpty()) { "a habit must have at least one schedule version" }

        val openVersions = versions.filter { it.effectiveUntil == null }
        require(openVersions.size <= 1) { "more than one open schedule version: data integrity error" }
        require(openVersions.isNotEmpty()) { "no open schedule version to close" }

        val latestStart = versions.maxOf { it.effectiveFrom }
        require(newFrom.isAfter(latestStart)) { "new schedule must start after the latest version" }

        val open = openVersions.single()
        val closeUntil = newFrom.minusDays(1)
        require(!closeUntil.isBefore(open.effectiveFrom)) { "closing the version would invert its interval" }

        return VersionChange(closeVersionId = open.id, closeVersionUntil = closeUntil)
    }

    /** Returns the single version valid on [date], or null when the date is outside every version. */
    fun findVersionOn(versions: List<ScheduleVersion>, date: LocalDate): ScheduleVersion? {
        val matches = versions.filter { version ->
            !date.isBefore(version.effectiveFrom) &&
                (version.effectiveUntil == null || !date.isAfter(version.effectiveUntil))
        }
        require(matches.size <= 1) { "overlapping schedule versions on " + date }
        return matches.firstOrNull()
    }
}
