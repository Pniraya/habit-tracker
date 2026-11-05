package com.habittracker.domain.usecase

import com.habittracker.data.local.entity.FrequencyType
import com.habittracker.data.local.entity.TrackingType
import com.habittracker.domain.model.NewHabit
import com.habittracker.domain.model.NewSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Every rejection case is arranged BEFORE the call under test and asserts the message, so an
 * exception thrown while arranging cannot masquerade as the behaviour being verified.
 * Status/completedAt are deliberately not asserted here: the fake computes them, so asserting them
 * would only test the fake. The stored values are asserted instead.
 */
class RecordHabitProgressUseCaseTest {

    private val repository = FakeHabitRepository()
    private val createHabit = CreateHabitUseCase(repository)
    private val recordProgress = RecordHabitProgressUseCase(repository)
    private val monday = LocalDate.of(2026, 11, 2)
    private val tuesday = monday.plusDays(1)
    private val now = 1_762_300_000_000L
    private val mondayKey = monday

    private suspend fun booleanHabit(): Long = createHabit(
        NewHabit(
            name = "Read",
            icon = "book",
            color = "#5B8C6A",
            trackingType = TrackingType.BOOLEAN,
            targetValue = 1.0,
            unit = null,
            initialSchedule = NewSchedule(FrequencyType.DAILY, null),
        ),
        monday,
        now,
    )

    private suspend fun numericHabit(): Long = createHabit(
        NewHabit(
            name = "Water",
            icon = "drop",
            color = "#8FAFC4",
            trackingType = TrackingType.NUMERIC,
            targetValue = 2000.0,
            unit = "ml",
            initialSchedule = NewSchedule(FrequencyType.DAILY, null),
        ),
        monday,
        now,
    )

    private fun expectRejection(fragment: String, block: suspend () -> Unit) {
        val error = try {
            kotlinx.coroutines.runBlocking { block() }
            null
        } catch (e: IllegalArgumentException) {
            e
        }
        assertNotNull("expected IllegalArgumentException but nothing was thrown", error)
        assertTrue(
            "message was: " + error!!.message,
            error.message.orEmpty().contains(fragment),
        )
    }

    @Test
    fun recordsTheValueTheCallerPassed() {
        kotlinx.coroutines.runBlocking {
            val id = booleanHabit()
            recordProgress(id, monday, 1.0, now)

            val stored = repository.dailyLog(id, mondayKey)
            assertNotNull(stored)
            assertEquals(1.0, stored!!.actualValue, 0.0)
            assertEquals(1.0, stored.targetSnapshot, 0.0)
        }
    }

    @Test
    fun numericProgressIsStoredWithItsSnapshot() {
        kotlinx.coroutines.runBlocking {
            val id = numericHabit()
            recordProgress(id, monday, 800.0, now)

            val stored = repository.dailyLog(id, mondayKey)
            assertEquals(800.0, stored!!.actualValue, 0.0)
            assertEquals(2000.0, stored.targetSnapshot, 0.0)
        }
    }

    @Test
    fun customScheduleRejectsAnUnscheduledWeekday() {
        val id = kotlinx.coroutines.runBlocking {
            createHabit(
                NewHabit(
                    name = "Gym",
                    icon = "gym",
                    color = "#F2A65A",
                    trackingType = TrackingType.BOOLEAN,
                    targetValue = 1.0,
                    unit = null,
                    initialSchedule = NewSchedule(FrequencyType.CUSTOM, 1),
                ),
                monday,
                now,
            )
        }
        expectRejection("not a scheduled day") { recordProgress(id, tuesday, 1.0, now) }
    }

    @Test
    fun aDateOutsideEveryScheduleVersionIsRejected() {
        val id = kotlinx.coroutines.runBlocking { booleanHabit() }
        val schedule = requireNotNull(repository.storedSchedule(id))
        repository.replaceSchedule(schedule.copy(effectiveFrom = monday.plusDays(10)))

        expectRejection("no schedule version covers") { recordProgress(id, monday, 1.0, now) }
    }

    @Test
    fun booleanHabitRejectsHalfValues() {
        val id = kotlinx.coroutines.runBlocking { booleanHabit() }
        expectRejection("only accepts 0 or 1") { recordProgress(id, monday, 0.5, now) }
    }

    @Test
    fun negativeValuesAreRejected() {
        val id = kotlinx.coroutines.runBlocking { numericHabit() }
        expectRejection("must not be negative") { recordProgress(id, monday, -1.0, now) }
    }

    @Test
    fun archivedHabitsRejectWrites() {
        val id = kotlinx.coroutines.runBlocking {
            val created = booleanHabit()
            ArchiveHabitUseCase(repository)(created, monday, now)
            created
        }
        expectRejection("archived habits cannot record progress") {
            recordProgress(id, monday, 1.0, now)
        }
    }

    @Test
    fun missingHabitIsRejected() {
        expectRejection("habit not found") { recordProgress(999L, monday, 1.0, now) }
    }
}
