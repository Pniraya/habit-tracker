package com.habittracker.domain.usecase

import com.habittracker.data.local.entity.FrequencyType
import com.habittracker.domain.calculation.LogStatusRules
import com.habittracker.domain.calculation.ScheduleRules
import com.habittracker.domain.model.DailyLog
import com.habittracker.domain.model.Habit
import com.habittracker.domain.model.HabitSchedule
import com.habittracker.domain.model.NewHabit
import com.habittracker.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** In-memory repository for pure JVM use-case tests; mirrors the Room implementation's contract. */
open class FakeHabitRepository : HabitRepository {

    private val habits = LinkedHashMap<Long, Habit>()
    private val schedules = LinkedHashMap<Long, MutableList<HabitSchedule>>()
    private val logs = LinkedHashMap<String, DailyLog>()
    private val state = MutableStateFlow<List<Habit>>(emptyList())
    private var nextId = 1L

    private fun key(habitId: Long, date: LocalDate): String = habitId.toString() + "@" + date

    var createCalls: Int = 0
        private set

    /** Counts repository reads so a test can prove the Today list does not scale queries by habit. */
    var readCalls: Int = 0
        private set

    fun resetReadCalls() {
        readCalls = 0
    }

    fun storedSchedule(habitId: Long): HabitSchedule? = schedules[habitId]?.lastOrNull()

    /** Current rows, for a subclass whose Flow does not re-emit (see TodayPresenterTest). */
    fun snapshotHabits(): List<Habit> = habits.values.toList()

    fun replaceSchedule(schedule: HabitSchedule) {
        schedules[schedule.habitId] = mutableListOf(schedule)
    }

    override suspend fun createHabit(draft: NewHabit, today: LocalDate, now: Long): Long {
        // Deliberately dumb: validation lives in CreateHabitUseCase now, and a fake that repeats
        // the rules would hide a use case that forgot them. It stores exactly what it is given.
        createCalls += 1
        val id = nextId++
        habits[id] = Habit(
            id = id,
            name = draft.name,
            icon = draft.icon,
            color = draft.color,
            trackingType = draft.trackingType,
            targetValue = draft.targetValue,
            unit = draft.unit,
            createdOn = today,
            archivedOn = null,
        )
        schedules[id] = mutableListOf(
            HabitSchedule(
                id = nextId++,
                habitId = id,
                frequencyType = draft.initialSchedule.frequencyType,
                weekdayMask = ScheduleRules.maskFor(
                    draft.initialSchedule.frequencyType,
                    draft.initialSchedule.weekdayMask,
                ),
                effectiveFrom = today,
                effectiveUntil = null,
            ),
        )
        state.value = habits.values.toList()
        return id
    }

    override fun observeActiveHabits(): Flow<List<Habit>> =
        state.map { list -> list.filter { habit -> habit.archivedOn == null } }

    override fun observeArchivedHabits(): Flow<List<Habit>> =
        state.map { list -> list.filter { habit -> habit.archivedOn != null } }

    override suspend fun getHabit(habitId: Long): Habit? = habits[habitId]

    // Deliberately permissive: a missing id is a no-op here, so a test can only pass when the
    // use case itself validates existence.
    override suspend fun archiveHabit(habitId: Long, today: LocalDate, now: Long) {
        val current = habits[habitId] ?: return
        habits[habitId] = current.copy(archivedOn = today)
        state.value = habits.values.toList()
    }

    override suspend fun updateHabit(
        habitId: Long,
        name: String,
        targetValue: Double,
        unit: String?,
        now: Long,
    ): Boolean {
        val current = habits[habitId] ?: return false
        if (current.archivedOn != null) return false
        habits[habitId] = current.copy(name = name, targetValue = targetValue, unit = unit)
        state.value = habits.values.toList()
        return true
    }

    override suspend fun deleteHabit(habitId: Long) {
        // Mirrors ON DELETE CASCADE: children never outlive their habit.
        habits.remove(habitId)
        schedules.remove(habitId)
        logs.keys.removeAll { key -> key.startsWith(habitId.toString() + "@") }
        state.value = habits.values.toList()
    }

