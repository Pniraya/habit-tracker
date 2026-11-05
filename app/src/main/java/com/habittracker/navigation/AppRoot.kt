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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.habittracker.data.local.HabitDatabase
import com.habittracker.data.repository.RoomHabitRepository
import com.habittracker.domain.usecase.CreateHabitUseCase
import com.habittracker.feature.create.CreateHabitPresenter
import com.habittracker.feature.create.CreateHabitScreen
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
                    onRecord = todayViewModel::record,
                    onCreate = { navController.navigate(AppRoutes.CREATE_HABIT) },
                    onDismissError = todayViewModel::dismissError,
                )
            }
            composable(AppRoutes.CREATE_HABIT) {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val presenter = remember {
                    val repository = RoomHabitRepository(HabitDatabase.getInstance(context))
                    CreateHabitPresenter(CreateHabitUseCase(repository)::invoke, scope)
                }
                val form by presenter.form.collectAsState()
                LaunchedEffect(form.createdHabitId) {
                    if (form.createdHabitId != null) navController.popBackStack()
                }
                CreateHabitScreen(
                    form = form,
                    onNameChange = presenter::onNameChange,
                    onTrackingTypeChange = presenter::onTrackingTypeChange,
                    onTargetValueChange = presenter::onTargetValueChange,
                    onUnitChange = presenter::onUnitChange,
                    onFrequencyChange = presenter::onFrequencyChange,
                    onWeekdayToggle = presenter::onWeekdayToggle,
                    onSave = presenter::save,
                    onCancel = { navController.popBackStack() },
                )
            }
            composable(AppDestination.Statistics.route) { StatisticsScreen() }
            composable(AppDestination.Profile.route) { ProfileScreen() }
        }
    }
}
