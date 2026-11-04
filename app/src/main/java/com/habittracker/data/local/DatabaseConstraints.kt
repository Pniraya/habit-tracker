package com.habittracker.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database-level backstops for the invariants that DATABASE.md 4 declares as CHECK constraints.
 *
 * Room cannot emit CHECK constraints from @Entity annotations, so the same rules are installed as
 * BEFORE INSERT/UPDATE triggers. Triggers keep the guarantee for *any* writer, including a raw
 * UPDATE, which is what the DATABASE.md 12 test list requires. They are created idempotently on
 * open as well as on create, so a restored or imported database is covered too.
 */
object DatabaseConstraints {

    val HABITS_INSERT: String = """
        CREATE TRIGGER IF NOT EXISTS trg_habits_insert
        BEFORE INSERT ON habits
        WHEN length(trim(NEW.name)) = 0
            OR NEW.target_value <= 0
            OR NEW.tracking_type NOT IN ('BOOLEAN', 'NUMERIC')
            OR (NEW.tracking_type = 'BOOLEAN' AND (NEW.target_value <> 1 OR NEW.unit IS NOT NULL))
            OR (NEW.tracking_type = 'NUMERIC' AND (NEW.unit IS NULL OR length(trim(NEW.unit)) = 0))
        BEGIN
            SELECT RAISE(ABORT, 'habits: invalid row');
        END
    """.trimIndent()

    val HABITS_UPDATE: String = """
        CREATE TRIGGER IF NOT EXISTS trg_habits_update
        BEFORE UPDATE ON habits
        WHEN length(trim(NEW.name)) = 0
            OR NEW.target_value <= 0
            OR NEW.tracking_type NOT IN ('BOOLEAN', 'NUMERIC')
            OR (NEW.tracking_type = 'BOOLEAN' AND (NEW.target_value <> 1 OR NEW.unit IS NOT NULL))
            OR (NEW.tracking_type = 'NUMERIC' AND (NEW.unit IS NULL OR length(trim(NEW.unit)) = 0))
        BEGIN
            SELECT RAISE(ABORT, 'habits: invalid row');
        END
    """.trimIndent()

    val SCHEDULES_INSERT: String = """
        CREATE TRIGGER IF NOT EXISTS trg_habit_schedules_insert
        BEFORE INSERT ON habit_schedules
        WHEN NEW.frequency_type NOT IN ('DAILY', 'WEEKDAYS', 'CUSTOM')
            OR NEW.weekday_mask < 1
            OR NEW.weekday_mask > 127
            OR (NEW.effective_until IS NOT NULL AND NEW.effective_until < NEW.effective_from)
            OR (NEW.frequency_type = 'DAILY' AND NEW.weekday_mask <> 127)
            OR (NEW.frequency_type = 'WEEKDAYS' AND NEW.weekday_mask <> 31)
        BEGIN
            SELECT RAISE(ABORT, 'habit_schedules: invalid schedule');
        END
    """.trimIndent()

    val SCHEDULES_UPDATE: String = """
        CREATE TRIGGER IF NOT EXISTS trg_habit_schedules_update
        BEFORE UPDATE ON habit_schedules
        WHEN NEW.frequency_type NOT IN ('DAILY', 'WEEKDAYS', 'CUSTOM')
            OR NEW.weekday_mask < 1
            OR NEW.weekday_mask > 127
            OR (NEW.effective_until IS NOT NULL AND NEW.effective_until < NEW.effective_from)
            OR (NEW.frequency_type = 'DAILY' AND NEW.weekday_mask <> 127)
            OR (NEW.frequency_type = 'WEEKDAYS' AND NEW.weekday_mask <> 31)
        BEGIN
            SELECT RAISE(ABORT, 'habit_schedules: invalid schedule');
        END
    """.trimIndent()

    val LOGS_INSERT: String = """
        CREATE TRIGGER IF NOT EXISTS trg_habit_logs_insert
        BEFORE INSERT ON habit_logs
        WHEN NEW.status NOT IN ('IN_PROGRESS', 'COMPLETED')
            OR NEW.actual_value < 0
            OR NEW.target_snapshot <= 0
            OR (NEW.duration_seconds IS NOT NULL AND NEW.duration_seconds < 0)
            OR ((NEW.status = 'COMPLETED') <> (NEW.actual_value >= NEW.target_snapshot))
            OR ((NEW.status = 'COMPLETED') <> (NEW.completed_at IS NOT NULL))
            OR (NEW.actual_value NOT IN (0, 1)
                AND EXISTS (SELECT 1 FROM habits h
                            WHERE h.id = NEW.habit_id AND h.tracking_type = 'BOOLEAN'))
        BEGIN
            SELECT RAISE(ABORT, 'habit_logs: invalid log');
        END
    """.trimIndent()

    val LOGS_UPDATE: String = """
        CREATE TRIGGER IF NOT EXISTS trg_habit_logs_update
        BEFORE UPDATE ON habit_logs
        WHEN NEW.status NOT IN ('IN_PROGRESS', 'COMPLETED')
            OR NEW.actual_value < 0
            OR NEW.target_snapshot <= 0
            OR (NEW.duration_seconds IS NOT NULL AND NEW.duration_seconds < 0)
            OR ((NEW.status = 'COMPLETED') <> (NEW.actual_value >= NEW.target_snapshot))
            OR ((NEW.status = 'COMPLETED') <> (NEW.completed_at IS NOT NULL))
            OR (NEW.actual_value NOT IN (0, 1)
                AND EXISTS (SELECT 1 FROM habits h
                            WHERE h.id = NEW.habit_id AND h.tracking_type = 'BOOLEAN'))
        BEGIN
            SELECT RAISE(ABORT, 'habit_logs: invalid log');
        END
    """.trimIndent()

    val REMINDERS_INSERT: String = """
        CREATE TRIGGER IF NOT EXISTS trg_habit_reminders_insert
        BEFORE INSERT ON habit_reminders
        WHEN NEW.enabled NOT IN (0, 1)
        BEGIN
            SELECT RAISE(ABORT, 'habit_reminders: enabled must be 0 or 1');
        END
    """.trimIndent()

    val REMINDERS_UPDATE: String = """
        CREATE TRIGGER IF NOT EXISTS trg_habit_reminders_update
        BEFORE UPDATE ON habit_reminders
        WHEN NEW.enabled NOT IN (0, 1)
        BEGIN
            SELECT RAISE(ABORT, 'habit_reminders: enabled must be 0 or 1');
        END
    """.trimIndent()

    fun statements(): List<String> = listOf(
        HABITS_INSERT,
        HABITS_UPDATE,
        SCHEDULES_INSERT,
        SCHEDULES_UPDATE,
        LOGS_INSERT,
        LOGS_UPDATE,
        REMINDERS_INSERT,
        REMINDERS_UPDATE,
    )

    fun apply(db: SupportSQLiteDatabase) {
        statements().forEach(db::execSQL)
    }

    fun callback(): RoomDatabase.Callback = object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) = apply(db)

        override fun onOpen(db: SupportSQLiteDatabase) = apply(db)
    }
}
