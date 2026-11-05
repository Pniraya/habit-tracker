package com.habittracker.feature.create

import com.habittracker.data.local.entity.FrequencyType
import com.habittracker.data.local.entity.TrackingType
import java.time.DayOfWeek

/** Everything the create-habit form needs to render, including its validation result. */
data class CreateHabitForm(
    val name: String = "",
    val trackingType: TrackingType = TrackingType.BOOLEAN,
    val targetValue: String = "",
    val unit: String = "",
    val frequency: FrequencyType = FrequencyType.DAILY,
    val weekdays: Set<DayOfWeek> = emptySet(),
    val isSaving: Boolean = false,
    val error: String? = null,
    val createdHabitId: Long? = null,
) {
    val needsTargetValue: Boolean get() = trackingType == TrackingType.NUMERIC
    val needsWeekdays: Boolean get() = frequency == FrequencyType.CUSTOM
}
