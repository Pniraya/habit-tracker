package com.habittracker.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pins the JSON contract in DATABASE.md 11 without needing Android or a JSON parser. */
class BackupJsonTest {

    private fun empty(exportedAt: Long = 1793750400000) = BackupDocument(
        exportedAt = exportedAt,
        habits = emptyList(),
        schedules = emptyList(),
        logs = emptyList(),
        reminders = emptyList(),
    )

    @Test
    fun anEmptyBackupStillCarriesTheFormatHeader() {
        val json = BackupJson.encode(empty())
        assertTrue(json.startsWith("{\n"))
        assertTrue(json.contains("\"format\": \"habit-tracker-backup\""))
        assertTrue(json.contains("\"schemaVersion\": 1"))
        assertTrue(json.contains("\"exportedAt\": 1793750400000"))
        assertTrue(json.contains("\"habits\": []"))
        assertTrue(json.contains("\"schedules\": []"))
        assertTrue(json.contains("\"logs\": []"))
        assertTrue(json.contains("\"reminders\": []"))
        assertTrue(json.endsWith("}\n"))
    }

    @Test
    fun aHabitKeepsItsColumnNamesAndIsoDates() {
        val json = BackupJson.encode(
            empty().copy(
                habits = listOf(
                    BackupHabit(
                        id = 7,
                        name = "晨跑",
                        icon = "custom:1.png",
                        color = "#5B8C6A",
                        trackingType = "NUMERIC",
                        targetValue = 2.5,
                        unit = "公里",
                        createdAt = 1_700_000_000_000,
                        updatedAt = 1_700_000_100_000,
                        archivedAt = 1_700_000_200_000,
                        createdOn = "2026-11-06",
                        archivedOn = "2026-11-08",
                    ),
                ),
            ),
        )
        assertTrue(json.contains("\"tracking_type\": \"NUMERIC\""))
        assertTrue(json.contains("\"target_value\": 2.5"))
        assertTrue(json.contains("\"created_on\": \"2026-11-06\""))
        assertTrue(json.contains("\"archived_on\": \"2026-11-08\""))
    }

    @Test
    fun nullsAreWrittenAsJsonNullNotOmitted() {
        val json = BackupJson.encode(
            empty().copy(
                habits = listOf(
                    BackupHabit(
                        id = 1,
                        name = "阅读",
                        icon = "sprout",
                        color = "#5B8C6A",
                        trackingType = "BOOLEAN",
                        targetValue = 1.0,
                        unit = null,
                        createdAt = 1,
                        updatedAt = 2,
                        archivedAt = null,
                        createdOn = "2026-11-06",
                        archivedOn = null,
                    ),
                ),
            ),
        )
        assertTrue(json.contains("\"unit\": null"))
        assertTrue(json.contains("\"archived_at\": null"))
        assertTrue(json.contains("\"archived_on\": null"))
        assertTrue(json.contains("\"target_value\": 1,"))
    }

    @Test
    fun quoteEscapesWhatUsersCanType() {
        assertEquals("\"a\\\"b\"", BackupJson.quote("a\"b"))
        assertEquals("\"a\\\\b\"", BackupJson.quote("a\\b"))
        assertEquals("\"a\\nb\"", BackupJson.quote("a\nb"))
        assertEquals("\"tab\\there\"", BackupJson.quote("tab\there"))
        assertEquals("\"\\u0001\"", BackupJson.quote("\u0001"))
    }

    @Test
    fun remindersKeepTheirWallClockTimeAndFlag() {
        val json = BackupJson.encode(
            empty().copy(
                reminders = listOf(
                    BackupReminder(
                        id = 3,
                        habitId = 1,
                        reminderTime = "07:30:00",
                        enabled = true,
                        createdAt = 10,
                        updatedAt = 20,
                    ),
                ),
            ),
        )
        assertTrue(json.contains("\"reminder_time\": \"07:30:00\""))
        assertTrue(json.contains("\"enabled\": true"))
    }

    @Test
    fun theSuggestedFileNameIsStableAndDated() {
        assertEquals(
            "habit-tracker-backup-2026-11-06.json",
            BackupExporter.fileName("2026-11-06"),
        )
    }
}
