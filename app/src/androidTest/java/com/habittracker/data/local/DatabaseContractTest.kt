package com.habittracker.data.local

import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.habittracker.data.local.entity.FrequencyType
import com.habittracker.data.local.entity.HabitEntity
import com.habittracker.data.local.entity.HabitLogEntity
import com.habittracker.data.local.entity.HabitScheduleEntity
import com.habittracker.data.local.entity.LogStatus
import com.habittracker.data.local.entity.TrackingType
import com.habittracker.data.repository.RoomHabitRepository
import com.habittracker.domain.model.NewHabit
import com.habittracker.domain.model.NewSchedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.TimeZone

/**
 * Device-side proof that Room, not only the hand-verifiable SQL, enforces the contract (TASK-026).
 *
 * The harness in `tools/verify-*.mjs` runs the exported schema and trigger DDL through node:sqlite.
 * This file runs the *production* open path (`DatabaseConstraints.callback()`) through Room on a
 * device and checks the things node:sqlite cannot see: callback registration, entity/converter
 * round-trips, Flow invalidation and transaction rollback of real repository code.
 *
 * Each test opens its own temporary database file, so a test rerun never depends on earlier state.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseContractTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val today = LocalDate.of(2026, 11, 6)
    private val now = 1_760_000_000_000L

    private lateinit var name: String
    private lateinit var database: HabitDatabase
    private lateinit var repository: RoomHabitRepository

    @Before
    fun openFreshDatabase() {
        name = "contract-" + System.nanoTime() + ".db"
        database = open()
        repository = RoomHabitRepository(database)
    }

    @After
    fun closeAndDelete() {
        database.close()
        context.deleteDatabase(name)
    }

    /** Mirrors HabitDatabase.getInstance: same callback, only the file name differs. */
    private fun open(): HabitDatabase =
        Room.databaseBuilder(context, HabitDatabase::class.java, name)
            .addCallback(DatabaseConstraints.callback())
            .build()

    private fun triggers(): List<String> =
        database.openHelper.readableDatabase
            .query("SELECT name FROM sqlite_master WHERE type = 'trigger' ORDER BY name")
            .use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(cursor.getString(0))
                }
            }

    private fun count(table: String): Int =
        database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM " + table).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    private fun booleanHabit(name: String = "Read") = NewHabit(
        name = name,
        icon = "sprout",
        color = "#5B8C6A",
        trackingType = TrackingType.BOOLEAN,
        targetValue = 1.0,
        unit = null,
        initialSchedule = NewSchedule(FrequencyType.DAILY, null),
    )

    @Test
    fun theProductionOpenPathInstallsEveryTriggerAndReopenKeepsThem() = runBlocking {
        val expected = listOf(
            "trg_habit_logs_insert",
            "trg_habit_logs_update",
            "trg_habit_reminders_insert",
            "trg_habit_reminders_update",
            "trg_habit_schedules_insert",
            "trg_habit_schedules_update",
            "trg_habits_insert",
            "trg_habits_update",
        )
        assertEquals(expected, triggers())

        // The trigger is not decoration: an invalid row must be rejected on the production path.
        val invalid = HabitEntity(
            name = "   ",
            icon = "sprout",
            color = "#5B8C6A",
            trackingType = TrackingType.BOOLEAN,
            targetValue = 1.0,
            unit = null,
            createdAt = now,
            updatedAt = now,
            createdOn = today,
        )
        assertRejected { database.habitDao().insert(invalid) }
        assertEquals(0, count("habits"))

        // DATABASE.md 12: a reopened database (restore/import) must be covered too.
        database.close()
        database = open()
        repository = RoomHabitRepository(database)
        assertEquals(expected, triggers())
        assertRejected { database.habitDao().insert(invalid) }
        assertEquals(0, count("habits"))
    }

    @Test
    fun convertersRoundTripDatesEnumsNullableUnitAndNumericPrecision() = runBlocking {
        val original = TimeZone.getDefault()
        try {
            // Leap day, REAL precision, null unit on a BOOLEAN habit and a non-null unit on NUMERIC.
            val numeric = NewHabit(
                name = "Water",
                icon = "water",
                color = "#8FAFC4",
                trackingType = TrackingType.NUMERIC,
                targetValue = 0.1 + 0.2,
                unit = "L",
                initialSchedule = NewSchedule(FrequencyType.WEEKDAYS, null),
            )
            val numericId = repository.createHabit(numeric, LocalDate.of(2028, 2, 29), now)
            val numericRow = requireNotNull(repository.getHabit(numericId))
            assertEquals(LocalDate.of(2028, 2, 29), numericRow.createdOn)
            assertEquals(TrackingType.NUMERIC, numericRow.trackingType)
            assertEquals(0.1 + 0.2, numericRow.targetValue, 0.0)
            assertEquals("L", numericRow.unit)
            assertNull(numericRow.archivedOn)

            val booleanId = repository.createHabit(booleanHabit(), today, now)
            val booleanRow = requireNotNull(repository.getHabit(booleanId))
            assertEquals(TrackingType.BOOLEAN, booleanRow.trackingType)
            assertNull(booleanRow.unit)

            val schedule = repository.activeSchedule(booleanId, today)
            assertNotNull(schedule)
            assertEquals(FrequencyType.DAILY, schedule!!.frequencyType)
            assertEquals(127, schedule.weekdayMask)
            assertEquals(today, schedule.effectiveFrom)
            assertNull(schedule.effectiveUntil)

            // A completed log exercises the enum converter and the completed_at pairing.
            val log = repository.recordDailyValue(booleanId, today, 1.0, now)
            assertEquals(LogStatus.COMPLETED, log.status)
            assertEquals(today, log.date)
            val reloaded = requireNotNull(repository.dailyLog(booleanId, today))
            assertEquals(LogStatus.COMPLETED, reloaded.status)
            assertEquals(now, reloaded.completedAt)

            // Converting through a different default time zone must not shift a calendar day.
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            assertEquals(today, requireNotNull(repository.getHabit(booleanId)).createdOn)
            assertEquals(today, requireNotNull(repository.dailyLog(booleanId, today)).date)
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test
    fun aWriteInvalidatesTheObservedFlow() = runBlocking {
        val active = database.habitDao().observeActive()
        coroutineScope {
            val firstRows = async(Dispatchers.IO) { active.first { it.isNotEmpty() } }
            // Give Room's InvalidationTracker time to register the observer before the write.
            delay(500)
            repository.createHabit(booleanHabit(), today, now)
            val rows = withTimeout(10_000) { firstRows.await() }
            assertEquals(listOf("Read"), rows.map { it.name })
        }
    }

    @Test
    fun deletingAHabitCascadesEveryChildRow() = runBlocking {
        val habitId = repository.createHabit(booleanHabit(), today, now)
        repository.recordDailyValue(habitId, today, 1.0, now)
        database.habitReminderDao().insert(
            com.habittracker.data.local.entity.HabitReminderEntity(
                habitId = habitId,
                reminderTime = java.time.LocalTime.of(21, 30),
                enabled = true,
                createdAt = now,
                updatedAt = now,
            ),
        )
        assertEquals(1, count("habits"))
        assertEquals(1, count("habit_schedules"))
        assertEquals(1, count("habit_logs"))
        assertEquals(1, count("habit_reminders"))

        repository.deleteHabit(habitId)

        assertEquals(0, count("habits"))
        assertEquals(0, count("habit_schedules"))
        assertEquals(0, count("habit_logs"))
        assertEquals(0, count("habit_reminders"))
        assertNull(repository.getHabit(habitId))
    }

    @Test
    fun aFailedScheduleChangeRollsBackTheVersionItAlreadyClosed() = runBlocking {
        val habitId = repository.createHabit(booleanHabit(), today, now)
        val before = repository.scheduleVersions(habitId).single()
        assertNull(before.effectiveUntil)

        // replaceSchedule closes the open version first and only then rejects the invalid mask,
        // so without a working transaction the closed version would survive the exception.
        try {
            repository.changeSchedule(habitId, FrequencyType.CUSTOM, 0, today.plusDays(1), now)
            fail("a weekday mask of 0 must be rejected")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("weekday mask"))
        }

        val after = repository.scheduleVersions(habitId)
        assertEquals(1, after.size)
        assertEquals(before.id, after.single().id)
        assertNull(after.single().effectiveUntil)
        assertEquals(1, count("habit_schedules"))
    }

    private suspend fun assertRejected(block: suspend () -> Unit) {
        try {
            block()
            fail("the database accepted a row that violates DATABASE.md 4")
        } catch (expected: SQLiteException) {
            assertTrue(expected.message!!.contains("invalid"))
        }
    }
}
