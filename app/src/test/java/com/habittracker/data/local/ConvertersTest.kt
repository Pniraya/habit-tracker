package com.habittracker.data.local

import com.habittracker.data.local.entity.FrequencyType
import com.habittracker.data.local.entity.LogStatus
import com.habittracker.data.local.entity.TrackingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * The storage formats the rest of the toolbox assumes: verify-dao.mjs hand-writes dates like
 * "2026-01-05" and compares them with BETWEEN, so the converter must produce exactly that shape.
 * Nothing else proves the two agree (TASK-024).
 */
class ConvertersTest {

    private val converters = Converters()

    private fun date(value: String): LocalDate = LocalDate.parse(value)

    @Test
    fun localDateIsStoredAsIsoText() {
        // Zero padding matters: BETWEEN compares text, so "2026-1-5" would sort wrongly.
        assertEquals("2026-01-05", converters.localDateToString(date("2026-01-05")))
        assertEquals("2026-10-05", converters.localDateToString(date("2026-10-05")))
        assertEquals(date("2026-12-31"), converters.stringToLocalDate("2026-12-31"))
        assertNull(converters.localDateToString(null))
        assertNull(converters.stringToLocalDate(null))
    }

    @Test
    fun leapDaysSurviveTheRoundTrip() {
        val leapDay = date("2028-02-29")
        assertEquals("2028-02-29", converters.localDateToString(leapDay))
        assertEquals(leapDay, converters.stringToLocalDate("2028-02-29"))
    }

    @Test
    fun anUnparseableDateIsRejectedInsteadOfStored() {
        val failure = runCatching { converters.stringToLocalDate("2026-1-5") }.exceptionOrNull()
        assertTrue(failure is java.time.format.DateTimeParseException)
    }

    @Test
    fun localTimeIsStoredWithSeconds() {
        assertEquals("08:05:00", converters.localTimeToString(LocalTime.of(8, 5)))
        assertEquals(LocalTime.of(23, 59, 59), converters.stringToLocalTime("23:59:59"))
        assertNull(converters.localTimeToString(null))
    }

    @Test
    fun enumsAreStoredByName() {
        assertEquals("BOOLEAN", converters.trackingTypeToString(TrackingType.BOOLEAN))
        assertEquals(TrackingType.NUMERIC, converters.stringToTrackingType("NUMERIC"))
        assertEquals("WEEKDAYS", converters.frequencyTypeToString(FrequencyType.WEEKDAYS))
        assertEquals(FrequencyType.CUSTOM, converters.stringToFrequencyType("CUSTOM"))
        assertEquals("COMPLETED", converters.logStatusToString(LogStatus.COMPLETED))
        assertEquals(LogStatus.IN_PROGRESS, converters.stringToLogStatus("IN_PROGRESS"))
    }

    @Test
    fun everyEnumMemberSurvivesARoundTrip() {
        TrackingType.values().forEach { value ->
            assertEquals(value, converters.stringToTrackingType(converters.trackingTypeToString(value)))
        }
        FrequencyType.values().forEach { value ->
            assertEquals(value, converters.stringToFrequencyType(converters.frequencyTypeToString(value)))
        }
        LogStatus.values().forEach { value ->
            assertEquals(value, converters.stringToLogStatus(converters.logStatusToString(value)))
        }
    }

    @Test
    fun aStoredDateParsesBackToTheSameDay() {
        val original = date("2026-03-29")
        assertEquals(original, converters.stringToLocalDate(converters.localDateToString(original)))
    }
}
