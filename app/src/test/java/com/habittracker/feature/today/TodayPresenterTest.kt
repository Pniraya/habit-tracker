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

    private fun createHabit(
        trackingType: TrackingType,
        name: String,
        frequency: FrequencyType = FrequencyType.DAILY,
        mask: Int? = null,
        createdOn: LocalDate = monday,
    ): Long =
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
                createdOn,
                now,
            )
        }

    @Test
    fun everyHabitCountsItsOwnDays() = runBlocking {
        // Two habits with different histories. If a batch read mixed the rows up, both rows would
        // report the same streak; the reviewer showed that no test noticed that (TASK-022 U4).
        val steady = createHabit(TrackingType.BOOLEAN, "Steady", createdOn = monday.minusDays(2))
        val fresh = createHabit(TrackingType.BOOLEAN, "Fresh", createdOn = monday.minusDays(2))
        listOf(monday.minusDays(2), monday.minusDays(1), monday).forEach { day ->
            RecordHabitProgressUseCase(repository)(steady, day, 1.0, now)
        }
        // Fresh records only yesterday, so it is scheduled but not completed today: a wrong log
        // attribution would also show up in isCompleted, not just in the streak.
        RecordHabitProgressUseCase(repository)(fresh, monday.minusDays(1), 1.0, now)

        val rows = presenter().state.value.habits.associateBy { it.id }

        assertEquals(3, rows.getValue(steady).currentStreak)
        assertEquals(1, rows.getValue(fresh).currentStreak)
        assertTrue(rows.getValue(steady).isCompleted)
        assertFalse(rows.getValue(fresh).isCompleted)
    }

    @Test
    fun theWholeListLoadsInAConstantNumberOfQueries() = runBlocking {
        repeat(5) { index -> createHabit(TrackingType.BOOLEAN, "Habit " + index) }
        repository.resetReadCalls()

        val state = presenter().state.value

        assertEquals(5, state.habits.size)
        // Four batch queries for five habits; the per-habit version needed twenty.
        assertEquals(4, repository.readCalls)
    }

    @Test
    fun aTapRefreshesTheListOnce() = runBlocking {
        val habitId = createHabit(TrackingType.BOOLEAN, "Read")
        repeat(4) { index -> createHabit(TrackingType.BOOLEAN, "Other " + index) }
        val presenter = presenter()
        repository.resetReadCalls()

        presenter.toggle(habitId)

        assertTrue(presenter.state.value.habits.first { it.id == habitId }.isCompleted)
        // One read in the write path (does this date have a schedule?) plus one four-query rebuild of
        // the whole list: five, whatever the number of habits. Rebuilding per habit would be 21.
        assertEquals(5, repository.readCalls)
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
    fun aRecordedDecimalIsShownAsADecimal() {
        // Regression: the subtitle cast actualValue to Long, so 0.5 rendered as "0".
        assertEquals("0.5 / 2000 ml", TodayPresenter.progressText(0.5, 2000.0, "ml"))
    }

    @Test
    fun wholeNumbersKeepNoTrailingDecimalPoint() {
        assertEquals("2000 / 2000 ml", TodayPresenter.progressText(2000.0, 2000.0, "ml"))
        assertEquals("0.25 / 1.5 h", TodayPresenter.progressText(0.25, 1.5, "h"))
        assertEquals("0 / 8", TodayPresenter.progressText(0.0, 8.0, null))
    }

    @Test
    fun theRowShowsTheStreakComputedFromCompletedDays() = runBlocking {
        val habitId = createHabit(TrackingType.BOOLEAN, "Read", createdOn = monday.minusDays(2))
        // Three consecutive days ending today, recorded through the real use case.
        listOf(monday.minusDays(2), monday.minusDays(1), monday).forEach { date ->
            RecordHabitProgressUseCase(repository)(habitId, date, 1.0, now)
        }

        val state = presenter(monday).state.value
        val habit = state.habits.single()

        assertEquals(3, habit.currentStreak)
        assertEquals(3, habit.longestStreak)
    }

    @Test
    fun anInProgressDayDoesNotJoinTheStreak() = runBlocking {
        // 1 ml of a 2000 ml target is IN_PROGRESS, not COMPLETED; such a day must not count.
        val habitId = createHabit(TrackingType.NUMERIC, "Water", createdOn = monday.minusDays(1))
        RecordHabitProgressUseCase(repository)(habitId, monday.minusDays(1), 1.0, now)

        // Yesterday stayed IN_PROGRESS at 1 of 2000 ml; today is not due to be counted yet.
        val habit = presenter(monday).state.value.habits.single()

        assertEquals(0, habit.currentStreak)
        assertEquals(0, habit.longestStreak)
        assertEquals(
            LogStatus.IN_PROGRESS,
            repository.dailyLog(habitId, monday.minusDays(1))!!.status,
        )
    }

    @Test
    fun everyScheduleVersionFeedsTheStreak() = runBlocking {
        // Daily from the 2nd, then Mon/Wed/Fri from the 5th. The completed 2nd-4th were due under the
        // old version, so reading only the newest version would wrongly report 0.
        val today = LocalDate.of(2026, 11, 5)
        val habitId = createHabit(TrackingType.BOOLEAN, "Read", createdOn = LocalDate.of(2026, 11, 2))
        listOf(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 4)).forEach {
            RecordHabitProgressUseCase(repository)(habitId, it, 1.0, now)
        }
        repository.changeSchedule(habitId, FrequencyType.CUSTOM, 21, today, now)

        val habit = presenter(today).state.value.habits.single()

        assertEquals(3, habit.currentStreak)
    }

    @Test
    fun aStreakTextStaysQuietWhenThereIsNothingToSay() {
        assertNull(TodayPresenter.streakText(0, 0))
        assertEquals("最长 7 天", TodayPresenter.streakText(0, 7))
        assertEquals("连续 3 天", TodayPresenter.streakText(3, 3))
        assertEquals("连续 3 天 · 最长 7 天", TodayPresenter.streakText(3, 7))
    }

    @Test
    fun aCommaDecimalSeparatorBecomesADot() {
        assertEquals("0.5", normaliseQuantityInput("0,5"))
        assertEquals("0.5", normaliseQuantityInput("0.5"))
    }

    @Test
    fun theNumericDialogPrefillsTheRecordedDecimal() {
        // Regression guard for the round-1 defect: a prefill of "0" let one tap on 记录 overwrite
        // the stored 0.5 with 0. Mutation: replacing prefillText's body with toLong() must fail here.
        assertEquals("0.5", TodayPresenter.prefillText(0.5))
        assertEquals("0.25", TodayPresenter.prefillText(0.25))
        assertEquals("2000", TodayPresenter.prefillText(2000.0))
        assertEquals("", TodayPresenter.prefillText(0.0))
    }

    @Test
    fun aThousandsGroupIsNotRewrittenIntoADecimal() {
        // Rewriting 1,234 to 1.234 would silently divide the value by 1000.
        assertEquals("1,234", normaliseQuantityInput("1,234"))
        assertEquals("10,000", normaliseQuantityInput("10,000"))
        // Not a thousands group, so it is treated as a decimal separator and then fails to parse.
        assertEquals("1.2.3", normaliseQuantityInput("1,2,3"))
        // 0,123 is a legitimate 0.123: a leading zero rules out a thousands group.
        assertEquals("0.123", normaliseQuantityInput("0,123"))
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
