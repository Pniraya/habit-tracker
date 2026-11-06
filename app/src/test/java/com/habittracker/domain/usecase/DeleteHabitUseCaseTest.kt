package com.habittracker.domain.usecase

import com.habittracker.data.local.entity.FrequencyType
import com.habittracker.data.local.entity.TrackingType
import com.habittracker.domain.model.NewHabit
import com.habittracker.domain.model.NewSchedule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DeleteHabitUseCaseTest {

    private val repository = FakeHabitRepository()
    private val today = LocalDate.of(2026, 11, 6)
    private val delete = DeleteHabitUseCase(repository)

    private suspend fun seed(name: String = "Walk"): Long = repository.createHabit(
        NewHabit(
            name = name,
            icon = "sprout",
            color = "#5B8C6A",
            trackingType = TrackingType.BOOLEAN,
            targetValue = 1.0,
            unit = null,
            initialSchedule = NewSchedule(frequencyType = FrequencyType.DAILY, weekdayMask = null),
        ),
        today = today,
        now = 1_762_300_000_000L,
    )

    @Test
    fun deletingAHabitRemovesItAndItsChildren() = runBlocking {
        val id = seed()

        delete(id)

        assertNull(repository.getHabit(id))
        assertNull(repository.storedSchedule(id))
        assertEquals(0, repository.snapshotHabits().size)
    }

    @Test
    fun anArchivedHabitCanStillBeDeleted() = runBlocking {
        // Archive is the default way to hide a habit; deleting it afterwards must work too,
        // otherwise an archived habit could never leave the database.
        val id = seed()
        repository.archiveHabit(id, today, 1_762_300_000_000L)

        delete(id)

        assertNull(repository.getHabit(id))
    }

    @Test
    fun aMissingIdIsRejectedInsteadOfSilentlySucceeding() = runBlocking {
        val failure = runCatching { delete(99L) }.exceptionOrNull()

        assertEquals(IllegalArgumentException::class.java, failure?.javaClass)
    }
}
