package com.habittracker.domain.calculation

import com.habittracker.data.local.entity.LogStatus
import com.habittracker.data.local.entity.TrackingType

/**
 * Single source of truth for the log status invariant (DATABASE.md 4.3, ADR-006): [status] and
 * [completedAt] are always computed together, never set independently.
 *
 * "First time reached" semantics: an existing completedAt is kept while the day stays completed, so
 * the timestamp does not move on every later increment. Dropping below the target clears it, and
 * reaching the target again yields a new timestamp.
 */
object LogStatusRules {

    data class Evaluation(val status: LogStatus, val completedAt: Long?)

    fun evaluate(
        trackingType: TrackingType,
        actualValue: Double,
        targetSnapshot: Double,
        previousCompletedAt: Long?,
        now: Long,
    ): Evaluation {
        require(targetSnapshot > 0.0) { "target snapshot must be positive" }
        require(actualValue >= 0.0) { "actual value must not be negative" }
        if (trackingType == TrackingType.BOOLEAN) {
            require(actualValue == 0.0 || actualValue == 1.0) { "BOOLEAN log value must be 0 or 1" }
        }

        val reached = actualValue >= targetSnapshot
        return if (reached) {
            Evaluation(LogStatus.COMPLETED, previousCompletedAt ?: now)
        } else {
            Evaluation(LogStatus.IN_PROGRESS, null)
        }
    }
}
