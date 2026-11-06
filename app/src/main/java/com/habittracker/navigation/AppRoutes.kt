package com.habittracker.navigation

/**
 * Route names for the three top-level destinations. Kept free of Compose types so the navigation
 * contract stays a plain-JVM unit-testable value.
 */
object AppRoutes {
    const val TODAY: String = "today"
    const val CREATE_HABIT: String = "create-habit"
    const val STATISTICS: String = "statistics"
    const val PROFILE: String = "profile"

    /** Detail route; not part of [all] because it is not a top-level destination. */
    const val HABIT_DETAIL: String = "habit/{habitId}"

    val all: List<String> = listOf(TODAY, STATISTICS, PROFILE)

    fun habitDetail(habitId: Long): String = "habit/" + habitId
}
