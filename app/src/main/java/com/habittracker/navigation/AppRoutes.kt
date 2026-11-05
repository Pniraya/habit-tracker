package com.habittracker.navigation

/**
 * Route names for the three top-level destinations. Kept free of Compose types so the navigation
 * contract stays a plain-JVM unit-testable value.
 */
object AppRoutes {
    const val TODAY: String = "today"
    const val STATISTICS: String = "statistics"
    const val PROFILE: String = "profile"

    val all: List<String> = listOf(TODAY, STATISTICS, PROFILE)
}
