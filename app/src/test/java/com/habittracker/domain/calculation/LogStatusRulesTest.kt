package com.habittracker.domain.calculation

import com.habittracker.data.local.entity.LogStatus
import com.habittracker.data.local.entity.TrackingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LogStatusRulesTest {

    private val now = 1_762_300_000_000L

    @Test
    fun booleanZeroIsInProgressWithoutTimestamp() {
        val result = LogStatusRules.evaluate(TrackingType.BOOLEAN, 0.0, 1.0, null, now)
        assertEquals(LogStatus.IN_PROGRESS, result.status)
        assertNull(result.completedAt)
    }

    @Test
    fun booleanOneCompletesAndStampsTheMoment() {
        val result = LogStatusRules.evaluate(TrackingType.BOOLEAN, 1.0, 1.0, null, now)
        assertEquals(LogStatus.COMPLETED, result.status)
        assertEquals(now, result.completedAt)
    }

    @Test
    fun numericBelowTargetIsInProgress() {
        val result = LogStatusRules.evaluate(TrackingType.NUMERIC, 800.0, 2000.0, null, now)
        assertEquals(LogStatus.IN_PROGRESS, result.status)
        assertNull(result.completedAt)
    }

    @Test
    fun numericAtTargetCompletes() {
        val result = LogStatusRules.evaluate(TrackingType.NUMERIC, 2000.0, 2000.0, null, now)
        assertEquals(LogStatus.COMPLETED, result.status)
    }

    @Test
    fun numericOverTargetStaysCompleted() {
        val result = LogStatusRules.evaluate(TrackingType.NUMERIC, 2500.0, 2000.0, null, now)
        assertEquals(LogStatus.COMPLETED, result.status)
    }

    @Test
    fun firstCompletionMomentIsNotOverwrittenByLaterIncrements() {
        val first = 1_762_000_000_000L
        val result = LogStatusRules.evaluate(TrackingType.NUMERIC, 2200.0, 2000.0, first, now)
        assertEquals(LogStatus.COMPLETED, result.status)
        assertEquals(first, result.completedAt)
    }

    @Test
    fun droppingBelowTargetClearsTheTimestamp() {
        val result = LogStatusRules.evaluate(TrackingType.NUMERIC, 1500.0, 2000.0, 1_762_000_000_000L, now)
        assertEquals(LogStatus.IN_PROGRESS, result.status)
        assertNull(result.completedAt)
    }

    @Test
    fun reachingTargetAgainAfterUndoStampsTheNewMoment() {
        val result = LogStatusRules.evaluate(TrackingType.NUMERIC, 2000.0, 2000.0, null, now)
        assertEquals(now, result.completedAt)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeValueIsRejected() {
        LogStatusRules.evaluate(TrackingType.NUMERIC, -1.0, 2000.0, null, now)
    }

    @Test(expected = IllegalArgumentException::class)
    fun booleanValueOtherThanZeroOrOneIsRejected() {
        LogStatusRules.evaluate(TrackingType.BOOLEAN, 0.5, 1.0, null, now)
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonPositiveTargetIsRejected() {
        LogStatusRules.evaluate(TrackingType.NUMERIC, 10.0, 0.0, null, now)
    }
}
