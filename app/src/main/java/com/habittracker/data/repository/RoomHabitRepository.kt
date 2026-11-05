package com.habittracker.data.repository

import androidx.room.withTransaction
import com.habittracker.data.local.HabitDatabase
import com.habittracker.data.local.entity.FrequencyType
import com.habittracker.data.local.entity.HabitEntity
import com.habittracker.data.local.entity.HabitLogEntity
import com.habittracker.data.local.entity.HabitScheduleEntity
import com.habittracker.data.mapper.toDomain
import com.habittracker.domain.calculation.LogStatusRules
import com.habittracker.domain.calculation.ScheduleRules
import com.habittracker.domain.model.DailyLog
import com.habittracker.domain.model.Habit
import com.habittracker.domain.model.HabitSchedule
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
        // The draft is validated and normalised by CreateHabitUseCase (see HabitRepository.createHabit);
        // this layer only persists. The database triggers still reject an inconsistent row.
        database.withTransaction {
            val habitId = habitDao.insert(
                HabitEntity(
                    name = draft.name,
                    icon = draft.icon,
                    color = draft.color,
                    trackingType = draft.trackingType,
                    targetValue = draft.targetValue,
                    unit = draft.unit,
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

    override suspend fun activeSchedule(habitId: Long, date: LocalDate): HabitSchedule? {
        val versions = scheduleDao.findVersionsOn(habitId, date)
        check(versions.size <= 1) { "overlapping schedule versions on " + date }
        return versions.firstOrNull()?.toDomain()
    }

    override suspend fun scheduleVersions(habitId: Long): List<HabitSchedule> =
        scheduleDao.findAll(habitId).map { it.toDomain() }

    override suspend fun logsInRange(habitId: Long, from: LocalDate, to: LocalDate): List<DailyLog> =
        logDao.findRange(habitId, from, to).map { it.toDomain() }

    override suspend fun dailyLog(habitId: Long, date: LocalDate): DailyLog? =
        logDao.findByDate(habitId, date)?.toDomain()

    override suspend fun activeSchedules(
        habitIds: List<Long>,
        date: LocalDate,
    ): Map<Long, HabitSchedule> {
        if (habitIds.isEmpty()) return emptyMap()
        val rows = scheduleDao.findVersionsOnForHabits(habitIds, date)
        val byHabit = rows.groupBy { it.habitId }
        check(byHabit.values.all { it.size <= 1 }) { "overlapping schedule versions on " + date }
        return byHabit.mapValues { (_, versions) -> versions.single().toDomain() }
    }

    override suspend fun dailyLogs(habitIds: List<Long>, date: LocalDate): Map<Long, DailyLog> {
        if (habitIds.isEmpty()) return emptyMap()
        return logDao.findForDate(habitIds, date).associate { it.habitId to it.toDomain() }
    }

    override suspend fun scheduleVersionsFor(habitIds: List<Long>): Map<Long, List<HabitSchedule>> {
        if (habitIds.isEmpty()) return emptyMap()
        return scheduleDao.findAllForHabits(habitIds)
            .groupBy { it.habitId }
            .mapValues { (_, rows) -> rows.map { it.toDomain() } }
    }

    override suspend fun logsInRangeFor(
        habitIds: List<Long>,
        from: LocalDate,
        to: LocalDate,
    ): Map<Long, List<DailyLog>> {
        if (habitIds.isEmpty()) return emptyMap()
        return logDao.findRangeForHabits(habitIds, from, to)
            .groupBy { it.habitId }
            .mapValues { (_, rows) -> rows.map { it.toDomain() } }
    }

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
