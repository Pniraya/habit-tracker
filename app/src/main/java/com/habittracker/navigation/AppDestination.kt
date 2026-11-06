package com.habittracker.navigation

import androidx.annotation.DrawableRes
import com.habittracker.R

/** The three top-level destinations of V1. Icons are the mockup's line artwork, not Material's. */
enum class AppDestination(
    val route: String,
    val label: String,
    @DrawableRes val icon: Int,
) {
    Today(AppRoutes.TODAY, "今日", R.drawable.ic_tab_today),
    Statistics(AppRoutes.STATISTICS, "统计", R.drawable.ic_tab_stats),
    Profile(AppRoutes.PROFILE, "我的", R.drawable.ic_tab_profile),
}
