package com.habittracker.data.local.entity

/**
 * Enums persisted by name, never by ordinal (DATABASE.md 5): reordering members must not
 * change stored values.
 */
enum class TrackingType { BOOLEAN, NUMERIC }

enum class FrequencyType { DAILY, WEEKDAYS, CUSTOM }

enum class LogStatus { IN_PROGRESS, COMPLETED }
