package com.habittracker.data.mapper

import com.habittracker.data.local.entity.HabitEntity
import com.habittracker.data.local.entity.HabitLogEntity
import com.habittracker.data.local.entity.HabitScheduleEntity
import com.habittracker.domain.model.DailyLog
import com.habittracker.domain.model.Habit
import com.habittracker.domain.model.HabitSchedule
import com.habittracker.domain.model.ScheduleVersion

fun HabitEntity.toDomain(): Habit = Habit(
    id = id,
    name = name,
    icon = icon,
    color = color,
    trackingType = trackingType,
    targetValue = targetValue,
    unit = unit,
    createdOn = createdOn,
    archivedOn = archivedOn,
)

fun HabitScheduleEntity.toDomain(): HabitSchedule = HabitSchedule(
    id = id,
    habitId = habitId,
    frequencyType = frequencyType,
    weekdayMask = weekdayMask,
    effectiveFrom = effectiveFrom,
    effectiveUntil = effectiveUntil,
)

fun HabitScheduleEntity.toVersion(): ScheduleVersion =
    ScheduleVersion(id = id, effectiveFrom = effectiveFrom, effectiveUntil = effectiveUntil)

fun HabitLogEntity.toDomain(): DailyLog = DailyLog(
    habitId = habitId,
    date = logDate,
    actualValue = actualValue,
    targetSnapshot = targetSnapshot,
    status = status,
    completedAt = completedAt,
)
