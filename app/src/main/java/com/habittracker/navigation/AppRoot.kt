package com.habittracker.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.habittracker.feature.profile.ProfileScreen
import com.habittracker.feature.statistics.StatisticsScreen
import com.habittracker.feature.today.TodayScreen
import com.habittracker.feature.today.TodayViewModel

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Today.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AppDestination.Today.route) {
                val context = LocalContext.current
                val todayViewModel: TodayViewModel = viewModel(factory = TodayViewModel.factory(context))
                val state by todayViewModel.state.collectAsState()
                val lastError by todayViewModel.lastError.collectAsState()
                TodayScreen(
                    state = state,
                    lastError = lastError,
                    onToggle = todayViewModel::toggle,
                    onDismissError = todayViewModel::dismissError,
                )
            }
            composable(AppDestination.Statistics.route) { StatisticsScreen() }
            composable(AppDestination.Profile.route) { ProfileScreen() }
        }
    }
}
