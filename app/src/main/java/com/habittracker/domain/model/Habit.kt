package com.habittracker.domain.model

import com.habittracker.data.local.entity.FrequencyType
import com.habittracker.data.local.entity.LogStatus
import com.habittracker.data.local.entity.TrackingType
import java.time.LocalDate

data class Habit(
    val id: Long,
    val name: String,
    val icon: String,
    val color: String,
    val trackingType: TrackingType,
    val targetValue: Double,
    val unit: String?,
    val createdOn: LocalDate,
    val archivedOn: LocalDate?,
)

data class HabitSchedule(
    val id: Long,
    val habitId: Long,
    val frequencyType: FrequencyType,
    val weekdayMask: Int,
    val effectiveFrom: LocalDate,
    val effectiveUntil: LocalDate?,
)

data class DailyLog(
    val habitId: Long,
    val date: LocalDate,
    val actualValue: Double,
    val targetSnapshot: Double,
    val status: LogStatus,
    val completedAt: Long?,
)

data class NewSchedule(
    val frequencyType: FrequencyType,
    val weekdayMask: Int?,
)

data class NewHabit(
    val name: String,
    val icon: String,
    val color: String,
    val trackingType: TrackingType,
    val targetValue: Double,
    val unit: String?,
    val initialSchedule: NewSchedule,
)

/** Minimal projection of a plan version used by the pure versioning rules. */
data class ScheduleVersion(
    val id: Long,
    val effectiveFrom: LocalDate,
    val effectiveUntil: LocalDate?,
)
