package com.habittracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.habittracker.data.local.dao.HabitDao
import com.habittracker.data.local.dao.HabitLogDao
import com.habittracker.data.local.dao.HabitReminderDao
import com.habittracker.data.local.dao.HabitScheduleDao
import com.habittracker.data.local.entity.HabitEntity
import com.habittracker.data.local.entity.HabitLogEntity
import com.habittracker.data.local.entity.HabitReminderEntity
import com.habittracker.data.local.entity.HabitScheduleEntity

/**
 * Room schema version 1 (DATABASE.md 10). No destructive fallback: every future schema change
 * must ship a Migration.
 */
@Database(
    entities = [
        HabitEntity::class,
        HabitScheduleEntity::class,
        HabitLogEntity::class,
        HabitReminderEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class HabitDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao

    abstract fun habitScheduleDao(): HabitScheduleDao

    abstract fun habitLogDao(): HabitLogDao

    abstract fun habitReminderDao(): HabitReminderDao

    companion object {
        const val NAME: String = "habit-tracker.db"

        @Volatile
        private var instance: HabitDatabase? = null

        fun getInstance(context: Context): HabitDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): HabitDatabase =
            Room.databaseBuilder(context, HabitDatabase::class.java, NAME)
                .addCallback(DatabaseConstraints.callback())
                .build()
    }
}
