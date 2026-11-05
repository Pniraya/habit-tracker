package com.habittracker.domain.calculation

import com.habittracker.data.local.entity.TrackingType

/**
 * Pure validation of habit fields. Mirrors DATABASE.md 4.1 so the UI can fail before touching the
 * database, while the database triggers remain the final backstop.
 */
object HabitRules {

    data class NormalizedTarget(val targetValue: Double, val unit: String?)

    fun validateName(name: String) {
        require(name.trim().isNotEmpty()) { "habit name must not be blank" }
    }

    fun normalizeTarget(trackingType: TrackingType, targetValue: Double, unit: String?): NormalizedTarget =
        when (trackingType) {
            TrackingType.BOOLEAN -> {
                require(targetValue == 1.0) { "BOOLEAN habit target must be exactly 1" }
                require(unit == null) { "BOOLEAN habit must not have a unit" }
                NormalizedTarget(targetValue = 1.0, unit = null)
            }

            TrackingType.NUMERIC -> {
                require(targetValue > 0.0) { "NUMERIC habit target must be positive" }
                require(!unit.isNullOrBlank()) { "NUMERIC habit needs a unit" }
                NormalizedTarget(targetValue = targetValue, unit = unit!!.trim())
            }
        }
}
