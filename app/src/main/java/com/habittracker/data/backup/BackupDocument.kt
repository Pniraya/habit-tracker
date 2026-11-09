package com.habittracker.data.backup

/**
 * Plain values for the JSON backup (DATABASE.md 11). Kept free of Room and Android types so the
 * encoder runs under the JVM tests, and free of java.time so the file format stays explicit.
 */
data class BackupHabit(
    val id: Long,
    val name: String,
    val icon: String,
    val color: String,
    val trackingType: String,
    val targetValue: Double,
    val unit: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val archivedAt: Long?,
    val createdOn: String,
    val archivedOn: String?,
)

data class BackupSchedule(
    val id: Long,
    val habitId: Long,
    val frequencyType: String,
    val weekdayMask: Int,
    val effectiveFrom: String,
    val effectiveUntil: String?,
    val createdAt: Long,
)

data class BackupLog(
    val id: Long,
    val habitId: Long,
    val logDate: String,
    val actualValue: Double,
    val targetSnapshot: Double,
    val status: String,
    val completedAt: Long?,
    val durationSeconds: Int?,
    val createdAt: Long,
    val updatedAt: Long,
)

data class BackupReminder(
    val id: Long,
    val habitId: Long,
    val reminderTime: String,
    val enabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

data class BackupDocument(
    val exportedAt: Long,
    val habits: List<BackupHabit>,
    val schedules: List<BackupSchedule>,
    val logs: List<BackupLog>,
    val reminders: List<BackupReminder>,
)
