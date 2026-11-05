package com.habittracker.data.repository

import androidx.room.withTransaction
import com.habittracker.data.local.HabitDatabase
import com.habittracker.data.local.entity.FrequencyType
import com.habittracker.data.local.entity.HabitEntity
import com.habittracker.data.local.entity.HabitLogEntity
import com.habittracker.data.local.entity.HabitScheduleEntity
import com.habittracker.data.mapper.toDomain
import com.habittracker.domain.calculation.HabitRules
import com.habittracker.domain.calculation.LogStatusRules
import com.habittracker.domain.calculation.ScheduleRules
import com.habittracker.domain.model.DailyLog
import com.habittracker.domain.model.Habit
import com.habittracker.domain.model.NewHabit
import com.habittracker.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class RoomHabitRepository(private val database: HabitDatabase) : HabitRepository {

    private val habitDao = database.habitDao()
    private val logDao = database.habitLogDao()
    private val scheduleDao = database.habitScheduleDao()
    private val scheduleWriter = HabitScheduleWriter(scheduleDao)

    override suspend fun createHabit(draft: NewHabit, today: LocalDate, now: Long): Long =
        database.withTransaction {
            HabitRules.validateName(draft.name)
            val target = HabitRules.normalizeTarget(draft.trackingType, draft.targetValue, draft.unit)

            val habitId = habitDao.insert(
                HabitEntity(
                    name = draft.name.trim(),
                    icon = draft.icon,
                    color = draft.color,
                    trackingType = draft.trackingType,
                    targetValue = target.targetValue,
                    unit = target.unit,
                    createdAt = now,
                    updatedAt = now,
                    createdOn = today,
                ),
            )
            check(habitId > 0) { "habit insert returned no id" }

            scheduleDao.insert(
                HabitScheduleEntity(
                    habitId = habitId,
                    frequencyType = draft.initialSchedule.frequencyType,
                    weekdayMask = ScheduleRules.maskFor(
                        draft.initialSchedule.frequencyType,
                        draft.initialSchedule.weekdayMask,
                    ),
                    effectiveFrom = today,
                    effectiveUntil = null,
                    createdAt = now,
                ),
            )
            habitId
        }

    override fun observeActiveHabits(): Flow<List<Habit>> =
        habitDao.observeActive().map { habits -> habits.map(HabitEntity::toDomain) }

    override suspend fun getHabit(habitId: Long): Habit? = habitDao.findById(habitId)?.toDomain()

    override suspend fun archiveHabit(habitId: Long, today: LocalDate, now: Long) {
        val changed = habitDao.archive(id = habitId, archivedAt = now, archivedOn = today, updatedAt = now)
        check(changed == 1) { "habit not found or already archived" }
    }

    override suspend fun restoreHabit(habitId: Long, now: Long) {
        val changed = habitDao.restore(id = habitId, updatedAt = now)
        check(changed == 1) { "habit not found" }
    }

    override suspend fun changeSchedule(
        habitId: Long,
        frequencyType: FrequencyType,
        weekdayMask: Int?,
        effectiveFrom: LocalDate,
        now: Long,
    ) {
        database.withTransaction {
            scheduleWriter.replaceSchedule(habitId, frequencyType, weekdayMask, effectiveFrom, now)
        }
    }

    override suspend fun recordDailyValue(
        habitId: Long,
        date: LocalDate,
        value: Double,
        now: Long,
    ): DailyLog = database.withTransaction {
        val habit = requireNotNull(habitDao.findById(habitId)) { "habit not found" }
        require(habit.archivedOn == null) { "habit is archived" }

        val existing = logDao.findByDate(habitId, date)
        val evaluation = LogStatusRules.evaluate(
            trackingType = habit.trackingType,
            actualValue = value,
            targetSnapshot = habit.targetValue,
            previousCompletedAt = existing?.completedAt,
            now = now,
        )

        val log = HabitLogEntity(
            id = existing?.id ?: 0,
            habitId = habitId,
            logDate = date,
            actualValue = value,
            targetSnapshot = habit.targetValue,
            status = evaluation.status,
            completedAt = evaluation.completedAt,
            durationSeconds = existing?.durationSeconds,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        logDao.upsert(log)
        log.toDomain()
    }
}
