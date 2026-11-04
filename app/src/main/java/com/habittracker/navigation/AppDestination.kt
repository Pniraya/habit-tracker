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
    Today("today", "今日", Icons.Filled.Home),
    Statistics("statistics", "统计", Icons.Filled.DateRange),
    Profile("profile", "我的", Icons.Filled.Person),
}
