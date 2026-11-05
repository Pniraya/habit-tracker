package com.habittracker.feature.today

import com.habittracker.data.local.entity.LogStatus
import com.habittracker.data.local.entity.TrackingType
import java.time.LocalDate

/** One row of the Today list: the habit plus what today's schedule and log say about it. */
data class TodayHabit(
    val id: Long,
    val name: String,
    val icon: String,
    val color: String,
    val trackingType: TrackingType,
    val targetValue: Double,
    val unit: String?,
    val isScheduledToday: Boolean,
    val status: LogStatus?,
    val actualValue: Double,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
) {
    val isCompleted: Boolean get() = status == LogStatus.COMPLETED

    /** True when the habit is not due today, so the row is shown but cannot be recorded. */
    val isRestingToday: Boolean get() = !isScheduledToday
}

data class TodayUiState(
    val date: LocalDate,
    val habits: List<TodayHabit> = emptyList(),
    val isLoading: Boolean = true,
)
