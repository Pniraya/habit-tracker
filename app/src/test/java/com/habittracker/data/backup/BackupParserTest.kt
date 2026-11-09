package com.habittracker.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pins the validation rules of ADR-019 that protect the database from a bad file. */
class BackupParserTest {

    private fun document() = BackupDocument(
        exportedAt = 1794187025585,
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
                archivedAt = null,
                createdOn = "2026-11-06",
                archivedOn = null,
            ),
        ),
        schedules = listOf(
            BackupSchedule(
                id = 3,
                habitId = 7,
                frequencyType = "DAILY",
                weekdayMask = 127,
                effectiveFrom = "2026-11-06",
                effectiveUntil = null,
                createdAt = 1_700_000_000_000,
            ),
        ),
        logs = listOf(
            BackupLog(
                id = 11,
                habitId = 7,
                logDate = "2026-11-08",
                actualValue = 1.0,
                targetSnapshot = 2.0,
                status = "IN_PROGRESS",
                completedAt = null,
                durationSeconds = null,
                createdAt = 1_700_000_200_000,
                updatedAt = 1_700_000_200_000,
            ),
        ),
        reminders = listOf(
            BackupReminder(
                id = 5,
                habitId = 7,
                reminderTime = "07:30:00",
                enabled = true,
                createdAt = 1_700_000_000_000,
                updatedAt = 1_700_000_000_000,
            ),
        ),
    )

    private fun errorsOf(json: String): List<String> {
        val result = BackupParser.parse(json)
        assertTrue("expected an Invalid result for: " + json, result is BackupParseResult.Invalid)
        return (result as BackupParseResult.Invalid).errors
    }

    @Test
    fun anExportedFileParsesBackIntoTheSameDocument() {
        val result = BackupParser.parse(BackupJson.encode(document()))
        assertTrue(result is BackupParseResult.Ready)
        val parsed = (result as BackupParseResult.Ready).document
        assertEquals(1, parsed.habits.size)
        assertEquals("晨跑", parsed.habits[0].name)
        assertEquals("2026-11-08", parsed.logs[0].logDate)
        assertEquals("07:30:00", parsed.reminders[0].reminderTime)
        assertEquals(true, parsed.reminders[0].enabled)
    }

    @Test
    fun anUnexpectedFormatOrVersionIsRejected() {
        val wrongFormat = BackupJson.encode(document()).replace("habit-tracker-backup", "other-app")
        assertTrue(errorsOf(wrongFormat).any { it.contains("format") })
        val wrongVersion = BackupJson.encode(document()).replace("\"schemaVersion\": 1", "\"schemaVersion\": 2")
        assertTrue(errorsOf(wrongVersion).any { it.contains("schemaVersion") })
    }

    @Test
    fun aDanglingHabitReferenceIsRejected() {
        val broken = BackupJson.encode(document()).replace("\"habit_id\": 7", "\"habit_id\": 999")
        assertTrue(errorsOf(broken).any { it.contains("habit_id 999") })
    }

    @Test
    fun anUnknownEnumOrDateIsRejected() {
        val badTracking = BackupJson.encode(document()).replace("\"tracking_type\": \"NUMERIC\"", "\"tracking_type\": \"WEIGHT\"")
        assertTrue(errorsOf(badTracking).any { it.contains("tracking_type") })
        val badDate = BackupJson.encode(document()).replace("\"log_date\": \"2026-11-08\"", "\"log_date\": \"08/11/2026\"")
        assertTrue(errorsOf(badDate).any { it.contains("log_date") })
    }

    @Test
    fun aCompletedLogWithoutATimestampIsRejected() {
        val broken = BackupJson.encode(document()).replace("\"status\": \"IN_PROGRESS\"", "\"status\": \"COMPLETED\"")
        assertTrue(errorsOf(broken).any { it.contains("completed_at") })
    }

    @Test
    fun aDailyScheduleMustUseTheFullWeekMask() {
        val broken = BackupJson.encode(document()).replace("\"weekday_mask\": 127", "\"weekday_mask\": 31")
        assertTrue(errorsOf(broken).any { it.contains("127") })
    }

    @Test
    fun aMalformedFileFailsWithAReadableMessage() {
        assertTrue(errorsOf("{\"format\": ").any { it.contains("JSON") })
    }

    @Test
    fun namesWithQuotesAndNewlinesSurviveTheRoundTrip() {
        val tricky = document().copy(
            habits = listOf(document().habits[0].copy(name = "读\"书\"\n计划")),
        )
        val result = BackupParser.parse(BackupJson.encode(tricky))
        assertTrue(result is BackupParseResult.Ready)
        assertEquals("读\"书\"\n计划", (result as BackupParseResult.Ready).document.habits[0].name)
    }

    @Test
    fun aFileWithExtraKeysIsStillAccepted() {
        // Forward compatibility: a newer exporter may add fields the current app ignores.
        val json = BackupJson.encode(document()).replace("\"format\"", "\"extra\": true,\n  \"format\"")
        assertTrue(BackupParser.parse(json) is BackupParseResult.Ready)
    }
}
