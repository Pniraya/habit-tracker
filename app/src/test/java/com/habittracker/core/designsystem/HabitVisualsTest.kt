package com.habittracker.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pins the presentation rules the mockups encode, without loading any Compose type. */
class HabitVisualsTest {

    @Test
    fun aHexColourBecomesTheStoredArgbValue() {
        assertEquals(0xFF5B8C6A.toInt(), HabitVisuals.parseColor("#5B8C6A"))
        assertEquals(0xFFF2A65A.toInt(), HabitVisuals.parseColor("#F2A65A"))
        assertEquals(0xFF8FAFC4.toInt(), HabitVisuals.parseColor(" 8FAFC4 "))
    }

    @Test
    fun anUnusableColourFallsBackInsteadOfCrashingTheList() {
        assertEquals(0xFF5B8C6A.toInt(), HabitVisuals.parseColor("star"))
        assertEquals(0xFF5B8C6A.toInt(), HabitVisuals.parseColor("#GGGGGG"))
        assertEquals(0xFF5B8C6A.toInt(), HabitVisuals.parseColor("#12345"))
        assertEquals(0xFF000000.toInt(), HabitVisuals.parseColor("bogus", fallback = 0xFF000000.toInt()))
    }

    @Test
    fun theRingIsFullOnlyWhenEveryScheduledHabitIsDone() {
        assertEquals(0f, HabitVisuals.ringSweep(0, 0), 0f)
        assertEquals(0f, HabitVisuals.ringSweep(0, 5), 0f)
        assertEquals(0.4f, HabitVisuals.ringSweep(2, 5), 0.0001f)
        assertEquals(1f, HabitVisuals.ringSweep(5, 5), 0f)
    }

    @Test
    fun theRingNeverOvershootsWhenLogsOutnumberThePlan() {
        // A habit can be completed before a plan edit drops it from today's plan; the arc must not
        // wrap past the full circle.
        assertEquals(1f, HabitVisuals.ringSweep(7, 5), 0f)
    }

    @Test
    fun theCardMessageCountsWhatIsLeft() {
        assertEquals("再完成 2 个，今天就圆满了", HabitVisuals.todayMessage(3, 5))
        assertEquals("再完成 1 个，今天就圆满了", HabitVisuals.todayMessage(4, 5))
    }

    @Test
    fun theCardMessageCelebratesOnlyWhenEverythingIsDone() {
        assertEquals("今天也做到了", HabitVisuals.todayMessage(5, 5))
        assertEquals("今天也做到了", HabitVisuals.todayMessage(6, 5))
    }

    @Test
    fun noPlanMeansNoCelebration() {
        assertEquals("还没有习惯，先添加一个吧", HabitVisuals.todayMessage(0, 0))
    }

    @Test
    fun theHeaderBadgeShowsTheBestRealStreak() {
        assertEquals(0, HabitVisuals.bestStreak(emptyList()))
        assertEquals(0, HabitVisuals.bestStreak(listOf(0, 0)))
        assertEquals(12, HabitVisuals.bestStreak(listOf(3, 12, 6)))
        assertEquals(0, HabitVisuals.bestStreak(listOf(-4)))
    }

    @Test
    fun theBadgeTextMatchesTheMockupCopy() {
        assertEquals("连续 12 天", HabitVisuals.badgeText(12))
    }

    @Test
    fun anUnknownIconStillRendersAsTheSprout() {
        // "star" is what habits created before the picker stored.
        assertEquals(HabitVisuals.ICON_SPROUT, HabitVisuals.iconKey("star"))
        assertEquals(HabitVisuals.ICON_SPROUT, HabitVisuals.iconKey(""))
        assertEquals(HabitVisuals.ICON_WATER, HabitVisuals.iconKey(HabitVisuals.ICON_WATER))
    }

    @Test
    fun aCustomIconKeepsItsFileNameAndIsNotReplacedByTheSprout() {
        val icon = HabitVisuals.CUSTOM_PREFIX + "a1b2c3.png"
        assertTrue(HabitVisuals.isCustom(icon))
        assertEquals("a1b2c3.png", HabitVisuals.customFileName(icon))
        assertEquals(icon, HabitVisuals.iconKey(icon))
        assertFalse(HabitVisuals.isCustom(HabitVisuals.ICON_SPROUT))
        assertNull(HabitVisuals.customFileName(HabitVisuals.ICON_SPROUT))
    }

    @Test
    fun aMalformedCustomIconCannotEscapeTheIconsDirectory() {
        // The value comes from the database, so a hand-edited row must not read outside filesDir.
        assertNull(HabitVisuals.customFileName(HabitVisuals.CUSTOM_PREFIX))
        assertNull(HabitVisuals.customFileName(HabitVisuals.CUSTOM_PREFIX + "../secrets.png"))
        assertNull(HabitVisuals.customFileName(HabitVisuals.CUSTOM_PREFIX + "dir/icon.png"))
        assertNull(HabitVisuals.customFileName(HabitVisuals.CUSTOM_PREFIX + "dir\\\\icon.png"))
    }

    @Test
    fun anUnknownColourNameFallsBackToMoss() {
        assertEquals(HabitVisuals.COLOR_MOSS, HabitVisuals.colorKey("#000000"))
        assertEquals(HabitVisuals.COLOR_APRICOT, HabitVisuals.colorKey(HabitVisuals.COLOR_APRICOT))
    }
}
