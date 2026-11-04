package com.habittracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "icon") val icon: String,
    @ColumnInfo(name = "color") val color: String,
    @ColumnInfo(name = "tracking_type") val trackingType: TrackingType,
    @ColumnInfo(name = "target_value", defaultValue = "1.0") val targetValue: Double = 1.0,
    @ColumnInfo(name = "unit") val unit: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "archived_at") val archivedAt: Long? = null,
    @ColumnInfo(name = "created_on") val createdOn: LocalDate,
    @ColumnInfo(name = "archived_on") val archivedOn: LocalDate? = null,
)
