package com.habittracker.feature.profile

import com.habittracker.data.local.entity.TrackingType
import com.habittracker.domain.model.Habit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ArchiveGroupsTest {

    private fun habit(id: Long, name: String, archivedOn: LocalDate?) = Habit(
        id = id,
        name = name,
        icon = "sprout",
        color = "#5B8C6A",
        trackingType = TrackingType.BOOLEAN,
        targetValue = 1.0,
        unit = null,
        createdOn = LocalDate.of(2026, 1, 1),
        archivedOn = archivedOn,
    )

    @Test
    fun theMonthIsTheParentAndTheDayIsTheChild() {
        val groups = ArchiveGroups.group(
            listOf(
                habit(1, "Walk", LocalDate.of(2026, 11, 6)),
                habit(2, "Read", LocalDate.of(2026, 11, 6)),
                habit(3, "Water", LocalDate.of(2026, 11, 5)),
                habit(4, "Run", LocalDate.of(2026, 10, 30)),
            ),
        )

        assertEquals(2, groups.size)
        assertEquals("2026 年 11 月", groups[0].label)
        assertEquals(listOf("11 月 6 日", "11 月 5 日"), groups[0].days.map { it.label })
        // Within a day the rows keep the archive page's stable alphabetical order.
        assertEquals(listOf("Read", "Walk"), groups[0].days[0].habits.map { it.name })
        assertEquals("2026 年 10 月", groups[1].label)
    }

    @Test
    fun newestMonthsAndDaysComeFirst() {
        val groups = ArchiveGroups.group(
            listOf(
                habit(1, "Old", LocalDate.of(2025, 12, 31)),
                habit(2, "New", LocalDate.of(2026, 2, 1)),
                habit(3, "Mid", LocalDate.of(2026, 1, 15)),
            ),
        )

        assertEquals(listOf("2026 年 2 月", "2026 年 1 月", "2025 年 12 月"), groups.map { it.label })
    }

    @Test
    fun habitsInsideADayAreSortedByName() {
        val groups = ArchiveGroups.group(
            listOf(
                habit(1, "Walk", LocalDate.of(2026, 11, 6)),
                habit(2, "Read", LocalDate.of(2026, 11, 6)),
                habit(3, "Meditate", LocalDate.of(2026, 11, 6)),
            ),
        )

        assertEquals(listOf("Meditate", "Read", "Walk"), groups[0].days[0].habits.map { it.name })
    }

    @Test
    fun aHabitWithoutAnArchiveDateIsNotRendered() {
        // The archived list should never contain one, but a stray row must not become a fake
        // 1970 group or crash the page.
        val groups = ArchiveGroups.group(
            listOf(
                habit(1, "Walk", LocalDate.of(2026, 11, 6)),
                habit(2, "Active", null),
            ),
        )

        assertEquals(1, groups.size)
        assertEquals(listOf(1L), groups[0].days.flatMap { it.habits }.map { it.id })
    }

    @Test
    fun anEmptyArchiveProducesNoGroups() {
        assertTrue(ArchiveGroups.group(emptyList()).isEmpty())
    }
}
