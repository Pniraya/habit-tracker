package com.habittracker.feature.today

import com.habittracker.data.local.entity.FrequencyType
import com.habittracker.data.local.entity.LogStatus
import com.habittracker.data.local.entity.TrackingType
import com.habittracker.domain.model.NewHabit
import com.habittracker.domain.model.NewSchedule
import com.habittracker.domain.usecase.ArchiveHabitUseCase
import com.habittracker.domain.usecase.CreateHabitUseCase
import com.habittracker.domain.usecase.FakeHabitRepository
import com.habittracker.domain.usecase.RecordHabitProgressUseCase
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

/**
 * The presenter is plain Kotlin, so these tests run on the JVM with an unconfined dispatcher:
 * no Android ViewModel, no main-thread dispatcher, no Robolectric.
 */
class TodayPresenterTest {

    private val repository = FakeHabitRepository()
    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val monday = LocalDate.of(2026, 11, 2)
    private val now = 1_762_300_000_000L

    private fun presenter(date: LocalDate = monday): TodayPresenter = TodayPresenter(
        repository = repository,
        recordProgress = RecordHabitProgressUseCase(repository),
        scope = scope,
        today = { date },
        now = { now },
    )

    private fun createHabit(trackingType: TrackingType, name: String, frequency: FrequencyType = FrequencyType.DAILY, mask: Int? = null): Long =
        runBlocking {
            CreateHabitUseCase(repository)(
                NewHabit(
                    name = name,
                    icon = "book",
                    color = "#5B8C6A",
                    trackingType = trackingType,
                    targetValue = if (trackingType == TrackingType.NUMERIC) 2000.0 else 1.0,
                    unit = if (trackingType == TrackingType.NUMERIC) "ml" else null,
                    initialSchedule = NewSchedule(frequency, mask),
                ),
                monday,
                now,
            )
        }

    @Test
    fun loadsTodaysHabitsWithTheirScheduleState() {
        createHabit(TrackingType.BOOLEAN, "Read")

        val state = presenter().state.value

        assertFalse(state.isLoading)
        assertEquals(1, state.habits.size)
        assertTrue(state.habits.single().isScheduledToday)
        assertNull(state.habits.single().status)
    }

    @Test
    fun togglingABooleanHabitRecordsCompletionAndRefreshesTheRow() {
        val id = createHabit(TrackingType.BOOLEAN, "Read")
        val presenter = presenter()

        presenter.toggle(id)

        val stored = runBlocking { repository.dailyLog(id, monday) }
        assertEquals(LogStatus.COMPLETED, stored?.status)
        assertEquals(1.0, stored!!.actualValue, 0.0)
        assertTrue(presenter.state.value.habits.single().isCompleted)
    }

    @Test
    fun togglingAgainClearsTheDayInsteadOfStackingValues() {
        val id = createHabit(TrackingType.BOOLEAN, "Read")
        val presenter = presenter()

        presenter.toggle(id)
        presenter.toggle(id)

        val stored = runBlocking { repository.dailyLog(id, monday) }
        assertEquals(0.0, stored!!.actualValue, 0.0)
        assertFalse(presenter.state.value.habits.single().isCompleted)
    }

    @Test
    fun numericProgressIsStoredAndShownAgainstTheTarget() {
        val id = createHabit(TrackingType.NUMERIC, "Water")
        val presenter = presenter()

        presenter.record(id, 800.0)

        val stored = runBlocking { repository.dailyLog(id, monday) }
        assertEquals(800.0, stored!!.actualValue, 0.0)
        assertEquals(2000.0, stored.targetSnapshot, 0.0)
        assertEquals(800.0, presenter.state.value.habits.single().actualValue, 0.0)
    }

    @Test
    fun aHabitScheduledForAnotherWeekdayIsMarkedAsResting() {
        createHabit(TrackingType.BOOLEAN, "Gym", FrequencyType.CUSTOM, 1)

        val state = presenter(date = monday.plusDays(1)).state.value

        assertTrue(state.habits.single().isRestingToday)
    }

    @Test
    fun togglingANumericHabitWritesNothing() {
        val id = createHabit(TrackingType.NUMERIC, "Water")
        val presenter = presenter()

        presenter.toggle(id)

        assertNull(runBlocking { repository.dailyLog(id, monday) })
        assertEquals(TodayPresenter.NUMERIC_NEEDS_INPUT, presenter.lastError.value)
    }

    /**
     * Only the use case rejects a write on a rest day; the repository would accept it. So this test
     * fails if the presenter ever talks to the repository directly instead of going through the
     * use case. A negative value would not discriminate: both layers reject that.
     */
    @Test
    fun aRestDayWriteIsRefusedByTheUseCasePath() {
        val id = createHabit(TrackingType.BOOLEAN, "Gym", FrequencyType.CUSTOM, 1)
        val presenter = presenter(date = monday.plusDays(1))

        presenter.record(id, 1.0)

        assertNull(runBlocking { repository.dailyLog(id, monday.plusDays(1)) })
        assertNotNull(presenter.lastError.value)
        assertTrue(presenter.lastError.value!!.contains("not a scheduled day"))
    }

    @Test
    fun aRejectedWriteSurfacesTheUseCaseMessage() {
        val id = createHabit(TrackingType.NUMERIC, "Water")
        val presenter = presenter()

        presenter.record(id, -1.0)

        assertNotNull(presenter.lastError.value)
        assertTrue(presenter.lastError.value!!.contains("negative"))
        assertNull(runBlocking { repository.dailyLog(id, monday) })

        presenter.dismissError()
        assertNull(presenter.lastError.value)
    }

    @Test
    fun theCreateActionAppearsOnlyOnceTheListHasRows() {
        assertFalse(TodayPresenter.showsCreateAction(TodayUiState(monday, emptyList(), isLoading = false)))
        // Loading with rows still hides the action: the list is not real yet.
        val loading = TodayUiState(
            date = monday,
            habits = listOf(
                TodayHabit(
                    id = 1,
                    name = "x",
                    icon = "star",
                    color = "#5B8C6A",
                    trackingType = TrackingType.BOOLEAN,
                    targetValue = 1.0,
                    unit = null,
                    isScheduledToday = true,
                    status = null,
                    actualValue = 0.0,
                ),
            ),
            isLoading = true,
        )
        assertFalse(TodayPresenter.showsCreateAction(loading))

        val id = createHabit(TrackingType.BOOLEAN, "Read")
        val state = presenter().state.value
        assertEquals(1, state.habits.size)
        assertTrue(TodayPresenter.showsCreateAction(state))
        assertNotNull(id)
    }

    @Test
    fun archivedHabitsLeaveTheList() {
        val id = createHabit(TrackingType.BOOLEAN, "Read")
        runBlocking { ArchiveHabitUseCase(repository)(id, monday, now) }

        val state = presenter().state.value

        assertTrue(state.habits.isEmpty())
    }
}
