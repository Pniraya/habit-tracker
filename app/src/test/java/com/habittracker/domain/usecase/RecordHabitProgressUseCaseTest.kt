package com.habittracker.domain.usecase

import com.habittracker.data.local.entity.FrequencyType
import com.habittracker.data.local.entity.LogStatus
import com.habittracker.data.local.entity.TrackingType
import com.habittracker.domain.model.NewHabit
import com.habittracker.domain.model.NewSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RecordHabitProgressUseCaseTest {

    private val repository = FakeHabitRepository()
    private val createHabit = CreateHabitUseCase(repository)
    private val recordProgress = RecordHabitProgressUseCase(repository)
    private val monday = LocalDate.of(2026, 11, 2)
    private val tuesday = monday.plusDays(1)
    private val now = 1_762_300_000_000L

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

    @Test
    fun booleanCompletionIsRecordedOnAScheduledDay() {
        val log = kotlinx.coroutines.runBlocking {
            val id = booleanHabit()
            recordProgress(id, monday, 1.0, now)
        }
        assertEquals(LogStatus.COMPLETED, log.status)
        assertEquals(now, log.completedAt)
    }

    @Test
    fun numericPartialValueStaysInProgress() {
        val log = kotlinx.coroutines.runBlocking {
            val id = numericHabit()
            recordProgress(id, monday, 800.0, now)
        }
        assertEquals(LogStatus.IN_PROGRESS, log.status)
        assertNull(log.completedAt)
    }

    @Test(expected = IllegalArgumentException::class)
    fun customScheduleRejectsAnUnscheduledWeekday() {
        kotlinx.coroutines.runBlocking {
            val id = createHabit(
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
            recordProgress(id, tuesday, 1.0, now)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun aDateOutsideEveryScheduleVersionIsRejected() {
        kotlinx.coroutines.runBlocking {
            val id = booleanHabit()
            repository.replaceSchedule(
                requireNotNull(repository.storedSchedule(id)).copy(effectiveFrom = monday.plusDays(10)),
            )
            recordProgress(id, monday, 1.0, now)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun booleanHabitRejectsHalfValues() {
        kotlinx.coroutines.runBlocking {
            val id = booleanHabit()
            recordProgress(id, monday, 0.5, now)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeValuesAreRejected() {
        kotlinx.coroutines.runBlocking {
            val id = numericHabit()
            recordProgress(id, monday, -1.0, now)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun archivedHabitsRejectWrites() {
        kotlinx.coroutines.runBlocking {
            val id = booleanHabit()
            ArchiveHabitUseCase(repository)(id, monday, now)
            recordProgress(id, monday, 1.0, now)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun missingHabitIsRejected() {
        kotlinx.coroutines.runBlocking { recordProgress(999L, monday, 1.0, now) }
    }
}
