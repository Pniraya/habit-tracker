package com.habittracker.data.local

import androidx.room.TypeConverter
import com.habittracker.data.local.entity.FrequencyType
import com.habittracker.data.local.entity.LogStatus
import com.habittracker.data.local.entity.TrackingType
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Storage formats fixed by DATABASE.md 2: dates and times are ISO text so they sort
 * lexicographically; enums are stored by name so member order can change freely.
 */
class Converters {

    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.format(DateTimeFormatter.ISO_LOCAL_DATE)

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun localTimeToString(value: LocalTime?): String? = value?.format(TIME_FORMAT)

    @TypeConverter
    fun stringToLocalTime(value: String?): LocalTime? = value?.let { LocalTime.parse(it, TIME_FORMAT) }

    @TypeConverter
    fun trackingTypeToString(value: TrackingType): String = value.name

    @TypeConverter
    fun stringToTrackingType(value: String): TrackingType = TrackingType.valueOf(value)

    @TypeConverter
    fun frequencyTypeToString(value: FrequencyType): String = value.name

    @TypeConverter
    fun stringToFrequencyType(value: String): FrequencyType = FrequencyType.valueOf(value)

    @TypeConverter
    fun logStatusToString(value: LogStatus): String = value.name

    @TypeConverter
    fun stringToLogStatus(value: String): LogStatus = LogStatus.valueOf(value)

    private companion object {
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    }
}
