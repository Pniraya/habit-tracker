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
class FakeHabitRepository : HabitRepository {

    private val habits = LinkedHashMap<Long, Habit>()
    private val schedules = LinkedHashMap<Long, HabitSchedule>()
    private val logs = LinkedHashMap<String, DailyLog>()
    private val state = MutableStateFlow<List<Habit>>(emptyList())
    private var nextId = 1L

    private fun key(habitId: Long, date: LocalDate): String = habitId.toString() + "@" + date

    fun storedSchedule(habitId: Long): HabitSchedule? = schedules[habitId]

    fun replaceSchedule(schedule: HabitSchedule) {
        schedules[schedule.habitId] = schedule
    }

    override suspend fun createHabit(draft: NewHabit, today: LocalDate, now: Long): Long {
        val id = nextId++
        habits[id] = Habit(
            id = id,
            name = draft.name.trim(),
            icon = draft.icon,
            color = draft.color,
            trackingType = draft.trackingType,
            targetValue = draft.targetValue,
            unit = draft.unit,
            createdOn = today,
            archivedOn = null,
        )
        schedules[id] = HabitSchedule(
            id = nextId++,
            habitId = id,
            frequencyType = draft.initialSchedule.frequencyType,
            weekdayMask = ScheduleRules.maskFor(
                draft.initialSchedule.frequencyType,
                draft.initialSchedule.weekdayMask,
            ),
            effectiveFrom = today,
            effectiveUntil = null,
        )
        state.value = habits.values.toList()
        return id
    }

    override fun observeActiveHabits(): Flow<List<Habit>> =
        state.map { list -> list.filter { habit -> habit.archivedOn == null } }

    override suspend fun getHabit(habitId: Long): Habit? = habits[habitId]

    override suspend fun archiveHabit(habitId: Long, today: LocalDate, now: Long) {
        habits[habitId] = requireNotNull(habits[habitId]).copy(archivedOn = today)
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
        schedules[habitId] = HabitSchedule(
            id = nextId++,
            habitId = habitId,
            frequencyType = frequencyType,
            weekdayMask = ScheduleRules.maskFor(frequencyType, weekdayMask),
            effectiveFrom = effectiveFrom,
            effectiveUntil = null,
        )
    }

    override suspend fun activeSchedule(habitId: Long, date: LocalDate): HabitSchedule? {
        val schedule = schedules[habitId] ?: return null
        if (date.isBefore(schedule.effectiveFrom)) return null
        if (schedule.effectiveUntil != null && date.isAfter(schedule.effectiveUntil)) return null
        return schedule
    }

    override suspend fun dailyLog(habitId: Long, date: LocalDate): DailyLog? = logs[key(habitId, date)]

    override suspend fun recordDailyValue(habitId: Long, date: LocalDate, value: Double, now: Long): DailyLog {
        val habit = requireNotNull(habits[habitId]) { "habit not found" }
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
