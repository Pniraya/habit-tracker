package com.habittracker.data.repository

import com.habittracker.data.local.dao.HabitScheduleDao
import com.habittracker.data.local.entity.FrequencyType
import com.habittracker.data.local.entity.HabitScheduleEntity
import com.habittracker.data.mapper.toVersion
import com.habittracker.domain.calculation.ScheduleRules
import java.time.LocalDate

/**
 * The only writer of schedule versions (DATABASE.md 4.2). It must be called inside a Room
 * transaction: it reads every version, validates the new interval, closes the open version and
 * inserts the replacement, so a concurrent reader never sees two open versions.
 */
class HabitScheduleWriter(private val scheduleDao: HabitScheduleDao) {

    suspend fun replaceSchedule(
        habitId: Long,
        frequencyType: FrequencyType,
        customMask: Int?,
        effectiveFrom: LocalDate,
        now: Long,
    ): HabitScheduleEntity {
        val versions = scheduleDao.findAll(habitId).map { it.toVersion() }
        val change = ScheduleRules.planVersionChange(versions, effectiveFrom)
        val closed = scheduleDao.closeVersion(change.closeVersionId, change.closeVersionUntil)
        check(closed == 1) { "schedule version was already closed concurrently" }

        val entity = HabitScheduleEntity(
            habitId = habitId,
            frequencyType = frequencyType,
            weekdayMask = ScheduleRules.maskFor(frequencyType, customMask),
            effectiveFrom = effectiveFrom,
            effectiveUntil = null,
            createdAt = now,
        )
        val id = scheduleDao.insert(entity)
        return entity.copy(id = id)
    }
}
