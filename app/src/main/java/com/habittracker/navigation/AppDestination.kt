package com.habittracker.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

/** The three top-level destinations of V1. */
enum class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Today(AppRoutes.TODAY, "今日", Icons.Filled.Home),
    Statistics(AppRoutes.STATISTICS, "统计", Icons.Filled.DateRange),
    Profile(AppRoutes.PROFILE, "我的", Icons.Filled.Person),
}
