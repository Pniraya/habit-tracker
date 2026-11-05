package com.habittracker.feature.create

import com.habittracker.data.local.entity.FrequencyType
import com.habittracker.data.local.entity.TrackingType
import com.habittracker.domain.usecase.CreateHabitUseCase
import com.habittracker.domain.usecase.FakeHabitRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class CreateHabitPresenterTest {

    private val repository = FakeHabitRepository()
    private val today = LocalDate.of(2026, 11, 2)
    private val now = 1_762_300_000_000L

    private fun presenter() = CreateHabitPresenter(
        createHabit = CreateHabitUseCase(repository),
        scope = CoroutineScope(Dispatchers.Unconfined),
        today = { today },
        now = { now },
    )

    @Test
    fun blankNameIsRejectedWithoutTouchingTheRepository() {
        val presenter = presenter()
        presenter.onNameChange("   ")

        presenter.save()

        assertEquals(CreateHabitPresenter.BLANK_NAME, presenter.form.value.error)
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun numericTargetMustBeANumber() {
        val presenter = presenter()
        presenter.onNameChange("Water")
        presenter.onTrackingTypeChange(TrackingType.NUMERIC)
        presenter.onTargetValueChange("abc")

        presenter.save()

        assertEquals(CreateHabitPresenter.BAD_TARGET, presenter.form.value.error)
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun numericTargetMustBePositive() {
        val presenter = presenter()
        presenter.onNameChange("Water")
        presenter.onTrackingTypeChange(TrackingType.NUMERIC)
        presenter.onTargetValueChange("0")

        presenter.save()

        assertEquals(CreateHabitPresenter.BAD_TARGET, presenter.form.value.error)
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun customFrequencyNeedsAtLeastOneDay() {
        val presenter = presenter()
        presenter.onNameChange("Gym")
        presenter.onFrequencyChange(FrequencyType.CUSTOM)

        presenter.save()

        assertEquals(CreateHabitPresenter.NO_WEEKDAYS, presenter.form.value.error)
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun dailyBooleanHabitGetsTheAllDaysMask() {
        val presenter = presenter()
        presenter.onNameChange("  Read  ")

        presenter.save()

        assertNotNull(presenter.form.value.createdHabitId)
        assertNull(presenter.form.value.error)
        val habitual = runBlocking { repository.getHabit(presenter.form.value.createdHabitId!!) }
        assertEquals("Read", habitual!!.name)
        assertEquals(TrackingType.BOOLEAN, habitual.trackingType)
        assertEquals(1.0, habitual.targetValue, 0.0)
        val schedule = repository.storedSchedule(habitual.id)
        assertEquals(127, schedule!!.weekdayMask)
    }

    @Test
    fun customWeekdaysProduceTheMask() {
        val presenter = presenter()
        presenter.onNameChange("Gym")
        presenter.onFrequencyChange(FrequencyType.CUSTOM)
        presenter.onWeekdayToggle(DayOfWeek.MONDAY)
        presenter.onWeekdayToggle(DayOfWeek.WEDNESDAY)

        presenter.save()

        val id = presenter.form.value.createdHabitId!!
        assertEquals(0b0000101, repository.storedSchedule(id)!!.weekdayMask)
    }

    /**
     * The presenter does not validate the unit; the repository does (HabitRules.normalizeTarget).
     * This is a rule-coverage test, not a call-chain test: the fake mirrors the production rules.
     */
    @Test
    fun numericHabitWithoutUnitIsRefused() {
        val presenter = presenter()
        presenter.onNameChange("Water")
        presenter.onTrackingTypeChange(TrackingType.NUMERIC)
        presenter.onTargetValueChange("2000")

        presenter.save()

        assertNotNull(presenter.form.value.error)
        assertTrue(presenter.form.value.error!!.contains("needs a unit"))
        assertNull(presenter.form.value.createdHabitId)
    }

    @Test
    fun editingTheNameClearsAPreviousError() {
        val presenter = presenter()
        presenter.save()
        assertNotNull(presenter.form.value.error)

        presenter.onNameChange("Read")

        assertNull(presenter.form.value.error)
        assertFalse(presenter.form.value.isSaving)
    }
}
