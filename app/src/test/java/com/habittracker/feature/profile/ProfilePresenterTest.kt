package com.habittracker.feature.profile

import com.habittracker.data.local.entity.FrequencyType
import com.habittracker.data.local.entity.TrackingType
import com.habittracker.domain.model.NewHabit
import com.habittracker.domain.model.NewSchedule
import com.habittracker.domain.usecase.ArchiveHabitUseCase
import com.habittracker.domain.usecase.CreateHabitUseCase
import com.habittracker.domain.usecase.FakeHabitRepository
import com.habittracker.domain.usecase.RestoreHabitUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProfilePresenterTest {

    private val repository = FakeHabitRepository()
    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val monday = LocalDate.of(2026, 11, 2)
    private val now = 1_762_300_000_000L

    private fun presenter() = ProfilePresenter(
        repository = repository,
        restoreHabit = RestoreHabitUseCase(repository),
        scope = scope,
        now = { now },
    )

    private fun createHabit(name: String): Long = runBlocking {
        CreateHabitUseCase(repository)(
            NewHabit(
                name = name,
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
    }

    private fun archive(habitId: Long) = runBlocking { ArchiveHabitUseCase(repository)(habitId, monday, now) }

    @Test
    fun onlyArchivedHabitsAreListed() {
        val keep = createHabit("Read")
        val gone = createHabit("Run")
        archive(gone)

        val ids = presenter().state.value.archived.map { it.id }

        assertEquals(listOf(gone), ids)
        assertFalse(ids.contains(keep))
    }

    @Test
    fun restoringBringsTheHabitBackAndRemovesTheRow() {
        val id = createHabit("Read")
        archive(id)
        val presenter = presenter()
        assertEquals(1, presenter.state.value.archived.size)

        presenter.restore(id)

        assertTrue(presenter.state.value.archived.isEmpty())
        assertNull(presenter.lastError.value)
        // And the habit is active again, which is what the Today list reads.
        val active = runBlocking { repository.getHabit(id) }
        assertNull(active?.archivedOn)
    }

    @Test
    fun restoringTwiceReportsTheSecondAsAnError() {
        val id = createHabit("Read")
        archive(id)
        val presenter = presenter()

        presenter.restore(id)
        assertNull(presenter.lastError.value)

        presenter.restore(id)

        assertNotNull(presenter.lastError.value)
    }

    @Test
    fun theRestoreRebuildDoesNotWaitForTheRepositoryFlow() {
        val repository = object : FakeHabitRepository() {
            override fun observeArchivedHabits(): kotlinx.coroutines.flow.Flow<List<com.habittracker.domain.model.Habit>> =
                kotlinx.coroutines.flow.flow { emit(snapshotHabits().filter { it.archivedOn != null }) }
        }
        val id = runBlocking {
            CreateHabitUseCase(repository)(
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
        }
        runBlocking { ArchiveHabitUseCase(repository)(id, monday, now) }
        val presenter = ProfilePresenter(
            repository = repository,
            restoreHabit = RestoreHabitUseCase(repository),
            scope = scope,
            now = { now },
        )
        assertEquals(1, presenter.state.value.archived.size)

        presenter.restore(id)

        assertTrue(presenter.state.value.archived.isEmpty())
    }

    @Test
    fun theEmptyWordingIsNotAnErrorMessage() {
        assertEquals("没有已归档的习惯。", ProfilePresenter.emptyText())
        assertFalse(ProfilePresenter.showsList(ProfileUiState(isLoading = false)))
        assertFalse(ProfilePresenter.showsList(ProfileUiState(isLoading = true)))
    }
}