    override suspend fun restoreHabit(habitId: Long, now: Long) {
        habits[habitId] = requireNotNull(habits[habitId]).copy(archivedOn = null)
        state.value = habits.values.toList()
    }

    override suspend fun changeSchedule(
        habitId: Long,
        frequencyType: FrequencyType,
        weekdayMask: Int?,
        effectiveFrom: LocalDate,
        now: Long,
    ) {
        val versions = schedules.getOrPut(habitId) { mutableListOf() }
        // Close the previous version the way the Room writer does, so the fake keeps history.
        versions.lastOrNull()?.let { open ->
            if (open.effectiveUntil == null) {
                versions[versions.lastIndex] = open.copy(effectiveUntil = effectiveFrom.minusDays(1))
            }
        }
        versions += HabitSchedule(
            id = nextId++,
            habitId = habitId,
            frequencyType = frequencyType,
            weekdayMask = ScheduleRules.maskFor(frequencyType, weekdayMask),
            effectiveFrom = effectiveFrom,
            effectiveUntil = null,
        )
    }

    override suspend fun activeSchedule(habitId: Long, date: LocalDate): HabitSchedule? {
        readCalls += 1
        return schedules[habitId]?.firstOrNull { schedule ->
            !date.isBefore(schedule.effectiveFrom) &&
                (schedule.effectiveUntil == null || !date.isAfter(schedule.effectiveUntil))
        }
    }

    override suspend fun dailyLog(habitId: Long, date: LocalDate): DailyLog? {
        readCalls += 1
        return logs[key(habitId, date)]
    }

    override suspend fun scheduleVersions(habitId: Long): List<HabitSchedule> {
        readCalls += 1
        return schedules[habitId].orEmpty().sortedBy { it.effectiveFrom }
    }

    override suspend fun logsInRange(habitId: Long, from: LocalDate, to: LocalDate): List<DailyLog> {
        readCalls += 1
        return logs.values
            .filter { it.habitId == habitId && !it.date.isBefore(from) && !it.date.isAfter(to) }
            .sortedBy { it.date }
    }

    override suspend fun activeSchedules(
        habitIds: List<Long>,
        date: LocalDate,
    ): Map<Long, HabitSchedule> {
        readCalls += 1
        return habitIds.mapNotNull { id ->
            schedules[id]
                ?.firstOrNull { schedule ->
                    !date.isBefore(schedule.effectiveFrom) &&
                        (schedule.effectiveUntil == null || !date.isAfter(schedule.effectiveUntil))
                }
                ?.let { id to it }
        }.toMap()
    }

    override suspend fun dailyLogs(habitIds: List<Long>, date: LocalDate): Map<Long, DailyLog> {
        readCalls += 1
        return habitIds.mapNotNull { id -> logs[key(id, date)]?.let { id to it } }.toMap()
    }

    override suspend fun scheduleVersionsFor(habitIds: List<Long>): Map<Long, List<HabitSchedule>> {
        readCalls += 1
        return habitIds.associateWith { id -> schedules[id].orEmpty().sortedBy { it.effectiveFrom } }
    }

    override suspend fun logsInRangeFor(
        habitIds: List<Long>,
        from: LocalDate,
        to: LocalDate,
    ): Map<Long, List<DailyLog>> {
        readCalls += 1
        return habitIds.associateWith { id ->
            logs.values
                .filter { it.habitId == id && !it.date.isBefore(from) && !it.date.isAfter(to) }
                .sortedBy { it.date }
        }
    }

    override suspend fun recordDailyValue(habitId: Long, date: LocalDate, value: Double, now: Long): DailyLog {
        val habit = habits[habitId] ?: error("fake repository: habit not found")
        val existing = logs[key(habitId, date)]
        val evaluation = LogStatusRules.evaluate(
            trackingType = habit.trackingType,
            actualValue = value,
            targetSnapshot = habit.targetValue,
            previousCompletedAt = existing?.completedAt,
            now = now,
        )
        val log = DailyLog(habitId, date, value, habit.targetValue, evaluation.status, evaluation.completedAt)
        logs[key(habitId, date)] = log
        return log
    }
}
