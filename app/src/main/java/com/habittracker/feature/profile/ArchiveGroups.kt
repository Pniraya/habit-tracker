package com.habittracker.feature.profile

import com.habittracker.domain.model.Habit
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** One archived day under a month, e.g. 11 月 6 日 with the habits archived that day. */
data class ArchiveDay(val date: LocalDate, val label: String, val habits: List<Habit>)

/** One archive month, the parent level of the profile page's archive tree. */
data class ArchiveMonth(val key: String, val label: String, val days: List<ArchiveDay>)

/**
 * Groups archived habits as 月 → 日 (the parent/child structure the product asked for).
 *
 * The database stays normalised: `habits.archived_on` is the single source of truth (DATABASE.md
 * 8.3), and this is a pure projection over it. Storing month/date as separate parent rows would
 * duplicate data that is already derivable and would need a schema migration, which the baseline
 * does not allow without an ADR.
 */
object ArchiveGroups {

    private val dayFormatter = DateTimeFormatter.ofPattern("M 月 d 日")

    fun group(habits: List<Habit>): List<ArchiveMonth> {
        val dated = habits.mapNotNull { habit -> habit.archivedOn?.let { date -> habit to date } }
        return dated
            .groupBy { (_, date) -> YearMonth.from(date) }
            .toSortedMap(compareByDescending { it })
            .map { (month, rows) ->
                ArchiveMonth(
                    key = month.toString(),
                    label = month.year.toString() + " 年 " + month.monthValue + " 月",
                    days = rows
                        .groupBy { (_, date) -> date }
                        .toSortedMap(compareByDescending { it })
                        .map { (date, dayRows) ->
                            ArchiveDay(
                                date = date,
                                label = date.format(dayFormatter),
                                habits = dayRows.map { it.first }.sortedBy { it.name },
                            )
                        },
                )
            }
    }
}
