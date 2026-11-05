package com.habittracker.domain.usecase

import com.habittracker.data.local.entity.FrequencyType
import com.habittracker.data.local.entity.TrackingType
import com.habittracker.domain.model.NewHabit
import com.habittracker.domain.model.NewSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CreateAndArchiveHabitUseCaseTest {

    private val repository = FakeHabitRepository()
    private val createHabit = CreateHabitUseCase(repository)
    private val archiveHabit = ArchiveHabitUseCase(repository)
    private val today = LocalDate.of(2026, 11, 2)
    private val now = 1_762_300_000_000L

    private fun draft(name: String = "Read"): NewHabit = NewHabit(
        name = name,
        icon = "book",
        color = "#5B8C6A",
        trackingType = TrackingType.BOOLEAN,
        targetValue = 1.0,
        unit = null,
        initialSchedule = NewSchedule(FrequencyType.DAILY, null),
    )

    private fun expectRejection(fragment: String, block: suspend () -> Unit) {
        val error = try {
            kotlinx.coroutines.runBlocking { block() }
            null
        } catch (e: IllegalArgumentException) {
            e
        }
        assertNotNull("expected IllegalArgumentException but nothing was thrown", error)
        assertTrue("message was: " + error!!.message, error.message.orEmpty().contains(fragment))
    }

    @Test
    fun creatingAHabitAlsoCreatesItsFirstScheduleVersion() {
        kotlinx.coroutines.runBlocking {
            val id = createHabit(draft(), today, now)
            val schedule = repository.storedSchedule(id)
            assertNotNull(schedule)
            assertEquals(today, schedule!!.effectiveFrom)
            assertNull(schedule.effectiveUntil)
            assertEquals(127, schedule.weekdayMask)
        }
    }

    @Test
    fun blankNamesNeverReachTheRepository() {
        expectRejection("must not be blank") { createHabit(draft("   "), today, now) }
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun archivingStampsTheLocalDate() {
        kotlinx.coroutines.runBlocking {
            val id = createHabit(draft(), today, now)
            archiveHabit(id, today, now)
            assertEquals(today, repository.getHabit(id)?.archivedOn)
        }
    }

    @Test
    fun archivingTwiceIsRejectedByTheUseCase() {
        val id = kotlinx.coroutines.runBlocking {
            val created = createHabit(draft(), today, now)
            archiveHabit(created, today, now)
            created
        }
        expectRejection("already archived") { archiveHabit(id, today, now) }
    }

    @Test
    fun archivingAnUnknownHabitIsRejectedByTheUseCase() {
        expectRejection("habit not found") { archiveHabit(999L, today, now) }
    }
}
